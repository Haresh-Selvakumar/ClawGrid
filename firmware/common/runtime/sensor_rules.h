#pragma once
#include <stdint.h>

// Atomic per-device configuration. Temperatures are tenths of Celsius;
// lease seconds are finite (3..3600). Bit31 is reserved and rejected.
struct SensorRuleConfig {
    uint16_t on=280, off=260;
    uint32_t lease_ms=60000;
    bool sensors_first=false;
    uint32_t packed() const {
        return off | ((uint32_t)on<<9) | ((lease_ms/1000)<<18) | ((uint32_t)sensors_first<<30);
    }
    static bool unpack(uint32_t value,SensorRuleConfig &out) {
        SensorRuleConfig c;
        c.off=value&511; c.on=(value>>9)&511;
        c.lease_ms=((value>>18)&4095)*1000; c.sensors_first=(value>>30)&1;
        if((value>>31) || c.on>500 || c.off>=c.on || c.lease_ms<3000 || c.lease_ms>3600000) return false;
        out=c; return true;
    }
};
