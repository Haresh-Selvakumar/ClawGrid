"""Actual guard logic on synthetic flash/transport; never opens a device."""
import binascii
import hashlib
import json
import struct
import sys
from pathlib import Path
import pytest
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'scripts'))
from device_flash_guard import (parse_table,inspect_layout,selected_app,plan_application,
                               protected_digest,transfer_application,sha,validate_boot_only_drift)

def table(rows):
    data=b''.join(struct.pack('<HBBII16sI',0x50aa,t,s,o,n,l.encode(),0) for t,s,o,n,l in rows)
    data+=b'\xeb\xeb'+b'\xff'*14+hashlib.md5(data).digest()
    return data.ljust(4096,b'\xff')

def fixture():
    t=table([(1,2,0x9000,0x5000,'nvs'),(1,0,0xe000,0x2000,'otadata'),
             (0,0x10,0x10000,0x180000,'app0'),(0,0x11,0x190000,0x180000,'app1'),
             (1,0x82,0x310000,0xf0000,'storage')])
    data=bytearray(b'\xff'*0x400000);data[0x8000:0x9000]=t;data[0x10000]=0xe9
    data[0x190000]=0xe9
    image=bytearray(320);image[0]=0xe9;image[3]=0x20
    image[32:36]=b'\x32\x54\xcd\xab';image[80:96]=b'clawgrid_native\0'
    image+=b'ClawGrid:outputs=enabled;faults=default\0ClawGrid:load=led_indicators_10k_active_high\0ClawGrid:console=uart0\0ClawGrid:pir=unavailable\0'
    identity=dict(chip='esp32',rom_verified=True,mac='02:00:00:00:00:03',flash_bytes=len(data),dual_core=True,secure_boot=False,flash_encryption=False)
    sdk='CONFIG_IDF_TARGET="esp32"\nCONFIG_ESP_CONSOLE_UART_DEFAULT=y\n'
    return bytes(data),t,bytes(image),identity,sdk

def test_layout_discovered_and_app_only_plan_uses_actual_size():
    data,t,img,ident,sdk=fixture();layout=inspect_layout(data)
    assert layout['table_offset']==0x8000
    p=plan_application(data,ident,img,t,sdk,'C')
    assert p['offset']==0x10000 and p['partition_size']==0x180000
    assert p['allowed_write_ranges']==[dict(offset=0x10000,bytes=len(img))]

def test_ota_slot_is_selected_from_crc_metadata_not_s3_assumption():
    data,t,*_=fixture();d=bytearray(data)
    entry=struct.pack('<I',2)+b'\xff'*20+struct.pack('<II',2,binascii.crc32(struct.pack('<I',2),0xffffffff)&0xffffffff)
    d[0xe000:0xe020]=entry
    assert selected_app(bytes(d),inspect_layout(d))['offset']==0x190000
    d[0xe01c]^=1
    with pytest.raises(ValueError,match='Corrupt'):selected_app(bytes(d),inspect_layout(d))

@pytest.mark.parametrize('state',[0,1])
def test_unconfirmed_ota_state_refuses_write(state):
    data,*_=fixture();d=bytearray(data)
    d[0xe000:0xe020]=struct.pack('<I',1)+b'\xff'*20+struct.pack('<II',state,binascii.crc32(struct.pack('<I',1),0xffffffff)&0xffffffff)
    with pytest.raises(ValueError,match='Unconfirmed'):selected_app(d,inspect_layout(d))

@pytest.mark.parametrize('change',['wrong_chip','encryption','secure_boot','single_core','flash_size','no_rom','table_mismatch','oversize','image_chip'])
def test_incompatible_device_or_image_refused(change):
    data,t,img,ident,sdk=fixture()
    if change=='wrong_chip':ident['chip']='esp32s3'
    if change=='encryption':ident['flash_encryption']=True
    if change=='secure_boot':ident['secure_boot']=None
    if change=='single_core':ident['dual_core']=False
    if change=='flash_size':ident['flash_bytes']=0x1000000
    if change=='no_rom':ident['rom_verified']=False
    if change=='table_mismatch':t=table([(0,0,0x10000,0x200000,'factory')])
    if change=='oversize':img+=b'x'*0x180000
    if change=='image_chip':b=bytearray(img);b[12]=9;img=bytes(b)
    with pytest.raises(ValueError):plan_application(data,ident,img,t,sdk,'C')

def test_blank_or_ambiguous_map_requires_separate_initial_provisioning():
    with pytest.raises(ValueError):inspect_layout(b'\xff'*0x400000)
    data,t,img,ident,sdk=fixture();d=bytearray(data);d[0x10000]=0xff
    with pytest.raises(ValueError,match='initial provisioning'):plan_application(d,ident,img,t,sdk,'C')

def test_legacy_checksumless_table_requires_explicit_board_verified_path():
    data,t,img,ident,sdk=fixture();d=bytearray(data);t=bytearray(t)
    # Fixture has five entries; erase only its checksum entry, not any partition.
    t[5*32:]=b'\xff'*(4096-5*32);d[0x8000:0x9000]=t
    with pytest.raises(ValueError):inspect_layout(d)
    assert inspect_layout(d,allow_verified_legacy=True)['table_offset']==0x8000
    # Candidate remains a modern generated/checksummed table with equal layout.
    original_table=fixture()[1]
    assert plan_application(d,ident,img,original_table,sdk,'C',board_verified_legacy=True)['offset']==0x10000
    with pytest.raises(ValueError,match='runtime'):
        plan_application(d,ident,img,original_table,sdk+'CONFIG_PARTITION_TABLE_MD5=y\n','C',board_verified_legacy=True)

def test_normal_boot_nvs_drift_is_preserved_but_app_and_model_drift_refuse():
    data,t,img,ident,sdk=fixture();p=plan_application(data,ident,img,t,sdk,'C')
    d=bytearray(data);d[0x9000]=0
    validate_boot_only_drift(data,d,p)
    for at in (0x10000,0x310000,0x1000):
        changed=bytearray(d);changed[at]^=1
        with pytest.raises(ValueError):validate_boot_only_drift(data,changed,p)

def test_partition_checksum_and_overlap_fail_closed():
    data,t,*_=fixture();bad=bytearray(t);bad[12]^=1
    with pytest.raises(ValueError):parse_table(bad,len(data))
    overlap=table([(1,2,0x9000,0x8000,'nvs'),(0,0,0x10000,0x100000,'factory')])
    with pytest.raises(ValueError,match='Overlapping'):parse_table(overlap,len(data))

class FakeRom:
    def __init__(self,data,identity,fail=False):self.data=bytearray(data);self.ident=identity;self.writes=[];self.fail=fail
    def identity(self):return self.ident
    def read(self,start,size):return bytes(self.data[start:start+size])
    def write(self,start,data):
        self.writes.append((start,len(data)))
        # Simulate the physical erase behavior, including the final sector.
        end=(start+len(data)+4095)//4096*4096
        self.data[start:end]=b'\xff'*(end-start);self.data[start:start+len(data)]=data
        if self.fail and len(self.writes)==1:raise RuntimeError('Injected interrupted write')
    def verify(self,start,data):assert self.data[start:start+len(data)]==data

def setup_transfer(tmp_path):
    data,t,img,ident,sdk=fixture();plan=plan_application(data,ident,img,t,sdk,'C')
    a=tmp_path/'a.bin';b=tmp_path/'b.bin';c=tmp_path/'candidate.bin'
    a.write_bytes(data);b.write_bytes(data);c.write_bytes(img)
    return data,ident,plan,a,b,c

def test_transfer_preserves_last_sector_tail_and_all_protected_bytes(tmp_path):
    data,ident,p,a,b,c=setup_transfer(tmp_path);rom=FakeRom(data,ident)
    result=transfer_application(rom,p,ident,a,b,c,tmp_path/'result.json',tmp_path/'rollback.bin')
    assert result['complete'] and len(rom.writes)==1
    assert protected_digest(rom.data,p)==protected_digest(data,p)
    assert rom.writes[0]==(p['offset'],4096)

def test_interrupted_transfer_has_one_app_only_rollback(tmp_path):
    data,ident,p,a,b,c=setup_transfer(tmp_path);rom=FakeRom(data,ident,fail=True)
    with pytest.raises(RuntimeError,match='APP_AND_PROTECTED_BYTES_VERIFIED'):
        transfer_application(rom,p,ident,a,b,c,tmp_path/'result.json',tmp_path/'rollback.bin')
    assert rom.read(0,len(data))==data and len(rom.writes)==2
    assert all(at==p['offset'] for at,_ in rom.writes)
    assert rom.writes[1][1]==p['partition_size']

@pytest.mark.parametrize('change',['backup','live','identity','image'])
def test_transfer_drift_blocks_before_any_write(tmp_path,change):
    data,ident,p,a,b,c=setup_transfer(tmp_path);rom=FakeRom(data,ident.copy())
    if change=='backup':b.write_bytes(b'changed')
    if change=='live':rom.data[0x9000]=0
    if change=='identity':rom.ident['mac']='02:00:00:00:00:04'
    if change=='image':c.write_bytes(b'changed')
    with pytest.raises(ValueError):transfer_application(rom,p,ident,a,b,c,tmp_path/'result.json',tmp_path/'rollback.bin')
    assert not rom.writes
