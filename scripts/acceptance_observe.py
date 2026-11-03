"""Bounded, read-only acceptance capture resolved by verified USB adapter identity."""
import argparse
from datetime import datetime, timezone
import json
from pathlib import Path
import sys
import time
from serial.tools import list_ports

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from scripts.research_hil import Bench

IDENTITIES = {
    'A': ('5C37271524', 'e0:72:a1:f5:bc:b0'),
    'B': ('5C37271527', 'e0:72:a1:f6:a9:68'),
    'C': ('5A7B059772', '80:f3:da:63:58:fc'),
}
EVIDENCE = ROOT / 'benchmarks/review/three-board-acceptance-20261102'

def resolve():
    ports = list(list_ports.comports())
    found = {}
    for role, (usb, mac) in IDENTITIES.items():
        matches = [p for p in ports if p.serial_number == usb]
        if len(matches) != 1:
            raise RuntimeError(f'{role}: verified USB adapter missing or ambiguous; no port guessing')
        found[role] = dict(port=matches[0].device, usb_serial=usb, verified_MAC=mac,
                           MAC_provenance='Prior ROM and guarded deployment; USB binding re-enumerated now')
    return found

def main(args):
    if not args.phase.replace('-', '').isalnum():
        raise ValueError('Invalid phase')
    EVIDENCE.mkdir(exist_ok=True)
    output = EVIDENCE / (args.phase + '.json')
    live = EVIDENCE / (args.phase + '-live.jsonl')
    if output.exists() or live.exists():
        raise ValueError('Preserve existing trial; choose a distinct phase')
    identities = resolve()
    bench = None
    rows = []
    read_gaps = []
    error = None
    try:
        bench = Bench([identities['A']['port'], identities['B']['port']], native=True)
        def read(path):
            response=bench.http('GET',path,timeout=12)
            if response is None or response.get('status')==502:
                gap=dict(UTC=datetime.now(timezone.utc).isoformat(),path=path,
                         first_status=response.get('status') if response else None,
                         reason=(response.get('body',{}).get('error') if response else 'No bridge response'),
                         read_only_retry=True)
                read_gaps.append(gap)
                print('READ_CAPTURE_GAP '+json.dumps(gap),flush=True)
                bench.poll(.25)
                response=bench.http('GET',path,timeout=12)
                gap['retry_status']=response.get('status') if response else None
            return response
        print('READ_ONLY_ACCEPTANCE_OBSERVER_READY', flush=True)
        last = None
        with live.open('x', encoding='utf-8') as journal:
            end = time.monotonic() + args.seconds
            while time.monotonic() < end and not output.with_suffix('.stop').exists():
                row = dict(UTC=datetime.now(timezone.utc).isoformat(), host_monotonic_ns=time.monotonic_ns(), rooms={})
                for room, node, suffix in [('room1', 1, ''), ('room2', 3, '?room=2')]:
                    state = read('/api/state' + suffix)
                    tx = read('/api/transaction' + suffix)
                    if not state or not tx or state['status'] != 200 or tx['status'] != 200:
                        raise RuntimeError('Read-only diagnostic response missing; no command retry')
                    if state['body'].get('node_id') != node or tx['body'].get('node_id') != node:
                        raise RuntimeError('Wrong-node response')
                    row['rooms'][room] = dict(state=state['body'], transaction=tx['body'])
                rows.append(row)
                journal.write(json.dumps(row) + '\n')
                journal.flush()
                brief = {room: {k: v['state'].get(k) for k in (
                    'boot_id', 'known', 'last_command', 'light', 'fan', 'manual_flags',
                    'light_schedule_ms', 'fan_schedule_ms', 'light_schedule_result', 'fan_schedule_result',
                    'light_control_source', 'fan_control_source', 'light_energy_cap_ms', 'fan_energy_cap_ms')}
                    for room, v in row['rooms'].items()}
                # Do not emit each countdown tick, but retain every row above.
                key = tuple((room, *(value.get(k) for k in value if not k.endswith('_ms')),
                             row['rooms'][room]['transaction'].get('id'),
                             row['rooms'][room]['transaction'].get('phase'),
                             row['rooms'][room]['transaction'].get('status')) for room, value in brief.items())
                if key != last:
                    print(json.dumps(brief), flush=True)
                    last = key
                bench.poll(.3)
    except Exception as failure:
        error = str(failure)
    finally:
        if bench:
            bench.stack.close()
        output.write_text(json.dumps(dict(source='HARDWARE_MEASURED', phase=args.phase,
            identities=identities, read_only=True, actuator_commands_sent=False, rows=rows,
            error=error, read_gaps=read_gaps, owned_serial_sessions_closed=True,
            physical_LED_observations='Recorded separately from user replies',
            microphone_audio_retained=False), indent=2) + '\n', encoding='utf-8')
    print(json.dumps(dict(phase=args.phase, rows=len(rows), error=error, serial_closed=True)), flush=True)
    return int(error is not None)

if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--phase', required=True)
    p.add_argument('--seconds', type=int, choices=range(1, 601), default=180)
    raise SystemExit(main(p.parse_args()))
