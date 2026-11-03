/**
 * @file clawgrid_config.h
 * @brief Hardware Pin Maps, Network Configuration, and Node IDs
 */

#ifndef CLAWGRID_CONFIG_H
#define CLAWGRID_CONFIG_H

/* Node Identifiers */
#ifndef CLAWGRID_ROOM_NODE_ID
#define CLAWGRID_ROOM_NODE_ID 1
#endif
#ifndef CLAWGRID_ROOM_ID
#define CLAWGRID_ROOM_ID 1
#endif
#if defined(CLAWGRID_NODE_C) && (CLAWGRID_ROOM_NODE_ID != 3 || CLAWGRID_ROOM_ID != 2)
#error Node C must use unique node3/room2 identity; never copy the A identity
#endif
#ifndef CLAWGRID_LIGHT_INPUT
#define CLAWGRID_LIGHT_INPUT 0
#endif
#define CLAWGRID_LIGHT_INPUT_PIN 6
#define NODE_ID_VOICE_FASTPATH 0x0001
#define NODE_ID_INTELLIGENCE_STATE 0x0002
#define NODE_ID_ROOM_C 0x0003
#define NODE_ID_BROADCAST 0xFFFF
#define CLAWGRID_WIFI_CHANNEL 1
#define NODE_A_PIR_PIN 8 /* Never acquired in current no-PIR builds. */
#if defined(CLAWGRID_NODE_C)
#define CLAWGRID_ROOM_LABEL "C"
#define NODE_A_DHT_PIN 21
#define NODE_A_RELAY_LIGHT_PIN 18
#define NODE_A_RELAY_FAN_PIN 19
#else
#define CLAWGRID_ROOM_LABEL "A"
#define NODE_A_DHT_PIN 7
#define NODE_A_RELAY_LIGHT_PIN 4
#define NODE_A_RELAY_FAN_PIN 5
#endif
/* Provisional GPIO-to-interface polarity, not verified module compatibility.
 * An intervening transistor may invert it. Check electrically before bench use. */
#ifndef NODE_A_RELAY_ACTIVE_LOW
#define NODE_A_RELAY_ACTIVE_LOW    true
#endif
#define NODE_A_PIR_WARMUP_MS       60000u /* Then require 200 ms LOW before arming. */
#ifndef CLAWGRID_PIR_AVAILABLE
#define CLAWGRID_PIR_AVAILABLE     0
#endif
#if CLAWGRID_PIR_AVAILABLE
#define CLAWGRID_INPUT_PROFILE "ClawGrid:pir=available"
#else
#define CLAWGRID_INPUT_PROFILE "ClawGrid:pir=unavailable"
#endif
#ifndef CLAWGRID_PHYSICAL_OUTPUTS
#define CLAWGRID_PHYSICAL_OUTPUTS  0 /* Simulation builds never drive relay pins. */
#endif
#ifndef CLAWGRID_DIAGNOSTIC_FAULTS
#define CLAWGRID_DIAGNOSTIC_FAULTS 0 /* Explicit USB diagnostic builds only. */
#endif
#ifndef CLAWGRID_LED_ONLY_BENCH
#define CLAWGRID_LED_ONLY_BENCH 0
#endif
#ifndef CLAWGRID_LED_ACTIVE_HIGH
#define CLAWGRID_LED_ACTIVE_HIGH 0
#endif
#if CLAWGRID_LED_ACTIVE_HIGH && !CLAWGRID_LED_ONLY_BENCH
#error Active-HIGH indicator selection requires explicit LED-only profile
#endif
#if CLAWGRID_LED_ONLY_BENCH && (!CLAWGRID_PHYSICAL_OUTPUTS || CLAWGRID_DIAGNOSTIC_FAULTS)
#error LED indicator profile requires explicit enabled/default-faults bench mode
#endif
#if CLAWGRID_LED_ONLY_BENCH
#ifdef __cplusplus
static_assert(NODE_A_RELAY_ACTIVE_LOW == !CLAWGRID_LED_ACTIVE_HIGH,"LED profile polarity/configuration mismatch");
#endif
#if CLAWGRID_LED_ACTIVE_HIGH
#define CLAWGRID_LOAD_PROFILE "ClawGrid:load=led_indicators_10k_active_high"
#else
#define CLAWGRID_LOAD_PROFILE "ClawGrid:load=led_indicators_10k_active_low"
#endif
#else
#define CLAWGRID_LOAD_PROFILE "ClawGrid:load=generic"
#endif
#if CLAWGRID_DIAGNOSTIC_FAULTS && CLAWGRID_PHYSICAL_OUTPUTS
#error Diagnostic fault injection requires physical outputs inhibited
#endif
#if CLAWGRID_PHYSICAL_OUTPUTS
#define CLAWGRID_OUTPUT_MODE "enabled"
#else
#define CLAWGRID_OUTPUT_MODE "inhibited"
#endif
#if CLAWGRID_DIAGNOSTIC_FAULTS
#define CLAWGRID_FAULT_MODE "diagnostic"
#else
#define CLAWGRID_FAULT_MODE "default"
#endif
#define CLAWGRID_BUILD_MODE "ClawGrid:outputs=" CLAWGRID_OUTPUT_MODE ";faults=" CLAWGRID_FAULT_MODE
/* Original DevKitC/most clones: GPIO48; official v1.1: GPIO38. */
#ifndef CLAWGRID_STATUS_LED_PIN
#define CLAWGRID_STATUS_LED_PIN    48
#endif
#define NODE_A_STATUS_LED_PIN      CLAWGRID_STATUS_LED_PIN

/* I2S Microphone Pin Mapping (INMP441 / PDM) */
#define I2S_MIC_SCK_PIN            12      /* Serial Clock (BCLK) */
#define I2S_MIC_WS_PIN             11      /* Word Select (LRCLK) */
#define I2S_MIC_SD_PIN             10      /* Serial Data (DOUT) */

/* I2S Speaker / DAC Pin Mapping (MAX98357A / Tone) */
#define I2S_SPK_BCLK_PIN           16
#define I2S_SPK_LRC_PIN            17
#define I2S_SPK_DIN_PIN            18

#if NODE_A_PIR_PIN == NODE_A_DHT_PIN || NODE_A_PIR_PIN == NODE_A_RELAY_LIGHT_PIN || NODE_A_PIR_PIN == NODE_A_RELAY_FAN_PIN || NODE_A_PIR_PIN == CLAWGRID_STATUS_LED_PIN || NODE_A_PIR_PIN == I2S_MIC_SCK_PIN || NODE_A_PIR_PIN == I2S_MIC_WS_PIN || NODE_A_PIR_PIN == I2S_MIC_SD_PIN || NODE_A_PIR_PIN == I2S_SPK_BCLK_PIN || NODE_A_PIR_PIN == I2S_SPK_LRC_PIN || NODE_A_PIR_PIN == I2S_SPK_DIN_PIN
#error PIR conflicts with another assigned hardware pin
#endif

/* Safety Timing Parameters */
#define CLAWGRID_MIN_DWELL_MS      3000    /* 3s anti-cycle dwell time */
#define CLAWGRID_VACANCY_GRACE_MS  120000  /* 2 minutes grace period */
#define CLAWGRID_HEARTBEAT_MS      1000    /* 1 Hz heartbeat */

#endif /* CLAWGRID_CONFIG_H */
