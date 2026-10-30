"""Finite read-only observer for the explicitly deployed LED-only profile; never commands GPIO."""
import argparse
import json
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from scripts.research_hil import Bench


def main(args):
    folder = (ROOT / args.evidence).resolve()
    if not folder.is_relative_to((ROOT / 'benchmarks/review').resolve()):
        raise ValueError('Evidence must remain inside review workspace')
    if not args.name.replace('-', '').replace('_', '').isalnum():
        raise ValueError('Invalid phase name')
    target = folder / (args.name + '.json')
    if target.exists():
        raise RuntimeError('Preserve prior evidence; choose a new phase')
    identities = json.loads((folder / 'board-identities.json').read_text())['observations']
    config = json.loads((ROOT / 'local_credentials.json').read_text())
    roles = {x['role']: x for x in identities}
    for role in ['A', 'B']:
        assert roles[role]['identity'] == 'ROM_MAC_MATCH'
        assert roles[role]['mac'] == config['node_' + role.lower() + '_mac'].lower()
    bench = None
    observations = []
    error = None
    boot = None
    previous = None
    profile = None
    try:
        bench = Bench([roles['A']['port'], roles['B']['port']], native=True, simulation_only=False, speech=True)
        end = time.monotonic() + args.seconds
        while time.monotonic() < end:
            row = {'host_ns': time.monotonic_ns()}
            for name, path in [('state', '/api/state'), ('transaction', '/api/transaction'), ('rtos', '/api/rtos')]:
                response = bench.http('GET', path, timeout=4)
                if not response or response['status'] != 200:
                    raise RuntimeError('Diagnostic unavailable; no command retry')
                row[name] = response['body']
            observations.append(row)
            state, tx = row['state'], row['transaction']
            if not state.get('known') or not state.get('authenticated') or state.get('age_ms', 99999) >= 3000:
                raise RuntimeError('STOP stale/unknown controller; do not send more commands')
            if state.get('physical_outputs_enabled') is not True or state.get('output_profile') not in ('LED_INDICATORS_10K_ACTIVE_LOW', 'LED_INDICATORS_10K_ACTIVE_HIGH'):
                raise RuntimeError('STOP output profile changed')
            if profile is None:
                profile = state['output_profile']
            if state['output_profile'] != profile:
                raise RuntimeError('STOP LED polarity/profile changed during test')
            if boot is None:
                boot = state['boot_id']
            if state['boot_id'] != boot:
                raise RuntimeError('STOP controller reboot; old test context invalid')
            key = (state['boot_id'], state['last_command'], state['light'], state['fan'], tx['id'], tx['phase'], tx['status'])
            if key != previous:
                print(json.dumps(row), flush=True)
                previous = key
            if tx['phase'] == 3 or (tx['phase'] == 2 and tx['status'] != 0):
                raise RuntimeError('STOP rejection/timeout; no automatic retry')
            bench.poll(.25)
    except Exception as exc:
        error = str(exc)
        print(error, flush=True)
    finally:
        target.write_text(json.dumps(dict(source='HARDWARE_MEASURED', read_only=True,
            observations=observations, error=error, physical_LED_visibility='REQUIRES_USER_OBSERVATION'), indent=2))
        if bench:
            bench.stack.close()
    return 1 if error else 0


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--evidence', required=True)
    parser.add_argument('--name', required=True)
    parser.add_argument('--seconds', type=int, choices=range(1, 181), default=90)
    raise SystemExit(main(parser.parse_args()))
