#pragma once
#include "pir_tracker.h"
#include "automation.h"

inline bool pir_startup_test(bool held_high) {
    PirTracker pir; pir.begin(1000,60000);
    pir.sample(true,1001); pir.sample(true,61000);
    if(pir.ready() || pir.motion() || pir.occupancy(61000,120000)!=Occupancy::Unknown) return false;
    if(held_high) {
        pir.sample(true,90000);
        if(pir.ready() || pir.motion()) return false;
    }
    uint32_t low=held_high?90001:61001;
    pir.sample(false,low); pir.sample(false,low+199);
    if(pir.ready()) return false;
    pir.sample(false,low+200);
    if(!pir.ready() || pir.motion() || pir.warmup_remaining(low+200)) return false;
    if(pir.sample(true,low+205) || pir.sample(true,low+404)) return false;
    return pir.sample(true,low+405) && pir.motion() && pir.occupancy(low+405,120000)==Occupancy::Occupied;
}
inline bool pir_wrap_test() {
    PirTracker pir; uint32_t start=0xfffff000u; pir.begin(start,60000);
    pir.sample(false,start+59999u);
    if(pir.ready() || pir.warmup_remaining(start+59999u)!=1) return false;
    pir.sample(false,start+60000u); pir.sample(false,start+60199u);
    if(pir.ready()) return false;
    pir.sample(false,start+60200u);
    if(!pir.ready() || pir.sample(true,start+60205u)) return false;
    return pir.sample(true,start+60405u) && pir.motion();
}
inline bool pir_vacancy_test() {
    PirTracker pir; pir.begin(0,0);
    pir.sample(false,0); pir.sample(false,200);
    // Noise below the filter duration must leave occupancy UNKNOWN.
    pir.sample(true,205); pir.sample(false,404);
    if(pir.motion() || pir.occupancy(405,120000)!=Occupancy::Unknown) return false;
    pir.sample(true,1000); pir.sample(true,1200);
    pir.sample(false,1205); pir.sample(false,1405); // Last qualified HIGH at 1205.
    return !pir.motion() && pir.occupancy(31204,120000)==Occupancy::Occupied &&
        pir.occupancy(31205,120000)==Occupancy::LikelyOccupied &&
        pir.occupancy(121204,120000)==Occupancy::LikelyOccupied &&
        pir.occupancy(121205,120000)==Occupancy::Vacant;
}
inline bool report_progress_test() {
    RoomReport previous,next;
    if(room_report_advances(next,previous)) return false;
    next.boot_id=1;
    if(!room_report_advances(next,previous)) return false;
    previous=next;
    // Even a new authenticated radio envelope cannot advance this sample.
    if(room_report_advances(next,previous)) return false;
    next.uptime_ms=1000;
    if(!room_report_advances(next,previous)) return false;
    if(!room_report_publishable(next,previous,1100) || room_report_publishable(next,previous,1101)) return false;
    previous=next; next.uptime_ms=999;
    if(room_report_advances(next,previous)) return false;
    previous.uptime_ms=0xfffffff0u; next.uptime_ms=16;
    if(!room_report_advances(next,previous)) return false;
    if(!room_report_publishable(next,previous,116) || room_report_publishable(next,previous,117)) return false;
    previous=next; next.boot_id=2; next.uptime_ms=0;
    if(!room_report_advances(next,previous)) return false;
    previous=next; next.boot_id=1; next.uptime_ms=100000;
    return !room_report_advances(next,previous);
}
inline bool integration_registry() {
    safety_kernel_init();
    return safety_kernel_register_device("bedroom","light",DEVICE_TYPE_LIGHT,-1,true)==0 &&
        safety_kernel_register_device("bedroom","fan",DEVICE_TYPE_FAN,-1,true)==1;
}
inline bool timer_dwell_test() {
    if(!integration_registry()) return false;
    Automation a; ControlCommand c; c.op=ControlOp::Device; c.device=1; c.action=1;
    if(a.execute(c,100)!=ControlStatus::Applied) return false;
    c.op=ControlOp::Timer; c.action=0; c.value=1000;
    if(a.execute(c,100)!=ControlStatus::Applied) return false;
    a.tick(1100,false,Occupancy::Unknown);
    if(!safety_kernel_get_device(1)->is_on || a.timer_remaining(1,1100)!=1) return false;
    a.tick(3099,false,Occupancy::Unknown);
    if(!safety_kernel_get_device(1)->is_on) return false;
    a.tick(3100,false,Occupancy::Unknown);
    return !safety_kernel_get_device(1)->is_on && !a.timer_remaining(1,3100);
}
inline bool recovery_transients_test() {
    if(!integration_registry()) return false;
    Automation original; original.automatic[1]=true;
    ControlCommand c; c.op=ControlOp::Device; c.action=1;
    if(original.execute(c,100)!=ControlStatus::Applied) return false;
    c.op=ControlOp::Timer; c.action=0; c.value=10000;
    if(original.execute(c,100)!=ControlStatus::Applied) return false;
    // Reboot restores configuration, never the previous ON/timer/manual state.
    PolicyConfig saved=original.policy();
    if(!integration_registry()) return false;
    Automation restored; restored.restore_policy(saved);
    restored.tick(1000,false,Occupancy::Unknown);
    RoomReport report; restored.report(report,1000);
    if(report.auto_flags!=3 || report.manual_flags || report.timer_light_ms ||
       safety_kernel_get_device(0)->is_on || safety_kernel_get_device(1)->is_on) return false;
    restored.tick(1005,true,Occupancy::Occupied);
    return safety_kernel_get_device(0)->is_on && safety_kernel_get_device(1)->is_on;
}
inline bool maximum_run_test() {
    if(!integration_registry()) return false;
    const char *reason=nullptr;
    if(!safety_kernel_request_action("bedroom","fan",ACTION_ON,true,100,&reason)) return false;
    uint32_t cutoff=100+DEFAULT_MAX_RUN_MS;
    // Refreshing a manual ON hold cannot extend the continuous-run deadline.
    safety_kernel_request_action("bedroom","fan",ACTION_ON,true,cutoff-1,&reason);
    safety_kernel_tick_periodic(cutoff-1);
    if(!safety_kernel_get_device(1)->is_on) return false;
    safety_kernel_tick_periodic(cutoff);
    if(safety_kernel_get_device(1)->is_on) return false;
    // No automatic retry after cutoff without a new motion event.
    Automation a; a.automatic[1]=true; a.tick(cutoff+3000,false,Occupancy::Occupied);
    return !safety_kernel_get_device(1)->is_on;
}
