"""Separate public-key subscriber: encrypted live telemetry and virtual-only commands."""
import json
import time
import uuid
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path
from websockets.sync.client import connect
from ecosystem_realtime import WireCipher

ROOT = Path(__file__).resolve().parents[1]


def main():
    config = json.loads((ROOT / 'supabase.local.json').read_text())
    token = json.loads((ROOT / 'local_credentials.json').read_text())['api_token']
    cipher = WireCipher(token, config['home_id'], config['gateway_id'])
    report = dict(source='HARDWARE_MEASURED', client='independent anon-key WSS subscriber',
                  physical_load_tested=False, snapshots=[], commands=[])

    def observation(path):
        # Reads may wait for USB contention. Commands are sent exactly once.
        for attempt in range(12):
            try:
                req = urllib.request.Request('http://127.0.0.1:8770'+path, headers={'X-ClawGrid-Token':token})
                with urllib.request.urlopen(req, timeout=6) as response:
                    return json.load(response)
            except urllib.error.HTTPError as error:
                if error.code not in (429,502) or attempt == 11: raise
                time.sleep(.4)

    try:
        before=observation('/api/state')
        assert before['known'] and before['authenticated'] and before['physical_outputs_enabled'] is False
        report['before']=before
        topic='realtime:clawgrid_live'
        url=config['url'].replace('https://','wss://')+'/realtime/v1/websocket?apikey='+config['anon_key']+'&vsn=1.0.0'
        with connect(url, open_timeout=10, max_size=65536) as socket:
            def frame(event,payload,ref):
                socket.send(json.dumps(dict(topic=topic,event=event,payload=payload,ref=ref)))
            frame('phx_join',dict(config=dict(broadcast=dict(ack=False,self=False),presence=dict(enabled=False),postgres_changes=[],private=False),access_token=config['anon_key']),'verify-join')
            deadline=time.monotonic()+25
            joined=False
            while time.monotonic()<deadline and len(report['snapshots'])<3:
                f=json.loads(socket.recv(timeout=8))
                if f.get('event')=='phx_reply' and f.get('ref')=='verify-join':
                    assert f['payload']['status']=='ok'
                    joined=True
                if f.get('event')=='broadcast':
                    v=cipher.decrypt(f['payload']['payload'])
                    if v.get('kind')=='snapshot': report['snapshots'].append(v)
            assert joined and len(report['snapshots'])==3
            assert len({v['sequence'] for v in report['snapshots']})==3
            latest=report['snapshots'][-1]
            assert latest['node_a']['known'] and latest['node_a']['authenticated']
            assert latest['node_a']['physical_outputs_enabled'] is False
            assert latest['sensors']['temperature_c'] is None and latest['sensors']['illuminance_lux'] is None
            assert latest['node_a']['rtos'] is None and latest['node_a']['memory'] is None
            assert latest['node_b']['rtos'] and latest['node_b']['memory']
            print('Independent Supabase subscriber: 3 real snapshots, A state and B diagnostics PASS',flush=True)
            for action in ('ON','OFF'):
                if action=='OFF': time.sleep(3.3)
                now=time.time()
                command=dict(kind='command',id=str(uuid.uuid4()),target='virtual_rooms',room='bedroom',device='light',action=action,issued_at=now,expires_at=now+10)
                case=dict(id=command['id'],action=action)
                report['commands'].append(case)
                frame('broadcast',dict(type='broadcast',event='clawgrid_secure_v1',payload=cipher.encrypt(command)),command['id'])
                deadline=time.monotonic()+17
                while time.monotonic()<deadline:
                    f=json.loads(socket.recv(timeout=8))
                    if f.get('event')!='broadcast': continue
                    v=cipher.decrypt(f['payload']['payload'])
                    if v.get('kind')=='command_ack' and v.get('cloud_id')==command['id']:
                        case['ack']=v['result']
                        break
                assert case.get('ack',{}).get('phase')==2 and case['ack'].get('status')==0,case
                virtual=observation('/api/simulation')
                case['virtual_state']=virtual
                assert virtual['known'] and bool(virtual['outputs'] & 1)==(action=='ON'),virtual
                print('Encrypted Supabase command -> B -> A: virtual '+action+' ACK/state PASS',flush=True)
        report['after']=observation('/api/state')
        assert report['after']['physical_outputs_enabled'] is False
        report['passed']=True
    except Exception as error:
        # Do not log socket exceptions: their URLs contain an API key.
        report.update(passed=False,error_type=type(error).__name__,error=str(error) if isinstance(error,AssertionError) else 'Verification did not finish')
        print('Ecosystem verification FAIL: '+report['error_type'],flush=True)
    stamp=datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')
    path=ROOT/('benchmarks/raw/hardware/ecosystem_realtime_'+stamp+'.json')
    path.write_text(json.dumps(report,indent=2)+'\n')
    print('Evidence: '+str(path),flush=True)
    return int(not report['passed'])


if __name__=='__main__': raise SystemExit(main())
