#pragma once
#include "state_wire.h"
#include <string.h>

enum class ControlOp : uint8_t { Device=1, Scene=2, Timer=3, Policy=4, Resume=5, Simulation=6 };
enum class ControlStatus : uint8_t { Applied=0, Denied=1, Invalid=2, Replay=3, WrongBoot=4, Disabled=5, Expired=6 };
struct ControlCommand {
    uint32_t boot=0, id=0, value=0;
    ControlOp op=ControlOp::Device;
    uint8_t device=0, action=0;
    uint32_t issued_ms=0,ttl_ms=3000;
};
inline bool parse_control_text(const char *text,ControlCommand &c) {
    size_t n=strlen(text); if(!n || n>45) return false;
    for(size_t i=0;i<n;++i) if((text[i]<'0'||text[i]>'9')&&text[i]!=' ') return false;
    uint32_t fields[4]={}; unsigned count=0; const char *p=text;
    while(*p) {
        while(*p==' ') ++p;
        if(!*p) break;
        if(count==4) return false;
        uint64_t value=0;
        while(*p>='0'&&*p<='9') {value=value*10+(*p++-'0');if(value>UINT32_MAX)return false;}
        fields[count++]=(uint32_t)value;
    }
    if(count!=4 || fields[0]<(unsigned)ControlOp::Device || fields[0]>(unsigned)ControlOp::Simulation ||
       fields[1]>(fields[0]==(unsigned)ControlOp::Simulation?7u:2u) || fields[2]>3) return false;
    c={};c.op=(ControlOp)fields[0];c.device=fields[1];c.action=fields[2];c.value=fields[3];return true;
}
static constexpr size_t kControlBytes=24, kAckBytes=40;
inline void encode_control(const ControlCommand &c, uint8_t *p) {
    wire_put32(p,c.boot); wire_put32(p+4,c.id); p[8]=(uint8_t)c.op;
    p[9]=c.device; p[10]=c.action; p[11]=0; wire_put32(p+12,c.value);
    wire_put32(p+16,c.issued_ms); wire_put32(p+20,c.ttl_ms);
}
inline bool decode_control(const uint8_t *p,size_t n,ControlCommand &c) {
    if (!p || n!=kControlBytes || p[11] || p[8]<1 || p[8]>6) return false;
    c.boot=wire_get32(p); c.id=wire_get32(p+4); c.op=(ControlOp)p[8];
    c.device=p[9]; c.action=p[10]; c.value=wire_get32(p+12);
    c.issued_ms=wire_get32(p+16); c.ttl_ms=wire_get32(p+20);
    return c.id!=0 && c.ttl_ms>=100 && c.ttl_ms<=5000;
}
inline bool control_fresh(const ControlCommand &c,uint32_t now) {
    int32_t age=(int32_t)(now-c.issued_ms);
    return c.ttl_ms>=100 && c.ttl_ms<=5000 && age>=-100 && age<=(int32_t)c.ttl_ms;
}
struct ControlAck {
    uint32_t boot=0,id=0,processing_us=0;
    ControlStatus status=ControlStatus::Invalid;
    bool light=false,fan=false;
    uint8_t mode=0;
    uint8_t request[kControlBytes]={}; // Bind ACK to the exact proposed action.
};
inline void encode_ack(const ControlAck &a,uint8_t *p) {
    wire_put32(p,a.boot); wire_put32(p+4,a.id); p[8]=(uint8_t)a.status;
    p[9]=a.light; p[10]=a.fan; p[11]=a.mode; wire_put32(p+12,a.processing_us);
    memcpy(p+16,a.request,kControlBytes);
}
inline bool decode_ack(const uint8_t *p,size_t n,ControlAck &a) {
    if(!p || n!=kAckBytes || p[8]>6 || p[9]>1 || p[10]>1 || p[11]>1) return false;
    a.boot=wire_get32(p); a.id=wire_get32(p+4); a.status=(ControlStatus)p[8];
    a.light=p[9]; a.fan=p[10]; a.mode=p[11]; a.processing_us=wire_get32(p+12);
    memcpy(a.request,p+16,kControlBytes); return true;
}

// One ordered controller stream per actuator boot. Duplicate payloads return
// the cached outcome; changed payloads with the same ID cannot re-execute.
class CommandGate {
public:
    uint32_t last=0;
    bool duplicate(const uint8_t *p) const { return last && memcmp(p,cached_,kControlBytes)==0; }
    bool next(uint32_t id) const { return id!=0 && last!=UINT32_MAX && id==last+1; }
    void commit(const uint8_t *p,uint32_t id) { memcpy(cached_,p,kControlBytes); last=id; }
private:
    uint8_t cached_[kControlBytes]={};
};
