#pragma once
#include "dht11_decode.h"
#include "clawgrid_config.h"
#include "driver/gpio.h"
#include "esp_timer.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"

// SensorTask owns this adapter and installs its GPIO ISR on core0. It never
// polls pulse edges or masks interrupts for a DHT frame. The bounded ISR only
// records edges; decoding runs after the10ms capture window. No legacy RMT RX
// driver (ESP-IDF4.4.7 issue13419) or dynamic per-sample allocations.
class Dht11Gpio {
public:
#if defined(CLAWGRID_NODE_C)
    // SensorTask only, after read() disables the ISR; never change thresholds
    // or publish invalid readings merely to diagnose a physical sensor.
    DhtPulse diagnosticPulse(size_t index) const { return index<count_ ? pulses_[index] : DhtPulse{0,false}; }
#endif
    bool begin() {
        const auto pin=(gpio_num_t)NODE_A_DHT_PIN;
        if(gpio_set_level(pin,1)!=ESP_OK || gpio_set_direction(pin,GPIO_MODE_INPUT_OUTPUT_OD)!=ESP_OK ||
           gpio_set_pull_mode(pin,GPIO_PULLUP_ONLY)!=ESP_OK) return false;
        // No other project input owns the GPIO ISR service. Fail unavailable
        // rather than assume an existing service's core/handler ownership.
        // Classic C shares a core with radio interrupts. Captured failures show
        // merged acknowledgement edges and delayed bit edges at default level1.
        // Level3 still uses the same bounded C ISR; no interrupt masking/polling
        // and no change to checksum, pulse-width or range validation.
#if defined(CLAWGRID_NODE_C)
        if(gpio_install_isr_service(ESP_INTR_FLAG_LEVEL3)!=ESP_OK) return false;
#else
        if(gpio_install_isr_service(0)!=ESP_OK) return false;
#endif
        if(gpio_set_intr_type(pin,GPIO_INTR_ANYEDGE)!=ESP_OK || gpio_isr_handler_add(pin,edge,this)!=ESP_OK) return false;
        gpio_intr_disable(pin); return true;
    }
    TemperatureSample read(uint32_t now) {
        TemperatureSample result; result.status=TemperatureStatus::DriverError; result.sampled_ms=now;
        const auto pin=(gpio_num_t)NODE_A_DHT_PIN;
        gpio_intr_disable(pin);
        portENTER_CRITICAL(&mux_); recording_=false; count_=0; overflow_=false; portEXIT_CRITICAL(&mux_);
        if(gpio_set_level(pin,0)!=ESP_OK) return result;
        vTaskDelay(pdMS_TO_TICKS(20)); // Host start LOW>=18ms; ControlTask is never blocked.
        portENTER_CRITICAL(&mux_); last_us_=(uint32_t)esp_timer_get_time(); previous_=false; recording_=true; portEXIT_CRITICAL(&mux_);
        if(gpio_intr_enable(pin)!=ESP_OK) { portENTER_CRITICAL(&mux_); recording_=false; portEXIT_CRITICAL(&mux_); gpio_set_level(pin,1); return result; }
        gpio_set_level(pin,1); // Open drain release to module's3.3V pullup.
        vTaskDelay(pdMS_TO_TICKS(10));
        gpio_intr_disable(pin);
        portENTER_CRITICAL(&mux_); recording_=false; portEXIT_CRITICAL(&mux_);
        result.pulse_count=(uint16_t)count_;
        if(overflow_) { result.status=TemperatureStatus::Invalid; return result; }
        if(count_<82) { result.status=TemperatureStatus::Timeout; return result; }
        return decode_dht11(pulses_,count_,now);
    }
private:
    static void edge(void *arg) {
        auto &self=*(Dht11Gpio*)arg;
        const uint32_t now=(uint32_t)esp_timer_get_time();
        const bool high=gpio_get_level((gpio_num_t)NODE_A_DHT_PIN)!=0;
        portENTER_CRITICAL_ISR(&self.mux_);
        if(self.recording_ && high!=self.previous_) {
            if(self.count_>=100) { self.overflow_=true; self.recording_=false; }
            else {
                const uint32_t elapsed=now-self.last_us_;
                self.pulses_[self.count_++]={(uint16_t)(elapsed>65535?65535:elapsed),self.previous_};
                self.last_us_=now; self.previous_=high;
            }
        }
        portEXIT_CRITICAL_ISR(&self.mux_);
    }
    portMUX_TYPE mux_=portMUX_INITIALIZER_UNLOCKED;
    bool recording_=false;
    DhtPulse pulses_[100]={};
    size_t count_=0; bool overflow_=false,previous_=false;
    uint32_t last_us_=0;
};
