import csv
import wave
import pytest
from scripts.audio_dataset import load_dataset,outcome,summarize

def dataset(tmp_path,**fields):
    with wave.open(str(tmp_path/'clip.wav'),'wb') as audio:
        audio.setnchannels(1);audio.setsampwidth(2);audio.setframerate(16000);audio.writeframes(bytes(3200))
    row=dict(file='clip.wav',expected_id='1',**fields)
    with (tmp_path/'manifest.csv').open('w',newline='') as stream:
        writer=csv.DictWriter(stream,fieldnames=row);writer.writeheader();writer.writerow(row)
    return tmp_path

def test_unknown_provenance_is_not_invented(tmp_path):
    clip=load_dataset(dataset(tmp_path))[0]
    assert clip['audio_source']=='UNKNOWN' and clip['split']=='unspecified'
    assert clip['speaker']=='' and clip['session']==''

def test_human_metadata_and_held_out_filter(tmp_path):
    clip=load_dataset(dataset(tmp_path,audio_source='HUMAN_RECORDING',speaker='speaker1',session='s2',conditions='quiet',split='held_out'),'held_out')[0]
    assert clip['audio_source']=='HUMAN_RECORDING' and clip['speaker']=='speaker1'
    with pytest.raises(ValueError):load_dataset(tmp_path,'development')

@pytest.mark.parametrize('field,value',[('audio_source','invented'),('split','training_maybe')])
def test_invalid_metadata_is_rejected(tmp_path,field,value):
    with pytest.raises(ValueError):load_dataset(dataset(tmp_path,**{field:value}))

def test_legacy_synthetic_manifest_is_explicitly_mapped():
    from pathlib import Path
    clips=load_dataset(Path(__file__).resolve().parents[2]/'benchmarks/data/synthetic_speech')
    assert len(clips)==16 and all(c['audio_source']=='SYNTHETIC' for c in clips)

def test_results_keep_errors_out_of_accuracy_denominator():
    rows=[dict(expected_id=e,outcome=outcome(e,r,error)) for e,r,error in [(1,1,None),(1,2,None),(1,0,None),(1,None,'engine_error'),(1,None,'transport_error'),(0,2,None),(0,0,None)]]
    summary=summarize(rows)
    assert summary['attempted']==7 and summary['positive_attempts']==5 and summary['positive_evaluable']==3
    assert summary['exact_command_accuracy']==pytest.approx(1/3)
    assert summary['negative_evaluable']==2 and summary['counts']['negative_detection']==1
    assert summary['per_command']['1']['evaluable']==3
