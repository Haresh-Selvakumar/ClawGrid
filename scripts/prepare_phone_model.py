"""Bundle the pinned official Vosk English model. Build-time only; no phone download."""
import hashlib
import json
import shutil
import urllib.request
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
NAME = 'vosk-model-small-en-us-0.15'
SHA = '30f26242c4eb449f948e42cb302dd7a686cb29a3423a8367f99ff41780942498'
URL = 'https://alphacephei.com/vosk/models/' + NAME + '.zip'

def main():
    cache = ROOT / '.speech_models' / (NAME + '.zip')
    if not cache.is_file():
        cache.parent.mkdir(exist_ok=True)
        urllib.request.urlretrieve(URL, cache)
    assert hashlib.sha256(cache.read_bytes()).hexdigest() == SHA, 'Model archive differs from pinned original'
    assets = ROOT / 'ecosystem/android/app/src/main/assets/offline-voice'
    assets.mkdir(parents=True, exist_ok=True)
    files = []
    with zipfile.ZipFile(cache) as archive:
        for entry in archive.infolist():
            if entry.is_dir():
                continue
            relative = entry.filename.removeprefix(NAME + '/')
            assert relative != entry.filename and '..' not in Path(relative).parts
            data = archive.read(entry)
            files.append(dict(path=relative, bytes=len(data), sha256=hashlib.sha256(data).hexdigest()))
    manifest = dict(model=NAME, url=URL, license='Apache-2.0', archive_sha256=SHA,
                    archive_bytes=cache.stat().st_size, extracted_bytes=sum(f['bytes'] for f in files), files=files)
    shutil.copyfile(cache, assets / 'model.zip')
    (assets / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf-8')
    # Official API license plus explicit model attribution, retained in the installed APK.
    with urllib.request.urlopen('https://raw.githubusercontent.com/alphacep/vosk-api/master/COPYING') as response:
        license_text = response.read().decode()
    (assets / 'NOTICE.txt').write_text('Vosk Android 0.3.75 / small English model 0.15\n'
        'Copyright Alpha Cephei / Vosk contributors. API and this model: Apache License 2.0.\n'
        'Model: ' + URL + '\nAPI: https://github.com/alphacep/vosk-api\n'
        'JNA 5.18.1 is distributed under Apache-2.0 / LGPL-2.1-or-later (Apache option used).\n\n'
        + license_text, encoding='utf-8')
    print(json.dumps({k: v for k, v in manifest.items() if k != 'files'}, indent=2))

if __name__ == '__main__':
    main()
