#pragma once
#include "command_target.h"

struct RoomTransaction {
    ControlCommand command;
    ControlAck ack;
    uint32_t started=0,last_send=0,start_us=0,rtt_us=0;
    uint8_t phase=0,attempts=0;
};
struct PhysicalRoomLink {
    RoomReport report;
    RoomTransaction transaction;
    ProposalStore proposal;
    uint32_t received=0,sequence=0,last_control=0;
    bool have=false;
    bool fresh(uint32_t now) const { return have && (uint32_t)(now-received)<3000; }
};
// Caller serializes access. No shared boot/request/replay state between rooms.
class RoomGateway {
public:
    PhysicalRoomLink rooms[2];
    static uint16_t node(unsigned i) { return i==0?1:3; }
    static int index(uint16_t node_id) { return node_id==1?0:node_id==3?1:-1; }
    bool report(uint16_t sender,const RoomReport &value,uint32_t seq,uint32_t now) {
        int i=index(sender);
#if defined(CLAWGRID_ROOM1_DEMO_ONLY)
        if(i!=0) return false;
#endif
        if(i<0 || value.room_id!=i+1) return false;
        auto &r=rooms[i];
        if(r.have && (value.boot_id<r.report.boot_id ||
            (value.boot_id==r.report.boot_id && !sequence_newer(seq,r.sequence)))) return false;
        if(!room_report_advances(value,r.report)) return false;
        if(r.have && value.boot_id!=r.report.boot_id) {
            r.last_control=0; // HTTP owns proposals; confirmation checks the new boot.
            if(r.transaction.phase==1) r.transaction.phase=3;
        }
        r.report=value; r.sequence=seq; r.received=now; r.have=true; return true;
    }
    bool queue(unsigned i,ControlCommand c,uint32_t now,uint32_t us,uint32_t &id) {
        if(i>1) return false;
#if defined(CLAWGRID_ROOM1_DEMO_ONLY)
        if(i!=0) return false;
#endif
        if(c.op==ControlOp::SensorOrder && (i!=0 || !c.boot || rooms[i].report.features_version!=3)) return false;
        auto &r=rooms[i];
        if(((c.op==ControlOp::SensorConfig || c.op==ControlOp::DeviceMode) && (!c.boot || r.report.features_version<2)) || !r.fresh(now) || r.transaction.phase==1 || (c.boot && c.boot!=r.report.boot_id)) return false;
        uint32_t last=r.last_control>r.report.last_command?r.last_control:r.report.last_command;
        if(last==UINT32_MAX) return false;
        c.boot=r.report.boot_id; c.id=last+1; c.issued_ms=r.report.uptime_ms+(now-r.received); c.ttl_ms=3000;
        r.transaction={}; r.transaction.command=c; r.transaction.phase=1;
        r.transaction.started=now; r.transaction.start_us=us; id=c.id; return true;
    }
    bool ack(uint16_t sender,const ControlAck &a,uint32_t now,uint32_t us) {
        int i=index(sender); if(i<0) return false;
        auto &r=rooms[i]; auto &t=r.transaction;
        uint8_t expected[kControlBytes]; encode_control(t.command,expected);
        if(!r.fresh(now) || t.phase!=1 || a.boot!=t.command.boot || a.id!=t.command.id ||
            memcmp(a.request,expected,sizeof(expected))) return false;
        t.ack=a; t.phase=2; t.rtt_us=us-t.start_us;
        if(a.status!=ControlStatus::WrongBoot && a.status!=ControlStatus::Replay) r.last_control=a.id;
        return true;
    }
};
