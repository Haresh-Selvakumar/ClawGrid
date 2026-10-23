#pragma once
#include "control_wire.h"
#include "safety_kernel.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

enum class TargetMode : uint8_t { PhysicalBedroom, VirtualRooms };
enum class TargetRoom : uint8_t { Bedroom, LivingRoom, Kitchen, Study };
enum class TargetDevice : uint8_t { Light, Fan };
enum class TargetAction : uint8_t { Off=ACTION_OFF, On=ACTION_ON, Toggle=ACTION_TOGGLE };
struct CommandTarget {
    TargetMode mode=TargetMode::VirtualRooms;
    TargetRoom room=TargetRoom::Bedroom;
    TargetDevice device=TargetDevice::Light;
    TargetAction action=TargetAction::Off;
};
inline const char *target_mode(TargetMode m) { return m==TargetMode::PhysicalBedroom?"physical_bedroom":"virtual_rooms"; }
inline const char *target_room(TargetRoom r) { const char *v[]={"bedroom","living_room","kitchen","study"}; return (unsigned)r<4?v[(unsigned)r]:"invalid"; }
inline const char *target_device(TargetDevice d) { return d==TargetDevice::Light?"light":"fan"; }
inline const char *target_action(TargetAction a) { const char *v[]={"OFF","ON","TOGGLE"}; return (unsigned)a<3?v[(unsigned)a]:"invalid"; }
inline bool target_valid(const CommandTarget &t) {
    return (unsigned)t.mode<2 && (unsigned)t.room<4 && (unsigned)t.device<2 && (unsigned)t.action<3 &&
        (t.mode!=TargetMode::PhysicalBedroom || t.room==TargetRoom::Bedroom);
}
inline bool same_target(const CommandTarget &a,const CommandTarget &b) {
    return a.mode==b.mode && a.room==b.room && a.device==b.device && a.action==b.action;
}
inline bool parse_target(const char *mode,const char *room,const char *device,const char *action,CommandTarget &out) {
    CommandTarget t;
    if(!strcmp(mode,"physical_bedroom")) t.mode=TargetMode::PhysicalBedroom;
    else if(strcmp(mode,"virtual_rooms")) return false;
    bool found=false;
    for(unsigned i=0;i<4;++i) if(!strcmp(room,target_room((TargetRoom)i))) { t.room=(TargetRoom)i; found=true; }
    if(!found) return false;
    if(!strcmp(device,"light")) t.device=TargetDevice::Light;
    else if(!strcmp(device,"fan")) t.device=TargetDevice::Fan; else return false;
    if(!strcmp(action,"ON")) t.action=TargetAction::On;
    else if(!strcmp(action,"OFF")) t.action=TargetAction::Off;
    else if(!strcmp(action,"TOGGLE")) t.action=TargetAction::Toggle; else return false;
    if(!target_valid(t)) return false;
    out=t; return true;
}
inline bool target_command(const CommandTarget &t,ControlCommand &c) {
    if(!target_valid(t)) return false;
    c={}; c.op=t.mode==TargetMode::PhysicalBedroom?ControlOp::Device:ControlOp::Simulation;
    c.device=(uint8_t)t.device+(t.mode==TargetMode::VirtualRooms?2*(uint8_t)t.room:0);
    c.action=(uint8_t)t.action; return true;
}
inline bool speech_target(uint8_t command,TargetMode mode,CommandTarget &t) {
    if(command<1 || command>16) return false;
    unsigned index=command-1;
    t.mode=mode; t.room=(TargetRoom)(index/4); t.device=(TargetDevice)((index/2)%2);
    t.action=index%2?TargetAction::Off:TargetAction::On;
    return target_valid(t);
}
inline bool parse_number(const char *s,uint32_t &n) {
    if(!s || !*s || strlen(s)>10) return false;
    uint64_t v=0; for(;*s;++s) { if(*s<'0'||*s>'9') return false; v=v*10+(*s-'0'); if(v>UINT32_MAX) return false; }
    n=(uint32_t)v; return true;
}
// Shared strict wire parser: source/id + the complete immutable target + job.
inline bool proposal_fields(const char *body,char (&first)[24],CommandTarget &t,uint32_t &job,bool confirm=false) {
    char mode[24],room[24],device[16],action[16],number[16],extra;
    int n=confirm?sscanf(body,"%23s %23s %23s %15s %15s %c",first,mode,room,device,action,&extra):
        sscanf(body,"%23s %23s %23s %15s %15s %15s %c",first,mode,room,device,action,number,&extra);
    if(n!=(confirm?5:6) || !parse_target(mode,room,device,action,t)) return false;
    return confirm?parse_number(first,job):parse_number(number,job);
}
enum class ProposalResult : uint8_t { Ok, Invalid, Unavailable, Mismatch, Expired, Consumed };
inline const char *proposal_error(ProposalResult r) {
    switch(r) {
        case ProposalResult::Unavailable:return "Controller unavailable or stale";
        case ProposalResult::Mismatch:return "Proposal or target mismatch";
        case ProposalResult::Expired:return "Proposal expired; prepare again";
        case ProposalResult::Consumed:return "Proposal already submitted or missing";
        default:return "Invalid target or proposal";
    }
}
class ProposalStore {
public:
    uint32_t id=0,boot=0,created=0,job=0;
    CommandTarget target; bool active=false,speech=false;
    void seed(uint32_t n) { next_=n?n:1; active=false; }
    ProposalResult prepare(const CommandTarget &t,uint32_t b,bool fresh,uint32_t now,bool from_speech,uint32_t source_job) {
        active=false;
        if(!target_valid(t) || (from_speech && !source_job)) return ProposalResult::Invalid;
        if(!fresh || !b) return ProposalResult::Unavailable;
        if(next_==UINT32_MAX) return ProposalResult::Invalid;
        id=++next_; target=t; boot=b; created=now; speech=from_speech; job=source_job; active=true;
        return ProposalResult::Ok;
    }
    ProposalResult confirm(uint32_t requested,const CommandTarget &t,uint32_t b,bool fresh,uint32_t now,ControlCommand &c) const {
        if(!active) return ProposalResult::Consumed;
        if(requested!=id || !same_target(t,target)) return ProposalResult::Mismatch;
        if((uint32_t)(now-created)>=15000) return ProposalResult::Expired;
        if(!fresh || b!=boot) return ProposalResult::Unavailable;
        if(!target_command(target,c)) return ProposalResult::Invalid;
        c.boot=boot; return ProposalResult::Ok;
    }
    void consume() { active=false; }
private:
    uint32_t next_=1;
};
