#include "platform_hal.h"
#include "network_hal.h"
#include <esp_http_server.h>
#include "clawgrid_config.h"
#include "secure_link.h"
#include "control_wire.h"
#include "neural_agent.h"
#include "memory_runtime.h"
#include "rx_mailbox.h"
#include "runtime_selftest.h"
#include "hardware_status.h"
#include "security_selftest.h"
#include "http_selftest.h"
#include "rtos_runtime.h"
#include "house_simulation.h"
#include "simulation_dashboard.h"
#include "speech_runtime.h"
#include "serial_bridge.h"
#include "evidence_selftest.h"
#include "command_target.h"
#include "room_gateway.h"
#include "serial_text_command.h"
#include "dashboard_client.h"
#include "pcm16.h"
#include <esp_system.h>
#include <esp_wifi.h>
#include <esp_netif.h>

static RxMailbox s_rx;
static httpd_handle_t s_http=nullptr;
static portMUX_TYPE s_mux=portMUX_INITIALIZER_UNLOCKED;
static RoomGateway s_gateway;
// HTTP has one SDK task; LinkTask has one owner. Selectors are owned by their
// respective task, not a shared mutable "selected room". Diagnostics is room1.
static TaskHandle_t s_http_owner=nullptr,s_link_owner=nullptr;
static unsigned s_http_room=0,s_link_room=0;
static unsigned active_room() {
    auto task=xTaskGetCurrentTaskHandle();
    return task==s_link_owner?s_link_room:task==s_http_owner?s_http_room:0;
}
static PhysicalRoomLink &room_link() { return s_gateway.rooms[active_room()]; }
static const uint8_t *room_mac(unsigned i) { return i?kNodeC:kNodeA; }
static const uint8_t *room_key(unsigned i) { return i?kRoom2HmacKey:kHmacKey; }
static bool room_provisioned(unsigned i) { return i==0 || kRoom2Provisioned; }
#define s_report (room_link().report)
#define s_received (room_link().received)
#define s_have (room_link().have)
#define s_last_seq (room_link().sequence)
#define s_last_control (room_link().last_control)
#define s_transaction (room_link().transaction)
#define s_proposal (room_link().proposal)

static SimState s_sim_report;
static uint32_t s_sim_received=0,s_sim_sequence=0;
static uint32_t s_accepted=0,s_invalid=0,s_replayed=0,s_nonprogress=0;
static std::atomic<uint32_t> s_seq{0},s_tx_failed{0};
static RuntimeHealth s_health;
static DiagnosticLog s_log;
static TaskTiming s_link_timing;
static StaticTask_t s_link_tcb;
static StackType_t s_link_stack[6144];

static bool s_radio=false,s_ok=false;
using Transaction=RoomTransaction;
static char s_json[4096],s_body[128]; // HTTP task only; bounded speech diagnostics
static char s_serial[160]; static size_t s_serial_used=0; static bool s_serial_bad=false;
static void receive(const uint8_t *mac,const uint8_t *data,int n) { s_rx.push(mac,data,n); }
static esp_err_t json(httpd_req_t *req,int n) {
    if(n<0 || (size_t)n>=sizeof(s_json)) return httpd_resp_send_err(req,HTTPD_500_INTERNAL_SERVER_ERROR,"Response limit");
    if(n>0 && s_json[n-1]=='}' && !strstr(s_json,"\"node_id\":")) {
        int extra=snprintf(s_json+n-1,sizeof(s_json)-(size_t)n+1,",\"node_id\":%u}",RoomGateway::node(active_room()));
        if(extra<0 || (size_t)extra>=sizeof(s_json)-(size_t)n+1) return httpd_resp_send_err(req,HTTPD_500_INTERNAL_SERVER_ERROR,"Response limit");
        n=n-1+extra;
    }
    httpd_resp_set_type(req,"application/json"); httpd_resp_set_hdr(req,"Cache-Control","no-store");
    return httpd_resp_send(req,s_json,n);
}
static bool authorized(httpd_req_t *req) {
    char token[64]; size_t n=httpd_req_get_hdr_value_len(req,"X-ClawGrid-Token");
    if(n!=strlen(kApiToken) || n>=sizeof(token) || httpd_req_get_hdr_value_str(req,"X-ClawGrid-Token",token,sizeof(token))!=ESP_OK) return false;
    uint8_t delta=0; for(size_t i=0;i<n;++i) delta|=token[i]^kApiToken[i]; return delta==0;
}
static esp_err_t unauthorized(httpd_req_t *req) {
    httpd_resp_set_status(req,"401 Unauthorized");
    return json(req,snprintf(s_json,sizeof(s_json),"{\"error\":\"Control token required\"}"));
}
static bool read_body(httpd_req_t *req) {
    if(!req->content_len || req->content_len>=sizeof(s_body)) return false;
    size_t n=0;
    while(n<req->content_len) { int got=httpd_req_recv(req,s_body+n,req->content_len-n); if(got<=0) return false; n+=got; }
    for(size_t i=0;i<n;++i) if((uint8_t)s_body[i]<32 || (uint8_t)s_body[i]>126) return false;
    s_body[n]=0; return true;
}
static esp_err_t bad_body(httpd_req_t *req) {
    httpd_resp_set_status(req,"400 Bad Request"); httpd_resp_set_hdr(req,"Connection","close");
    json(req,snprintf(s_json,sizeof(s_json),"{\"error\":\"Use 1 to 127 ASCII bytes\"}")); return ESP_FAIL;
}
static bool parse_fields(const char *text,ControlCommand &c) {
    return parse_control_text(text,c);
}
static bool queue_command(ControlCommand c,uint32_t &id) {
#if defined(CLAWGRID_LIGHT_PREFLIGHT)
    return false; // No actuator/serial control dispatch in a recognition-only image.
#endif
    if(!s_ok || !s_radio || !s_health.healthy(kLinkReady) || !room_provisioned(active_room())) return false;
    portENTER_CRITICAL(&s_mux);
    bool accepted=s_gateway.queue(active_room(),c,millis(),micros(),id);
    portEXIT_CRITICAL(&s_mux); return accepted;
}
static esp_err_t root(httpd_req_t *req) {
    httpd_resp_set_type(req,"text/html; charset=utf-8");
    httpd_resp_set_hdr(req,"Cache-Control","no-store");
    httpd_resp_set_hdr(req,"Content-Security-Policy","default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'unsafe-inline'; connect-src 'self'; frame-ancestors 'none'");
    return httpd_resp_send(req,kHardwareStatus,sizeof(kHardwareStatus)-1);
}
static esp_err_t simulation_page(httpd_req_t *req) {
    httpd_resp_set_type(req,"text/html; charset=utf-8");
    httpd_resp_set_hdr(req,"Content-Security-Policy","default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'unsafe-inline'; connect-src 'self'; media-src blob:; frame-ancestors 'none'");
    return httpd_resp_send(req,kSimulationDashboard,sizeof(kSimulationDashboard)-1);
}
static esp_err_t simulation_state_api(httpd_req_t *req) {
    portENTER_CRITICAL(&s_mux); SimState s=s_sim_report; uint32_t age=millis()-s_sim_received; bool known=s_sim_received && age<3000 && s_have && s.boot==s_report.boot_id;
    portEXIT_CRITICAL(&s_mux);
    return json(req,snprintf(s_json,sizeof(s_json),"{\"input_source\":\"SIMULATED\",\"processing_source\":\"HARDWARE_MEASURED\",\"known\":%s,\"boot\":%u,\"sequence\":%u,\"age_ms\":%u,\"outputs\":%u,\"sensor_known\":%u,\"occupied\":%u,\"processing_us\":%u,\"accepted\":%u,\"rejected\":%u,\"physical_outputs_enabled\":%s}",known?"true":"false",s.boot,s.sequence,age,s.outputs,s.known,s.occupied,s.processing_us,s.accepted,s.rejected,s.physical_outputs_enabled?"true":"false"));
}
static esp_err_t simulation_input_api(httpd_req_t *req) {
#if defined(CLAWGRID_LIGHT_PREFLIGHT)
    httpd_resp_set_status(req,"409 Conflict"); return json(req,snprintf(s_json,sizeof(s_json),"{\"error\":\"Recognition-only preflight\"}"));
#endif
    if(!authorized(req)) { httpd_resp_set_hdr(req,"Connection","close"); unauthorized(req); return ESP_FAIL; }
    if(!read_body(req)) return bad_body(req);
    unsigned values[12]={}; int count=0; const char *p=s_body;
    while(*p) {
        while(*p==' ') ++p;
        if(!*p) break;
        if(count==12 || *p<'0' || *p>'9') goto invalid_sim;
        unsigned v=0; while(*p>='0' && *p<='9') { v=v*10+(*p++-'0'); if(v>2000) goto invalid_sim; }
        if(*p && *p!=' ') goto invalid_sim;
        values[count++]=v;
    }
    if(count!=12) goto invalid_sim;
    {
        SimInput input;
        for(int i=0;i<4;++i) {
            if(values[3*i]>63 || values[3*i+1]>600) goto invalid_sim;
            input.flags[i]=values[3*i]; input.temperature[i]=values[3*i+1]; input.lux[i]=values[3*i+2];
        }
        uint32_t now=millis(); bool fresh;
        portENTER_CRITICAL(&s_mux); fresh=s_have && (uint32_t)(now-s_received)<3000;
        input.boot=s_report.boot_id; input.issued_ms=s_report.uptime_ms+(now-s_received);
        if(s_sim_report.boot==input.boot && sequence_newer(s_sim_report.sequence,s_sim_sequence)) s_sim_sequence=s_sim_report.sequence;
        input.sequence=++s_sim_sequence; if(!input.sequence) input.sequence=++s_sim_sequence;
        portEXIT_CRITICAL(&s_mux);
        if(!fresh || !s_radio || !s_health.healthy(kLinkReady)) { httpd_resp_set_status(req,"409 Conflict"); return json(req,snprintf(s_json,sizeof(s_json),"{\"error\":\"Node A unavailable\"}")); }
        uint8_t bytes[kSimInputBytes]; encode_sim_input(input,bytes);
        if(!secure_send(kNodeA,MSG_TYPE_SENSOR_EVENT,2,1,s_seq++,bytes,sizeof(bytes))) { ++s_tx_failed; httpd_resp_set_status(req,"503 Service Unavailable"); return json(req,snprintf(s_json,sizeof(s_json),"{\"error\":\"Radio busy\"}")); }
        httpd_resp_set_status(req,"202 Accepted");
        return json(req,snprintf(s_json,sizeof(s_json),"{\"queued\":true,\"sequence\":%u,\"input_source\":\"SIMULATED\"}",input.sequence));
    }
invalid_sim:
    httpd_resp_set_status(req,"400 Bad Request"); return json(req,snprintf(s_json,sizeof(s_json),"{\"error\":\"Expected flags temperature_deciC lux for four rooms\"}"));
}
static esp_err_t memory_api(httpd_req_t *req) {
    MemorySnapshot m=memory_snapshot();
    return json(req,snprintf(s_json,sizeof(s_json),"{\"source\":\"HARDWARE_MEASURED\",\"internal_free\":%u,\"internal_min\":%u,\"internal_largest\":%u,\"psram_total\":%u,\"psram_free\":%u,\"psram_largest\":%u,\"stack_task\":\"http\",\"stack_free_bytes\":%u,\"rx_dropped\":%u,\"memory_config_ok\":%s}",m.internal_free,m.internal_min,m.internal_largest,m.psram_total,m.psram_free,m.psram_largest,m.stack_free_bytes,s_rx.dropped(),memory_hardware_matches()?"true":"false"));
}
static esp_err_t rtos_api(httpd_req_t *req) {
    // Read-only link/AP diagnostics stay on the HTTP task, outside ControlTask.
    uint8_t channel=0; wifi_second_chan_t secondary;
    bool channel_ok=esp_wifi_get_channel(&channel,&secondary)==ESP_OK;
    wifi_sta_list_t clients={}; bool clients_ok=esp_wifi_ap_get_sta_list(&clients)==ESP_OK;
    esp_netif_ip_info_t ip={}; esp_netif_t *ap=esp_netif_get_handle_from_ifkey("WIFI_AP_DEF");
    bool ip_ok=ap && esp_netif_get_ip_info(ap,&ip)==ESP_OK;

    return json(req,snprintf(s_json,sizeof(s_json),"{\"source\":\"HARDWARE_MEASURED\",\"task\":\"LinkTask\",\"core\":0,\"priority\":3,\"period_ms\":5,\"jobs\":%u,\"max_exec_us\":%u,\"max_late_us\":%u,\"deadline_misses\":%u,\"stack_free_bytes\":%u,\"watchdog\":%s,\"task_count\":%u,\"reset_reason\":%d,\"wifi_channel_known\":%s,\"wifi_channel\":%u,\"ap_ip_valid\":%s,\"ap_ip\":\"%u.%u.%u.%u\",\"ap_clients_known\":%s,\"ap_clients\":%u}",s_link_timing.jobs.load(),s_link_timing.max_exec_us.load(),s_link_timing.max_late_us.load(),s_link_timing.deadline_misses.load(),s_link_timing.handle?(unsigned)uxTaskGetStackHighWaterMark(s_link_timing.handle):0,s_link_timing.watchdog.load()?"true":"false",(unsigned)uxTaskGetNumberOfTasks(),(int)esp_reset_reason(),channel_ok?"true":"false",channel,ip_ok?"true":"false",IP2STR(&ip.ip),clients_ok?"true":"false",(unsigned)clients.num));
}
static esp_err_t state_api(httpd_req_t *req) {
    portENTER_CRITICAL(&s_mux); RoomReport r=s_report; bool have=s_have;
    uint32_t age=millis()-s_received,accepted=s_accepted,invalid=s_invalid,replayed=s_replayed,nonprogress=s_nonprogress;
    portEXIT_CRITICAL(&s_mux); bool known=have&&age<3000;
    if(!known) return json(req,snprintf(s_json,sizeof(s_json),"{\"room\":\"%s\",\"node_id\":%u,\"known\":false,\"occupancy\":\"UNKNOWN\",\"light\":null,\"fan\":null,\"authenticated\":true,\"age_ms\":%u,\"boot_id\":%u,\"sample_uptime_ms\":%u,\"nonprogress\":%u,\"physical_state\":\"not measured\"}",active_room()?"living_room":"bedroom",RoomGateway::node(active_room()),have?age:0,r.boot_id,r.uptime_ms,nonprogress));
    const uint64_t sensor_age=(uint64_t)r.temperature_age_ms+age;
    const bool sensor_valid=temperature_valid(r) && sensor_age<10000;
    const auto sensor_status=r.temperature_status==TemperatureStatus::Ok && !sensor_valid?TemperatureStatus::Stale:r.temperature_status;
    static char sensor_json[2600]; // HTTP owner only; keep the expanded readback off its stack.
    if(sensor_valid) snprintf(sensor_json,sizeof(sensor_json),",\"telemetry_schema\":4,\"temperature_deci_c\":%d,\"humidity_deci_percent\":%u,\"temperature_age_ms\":%u,\"temperature_status\":\"OK\"",r.temperature_deci_c,r.humidity_deci,(unsigned)sensor_age);
    else snprintf(sensor_json,sizeof(sensor_json),",\"telemetry_schema\":4,\"temperature_deci_c\":null,\"humidity_deci_percent\":null,\"temperature_age_ms\":null,\"temperature_status\":\"%s\"",temperature_status_name(sensor_status));
    const char *output_profile=r.led_only_bench?(r.relay_active_low?"LED_INDICATORS_10K_ACTIVE_LOW":"LED_INDICATORS_10K_ACTIVE_HIGH"):r.physical_outputs_enabled?"GENERIC_ENABLED":"INHIBITED";
    const size_t sensor_len=strlen(sensor_json);
    snprintf(sensor_json+sensor_len,sizeof(sensor_json)-sensor_len,",\"control_policy_version\":%u,\"manual_hold_mode\":\"%s\",\"output_profile\":\"%s\"",r.control_policy_version,r.control_policy_version==2?"until_auto":"five_minutes",output_profile);
    const size_t availability_len=strlen(sensor_json);
    snprintf(sensor_json+availability_len,sizeof(sensor_json)-availability_len,",\"pir_available\":%s,\"automation_available\":%s",r.pir_available?"true":"false",(r.pir_available || r.features_version>=2)?"true":"false");
    const size_t light_len=strlen(sensor_json);
    snprintf(sensor_json+light_len,sizeof(sensor_json)-light_len,",\"light_input_available\":%s,\"light_input_raw\":%s,\"light_input_polarity\":\"UNVERIFIED\"",r.light_available?"true":"false",r.light_available?(r.light_raw?"true":"false"):"null");
    if(r.features_version>=2 && active_room()==0) {
        const size_t pos=strlen(sensor_json);
        snprintf(sensor_json+pos,sizeof(sensor_json)-pos,",\"light_dark_high_verified\":true");
    }
    const size_t feature_len=strlen(sensor_json);
    snprintf(sensor_json+feature_len,sizeof(sensor_json)-feature_len,
        ",\"features_version\":%u,\"light_schedule_ms\":%u,\"fan_schedule_ms\":%u,\"light_schedule_action\":%u,\"fan_schedule_action\":%u,\"light_schedule_result\":%u,\"fan_schedule_result\":%u,\"light_control_source\":%u,\"fan_control_source\":%u,\"light_energy_cap_ms\":%u,\"fan_energy_cap_ms\":%u",
        r.features_version,r.schedule_ms[0],r.schedule_ms[1],r.schedule_action[0],r.schedule_action[1],r.schedule_result[0],r.schedule_result[1],r.control_source[0],r.control_source[1],r.energy_cap_ms[0],r.energy_cap_ms[1]);
    if(r.features_version>=2) for(int i=0;i<2;++i) {
        const size_t pos=strlen(sensor_json); const char *dev=i?"fan":"light";
        snprintf(sensor_json+pos,sizeof(sensor_json)-pos,
            ",\"%s_sensor_config\":%u,\"%s_override_ms\":%u,\"%s_sensor_demand\":%u,\"%s_winner\":%u,\"%s_blocking\":%u,\"%s_override_action\":%u",
            dev,r.sensor_config[i],dev,r.override_ms[i],dev,r.sensor_demand[i],dev,r.winner[i],dev,r.blocking[i],dev,r.override_action[i]);
    }
    if(r.features_version==3) {
        const size_t pos=strlen(sensor_json);
        snprintf(sensor_json+pos,sizeof(sensor_json)-pos,
            ",\"light_sample_age_ms\":%llu,\"temperature_sample_age_ms\":%llu,\"sensor_snapshot_ms\":%u,\"sensor_order\":%u,\"evaluation_cycle\":%u,\"evaluation_started_us\":%u,\"evaluation_clock_valid\":%s,\"light_eval_step\":%u,\"fan_eval_step\":%u,\"light_eval_start_us\":%u,\"fan_eval_start_us\":%u,\"light_eval_elapsed_us\":%u,\"fan_eval_elapsed_us\":%u,\"light_sensor_requested\":%u,\"fan_sensor_requested\":%u,\"light_sensor_result\":%u,\"fan_sensor_result\":%u",
            (unsigned long long)(r.light_age_ms==UINT32_MAX?UINT32_MAX:(uint64_t)r.light_age_ms+age),
            (unsigned long long)(r.temperature_sample_age_ms==UINT32_MAX?UINT32_MAX:(uint64_t)r.temperature_sample_age_ms+age),
            r.sensor_snapshot_ms,r.sensor_order,r.evaluation_cycle,r.evaluation_started_us,r.evaluation_clock_valid?"true":"false",
            r.evaluation_step[0],r.evaluation_step[1],r.rule_started_us[0],r.rule_started_us[1],r.rule_elapsed_us[0],r.rule_elapsed_us[1],r.sensor_requested[0],r.sensor_requested[1],r.sensor_result[0],r.sensor_result[1]);
    }
    return json(req,snprintf(s_json,sizeof(s_json),"{\"room\":\"%s\",\"node_id\":%u,\"known\":true,\"occupancy\":\"%s\",\"motion\":%s,\"light\":%s,\"fan\":%s,\"age_ms\":%u,\"reports\":%u,\"invalid\":%u,\"replayed\":%u,\"boot_id\":%u,\"sample_uptime_ms\":%u,\"nonprogress\":%u,\"authenticated\":true,\"mode\":\"%s\",\"auto_flags\":%u,\"manual_flags\":%u,\"grace_ms\":%u,\"light_timer_ms\":%u,\"fan_timer_ms\":%u,\"light_on_ms\":%u,\"fan_on_ms\":%u,\"last_command\":%u,\"physical_state\":\"not measured\",\"physical_outputs_enabled\":%s,\"relay_active_low_assumption\":%s%s}",active_room()?"living_room":"bedroom",RoomGateway::node(active_room()),r.pir_available?occupancy_name(r.occupancy):"UNAVAILABLE",r.pir_available?(r.motion?"true":"false"):"null",r.light?"true":"false",r.fan?"true":"false",age,accepted,invalid,replayed,r.boot_id,r.uptime_ms,nonprogress,r.mode?"AWAY":"HOME",r.auto_flags,r.manual_flags,r.grace_ms,r.timer_light_ms,r.timer_fan_ms,r.light_on_ms,r.fan_on_ms,r.last_command,r.physical_outputs_enabled?"true":"false",r.relay_active_low?"true":"false",sensor_json));
}
static esp_err_t transaction_api(httpd_req_t *req) {
    if(!authorized(req)) return unauthorized(req);
    portENTER_CRITICAL(&s_mux); Transaction t=s_transaction; portEXIT_CRITICAL(&s_mux);
    return json(req,snprintf(s_json,sizeof(s_json),"{\"source\":\"HARDWARE_MEASURED\",\"id\":%u,\"phase\":%u,\"status\":%u,\"light\":%s,\"fan\":%s,\"attempts\":%u,\"rtt_us\":%u,\"processing_us\":%u,\"outcome_known\":%s}",t.command.id,t.phase,(unsigned)t.ack.status,t.ack.light?"true":"false",t.ack.fan?"true":"false",t.attempts,t.rtt_us,t.ack.processing_us,t.phase==2?"true":"false"));
}
static esp_err_t control_api(httpd_req_t *req) {
    if(!authorized(req)) { httpd_resp_set_hdr(req,"Connection","close"); unauthorized(req); return ESP_FAIL; }
    if(!read_body(req)) return bad_body(req);
    ControlCommand c; uint32_t id=0;
    if(!parse_api_control(s_body,c)) { httpd_resp_set_status(req,"400 Bad Request"); return json(req,snprintf(s_json,sizeof(s_json),"{\"error\":\"Expected boot ID op device action value for configuration\"}")); }
    if(!queue_command(c,id)) { httpd_resp_set_status(req,"409 Conflict"); return json(req,snprintf(s_json,sizeof(s_json),"{\"error\":\"Busy or Node A unavailable\"}")); }
    httpd_resp_set_status(req,"202 Accepted");
    return json(req,snprintf(s_json,sizeof(s_json),"{\"queued\":true,\"id\":%u,\"message\":\"Awaiting SafetyKernel acknowledgement\"}",id));
}
static bool proposal_controller(uint32_t &boot) {
    uint32_t now=millis();
    portENTER_CRITICAL(&s_mux);
    boot=s_report.boot_id;
    bool fresh=s_have && (uint32_t)(now-s_received)<3000;
    portEXIT_CRITICAL(&s_mux);
    return fresh && s_ok && s_radio && s_health.healthy(kLinkReady);
}
static esp_err_t proposal_failure(httpd_req_t *req,ProposalResult result) {
    httpd_resp_set_status(req,result==ProposalResult::Invalid?"400 Bad Request":"409 Conflict");
    return json(req,snprintf(s_json,sizeof(s_json),"{\"error\":\"%s\",\"authorized\":false}",proposal_error(result)));
}
static bool speech_matches(const SpeechSnapshot &s,uint32_t job,const CommandTarget &target,uint32_t boot,bool fresh) {
    CommandTarget expected;
    if(s.phase!=4 || s.job!=job || s.error || s.engine_errors) return false;
    if(kShortVoice) {
        if(!short_voice_target(s.binding,boot,fresh,millis(),s.command_id,s.confidence,speech_is_reference(s.input),expected)) return false;
    } else if(!speech_target(s.command_id,target.mode,expected)) return false;
    return same_target(expected,target);
}
static esp_err_t proposal_api(httpd_req_t *req) {
#if defined(CLAWGRID_LIGHT_PREFLIGHT)
    s_proposal.consume(); return proposal_failure(req,ProposalResult::Invalid);
#endif
    if(!authorized(req)) { httpd_resp_set_hdr(req,"Connection","close"); unauthorized(req); return ESP_FAIL; }
    if(!read_body(req)) return bad_body(req);
    char source[24]={}; CommandTarget target; uint32_t job=0,boot=0,captured_boot=0;
    bool from_phone=phone_proposal_fields(s_body,target,job,captured_boot);
    if(!from_phone && (!proposal_fields(s_body,source,target,job) || (strcmp(source,"manual") && strcmp(source,"speech")))) {
        s_proposal.consume(); return proposal_failure(req,ProposalResult::Invalid);
    }
    if(target.mode==TargetMode::PhysicalBedroom && (unsigned)target.room!=active_room()) {
        s_proposal.consume(); return proposal_failure(req,ProposalResult::Mismatch);
    }
    bool fresh=proposal_controller(boot);
    if(from_phone && captured_boot!=boot) {
        s_proposal.consume(); return proposal_failure(req,ProposalResult::Mismatch);
    }
    bool from_speech=!strcmp(source,"speech");
    if(from_speech && active_room()!=0) return proposal_failure(req,ProposalResult::Invalid);
    if(from_speech) {
        SpeechSnapshot s=speech_snapshot();
        if(!speech_matches(s,job,target,boot,fresh)) {
            s_proposal.consume(); return proposal_failure(req,ProposalResult::Mismatch);
        }
    } else if(job && !from_phone) { s_proposal.consume(); return proposal_failure(req,ProposalResult::Invalid); }
    ProposalResult result=s_proposal.prepare(target,boot,fresh,millis(),from_speech,job);
    if(result!=ProposalResult::Ok) return proposal_failure(req,result);
    return json(req,snprintf(s_json,sizeof(s_json),"{\"proposal_id\":%u,\"target\":\"%s\",\"room\":\"%s\",\"device\":\"%s\",\"action\":\"%s\",\"expires_in_ms\":15000,\"authorized\":false,\"origin\":\"%s\",\"controller_boot\":%u,\"capture_id\":%u}",s_proposal.id,target_mode(target.mode),target_room(target.room),target_device(target.device),target_action(target.action),from_phone?"phone":source,boot,job));
}
static esp_err_t text_api(httpd_req_t *req) {
#if defined(CLAWGRID_LIGHT_PREFLIGHT)
    s_proposal.consume(); return proposal_failure(req,ProposalResult::Invalid);
#endif
    if(!authorized(req)) { httpd_resp_set_hdr(req,"Connection","close"); unauthorized(req); return ESP_FAIL; }
    if(!read_body(req)) return bad_body(req);
    SerialTextCommand parsed; uint32_t boot=0;
    if(!parse_serial_text(s_body,parsed)) {
        s_proposal.consume(); return proposal_failure(req,ProposalResult::Invalid);
    }
    bool fresh=proposal_controller(boot);
    ProposalResult result=s_proposal.prepare(parsed.target,boot,fresh,millis(),false,0);
    if(result!=ProposalResult::Ok) return proposal_failure(req,result);
    const CommandTarget &t=parsed.target;
    return json(req,snprintf(s_json,sizeof(s_json),"{\"proposal_id\":%u,\"target\":\"%s\",\"room\":\"%s\",\"device\":\"%s\",\"action\":\"%s\",\"requested_room\":\"%s\",\"bench_alias\":%s,\"acoustic_fallback\":%s,\"expires_in_ms\":15000,\"authorized\":false}",s_proposal.id,target_mode(t.mode),target_room(t.room),target_device(t.device),target_action(t.action),parsed.requested_hall?"hall":"bedroom",parsed.bench_alias?"true":"false",parsed.acoustic_fallback?"true":"false"));
}
static esp_err_t confirm_api(httpd_req_t *req) {
#if defined(CLAWGRID_LIGHT_PREFLIGHT)
    s_proposal.consume(); return proposal_failure(req,ProposalResult::Invalid);
#endif
    if(!authorized(req)) { httpd_resp_set_hdr(req,"Connection","close"); unauthorized(req); return ESP_FAIL; }
    if(!read_body(req)) return bad_body(req);
    char first[24]; CommandTarget target; uint32_t requested=0,boot=0,id=0; ControlCommand c;
    if(!proposal_fields(s_body,first,target,requested,true)) return proposal_failure(req,ProposalResult::Invalid);
    if(target.mode==TargetMode::PhysicalBedroom && (unsigned)target.room!=active_room()) return proposal_failure(req,ProposalResult::Mismatch);
    bool fresh=proposal_controller(boot);
    ProposalResult result=s_proposal.confirm(requested,target,boot,fresh,millis(),c);
    if(result!=ProposalResult::Ok) return proposal_failure(req,result);
    if(s_proposal.speech) {
        SpeechSnapshot s=speech_snapshot();
        if(!speech_matches(s,s_proposal.job,target,boot,fresh))
            return proposal_failure(req,ProposalResult::Mismatch);
    }
    if(!queue_command(c,id)) return proposal_failure(req,ProposalResult::Unavailable);
    s_proposal.consume(); httpd_resp_set_status(req,"202 Accepted");
    return json(req,snprintf(s_json,sizeof(s_json),"{\"queued\":true,\"id\":%u,\"message\":\"Confirmed and submitted; awaiting software ACK\",\"physical_state\":\"not measured\"}",id));
}
static esp_err_t voice_api(httpd_req_t *req) {
    if(!read_body(req)) return bad_body(req);
    neural_parse_result_t r={}; uint32_t start=micros(); neural_agent_process(s_body,nullptr,&r);
    return json(req,snprintf(s_json,sizeof(s_json),"{\"source\":\"HARDWARE_MEASURED\",\"intent\":%d,\"action\":\"%s\",\"room\":\"%s\",\"device\":\"%s\",\"requires_clarification\":%s,\"authorized\":false,\"parser_us\":%u}",(int)r.intent,r.action,r.room,r.device,r.requires_clarification?"true":"false",micros()-start));
}
static esp_err_t speech_state_api(httpd_req_t *req) {
    if(!authorized(req)) return unauthorized(req);
    SpeechSnapshot s=speech_snapshot();
#if defined(CLAWGRID_ESP_SR)
    const char *supported="true";
#else
    const char *supported="false";
#endif
    return json(req,snprintf(s_json,sizeof(s_json),"{\"processing_source\":\"HARDWARE_MEASURED\",\"supported\":%s,\"engine\":\"%s\",\"phase\":%u,\"job\":%u,\"command_id\":%u,\"confidence\":%.5f,\"samples\":%u,\"inference_us\":%u,\"max_frame_us\":%u,\"frames\":%u,\"stack_free_bytes\":%u,\"pcm_crc32\":%u,\"diagnostic_text\":\"%s\",\"error_code\":%u,\"last_state\":%u,\"candidate_id\":%u,\"candidate_score\":%.5f,\"peak\":%u,\"clipped_samples\":%u,\"nonzero_samples\":%u,\"chunk_samples\":%u,\"padded_samples\":%u,\"detecting_frames\":%u,\"engine_errors\":%u,\"model_name\":\"%s\",\"sample_rate_hz\":16000,\"channels\":1,\"pcm_format\":\"PCM_S16LE\",\"model_window_ms\":%u,\"planned_frames\":%u,\"fed_samples\":%u,\"command_count\":%u,\"rms\":%u,\"dc_mean\":%d,\"diagnostic_only\":%s,\"reference_detected\":%s,\"legacy_feed\":%s,\"window_exhausted\":%s,\"raw_result_count\":%d,\"raw_result_state\":%u,\"afe_feed_frames\":%u,\"afe_fetch_frames\":%u,\"afe_samples\":%u,\"afe_crc32\":%u,\"afe_peak\":%u,\"afe_rms\":%u,\"mn_internal_free\":%u,\"mn_internal_min\":%u,\"mn_internal_largest\":%u,\"mn_psram_free\":%u,\"grammar_id\":\"%s\",\"bound_room\":\"%s\",\"bound_boot\":%u,\"capture_id\":%u,\"screen_only\":%s,\"authorized\":false}",supported,kVoiceEngine,s.phase,s.job,s.command_id,s.confidence,s.samples,s.inference_us,s.max_frame_us,s.frames,s.stack_free,s.pcm_crc32,s.diagnostic_text,s.error,s.last_state,s.candidate_id,s.candidate_score,s.peak,s.clipped,s.nonzero,s.chunk_samples,s.padded_samples,s.detecting_frames,s.engine_errors,s.model_name,s.model_window_ms,s.planned_frames,s.frames*s.chunk_samples,s.command_count,s.rms,s.dc_mean,speech_is_reference(s.input)?"true":"false",s.reference_detected?"true":"false",s.input==SpeechInput::ReferenceLegacy?"true":"false",s.window_exhausted?"true":"false",s.raw_result_count,s.raw_result_state,s.afe_feed_frames,s.afe_fetch_frames,s.afe_samples,s.afe_crc32,s.afe_peak,s.afe_rms,s.internal_free,s.internal_min,s.internal_largest,s.psram_free,kVoiceGrammar,s.binding.bedroom?"bedroom":"",s.binding.boot,s.binding.capture,s.binding.screen?"true":"false"));
}
static esp_err_t receive_speech_audio(httpd_req_t *req,SpeechInput input) {
    if(!authorized(req)) { httpd_resp_set_hdr(req,"Connection","close"); unauthorized(req); return ESP_FAIL; }
    if(speech_is_reference(input)) {
        portENTER_CRITICAL(&s_mux);
        bool safe=s_ok && s_radio && s_have && (uint32_t)(millis()-s_received)<3000 && !s_report.physical_outputs_enabled;
        portEXIT_CRITICAL(&s_mux);
        if(!safe) { httpd_resp_set_status(req,"409 Conflict"); httpd_resp_set_hdr(req,"Connection","close");
            return json(req,snprintf(s_json,sizeof(s_json),"{\"error\":\"Reference test requires fresh inhibited controller\"}")); }
    }
    if(!pcm_valid_size(req->content_len)) {
        httpd_resp_set_hdr(req,"Connection","close"); httpd_resp_set_status(req,"400 Bad Request");
        json(req,snprintf(s_json,sizeof(s_json),"{\"error\":\"Use 16000 Hz mono signed PCM16LE, 3200 to 128000 bytes\"}")); return ESP_FAIL;
    }
    if(speech_is_reference(input) && input!=SpeechInput::ShortPreflight && input!=SpeechInput::Mn7Official && req->content_len!=speech_reference_bytes(input)) {
        httpd_resp_set_hdr(req,"Connection","close"); httpd_resp_set_status(req,"400 Bad Request");
        return json(req,snprintf(s_json,sizeof(s_json),"{\"error\":\"Use the exact pinned official reference PCM\"}"));
    }
    SpeechBinding binding;
    #if defined(CLAWGRID_LIGHT_PREFLIGHT)
    if(input!=SpeechInput::ShortPreflight) return proposal_failure(req,ProposalResult::Invalid);
    bool bind_reference=true;
#else
    bool bind_reference=false;
#endif
    if(kShortVoice && (input==SpeechInput::Application || bind_reference)) {
        char room[24]={},boot_text[16]={},capture_text[16]={},intent[16]={},grammar[24]={};
        uint32_t boot=0; bool fresh=proposal_controller(boot);
        bool headers=httpd_req_get_hdr_value_str(req,"X-ClawGrid-Room",room,sizeof(room))==ESP_OK &&
            httpd_req_get_hdr_value_str(req,"X-ClawGrid-Boot",boot_text,sizeof(boot_text))==ESP_OK &&
            httpd_req_get_hdr_value_str(req,"X-ClawGrid-Capture",capture_text,sizeof(capture_text))==ESP_OK &&
            httpd_req_get_hdr_value_str(req,"X-ClawGrid-Intent",intent,sizeof(intent))==ESP_OK &&
            httpd_req_get_hdr_value_str(req,"X-ClawGrid-Grammar",grammar,sizeof(grammar))==ESP_OK;
        binding.bedroom=!strcmp(room,"bedroom"); binding.screen=!strcmp(intent,"screen"); binding.created=millis();
        if(bind_reference && !binding.screen) return proposal_failure(req,ProposalResult::Invalid);
        portENTER_CRITICAL(&s_mux); bool inhibited=!s_report.physical_outputs_enabled; portEXIT_CRITICAL(&s_mux);
        if(!headers || strcmp(grammar,kVoiceGrammar) || (!binding.screen && strcmp(intent,"control")) ||
            !binding.bedroom || !parse_number(boot_text,binding.boot) || !parse_number(capture_text,binding.capture) ||
            !binding.capture || !fresh || binding.boot!=boot || !inhibited) {
            s_proposal.consume(); httpd_resp_set_status(req,"409 Conflict"); httpd_resp_set_hdr(req,"Connection","close");
            return json(req,snprintf(s_json,sizeof(s_json),"{\"error\":\"Explicit Bedroom, current boot, capture ID and inhibited short-v1 context required\",\"authorized\":false}"));
        }
    }
    // A new clip invalidates any unconfirmed speech proposal; no old ON replay.
    s_proposal.consume();
    uint32_t job=0; uint8_t *buffer=speech_reserve_clip(req->content_len,job,input,binding);
    if(!buffer) { httpd_resp_set_status(req,"409 Conflict"); httpd_resp_set_hdr(req,"Connection","close");
        json(req,snprintf(s_json,sizeof(s_json),"{\"error\":\"Speech model unavailable or busy\"}")); return ESP_FAIL;
    }
    size_t received=0; uint32_t upload_start=millis();
    while(received<req->content_len) {
        if((uint32_t)(millis()-upload_start)>5000) { speech_finish_clip(false); return ESP_FAIL; }
        int n=httpd_req_recv(req,(char*)buffer+received,std::min((size_t)4096,req->content_len-received));
        if(n<=0) { speech_finish_clip(false); return ESP_FAIL; }
        received+=n;
    }
    if(!speech_finish_clip(true)) {
        httpd_resp_set_status(req,"400 Bad Request");
        return json(req,snprintf(s_json,sizeof(s_json),"{\"error\":\"Reference PCM checksum mismatch; not inferred\"}"));
    }
    httpd_resp_set_status(req,"202 Accepted");
    return json(req,snprintf(s_json,sizeof(s_json),"{\"queued\":true,\"job\":%u,\"authorized\":false}",job));
}
static esp_err_t speech_audio_api(httpd_req_t *req) { return receive_speech_audio(req,SpeechInput::Application); }
static esp_err_t speech_reference_api(httpd_req_t *req) {
    char query[24]={}; SpeechInput input=SpeechInput::ReferenceWindow;
    if(kShortVoice) {
        if(httpd_req_get_url_query_str(req,query,sizeof(query))!=ESP_OK) return proposal_failure(req,ProposalResult::Invalid);
#if defined(CLAWGRID_LIGHT_PREFLIGHT)
        const char *expected_feed="feed=light-v1";
#elif defined(CLAWGRID_MN7_EVAL)
        if(!strcmp(query,"feed=mn7-official")) return receive_speech_audio(req,SpeechInput::Mn7Official);
        const char *expected_feed="feed=mn7-v1";
#else
        const char *expected_feed="feed=short-v1";
#endif
        if(strcmp(query,expected_feed)) {
            httpd_resp_set_status(req,"400 Bad Request"); return json(req,snprintf(s_json,sizeof(s_json),"{\"error\":\"Only frozen short-v1 preflight is enabled in this experiment\"}"));
        }
        return receive_speech_audio(req,SpeechInput::ShortPreflight);
    }
    if(httpd_req_get_url_query_len(req)) {
        if(httpd_req_get_url_query_str(req,query,sizeof(query))!=ESP_OK) return bad_body(req);
        if(strcmp(query,"feed=legacy")==0) input=SpeechInput::ReferenceLegacy;
        else if(strcmp(query,"feed=reset-first")==0) input=SpeechInput::ReferenceResetFirst;
        else if(strcmp(query,"feed=fresh-window")==0) input=SpeechInput::ReferenceFreshWindow;
        else if(strcmp(query,"feed=official")==0) input=SpeechInput::ReferenceOfficial;
        else if(strcmp(query,"feed=pinned")==0) input=SpeechInput::ReferencePinned;
        else if(strcmp(query,"feed=afe")==0) input=SpeechInput::ReferenceAfe;
        else if(strcmp(query,"feed=phone-afe")==0) input=SpeechInput::ReferencePhoneAfe;
        else if(strcmp(query,"feed=phone-bedroom")==0) input=SpeechInput::ReferencePhoneBedroom;
        else if(strcmp(query,"feed=phone-trace")==0) input=SpeechInput::ReferencePhoneTrace;
        else if(strcmp(query,"feed=zira-trace")==0) input=SpeechInput::ReferenceZiraTrace;
        else if(strcmp(query,"feed=silence-trace")==0) input=SpeechInput::ReferenceSilenceTrace;
        else if(strcmp(query,"feed=noncommand-trace")==0) input=SpeechInput::ReferenceNonCommandTrace;
        else if(strcmp(query,"feed=phone-split")==0) input=SpeechInput::ReferencePhoneSplit;
        else if(strcmp(query,"feed=zira-split")==0) input=SpeechInput::ReferenceZiraSplit;
        else if(strcmp(query,"feed=silence-split")==0) input=SpeechInput::ReferenceSilenceSplit;
        else if(strcmp(query,"feed=noncommand-split")==0) input=SpeechInput::ReferenceNonCommandSplit;
        else if(strcmp(query,"feed=trace")==0) input=SpeechInput::ReferenceTrace;
        else if(strcmp(query,"feed=direct")==0) input=SpeechInput::ReferenceDirect;
        else if(strcmp(query,"feed=sdk-heap")==0) input=SpeechInput::ReferenceSdkHeap;
        else if(strcmp(query,"feed=psram")==0) input=SpeechInput::ReferencePsram;
        else if(strcmp(query,"feed=gain15")==0) input=SpeechInput::ReferenceGain15;
        else if(strcmp(query,"feed=synthetic")==0) input=SpeechInput::ReferenceSynthetic;
        else if(strcmp(query,"feed=window")!=0) return bad_body(req);
    }
    return receive_speech_audio(req,input);
}
static esp_err_t evidence_api(httpd_req_t *req) {
    if(!authorized(req)) return unauthorized(req);
    // Pure shadow verification: synthetic vectors, no device requests/GPIO.
    uint32_t started=micros(),mask=evidence_campaign(),elapsed=micros()-started;
    return json(req,snprintf(s_json,sizeof(s_json),"{\"processing_source\":\"HARDWARE_MEASURED\",\"input_source\":\"SIMULATED\",\"scope\":\"shadow monitor selftest; no actuation\",\"passed_mask\":%u,\"cases\":%u,\"execution_us\":%u,\"monitor_bytes\":%u,\"physical_verification\":false}",mask,kEvidenceCases,elapsed,(unsigned)sizeof(EvidenceMonitor)));
}
static esp_err_t client_api(httpd_req_t *req) {
    httpd_resp_set_type(req,"application/javascript; charset=utf-8");
    return httpd_resp_send(req,kDashboardClient,sizeof(kDashboardClient)-1);
}
struct RoutedHandler { esp_err_t (*handler)(httpd_req_t*); bool scoped; };
static RoutedHandler s_handlers[18];
static esp_err_t room_route(httpd_req_t *req) {
    s_http_owner=xTaskGetCurrentTaskHandle(); s_http_room=0;
    auto *route=static_cast<RoutedHandler*>(req->user_ctx);
    const size_t n=httpd_req_get_url_query_len(req);
    if(n) {
        char query[64],value[8];
        if(n>=sizeof(query) || httpd_req_get_url_query_str(req,query,sizeof(query))!=ESP_OK) return bad_body(req);
        if(httpd_query_key_value(query,"room",value,sizeof(value))==ESP_OK) {
            if(strcmp(value,"1") && strcmp(value,"2")) return bad_body(req);
            s_http_room=!strcmp(value,"2")?1:0;
        } else if(strstr(query,"room=")) return bad_body(req);
    }
#if defined(CLAWGRID_ROOM1_DEMO_ONLY)
    if(s_http_room) {
        httpd_resp_set_status(req,"409 Conflict");
        return json(req,snprintf(s_json,sizeof(s_json),"{\"room\":\"living_room\",\"node_id\":3,\"known\":false,\"error\":\"Room 2 unavailable/unverified; outside final physical acceptance\"}"));
    }
#endif
    if(s_http_room && (!route->scoped || !kRoom2Provisioned)) {
        httpd_resp_set_status(req,"409 Conflict"); return json(req,snprintf(s_json,sizeof(s_json),"{\"error\":\"Room2 unavailable or unsupported endpoint\"}"));
    }
    return route->handler(req);
}
static bool start_http() {
    httpd_config_t c=HTTPD_DEFAULT_CONFIG(); c.stack_size=8192; c.core_id=1; c.max_open_sockets=2;
    c.max_uri_handlers=18; c.lru_purge_enable=true; c.recv_wait_timeout=1; c.send_wait_timeout=1;
    if(httpd_start(&s_http,&c)!=ESP_OK) return false;
    const char *paths[]={"/","/api/state","/api/memory","/api/transaction","/api/control","/api/voice","/api/rtos","/simulation","/api/simulation","/api/simulation/input","/api/speech","/api/speech/audio","/api/evidence","/api/proposal","/api/confirm","/api/client","/api/text","/api/speech/reference"};
    esp_err_t (*handlers[])(httpd_req_t*)={root,state_api,memory_api,transaction_api,control_api,voice_api,rtos_api,simulation_page,simulation_state_api,simulation_input_api,speech_state_api,speech_audio_api,evidence_api,proposal_api,confirm_api,client_api,text_api,speech_reference_api};
    for(int i=0;i<18;++i) { httpd_uri_t r={}; r.uri=paths[i]; r.method=((i>=4&&i<6)||i==9||i==11||i==13||i==14||i==16||i==17)?HTTP_POST:HTTP_GET; s_handlers[i]={handlers[i],i==1||i==2||i==3||i==4||i==6||i==13||i==14}; r.handler=room_route; r.user_ctx=&s_handlers[i];
        if(httpd_register_uri_handler(s_http,&r)!=ESP_OK) { httpd_stop(s_http); s_http=nullptr; return false; }
    }
    return true;
}
static void control_tick(uint32_t now) {
    Transaction t; bool send=false,timeout=false;
    portENTER_CRITICAL(&s_mux);
    if(s_transaction.phase==1 && (!s_transaction.attempts || (uint32_t)(now-s_transaction.last_send)>=300)) {
        if(s_transaction.attempts>=4) { s_transaction.phase=3; timeout=true; }
        else { ++s_transaction.attempts; s_transaction.last_send=now; send=true; }
    }
    t=s_transaction; portEXIT_CRITICAL(&s_mux);
    if(send) { uint8_t p[kControlBytes]; encode_control(t.command,p);
        if(!secure_send(room_mac(active_room()),MSG_TYPE_DEVICE_COMMAND,2,RoomGateway::node(active_room()),s_seq++,p,sizeof(p),room_key(active_room()))) ++s_tx_failed;
    }
    if(timeout) traffic_led_event(TrafficError);
    if(timeout) s_log.write("{\"source\":\"HARDWARE_MEASURED\",\"kind\":\"transaction\",\"id\":%u,\"phase\":3,\"attempts\":%u,\"outcome_known\":false}\n",t.command.id,t.attempts);
}
static void process_frame(const RxFrame &f) {
    clawgrid_header_t h; const uint8_t *p=nullptr;
    int incoming=!memcmp(f.mac,kNodeA,6)?0:kRoom2Provisioned&&!memcmp(f.mac,kNodeC,6)?1:-1;
    if(incoming<0) { portENTER_CRITICAL(&s_mux); ++s_invalid; portEXIT_CRITICAL(&s_mux); return; }
    s_link_room=(unsigned)incoming;
    if(!secure_decode(f.mac,room_mac(s_link_room),f.bytes,f.length,h,p,room_key(s_link_room)) ||
       h.src_node!=RoomGateway::node(s_link_room) || h.dst_node!=2) {
        portENTER_CRITICAL(&s_mux); ++s_invalid; portEXIT_CRITICAL(&s_mux); return;
    }
    traffic_led_event(TrafficRx);
    if(h.msg_type==MSG_TYPE_NODE_STATUS) {
        if(s_link_room!=0) return;
        SimState state; if(!decode_sim_state(p,h.payload_len,state)) return;
        portENTER_CRITICAL(&s_mux);
        if(s_have && state.boot==s_report.boot_id && (!s_sim_received || state.boot!=s_sim_report.boot || sequence_newer(state.sequence,s_sim_report.sequence) || (state.sequence==s_sim_report.sequence && sequence_newer(state.at_ms,s_sim_report.at_ms)))) { s_sim_report=state; s_sim_received=millis(); }
        portEXIT_CRITICAL(&s_mux);
    } else if(h.msg_type==MSG_TYPE_ROOM_STATE) {
        RoomReport r;
        if(!decode_room_report(p,h.payload_len,r)) { portENTER_CRITICAL(&s_mux); ++s_invalid; portEXIT_CRITICAL(&s_mux); return; }
        portENTER_CRITICAL(&s_mux);
        if(s_gateway.report(h.src_node,r,h.seq_num,millis())) ++s_accepted;
        else ++s_replayed;
        portEXIT_CRITICAL(&s_mux);
    } else if(h.msg_type==MSG_TYPE_COMMAND_ACK) {
        ControlAck a; if(!decode_ack(p,h.payload_len,a)) return;
        bool accepted=false; Transaction t;
        portENTER_CRITICAL(&s_mux);
        accepted=s_gateway.ack(h.src_node,a,millis(),micros());
        t=s_transaction; portEXIT_CRITICAL(&s_mux);
        if(accepted) traffic_led_event(a.status==ControlStatus::Applied ? TrafficAck : TrafficError);
        if(accepted) s_log.write("{\"source\":\"HARDWARE_MEASURED\",\"kind\":\"transaction\",\"id\":%u,\"phase\":2,\"status\":%u,\"attempts\":%u,\"rtt_us\":%u,\"processing_us\":%u,\"light\":%s,\"fan\":%s,\"outcome_known\":true}\n",a.id,(unsigned)a.status,t.attempts,t.rtt_us,a.processing_us,a.light?"true":"false",a.fan?"true":"false");
    }
}
static void diagnostic_service() {
    // LinkTask continues independently during an HTTP or memory diagnostic.
}
static void link_task(void*) {
    s_link_owner=xTaskGetCurrentTaskHandle();
    s_health.ready(kLinkReady,s_link_timing);
    TickType_t release=xTaskGetTickCount(); uint32_t expected=micros();
    for(;;) {
        uint32_t start=micros();
        RxFrame f; for(int i=0;i<4&&s_rx.pop(f);++i) process_frame(f);
        for(s_link_room=0;s_link_room<2;++s_link_room) control_tick(millis());
        s_link_room=0;
        if(s_link_timing.watchdog.load()) esp_task_wdt_reset();
        s_link_timing.record(start,expected,5000); expected+=5000;
        vTaskDelayUntil(&release,pdMS_TO_TICKS(5));
    }
}
static void serial_tick() {
    for(int i=0;i<80 && Serial.available();++i) {
        char c=Serial.read();
        if(!s_serial_used && !s_serial_bad && c=='S') {
            portENTER_CRITICAL(&s_mux); RoomReport r=s_report; uint32_t age=millis()-s_received; bool known=s_have&&age<3000; portEXIT_CRITICAL(&s_mux);
            Serial.printf("{\"source\":\"HARDWARE_MEASURED\",\"kind\":\"state\",\"known\":%s,\"light\":%s,\"fan\":%s,\"mode\":%u,\"grace_ms\":%u,\"auto_flags\":%u,\"last_command\":%u,\"age_ms\":%u}\n",known?"true":"false",r.light?"true":"false",r.fan?"true":"false",r.mode,r.grace_ms,r.auto_flags,r.last_command,age); continue;
        }
        if(!s_serial_used && !s_serial_bad && (c=='F'||c=='R'||c=='W'||c=='V')) {
            if(!CLAWGRID_DIAGNOSTIC_FAULTS) {
                Serial.print("{\"kind\":\"fault_injection_disabled\",\"node\":\"B\",\"armed\":false}\n");
                continue;
            }
            portENTER_CRITICAL(&s_mux); ControlCommand cmd=s_transaction.command; RoomReport room=s_report; portEXIT_CRITICAL(&s_mux);
            if(c=='W') { cmd.boot=room.boot_id^0x80000000u; cmd.id=room.last_command+1; }
            if(c=='V') cmd.action^=1; // Authenticated altered payload, same ID.
            uint8_t payload[kControlBytes]; encode_control(cmd,payload);
            bool sent=false;
            if(c=='F') {
                uint8_t packet[CLAWGRID_MAX_PACKET],tag[32];
                size_t n=clawgrid_serialize(MSG_TYPE_DEVICE_COMMAND,2,1,s_seq++,0,payload,sizeof(payload),packet,sizeof(packet)-kAuthBytes);
                if(n && auth_tag(packet,n,tag)) { memcpy(packet+n,tag,kAuthBytes); packet[n]^=1; sent=esp_now_send(kNodeA,packet,n+kAuthBytes)==ESP_OK; }
            } else sent=secure_send(kNodeA,MSG_TYPE_DEVICE_COMMAND,2,1,s_seq++,payload,sizeof(payload));
            Serial.printf("{\"source\":\"HARDWARE_MEASURED\",\"kind\":\"injection\",\"type\":\"%c\",\"sent\":%s}\n",c,sent?"true":"false"); continue;
        }
        if(!s_serial_used && !s_serial_bad && c=='H') { http_selftest(diagnostic_service); continue; }
        if(!s_serial_used && !s_serial_bad && (c=='t'||c=='m')) {
            if(c=='m') memory_probe("B");
            else Serial.printf("{\"source\":\"HARDWARE_MEASURED\",\"kind\":\"selftest\",\"node\":\"B\",\"passed\":%s,\"rtos_ready\":%s}\n",(s_ok&&runtime_selftest()&&neural_agent_selftest()&&s_health.healthy(kLinkReady))?"true":"false",s_health.healthy(kLinkReady)?"true":"false");
            continue;
        }
        if(c=='\n') {
            s_serial[s_serial_used]=0; ControlCommand cmd; uint32_t id=0;
            if(!s_serial_bad && s_serial[0]=='@') {
                serial_http_bridge(s_serial); s_serial_used=0; s_serial_bad=false; continue;
            }
            bool ok=!s_serial_bad && s_serial[0]=='C' && parse_fields(s_serial+1,cmd) && queue_command(cmd,id);
            Serial.printf("{\"source\":\"HARDWARE_MEASURED\",\"kind\":\"queued\",\"accepted\":%s,\"id\":%u}\n",ok?"true":"false",id);
            s_serial_used=0; s_serial_bad=false;
        } else if(c!='\r') {
            if(s_serial_used+1<sizeof(s_serial)) s_serial[s_serial_used++]=c; else s_serial_bad=true;
        }
    }
}
void setup() {
    for(auto &room:s_gateway.rooms) room.proposal.seed(esp_random()&0x7fffffffu);
    Serial.begin(115200); traffic_led_begin(); s_rx.begin(); s_log.begin(); neural_agent_init(); s_ok=runtime_selftest()&&neural_agent_selftest()&&security_selftest()&&s_health.begin();
    Serial.printf("{\"kind\":\"firmware_mode\",\"node\":\"B\",\"schema\":5,\"build_mode\":\"%s\"}\n",CLAWGRID_BUILD_MODE);
    Serial.printf("{\"kind\":\"console_transport\",\"transport\":\"%s\"}\n",native_serial_transport());
    WiFi.mode(WIFI_AP_STA); WiFi.setSleep(false);
    bool ap=WiFi.softAP(kApSsid,kApPassword,CLAWGRID_WIFI_CHANNEL,false,2);
    s_radio=ap && esp_now_init()==ESP_OK && secure_peer(kNodeA);
    if(s_radio && kRoom2Provisioned) s_radio=secure_peer(kNodeC,kRoom2Lmk);
    if(s_radio) { esp_now_register_recv_cb(receive); esp_now_register_send_cb(traffic_send_complete); }
    if(s_ok) {
        s_link_timing.handle=xTaskCreateStaticPinnedToCore(link_task,"LinkTask",sizeof(s_link_stack),nullptr,3,s_link_stack,&s_link_tcb,0);
        if(!s_link_timing.handle) s_health.fail();
    }
    bool http=s_ok&&start_http();
    if(s_ok) speech_begin();
    Serial.printf("[B] radio=%s HTTP=%s IP=%s\n",s_radio?"ready":"failed",http?"ready":"failed",WiFi.softAPIP().toString().c_str());
}
void loop() {
    s_log.drain(); serial_tick(); uint32_t now=millis();
    static uint32_t last_memory=0;
    if((uint32_t)(now-last_memory)>=5000) {
        last_memory=now; memory_log("B",s_rx.dropped(),s_tx_failed.load());
        portENTER_CRITICAL(&s_mux); uint32_t a=s_accepted,b=s_invalid,c=s_replayed,d=s_nonprogress; portEXIT_CRITICAL(&s_mux);
        if(Serial) Serial.printf("{\"source\":\"HARDWARE_MEASURED\",\"kind\":\"link\",\"node\":\"B\",\"accepted\":%u,\"invalid\":%u,\"replayed\":%u,\"nonprogress\":%u,\"authenticated\":true,\"radio\":%s}\n",a,b,c,d,s_radio?"true":"false");
        s_link_timing.log("B","LinkTask",0,3,5,s_log.dropped.load());
    }
    vTaskDelay(pdMS_TO_TICKS(5));
}
