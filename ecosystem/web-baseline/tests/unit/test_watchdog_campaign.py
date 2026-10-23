import subprocess
import sys
from pathlib import Path
import pytest
from scripts.recovery_hil import classify_reset,policy_matches
from scripts.flash_native import validate_image_mode,validate_console_mode,build_environment

@pytest.mark.parametrize('uart,config,marker,valid',[
    (True,'CONFIG_ESP_CONSOLE_UART_DEFAULT=y',b'ClawGrid:console=uart0\0',True),
    (False,'CONFIG_ESP_CONSOLE_USB_SERIAL_JTAG=y',b'ClawGrid:console=usb_serial_jtag\0',True),
    (True,'CONFIG_ESP_CONSOLE_USB_SERIAL_JTAG=y',b'ClawGrid:console=uart0\0',False),
    (True,'CONFIG_ESP_CONSOLE_UART_DEFAULT=y',b'ClawGrid:console=usb_serial_jtag\0',False),
    (False,'CONFIG_ESP_CONSOLE_USB_SERIAL_JTAG=y',b'ClawGrid:console=uart0\0',False)])
def test_console_image_and_sdk_must_agree(uart,config,marker,valid):
    if valid:validate_console_mode(marker,config,uart)
    else:
        with pytest.raises(RuntimeError):validate_console_mode(marker,config,uart)

@pytest.mark.parametrize('image,diagnostic,valid',[
    (b'ClawGrid:outputs=inhibited;faults=default\0',False,True),
    (b'ClawGrid:outputs=inhibited;faults=diagnostic\0',True,True),
    (b'ClawGrid:outputs=inhibited;faults=diagnostic\0',False,False),
    (b'ClawGrid:outputs=enabled;faults=default\0',False,False),
    (b'no marker',False,False),
    (b'ClawGrid:outputs=inhibited;faults=default\0ClawGrid:outputs=inhibited;faults=diagnostic\0',False,False)])
def test_flasher_uses_actual_image_mode_and_rejects_ambiguity(image,diagnostic,valid):
    if valid:validate_image_mode(image,diagnostic)
    else:
        with pytest.raises(RuntimeError):validate_image_mode(image,diagnostic)

@pytest.mark.parametrize('image,diagnostic,bench,valid',[
    (b'ClawGrid:outputs=enabled;faults=default\0',False,True,True),
    (b'ClawGrid:outputs=inhibited;faults=default\0',False,True,False),
    (b'ClawGrid:outputs=enabled;faults=diagnostic\0',True,True,False),
    (b'ClawGrid:outputs=enabled;faults=default\0ClawGrid:outputs=inhibited;faults=default\0',False,True,False)])
def test_bench_flash_requires_exact_explicit_mode(image,diagnostic,bench,valid):
    if valid:validate_image_mode(image,diagnostic,bench)
    else:
        with pytest.raises(RuntimeError):validate_image_mode(image,diagnostic,bench)

@pytest.mark.parametrize('role,uart,diagnostic,bench,expected',[
    ('A',True,False,False,'native_voice_uart'),
    ('A',False,False,True,'native_voice_bench'),
    ('A',True,False,True,'native_voice_uart_bench'),
    ('A',True,True,False,'native_voice_uart_diagnostic'),
    ('B-speech',False,False,False,'speech_intelligence'),
    ('A',True,True,True,None),('B-speech',False,False,True,None),
    ('B-speech',True,False,False,None),('B',False,True,False,None)])
def test_flash_environment_rejects_unsafe_or_wrong_role_selection(role,uart,diagnostic,bench,expected):
    if expected:assert build_environment(role,uart,diagnostic,bench)==expected
    else:
        with pytest.raises(RuntimeError):build_environment(role,uart,diagnostic,bench)

@pytest.mark.parametrize('reason,text,before,after,expected',[
    (6,True,10,11,True),(3,True,10,11,False),(0,True,10,11,False),
    (6,False,10,11,False),(6,True,10,10,False),(6,True,10,12,False)])
def test_watchdog_requires_cause_panic_evidence_and_single_boot(reason,text,before,after,expected):
    assert classify_reset(reason,text,before,after)==expected

def test_policy_includes_automation_flags_and_missing_fields():
    saved=dict(grace_ms=120000,auto_flags=1,away=False)
    assert policy_matches(saved,{**saved,'boot':20})
    for key,value in [('grace_ms',45000),('auto_flags',3),('away',True)]:
        assert not policy_matches(saved,{**saved,key:value})
        assert not policy_matches(saved,{k:v for k,v in saved.items() if k!=key})
    assert not policy_matches(None,saved)

@pytest.mark.parametrize('outputs,faults,valid',[(0,0,True),(0,1,True),(1,0,True),(1,1,False)])
def test_preprocessor_forbids_faults_with_enabled_outputs(outputs,faults,valid):
    header=Path(__file__).resolve().parents[2]/'firmware/common/config/clawgrid_config.h'
    command=[sys.executable,'-m','ziglang','cc','-E','-P','-x','c','-include',str(header),f'-DCLAWGRID_PHYSICAL_OUTPUTS={outputs}',f'-DCLAWGRID_DIAGNOSTIC_FAULTS={faults}','-']
    result=subprocess.run(command,input='CLAWGRID_PHYSICAL_OUTPUTS CLAWGRID_DIAGNOSTIC_FAULTS\n',capture_output=True,text=True,timeout=60)
    assert (result.returncode==0)==valid
    if valid:assert result.stdout.strip()==f'{outputs} {faults}'
    else:assert 'Diagnostic fault injection requires physical outputs inhibited' in result.stderr
