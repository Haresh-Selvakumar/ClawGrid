"""Capture this debug app's version/resources and allowlisted UI outcomes, never transcripts/tokens."""
import argparse
import hashlib
import json
import re
import subprocess
import xml.etree.ElementTree as ET
from phone_ui_memory import read_ui
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ADB = Path.home()/'AppData/Local/Android/Sdk/platform-tools/adb.exe'
PACKAGE = 'com.aistudio.clawgrid.v2edge'

def run(*args):
    return subprocess.run([str(ADB), *args], capture_output=True, check=True, timeout=25).stdout.decode(errors='replace')

def main(args):
    assert re.fullmatch('[a-z0-9_-]+', args.name)
    destination=ROOT/'benchmarks/review/phone-offline-v1-20261025'/(args.name+'-phone.json')
    if destination.exists(): raise RuntimeError('Evidence already exists')
    version=run('shell','dumpsys','package',PACKAGE)
    memory=run('shell','dumpsys','meminfo',PACKAGE)
    wifi=run('shell','cmd','wifi','status')
    # Pipe only: even a temporary on-device XML could persist a visible transcript.
    root=read_ui(ADB)
    allowed=[]
    prefixes=('Offline on-phone recognition','App 1.0-phone-offline-', 'Offline phone model ready.',
        'Offline model preparing or unavailable;', 'Offline model could not load.', 'Offline native engine unsupported',
        'Insufficient phone memory', 'Recognized on phone: Bedroom / light /', 'No accepted allowed command.',
        'Voice stopped:', 'On-phone Vosk ·', 'Confirm Bedroom light ', 'REQUEST ID', 'Phase: ',
        'Board-reported last command #', 'Outputs: ', 'Matching ACK + fresh board report #',
        'Hold, wait for Speak now,', 'Choose Bedroom explicitly.', 'Require fresh inhibited board state',
        'Bedroom selected ·', 'Select Bedroom', 'Voice cancelled;', 'Recognizing entirely on phone;',
        'Last recognition outcome:', 'Speech was not one complete allowed command;',
        'No speech recognized;', 'Word confidence below the fixed 0.7 gate;',
        'Local engine returned an invalid/incomplete result;', 'Voice context cancelled;')
    for node in root.iter('node'):
        if node.get('class') == 'android.widget.EditText': continue
        text=node.get('text','')
        if text.startswith(prefixes) and len(text)<700: allowed.append(text)
    result=dict(source='PHONE_OBSERVED_VIA_ADB',utc=datetime.now(timezone.utc).isoformat(),phase=args.name,
        version=re.findall(r'version(?:Name|Code)=[^\r\n]+',version),
        microphone_granted='android.permission.RECORD_AUDIO: granted=true' in version,
        mobile_data=run('shell','settings','get','global','mobile_data').strip(),
        clawgrid_ap_reported='ClawGrid-Assistant' in wifi,
        ui_labels=allowed,
        memory_summary=[line.strip() for line in memory.splitlines() if any(x in line for x in ['TOTAL PSS:', 'TOTAL RSS:', 'TOTAL SWAP PSS:', 'Native Heap:','Java Heap:'])],
        local_model_storage=run('shell','run-as',PACKAGE,'du','-sk','no_backup/offline-voice').strip(),
        limits='UI/OS snapshots only; correlate command IDs with gateway ACK and advancing same-boot readback. No transcript/audio retained.')
    destination.write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps(result,indent=2))

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--name',required=True)
    main(p.parse_args())
