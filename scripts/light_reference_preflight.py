"""Only the approved four frozen LIGHT ON/OFF fixtures on ESP; never proposes/confirms control.

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

FROZEN_PLAN_SHA256='ccf13855ed05da117b3d4d8ecbe92cb89007db454bd9ec92b0aef000b4fe9104'

def run(args):
    if args.output.exists(): raise RuntimeError('Preserve existing preflight evidence; no automatic rerun')
    plan=(ROOT/'experiments/light-on-off-reference-preflight-v1.json').read_bytes()
    if hashlib.sha256(plan).hexdigest()!=FROZEN_PLAN_SHA256: raise RuntimeError('Frozen plan changed')
    manifest=json.loads(plan)
    if args.room!='bedroom' or manifest['selected_room']!=args.room:raise RuntimeError('Explicit approved Bedroom required')
    cfg=json.loads((ROOT/'local_credentials.json').read_text())
    identities=json.loads(args.identities.read_text())['observations']
    for role,port,key in [('A',args.node_a,'node_a_mac'),('B',args.node_b,'node_b_mac')]:
        if not any(x.get('role')==role and x.get('port')==port and x.get('mac')==cfg[key].lower() and x.get('identity')=='ROM_MAC_MATCH' for x in identities):
            raise RuntimeError('Fresh ROM identity record required for '+role)
    report=dict(experiment='light-on-off-reference-preflight-v1',plan_sha256=FROZEN_PLAN_SHA256,source='HARDWARE_MEASURED',audio_source='SIMULATED',started_utc=datetime.now(timezone.utc).isoformat(),maximum_jobs=4,phone_clips=0,results=[],status='RUNNING',control_issued=False,proposals_issued=False,selected_room=args.room,not_run=[])
    opener=urllib.request.build_opener(urllib.request.ProxyHandler({}));bench=None
    def save():
        args.output.parent.mkdir(parents=True,exist_ok=True)
        args.output.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    upload_context={}
    def request(path,pcm=None):
        allowed=('/api/state','/api/rtos','/api/memory','/api/transaction','/api/speech')
        if pcm is None and path not in allowed:raise RuntimeError('Read path not allowed')
        if pcm is not None and path!='/api/speech/reference?feed=light-v1':raise RuntimeError('No control/proposal POST allowed')
        req=urllib.request.Request('http://192.168.4.1'+path,data=pcm,headers={'X-ClawGrid-Token':cfg['api_token'],'Content-Type':'application/octet-stream','Connection':'close',**(upload_context if pcm is not None else {})})
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
        report['before']=safe();report['rtos_before']=request('/api/rtos');report['health_before']=health;report['transaction_before']=request('/api/transaction')
        ready=request('/api/speech')
        if ready.get('grammar_id')!='light-preflight-v1' or ready.get('model_name')!='mn6_en' or ready.get('phase') not in (1,4): raise RuntimeError('Frozen light reference model not ready')
        if ready.get('job')!=0 or ready.get('command_count')!=2:raise RuntimeError('Fresh four-job boot required; no reset/retry')
        for index,fixture in enumerate(manifest['fixtures'],1):
            with wave.open(str(ROOT/fixture['path']),'rb') as w:
                if (w.getframerate(),w.getnchannels(),w.getsampwidth())!=(16000,1,2): raise RuntimeError('Fixture PCM format changed')
                pcm=w.readframes(w.getnframes())
            if (len(pcm)//2,zlib.crc32(pcm),hashlib.sha256(pcm).hexdigest())!=(fixture['samples'],fixture['crc32'],fixture['sha256']): raise RuntimeError('Frozen fixture changed')
            before=safe()
            upload_context.update({'X-ClawGrid-Room':args.room,'X-ClawGrid-Boot':str(before['boot_id']),
                'X-ClawGrid-Capture':str(index),'X-ClawGrid-Intent':'screen','X-ClawGrid-Grammar':'light-preflight-v1'})
            row=dict(fixture=fixture,index=index,before=before,upload_attempted=True)
            report['results'].append(row);save()
            queued=request('/api/speech/reference?feed=light-v1',pcm)
            if queued.get('queued') is not True or queued.get('authorized') is not False: raise RuntimeError('Audio not queued safely')
            row['job']=queued['job'];save()
            if queued['job']!=index:raise RuntimeError('Wrong job/order; no replay')
            deadline=time.monotonic()+15
            while time.monotonic()<deadline:
                result=request('/api/speech');after=safe()
                if result.get('job')!=queued['job'] or after['boot_id']!=before['boot_id']: raise RuntimeError('Job replaced or controller reboot')
                if result.get('phase') in (4,5):break
                bench.poll(.15)
            else:raise RuntimeError('Inference timeout; no retry')
            row['board']=result;row['after']=after;save()
            if result.get('bound_room')!=args.room or result.get('bound_boot')!=before['boot_id'] or result.get('capture_id')!=index or result.get('screen_only') is not True:raise RuntimeError('Reference context mismatch')
            if result.get('samples')!=fixture['samples'] or result.get('pcm_crc32')!=fixture['crc32']:raise RuntimeError('PCM mismatch')
            if result.get('command_id')!=0 or result.get('authorized') is not False or result.get('diagnostic_only') is not True:raise RuntimeError('Reference crossed command boundary')
            if after['last_command']!=before['last_command'] or result.get('error_code')!=0 or result.get('engine_errors')!=0:raise RuntimeError('Control change or speech engine error')
            accepted=result.get('reference_detected') is True
            expected=fixture['expected_id']
            row['passed']=(not accepted and result.get('candidate_id')==0 and result.get('raw_result_count')==0 and result.get('last_state')==2) if expected==0 else (accepted and result.get('candidate_id')==expected and result.get('last_state')==1 and 0.7<=result.get('candidate_score',0)<=1)
            print(json.dumps(dict(fixture=fixture['name'],job=queued['job'],accepted=accepted,candidate_id=result.get('candidate_id'),score=result.get('candidate_score'),passed=row['passed'])),flush=True);save()
            if not row['passed']:
                report['stop_reason']='First failed frozen clip; stop immediately, no replacement/retry';break
        report['after']=safe();report['rtos_after']=request('/api/rtos');report['transaction_after']=request('/api/transaction');report['health_after']=bench.request(args.node_a,b'q','task_health')
        if report['transaction_after'].get('id')!=report['transaction_before'].get('id'):raise RuntimeError('Unexpected command transaction')
        rt=report['rtos_after']
        if rt.get('watchdog') is not True or rt.get('deadline_misses')!=report['rtos_before'].get('deadline_misses') or rt.get('wifi_channel')!=1 or rt.get('reset_reason')!=report['rtos_before'].get('reset_reason'):raise RuntimeError('RTOS/watchdog/channel regression')
        if report['after']['boot_id']!=report['before']['boot_id'] or report['after']['sample_uptime_ms']<=report['before']['sample_uptime_ms']:raise RuntimeError('No advancing telemetry')
        report['status']='PASS' if len(report['results'])==4 and all(x.get('passed') for x in report['results']) else 'FAIL_STOP_NO_PHONE_COLLECTION'
    except Exception as e:
        report['status']='INCONCLUSIVE_STOP';report['error']=str(e)
    finally:
        if bench:bench.stack.close()
        report['serial_closed']=True
        report['not_run']=[x['name'] for x in manifest['fixtures'][len(report['results']):]]
        save()
    print(json.dumps(dict(status=report['status'],phone_clips=0,output=str(args.output))),flush=True)
    return 0 if report['status']=='PASS' else 1

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--room',choices=['bedroom'],required=True);p.add_argument('--node-a',required=True);p.add_argument('--node-b',required=True);p.add_argument('--identities',type=Path,required=True);p.add_argument('--output',type=Path,required=True)
    raise SystemExit(run(p.parse_args()))
