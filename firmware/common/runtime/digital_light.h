#pragma once
#include <stdint.h>

// LM393 DO is a comparator threshold, never lux or occupancy. Unknown polarity
// is retained until the user completes a physical illumination/cover check.
class DigitalLightFilter {
public:
    void sample(bool raw, uint32_t now) {
        if(!started_ || raw != candidate_) { started_=true; candidate_=raw; since_=now; }
        if((uint32_t)(now-since_)>=50) { value_=candidate_; available_=true; at_=now; }
    }
    bool available(uint32_t now) const { return available_ && (uint32_t)(now-at_)<1000; }
    bool raw() const { return value_; }
    uint32_t age(uint32_t now) const { return available_?now-at_:UINT32_MAX; }
private:
    bool started_=false, candidate_=false, value_=false, available_=false;
    uint32_t since_=0, at_=0;
};
