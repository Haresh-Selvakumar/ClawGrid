#pragma once
#include <stdint.h>
#include <stddef.h>
#include "speech_feed.h"
#include "short_voice.h"
struct SpeechSnapshot {
    uint32_t job=0,samples=0,inference_us=0,max_frame_us=0,frames=0,stack_free=0,pcm_crc32=0;
    uint8_t phase=0,command_id=0; float confidence=0;
    char diagnostic_text[96]={};
    uint32_t peak=0,clipped=0,nonzero=0,chunk_samples=0,padded_samples=0,detecting_frames=0;
    uint32_t engine_errors=0;
    uint8_t error=0,last_state=0,candidate_id=0;
    float candidate_score=0;
    char model_name[24]={};
    uint32_t planned_frames=0,model_window_ms=kSpeechWindowMs,command_count=16;
    uint32_t rms=0; int32_t dc_mean=0;
    SpeechInput input=SpeechInput::Application;
    SpeechBinding binding;
    uint32_t internal_free=0,internal_min=0,internal_largest=0,psram_free=0;
    bool reference_detected=false,window_exhausted=false;
    int32_t raw_result_count=0; uint8_t raw_result_state=0;
    uint32_t afe_feed_frames=0,afe_fetch_frames=0,afe_samples=0,afe_crc32=0,afe_peak=0,afe_rms=0;
};
#if defined(CLAWGRID_ESP_SR)
bool speech_begin();
uint8_t *speech_reserve_clip(size_t bytes,uint32_t &job,SpeechInput input=SpeechInput::Application,SpeechBinding binding={});
bool speech_finish_clip(bool complete);
SpeechSnapshot speech_snapshot();
#else
inline bool speech_begin() { return false; }
inline uint8_t *speech_reserve_clip(size_t,uint32_t&,SpeechInput=SpeechInput::Application,SpeechBinding={}) { return nullptr; }
inline bool speech_finish_clip(bool) { return false; }
inline SpeechSnapshot speech_snapshot() { return {}; }
#endif
