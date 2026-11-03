#pragma once
#include <stdint.h>
#include <stddef.h>
#include <string.h>
#include "sensor_rules.h"

// Explicit bytes: no compiler padding or host endianness is stored in flash.
struct PolicyConfig {
    bool away=false, light_auto=true, fan_auto=false;
    uint32_t grace_ms=120000;
    uint32_t sensor_config[2]={SensorRuleConfig().packed(),SensorRuleConfig().packed()};
    bool temperature_first=false;
    uint8_t manual_flags=0; // Stored under a separate backward-compatible NVS key.
    bool operator==(const PolicyConfig &b) const {
        return away==b.away && light_auto==b.light_auto && fan_auto==b.fan_auto && grace_ms==b.grace_ms && manual_flags==b.manual_flags && sensor_config[0]==b.sensor_config[0] && sensor_config[1]==b.sensor_config[1] && temperature_first==b.temperature_first;
    }
    bool operator!=(const PolicyConfig &b) const { return !(*this==b); }
};
static constexpr size_t kPolicyRecordBytes=16;
inline uint32_t policy_crc(const uint8_t *p,size_t n) {
    uint32_t crc=~0u;
    while(n--) { crc^=*p++; for(int bit=0;bit<8;++bit) crc=(crc>>1)^(0xedb88320u & (0u-(crc&1))); }
    return ~crc;
}
inline void policy_put32(uint8_t *p,uint32_t value) {
    for(int i=0;i<4;++i) p[i]=(uint8_t)(value>>(24-i*8));
}
inline uint32_t policy_get32(const uint8_t *p) {
    return (uint32_t)p[0]<<24|(uint32_t)p[1]<<16|(uint32_t)p[2]<<8|p[3];
}
inline bool valid_policy(const PolicyConfig &c) { return c.grace_ms>=30000 && c.grace_ms<=1800000 && c.manual_flags<=3; }
inline void encode_policy(const PolicyConfig &c,uint8_t *p) {
    memset(p,0,kPolicyRecordBytes); memcpy(p,"CGP",3); p[3]=1;
    p[4]=c.away|(c.light_auto<<1)|(c.fan_auto<<2); policy_put32(p+8,c.grace_ms);
    policy_put32(p+12,policy_crc(p,12));
}
inline bool decode_policy(const uint8_t *p,size_t n,PolicyConfig &out) {
    if(!p || n!=kPolicyRecordBytes || memcmp(p,"CGP",3) || p[3]!=1 ||
       p[4]>7 || p[5] || p[6] || p[7] || policy_get32(p+12)!=policy_crc(p,12)) return false;
    PolicyConfig c; c.away=p[4]&1; c.light_auto=p[4]&2; c.fan_auto=p[4]&4;
    c.grace_ms=policy_get32(p+8); if(!valid_policy(c)) return false;
    out=c; return true;
}
static constexpr size_t kManualRecordBytes=12;
inline void encode_manual_policy(uint8_t flags,uint8_t *p) {
    memset(p,0,kManualRecordBytes); memcpy(p,"CGM",3); p[3]=1; p[4]=flags;
    policy_put32(p+8,policy_crc(p,8));
}
inline bool decode_manual_policy(const uint8_t *p,size_t n,uint8_t &flags) {
    if(!p || n!=kManualRecordBytes || memcmp(p,"CGM",3) || p[3]!=1 || p[4]>3 || p[5] || p[6] || p[7] || policy_get32(p+8)!=policy_crc(p,8)) return false;
    flags=p[4]; return true;
}

// New optional key; original policy/manual blobs and all provisioning retained.
static constexpr size_t kSensorPolicyBytes=16;
inline void encode_sensor_policy(const PolicyConfig &c,uint8_t *p) {
    memset(p,0,kSensorPolicyBytes); memcpy(p,"CGA",3); p[3]=1;
    policy_put32(p+4,c.sensor_config[0]); policy_put32(p+8,c.sensor_config[1]);
    policy_put32(p+12,policy_crc(p,12));
}
inline bool decode_sensor_policy(const uint8_t *p,size_t n,PolicyConfig &c) {
    SensorRuleConfig a,b;
    if(n!=kSensorPolicyBytes || memcmp(p,"CGA",3) || p[3]!=1 || policy_get32(p+12)!=policy_crc(p,12) ||
       !SensorRuleConfig::unpack(policy_get32(p+4),a) || !SensorRuleConfig::unpack(policy_get32(p+8),b)) return false;
    c.sensor_config[0]=a.packed(); c.sensor_config[1]=b.packed(); return true;
}

static constexpr size_t kSensorOrderBytes=12;
inline void encode_sensor_order(bool temperature_first,uint8_t *p) {
    memset(p,0,kSensorOrderBytes); memcpy(p,"CGO",3); p[3]=1; p[4]=temperature_first;
    policy_put32(p+8,policy_crc(p,8));
}
inline bool decode_sensor_order(const uint8_t *p,size_t n,bool &out) {
    if(!p || n!=kSensorOrderBytes || memcmp(p,"CGO",3) || p[3]!=1 || p[4]>1 || p[5] || p[6] || p[7] || policy_get32(p+8)!=policy_crc(p,8)) return false;
    out=p[4]; return true;
}
