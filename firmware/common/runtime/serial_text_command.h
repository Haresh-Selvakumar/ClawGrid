#pragma once
#include "command_target.h"

// Bounded text adapter for the submitted USB relay sketch. It proposes a target;
// it never writes GPIO or sends a radio packet. No substring matching is used.
struct SerialTextCommand {
    CommandTarget target;
    bool requested_hall=false, bench_alias=false, acoustic_fallback=false;
};
inline bool parse_serial_text(const char *body,SerialTextCommand &out) {
    if(!body || !*body || strlen(body)>127) return false;
    const char *p=body;
    char scope[24]={}; unsigned used=0;
    while(*p && *p!=' ') {
        if(used+1>=sizeof(scope)) return false;
        scope[used++]=*p++;
    }
    bool physical=!strcmp(scope,"physical_bedroom"), bench=!strcmp(scope,"bench_pair");
    if(!physical && !bench && strcmp(scope,"virtual_rooms")) return false;
    char words[20][16]={}; unsigned count=0;
    while(*p) {
        while(*p==' ') ++p;
        if(!*p) break;
        if(count>=20) return false;
        used=0;
        while(*p && *p!=' ') {
            unsigned char c=(unsigned char)*p++;
            if(c>='A' && c<='Z') c=(unsigned char)(c+'a'-'A');
            if(c<'a' || c>'z' || used+1>=sizeof(words[0])) return false;
            words[count][used++]=(char)c;
        }
        ++count;
    }
    int room=-1,device=-1,action=-1; bool fallback=false;
    for(unsigned i=0;i<count;++i) {
        const char *w=words[i]; int r=-1,d=-1,a=-1;
        if(!strcmp(w,"bedroom")) r=0;
        else if(!strcmp(w,"bed")) {
            if(i+1>=count || strcmp(words[++i],"room")) return false;
            r=0;
        } else if(!strcmp(w,"hall") || !strcmp(w,"living") || !strcmp(w,"all")) {
            r=1; fallback|=!strcmp(w,"all");
            if(!strcmp(w,"living") && i+1<count && !strcmp(words[i+1],"room")) ++i;
        } else if(!strcmp(w,"light") || !strcmp(w,"lights") || !strcmp(w,"lamp") || !strcmp(w,"bulb")) d=0;
        else if(!strcmp(w,"fan")) d=1;
        else if(!strcmp(w,"pan") || !strcmp(w,"fun")) { d=1; fallback=true; }
        else if(!strcmp(w,"on") || !strcmp(w,"onn")) { a=1; fallback|=!strcmp(w,"onn"); }
        else if(!strcmp(w,"off") || !strcmp(w,"of")) { a=0; fallback|=!strcmp(w,"of"); }
        else if(strcmp(w,"turn") && strcmp(w,"switch") && strcmp(w,"set") &&
                strcmp(w,"please") && strcmp(w,"the") && strcmp(w,"in") &&
                strcmp(w,"at") && strcmp(w,"to")) return false;
        if(r>=0) { if(room>=0) return false; room=r; }
        if(d>=0) { if(device>=0) return false; device=d; }
        if(a>=0) { if(action>=0) return false; action=a; }
    }
    if(room<0 || device<0 || action<0 || (physical && room!=0)) return false;
    SerialTextCommand candidate;
    candidate.requested_hall=room==1; candidate.bench_alias=bench && room==1;
    candidate.acoustic_fallback=fallback;
    candidate.target.mode=physical||bench?TargetMode::PhysicalBedroom:TargetMode::VirtualRooms;
    candidate.target.room=physical||bench?TargetRoom::Bedroom:room==0?TargetRoom::Bedroom:TargetRoom::LivingRoom;
    candidate.target.device=(TargetDevice)device;
    candidate.target.action=action?TargetAction::On:TargetAction::Off;
    if(!target_valid(candidate.target)) return false;
    out=candidate; return true;
}
