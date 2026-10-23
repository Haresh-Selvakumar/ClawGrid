# Onboard ESP-NOW traffic LED

The normal native A and B images now drive the onboard addressable RGB LED.
GPIO48 is the project default. GPIO38 is used by official DevKitC-1 v1.1;
verify the board revision if the onboard RGB LED does not respond. Change
`CLAWGRID_STATUS_LED_PIN` in `firmware/common/config/clawgrid_config.h` and rebuild
both roles to change this setting. Do not wire an LED to GPIO4/5/6 for this feature.

| Indication | Meaning |
|---|---|
| Short blue pulse, either board | ESP-NOW send callback reported delivery at the MAC layer |
| Short green pulse, either board | Authenticated frame from the configured peer, with expected node IDs |
| Longer green pulse on A | A decoded a command and queued it for its ControlTask |
| White pulse on B | A matching authenticated application ACK says the command was accepted |
| Red pulse | Send failure, command queue overflow, rejected command ACK or application ACK timeout |

Background room/virtual telemetry is also data, so regular traffic pulses are
expected while the boards communicate. Nearby events can merge; a stronger
command/ACK/error indication takes precedence. Pulses are dim to limit LED power.
A red timeout means the final command outcome is unknown; it does not establish
that nothing was applied. Consult fresh state and the webpage ACK message.

A blue pulse alone is not proof that A's application accepted a command. A white
ACK also does not independently measure relay contacts or the load. Physical
relay GPIOs remain inhibited in the normal images.

## Implementation and isolation

`firmware/common/runtime/traffic_indicator.h` schedules pulses using wrap-safe
timing. `native/main/traffic_led.cpp` drives one WS2812-compatible GRB pixel using
ESP-IDF 4.4 RMT channel 0. Radio callbacks only update atomic event bits; RMT waits
occur in a separate priority-1 task. LED setup/transmit errors do not disable the
radio or SafetyKernel. The `traffic_led` serial startup record reports driver
setup/pin, not electrical detection of the physical RGB LED.

## Upload the prepared images

Both images need updating to show the complete pattern. The user-confirmed ports
are A COM8 (relays/PIR) and B COM7 (PC bridge); rediscover after reconnecting.
Temporarily connect A by data USB for its upload. Keep relay power disconnected.
Stop the PC voice bridge and any serial monitors before uploading B.

From the project root in PowerShell:

```powershell
$py = 'C:\Users\hares\Documents\Codex\2026-10-21\i-h\work\clawgrid-env\Scripts\python.exe'
& $py scripts/flash_native.py --role A --port COM8 --app-only
& $py scripts/flash_native.py --role B --port COM7 --app-only
.\Start-Voice.ps1 -SerialPort COM7
```

App-only is appropriate for this pair's already verified 16 MB layout. The
guarded flasher still checks each role MAC, pair marker and inhibited-output
marker, and does not erase NVS. A returns to remote power after upload.

Then open http://127.0.0.1:8770/voice, select Virtual rooms with the Hall bench
alias unchecked, Start listening and say `bedroom light on`. B's white flash plus
the webpage's `ACK: controller accepted` is evidence of A's software ACK. A
physical lamp is not expected to switch with outputs inhibited.

## Verification limits

The portable timing/priority/wrap tests are HOST_MEASURED, not a visual hardware
test. Compilation confirms the native driver/API integration. Actual onboard
RGB pin/color/brightness and radio behavior require observation after uploading.
Existing live radio evidence before this feature applies to the earlier images.

References: [Espressif board revision LED pins](https://documentation.espressif.com/esp-dev-kits/en/latest/esp32s3/esp32-s3-devkitc-1/user_guide_v1.1.html),
[ESP-NOW send callbacks and application ACKs](https://docs.espressif.com/projects/esp-idf/en/v4.4.4/esp32s3/api-reference/network/esp_now.html).
