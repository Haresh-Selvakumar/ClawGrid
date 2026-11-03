#pragma once
#include <stdint.h>
#include <stddef.h>
#include "dht11_decode.h"
#include "sensor_rules.h"

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
    uint8_t features_version=0, schedule_action[2]={},schedule_result[2]={},control_source[2]={};
    uint32_t schedule_ms[2]={},energy_cap_ms[2]={};
    uint32_t sensor_config[2]={SensorRuleConfig().packed(),SensorRuleConfig().packed()}, override_ms[2]={};
    uint8_t sensor_demand[2]={}, winner[2]={}, blocking[2]={}, override_action[2]={};
    uint8_t room_id=1; bool light_available=false,light_raw=false;
    uint32_t light_age_ms=UINT32_MAX,temperature_sample_age_ms=UINT32_MAX;
    uint32_t sensor_snapshot_ms=0,evaluation_cycle=0,evaluation_started_us=0;
    uint32_t rule_started_us[2]={},rule_elapsed_us[2]={};
    uint8_t sensor_order=0,evaluation_step[2]={},sensor_requested[2]={},sensor_result[2]={};
    bool evaluation_clock_valid=false;
    bool pir_available=true; // Legacy reports default to the previously connected input.
};
static constexpr size_t kRoomReportBytes = 152;
inline bool temperature_valid(const RoomReport &s) {
    return s.temperature_status==TemperatureStatus::Ok && s.temperature_age_ms<10000 &&
        s.temperature_deci_c>=0 && s.temperature_deci_c<=500 && s.humidity_deci<=1000;
}
inline void report_temperature(RoomReport &r,const TemperatureSample &s,uint32_t now) {
    r.temperature_status=s.status;
    r.temperature_sample_age_ms=s.status==TemperatureStatus::NotReady?UINT32_MAX:now-s.sampled_ms;
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
    wire_put32(p+108,s.light_age_ms); wire_put32(p+112,s.sensor_snapshot_ms);
    wire_put32(p+116,s.evaluation_cycle); wire_put32(p+120,s.evaluation_started_us);
    for(int i=0;i<2;++i) { wire_put32(p+124+i*4,s.rule_started_us[i]); wire_put32(p+132+i*4,s.rule_elapsed_us[i]);
        p[141+i]=s.evaluation_step[i]; p[143+i]=s.sensor_requested[i]; p[145+i]=s.sensor_result[i]; }
    p[140]=s.sensor_order; p[147]=s.evaluation_clock_valid; wire_put32(p+148,s.temperature_sample_age_ms);
    p[0] = 9; p[1] = s.room_id; // schema6 adds controller schedule/cap state; schema5 manual policy remains.
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
    p[80]=s.light_available; p[81]=s.light_available && s.light_raw; p[82]=s.features_version; p[83]=0;
    for(int i=0;i<2;++i) {
        unsigned o=84+i*12; wire_put32(p+o,s.sensor_config[i]); wire_put32(p+o+4,s.override_ms[i]);
        p[o+8]=s.sensor_demand[i]; p[o+9]=s.winner[i]; p[o+10]=s.blocking[i]; p[o+11]=s.override_action[i];
    }
    for(int i=0;i<2;++i) {
        unsigned o=56+i*12; wire_put32(p+o,s.schedule_ms[i]); wire_put32(p+o+4,s.energy_cap_ms[i]);
        p[o+8]=s.schedule_action[i]; p[o+9]=s.schedule_result[i]; p[o+10]=s.control_source[i]; p[o+11]=0;
    }
}
inline bool decode_room_report(const uint8_t *p, size_t n, RoomReport &s) {
    if (!p || !((n==kRoomReportBytes && p[0]==9) || (n==108 && p[0]==8) || (n==84 && p[0]==7) || (n==80 && p[0]==6) || (n==56 && (p[0]==4 || p[0]==5)) || (n==44 && p[0]==3)) || (p[1] != 1 && (p[0]<7 || p[1]!=2)) ||
        p[2] > 1 || p[3] > 3 || p[4] > 1 || p[5] > 1 || p[6]>1 || p[7]>1 ||
        p[20]>1 || p[21]>3 || p[22]>3 || p[23]) return false;
    s.motion = p[2]; s.occupancy = static_cast<Occupancy>(p[3]);
    s.light = p[4]; s.fan = p[5]; s.boot_id = wire_get32(p + 8); s.uptime_ms = wire_get32(p + 12);
    s.physical_outputs_enabled=p[6]; s.relay_active_low=p[7];
    s.last_command=wire_get32(p+16); s.mode=p[20]; s.auto_flags=p[21]; s.manual_flags=p[22];
    s.grace_ms=wire_get32(p+24); s.timer_light_ms=wire_get32(p+28); s.timer_fan_ms=wire_get32(p+32);
    s.light_on_ms=wire_get32(p+36); s.fan_on_ms=wire_get32(p+40);
    s.temperature_deci_c=0; s.humidity_deci=0; s.temperature_status=TemperatureStatus::NotReady; s.temperature_age_ms=UINT32_MAX;
    s.control_policy_version=p[0]>=5?2:1;
    s.led_only_bench=false;
    s.pir_available=true;
    if(p[0]>=4) {
        if(p[48]>6 || p[49]!=(p[0]>=5?2:0) || p[50]>(p[0]>=5?1:0) || p[51]>(p[0]>=5?1:0)) return false;
        s.led_only_bench=p[50];
        s.pir_available=p[51]==0;
        if(!s.pir_available && (s.motion || s.occupancy!=Occupancy::Unknown || (s.auto_flags && (p[0]<8 || p[82]<2)))) return false;
        if(s.led_only_bench && !s.physical_outputs_enabled) return false;
        s.temperature_status=(TemperatureStatus)p[48]; s.temperature_age_ms=wire_get32(p+52);
        s.temperature_deci_c=(int16_t)((uint16_t)p[44]<<8|p[45]); s.humidity_deci=(uint16_t)p[46]<<8|p[47];
        if(s.temperature_status==TemperatureStatus::Ok && !temperature_valid(s)) return false;
        if(s.temperature_status!=TemperatureStatus::Ok && (s.temperature_deci_c || s.humidity_deci)) return false;
    }
    s.room_id=p[1]; s.light_available=false; s.light_raw=false;
    if(p[0]>=7) {
        if(p[80]>1 || p[81]>1 || (!p[80] && p[81]) || (p[0]==7?p[82]!=0:p[82]>(p[0]==9?3:2)) || p[83] || (s.room_id==2 && p[80])) return false;
        s.light_available=p[80]; s.light_raw=p[81];
    }
    s.features_version=p[0]>=8?p[82]:p[0]>=6?1:0;
    for(int i=0;i<2;++i) {
        s.sensor_config[i]=SensorRuleConfig().packed(); s.override_ms[i]=0;
        s.sensor_demand[i]=s.winner[i]=s.blocking[i]=s.override_action[i]=0;
        if(p[0]>=8) {
            unsigned o=84+i*12; SensorRuleConfig config;
            if(!SensorRuleConfig::unpack(wire_get32(p+o),config) || wire_get32(p+o+4)>3600000 || p[o+8]>2 || p[o+9]>7 || p[o+10]>6 || p[o+11]>1 || (s.room_id==2 && i==0 && config.sensors_first)) return false;
            s.sensor_config[i]=config.packed(); s.override_ms[i]=wire_get32(p+o+4);
            s.sensor_demand[i]=p[o+8]; s.winner[i]=p[o+9]; s.blocking[i]=p[o+10]; s.override_action[i]=p[o+11];
        }
    }
    for(int i=0;i<2;++i) {
        s.schedule_ms[i]=s.energy_cap_ms[i]=0; s.schedule_action[i]=s.schedule_result[i]=s.control_source[i]=0;
        if(p[0]>=6) {
            unsigned o=56+i*12;
            if(p[o+8]>1 || p[o+9]>(p[0]>=8?5:4) || p[o+10]>(p[0]>=8?7:4) || p[o+11]) return false;
            s.schedule_ms[i]=wire_get32(p+o); s.energy_cap_ms[i]=wire_get32(p+o+4);
            if(s.schedule_ms[i]>86400000 || (s.energy_cap_ms[i] && (s.energy_cap_ms[i]<3000 || s.energy_cap_ms[i]>14400000))) return false;
            s.schedule_action[i]=p[o+8]; s.schedule_result[i]=p[o+9]; s.control_source[i]=p[o+10];
        }
    }
    s.light_age_ms=s.temperature_sample_age_ms=UINT32_MAX; s.sensor_snapshot_ms=s.evaluation_cycle=s.evaluation_started_us=0;
    s.sensor_order=0; s.evaluation_clock_valid=false;
    for(int i=0;i<2;++i) { s.rule_started_us[i]=s.rule_elapsed_us[i]=0; s.evaluation_step[i]=s.sensor_requested[i]=s.sensor_result[i]=0; }
    if(p[0]==9) {
        if(p[140]>1 || p[147]>1) return false;
        s.light_age_ms=wire_get32(p+108); s.sensor_snapshot_ms=wire_get32(p+112);
        s.evaluation_cycle=wire_get32(p+116); s.evaluation_started_us=wire_get32(p+120);
        s.sensor_order=p[140]; s.evaluation_clock_valid=p[147]; s.temperature_sample_age_ms=wire_get32(p+148);
        for(int i=0;i<2;++i) {
            if(p[141+i]>2 || p[143+i]>1 || p[145+i]>7) return false;
            s.rule_started_us[i]=wire_get32(p+124+i*4); s.rule_elapsed_us[i]=wire_get32(p+132+i*4);
            s.evaluation_step[i]=p[141+i]; s.sensor_requested[i]=p[143+i]; s.sensor_result[i]=p[145+i];
        }
        if(s.features_version==3 && (s.room_id!=1 || (s.evaluation_cycle &&
           (s.evaluation_step[s.sensor_order?1:0]!=1 || s.evaluation_step[s.sensor_order?0:1]!=2)))) return false;
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
