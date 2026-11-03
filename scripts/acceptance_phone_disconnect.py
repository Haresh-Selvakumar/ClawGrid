"""One bounded schedule test: Wi-Fi disconnect after ACK/app readback, restore after OFF.

Reads observer evidence and changes only phone Wi-Fi. Never submits device/rule commands.
"""
import argparse
from datetime import datetime, timezone
import json
from pathlib import Path
import subprocess
import sys
import time
from phone_ui_memory import read_ui

ROOT = Path(__file__).resolve().parents[1]
EVIDENCE = ROOT/'benchmarks/review/three-board-acceptance-20261102'
ADB = Path.home()/'AppData/Local/Android/Sdk/platform-tools/adb.exe'
BASE = [str(ADB), '-s', 'AWJDVB4705001033', 'shell']

def wifi():
    return subprocess.run(BASE+['settings','get','global','wifi_on'],capture_output=True,text=True,check=True,timeout=10).stdout.strip()

def change_wifi(enabled):
    subprocess.run(BASE+['svc','wifi','enable' if enabled else 'disable'],capture_output=True,check=True,timeout=15)
    end=time.monotonic()+8
    while time.monotonic()<end:
        state=wifi()
        if state == ('1' if enabled else '0'):
            return state
        time.sleep(.2)
    raise RuntimeError('Phone Wi-Fi state did not match requested test transition')

def main(args):
    if not args.phase.replace('-','').isalnum():raise ValueError('Invalid phase')
    output=EVIDENCE/(args.phase+'-phone-disconnect.json')
    if output.exists():raise ValueError('Preserve prior evidence')
    live=EVIDENCE/(args.phase+'-live.jsonl')
    result=dict(source='HARDWARE_MEASURED',phase=args.phase,room=args.room,
                rule_or_actuator_commands_sent=False,events=[],error=None)
    changed=False
    try:
        if wifi()!='1':raise RuntimeError('Phone Wi-Fi must initially be ON')
        print('PHONE_DISCONNECT_WATCH_READY',flush=True)
        end=time.monotonic()+args.seconds
        accepted=None
        while time.monotonic()<end:
            if not live.exists():time.sleep(.3);continue
            lines=live.read_text(encoding='utf-8').splitlines()
            if not lines:time.sleep(.3);continue
            try:row=json.loads(lines[-1])
            except json.JSONDecodeError:time.sleep(.2);continue
            v=row['rooms'][args.room];s=v['state'];tx=v['transaction']
            if not changed:
                if not (s.get('known') and s.get('authenticated') and s.get('light') is True and
                        s.get('light_schedule_ms',0)>0 and s.get('light_schedule_result')==1 and
                        tx.get('id')==s.get('last_command') and tx.get('phase')==2 and
                        tx.get('status')==0 and tx.get('outcome_known')):
                    time.sleep(.3);continue
                # Confirm displayed app readback before taking its transport away.
                ui=read_ui(ADB)
                labels=[n.get('text','') for n in ui.iter('node')
                        if n.get('package')=='com.aistudio.clawgrid.v2edge' and n.get('password')!='true']
                app_armed=any(t.startswith('Last schedule: Armed') for t in labels)
                app_countdown=any(t.startswith('Next action: OFF in ') for t in labels)
                if not (app_armed and app_countdown):
                    if s['light_schedule_ms']<12000:raise RuntimeError('App countdown not visible before safe disconnect margin')
                    time.sleep(.3);continue
                if s['light_schedule_ms']<12000:raise RuntimeError('Too little countdown remains to disconnect safely')
                if s['light_schedule_ms']/1000 > end-time.monotonic()-15:
                    raise RuntimeError('Schedule exceeds bounded observation window; Wi-Fi left connected')
                if time.monotonic_ns()-row['host_monotonic_ns']>8_000_000_000:
                    continue # Refresh state before touching phone networking.
                accepted=dict(node=s['node_id'],boot=s['boot_id'],configuration=s['last_command'],
                              remaining_ms=s['light_schedule_ms'],ACK=True,app_armed=True,app_countdown=True)
                result['configuration']=accepted
                change_wifi(False);changed=True
                result['events'].append(dict(event='WIFI_OFF',UTC=datetime.now(timezone.utc).isoformat(),
                    host_monotonic_ns=time.monotonic_ns(),wifi_on=wifi()))
                print('ACK/app countdown confirmed; PHONE_WIFI_OFF',flush=True)
            else:
                if s.get('boot_id')!=accepted['boot']:raise RuntimeError('Controller rebooted during schedule test')
                if s.get('last_command')!=accepted['configuration']:raise RuntimeError('Another device command invalidated isolated execution window')
                if s.get('light_schedule_result')==3:raise RuntimeError('Scheduled action blocked by policy/dwell')
                if s.get('known') and s.get('light_schedule_result')==2 and s.get('light') is False and s.get('light_control_source')==2:
                    result['events'].append(dict(event='SCHEDULE_EXECUTED_OFF',UTC=row['UTC'],
                        host_monotonic_ns=row['host_monotonic_ns'],wifi_on_at_execution_check=wifi(),state=s))
                    assert result['events'][-1]['wifi_on_at_execution_check']=='0'
                    result['execution_while_phone_disconnected']=True
                    print('SCHEDULE_OFF_CONFIRMED_WHILE_PHONE_WIFI_OFF',flush=True)
                    break
            time.sleep(.3)
        else:raise RuntimeError('Bounded schedule/disconnection test deadline reached')
    except Exception as failure:
        result['error']=str(failure)
    finally:
        if changed:
            try:
                change_wifi(True)
                result['events'].append(dict(event='WIFI_REENABLED',UTC=datetime.now(timezone.utc).isoformat(),
                    host_monotonic_ns=time.monotonic_ns(),wifi_on=wifi()))
            except Exception as failure:result['restore_error']=str(failure)
        result['microphone_audio_retained']=False
        output.write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({'phase':args.phase,'execution_while_phone_disconnected':result.get('execution_while_phone_disconnected',False),
                      'error':result['error'],'WiFi_radio_reenabled':any(e['event']=='WIFI_REENABLED' for e in result['events'])}),flush=True)
    return int(result['error'] is not None)

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--phase',required=True)
    p.add_argument('--room',choices=['room1','room2'],required=True)
    p.add_argument('--seconds',type=int,choices=range(1,601),default=240)
    raise SystemExit(main(p.parse_args()))
