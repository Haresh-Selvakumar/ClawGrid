#include "native_hal.h"
#include "safety_kernel.h"
#include "clawgrid_config.h"
#include <assert.h>
#include <stdlib.h>
static int latch[49],configured[49],calls;
static int level_calls;
int gpio_set_level(gpio_num_t pin,int value) { assert(pin>=0 && pin<49); latch[pin]=value; ++level_calls; return 0; }
int gpio_get_level(gpio_num_t pin) { return latch[pin]; }
int gpio_config(const gpio_config_t *c) {
    assert(c->pull_up_en==GPIO_PULLUP_DISABLE && c->intr_type==GPIO_INTR_DISABLE);
    if(c->mode==GPIO_MODE_OUTPUT) {
        assert(c->pin_bit_mask==(UINT64_C(1)<<4) || c->pin_bit_mask==(UINT64_C(1)<<5));
        int pin=c->pin_bit_mask==(UINT64_C(1)<<4)?4:5;
        // Actual native adapter must preserve the kernel's preloaded OFF latch.
        assert(latch[pin]==(calls<2?0:1));
        assert(c->pull_down_en==GPIO_PULLDOWN_DISABLE && level_calls>0);
        configured[pin]=1;
    } else {
        assert(c->pin_bit_mask==(UINT64_C(1)<<8));
        assert(c->pull_down_en==GPIO_PULLDOWN_ENABLE);
        configured[8]=1;
    }
    ++calls;return 0;
}
int main(void) {
    assert(NODE_A_PIR_PIN==8 && NODE_A_DHT_PIN==7);
    for(int active_low=0;active_low<2;++active_low) {
        safety_kernel_init();
        assert(safety_kernel_register_device("bedroom","light",DEVICE_TYPE_LIGHT,4,active_low)==0);
        assert(safety_kernel_register_device("bedroom","fan",DEVICE_TYPE_FAN,5,active_low)==1);
        assert(configured[4] && configured[5]);
        const char *reason=0;
        assert(safety_kernel_request_action("bedroom","light",ACTION_ON,true,100,&reason));
        assert(latch[4]==!active_low && latch[5]==active_low);
        assert(!safety_kernel_request_action("bedroom","light",ACTION_OFF,true,3099,&reason));
        assert(safety_kernel_request_action("bedroom","light",ACTION_OFF,true,3100,&reason));
        assert(latch[4]==active_low && latch[5]==active_low);
    }
    pinMode(NODE_A_PIR_PIN,INPUT_PULLDOWN); assert(configured[8]);
    return 0;
}
