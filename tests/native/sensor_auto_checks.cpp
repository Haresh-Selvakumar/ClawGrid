#include "automation.h"
#include "room_gateway.h"
#include <stdio.h>
static unsigned checks=0;
#define CHECK(x) do { ++checks; if(!(x)) { fprintf(stderr,"line %d: %s\n",__LINE__,#x); return 1; } } while(0)
static void reset() {
    safety_kernel_init();
    safety_kernel_register_device("bedroom","light",DEVICE_TYPE_MOCK,-1,false);
    safety_kernel_register_device("bedroom","fan",DEVICE_TYPE_MOCK,-1,false);
}
static ControlCommand cmd(ControlOp op,int d,int a,uint32_t v=0) { ControlCommand c; c.op=op;c.device=d;c.action=a;c.value=v;return c; }
static RoomReport input(int temperature,bool dark=true,bool valid=true) {
    RoomReport r; r.temperature_status=valid?TemperatureStatus::Ok:TemperatureStatus::Invalid;
    r.temperature_deci_c=temperature;r.temperature_age_ms=0;r.humidity_deci=600;
    r.light_available=true;r.light_age_ms=0;r.light_raw=dark;return r;
}
static void tick(Automation &a,uint32_t at,int temp,bool dark=true,bool valid=true) {
    a.sensor_inputs(input(temp,dark,valid)); a.tick(at,false,Occupancy::Unknown,false);
}
static bool on(int d) { return safety_kernel_get_device(d)->is_on; }
int main() {
    SensorRuleConfig cfg,decoded;
    CHECK(SensorRuleConfig::unpack(cfg.packed(),decoded) && decoded.on==280 && decoded.off==260);
    CHECK(!SensorRuleConfig::unpack(0,decoded));CHECK(!SensorRuleConfig::unpack(cfg.packed()|0x80000000u,decoded));
    reset();Automation a;a.enable_sensor_rules(true);PolicyConfig saved;saved.light_auto=saved.fan_auto=true;
    a.restore_policy(saved);tick(a,10000,300);CHECK(!on(0)&&!on(1)); // Reboot never opts in.
    RoomReport r;a.report(r,10000);CHECK(r.auto_flags==0 && r.manual_flags==3 && r.features_version==3);
    CHECK(a.execute(cmd(ControlOp::SensorConfig,1,0,cfg.packed()),10000,false)==ControlStatus::Applied);
    tick(a,11000,300);CHECK(!on(1)); // Editing config cannot enable AUTO.
    a.sensor_inputs(input(270));
    CHECK(a.execute(cmd(ControlOp::DeviceMode,1,1),11000,false)==ControlStatus::Applied);
    tick(a,11005,270);CHECK(!on(1)); // Initial band demand is conservative OFF.
    tick(a,12000,280);CHECK(on(1));tick(a,16000,270);CHECK(on(1));
    tick(a,17000,260);CHECK(!on(1));tick(a,21000,270);CHECK(!on(1));
    // Schedule-first conflict and independent sensor hysteresis memory.
    CHECK(a.execute(cmd(ControlOp::Schedule,1,1,1000),21000,false)==ControlStatus::Applied);
    tick(a,22000,270);CHECK(on(1));a.report(r,22000);CHECK(r.override_ms[1]==60000 && r.winner[1]==2 && r.sensor_demand[1]==1);
    tick(a,30000,270);a.report(r,30000);CHECK(r.override_ms[1]==52000 && r.sensor_demand[1]==1);
    // Config changes and repeated telemetry do not renew the existing lease.
    cfg.lease_ms=3000;CHECK(a.execute(cmd(ControlOp::SensorConfig,1,0,cfg.packed()),30000,false)==ControlStatus::Applied);
    a.report(r,30000);CHECK(r.override_ms[1]==52000);
    tick(a,81999,270);CHECK(on(1));tick(a,82000,270);CHECK(!on(1));a.report(r,82000);CHECK(!r.override_ms[1] && r.winner[1]==5);
    // Sensors-first consumes but exposes a conflicting schedule as overridden.
    cfg.sensors_first=true;CHECK(a.execute(cmd(ControlOp::SensorConfig,1,0,cfg.packed()),83000,false)==ControlStatus::Applied);
    CHECK(a.execute(cmd(ControlOp::Schedule,1,1,1000),83000,false)==ControlStatus::Applied);
    tick(a,84000,250);CHECK(!on(1));a.report(r,84000);CHECK(r.schedule_result[1]==5 && r.override_ms[1]==3000 && r.winner[1]==5);
    tick(a,87000,250);a.report(r,87000);CHECK(!r.override_ms[1]);
    // Reverse conflict: sensor ON wins a scheduled OFF.
    tick(a,88000,300);CHECK(on(1));CHECK(a.execute(cmd(ControlOp::Schedule,1,0,1000),89000,false)==ControlStatus::Applied);
    tick(a,90000,300);CHECK(on(1));a.report(r,90000);CHECK(r.schedule_result[1]==5);
    // Invalid and stale input requests OFF even during a schedule-first lease.
    cfg.sensors_first=false;cfg.lease_ms=60000;a.execute(cmd(ControlOp::SensorConfig,1,0,cfg.packed()),91000,false);
    a.execute(cmd(ControlOp::Schedule,1,1,1000),91000,false);tick(a,92000,300);CHECK(on(1));
    tick(a,93000,300,true,false);CHECK(!on(1));a.report(r,93000);CHECK(r.override_ms[1]==0 && r.sensor_demand[1]==0 && r.winner[1]==6);
    RoomReport stale=input(300);stale.temperature_age_ms=10000;a.sensor_inputs(stale);a.tick(97000,false,Occupancy::Unknown,false);CHECK(!on(1));
    tick(a,98000,270);CHECK(!on(1)); // Recovery in band does not reuse old ON demand.
    // Manual OFF cancels a pending ON before its due tick; priority edit stays MANUAL.
    a.execute(cmd(ControlOp::Schedule,1,1,1000),99000,false);
    CHECK(a.execute(cmd(ControlOp::Device,1,0),99500,false)==ControlStatus::Applied);
    cfg.sensors_first=true;a.execute(cmd(ControlOp::SensorConfig,1,0,cfg.packed()),99500,false);
    tick(a,100000,300);CHECK(!on(1));a.report(r,100000);CHECK(r.schedule_result[1]==4 && !(r.auto_flags&2));
    a.execute(cmd(ControlOp::DeviceMode,1,1),101000,false);tick(a,101005,300);CHECK(on(1));
    // Countdown and cap latch OFF, cannot re-energize from continuing demand/schedules.
    a.execute(cmd(ControlOp::Energy,1,0,6000),102000,false);
    a.execute(cmd(ControlOp::Timer,1,0,3000),102000,false);
    tick(a,105000,300);CHECK(!on(1));tick(a,110000,300);CHECK(!on(1));a.report(r,110000);CHECK(r.blocking[1]==5);
    a.execute(cmd(ControlOp::Schedule,1,1,1000),110000,false);tick(a,111000,300);CHECK(!on(1));
    a.execute(cmd(ControlOp::DeviceMode,1,1),112000,false);tick(a,112005,300);CHECK(on(1));
    tick(a,118005,300);CHECK(!on(1));tick(a,130000,300);CHECK(!on(1));
    // Maximum-run cutoff also holds OFF until explicit release.
    a.execute(cmd(ControlOp::Energy,1,0,0),131000,false);a.execute(cmd(ControlOp::DeviceMode,1,1),131000,false);
    tick(a,131005,300);CHECK(on(1));safety_kernel_tick_periodic(14531005);a.note_safety_cutoff(1);tick(a,14531005,300);CHECK(!on(1));
    // Room 1 light: filtered source already provided; dark HIGH / bright LOW.
    reset();Automation light;light.enable_sensor_rules(true);light.restore_policy(saved);
    light.execute(cmd(ControlOp::DeviceMode,0,1),10000,false);tick(light,10005,270,true);CHECK(on(0));
    tick(light,14000,270,false);CHECK(!on(0));
    // Room 2 light has no sensor and no sensors-first option; schedule-only lease.
    reset();Automation c;c.enable_sensor_rules(false);c.restore_policy(saved);
    cfg.sensors_first=true;CHECK(c.execute(cmd(ControlOp::SensorConfig,0,0,cfg.packed()),10000,false)==ControlStatus::Invalid);
    cfg.sensors_first=false;cfg.lease_ms=3000;c.execute(cmd(ControlOp::SensorConfig,0,0,cfg.packed()),10000,false);
    c.execute(cmd(ControlOp::DeviceMode,0,1),10000,false);tick(c,10005,300);CHECK(!on(0));
    c.execute(cmd(ControlOp::Schedule,0,1,1000),11000,false);tick(c,12000,300);CHECK(on(0));
    tick(c,15000,300);CHECK(!on(0));c.report(r,15000);CHECK(r.sensor_demand[0]==0 && r.winner[0]==7);
    // Dwell-blocked ON is consumed and cannot replay after dwell.
    c.execute(cmd(ControlOp::Schedule,0,1,1000),15000,false);tick(c,16000,300);CHECK(!on(0));
    tick(c,20000,300);CHECK(!on(0));c.report(r,20000);CHECK(r.schedule_result[0]==3 && !r.override_ms[0]);
    c.execute(cmd(ControlOp::Schedule,0,1,1000),20000,false);tick(c,22001,300);CHECK(!on(0)); // >1s late consumed.
    // Reboot restores settings, never a mode/lease/pending ON.
    PolicyConfig persisted=c.policy();uint8_t storage[kSensorPolicyBytes];encode_sensor_policy(persisted,storage);
    PolicyConfig restored;CHECK(decode_sensor_policy(storage,sizeof(storage),restored));storage[5]^=1;CHECK(!decode_sensor_policy(storage,sizeof(storage),restored));
    reset();Automation reboot;reboot.enable_sensor_rules(false);reboot.restore_policy(persisted);tick(reboot,25000,300);reboot.report(r,25000);
    CHECK(!on(0)&&!on(1)&&r.auto_flags==0&&!r.override_ms[0]&&!r.schedule_ms[0]);
    // New/legacy reports and boot-bound config; old controllers fail closed.
    r.room_id=2;r.pir_available=false;uint8_t wire[kRoomReportBytes];encode_room_report(r,wire);RoomReport decodedReport;
    CHECK(decode_room_report(wire,sizeof(wire),decodedReport) && decodedReport.features_version==2);
    wire[0]=7;wire[82]=0;CHECK(decode_room_report(wire,84,decodedReport) && decodedReport.features_version==1);
    ControlCommand parsed;CHECK(!parse_api_control("9 1 0 1",parsed));CHECK(parse_api_control("boot 7 9 1 0 1",parsed));
    RoomGateway gateway;r.boot_id=7;r.uptime_ms=100;r.features_version=1;CHECK(gateway.report(3,r,1,100));
    auto configCommand=cmd(ControlOp::SensorConfig,1,0,cfg.packed());configCommand.boot=7;uint32_t id;
    CHECK(!gateway.queue(1,configCommand,101,101000,id));r.features_version=2;r.uptime_ms=200;CHECK(gateway.report(3,r,2,200));
    configCommand.boot=0;CHECK(!gateway.queue(1,configCommand,201,201000,id));
    configCommand.boot=6;CHECK(!gateway.queue(1,configCommand,201,201000,id));configCommand.boot=7;
    CHECK(gateway.queue(1,configCommand,201,201000,id));ControlAck ack;ack.boot=7;ack.id=id;encode_control(gateway.rooms[1].transaction.command,ack.request);
    CHECK(!gateway.ack(1,ack,202,202000));CHECK(gateway.ack(3,ack,202,202000));
    printf("{\"source\":\"HOST_MEASURED\",\"assertions\":%u,\"passed\":true}\n",checks);
}
