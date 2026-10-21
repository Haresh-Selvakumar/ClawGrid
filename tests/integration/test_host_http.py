"""Loopback integration of the shipped dashboard, assets and explicit host mode."""
import json
from pathlib import Path
import socket
import subprocess
import sys
import time
import urllib.request
import urllib.error
import pytest
from scripts.host_build import build

ROOT=Path(__file__).resolve().parents[2]

@pytest.fixture(scope='module')
def host_url():
    build()
    with socket.socket() as s:s.bind(('127.0.0.1',0));port=s.getsockname()[1]
    process=subprocess.Popen([sys.executable,'scripts/browser_bridge.py','--host-dev','--port',str(port)],cwd=ROOT,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
    base=f'http://127.0.0.1:{port}'
    try:
        for _ in range(100):
            try:
                with urllib.request.urlopen(base+'/simulation',timeout=.3):break
            except OSError:time.sleep(.05)
        else:raise RuntimeError('Host dashboard did not start')
        yield base
    finally:
        # Request a graceful shutdown using CTRL_BREAK is not portable for test
        # subprocess pipes. The adapter exits when the parent's stdin closes.
        process.terminate();process.wait(timeout=5)

def test_host_banner_and_client_content_policy(host_url):
    with urllib.request.urlopen(host_url+'/simulation') as response:
        html=response.read().decode()
        assert 'HOST-ONLY SIMULATION' in html and 'no physical measurements' in html
        assert '<script src="/api/client">' in html
        assert "script-src 'self'" in response.headers['Content-Security-Policy']
    with urllib.request.urlopen(host_url+'/api/client') as response:
        assert 'javascript' in response.headers['Content-Type']
        assert b'proposal_id' in response.read()

def test_host_auth_and_provenance(host_url):
    request=urllib.request.Request(host_url+'/api/proposal',data=b'manual physical_bedroom bedroom light ON 0')
    with pytest.raises(urllib.error.HTTPError) as error:urllib.request.urlopen(request)
    assert error.value.code==401
    request.add_header('X-ClawGrid-Token','clawgrid-dev-only')
    with urllib.request.urlopen(request) as response:value=json.load(response)
    assert value['processing_source']=='HOST_MEASURED' and value['input_source']=='SIMULATED'
    assert 'HARDWARE_MEASURED' not in json.dumps(value)

def test_host_never_recognizes_uploaded_pcm(host_url):
    request=urllib.request.Request(host_url+'/api/speech/audio',data=bytes(3200),headers={'X-ClawGrid-Token':'clawgrid-dev-only'})
    with pytest.raises(urllib.error.HTTPError) as error:urllib.request.urlopen(request)
    assert error.value.code==501
