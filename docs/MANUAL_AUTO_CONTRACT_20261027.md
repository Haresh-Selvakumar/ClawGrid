# Manual/AUTO contract — before persistent policy changes

The currently board-verified DHT baseline remains A2b59eab9.../Be549037e.../phone1.0-dht11-20261027b. Keep it as rollback. This bounded source milestone addresses the user's existing requirement: manual ON/OFF overrides motion until the selected device returns to AUTO. No voice/model, wiring or output enablement is part of this policy change.

## Existing mismatches inspected

SafetyKernel automatically releases a manual hold after5minutes. Per-device Resume only releases the hold; it does not enable disabled automatic policy (fan defaults disabled). Returning AUTO during already-established occupancy can wait for another motion edge. HOME clears both holds without necessarily enabling both automatic policies. AWAY globally suppresses motion. Manual holds are RAM-only and disappear on reboot. Timers call manual OFF. These are firmware behaviors, not app simulations.

## Selected semantics

- Accepted manual ON/OFF, including state no-ops, latches the selected device's manual mode until its explicit AUTO. No five-minute expiry. Other device mode/state untouched. Rejected dwell requests do not change modes.
- Per-device AUTO explicitly enables that device's automatic policy and releases its hold. Established OCCUPIED/LIKELY_OCCUPIED may queue the corresponding motion request; UNKNOWN/VACANT cannot produce an ON. Every resulting transition still goes through SafetyKernel/dwell/health. ACK/readback confirms mode; actual reported output is displayed separately and may change after the policy ACK.
- AWAY remains a global motion inhibitor. AUTO in AWAY is explicitly rejected; it does not silently leave AWAY or change other devices. HOME is an explicit global action enabling both automatic policies/releasing both holds. Hardware scene UI remains outside this slice; no HOME/AWAY scene demonstration is claimed.
- Manual mode flags persist separately from output state. Boot always registers outputs OFF; a saved manual mode restores **held OFF**, never an old ON or timer. Saved AUTO devices need real post-startup PIR evidence. No old command/proposal/audio job is replayed.
- Keep the existing version1 policy blob and its keys/layout. Add a separate CRC-protected/versioned `manual` NVS blob. Missing key means legacy manual flags0; invalid data fails closed without erasure/regeneration. Older firmware ignores the extra key, so rollback still decodes its existing policy. Preserve credentials/radio replay history/models/partitions.
- Existing asynchronous5s-coalesced NVS commits remain outside ControlTask. ACK+readback confirms live mode/state, not durability: a power loss before successful commit can retain the previous saved mode. Verify saved flags before the reboot demonstration; report commit errors honestly. No commanded ON state is persisted in either case.
- Four-hour max-run cutoff/three-second dwell remain. A safety cutoff turns a manual-ON device into held OFF; it does not silently release its manual hold. Temperature remains monitoring only.

## Capability/readback boundary

Controller payload schema5 will explicitly advertise control-policy version2. New gateway remains compatible with payloads3/4, advertising legacy policy1 for those. Android exposes AUTO only when the actual controller reports version2 and HOME/fresh state. Older gateway/controller combinations stay manual-only or reject; they must not falsely claim new hold semantics. Temperature API contract4 is unchanged.

Every request uses immutable selected device/action, proposal→explicit confirmation→matching ACK→newer same-boot authoritative report. AUTO readback requires the selected automatic bit set and manual bit clear; light flags cannot satisfy fan. HTTP acceptance is never physical success. Firmware/app builds, host checks, board/manual/AUTO/reboot results will be recorded separately; nothing in this contract is physical verification.

## Prepared LED-only output profile (no enablement yet)

The original phone voice gate allowed inhibited outputs only. For the user's later LED demonstration, prepare a separate declared LED_INDICATORS_10K_ACTIVE_LOW profile:3.3V sinking LEDs on4/5 with separate10k series resistors, no relays/appliances. It needs a separate A/UART output-enabled environment and explicit `--bench --led-indicators` selector; normal builds remain inhibited. A's authenticated schema5 report carries the declaration; new gateway relays it. Android can allow voice only for inhibited mode or that exact policy2 LED profile, still requiring same-boot manual-light ACK+fresh-report proof and explicit speech confirmation. Generic/unknown enabled profiles remain voice-locked. Recognition model/parser/thresholds unchanged. This is a declared build/wiring contract, **not electronic detection of the connected loads**. Inspect actual wiring/polarity before ever deploying that profile; never enable it merely to unlock voice. No LED profile is currently installed or physically verified.

## Deployed inhibited results

# CURRENT — three manual actions PASS; physical LED wiring gate, 27 October 2026 IST

User now prioritizes actual GPIO4/light and GPIO5/fan LED demonstration with two separate10kΩ resistors; keep relays unused/DHT working. PIR/external9V battery remain disconnected until exact supply/module compatibility is verified. **Do not infer5Vin damage from earlier readings.** N16R8 is memory configuration, not PCB power-path identification.

| Checklist | Actual status |
|---|---|
| Implemented |Real DHT11 GPIO7/task/checksum/validity/telemetry; hardware temperature tile/diagnostics placeholder fixed. Manual-untilAUTO/per-device AUTO/persistence with safeOFF boot, schema5 controller capability, simulation hold isolation. Separate guarded LED-only environment/selector/profile prepared; no recognition-model/parser/.7 threshold change. |
| Host/build verified |Latest77actual-source JVM cases PASS1.173s;59focused host cases PASS8.40s (50C/C++,6LED guards,3privacy). Manual/AUTO firmware A final inhibited SUCCESS22.757s/B29.101s; built/rebuilt only for changed sources. APKd assembled and artifact/signature/offline-model/alignment/private-key checks PASS. LED-enabled profile build is currently in progress; NOT deployed. |
| Installed firmware / phone |A inhibited75d668711108bdbd5db5ecd03060eb85bf9fd9883420f77a90405d8804105b72,689440B; B30f42429c18876819a02d7c3ac0e74f682695e25402755d6772d2c67f189d828,1215776B. Guarded app-only B-first/A deploy+readback and protected system/model checks PASS. Installed phone1.0-local-demo-20261027d/code1/SHAfb1a8d138529476ab8895807d0a74ad5d3ce62bc9caaa0cad82cfb03ee299545,100112968B, pulled-base hashMATCH. c was built but never installed; prior b/a rollback preserved. |
| Board verified / manual results |Boot34 policy2/until_auto and DHT~29.2°C/statusOK observed. User completed lightOFF#1, fanON#2, fanOFF#3: all appliedACK phase2/status0 plus fresh sameboot report/lastcommand matches. Exact samples274996/290996/441996ms and ages746/496/246ms. OFF/OFF final, outputsINHIBITED. No physical switching claim. Stored manual flags3 confirmed by read-only P diagnostic after successful NVS commit; control/link/sensor jobs advance/watchdogs active. |
| DHT phone display |User explicitly reports29°C after APKd fixed the hardcoded Hardware Unavailable tile and stale diagnostics. Board/gateway sensor values independently observed. RAM-only UI inspection did not independently extract the displayed digits; label this USER-OBSERVED Android display, not an automated pixel/value match. Temperature monitoring only, no calibration claim. |
| Awaiting physical LED connection |Current requested step: both USBs OFF, preserve DHT wires, locate printed4/5 (not header index) on physical A, identify LED anode/cathode; await acknowledgment. Circuit will be activeLOW:3V3→own10k→anode; cathode→GPIO4 or5. Each separately staged and inspected before power/output enablement. Low brightness expected/accepted. |
| Blocked / remaining |Actual LED wiring/polarity/ONOFF observations pending; outputs must remain inhibited until checked. PIR model/input rating and board5Vin path unresolved; battery only identified9V6F22, not a regulated5V source; no battery wiring authorized. AUTO motion/vacancy/manual override and reboot/stale/recovery/brief existing phone-voice acceptance remain pending. Cloud/extra rooms/model investigation deferred. |

Evidence: benchmarks/review/manual-auto-20261027/{manual-three-actions-verdict.json,after-manual-batch.json,manual-mode-saved-before-auto.json,first-policy-runtime.json,deployment/}; temperature UI fix evidence benchmarks/review/temperature-kpi-fix-20261027/installed-receipt.json. Working policy bin/ELF preserved separately in benchmarks/review/physical-led-20261027/inhibited-baseline before LED build. Full working prior DHT app recovery copies in benchmarks/backups/manual-auto-20261027 and Documents/ClawGrid-private-backups/manual-auto-20261027 were both verified against complete board flash before policy writes (host reconstruction is now board verified). No NVS erase/restore/key regeneration/model/partition write. Preserve all credentials/baselines/failed evidence.

**Next:** wait for power-off/printedGPIO4+5/LED-polarity acknowledgment; guide light circuit first, then fan, verify actual wiring/photo. Build/hash/guard LED profile without deployment until gate met; then freshly MAC-verify/check OFF/saved holds, app-only guarded A --bench --led-indicators, verify explicit LED declaration/safeOFF/DHT/link health. Obtain human LED observations together with captured ACK+fresh reports, endOFF/OFF. Then solve PIR supply from actual hardware, not assumptions, before motionAUTO. Previous checkpoints below are history.

---


## Physical LED milestone started —27October2026

All three captured app actions independently reviewed PASS on boot34: lightOFF#1,fanON#2,fanOFF#3 each phase2/status0 ACK plus fresh sameboot board report/lastcommand/output matches; see manual-three-actions-verdict.json. FinalOFF/OFF/inhibited; these do not establish LED operation.

User confirmed both USBs unplugged, controllerA printedGPIO4/5 identified (not header indices), LED anode/cathode identified; DHT wires unchanged/PIR,battery,relays disconnected. Current **pending single wiring step**:3V3→one10k resistor→lightLEDanode; lightLEDcathode→printedGPIO4. FanLED remains disconnected; power remainsOFF. Await acknowledgment then guide fan separately and inspect photo/wiring before enablement.

LED profile build attempt1 FAILED53.567s at native_hal compile-time UART guard (new _led_bench environment absent from CMake's explicit UART defaults regex), not a flash failure. Updated native/CMakeLists.txt regex to include only intended _led_bench; saved/moved failed generated SDK configs to benchmarks/review/physical-led-20261027/*.failed-console, no deletion of baselines. Six selector checks PASS. Rebuild build-led-profile-uart.log in progress; do not claim candidate built/installed until success and guards. Current inhibited A/B bin+ELF saved independently under physical-led-20261027/inhibited-baseline because PlatformIO can purge sibling environment caches.

No output-enabled flash, diagnostic-fault image, actuator command, model/NVS/partition/credential change in this physical-preparation step. Exact ESPPCB and PIRinput rating remain unresolved; no9Vbattery connection authorized and no5Vin damage diagnosis.

**LED candidate build complete, not deployed:** native_voice_uart_led_bench SUCCESS120.259s after explicit CMake UART-default correction;689456B/SHA256742c371bc3904e60eb300deee5ce7d976615405fdb8aa5f290339cff80a28275; linkedRAM69396B (build size, not runtime measurement). Enabled/default-fault/board-pair/UART0/explicitLEDselector guards PASS. Candidate bin/ELF sealed separately in physical-led-20261027/candidate; candidate-identity.json deployed=false. No COM board currently visible (both unplugged as requested), no flash write/serial handle. Await user's light-only activeLOW circuit acknowledgment; then fan circuit/photo inspection before power/deploy. Installed inhibited policy A75d668.../B30f424... unchanged.

# CURRENT — LED-only outputs ENABLED on A, safe-boot human check pending;27October2026

**Output inhibition status changed by explicit user-authorized LED stage:** controllerA now runs guarded native_voice_uart_led_bench SHA256742c371bc3904e60eb300deee5ce7d976615405fdb8aa5f290339cff80a28275 (689456B), physical_outputs_enabled=true, profileLED_INDICATORS_10K_ACTIVE_LOW, activeLOWGPIO4/5/defaultfaults. Do not call current outputs inhibited. Only LEDs/separate10k resistors are user-confirmed connected; relays/PIR/battery unused/disconnected; DHT3V3/GPIO7/GND preserved. Independent wiring photo/electrical inspection was declined and remains unverified.

Fresh MACs ACOM10/e0:72:a1:f5:bc:b0 and BCOM9/e0:72:a1:f6:a9:68 matched before hardware actions. Before enabling: boot36/OFF/OFF/inhibited/savedmanual3/DHT29.0°C/statusOK; user bothLEDoff on inhibited image. Full current inhibitedAapp0 read/board-verified and preserved twice in benchmarks/backups/physical-led-20261027 and Documents/ClawGrid-private-backups/physical-led-20261027, systemverified. Existing guarded script with explicit --role A --uart-console --app-only --bench --led-indicators wrote app0 only; afterward applicationSHA/system readbacksPASS. No B write/model/NVS/table/key changes. B remains30f42429c18876819a02d7c3ac0e74f682695e25402755d6772d2c67f189d828; phone remains installed1.0-local-demo-20261027d/SHAfb1a8d13... . Rollback inhibited artifact75d668... remains independently saved.

Postdeployment board observations boot39/samples3998→6998→9998ms/controljobs788→1389→1987/sensorjobs1→2→4; sensor/gatewaywatchdogtrue, gateway0sampledmisses, DHT29.0°C/statusOK. **Logical lightOFF/fanOFF/manualflags3/lastcommand0**; no ON/actuator command sent since deployment. These are board-state observations, not physical LED proof. Current user question queued: confirm BOTH external LEDsOFF before any ON test. Stop if unexpectedly lit; no further command until answer. All owned serial handles closed after finite checks.

Evidence benchmarks/review/physical-led-20261027/{board-identities.json,before-enable-runtime.json,deployment/preflight.json,deployment/deployed.json,after-enable-runtime.json,enabled-profile-summary.json}. Prior three inhibited manual actions allPASS#1OFF/#2fanON/#3fanOFF, no physical claim. PIR exactinput/boardpower unresolved;9V6F22 onlyidentified, battery notconnected/authorized; no5Vin damage assumption. Next safe-boot humanLEDcheck, then one lightON→humanvisibility+ACK/freshreadback→OFF, then fan similarly, respecting3sdwell and no automaticreplay.

---

