"""Synthetic offline-PC ASR + actual portable C++ parser; never controls hardware."""
import argparse
import csv
import json
import random
import struct
import subprocess
import wave
from pathlib import Path
from offline_speech import OfflineSpeech

ROOT = Path(__file__).resolve().parents[1]


def main(args):
    voice = OfflineSpeech(args.model)
    cases = []
    directory = ROOT / 'benchmarks/data/synthetic_speech'
    for row in csv.DictReader((directory / 'manifest.csv').open()):
        command = int(row['expected_id'])
        if command not in range(1, 5):
            continue
        with wave.open(str(directory / row['file'])) as audio:
            assert (audio.getnchannels(), audio.getsampwidth(), audio.getframerate()) == (1, 2, 16000)
            pcm = audio.readframes(audio.getnframes())
        cases.append(dict(name=row['file'], phrase=row['phrase'], pcm=pcm,
                          expected={'room': 'bedroom', 'device': 'light' if command < 3 else 'fan',
                                    'action': 'ON' if command % 2 else 'OFF'}))
    if args.extra:
        for row in json.loads(args.extra.read_text()):
            with wave.open(str(args.extra.parent / row['file'])) as audio:
                assert (audio.getnchannels(), audio.getsampwidth(), audio.getframerate()) == (1, 2, 16000)
                row['pcm'] = audio.readframes(audio.getnframes())
            cases.append(row)
    rng = random.Random(20261022)
    cases.extend([
        dict(name='silence', phrase='', expected=None, pcm=bytes(32000)),
        dict(name='noise', phrase='', expected=None,
             pcm=b''.join(struct.pack('<h', rng.randint(-1000, 1000)) for _ in range(16000))),
    ])
    results = []
    process = subprocess.Popen([str(args.controller.resolve())], stdin=subprocess.PIPE,
                               stdout=subprocess.PIPE, text=True, bufsize=1)
    try:
        for row in cases:
            session = voice.start()['session']
            finals = []
            pcm = row['pcm'] + bytes(64000)  # Endpoint silence for this fixture only.
            for sequence, offset in enumerate(range(0, len(pcm), 8000), 1):
                answer = voice.feed(session, sequence, pcm[offset:offset + 8000])
                if answer['final'] and answer['text']:
                    finals.append(answer['text'])
            voice.stop(session)
            proposals = []
            for text in finals:
                process.stdin.write('text physical_bedroom ' + text + '\n')
                process.stdin.flush()
                proposals.append(json.loads(process.stdout.readline()))
            accepted = [p for p in proposals if p.get('http_status', 200) == 200]
            expected = row['expected']
            passed = not accepted if expected is None else len(accepted) == 1 and all(
                accepted[0].get(key) == value for key, value in expected.items())
            result = {k: v for k, v in row.items() if k != 'pcm'}
            result.update(audio_source='SIMULATED', recognition_source='HOST_MEASURED',
                          parser_source='HOST_MEASURED', transcripts=finals,
                          proposals=proposals, passed=passed)
            results.append(result)
            print(result.get('name', result['file'] if 'file' in result else ''),
                  'PASS' if passed else 'FAIL', finals, flush=True)
    finally:
        process.terminate()
        process.wait(timeout=5)
    report = dict(model=voice.model_name, grammar=voice.grammar, cases=results,
                  passed=sum(r['passed'] for r in results), total=len(results),
                  limitation='Synthetic fixtures; no human accuracy or physical actuation measured')
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2) + '\n')
    print(f"{report['passed']}/{report['total']} synthetic cases passed; no hardware commands sent.")
    return int(report['passed'] != report['total'])


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--model', type=Path, default=ROOT / '.speech_models/vosk-model-small-en-us-0.15')
    parser.add_argument('--controller', type=Path, required=True)
    parser.add_argument('--extra', type=Path)
    parser.add_argument('--output', type=Path, default=ROOT / 'benchmarks/raw/pc_voice_synthetic_20261022.json')
    raise SystemExit(main(parser.parse_args()))
