#include "safety_kernel.h"
#include "platform_hal.h"
#include <assert.h>
#include <stdlib.h>
static struct { char operation; int pin,value; } calls[16];
static int count;
void pinMode(int pin,int mode) { assert(count<16); calls[count].operation='M'; calls[count].pin=pin; calls[count++].value=mode; }
void digitalWrite(int pin,int level) { assert(count<16); calls[count].operation='W'; calls[count].pin=pin; calls[count++].value=level; }
int main(int argc,char **argv) {
    assert(argc==2); bool active_low=atoi(argv[1])!=0;
    int off=active_low?HIGH:LOW,on=active_low?LOW:HIGH;
    safety_kernel_init();
    assert(safety_kernel_register_device("bedroom","light",DEVICE_TYPE_LIGHT,4,active_low)==0);
    assert(count==2 && calls[0].operation=='W' && calls[0].pin==4 && calls[0].value==off);
    assert(calls[1].operation=='M' && calls[1].pin==4 && calls[1].value==OUTPUT);
    assert(!safety_kernel_get_device(0)->is_on);
    // Invalid registration and inhibited/virtual devices never touch GPIO.
    assert(safety_kernel_register_device("bedroom","light",DEVICE_TYPE_LIGHT,5,active_low)==-1);
    assert(safety_kernel_register_device("virtual","fan",DEVICE_TYPE_FAN,-1,active_low)==1);
    assert(count==2);
    const char *reason=0;
    assert(safety_kernel_request_action("bedroom","light",ACTION_ON,true,100,&reason));
    assert(count==3 && calls[2].operation=='W' && calls[2].value==on);
    assert(safety_kernel_request_action("virtual","fan",ACTION_ON,true,100,&reason));
    assert(count==3 && safety_kernel_get_device(0)->is_on);
    assert(!safety_kernel_request_action("bedroom","light",ACTION_OFF,true,3099,&reason));
    assert(count==3);
    assert(safety_kernel_request_action("bedroom","light",ACTION_OFF,true,3100,&reason));
    assert(count==4 && calls[3].value==off);
    return 0;
}
