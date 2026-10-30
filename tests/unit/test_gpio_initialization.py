"""Host call-order evidence only; no claim about electrical reset transients."""
import subprocess
import sys
from pathlib import Path
import pytest

ROOT=Path(__file__).resolve().parents[2]

@pytest.fixture(scope='module')
def gpio_checks(tmp_path_factory):
    path=tmp_path_factory.mktemp('gpio')/'checks.exe'
    fixture=ROOT/'tests/native/gpio_fixture'
    kernel=ROOT/'firmware/common/safety_kernel'
    command=[sys.executable,'-m','ziglang','cc','-std=c11','-O2','-DESP_PLATFORM',
        f'-I{fixture}',f'-I{kernel}',str(kernel/'safety_kernel.c'),str(fixture/'checks.c'),'-o',str(path)]
    result=subprocess.run(command,capture_output=True,text=True,timeout=180)
    assert result.returncode==0,result.stderr
    return path

@pytest.mark.parametrize('active_low',[0,1])
def test_kernel_gpio_off_before_output(gpio_checks,active_low):
    result=subprocess.run([str(gpio_checks),str(active_low)],capture_output=True,text=True,timeout=5)
    assert result.returncode==0,result.stdout+result.stderr


def test_actual_native_gpio_adapter(tmp_path):
    fixture=ROOT/'tests/native/native_gpio_fixture'
    kernel=ROOT/'firmware/common/safety_kernel'
    output=tmp_path/'native_gpio.exe'
    command=[sys.executable,'-m','ziglang','cc','-std=c11','-O2',
        '-DESP_PLATFORM','-DCLAWGRID_NATIVE_IDF=1',
        f'-I{fixture}',f'-I{ROOT/"native/main"}',f'-I{ROOT/"firmware/common/runtime"}',
        f'-I{ROOT/"firmware/common/config"}',f'-I{kernel}',
        str(kernel/'safety_kernel.c'),str(fixture/'checks.c'),'-o',str(output)]
    result=subprocess.run(command,capture_output=True,text=True,timeout=180)
    assert result.returncode==0,result.stderr
    result=subprocess.run([str(output)],capture_output=True,text=True,timeout=5)
    assert result.returncode==0,result.stdout+result.stderr
