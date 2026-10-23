#pragma once
#include "traffic_indicator.h"

#if defined(CLAWGRID_NATIVE_IDF)
void traffic_led_begin();
void traffic_led_event(TrafficEvent event);
#else
// The active implementation is the native ESP-IDF build.
inline void traffic_led_begin() {}
inline void traffic_led_event(TrafficEvent) {}
#endif
