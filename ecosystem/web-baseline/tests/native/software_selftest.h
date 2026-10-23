#pragma once
#include "command_target.h"
#include "pcm16.h"
#include "../../firmware/common/config/clawgrid_config.h"
static_assert(CLAWGRID_PHYSICAL_OUTPUTS==0,"Host/default tests must remain output-inhibited");

inline bool software_routing_test() {
    ControlCommand text;
    if(!parse_control_text("1 0 1 0",text)||text.op!=ControlOp::Device || parse_control_text("4294967297 0 1 0",text)||parse_control_text("1 0 -1 0",text)||parse_control_text("1 0 1 0 extra",text))return false;
    for(int command=1;command<=16;++command) {
        CommandTarget t;ControlCommand c;
        if(!speech_target(command,TargetMode::VirtualRooms,t)||!target_command(t,c)||c.op!=ControlOp::Simulation||c.device!=(command-1)/2) return false;
        bool physical=speech_target(command,TargetMode::PhysicalBedroom,t);
        if(physical!=(command<=4)) return false;
        if(physical&&(!target_command(t,c)||c.op!=ControlOp::Device||c.device!=(command-1)/2)) return false;
    }
    CommandTarget t;ControlCommand c;ProposalStore store;
    if(!parse_target("physical_bedroom","bedroom","fan","ON",t)) return false;
    if(parse_target("physical_bedroom","kitchen","fan","ON",t)||parse_target("bad","bedroom","fan","ON",t))return false;
    if(store.confirm(1,t,1,true,100,c)!=ProposalResult::Consumed)return false;
    if(store.prepare(t,1,false,100,false,0)!=ProposalResult::Unavailable)return false;
    if(store.prepare(t,1,true,100,false,0)!=ProposalResult::Ok)return false;
    auto changed=t;changed.mode=TargetMode::VirtualRooms;
    if(store.confirm(store.id,changed,1,true,101,c)!=ProposalResult::Mismatch)return false;
    if(store.confirm(store.id,t,2,true,101,c)!=ProposalResult::Unavailable)return false;
    if(store.confirm(store.id,t,1,false,101,c)!=ProposalResult::Unavailable)return false;
    if(store.confirm(store.id,t,1,true,15100,c)!=ProposalResult::Expired)return false;
    if(store.confirm(store.id,t,1,true,101,c)!=ProposalResult::Ok)return false;
    store.consume();if(store.confirm(store.id,t,1,true,101,c)!=ProposalResult::Consumed)return false;
    return true;
}
inline bool software_pcm_test() {
    uint8_t bytes[]={0,128,255,127,1,0,255,255};int16_t frame[5];
    if(pcm_frame(bytes,sizeof(bytes),0,frame,5)!=4||frame[0]!=-32768||frame[1]!=32767||frame[2]!=1||frame[3]!=-1||frame[4]!=0)return false;
    auto levels=pcm_levels(bytes,sizeof(bytes));
    if(levels.peak!=32768||levels.clipped!=2||levels.nonzero!=4)return false;
    if(pcm_frame(bytes,sizeof(bytes),4,frame,5)!=0)return false;
    for(int value:frame)if(value)return false;
    return pcm_valid_size(3200)&&pcm_valid_size(128000)&&!pcm_valid_size(3198)&&!pcm_valid_size(128002)&&!pcm_valid_size(3201);
}
inline bool software_wire_test() {
    RoomReport original,decoded;original.boot_id=1;original.physical_outputs_enabled=true;original.relay_active_low=false;
    uint8_t bytes[kRoomReportBytes];encode_room_report(original,bytes);
    if(!decode_room_report(bytes,sizeof(bytes),decoded)||!decoded.physical_outputs_enabled||decoded.relay_active_low)return false;
    bytes[0]=2;if(decode_room_report(bytes,sizeof(bytes),decoded))return false;
    bytes[0]=3;bytes[6]=2;return !decode_room_report(bytes,sizeof(bytes),decoded);
}
inline bool software_policy_test() {
    Automation original;
    if(!original.automatic[0] || original.automatic[1] || original.grace!=120000)return false;
    PolicyConfig saved;saved.light_auto=false;saved.fan_auto=true;saved.away=true;saved.grace_ms=45000;
    uint8_t record[kPolicyRecordBytes];encode_policy(saved,record);PolicyConfig decoded;
    if(!decode_policy(record,sizeof(record),decoded))return false;
    safety_kernel_init();
    safety_kernel_register_device("bedroom","light",DEVICE_TYPE_MOCK,-1,true);
    safety_kernel_register_device("bedroom","fan",DEVICE_TYPE_MOCK,-1,true);
    Automation restored;restored.restore_policy(decoded);RoomReport report;restored.report(report,100);
    bool valid=report.auto_flags==2 && report.mode==1 && report.grace_ms==45000 && report.manual_flags==0 && !report.timer_light_ms && !report.timer_fan_ms && !safety_kernel_get_device(0)->is_on && !safety_kernel_get_device(1)->is_on;
    record[4]^=1;
    return valid&&!decode_policy(record,sizeof(record),decoded);
}
