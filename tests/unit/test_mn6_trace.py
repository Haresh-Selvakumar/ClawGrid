"""Verify bounded read-only decoder observations on a synthetic memory image."""
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]


def test_decoder_trace_bounds_and_score_units(tmp_path):
    source = tmp_path / 'trace.cpp'
    exe = tmp_path / 'trace.exe'
    source.write_text(r'''
#include "mn6_trace.h"
#include <assert.h>
#include <string.h>
#include <vector>
int main() {
  std::vector<unsigned char> memory(4096,0);
  auto put=[&](uint32_t p,const auto &v){memcpy(memory.data()+p-0x1000,&v,sizeof(v));};
  auto read=[&](uint32_t p,void *v,size_t n){
    if(p<0x1000 || n>memory.size() || (uint64_t)p+n>0x1000+memory.size())return false;
    memcpy(v,memory.data()+p-0x1000,n);return true;
  };
  uint32_t model=0x1000,active=0x1200,finished=0x1210,array=0x1300,path=0x1400,tokens=0x1500;
  put(model+224,logf(0.7f));put(model+92,active);put(model+96,finished);
  put(active,array);put(active+4,1);put(finished+4,0);put(array,path);
  put(path,tokens);put(path+8,2);put(path+12,logf(0.8f)*5);put(path+16,3);
  put(tokens,17u);put(tokens+4,23u);put(tokens+8,42u);
  auto original=memory; Mn6Trace trace;
  assert(mn6_read_trace(model,read,trace));assert(memory==original);
  assert(trace.count[0]==1 && trace.count[1]==0);
  assert(trace.paths[0][0].command==2 && trace.paths[0][0].tokens[2]==42);
  assert(fabsf(trace.paths[0][0].probability-0.8f)<0.00001f);
  put(path+12,logf(0.6f)*5);trace={};assert(mn6_read_trace(model,read,trace));
  assert(trace.paths[0][0].mean_log<trace.threshold);
  put(path+12,-INFINITY);trace={};assert(mn6_read_trace(model,read,trace));
  assert(!trace.paths[0][0].score_available);
  put(path+12,1.0f);trace={};assert(mn6_read_trace(model,read,trace));
  assert(trace.paths[0][0].score_available && trace.paths[0][0].probability>1.0f);
  put(path+16,129);trace={};assert(!mn6_read_trace(model,read,trace));
  put(path+16,3);put(array,0xfffffffcu);trace={};assert(!mn6_read_trace(model,read,trace));
  put(array,path);put(active+4,65);trace={};assert(!mn6_read_trace(model,read,trace));
  put(active+4,1);put(model+224,0.7f);trace={};assert(!mn6_read_trace(model,read,trace));
  assert(!mn6_read_trace(0,read,trace));
}
''', encoding='utf-8')
    subprocess.run([sys.executable, '-m', 'ziglang', 'c++', '-std=c++17', '-O2',
                    '-I' + str(ROOT / 'firmware/common/runtime'), str(source), '-o', str(exe)],
                   capture_output=True, check=True, timeout=120)
    subprocess.run([str(exe)], capture_output=True, check=True, timeout=5)
