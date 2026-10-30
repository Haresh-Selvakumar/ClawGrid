import json
import math
import hashlib
import struct
from pathlib import Path
import pytest
from scripts.configure_mn7_model import configure,PINNED_DATA_SHA
from scripts.mn7_migration import REGIONS,check_backup,validate_table,validate_candidate,sha

ROOT=Path(__file__).resolve().parents[2]
DATA=ROOT/'models/mn7-eval-v1/upstream/model/multinet_model/mn7_en/mn7_data'

def system():
    data=bytearray(b'\xff'*0x9000)
    rows=[('nvs',0x9000,0x5000),('otadata',0xe000,0x2000),('app0',0x10000,0x300000),('app1',0x310000,0x300000),('model',0x610000,0x600000),('storage',0xc10000,0x3f0000)]
    for i,(name,offset,size) in enumerate(rows):struct.pack_into('<HBBII16sI',data,0x8000+32*i,0x50aa,1,0,offset,size,name.encode(),0)
    return bytes(data)

def test_metadata_configuration_retains_every_acoustic_byte_and_index():
    original=DATA.read_bytes();assert sha(original)==PINNED_DATA_SHA
    result,r=configure(original);a=r['json_start'];b=a+r['json_bytes']
    assert result[:a]==original[:a] and result[b:]==original[b:] and len(result)==len(original)
    old=json.loads(original[a:b]);new=json.loads(result[a:b]);assert new.pop('det_threshold')==math.log(.7);assert new==old
    assert r['runtime_private_abi_access'] is False and r['public_setter_called'] is False

@pytest.mark.parametrize('probability',[.1,.699,.8,-1])
def test_no_threshold_tuning(probability):
    with pytest.raises(ValueError):configure(DATA.read_bytes(),probability)

def test_wrong_or_already_transformed_model_fails_closed():
    data=DATA.read_bytes();configured,_=configure(data)
    for bad in (b'',data[:-1],bytes([data[0]^1])+data[1:],configured):
        with pytest.raises(ValueError):configure(bad)

def test_actual_pinned_mn7_scalar_threshold_contract_is_not_reused_as_patch():
    # Independently audited MN7 setter clamps raw probability0.1..0.9999.
    # Qualification compares log scores; constructor JSON avoids that setter.
    scalar=lambda p:min(.9999,max(.1,p))
    assert scalar(.7)>0 and scalar(math.log(.7))>0
    assert math.exp(scalar(.7))>1 and math.exp(math.log(.7))==pytest.approx(.7)
    runtime=(ROOT/'speech/main/speech_runtime_mn7.cpp').read_text()
    for private in ('mn6_threshold.h','mn6_trace.h','set_det_threshold(','uintptr_t','+224','+228'):
        assert private not in runtime

def test_partition_change_and_unready_candidate_are_rejected(tmp_path):
    data=system();validate_table(data)
    changed=bytearray(data);struct.pack_into('<I',changed,0x8000+4,0xa000)
    with pytest.raises(RuntimeError):validate_table(changed)
    candidate=tmp_path/'candidate.json';candidate.write_text(json.dumps(dict(state='BLOCKED',threshold_verified=False,private_abi_access=False)))
    with pytest.raises(RuntimeError,match='not READY'):validate_candidate(candidate,{})

def test_two_backups_required_and_tampering_rejected(tmp_path):
    primary=tmp_path/'primary';other=tmp_path/'other';primary.mkdir();other.mkdir()
    m=dict(second_copy=str(other),regions={},private_files=[])
    for name,(address,size) in REGIONS.items():
        data=system() if name=='system' else b'\xff'*size
        for folder in (primary,other):(folder/(name+'.bin')).write_bytes(data)
        m['regions'][name]=dict(address=address,size=size,sha256=sha(data))
    (primary/'backup.json').write_text(json.dumps(m));check_backup(primary)
    p=other/'model.bin'
    with p.open('r+b') as f:f.write(b'\0')
    with pytest.raises(RuntimeError,match='missing/changed'):check_backup(primary)

def test_frozen_finite_budget_and_model_source_are_separate():
    m=json.loads((ROOT/'experiments/mn7-eval-v1.json').read_text())
    assert m['probability_threshold']==.7 and m['required_decoder_log_threshold']==math.log(.7)
    assert m['phrases']=={'1':'LIGHT ENABLE','2':'LIGHT DISABLE'}
    assert len(m['phone_trials'])==40 and m['post_pass_maximum_clips']==5
    assert m['preflight_maximum_jobs']==5 and m['frontend']['implementation']=='official ESP-SR AFE'

def test_failed_deploy_attempts_one_bounded_rollback_without_protected_writes(tmp_path,monkeypatch):
    import argparse
    import scripts.mn7_migration as transport
    primary=tmp_path/'backup';primary.mkdir()
    model=primary/'model.bin';model.write_bytes(b'\xff'*0x600000)
    app=primary/'app0.bin';app.write_bytes(b'\xff'*0x300000)
    (primary/'system.bin').write_bytes(system())
    candidate=tmp_path/'candidate.bin';candidate.write_bytes(b'candidate')
    cfg=dict(node_a_mac='01:02:03:04:05:06',node_b_mac='07:08:09:0a:0b:0c')
    m=dict(complete=True,mac=cfg['node_b_mac'],regions=dict(model=dict(sha256=sha(model.read_bytes()))))
    monkeypatch.setattr(transport,'check_backup',lambda p:m)
    monkeypatch.setattr(transport,'validate_candidate',lambda c,b:({},[(0x10000,candidate),(0x610000,candidate)]))
    monkeypatch.setattr(transport,'validate_image_mode',lambda *a:None)
    monkeypatch.setattr(transport,'validate_pair_identity',lambda *a:None)
    writes=[];resets=[]
    class FakeRom:
        def __init__(self,port,log):self.log=log
        def identity(self,expected):assert expected==cfg['node_b_mac']
        def run(self,parts,after='no-reset',timeout=420):
            if parts[0]=='read-flash':
                Path(parts[-1]).write_bytes(system()+b'\xff'*0x7000 if parts[1]=='0' else model.read_bytes())
            if parts[0]=='write-flash':
                writes.append(parts)
                if len(writes)==1:raise RuntimeError('injected partial write failure')
            if after=='hard-reset':resets.append(after)
            return ''
    monkeypatch.setattr(transport,'Rom',FakeRom)
    with pytest.raises(RuntimeError,match='verified flash restoration'):
        transport.transfer(argparse.Namespace(mode='deploy',backup=primary,port='FAKE',candidate=tmp_path/'unused'),cfg)
    assert len(writes)==2 and len(resets)==2
    for command in writes:
        addresses=command[7::2]
        assert set(addresses)=={'0x10000','0x610000'}

def test_preflight_plan_path_and_fingerprint_are_bound():
    from scripts.mn7_preflight import FROZEN_PLAN_SHA256
    assert sha((ROOT/'experiments/mn7-eval-v1.json').read_bytes())==FROZEN_PLAN_SHA256
