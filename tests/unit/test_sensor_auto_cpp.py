"""Actual source execution: priority, one-shot schedules, energy and wire boundaries."""
import subprocess
import sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def test_sensor_auto_actual_source(tmp_path):
    common=ROOT/'firmware/common'
    obj=tmp_path/'kernel.o'
    subprocess.run([sys.executable,'-m','ziglang','cc','-std=c11','-I'+str(common/'safety_kernel'),'-c',str(common/'safety_kernel/safety_kernel.c'),'-o',str(obj)],check=True,capture_output=True)
    executable=tmp_path/'sensor-auto.exe'
    command=[sys.executable,'-m','ziglang','c++','-std=c++17','-Wall','-Wextra','-Werror','-O2']
    command += ['-I'+str(common/p) for p in ['runtime','safety_kernel','protocol']]
    command += [str(ROOT/'tests/native/sensor_auto_checks.cpp'),str(obj),'-o',str(executable)]
    result=subprocess.run(command,capture_output=True,text=True)
    assert result.returncode==0,result.stderr
    run=subprocess.run([str(executable)],capture_output=True,text=True)
    assert run.returncode==0,run.stdout+run.stderr
    print(run.stdout)
