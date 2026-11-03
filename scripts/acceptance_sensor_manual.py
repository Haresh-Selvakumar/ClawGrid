"""One finite Room1 manual-priority case; no immediate ON commands or C writes.

An ACK/readback-proven 15s OFF countdown precedes the 30s pending ON rule.
Manual OFF must cancel that rule before any fallback. Otherwise the case fails.
Real sensor values only. Existing OFF proposal/confirmation and SafetyKernel apply.
"""
from datetime import datetime, timezone
import json
from pathlib import Path
import sys
import time

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from scripts.acceptance_observe import resolve
from scripts.research_hil import Bench

OUT = ROOT / 'benchmarks/review/sensor-auto-20261102/manual-priority.json'

def main():
    if OUT.exists():
        raise RuntimeError('Preserve prior trial')
    b = None
    rows, operations = [], []
    result = dict(source='HARDWARE_MEASURED', read_only=False,
                  Room2_commands_sent=False, immediate_ON_commands_sent=False,
                  synthetic_sensor_input=False, fallback_used=False, status='NOT_PASS')
    boot = None
    try:
        identities = resolve()
        b = Bench([identities['A']['port'], identities['B']['port']], native=True)
        def api(method, path, body=None):
            r = b.http(method, path, b'' if body is None else body.encode(), timeout=5)
            if not r or r['status'] not in (200, 202):
                raise RuntimeError('Transport/rejection; no mutation retry')
            return r['body']
        def capture():
            row = dict(UTC=datetime.now(timezone.utc).isoformat(), host_monotonic_ns=time.monotonic_ns(), rooms={})
            for name, node, suffix in [('room1', 1, ''), ('room2', 3, '?room=2')]:
                s = api('GET', '/api/state'+suffix)
                tx = api('GET', '/api/transaction'+suffix)
                if s.get('node_id') != node or tx.get('node_id') != node:
                    raise RuntimeError('Wrong room/node')
                if not s.get('known') or not s.get('authenticated') or s.get('age_ms', 9999) >= 3000:
                    raise RuntimeError('Required fresh authoritative telemetry unavailable')
                row['rooms'][name] = dict(state=s, transaction=tx)
            rows.append(row)
            return row
        first = capture()
        a, c = (first['rooms'][room]['state'] for room in ['room1', 'room2'])
        boot = a['boot_id']; c_baseline = (c['boot_id'], c['last_command'])
        if a.get('features_version') != 2 or a['fan'] or not (a['auto_flags'] & 2) or a['manual_flags'] & 2:
            raise RuntimeError('Expected Room1 fan AUTO/OFF baseline')
        if c['light'] or c['fan'] or any(a[k] for k in ['light_schedule_ms', 'fan_schedule_ms', 'light_timer_ms', 'fan_timer_ms']):
            raise RuntimeError('Unrelated pending rule/output; do not replace it')
        expected_config = 290 | (300 << 9) | (60 << 18)
        if a['fan_sensor_config'] != expected_config or a.get('temperature_status') != 'OK' or a['temperature_deci_c'] > 290:
            raise RuntimeError('Required real fresh sensor-OFF baseline not met')
        def await_applied(queued, name, check):
            if queued.get('queued') is not True:
                raise RuntimeError('Not queued')
            op = dict(name=name, id=queued['id'], applied=False)
            operations.append(op)
            end = time.monotonic()+5
            while time.monotonic() < end:
                row = capture(); s = row['rooms']['room1']['state']; tx = row['rooms']['room1']['transaction']
                other = row['rooms']['room2']['state']
                if (other['boot_id'], other['last_command']) != c_baseline or other['light'] or other['fan']:
                    raise RuntimeError('Room2 changed; stop this isolated case')
                if s['boot_id'] != boot:
                    raise RuntimeError('Room1 boot changed')
                if tx.get('id') == queued['id'] and tx.get('phase') == 2:
                    if tx.get('status') != 0 or not tx.get('outcome_known'):
                        raise RuntimeError('Controller rejected operation')
                    if s['last_command'] == queued['id'] and check(s):
                        op.update(applied=True, UTC=row['UTC'], state=s, transaction=tx)
                        return s
                b.poll(.1)
            raise RuntimeError('ACK/readback incomplete; no retry')
        def config(op, device, action, value, name, check):
            return await_applied(api('POST', '/api/control', f'boot {boot} {op} {device} {action} {value}'), name, check)
        def manual_off(device):
            p = api('POST', '/api/proposal', f'manual physical_bedroom bedroom {device} OFF 0')
            if p.get('controller_boot') != boot or p.get('room') != 'bedroom' or p.get('device') != device or p.get('action') != 'OFF' or p.get('authorized') is not False:
                raise RuntimeError('Proposal binding mismatch')
            return await_applied(api('POST', '/api/confirm', f"{p['proposal_id']} physical_bedroom bedroom {device} OFF"),
                'manual_'+device+'_OFF', lambda s: s[device] is False)
        manual_off('light')
        config(3, 1, 0, 15000, 'fallback_OFF_countdown', lambda s: 3000 < s['fan_timer_ms'] <= 15000 and s['auto_flags'] & 2)
        arm = config(7, 1, 1, 30000, 'arm_pending_fan_ON', lambda s: s['fan_schedule_ms'] > 20000 and s['fan_timer_ms'] > 2000 and s['auto_flags'] & 2)
        due = time.monotonic() + arm['fan_schedule_ms']/1000
        off = manual_off('fan')
        if off['fan_schedule_ms'] or off['fan_schedule_result'] != 4 or not off['manual_flags'] & 2 or off['auto_flags'] & 2:
            raise RuntimeError('Applied manual OFF did not cancel pending ON')
        result['manual_OFF_cancelled_before_fallback'] = True
        config(3, 1, 0, 0, 'cancel_guard_after_manual_proof', lambda s: s['fan_timer_ms'] == 0)
        demanding = 260 | (270 << 9) | (60 << 18)
        config(9, 1, 0, demanding, 'sensor_ON_thresholds_while_MANUAL', lambda s: s['fan_sensor_config'] == demanding and s['manual_flags'] & 2 and not s['auto_flags'] & 2)
        observed = []
        while time.monotonic() < due+2:
            row = capture(); s = row['rooms']['room1']['state']; other = row['rooms']['room2']['state']
            if s['boot_id'] != boot or (other['boot_id'], other['last_command']) != c_baseline or any(s[d] or other[d] for d in ['light','fan']):
                raise RuntimeError('Unexpected boot/command/output during manual hold')
            if s.get('temperature_status') == 'OK' and s.get('temperature_deci_c', 0) >= 270 and s['fan_sensor_demand'] == 2:
                if not s['manual_flags'] & 2 or s['auto_flags'] & 2 or s['fan_winner'] != 1 or s['fan_blocking'] != 1:
                    raise RuntimeError('Manual/source flag mismatch')
                observed.append(row['UTC'])
            b.poll(.2)
        if not observed:
            raise RuntimeError('No real sensor ON-demand evidence; no synthetic substitute')
        result['fresh_sensor_ON_demand_held_OFF_samples'] = len(observed)
        result['old_ON_due_passed_without_execution'] = True
        preferred = 260 | (280 << 9) | (60 << 18)
        config(9, 1, 0, preferred, 'restore_preferred_28_26_MANUAL', lambda s: s['fan_sensor_config'] == preferred and s['manual_flags'] & 2 and not s['auto_flags'] & 2 and not s['fan'])
        result['status'] = 'PASS'
    except Exception as error:
        result['error'] = str(error)
        # The ACK/readback-proven earlier countdown prevents late scheduled ON
        # independently of host cleanup. A failed case is never called PASS.
        if b and boot:
            result['fallback_used'] = True
            try:
                result['fallback_cancel_response'] = b.http('POST', '/api/control', f'boot {boot} 7 1 2 0'.encode(), timeout=5)
            except Exception as cleanup_error:
                result['cleanup_error'] = str(cleanup_error)
    finally:
        if b:
            b.stack.close()
        result.update(rows=rows, operations=operations, serial_closed=True,
                      visible_LED_observations='Separate user reply required')
        OUT.write_text(json.dumps(result, indent=2)+'\n', encoding='utf-8')
    print(json.dumps({k: result.get(k) for k in ['status','error','manual_OFF_cancelled_before_fallback','fallback_used','fresh_sensor_ON_demand_held_OFF_samples','serial_closed']}))

if __name__ == '__main__':
    main()
