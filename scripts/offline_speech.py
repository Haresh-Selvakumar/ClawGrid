"""Explicit local-PC speech recognizer. No network calls or actuator ownership."""
import json
import secrets
import threading
import time
from pathlib import Path


def command_grammar():
    """Command vocabulary with explicit negations and unknown-word paths.

    Include negative utterances so decoding is not forced into an ON/OFF command.
    The MCU's whole-word parser remains the authorization boundary for text.
    """
    phrases = {'[unk]'}
    for room in ('bedroom', 'bed room', 'hall', 'living room'):
        for device in ('light', 'lights', 'lamp', 'bulb', 'fan'):
            for action in ('on', 'off'):
                for phrase in (f'{room} {device} {action}', f'turn {action} {room} {device}',
                               f'turn {action} the {room} {device}', f'turn {room} {device} {action}',
                               f'switch {action} {room} {device}'):
                    phrases.add(phrase)
                    phrases.add('please ' + phrase)
                    for negative in ('do not ', "don't ", 'never '):
                        phrases.add(negative + phrase)
    return sorted(phrases)


class SpeechError(Exception):
    def __init__(self, status, message):
        super().__init__(message)
        self.status = status


class OfflineSpeech:
    RATE = 16000
    MAX_BYTES = 16000  # At most 0.5 s of mono PCM16 per request.
    IDLE_SECONDS = 15

    def __init__(self, model_path, factory=None, clock=time.monotonic):
        self.clock = clock
        self.lock = threading.RLock()
        self.session = None
        self.recognizer = None
        self.sequence = self.final_id = 0
        self.last_seen = 0
        self.model_name = Path(model_path).name
        self.grammar = command_grammar()
        if factory is None:
            from vosk import Model, KaldiRecognizer, SetLogLevel
            path = Path(model_path).resolve()
            if not (path / 'am/final.mdl').is_file():
                raise ValueError('Offline model missing; run scripts/setup_offline_speech.py first')
            SetLogLevel(-1)
            self.model = Model(str(path))  # Explicit path: never auto-download.
            self.factory = lambda: KaldiRecognizer(self.model, self.RATE, json.dumps(self.grammar))
        else:
            self.factory = factory

    def _expire(self):
        if self.session and self.clock() - self.last_seen >= self.IDLE_SECONDS:
            self.session = self.recognizer = None

    def status(self):
        with self.lock:
            self._expire()
            return dict(ready=True, active=bool(self.session), engine='Vosk offline on PC',
                        model=self.model_name, sample_rate=self.RATE,
                        processing_source='HOST_MEASURED', audio_saved=False)

    def start(self):
        with self.lock:
            self._expire()
            if self.session:
                raise SpeechError(409, 'Another listening session is active')
            self.recognizer = self.factory()
            self.session = secrets.token_hex(16)
            self.sequence = self.final_id = 0
            self.last_seen = self.clock()
            return dict(session=self.session, sample_rate=self.RATE, sequence=1,
                        processing_source='HOST_MEASURED')

    def feed(self, session, sequence, pcm):
        with self.lock:
            self._expire()
            if not self.session or session != self.session:
                raise SpeechError(409, 'Listening session expired or changed; start again')
            if sequence != self.sequence + 1:
                raise SpeechError(409, 'Audio sequence must advance exactly once; do not retry')
            if not pcm or len(pcm) % 2 or len(pcm) > self.MAX_BYTES:
                raise SpeechError(400, 'Use 1–8000 mono PCM16 samples at 16000 Hz')
            final = bool(self.recognizer.AcceptWaveform(pcm))
            result = json.loads(self.recognizer.Result() if final else self.recognizer.PartialResult())
            text = str(result.get('text' if final else 'partial', '')).strip().lower()
            self.sequence = sequence
            self.last_seen = self.clock()
            if final and text:
                self.final_id += 1
            oversized = len(text) > 110  # Leave room for /api/text target scope.
            return dict(sequence=sequence, final=final, final_id=self.final_id,
                        text=text if final and not oversized else '',
                        partial=text[:200] if not final else '',
                        rejected='Utterance too long' if oversized else '',
                        processing_source='HOST_MEASURED')

    def stop(self, session):
        with self.lock:
            self._expire()
            if self.session and session != self.session:
                raise SpeechError(409, 'Listening session changed')
            # Do not flush a partial utterance into a command when Stop is pressed.
            self.session = self.recognizer = None
            return dict(stopped=True)
