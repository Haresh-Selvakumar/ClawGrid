"""Prime existing voice proof through four normal UI OFF proposals/confirmations."""
import json
from pathlib import Path
import re
import subprocess
import time
from phone_ui_memory import read_ui

ROOT=Path(__file__).resolve().parents[1]
ADB=Path.home()/'AppData/Local/Android/Sdk/platform-tools/adb.exe'
SHELL=[str(ADB),'-s','AWJDVB4705001033','shell']
OUT=ROOT/'benchmarks/review/three-board-acceptance-20261102/voice-off-ui-setup.json'

def nodes():
    return [n for n in read_ui(ADB).iter('node') if n.get('package')=='com.aistudio.clawgrid.v2edge']

def bounds(n):return list(map(int,re.findall(r'\d+',n.get('bounds'))))

def tap(n):
    b=bounds(n)
    subprocess.run(SHELL+['input','tap',str((b[0]+b[2])//2),str((b[1]+b[3])//2)],check=True,capture_output=True)

def move(down,amount=1200):
    y=2200 if down else 850
    subprocess.run(SHELL+['input','swipe','600',str(y),'600',str(y-amount if down else y+amount),'350'],check=True,capture_output=True)

def find_card(title):
    for _ in range(9):
        ns=nodes();n=next((n for n in ns if n.get('text')==title),None)
        if n is not None:
            y=bounds(n)[1]
            if y>800:move(True,min(1300,y-650));continue
            return ns,n
        move(True)
    # Context changes can move the previously selected card above the viewport.
    for _ in range(9):
        ns=nodes();n=next((n for n in ns if n.get('text')==title),None)
        if n is not None:return ns,n
        move(False)
    raise RuntimeError('Room card not visible: '+title)

v=dict(source='HARDWARE_MEASURED',ON_commands_sent=False,guard_bypassed=False,
       raw_audio_retained=False,UI_dump_saved=False,actions=[])
old=subprocess.run(SHELL+['settings','get','global','stay_on_while_plugged_in'],capture_output=True,text=True,check=True).stdout.strip()
assert old in [str(x) for x in range(8)]+['null']
try:
    subprocess.run(SHELL+['svc','power','stayon','usb'],capture_output=True,check=True)
    subprocess.run(SHELL+['input','keyevent','224'],capture_output=True,check=True)
    subprocess.run(SHELL+['am','start','-n','com.aistudio.clawgrid.v2edge/com.example.MainActivity'],capture_output=True,check=True)
    end=time.monotonic()+20
    while not nodes():
        if time.monotonic()>end:raise RuntimeError('Phone unlock required for normal UI confirmations')
        time.sleep(.5)
    hide=next((n for n in nodes() if n.get('text')=='Hide timing controls'),None)
    if hide is not None:tap(hide)
    for room,title,next_title in [('Room 1','Room 1 · Node A','Room 2 · Node C'),('Room 2','Room 2 · Node C','Kitchen')]:
        for index,device in enumerate(['light','fan']):
            ns,heading=find_card(title);top=bounds(heading)[3]
            bottom=min([bounds(n)[1] for n in ns if n.get('text')==next_title and bounds(n)[1]>top]+[2450])
            choices=sorted([n for n in ns if n.get('text')=='OFF' and top<bounds(n)[1]<bottom],key=lambda n:bounds(n)[1])
            if len(choices)<2:raise RuntimeError('Both manual OFF controls must be visible before action')
            tap(choices[index])
            expected=f'Confirm {room} {device} OFF'
            for _ in range(3):
                ns=nodes()
                if any(n.get('text')==expected for n in ns):break
            else:raise RuntimeError('Expected OFF proposal absent; no blind confirmation')
            confirm=next(n for n in ns if n.get('text')=='Confirm');tap(confirm)
            status=None
            for _ in range(4):
                ns=nodes()
                status=next((n.get('text') for n in ns if n.get('text','').startswith('Matching ACK + fresh board report') and f'{device} OFF' in n.get('text','') and 'REPORTED' in n.get('text','')),None)
                if status:break
                time.sleep(.25)
            if status is None:raise RuntimeError('Client manual ACK/readback proof not observed')
            v['actions'].append(dict(room=room,device=device,action='OFF',proposal_title=expected,client_reported=True,status=status))
            print(room+' '+device+' OFF proof recorded',flush=True)
    v['result']='PASS normal OFF confirmations, existing client voice guard preserved'
except Exception as failure:
    v['error']=str(failure)
finally:
    command=SHELL+(['settings','delete','global','stay_on_while_plugged_in'] if old=='null' else ['settings','put','global','stay_on_while_plugged_in',old])
    subprocess.run(command,capture_output=True,check=True)
    v['screen_setting_restored']=True
    OUT.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(v),flush=True)
