#pragma once
#include <stdint.h>
#include <assert.h>
typedef int gpio_num_t;
enum { GPIO_MODE_INPUT=1, GPIO_MODE_OUTPUT=2, GPIO_PULLUP_DISABLE=0, GPIO_PULLDOWN_DISABLE=0, GPIO_PULLDOWN_ENABLE=1, GPIO_INTR_DISABLE=0 };
typedef struct { uint64_t pin_bit_mask; int mode,pull_up_en,pull_down_en,intr_type; } gpio_config_t;
int gpio_config(const gpio_config_t *config);
int gpio_set_level(gpio_num_t pin,int level);
int gpio_get_level(gpio_num_t pin);
#define ESP_ERROR_CHECK(expr) assert((expr)==0)
