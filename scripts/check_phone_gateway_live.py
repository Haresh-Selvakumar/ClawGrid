"""Bounded non-actuating protocol checks. Requires already ROM-verified identities."""
import json
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from scripts.research_hil import Bench

def main():
    evidence = ROOT / 'benchmarks/review/phone-offline-v1-20261025'
    identities = json.loads((evidence/'board-identities.json').read_text())['observations']
    config = json.loads((ROOT/'local_credentials.json').read_text())
    roles = {x['role']:x for x in identities}
    for role in ['A','B']:
        assert roles[role]['identity'] == 'ROM_MAC_MATCH'
        assert roles[role]['mac'] == config['node_' + role.lower() + '_mac'].lower()
    b = Bench([roles['A']['port'], roles['B']['port']], native=True, simulation_only=True, speech=True)
    records = []
    def call(path, body=None, expected=200):
        response = b.http('GET' if body is None else 'POST', path, body.encode() if body else b'', timeout=4)
        records.append(dict(path=path, request=body, response=response))
        assert response and response['status'] == expected, 'Unexpected status on ' + path
        return response['body']
    def state():
        value = call('/api/state')
        assert value['known'] and value['authenticated'] and value['age_ms'] < 3000
        assert value['physical_outputs_enabled'] is False and value['light'] is False and value['fan'] is False
        return value
    error = None
    try:
        b.poll(1)
        a = b.request(b.a, b't', 'selftest'); gateway = b.request(b.b, b't', 'selftest')
        health = b.request(b.a, b'q', 'task_health')
        records += [dict(a_selftest=a), dict(b_selftest=gateway), dict(a_health=health)]
        assert a and gateway and a['passed'] and gateway['passed']
        assert health and health['physical_outputs_enabled'] is False and health['diagnostic_faults_enabled'] is False
        before = state(); tx_before = call('/api/transaction'); rt_before = call('/api/rtos')
        boot = before['boot_id']
        assert boot in range(2, 0xffffffff)
        call('/api/proposal', f'phone physical_bedroom bedroom light OFF 77 {boot-1}', 409)
        call('/api/proposal', f'phone physical_bedroom kitchen light OFF 77 {boot}', 400)
        call('/api/proposal', f'phone physical_bedroom bedroom light OFF 0 {boot}', 400)
        phone = call('/api/proposal', f'phone physical_bedroom bedroom light OFF 77 {boot}')
        assert (phone['origin'], phone['controller_boot'], phone['capture_id'], phone['authorized']) == ('phone',boot,77,False)
        assert (phone['target'],phone['room'],phone['device'],phone['action']) == ('physical_bedroom','bedroom','light','OFF')
        manual = call('/api/proposal', 'manual physical_bedroom bedroom light OFF 0')
        assert manual['origin'] == 'manual' and not manual['authorized']
        # No confirmation endpoint is called. Let every outstanding proposal expire.
        b.poll(3.2)
        after = state(); tx_after = call('/api/transaction'); rt_after = call('/api/rtos')
        memory = call('/api/memory'); speech = call('/api/speech')
        assert after['boot_id'] == boot and after['sample_uptime_ms'] > before['sample_uptime_ms']
        assert after['last_command'] == before['last_command'] and tx_before == tx_after
        assert rt_after['jobs'] > rt_before['jobs'] and rt_after['watchdog']
        assert rt_after['deadline_misses'] == 0 and rt_after['wifi_channel'] == 1 and rt_after['ap_ip'] == '192.168.4.1'
        assert speech['job'] == 0 and speech['engine_errors'] == 0
        result = dict(source='HARDWARE_MEASURED', result='PASS', controller_boot=boot,
            samples=[before['sample_uptime_ms'], after['sample_uptime_ms']], last_command=after['last_command'],
            task_jobs=[rt_before['jobs'],rt_after['jobs']], output_inhibited=True, light='OFF', fan='OFF',
            phone_proposal_only=phone, manual_proposal_compatibility=True, wrong_boot_room_zero_nonce_rejected=True,
            confirmations_sent=0, actuator_commands_sent=0, esp_asr_jobs=0, physical_switching_tested=False,
            rtos=rt_after, memory=memory)
        print(json.dumps(result, indent=2))
        (evidence/'gateway-live-result.json').write_text(json.dumps(result, indent=2)+'\n')
    except Exception as e:
        error = str(e); raise
    finally:
        (evidence/'gateway-live-observations.json').write_text(json.dumps(dict(records=records,error=error),indent=2)+'\n')
        b.stack.close()

if __name__ == '__main__':
    main()
