#include "traffic_indicator.h"
#include <assert.h>
#include <stdio.h>

int main() {
    TrafficIndicator idle;
    assert(idle.tick(0, 0) == 0);
    assert(idle.tick(TrafficTx, 10) == 0x000008);
    assert(idle.tick(TrafficTx, 69) == 0x000008);
    assert(idle.tick(TrafficTx, 70) == 0);
    assert(idle.tick(TrafficTx, 149) == 0);
    assert(idle.tick(TrafficTx, 150) == 0x000008);

    TrafficIndicator command;
    command.tick(TrafficTx, 0);
    assert(command.tick(TrafficCommand, 5) == 0x000800);
    assert(command.tick(TrafficRx | TrafficTx, 100) == 0x000800);
    assert(command.tick(TrafficTx, 185) == 0);

    TrafficIndicator ack;
    ack.tick(TrafficCommand, 0);
    assert(ack.tick(TrafficAck | TrafficRx, 10) == 0x080808);
    assert(ack.tick(TrafficRx | TrafficTx, 289) == 0x080808);
    assert(ack.tick(0, 290) == 0);

    TrafficIndicator failure;
    failure.tick(TrafficAck, 0);
    assert(failure.tick(TrafficError, 10) == 0x080000);
    assert(failure.tick(TrafficTx, 100) == 0x080000);
    assert(failure.tick(0, 230) == 0);

    TrafficIndicator wrap;
    const uint32_t start = UINT32_MAX - 20;
    assert(wrap.tick(TrafficTx, start) == 0x000008);
    assert(wrap.tick(0, start + 59) == 0x000008);
    assert(wrap.tick(0, start + 60) == 0);
    assert(wrap.tick(TrafficRx, start + 139) == 0);
    assert(wrap.tick(TrafficRx, start + 140) == 0x000800);

    TrafficIndicator stream;
    unsigned off = 0, lit = 0;
    for (uint32_t now = 0; now < 1000; now += 10) {
        if (stream.tick(TrafficRx | TrafficTx, now)) ++lit; else ++off;
    }
    assert(off > 30 && lit > 30);
    puts("HOST_MEASURED: LED timing, telemetry coalescing, command/ACK/error priority and clock-wrap checks passed.");
}
