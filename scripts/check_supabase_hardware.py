"""Verify Supabase mailbox -> USB B -> ESP-NOW A using virtual light only."""
import json
import time
import urllib.error
import urllib.request
import uuid
from datetime import datetime, timezone
from pathlib import Path

from supabase_bridge import Supabase

ROOT = Path(__file__).resolve().parents[1]


def main():
    token = json.loads((ROOT / 'local_credentials.json').read_text())['api_token']
    config = json.loads((ROOT / 'supabase.local.json').read_text())
    report = dict(source='HARDWARE_MEASURED', virtual_test=True, physical_load_tested=False, cases=[])

    def request(path, value=None, authenticated=True):
        headers = {'Content-Type': 'application/json'}
        if authenticated:
            headers['X-ClawGrid-Token'] = token
        body = None if value is None else json.dumps(value).encode()
        req = urllib.request.Request('http://127.0.0.1:8770' + path, data=body, headers=headers)
        try:
            with urllib.request.urlopen(req, timeout=12) as response:
                return json.load(response)
        except urllib.error.HTTPError as error:
            if authenticated:
                try: detail=json.load(error).get('error','HTTP failure')
                except ValueError: detail='HTTP failure'
                raise RuntimeError(path+': '+str(error.code)+' '+detail) from None
            raise

    def observe(path):
        for attempt in range(12):
            try:
                return request(path)
            except RuntimeError as error:
                if not any(code in str(error) for code in (': 429 ', ': 502 ')) or attempt==11:
                    raise
                time.sleep(.4)

    try:
        report['before'] = observe('/api/state')
        assert report['before']['known'] and report['before']['physical_outputs_enabled'] is False
        try:
            request('/local/cloud/status', authenticated=False)
            raise AssertionError('Cloud status allowed missing control token')
        except urllib.error.HTTPError as error:
            assert error.code == 401
        for _ in range(15):
            status = request('/local/cloud/status')
            if status['ready']:
                break
            time.sleep(1)
        assert status['ready'], status
        report['cloud'] = status
        database = Supabase(config)
        for action in ('ON', 'OFF'):
            if action == 'OFF':
                time.sleep(3.2)
            value = dict(id=str(uuid.uuid4()), target='virtual_rooms', room='bedroom', device='light', action=action)
            queued = request('/local/cloud/commands', value)  # Never retry a command POST.
            result = None
            for _ in range(45):
                observed = request('/local/cloud/result', dict(id=queued['cloud_id']))
                if observed['result']:
                    result = observed['result']
                    break
                time.sleep(.25)
            assert result and result.get('phase') == 2 and result.get('status') == 0, result
            cloud_rows = database.request('clawgrid_events?id=eq.' + queued['cloud_id'] + '&select=id,event_type,source,description')
            assert len(cloud_rows) == 1 and cloud_rows[0]['event_type'] == 'CLOUD_COMMAND_REQUESTED'
            signed = json.loads(cloud_rows[0]['description'])
            assert signed['command']['action'] == action and signed['command']['target'] == 'virtual_rooms'
            state = observe('/api/simulation')
            assert state['known'] and bool(state['outputs'] & 1) == (action == 'ON'), state
            report['cases'].append(dict(action=action, cloud_id=queued['cloud_id'], ack=result, virtual_state=state))
            print('Supabase -> B -> ESP-NOW A: virtual light ' + action + ' ACK/state PASS', flush=True)
        rows = database.request('clawgrid_telemetry?home_id=eq.' + config['home_id'] + '&source=eq.HARDWARE_MEASURED&order=received_at.desc&limit=1')
        assert rows and rows[0]['outputs_enabled'] is False
        assert rows[0]['temperature_c'] is None and rows[0]['illuminance_lux'] is None
        report['persisted_telemetry'] = rows[0]
        report['after'] = observe('/api/state')
        assert report['after']['known'] and report['after']['physical_outputs_enabled'] is False
        report['passed'] = True
    except Exception as error:
        report.update(passed=False, error=str(error))
        print('Supabase check FAIL: ' + str(error), flush=True)
    stamp = datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')
    path = ROOT / ('benchmarks/raw/hardware/supabase_end_to_end_' + stamp + '.json')
    path.write_text(json.dumps(report, indent=2) + '\n')
    print('Evidence: ' + str(path))
    return int(not report['passed'])


if __name__ == '__main__':
    raise SystemExit(main())
