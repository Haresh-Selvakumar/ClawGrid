#pragma once
#include <stdint.h>

enum TrafficEvent : uint32_t {
    TrafficTx = 1u, TrafficRx = 2u, TrafficCommand = 4u,
    TrafficAck = 8u, TrafficError = 16u
};

// Portable, wrap-safe pulse scheduling. Continuous telemetry cannot hold the
// LED on forever or obscure the longer command/ACK indications.
struct TrafficIndicator {
    uint32_t color = 0, started = 0, duration = 0, off_started = 0;
    uint8_t priority = 0;
    bool active = false, gap = false;

    uint32_t tick(uint32_t events, uint32_t now) {
        if (active && (uint32_t)(now - started) >= duration) {
            active = false; color = 0; priority = 0;
            gap = true; off_started = now;
        }
        if (gap && (uint32_t)(now - off_started) >= 80u) gap = false;
        uint32_t next = 0, ms = 0; uint8_t rank = 0;
        if (events & TrafficError) { next = 0x080000; ms = 220; rank = 4; }
        else if (events & TrafficAck) { next = 0x080808; ms = 280; rank = 3; }
        else if (events & TrafficCommand) { next = 0x000800; ms = 180; rank = 2; }
        else if (events & TrafficRx) { next = 0x000800; ms = 60; rank = 1; }
        else if (events & TrafficTx) { next = 0x000008; ms = 60; rank = 1; }
        if (rank && (!active || rank > priority) && (!gap || rank > 1)) {
            color = next; started = now; duration = ms; priority = rank;
            active = true; gap = false;
        }
        return color;
    }
};
