// Host-only adapter around actual portable firmware routing and SafetyKernel.
// No ESP32 inference, GPIO, radio, or hardware timing is emulated as measured.
#include "command_target.h"
#include "serial_text_command.h"
#include "house_simulation.h"
#include "automation.h"
#include "pcm16.h"
#include <chrono>
#include <iostream>
#include <string>

int main() {
    safety_kernel_init();
    safety_kernel_register_device("bedroom","light",DEVICE_TYPE_LIGHT,-1,true);
    safety_kernel_register_device("bedroom","fan",DEVICE_TYPE_FAN,-1,true);
    HouseSimulation house; house.begin(); Automation automation; ProposalStore proposal;
    const auto started=std::chrono::steady_clock::now();
    uint32_t offset=0,id=0,sequence=0,boot=1,job=0; uint8_t speech_id=0;
    std::string scenario="connected",line; ControlStatus status=ControlStatus::Invalid; unsigned phase=0;
    while(std::getline(std::cin,line)) {
        uint32_t now=10000+offset+(uint32_t)std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::steady_clock::now()-started).count();
        safety_kernel_tick_periodic(now);
        auto sim=house.tick(boot,now);
        auto physical=[&](int i){return safety_kernel_get_device(i)->is_on?"true":"false";};
        auto fail=[&](const char *error,int code=409){std::cout<<"{\"http_status\":"<<code<<",\"error\":\""<<error<<"\",\"authorized\":false}";};
        bool fresh=scenario!="disconnected"&&scenario!="stale";
        std::string verb=line.substr(0,line.find(' ')),body=line.find(' ')==std::string::npos?"":line.substr(line.find(' ')+1);
        if(verb=="scenario") { scenario=body; proposal.consume(); std::cout<<"{\"scenario\":\""<<scenario<<"\"}"; }
        else if(verb=="advance") { uint32_t value; if(parse_number(body.c_str(),value)) offset+=value; std::cout<<"{}"; }
        else if(verb=="reboot") { ++boot; proposal.consume(); std::cout<<"{}"; }
        else if(verb=="state") {
            std::cout<<"{\"known\":"<<(fresh?"true":"false")<<",\"room\":\"bedroom\",\"occupancy\":\"UNKNOWN\",\"motion\":false,\"light\":"<<(fresh?physical(0):"null")<<",\"fan\":"<<(fresh?physical(1):"null")<<",\"age_ms\":"<<(fresh?0:6000)<<",\"mode\":\""<<(automation.away?"AWAY":"HOME")<<"\",\"auto_flags\":"<<(automation.automatic[0]|automation.automatic[1]<<1)<<",\"manual_flags\":0,\"grace_ms\":"<<automation.grace<<",\"light_timer_ms\":0,\"fan_timer_ms\":0,\"light_on_ms\":0,\"fan_on_ms\":0,\"physical_outputs_enabled\":false,\"physical_state\":\"not measured\"}";
        } else if(verb=="simstate") {
            std::cout<<"{\"known\":"<<(fresh?"true":"false")<<",\"boot\":"<<boot<<",\"sequence\":"<<sequence<<",\"age_ms\":"<<(fresh?0:6000)<<",\"outputs\":"<<(unsigned)sim.outputs<<",\"sensor_known\":"<<(unsigned)sim.known<<",\"occupied\":"<<(unsigned)sim.occupied<<",\"processing_us\":null,\"accepted\":"<<sim.accepted<<",\"rejected\":"<<sim.rejected<<",\"physical_outputs_enabled\":false}";
        } else if(verb=="siminput") {
            unsigned values[12]; char extra; int n=sscanf(body.c_str(),"%u %u %u %u %u %u %u %u %u %u %u %u %c",&values[0],&values[1],&values[2],&values[3],&values[4],&values[5],&values[6],&values[7],&values[8],&values[9],&values[10],&values[11],&extra);
            bool valid=n==12; SimInput input; input.boot=boot; input.sequence=++sequence; input.issued_ms=now;
            for(int i=0;valid&&i<4;++i) {valid=values[3*i]<=63&&values[3*i+1]<=600&&values[3*i+2]<=2000;input.flags[i]=values[3*i];input.temperature[i]=values[3*i+1];input.lux[i]=values[3*i+2];}
            if(!valid) fail("Invalid simulation inputs",400); else if(!fresh) fail("Controller unavailable or stale");
            else { house.input(input,boot,now); std::cout<<"{\"sequence\":"<<sequence<<",\"queued\":true}"; }
        } else if(verb=="speechfixture") {
            uint32_t value; if(!parse_number(body.c_str(),value)||value>16) fail("Invalid fixture ID",400);
            else {speech_id=(uint8_t)value;++job;proposal.consume();std::cout<<"{\"job\":"<<job<<",\"command_id\":"<<value<<",\"fixture\":true}";}
        } else if(verb=="speech") {
            std::cout<<"{\"supported\":false,\"fixture\":true,\"engine\":\"HOST FIXTURE ONLY - no recognition\",\"phase\":4,\"job\":"<<job<<",\"command_id\":"<<(unsigned)speech_id<<",\"confidence\":0,\"inference_us\":null,\"authorized\":false}";
        } else if(verb=="text") {
            SerialTextCommand parsed;
            if(!parse_serial_text(body.c_str(),parsed)) {proposal.consume();fail("Invalid target or proposal",400);}
            else {
                auto result=proposal.prepare(parsed.target,boot,fresh,now,false,0);
                const CommandTarget &t=parsed.target;
                if(result!=ProposalResult::Ok) fail(proposal_error(result));
                else std::cout<<"{\"proposal_id\":"<<proposal.id<<",\"target\":\""<<target_mode(t.mode)<<"\",\"room\":\""<<target_room(t.room)<<"\",\"device\":\""<<target_device(t.device)<<"\",\"action\":\""<<target_action(t.action)<<"\",\"requested_room\":\""<<(parsed.requested_hall?"hall":"bedroom")<<"\",\"bench_alias\":"<<(parsed.bench_alias?"true":"false")<<",\"acoustic_fallback\":"<<(parsed.acoustic_fallback?"true":"false")<<",\"expires_in_ms\":15000,\"authorized\":false}";
            }
        } else if(verb=="proposal") {
            char source[24];CommandTarget target;uint32_t source_job=0;
            if(!proposal_fields(body.c_str(),source,target,source_job)||(strcmp(source,"manual")&&strcmp(source,"speech"))) {proposal.consume();fail("Invalid target or proposal",400);}
            else {
                bool speech=!strcmp(source,"speech");CommandTarget expected;
                if((speech&&(source_job!=job||!speech_target(speech_id,target.mode,expected)||!same_target(expected,target)))||(!speech&&source_job)) {proposal.consume();fail("Proposal or target mismatch");}
                else {
                    auto result=proposal.prepare(target,boot,fresh,now,speech,source_job);
                    if(result!=ProposalResult::Ok) fail(proposal_error(result));
                    else std::cout<<"{\"proposal_id\":"<<proposal.id<<",\"target\":\""<<target_mode(target.mode)<<"\",\"room\":\""<<target_room(target.room)<<"\",\"device\":\""<<target_device(target.device)<<"\",\"action\":\""<<target_action(target.action)<<"\",\"authorized\":false}";
                }
            }
        } else if(verb=="confirm" || verb=="control") {
            ControlCommand command;bool valid=false;ProposalResult validation=ProposalResult::Invalid;
            if(verb=="confirm") {
                char first[24];CommandTarget target;uint32_t requested;
                if(proposal_fields(body.c_str(),first,target,requested,true)) {validation=proposal.confirm(requested,target,boot,fresh,now,command);valid=validation==ProposalResult::Ok;}
            } else {
                valid=fresh&&parse_control_text(body.c_str(),command);
                validation=fresh?ProposalResult::Invalid:ProposalResult::Unavailable;
            }
            if(!valid) fail(proposal_error(validation),validation==ProposalResult::Invalid?400:409);
            else {
                command.id=++id;command.boot=boot;command.issued_ms=now;
                phase=scenario=="timeout"?3:2;
                if(phase==2) status=scenario=="reject"?ControlStatus::Denied:command.op==ControlOp::Simulation?house.manual(command,now):automation.execute(command,now);
                proposal.consume();std::cout<<"{\"http_status\":202,\"queued\":true,\"id\":"<<id<<"}";
            }
        } else if(verb=="transaction") {
            std::cout<<"{\"id\":"<<id<<",\"phase\":"<<phase<<",\"status\":"<<(unsigned)status<<",\"light\":"<<physical(0)<<",\"fan\":"<<physical(1)<<",\"attempts\":1,\"rtt_us\":null,\"processing_us\":null,\"outcome_known\":"<<(phase==2?"true":"false")<<"}";
        } else fail("Unknown host command",404);
        std::cout<<std::endl;
    }
}
