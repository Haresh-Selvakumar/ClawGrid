"""Synthetic API fixtures test evaluator handling, never ESP32 recognition."""
from argparse import Namespace
from contextlib import contextmanager
import csv
import io
import json
from pathlib import Path
import wave
import zlib
import pytest
from scripts import audio_bench,resume_validation

@pytest.mark.parametrize('recognized,engine_error,bad_crc,expected',[(1,0,False,'correct_command'),(2,0,False,'wrong_command'),(0,0,False,'no_detection'),(0,11,False,'engine_error'),(1,0,True,'transport_error')])
def test_evaluator_records_fixture_outcomes_without_target(tmp_path,monkeypatch,recognized,engine_error,bad_crc,expected):
    directory=tmp_path/'dataset';directory.mkdir();pcm=bytes(3200)
    with wave.open(str(directory/'fixture.wav'),'wb') as audio:
        audio.setnchannels(1);audio.setsampwidth(2);audio.setframerate(16000);audio.writeframes(pcm)
    (directory/'manifest.csv').write_text('file,expected_id,audio_source,split\nfixture.wav,1,SYNTHETIC,diagnostic\n')
    for name in ['speech/.pio/build/speech_intelligence/firmware.bin','speech/.pio/build/speech_intelligence/srmodels/srmodels.bin']:
        path=tmp_path/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(b'UNIT TEST FIXTURE - NOT FIRMWARE')
    (tmp_path/'local_credentials.json').write_text(json.dumps({'api_token':'test-fixture-only'}))
    monkeypatch.setattr(audio_bench,'ROOT',tmp_path)
    monkeypatch.setattr(resume_validation,'verified_flash_artifact',lambda *args:True)
    uploaded=False
    @contextmanager
    def fake_open(request,timeout):
        nonlocal uploaded
        if request.full_url.endswith('/api/speech/audio'):
            uploaded=True;value={'job':1}
        elif not uploaded:value={'processing_source':'HARDWARE_MEASURED','supported':True,'phase':1}
        else:value={'processing_source':'HARDWARE_MEASURED','phase':4,'job':1,'command_id':recognized,'pcm_crc32':zlib.crc32(pcm)+(1 if bad_crc else 0),'error_code':engine_error}
        yield io.BytesIO(json.dumps(value).encode())
    monkeypatch.setattr(audio_bench.urllib.request,'urlopen',fake_open)
    args=Namespace(dataset=directory,split=None,validate_only=False,speech_only=True,no_generated_negatives=True,limit=None,url='http://fixture.invalid',output=tmp_path/'test-fixture-result.json')
    code=audio_bench.run(args)
    data=json.loads(args.output.read_text())
    assert data['results'][0]['outcome']==expected
    assert data['results'][0]['audio_source']=='SYNTHETIC'
    assert data['summary']['attempted']==1
    assert code==(expected!='correct_command')
