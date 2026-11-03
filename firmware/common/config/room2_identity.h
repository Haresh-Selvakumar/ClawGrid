#pragma once
#include <stdint.h>
// No C peer is trusted until ROM identity has been established and a separate
// private provisioning file supplied. Never rotate the existing A/B material.
#if __has_include("private_room2_config.h")
#include "private_room2_config.h"
#else
static constexpr bool kRoom2Provisioned=false;
static const uint8_t kNodeC[6]={};
static const uint8_t kRoom2Lmk[16]={};
static const uint8_t kRoom2HmacKey[32]={};
#define CLAWGRID_C_ID "ClawGrid:node:C=unprovisioned;room=2"
#endif
