"""Synthetic speech -> PC -> B -> ESP-NOW A; virtual light only, no microphone."""
import argparse
import json
import time
import urllib.error
import urllib.request
import wave
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def main(url):
    token = json.loads((ROOT / 'local_credentials.json').read_text())['api_token']
    report = dict(recognition_source='HOST_MEASURED', audio_source='SIMULATED',
                  control_source='HARDWARE_MEASURED', physical_load_tested=False, cases=[])
    session = None

    def request(path, body=None, sequence=None):
        headers = {'Content-Type': 'application/octet-stream', 'X-ClawGrid-Token': token}
        if sequence is not None:
            headers.update({'X-ClawGrid-Speech-Session': session,
                            'X-ClawGrid-Speech-Sequence': str(sequence)})
        if path == '/local/speech/stop':
            headers['X-ClawGrid-Speech-Session'] = session
        for attempt in range(4 if body is None else 1):
            try:
                req = urllib.request.Request(url + path, data=body, headers=headers)
                with urllib.request.urlopen(req, timeout=15) as response:
                    return json.load(response)
            except urllib.error.HTTPError as error:
                if body is not None or error.code != 502 or attempt == 3:
                    raise
                time.sleep(.15)  # Retry observation only; never resubmit a command.

    try:
        report['before'] = request('/api/state')
        assert report['before']['known'] and report['before']['physical_outputs_enabled'] is False
        session = request('/local/speech/start', b'')['session']
        sequence = seen = 0
        for file, action in [('light_on.wav', 'ON'), ('lights_off.wav', 'OFF')]:
            if action == 'OFF':
                time.sleep(3.2)
            with wave.open(str(ROOT / 'benchmarks/review/pc_voice/fixtures' / file), 'rb') as audio:
                pcm = audio.readframes(audio.getnframes()) + bytes(64000)
            finals = []
            for offset in range(0, len(pcm), 8000):
                sequence += 1
                answer = request('/local/speech/audio', pcm[offset:offset + 8000], sequence)
                if answer['final'] and answer['final_id'] > seen:
                    seen = answer['final_id']
                    if answer['text']:
                        finals.append(answer['text'])
            assert len(finals) == 1, finals
            proposal = request('/api/text', ('virtual_rooms ' + finals[0]).encode())
            assert (proposal['target'], proposal['room'], proposal['device'], proposal['action']) == (
                'virtual_rooms', 'bedroom', 'light', action), proposal
            queued = request('/api/confirm', (
                f"{proposal['proposal_id']} {proposal['target']} {proposal['room']} "
                f"{proposal['device']} {proposal['action']}").encode())
            transaction = None
            for attempt in range(20):
                value = request('/api/transaction')
                if value['id'] == queued['id'] and value['phase'] in (2, 3):
                    transaction = value
                    break
                time.sleep(.1)
            assert transaction and transaction['phase'] == 2 and transaction['status'] == 0, transaction
            state = request('/api/simulation')
            assert state['known'] and bool(state['outputs'] & 1) == (action == 'ON'), state
            report['cases'].append(dict(transcript=finals[0], proposal=proposal,
                                        ack=transaction, virtual_state=state))
            print(f'PC speech -> USB B -> ESP-NOW A: virtual {action} ACK PASS', flush=True)
        report['after'] = request('/api/state')
        assert report['after']['known'] and report['after']['physical_outputs_enabled'] is False
        report['memory_b'] = request('/api/memory')
        report['rtos_b'] = request('/api/rtos')
        assert report['memory_b']['memory_config_ok']
        report['passed'] = True
    except Exception as error:
        report.update(passed=False, error=repr(error))
        print('FAIL:', repr(error), flush=True)
    finally:
        if session:
            try:
                request('/local/speech/stop', b'')
            except Exception as error:
                report['stop_error'] = repr(error)
        stamp = datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')
        output = ROOT / f'benchmarks/raw/hardware/pc_voice_end_to_end_{stamp}.json'
        output.write_text(json.dumps(report, indent=2) + '\n')
        print('Evidence:', output)
    return int(not report.get('passed'))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--url', default='http://127.0.0.1:8770')
    raise SystemExit(main(parser.parse_args().url))
