"""Exercise the actual pinned adapter, including confidence boundary equivalence."""
import subprocess
import sys
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]


def test_probability_log_adapter_preserves_gate_and_rejects_wrong_abi(tmp_path):
    source=tmp_path/'threshold.cpp'; executable=tmp_path/'threshold.exe'
    source.write_text(r'''
#include "mn6_threshold.h"
#include <assert.h>
int main() {
  alignas(16) unsigned char model[280]={};
  auto vendor=[&](float p){ memcpy(model+kPinnedMn6ThresholdOffset,&p,4); };
  assert(mn6_probability_threshold(model,kSpeechMinimumConfidence,vendor));
  float threshold=0; memcpy(&threshold,model+kPinnedMn6ThresholdOffset,4);
  assert(fabsf(threshold-logf(0.7f))<0.000001f);
  assert(fabsf(expf(threshold)-0.7f)<0.000001f);
  for(float confidence: {0.1f,0.69f,0.7f,0.71f,0.99f}) {
    assert((logf(confidence)>threshold)==(confidence>0.7f));
  }
  bool called=false;
  auto wrong_abi=[&](float){ called=true; float invalid=-2; memcpy(model+224,&invalid,4); };
  assert(!mn6_probability_threshold(model,0.7f,wrong_abi) && called);
  called=false;
  assert(!mn6_probability_threshold(model,NAN,wrong_abi));
  assert(!mn6_probability_threshold(model,0.0f,wrong_abi));
  assert(!mn6_probability_threshold(model,1.0f,wrong_abi));
  assert(!mn6_probability_threshold(nullptr,0.7f,wrong_abi));
  assert(!called);
}
'''.replace('#include <assert.h>','#include <assert.h>\n#include <initializer_list>'),encoding='utf-8')
    subprocess.run([sys.executable,'-m','ziglang','c++','-std=c++17','-O2','-I'+str(ROOT/'firmware/common/runtime'),str(source),'-o',str(executable)],check=True,capture_output=True,timeout=120)
    subprocess.run([str(executable)],check=True,capture_output=True,timeout=5)
