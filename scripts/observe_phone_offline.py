"""Read-only finite phone-test observer. Never proposes, confirms or replays commands."""
import argparse
import json
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from scripts.research_hil import Bench

def main(args):
    evidence = ROOT / args.evidence
    assert args.name.replace('-','').replace('_','').isalnum()
    destination = evidence / (args.name + '.json')
    if destination.exists(): raise RuntimeError('Evidence already exists; choose a new phase name')
    identities = json.loads((evidence/'board-identities.json').read_text())['observations']
    roles = {row['role']: row for row in identities}
    config = json.loads((ROOT/'local_credentials.json').read_text())
    for role in ['A','B']:
        assert roles[role]['identity'] == 'ROM_MAC_MATCH' and roles[role]['mac'] == config['node_'+role.lower()+'_mac'].lower()
    b = Bench([roles['A']['port'],roles['B']['port']],native=True,simulation_only=True,speech=True)
    observations=[]; error=None; previous=None; boot=None
    try:
        end=time.monotonic()+args.seconds
        while time.monotonic()<end:
            row=dict(host_ns=time.monotonic_ns())
            for name,path in [('state','/api/state'),('transaction','/api/transaction'),('rtos','/api/rtos')]:
                response=b.http('GET',path,timeout=4)
                if not response or response['status']!=200: raise RuntimeError('Read-only diagnostics unavailable; no commands retried')
                row[name]=response['body']
            observations.append(row)
            state=row['state'];tx=row['transaction'];rt=row['rtos']
            if state.get('physical_outputs_enabled') is not False: raise RuntimeError('STOP: outputs not explicitly inhibited')
            if not state.get('known') or not state.get('authenticated') or state.get('age_ms',99999)>=3000: raise RuntimeError('STOP: stale/unknown controller')
            if boot is None: boot=state['boot_id']
            if state['boot_id']!=boot: raise RuntimeError('STOP: controller reboot, old test context invalid')
            key=(state['boot_id'],state['last_command'],state['light'],tx['id'],tx['phase'],tx['status'])
            if key!=previous:
                print(json.dumps(dict(state=state,transaction=tx,rtos=rt)),flush=True);previous=key
            if tx['phase']==3 or (tx['phase']==2 and tx['status']!=0): raise RuntimeError('STOP: transaction timeout/rejection; no retry')
            b.poll(.25)
    except Exception as e:
        error=str(e);print(error,flush=True)
    finally:
        destination.write_text(json.dumps(dict(source='HARDWARE_MEASURED',phase=args.name,
            observer_read_only=True,observations=observations,error=error),indent=2)+'\n')
        b.stack.close()
    return 1 if error else 0

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--seconds',type=int,default=60,choices=range(1,181))
    p.add_argument('--name',required=True)
    p.add_argument('--evidence',default='benchmarks/review/phone-offline-v1-20261025')
    raise SystemExit(main(p.parse_args()))
