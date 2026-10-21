"""Evaluate labeled PCM on ESP32 MultiNet. Host dataset checks perform no inference."""
import argparse
import csv
import hashlib
import json
import random
import struct
import sys
import time
import urllib.request
import urllib.error
import zlib
from datetime import datetime,timezone
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT))
from scripts.audio_dataset import load_dataset,outcome,summarize

class EngineError(RuntimeError):
    pass

def run(args):
    clips=load_dataset(args.dataset,args.split)
    if args.validate_only:
        description=[{k:v for k,v in row.items() if k!='pcm'}|{'samples':len(row['pcm'])//2} for row in clips]
        report=dict(source='HOST_MEASURED',operation='dataset validation only; no recognition',clips=description,human_evaluation='pending; no recognition executed')
        if args.output:
            args.output.parent.mkdir(parents=True,exist_ok=True);args.output.write_text(json.dumps(report,indent=2)+'\n')
        print(f'Dataset validated: {len(clips)} clips. No recognizer executed.');return 0
    from scripts.resume_validation import verified_flash_artifact
    targets=[('speech','speech_intelligence')] if args.speech_only else [('native','native_voice'),('speech','speech_intelligence')]
    for project,env in targets:
        if not verified_flash_artifact(ROOT/project/'.pio/build'/env/'firmware.bin',(ROOT/'benchmarks/raw/hardware').glob(f'flash_{env}_*.json')):
            raise RuntimeError(f'{env}: build differs from last successful flash; deploy before recording metrics')
    token=json.loads((ROOT/'local_credentials.json').read_text())['api_token']
    def request(path,body=None):
        req=urllib.request.Request(args.url+path,data=body,headers={'X-ClawGrid-Token':token,'Content-Type':'application/octet-stream'})
        try:
            with urllib.request.urlopen(req,timeout=20) as response:value=json.load(response)
        except urllib.error.HTTPError as error:
            if path=='/api/speech/audio' and error.code==409:raise EngineError('Speech engine busy/unavailable') from error
            raise
        if not isinstance(value,dict):raise ValueError('Expected an object response')
        return value
    if not args.no_generated_negatives and not args.split:
        rng=random.Random(20261020)
        for name,pcm in [('generated_silence',bytes(64000)),('generated_white_noise',b''.join(struct.pack('<h',rng.randint(-1000,1000)) for _ in range(32000)))]:
            clips.append(dict(name=name,expected_id=0,pcm=pcm,audio_source='SYNTHETIC',speaker='',session='',conditions='generated diagnostic fixture',split='diagnostic',phrase='',pcm_sha256=hashlib.sha256(pcm).hexdigest()))
    if args.limit is not None:
        if args.limit<1:raise ValueError('--limit must be positive')
        clips=clips[:args.limit]
    results=[]
    for clip in clips:
        pcm=clip['pcm'];row={k:v for k,v in clip.items() if k!='pcm'}
        row.update(processing_source='HARDWARE_MEASURED',samples=len(pcm)//2,recognized_id=None,board={},control=None)
        start=time.perf_counter_ns()
        try:
            engine=request('/api/speech')
            if engine.get('processing_source')!='HARDWARE_MEASURED':raise RuntimeError('Endpoint is not a target-hardware recognition engine')
            if not engine.get('supported') or engine.get('phase')==5:
                row['outcome']='engine_error';row['error']='Speech engine unavailable'
            else:
                if engine.get('phase') not in (1,4):raise EngineError('Speech engine busy')
                queued=request('/api/speech/audio',pcm)
                phase_before=request('/api/speech');row['phase_before_control']=phase_before.get('phase')
                command=None
                if not args.speech_only:
                    try:command=request('/api/control',b'1 0 0 0')
                    except (OSError,ValueError) as error:row['control_error']=type(error).__name__
                deadline=time.monotonic()+15;detected=None;control=None
                while time.monotonic()<deadline:
                    state=request('/api/speech')
                    transaction={}
                    if command:
                        try:transaction=request('/api/transaction')
                        except (OSError,ValueError) as error:row['control_error']=type(error).__name__
                    if command and transaction.get('id')==command.get('id') and transaction.get('phase')==2:control=transaction
                    if state.get('job')==queued.get('job') and state.get('phase') in (4,5):
                        detected=state
                        if args.speech_only or control or row.get('control_error'):break
                    time.sleep(.03)
                if not detected:raise TimeoutError('Speech result not received')
                row.update(board=detected,control=control,recognized_id=detected.get('command_id'))
                if detected.get('phase')==5 or detected.get('error_code',0) or detected.get('engine_errors',0):row['outcome']='engine_error'
                elif detected.get('pcm_crc32')!=zlib.crc32(pcm):raise RuntimeError('PCM checksum mismatch')
                elif type(row['recognized_id']) is not int or not 0<=row['recognized_id']<=16:
                    row['outcome']='engine_error';row['error']='Invalid recognizer command ID'
                else:row['outcome']=outcome(row['expected_id'],row['recognized_id'])
                if command and control is None:row['control_error']='Control ACK not received; not a recognition result'
        except EngineError as error:
            row['outcome']='engine_error';row['error']=str(error)
        except (OSError,ValueError,RuntimeError,TimeoutError) as error:
            row['outcome']='transport_error';row['error']=type(error).__name__
        finally:
            row['host_job_ms']=(time.perf_counter_ns()-start)/1e6
        results.append(row)
        print(row['name'],row['outcome'],flush=True)
    stamp=datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')
    path=args.output or ROOT/f'benchmarks/raw/hardware/audio_{stamp}.json';path.parent.mkdir(parents=True,exist_ok=True)
    artifacts={'B':'speech/.pio/build/speech_intelligence/firmware.bin','model':'speech/.pio/build/speech_intelligence/srmodels/srmodels.bin'}
    if not args.speech_only:artifacts['A']='native/.pio/build/native_voice/firmware.bin'
    hashes={name:hashlib.sha256((ROOT/file).read_bytes()).hexdigest() for name,file in artifacts.items()}
    data=dict(processing_source='HARDWARE_MEASURED',firmware_sha256=hashes,results=results,summary=summarize(results),limitations='Dataset provenance is supplied by the manifest, not attested by the API. No physical switching or accuracy generalization follows from synthetic clips.')
    path.write_text(json.dumps(data,indent=2)+'\n')
    fields=['name','audio_source','speaker','session','conditions','split','expected_id','recognized_id','outcome','samples','pcm_sha256','host_job_ms']
    with path.with_suffix('.csv').open('w',newline='',encoding='utf-8') as stream:
        writer=csv.DictWriter(stream,fieldnames=fields,extrasaction='ignore');writer.writeheader();writer.writerows(results)
    print(json.dumps(data['summary'],indent=2));print('Evidence:',path)
    return int(any(r['outcome'] not in ('correct_command','negative_rejection') or r.get('control_error') for r in results))

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--url',default='http://127.0.0.1:8766');p.add_argument('--dataset',type=Path,default=ROOT/'benchmarks/data/synthetic_speech')
    p.add_argument('--speech-only',action='store_true');p.add_argument('--limit',type=int)
    p.add_argument('--split',choices=['development','held_out','diagnostic','unspecified'])
    p.add_argument('--no-generated-negatives',action='store_true');p.add_argument('--validate-only',action='store_true');p.add_argument('--output',type=Path)
    raise SystemExit(run(p.parse_args()))
