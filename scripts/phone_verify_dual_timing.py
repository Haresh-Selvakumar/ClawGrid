"""Verify both timing controls on-phone; authorize/cancel one OFF-only timer.

No ON actions, audio capture, screenshots, UI dump files or printed tokens.
"""
import json
from pathlib import Path
import re
import subprocess
import time
from phone_ui_memory import read_ui

ROOT=Path(__file__).resolve().parents[1]
ADB=Path.home()/'AppData/Local/Android/Sdk/platform-tools/adb.exe'
BASE=[str(ADB),'-s','AWJDVB4705001033','shell','input']
OUT=ROOT/'benchmarks/review/three-board-acceptance-20261102/dual-timing/phone-verification-final.json'

def nodes():
    result=[n for n in read_ui(ADB).iter('node') if n.get('package')=='com.aistudio.clawgrid.v2edge']
    if not result:raise RuntimeError('Unlock/open ClawGrid required')
    return result

def swipe(direction,delta=1300):
    y1,y2=(2200,2200-delta) if direction=='down' else (800,800+delta)
    subprocess.run(BASE+['swipe','1150',str(y1),'1150',str(y2),'300'],check=True,capture_output=True)

def match(label):return next((n for n in nodes() if n.get('password')!='true' and n.get('text')==label),None)

def find(label,direction=None,limit=1):
    for _ in range(limit):
        n=match(label)
        if n is not None:return n
        if direction:swipe(direction)
    raise RuntimeError('Expected timing control not visible: '+label)

def tap(n):
    b=list(map(int,re.findall(r'\d+',n.get('bounds'))))
    subprocess.run(BASE+['tap',str((b[0]+b[2])//2),str((b[1]+b[3])//2)],check=True,capture_output=True)

def summary():
    prefixes=('Observed continuous ON:','Continuous ON cap:','OFF countdown:')
    return [n.get('text') for n in nodes() if n.get('password')!='true' and n.get('text','').startswith(prefixes)]

receipt=dict(source='HARDWARE_MEASURED',ON_commands_sent=False,raw_audio_retained=False,
             token_printed=False,UI_dump_saved=False)
try:
    tap(find('Safety & Cloud'))
    field=None
    for _ in range(9):
        field=next((n for n in nodes() if n.get('password')=='true'),None)
        if field is not None:break
        swipe('down')
    if field is None:raise RuntimeError('Existing gateway token field unavailable')
    top=int(re.findall(r'\d+',field.get('bounds'))[1])
    if top>1500:
        swipe('down',min(1000,top-1000))
        field=next(n for n in nodes() if n.get('password')=='true')
    if not field.get('text'):
        token=json.loads((ROOT/'local_credentials.json').read_text())['api_token']
        if not re.fullmatch('[0-9a-fA-F]{16,128}',token):raise RuntimeError('Unsupported private token input format')
        tap(field);subprocess.run(BASE+['text',token],check=True,capture_output=True);del token
        subprocess.run(BASE+['keyevent','4'],check=True,capture_output=True)
    apply=match('Apply Gateway Connection')
    if apply is None:
        # Small steps avoid skipping the button above the audit section.
        for direction in ['down','up','up','down']:
            swipe(direction,500);apply=match('Apply Gateway Connection')
            if apply is not None:break
    if apply is None:raise RuntimeError('Apply connection button unavailable')
    tap(apply);print('Existing connection restored privately',flush=True)
    tap(find('Relays'))
    tap(find('Room 2 / Node C','up',14))
    open_controls=find('Open timing controls','down',10)
    tap(find('fan'))
    receipt['before']=summary()
    if not receipt['before'] or any('Unavailable' in x for x in receipt['before']):
        time.sleep(1);receipt['before']=summary()
    if len(receipt['before'])!=3 or any('Unavailable' in x for x in receipt['before']):raise RuntimeError('Fresh timing summary unavailable')
    tap(find('Open timing controls'))
    review=find('Review OFF countdown','down',3)
    # The board must still report all four OFF before authorizing an OFF-only timer.
    live=ROOT/'benchmarks/review/three-board-acceptance-20261102/dual-timing-final-check-live.jsonl'
    row=json.loads(live.read_text(encoding='utf-8').splitlines()[-1])
    if time.monotonic_ns()-row['host_monotonic_ns']>8_000_000_000:raise RuntimeError('Fresh guard readback required')
    if any(v['state'].get('light') is not False or v['state'].get('fan') is not False for v in row['rooms'].values()):
        raise RuntimeError('All outputs must be OFF before this non-energizing UI check')
    tap(review)
    text=[n.get('text','') for n in nodes() if n.get('password')!='true']
    title='Authorize Room 2 / Node C / fan / OFF countdown'
    if title not in text or not any(t.startswith('Start a fresh 1-minute OFF countdown') for t in text):
        raise RuntimeError('Wrong room/device/duration in countdown review')
    receipt['review_title']=title;receipt['review_opened_ns']=time.monotonic_ns()
    time.sleep(5) # Verify the review delay is not deducted from the new minute.
    tap(find('Authorize'));receipt['authorized_ns']=time.monotonic_ns()
    print('Authorized one OFF-only countdown with existing cap preserved',flush=True)
    for _ in range(4):
        labels=summary()
        if any(re.fullmatch(r'OFF countdown: \d+:\d{2} remaining',x) for x in labels):break
        time.sleep(.5)
    else:raise RuntimeError('Controller countdown readback not visible')
    receipt['armed_summary']=labels
    tap(find('Cancel OFF countdown','down',3));receipt['cancel_requested_ns']=time.monotonic_ns()
    for _ in range(4):
        labels=summary()
        if 'OFF countdown: Not armed' in labels:break
        time.sleep(.5)
    else:raise RuntimeError('Cancelled timer readback not visible')
    receipt['after_cancel']=labels
    receipt['result']='PASS visible independent cap/countdown, countdown authorize/readback/cancel; LED execution not exercised'
except Exception as failure:
    receipt['error']=str(failure)
    raise
finally:
    OUT.write_text(json.dumps(receipt,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(receipt),flush=True)
