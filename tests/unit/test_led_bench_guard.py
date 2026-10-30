import pytest
from scripts.flash_native import build_environment, validate_led_profile

@pytest.mark.parametrize('role,uart,diagnostic,bench,experiment',[
    ('B-speech',True,False,True,None),('A',False,False,True,None),
    ('A',True,False,False,None),('A',True,True,True,None),('A',True,False,True,'short-v1')])
def test_led_selector_rejects_unsafe_combinations(role,uart,diagnostic,bench,experiment):
    with pytest.raises(RuntimeError):build_environment(role,uart,diagnostic,bench,experiment,True)

def test_led_selector_and_image_must_explicitly_match():
    assert build_environment('A',True,False,True,None,True)=='native_voice_uart_led_bench'
    assert build_environment('A',True,False,True)=='native_voice_uart_bench'
    led=b'ClawGrid:load=led_indicators_10k_active_low\0'
    validate_led_profile(led,True);validate_led_profile(b'ClawGrid:load=generic\0',False)
    with pytest.raises(RuntimeError):validate_led_profile(led,False)
    with pytest.raises(RuntimeError):validate_led_profile(b'ClawGrid:load=generic\0',True)

def test_active_high_requires_its_own_explicit_selector_and_marker():
    assert build_environment('A',True,False,True,None,True,True)=='native_voice_uart_led_high_bench'
    with pytest.raises(RuntimeError):build_environment('A',True,False,True,None,False,True)
    with pytest.raises(RuntimeError):build_environment('B-speech',True,False,True,None,True,True)
    high=b'ClawGrid:load=led_indicators_10k_active_high\0'
    low=b'ClawGrid:load=led_indicators_10k_active_low\0'
    validate_led_profile(high,True,True)
    with pytest.raises(RuntimeError):validate_led_profile(high,True)
    with pytest.raises(RuntimeError):validate_led_profile(low,True,True)
    with pytest.raises(RuntimeError):validate_led_profile(high,False)
    with pytest.raises(RuntimeError):validate_led_profile(low+high,True,True)
