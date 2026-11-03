"""Bounded read-only ROM flash-ID inventory. Never writes flash or NVS."""
import argparse
import json
import re
import subprocess
import sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]

def main(args):
    out=ROOT/'benchmarks/review/three-board-20261102'
    out.mkdir(parents=True,exist_ok=True)
    results=[]
    expected={'A':'e0:72:a1:f5:bc:b0','B':'e0:72:a1:f6:a9:68'}
    for role,port in [('A','COM8'),('B','COM9'),('C','COM11')]:
        if args.role and role!=args.role: continue
        command=[sys.executable,'-m','esptool','--port',port,'--baud','115200']
        if args.no_reset: command+=['--before','no-reset']
        command+=['flash-id']
        try:
            p=subprocess.run(command,capture_output=True,text=True,timeout=25)
            output=p.stdout+'\n'+p.stderr; code=p.returncode
        except subprocess.TimeoutExpired as error:
            def decode(v): return v.decode(errors='replace') if isinstance(v,bytes) else v or ''
            output=decode(error.stdout)+decode(error.stderr)+'\nBounded probe timed out.';code=-1
        log=out/f'ROM-{role}-{args.label}.log'
        with log.open('x',encoding='utf-8') as f:f.write(output)
        mac=re.search(r'^MAC:\s*([0-9a-f:]{17})',output,re.I|re.M)
        row={'role':role,'port':port,'exit_code':code,'log':str(log.relative_to(ROOT)),
             'mac':mac[1].lower() if mac else None,'expected_mac_match':bool(mac and mac[1].lower()==expected[role]) if role in expected else None,
             'source':'HARDWARE_MEASURED','flash_write':False}
        results.append(row)
        print(output,flush=True);print(json.dumps(row),flush=True)
    with (out/f'ROM-inventory-{args.label}.json').open('x',encoding='utf-8') as f:json.dump(results,f,indent=2)

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--role',choices=['A','B','C']);p.add_argument('--no-reset',action='store_true');p.add_argument('--label',required=True)
    a=p.parse_args()
    if not re.fullmatch(r'[a-z0-9-]+',a.label):p.error('Invalid label')
    main(a)
