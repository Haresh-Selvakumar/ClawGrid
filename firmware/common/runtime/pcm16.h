#pragma once
#include <stdint.h>
#include <stddef.h>
#include <string.h>

static constexpr size_t kPcmRate=16000,kPcmMaxSamples=64000,kPcmMaxBytes=kPcmMaxSamples*2,kPcmMinBytes=3200;
inline bool pcm_valid_size(size_t bytes) { return bytes>=kPcmMinBytes && bytes<=kPcmMaxBytes && !(bytes&1); }
inline int16_t pcm_sample(const uint8_t *p) { return (int16_t)((uint16_t)p[0]|((uint16_t)p[1]<<8)); }
// Exactly one model frame, with the final partial frame zero-padded.
inline size_t pcm_frame(const uint8_t *pcm,size_t bytes,size_t offset,int16_t *out,size_t chunk) {
    memset(out,0,chunk*sizeof(int16_t));
    size_t samples=bytes/2,n=offset<samples?(samples-offset<chunk?samples-offset:chunk):0;
    for(size_t i=0;i<n;++i) out[i]=pcm_sample(pcm+2*(offset+i));
    return n;
}
struct PcmLevels { uint32_t peak=0,clipped=0,nonzero=0; uint64_t sum_squares=0; };
inline PcmLevels pcm_levels(const uint8_t *pcm,size_t bytes) {
    PcmLevels v;
    for(size_t i=0;i<bytes/2;++i) {
        int32_t s=pcm_sample(pcm+2*i),a=s<0?-s:s;
        if((uint32_t)a>v.peak) v.peak=a;
        v.clipped+=(s==32767 || s==-32768); v.nonzero+=s!=0; v.sum_squares+=(int64_t)s*s;
    }
    return v;
}
