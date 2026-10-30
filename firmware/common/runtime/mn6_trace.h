#pragma once
#include <stdint.h>
#include <stddef.h>
#include <math.h>
#include <string.h>

// Read-only layout for the SHA-guarded MN6 archive. Never authorizes a command.
struct Mn6PathTrace {
    int32_t command=0,length=0;
    float total_log=0,mean_log=0,probability=0;
    uint32_t score_bits=0;
    bool score_available=false;
    uint32_t token_count=0,tokens[64]={};
};
struct Mn6Trace {
    uint32_t frame=0,state=0;
    uint32_t failure=0;
    int32_t count[2]={0,0};
    uint32_t recorded[2]={0,0};
    float threshold=0;
    Mn6PathTrace paths[2][2];
};
template<class Read>
inline bool mn6_read_trace(uint32_t model,Read read,Mn6Trace &out) {
    if(!model || !read(model+224,&out.threshold,4)) { out.failure=1; return false; }
    if(!isfinite(out.threshold) || fabsf(out.threshold-logf(0.7f))>0.00001f) { out.failure=2; return false; }
    for(uint32_t group=0;group<2;++group) {
        uint32_t beams=0,array=0;
        if(!read(model+92+group*4,&beams,4) || !beams ||
           !read(beams,&array,4) || !read(beams+4,&out.count[group],4) ||
           out.count[group]<0 || out.count[group]>64) { out.failure=3+group; return false; }
        for(int i=0;i<out.count[group] && i<2;++i) {
            uint32_t path=0,tokens=0;
            Mn6PathTrace &p=out.paths[group][i];
            if(!array || !read(array+4*i,&path,4) || !path ||
               !read(path,&tokens,4) || !read(path+8,&p.command,4) ||
               !read(path+12,&p.total_log,4) || !read(path+16,&p.length,4) ||
               p.length<0 || p.length>128) { out.failure=5+group*4; return false; }
            memcpy(&p.score_bits,&p.total_log,4);
            p.score_available=isfinite(p.total_log);
            p.token_count=(uint32_t)p.length<64?(uint32_t)p.length:64;
            if(p.token_count && (!tokens || !read(tokens,p.tokens,p.token_count*4))) { out.failure=7+group*4; return false; }
            if(p.score_available) {
                p.mean_log=p.total_log/(p.length+2);
                p.probability=expf(p.mean_log);
                if(!isfinite(p.probability)) p.score_available=false;
            }
            ++out.recorded[group];
        }
    }
    return true;
}
