#define CLAWGRID_ROOM1_DEMO_ONLY 1
#include "automation.h"
#include "room_gateway.h"
#include <stdio.h>
static unsigned checks=0;
#define CHECK(x) do { ++checks; if(!(x)) { fprintf(stderr,"line %d: %s\n",__LINE__,#x); return 1; } } while(0)
static uint32_t clock_us=0;
static uint32_t clock_step() { clock_us+=7; return clock_us; }
static void reset() { safety_kernel_init(); safety_kernel_register_device("bedroom","light",DEVICE_TYPE_MOCK,-1,false); safety_kernel_register_device("bedroom","fan",DEVICE_TYPE_MOCK,-1,false); }
static ControlCommand cmd(ControlOp op,int device,int action,uint32_t value=0) { ControlCommand c;c.op=op;c.device=device;c.action=action;c.value=value;return c; }
static RoomReport inputs(uint32_t at,int temp=300,bool dark=true) {
    RoomReport r;r.uptime_ms=at;r.temperature_status=TemperatureStatus::Ok;r.temperature_deci_c=temp;
    r.humidity_deci=600;r.temperature_age_ms=100;r.light_available=true;r.light_raw=dark;r.light_age_ms=0;return r;
}
int main() {
    reset();Automation a;a.enable_sensor_rules(true);a.set_evaluation_clock(clock_step);a.restore_policy(PolicyConfig());
    RoomReport in=inputs(10000);a.sensor_inputs(in);
    CHECK(a.execute(cmd(ControlOp::SensorOrder,0,0,1),10000,false)==ControlStatus::Applied);
    a.tick(10000,false,Occupancy::Unknown,false);RoomReport r;a.report(r,10000);
    CHECK(r.features_version==3 && r.sensor_order==1 && r.auto_flags==0 && r.manual_flags==3);
    CHECK(!safety_kernel_get_device(0)->is_on && !safety_kernel_get_device(1)->is_on);
    CHECK(r.evaluation_cycle==1 && r.sensor_snapshot_ms==10000 && r.evaluation_clock_valid);
    CHECK(r.evaluation_step[1]==1 && r.evaluation_step[0]==2);
    CHECK(r.rule_started_us[1]<r.rule_started_us[0] && r.rule_elapsed_us[0]==7 && r.rule_elapsed_us[1]==7);
    CHECK(r.sensor_requested[0]==1 && r.sensor_requested[1]==1 && r.sensor_result[0]==2 && r.sensor_result[1]==2);
    CHECK(a.execute(cmd(ControlOp::SensorOrder,1,0,0),10001,false)==ControlStatus::Invalid);
    CHECK(a.execute(cmd(ControlOp::SensorOrder,0,1,0),10001,false)==ControlStatus::Invalid);
    CHECK(a.execute(cmd(ControlOp::SensorOrder,0,0,2),10001,false)==ControlStatus::Invalid);
    // Higher-priority temperature blocked by manual hold must not starve light.
    a.execute(cmd(ControlOp::DeviceMode,0,1),11000,false);a.sensor_inputs(inputs(11005));a.tick(11005,false,Occupancy::Unknown,false);a.report(r,11005);
    CHECK(r.sensor_result[1]==2 && r.sensor_result[0]==1 && safety_kernel_get_device(0)->is_on && !safety_kernel_get_device(1)->is_on);
    CHECK(r.evaluation_step[1]==1 && r.evaluation_step[0]==2 && r.evaluation_cycle==2);
    // Flip order without changing modes, thresholds or source arbitration config.
    const auto before=a.policy();a.execute(cmd(ControlOp::SensorOrder,0,0,0),12000,false);
    a.sensor_inputs(inputs(12000));a.tick(12000,false,Occupancy::Unknown,false);a.report(r,12000);
    CHECK(r.evaluation_step[0]==1 && r.evaluation_step[1]==2 && r.auto_flags==1 && r.manual_flags==2);
    CHECK(a.policy().sensor_config[0]==before.sensor_config[0] && a.policy().sensor_config[1]==before.sensor_config[1]);
    // Immutable input copy: later producer/caller mutation cannot change this cycle.
    a.execute(cmd(ControlOp::DeviceMode,1,1),13000,false);in=inputs(13005);a.sensor_inputs(in);in.light_raw=false;in.temperature_deci_c=0;in.uptime_ms=99999;
    a.tick(13005,false,Occupancy::Unknown,false);a.report(r,13005);
    CHECK(r.sensor_snapshot_ms==13005 && r.sensor_requested[0]==1 && r.sensor_requested[1]==1);
    CHECK(safety_kernel_get_device(0)->is_on && safety_kernel_get_device(1)->is_on);
    // Higher-priority light dwell block still allows lower temperature OFF.
    CHECK(a.execute(cmd(ControlOp::Device,0,0),16500,false)==ControlStatus::Applied);
    a.sensor_inputs(inputs(17000,250,true));a.tick(17000,false,Occupancy::Unknown,false);a.report(r,17000);
    CHECK(r.sensor_result[0]==2 && r.sensor_result[1]==1 && !safety_kernel_get_device(1)->is_on);
    a.sensor_inputs(inputs(18000,300,false));a.tick(18000,false,Occupancy::Unknown,false);a.report(r,18000);
    CHECK(r.sensor_result[1]==4 && r.evaluation_step[0]==1 && r.evaluation_step[1]==2);
    // Stable sensor demand inside fan band is independent of selected order.
    a.execute(cmd(ControlOp::DeviceMode,0,1),21000,false);
    a.sensor_inputs(inputs(22000,300,true));a.tick(22000,false,Occupancy::Unknown,false);
    a.execute(cmd(ControlOp::SensorOrder,0,0,1),23000,false);a.sensor_inputs(inputs(23000,270,true));a.tick(23000,false,Occupancy::Unknown,false);a.report(r,23000);
    CHECK(r.sensor_requested[1]==1 && safety_kernel_get_device(1)->is_on);
    // Countdown safety hold and its earlier priority never starve the other rule.
    a.execute(cmd(ControlOp::Timer,1,0,3000),23000,false);a.sensor_inputs(inputs(26000));a.tick(26000,false,Occupancy::Unknown,false);a.report(r,26000);
    CHECK(!safety_kernel_get_device(1)->is_on && r.sensor_result[1]==3 && r.sensor_result[0]==1);
    a.execute(cmd(ControlOp::SensorOrder,0,0,0),30000,false);a.sensor_inputs(inputs(30000));a.tick(30000,false,Occupancy::Unknown,false);CHECK(!safety_kernel_get_device(1)->is_on);
    // Invalid/stale required input never reuses a prior good reading.
    a.execute(cmd(ControlOp::DeviceMode,1,1),31000,false);in=inputs(31005);in.temperature_status=TemperatureStatus::Invalid;in.light_age_ms=1000;
    a.sensor_inputs(in);a.tick(31005,false,Occupancy::Unknown,false);a.report(r,31005);
    CHECK(r.sensor_requested[0]==0 && r.sensor_requested[1]==0 && r.sensor_result[0]==6 && r.sensor_result[1]==6);
    CHECK(!safety_kernel_get_device(0)->is_on && !safety_kernel_get_device(1)->is_on);
    in=inputs(35000);in.temperature_age_ms=10000;a.sensor_inputs(in);a.tick(35000,false,Occupancy::Unknown,false);a.report(r,35000);
    CHECK(r.sensor_requested[1]==0 && r.sensor_result[1]==6 && r.sensor_result[0]==1);
    // Wrapping microsecond counter uses unsigned duration; no artificial delay.
    clock_us=0xfffffff8;a.sensor_inputs(inputs(36000));a.tick(36000,false,Occupancy::Unknown,false);a.report(r,36000);
    CHECK(r.rule_elapsed_us[0]==7 && r.rule_elapsed_us[1]==7 && r.evaluation_step[0]==1 && r.evaluation_step[1]==2);
    // Persist only order/configuration, never active modes or outputs.
    a.execute(cmd(ControlOp::SensorOrder,0,0,1),37000,false);auto stored=a.policy();uint8_t record[kSensorOrderBytes];encode_sensor_order(stored.temperature_first,record);bool order=false;
    CHECK(decode_sensor_order(record,sizeof(record),order) && order);record[4]^=1;CHECK(!decode_sensor_order(record,sizeof(record),order));
    reset();Automation reboot;reboot.enable_sensor_rules(true);reboot.restore_policy(stored);reboot.sensor_inputs(inputs(100));reboot.tick(100,false,Occupancy::Unknown,false);reboot.report(r,100);
    CHECK(r.sensor_order==1 && r.auto_flags==0 && r.manual_flags==3 && !safety_kernel_get_device(0)->is_on && !safety_kernel_get_device(1)->is_on);
    r.pir_available=false;r.boot_id=10;r.room_id=1;r.light_available=true;r.light_age_ms=5;r.temperature_sample_age_ms=100;
    uint8_t bytes[kRoomReportBytes];encode_room_report(r,bytes);RoomReport decoded;
    CHECK(decode_room_report(bytes,sizeof(bytes),decoded) && decoded.features_version==3 && decoded.sensor_order==1 && decoded.light_age_ms==5);
    bytes[142]=2;CHECK(!decode_room_report(bytes,sizeof(bytes),decoded));
    encode_room_report(r,bytes);bytes[0]=8;bytes[82]=2;CHECK(decode_room_report(bytes,108,decoded) && decoded.features_version==2 && decoded.evaluation_cycle==0);
    ControlCommand parsed;CHECK(!parse_api_control("11 0 0 1",parsed));CHECK(parse_api_control("boot 10 11 0 0 1",parsed));
    // ROOM1-only gateway accepts no Room2 telemetry or command, even if retained source/provisioning exists.
    RoomGateway gateway;r.uptime_ms=100;CHECK(gateway.report(1,r,1,100));RoomReport c=r;c.room_id=2;CHECK(!gateway.report(3,c,1,100));
    uint32_t id;auto setting=cmd(ControlOp::SensorOrder,0,0,0);setting.boot=10;
    CHECK(!gateway.queue(1,setting,101,101000,id));setting.boot=9;CHECK(!gateway.queue(0,setting,101,101000,id));setting.boot=10;CHECK(gateway.queue(0,setting,101,101000,id));
    printf("{\"source\":\"HOST_MEASURED\",\"assertions\":%u,\"passed\":true}\n",checks);
}
