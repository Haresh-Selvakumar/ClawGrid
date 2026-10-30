#pragma once
#include <stddef.h>
#include <stdint.h>
struct LightPreflightFixture { size_t bytes; uint32_t crc; uint8_t expected; };
static constexpr LightPreflightFixture kLightPreflightFixtures[] = {
    {46560,1936402414u,1},
    {46080,2336777373u,2},
    {64000,2109903275u,0},
    {67520,3492560341u,0},
};
inline bool light_preflight_size(uint32_t job,size_t bytes) {
    return job>=1 && job<=4 && kLightPreflightFixtures[job-1].bytes==bytes;
}
inline bool light_preflight_matches(uint32_t job,size_t bytes,uint32_t crc) {
    return light_preflight_size(job,bytes) && kLightPreflightFixtures[job-1].crc==crc;
}
