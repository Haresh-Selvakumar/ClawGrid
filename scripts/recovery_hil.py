"""Three bounded task-watchdog trials on explicit inhibited diagnostic images.

No policy edits, GPIO commands, manual resets or automatic retry after failure.
Restore default non-diagnostic firmware separately after the campaign.
"""
import argparse
from datetime import datetime,timezone
import json
from pathlib import Path
import sys
import time
import serial
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT))
from scripts.research_hil import Bench

def policy_matches(before,after):
    return bool(before and after and all(k in before and k in after and before[k]==after[k] for k in ('grace_ms','auto_flags','away')))

def classify_reset(reason,watchdog_text,boot_before,boot_after):
    return bool(reason==6 and watchdog_text and boot_after==boot_before+1)

class RecoveryBench(Bench):
    def __init__(self,ports):
        super().__init__(ports,native=True,simulation_only=True,speech=True)
        self.timed_lines=[];self.reconnections=[]
    def poll(self,seconds=.05):
        deadline=time.monotonic()+seconds
        while time.monotonic()<deadline:
            first=len(self.lines)
            try:super().poll(min(.01,max(.001,deadline-time.monotonic())))
            except serial.SerialException:
                connection=self.connections[self.a]
                connection.close();connection.dtr=False;connection.rts=False
                until=time.monotonic()+5
                while True:
                    try:connection.open();break
                    except serial.SerialException:
                        if time.monotonic()>=until:raise
                        time.sleep(.1)
                self.buffers[self.a].clear()
                self.reconnections.append(dict(host_monotonic_ns=time.monotonic_ns(),action='transport_reopen_only'))
            observed=time.monotonic_ns()
            self.timed_lines.extend(dict(host_monotonic_ns=observed,text=line) for line in self.lines[first:])

def run(args):
    stamp=datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')
    out=ROOT/'benchmarks/raw/hardware'/f'watchdog_{stamp}';out.mkdir(parents=True)
    b=RecoveryBench([args.node_a,args.node_b]);trials=[];errors=[];original=None
    def require(name,condition):
        b.case(name,bool(condition))
        if not condition:raise RuntimeError(name)
    def state():
        response=b.http('GET','/api/state',timeout=3)
        return response['body'] if response and response.get('status')==200 else {}
    def virtual_off():
        target='virtual_rooms bedroom light OFF'
        proposal=b.http('POST','/api/proposal',f'manual {target} 0'.encode())
        require('virtual_prepare',proposal and proposal['status']==200)
        response=b.http('POST','/api/confirm',f'{proposal["body"]["proposal_id"]} {target}'.encode())
        require('virtual_confirm',response and response['status']==202)
        deadline=time.monotonic()+3
        while time.monotonic()<deadline:
            result=b.http('GET','/api/transaction',timeout=2)
            if result and result['body'].get('phase')==2:
                require('virtual_ACK',result['body'].get('status')==0 and result['body'].get('id')==response['body']['id'])
                return result['body']
            b.poll(.05)
        require('virtual_ACK',False)
    try:
        b.poll(.5);original=b.request(b.a,b'P','policy_saved')
        require('initial_inhibited',original and original.get('physical_outputs_enabled') is False)
        health=b.request(b.a,b'q','task_health')
        require('explicit_diagnostic_build',health and health.get('diagnostic_faults_enabled') is True and not health.get('control_stalled'))
        for number in range(1,args.trials+1):
            t=dict(trial=number,completed=False);trials.append(t)
            before=b.request(b.a,b'P','policy_saved');t['before_policy']=before
            require('policy_unchanged_before',policy_matches(original,before))
            fresh=state();require('fresh_schema3_before',fresh.get('known') and fresh.get('physical_outputs_enabled') is False and fresh.get('physical_state')=='not measured')
            t['pre_reset_command']=virtual_off()
            target='virtual_rooms bedroom fan OFF'
            proposal=b.http('POST','/api/proposal',f'manual {target} 0'.encode())
            require('pending_pre_reset_proposal',proposal and proposal['status']==200);t['pre_reset_proposal']=proposal
            first_record=len(b.records);first_line=len(b.timed_lines)
            t['injected_ns']=time.monotonic_ns()
            t['armed']=b.request(b.a,b'X','watchdog_injection')
            require('control_stall_armed',t['armed'] and t['armed'].get('armed'))
            b.poll(.25);one=b.request(b.a,b'q','task_health')
            b.poll(.65);two=b.request(b.a,b'q','task_health');t['during_stall_health']=[one,two]
            require('only_control_stalled_link_and_diagnostics_live',one and two and one['control_stalled'] and two['control_stalled'] and one['control_jobs']==two['control_jobs'] and two['link_jobs']>one['link_jobs'])
            live=b.http('GET','/api/rtos',timeout=2);t['B_during_fault']=live
            require('B_unrelated_activity_live',live and live['status']==200 and live['body'].get('jobs',0)>0)
            while time.monotonic_ns()-t['injected_ns']<3_400_000_000:b.poll(.05)
            # The current pair must expire frozen control samples even while
            # unrelated radio/HTTP tasks are alive. Never count packet receipt
            # alone as control progress.
            t['during_fault_state']=state()
            require('frozen_control_telemetry_expires',t['during_fault_state'].get('known') is False)
            # Recheck the existing proposal without replacing/consuming it;
            # retain the same one for the post-reset boot-binding assertion.
            unavailable=b.http('POST','/api/confirm',f'{proposal["body"]["proposal_id"]} {target}'.encode())
            t['during_fault_confirmation']=unavailable
            require('stale_controller_confirmation_rejected',unavailable and unavailable['status']==409 and unavailable['body'].get('error')=='Controller unavailable or stale')
            boot=b.wait(lambda r:r['port']==b.a and r['device'].get('kind')=='boot_recovery',timeout=12,start=first_record);t['boot']=boot
            t['boot_observed_ns']=next((r['host_monotonic_ns'] for r in b.records[first_record:] if r['port']==b.a and r['device'].get('kind')=='boot_recovery'),None)
            text=b.timed_lines[first_line:]
            event=next((r for r in text if 'Task watchdog got triggered' in r['text']),None)
            culprit=any('ControlTask' in r['text'] and 'CPU 1' in r['text'] for r in text)
            t['watchdog_text_observed_ns']=event['host_monotonic_ns'] if event else None
            require('task_watchdog_panic_and_reset',boot and classify_reset(boot.get('reset_reason'),bool(event and culprit),before['boot'],boot.get('boot',0)))
            require('boot_inhibited',boot.get('physical_outputs_enabled') is False)
            after=b.request(b.a,b'P','policy_saved');t['after_policy']=after
            require('policy_restored',policy_matches(original,after) and after.get('reset_reason')==6)
            deadline=time.monotonic()+5
            while True:
                fresh=state()
                if fresh.get('known') and fresh.get('physical_outputs_enabled') is False and fresh.get('physical_state')=='not measured':break
                if time.monotonic()>=deadline:require('telemetry_resumed',False)
                b.poll(.1)
            t['fresh_state']=fresh;t['fresh_observed_ns']=time.monotonic_ns()
            t['recovery_seconds']=(t['fresh_observed_ns']-t['injected_ns'])/1e9
            require('fresh_schema3_restored',fresh.get('age_ms',3000)<3000 and not fresh.get('light') and not fresh.get('fan'))
            stale=b.http('POST','/api/confirm',f'{proposal["body"]["proposal_id"]} {target}'.encode());t['stale_confirmation']=stale
            require('old_confirmation_boot_mismatch_rejected',stale and stale['status']==409 and stale['body'].get('error')=='Controller unavailable or stale')
            index=len(b.records);replay=b.request(b.b,b'R','injection');t['replay']=replay
            require('old_exact_command_replayed_for_test',replay and replay.get('sent'))
            rejected=b.wait(lambda r:r['port']==b.a and r['device'].get('kind')=='actuation' and r['device'].get('id')==t['pre_reset_command']['id'] and r['device'].get('status')==4,timeout=3,start=index);t['stale_command_result']=rejected
            require('old_command_wrong_boot_rejected',rejected)
            post=state();t['post_replay_state']=post
            require('stale_replay_no_gate_or_output_change',post.get('known') and post.get('last_command')==0 and post.get('light') is False and post.get('fan') is False)
            health=b.request(b.a,b'q','task_health');t['recovered_health']=health
            require('fault_cleared_by_reboot',health and health.get('boot')==boot['boot'] and health.get('control_stalled') is False)
            one=health;b.poll(.3);two=b.request(b.a,b'q','task_health')
            require('control_and_link_progress_again',two and two['control_jobs']>one['control_jobs'] and two['link_jobs']>one['link_jobs'])
            t['normal_command_after']=virtual_off();t['completed']=True
            print(f'Trial {number}: PASS; boot {before["boot"]}->{boot["boot"]}; host-observed fresh-state recovery {t["recovery_seconds"]:.3f}s',flush=True)
    except Exception as error:
        errors.append(repr(error));b.case('campaign_stopped_on_failure',False,repr(error))
    finally:
        try:
            final=b.request(b.a,b'P','policy_saved',timeout=3)
            b.case('original_policy_preserved_at_end',policy_matches(original,final))
        except Exception as error:final=None;errors.append('Final policy read: '+repr(error))
        report=dict(source='HARDWARE_MEASURED',utc=stamp,ports=dict(A=args.node_a,B=args.node_b),trials=trials,original_policy=original,final_policy=final,cases=b.cases,errors=errors,records=b.records,timed_lines=b.timed_lines,reconnections=b.reconnections,
                    time_definition='Host monotonic seconds from writing X to first observed fresh schema-3 state after boot; includes boot/radio/probe delivery.',policy_changes_sent=False,manual_resets_during_trials=False,physical_commands_sent=False)
        b.stack.close();(out/'results.json').write_text(json.dumps(report,indent=2)+'\n');(out/'serial.log').write_text('\n'.join(b.lines)+'\n')
        print('Evidence:',out,flush=True)
    return int(bool(errors) or len(trials)!=args.trials or not all(t['completed'] for t in trials) or any(c['status']=='FAIL' for c in b.cases))

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--node-a',required=True);parser.add_argument('--node-b',required=True)
    parser.add_argument('--trials',type=int,choices=range(1,4),default=3)
    raise SystemExit(run(parser.parse_args()))
