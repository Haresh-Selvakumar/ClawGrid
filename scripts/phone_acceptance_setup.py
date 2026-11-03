"""Restore the existing memory-only connection; inspect/cancel Room2 rule review.

No actuator controls, authorization buttons, audio or transcript storage.
"""
import json
from pathlib import Path
import re
import subprocess
from phone_ui_memory import read_ui

ROOT = Path(__file__).resolve().parents[1]
ADB = Path.home() / 'AppData/Local/Android/Sdk/platform-tools/adb.exe'
BASE = [str(ADB), '-s', 'AWJDVB4705001033', 'shell', 'input']

def ui():
    root = read_ui(ADB)
    nodes = [n for n in root.iter('node') if n.get('package') == 'com.aistudio.clawgrid.v2edge']
    if not nodes:
        raise RuntimeError('ClawGrid not visible; unlock/open required')
    return nodes

def tap(node):
    b = list(map(int, re.findall(r'\d+', node.get('bounds'))))
    subprocess.run(BASE + ['tap', str((b[0]+b[2])//2), str((b[1]+b[3])//2)], check=True, capture_output=True)

def find_text(label, direction=None, limit=1):
    for attempt in range(limit):
        nodes = ui()
        match = next((n for n in nodes if n.get('password') != 'true' and n.get('text') == label), None)
        if match is not None:
            return match
        if direction:
            y1,y2 = (2200,700) if direction == 'down' else (800,2200)
            subprocess.run(BASE + ['swipe','1150',str(y1),'1150',str(y2),'350'],check=True,capture_output=True)
    raise RuntimeError('Expected UI control not visible: ' + label)

tap(find_text('Safety & Cloud'))
field = None
for attempt in range(9):
    field = next((n for n in ui() if n.get('password') == 'true'), None)
    if field is not None:
        break
    subprocess.run(BASE+['swipe','1150','2200','1150','700','350'],check=True,capture_output=True)
if field is None:
    raise RuntimeError('Gateway token field not visible; no unsafe fallback')
if not field.get('text'):
    # Existing token remains local and memory-only. Never print it or dump XML.
    token = json.loads((ROOT/'local_credentials.json').read_text())['api_token']
    if not re.fullmatch('[0-9a-fA-F]{16,128}', token):
        raise RuntimeError('Unsupported token format for private ADB input')
    tap(field)
    subprocess.run(BASE+['text',token],check=True,capture_output=True)
    del token
    subprocess.run(BASE+['keyevent','4'],check=True,capture_output=True)
tap(find_text('Apply Gateway Connection', 'down', 4))
print('Existing gateway connection applied privately; no actuator request',flush=True)
tap(find_text('Relays'))
tap(find_text('Room 2 / Node C','up',12))
print('Room2 selected',flush=True)
tap(find_text('Open controls and speech report','down',10))
tap(find_text('Review schedule','down',5))
nodes=ui()
title=next((n.get('text') for n in nodes if n.get('text','').startswith('Authorize ')),None)
if title != 'Authorize Room 2 / Node C / light rule':
    raise RuntimeError('Wrong room-bound review title: ' + str(title))
body=next((n.get('text') for n in nodes if n.get('text','').startswith('Arm OFF after ')),None)
if body is None:
    raise RuntimeError('Expected OFF review absent; no authorization sent')
tap(find_text('Cancel'))
receipt=dict(source='HARDWARE_MEASURED', title=title, expected_action='OFF',
    review_opened=True, review_cancelled=True, authorize_pressed=False,
    actuator_commands_sent=False, private_token_printed=False, UI_dump_saved=False,
    microphone_audio_retained=False)
path=ROOT/'benchmarks/review/three-board-acceptance-20261102/android-planning-review/phone-review-check.json'
path.write_text(json.dumps(receipt,indent=2)+'\n',encoding='utf-8')
print(json.dumps(receipt),flush=True)
