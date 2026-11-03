#include "automation.h"
#include <assert.h>
#include <stdio.h>
static void reset() {
    safety_kernel_init();
    safety_kernel_register_device("bedroom","light",DEVICE_TYPE_MOCK,-1,false);
    safety_kernel_register_device("bedroom","fan",DEVICE_TYPE_MOCK,-1,false);
}
static ControlCommand cmd(ControlOp op,int d,int a,uint32_t value=0) { ControlCommand c; c.op=op;c.device=d;c.action=a;c.value=value;return c; }
int main() {
    unsigned checks=0;
    #define CHECK(x) do { if(!(x)) { fprintf(stderr,"line %d: %s\n",__LINE__,#x); return 1; } ++checks; } while(0)
    reset(); Automation a;
    CHECK(a.execute(cmd(ControlOp::Schedule,0,1,999),0,false)==ControlStatus::Invalid);
    CHECK(a.execute(cmd(ControlOp::Schedule,2,1,1000),0,false)==ControlStatus::Invalid);
    CHECK(a.execute(cmd(ControlOp::Schedule,0,3,0),0,false)==ControlStatus::Invalid);
    CHECK(a.execute(cmd(ControlOp::Schedule,0,2,1),0,false)==ControlStatus::Invalid);
    CHECK(a.execute(cmd(ControlOp::Energy,0,0,2999),0,false)==ControlStatus::Invalid);
    CHECK(a.execute(cmd(ControlOp::Energy,0,0,14400001),0,false)==ControlStatus::Invalid);
    CHECK(a.execute(cmd(ControlOp::Energy,0,1,60000),0,false)==ControlStatus::Invalid);
    CHECK(a.execute(cmd(ControlOp::Schedule,0,1,5000),1000,false)==ControlStatus::Applied);
    CHECK(a.schedule_remaining(0,1001)==4999);
    a.tick(5999,false,Occupancy::Unknown,false);CHECK(!safety_kernel_get_device(0)->is_on);
    a.tick(6000,false,Occupancy::Unknown,false);CHECK(safety_kernel_get_device(0)->is_on && a.schedule_result[0]==2 && a.source[0]==2);
    CHECK(a.schedule_remaining(0,6000)==0);
    CHECK(a.execute(cmd(ControlOp::Schedule,0,0,5000),6500,false)==ControlStatus::Applied);
    // Rejected manual request within dwell preserves scheduled mode and pending action.
    CHECK(a.execute(cmd(ControlOp::Device,0,0),6501,false)==ControlStatus::Denied);
    CHECK(a.schedule_remaining(0,6501)==4999);
    CHECK(a.execute(cmd(ControlOp::Device,0,0),10000,false)==ControlStatus::Applied);
    CHECK(a.schedule_remaining(0,10001)==0 && a.schedule_result[0]==4 && a.source[0]==1);
    a.tick(20000,false,Occupancy::Unknown,false);CHECK(!safety_kernel_get_device(0)->is_on);
    // Arming is explicit release from manual hold; devices remain independent.
    CHECK(a.execute(cmd(ControlOp::Schedule,0,1,5000),20000,false)==ControlStatus::Applied);
    CHECK(!safety_kernel_get_device(0)->manual_override);
    CHECK(a.execute(cmd(ControlOp::Schedule,1,1,5000),20000,false)==ControlStatus::Applied);
    a.tick(25000,false,Occupancy::Unknown,false);
    CHECK(safety_kernel_get_device(0)->is_on && safety_kernel_get_device(1)->is_on);
    CHECK(a.execute(cmd(ControlOp::Energy,0,0,3000),25000,false)==ControlStatus::Applied);
    a.tick(27999,false,Occupancy::Unknown,false);CHECK(safety_kernel_get_device(0)->is_on);
    a.tick(28000,false,Occupancy::Unknown,false);CHECK(!safety_kernel_get_device(0)->is_on && a.source[0]==3 && safety_kernel_get_device(0)->manual_override);
    CHECK(safety_kernel_get_device(1)->is_on);
    CHECK(a.execute(cmd(ControlOp::Energy,0,0,0),28001,false)==ControlStatus::Applied);
    CHECK(!safety_kernel_get_device(0)->is_on);
    // Reboot clears volatile configuration and never restores an old ON.
    reset(); Automation reboot; RoomReport report;reboot.report(report,0);
    CHECK(report.features_version==1 && report.energy_cap_ms[0]==0 && report.schedule_ms[0]==0 && !safety_kernel_get_device(0)->is_on);
    // Timer rollover: a delay crossing UINT32_MAX fires once.
    reset(); Automation wrap; uint32_t start=0xfffff000;
    CHECK(wrap.execute(cmd(ControlOp::Schedule,0,1,5000),start,false)==ControlStatus::Applied);
    wrap.tick(start+4999,false,Occupancy::Unknown,false);CHECK(!safety_kernel_get_device(0)->is_on);
    wrap.tick(start+5000,false,Occupancy::Unknown,false);CHECK(safety_kernel_get_device(0)->is_on);
    // A blocked ON is consumed, never retried later when dwell expires.
    reset(); Automation denied;
    CHECK(denied.execute(cmd(ControlOp::Device,0,1),10000,false)==ControlStatus::Applied);
    CHECK(denied.execute(cmd(ControlOp::Device,0,0),14000,false)==ControlStatus::Applied);
    CHECK(denied.execute(cmd(ControlOp::Schedule,0,1,1000),14000,false)==ControlStatus::Applied);
    denied.tick(15000,false,Occupancy::Unknown,false);CHECK(denied.schedule_result[0]==3 && !safety_kernel_get_device(0)->is_on);
    denied.tick(20000,false,Occupancy::Unknown,false);CHECK(!safety_kernel_get_device(0)->is_on);
    denied.away=true;CHECK(denied.execute(cmd(ControlOp::Schedule,0,1,1000),21000,false)==ControlStatus::Denied);
    // Wire round trip, legacy compatibility and fail-closed extension bytes.
    RoomReport r,d; denied.report(r,21000);uint8_t bytes[kRoomReportBytes];encode_room_report(r,bytes);
    CHECK(decode_room_report(bytes,sizeof(bytes),d) && d.features_version==1);
    bytes[67]=1;CHECK(!decode_room_report(bytes,sizeof(bytes),d));bytes[67]=0;
    bytes[64]=2;CHECK(!decode_room_report(bytes,sizeof(bytes),d));bytes[64]=0;
    bytes[0]=5;CHECK(decode_room_report(bytes,56,d) && d.features_version==0 && d.schedule_ms[0]==0);
    CHECK(!decode_room_report(bytes,sizeof(bytes),d));
    ControlCommand c;CHECK(parse_control_text("7 0 1 5000",c)); c.id=1; uint8_t cb[kControlBytes];encode_control(c,cb);CHECK(decode_control(cb,sizeof(cb),c));
    CHECK(parse_control_text("8 1 0 60000",c));CHECK(!parse_control_text("12 0 0 0",c));
    CHECK(parse_api_control("boot 9 7 0 1 5000",c) && c.boot==9);
    CHECK(!parse_api_control("7 0 1 5000",c));
    CHECK(!parse_api_control("boot 0 7 0 1 5000",c));
    CHECK(!parse_api_control("boot 4294967296 7 0 1 5000",c));
    CHECK(!parse_api_control("boot -1 7 0 1 5000",c));
    CHECK(parse_api_control("1 0 0 0",c));
    reset(); Automation priority;
    CHECK(priority.execute(cmd(ControlOp::Schedule,0,1,5000),1000,false)==ControlStatus::Applied);
    CHECK(priority.execute(cmd(ControlOp::Device,0,0),6000,false)==ControlStatus::Applied);
    priority.tick(6000,false,Occupancy::Unknown,false);CHECK(!safety_kernel_get_device(0)->is_on && priority.schedule_result[0]==4);
    CHECK(priority.execute(cmd(ControlOp::Schedule,0,1,5000),7000,false)==ControlStatus::Applied);
    priority.tick(13001,false,Occupancy::Unknown,false);CHECK(!safety_kernel_get_device(0)->is_on && priority.schedule_result[0]==3);
    CHECK(priority.execute(cmd(ControlOp::Schedule,0,1,5000),14000,false)==ControlStatus::Applied);
    CHECK(priority.execute(cmd(ControlOp::Schedule,1,1,5000),14000,false)==ControlStatus::Applied);
    CHECK(priority.execute(cmd(ControlOp::Schedule,0,0,6000),14000,false)==ControlStatus::Applied);
    CHECK(priority.execute(cmd(ControlOp::Schedule,0,2),14000,false)==ControlStatus::Applied);
    priority.tick(19000,false,Occupancy::Unknown,false);CHECK(!safety_kernel_get_device(0)->is_on && safety_kernel_get_device(1)->is_on);
    // Existing Timer3 is a fresh OFF countdown; Energy8 stays continuous-from-ON.
    reset(); Automation both;
    CHECK(both.execute(cmd(ControlOp::Device,0,1),10000,false)==ControlStatus::Applied);
    CHECK(both.execute(cmd(ControlOp::Energy,0,0,60000),20000,false)==ControlStatus::Applied);
    CHECK(both.execute(cmd(ControlOp::Timer,0,0,60000),30000,false)==ControlStatus::Applied);
    both.tick(69999,false,Occupancy::Unknown,false);CHECK(safety_kernel_get_device(0)->is_on);
    both.tick(70000,false,Occupancy::Unknown,false);CHECK(!safety_kernel_get_device(0)->is_on && both.source[0]==3);
    RoomReport simultaneous;both.report(simultaneous,70000);
    CHECK(simultaneous.energy_cap_ms[0]==60000 && simultaneous.timer_light_ms==20000);
    CHECK(both.execute(cmd(ControlOp::Timer,0,0,0),71000,false)==ControlStatus::Applied);
    both.report(simultaneous,71000);CHECK(!safety_kernel_get_device(0)->is_on && simultaneous.timer_light_ms==0 && simultaneous.energy_cap_ms[0]==60000);
    reset(); Automation earlierTimer;
    CHECK(earlierTimer.execute(cmd(ControlOp::Device,0,1),10000,false)==ControlStatus::Applied);
    CHECK(earlierTimer.execute(cmd(ControlOp::Energy,0,0,120000),15000,false)==ControlStatus::Applied);
    CHECK(earlierTimer.execute(cmd(ControlOp::Timer,0,0,60000),20000,false)==ControlStatus::Applied);
    earlierTimer.tick(79999,false,Occupancy::Unknown,false);CHECK(safety_kernel_get_device(0)->is_on);
    earlierTimer.tick(80000,false,Occupancy::Unknown,false);CHECK(!safety_kernel_get_device(0)->is_on);
    earlierTimer.report(simultaneous,80000);CHECK(simultaneous.energy_cap_ms[0]==120000 && simultaneous.timer_light_ms==0);
    reset(); Automation overdueCap;
    CHECK(overdueCap.execute(cmd(ControlOp::Device,0,1),10000,false)==ControlStatus::Applied);
    CHECK(overdueCap.execute(cmd(ControlOp::Timer,0,0,60000),50000,false)==ControlStatus::Applied);
    CHECK(overdueCap.execute(cmd(ControlOp::Energy,0,0,60000),70001,false)==ControlStatus::Applied);
    overdueCap.tick(70001,false,Occupancy::Unknown,false);CHECK(!safety_kernel_get_device(0)->is_on);
    overdueCap.report(simultaneous,70001);CHECK(simultaneous.timer_light_ms==39999 && simultaneous.energy_cap_ms[0]==60000);
    reset(); Automation cancelTimer;
    CHECK(cancelTimer.execute(cmd(ControlOp::Timer,0,0,60000),10000,false)==ControlStatus::Applied);
    CHECK(cancelTimer.execute(cmd(ControlOp::Timer,0,0,0),12000,false)==ControlStatus::Applied);
    cancelTimer.tick(90000,false,Occupancy::Unknown,false);CHECK(!safety_kernel_get_device(0)->is_on);
    CHECK(cancelTimer.execute(cmd(ControlOp::Device,0,1),100000,false)==ControlStatus::Applied);
    cancelTimer.tick(200000,false,Occupancy::Unknown,false);CHECK(safety_kernel_get_device(0)->is_on);
    printf("{\"source\":\"HOST_MEASURED\",\"assertions\":%u,\"passed\":true}\n",checks);
}
