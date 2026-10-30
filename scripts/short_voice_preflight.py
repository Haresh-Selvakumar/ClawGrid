"""Exactly the four frozen short-v1 fixtures on ESP; never proposes/confirms control.

Run under with_gateway_ap.py. Private credentials are read locally, never printed.
"""
import argparse
import hashlib
import json
import sys
import time
import urllib.request
import wave
import zlib
from datetime import datetime, timezone
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT))
from scripts.research_hil import Bench

FROZEN_PLAN_SHA256='e9369a25f9ff6583fb7023cf492326ceb187f2025642836934d126435f3cc700'

def run(args):
    if args.output.exists(): raise RuntimeError('Preserve existing preflight evidence; no automatic rerun')
    plan=(ROOT/'experiments/voice-short-v1.json').read_bytes()
    if hashlib.sha256(plan).hexdigest()!=FROZEN_PLAN_SHA256: raise RuntimeError('Frozen plan changed')
    manifest=json.loads(plan);cfg=json.loads((ROOT/'local_credentials.json').read_text())
    identities=json.loads(args.identities.read_text())['observations']
    for role,port,key in [('A',args.node_a,'node_a_mac'),('B',args.node_b,'node_b_mac')]:
        if not any(x.get('role')==role and x.get('port')==port and x.get('mac')==cfg[key].lower() and x.get('identity')=='ROM_MAC_MATCH' for x in identities):
            raise RuntimeError('Fresh ROM identity record required for '+role)
    report=dict(experiment='short-v1',plan_sha256=FROZEN_PLAN_SHA256,source='HARDWARE_MEASURED',audio_source='SIMULATED',started_utc=datetime.now(timezone.utc).isoformat(),maximum_jobs=4,phone_clips=0,results=[],status='RUNNING',control_issued=False)
    opener=urllib.request.build_opener(urllib.request.ProxyHandler({}));bench=None
    def save():
        args.output.parent.mkdir(parents=True,exist_ok=True)
        args.output.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    def request(path,pcm=None):
        req=urllib.request.Request('http://192.168.4.1'+path,data=pcm,headers={'X-ClawGrid-Token':cfg['api_token'],'Content-Type':'application/octet-stream','Connection':'close'})
        with opener.open(req,timeout=6) as r:
            if r.status!=(200 if pcm is None else 202): raise RuntimeError('Unexpected HTTP status')
            return json.loads(r.read(16384))
    def safe():
        s=request('/api/state')
        if s.get('known') is not True or s.get('authenticated') is not True or s.get('age_ms',99999)>=3000 or s.get('physical_outputs_enabled') is not False:
            raise RuntimeError('Fresh authenticated inhibited controller required')
        if s.get('light') is not False or s.get('fan') is not False: raise RuntimeError('Logical light/fan must be OFF')
        return s
    try:
        bench=Bench([args.node_a,args.node_b],native=True,simulation_only=True,speech=True)
        health=bench.request(args.node_a,b'q','task_health')
        if not health or health.get('physical_outputs_enabled') is not False or health.get('diagnostic_faults_enabled') is not False: raise RuntimeError('A inhibition/default faults not verified')
        report['before']=safe();report['rtos_before']=request('/api/rtos');report['health_before']=health
        ready=request('/api/speech')
        if ready.get('grammar_id')!='short-v1' or ready.get('model_name')!='mn6_en' or ready.get('phase') not in (1,4): raise RuntimeError('Frozen short-v1 model not ready')
        for fixture in manifest['preflight']:
            with wave.open(str(ROOT/fixture['path']),'rb') as w:
                if (w.getframerate(),w.getnchannels(),w.getsampwidth())!=(16000,1,2): raise RuntimeError('Fixture PCM format changed')
                pcm=w.readframes(w.getnframes())
            if (len(pcm)//2,zlib.crc32(pcm),hashlib.sha256(pcm).hexdigest())!=(fixture['samples'],fixture['crc32'],fixture['sha256']): raise RuntimeError('Frozen fixture changed')
            before=safe();queued=request('/api/speech/reference?feed=short-v1',pcm)
            if queued.get('queued') is not True or queued.get('authorized') is not False: raise RuntimeError('Audio not queued safely')
            row=dict(fixture=fixture,job=queued['job']);report['results'].append(row);save()
            deadline=time.monotonic()+15
            while time.monotonic()<deadline:
                result=request('/api/speech');after=safe()
                if result.get('job')!=queued['job'] or after['boot_id']!=before['boot_id']: raise RuntimeError('Job replaced or controller reboot')
                if result.get('phase') in (4,5):break
                bench.poll(.15)
            else:raise RuntimeError('Inference timeout; no retry')
            row['board']=result;row['after']=after;save()
            if result.get('samples')!=fixture['samples'] or result.get('pcm_crc32')!=fixture['crc32']:raise RuntimeError('PCM mismatch')
            if result.get('command_id')!=0 or result.get('authorized') is not False or result.get('diagnostic_only') is not True:raise RuntimeError('Reference crossed command boundary')
            if after['last_command']!=before['last_command'] or result.get('error_code')!=0 or result.get('engine_errors')!=0:raise RuntimeError('Control change or speech engine error')
            accepted=result.get('reference_detected') is True
            expected=fixture['expected_id']
            row['passed']=(not accepted) if expected==0 else (accepted and result.get('candidate_id')==expected and 0.7<=result.get('candidate_score',0)<=1)
            print(json.dumps(dict(fixture=fixture['name'],job=queued['job'],accepted=accepted,candidate_id=result.get('candidate_id'),score=result.get('candidate_score'),passed=row['passed'])),flush=True);save()
            if accepted and (expected==0 or result.get('candidate_id')!=expected):
                report['stop_reason']='Wrong/negative acceptance; stop immediately';break
        report['after']=safe();report['rtos_after']=request('/api/rtos')
        rt=report['rtos_after']
        if rt.get('watchdog') is not True or rt.get('deadline_misses')!=report['rtos_before'].get('deadline_misses') or rt.get('wifi_channel')!=1 or rt.get('reset_reason')!=report['rtos_before'].get('reset_reason'):raise RuntimeError('RTOS/watchdog/channel regression')
        if report['after']['boot_id']!=report['before']['boot_id'] or report['after']['sample_uptime_ms']<=report['before']['sample_uptime_ms']:raise RuntimeError('No advancing telemetry')
        report['status']='PASS' if len(report['results'])==4 and all(x.get('passed') for x in report['results']) else 'FAIL_STOP_NO_PHONE_COLLECTION'
    except Exception as e:
        report['status']='INCONCLUSIVE_STOP';report['error']=str(e)
    finally:
        if bench:bench.stack.close()
        report['serial_closed']=True;save()
    print(json.dumps(dict(status=report['status'],phone_clips=0,output=str(args.output))),flush=True)
    return 0 if report['status']=='PASS' else 1

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--node-a',required=True);p.add_argument('--node-b',required=True);p.add_argument('--identities',type=Path,required=True);p.add_argument('--output',type=Path,required=True)
    raise SystemExit(run(p.parse_args()))
