#pragma once
#include <stdint.h>
#include <stddef.h>
#include "dht11_decode.h"

enum class Occupancy : uint8_t { Unknown = 0, Occupied = 1, LikelyOccupied = 2, Vacant = 3 };
struct RoomReport {
    bool motion = false, light = false, fan = false;
    Occupancy occupancy = Occupancy::Unknown;
    uint32_t boot_id = 0, uptime_ms = 0;
    uint32_t last_command=0, grace_ms=120000, timer_light_ms=0, timer_fan_ms=0;
    uint32_t light_on_ms=0, fan_on_ms=0;
    uint8_t mode=0, auto_flags=1, manual_flags=0;
    bool physical_outputs_enabled=false,relay_active_low=true;
    int16_t temperature_deci_c=0;
    uint16_t humidity_deci=0;
    TemperatureStatus temperature_status=TemperatureStatus::NotReady;
    uint32_t temperature_age_ms=UINT32_MAX;
    uint8_t control_policy_version=2;
    bool led_only_bench=false; // Declared build profile, not load sensing.
    bool pir_available=true; // Legacy reports default to the previously connected input.
};
static constexpr size_t kRoomReportBytes = 56;
inline bool temperature_valid(const RoomReport &s) {
    return s.temperature_status==TemperatureStatus::Ok && s.temperature_age_ms<10000 &&
        s.temperature_deci_c>=0 && s.temperature_deci_c<=500 && s.humidity_deci<=1000;
}
inline void report_temperature(RoomReport &r,const TemperatureSample &s,uint32_t now) {
    r.temperature_status=s.status;
    if(s.status==TemperatureStatus::Ok) {
        r.temperature_age_ms=now-s.sampled_ms;
        if(r.temperature_age_ms<10000) { r.temperature_deci_c=s.deci_c; r.humidity_deci=s.humidity_deci; }
        else r.temperature_status=TemperatureStatus::Stale;
    }
}
inline void wire_put32(uint8_t *p, uint32_t v) {
    p[0] = v >> 24; p[1] = v >> 16; p[2] = v >> 8; p[3] = v;
}
inline uint32_t wire_get32(const uint8_t *p) {
    return ((uint32_t)p[0] << 24) | ((uint32_t)p[1] << 16) | ((uint32_t)p[2] << 8) | p[3];
}
inline void encode_room_report(const RoomReport &s, uint8_t (&p)[kRoomReportBytes]) {
    p[0] = 5; p[1] = 1; // schema5 advertises actual controller manual-until-AUTO capability.
    p[2] = s.motion; p[3] = static_cast<uint8_t>(s.occupancy);
    p[4] = s.light; p[5] = s.fan; p[6] = s.physical_outputs_enabled; p[7] = s.relay_active_low;
    wire_put32(p + 8, s.boot_id); wire_put32(p + 12, s.uptime_ms);
    wire_put32(p+16,s.last_command); p[20]=s.mode; p[21]=s.auto_flags;
    p[22]=s.manual_flags; p[23]=0; wire_put32(p+24,s.grace_ms);
    wire_put32(p+28,s.timer_light_ms); wire_put32(p+32,s.timer_fan_ms);
    wire_put32(p+36,s.light_on_ms); wire_put32(p+40,s.fan_on_ms);
    p[44]=(uint16_t)s.temperature_deci_c>>8; p[45]=(uint16_t)s.temperature_deci_c;
    p[46]=s.humidity_deci>>8; p[47]=s.humidity_deci;
    p[48]=(uint8_t)s.temperature_status; p[49]=2; p[50]=s.led_only_bench; p[51]=s.pir_available?0:1;
    wire_put32(p+52,s.temperature_age_ms);
}
inline bool decode_room_report(const uint8_t *p, size_t n, RoomReport &s) {
    if (!p || !((n==kRoomReportBytes && (p[0]==4 || p[0]==5)) || (n==44 && p[0]==3)) || p[1] != 1 ||
        p[2] > 1 || p[3] > 3 || p[4] > 1 || p[5] > 1 || p[6]>1 || p[7]>1 ||
        p[20]>1 || p[21]>3 || p[22]>3 || p[23]) return false;
    s.motion = p[2]; s.occupancy = static_cast<Occupancy>(p[3]);
    s.light = p[4]; s.fan = p[5]; s.boot_id = wire_get32(p + 8); s.uptime_ms = wire_get32(p + 12);
    s.physical_outputs_enabled=p[6]; s.relay_active_low=p[7];
    s.last_command=wire_get32(p+16); s.mode=p[20]; s.auto_flags=p[21]; s.manual_flags=p[22];
    s.grace_ms=wire_get32(p+24); s.timer_light_ms=wire_get32(p+28); s.timer_fan_ms=wire_get32(p+32);
    s.light_on_ms=wire_get32(p+36); s.fan_on_ms=wire_get32(p+40);
    s.temperature_deci_c=0; s.humidity_deci=0; s.temperature_status=TemperatureStatus::NotReady; s.temperature_age_ms=UINT32_MAX;
    s.control_policy_version=p[0]==5?2:1;
    s.led_only_bench=false;
    s.pir_available=true;
    if(p[0]>=4) {
        if(p[48]>6 || p[49]!=(p[0]==5?2:0) || p[50]>(p[0]==5?1:0) || p[51]>(p[0]==5?1:0)) return false;
        s.led_only_bench=p[50];
        s.pir_available=p[51]==0;
        if(!s.pir_available && (s.motion || s.occupancy!=Occupancy::Unknown || s.auto_flags)) return false;
        if(s.led_only_bench && !s.physical_outputs_enabled) return false;
        s.temperature_status=(TemperatureStatus)p[48]; s.temperature_age_ms=wire_get32(p+52);
        s.temperature_deci_c=(int16_t)((uint16_t)p[44]<<8|p[45]); s.humidity_deci=(uint16_t)p[46]<<8|p[47];
        if(s.temperature_status==TemperatureStatus::Ok && !temperature_valid(s)) return false;
        if(s.temperature_status!=TemperatureStatus::Ok && (s.temperature_deci_c || s.humidity_deci)) return false;
    }
    return true;
}
inline const char *occupancy_name(Occupancy s) {
    switch (s) {
        case Occupancy::Occupied: return "OCCUPIED";
        case Occupancy::LikelyOccupied: return "LIKELY_OCCUPIED";
        case Occupancy::Vacant: return "VACANT";
        default: return "UNKNOWN";
    }
}
inline bool sequence_newer(uint32_t next, uint32_t last) {
    uint32_t delta = next - last;
    return delta != 0 && delta < 0x80000000u;
}
// Radio activity alone does not prove ControlTask has produced a new sample.
// Same-boot timestamps use wrap-safe ordering; persisted boot IDs increase.
inline bool room_report_advances(const RoomReport &next, const RoomReport &last) {
    return next.boot_id && (next.boot_id > last.boot_id ||
        (next.boot_id == last.boot_id && sequence_newer(next.uptime_ms,last.uptime_ms)));
}
inline bool room_report_publishable(const RoomReport &next, const RoomReport &last, uint32_t now) {
    // Control runs every 5 ms. Do not publish an aged mailbox sample after a
    // stall merely because the 1-second radio heartbeat is due.
    return room_report_advances(next,last) && (uint32_t)(now-next.uptime_ms)<=100;
}
