# ESP32-S3 connections and first run

Current role assignment: **A (relays/PIR) = COM8, MAC e0:72:a1:f5:bc:b0; B (PC USB bridge) = COM7, MAC e0:72:a1:f6:a9:68**. Port numbers may change after reconnection. Earlier measurements used the previous assignment.

This guide applies to the existing ClawGrid pair of **ESP32-S3 N16R8** boards.
Use the **GPIO numbers printed on the board**, not the physical header position.

| Board | Assigned MAC | Job |
|---|---|---|
| A | `e0:72:a1:f5:bc:b0` | Bedroom PIR, light, fan, SafetyKernel |
| B | `e0:72:a1:f6:a9:68` | Dashboard/API and ESP-SR speech processing |

Both boards connect to the PC by USB. A and B communicate by encrypted
ESP-NOW on channel 1: **no TX/RX jumper wires are needed between them**.
COM numbers can change; verify MAC before assigning a role.

## HW-416B PIR on board A

The user identified the PIR as **HW-416B**, commonly sold as an HC-SR501
variant. For that module, use **5 V VCC**, GND and its nominal 3.3 V OUT.
Confirm the terminal labels and actual output voltage on the unit before hookup.

| Sensor terminal | Connect to |
|---|---|
| OUT | A **GPIO6**, provided OUT is verified 3.3 V logic |
| GND | A **GND** |
| VCC | **5 V** for the identified HW-416B/HC-SR501 module |

Do not guess the sensor terminal order. A sensor powered from 5 V may or may
not have a 3.3 V output. Measure/check its OUT specification before connecting
GPIO6; use a suitable level interface if necessary. Do not connect 5 V to a GPIO.

The firmware waits **60 seconds**, then requires **200 ms LOW** before arming.
A later stable HIGH indicates motion. One PIR covers the bedroom only.
An unconnected input is not evidence of an empty room.

## Two separate single-channel relay modules on board A

The supplied handoff identifies **5 V, low-level-trigger modules**, but does not
establish their 3.3 V input compatibility. The signal assignments are fixed;
the electrical interface remains dependent on the actual module.

The user supplied the marking **SRD-05VDC-SL-C**. This identifies the Songle
5 V coil relay component, not the surrounding module's input/driver circuit.
It therefore does not establish whether IN can connect directly to an ESP GPIO.

| Module terminal | Connection |
|---|---|
| Light IN | A **GPIO4 through a verified compatible control interface** |
| Fan IN | A **GPIO5 through a verified compatible control interface** |
| Each module VCC | Regulated **5 V relay supply**, if confirmed by its label/specification |
| Module GND | Interface reference ground; share with A GND for a verified non-isolated interface |

For modules with separate JD-VCC/opto-isolation wiring, follow that module's
schematic instead of assuming the ground or jumper arrangement above.
Never power a relay coil from an ESP GPIO or the 3V3 pin.

**Direct IN-to-GPIO wiring is only appropriate after verifying the module accepts
3.3 V control, does not pull IN above the GPIO limit, and remains OFF through
reset and either supply being unpowered.** Otherwise use an appropriate
level/driver interface. Identify the module before selecting that circuit.

Current firmware assumes the complete interface is active LOW:

| GPIO level | Requested relay state |
|---|---|
| HIGH | OFF |
| LOW | ON |

A driver can invert this relationship. In that case set
`NODE_A_RELAY_ACTIVE_LOW=false` in the build and verify it with unloaded contacts.
The complete interface must provide an OFF bias while the ESP is resetting or
unpowered. Application initialization alone cannot guarantee that condition.

## Relay contact wiring: low-voltage DC loads only

Wire each module's contacts as a separate switch:

```text
DC load supply +  ---- fuse ---- relay COM
relay NO         -------------- load +
load -           -------------- DC load supply -
relay NC         -------------- leave unused
```

Use the light module for the lamp and the fan module for the fan. These contact
circuits are separate from the relay module's VCC/IN/GND control header. Do not
connect a relay contact to an ESP pin. Size supplies, fuse, wiring and contact
DC/inrush rating for the actual load. For an inductive DC fan/motor, use suppression
appropriate to that exact load and its electronics. **No mains AC in this prototype.**

## Board B and voice

B needs USB power/data only for the existing workflow. The laptop browser records
audio; B runs the speech model. The microphone/speaker pin constants in the
config header are reserved: the current firmware does **not** implement an
INMP441/MAX98357A audio driver. Wiring those modules alone will not enable voice.
The retained synthetic recognition result is 0/16, so recognition still needs
diagnosis and real recordings before relying on voice control.

## Build, deploy and check

Keep existing `local_credentials.json` and `private_config.h`; do not regenerate
the paired keys or erase flash. Build sequentially using the pinned toolchain:

```powershell
python -m platformio run -d native -e native_voice
python -m platformio run -d speech -e speech_intelligence
```

With relay power and all loads disconnected, verify each observed board's MAC.
For boards using native USB, deploy the default **output-inhibited** images:

```powershell
$aPort = 'COM7' # replace with current identity-verified A port
$bPort = 'COM8' # replace with current identity-verified B port
python scripts/flash_native.py --role A --port $aPort --app-only
python scripts/flash_native.py --role B-speech --port $bPort --app-only
```

`--app-only` is for this previously provisioned pair with its verified existing
partition/boot/model installation. It does not install missing speech models.
For A on the CH343/UART0 route, build `native_voice_uart` and add
`--uart-console` to its flash command. Use the guarded flasher, not the INI's
historical upload-port defaults.

On A, serial commands `t`, `P`, and repeated `q` verify self-tests, preserved
policy, increasing control/link job counters, output inhibition and PIR readiness.
Verify B receives advancing A state before submitting control commands.

After closing B's serial monitor:

```powershell
python scripts/browser_bridge.py --serial $bPort --port 8770
```

Open `http://127.0.0.1:8770/simulation`, enter the API token privately from the
local credentials file, and explicitly select the physical bedroom target when
testing physical control. Virtual-room controls do not actuate bedroom relays.
Commands pass through proposal/confirmation, ACK and SafetyKernel.

**Default firmware does not drive GPIO4/5.** After module identification,
interface/polarity/reset checks, and a PIR-only check, build the opt-in image and
test each relay with contacts unloaded before connecting DC loads:

```powershell
python -m platformio run -d native -e native_voice_bench
python scripts/flash_native.py --role A --port $aPort --bench --app-only
# Restore output inhibition after the bench session:
python scripts/flash_native.py --role A --port $aPort --app-only
```

Use `native_voice_uart_bench` and `--uart-console` for the verified UART0 route.
Allow at least 3 seconds between transitions. Observe actual relay contacts/load
behavior separately from software ACKs. Stop on startup energization, wrong
polarity/channel, chatter, overheating, brownouts or unexplained resets.

Factory policy is light automation ON, fan automation OFF, 120 s vacancy grace;
preserved policy may differ. Manual control holds automation for 5 minutes.
B disconnecting does not force A OFF: A continues local control. Rebooting A
starts logical OFF. See `NEXT_CHAT_HANDOFF.md` for the complete behavior and tests.

Electrical reference: [Espressif ESP32-S3 datasheet](https://documentation.espressif.com/esp32_s3_datasheet_en.pdf).
PIR references: [HW416B/HC-SR501 supplier identification](https://www.taydaelectronics.com/pir-motion-sensor-hc-sr501.html),
[Adafruit PIR connection guide](https://learn.adafruit.com/pir-passive-infrared-proximity-motion-sensor/connecting-to-a-pir),
and [PIR startup and retriggering](https://learn.adafruit.com/pir-passive-infrared-proximity-motion-sensor/testing-a-pir).
Relay reference: [Songle SRD component datasheet](https://lcsc.com/datasheet/lcsc_datasheet_1811021933_Ningbo-Songle-Relay-SRD-05VDC-SL-C_C35449.pdf).
