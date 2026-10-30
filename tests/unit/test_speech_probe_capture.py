"""Preserve real failure evidence even with malformed UART bytes or cleanup errors."""
import json
from pathlib import Path
from types import SimpleNamespace

from scripts import research_hil, speech_pipeline_probe


def test_uart_replacement_character_is_preserved_as_utf8(monkeypatch, tmp_path):
    class Serial:
        def __init__(self, **_): self.data=b''; self.closed=False
        def open(self): pass
        def close(self): self.closed=True
        @property
        def in_waiting(self): return len(self.data)
        def read(self, count):
            result=self.data[:count]; self.data=self.data[count:]; return result

    monkeypatch.setattr(research_hil, 'ROOT', tmp_path)
    monkeypatch.setattr(research_hil.serial, 'Serial', Serial)
    bench=research_hil.Bench(['A','B'])
    bench.connections['B'].data=b'\xffGuru Meditation\n{"kind":"boot","reset_reason":4}\n'
    try:
        bench.poll(.02)
        assert bench.records[0]['device']['reset_reason']==4
    finally: bench.stack.close()
    log=next((tmp_path/'benchmarks/raw/hardware').glob('*.log')).read_text(encoding='utf-8')
    assert '\ufffdGuru Meditation' in log
    assert all(port.closed for port in bench.connections.values())


def test_probe_saves_primary_and_cleanup_failures_and_closes_ports(monkeypatch, tmp_path):
    (tmp_path/'local_credentials.json').write_text(json.dumps({'node_a_mac':'a','node_b_mac':'b','api_token':'test-only'}))
    identities=tmp_path/'identities.json'
    identities.write_text(json.dumps({'observations':[{'role':role,'port':role,'mac':role.lower(),'identity':'ROM_MAC_MATCH'} for role in ('A','B')]}))

    class Bench:
        instance=None
        def __init__(self, *_args, **_kwargs):
            Bench.instance=self; self.polls=0; self.records=[]; self.closed=False
            self.stack=SimpleNamespace(close=self.close)
        def close(self): self.closed=True
        def poll(self, _seconds):
            self.polls+=1
            if self.polls>1: raise RuntimeError('capture cleanup failed')
        def request(self, *_args):
            return {'physical_outputs_enabled':False,'diagnostic_faults_enabled':False}
        def http(self, method, path, *_args, **_kwargs):
            assert method=='GET'
            if path=='/api/speech': raise RuntimeError('primary speech query failed')
            return {'status':200,'body':{'known':True,'authenticated':True,'age_ms':0,'physical_outputs_enabled':False}}

    monkeypatch.setattr(speech_pipeline_probe,'ROOT',tmp_path)
    monkeypatch.setattr(speech_pipeline_probe,'Bench',Bench)
    monkeypatch.setattr(speech_pipeline_probe,'read_pcm',lambda _:bytes(3200))
    args=SimpleNamespace(identities=identities,node_a='A',node_b='B',wav=Path('unused.wav'),url=None,reference=False,output=tmp_path/'result.json')
    assert speech_pipeline_probe.run(args)==1
    report=json.loads(args.output.read_text(encoding='utf-8'))
    assert any('primary speech query failed' in error for error in report['errors'])
    assert any('capture cleanup failed' in error for error in report['errors'])
    assert Bench.instance.closed
