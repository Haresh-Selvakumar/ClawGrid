"""Fetch a separate hash-verified ESP-SR1.7 MN7 asset set; never touch installed MN6."""
from pathlib import Path
import hashlib
import json
import urllib.request

ROOT=Path(__file__).resolve().parents[1]
COMMIT='07b0a4d0f6b5de755874d5405150ee0a8eec9865'
DEST=ROOT/'models/mn7-eval-v1/upstream'

def main():
    tree=json.load(urllib.request.urlopen(f'https://api.github.com/repos/espressif/esp-sr/git/trees/{COMMIT}?recursive=1',timeout=30))
    if tree.get('truncated'):raise RuntimeError('Truncated official inventory')
    records=[]
    for e in tree['tree']:
        path=e['path']
        if e['type']!='blob' or not(path.startswith('model/multinet_model/mn7_en/') or path in ['LICENSE','tool/multinet_g2p.py','docs/en/speech_command_recognition/README.rst','docs/en/audio_front_end/README.rst']):continue
        p=DEST/path
        data=p.read_bytes() if p.exists() else urllib.request.urlopen(f'https://raw.githubusercontent.com/espressif/esp-sr/{COMMIT}/{path}',timeout=60).read()
        if hashlib.sha1(f'blob {len(data)}\0'.encode()+data).hexdigest()!=e['sha']:raise RuntimeError('Pinned Git blob mismatch: '+path)
        p.parent.mkdir(parents=True,exist_ok=True)
        if not p.exists():p.write_bytes(data)
        records.append(dict(path=path,git_blob=e['sha'],sha256=hashlib.sha256(data).hexdigest(),bytes=len(data)))
    manifest=dict(commit=COMMIT,version='1.7.0',files=records,installed=False)
    p=DEST.parent/'upstream-manifest.json'
    if p.exists() and json.loads(p.read_text())!=manifest:raise RuntimeError('Existing inventory differs; preserve and stop')
    p.write_text(json.dumps(manifest,indent=2)+'\n')
    print('Separate pinned MN7 assets verified:',len(records),'files;',sum(x['bytes'] for x in records),'bytes; no deployment')

if __name__=='__main__':main()
