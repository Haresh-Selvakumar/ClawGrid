"""Verify actual bundled model, native 16 KB ELF alignment and active offline boundary."""
import argparse
import hashlib
import io
import json
import struct
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def main(apk):
    with zipfile.ZipFile(apk) as archive:
        manifest = json.loads(archive.read('assets/offline-voice/manifest.json'))
        data = archive.read('assets/offline-voice/model.zip')
        assert len(data) == manifest['archive_bytes']
        assert hashlib.sha256(data).hexdigest() == manifest['archive_sha256']
        with zipfile.ZipFile(io.BytesIO(data)) as model:
            expected = {manifest['model'] + '/' + f['path']: f for f in manifest['files']}
            assert {f.filename for f in model.infolist() if not f.is_dir()} == set(expected)
            for name, row in expected.items():
                content = model.read(name)
                assert len(content) == row['bytes'] and hashlib.sha256(content).hexdigest() == row['sha256']
        assert 'Apache License' in archive.read('assets/offline-voice/NOTICE.txt').decode()
        native = []
        for name in archive.namelist():
            if not name.startswith('lib/') or not name.endswith('.so'):
                continue
            content = archive.read(name)
            assert content[:4] == b'\x7fELF' and content[5] == 1
            is64 = content[4] == 2
            if is64:
                offset = struct.unpack_from('<Q', content, 32)[0]
                size, count = struct.unpack_from('<HH', content, 54)
                align = [struct.unpack_from('<Q', content, offset+i*size+48)[0] for i in range(count)
                         if struct.unpack_from('<I', content, offset+i*size)[0] == 1]
                assert align and min(align) >= 16384, 'Native library lacks 16 KB load alignment: ' + name
            else:
                align = []
            native.append(dict(path=name, bytes=len(content), sha256=hashlib.sha256(content).hexdigest(),
                               min_load_alignment=min(align) if align else None))
        for abi in ['arm64-v8a','armeabi-v7a','x86','x86_64']:
            assert any(x['path'] == f'lib/{abi}/libvosk.so' for x in native)
            assert any(x['path'] == f'lib/{abi}/libjnidispatch.so' for x in native)
    source = ROOT / 'ecosystem/android/app/src/main/java/com/example'
    viewmodel = (source / 'ui/ClawGridViewModel.kt').read_text()
    engine = (source / 'data/remote/OfflinePhoneRecognizer.kt').read_text()
    assert 'LocalVoiceRepository(' not in viewmodel and 'PhoneVoiceDiagnostic.save' not in viewmodel
    policy = (source / 'data/remote/PhoneVoicePolicy.kt').read_text(encoding='utf-8')
    assert 'Recognizer(loaded, 16000f, PhoneVoicePolicy.decoderGrammar)' in engine and 'decoder.setWords(true)' in engine
    assert '"[unk]"' in policy and 'rejectionPhrases' in policy and 'MIN_WORD_CONFIDENCE = 0.7' in policy
    assert 'vosk_set_log_level(-1)' in engine
    for forbidden in ['SpeechRecognizer', 'HttpSpeechApi', 'java.net.', 'okhttp', 'Log.', 'println(']:
        assert forbidden not in engine.replace('No SpeechRecognizer', 'No default recognizer'), forbidden
    print(json.dumps(dict(source='HOST_MEASURED', apk=str(apk.resolve()), bytes=apk.stat().st_size,
        sha256=hashlib.sha256(apk.read_bytes()).hexdigest(), model=manifest['model'],
        archive_bytes=manifest['archive_bytes'], extracted_bytes=manifest['extracted_bytes'],
        model_files=len(manifest['files']), native=native,
        limits='Artifact/source checks; not a phone recognition, storage, RAM or offline-runtime observation'), indent=2))

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('apk', type=Path)
    main(parser.parse_args().apk)
