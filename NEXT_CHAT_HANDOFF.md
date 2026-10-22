# ClawGrid physical integration handoff — 22 October 2026

Preparation started 21 October; finalized 22 October 2026 IST (21 October UTC), branch `physical-integration-prep`. This handoff supersedes the older software summary here. Historical evidence is retained unchanged in the linked records.

## Tonight's result and hardware availability

**167 host tests passed (26.81 s), frontend checks passed.** Both inhibited roles and both explicit bench variants compiled; actual output/console/watchdog checks passed. Final image hashes are recorded below. These changes have **not been deployed or exercised on S3** tonight. Serial enumeration returned only Bluetooth COM3/COM4; no ESP32 or CH343 USB board was available. No serial device was opened, no flash/NVS was written, no policy was edited, and no credentials were regenerated. Last verified on-board state remains the normal inhibited, fault-disabled pair; that state cannot be freshly confirmed while disconnected.

| Role | Identity to verify before use | Last verified port/console | Last flashed application SHA256 (historical) |
|---|---|---|---|
| A: bedroom GPIO/PIR/SafetyKernel | `e0:72:a1:f6:a9:68` | COM9, CH343/UART0; earlier COM7 native USB | `82f3450186dae4bbf92b649059d58b4c341bfffe9fd6332d2a01219a31a5b4f2` |
| B: API/dashboard/local speech | `e0:72:a1:f5:bc:b0` | COM8, native USB | `e7bedef3a79ee225122b80290a9704e450541d13f97c2351f5b426e1586d075a` |

Do not assign a role from a COM number. Stop on a different MAC; retain existing provisioning. The installed pair's ignored `local_credentials.json` and `firmware/common/config/private_config.h` must remain intact. Never run key generation or erase-flash to resolve a connection problem. The app-only workflow writes app0 at `0x10000`, retaining the verified partition/model images and NVS at `0x9000..0xe000`.

Earlier S3 evidence: [17/17 USB bring-up](benchmarks/reports/USB_BRINGUP_20261021.md), then [3/3 watchdog trials, 69/69 checks and 16/16 restoration checks](benchmarks/reports/WATCHDOG_VALIDATION_20261021.md). The earlier host count was 144, newer than the quoted 122 baseline. Those hardware results validate their recorded hashes, not tonight's binaries.

## Repairs and reviewed behavior

- Physical PIR now calls `PirTracker::begin()` with **60 s warm-up**, then requires **200 ms continuous LOW** before arming. HIGH held through warm-up stays unarmed; it is never a startup motion event. Subsequent HIGH/LOW changes each require 200 ms filtering. The 60 s duration is provisional until checked against the actual PIR module. Readiness means this software gate passed, not validated human presence. GPIO6 uses `INPUT_PULLDOWN`; an unconnected LOW stays occupancy UNKNOWN until a qualified HIGH has occurred. `q` shows `pir_ready` and remaining warm-up; zero remaining with ready=false means waiting for LOW. Events identify `PHYSICAL_DIGITAL_GPIO`. Simulation inputs remain confined to HouseSimulation and cannot arm the physical tracker.
- The historical cached-report freshness defect is repaired on both roles. A sends only advancing control samples no older than 100 ms, and B rejects same-boot non-advancing sample timestamps even in new authenticated radio envelopes. Timestamp rollover is supported without changing schema 3 (44 bytes). B's existing 3 s freshness gate now ages frozen samples out; new commands/proposals refuse when stale. API state exposes boot/sample uptime and `nonprogress`; serial link counters expose the latter. Target validation of this repair remains pending.
- Relay polarity is an explicit, overrideable **provisional GPIO-to-interface** setting. `NODE_A_RELAY_ACTIVE_LOW=true` means OFF=HIGH, ON=LOW. An inverting transistor would generally require the opposite GPIO polarity. SafetyKernel preloads OFF before setting OUTPUT. Host HAL-call tests verify order and both polarities; they do not measure pin voltage, reset pulses or module behavior.
- Default A variants still register physical devices with GPIO=-1. They do not drive relay pins; inhibition does not provide an electrical OFF bias to a connected module. Only explicit bench environments enable GPIO4/5. Startup creates OFF devices and restores configuration only, never energized state, timers, manual holds, pending motion requests or queued commands. Failed policy/storage/self-test initialization disables command/control startup without erasing NVS. GPIO driver return codes and electrical startup still require target/electrical checks.
- Factory policy is HOME, light auto ON, fan auto OFF, grace 120000 ms. Preserved NVS can change those defaults. Last verified saved policy matches that factory policy. `/api/state` is the effective policy; `P` is the saved policy. Writes coalesce for 5 s; wait for successful `persistence_commit` and matching `P` before testing restoration.
- Manual ON/OFF establishes a 5 min automation hold. Resume releases it and can apply a retained motion request on the next tick if still eligible; it does not independently force ON. Transitions obey a 3 s dwell, including OFF requests. Timers retry an OFF denied by dwell; expiry is not a guaranteed instantaneous electrical cutoff. Vacancy clears pending motion and requests OFF, respecting manual hold. Occupancy is Occupied until 30 s since last qualified HIGH, then LikelyOccupied until the configured grace, then Vacant. After a previous HIGH, unplugging the input resembles no motion and eventually vacancy; it cannot identify that disconnection. A single PIR cannot establish stationary-person absence or diagnose every disconnected/stuck/wired-wrong input.
- Maximum continuous ON time is 4 h, enforced by the periodic kernel even if a manual ON hold is refreshed. It forces logical OFF and respects dwell before another transition; it is not a permanent fault latch. A later fresh request/event may energize again.

## Watchdog, disconnect and reboot policy

No watchdog timeouts were changed. RuntimeHealth uses `esp_task_wdt_init(5,true)`; generated A/B SDK configuration enables a **5 s task watchdog, panic, print/reboot**, and both idle-task subscriptions. A explicitly subscribes ControlTask (core1, priority5, 5 ms) and LinkTask (core0, priority3, 5 ms); B subscribes LinkTask. Each feeds its own subscription only after bounded work. Diagnostics or LinkTask cannot feed a stalled ControlTask's subscription. SpeechTask is not separately subscribed; speech-job completion and a bounded processing timeout must be observed separately. No speech-liveness guarantee is inferred from B LinkTask health.

`X` is a RAM-only ControlTask stall, enabled only by explicit diagnostic builds. It leaves link/diagnostics running and stops that task's watchdog feed. Compilation rejects diagnostic faults plus enabled outputs. Normal images reject X/d/O and B F/R/W/V. Existing three trials showed panic naming ControlTask, SDK cause 6 (ESP_RST_TASK_WDT), incrementing boot IDs, policy recovery and old-boot command/confirmation refusal; host-observed recovery was 6.267–6.270 s. No repeat was possible tonight. The maintained recovery campaign now additionally requires frozen telemetry to expire and the pending confirmation to be rejected during the stall. Old diagnostic binaries must be rebuilt before running that updated campaign.

Actual local demo policy: **B disappearing does not switch A's commanded outputs OFF.** A continues local PIR automation, manual holds, timers, vacancy and the 4 h maximum-run guard while its critical tasks remain healthy. This supports local autonomous operation; do not describe network loss as an immediate load shutdown. B marks A unavailable after 3 s without an advancing accepted sample. No retries are stored to disk. A pending command has at most four sends 300 ms apart (timeout about 1.2 s), and A validates its 3 s TTL, boot, ordered ID and duplicate bytes. An ACK timeout means outcome unknown, never proof of OFF.

A reboot creates a new persisted boot before accepting commands and starts logical OFF. B invalidates pending work on observing the new A boot. Old exact commands receive WrongBoot; old proposal confirmation fails boot binding. B reboot loses RAM proposals/transactions and seeds new proposal IDs; it waits for current A state before new submissions. A itself does not turn OFF just because B reboots. Reconnect with fresh state and prepare/confirm again; do not automatically retry an old UI request. Scenes may partially apply if one transition is denied; inspect both ACK/state fields.

## Exact software validation

Project root: `C:\Users\hares\Downloads\ClawGrid-software\AUTOHOME-main`.

```powershell
Set-Location 'C:\Users\hares\Downloads\ClawGrid-software\AUTOHOME-main'
$python = 'C:\Users\hares\Documents\Codex\2026-10-21\i-h\work\clawgrid-env\Scripts\python.exe'
$env:PLATFORMIO_CORE_DIR = 'C:\Users\hares\Documents\Codex\2026-10-21\i-h\work\pio-core'
$env:ZIG_GLOBAL_CACHE_DIR = Join-Path $PWD 'benchmarks\.host_build\zig-cache'
$env:TEMP = Join-Path $PWD 'benchmarks\review\physical-integration\temporary'
$env:TMP = $env:TEMP
& $python -m pytest tests -q --basetemp=benchmarks/review/physical-integration/pytest-after-console --junitxml=benchmarks/review/physical-integration/host-after-console.xml
& 'C:\Program Files\nodejs\node.exe' scripts/check_frontend.cjs
& $python -m platformio run -d speech -e speech_intelligence
& $python -m platformio run -d native -e native_voice_uart -e native_voice -e native_voice_bench -e native_voice_uart_bench
```

Pinned: Python 3.13.7, PlatformIO 6.2.0, Zig 0.13.0, pyserial 3.5, pytest 9.0.3, esptool 5.4.0; espressif32 6.6.0 / ESP-IDF 4.4.7 and ESP-SR 1.7.0 commit `07b0a4d0f6b5de755874d5405150ee0a8eec9865`. Existing fetched assets/cache were reused. Requirements remain in `requirements-dev.txt`; speech assets use `scripts/fetch_speech_dependencies.py`. No framework/model migration or confidence-threshold reduction occurred.

All commands above returned **0**. Native builds: four environments succeeded; B: one environment succeeded. Actual binaries passed retained output/fault marker and console checks; generated SDK configurations retain 5 s panic/reboot watchdog settings. Default mode validation rejects both bench binaries. Fresh UART bench configuration was regenerated from maintained CMake defaults after preserving the incorrect USB configuration in scratch; no private SDK/provisioning data was replaced. `pip check`, 16-clip format validation and `git diff --check` also returned 0. Installed credentials match the preserved pair backup and the changed public files contain none of the scanned private values.

| Final local image (not deployed) | Output mode / console | Application SHA256 |
|---|---|---|
| native_voice | inhibited / usb_serial_jtag | `b86539ead33cf15bae00f1851209e8802a6a2d249e6412d54714f8e5417a6316` |
| native_voice_uart | inhibited / uart0 | `8f0bc04a94922abbf5650035bf804cb2be188963e64b034d81eb70e4fc6b3174` |
| speech_intelligence | inhibited / usb_serial_jtag | `69d74bac4b349e32dca6a8469927e07366731016a33d7d98a0aacc20000f7367` |
| native_voice_bench | enabled (opt-in bench only) / usb_serial_jtag | `caf16c46fcdbcd7a7774033f7b43d6ed9f79c59cefbd95648f079952905c4742` |
| native_voice_uart_bench | enabled (opt-in bench only) / uart0 | `40a36d2d4fdb06b4372954ccd06c5f651883853165a1d5dd40283ae287ceab9c` |

[Machine-readable commands, image identities and limitations](benchmarks/raw/physical_integration_20261021.json). Binary files stay local/ignored because they embed credentials.

Working logs are in ignored `benchmarks/review/physical-integration/`: `host-after-console.log/xml`, `frontend.log`, `build-speech-final.log`, `build-native-console-fixed.log`, and `ports.log`. The initial UART bench linked with an incorrect USB console; binary/SDK guards caught it before any deployment. Pinned PlatformIO ignores the INI sdkconfig-defaults option, so maintained CMake now selects UART defaults and the HAL fails compilation on environment/console mismatch. Corrected builds passed. The initial UART bench linked with an incorrect USB console; binary/SDK guards caught it before any deployment. Pinned PlatformIO ignores the INI sdkconfig-defaults option, so maintained CMake now selects UART defaults and the HAL fails compilation on environment/console mismatch. Corrected builds passed. Initial sandbox run returned 1 (80 passed, 4 failed, 70 setup errors; compiler `error: Unexpected`); the permitted full run passed 154 tests before final guards, and final run passed 167. Both failed and passing logs are preserved. No target engine, physical latency or electrical measurement is attributed to host tests. GPIO tests capture real kernel HAL calls, not voltages.

## Identity, flash and monitor commands for tomorrow

Close bridges/monitors with Ctrl-C before ownership changes. Enumerate once, set `$aPort`/`$bPort` to observed candidates, and read identity before assigning roles:

```powershell
& $python -m serial.tools.list_ports --verbose
$aPort = 'COM9' # example only: verify MAC below
$bPort = 'COM8' # example only: verify MAC below
& $python -m esptool --chip esp32s3 --port $aPort --after no-reset read-mac
& $python -m esptool --chip esp32s3 --port $bPort --after no-reset read-mac
```

These ROM identity reads enter the loader; they are not passive runtime monitoring. A different MAC is a STOP, not permission to edit provisioning. After matching A/B and retaining current NVS/partition/model evidence, deploy the **inhibited normal pair**, together:

```powershell
& $python scripts/flash_native.py --role A --port $aPort --uart-console --app-only
& $python scripts/flash_native.py --role B-speech --port $bPort --app-only
# Separate terminals; exit with Ctrl+] before another process uses a port:
& $python -m serial.tools.miniterm $aPort 115200 --dtr 0 --rts 0
& $python -m serial.tools.miniterm $bPort 115200 --dtr 0 --rts 0
```

Flasher independently rechecks MAC, exact binary output/fault mode, console, N16R8 SDK configuration and artifact existence; it records artifact SHA256 and exit code. No erase, credentials regeneration or NVS rewrite is part of this workflow. A uses UART0 only if connected through the verified CH343 route; use native_voice without `--uart-console` for a verified native USB console. Ports are always explicit.

Check boot role, mode `outputs=inhibited;faults=default`, reset cause, `P`, `q`, `t`, advancing sample uptime, current policy and fresh schema-3 state. Request `q` again after 0.5 s: both job counters must increase. `m` is an optional bounded arena/heap probe. Every 5 s, existing logs expose internal free/minimum/largest heap, PSRAM total/free/largest, RX drops/TX failures, command drops, diagnostic drops, task jobs/max execution/max lateness/missed deadlines and stack high-water margins. API `/api/memory`, `/api/rtos`, `/api/state` and `/api/transaction` expose B memory, task timing, freshness and command/ACK RTT. These counters need fresh target observation; linker RAM percentages are not heap/headroom measurements.

For a 30 s retained telemetry capture after identity checks, close monitors and run `& $python scripts/capture_hardware.py --ports $aPort $bPort --seconds 30`. This legacy collector's firmware hash lookup targets older `.pio/build/node_*` paths: use the identity-checked flash manifests above to establish actual image provenance rather than treating that optional hash field as deployed-image proof. It sends only self-test requests unless `--probe` is supplied. Do not run another process on the same serial ports simultaneously.

For USB dashboard/audio through B (close B's serial monitor first): `& $python scripts/browser_bridge.py --serial $bPort --port 8770`, then open `http://127.0.0.1:8770/simulation`. Repeated virtual OFF prepare/confirm actions exercise transport without changing physical devices. Retain transaction RTTs, task counters/heap before and after, and bounded speech-job results; ACKs describe software acceptance, independent physical state remains **not measured**. `scripts/audio_bench.py --url http://127.0.0.1:8770 --speech-only` avoids its optional physical control command during evaluation. It uses the existing private token internally; never paste tokens in reports.

Only if watchdog repetition is needed, with every external component disconnected, rebuild `native_voice_uart_diagnostic` and `speech_intelligence_diagnostic`, flash each with the same normal command plus `--diagnostic`, then run `& $python scripts/recovery_hil.py --node-a $aPort --node-b $bPort --trials 3`. Stop at any failure. Capture panic+ControlTask, cause 6, exactly one boot increment, stale/confirmation refusal, fresh telemetry and identical saved policy. Immediately reflash the normal inhibited pair and verify fault hooks disabled and increasing task counters. Never run faults on a bench/output-enabled image.

## Pin and electrical checks before any output-enabled use

Actual parts are **two separate 5 V single-channel low-level-trigger relay modules**, not a two-channel PCB. Their GPIO compatibility, input current/voltage, isolation arrangement and reset behavior are unverified. PIR pin order and output voltage are also unverified.

| A signal | GPIO | Current software assumption | Required electrical check |
|---|---:|---|---|
| Bedroom light | 4 | Active-low GPIO: OFF HIGH / ON LOW | Separate light module IN/VCC/GND, 3.3 V compatibility and any transistor inversion |
| Bedroom fan | 5 | Same provisional polarity | Separate fan module interface, supply/current and DC motor transients |
| Bedroom PIR | 6 | Digital active HIGH, input pulldown, 60 s warm-up + LOW baseline | Identify VCC/OUT/GND from the actual sensor, supply and OUT voltage; never feed 5 V into ESP GPIO |

Before power, identify terminal labels/pin order and relay NO/COM contacts. Verify grounds/isolation from actual modules; do not defeat an isolation scheme by guessing connections. Measure the control-interface levels/current and powered/unpowered/backfeed behavior. Check GPIO voltage remains within ESP32-S3 limits. A 5 V relay supply does not imply 3.3 V-safe IN. Use a suitable interface when needed, then set GPIO polarity to the **complete** interface and recompile. No final transistor circuit/polarity is assumed. Provide a defined OFF bias during reset/boot/high impedance; firmware OFF preloading starts only after application initialization. Verify reset/cold-start behavior without loads before relying on it. For the DC fan, check supply capacity, contact DC/inrush rating and inductive suppression. Low-voltage DC only; no mains work.

Bench enable is a **later explicit step after the electrical checks**, not part of tonight's deployment. Use `native_voice_uart_bench` for CH343, or `native_voice_bench` for native USB. New flasher `--bench` selects only A's exact output-enabled/default-fault image and retains MAC/console guards. `--bench` and `--diagnostic` cannot combine. Example only after the relay-without-loads stage is ready:

```powershell
& $python scripts/flash_native.py --role A --port $aPort --uart-console --bench --app-only
# Return to inhibited operation after the bench session:
& $python scripts/flash_native.py --role A --port $aPort --uart-console --app-only
```

Do not use PlatformIO's stale upload_port defaults or bypass the guarded flasher. Bench images contain private installed credentials and stay local/ignored.

## Ordered component tests and stopping criteria

| Order | Test | Expected observation | Stop and diagnose |
|---:|---|---|---|
| 1 | **PIR alone, inhibited firmware; relays/loads disconnected** | Verified pinout and safe OUT voltage; q stays unready during 60 s warm-up, then waits for 200 ms LOW; a subsequent stable HIGH creates a physical-source edge. An unconnected LOW stays occupancy UNKNOWN. | Unsafe voltage/pin uncertainty, boot-time motion accepted, unexplained resets or HIGH stuck after warm-up. Do not simulate motion on the physical route. |
| 2 | Relay modules individually, contacts unloaded | Verify safe interface/polarity/OFF bias first, then opt-in bench. Each physical bedroom ON/OFF goes through confirmation/ACK/SafetyKernel and changes only its intended module; observe coil/contact behavior independently. Check reset/power-up stays OFF. Allow at least 3 s between transitions. | Wrong channel/polarity, voltage/backfeed problem, chatter, startup energization or unintended contact state. Disconnect relay power and restore inhibited image. |
| 3 | Light, one low-voltage DC load | Correct NO/COM wiring, manual ON/OFF, confirmed independent lamp observation, inhibited restoration after test. | ACK/state disagrees with observation, unexpected heating/current, flicker or reset. |
| 4 | Fan, separately | Correct DC rating/suppression, controlled start/stop, no brownouts/chatter; other channel unaffected. | Supply droop, repeated resets, excessive inrush/heat, inconsistent OFF behavior. |
| 5 | Automation with real PIR | Warm-up/LOW arm, factory fan-auto OFF unless deliberately changed, manual hold/resume, configured vacancy grace/timer/dwell. Disconnect B: A continues documented local policy. Reboot A: OFF and original saved configuration, no old confirmation/ON replay. | Unexpected ON, auto fan contrary to effective policy, stale command accepted, failure to recover or unmeasured electrical startup. Do not wait 4 h to infer max-run proof from a short test; the boundary is host-tested only tonight. |
| 6 | Voice last, physical target explicitly selected | First check speech-only PCM/results on B, then correct proposal/target and confirmation, software ACK, independent load observation. Negative/other-room utterances must not actuate bedroom. | Wrong ID, false activation, stale target, unexplained engine error or no detection. Preserve the failure; do not lower thresholds to obtain a pass. |

At every stage stop for loss of inhibition during USB-only tests, credential/identity mismatch, non-advancing telemetry, sustained queue overflow, declining heap/stack margin or a watchdog event outside the deliberate disconnected-load diagnostic campaign. Keep physical observations separate from software ACKs and virtual events.

Speech uncertainty remains: the latest retained recognition smoke test is **0/16 synthetic commands**, with no human corpus. Byte-exact transfer/PCM host tests and successful linking do not establish recognition. Fixed-command model, framing/end silence and bounded candidate/state/level diagnostics are already present. Keep the pinned pipeline and 0.7 threshold; AFE remains a hypothesis. Use `benchmarks/templates/audio_manifest.csv` for real recordings with speaker/session/conditions and development/held_out split; report correct/wrong/no detection/engine/transport outcomes and negatives with real denominators. Human/held-out accuracy, simultaneous speech/control timing, physical sensor/relay behavior, cold-start/brownout behavior and long-run stability remain pending.

## Files changed tonight

- `firmware/common/config/clawgrid_config.h`: provisional overrideable relay polarity, real module wording and PIR warm-up duration.
- `firmware/common/runtime/pir_tracker.h`: explicit physical startup gate and readiness diagnostics.
- `firmware/common/runtime/state_wire.h`: portable wrap-safe advancing/fresh-sample rules.
- `firmware/node_voice/src/main.cpp`: activate physical startup gate, label events, expose readiness and stop cached/aged report transmission.
- `firmware/node_intelligence/src/main.cpp`: reject frozen samples without refreshing controller age; expose boot/sample/nonprogress diagnostics.
- `native/platformio.ini`, `native/CMakeLists.txt`, `native/main/native_hal.cpp`: explicit UART bench variant and reproducible clean SDK console defaults; compile-time console mismatch refusal, leaving normal outputs inhibited.
- `scripts/flash_native.py`: guarded explicit A bench selection, mutually exclusive faults, artifact mode/console/MAC checks and manifest mode.
- `scripts/recovery_hil.py`: next target campaign must prove stale-control state and confirmation refusal during fault.
- `tests/native/integration_selftest.h`, `tests/native/firmware_checks.cpp`, `tests/unit/test_firmware_cpp.py`: PIR startup/HIGH/noise/wrap/vacancy, report progress/age, dwell/timer, restored transients and max-run boundaries.
- `tests/native/gpio_fixture/{platform_hal.h,checks.c}`, `tests/unit/test_gpio_initialization.py`: real kernel HAL call order and both polarities, inhibited/invalid devices do not write GPIO.
- `tests/unit/test_watchdog_campaign.py`: bench mode/environment and unsafe-combination refusal.
- `benchmarks/raw/physical_integration_20261021.json`: HOST_MEASURED commands, exit codes, final image hashes, checks and target-validation limits (date is UTC).
- `NEXT_CHAT_HANDOFF.md`: one current integration package; historical reports remain unchanged.

The explicit host-only dashboard path remains available with `& $python scripts/browser_bridge.py --host-dev --port 8770`. It always shows its simulation banner and never replaces a failed real connection. No simulator expansion, Pi migration or external push occurred in this preparation session.
