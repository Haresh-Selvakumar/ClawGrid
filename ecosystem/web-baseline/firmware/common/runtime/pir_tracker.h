#pragma once
#include <stdint.h>
#include "state_wire.h"

class PirTracker {
public:
    // Physical input must warm up, then establish a filtered LOW baseline.
    // A HIGH carried across startup is not evidence of a new motion event.
    void begin(uint32_t now, uint32_t warmup_ms) {
        candidate_ = stable_ = seen_ = false;
        candidate_since_ = last_motion_ = 0;
        started_ = now; warmup_ms_ = warmup_ms;
        ready_ = low_timing_ = false;
    }
    bool ready() const { return ready_; }
    uint32_t warmup_remaining(uint32_t now) const {
        if (ready_) return 0;
        uint32_t elapsed = now - started_;
        return elapsed < warmup_ms_ ? warmup_ms_ - elapsed : 0;
    }
    bool sample(bool raw, uint32_t now) {
        if (!ready_) {
            if (warmup_remaining(now) || raw) { low_timing_ = false; return false; }
            if (!low_timing_) { candidate_since_ = now; low_timing_ = true; }
            if ((uint32_t)(now - candidate_since_) >= 200) ready_ = true;
            return false;
        }
        if (raw != candidate_) { candidate_ = raw; candidate_since_ = now; }
        bool changed = false;
        if (candidate_ != stable_ && (uint32_t)(now - candidate_since_) >= 200) {
            stable_ = candidate_; changed = true;
        }
        if (stable_) { seen_ = true; last_motion_ = now; }
        return changed;
    }
    bool motion() const { return stable_; }
    Occupancy occupancy(uint32_t now, uint32_t grace_ms) const {
        if (!seen_) return Occupancy::Unknown;
        uint32_t elapsed = now - last_motion_;
        if (stable_ || elapsed < 30000) return Occupancy::Occupied;
        return elapsed < grace_ms ? Occupancy::LikelyOccupied : Occupancy::Vacant;
    }
private:
    bool candidate_ = false, stable_ = false, seen_ = false;
    uint32_t candidate_since_ = 0, last_motion_ = 0;
    // Default construction preserves the portable tracker fixtures. The real
    // GPIO path explicitly calls begin(); virtual input uses HouseSimulation.
    bool ready_ = true, low_timing_ = false;
    uint32_t started_ = 0, warmup_ms_ = 0;
};
