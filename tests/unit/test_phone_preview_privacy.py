"""Synthetic metadata/pipe tests; no device, model inference or real transcript."""
import json
import sys
from pathlib import Path
from types import SimpleNamespace
import pytest

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'scripts'))
from inspect_phone_preview_memory import compare
import phone_ui_memory

def test_comparison_distinguishes_recognition_from_normalization_without_echoing_text():
    exact=['private sentinel']
    result=compare(exact,'private sentinel','light off')
    assert not result['raw_matches_prompt_after_case_whitespace_only']
    assert result['normalized_matches_raw_transformation']
    assert result['mismatch_positions']==[1,2]
    assert 'private' not in json.dumps(result) and 'sentinel' not in json.dumps(result)
    assert not compare(['LIGHT OFF'],'light on','light off')['normalized_matches_raw_transformation']
    assert compare(['  LIGHT\tOFF '],'light off','light off')['normalized_matches_raw_transformation']

def test_ui_reader_uses_stdout_pipe_only(monkeypatch):
    calls=[]
    def run(args,**kwargs):
        calls.append(args)
        return SimpleNamespace(returncode=0,stdout=b'<?xml version="1.0"?><hierarchy><node text="synthetic"/></hierarchy>\nUI hierarchy dumped to: /proc/self/fd/1')
    monkeypatch.setattr(phone_ui_memory.subprocess,'run',run)
    root=phone_ui_memory.read_ui('adb')
    assert root.tag=='hierarchy' and calls==[['adb','exec-out','uiautomator','dump','/proc/self/fd/1']]

def test_failed_pipe_never_falls_back_to_dump_file_or_prints_output(monkeypatch):
    calls=[]
    def run(args,**kwargs):
        calls.append(args);return SimpleNamespace(returncode=1,stdout=b'private sentinel')
    monkeypatch.setattr(phone_ui_memory.subprocess,'run',run)
    with pytest.raises(RuntimeError) as error:phone_ui_memory.read_ui('adb')
    assert 'private' not in str(error.value) and len(calls)==1
