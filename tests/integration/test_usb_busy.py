"""HOST-only transport checks: queued requests never reach the fake serial port."""
import json
from pathlib import Path
import socket
import subprocess
import sys
import time
import urllib.error
import urllib.request

import pytest

ROOT = Path(__file__).resolve().parents[2]


@pytest.fixture(scope='module')
def busy_bridge():
    with socket.socket() as listener:
        listener.bind(('127.0.0.1', 0))
        port = listener.getsockname()[1]
    source = '''
import sys, types
from contextlib import contextmanager
from scripts import serial_gate, browser_bridge
class Serial:
    def __init__(self, **kwargs): pass
    def open(self): pass
    def close(self): pass
    def write(self, data): raise AssertionError('Busy request reached serial')
sys.modules['serial'] = types.SimpleNamespace(Serial=Serial)
@contextmanager
def busy(self, urgent):
    raise serial_gate.SerialBusyError('HOST fixture: queue busy')
    yield
serial_gate.SerialGate.turn = busy
browser_bridge.run('192.168.4.1', int(sys.argv[1]), serial_port='HOST_FAKE')
'''
    child = subprocess.Popen([sys.executable, '-c', source, str(port)], cwd=ROOT,
                             stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    base = f'http://127.0.0.1:{port}'
    try:
        for _ in range(100):
            if child.poll() is not None:
                pytest.fail('Fixture exited: ' + child.communicate()[1])
            try:
                with urllib.request.urlopen(base + '/voice', timeout=.2):
                    break
            except OSError:
                time.sleep(.02)
        else:
            pytest.fail('Busy bridge did not start')
        yield base
    finally:
        child.terminate()
        output, error = child.communicate(timeout=5)
        assert 'Board transport failure' not in output
        assert 'Busy request reached serial' not in error


@pytest.mark.parametrize('path,body', [('/api/simulation', None),
                                     ('/api/state', None),
                                     ('/api/confirm', b'HOST fixture only')])
def test_congestion_is_reported_without_claiming_board_disconnected(busy_bridge, path, body):
    request = urllib.request.Request(busy_bridge + path, data=body)
    with pytest.raises(urllib.error.HTTPError) as failed:
        urllib.request.urlopen(request, timeout=2)
    assert failed.value.code == 429
    assert failed.value.headers['Retry-After'] == '1'
    data = json.load(failed.value)
    assert data['stage'] == 'queue'
    assert 'request not sent' in data['error']
    assert 'known' not in data


def test_missing_usb_at_startup_keeps_website_available_without_fake_data():
    with socket.socket() as listener:
        listener.bind(('127.0.0.1',0)); port=listener.getsockname()[1]
    source='''
import sys,types
from scripts import browser_bridge
class Serial:
    is_open=False
    def __init__(self,**kwargs):pass
    def open(self):raise OSError('HOST fixture: USB device unavailable')
    def close(self):pass
sys.modules['serial']=types.SimpleNamespace(Serial=Serial)
browser_bridge.run('192.168.4.1',int(sys.argv[1]),serial_port='HOST_FAKE')
'''
    child=subprocess.Popen([sys.executable,'-c',source,str(port)],cwd=ROOT,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
    base=f'http://127.0.0.1:{port}'
    try:
        for _ in range(100):
            try:
                with urllib.request.urlopen(base+'/voice',timeout=.2) as response:
                    assert response.status==200
                    break
            except OSError:time.sleep(.02)
        else:pytest.fail('Website failed to start without USB')
        with pytest.raises(urllib.error.HTTPError) as failed:
            urllib.request.urlopen(base+'/api/state',timeout=2)
        assert failed.value.code==502
        assert b'no simulated fallback' in failed.value.read()
        assert child.poll() is None
    finally:
        child.terminate();child.wait(timeout=5)
