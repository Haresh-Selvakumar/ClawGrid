# Program both ESPs and connect the bench

Use the existing two ESP32-S3 N16R8 boards with these distinct roles:

| Board | MAC | Firmware build | Function |
|---|---|---|---|
| A | e0:72:a1:f5:bc:b0 | `native_voice` for native USB; `native_voice_uart` for UART/CH343 | PIR, light/fan policy and SafetyKernel |
| B | e0:72:a1:f6:a9:68 | `native_intelligence` | PC USB bridge, text aliases, browser API and radio command bridge |

Role selection is compiled into each build. Do not paste the same role image
onto both boards. The guarded flasher checks the assigned MAC.

Both boards need power at the same time. B needs a data USB connection to the PC
for the localhost dashboard; A can run from USB power. There are **no TX/RX wires
between A and B**: they use ESP-NOW on Wi-Fi channel 1. No microphone/speaker module
is required by the existing laptop-browser audio path.

## PIR: HW-416B on A

| PIR label | Connect to |
|---|---|
| VCC / + | A's labeled **5 V** rail while A is USB-powered |
| GND / - | A **GND** |
| OUT | A **GPIO6**, after verifying OUT is 3.3 V logic |

Use the labels on the sensor PCB; do not infer left/right order from a photo of
another revision. The HW-416B/HC-SR501 family normally uses a 5 V supply and a
3.3 V output. Check the actual unit. Never apply 5 V to GPIO6.
Allow 60 s warm-up, then a 200 ms LOW baseline before a later stable HIGH is
accepted as motion. Start with a short sensor delay so LOW can be observed.

## Relay control headers: two separate single-channel modules

| Item | Connection |
|---|---|
| A GPIO4 | Verified 3.3 V-compatible interface → **light module IN** |
| A GPIO5 | Verified 3.3 V-compatible interface → **fan module IN** |
| Each module VCC | Regulated **+5 V relay supply** |
| Each module GND | Relay supply negative; share A GND for a verified non-isolated interface |

The SRD-05VDC-SL-C text identifies the relay component's 5 V coil, not the module's
input circuit. It is insufficient to approve direct IN-to-ESP wiring. A clear
view of the PCB control header, components, jumpers and both sides is needed to
select/verify that interface. A module with separate JD-VCC/opto-isolation pins
must use its own isolation wiring instead of assuming common ground.

Do not power relay coils from A's 3V3 pin. When USB powers A and a separate supply
powers the relays, do not join the external +5 V to A's USB/5 V rail; connect only
the reference ground when required by the verified interface. GPIO polarity must
match the **whole interface**. The current provisional setting is active LOW:
HIGH requests OFF and LOW requests ON. An inverting driver requires a different
build setting. Verify unloaded contacts and a defined electrical OFF state during
reset/unpowered operation before enabling outputs.

## Relay screw terminals: one DC circuit per load

```text
DC supply + ---- fuse ---- relay COM
relay NO ---------------- load +
load - ------------------ DC supply -
relay NC ---------------- leave unused
```

Repeat for the light and fan separately. Use a lamp/fan rated for that DC supply,
adequate current capacity, suitable contact/inrush ratings and suppression
appropriate to the fan. The screw contacts do not connect to ESP GPIOs.
**Low-voltage DC loads only; no mains AC in this prototype.**

## Your picture: prepared software versus actual hardware validation

| Picture item | Prepared software | Still requires a wired bench test |
|---|---|---|
| Watchdog and recovery | 5 s watchdog, recovery/boot handling, diagnostic tooling | Recovery under the current image and final electrical setup |
| Timing, queues and memory | Per-task timing, queue/drop and heap/PSRAM diagnostics | Timing and supply stability during actual relay/load switching |
| PIR warm-up | 60 s warm-up, LOW baseline, 200 ms input filtering | Sensor voltage, real detection and false triggers |
| Pins and polarity | GPIO4/5/6 and configurable relay polarity | Module IN compatibility, OFF bias and relay startup/reset behavior |
| Automation and timers | Light/fan policy, vacancy, manual holds, timers and dwell | Real lamp/fan behavior and independent observation |
| Boot defaults and recovery | Logical OFF at boot; saved policy restored | Power cycling with complete wiring and no unintended energization |

Host tests and logical OFF/ACKs do not establish an electrical OFF state. The
normal firmware has relay GPIO outputs **inhibited**. After the interface checks,
use the explicit A bench image; first test relay contacts unloaded, then light,
then fan. Keep B on the normal native_intelligence image. Never combine diagnostics/fault
injection with output-enabled firmware.

## Build/flash reference

The local pinned Python executable is
`C:\Users\hares\Documents\Codex\2026-10-21\i-h\work\clawgrid-env\Scripts\python.exe`.
Use it in place of `python` below, from the project root. Build sequentially:

```powershell
python -m platformio run -d native -e native_voice
python -m platformio run -d native -e native_intelligence
```

For a pair already using the correct installed 16 MB layout/models, verify the
current ports/MACs, then:

```powershell
python scripts/flash_native.py --role A --port COM8 --app-only
python scripts/flash_native.py --role B --port COM7 --app-only
```

COM7/COM8 are examples reflecting this session; rediscover before use. If A uses
UART/CH343, build `native_voice_uart` and add `--uart-console`. App-only flashing
does not correct an incompatible bootloader/partition layout or install missing
speech models. See current evidence before deciding which flash mode to use.

With B connected, run `powershell -ExecutionPolicy Bypass -File .\Start-Voice.ps1 -SerialPort COM7`
and open `http://127.0.0.1:8770/voice`. Speech recognition runs offline on the PC;
B forwards commands over ESP-NOW to powered remote A. After Start listening,
recognized complete commands are submitted automatically through the existing
proposal/confirmation path. See [PC voice instructions](PC_OFFLINE_VOICE.md).

[Detailed text commands](SERIAL_TEXT_COMMANDS.md) · [Current measurements/status](CURRENT_INTEGRATION.md)

References: [Adafruit PIR connections](https://learn.adafruit.com/pir-passive-infrared-proximity-motion-sensor/connecting-to-a-pir),
[PIR startup and logic levels](https://learn.adafruit.com/pir-passive-infrared-proximity-motion-sensor/testing-a-pir),
[Espressif ESP32-S3 datasheet](https://documentation.espressif.com/esp32_s3_datasheet_en.pdf).
