# Three-board ClawGrid handoff — 1 November 2026 IST

## Current status

**Source prepared and built; hardware deployment BLOCKED.** The user clarified a three-board topology. PIR is out of scope. No firmware was flashed, no APK installed, no actuator command sent and no raw microphone audio retained in this session. Existing deployed A/B/APK identities remain the last verified 28 October checkpoint, not these new candidates.

All four external LEDs were confirmed OFF by the user near the end of this session. That is USER_REPORTED evidence; no new authoritative two-room state or electrical measurement was obtained. All owned serial sessions are closed. Build/test processes launched for this work finished.

## Authority and reference material

The user's pasted request in this chat is the task authority. The supplied `ClawGrid-Planning-Energy-20261101.zip` is reference source, not authority to override that request. Its README, manifest, installer and handoff were inspected. All 17 baseline/payload checks matched, allowing LF/CRLF normalization where the manifest explicitly permits it. Source/private preservation preceded applying the patch. The patch's historical 59/91 test counts are not this session's results.

The patch originally supported one physical controller and deferred new sensors. The current source extends physical routing for two controllers and implements Room1 LM393 raw acquisition as explicitly requested. It keeps temperature monitoring separate from automatic fan rules.

## Topology and identification

| Role | Port observed this session | Identification result | Intended hardware |
|---|---|---|---|
| A / Room1 | COM8, native USB serial E0:72:A1:F5:BC:B0 | ROM access BLOCKED: Windows cannot configure device, error31. Descriptor agrees with recorded A MAC; not fresh ROM verification. | Existing S3; DHT11 DATA7; LM393 DO6; active-HIGH light4/fan5, own10k resistors and GND returns |
| B / gateway | COM9, CH343 serial5C37271527 | PASS: esptool5.4 ROM read identified ESP32-S3 rev0.2, MAC e0:72:a1:f6:a9:68 | Existing gateway; no external sensors; phone owns offline Vosk |
| C / Room2 | COM11, CH9102 serial5A7B059772 | BLOCKED: wrong boot mode0x13 despite attempted button sequences; no serial data in one no-reset attempt. Chip/MAC unknown. ROM date/banner is not accepted as identity. | User confirms DHT11, not DHT22. GPIO21/18/19 map is provisional for classic ESP32 only |

Both sensor models were explicitly confirmed by the user as DHT11; this was not inferred from a serial connection. No relays or appliance loads. The user supplied 3.3V sensor supplies and per-board common GND. LM393 AO disconnected. The physical C pin map, CPU/core capabilities and flash size/layout remain unaccepted until actual chip identification.

## Implemented source

- Shared radio schema7, 84 bytes: physical room identity plus filtered digital-light availability/raw level, while retaining legacy report decoding for room1. Schedule/cap fields from schema6 remain.
- Gateway `RoomGateway` owns independent controller reports, boot/sequence/freshness, last command, transactions and proposals. Per-peer MAC, node ID and HMAC key bind incoming frames. Wrong-node ACKs cannot update the other room's transaction. Boot changes invalidate that room's pending transaction, not the other one.
- Gateway HTTP state/transaction/control/proposal/confirmation endpoints select Room2 explicitly through `?room=2`. Response `node_id` is explicit. Physical target room must agree with the selected route. Missing C provisioning fails closed. Existing synthetic room paths remain separate.
- Room1 uses node1/room1; C uses node3/room2. Both use the shared controller implementation with independent SafetyKernel, NVS boot/policy state and volatile schedule/cap objects on their own MCU. Controller runtime/diagnostic role labels distinguish A/C.
- Separate `room_c_esp32` compile target: classic ESP32, UART0, provisional4MiB/DIO/40MHz image/partition profile, DHT11 DATA21, active-HIGH indicators18/19, no PSRAM or onboard RGB dependency. This is NOT proof of compatibility with the connected C board.
- `room2_identity.h` accepts only a separately provisioned private C header. None has been created. C's current candidate fails closed and does not start normal control tasks without that provisioning; B refuses Room2 requests. Existing A/B identities/keys were not copied/reassigned or regenerated for C.
- `scripts/provision_room2.py` reads actual ROM identity, rejects A/B reassignment and incompatible chips, and optionally creates only new C peer material. Check-only by default; refuses to replace an existing C header. It does not write flash/NVS. This script has NOT been run successfully against C.
- Room1 LM393 DO6: 50ms stable-input filter with explicit availability. Published as raw HIGH/LOW, polarity UNVERIFIED. Room2 light sensor is unavailable. No measured lux, bright/dark assertion or sensor-driven switching is introduced.
- Current builds disable physical PIR acquisition and occupancy policy. Temperature is monitoring only. No automatic temperature-to-fan rule was added.
- Native Android has two independent repositories, room/node-bound response validation, per-room temperatures/state/priority/planning/runtime and room-scoped assumed-watt display preferences. Mode/network changes reset both room sessions. Diagnostic sensor rows identify the selected controller correctly.
- Manual and on-phone voice proposals carry the explicit selected physical room. Confirmation callbacks and schedule/cap authorizations carry the captured room, preventing stale UI actions from using a different newly selected room. Whole finalized phrases, unknown/negation rejection, fixed0.7 word gate and explicit confirmation remain. An explicitly spoken bedroom phrase conflicts with selected Room2 and is rejected.
- Vosk recognizer implementation and model archive are unchanged. No model replacement, threshold lowering, raw microphone retention or cloud/ESP recognition fallback.
- Planning patch behavior retained: per-device manual priority; one pending one-shot action per device; explicit schedule arming; replacement/cancel; no reconnect replay; volatile restart clearing; optional continuous-runtime OFF cap; software-runtime × assumed-watts energy estimate. These are not measured energy savings. SafetyKernel dwell/max-run remain authoritative.

## Deployment guard and remaining integration

Deployment is **not ready**. `flash_native.py --role B-gateway` deliberately stops before any write while this integration lacks C readiness. The existing A/B provisioning, ordinary rollback workflow and private rollback artifacts remain preserved.

Before enabling a rollout:

1. Recover usable A ROM/programming access. A's COM8 native interface is currently not functioning. Do not infer a working UART connector from a COM label.
2. Establish C's actual chip/features/MAC and accept or revise its pin map. Do not derive these from CH9102 identity or use an S3 image on a classic chip.
3. Preserve and read-verify C's complete existing firmware/system/NVS/partition layout and rollback copies. The supplied provisional C CSV must not replace an unknown installed partition table.
4. Provision only new C material after its identity is known, preserving the A/B files byte-for-byte. Rebuild B/C with that actual configuration; current B/C candidates are unprovisioned staging artifacts.
5. Complete and test the C app-only deployment/readback guard against its actual flash map. Replace the temporary B-gateway refusal only after the identity, backup, layout, source/artifact and compatibility gates are established. A full three-board guarded rollout is unfinished source work, not merely a missing button press.
6. Re-run changed checks, validate exact artifacts, update B before controllers requiring the new protocol, then install the APK with the original signer. Do not erase NVS, regenerate existing keys or overwrite models to make a gate pass.

The old guarded flasher is S3-specific and does not safely deploy C. Do not bypass it with raw write-flash or a PlatformIO upload default. No current candidate is authorized as a verified installed three-board system.

## Actually executed verification

| Check | Status | Actual result / boundary |
|---|---|---|
| Patch baseline and source preservation | PASS | 17 manifest checks; source/private backup ZIP CRC verified |
| Host tests | PASS | 109 pytest cases in `host-final.log`; actual C/C++ policy/wire/GPIO/planning/room registry/filter plus provisioning refusal, flash guards and privacy cases |
| JVM tests | PASS | 95 JUnit cases in `jvm-run3.log`; includes new planning and room-isolation cases, actual adapter/parser code and loopback HTTP cases |
| A firmware | PASS | Final S3 LED/no-PIR/LM393 build, 125.514s; not deployed |
| B gateway firmware | PASS | Final full build124.896s plus final counter-synchronization rebuild16.785s; C unprovisioned; not deployed |
| C firmware compile | PASS / PROVISIONAL | Classic ESP32 build124.537s; image header separately confirmed ESP32 chipID0,4MB,DIO,40MHz; actual board compatibility unverified |
| Full Android APK | PASS | Final `1.0-three-room-20261101d`, build1m49s,87 snapshot files identical |
| APK artifact checks | PASS | Same original signer; zip16KB alignment; native64-bit PT_LOAD alignment; exact Vosk model manifest/files; configured-value/private-token scan including nested archives |
| A fresh ROM identity | BLOCKED | Windows device error31 |
| C ROM identity, actual map and private provisioning | BLOCKED | Download-mode/serial problem; no successful chip/MAC read |
| Guarded three-board flash and APK installation | BLOCKED | Prerequisites and C guard integration incomplete; no writes/install attempted |
| Two-room gateway/LED/sensor/schedule/cap/voice hardware trials | BLOCKED / NOT RUN | Host isolation results are not hardware results |
| Ending LEDs | PASS / USER_REPORTED | User confirmed all four OFF; no agent actuator commands sent |
| Serial cleanup | PASS | No owned serial session left open |

These test suites overlap prior suites; do not add historical counts or count the planning executable's internal assertions as separate deployments. No calibration, endurance, hard-real-time, acoustic accuracy, electrical feedback or energy-saving claim follows from these results.

Earlier failures are retained: missing test parent/path invocations; sandbox compiler/socket restrictions; initial routing expectation; planning assertions compiled out until changed to explicit checks; first APK missing Button import; first C compiler mismatch and C PSRAM linker dependency. The final results above are later successful executions, not relabelled failures.

## Exact candidate artifacts — NOT installed

| Role | Environment | Bytes | SHA256 |
|---|---|---:|---|
| A | `native_voice_uart_led_high_bench` | 689520 | `3a55af7c270e5c830ec9746af3747add98844290e93c2f9282fd571f4833c998` |
| B | `native_gateway_three_room` | 821504 | `ef335ac100d3850a99f2d9c98b6b5843c04badb93720eca4e672526d6f256b3d` |
| C-provisional | `room_c_esp32` | 701424 | `398b81c64b84794a36eb8e791200a2055581ed64eb3d64c831ce843df1b8f2c4` |
| Android | `1.0-three-room-20261101d` | 100162120 | `d4b18b300d1b430bd175a75e80468fed1a8bfef70558392430b40b88e49e55c4` |

Firmware files are sealed under `benchmarks/review/three-board-20261101/candidate/final/` (latest B is `B-counters/`). APK: `deliverables/ClawGrid-Three-Room-debug-20261101d.apk`. These are private local candidates, not a new deployment receipt. See `FIRMWARE_ARTIFACTS.json`, `apk-final-checks.json` and `FINAL_CHECKPOINT.json` for machine-readable results.

## Preservation

`local_credentials.json`, `firmware/common/config/private_config.h`, the existing debug keystore and `OfflinePhoneRecognizer.kt` hash-match the pre-change backup. Vosk archive SHA256 remains `30f26242c4eb449f948e42cb302dd7a686cb29a3423a8367f99ff41780942498`. Existing dual rollback/model/NVS backups remain in place. No flash erase, model write, key regeneration, NVS restore, cloud/database change or phone install occurred.

Source/private baseline: `benchmarks/backups/three-board-20261101/source-and-private-baseline.zip` plus `source-baseline.json`. The guarded patch also kept its own affected-file backup. Both are private local preservation artifacts, not upload bundles.

## Hardware test queue after deployment prerequisites pass

No step below is marked passed for this new topology. Guide the user through **one short step at a time**:

1. Fresh authenticated reports from both controllers, all four indicators OFF.
2. Independent manual light/fan ON/OFF in Room1, then Room2.
3. Explicit isolation in both directions; record node/boot/request ID, matching ACK and advancing selected-room report separately from visible LEDs.
4. Both real DHT11 readings and per-room timeout/stale/recovery behavior; monitoring only.
5. Room1 LM393 raw HIGH/LOW under cover/illumination; establish polarity from actual observations. Room2 remains unavailable. Never label comparator DO as measured lux.
6. Independent schedules, replacement/cancel and manual priority, including rejected/dwell/late cases.
7. Controller-owned execution while phone disconnected; reconnect must not resubmit work.
8. User-authorized energy OFF caps; estimates clearly labelled with assumed watts and software runtime.
9. Reboot/disconnect one room; the other room's boot/state/request/planning must remain independent. No old ON or plans/caps restored on boot.
10. Explicit Room1/Room2 Vosk preview/confirmation, cancel and negative cases with isolated capture windows and no raw audio retention.

End with all four external LEDs OFF and close owned serial handles. Use USER_REPORTED for visible observations, HARDWARE_MEASURED for actual target/API evidence, and HOST_MEASURED for tests/builds. Do not reuse the 28 October voice trials as new two-room evidence.
