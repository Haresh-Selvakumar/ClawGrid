"""Normal acknowledged Room1 OFF cleanup only; no Room2 access or test commands."""
import json
import time
from datetime import datetime, timezone
from observe_room1_evaluation import EVIDENCE, resolve_room1, Bench

out = EVIDENCE / 'final-cleanup.json'
if out.exists():
    raise RuntimeError('Preserve existing cleanup evidence; do not replay')
result = dict(scope='ROOM1 ONLY', Room2_accessed=False, purpose='Final MANUAL/OFF cleanup; no additional tests', operations=[], rows=[])
b = None
try:
    ids = resolve_room1()
    b = Bench([ids['A']['port'], ids['B']['port']], native=True)
    def api(method, path, body=''):
        r = b.http(method, path, body.encode(), timeout=5)
        if method == 'GET' and (not r or r['status'] == 502):
            result.setdefault('read_gaps', []).append(dict(path=path, UTC=datetime.now(timezone.utc).isoformat()))
            b.poll(.25)
            r = b.http(method, path, timeout=5)
        if not r or r['status'] not in (200, 202):
            raise RuntimeError('Unavailable/rejected operation; no mutation retry')
        return r['body']
    def capture():
        row = dict(UTC=datetime.now(timezone.utc).isoformat(), state=api('GET', '/api/state'), transaction=api('GET', '/api/transaction'))
        s = row['state']
        if s.get('node_id') != 1 or not s.get('known') or not s.get('authenticated') or s.get('age_ms',9999) >= 3000:
            raise RuntimeError('Fresh authenticated Room1 state required')
        result['rows'].append(row)
        return row
    first = capture()['state']
    boot = first['boot_id']
    for device in ['light', 'fan']:
        before = capture()['state']['sample_uptime_ms']
        p = api('POST', '/api/proposal', f'manual physical_bedroom bedroom {device} OFF 0')
        if p.get('controller_boot') != boot or p.get('room') != 'bedroom' or p.get('device') != device or p.get('action') != 'OFF' or p.get('authorized') is not False:
            raise RuntimeError('Proposal binding mismatch')
        q = api('POST', '/api/confirm', f"{p['proposal_id']} physical_bedroom bedroom {device} OFF")
        if q.get('queued') is not True:
            raise RuntimeError('Not queued')
        end = time.monotonic() + 6
        while time.monotonic() < end:
            row = capture(); s = row['state']; tx = row['transaction']
            if s['boot_id'] != boot:
                raise RuntimeError('Boot changed')
            if tx.get('id') == q['id'] and tx.get('phase') == 2:
                if tx.get('status') != 0 or not tx.get('outcome_known'):
                    raise RuntimeError('OFF not acknowledged as applied')
                if s['last_command'] == q['id'] and s['sample_uptime_ms'] > before and not s[device]:
                    result['operations'].append(dict(device=device, action='OFF', applied=True, id=q['id'], state=s, transaction=tx))
                    break
            b.poll(.1)
        else:
            raise RuntimeError('OFF fresh readback incomplete; no mutation retry')
    for _ in range(3):
        b.poll(.5)
        final = capture()['state']
        assert final['boot_id'] == boot and not final['light'] and not final['fan']
        assert final['auto_flags'] == 0 and final['manual_flags'] == 3
        assert not any(final[k] for k in ['light_schedule_ms','fan_schedule_ms','light_timer_ms','fan_timer_ms','light_energy_cap_ms','fan_energy_cap_ms','light_override_ms','fan_override_ms'])
    result['complete'] = True
except Exception as error:
    result.update(complete=False, error=str(error))
finally:
    if b:
        b.stack.close()
    result['owned_serial_sessions_closed'] = True
    out.write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
print(json.dumps(dict(complete=result['complete'], error=result.get('error'), operations=[(x['device'],x['id']) for x in result['operations']], serial_closed=True)))
