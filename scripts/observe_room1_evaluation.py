"""Finite read-only Room1 capture; never opens, polls or commands Room2."""
import argparse
from datetime import datetime,timezone
import json
from pathlib import Path
import sys
import time
from serial.tools import list_ports
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT))
from scripts.research_hil import Bench
EVIDENCE=ROOT/'benchmarks/review/room1-sensor-order-20261102'

def resolve_room1():
    ports=list(list_ports.comports());result={}
    for role,serial,mac in [('A','5C37271524','e0:72:a1:f5:bc:b0'),('B','5C37271527','e0:72:a1:f6:a9:68')]:
        found=[p for p in ports if p.serial_number==serial]
        if len(found)!=1:raise RuntimeError('Required Room1/gateway identity missing or ambiguous')
        result[role]=dict(port=found[0].device,USB_serial=serial,verified_MAC=mac,MAC_provenance='Existing ROM/guarded deployment identity; USB re-enumerated')
    return result

def main(args):
    if not args.phase.replace('-','').isalnum():raise ValueError('Invalid phase')
    out=EVIDENCE/(args.phase+'.json');live=out.with_suffix('.jsonl')
    if out.exists() or live.exists():raise ValueError('Preserve previous evidence')
    b=None;rows=[];gaps=[];error=None;ids=resolve_room1()
    try:
        b=Bench([ids['A']['port'],ids['B']['port']],native=True)
        print('ROOM1_READ_ONLY_READY',flush=True)
        def read(path):
            reply=b.http('GET',path,timeout=5)
            if reply is None or reply.get('status')==502:
                gap=dict(path=path,UTC=datetime.now(timezone.utc).isoformat(),first_status=reply.get('status') if reply else None)
                gaps.append(gap);b.poll(.25);reply=b.http('GET',path,timeout=5);gap['retry_status']=reply.get('status') if reply else None
            if not reply or reply['status']!=200:raise RuntimeError('Read-only diagnostic unavailable')
            return reply['body']
        end=time.monotonic()+args.seconds;last=None
        with live.open('x',encoding='utf-8') as journal:
            while time.monotonic()<end and not out.with_suffix('.stop').exists():
                s=read('/api/state');tx=read('/api/transaction')
                if s.get('node_id')!=1 or tx.get('node_id')!=1:raise RuntimeError('Wrong node')
                row=dict(UTC=datetime.now(timezone.utc).isoformat(),host_monotonic_ns=time.monotonic_ns(),state=s,transaction=tx)
                rows.append(row);journal.write(json.dumps(row)+'\n');journal.flush()
                key=tuple(s.get(k) for k in ['boot_id','last_command','light','fan','sensor_order','auto_flags','manual_flags'])
                if key!=last:
                    print(json.dumps({k:s.get(k) for k in ['boot_id','last_command','light','fan','sensor_order','auto_flags','manual_flags','evaluation_cycle','light_eval_step','fan_eval_step','temperature_deci_c','light_input_raw']}),flush=True);last=key
                b.poll(.2)
    except Exception as failure:error=str(failure)
    finally:
        if b:b.stack.close()
        out.write_text(json.dumps(dict(source='HARDWARE_MEASURED',scope='ROOM1 ONLY',Room2_accessed=False,read_only=True,actuator_commands_sent=False,identities=ids,rows=rows,read_gaps=gaps,error=error,owned_serial_sessions_closed=True),indent=2)+'\n',encoding='utf-8')
    print(json.dumps(dict(rows=len(rows),error=error,serial_closed=True)),flush=True)

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--phase',required=True);p.add_argument('--seconds',type=int,choices=range(1,601),default=120);main(p.parse_args())
