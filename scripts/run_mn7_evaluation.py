"""One guarded MN7 deployment/preflight, with one rollback on failure. No phone audio."""
import argparse
import json
import subprocess
import sys
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT))
from scripts.research_hil import Bench

def main(a):
    if (a.output/'evaluation.json').exists():raise RuntimeError('Existing evaluation preserved; no automatic repeat')
    a.output.mkdir(parents=True,exist_ok=True)
    status=dict(experiment='mn7-eval-v1',phone_clips=0,control_commands=0,state='DEPLOYING',rollback='not needed')
    def save():(a.output/'evaluation.json').write_text(json.dumps(status,indent=2)+'\n')
    def run(name,cmd,timeout=600):
        with (a.output/(name+'.log')).open('w',encoding='utf-8') as log:
            r=subprocess.run([sys.executable,*cmd],cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,timeout=timeout)
        status[name+'_exit']=r.returncode;save();print(name,r.returncode,flush=True);return r.returncode
    save();deployed=False
    try:
        code=run('deploy',['scripts/mn7_migration.py','deploy','--port',a.gateway,'--backup',str(a.backup),'--candidate',str(a.candidate)])
        if code:raise RuntimeError('Deployment guard/verification failed; inspect migration result for its rollback receipt')
        deployed=True;status['state']='PREFLIGHT';save()
        bench=Bench([a.controller,a.gateway],native=True,simulation_only=True,speech=True)
        try:
            bench.poll(6)
            status['post_deploy_health']=bench.request(a.controller,b'q','task_health')
            (a.output/'startup-records.json').write_text(json.dumps(bench.records,indent=2)+'\n')
        finally:bench.stack.close()
        code=run('preflight',['scripts/with_gateway_ap.py','--output',str(a.output/'wifi.json'),'--',sys.executable,'scripts/mn7_preflight.py',
            '--node-a',a.controller,'--node-b',a.gateway,'--backup',str(a.backup),'--output',str(a.output/'preflight.json')],180)
        if code:raise RuntimeError('Frozen preflight failed/inconclusive; no tuning or phone clips')
        report=json.loads((a.output/'preflight.json').read_text())
        if report['status']!='PASS':raise RuntimeError('Preflight did not pass')
        status['state']='PREFLIGHT_PASS_PHONE_EVALUATION_NOT_STARTED'
    except Exception as e:
        status['error']=str(e);status['state']='STOPPED';save()
        if deployed:
            code=run('rollback',['scripts/mn7_migration.py','rollback','--port',a.gateway,'--backup',str(a.backup)])
            status['rollback']='FLASH_VERIFIED_RUNTIME_CHECK_REQUIRED' if not code else 'FAILED_STOP_PHYSICAL_RECOVERY_NEEDED'
            if not code:
                code=run('baseline-runtime',['benchmarks/review/physical-phone-20261023/phone_probe.py','--gateway',a.gateway,'--seconds','3','--name','mn7-rollback-20261025'],35)
                if not code:status['rollback']='FLASH_AND_BASELINE_RUNTIME_VERIFIED'
        status['state']='FAILED_STOP_NO_PHONE_COLLECTION';save()
    finally:
        status['serial_closed']=True;save()
    print(json.dumps(status,indent=2),flush=True)
    return 0 if status['state']=='PREFLIGHT_PASS_PHONE_EVALUATION_NOT_STARTED' else 1

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--controller',required=True);p.add_argument('--gateway',required=True);p.add_argument('--backup',type=Path,required=True);p.add_argument('--candidate',type=Path,required=True);p.add_argument('--output',type=Path,required=True)
    raise SystemExit(main(p.parse_args()))
