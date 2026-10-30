"""Recognition-only ESP probe: no proposal, confirmation or actuator commands."""
import argparse
import hashlib
import json
import sys
import time
import wave
import zlib
import urllib.request
import urllib.error
import urllib.parse
from datetime import datetime, timezone
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT))
from scripts.research_hil import Bench


def read_pcm(path):
    with wave.open(str(path),'rb') as wav:
        if (wav.getnchannels(),wav.getsampwidth(),wav.getframerate(),wav.getcomptype())!=(1,2,16000,'NONE'):
            raise ValueError('Require uncompressed mono signed PCM16LE WAV at16000Hz')
        pcm=wav.readframes(wav.getnframes())
    if not 3200<=len(pcm)<=128000 or len(pcm)%2: raise ValueError('PCM byte bounds')
    return pcm


def run(args):
    cfg=json.loads((ROOT/'local_credentials.json').read_text())
    identities=json.loads(args.identities.read_text())['observations']
    for role,port,key in [('A',args.node_a,'node_a_mac'),('B',args.node_b,'node_b_mac')]:
        if not any(r.get('role')==role and r.get('port')==port and r.get('mac')==cfg[key].lower() and r.get('identity')=='ROM_MAC_MATCH' for r in identities):
            raise RuntimeError('Fresh externally verified ROM identity record required for '+role)
    pcm=read_pcm(args.wav)
    if not args.url and len(pcm)>48000:
        raise ValueError('Use actual gateway Wi-Fi --url for this clip;115200-baud UART cannot deliver it within the HTTP upload deadline')
    synthetic=args.reference and args.feed==['synthetic']
    phone_reference=args.reference and args.feed in (['phone-afe'],['phone-bedroom'],['phone-trace'],['phone-split'])
    fixture_feed=args.feed[0].replace('-split','-trace') if args.reference and len(args.feed)==1 else ''
    traced_fixtures={
        'zira-trace':(69760,2182999710,'2a810b173e3ddad78f0febfac683a1b05499165badc17eed13fa968e4c4983ae'),
        'silence-trace':(64000,2109903275,'4f7988030a00d082fe445e00a2ac5dab502300ff1b80e8592dd569867b60ef74'),
        'noncommand-trace':(67520,3492560341,'d61bddb3b96a677b3b08a1279c9888bdc56b705d4e2a635cc627e2b37c5fc45d')}
    if args.reference:
        expected_crc=2905759910 if synthetic else 330285049
        expected_sha='ac05fd695174e1919fd2a8fa0e7f8c6cdbf02a5fbbed518130a049f4c6ff9035' if synthetic else '6277bddf1b8e5b44cf2c453b9763e013c2817f5011320d42128eb732d5ac4ac4'
        expected_bytes=89658
        if phone_reference:
            expected_bytes=64000;expected_crc=679739021
            expected_sha='78d4ceff766e0fa2f6cdc068447e018b2f7866a854b1bf3bc9508f0b9e03d1fd'
        if fixture_feed in traced_fixtures:
            expected_bytes,expected_crc,expected_sha=traced_fixtures[fixture_feed]
        if len(pcm)!=expected_bytes or zlib.crc32(pcm)!=expected_crc or hashlib.sha256(pcm).hexdigest()!=expected_sha:
            raise ValueError('Reference must match the selected fixed diagnostic clip; no transcript substitution')
    report={'source':'HARDWARE_MEASURED','started_utc':datetime.now(timezone.utc).isoformat(),'scope':'recognition only; no control/proposal/confirmation','pcm_source':'LABELLED_SYNTHETIC_REFERENCE' if synthetic else ('PINNED_UPSTREAM_REFERENCE' if args.reference else 'LABELLED_SYNTHETIC_DIAGNOSTIC'),'wav':str(args.wav),'samples':len(pcm)//2,'pcm_crc32':zlib.crc32(pcm),'pcm_sha256':hashlib.sha256(pcm).hexdigest(),'ports':{'A':args.node_a,'B':args.node_b},'results':[],'errors':[]}
    if phone_reference or getattr(args,'pcm_source',None)=='phone-capture': report['pcm_source']='ACTUAL_PHONE_CAPTURE_REPLAY'
    elif args.reference and fixture_feed in traced_fixtures: report['pcm_source']='LABELLED_SYNTHETIC_DIAGNOSTIC'
    bench=Bench([args.node_a,args.node_b],native=True,simulation_only=True,speech=True)
    opener=urllib.request.build_opener(urllib.request.ProxyHandler({}))
    if args.url and args.url!='http://192.168.4.1': raise ValueError('This AP probe requires the verified gateway http://192.168.4.1')
    def request(method,path,body=b''):
        if not args.url: return bench.http(method,path,body,timeout=8)
        req=urllib.request.Request(args.url+path,data=body if method=='POST' else None,
            headers={'X-ClawGrid-Token':cfg['api_token'],'Content-Type':'application/octet-stream','Connection':'close'})
        try:
            with opener.open(req,timeout=5) as response:
                return {'status':response.status,'body':json.loads(response.read(16384))}
        except urllib.error.HTTPError as exc:
            return {'status':exc.code,'body':json.loads(exc.read(16384))}
    def get(path):
        response=request('GET',path)
        if not response or response.get('status')!=200: raise RuntimeError('GET unavailable: '+path)
        return response['body']
    def safe():
        state=get('/api/state')
        if state.get('known') is not True or state.get('authenticated') is not True or state.get('age_ms',99999)>=3000 or state.get('physical_outputs_enabled') is not False:
            raise RuntimeError('STOP: fresh authenticated inhibited controller required')
        return state
    error=None
    try:
        bench.poll(.5)
        health=bench.request(args.node_a,b'q','task_health')
        if not health or health.get('physical_outputs_enabled') is not False or health.get('diagnostic_faults_enabled') is not False:
            raise RuntimeError('STOP: A health/inhibition not verified')
        report['before']=safe();report['rtos_before']=get('/api/rtos');report['model_before']=get('/api/speech')
        if report['model_before'].get('supported') is not True or report['model_before'].get('phase') not in (1,4): raise RuntimeError('Model unavailable/busy')
        for feed in (args.feed if args.reference else ['application']):
            before=safe()
            path='/api/speech/reference?feed='+feed if args.reference else '/api/speech/audio'
            queued=request('POST',path,pcm)
            row={'feed':feed,'post_path':path,'acceptance':queued}
            report['results'].append(row)
            if not queued or queued.get('status')!=202 or queued.get('body',{}).get('authorized') is not False:
                raise RuntimeError('Audio rejected; no automatic retry')
            job=queued['body']['job'];deadline=time.monotonic()+15
            while time.monotonic()<deadline:
                snapshot=get('/api/speech')
                if snapshot.get('job')!=job:raise RuntimeError('Speech job replaced/rebooted; stop')
                state=safe()
                if state['boot_id']!=before['boot_id']:raise RuntimeError('Controller rebooted; stop')
                if snapshot.get('phase') in (4,5):break
                bench.poll(.1)
            else:raise TimeoutError('No speech completion; no retry')
            row['board']=snapshot;row['after']=state
            if snapshot.get('pcm_crc32')!=zlib.crc32(pcm) or snapshot.get('samples')!=len(pcm)//2:raise RuntimeError('PCM delivery mismatch')
            if state['last_command']!=before['last_command'] or state['light']!=before['light'] or state['fan']!=before['fan']:raise RuntimeError('STOP: unexpected control change during recognition-only test')
            if args.reference:
                if snapshot.get('diagnostic_only') is not True or snapshot.get('command_id')!=0:raise RuntimeError('STOP: reference result crossed command boundary')
                row['outcome']='reference_detected' if snapshot.get('reference_detected') is True else 'no_detection'
                if getattr(args,'expect_no_command',False):
                    row['outcome']='unexpected_command' if snapshot.get('reference_detected') is True else 'no_command'
                elif (phone_reference or fixture_feed=='zira-trace') and snapshot.get('reference_detected') is True and snapshot.get('candidate_id')!=args.expected_id:
                    row['outcome']='wrong_command'
            elif getattr(args,'expect_no_command',False):
                row['outcome']='no_command' if snapshot.get('command_id')==0 and snapshot.get('confidence')==0 else 'unexpected_command'
            else:row['outcome']='correct_command' if snapshot.get('command_id')==args.expected_id and snapshot.get('confidence',0)>=.7 else ('no_detection' if snapshot.get('command_id')==0 else 'wrong_command')
            if snapshot.get('phase')==5 or snapshot.get('error_code') or snapshot.get('engine_errors'):row['outcome']='engine_error'
            print(json.dumps({'feed':feed,'job':job,'outcome':row['outcome'],'board':snapshot}),flush=True)
        report['after']=safe();report['rtos_after']=get('/api/rtos')
        report['health_after']=bench.request(args.node_a,b'q','task_health')
    except Exception as exc:
        error=type(exc).__name__+': '+str(exc);report['errors'].append(error);print(error,flush=True)
    finally:
        # Preserve reset/panic output queued while a failed Wi-Fi call was blocking.
        try: bench.poll(.5)
        except Exception as exc:
            report['errors'].append('Serial capture cleanup: '+type(exc).__name__+': '+str(exc))
            error=error or str(exc)
        finally:
            report['records']=bench.records;bench.stack.close()
        args.output.parent.mkdir(parents=True,exist_ok=True)
        args.output.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    return 1 if error or any(r.get('outcome') not in ('reference_detected','correct_command','no_command') for r in report['results']) else 0


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--node-a',required=True);parser.add_argument('--node-b',required=True)
    parser.add_argument('--url',help='Actual reachable gateway AP HTTP address; required for large PCM clips')
    parser.add_argument('--identities',type=Path,required=True);parser.add_argument('--wav',type=Path,required=True)
    parser.add_argument('--reference',action='store_true');parser.add_argument('--feed',nargs='+',choices=['legacy','window','reset-first','fresh-window','official','pinned','afe','trace','direct','sdk-heap','psram','gain15','synthetic','phone-afe','phone-bedroom','phone-trace','zira-trace','silence-trace','noncommand-trace','phone-split','zira-split','silence-split','noncommand-split'],default=['legacy','window','window'])
    parser.add_argument('--pcm-source',choices=['synthetic','phone-capture'],default='synthetic')
    parser.add_argument('--expected-id',type=int,default=2);parser.add_argument('--expect-no-command',action='store_true');parser.add_argument('--output',type=Path,required=True)
    raise SystemExit(run(parser.parse_args()))
