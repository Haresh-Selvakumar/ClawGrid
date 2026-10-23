"""One-time official Vosk model download; normal voice operation stays offline."""
import hashlib
import json
import shutil
import urllib.request
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODEL = 'vosk-model-small-en-us-0.15'
URL = f'https://alphacephei.com/vosk/models/{MODEL}.zip'


def main():
    destination = ROOT / '.speech_models'
    destination.mkdir(exist_ok=True)
    archive = destination / (MODEL + '.zip')
    if not archive.exists():
        temporary = archive.with_suffix('.download')
        with urllib.request.urlopen(URL, timeout=60) as source, temporary.open('wb') as target:
            shutil.copyfileobj(source, target)
        temporary.replace(archive)
    with zipfile.ZipFile(archive) as package:
        if sum(info.file_size for info in package.infolist()) > 300 * 1024 * 1024:
            raise ValueError('Unexpected model archive size')
        for info in package.infolist():
            target = (destination / info.filename).resolve()
            if not target.is_relative_to(destination.resolve()) or not info.filename.startswith(MODEL + '/'):
                raise ValueError('Invalid archive member path')
        package.extractall(destination)
    assert (destination / MODEL / 'am/final.mdl').is_file()
    (destination / 'download_manifest.json').write_text(json.dumps(dict(
        url=URL, model=MODEL, archive_sha256=hashlib.sha256(archive.read_bytes()).hexdigest(),
        bytes=archive.stat().st_size), indent=2) + '\n')
    print('Offline model ready:', destination / MODEL)


if __name__ == '__main__':
    main()
