#pragma once
#include <stdint.h>
#include <stddef.h>

enum class TemperatureStatus : uint8_t { NotReady=0, Ok=1, Timeout=2, Checksum=3, Invalid=4, DriverError=5, Stale=6 };
struct TemperatureSample {
    TemperatureStatus status=TemperatureStatus::NotReady;
    int16_t deci_c=0;
    uint16_t humidity_deci=0;
    uint32_t sampled_ms=0;
    uint16_t pulse_count=0;
};
struct DhtPulse { uint16_t us; bool high; };

// DHT11 only; no DHT22 auto-detection. Invalid frames never expose old readings.
inline TemperatureSample decode_dht11(const DhtPulse *p,size_t count,uint32_t now) {
    TemperatureSample s; s.sampled_ms=now; s.status=TemperatureStatus::Invalid;
    if(!p || count<82 || count>100) return s;
    s.pulse_count=(uint16_t)count;
    size_t start=count;
    // RX starts immediately before host releases DATA. A short leading host
    // pulse may precede the sensor's LOW/HIGH acknowledgement.
    for(size_t i=0;i<6 && i+1<count;++i) {
        if(!p[i].high && p[i].us>=65 && p[i].us<=110 && p[i+1].high && p[i+1].us>=65 && p[i+1].us<=110) { start=i+2; break; }
    }
    if(start==count || count<start+80 || count>start+83) return s;
    uint8_t bytes[5]={};
    for(size_t bit=0;bit<40;++bit) {
        const auto &lo=p[start+2*bit],&hi=p[start+2*bit+1];
        if(lo.high || !hi.high || lo.us<30 || lo.us>80 || hi.us<15 || hi.us>100) return s;
        // The datasheet separates ~26-28us zero from ~70us one; the gap is invalid.
        if(hi.us>45 && hi.us<55) return s;
        bytes[bit/8]=(uint8_t)((bytes[bit/8]<<1)|(hi.us>=55));
    }
    if((uint8_t)(bytes[0]+bytes[1]+bytes[2]+bytes[3])!=bytes[4]) { s.status=TemperatureStatus::Checksum; return s; }
    // Conservative range common to the photographed DHT11 module revisions.
    if(bytes[0]>100 || bytes[1]>9 || bytes[2]>50 || bytes[3]>9 ||
       (bytes[0]==100 && bytes[1]) || (bytes[2]==50 && bytes[3])) return s;
    s.status=TemperatureStatus::Ok; s.deci_c=bytes[2]*10+bytes[3]; s.humidity_deci=bytes[0]*10+bytes[1];
    return s;
}
inline const char *temperature_status_name(TemperatureStatus s) {
    const char *names[]={"NOT_READY","OK","TIMEOUT","CHECKSUM","INVALID","DRIVER_ERROR","STALE"};
    return (unsigned)s<7?names[(unsigned)s]:"INVALID";
}
