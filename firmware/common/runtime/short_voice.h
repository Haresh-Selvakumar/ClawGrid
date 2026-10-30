#pragma once
#include <stdint.h>
#include <math.h>
#include "command_target.h"

// Frozen feasibility experiment. Other builds retain the original vocabulary.
#if defined(CLAWGRID_SHORT_V1) || defined(CLAWGRID_MN7_EVAL) || defined(CLAWGRID_LIGHT_PREFLIGHT)
static constexpr bool kShortVoice=true;
#else
static constexpr bool kShortVoice=false;
#endif
#if defined(CLAWGRID_LIGHT_PREFLIGHT)
static constexpr const char *kVoiceGrammar="light-preflight-v1";
static constexpr const char *kVoiceEngine="ESP-SR 1.7.0 MultiNet6 English";
#elif defined(CLAWGRID_MN7_EVAL)
static constexpr const char *kVoiceGrammar="mn7-eval-v1";
static constexpr const char *kVoiceEngine="ESP-SR 1.7.0 MultiNet7 English";
#else
static constexpr const char *kVoiceGrammar=kShortVoice?"short-v1":"full-room-v1";
static constexpr const char *kVoiceEngine="ESP-SR 1.7.0 MultiNet6 English";
#endif
static constexpr uint32_t kVoiceBindingLeaseMs=30000;
struct SpeechBinding {
    uint32_t boot=0,capture=0,created=0;
    bool bedroom=false,screen=false;
};
inline bool short_voice_target(const SpeechBinding &b,uint32_t boot,bool fresh,uint32_t now,
        uint8_t command,float confidence,bool diagnostic,CommandTarget &out) {
    if(!fresh || !b.bedroom || !b.boot || b.boot!=boot || !b.capture || b.screen || diagnostic ||
       (uint32_t)(now-b.created)>=kVoiceBindingLeaseMs || command<1 || command>2 ||
       !isfinite(confidence) || confidence<0.7f || confidence>1.0f) return false;
    out={}; out.mode=TargetMode::PhysicalBedroom; out.room=TargetRoom::Bedroom;
    out.device=TargetDevice::Light; out.action=command==1?TargetAction::On:TargetAction::Off;
    return true;
}
