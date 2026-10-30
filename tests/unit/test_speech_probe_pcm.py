"""Format validation for the actual recognition-only upload tool; no inference."""
from pathlib import Path
import struct
import sys
import wave
import pytest

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT))
from scripts.speech_pipeline_probe import read_pcm


@pytest.mark.parametrize('channels,width,rate',[(2,2,16000),(1,1,16000),(1,4,16000),(1,2,44100)])
def test_wrong_format_never_becomes_gateway_pcm(tmp_path,channels,width,rate):
    path=tmp_path/'wrong.wav'
    with wave.open(str(path),'wb') as wav:
        wav.setparams((channels,width,rate,0,'NONE','not compressed'));wav.writeframes(bytes(6400))
    with pytest.raises(ValueError):read_pcm(path)


def test_valid_pcm_preserves_signed_little_endian_bytes(tmp_path):
    path=tmp_path/'valid.wav';pcm=struct.pack('<hhhh',-32768,-1,0,32767)*400
    with wave.open(str(path),'wb') as wav:
        wav.setparams((1,2,16000,0,'NONE','not compressed'));wav.writeframes(pcm)
    assert read_pcm(path)==pcm
