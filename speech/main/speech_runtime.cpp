#include "speech_runtime.h"
#include "native_hal.h"
#include "esp_heap_caps.h"
#include "esp_mn_models.h"
#include "esp_mn_speech_commands.h"
#include "model_path.h"
#include <algorithm>
#include <ctype.h>
#include <math.h>
#include "pcm16.h"
#include "mn6_threshold.h"
#include "mn6_trace.h"
#include "soc/soc_memory_types.h"
#include "speech_reference_afe.h"
#if defined(CLAWGRID_LIGHT_PREFLIGHT)
#include "light_preflight_contract.h"
#endif

static portMUX_TYPE lock=portMUX_INITIALIZER_UNLOCKED;
static SpeechSnapshot state;
static TaskHandle_t worker=nullptr; static StaticTask_t tcb; static StackType_t stack[12288];
static uint8_t *clip=nullptr; static size_t clip_bytes=0;
static const esp_mn_iface_t *engine=nullptr; static model_iface_data_t *model=nullptr;
static srmodel_list_t *models=nullptr;
static constexpr size_t kMaxClipBytes=kPcmMaxBytes;
static uint8_t init_error=0;
#if defined(CLAWGRID_SHORT_V1)
static const char experiment_marker[] __attribute__((used))="ClawGrid:voice=short-v1";
#elif defined(CLAWGRID_LIGHT_PREFLIGHT)
static const char experiment_marker[] __attribute__((used))="ClawGrid:voice=light-preflight-v1";
#endif
static bool init_failed(uint8_t code) { init_error=code; return false; }
static char loaded_model[24]={};
static bool trace_memory(uint32_t address,void *output,size_t bytes) {
    if(!address || !bytes || bytes>1024 || address+bytes<address) return false;
    const void *start=(const void*)(uintptr_t)address,*end=(const void*)(uintptr_t)(address+bytes-1);
    bool internal=esp_ptr_in_dram(start) && esp_ptr_in_dram(end);
    bool external=esp_ptr_external_ram(start) && esp_ptr_external_ram(end);
    if(!internal && !external) return false;
    memcpy(output,start,bytes); return true;
}
static void print_decoder_trace(const Mn6Trace &t,uint32_t job) {
    if(t.failure) Serial.printf("{\"kind\":\"mn6_trace_read_failure\",\"job\":%u,\"frame\":%u,\"reason\":%u}\n",job,t.frame,t.failure);
    for(int group=0;group<2;++group) {
        Serial.printf("{\"kind\":\"mn6_beam_count\",\"job\":%u,\"frame\":%u,\"state\":%u,\"group\":%d,\"count\":%d,\"log_threshold\":%.7f}\n",job,t.frame,t.state,group,t.count[group],t.threshold);
        for(uint32_t i=0;i<t.recorded[group];++i) {
            const auto &p=t.paths[group][i];
            char total[32]="null",mean[32]="null",probability[32]="null";
            if(p.score_available) { snprintf(total,sizeof(total),"%.7f",p.total_log); snprintf(mean,sizeof(mean),"%.7f",p.mean_log); snprintf(probability,sizeof(probability),"%.7f",p.probability); }
            char line[1400]; size_t used=snprintf(line,sizeof(line),"{\"kind\":\"mn6_path\",\"job\":%u,\"frame\":%u,\"group\":%d,\"rank\":%u,\"command\":%d,\"length\":%d,\"log_total\":%s,\"log_mean\":%s,\"probability\":%s,\"score_bits\":%u,\"score_available\":%s,\"score_gate_pass\":%s,\"tokens\":[",job,t.frame,group,i,p.command,p.length,total,mean,probability,p.score_bits,p.score_available?"true":"false",p.score_available && p.mean_log>t.threshold?"true":"false");
            for(uint32_t j=0;j<p.token_count && used<sizeof(line);++j) used+=snprintf(line+used,sizeof(line)-used,"%s%u",j?",":"",p.tokens[j]);
            if(used<sizeof(line)) { snprintf(line+used,sizeof(line)-used,"],\"authorized\":false}\n"); Serial.printf("%s",line); }
        }
    }
}
static bool apply_confidence_threshold() {
    return mn6_probability_threshold(model,kSpeechMinimumConfidence,
        [](float probability) { engine->set_det_threshold(model,probability); });
}

static bool register_commands(bool reference,bool bedroom_only=false,bool split_bedroom=false) {
    if(esp_mn_commands_clear()!=ESP_OK) return false;
    if(reference) {
        char phrase[]="TELL ME A JOKE";
        if(esp_mn_commands_add(kSpeechReferenceId,phrase)!=ESP_OK) return false;
    } else if(kShortVoice && !bedroom_only && !split_bedroom) {
#if defined(CLAWGRID_LIGHT_PREFLIGHT)
        char enable[]="LIGHT ON",disable[]="LIGHT OFF";
#else
        char enable[]="LIGHT ENABLE",disable[]="LIGHT DISABLE";
#endif
        if(esp_mn_commands_add(1,enable)!=ESP_OK || esp_mn_commands_add(2,disable)!=ESP_OK) return false;
    } else {
        const char *rooms[]={"bedroom","living room","kitchen","study"};
        char phrase[64];
        for(int device=0;device<(bedroom_only?1:8);++device) for(int action=0;action<2;++action) {
            snprintf(phrase,sizeof(phrase),"turn %s %s %s",action?"off":"on",split_bedroom && device/2==0?"bed room":rooms[device/2],device%2?"fan":"light");
            for(char *p=phrase;*p;++p) *p=(char)toupper((unsigned char)*p);
            if(esp_mn_commands_add(device*2+action+1,phrase)!=ESP_OK) return false;
        }
    }
    if(esp_mn_commands_update()!=nullptr) return false;
    int count=reference?1:((bedroom_only || kShortVoice)?2:16);
    for(int i=0;i<count;++i) {
        esp_mn_phrase_t *phrase=esp_mn_commands_get_from_index(i);
        if(!phrase || phrase->command_id!=(reference?kSpeechReferenceId:i+1)) return false;
    }
    return esp_mn_commands_get_from_index(count)==nullptr;
}
static bool recreate_model(size_t malloc_threshold=1024,bool inspect_pinned=false) {
    // Pinned MN6 create/destroy own the global command allocation. Do not free
    // or allocate it a second time around those calls.
    if(model) engine->destroy(model);
    heap_caps_malloc_extmem_enable(malloc_threshold);
    model=engine->create(loaded_model,kSpeechWindowMs);
    if(!model || engine->get_samp_rate(model)!=16000 || engine->get_samp_chunksize(model)!=512) return false;
    // Read-only diagnostic offsets from the hash-verified pinned MN6 archive:
    // model_set_det_threshold stores224; build_fsts stores108/112. Never used
    // for a microphone job or authorization, and never write private fields.
    float native_threshold=0;
    if(inspect_pinned) memcpy(&native_threshold,(const uint8_t*)model+224,4);
    if(!apply_confidence_threshold()) return false;
    if(inspect_pinned) {
        float effective=0; uint32_t forward=0,reverse=0;
        memcpy(&effective,(const uint8_t*)model+224,4);
        memcpy(&forward,(const uint8_t*)model+108,4); memcpy(&reverse,(const uint8_t*)model+112,4);
        Serial.printf("{\"kind\":\"mn6_pinned_config\",\"native_threshold\":%.7f,\"configured_threshold\":%.7f,\"forward_fst_present\":%s,\"reverse_fst_present\":%s,\"read_only\":true}\n",native_threshold,effective,forward?"true":"false",reverse?"true":"false");
    }
    return esp_mn_commands_get_from_index(0)!=nullptr;
}

static bool initialize() {
#if defined(CLAWGRID_SHORT_V1) || defined(CLAWGRID_LIGHT_PREFLIGHT)
    Serial.printf("%s\n",experiment_marker);
#endif
    // Keep medium-sized libc allocations in PSRAM. DMA and task stacks keep
    // their explicit internal-memory capabilities in ESP-IDF.
    heap_caps_malloc_extmem_enable(1024);
    if(heap_caps_get_free_size(MALLOC_CAP_INTERNAL|MALLOC_CAP_8BIT)<80000 ||
       heap_caps_get_free_size(MALLOC_CAP_SPIRAM|MALLOC_CAP_8BIT)<5*1024*1024) return init_failed(1);
    clip=(uint8_t*)heap_caps_aligned_alloc(16,kMaxClipBytes,MALLOC_CAP_SPIRAM|MALLOC_CAP_8BIT);
    if(!clip) return init_failed(2);
    models=esp_srmodel_init("model"); if(!models) return init_failed(3);
    char *name=esp_srmodel_filter(models,ESP_MN_PREFIX,ESP_MN_ENGLISH); if(!name) return init_failed(4);
    engine=esp_mn_handle_from_name(name); if(!engine) return init_failed(5);
    model=engine->create(name,kSpeechWindowMs);
    const char *language=model?engine->get_language(model):nullptr;
    if(!model || engine->get_samp_rate(model)!=16000 || !language || strcmp(language,ESP_MN_ENGLISH)!=0 || strcmp(name,"mn6_en")!=0) return init_failed(6);
    snprintf(loaded_model,sizeof(loaded_model),"%s",name);
    if(!esp_mn_commands_get_from_index(0)) return init_failed(7);
    if(!register_commands(false)) return init_failed(9);
    // Fixed 16-phrase public vocabulary, once at startup; never raw audio/tokens.
    esp_mn_active_commands_print();
    if(!apply_confidence_threshold()) return init_failed(8);
    int chunk=engine->get_samp_chunksize(model);
    if(chunk<=0 || chunk>1024) return init_failed(10);
    Serial.printf("{\"kind\":\"speech_init\",\"model\":\"%s\",\"commands\":%u,\"sample_rate\":16000,\"chunk_samples\":%d,\"threshold\":0.7,\"afe\":false}\n",name,kShortVoice?2u:16u,chunk);
    return true;
}
static void run(void*) {
    bool ready=initialize();
    portENTER_CRITICAL(&lock); state.phase=ready?1:5; state.error=init_error; state.command_count=kShortVoice?2:16;
    snprintf(state.model_name,sizeof(state.model_name),"%s",loaded_model); portEXIT_CRITICAL(&lock);
    if(!ready) {
        if(model) engine->destroy(model);
        else esp_mn_commands_free();
        if(models) esp_srmodel_deinit(models);
        if(clip) heap_caps_free(clip);
        clip=nullptr; vTaskDelete(nullptr); return;
    }
    alignas(16) int16_t frame[1024]; int chunk=engine->get_samp_chunksize(model);
    for(;;) {
        ulTaskNotifyTake(pdTRUE,portMAX_DELAY);
        SpeechSnapshot result; size_t bytes;
        portENTER_CRITICAL(&lock); result=state; bytes=clip_bytes; portEXIT_CRITICAL(&lock);
        bool reference=speech_is_reference(result.input);
        bool short_preflight=result.input==SpeechInput::ShortPreflight;
        bool phone_reference=speech_phone_reference(result.input);
        bool bedroom_reference=result.input==SpeechInput::ReferencePhoneBedroom;
        bool beam_trace=speech_decoder_trace(result.input) || result.input==SpeechInput::ReferenceTrace;
        Mn6Trace *beam_records=beam_trace?(Mn6Trace*)heap_caps_calloc(48,sizeof(Mn6Trace),MALLOC_CAP_SPIRAM|MALLOC_CAP_8BIT):nullptr;
        size_t beam_count=0; bool beam_failed=beam_trace && !beam_records;
        // Reference jobs use a separate diagnostic vocabulary. Never publish
        // their accepted ID as command_id, including factory-vocabulary ID1.
        // Restore the unchanged16-command vocabulary before completion.
        bool trace=speech_trace_reference(result.input);
        bool fresh=result.input==SpeechInput::ReferenceFreshWindow || result.input==SpeechInput::ReferenceOfficial || result.input==SpeechInput::ReferencePinned || trace;
        bool reset_first=result.input==SpeechInput::ReferenceResetFirst;
        bool commands_ok=!fresh || recreate_model(result.input==SpeechInput::ReferenceSdkHeap?16384:1024,trace);
        if(commands_ok && result.input==SpeechInput::ReferencePsram) {
            model_iface_data_t *loaded=engine->switch_loader_mode?engine->switch_loader_mode(model,ESP_MN_LOAD_FROM_PSRAM):nullptr;
            commands_ok=loaded!=nullptr;
            if(loaded) { model=loaded; commands_ok=apply_confidence_threshold(); }
            Serial.printf("{\"kind\":\"speech_reference_loader\",\"requested_mode\":\"all_psram\",\"success\":%s,\"psram_free\":%u}\n",commands_ok?"true":"false",heap_caps_get_free_size(MALLOC_CAP_SPIRAM|MALLOC_CAP_8BIT));
        }
        if(trace) Serial.printf("{\"kind\":\"speech_reference_heap\",\"malloc_threshold\":%u,\"internal_free\":%u,\"psram_free\":%u}\n",result.input==SpeechInput::ReferenceSdkHeap?16384:1024,heap_caps_get_free_size(MALLOC_CAP_INTERNAL|MALLOC_CAP_8BIT),heap_caps_get_free_size(MALLOC_CAP_SPIRAM|MALLOC_CAP_8BIT));
        if(commands_ok && reset_first) engine->clean(model);
        bool official=result.input==SpeechInput::ReferenceOfficial || result.input==SpeechInput::ReferencePinned || trace;
        if(commands_ok && reference && !official) commands_ok=register_commands(!phone_reference && !short_preflight,bedroom_reference,speech_split_reference(result.input));
        if(commands_ok && !fresh && !reset_first) engine->clean(model);
        if(commands_ok && reference) {
            esp_mn_active_commands_print();
            Serial.printf("{\"kind\":\"speech_reference_config\",\"mode\":%u,\"model\":\"%s\",\"chunk_api_count\":%d,\"threshold\":0.7,\"reference_id\":%u}\n",(unsigned)result.input,loaded_model,engine->get_samp_chunknum(model),phone_reference?2:(official?1:kSpeechReferenceId));
        }
        if(commands_ok && trace) {
            // MN6 in the pinned archive exposes a null optional open_log slot.
            // Our bounded per-frame result trace remains available without it.
            Serial.printf("{\"kind\":\"speech_decoder_capability\",\"open_log_supported\":%s}\n",engine->open_log?"true":"false");
            if(engine->open_log) engine->open_log(model);
        }
        result.frames=0; result.max_frame_us=0; result.command_id=0; result.confidence=0;
        result.error=commands_ok?0:15; result.candidate_id=0; result.candidate_score=0;
        result.command_count=(short_preflight || (!reference && kShortVoice) || bedroom_reference)?2:(reference && !phone_reference?1:16);
        if(official) {
            result.command_count=0;
            while(esp_mn_commands_get_from_index(result.command_count)) ++result.command_count;
        }
        result.detecting_frames=0; result.engine_errors=0; result.padded_samples=0; result.chunk_samples=chunk;
        PcmLevels levels=pcm_levels(clip,bytes);
        result.peak=levels.peak; result.clipped=levels.clipped; result.nonzero=levels.nonzero;
        result.rms=bytes?(uint32_t)sqrt((double)levels.sum_squares/(bytes/2)):0;
        result.dc_mean=bytes?(int32_t)(levels.sum/(int64_t)(bytes/2)):0;
        result.planned_frames=speech_feed_frames(bytes/2,chunk,result.input);
        uint8_t *processed=nullptr; size_t inference_bytes=bytes;
        const uint8_t *inference_clip=clip;
        if(commands_ok && (result.input==SpeechInput::ReferenceAfe || result.input==SpeechInput::ReferencePhoneAfe)) {
            commands_ok=prepare_reference_afe(clip,bytes,frame,processed,inference_bytes,result);
            if(commands_ok) inference_clip=processed;
            else { result.error=17; ++result.engine_errors; }
        }
        if(commands_ok && result.input==SpeechInput::ReferenceGain15) {
            processed=(uint8_t*)heap_caps_aligned_alloc(16,bytes,MALLOC_CAP_SPIRAM|MALLOC_CAP_8BIT);
            if(!processed) { commands_ok=false; result.error=19; ++result.engine_errors; }
            else {
                for(size_t i=0;i<bytes/2;++i) {
                    int32_t scaled=std::max(-32768,std::min(32767,(int32_t)pcm_sample(clip+2*i)*3/2));
                    processed[2*i]=(uint8_t)scaled; processed[2*i+1]=(uint8_t)((uint16_t)scaled>>8);
                }
                inference_clip=processed;
                PcmLevels measured=pcm_levels(processed,bytes);
                Serial.printf("{\"kind\":\"speech_reference_gain\",\"job\":%u,\"gain\":1.5,\"samples\":%u,\"pcm_crc32\":%u,\"peak\":%u,\"clipped\":%u}\n",result.job,(unsigned)(bytes/2),pcm_crc32(processed,bytes),measured.peak,measured.clipped);
            }
        }
        uint32_t start=micros();
        uint32_t feed_fingerprint=2166136261u; uint8_t previous_state=255;
        // Continue to the recognizer terminal state within the official601-call
        // bound. Even195 calls experimentally stopped before its198-call timeout.
        for(size_t offset=0;commands_ok && offset<result.planned_frames*(size_t)chunk;offset+=chunk) {
            size_t copied=pcm_frame(inference_clip,inference_bytes,offset,frame,chunk);
            // Literal byte-copy branch from pinned test_multinet.c. Paired
            // trace mode uses our portable frame decoder with identical tail.
            if(result.input==SpeechInput::ReferenceDirect) {
                if((offset+chunk)*sizeof(int16_t)<=bytes) memcpy(frame,clip+offset*sizeof(int16_t),chunk*sizeof(int16_t));
                else memset(frame,0,chunk*sizeof(int16_t));
            }
            if(speech_drop_partial_frame(result.input,offset,chunk,bytes/2)) {
                copied=0; memset(frame,0,chunk*sizeof(int16_t));
            }
            result.padded_samples+=chunk-copied;
            if(trace) feed_fingerprint=(feed_fingerprint^pcm_crc32((uint8_t*)frame,chunk*sizeof(int16_t)))*16777619u;
            uint32_t frame_start=micros(); esp_mn_state_t detected=engine->detect(model,frame);
            result.max_frame_us=std::max(result.max_frame_us,micros()-frame_start); ++result.frames;
            result.last_state=(uint8_t)detected;
            if(beam_records && (result.frames%8==0 || detected!=ESP_MN_STATE_DETECTING)) {
                size_t slot=beam_count<48?beam_count:47;
                Mn6Trace &snapshot=beam_records[slot]; snapshot={};
                snapshot.frame=result.frames; snapshot.state=(uint8_t)detected;
                if(!mn6_read_trace((uint32_t)(uintptr_t)model,trace_memory,snapshot)) beam_failed=true;
                if(beam_count<48) ++beam_count;
            }
            if(trace && (result.frames%16==0 || previous_state!=(uint8_t)detected)) {
                esp_mn_results_t *diagnostic=engine->get_results(model);
                char text[96]={};
                if(diagnostic) for(size_t i=0;i+1<sizeof(text) && diagnostic->string[i];++i) {
                    char c=diagnostic->string[i]; text[i]=((c>='A'&&c<='Z')||(c>='a'&&c<='z')||c==' ')?c:'_';
                }
                Serial.printf("{\"kind\":\"speech_decoder_frame\",\"mode\":%u,\"frame\":%u,\"state\":%u,\"num\":%d,\"candidate\":%d,\"score\":%.7f,\"feed_fingerprint\":%u,\"text\":\"%s\"}\n",(unsigned)result.input,result.frames,(unsigned)detected,diagnostic?diagnostic->num:-1,diagnostic&&diagnostic->num>0?diagnostic->command_id[0]:0,diagnostic&&diagnostic->num>0?diagnostic->prob[0]:0,feed_fingerprint,text);
            }
            previous_state=(uint8_t)detected;
            if(detected==ESP_MN_STATE_DETECTING) ++result.detecting_frames;
            if(detected==ESP_MN_STATE_DETECTED) {
                esp_mn_results_t *found=engine->get_results(model);
                bool valid=found && found->num>0 && found->num<=ESP_MN_RESULT_MAX_NUM &&
                    (reference && !phone_reference && !short_preflight?found->command_id[0]==(official?1:kSpeechReferenceId):(found->command_id[0]>=1 && found->command_id[0]<=(kShortVoice?2:16))) &&
                    isfinite(found->prob[0]) && found->prob[0]>=0 && found->prob[0]<=1;
                if(valid) {
                    result.candidate_id=found->command_id[0]; result.candidate_score=found->prob[0];
                }
                if(valid && found->prob[0]>=kSpeechMinimumConfidence) {
                    if(reference) result.reference_detected=true;
                    else { result.command_id=found->command_id[0]; result.confidence=found->prob[0]; }
                    break;
                }
                if(!valid) { result.error=11; ++result.engine_errors; break; }
            }
            if(detected!=ESP_MN_STATE_DETECTED && detected!=ESP_MN_STATE_DETECTING && detected!=ESP_MN_STATE_TIMEOUT) { result.error=12; ++result.engine_errors; break; }
            if(detected==ESP_MN_STATE_TIMEOUT && speech_stop_on_timeout(result.input)) break;
            vTaskDelay(1); // Yield to LinkTask and HTTP; no GPIO ownership here.
        }
        result.inference_us=micros()-start; result.phase=4; result.samples=bytes/2;
        if(beam_records) {
            for(size_t i=0;i<beam_count;++i) { print_decoder_trace(beam_records[i],result.job); vTaskDelay(1); }
            heap_caps_free(beam_records);
        }
        if(beam_trace) Serial.printf("{\"kind\":\"mn6_trace_status\",\"job\":%u,\"records\":%u,\"layout_read_failed\":%s,\"authorized\":false}\n",result.job,(unsigned)beam_count,beam_failed?"true":"false");
        if(processed) { memset(processed,0,inference_bytes); heap_caps_free(processed); }
        result.window_exhausted=result.frames==result.planned_frames && result.last_state==ESP_MN_STATE_DETECTING;
        // Diagnostic only: bounded ASCII decoder text, never authorization.
        // Safe alphabet avoids injecting JSON delimiters into the API.
        esp_mn_results_t *final_result=commands_ok?engine->get_results(model):nullptr;
        if(final_result) {
            result.raw_result_count=final_result->num; result.raw_result_state=(uint8_t)final_result->state;
            if(final_result->num>0 && final_result->num<=ESP_MN_RESULT_MAX_NUM &&
               final_result->command_id[0]>0 && final_result->command_id[0]<=255 &&
               isfinite(final_result->prob[0]) && final_result->prob[0]>=0 && final_result->prob[0]<=1) {
                result.candidate_id=final_result->command_id[0]; result.candidate_score=final_result->prob[0];
            }
        }
        memset(result.diagnostic_text,0,sizeof(result.diagnostic_text));
        if(final_result) for(size_t i=0;i+1<sizeof(result.diagnostic_text) && final_result->string[i];++i) {
            char c=final_result->string[i];
            result.diagnostic_text[i]=((c>='A'&&c<='Z')||(c>='a'&&c<='z')||(c>='0'&&c<='9')||c==' ')?c:'_';
        }
        if(reference) {
            // create/destroy resets open_log; never leave reference verbosity
            // enabled for a later real microphone job.
            if(trace && !recreate_model()) { result.error=18; ++result.engine_errors; }
            bool restored=model && register_commands(false);
            if(!restored) { result.error=16; ++result.engine_errors; result.phase=5; }
            else engine->clean(model);
        }
        result.stack_free=uxTaskGetStackHighWaterMark(nullptr);
        Serial.printf("{\"kind\":\"speech_job\",\"job\":%u,\"samples\":%u,\"frames\":%u,\"peak\":%u,\"clipped\":%u,\"command_id\":%u,\"error\":%u}\n",result.job,result.samples,result.frames,result.peak,result.clipped,result.command_id,result.error);
        memset(clip,0,bytes); // Do not retain microphone content after processing.
        portENTER_CRITICAL(&lock); state=result; portEXIT_CRITICAL(&lock);
    }
}
bool speech_begin() {
    worker=xTaskCreateStaticPinnedToCore(run,"SpeechTask",sizeof(stack),nullptr,2,stack,&tcb,1);
    if(!worker) { portENTER_CRITICAL(&lock); state.phase=5; state.error=13; portEXIT_CRITICAL(&lock); }
    return worker!=nullptr;
}
uint8_t *speech_reserve_clip(size_t bytes,uint32_t &job,SpeechInput input,SpeechBinding binding) {
#if defined(CLAWGRID_LIGHT_PREFLIGHT)
    if(input!=SpeechInput::ShortPreflight || !binding.bedroom || !binding.screen || !binding.boot || !binding.capture) return nullptr;
    if(!light_preflight_size(speech_snapshot().job+1,bytes)) return nullptr;
#endif
    if(!pcm_valid_size(bytes) || (speech_is_reference(input) && input!=SpeechInput::ShortPreflight && bytes!=speech_reference_bytes(input))) return nullptr;
    bool accepted=false;
    portENTER_CRITICAL(&lock);
    if((state.phase==1 || state.phase==4) && clip) {
        uint32_t next=state.job+1;
        if(next) { state={}; state.phase=2; state.job=next; state.input=input; state.binding=binding;
            snprintf(state.model_name,sizeof(state.model_name),"%s",loaded_model);
            clip_bytes=bytes; job=next; accepted=true; }
    }
    portEXIT_CRITICAL(&lock); return accepted?clip:nullptr;
}
bool speech_finish_clip(bool complete) {
    uint32_t crc=complete?pcm_crc32(clip,clip_bytes):0;
    bool valid=state.input==SpeechInput::ShortPreflight || !speech_is_reference(state.input) || speech_reference_matches(clip_bytes,crc,state.input);
#if defined(CLAWGRID_LIGHT_PREFLIGHT)
    valid=light_preflight_matches(state.job,clip_bytes,crc);
#endif
    complete=complete && valid;
    portENTER_CRITICAL(&lock); state.pcm_crc32=crc; state.phase=complete?3:1;
    if(!valid) state.error=14; portEXIT_CRITICAL(&lock);
    if(complete) xTaskNotifyGive(worker);
    else if(clip) memset(clip,0,clip_bytes);
    return complete;
}
SpeechSnapshot speech_snapshot() {
    portENTER_CRITICAL(&lock); SpeechSnapshot copy=state; portEXIT_CRITICAL(&lock); return copy;
}
