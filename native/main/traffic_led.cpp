#include "traffic_led.h"
#include "clawgrid_config.h"
#include "driver/rmt.h"
#include "esp_timer.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include <atomic>
#include <stdio.h>

// One WS2812-compatible onboard RGB LED. Never use relay, USB or memory pins.
static_assert(CLAWGRID_STATUS_LED_PIN == 48 || CLAWGRID_STATUS_LED_PIN == 38,
              "Select the verified onboard RGB LED pin: GPIO48 or GPIO38");
static constexpr rmt_channel_t channel = RMT_CHANNEL_0;
static std::atomic<uint32_t> pending{0};
static std::atomic<bool> ready{false};
static StaticTask_t task_control;
static StackType_t task_stack[2048];

void traffic_led_event(TrafficEvent event) {
    // Wi-Fi callbacks only record bits; no GPIO, logging, allocation or waits.
    if (ready.load(std::memory_order_relaxed))
        pending.fetch_or((uint32_t)event, std::memory_order_relaxed);
}

static bool write_rgb(uint32_t rgb) {
    const uint32_t grb = ((rgb & 0x00ff00) << 8) |
                         ((rgb & 0xff0000) >> 8) | (rgb & 0x0000ff);
    rmt_item32_t items[25] = {};
    // APB 80 MHz / 2 = 40 MHz, 25 ns per tick, 1.25 us per data bit.
    for (int i = 0; i < 24; ++i) {
        const bool one = (grb & (1u << (23 - i))) != 0;
        items[i].level0 = 1; items[i].duration0 = one ? 32 : 16;
        items[i].level1 = 0; items[i].duration1 = one ? 18 : 34;
    }
    items[24].level0 = items[24].level1 = 0;
    items[24].duration0 = items[24].duration1 = 1600; // 80 us latch LOW
    if (rmt_write_items(channel, items, 25, false) != ESP_OK) return false;
    if (rmt_wait_tx_done(channel, pdMS_TO_TICKS(20)) == ESP_OK) return true;
    rmt_tx_stop(channel);
    return false;
}

static void activity_task(void *) {
    TrafficIndicator indicator;
    uint32_t previous = UINT32_MAX;
    for (;;) {
        const uint32_t events = pending.exchange(0, std::memory_order_relaxed);
        const uint32_t color = indicator.tick(events, (uint32_t)(esp_timer_get_time() / 1000));
        if (color != previous) {
            if (!write_rgb(color)) {
                ready.store(false, std::memory_order_relaxed);
                printf("{\"kind\":\"traffic_led\",\"ready\":false,\"error\":\"RMT transmit failed\"}\n");
                // Diagnostic failure never disables the radio or SafetyKernel.
                vTaskDelete(nullptr);
                return;
            }
            previous = color;
        }
        vTaskDelay(pdMS_TO_TICKS(20));
    }
}

void traffic_led_begin() {
    rmt_config_t config = RMT_DEFAULT_CONFIG_TX((gpio_num_t)CLAWGRID_STATUS_LED_PIN, channel);
    config.clk_div = 2;
    bool ok = rmt_config(&config) == ESP_OK;
    if (ok) ok = rmt_driver_install(channel, 0, 0) == ESP_OK;
    if (ok) {
        ready.store(true, std::memory_order_relaxed);
        ok = xTaskCreateStaticPinnedToCore(activity_task, "TrafficLED", sizeof(task_stack),
                nullptr, 1, task_stack, &task_control, 0) != nullptr;
        if (!ok) { ready.store(false); rmt_driver_uninstall(channel); }
    }
    printf("{\"kind\":\"traffic_led\",\"ready\":%s,\"pin\":%u,\"type\":\"WS2812_GRB\"}\n",
           ok ? "true" : "false", (unsigned)CLAWGRID_STATUS_LED_PIN);
}
