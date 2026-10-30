#pragma once
#include "state_wire.h"

inline size_t dht_fixture(DhtPulse (&p)[100],bool prefix=true) {
    const uint8_t bytes[]={60,0,25,0,85}; size_t n=0;
    if(prefix) { p[n++]={5,false}; p[n++]={25,true}; }
    p[n++]={80,false}; p[n++]={80,true};
    for(int b=0;b<40;++b) { p[n++]={50,false}; p[n++]={(uint16_t)((bytes[b/8]&(1<<(7-b%8)))?70:27),true}; }
    p[n++]={50,false}; return n;
}
inline bool dht_decode_test() {
    DhtPulse p[100]; auto n=dht_fixture(p);
    auto s=decode_dht11(p,n,1000);
    if(s.status!=TemperatureStatus::Ok || s.deci_c!=250 || s.humidity_deci!=600 || s.sampled_ms!=1000) return false;
    n=dht_fixture(p,false); return decode_dht11(p,n,1000).status==TemperatureStatus::Ok;
}
inline bool dht_invalid_test() {
    DhtPulse p[100]; auto n=dht_fixture(p);
    p[5].us=p[5].us==70?27:70;
    if(decode_dht11(p,n,0).status!=TemperatureStatus::Checksum) return false;
    n=dht_fixture(p); p[5].us=50;
    if(decode_dht11(p,n,0).status!=TemperatureStatus::Invalid) return false;
    n=dht_fixture(p); p[6].high=true;
    if(decode_dht11(p,n,0).status!=TemperatureStatus::Invalid) return false;
    n=dht_fixture(p); p[2].us=200;
    if(decode_dht11(p,n,0).status!=TemperatureStatus::Invalid) return false;
    return decode_dht11(nullptr,0,0).status==TemperatureStatus::Invalid &&
        decode_dht11(p,81,0).status==TemperatureStatus::Invalid && decode_dht11(p,101,0).status==TemperatureStatus::Invalid;
}
inline bool dht_wire_test() {
    RoomReport r,d; r.temperature_status=TemperatureStatus::Ok; r.temperature_deci_c=250; r.humidity_deci=600; r.temperature_age_ms=2500;
    uint8_t p[kRoomReportBytes]; encode_room_report(r,p);
    if(!decode_room_report(p,sizeof(p),d) || !temperature_valid(d) || d.temperature_deci_c!=250 || d.humidity_deci!=600 || d.temperature_age_ms!=2500) return false;
    p[49]=1; if(decode_room_report(p,sizeof(p),d)) return false; p[49]=2;
    p[48]=7; if(decode_room_report(p,sizeof(p),d)) return false; p[48]=1;
    wire_put32(p+52,10000); if(decode_room_report(p,sizeof(p),d)) return false;
    wire_put32(p+52,2500); p[0]=4; p[49]=0;
    if(!decode_room_report(p,sizeof(p),d) || d.control_policy_version!=1 || !temperature_valid(d)) return false;
    // Schema3 remains readable during B-first rollout; no old/new reading leaks.
    p[0]=3; return decode_room_report(p,44,d) && d.temperature_status==TemperatureStatus::NotReady && d.temperature_age_ms==UINT32_MAX && d.temperature_deci_c==0;
}
inline bool dht_age_test() {
    TemperatureSample s; s.status=TemperatureStatus::Ok; s.deci_c=250; s.humidity_deci=600; s.sampled_ms=0xffffff00;
    RoomReport r; report_temperature(r,s,0x100);
    if(!temperature_valid(r) || r.temperature_age_ms!=512) return false;
    RoomReport stale; report_temperature(stale,s,s.sampled_ms+10000);
    if(stale.temperature_status!=TemperatureStatus::Stale || temperature_valid(stale) || stale.temperature_deci_c) return false;
    TemperatureSample failed; failed.status=TemperatureStatus::Checksum; RoomReport invalid; report_temperature(invalid,failed,10);
    return !temperature_valid(invalid) && invalid.temperature_status==TemperatureStatus::Checksum && !invalid.temperature_deci_c;
}
