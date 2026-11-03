"""Finite read-only two-room API/ACK observer. Never dispatches device commands."""
import argparse
import json
import sys
import time
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT))
from scripts.research_hil import Bench

def main(args):
    if not args.phase.replace('-','').isalnum():raise ValueError('Invalid phase')
    output=ROOT/'benchmarks/review/three-board-20261102'/(args.phase+'.json')
    if output.exists():raise ValueError('Preserve previous trial evidence')
    bench=None;rows=[];error=None;last=None
    try:
        bench=Bench(['COM10','COM9'],native=True)
        print('READ_ONLY_OBSERVER_READY',flush=True)
        end=time.monotonic()+args.seconds
        while time.monotonic()<end:
            if output.with_suffix('.stop').exists():break
            row={'host_monotonic_ns':time.monotonic_ns(),'rooms':{}}
            for room,node,suffix in [('room1',1,''),('room2',3,'?room=2')]:
                state=bench.http('GET','/api/state'+suffix,timeout=4)
                tx=bench.http('GET','/api/transaction'+suffix,timeout=4)
                if not state or state['status']!=200 or not tx or tx['status']!=200:
                    raise RuntimeError('Diagnostic transport unavailable; no command retries')
                s=state['body'];t=tx['body']
                if s.get('node_id')!=node or t.get('node_id')!=node:
                    raise RuntimeError('Wrong-node diagnostic response')
                row['rooms'][room]={'state':s,'transaction':t}
            rows.append(row)
            key=tuple((v['state'].get('boot_id'),v['state'].get('last_command'),
                       v['state'].get('light'),v['state'].get('fan'),v['state'].get('known'),
                       v['transaction'].get('id'),v['transaction'].get('phase'),v['transaction'].get('status'))
                      for v in row['rooms'].values())
            if key!=last:
                brief={room:{'node':v['state'].get('node_id'),'boot':v['state'].get('boot_id'),
                       'command':v['state'].get('last_command'),'light':v['state'].get('light'),
                       'fan':v['state'].get('fan'),'ack_id':v['transaction'].get('id'),
                       'ack_phase':v['transaction'].get('phase'),'ack_status':v['transaction'].get('status')}
                       for room,v in row['rooms'].items()}
                print(json.dumps(brief),flush=True);last=key
            bench.poll(.3)
    except KeyboardInterrupt:
        pass # User has completed this bounded phase; all observations retained.
    except Exception as failure:
        error=str(failure)
    finally:
        output.write_text(json.dumps({'source':'HARDWARE_MEASURED','phase':args.phase,'read_only':True,
                         'actuator_commands_sent':False,'rows':rows,'error':error,
                         'physical_LED_observations':'Recorded separately from user replies',
                         'microphone_audio_retained':False},indent=2)+'\n',encoding='utf-8')
        if bench:bench.stack.close()
    print(json.dumps({'phase':args.phase,'rows':len(rows),'error':error,'serial_closed':True}),flush=True)
    return 1 if error else 0

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--phase',required=True);p.add_argument('--seconds',type=int,choices=range(1,181),default=120)
    raise SystemExit(main(p.parse_args()))
