"""Frozen experiment and fail-closed speech context; no hardware required."""
import json
import subprocess
import sys
from collections import Counter
from pathlib import Path
import pytest
from scripts.flash_native import build_environment

ROOT=Path(__file__).resolve().parents[2]

def test_frozen_screening_budget_and_mapping():
    m=json.loads((ROOT/'experiments/voice-short-v1.json').read_text())
    assert m['phrases']=={'1':'LIGHT ENABLE','2':'LIGHT DISABLE'}
    assert m['mapping']=={'1':'bedroom/light/ON','2':'bedroom/light/OFF'}
    assert m['threshold']==0.7 and m['frontend']['esp_afe'] is False
    assert len(m['trials'])==40 and [t['index'] for t in m['trials']]==list(range(1,41))
    assert Counter(t['expected_id'] for t in m['trials'])=={0:20,1:10,2:10}
    assert Counter(t['kind'] for t in m['trials'])=={'positive':20,'silence':5,'background':5,'noncommand':10}
    for session in (1,2):
        assert Counter(t['expected_id'] for t in m['trials'] if t['session']==session)=={0:10,1:5,2:5}

def test_experiment_flashing_requires_explicit_safe_environment():
    assert build_environment('B-speech',True,False,False,'short-v1')=='speech_intelligence_uart_short_v1'
    assert build_environment('B-speech',True)=='speech_intelligence_uart'
    for args in [('A',True,False,False),('B-speech',False,False,False),('B-speech',True,True,False),('B-speech',True,False,True)]:
        with pytest.raises(RuntimeError): build_environment(*args,voice_experiment='short-v1')

def test_binding_cannot_cross_room_boot_screening_or_expiry(tmp_path):
    source=tmp_path/'binding.cpp';exe=tmp_path/'binding.exe'
    source.write_text(r'''
#include "short_voice.h"
#include <assert.h>
int main() {
 SpeechBinding b; b.boot=16;b.capture=23;b.created=100;b.bedroom=true;
 CommandTarget t;
 auto ok=[&](uint32_t boot=16,bool fresh=true,uint32_t now=101,uint8_t cmd=1,float score=.7f,bool diag=false){return short_voice_target(b,boot,fresh,now,cmd,score,diag,t);};
 assert(ok());assert(t.mode==TargetMode::PhysicalBedroom && t.room==TargetRoom::Bedroom && t.device==TargetDevice::Light && t.action==TargetAction::On);
 assert(ok(16,true,101,2));assert(t.action==TargetAction::Off);
 assert(!ok(17));assert(!ok(16,false));assert(!ok(16,true,30100));
 assert(!ok(16,true,101,0));assert(!ok(16,true,101,3));assert(!ok(16,true,101,1,.699f));
 assert(!ok(16,true,101,1,NAN));assert(!ok(16,true,101,1,1.1f));assert(!ok(16,true,101,1,.9f,true));
 b.screen=true;assert(!ok());b.screen=false;b.bedroom=false;assert(!ok());
 b.bedroom=true;b.capture=0;assert(!ok());
 b.capture=23;b.created=0xfffffff0u;assert(ok(16,true,15));assert(!ok(16,true,30000));
}
''')
    include=[ROOT/'firmware/common'/p for p in ['runtime','protocol','safety_kernel']]
    subprocess.run([sys.executable,'-m','ziglang','c++','-std=c++17','-O2',*[f'-I{p}' for p in include],str(source),'-o',str(exe)],capture_output=True,check=True,timeout=120)
    subprocess.run([str(exe)],capture_output=True,check=True,timeout=5)
