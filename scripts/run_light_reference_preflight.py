"""ONLY four recognition references, then exact baseline app restoration. No phone phase."""
import argparse
import hashlib
import json
import shutil
import subprocess
import sys
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT))
from scripts.research_hil import Bench
from scripts.mn7_migration import Rom,check_backup

def run(a):
    out=a.output
    if (out/'campaign.json').exists():raise RuntimeError('Existing campaign preserved; no rerun')
    ids=json.loads(a.identities.read_text())['observations']
    ports={x['role']:x['port'] for x in ids};cfg=json.loads((ROOT/'local_credentials.json').read_text())
    for role,key in [('A','node_a_mac'),('B','node_b_mac')]:
        if not any(x['role']==role and x['mac']==cfg[key].lower() and x['identity']=='ROM_MAC_MATCH' for x in ids):
            raise RuntimeError('Fresh verified identities required')
    backup=ROOT/'benchmarks/backups/mn7-20261025T072239Z';check_backup(backup)
    baseline=out/'baseline-firmware.bin'
    if hashlib.sha256(baseline.read_bytes()).hexdigest()!='f30b10cfb74a4dd79ff28588de2d3be9f6c760c934b48cf1475bf6f7c271b9a4':
        raise RuntimeError('Rollback image changed')
    status=dict(experiment='light-reference-preflight-v1',state='DEPLOYING',phone_clips=0,
        proposals=0,commands=0,maximum_audio_jobs=4,selected_room=a.room,restore='pending')
    def save():(out/'campaign.json').write_text(json.dumps(status,indent=2)+'\n')
    def child(name,command,timeout=200):
        with (out/(name+'.log')).open('w',encoding='utf-8') as log:
            p=subprocess.run([sys.executable,*command],cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,timeout=timeout)
        status[name+'_exit']=p.returncode;save();print(name,p.returncode,flush=True);return p.returncode
    restore_needed=False;save()
    try:
        code=child('deploy',['scripts/flash_native.py','--role','B-speech','--port',ports['B'],
            '--uart-console','--app-only','--voice-experiment','light-preflight-v1'])
        text=(out/'deploy.log').read_text(encoding='utf-8')
        restore_needed=code==0 or 'Flash FAIL:' in text or 'write-flash' in text
        if code:raise RuntimeError('Deployment guard/write failed; no audio')
        bench=Bench([ports['A'],ports['B']],native=True,simulation_only=True,speech=True)
        try:
            bench.poll(4)
            (out/'startup-records.json').write_text(json.dumps(bench.records,indent=2)+'\n')
        finally:bench.stack.close()
        status['state']='FOUR_CLIP_PREFLIGHT';save()
        code=child('preflight',['scripts/with_gateway_ap.py','--output',str(out/'wifi.json'),'--',
            sys.executable,'scripts/light_reference_preflight.py','--room',a.room,
            '--node-a',ports['A'],'--node-b',ports['B'],'--identities',str(a.identities),
            '--output',str(out/'preflight.json')],140)
        report=json.loads((out/'preflight.json').read_text()) if (out/'preflight.json').exists() else {}
        status['preflight_status']=report.get('status','NO_RESULT')
        status['state']='PREFLIGHT_PASS_PHONE_NOT_AUTHORIZED' if code==0 and report.get('status')=='PASS' else 'PREFLIGHT_FAILED_STOP'
    except Exception as e:
        status['error']=str(e);status['state']='STOPPED'
        if (out/'deploy.log').exists() and 'write-flash' in (out/'deploy.log').read_text(encoding='utf-8'):
            restore_needed=True
    finally:
        if restore_needed:
            # PlatformIO may purge other environment caches. Restore saved files,
            # never rebuild a substitute or alter the rollback model/NVS.
            dst=ROOT/'speech/.pio/build/speech_intelligence_uart';dst.mkdir(parents=True,exist_ok=True)
            for name in ['firmware.bin','firmware.elf']:shutil.copy2(out/('baseline-'+name),dst/name)
            status['restore']='IN_PROGRESS';save()
            code=child('restore',['scripts/flash_native.py','--role','B-speech','--port',ports['B'],
                '--uart-console','--app-only'])
            status['restore']='APP_FLASH_VERIFIED' if code==0 else 'FAILED_STOP'
            if not code:
                logs=out/'restored-flash-verification';logs.mkdir(exist_ok=False);rom=Rom(ports['B'],logs)
                matched=False
                try:
                    rom.identity(cfg['node_b_mac'].lower());matched=True
                    for address,path in [('0x10000',baseline),('0x610000',backup/'model.bin'),('0',backup/'system.bin')]:
                        rom.run(['verify-flash',address,str(path)])
                    status['restore']='BASELINE_APP_MODEL_SYSTEM_FLASH_VERIFIED'
                finally:
                    if matched:rom.run(['read-mac'],after='hard-reset',timeout=30)
                code=child('baseline-runtime',['benchmarks/review/physical-phone-20261023/phone_probe.py',
                    '--gateway',ports['B'],'--seconds','3','--name','light-reference-restored-20261025'],35)
                if not code:status['restore']='BASELINE_FLASH_AND_RUNTIME_VERIFIED'
        else:status['restore']='No candidate write identified; inspect blocked hardware/guard state'
        status['serial_closed']=True;save()
    print(json.dumps(status,indent=2),flush=True)
    return 0 if status['state']=='PREFLIGHT_PASS_PHONE_NOT_AUTHORIZED' and status['restore']=='BASELINE_FLASH_AND_RUNTIME_VERIFIED' else 1

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--room',choices=['bedroom'],required=True)
    p.add_argument('--identities',type=Path,required=True);p.add_argument('--output',type=Path,required=True)
    raise SystemExit(run(p.parse_args()))
