// Separate MN7 evaluation runtime: no MN6 header, model offsets or ABI patch.
#include "speech_runtime.h"
#include "native_hal.h"
#include "esp_heap_caps.h"
#include "esp_mn_models.h"
#include "esp_mn_speech_commands.h"
#include "esp_afe_sr_models.h"
#include "model_path.h"
#include "pcm16.h"
#include <algorithm>
#include <math.h>
#include <string.h>

static portMUX_TYPE guard=portMUX_INITIALIZER_UNLOCKED;
static SpeechSnapshot state;
static TaskHandle_t worker=nullptr;static StaticTask_t tcb;static StackType_t stack[12288];
static uint8_t *clip=nullptr;static size_t clip_bytes=0;
static const esp_mn_iface_t *engine=nullptr;static model_iface_data_t *model=nullptr;
static srmodel_list_t *models=nullptr;
static constexpr float probability_gate=.7f;
static const char marker[]="ClawGrid:voice=mn7-eval-v1";

static void resources(SpeechSnapshot &s) {
    s.internal_free=heap_caps_get_free_size(MALLOC_CAP_INTERNAL|MALLOC_CAP_8BIT);
    s.internal_min=heap_caps_get_minimum_free_size(MALLOC_CAP_INTERNAL|MALLOC_CAP_8BIT);
    s.internal_largest=heap_caps_get_largest_free_block(MALLOC_CAP_INTERNAL|MALLOC_CAP_8BIT);
    s.psram_free=heap_caps_get_free_size(MALLOC_CAP_SPIRAM|MALLOC_CAP_8BIT);
}
static bool commands(bool reference=false) {
    if(esp_mn_commands_clear()!=ESP_OK)return false;
    char enable[]="light enable",disable[]="light disable",joke[]="tell me a joke";
    if(reference) { if(esp_mn_commands_add(17,joke)!=ESP_OK)return false; }
    else if(esp_mn_commands_add(1,enable)!=ESP_OK || esp_mn_commands_add(2,disable)!=ESP_OK)return false;
    for(int i=0;i<(reference?1:2);++i) {
        auto *p=esp_mn_commands_get_from_index(i);
        if(!p || p->command_id!=(reference?17:i+1) || !p->phonemes || !p->phonemes[0])return false;
        Serial.printf("{\"kind\":\"mn7_registration\",\"id\":%d,\"graphemes\":\"%s\",\"phonemes\":\"%s\",\"method\":\"pinned_internal_FLITE_G2P\"}\n",p->command_id,p->string,p->phonemes);
    }
    return esp_mn_commands_get_from_index(reference?1:2)==nullptr && esp_mn_commands_update()==nullptr;
}
static bool initialize() {
    Serial.printf("%s\n",marker);heap_caps_malloc_extmem_enable(1024);
    clip=(uint8_t*)heap_caps_aligned_alloc(16,kPcmMaxBytes,MALLOC_CAP_SPIRAM|MALLOC_CAP_8BIT);
    models=esp_srmodel_init("model");if(!clip || !models)return false;
    char *name=esp_srmodel_filter(models,"mn7_en",nullptr);
    if(!name || strcmp(name,"mn7_en"))return false;
    engine=esp_mn_handle_from_name(name);if(!engine)return false;
    model=engine->create(name,6000);
    if(!model || engine->get_samp_rate(model)!=16000 || engine->get_samp_chunksize(model)!=512 ||
       strcmp(engine->get_language(model),ESP_MN_ENGLISH))return false;
    // Equivalent ln(0.7) is supplied through the constructor's native JSON
    // det_threshold config. Broken public setter is never called. The artifact
    // and full deployed model partition are hash-bound by mn7_migration.py.
    return commands();
}
static bool frontend(SpeechSnapshot &s,uint8_t *&output,size_t &bytes) {
    const esp_afe_sr_iface_t *afe=&ESP_AFE_SR_HANDLE;
    afe_config_t c={};c.se_init=true;c.vad_init=true;c.vad_mode=VAD_MODE_3;
    c.afe_mode=SR_MODE_LOW_COST;c.afe_perferred_core=1;c.afe_perferred_priority=2;
    c.afe_ringbuf_size=600;c.memory_alloc_mode=AFE_MEMORY_ALLOC_MORE_PSRAM;
    c.afe_linear_gain=1;c.agc_mode=AFE_MN_PEAK_AGC_MODE_2;c.pcm_config={1,1,0,16000};c.afe_ns_mode=NS_MODE_SSP;
    auto *data=afe->create_from_config(&c);if(!data)return false;
    int feed=afe->get_feed_chunksize(data),fetch=afe->get_fetch_chunksize(data);
    bool ok=afe->get_samp_rate(data)==16000 && afe->get_total_channel_num(data)==1 && feed>0 && feed<=1024 && fetch==512;
    size_t feeds=ok?(4096+clip_bytes/2+32768+feed-1)/feed:0;
    size_t fetches=feeds?feeds*feed/512-8:0;
    if(feeds>=600 || !fetches)ok=false;
    if(ok)output=(uint8_t*)heap_caps_aligned_alloc(16,fetches*1024,MALLOC_CAP_SPIRAM|MALLOC_CAP_8BIT);
    ok=ok && output;alignas(16) int16_t input[1024];uint32_t started=millis();
    for(size_t i=0;ok && i<feeds;++i) {
        pcm_frame_delayed(clip,clip_bytes,i*feed,4096,input,feed);
        ok=afe->feed(data,input)>=0 && (uint32_t)(millis()-started)<8000;
        if(ok)++s.afe_feed_frames;
        vTaskDelay(1);
    }
    for(size_t i=0;ok && i<fetches;++i) {
        auto *f=afe->fetch(data);
        ok=f && f->ret_value!=ESP_FAIL && f->data && f->data_size==1024 && (uint32_t)(millis()-started)<8000;
        if(ok) {memcpy(output+i*1024,f->data,1024);++s.afe_fetch_frames;}vTaskDelay(1);
    }
    resources(s);afe->destroy(data);
    if(!ok) {if(output)heap_caps_free(output);output=nullptr;return false;}
    bytes=fetches*1024;s.afe_samples=bytes/2;s.afe_crc32=pcm_crc32(output,bytes);
    auto levels=pcm_levels(output,bytes);s.afe_peak=levels.peak;s.afe_rms=(uint32_t)sqrt((double)levels.sum_squares/s.afe_samples);
    return true;
}
static void run(void*) {
    bool ready=initialize();
    SpeechSnapshot initial;initial.phase=ready?1:5;initial.error=ready?0:30;initial.command_count=2;
    snprintf(initial.model_name,sizeof(initial.model_name),"mn7_en");resources(initial);
    portENTER_CRITICAL(&guard);state=initial;portEXIT_CRITICAL(&guard);
    if(!ready) {Serial.printf("{\"kind\":\"mn7_init_failure\",\"error\":30}\n");vTaskDelete(nullptr);return;}
    Serial.printf("{\"kind\":\"mn7_init\",\"model\":\"mn7_en\",\"commands\":2,\"probability_gate\":0.7,\"threshold_source\":\"constructor_JSON_log_probability\",\"private_abi_patch\":false,\"afe\":true}\n");
    for(;;) {
        ulTaskNotifyTake(pdTRUE,portMAX_DELAY);SpeechSnapshot s;
        portENTER_CRITICAL(&guard);s=state;portEXIT_CRITICAL(&guard);
        bool reference=s.input==SpeechInput::Mn7Official;bool diagnostic=speech_is_reference(s.input);
        s.error=0;s.engine_errors=0;s.command_count=reference?1:2;uint8_t *processed=nullptr;size_t bytes=0;
        uint32_t started=micros();
        if(reference && !commands(true))s.error=31;
        engine->clean(model);
        auto levels=pcm_levels(clip,clip_bytes);s.samples=clip_bytes/2;s.peak=levels.peak;s.clipped=levels.clipped;s.nonzero=levels.nonzero;
        s.rms=(uint32_t)sqrt((double)levels.sum_squares/s.samples);s.dc_mean=(int32_t)(levels.sum/(int64_t)s.samples);
        if(!s.error && !frontend(s,processed,bytes))s.error=32;
        s.chunk_samples=512;s.planned_frames=601;alignas(16) int16_t frame[512];
        for(size_t i=0;!s.error && i<601;++i) {
            size_t copied=pcm_frame(processed,bytes,i*512,frame,512);s.padded_samples+=512-copied;
            uint32_t at=micros();auto detected=engine->detect(model,frame);s.max_frame_us=std::max(s.max_frame_us,micros()-at);
            ++s.frames;s.last_state=(uint8_t)detected;
            if(detected==ESP_MN_STATE_DETECTING)++s.detecting_frames;
            else if(detected==ESP_MN_STATE_TIMEOUT)break;
            else if(detected==ESP_MN_STATE_DETECTED) {
                auto *f=engine->get_results(model);
                if(!f || f->num<1 || f->num>ESP_MN_RESULT_MAX_NUM || !isfinite(f->prob[0]) || f->prob[0]<0 || f->prob[0]>1) {s.error=33;break;}
                s.candidate_id=f->command_id[0];s.candidate_score=f->prob[0];
                if((reference?s.candidate_id!=17:(s.candidate_id<1 || s.candidate_id>2))) {s.error=34;break;}
                if(f->prob[0]>=probability_gate) {
                    if(diagnostic)s.reference_detected=true;else {s.command_id=s.candidate_id;s.confidence=s.candidate_score;}break;
                }
            } else {s.error=35;break;}
            if((uint32_t)(micros()-started)>15000000) {s.error=36;break;}vTaskDelay(1);
        }
        auto *f=engine->get_results(model);
        if(f) {s.raw_result_count=f->num;s.raw_result_state=(uint8_t)f->state;
            for(size_t i=0;i+1<sizeof(s.diagnostic_text) && f->string[i];++i) {char c=f->string[i];s.diagnostic_text[i]=((c>='A'&&c<='Z')||(c>='a'&&c<='z')||c==' ')?c:'_';}}
        s.inference_us=micros()-started;s.engine_errors=s.error?1:0;s.phase=s.error?5:4;
        if(processed) {memset(processed,0,bytes);heap_caps_free(processed);}
        if(reference && !commands()) {s.error=37;++s.engine_errors;s.phase=5;}
        engine->clean(model);resources(s);s.stack_free=uxTaskGetStackHighWaterMark(nullptr);
        memset(clip,0,clip_bytes);Serial.printf("{\"kind\":\"mn7_job\",\"job\":%u,\"samples\":%u,\"frames\":%u,\"candidate\":%u,\"score\":%.7f,\"error\":%u,\"authorized\":false}\n",s.job,s.samples,s.frames,s.candidate_id,s.candidate_score,s.error);
        portENTER_CRITICAL(&guard);state=s;portEXIT_CRITICAL(&guard);
    }
}
bool speech_begin() {worker=xTaskCreateStaticPinnedToCore(run,"SpeechTask",sizeof(stack),nullptr,2,stack,&tcb,1);return worker;}
uint8_t *speech_reserve_clip(size_t bytes,uint32_t &job,SpeechInput input,SpeechBinding binding) {
    if(!pcm_valid_size(bytes) || (input!=SpeechInput::Application && input!=SpeechInput::ShortPreflight && input!=SpeechInput::Mn7Official))return nullptr;
    bool accepted=false;portENTER_CRITICAL(&guard);
    if((state.phase==1 || state.phase==4) && state.job!=UINT32_MAX && clip) {
        uint32_t next=state.job+1;state={};state.job=next;state.phase=2;state.input=input;state.binding=binding;
        snprintf(state.model_name,sizeof(state.model_name),"mn7_en");clip_bytes=bytes;job=next;accepted=true;
    }
    portEXIT_CRITICAL(&guard);return accepted?clip:nullptr;
}
bool speech_finish_clip(bool complete) {
    uint32_t crc=complete?pcm_crc32(clip,clip_bytes):0;
    portENTER_CRITICAL(&guard);state.pcm_crc32=crc;state.phase=complete?3:1;portEXIT_CRITICAL(&guard);
    if(complete)xTaskNotifyGive(worker);else if(clip)memset(clip,0,clip_bytes);return complete;
}
SpeechSnapshot speech_snapshot() {portENTER_CRITICAL(&guard);auto s=state;portEXIT_CRITICAL(&guard);return s;}
