"""Short boot-bound replacement/cancellation acceptance through existing control API.

Never drives GPIO or bypasses SafetyKernel. Expected outputs remain OFF. Each
configuration is submitted once; failed cases get one explicitly logged cancel.
"""
import argparse
from datetime import datetime, timezone
import json
from pathlib import Path
import sys
import time

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT))
from scripts.acceptance_observe import EVIDENCE, resolve
from scripts.research_hil import Bench

class Trial:
    def __init__(self,args):
        self.args=args
        self.identities=resolve()
        self.bench=Bench([self.identities['A']['port'],self.identities['B']['port']],native=True)
        self.room=args.room
        self.suffix='' if self.room=='room1' else '?room=2'
        self.other='room2' if self.room=='room1' else 'room1'
        self.device=args.device
        self.index=0 if self.device=='light' else 1
        self.node=1 if self.room=='room1' else 3
        self.data=dict(source='HARDWARE_MEASURED',case=args.case,room=args.room,device=args.device,
                       identities=self.identities,events=[],rows=[],error=None,
                       protocol='Existing boot-bound /api/control; controller SafetyKernel owns actuation',
                       physical_LED_observation='PENDING independent user observation')
        self.base=None
        self.boot=None
        self.mutation_attempted=False
        self.last_config_id=None

    def sample(self):
        row=dict(UTC=datetime.now(timezone.utc).isoformat(),host_monotonic_ns=time.monotonic_ns(),rooms={})
        for room,node,suffix in [('room1',1,''),('room2',3,'?room=2')]:
            s=self.bench.http('GET','/api/state'+suffix,timeout=4)
            t=self.bench.http('GET','/api/transaction'+suffix,timeout=4)
            if not s or not t or s['status']!=200 or t['status']!=200:
                raise RuntimeError('Diagnostic response missing; no rule retry')
            state=s['body'];tx=t['body']
            if state.get('node_id')!=node or tx.get('node_id')!=node:
                raise RuntimeError('Wrong-node response')
            if not state.get('known') or not state.get('authenticated') or state.get('age_ms',99999)>3000:
                raise RuntimeError('Fresh authenticated state required')
            row['rooms'][room]=dict(state=state,transaction=tx)
        self.data['rows'].append(row)
        if self.base:
            other=row['rooms'][self.other]['state'];initial=self.base['rooms'][self.other]['state']
            keys=['boot_id','last_command','light','fan','light_schedule_ms','fan_schedule_ms',
                  'light_energy_cap_ms','fan_energy_cap_ms','light_control_source','fan_control_source']
            if any(other.get(k)!=initial.get(k) for k in keys):
                raise RuntimeError('Other room changed during isolated test')
            s=row['rooms'][self.room]['state']
            if s['boot_id']!=self.boot:raise RuntimeError('Controller boot changed')
            other_device='fan' if self.device=='light' else 'light'
            initial_selected=self.base['rooms'][self.room]['state']
            if s.get(other_device) is not False or any(s.get(other_device+k)!=initial_selected.get(other_device+k)
                    for k in ['_schedule_ms','_schedule_action','_schedule_result','_energy_cap_ms','_control_source']):
                raise RuntimeError('Unrelated device changed')
        return row

    def state(self,row):return row['rooms'][self.room]['state']

    def configure(self,action,value,cleanup=False):
        if action not in (0,1,2) or (action==2 and value!=0) or (action!=2 and not 1000<=value<=60000):
            raise ValueError('Acceptance rule outside supported short bounds')
        event=dict(event='cleanup_cancel' if cleanup else 'configuration_request',boot=self.boot,
                   node=self.node,device=self.device,action=action,value_ms=value,host_monotonic_ns=time.monotonic_ns())
        self.data['events'].append(event)
        self.mutation_attempted=True
        response=self.bench.http('POST','/api/control'+self.suffix,
            f'boot {self.boot} 7 {self.index} {action} {value}'.encode(),timeout=4)
        if not response or response['status']!=202 or not response['body'].get('queued'):
            raise RuntimeError('Configuration not accepted; no automatic retry')
        ident=response['body']['id'];event['id']=ident
        end=time.monotonic()+7
        while time.monotonic()<end:
            row=self.sample();s=self.state(row);tx=row['rooms'][self.room]['transaction']
            if tx.get('id')==ident and tx.get('phase')==2:
                if tx.get('status')!=0:raise RuntimeError('Configuration rejected by controller')
                if s['last_command']==ident and tx.get('outcome_known'):
                    result=s[self.device+'_schedule_result'];remaining=s[self.device+'_schedule_ms']
                    if action==2 and result==4 and remaining==0 or action!=2 and result==1 and remaining>0 and s[self.device+'_schedule_action']==action:
                        event.update(ACK=True,fresh_configuration_readback=True,accepted_host_monotonic_ns=row['host_monotonic_ns'])
                        self.last_config_id=ident
                        return row
            self.bench.poll(.15)
        raise RuntimeError('Matching ACK/configuration readback not established')

    def run(self):
        row=self.sample();self.base=row;self.boot=self.state(row)['boot_id']
        for v in row['rooms'].values():
            s=v['state']
            if s.get('output_profile')!='LED_INDICATORS_10K_ACTIVE_HIGH' or not s.get('physical_outputs_enabled') or s.get('pir_available') is not False:
                raise RuntimeError('Verified LED-only/no-PIR profile required')
            if any(s.get(k)!=0 for k in ['light_schedule_ms','fan_schedule_ms','light_energy_cap_ms','fan_energy_cap_ms']) or s['light'] or s['fan']:
                raise RuntimeError('Begin with all outputs/rules/caps OFF')
            if v['transaction'].get('phase') in (1,):raise RuntimeError('Transaction pending')
        if self.args.case=='inspect':return
        print('GUARDS_PASS: all four OFF; submitting short '+self.args.case+' rule test',flush=True)
        first=self.configure(1,60000 if self.args.case=='manual' else 15000)
        old_due=first['host_monotonic_ns']/1e9+self.state(first)[self.device+'_schedule_ms']/1000
        if self.args.case=='replace':
            second=self.configure(0,25000)
            due=second['host_monotonic_ns']/1e9+self.state(second)[self.device+'_schedule_ms']/1000
            expected=2
        elif self.args.case=='cancel':
            self.configure(2,0)
            due=old_due
            expected=4
        else:
            print('ARMED: confirm ONLY selected room/device OFF in phone app now',flush=True)
            manual=None
            while time.monotonic()<old_due-10:
                row=self.sample();s=self.state(row);tx=row['rooms'][self.room]['transaction']
                if s[self.device] is not False:raise RuntimeError('Unexpected ON before manual cancellation')
                if s['last_command']!=self.last_config_id and tx.get('id')==s['last_command'] and tx.get('phase')==2 and tx.get('status')==0 and tx.get('outcome_known'):
                    if s[self.device+'_schedule_ms']==0 and s[self.device+'_schedule_result']==4 and s[self.device+'_control_source']==1 and s['manual_flags']&(1<<self.index):
                        manual=dict(id=s['last_command'],boot=s['boot_id'],node=s['node_id'],ACK=True,
                                    fresh_readback=True,source='manual',schedule_result='cancelled')
                        self.last_config_id=s['last_command'];self.data['manual_command']=manual
                        print('MANUAL_OFF_ACK_READBACK_CANCELLED_RULE',flush=True)
                        break
                self.bench.poll(.2)
            if manual is None:raise RuntimeError('Manual OFF did not arrive before expiry guard; cancelling safely')
            due=old_due
            expected=4
        while time.monotonic()<due+3:
            row=self.sample();s=self.state(row)
            if s[self.device] is not False:raise RuntimeError('Old scheduled ON executed unexpectedly')
            if s['last_command']!=self.last_config_id:raise RuntimeError('Unexpected additional command during isolated rule test')
            self.bench.poll(.2)
        last=self.sample();s=self.state(last)
        if s[self.device+'_schedule_ms']!=0 or s[self.device+'_schedule_result']!=expected:
            raise RuntimeError('Expected terminal schedule outcome missing')
        if self.args.case=='replace' and s[self.device+'_control_source']!=2:
            raise RuntimeError('Replacement schedule source missing')
        self.data['software_result']='PASS'
        self.data['old_ON_prevented']=True
        self.data['other_room_and_device_unchanged']=True
        self.data['configuration_acceptance_separate_from_execution']=True

def main(args):
    if not args.phase.replace('-','').isalnum():raise ValueError('Invalid phase')
    output=EVIDENCE/(args.phase+'.json')
    if output.exists():raise ValueError('Preserve previous trial evidence')
    trial=Trial(args)
    try:trial.run()
    except Exception as failure:
        trial.data['error']=str(failure)
        # One new cancel, never a retry of the armed ON. If this fails, require
        # user cleanup; do not falsely report OFF or issue blind GPIO operations.
        if trial.mutation_attempted and trial.boot is not None and args.case!='inspect':
            try:trial.configure(2,0,cleanup=True)
            except Exception as cleanup_failure:trial.data['cleanup_error']=str(cleanup_failure)
    finally:
        trial.bench.stack.close()
        trial.data['owned_serial_sessions_closed']=True
        output.write_text(json.dumps(trial.data,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({k:v for k,v in trial.data.items() if k not in ('rows','identities','events')}),flush=True)
    return int(trial.data['error'] is not None)

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--phase',required=True)
    p.add_argument('--room',choices=['room1','room2'],required=True)
    p.add_argument('--device',choices=['light','fan'],default='light')
    p.add_argument('--case',choices=['inspect','replace','cancel','manual'],required=True)
    raise SystemExit(main(p.parse_args()))
