"""Portable dataset validation and honest denominators; no speech inference."""
import csv
import hashlib
from pathlib import Path
import wave

SOURCES={'SIMULATED':'SYNTHETIC','SYNTHETIC':'SYNTHETIC','HUMAN':'HUMAN_RECORDING','HUMAN_RECORDING':'HUMAN_RECORDING','UNKNOWN':'UNKNOWN'}
SPLITS={'development','held_out','diagnostic','unspecified'}

def load_dataset(directory,split=None):
    directory=Path(directory).resolve();clips=[];seen=set()
    with (directory/'manifest.csv').open(encoding='utf-8-sig',newline='') as stream:
        for row in csv.DictReader(stream):
            name=row.get('file','');path=(directory/name).resolve()
            if not name or not path.is_relative_to(directory) or path in seen:raise ValueError('Missing, duplicate or out-of-directory clip')
            seen.add(path)
            expected=int(row['expected_id'])
            if not 0<=expected<=16:raise ValueError('Expected ID must be 0 (negative) or 1..16')
            source=row.get('audio_source','UNKNOWN').strip().upper() or 'UNKNOWN'
            if source not in SOURCES:raise ValueError('Unrecognized audio_source')
            partition=row.get('split','unspecified').strip().lower() or 'unspecified'
            if partition not in SPLITS:raise ValueError('Invalid evaluation split')
            with wave.open(str(path),'rb') as audio:
                if (audio.getframerate(),audio.getnchannels(),audio.getsampwidth(),audio.getcomptype())!=(16000,1,2,'NONE'):raise ValueError('Use 16 kHz mono PCM16 WAV')
                frames=audio.getnframes();pcm=audio.readframes(frames)
            if len(pcm)!=frames*2 or not 3200<=len(pcm)<=128000:raise ValueError('Truncated WAV or clip outside 0.1–4 seconds')
            if split and partition!=split:continue
            clips.append(dict(name=name,expected_id=expected,audio_source=SOURCES[source],speaker=row.get('speaker',''),session=row.get('session',''),conditions=row.get('conditions',''),split=partition,phrase=row.get('phrase',''),pcm=pcm,pcm_sha256=hashlib.sha256(pcm).hexdigest()))
    if not clips:raise ValueError('No clips in selected dataset/split')
    return clips

def outcome(expected,recognized,error=None):
    if error:return error
    if expected==0:return 'negative_detection' if recognized else 'negative_rejection'
    if recognized==0:return 'no_detection'
    return 'correct_command' if recognized==expected else 'wrong_command'

def summarize(rows):
    categories=('correct_command','wrong_command','no_detection','negative_detection','negative_rejection','engine_error','transport_error')
    totals={k:sum(r['outcome']==k for r in rows) for k in categories}
    per_command={}
    for command in sorted({r['expected_id'] for r in rows}):
        group=[r for r in rows if r['expected_id']==command]
        counts={k:sum(r['outcome']==k for r in group) for k in categories}
        per_command[str(command)]=dict(attempted=len(group),evaluable=len(group)-counts['engine_error']-counts['transport_error'],**counts)
    positives=sum(r['expected_id']>0 for r in rows)
    eligible=sum(r['expected_id']>0 and r['outcome'] not in ('engine_error','transport_error') for r in rows)
    negatives=sum(r['expected_id']==0 for r in rows)
    groups={}
    for source in sorted({r.get('audio_source','UNKNOWN') for r in rows}):
        selected=[r for r in rows if r.get('audio_source','UNKNOWN')==source]
        denominator=sum(r['expected_id']>0 and r['outcome'] not in ('engine_error','transport_error') for r in selected)
        correct=sum(r['outcome']=='correct_command' for r in selected)
        groups[source]=dict(attempted=len(selected),positive_evaluable=denominator,exact_command_accuracy=correct/denominator if denominator else None)
    return dict(attempted=len(rows),positive_attempts=positives,positive_evaluable=eligible,exact_command_accuracy=totals['correct_command']/eligible if eligible else None,negative_attempts=negatives,negative_evaluable=totals['negative_detection']+totals['negative_rejection'],counts=totals,per_command=per_command,by_audio_source=groups)
