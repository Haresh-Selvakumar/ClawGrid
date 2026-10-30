"""Host safety/stop checks for the four-clip preflight, never ESP inference."""
import argparse
import hashlib
import json
import shutil
import subprocess
import sys
from pathlib import Path
import pytest
from scripts.flash_native import build_environment
import scripts.light_reference_preflight as preflight

ROOT=Path(__file__).resolve().parents[2]

def test_frozen_contract_rejects_replacement_wrong_order_or_fifth_job(tmp_path):
    plan=(ROOT/'experiments/light-on-off-reference-preflight-v1.json').read_bytes()
    assert hashlib.sha256(plan).hexdigest()==preflight.FROZEN_PLAN_SHA256
    m=json.loads(plan);assert m['probability_threshold']==.7
    assert m['phone_recordings_authorized'] is False and len(m['fixtures'])==4
    assert [x['expected_id'] for x in m['fixtures']]==[1,2,0,0]
    cpp=tmp_path/'contract.cpp';exe=tmp_path/'contract.exe'
    cpp.write_text('''#include "light_preflight_contract.h"
#include <assert.h>
int main() {
 assert(!light_preflight_size(0,100));assert(!light_preflight_size(5,100));
 for(unsigned i=1;i<=4;++i) {
  const auto &f=kLightPreflightFixtures[i-1];
  assert(light_preflight_matches(i,f.bytes,f.crc));
  assert(!light_preflight_matches(i,f.bytes,f.crc^1));
  assert(!light_preflight_matches(i,f.bytes+2,f.crc));
 }
 assert(!light_preflight_matches(2,kLightPreflightFixtures[0].bytes,kLightPreflightFixtures[0].crc));
 assert(kLightPreflightFixtures[0].expected==1 && kLightPreflightFixtures[1].expected==2);
}''')
    subprocess.run([sys.executable,'-m','ziglang','c++','-std=c++17',
        '-I'+str(ROOT/'firmware/common/runtime'),str(cpp),'-o',str(exe)],capture_output=True,check=True,timeout=120)
    subprocess.run([str(exe)],capture_output=True,check=True,timeout=5)

def test_preflight_environment_cannot_select_other_role_or_output_mode():
    assert build_environment('B-speech',True,False,False,'light-preflight-v1')=='speech_intelligence_uart_light_preflight'
    for role,uart,diagnostic,bench in [('A',True,False,False),('B-speech',False,False,False),
                                     ('B-speech',True,True,False),('B-speech',True,False,True)]:
        with pytest.raises(RuntimeError):build_environment(role,uart,diagnostic,bench,'light-preflight-v1')

@pytest.mark.parametrize('failure_at',[1,3,None])
def test_harness_stops_first_failure_and_never_posts_proposals(tmp_path,monkeypatch,failure_at):
    plan=ROOT/'experiments/light-on-off-reference-preflight-v1.json'
    p=tmp_path/'experiments'/plan.name;p.parent.mkdir();p.write_bytes(plan.read_bytes())
    m=json.loads(plan.read_text())
    for f in m['fixtures']:
        dst=tmp_path/f['path'];dst.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(ROOT/f['path'],dst)
    cfg=dict(api_token='TEST_ONLY',node_a_mac='a',node_b_mac='b')
    (tmp_path/'local_credentials.json').write_text(json.dumps(cfg))
    identity=tmp_path/'identities.json';identity.write_text(json.dumps(dict(observations=[
        dict(role=x,port=x,mac=x.lower(),identity='ROM_MAC_MATCH') for x in ['A','B']])))
    class FakeBench:
        def __init__(self,*a,**k):self.stack=self;self.closed=False
        def request(self,*a):return dict(physical_outputs_enabled=False,diagnostic_faults_enabled=False)
        def poll(self,*a):pass
        def close(self):self.closed=True
    class Reply:
        def __init__(self,value,status):self.value=value;self.status=status
        def __enter__(self):return self
        def __exit__(self,*a):pass
        def read(self,*a):return json.dumps(self.value).encode()
    class Opener:
        job=0;sample=100;posts=[]
        def open(self,req,timeout):
            path=req.full_url.removeprefix('http://192.168.4.1')
            if req.data is not None:
                assert path=='/api/speech/reference?feed=light-v1'
                self.posts.append(path);self.job+=1
                return Reply(dict(queued=True,authorized=False,job=self.job),202)
            if path=='/api/state':
                self.sample+=1
                return Reply(dict(known=True,authenticated=True,age_ms=0,physical_outputs_enabled=False,
                    light=False,fan=False,boot_id=8,last_command=0,sample_uptime_ms=self.sample),200)
            if path=='/api/rtos':return Reply(dict(watchdog=True,deadline_misses=0,wifi_channel=1,reset_reason=1),200)
            if path=='/api/transaction':return Reply(dict(id=0,phase=0),200)
            assert path=='/api/speech'
            if not self.job:return Reply(dict(grammar_id='light-preflight-v1',model_name='mn6_en',phase=1,job=0,command_count=2),200)
            f=m['fixtures'][self.job-1];expected=f['expected_id'];accepted=expected!=0
            if self.job==failure_at:accepted=not accepted
            candidate=expected if accepted and expected else (1 if accepted else 0)
            return Reply(dict(job=self.job,phase=4,bound_room='bedroom',bound_boot=8,capture_id=self.job,
                screen_only=True,samples=f['samples'],pcm_crc32=f['crc32'],command_id=0,authorized=False,
                diagnostic_only=True,error_code=0,engine_errors=0,reference_detected=accepted,
                candidate_id=candidate,candidate_score=.8 if accepted else 0,
                raw_result_count=1 if accepted else 0,last_state=1 if accepted else 2),200)
    opener=Opener();opener.posts=[]
    monkeypatch.setattr(preflight,'ROOT',tmp_path);monkeypatch.setattr(preflight,'Bench',FakeBench)
    monkeypatch.setattr(preflight.urllib.request,'build_opener',lambda *a:opener)
    output=tmp_path/'result.json'
    code=preflight.run(argparse.Namespace(room='bedroom',node_a='A',node_b='B',identities=identity,output=output))
    d=json.loads(output.read_text());assert d['serial_closed'] and not d['control_issued'] and not d['proposals_issued']
    assert len(opener.posts)==(failure_at or 4)
    assert code==(1 if failure_at else 0)
    assert len(d['not_run'])==(4-failure_at if failure_at else 0)
    with pytest.raises(RuntimeError,match='no automatic rerun'):
        preflight.run(argparse.Namespace(output=output))
