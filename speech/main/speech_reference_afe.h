#pragma once
#include "esp_afe_sr_models.h"

// Bounded fingerprint-verified diagnostic preprocessing. Ring capacity exceeds
// the entire clip plus padding, so feed-before-fetch needs no extra application
// thread. Normal phone jobs never enter here. The pinned fetch uses a bounded
// sr_rb_read; there is only one caller of this AFE instance.
static bool prepare_reference_afe(const uint8_t *raw,size_t bytes,int16_t *scratch,
                                  uint8_t *&output,size_t &output_bytes,SpeechSnapshot &result) {
    output=nullptr; output_bytes=0;
    if(bytes!=speech_reference_bytes(result.input)) return false;
    afe_config_t config={};
    config.aec_init=false; config.se_init=true; config.vad_init=true;
    config.wakenet_init=false; config.voice_communication_init=false;
    config.vad_mode=VAD_MODE_3; config.wakenet_mode=DET_MODE_90;
    config.afe_mode=SR_MODE_LOW_COST; config.afe_perferred_core=0;
    config.afe_perferred_priority=4; config.afe_ringbuf_size=600;
    config.memory_alloc_mode=AFE_MEMORY_ALLOC_MORE_PSRAM;
    config.afe_linear_gain=1.0f; config.agc_mode=AFE_MN_PEAK_AGC_MODE_2;
    config.pcm_config={1,1,0,16000}; config.afe_ns_mode=NS_MODE_SSP;
    const esp_afe_sr_iface_t *afe=&ESP_AFE_SR_HANDLE;
    Serial.printf("{\"kind\":\"afe_reference_init\",\"stage\":\"create\",\"internal_free\":%u,\"psram_free\":%u}\n",heap_caps_get_free_size(MALLOC_CAP_INTERNAL|MALLOC_CAP_8BIT),heap_caps_get_free_size(MALLOC_CAP_SPIRAM|MALLOC_CAP_8BIT));
    esp_afe_sr_data_t *data=afe->create_from_config(&config);
    if(!data) { Serial.printf("{\"kind\":\"afe_reference_failure\",\"stage\":\"create\"}\n"); return false; }
    Serial.printf("{\"kind\":\"afe_reference_format\",\"rate\":%d,\"channels\":%d,\"feed_chunk\":%d,\"fetch_chunk\":%d}\n",afe->get_samp_rate(data),afe->get_total_channel_num(data),afe->get_feed_chunksize(data),afe->get_fetch_chunksize(data));
    const int feed_chunk=afe->get_feed_chunksize(data);
    const size_t feeds=speech_afe_feed_frames(bytes/2,feed_chunk);
    const size_t fetches=feeds?feeds*feed_chunk/512-8:0;
    bool ok=afe->get_samp_rate(data)==16000 && afe->get_total_channel_num(data)==1 &&
        feed_chunk>0 && feed_chunk<=1024 && feeds<600 && fetches>0 && afe->get_fetch_chunksize(data)==512;
    if(ok) output=(uint8_t*)heap_caps_aligned_alloc(16,fetches*1024,MALLOC_CAP_SPIRAM|MALLOC_CAP_8BIT);
    if(!ok || !output) Serial.printf("{\"kind\":\"afe_reference_failure\",\"stage\":\"%s\"}\n",ok?"output_allocation":"format");
    ok=ok && output;
    uint32_t started=millis();
    for(size_t i=0;ok && i<feeds;++i) {
        pcm_frame_delayed(raw,bytes,i*feed_chunk,4096,scratch,feed_chunk);
        if(afe->feed(data,scratch)<0) ok=false;
        else ++result.afe_feed_frames;
        if((uint32_t)(millis()-started)>8000) ok=false;
        vTaskDelay(1);
    }
    for(size_t i=0;ok && i<fetches;++i) {
        afe_fetch_result_t *frame=afe->fetch(data);
        if(!frame || frame->ret_value==ESP_FAIL || !frame->data || frame->data_size!=1024) ok=false;
        else { memcpy(output+i*1024,frame->data,1024); ++result.afe_fetch_frames; }
        if((uint32_t)(millis()-started)>8000) ok=false;
        vTaskDelay(1);
    }
    afe->destroy(data);
    if(!ok) { if(output) heap_caps_free(output); output=nullptr; return false; }
    output_bytes=fetches*1024;
    result.afe_samples=output_bytes/2; result.afe_crc32=pcm_crc32(output,output_bytes);
    PcmLevels levels=pcm_levels(output,output_bytes);
    result.afe_peak=levels.peak; result.afe_rms=(uint32_t)sqrt((double)levels.sum_squares/result.afe_samples);
    Serial.printf("{\"kind\":\"speech_reference_afe\",\"feed_frames\":%u,\"fetch_frames\":%u,\"samples\":%u,\"crc32\":%u,\"peak\":%u,\"rms\":%u,\"threshold\":0.7}\n",result.afe_feed_frames,result.afe_fetch_frames,result.afe_samples,result.afe_crc32,result.afe_peak,result.afe_rms);
    return true;
}
