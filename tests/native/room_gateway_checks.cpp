#include "room_gateway.h"
#include "digital_light.h"
#include "automation.h"
#include "bridge_path.h"
#include <stdio.h>
#define CHECK(x) do { if(!(x)) { fprintf(stderr,"line %d: %s\n",__LINE__,#x); return 1; } } while(0)
int main() {
    CHECK(bridge_path_valid("/api/state?room=2"));
    CHECK(bridge_path_valid("/api/transaction?room=1"));
    CHECK(bridge_path_valid("/api/state"));
    CHECK(!bridge_path_valid("/api/state?room=3"));
    CHECK(!bridge_path_valid("/api/state?room=2&room=1"));
    CHECK(!bridge_path_valid("/api/state?room=2%00"));
    RoomGateway gateway;
    RoomReport a,c; a.room_id=1; c.room_id=2;
    a.boot_id=c.boot_id=7; a.uptime_ms=c.uptime_ms=100;
    CHECK(gateway.report(1,a,1,100)); CHECK(gateway.report(3,c,1,100));
    CHECK(!gateway.report(3,a,2,101)); // Authenticated node identity must match room schema.
    CHECK(!gateway.report(2,c,2,101));
    CHECK(!gateway.report(1,a,1,200)); CHECK(gateway.rooms[0].received==100);
    ControlCommand cmd; cmd.op=ControlOp::Device; cmd.action=1;
    uint32_t id=0; CHECK(gateway.queue(0,cmd,110,110000,id) && id==1);
    ControlAck ack; ack.boot=7; ack.id=1;
    encode_control(gateway.rooms[0].transaction.command,ack.request);
    CHECK(!gateway.ack(3,ack,120,120000));
    CHECK(gateway.rooms[0].transaction.phase==1 && gateway.rooms[1].transaction.phase==0);
    CHECK(gateway.ack(1,ack,120,120000));
    CHECK(!gateway.ack(1,ack,121,121000));
    CHECK(gateway.queue(1,cmd,130,130000,id) && id==1);
    CHECK(!gateway.queue(0,cmd,3100,3100000,id)); // One room stale independently.
    c.uptime_ms=3100; CHECK(gateway.report(3,c,2,3100));
    CHECK(gateway.rooms[1].fresh(3200) && !gateway.rooms[0].fresh(3200));
    c.boot_id=8;c.uptime_ms=10; CHECK(gateway.report(3,c,1,3210));
    CHECK(gateway.rooms[1].transaction.phase==3);
    CHECK(gateway.rooms[0].transaction.phase==2 && gateway.rooms[0].report.boot_id==7);
    CHECK(!gateway.ack(3,ack,3220,3220000));
    c.boot_id=7;c.uptime_ms=5000; CHECK(!gateway.report(3,c,3,3230));
    CommandTarget target; CHECK(parse_target("physical_bedroom","living_room","fan","OFF",target));
    CHECK(!parse_target("physical_bedroom","kitchen","light","ON",target));
    DigitalLightFilter light; CHECK(!light.available(0));
    light.sample(true,0);light.sample(false,49);light.sample(true,50);CHECK(!light.available(99));
    light.sample(true,100);CHECK(light.available(100) && light.raw());
    CHECK(!light.available(1100));
    RoomReport decoded; uint8_t wire[kRoomReportBytes];
    a.pir_available=false;a.auto_flags=0;a.light_available=true;a.light_raw=true;
    encode_room_report(a,wire);CHECK(decode_room_report(wire,sizeof(wire),decoded) && decoded.light_available && decoded.light_raw);
    a.room_id=2;encode_room_report(a,wire);CHECK(!decode_room_report(wire,sizeof(wire),decoded));
    a.light_available=false;a.light_raw=false;encode_room_report(a,wire);
    CHECK(decode_room_report(wire,sizeof(wire),decoded) && !decoded.light_available);
    safety_kernel_init();
    safety_kernel_register_device("bedroom","light",DEVICE_TYPE_LIGHT,-1,false);
    safety_kernel_register_device("bedroom","fan",DEVICE_TYPE_FAN,-1,false);
    Automation room1,room2;
    ControlCommand cap;cap.op=ControlOp::Energy;cap.device=0;cap.value=60000;
    CHECK(room1.execute(cap,100,false)==ControlStatus::Applied);
    ControlCommand schedule; schedule.op=ControlOp::Schedule;schedule.device=1;schedule.action=1;schedule.value=5000;
    CHECK(room2.execute(schedule,100,false)==ControlStatus::Applied);
    RoomReport one,two;room1.report(one,100);room2.report(two,100);
    CHECK(one.energy_cap_ms[0]==60000 && two.energy_cap_ms[0]==0);
    CHECK(one.schedule_ms[1]==0 && two.schedule_ms[1]==5000);
    schedule.action=2;schedule.value=0;CHECK(room2.execute(schedule,200,false)==ControlStatus::Applied);
    room1.report(one,200);room2.report(two,200);
    CHECK(two.schedule_ms[1]==0 && two.schedule_result[1]==4 && one.energy_cap_ms[0]==60000);
    puts("{\"source\":\"HOST_MEASURED\",\"room_isolation\":true,\"passed\":true}");
}
