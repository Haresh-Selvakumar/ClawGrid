**Superseding no-PIR scope:** [final LED demonstration](VOICE_LED_DEMONSTRATION_20261028.md). Active-HIGH lightGPIO4/fanGPIO5, own series resistors/cathodesGND, DHT11GPIO7; PIR acquisition/physicalAUTO disabled. Earlier wiring/status blocks below are historical.

# Integrated LED prototype checkpoint - 28 October 2026 IST

Status: physical manual LED control and DHT11 verified; final voice/policy/recovery tests in progress. PIR motion/AUTO acceptance is blocked by electrical measurements. This is the current integration record; older dated instructions are history.

## Installed identities

| Component | Installed image | SHA256 |
|---|---|---|
| Controller A | native_voice_uart_led_high_bench; enabled active-HIGH LED-only; GPIO8 PIR | a1a2dd04a400e7b1557df4534171aa3f34cd8cb95a5062c04bda82e01a916837 |
| Gateway B | speech_intelligence_uart; strict phone light/fan target guard | 9e7f6d9a5650df81eb27404236bfd55a7ec2a98dfa90132183456387d8a0d15b |
| Android | 1.0-led-high-pir8-20261028h; same signing identity | 1765ca848d053d23d60d963394a74715bf83ce1da2f1dd5b30e7f30871aab071 |

ROM identities: A e0:72:a1:f5:bc:b0 / CH343 serial5C37271524; B e0:72:a1:f6:a9:68 / serial5C37271527. Ports at verification COM10/COM9; re-enumerate before access. App-only guarded updates/readback and protected system verification passed. B model unchanged verified. NVS, credentials, partitions, keys and rollback copies preserved. Installed APK pulled and hash-matched.

## Wiring and electrical observations

| Function | Connection | Evidence |
|---|---|---|
| Light indicator | A GPIO4 -> own10k series resistor -> LED anode; cathode -> GND | USER_REPORTED series wiring checked; OFF0V; correct independent ON/OFF visible |
| Fan indicator | A GPIO5 -> own10k series resistor -> LED anode; cathode -> GND | USER_REPORTED series wiring checked; OFF0V; correct independent ON/OFF visible |
| DHT11 | DATA A GPIO7; existing3V3/GND | Fresh checksummed board telemetry and app display; monitoring only |
| HC-SR501 PIR | Intended OUT A GPIO8; supply negative/module GND/controller GND must share reference | **Keep OUT isolated pending valid HIGH.** User measured supply7V; OUT initially3.9V, later0.12V and0.42V during motion,0V quiet. No stable compatible motion signal established. |

Never connect supply positive9V to ESP GPIO/3V3. Generic HC-SR501 supply ratings do not prove this unit's output compatibility. The later approximate acknowledgement of0V/3.3V was superseded by explicit0.42V motion reading. Cause remains undetermined; no claim that module is defective. PIR-ready firmware flag with an isolated LOW input only proves warmup/baseline logic, not sensor presence.

GPIO8 conflict audit covers DHT7/light4/fan5/status48/microphone10-12/speaker16-18. Build-time conflicts now rejected. UART0 uses the existing SDK pin route. Native gpio_config explicitly selects GPIO function while preserving SafetyKernel OFF preload. Host tests do not prove electrical reset transients; human boot observation remains required.

## Verification

- HOST_MEASURED:17 focused GPIO/guards/state/proposal cases PASS;50 actual firmware policy/PIR/recovery cases PASS. After fan integration:51 actual parser/firmware cases PASS and80 Android actual-source control/voice cases PASS.
- Firmware builds A40.085s, first B24.809s, final B21.155s SUCCESS. Final APK h SUCCESS1m39s;83 source snapshot files byte-identical. Model archive contents/hashes, native16KB load alignment, ZIP alignment, existing signature and configured private-value boundary PASS.
- HARDWARE_MEASURED + USER_REPORTED: boot56 manual lightON#1/OFF#2, fanON#3/OFF#4 each applied ACK/status0 and matching advancing fresh state, with correct individual LED observed. End OFF/OFF. Tested APK f; g changed explanatory settings text only, and h adds strict fan voice targeting. Final h further acceptance is recorded below as completed.
- Real DHT11 updates observed23.5C ->22.0C, with valid sample ages, advancing sensor/control/link jobs and active watchdogs. This is not calibration evidence.
- Boot57 after physical power cycle: fresh authenticated OFF/OFF, last_command0, manual_flags3, auto_flags1, HOME, real vacancy grace120000ms. Gateway-only update recovered fresh telemetry without replaying prior ON requests.

## Software behavior and bounded voice test

Preserved original six light phrases and the exact whole-text parser, unrestricted bundled Vosk model, per-word0.7 gate, explicit Bedroom selection, short boot/capture-bound context, proposal expiry and explicit confirmation. Added parallel exact fan phrases through the same parser and gateway guard because the old path explicitly rejected fan. Fan voice requires its own manual fan ACK+fresh readback in the current boot. No model migration, grammar forcing, fuzzy matching, confidence relaxation, audio upload or automatic confirmation.

Defined test set: light on, light off, fan on, fan off; one cancelled light-on proposal; unrelated what time is it. Recognition alone never commands an actuator. Raw audio/transcripts remain memory-only; export needs user permission if a failure requires exact diagnostics.

Manual ON/OFF holds until explicit AUTO for that device. Returning light to AUTO must preserve fan manual override and vice versa. Actual board vacancy grace is120s; firmware motion filter200ms plus60s startup and200ms LOW baseline. Temperature never drives the hardware fan. Physical motion/vacancy actuation cannot pass with the unresolved PIR signal.

## Demonstration sequence

1. Join ClawGrid-Assistant; keep mobile data off; apply existing gateway URL/token privately. Require fresh Source BOARD, enabled exact active-HIGH profile and both indicators OFF.
2. Demonstrate independent manual light and fan ON/OFF with explicit confirmation and ACK+fresh readback; allow3s dwell; end OFF.
3. On each new controller boot, verify manual light/fan OFF ACK/readback before voice. Select Bedroom; hold until Speak now; speak one supported phrase; review exact device/action; Confirm. Demonstrate cancellation and unrelated speech with no command.
4. PIR/AUTO demonstration remains blocked until isolated module OUT is near0V quiet and stable around3.3V motion, labels/supply/common ground verified. Power down before connecting OUT GPIO8. Then verify real motion, per-device manual holds, AUTO independence and quiet vacancy at actual grace.
5. Reconnect/restart with held-OFF startup; require fresh state before controls; confirm no old ON replay. End both manual OFF.

Evidence: benchmarks/review/gpio8-integrated-20261028. Automated records, phone observations and user physical observations are labelled separately. Project completion is not claimed while physical PIR/AUTO or bounded final acceptance is pending.
