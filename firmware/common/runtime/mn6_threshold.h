#pragma once
#include <stddef.h>
#include <math.h>
#include <string.h>

// Compatibility repair for ESP-SR1.7.0 MN6 archive SHA256c5d8c65b... .
// Its public setter stores a probability directly at224, but the decoder
// compares that field with mean log probability. Results use exp(mean_log).
// speech/CMakeLists.txt rejects any other archive before this ABI is used.
// Keep the policy probability unchanged; convert units, never relax the gate.
static constexpr size_t kPinnedMn6ThresholdOffset=224;
static constexpr float kSpeechMinimumConfidence=0.7f;
template<class VendorSetter>
inline bool mn6_probability_threshold(void *model,float probability,VendorSetter set_vendor) {
    if(!model || !isfinite(probability) || probability<0.1f || probability>0.9999f) return false;
    set_vendor(probability);
    float stored=0;
    memcpy(&stored,(const unsigned char*)model+kPinnedMn6ThresholdOffset,sizeof(stored));
    // Fail closed if the pinned setter/layout no longer has the verified ABI.
    if(stored!=probability) return false;
    float log_probability=logf(probability);
    if(!isfinite(log_probability) || log_probability>=0) return false;
    memcpy((unsigned char*)model+kPinnedMn6ThresholdOffset,&log_probability,sizeof(log_probability));
    return true;
}
