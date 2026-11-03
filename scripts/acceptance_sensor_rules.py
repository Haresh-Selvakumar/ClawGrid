"""Finite Room1 fan priority/OFF-protection acceptance using real sensor input.

No C writes, no synthetic inputs, no mutation retries. Applied cap + countdown
cap precedes AUTO entry; timing cases authorize countdown after actual ON. Failed cases retain independent controller guards.
"""
import argparse
from datetime import datetime, timezone
import json
from pathlib import Path
import sys
import time

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from scripts.acceptance_observe import resolve
from scripts.research_hil import Bench

def main(case, trial):
    out = ROOT / 'benchmarks/review/sensor-auto-20261102' / (case+('-'+trial if trial else '')+'.json')
    if out.exists():
        raise RuntimeError('Preserve previous case')
    b = None; rows = []; operations = []
    result = dict(source='HARDWARE_MEASURED', case=case, Room2_commands_sent=False,
                  synthetic_sensor_input=False, status='NOT_PASS')
    try:
        ids = resolve(); b = Bench([ids['A']['port'],ids['B']['port']],native=True)
        def api(method,path,body=None):
            r=b.http(method,path,b'' if body is None else body.encode(),timeout=5)
            if method=='GET' and (not r or r['status']==502):
                gap=dict(UTC=datetime.now(timezone.utc).isoformat(),path=path,first_status=r.get('status') if r else None,read_only_retry=True)
                result.setdefault('read_capture_gaps',[]).append(gap)
                b.poll(.25)
                r=b.http('GET',path,timeout=5)
                gap['retry_status']=r.get('status') if r else None
            if not r or r['status'] not in (200,202): raise RuntimeError('Transport/rejection '+method+' '+path+'; no mutation retry')
            return r['body']
        def capture():
            row=dict(UTC=datetime.now(timezone.utc).isoformat(),host_monotonic_ns=time.monotonic_ns(),rooms={})
            for room,node,suffix in [('room1',1,''),('room2',3,'?room=2')]:
                s=api('GET','/api/state'+suffix); tx=api('GET','/api/transaction'+suffix)
                if s.get('node_id')!=node or tx.get('node_id')!=node or not s.get('known') or not s.get('authenticated') or s.get('age_ms',9999)>=3000:
                    raise RuntimeError('Fresh room/node-bound telemetry required')
                row['rooms'][room]=dict(state=s,transaction=tx)
            rows.append(row)
            with out.with_suffix('.jsonl').open('a',encoding='utf-8') as f:f.write(json.dumps(row)+'\n')
            return row
        first=capture(); a=first['rooms']['room1']['state']; c=first['rooms']['room2']['state']
        boot=a['boot_id']; c_id=(c['boot_id'],c['last_command'])
        if a.get('features_version')!=2 or a['fan'] or c['light'] or c['fan']:
            raise RuntimeError('Expected both-room OFF baseline')
        if any(a[k] for k in ['light_schedule_ms','fan_schedule_ms','light_timer_ms','fan_timer_ms','light_energy_cap_ms','fan_energy_cap_ms']):
            raise RuntimeError('Existing rule must not be silently replaced')
        def check_isolation(row):
            s=row['rooms']['room1']['state']; other=row['rooms']['room2']['state']
            if s['boot_id']!=boot or (other['boot_id'],other['last_command'])!=c_id or s['light'] or other['light'] or other['fan']:
                raise RuntimeError('Unrelated room/output/boot changed')
            return s
        def normal_off(device):
            p=api('POST','/api/proposal',f'manual physical_bedroom bedroom {device} OFF 0')
            if p.get('controller_boot')!=boot or p.get('room')!='bedroom' or p.get('device')!=device or p.get('action')!='OFF':raise RuntimeError('OFF proposal binding changed')
            q=api('POST','/api/confirm',f"{p['proposal_id']} physical_bedroom bedroom {device} OFF")
            end=time.monotonic()+5
            while time.monotonic()<end:
                row=capture();s=row['rooms']['room1']['state'];tx=row['rooms']['room1']['transaction'];other=row['rooms']['room2']['state']
                if s['boot_id']!=boot or (other['boot_id'],other['last_command'])!=c_id or other['light'] or other['fan']:raise RuntimeError('OFF context changed')
                if tx.get('id')==q.get('id') and tx.get('phase')==2:
                    if tx.get('status')!=0:raise RuntimeError('OFF rejected by policy/dwell')
                    if s['last_command']==q['id'] and not s[device]:
                        operations.append(dict(name='normal_manual_'+device+'_OFF',id=q['id'],applied=True,state=s,transaction=tx,UTC=row['UTC']));return
                b.poll(.1)
            raise RuntimeError('Manual OFF readback incomplete')
        def configure(op,action,value,name,check,device=1):
            before=capture()['rooms']['room1']['state']['sample_uptime_ms']
            queued=api('POST','/api/control',f'boot {boot} {op} {device} {action} {value}')
            if queued.get('queued') is not True:raise RuntimeError('Not queued')
            operation=dict(name=name,id=queued['id'],applied=False);operations.append(operation)
            end=time.monotonic()+5
            while time.monotonic()<end:
                row=capture();s=check_isolation(row);tx=row['rooms']['room1']['transaction']
                if tx.get('id')==queued['id'] and tx.get('phase')==2:
                    if tx.get('status')!=0 or not tx.get('outcome_known'):raise RuntimeError('Controller rejection')
                    if s['last_command']==queued['id'] and s['sample_uptime_ms']!=before and check(s):
                        operation.update(applied=True,state=s,transaction=tx,UTC=row['UTC']);return s
                b.poll(.1)
            raise RuntimeError('ACK/readback incomplete; no retry')
        b.poll(3.2) # Read-only settling; do not bypass dwell.
        normal_off('light')
        configure(10,0,0,'baseline_fan_MANUAL_OFF',lambda s:s['manual_flags']==3 and not s['auto_flags'] and not s['fan'])
        priority=case in ('schedules-first','sensors-first')
        # Real sensor input must oppose the scheduled request in priority cases.
        if a.get('temperature_status')!='OK' or a['temperature_deci_c']>=290 or a['temperature_deci_c']<270:
            raise RuntimeError('Real measured 27..28.9C baseline required; no synthetic substitute')
        on,off=(300,290) if priority else (270,260)
        sensors_first=case=='sensors-first'; seconds=15 if priority else 60
        packed=off | (on<<9) | (seconds<<18) | (int(sensors_first)<<30)
        configure(9,0,packed,'reviewed_fan_sensor_settings',lambda s:s['fan_sensor_config']==packed and s['manual_flags']&2)
        cap=30000 if priority else (60000 if case=='continuous-cap' else 120000)
        countdown=60000 if priority else (120000 if case=='continuous-cap' else 60000)
        configure(8,0,cap,'independent_continuous_ON_cap',lambda s:s['fan_energy_cap_ms']==cap)
        if priority: configure(3,0,countdown,'independent_OFF_countdown',lambda s:0<s['fan_timer_ms']<=countdown)
        configure(10,1,0,'explicit_AUTO_entry',lambda s:s['auto_flags']&2 and not s['manual_flags']&2)
        if not priority: configure(3,0,countdown,'fresh_OFF_countdown_after_ON',lambda s:0<s['fan_timer_ms']<=countdown and s['fan'])
        if priority:
            armed=configure(7,1,10000,'finite_scheduled_ON',lambda s:s['fan_schedule_ms']>0 and s['fan_schedule_action']==1)
            print('CASE_ARMED '+case+'; ON due in about10s, lease15s',flush=True)
            deadline=time.monotonic()+32
        else:
            print('AUTO_ON_PROTECTED '+case+'; earlier OFF rule due within60s',flush=True)
            deadline=time.monotonic()+max(5, min(cap,countdown)/1000)+12
        expected_last=operations[-1]['id']
        observed_on=False; observed_off=False; overridden=False; post_off_rows=0; last_on=None; first_off=None
        sensor_rows=[]
        while time.monotonic()<deadline:
            row=capture();s=check_isolation(row)
            if s['last_command']!=expected_last:raise RuntimeError('Unrelated user/controller command during isolated window; do not misclassify')
            if s.get('temperature_status')!='OK':raise RuntimeError('Required sensor became unavailable; fault OFF is not a timing/priority pass')
            if (priority and s['temperature_deci_c']>290) or (not priority and s['temperature_deci_c']<270):
                raise RuntimeError('Real temperature crossed fixture boundary; result inconclusive')
            sensor_rows.append(s['fan_sensor_demand'])
            if s['fan']:
                observed_on=True;last_on=row
                if case=='sensors-first':raise RuntimeError('Sensor priority failed: fan energized')
            elif observed_on:
                observed_off=True;post_off_rows+=1
                if first_off is None:first_off=row
            if case=='sensors-first' and s['fan_schedule_result']==5 and s['fan_winner']==5:overridden=True
            b.poll(.2)
        if case=='sensors-first':
            if not overridden or observed_on:raise RuntimeError('No isolated overridden/OFF proof')
        elif not (observed_on and observed_off and post_off_rows>=2):raise RuntimeError('Missing ON-to-OFF boundary capture')
        end_state=rows[-1]['rooms']['room1']['state']
        if priority:
            if end_state['fan_override_ms'] or end_state['fan_winner']!=5 or end_state['fan']:
                raise RuntimeError('Lease did not expire back to valid sensor OFF')
        elif not (end_state['fan_blocking']==5 and end_state['manual_flags']&2 and not end_state['auto_flags']&2):
            raise RuntimeError('Protection OFF did not latch manual hold')
        result.update(observed_ON=observed_on,observed_automatic_OFF=observed_off,
            overridden_without_ON=overridden,sensor_demand_codes=sorted(set(sensor_rows)),post_OFF_samples=post_off_rows,
            transition_boundary=dict(last_ON=last_on,first_OFF=first_off),
            timing_limit='Controller software state samples bracket transition; independent visible LED observation is separate, not oscilloscope timing.')
        # MANUAL selection at confirmed OFF cancels pending automatic work.
        configure(10,0,0,'final_MANUAL_OFF_hold',lambda s:s['manual_flags']&2 and not s['auto_flags']&2 and not s['fan'] and not s['fan_schedule_ms'] and not s['fan_override_ms'])
        configure(3,0,0,'cancel_OFF_countdown',lambda s:s['fan_timer_ms']==0)
        configure(8,0,0,'disable_test_cap',lambda s:s['fan_energy_cap_ms']==0 and not s['fan'])
        preferred=260 | (280<<9) | (60<<18)
        configure(9,0,preferred,'restore_preferred_28_26',lambda s:s['fan_sensor_config']==preferred and s['manual_flags']==3 and not s['fan'])
        proposal=api('POST','/api/proposal','manual physical_bedroom bedroom fan OFF 0')
        if proposal.get('controller_boot')!=boot or proposal.get('device')!='fan' or proposal.get('action')!='OFF':raise RuntimeError('Cleanup proposal binding changed')
        queued=api('POST','/api/confirm',f"{proposal['proposal_id']} physical_bedroom bedroom fan OFF")
        end=time.monotonic()+5
        while time.monotonic()<end:
            row=capture();s=check_isolation(row);tx=row['rooms']['room1']['transaction']
            if tx.get('id')==queued.get('id') and tx.get('phase')==2 and tx.get('status')==0 and s['last_command']==queued['id'] and not s['fan'] and s['manual_flags']==3 and s['fan_blocking']==1:
                operations.append(dict(name='normal_manual_OFF_cleanup',id=queued['id'],applied=True,state=s,transaction=tx,UTC=row['UTC']));break
            b.poll(.1)
        else:raise RuntimeError('Final manual OFF readback missing')
        result['status']='PASS'
    except Exception as error:
        result['error']=str(error)
        # Never turn an unknown output ON or disable an applied OFF guard here.
        if b and 'boot' in locals():
            try:result['fallback_cancel']=b.http('POST','/api/control',f'boot {boot} 7 1 2 0'.encode(),timeout=5)
            except Exception as cleanup:result['cleanup_error']=str(cleanup)
    finally:
        if b:b.stack.close()
        result.update(rows=rows,operations=operations,serial_closed=True,visible_LED_observation='Separate user reply required')
        out.write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({k:result.get(k) for k in ['case','status','error','observed_ON','observed_automatic_OFF','overridden_without_ON','post_OFF_samples','serial_closed']}),flush=True)

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('case',choices=['schedules-first','sensors-first','off-countdown','continuous-cap'])
    parser.add_argument('--trial',default='')
    args=parser.parse_args()
    if args.trial and not args.trial.isalnum():parser.error('Alphanumeric trial only')
    main(args.case,args.trial)
