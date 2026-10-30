**Superseding no-PIR scope:** [final LED demonstration](VOICE_LED_DEMONSTRATION_20261028.md). Active-HIGH lightGPIO4/fanGPIO5, own series resistors/cathodesGND, DHT11GPIO7; PIR acquisition/physicalAUTO disabled. Earlier wiring/status blocks below are historical.

**Current 28 October update:** [GPIO8 active-HIGH LED checkpoint](INTEGRATED_LED_CHECKPOINT_20261028.md) supersedes earlier deployment/status statements below. PIR OUT currently requires an electrical compatibility check; keep it isolated.

# DHT11 and LED preparation — 27 October 2026 IST

## Actual inventory and wiring boundary

User reports visible sensor connector labels PIR VCC/OUT/GND and DHT VCC/DATA/GND; these supersede the earlier unreadable-photo limitation. The preceding photos show HC-SR501-style PIR and blue DHT11-style three-pin breakout. User answered the explicit board/model/free-pin/resistor check affirmatively: header4/5/6/7/3V3/5V/GND, GPIO7 unused, DHT11 and checked resistor colour code. This is user confirmation, not an independently read close-up or electrical measurement. Corrected inventory is **two10kΩ LED series resistors**, superseding the earlier reported blue band/10MΩ inference. Four-band10kΩ±5% is brown-black-orange-gold. Do not reuse the superseded10MΩ identification as the current inventory.

Both boards are unplugged for wiring preparation; fresh host enumeration shows no ESP serial adapters. Last verified board state remains boot24/OFF/OFF/outputsINHIBITED from the earlier baseline, not a new powered measurement. All relay modules and appliance loads remain unused. GPIO output enablement has not occurred.

The HC-SR501 family reference uses5V supply and3.3V digital signalling; check the actual module OUT level before connecting to ESP GPIO8. The manufacturer's DHT11 datasheet specifies3.3–5.5V; use3.3V for this breakout so its pullup cannot apply5V to the ESP input. If this actual revision does not work at3.3V, diagnose supply/specification before any5V change; a5V pullup must not be connected directly to an ESP input. Sources: [Joy-IT HC-SR501 manual](https://joy-it.net/files/files/Produkte/SEN-HC-SR501/SEN-HC-SR501_Manual_2024-04-16.pdf), [Aosong DHT11 datasheet, manufacturer document mirrored](https://device.report/m/185b261bd57a556c843c6d3bf978e78fc3d57d88239ebaa1b4c82f0c00e5c06d), [ESP32-S3 datasheet](https://documentation.espressif.com/esp32_s3_datasheet_en.pdf).

This table is the staged design, **not permission to wire all stages at once**. Give/acknowledge one power-disconnected step at a time. Only A gets component wiring; B remains gateway.

| Stage/function | Actual labelled component connection | Controller A connection / condition |
|---|---|---|
| Current queued step: PIR power only | VCC; GND |5V; GND, both USBs unplugged. OUT stays disconnected; await completion acknowledgment |
| PIR signal, later | OUT |GPIO8 only after supply/OUT voltage check; reuse60s warmup/200ms LOW baseline/200ms filter |
| DHT11, later | VCC; DATA; GND |3V3; GPIO7; GND. Firmware GPIO7 newly configured; no wire-colour or left/right assumptions |
| Light LED, later inspected LED-only output stage |3V3 → own10kΩ resistor → LED anode; LED cathode |GPIO4. Deliberate active-LOW sinking circuit matching existing active_low=true |
| Fan LED, later inspected LED-only output stage |3V3 → own10kΩ resistor → LED anode; LED cathode |GPIO5. Separate resistor, same active-LOW convention |

For each LED at3.3V the resistor bounds current below0.33mA even ignoring forward voltage; actual current/brightness depend on colour. Low brightness accepted by user. Inhibited firmware registers actuator pins as-1 and will not display logical state on these LEDs; no physical indicator success until a separately inspected, guarded LED-only enabled stage. No5V on LED/GPIO circuits, no relay/appliance demonstration. Keep3V3 and5V rails distinct. Preserve UART43/44, USB19/20 and memory/strap pins.

## Implemented, not yet hardware validated

- Added DHT11 GPIO7 configuration, portable40-bit decoder/acknowledgment-pulse checks/checksum and conservative0–50°C/0–100%RH bounds. DHT22 is not silently autodetected. Invalid frames return an explicit status and no reading.
- SensorTask: priority1/core0, static stack/mailbox,20ms host-start sleep +10ms edge-capture sleep, reads separated by2500ms after2s settling. At most100 pulses in a fixed buffer; GPIO ISR only timestamps/transcribes an edge with a short protected update. No full-frame interrupt masking/busy polling/per-read heap allocations, no waits in ControlTask. SensorTask joins the existing watchdog when registration succeeds; q diagnostics expose its jobs/watchdog. Essential control/link readiness unchanged; temperature failure is unavailable monitoring, not an actuator-policy change.
- Initial source-only RMT RX sketch was replaced before any build/deploy after checking Espressif's open [legacy S3 IDF4.4.7 RX buffer issue13419](https://github.com/espressif/esp-idf/issues/13419). Existing onboard RGB RMT TX0 remains unchanged. No SDK/model migration or private patch.
- Version4 room report56bytes adds deci-Celsius, deci-%RH, status and sample age. New B also decodes old schema3/44byte reports, explicitly clearing temperature for B-first rollout. Reserved bytes/status/ranges/age validated. No fabricated reading on boot/disconnect/checksum/error/stall; successful readings expire after10s.
- Gateway appends telemetry_schema4, numeric values only when valid, otherwise JSON null plus NOT_READY/TIMEOUT/CHECKSUM/INVALID/DRIVER_ERROR/STALE. Age includes room-report age. Android adds request transit/elapsed time and requires fresh controller state; hardware Bedroom card shows DHT11 temperature or unavailable/status. Other room values remain unknown. No temperature-driven fan behavior.
- Manual fan/light proposal→Confirm→matching ACK+fresh report, ESP-NOW security, boot/expiry, SafetyKernel, RTOS/watchdog, PIR tracker, simulation boundary and on-phone offline recognition are retained. Current voice safety gate still requires inhibited outputs; LED-enabled voice support is not claimed/changed by this sensor milestone. Manual-untilAUTO/HOME-AWAY semantics remain a separate pending milestone.

Changed: firmware/common/config/clawgrid_config.h; runtime/dht11_decode.h, dht11_gpio.h, state_wire.h; node_voice/src/main.cpp; node_intelligence/src/main.cpp; Android HardwareRepository.kt, ClawGridViewModel.kt, RoomsAndRelaysScreen.kt, build.gradle.kts; tests/native/dht11_selftest.h, firmware_checks.cpp, software_selftest.h; tests/unit/test_firmware_cpp.py; Android HardwareRepositoryTest.kt.

## Actual checks/artifacts

Evidence: benchmarks/review/dht11-20261027/. Existing installed A/B firmware.bin+ELF and source/doc snapshots saved in before/ and baseline/ with hashes; candidate bin/ELF copied separately. Neither baseline backup was overwritten.

| Check | Actual result |
|---|---|
| pytest tests/unit/test_firmware_cpp.py -q | First attempt could not create fixture temp dir: WinError5 on existing AppData/Local/Temp/pytest-of-hares;45 setup errors, no firmware assertions executed. Log preserved |
| Same tests --basetemp C:/Users/hares/Documents/ClawGrid-test-dht11-20261027 | Fresh isolated directory verified absent first;45PASS40.45s, includes actual C/C++ decoder/frame corruption/schema3+4/age-wrap/stale and existing SafetyKernel/PIR/control cases |
| scripts/check_android_control_jvm.py |72PASS1.156s; includes schema/status/value/type/age checks and missing/failed sensor remaining compatible with manual control |
| platformio run -d native -e native_voice_uart |SUCCESS69.443s; linked RAM69,324B, flash686,913B (build sizes, not runtime resource measurements) |
| platformio run -d speech -e speech_intelligence_uart |SUCCESS24.466s |
| build_phone_outside_onedrive.py --name dht11-20261027b --evidence benchmarks/review/dht11-20261027 --build-folder dht11-build-20261027 --apk-name ClawGrid-DHT11-debug-20261027b.apk --gradle-cache C:/Users/hares/Documents/ClawGrid-Android-build-20261025b/gradle-user |SUCCESS1m36s;83source files byte-identical; existing debug key/release configuration retained |
| Firmware artifact mode/pair/UART guards |PASS for both candidate images: inhibited/default faults/expected provisioning/console |
| APK offline-model/native4ABI/16KB alignment/signature/client-boundary |PASS model14hashes, bundled local engine, zipalign, same debug cert71ce1bf6..., private-value matches0/privileged fields0 |
| Deployment / real sensors |NOT RUN. Both ESP serial ports absent during powered-off wiring stage; no flash write/proposal/control/recording or real DHT reading |
| New APK installation |NOT RUN: phone unavailable/unauthorized to ADB at attempt; no install command issued. Do not claim new build installed |

Candidate A native_voice_uart:687,280B/SHA2562b59eab9af0b05367d0e503682998d868ea0df1c9c6e98102b720ce2eb6ed64b. Candidate B speech_intelligence_uart:1,214,352B/SHA256e549037e0093829bfa02882c0141ca1c7db0eac05cafed1f986334649e29d7b8. Both **built only, not installed**. Saved installed baseline remains A8f028bd9... and B97daa84e...; no model/NVS/partition/key write.

New APK deliverables/ClawGrid-DHT11-debug-20261027b.apk/version1.0-dht11-20261027b/code1/100,112,964B/SHA25659b6b2423992bc3c1f6cfd2b080da8d6abd0a173b437a94f0db72cd8dee12fac. Snapshot C:/Users/hares/Documents/ClawGrid-Android-build-dht11-20261027b. Last verified installed phone remains **1.0-local-led-20261027a/SHA365ee5e6...**, from prior milestone. Voice engine/model/parser/.7threshold unchanged; no new speech trial.

## Next executable step and guarded deployment order

1. Await acknowledgment of **PIR VCC→A5V and GND→AGND only, all power disconnected; OUT and other components disconnected**. Then guide supply/OUT measurement before connecting signal, with each wiring change powered off. Do not advance on silence.
2. When boards are deliberately powered again, enumerate fresh ports and ROM MACs against provisioning, confirm actual setup. Snapshot existing state/inhibition; discard all prior request/boot context. Preserve both candidate and rollback copies.
3. Deploy compatible inhibited B first (schema3 backward decoding), then A, each using scripts/flash_native.py --role B-speech|A --port <fresh MAC-verified port> --uart-console --app-only. Never --bench/--diagnostic or a model/partition update. Verify binary against candidate manifest, current image/protected model/system against saved baseline before writing, then application/protected-region readbacks after. Stop on mismatch or failed write; restore saved normal baseline app-only if needed. Do not erase NVS, models or flash; do not restore old NVS/replay counters.
4. Verify fresh advancing OFF/OFF/inhibited state, schema4 and explicit unavailable status with DHT still disconnected; task/watchdog/link/AP health. Install/hash-verify new APK when phone USB is available. Reapply private token/manual-light proof only when wiring/boot context is stable.
5. Guide DHT VCC3V3/DATAGPIO7/GND using actual labels, powered off; verify real readings A→B→Android and timeout/stale/unplug behavior. No physical success until observed. Continue later LED-only/manual/AUTO/recovery/voice milestones and package actual results.

**Physical identity checkpoint:** User acknowledged the PIR supply-only step, then asked whether blue- or green-blinking board is A/B. Physical association of the wired board is therefore unresolved; do not record these wires as definitely on A. Blink colours are activity indicators, not a role identifier. Fresh Windows enumeration shows no CH343 ports. Requested connecting ONLY the board carrying PIR VCC/GND via its UART/CH343 socket, other board unplugged/OUT disconnected; await acknowledgment, then enumerate and ROM-MAC verify it and label physically before further wiring. No flash/serial probe/new test for this clarification.

**Physical A identified:** After user connected only the PIR-supply board, fresh enumeration found only CH343 COM10/adapter5C37271524. ROM MACe0:72:a1:f5:bc:b0 matched provisioned controllerA; normal reset, no write. Evidence benchmarks/review/dht11-20261027/physical-single-board-identity.json and .log. The user-reported PIR VCC/GND wires are therefore associated with A; requested physical A label. B is unplugged, must freshly verify when connected. Blink colour is not used. Next queued action is DC-voltage measurement at PIR VCC/GND and HIGH OUT/GND with OUT still disconnected; await readings or explicit no-meter report before signal wiring. No new firmware deployed, sensor/LED readings not yet observed.

# HARDWARE HOLD — PIR voltage report, 27 October 2026 IST

User reports approximately0.67 and0.6 for requested PIR VCC/GND and HIGH OUT/GND checks. Units/range and exact measurement mapping are not yet confirmed; this is USER_REPORTED, not independently measured or proof of a faulty module. If voltsDC in requested order, supply is far below expected5V. PIR OUT remains disconnected; do not connect GPIO6, DHT or LEDs, enable outputs, flash or infer motion success from these readings.

Immediate single physical step requested: unplug controllerAUSB and remove both PIR supply wires, B unplugged/all components disconnected; await acknowledgment before repowering. Also requested meter DC range/display units and exact source pin label5V/VBUS versus5/GPIO5. Check meter setup and bare-board rail at the next deliberate stage; don't assume a resistor, voltage or module fault or move VCC to another rail blindly. All source/build artifacts and installed baseline remain unchanged. Latest code candidates remain built-only/not deployed; last verified installed phone1.0-local-led-20261027a. No new host/board tests, serial connection, firmware write or output enablement for this follow-up.

---


# CURRENT HARDWARE CHECK — bare-board5Vin measurement pending, 27 October 2026 IST

User confirms controllerAUSB unplugged/PIR supply wires removed/all components disconnected/B unplugged. Reports0.01 and0.00 and says original supply connection was labelled5Vin/GND. Exact units/meter setting and whether these latest readings were taken powered are still unconfirmed; near-zero is expected if measured after the requested unplug, so do not diagnose a defective rail/sensor from this report.

Queued next physical action: with every component disconnected and B unplugged, set meter DCV/20V or autorange (COM and VΩ leads), connect only AUSB, probe GND→5Vin without bridging adjacent pins; report reading/units and powerLED. No sensor signal wiring or output enablement until supply/logic checks pass. No serial probe/build/test/flash for this clarification; driver candidates remain built-only/not deployed, phone remains last verified local-led-a. Prior readings and context remain history below.

---


# CURRENT — DHT wiring resolved; inhibited deployment preflight in progress, 27 October 2026 IST

User confirmed DATA is on the pin **printed7/IO7/GPIO7**, resolving physical-header-index ambiguity, then confirmed both USB boards powered. DHT VCC3V3/DATAGPIO7/GND on physically MAC-identified A; PIR/LEDs/relays disconnected. DHT power indicator is user-reported power only, not sensing proof. Fresh MAC verification ACOM10/e0:72:a1:f5:bc:b0, BCOM9/e0:72:a1:f6:a9:68. Before deployment: boot28/fresh authenticated OFF/OFF/outputsINHIBITED/channel1/watchdogtrue/0 sampled misses, lastcommand0. Evidence before-deploy.json.

**Phone update now installed:**1.0-dht11-20261027b/code1; installed base pulled SHA25659b6b2423992bc3c1f6cfd2b080da8d6abd0a173b437a94f0db72cd8dee12fac matches new APK. No uninstall/data clear/recording; prior a preserved. Token/manual proof will need private restoration after firmware/wiring stabilizes.

Normal inhibited candidate images remain A2b59eab9.../Be549037e...; app-only guarded B-first/A deployment is authorized and pending completion of baseline/protected-region/full-A-backup preflight. Two original full private B backup copies reverified. No output-enabled/diagnostic/model/NVS/partition write. Do not claim candidate firmware installed until receipts/readback succeed. All source/tests/build evidence in docs/DHT11_AND_LED_WIRING_20261027.md and benchmarks/review/dht11-20261027/.

**PIR remains blocked separately:** bare-board5Vin reading0.67 and working3V3 reported by user; units/exact meter setup/board power path unverified. This does not establish a damaged5Vin pin. Board model/5Vin-area photo requested; do not feed HC-SR501 from3V3 or wire OUT by assumption. DHT3V3 stage proceeds independently. Prior hardware-hold notes below are history superseded for DHT only.

---


# CURRENT — real DHT A→B verified; Android value pending; manual/AUTO source prepared, 27 October 2026 IST

| Completion category | Current observed status |
|---|---|
| Board verified |DHT11 on A3V3/printedGPIO7/GND returns28.9→28.8°C/statusOK;6fresh gateway reports, controller boot31, samples2998→17998ms. Control jobs571→3570/sensor1→7/link19955→22955; sensor/link watchdogtrue/0gateway sampled misses/channel1/AP192.168.4.1. OFF/OFF/inhibited/lastcommand0. No actuator commands/recordings. |
| Installed firmware |Guarded app-only B-first/A deploy and application readbacks PASS: A2b59eab9af0b05367d0e503682998d868ea0df1c9c6e98102b720ce2eb6ed64b; Be549037e0093829bfa02882c0141ca1c7db0eac05cafed1f986334649e29d7b8. A/B system regions and original Bmodel unchanged; exact current pre-DHT full apps board-verified/two copies in benchmarks/backups/dht11-20261027 and Documents/ClawGrid-private-backups/dht11-20261027. No NVS erase/restore/key regeneration. B-new/A-old compatibility was board verified before A update. |
| Phone verified / pending |1.0-dht11-20261027b installed/pulled SHA59b6b2423992bc3c1f6cfd2b080da8d6abd0a173b437a94f0db72cd8dee12fac MATCH. User reapplied private token and replied done. RAM-only UI inspection sees app/fresh report, but Bedroom temperature line wasn't captured while visible; navigation/scroll only/no raw UI/transcript/actuator calls. Asked user for exact temperature line; **do not claim Android temperature display verified yet**. |
| Implemented next source milestone |Manual until explicit AUTO, independent device AUTO enabling and occupancy reevaluation through SafetyKernel; saved manual modes restore heldOFF, no oldON/timer replay. Separate CRC/versioned NVSmanual key preserves existing V1policy format; commits remain5s-coalesced/outsideControlTask (ACK does not prove durability). AWAY explicitly rejects AUTO; HOME explicit global enables both. Payload5 advertises actual controllerpolicy2; Android gates AUTO on capability+HOME/fresh and requires selected mode ACK+fresh report. Details/semantics documented BEFORE changes in docs/MANUAL_AUTO_CONTRACT_20261027.md. |
| Host/build verified next source |76actual-source JVM tests PASS1.196s;58focused tests PASS6.15s =49C/C++ +6LEDflash guards +3privacy. Initial policy48cases PASS7.13s/75JVM PASS1.144s preceded added LED gate checks. A text-edit helper initially failed Windows CP1252 decode before any edits; rerun explicitUTF8 succeeded. New APK deliverables/ClawGrid-Manual-Auto-debug-20261027c.apk/version1.0-manual-auto-20261027c/100112972B/SHA179e7e94dc4a74a36115f27469c0b8a4a0ba2482e850702bc701e4c8e8fc3b1b built and model/ABI/signature/16KB/client-key checks PASS; **not installed**. New normal inhibited firmware builds in progress/completed logs; no policy firmware deploy yet. |
| Awaiting hardware / blocked |PIR disconnected: user reports5Vin~0.67 and3V3 working; units/meter/actual board power path not independently verified, pin failure NOT established. Board model/5Vin-area photo requested; no3V3 substitution for HC-SR501. LEDs/relays disconnected. LED-only future source profile prepared (GPIO4/5activeLOW, separate10k): requires explicit A/UART/bench/LED selector and declared/authenticated profile for voice; normal remains inhibited. No enabled profile built/deployed/physically verified. |

Evidence: benchmarks/review/dht11-20261027/{deployment-complete.json,sensor-summary.json,first-sensor-runtime.json,deployment/,phone-*.json}; guarded manifests flash_native_voice_uart_20261027T133017Z.json and flash_speech_intelligence_uart_20261027T132818Z.json. Source/rollback for next policy milestone: benchmarks/review/manual-auto-20261027/before/ and baseline/ (working DHT2b/E5 bin+ELF). **Physical sensor values are not calibration proof, LED/appliance switching, PIR motion or watchdog recovery verification.**

Next: capture the exact Android Bedroom temperature line; finish/hash/guard the new normal inhibited policy candidates, preserve operational DHT rollback and deploy/test per-device modes without generated occupancy or physical output enablement. Then actual PIR supply identification and one-at-a-time LED wiring/guarded LED-only stage, independent manual/AUTO/motion/recovery and bounded unchanged on-phone recognition checks. No model migration, thresholds, transcript logging or cloud dependency. All old observations below are history.

---

