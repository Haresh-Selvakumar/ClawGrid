#pragma once
#include <stddef.h>
#include <stdint.h>

// Pinned ESP-SR1.7.0 test_multinet.c creates a 6000ms model and continues
// feeding zero frames after the clip, bounded at601 calls. The benchmark's
// seven-frame CPU accounting adjustment is not a decoder timeout contract.
static constexpr uint32_t kSpeechWindowMs=6000;
static constexpr size_t kSpeechRate=16000,kSpeechMaxFrames=601;
static constexpr uint8_t kSpeechReferenceId=17;
static constexpr size_t kSpeechReferenceBytes=89658;
static constexpr uint32_t kSpeechReferenceCrc=330285049;
enum class SpeechInput : uint8_t { Application=0, ReferenceWindow=1, ReferenceLegacy=2,
    ReferenceResetFirst=3, ReferenceFreshWindow=4, ReferenceOfficial=5, ReferencePinned=6, ReferenceAfe=7,
    ReferenceTrace=8, ReferenceDirect=9, ReferenceSdkHeap=10, ReferencePsram=11,
    ReferenceGain15=12, ReferenceSynthetic=13, ReferencePhoneAfe=14, ReferencePhoneBedroom=15,
    ReferencePhoneTrace=16, ReferenceZiraTrace=17, ReferenceSilenceTrace=18, ReferenceNonCommandTrace=19,
    ReferencePhoneSplit=20, ReferenceZiraSplit=21, ReferenceSilenceSplit=22, ReferenceNonCommandSplit=23,
    ShortPreflight=24, Mn7Official=25 };
inline bool speech_split_reference(SpeechInput kind) { return (uint8_t)kind>=20 && (uint8_t)kind<=23; }
inline SpeechInput speech_reference_fixture(SpeechInput kind) { return speech_split_reference(kind)?(SpeechInput)((uint8_t)kind-4):kind; }
inline bool speech_decoder_trace(SpeechInput kind) { return (uint8_t)kind>=16 && (uint8_t)kind<=23; }
inline bool speech_phone_reference(SpeechInput kind) { return kind==SpeechInput::ReferencePhoneAfe || kind==SpeechInput::ReferencePhoneBedroom || speech_decoder_trace(kind); }
inline size_t speech_reference_bytes(SpeechInput kind) {
    kind=speech_reference_fixture(kind);
    return kind==SpeechInput::ReferenceZiraTrace?69760:kind==SpeechInput::ReferenceNonCommandTrace?67520:speech_phone_reference(kind)?64000:kSpeechReferenceBytes;
}
inline bool speech_trace_reference(SpeechInput kind) { return kind==SpeechInput::ReferenceTrace || kind==SpeechInput::ReferenceDirect || kind==SpeechInput::ReferenceSdkHeap || kind==SpeechInput::ReferencePsram || kind==SpeechInput::ReferenceGain15 || kind==SpeechInput::ReferenceSynthetic; }
inline size_t speech_afe_feed_frames(size_t samples,size_t chunk=512) {
    return (samples==44829 || samples==32000) && chunk && chunk<=1024?(4096+samples+32768+chunk-1)/chunk:0;
}
inline bool speech_is_reference(SpeechInput kind) { return kind!=SpeechInput::Application; }
// Literal upstream CPU test keeps feeding after TIMEOUT and discards its last
// partial PCM frame. Confine both differences to the isolated reference job.
inline bool speech_stop_on_timeout(SpeechInput kind) {
    return kind==SpeechInput::ReferenceGain15 || kind==SpeechInput::ReferenceSynthetic ||
        (kind!=SpeechInput::ReferencePinned && !speech_trace_reference(kind));
}
inline bool speech_drop_partial_frame(SpeechInput kind,size_t offset,size_t chunk,size_t samples) {
    return (kind==SpeechInput::ReferencePinned || speech_trace_reference(kind)) && offset+chunk>samples;
}
inline size_t speech_feed_frames(size_t samples,size_t chunk,SpeechInput kind) {
    if(!chunk || chunk>1024 || samples>64000) return 0;
    size_t clip_tail=(samples+kSpeechRate+chunk-1)/chunk;
    if(kind==SpeechInput::ReferenceLegacy) return clip_tail;
    return kSpeechMaxFrames;
}
inline bool speech_reference_matches(size_t bytes,uint32_t crc,SpeechInput kind=SpeechInput::ReferencePinned) {
    kind=speech_reference_fixture(kind);
    if(speech_phone_reference(kind)) {
        uint32_t expected=kind==SpeechInput::ReferenceZiraTrace?2182999710u:kind==SpeechInput::ReferenceSilenceTrace?2109903275u:kind==SpeechInput::ReferenceNonCommandTrace?3492560341u:679739021u;
        return bytes==speech_reference_bytes(kind) && crc==expected;
    }
    return bytes==kSpeechReferenceBytes && crc==(kind==SpeechInput::ReferenceSynthetic?2905759910u:kSpeechReferenceCrc);
}
