"""Compile and execute the real multi-room registry, wire, filter and policy."""
import subprocess
import sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]

def test_room_gateway_actual_source(tmp_path):
    common=ROOT/'firmware/common'
    obj=tmp_path/'kernel.o'; exe=tmp_path/'rooms.exe'
    subprocess.run([sys.executable,'-m','ziglang','cc','-std=c11','-I'+str(common/'safety_kernel'),
                    '-c',str(common/'safety_kernel/safety_kernel.c'),'-o',str(obj)],check=True,capture_output=True)
    args=[sys.executable,'-m','ziglang','c++','-std=c++17','-Wall','-Wextra','-Werror','-O2']
    args += ['-I'+str(common/p) for p in ['runtime','safety_kernel','protocol']]
    build=subprocess.run(args+[str(ROOT/'tests/native/room_gateway_checks.cpp'),str(obj),'-o',str(exe)],capture_output=True,text=True)
    assert build.returncode==0,build.stderr
    run=subprocess.run([str(exe)],capture_output=True,text=True)
    assert run.returncode==0,run.stdout+run.stderr
    assert '"passed":true' in run.stdout
