#pragma once
#include "safety_kernel.h"
#include "control_wire.h"
#include "policy_record.h"

struct AuditEvent { uint32_t at,id; uint8_t op,status,light,fan; };
class Automation {
public:
    bool away=false, automatic[2]={true,false};
    void enable_sensor_rules(bool room1_light) {
        sensor_rules_=true; light_sensor_=room1_light;
        automatic[0]=automatic[1]=false; // Never opt in on installation or reboot.
    }
    void sensor_inputs(const RoomReport &r) {
        last_temperature_=r.temperature_deci_c; last_dark_=r.light_raw;
        valid_[0]=light_sensor_ && r.light_available && r.light_age_ms<1000;
        valid_[1]=temperature_valid(r);
        snapshot_ms_=r.uptime_ms;
    }
    void set_evaluation_clock(uint32_t (*clock)()) { evaluation_clock_=clock; }
    uint32_t grace=120000, on_ms[2]={}, transitions[2]={};
    AuditEvent events[32]={}; uint32_t event_count=0;
    PolicyConfig policy() const {
        PolicyConfig p; p.away=away; p.light_auto=automatic[0]; p.fan_auto=automatic[1]; p.grace_ms=grace;
        for(int i=0;i<2;++i) { const auto *d=safety_kernel_get_device(i); if(d && d->manual_override) p.manual_flags|=1<<i; }
        if(sensor_rules_) for(int i=0;i<2;++i) p.sensor_config[i]=config_[i].packed();
        p.temperature_first=temperature_first_;
        return p;
    }
    void restore_policy(const PolicyConfig &p) {
        temperature_first_=p.temperature_first;
        away=p.away; automatic[0]=p.light_auto; automatic[1]=p.fan_auto; grace=p.grace_ms;
        for(int i=0;i<2;++i) {
            if(sensor_rules_) {
                SensorRuleConfig::unpack(p.sensor_config[i],config_[i]);
                if(i==0 && !light_sensor_) config_[i].sensors_first=false;
                automatic[i]=false; act(i,ACTION_OFF,true,0);
            } else if(p.manual_flags&(1<<i)) act(i,ACTION_OFF,true,0);
        }
    }
    // Sources: 0 unknown, 1 manual, 2 schedule, 3 energy cap, 4 safety cutoff.
    uint8_t source[2]={}, schedule_result[2]={};
    void note_safety_cutoff(int i) { if(i>=0 && i<2) { source[i]=4; cancel_schedule(i); if(sensor_rules_) force_hold(i,4,0); } }
    uint32_t schedule_remaining(int i,uint32_t now) const {
        if(!scheduled_[i]) return 0;
        uint32_t e=now-schedule_start_[i]; return e>=schedule_delay_[i]?1:schedule_delay_[i]-e;
    }
    ControlStatus execute(const ControlCommand &c,uint32_t now,bool occupancy_available=true) {
        if(c.op==ControlOp::SensorOrder) {
            if(!sensor_rules_ || !light_sensor_ || c.device || c.action || c.value>1) return record(c,now,ControlStatus::Invalid);
            temperature_first_=c.value; // Only order; no AUTO/hold/lease/output changes.
            return record(c,now,ControlStatus::Applied);
        }
        if(c.op==ControlOp::SensorConfig) {
            SensorRuleConfig config;
            if(!sensor_rules_ || c.device>1 || c.action || !SensorRuleConfig::unpack(c.value,config) ||
               (c.device==0 && !light_sensor_ && config.sensors_first)) return record(c,now,ControlStatus::Invalid);
            config_[c.device]=config; // Does not enable AUTO or renew a lease.
            return record(c,now,ControlStatus::Applied);
        }
        if(sensor_rules_ && (c.op==ControlOp::DeviceMode || c.op==ControlOp::Resume)) {
            const bool resume=c.op==ControlOp::Resume;
            if(c.device>1 || c.value || c.action>(resume?0:1)) return record(c,now,ControlStatus::Invalid);
            const int i=c.device; const bool auto_mode=resume || c.action==1;
            if(auto_mode && away) return record(c,now,ControlStatus::Denied);
            cancel_schedule(i); lease_[i]=false;
            demand_[i]=auto_mode && valid_[i] && (i==0?last_dark_:last_temperature_>=config_[1].on);
            automatic[i]=auto_mode;
            if(auto_mode) { forced_[i]=false; safety_kernel_release_manual("bedroom",name(i)); }
            else act(i,safety_kernel_get_device(i)->is_on?ACTION_ON:ACTION_OFF,true,now);
            return record(c,now,ControlStatus::Applied);
        }
        if(c.op==ControlOp::DeviceMode) return record(c,now,ControlStatus::Denied);
        if(c.op==ControlOp::Schedule) {
            if(c.device>1 || c.action>2 || (c.action==2?c.value!=0:(c.value<1000 || c.value>86400000)))
                return record(c,now,ControlStatus::Invalid);
            const int i=c.device;
            if(c.action==2) { scheduled_[i]=false; lease_[i]=false; schedule_result[i]=4; }
            else {
                // Explicitly arming scheduled mode releases manual priority for this device.
                // A subsequent successful manual command cancels it. No ON state is restored.
                if(away) return record(c,now,ControlStatus::Denied);
                scheduled_[i]=true; schedule_start_[i]=now; schedule_delay_[i]=c.value;
                schedule_on_[i]=c.action; schedule_result[i]=1;
                if(!sensor_rules_) { automatic[i]=false; pending_[i]=false;
                    safety_kernel_release_manual("bedroom",name(i)); }
            }
            return record(c,now,ControlStatus::Applied);
        }
        if(c.op==ControlOp::Energy) {
            if(c.device>1 || c.action || (c.value && (c.value<3000 || c.value>14400000)))
                return record(c,now,ControlStatus::Invalid);
            cap_ms_[c.device]=c.value;
            return record(c,now,ControlStatus::Applied);
        }
        if((!occupancy_available || sensor_rules_) && (c.op==ControlOp::Resume || c.op==ControlOp::Policy ||
            (c.op==ControlOp::Scene && c.action==0))) return record(c,now,ControlStatus::Denied);
        if(c.op==ControlOp::Device) {
            if(c.device>1 || c.action>2 || c.value) return record(c,now,ControlStatus::Invalid);
            bool applied=act(c.device,(action_type_t)c.action,true,now);
            if(applied) { cancel_schedule(c.device); source[c.device]=1;
                if(sensor_rules_) { automatic[c.device]=false; lease_[c.device]=false; forced_[c.device]=false; } }
            return record(c,now,applied?ControlStatus::Applied:ControlStatus::Denied);
        }
        if(c.op==ControlOp::Scene) {
            // Individual outcomes are real; a denied scene may partially apply.
            if(c.device || c.action>3 || c.value) return record(c,now,ControlStatus::Invalid);
            bool a=true,b=true;
            if(c.action) {
                a=act(0,ACTION_OFF,true,now);
                b=act(1,c.action==2?ACTION_ON:ACTION_OFF,true,now);
            }
            if(sensor_rules_ && c.action) for(int i=0;i<2;++i) if(i?b:a) { automatic[i]=false; lease_[i]=false; forced_[i]=false; }
            if(c.action) { if(a) { cancel_schedule(0); source[0]=1; } if(b) { cancel_schedule(1); source[1]=1; } }
            if(a && b) {
                cancel_schedule(0); cancel_schedule(1); source[0]=source[1]=1;
                away=c.action==1;
                for(int i=0;i<2;++i) { timer_[i]=false; pending_[i]=false; }
                if(c.action==0) for(int i=0;i<2;++i) { automatic[i]=true; safety_kernel_release_manual("bedroom",name(i)); pending_[i]=occupied(); }
            }
            return record(c,now,a&&b?ControlStatus::Applied:ControlStatus::Denied);
        }
        if(c.op==ControlOp::Timer) {
            if(c.device>1 || c.action || (c.value && (c.value<1000 || c.value>86400000))) return record(c,now,ControlStatus::Invalid);
            timer_[c.device]=c.value!=0; timer_start_[c.device]=now; timer_duration_[c.device]=c.value;
            return record(c,now,ControlStatus::Applied);
        }
        if(c.op==ControlOp::Policy) {
            if(c.device<2 && c.action<2 && !c.value) automatic[c.device]=c.action;
            else if(c.device==2 && !c.action && c.value>=30000 && c.value<=1800000) grace=c.value;
            else return record(c,now,ControlStatus::Invalid);
            pending_[0]=pending_[1]=false;
            return record(c,now,ControlStatus::Applied);
        }
        if(c.op==ControlOp::Resume && c.device<2 && !c.action && !c.value) {
            if(away) return record(c,now,ControlStatus::Denied);
            cancel_schedule(c.device);
            automatic[c.device]=true; pending_[c.device]=occupied();
            safety_kernel_release_manual("bedroom",name(c.device));
            return record(c,now,ControlStatus::Applied);
        }
        return record(c,now,ControlStatus::Invalid);
    }
    void tick(uint32_t now,bool motion_rise,Occupancy occupancy,bool occupancy_available=true) {
        occupancy_=occupancy;
        uint32_t elapsed=now-last_tick_; last_tick_=now;
        if(sensor_rules_ && light_sensor_) { ++evaluation_cycle_; evaluation_started_us_=stamp(); }
        for(int step=0;step<2;++step) {
            const int i=sensor_rules_ && light_sensor_ && temperature_first_?1-step:step;
            if(sensor_rules_) {
                step_[i]=step+1; started_[i]=stamp();
                if(i==0) demand_[i]=valid_[i] && last_dark_;
                else if(!valid_[i]) demand_[i]=false;
                else if(last_temperature_>=config_[i].on) demand_[i]=true;
                else if(last_temperature_<=config_[i].off) demand_[i]=false;
                requested_[i]=demand_[i];
            }
            const managed_device_t *d=safety_kernel_get_device(i);
            if(d && previous_[i]) on_ms[i]+=elapsed;
            if(d && d->is_on!=previous_[i]) ++transitions[i];
            previous_[i]=d && d->is_on;
            if(d && d->is_on && cap_ms_[i] && (uint32_t)(now-d->turned_on_ms)>=cap_ms_[i]) {
                // User-configured conservation cap may turn OFF manual ON; never turns ON.
                if(act(i,ACTION_OFF,true,now)) { source[i]=3; cancel_schedule(i); if(sensor_rules_) force_hold(i,3,now); }
            }
            if(scheduled_[i] && (uint32_t)(now-schedule_start_[i])>=schedule_delay_[i]) {
                scheduled_[i]=false; // one attempt only; no late ON/reconnect replay
                bool timely=(uint32_t)(now-schedule_start_[i])-schedule_delay_[i]<=1000;
                bool ok=false;
                if(sensor_rules_) {
                    if(!away && timely && automatic[i] && !forced_[i]) {
                        lease_[i]=true; lease_start_[i]=now; lease_duration_[i]=config_[i].lease_ms;
                        // A due ON gets a single dwell attempt; never a late lease replay.
                        const bool sensor_exists=i==1 || light_sensor_;
                        if(sensor_exists && !valid_[i]) { lease_[i]=false; }
                        else if(sensor_exists && config_[i].sensors_first && demand_[i]!=schedule_on_[i]) {
                            schedule_result[i]=5; ok=true;
                        } else {
                            ok=act(i,schedule_on_[i]?ACTION_ON:ACTION_OFF,false,now);
                            if(!ok) lease_[i]=false;
                        }
                    }
                } else ok=!away && timely && act(i,schedule_on_[i]?ACTION_ON:ACTION_OFF,false,now);
                if(schedule_result[i]!=5 || !ok) schedule_result[i]=ok?2:3;
                if(ok && schedule_result[i]!=5) source[i]=2;
            }
            if(timer_[i] && (uint32_t)(now-timer_start_[i])>=timer_duration_[i]) {
                if(act(i,ACTION_OFF,true,now)) { timer_[i]=false; pending_[i]=false; source[i]=3; cancel_schedule(i); if(sensor_rules_) force_hold(i,3,now); }
            }
            if(sensor_rules_) {
                arbitrate(i,now);
                result_[i]=blocking_[i]==1?2:blocking_[i]==5?3:blocking_[i]==4?4:blocking_[i]==6?5:
                    winner_[i]==6?6:winner_[i]==2?7:1;
                elapsed_[i]=evaluation_clock_?stamp()-started_[i]:0;
                continue;
            }
            if(!occupancy_available) continue;
            if(motion_rise && automatic[i] && !away) pending_[i]=true;
            if(!automatic[i] || away) continue;
            if(occupancy==Occupancy::Vacant) {
                pending_[i]=false; act(i,ACTION_OFF,false,now);
            } else if(pending_[i] && act(i,ACTION_ON,false,now)) pending_[i]=false;
        }
    }
    uint32_t timer_remaining(int i,uint32_t now) const {
        if(!timer_[i]) return 0;
        uint32_t elapsed=now-timer_start_[i];
        return elapsed>=timer_duration_[i]?1:timer_duration_[i]-elapsed;
    }
    void report(RoomReport &s,uint32_t now) const {
        s.features_version=sensor_rules_?(light_sensor_?3:2):1;
        if(s.features_version==3) {
            s.sensor_order=temperature_first_; s.sensor_snapshot_ms=snapshot_ms_; s.evaluation_cycle=evaluation_cycle_;
            s.evaluation_started_us=evaluation_started_us_; s.evaluation_clock_valid=evaluation_clock_!=nullptr;
            for(int i=0;i<2;++i) { s.rule_started_us[i]=started_[i]; s.rule_elapsed_us[i]=elapsed_[i];
                s.evaluation_step[i]=step_[i]; s.sensor_requested[i]=requested_[i]; s.sensor_result[i]=result_[i]; }
        }
        for(int i=0;i<2;++i) {
            if(sensor_rules_) {
                s.sensor_config[i]=config_[i].packed();
                s.override_ms[i]=lease_[i] && (uint32_t)(now-lease_start_[i])<lease_duration_[i]?lease_duration_[i]-(uint32_t)(now-lease_start_[i]):0;
                s.override_action[i]=schedule_on_[i];
                s.sensor_demand[i]=valid_[i]?(demand_[i]?2:1):0;
                s.winner[i]=winner_[i]; s.blocking[i]=blocking_[i];
            }
            s.schedule_ms[i]=schedule_remaining(i,now); s.schedule_action[i]=schedule_on_[i];
            s.schedule_result[i]=schedule_result[i]; s.control_source[i]=source[i]; s.energy_cap_ms[i]=cap_ms_[i];
        }
        s.mode=away; s.auto_flags=automatic[0]|(automatic[1]<<1); s.grace_ms=grace;
        s.timer_light_ms=timer_remaining(0,now); s.timer_fan_ms=timer_remaining(1,now);
        s.light_on_ms=on_ms[0]; s.fan_on_ms=on_ms[1];
        s.manual_flags=0;
        for(int i=0;i<2;++i) { const auto *d=safety_kernel_get_device(i); if(d && d->manual_override) s.manual_flags|=1<<i; }
    }
private:
    bool temperature_first_=false;
    uint32_t (*evaluation_clock_)()=nullptr;
    uint32_t snapshot_ms_=0,evaluation_cycle_=0,evaluation_started_us_=0,started_[2]={},elapsed_[2]={};
    uint8_t step_[2]={},requested_[2]={},result_[2]={};
    uint32_t stamp() const { return evaluation_clock_?evaluation_clock_():0; }
    bool sensor_rules_=false,light_sensor_=false,last_dark_=false;
    int16_t last_temperature_=0;
    SensorRuleConfig config_[2];
    bool valid_[2]={},demand_[2]={},lease_[2]={},forced_[2]={};
    uint32_t lease_start_[2]={},lease_duration_[2]={};
    uint8_t winner_[2]={},blocking_[2]={};
    void force_hold(int i,uint8_t why,uint32_t now) {
        forced_[i]=true; lease_[i]=false; automatic[i]=false;
        act(i,ACTION_OFF,true,now); source[i]=why;
    }
    void arbitrate(int i,uint32_t now) {
        if(lease_[i] && (uint32_t)(now-lease_start_[i])>=lease_duration_[i]) lease_[i]=false;
        blocking_[i]=0;
        if(forced_[i]) { winner_[i]=source[i]; blocking_[i]=5; return; }
        if(!automatic[i]) { winner_[i]=1; blocking_[i]=1; return; }
        if(away) { lease_[i]=false; winner_[i]=6; blocking_[i]=6; act(i,ACTION_OFF,false,now); return; }
        const bool exists=i==1 || light_sensor_;
        if(exists && !valid_[i]) {
            // Required-input fault beats and discards an active lease. No
            // last-good sample, schedule fallback, or recovery ON replay.
            lease_[i]=false; winner_[i]=6; blocking_[i]=2;
            if(act(i,ACTION_OFF,false,now)) source[i]=6;
            else blocking_[i]=4;
            return;
        }
        bool requested=false;
        if(lease_[i] && (!exists || !config_[i].sensors_first)) {
            requested=schedule_on_[i]; winner_[i]=2;
        } else if(exists) {
            requested=demand_[i]; winner_[i]=5;
            if(lease_[i] && requested!=schedule_on_[i]) schedule_result[i]=5;
        } else { winner_[i]=7; blocking_[i]=3; } // Schedule-only idle is OFF.
        if(act(i,requested?ACTION_ON:ACTION_OFF,false,now)) source[i]=winner_[i];
        else blocking_[i]=4; // Sensor demand may retry after dwell; clock ON cannot.
    }
    bool scheduled_[2]={},schedule_on_[2]={};
    uint32_t schedule_start_[2]={},schedule_delay_[2]={},cap_ms_[2]={};
    void cancel_schedule(int i) { if(scheduled_[i]) { scheduled_[i]=false; schedule_result[i]=4; } }
    Occupancy occupancy_=Occupancy::Unknown;
    bool occupied() const { return occupancy_==Occupancy::Occupied || occupancy_==Occupancy::LikelyOccupied; }
    bool timer_[2]={},pending_[2]={},previous_[2]={};
    uint32_t timer_start_[2]={},timer_duration_[2]={},last_tick_=0;
    static const char *name(int i) { return i?"fan":"light"; }
    bool act(int i,action_type_t action,bool manual,uint32_t now) {
        const char *reason=nullptr;
        return safety_kernel_request_action("bedroom",name(i),action,manual,now,&reason);
    }
    ControlStatus record(const ControlCommand &c,uint32_t now,ControlStatus status) {
        const auto *a=safety_kernel_get_device(0),*b=safety_kernel_get_device(1);
        events[event_count++%32]={now,c.id,(uint8_t)c.op,(uint8_t)status,(uint8_t)(a&&a->is_on),(uint8_t)(b&&b->is_on)};
        return status;
    }
};
