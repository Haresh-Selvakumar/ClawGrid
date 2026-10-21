"""Reproducible software-only validation. Never discovers, opens, or flashes a board."""
import argparse
from datetime import datetime,timezone
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import time

ROOT=Path(__file__).resolve().parents[1]

def main(args):
    directory=ROOT/'benchmarks/review';directory.mkdir(parents=True,exist_ok=True)
    stamp=datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')
    cases=[('host',[sys.executable,'-m','pytest','tests','-q',f'--basetemp={directory/f"pytest-{stamp}"}',f'--junitxml={directory/f"host-{stamp}.xml"}']),('frontend',[shutil.which('node') or 'node','scripts/check_frontend.cjs']),('dataset',[sys.executable,'scripts/audio_bench.py','--validate-only','--output',str(directory/f'dataset-{stamp}.json')])]
    if args.build:
        cases.extend([('node-a',[sys.executable,'-m','platformio','run','-d','native','-e','native_voice']),('node-b',[sys.executable,'-m','platformio','run','-d','speech','-e','speech_intelligence'])])
    if args.bench:cases.append(('bench-compile-only',[sys.executable,'-m','platformio','run','-d','native','-e','native_voice_bench']))
    results=[]
    environment=os.environ.copy()
    scratch=directory/f'temporary-{stamp}';scratch.mkdir(exist_ok=True)
    environment.update(TMP=str(scratch),TEMP=str(scratch))
    environment.setdefault('ZIG_GLOBAL_CACHE_DIR',str(ROOT/'benchmarks/.host_build/zig-cache'))
    for name,command in cases:
        start=time.monotonic();path=directory/f'{name}-{stamp}.log';record=dict(name=name,command=command,log=str(path.relative_to(ROOT)))
        try:
            with path.open('w',encoding='utf-8') as log:
                result=subprocess.run(command,cwd=ROOT,env=environment,stdout=log,stderr=subprocess.STDOUT,timeout=args.timeout)
            record['exit_code']=result.returncode
        except subprocess.TimeoutExpired:record.update(exit_code=None,error='Timed out; uncompleted validation')
        except OSError as error:record.update(exit_code=None,error=type(error).__name__)
        record['seconds']=round(time.monotonic()-start,3);results.append(record)
        print(name,'exit='+str(record['exit_code']),flush=True)
    report=dict(source='HOST_MEASURED',hardware_access=False,started_utc=stamp,results=results,limitations='Compilation does not establish target recognition, electrical state, sensor accuracy or device timing.')
    path=directory/'software-validation.json';path.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print('Validation record:',path)
    return int(any(r['exit_code']!=0 for r in results))

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--build',action='store_true');parser.add_argument('--bench',action='store_true',help='Compile opt-in bench image only; never deploy it')
    parser.add_argument('--timeout',type=int,default=1800)
    args=parser.parse_args()
    if args.timeout<1:parser.error('--timeout must be positive')
    raise SystemExit(main(args))
