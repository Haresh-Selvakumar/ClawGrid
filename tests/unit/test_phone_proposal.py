"""Run the actual portable firmware parser/store; no serial or actuator access."""
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

def test_phone_intent_binding_and_existing_proposals(tmp_path):
    source = tmp_path / 'phone.cpp'
    exe = tmp_path / 'phone.exe'
    source.write_text('''#include "command_target.h"
#include <assert.h>
int main() {
 CommandTarget t; uint32_t capture=0,boot=0; ControlCommand command;
 assert(phone_proposal_fields("phone physical_bedroom bedroom light ON 77 20",t,capture,boot));
 assert(capture==77 && boot==20 && t.action==TargetAction::On);
 ProposalStore store; store.seed(1);
 assert(store.prepare(t,boot,true,100,false,capture)==ProposalResult::Ok);
 assert(store.confirm(store.id,t,21,true,101,command)==ProposalResult::Unavailable);
 assert(store.confirm(store.id,t,20,false,101,command)==ProposalResult::Unavailable);
 assert(store.confirm(store.id,t,20,true,15100,command)==ProposalResult::Expired);
 auto wrong=t;wrong.action=TargetAction::Off;
 assert(store.confirm(store.id,wrong,20,true,101,command)==ProposalResult::Mismatch);
 assert(store.confirm(store.id,t,20,true,101,command)==ProposalResult::Ok);
 assert(command.boot==20 && command.op==ControlOp::Device && command.action==ACTION_ON);
 store.consume();assert(store.confirm(store.id,t,20,true,101,command)==ProposalResult::Consumed);
 const char *bad[]={
 "phone physical_bedroom bedroom light ON 0 20", "phone physical_bedroom bedroom light ON 77 0",
 "phone physical_bedroom bedroom light ON 4294967296 20", "phone physical_bedroom bedroom light ON -1 20",
 "phone physical_bedroom bedroom light ON 77 20 extra", "phone physical_bedroom bedroom light ON 77",
 "phone virtual_rooms bedroom light ON 77 20", "phone physical_bedroom kitchen light ON 77 20",
 "phone physical_bedroom bedroom heater ON 77 20", "phone physical_bedroom bedroom fan AUTO 77 20", "phone physical_bedroom bedroom light TOGGLE 77 20",
 "speech physical_bedroom bedroom light ON 77 20", "phone physical_bedroom bedroom light ON 77 1.0"};
 for(auto text:bad) assert(!phone_proposal_fields(text,t,capture,boot));
 assert(phone_proposal_fields("phone physical_bedroom bedroom light OFF 4294967295 4294967295",t,capture,boot));
 assert(t.action==TargetAction::Off && capture==UINT32_MAX && boot==UINT32_MAX);
 assert(phone_proposal_fields("phone physical_bedroom bedroom fan ON 77 20",t,capture,boot));
 assert(t.device==TargetDevice::Fan && t.action==TargetAction::On && capture==77 && boot==20);
 assert(phone_proposal_fields("phone physical_bedroom bedroom fan OFF 78 20",t,capture,boot));
 assert(t.device==TargetDevice::Fan && t.action==TargetAction::Off);
 char first[24]; uint32_t job=1;
 assert(proposal_fields("manual physical_bedroom bedroom light ON 0",first,t,job) && job==0);
 assert(proposal_fields("speech physical_bedroom bedroom light OFF 6",first,t,job) && job==6);
 assert(proposal_fields("12 physical_bedroom bedroom light OFF",first,t,job,true) && job==12);
}''')
    common = ROOT / 'firmware/common'
    result = subprocess.run([sys.executable, '-m', 'ziglang', 'c++', '-std=c++17',
        *['-I'+str(common/x) for x in ('runtime','safety_kernel','protocol')], str(source), '-o', str(exe)],
        capture_output=True, text=True, timeout=120)
    assert result.returncode == 0, result.stderr
    subprocess.run([str(exe)], check=True, capture_output=True, timeout=5)
