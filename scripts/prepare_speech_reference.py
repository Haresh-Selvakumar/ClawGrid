"""Reproduce the pinned official ESP-SR test clip without changing models/keys."""
import hashlib
import json
import re
import urllib.request
import wave
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
COMMIT='07b0a4d0f6b5de755874d5405150ee0a8eec9865'
SOURCE='test_apps/esp-sr/main/samples/tell_me_a_joke.h'
BLOB='4930286028f73aa31af61af0f9e009237a1a64cb'
PCM_SHA='6277bddf1b8e5b44cf2c453b9763e013c2817f5011320d42128eb732d5ac4ac4'


def main():
    out=ROOT/'benchmarks/review/speech-pipeline-20261024'
    header=out/'reference'/SOURCE
    header.parent.mkdir(parents=True,exist_ok=True)
    data=header.read_bytes() if header.exists() else urllib.request.urlopen('https://raw.githubusercontent.com/espressif/esp-sr/'+COMMIT+'/'+SOURCE,timeout=30).read(700000)
    if hashlib.sha1(('blob '+str(len(data))+'\0').encode()+data).hexdigest()!=BLOB:raise RuntimeError('Official source Git blob mismatch')
    header.write_bytes(data)
    body=data.decode().split('{',1)[1].split('}',1)[0]
    pcm=bytes(int(x,16) for x in re.findall(r'0x([0-9a-fA-F]{2})',body))
    if len(pcm)!=89658 or hashlib.sha256(pcm).hexdigest()!=PCM_SHA:raise RuntimeError('Reference PCM fingerprint mismatch')
    target=out/'tell_me_a_joke.wav'
    with wave.open(str(target),'wb') as wav:
        wav.setparams((1,2,16000,0,'NONE','not compressed'));wav.writeframes(pcm)
    print(json.dumps({'source':'HOST_MEASURED','operation':'reference data preparation only; no recognition','upstream_commit':COMMIT,'source_git_blob':BLOB,'wav':str(target),'samples':len(pcm)//2,'pcm_sha256':PCM_SHA,'provenance':'Official upstream diagnostic sample; no claim of held-out human evaluation'},indent=2))


if __name__=='__main__':main()
