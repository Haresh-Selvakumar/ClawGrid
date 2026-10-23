import json
import pytest
from scripts.offline_speech import OfflineSpeech, SpeechError


class Decoder:
    def AcceptWaveform(self, data):
        return data == b'\x01\x00'

    def Result(self):
        return json.dumps({'text': 'bedroom lights off'})

    def PartialResult(self):
        return json.dumps({'partial': 'bedroom'})


def service():
    return OfflineSpeech('test-model', factory=Decoder)


def test_streaming_partial_final_and_exactly_once_audio_sequence():
    voice = service()
    session = voice.start()['session']
    partial = voice.feed(session, 1, b'\0\0')
    assert not partial['final'] and partial['text'] == ''
    final = voice.feed(session, 2, b'\x01\x00')
    assert final['final'] and final['text'] == 'bedroom lights off'
    assert final['final_id'] == 1
    for sequence in (2, 4, 0):
        with pytest.raises(SpeechError) as error:
            voice.feed(session, sequence, b'\x01\x00')
        assert error.value.status == 409
    assert voice.feed(session, 3, b'\x01\x00')['final_id'] == 2


@pytest.mark.parametrize('pcm', [b'', b'\0', bytes(16002)], ids=['empty', 'odd', 'oversized'])
def test_invalid_pcm_cannot_advance_stream(pcm):
    voice = service()
    session = voice.start()['session']
    with pytest.raises(SpeechError):
        voice.feed(session, 1, pcm)
    assert voice.feed(session, 1, b'\0\0')['sequence'] == 1


def test_session_ownership_stop_and_no_partial_flush():
    voice = service()
    old = voice.start()['session']
    with pytest.raises(SpeechError):
        voice.start()
    with pytest.raises(SpeechError):
        voice.feed('wrong', 1, b'\0\0')
    with pytest.raises(SpeechError):
        voice.stop('wrong')
    assert voice.stop(old) == {'stopped': True}
    assert not voice.status()['active']
    new = voice.start()['session']
    assert new != old
    with pytest.raises(SpeechError):
        voice.feed(old, 1, b'\0\0')


def test_expired_audio_never_reopens_old_session():
    now = [0.0]
    voice = OfflineSpeech('test', factory=Decoder, clock=lambda: now[0])
    old = voice.start()['session']
    now[0] = 15
    with pytest.raises(SpeechError):
        voice.feed(old, 1, b'\x01\x00')
    assert voice.start()['session'] != old


def test_long_transcript_is_rejected_whole_not_truncated_into_a_command():
    class Long(Decoder):
        def Result(self):
            return json.dumps({'text': 'bedroom light on ' + 'words ' * 30})
    voice = OfflineSpeech('test', factory=Long)
    session = voice.start()['session']
    result = voice.feed(session, 1, b'\x01\x00')
    assert result['text'] == '' and result['rejected']
