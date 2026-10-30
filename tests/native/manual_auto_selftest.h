#pragma once
#include "command_target.h"
#include "automation.h"

inline bool manual_until_auto_test() {
    safety_kernel_init();
    safety_kernel_register_device("bedroom","light",DEVICE_TYPE_LIGHT,-1,false);
    safety_kernel_register_device("bedroom","fan",DEVICE_TYPE_FAN,-1,false);
    Automation a; ControlCommand c; c.action=0;
    if(a.execute(c,100)!=ControlStatus::Applied) return false;
    safety_kernel_tick_periodic(600100); a.tick(600100,true,Occupancy::Occupied);
    if(safety_kernel_get_device(0)->is_on || !safety_kernel_get_device(0)->manual_override) return false;
    c.op=ControlOp::Resume;
    if(a.execute(c,600101)!=ControlStatus::Applied) return false;
    a.tick(600102,false,Occupancy::Occupied);
    return safety_kernel_get_device(0)->is_on && !safety_kernel_get_device(0)->manual_override;
}
inline bool per_device_auto_test() {
    safety_kernel_init();
    safety_kernel_register_device("bedroom","light",DEVICE_TYPE_LIGHT,-1,false);
    safety_kernel_register_device("bedroom","fan",DEVICE_TYPE_FAN,-1,false);
    Automation a; ControlCommand c; c.action=0;
    a.execute(c,0); c.device=1; a.execute(c,0);
    a.tick(4000,false,Occupancy::Occupied); c.op=ControlOp::Resume;
    if(a.execute(c,4001)!=ControlStatus::Applied || !a.automatic[1]) return false;
    a.tick(4002,false,Occupancy::Occupied);
    if(!safety_kernel_get_device(1)->is_on || safety_kernel_get_device(0)->is_on || !safety_kernel_get_device(0)->manual_override) return false;
    a.away=true;
    if(a.execute(c,5000)!=ControlStatus::Denied) return false;
    CommandTarget t; ControlCommand mapped;
    return parse_target("physical_bedroom","bedroom","fan","AUTO",t) && target_command(t,mapped) &&
        mapped.op==ControlOp::Resume && mapped.device==1 && !mapped.action &&
        !parse_target("virtual_rooms","bedroom","fan","AUTO",t);
}
inline bool manual_record_test() {
    uint8_t p[kManualRecordBytes]; uint8_t flags=0;
    encode_manual_policy(3,p); if(!decode_manual_policy(p,sizeof(p),flags) || flags!=3) return false;
    p[4]=2; if(decode_manual_policy(p,sizeof(p),flags)) return false;
    encode_manual_policy(4,p); if(decode_manual_policy(p,sizeof(p),flags)) return false;
    // Existing version1 policy bytes stay readable by baseline firmware.
    PolicyConfig c; c.manual_flags=3; uint8_t legacy[kPolicyRecordBytes]; encode_policy(c,legacy);
    PolicyConfig decoded; return legacy[3]==1 && !legacy[5] && decode_policy(legacy,sizeof(legacy),decoded) && decoded.manual_flags==0;
}
inline bool led_profile_wire_test() {
    RoomReport r,d; r.led_only_bench=true; uint8_t p[kRoomReportBytes]; encode_room_report(r,p);
    if(decode_room_report(p,sizeof(p),d)) return false; // Cannot claim LED-enabled profile with inhibited outputs.
    r.physical_outputs_enabled=true; r.relay_active_low=true; encode_room_report(r,p);
    if(!decode_room_report(p,sizeof(p),d) || !d.led_only_bench || d.control_policy_version!=2) return false;
    p[7]=0; return decode_room_report(p,sizeof(p),d) && d.led_only_bench && !d.relay_active_low;
}
inline bool virtual_hold_isolation_test() {
    safety_kernel_init();
    safety_kernel_register_device("bedroom","light",DEVICE_TYPE_LIGHT,-1,false);
    safety_kernel_register_device("bedroom","fan",DEVICE_TYPE_FAN,-1,false);
    safety_kernel_register_device("virtual","light",DEVICE_TYPE_LIGHT,-1,false);
    if(safety_kernel_configure_virtual_hold(0) || safety_kernel_configure_virtual_hold(1) || !safety_kernel_configure_virtual_hold(2)) return false;
    const char *reason=nullptr;
    safety_kernel_request_action("bedroom","light",ACTION_OFF,true,0,&reason);
    safety_kernel_request_action("virtual","light",ACTION_OFF,true,0,&reason);
    safety_kernel_tick_periodic(300001);
    return safety_kernel_get_device(0)->manual_override && !safety_kernel_get_device(2)->manual_override;
}
inline bool no_pir_scope_test() {
    safety_kernel_init();
    safety_kernel_register_device("bedroom","light",DEVICE_TYPE_LIGHT,-1,false);
    safety_kernel_register_device("bedroom","fan",DEVICE_TYPE_FAN,-1,false);
    Automation a; ControlCommand c;
    c.op=ControlOp::Device; c.action=ACTION_OFF;
    if(a.execute(c,0,false)!=ControlStatus::Applied) return false;
    c.op=ControlOp::Resume;
    if(a.execute(c,1,false)!=ControlStatus::Denied) return false;
    c.op=ControlOp::Policy;
    if(a.execute(c,2,false)!=ControlStatus::Denied) return false;
    c.op=ControlOp::Scene; c.action=0;
    if(a.execute(c,3,false)!=ControlStatus::Denied) return false;
    c.op=ControlOp::Device; c.action=ACTION_ON;
    if(a.execute(c,4,false)!=ControlStatus::Applied) return false;
    a.automatic[0]=a.automatic[1]=true;
    a.tick(100,true,Occupancy::Occupied,false); a.tick(200,true,Occupancy::Occupied,false);
    if(!safety_kernel_get_device(0)->is_on || safety_kernel_get_device(1)->is_on || a.on_ms[0]!=100) return false;
    RoomReport r,d; r.pir_available=false; r.auto_flags=0;
    uint8_t p[kRoomReportBytes]; encode_room_report(r,p);
    if(!decode_room_report(p,sizeof(p),d) || d.pir_available || d.auto_flags || d.motion) return false;
    p[2]=1; if(decode_room_report(p,sizeof(p),d)) return false;
    p[2]=0; p[21]=1; if(decode_room_report(p,sizeof(p),d)) return false;
    r.pir_available=true; r.auto_flags=1; encode_room_report(r,p);
    return decode_room_report(p,sizeof(p),d) && d.pir_available;
}
