# Latest scope — ROOM1 ONLY; sensor-evaluation order update in progress — 2 November 2026 IST

Read `docs/ROOM1_SENSOR_ORDER_20261102.md` and `benchmarks/review/room1-sensor-order-20261102/CHECKPOINT.json` first. Room2 is outside final physical acceptance: unavailable/unverified UI only. Stop its stability/deployment/acceptance work; preserve its source/evidence/provisioning. It is not an active blocker for the Room1-only demonstration.

New independent setting: Light sensor first / Temperature sensor first, distinct from per-device Schedules first / Sensors first source priority. Both rules service one coherent latest-input snapshot every bounded cycle, with actual ordinals/start/elapsed/result evidence. Host92/JVM114pass; A/B firmware and APK-e built. A/B-only guarded rollout PASS, with application readback and protected full-flash preservation. APK-e installed/hashmatched. Fresh A boot74 command0 reports both Room1 outputs OFF, MANUAL flags3/AUTO0, fresh DHT11/LM393 and actual Light step1/Fan step2 timing. No owned serial session. Short phone acceptance is pending the requested user ready reply; no AUTO or order-change acceptance is claimed yet. Inspect running processes/checkpoint before taking serial ownership. Preserve earlier passed Room1 evidence and original model/signer/credentials. No C build, deployment or live test.

---

# Latest — app thresholds and Room1 sensor AUTO verified; C remains BLOCKED — 2 November 2026 IST

Read `docs/SENSOR_AUTO_THRESHOLDS_20261102.md`, `benchmarks/review/sensor-auto-20261102/CHECKPOINT.json` and `FINAL_RESULTS.json` first. These supersede older current-version/boot and monitoring-only scope below. B then A guarded app-only update/readback/full-flash preservation PASS; APK1.0-sensor-auto-20261102d installed/hashmatched. C newtarget built/guardchecked but NOT deployed; its earlier watchdog cause remains unresolved.

Room1 observedPASS: editable real-temperature fan thresholds27/26→ON and30/29→OFF; user lightAUTO darkHIGH/brightLOW; clean manualOFF cancels pendingON before fallback and holdsOFF under fresh sensorON demand; fanSchedulesfirst15slease→sensorOFF; Sensorsfirstoverride/noON; fresh60scountdown earlier than120scap; corrected60scap earlier than120scountdown. Separate userLEDconfirmations retained. Initial interrupted setup and missing-response cap capture remainNOTPASS/INCOMPLETE and are superseded only by their successful isolated/corrected captures. No C ON host tests. User-intended C lightON1/OFF2 in earlier mixed phonewindow is retained without misclassification.

Final confirmedstate: Aboot73/cmd78, Cboot5/cmd2, allfourOFF; A MANUAL flags3/AUTOflags0; noarmed schedules/countdowns/caps/leases. Fan preferred28°C ON/26°C OFF, Schedulesfirst, override60s; do not silently enableAUTO. Original phonekeepawake0; allownedserialclosed and noobserverworker. CurrentAPK SHA256bf91cf4445d3420d4f63d89aa9d0ef09a7afd1a6b808b7094853b04f0057107e. Actualfirmwarehashes and all evidence paths in linkedhandoff/ARTIFACTS.json.

Cstability/liveAUTO/voice BLOCKED. Remainingnegative speech NOTRUN; newboot73/currentAPK microphone acceptance not rerun (historical Aboot72voicePASS retained). Light-specific priority/timing, hardware reboot of edited/persistedconfiguration, inducedsensorfaultAUTO and full late/dwell/reconnect/endurance remainunproven beyond documentedhost/prior evidence. Project remainsINCOMPLETE; do not call staged C image a watchdogfix or reflash unchanged artifacts merely to resume.

---

# Latest resume — voice gate resolved; Room 1 voice observed; C stability BLOCKED — 2 November 2026 IST

Read `docs/VOICE_STABILITY_RESUME_20261102.md` and `benchmarks/review/three-board-acceptance-20261102/CHECKPOINT.json` first. This entry supersedes older current-version, voice NOT_RUN, unfinished C capture and serial-ownership claims below. Historical sections are retained.

Room 1 voice gate required current-app-session manual proof, established through normal OFF #20/light and #21/fan on Aboot72. Hold to talk became ready with no source change. Real spoken light ON22/OFF23 and fan ON24/OFF25 have matching ACK/fresh readback; user reports working/only intended LED in response to targeted instructions and explicitly confirms both OFF commands were spoken. Cboot5/command0 stayedOFF. Isolated cancellation has36 samples with unchanged counters/allOFF plus user report. No broad speech accuracy claim or retained audio. Negation/unsupported/ambiguous/unsupported-room negatives remainNOT_RUN: user says no more tests; do not request more trials without new direction.

C-watchdog-followup completed84 records over~178.077seconds and closed serial. Reset7 is ESP-IDF4.4.7 ESP_RST_WDT/other watchdogs, not proof of a particular task. No contemporaneous C panic/backtrace or core dump retained; core dumps disabled. Quiet windows/current advancing counters do not resolve cause. No justified repair/reflash; C ON acceptance and Room2 voice remainBLOCKED. See resume-C-stability-assessment.json.

Fresh installed APK-c SHA256125eccf8d4cf5bc988e3476cf5e74846bc870d20a0c6b6e09d9ac891c813ba99 matches; keep-awake original0 restored/verified. All owned serial closed/no observer remains. Final retained12:22:56IST: Aboot72/cmd25, Cboot5/cmd0, alloutputsOFF, noarmed schedules/countdowns. A_lightcap60000ms persists; A_fan/Ccaps0. Sensor/command/schedule campaigns not restarted. Project remainsINCOMPLETE. Exact HOST_MEASURED/HARDWARE_MEASURED/USER_REPORTED evidence is in resume-voice-results.json, resume-voice-gate-assessment.json, resume-C-stability-assessment.json and resume-phone-runtime.json.

---

# CURRENT — three boards deployed; sensor acceptance in progress — 2 November 2026 IST

Read `docs/THREE_BOARD_HANDOFF_20261102.md` and `benchmarks/review/three-board-20261102/ACCEPTANCE_CHECKPOINT.json`. Older source-only/BLOCKED entries below are historical. A=CH343 COM10/S3/e0:72:a1:f5:bc:b0; B=CH343 COM9/S3/e0:72:a1:f6:a9:68; C=CH9102 COM11/classic ESP32-D0WD-V3/80:f3:da:63:58:fc/4MB. C preserves its actual legacy partition table and app offset0x100000. All backups and original A/B credentials/signer/Vosk preserved.

Current A image e7ffd52a0dd351a40d45070a592b0096f9e15105aee55419dc9f88f125f7cd06; B d82e8fa66e72a35e0e6a56e86ab88cbd209b0d1e89ab7c54af2eba1ae25f0273; C capture-priority 1bbcd5b971233ae02bdd9a65635ab9d0f029a49b8d48c97333aa875cfb611e57. Guarded app-only readback/full-flash preservation passed. Current APK1.0-three-room-20261102a SHA2561bad8be832e2f35bb091f02afa494c4731853f526de0ff38b4bed51bb68fcdb9 installed/hash matched, signer/alignment/model/privacy checks PASS. It fixes Room2's hardcoded disabled controls and per-card repository binding; user confirms controls work.

Room1 manual light/fan software/user evidence passed, Room2 stayed unchanged. Fan OFF#4 dwell-rejected; finalOFF#5 applied. Broad Room2 controls-working report is not isolated per-LED acceptance. A boot72, latest C boot4; both rooms fresh/authenticated/OFF at last read.

User sensor priority COMPLETE for observed windows: C DHT11 capture-priority update yielded35/35 valid physical frames over90seconds and41/41 fresh authenticated gateway statusOK samples. C boot4; phone user-confirmed working. Decoder unchanged. Earlier intermittent failures retained; no calibration/long-endurance claim. See SENSOR_FINAL_CHECKPOINT.json.

Room1 LM393 GPIO6 polarity established by user conditions plus fresh hardware evidence: covered/darkHIGH (4/4), torch/brightLOW (4/4). Potentiometer unchanged, no lux measurement or daylight automation; firmware metadata remainsUNVERIFIED until explicit configuration. Evidence LM393-polarity-verdict.json.

All four LEDs user-confirmedOFF, both rooms fresh OFF throughout final92second API capture. All owned serial sessions CLOSED. Remaining original acceptance: isolated Room2 manual/isolation capture, schedules/replacement/cancel/manual priority/phone-disconnected execution, OFF caps/labelled estimates, room-targeted Vosk confirm/cancel, full disconnection/stale/recovery coverage. No need to rebuild/reflash unchanged images merely to resume.

---

# CURRENT — three-board source built; hardware deployment BLOCKED — 1 November 2026 IST

Read `docs/THREE_BOARD_HANDOFF_20261101.md` and `benchmarks/review/three-board-20261101/FINAL_CHECKPOINT.json` before acting. This source milestone supersedes earlier one-controller preparation notes; it does not supersede the last verified installed 28 October artifacts.

User topology: B gateway (no sensors); A Room1 DHT11 DATA7, LM393 DO6/raw filtered input, active-HIGH light4/fan5; C Room2 DHT11 confirmed by user, proposed classic GPIO21/18/19 still provisional until actual chip/MAC identification. PIR/occupancy automation completely out of scope; temperature monitoring only.

Implemented source: independent room/node-bound gateway reports/transactions/proposals, per-peer C auth hooks, separate provisional classic target/no S3 PSRAM dependency, Room1 digital light availability, independent Android room state/planning/estimates and explicit room-bound voice/manual confirmations. Existing Vosk source/model/0.7 gate, signer and A/B credentials preserved.

Actually executed:109 host tests PASS;95 JVM tests PASS; A/B/provisional-classic-C full firmware builds PASS; APK1.0-three-room-20261101d build/signature/model/alignment/private-value checks PASS. No firmware flash, APK install, model/NVS write, actuator command or raw microphone retention. Exact hashes are in the linked handoff/checkpoint.

BLOCKED: A COM8 cannot be configured (Windows device error31); B COM9 fresh ROM identity confirmed S3/e0:72:a1:f6:a9:68; C COM11/CH9102 ROM identity failed (wrong boot0x13/no-data), so chip/MAC/layout unknown. C private provisioning is absent. Current B/C staging candidates fail closed for Room2. Finish C preservation/layout and app-only guard; the B-gateway flasher currently deliberately refuses rollout. Do not bypass this or flash the provisional C partition table/image.

End state: user confirmed all four LEDs OFF; no owned serial sessions. All10 requested new guided hardware test groups remain NOT RUN/BLOCKED. Next prerequisite is resolving USB/ROM identification (actual C module/button inspection, usable A programming interface), followed by C backup/provisioning and gated gateway-first rollout. No automatic retry, ON replay or rebuild of unchanged artifacts merely to resume.

---

# Source-only planning/energy extension — 1 November 2026 IST

Software changes and host validation are documented in `docs/PLANNING_ENERGY_HANDOFF_20261101.md`. No boards/phone were accessed; no target binaries were built or deployed here. Installed-device status remains the historical 28 October checkpoint below. Updated B is required before updated A because of radio schema6. Source-only upload omits existing Android UI components/model/build dependencies: merge the guarded patch into the complete local tree. Sensor additions remain deferred.

---

# Final saved checkpoint — 28 October 2026 IST

The declared LED demonstration has been implemented and observed. PIR and physical occupancy AUTO are excluded. Controller A uses active-HIGH light GPIO4 and fan GPIO5, each through its own series resistor to the LED anode, with cathode to GND. DHT11 DATA is GPIO7. Relays remain unused. The existing offline Vosk model is preserved.

## Installed artifacts

- Controller A: `2fad039a863ba65484e09cf761b8f1d3e4b598fb37a63bf95517922fb0ae9955`
- Gateway B: `60da71c68dce57a5e0f663ff25c53e7b0911008ac20562ec2dc3ce7063b4881c`
- Android: `1.0-voice-led-no-pir-20261028j`, SHA256 `c349a275575dd683b76be69db31ce99fde5900888db7adaae89fec4d276f9c92`

Both boards passed guarded application-only flash, readback and protected-system verification. B's model partition is unchanged. The installed APK was pulled and hash-matched; signing, alignment, model and client-boundary checks passed. Final APK j fixes only a stale reconnect message; its recognizer, parser, ViewModel, push-to-talk and model match voice-tested APK i.

Fresh ROM identities: A e0:72:a1:f5:bc:b0 / CH343 serial 5C37271524; B e0:72:a1:f6:a9:68 / serial 5C37271527. Last verified ports were COM10/COM9; re-enumerate before future hardware access. Credentials, provisioning, NVS formats and rollback copies are preserved. Normal application boot/policy writes still occur.

## Verification and ending state

Final endpoint: boot 62, last command 2, fresh authenticated light OFF / fan OFF, outputs enabled in the exact active-HIGH LED profile, DHT11 24.3 °C / OK. PIR is unavailable, motion is null, AUTO flags are zero. Essential tasks advance and watchdogs are active. Owned serial handles are closed; no test worker remains.

85 Android JVM tests passed, along with 65 focused firmware/GPIO/proposal/privacy cases and 51 final native firmware cases. User-observed manual control and two microphone trials per canonical command correlated with applied ACKs and fresh readback. Voice IDs: light ON/OFF #6/#7 and #11/#12; fan ON/OFF #9/#10 and #13/#14, all on boot 61. Extra OFF #8 remains in the record with unspecified provenance.

DHT timeout and recovery were observed on boards and Android: invalid readings became unavailable while control/link/sensor counters advanced and OFF commands still worked; valid temperature returned after DATA restoration. Restart advanced boot 61 to 62, held both outputs OFF, and replayed no old ON request. Final APK j showed UNKNOWN during Wi-Fi disconnection and fresh BOARD state on recovery without actuator requests. Real transport checks rejected wrong boot/room/nonce/target, duplicate confirmation and expired proposals; an unconfirmed phone proposal did not dispatch.

Four negative phrases and cancellation were user-reported as working. Their capture also contains deliberate extra ON #15 / OFF #16. The user explicitly corrected the mistaken report of unconfirmed dispatch. No confirmation defect was established, but the mixed capture is not isolated automated zero-dispatch proof for every negative. Earlier failed recognition evidence remains intact.

## Resume and documentation

Read `docs/VOICE_LED_DEMONSTRATION_20261028.md` for results, limitations and the short demo, and `docs/EMBEDDED_LED_REVIEW_20261028.md` for task/queue ownership. Machine state is in `benchmarks/review/voice-led-no-pir-20261028/FINAL_CHECKPOINT.json`.

Do not rebuild or reflash unchanged artifacts merely to resume. If stricter acceptance is needed, capture isolated negative/cancellation trials without extra positive actions. No four-hour endurance, oscilloscope transient, forced queue-saturation, calibration or hard real-time guarantee is claimed. This checkpoint concerns the declared low-voltage prototype, not cloud, multi-room or appliance project completion. Both indicators end OFF.

---

# CURRENT RESUME POINT — final integrated no-PIR verification, 28 October 2026 IST

Supersedes the stale preparation blocks below. Both no-PIR firmware images are DEPLOYED/readback-verified: A SHA2562fad039a863ba65484e09cf761b8f1d3e4b598fb37a63bf95517922fb0ae9955; B60da71c68dce57a5e0f663ff25c53e7b0911008ac20562ec2dc3ce7063b4881c. APK i installed/hash-verified and microphone positively demonstrated; final APK j is building a targeted stale-reconnect-message correction only.85 JVM tests pass. Existing offline model/parser/thresholds unchanged between i and j.

Current evidence: benchmarks/review/voice-led-no-pir-20261028. User verified light4/fan5 individual resistor/GND wiring and startupOFF; earlier0V OFF output measurements retained. DHT7 timeout was actually observed while tasks advanced/watchdogs stayed active and OFF-only commands succeeded. Android showed Unavailable/DHT11/TIMEOUT, then restored24.4C/statusOK without firmware changes. Boot61->62 startupOFF/no oldON replay observed; PIR unavailable, motionnull, auto_flags0 and hardwareAUTO disabled.

Two fixed microphone rounds user-confirmed, corresponding boot61 applied ACK/fresh readback: lightON/OFF#6/#7, fanON/OFF#9/#10; second round#11/#12/#13/#14. Extra#8OFF retained with unspecified provenance. Four negatives/cancellation user-confirmed working; capture also includes deliberate extra lightON#15/OFF#16. User explicitly retracted the mistaken unconfirmed-dispatch report: no confirmation safety defect established. Do not call the mixed window an isolated automated negative pass; exact limitations recorded in trial-results-current.json. Prior unrestricted-recognition failure retained, not overwritten.

Embedded final transport checks PASS: oldboot/wrongroom/zero nonce/wrongtarget rejected; light/fanOFF ACK+freshreadback; duplicate confirm rejected; expired proposal rejected; unconfirmed phone fan proposal no dispatch.3s dwell/4h limit and overload boundaries are actual-source host tested/inspected, not4h physical or queue-saturation endurance claims. Task review: docs/EMBEDDED_LED_REVIEW_20261028.md.

Next: finish j APK artifact/model/signature checks; install/hashverify; restore existing AP/token privately; verify corrected freshness message and temperature with final OFF/OFF; save final handoff/demo/results. Do not rebuild/reflash unchanged ESP images or restart voice/model investigations. PIR and physicalAUTO excluded. Current boot62 lastcommand2 (OFF-only transport checks), OFF/OFF. No ongoing ON campaign.

---

# CURRENT SCOPE - voice-controlled LEDs, PIR removed (28 October 2026 IST)

The user's latest pasted request supersedes physical PIR and occupancy-AUTO acceptance. PIR has been removed. This milestone is manual and offline phone voice control of A GPIO4/light and GPIO5/fan, real DHT11 DATA GPIO7, both LEDs with own series resistors and cathodes GND. Hardware PIR/occupancy/AUTO must be UNAVAILABLE; separately labelled simulation remains intact.

Current work/evidence: `benchmarks/review/voice-led-no-pir-20261028/`. Prior verified manual LEDs, electrical OFF0V checks, DHT, strict parser rejection and phone reconnect evidence remain in `benchmarks/review/gpio8-integrated-20261028/`. Prior microphone failure is genuine, not a pass: authorized local exact-word diagnostic saved there. No model replacement, threshold lowering, homophone aliases, cloud speech or new dependency.

Implemented in current source: explicit no-PIR LED build flag; no reading/configuring removed input; physical occupancy-AUTO denied while SafetyKernel/manual bookkeeping remain; authenticated radio availability flag and gateway null motion/UNAVAILABLE occupancy; Android hardware availability/disabled AUTO and preserved simulation. Added exact phrase patterns and conservative punctuation/case/spacing handling. Whole finalized utterances only; negation, ambiguity, unknown speech and missing targets/actions reject. Immutable boot/capture-bound proposal, explicit confirmation and ACK+fresh readback gates preserved. Memory-only preview now shows interpreted intent too.

Current grammar before this correction was unrestricted `Recognizer(model,16000f)`. Actual saved recognition mismatch justifies evaluating a bounded runtime phrase list with `[unk]` and negative utterances on the SAME bundled dynamic-graph English0.15 model. Candidate APK i built; final artifact/installation and two trials per canonical command plus four negatives/cancellation remain pending. Initial83 JVM tests and65 focused firmware/GPIO/parser/privacy cases passed; final bookkeeping/duplicate-confirm cases are being checked. Final A/B builds pending/finishing; no new-PIR-scope firmware deployed at this checkpoint. Installed images remain A a1a2dd04... / B9e7f6d9a... / APK h until new deployment receipts prove otherwise.

Next: finish final source checks/builds; fresh ROM identity/current image and protected-region checks; guarded app-only B then A update (A input marker unavailable), model/NVS/credential preservation, final APK i artifact/signature/hash checks and install; verify fresh manual OFF/OFF and unavailable PIR/AUTO/DHT; bounded trial ledger (no hiding failures); final restart/reconnect and OFF/OFF checkpoint. No physical PIR troubleshooting or AUTO motion tests in this milestone.

---

# RESUMED - GPIO8 LED prototype, 28 October 2026 IST

The user's explicit resume supersedes the pause blocks below. Current evidence: `benchmarks/review/gpio8-integrated-20261028/`.

Implemented: shared PIR assignment GPIO8 with compile-time conflicts checked against DHT7/light4/fan5/status48/microphone10-12/speaker16-18; native adapter now uses ESP-IDF gpio_config to select GPIO function and direction while preserving SafetyKernel's OFF latch. No SafetyKernel bypass. Updated active gateway, Android and wiring references; historical archived evidence remains historical. Vosk/model/parser/confirmation behavior unchanged.

HOST_MEASURED: 17 focused tests PASS, including actual native GPIO adapter and both polarities; 50 actual firmware policy/PIR/recovery cases PASS; 78 actual-source Android control/voice cases PASS. A build SUCCESS40.085s; B SUCCESS24.809s. APK f built SUCCESS1m47s, model contents/native16KB/ZIP alignment/signature/client configured-value checks PASS; installed base hash MATCH. Final g build corrects a stale settings sentence about five-minute hold; no control or recognition behavior change.

HARDWARE_MEASURED: fresh ROM identities A COM10/e0:72:a1:f5:bc:b0, B COM9/e0:72:a1:f6:a9:68; prior installed images and system regions MATCH. Existing dual rollback copies verified. Guarded application-only B then A update/readback/system verification PASS; B model region MATCH. NVS/table/model/credentials excluded from writes. New A SHA a1a2dd04a400e7b1557df4534171aa3f34cd8cb95a5062c04bda82e01a916837, new B SHA 1cbb912bd886ab9a72585e2059a926d8a64ce29d47546b4e75b830a05629b0f6.

Post-update boot56: fresh authenticated OFF/OFF, last_command0, exact active-HIGH enabled LED profile, HOME/manual_flags3/auto_flags1, actual vacancy grace120000ms, real DHT11 23.5C/statusOK, tasks advancing, sensor and gateway watchdogs active. PIR tracker arms after warmup with OUT isolated; this is NOT evidence of a connected working PIR. Phone local AP connection restored/mobile data disabled; UI Source BOARD/fresh OFF/OFF/outputs ENABLED/DHT11 observed.

USER_REPORTED: own10k series LED wiring checked; PIR isolated. Both indicators visibly OFF and GPIO4/GPIO5 OFF voltages0V. PIR identified as HC-SR501; middle OUT-to-GND measured3.9V. **PIR hardware step blocked: do not connect this OUT to GPIO8.** Need verified compatible output or suitable level interface and common ground. Exact module supply labels/common-ground observation remain unconfirmed. Never apply9V to ESP GPIO/3V3. Do not claim motion/AUTO physical acceptance.

Manual light/fan, final voice, cancellation/unrelated speech, restart/reconnect observations and final checkpoint are in progress. End OFF/OFF. No old ON request replay. No raw audio/transcript retention without the user's permission. Project end-to-end acceptance remains incomplete.

---

# PAUSED BY USER - 28 October 2026 IST

Resume only after an explicit user request. This checkpoint supersedes the status headings below; historical evidence is retained.

- Integration chat `01a168bb-f3c0-76d0-ba5e-05b76ef1772e` is idle; its last two turns were interrupted, including its attempt to save a pause checkpoint. Its review agents were interrupted in that chat. No build, test, flash, phone action, or hardware command was started for this checkpoint.
- Process inspection with Get-Process found no python/esptool/platformio/ninja/cmake/zig/java processes. Detailed CIM inspection was unavailable. No physical state or live board telemetry was remeasured.
- Last documented installed A: active-HIGH `native_voice_uart_led_high_bench`, SHA256 b7577f39afd72d09f2ffbbd8aa386e8f24a13dd36e2ae123de8047e243b87a7f. Last documented B: SHA256 79927efac36102177c57a7cbad081f8b46e727ac22469d518e40e7188410f19e. Phone: 1.0-led-high-20261027e, SHA256 a80b86a1ba5b92d21cc529ae502b5778d3ab83bc589a9b71077b13b77c9137c4. These are prior deployment records, not fresh device verification.
- Source work remains incomplete: native/main/native_hal.h pinMode still calls gpio_set_direction without explicit GPIO function selection. The interrupted turn discussed correcting it but the correction is absent at this checkpoint. Existing tests/unit/test_gpio_initialization.py compiles a SafetyKernel fixture; it does not establish that the actual native adapter was fixed or tested. No new validation result is claimed.
- Existing source, packages, backups, and evidence remain in place. This directory has no Git repository; no Git commit was made. Snapshot copies and hashes of the GPIO source/test files are saved beside this note.
- Physical LED/PIR/AUTO and final voice/recovery acceptance remain pending. The latest user statement in the integration chat says hardware is positioned as requested; exact current wiring, power, and output states were not independently observed. Older isolated-lead notes are historical and must be reconfirmed before hardware work.

Next executable step after explicit resume: inspect the GPIO initialization gap and actual-adapter test coverage, complete the source correction, then run focused host validation and update the master document. Review installed-versus-source differences before any separately authorized deployment or physical test. Never automatically replay an old ON command.

No automatic resume, new tests, builds, deployments, or hardware actions while paused.

---

# CURRENT — active-HIGH firmware deployed; isolated OFF voltage check pending

27 October 2026. This supersedes the earlier resume/preparation and pause blocks.

| Milestone | Status and evidence |
|---|---|
| Prepared firmware/APK | Implemented and host verified: exact mode, polarity, pair, UART/N16R8, offline model, signature, native/ZIP alignment and client-boundary checks PASS. No unchanged rebuilds. |
| A/B identity and recovery | Board verified: A COM10 e0:72:a1:f5:bc:b0; B COM9 e0:72:a1:f6:a9:68. Full installed 3MiB application and system backups read/board-hash verified in two locations. Existing full model backup verified against B. |
| New installed A | `native_voice_uart_led_high_bench`, 689456 bytes, SHA256 b7577f39afd72d09f2ffbbd8aa386e8f24a13dd36e2ae123de8047e243b87a7f. Guarded application-only flash PASS and application/system readback PASS. **LED-only outputs ENABLED; active-HIGH**, not inhibited. |
| New installed B | `speech_intelligence_uart`, 1215776 bytes, SHA256 79927efac36102177c57a7cbad081f8b46e727ac22469d518e40e7188410f19e. Guarded application-only flash PASS; application readback, unchanged system and unchanged model partition PASS. No NVS/table/model/key writes. |
| Installed phone APK | `1.0-led-high-20261027e`, code1; SHA256 a80b86a1ba5b92d21cc529ae502b5778d3ab83bc589a9b71077b13b77c9137c4, pulled installed base MATCH. Offline phone recognition unchanged; runtime voice acceptance pending. |
| Current real telemetry | Board verified boot52, authenticated/fresh OFF/OFF, no commands since update, DHT11 29.1°C/statusOK; samples/task counters advance. Exact `LED_INDICATORS_10K_ACTIVE_HIGH`, active_low=false, manual flags3, HOME, policy2, until_auto. AP192.168.4.1/channel1, controller channel1. Watchdog/sensor watchdog active, control not stalled, sampled gateway deadline misses0. |
| LED physical operation | **Pending.** Both signal leads remain isolated by user confirmation. Requested measured GPIO4/GPIO5 OFF voltages near0V before power-disconnected reconnection, one LED at a time. Previous LOW-profile mismatch evidence retained; no root-cause fix claimed from polarity change. |
| PIR / AUTO | **Pending physical supply identification/verification and wiring.** PIR/battery/relays disconnected. Do not infer broken5Vin from earlier reading. Check a verified5V source and sensor OUT voltage before GPIO6; no9V connection. Real motion/vacancy/manual-untilAUTO/per-deviceAUTO not demonstrated yet. |
| Final voice / recovery / package | Pending bounded phone test, visible LEDs, negative speech, safe boot and reconnect observations. Existing hardware ACK/readback and earlier user-correlated voice confirmation remain historical evidence, not new build acceptance. |

Exact next step: receive isolated GPIO4/5 OFF meter readings. If both near0V, unplug BOTH USBs, reconnect only light circuit GPIO4 -> own10kΩ -> anode / cathode -> GND (no LED3V3), then verify safe boot OFF before one proposal/Confirm ON/OFF trial with automatic transaction capture. Fan follows separately. DHT remains A3V3/DATA GPIO7/GND throughout. Do not connect PIR or replay an ON request.

New evidence: `benchmarks/review/led-active-high-20261027/{resume-backup.json,resume-artifact-guards.json,resume-apk-checks.json,verified-A/result.json,verified-B/result.json,B-compatibility-before-A.json,deployed-runtime.json}`. Read-only serial handles closed after each check. Guarded flash logs retained under benchmarks/raw/hardware. New dual backups identified in resume-backup.json; original rollback copies untouched. Project core physical acceptance remains incomplete.

---

# RESUMED — active-HIGH LED deployment preparation, 27 October 2026

This entry supersedes the pause below. The user authorized completion of the one-room local LED prototype. Cloud, extra rooms, appliance loads and further ESP speech experiments remain deferred.

- **Implemented / host verified:** A/B active-HIGH-compatible candidate hashes and exact output/fault/polarity/pair/UART/N16R8 guards PASS; cached binaries match sealed candidates. APK bundled-model, native alignment, ZIP alignment, existing debug signature and private-value/client-boundary checks PASS. No rebuild needed.
- **Phone package verified:** installed `1.0-led-high-20261027e`, code 1; pulled installed APK SHA256 `a80b86a1ba5b92d21cc529ae502b5778d3ab83bc589a9b71077b13b77c9137c4` matches. Runtime voice not yet tested on this build.
- **Board verified before update:** fresh ROM identities A COM10 / CH343 5C37271524 / e0:72:a1:f5:bc:b0; B COM9 / CH343 5C37271527 / e0:72:a1:f6:a9:68 match provisioning. Installed applications remain the documented LOW baseline until a successful flash receipt. Read-only state: boot49, light OFF/fan OFF, real DHT11 28.9°C/status OK, fresh authenticated reports. Outputs internally enabled with the old active-LOW profile; do not call this inhibited.
- **Physical checkpoint USER_CONFIRMED:** after initial report that LEDs were connected, both USBs were unplugged, both LED signal leads isolated and PIR disconnected; both ESPs then reconnected with DHT unchanged. No PIR/battery/relay wiring authorized. No active-HIGH LED physical test yet.
- **In progress:** read-only dual application/system backup with installed-prefix, partition-table and board-hash verification; existing dual model backup verification. No firmware write yet at this checkpoint.
- **Next:** complete backups, guarded B application-only update then A explicit active-HIGH LED-only update; verify protected system/model regions, fresh OFF/OFF/health/DHT, then require isolated GPIO4/GPIO5 OFF voltage near 0V before any LED reconnection. Investigate any Fresh/UNKNOWN category without replaying commands.

Evidence: `benchmarks/review/led-active-high-20261027/{board-identities.json,resume-before-state.json,resume-artifact-guards.json,resume-apk-checks.json,resume_backup.py}`. Prior failed LED and voice evidence preserved. Core LED/PIR/AUTO/voice physical acceptance remains pending; project is not complete.

---

# PAUSED BY USER —27October2026 IST; resume only on explicit return

**Safe to unplug both ESPs. No activeflashwrite or ownedserialconnection.** Userlastconfirmed bothUSBboardsconnectedwithonlyDHTonA andBOTHLEDsignalleadsdisconnected/isolated. Physicalpowerafterunplugmessageisnotindependentlyobserved. KeepLEDleadsisolated; doNOTreconnectnewGPIO→LED→GNDcircuitsunderinstalledactiveLOWfirmware. PIR/9Vbattery/relaysdisconnected. Noautomaticresume/job/reminder. Already-runninghostbuildsfinished; no owned serial/flash/build worker remains. No new test/deployment/wiring campaign while paused.

## Exact installed baseline (unchanged by HIGH preparation)

- **A controller:**MACe0:72:a1:f5:bc:b0,lastCOM10/CH3435C37271524; installednative_voice_uart_led_bench SHA742c371bc3904e60eb300deee5ce7d976615405fdb8aa5f290339cff80a28275/689456B. **ACTIVE-LOW LED OUTPUTS ENABLED internally**, not inhibited. Lastcapturedboot45/lightON/fanOFF/request1/appliedACK/status0/freshreadback duringinterruptedfantrial. Lastlogicalstateafterhumanpowercycleisnotremeasured; bootrestorepolicyforcesOFF but donotclaimfreshreadbackatpause. ExternalLEDsarenowuser-disconnected, isolatingGPIOactuationfromLEDloads.
- **B gateway:**MACe0:72:a1:f6:a9:68,lastCOM9/CH3435C37271527; installed30f42429c18876819a02d7c3ac0e74f682695e25402755d6772d2c67f189d828/1215776B. APClawGrid-Assistant/http://192.168.4.1/channel1matchingESP-NOW. EditableURL/tokenmemoryonly.
- **Phone installed:**1.0-local-demo-20261027d/code1/packagecom.aistudio.clawgrid.v2edge;100112968B/SHAfb1a8d138529476ab8895807d0a74ad5d3ce62bc9caaa0cad82cfb03ee299545; pulledinstalledbasehashMATCH earlier. ExistingbundledVosk0.3.75/JNA5.18.1/smallEnglish0.15/unrestrictedgraph/0.7eachword unchanged; noESP/cloud/PCspeechfallback/transcriptlogging.

## Completed and observed; limitations preserved

- DHT11VCC3V3/DATAprintedGPIO7/GND realchecksummedtemperaturetelemetryA→B~28.8–29.2°C/statusOK; userAndroid29°Cafterhardcodedtemperaturetile/diagnosticplaceholderfix. Sensor/control/linktasksadvance/watchdogactive/zero sampled gatewaymisses. Notcalibrationproof.
- Earlierinhibitedmanualactionsboot34 lightOFF#1/fanON#2/fanOFF#3 allmatchingACK+freshreadbackPASS. ManualuntilAUTO/per-deviceAUTO/persistmanualmode-as-heldOFF/health/dwell/maxrunimplemented andhosttested; actualPIRmotion/AUTOinactivity/notyetphysicallyverified.
- ActiveLOWlightbranch rebuiltoneatatime:knownGNDreturnlit;returnGPIO4atlogicalOFFoff; userobservedlightONafterconfirmedappaction, lightOFF#4/cleanupfanOFF#5matchingACK+freshOFF/OFFboot44 anduserlightOFFconfirmed. ExtraONcommands/provenancelimitsretained—notcontrolledsingletrial.
- Fanphysicalstage **NOT PASSED**: originallybothLEDsstayedONwhileboardOFF/OFF; wiringrebuildlaterbothsafeOFFuserreported. Laterrequestedfantrialboot45 insteadcapturedlightON#1/fanOFFwithappliedACK+freshlighttrue/fanfalse; userreportedbothLEDlit. NoGPIOpatch/polarityflipclaimedcausefix. NativeIOMUXinitializationomissionisasourceobservationonly; disconnectedGPIO4/5both~3.3Vusermeasured, notcausalproof.
- FirstUNKNOWNcausewasphoneonotherWiFi/mobiledataON; fixedbyjoinAP/mobileOFF. SecondFresh/UNKNOWNalternationoccurredwhileADBconfirmedAP/mobileOFF/appalive. **Rootcauseunresolved**, notclaimedfixed. Noactuationretries/thresholdrelaxation. Safe-stop/unplugandLEDremovalrequested/confirmed.

## User-approved new direction: active-HIGH LED-only (prepared, NOT deployed)

UserexplicitlyrequestsGPIO4/lightandGPIO5/fanwithGNDreturn. Newcircuit **GPIO4or5→own10kΩ→LEDanode/cathode→GND**, noLED3V3connections. BothLEDleadsisolatedforflash. Thisisanintentionalwiring/profilechange, NOTevidenceofrootcauseoffailure.

Sourcechanges: independentnative_voice_uart_led_high_bench/NODE_A_RELAY_ACTIVE_LOW=false/LED_ACTIVE_HIGHmarker, correctUARTdefaults; flashguardexplicit--bench--led-indicators--led-active-highwithpolaritymarkerchecks; gatewaytruthfulLOW/HIGHLEDprofilenamesfromauthenticatedAflag/polarity; schemaacceptsbothonlywithactualoutputs_enabled; existingSafetyKernellogicalmappingpreserved. AndroidexactHIGHprofilegateplusstaticlocalconnectionerrorcategories/clearonvalidreport/historicalrequestlabel; noautofreshnessrelaxation/replay/transcripts. Models/thresholdsunchanged.

- HOST62firmware/guard/GPIO/privacycasesPASS8.95s;78actualsourceJVMcasesPASS1.175s. Thesearehostonly, notactiveHIGHboard/LEDproof.
- BcandidatebuiltSUCCESS29.115s/SHA79927efac36102177c57a7cbad081f8b46e727ac22469d518e40e7188410f19e/1215776B; mode/pair/UARTguardsPASSandcandidatecopied. **Notdeployed.**
- APKe buildSUCCESS2m4s/83identicalsnapshotfiles;deliverables/ClawGrid-LED-High-debug-20261027e.apk,100112964B/SHAa80b86a1ba5b92d21cc529ae502b5778d3ab83bc589a9b71077b13b77c9137c4. SnapshotC:/Users/hares/Documents/ClawGrid-Android-build-led-high-20261027e. Existingdebugkeyreused/releasepreserved. **Notinstalled; finalAPKartifact/signature/clientboundarychecksNOTRUNafterpause** (doonresume, don'tclaimdone).
- AactiveHIGHhostbuildfinishedSUCCESS171.158s;689456B/SHAb7577f39afd72d09f2ffbbd8aa386e8f24a13dd36e2ae123de8047e243b87a7f;bin+ELFsealedincandidate/A,candidate-A.json. NoHIGHfirmwarewrite. Finalmode/pair/UART/exactHIGHartifactguardsNOTRUNafterpause; do them onresume. No candidateinstalled.

## Exact next executable steps AFTER human resume

1. Readthispauseblockandbenchmarks/review/led-active-high-20261027/PREPARATION.md; checkfinalAhostbuildstatus/artifactseals. Noautomaticresumenow. Re-enumerateports/MACsagainstprovisioningbeforehardwareaccess; verifyactualwiringLEDleadsisolated/DHTunchanged/batteryPIRrelaysunused. PreservecurrentfullLOW+inhibitedrollbackcopies. Noerase/NVSrestore/keyregeneration/modelpartitionwrite.
2. FinishrequiredHIGHartifact/pair/mode/UART/exactselectorchecksandAPKeoffline/signature/key/16KBchecks. Guardedapp-onlyBfirst(acceptsoldLOWA),verifyprotectedregions/compatibility; thenAexplicitHIGHprofilewithLEDleadsstillisolated. Readbackapplication/system/protectedmodelasappropriate; stop/rollbacknormalknownimageonfailure. Preservecredentials/NVS/replaycounters.
3. VerifyfreshOFF/OFF/savedmanualheldOFF/exactHIGHprofile/active_lowfalse/DHTOK/control-linkhealth. WithLEDsisolatedmeasureGPIO4/5OFF~0V. Install/hash-verifyAPKe,reapplytokenprivately/mobileOFF/APonly; diagnoseFresh/UNKNOWNusingnewstaticcategorywithnoactuation. Don'tcallcategoryadditionafixwithoutobservation.
4. PowerOFFoneLEDwireatonceusingGPIO→own10k→anode/cathodeGND; startupOFFhumancheck. TestonlylightON/OFFproposalConfirmACKfreshreport+humanvisibilitythenfanindependently/dwell3s; noothercommandextra/replay. CaptureIDsautomatically; stopmismatch, endOFF/OFF.
5. PIRexactmodel/supplyratingsstillunverified; boardonlymarkingN16R8(memorynotPCBpowerpath),5Vin0.67readingdoesNOTprovepinbroken. Battery9V6F22unconnected; no9Vwiringauthorized. Userapprovedbounded3.3Vsupply-onlydiagnosticlater,OUTinitiallyisolated; insufficientvoltagefailuredoesNOTprovePIRbad/reliableoperation. Confirmcompatiblesupply/OUTlevelbeforeGPIO6; thenreal60swarmup/200msLOWbaseline/filter/motion/vacancy/manualuntilAUTO/independentdeviceAUTO. Temperaturemonitoringonly.
6. Safe boot/stale/disconnect/reconnect/reboot/noONreplay/watchdogrecoveryandbriefexistingofflinephonevoice+confirmationlater; nofurthermodelinvestigation. FinalAPK/wiring/demo/results/gaps; cloud/extrarooms/relays/appliancesdeferred.

Evidence/preservation: led-active-high-20261027/{before/,rollback/,rollback-hashes.json,focused-tests.log,jvm-control.log,build-*.log,outside-build-receipt.json,pause-checkpoint.json}; earlierphysical-led-20261027/fan-control/fan-reconnected-ON-OFF.json retainsfailedtrial. Existingfullverifiedprivatebackupsincludingphysical-led-20261027/manual-auto-20261027/dht11-20261027 andoriginalMN7dualbackupsunchanged. NoGitHub/Downloadspublication/noDBchanges/nocredentialsprinted. **Paused; safeUSBunplug; nofurtherphysicalactionsuntilreturn.**

---

# CURRENT — user requested active-HIGH LEDs; builds in progress,27October2026

BothESPUSBsunplugged/bothLEDbranchesdisconnected explicitlyconfirmed; DHTunchanged/PIRbatteryrelaysdisconnected. Prepare conventionalGPIO4/5→own10k→LEDanode/cathode→GND; noLED3V3connections; keepLEDsignalwiresisolatedduringflash. CurrentinstalledactiveLOWfirmwaremustnotpowernewcircuit. No enabledflash/startupLEDtestbeforematchingHIGHprofileandisolationcheck.

ImplementedseparateHIGHenv/marker/explicitflashselector, truthfulgatewayLOW/HIGHprofile, exactprofileappgate; retainedLOW/inhibitedrollback. Staticlocalconnectionissuecategories/historicalrequestlabel added to diagnoseFresh/UNKNOWNwithoutthresholdrelaxation/replay ortranscriptlogging. HOST62firmware/guard/GPIO/privacyPASS8.95s+78JVMcasesPASS1.175s;A/B/APKbuildsinprogress. NeitherHIGHfirmwarenorAPKeinstalledyet. Details benchmarks/review/led-active-high-20261027/PREPARATION.md. Previousfantrialnotpass: actualboot45lightON#1/fanOFFwhileuserbothLEDlit; fancommandnotcaptured. Rootcauseunresolved; user-directedcircuitchangeisnotclaimedcausefix. InstalledA742c371.../B30f424.../phone1.0-local-demo-20261027d preserved.

---

# CURRENT — phoneAP restored/dataOFF; bounded fantrial pending,27October2026

Userreportsdashboardreturned. IndependentADBchecks phoneonClawGrid-Assistant/mobile_data0. Read-onlysameadapter/MACbindingdiagnostics withoutresetverifyboot45/freshOFF/OFF/lastcommand0/DHT29.0°COK/LEDprofileenabled. Evidence fan-control/reconnected-ready.json. Startednew120sread-onlyobserverfan-reconnected-ON-OFF; priorwindowcorrectlyNOTRUN(nofancommand)ratherthanfailure. UseraskedONLYfanONproposalConfirmREPORTED/humanonlyfanLEDlit/lightOFF, wait3s, fanOFFConfirmREPORTED/bothLEDsOFF. Awaitactualresults; noagentcommand/replay/build/flash. Do notresetboard/networkmidtrial. ExistingDHT/lightresults preserved; PIRbatteryrelaysdisconnected.

---

# CURRENT — fantrialNOTRUN; phone switchedWiFi, reconnectpending,27October2026

UserreportsappUNKNOWN/controlsdisabledbeforefantrial. ADBWiFi status independentlyshowsphoneconnectedtoanotherWiFi(notClawGridAP), mobile_data=1. NoSSID/password/tokenretainedinprojectreport. Gatewayread-onlydiagnostics confirm **boot45/freshknownauthenticated/lightOFF/fanOFF/lastcommand0/DHT29.0°COK/exactLEDprofileenabled/APclients0**; noreset/flash/ONcommand. Prior120sfanobserverclosed/errornone/no newcommand. This is phoneconnectionfailure, NOTfanSafetyKernelrejection/physicalfanresult.

AskedturnmobiledataOFF/joinClawGrid-Assistant/StayconnectedwithoutInternet/disableautoswitch/appforeground, reapplyexistingprivateapi_token/http://192.168.4.1onlyifneeded; waitfreshandreply, NOcommandsyet. NextverifyphoneAP/data0andfreshboardwithoutROMreset, thenboundedfanON→humanonlyfanLEDlit/lightOFF→3sdwellfanOFF→bothOFF withproposalConfirmACKreadback. Don'trepeatalreadyattemptedcommands. Evidence fan-control/unknown-phone-network-diagnosis.json. Allserialconnectionsclosedafterfiniteobserver/readcheck; firmwareunchanged/DHTpreserved/PIRbatteryrelaysdisconnected.

---

# CURRENT — lightLEDON/OFF observed; fanON/OFF trial pending,27October2026

Physical lightLEDON confirmed byuser afterappconfirmation, lightOFFACK#4/readbackPASS, fanOFFcleanupACK#5/readbackOFF/OFFPASSboot44, and userexplicitlyconfirmedphysicallightOFF. ExtraearlierONcommands/provenancelimits preserved—notformalcontrolledsingletrial. Fan wiredpowerOFF3V3→separate10k→anode/cathode→printedGPIO5, existinglightGPIO4/DHTGPIO7unchanged; userreportsBOTHLEDsOFFafterpowerup. This succeeds currentrebuiltbranchsafeOFFcheck; earlierbothONfailure/rootcauseunproven remainsretained.

FreshMACverificationA COM10/e0:72:a1:f5:bc:b0,B COM9/e0:72:a1:f6:a9:68PASS; normalresetsadvancedAboot45. FreshOFF/OFF/manual3/exactLEDprofileenabled/DHT29.0°COK/sensorwatchdogverified. Finite120sread-onlyfanobserverstarted, useraskedONLYfanONproposal→explicitConfirm→REPORTED/humanonlyfanLEDlit/lightstaysOFF, wait3s thenfanOFFConfirmREPORTED/bothLEDsOFF. Awaitresult; noagentcommands/replays/noAUTOvoice. Evidence physical-led-20261027/{light-completed-fan-safe-boot.json,fan-control/board-identities.json,before-fan-ON.json,fan-ON-OFF.json/.log}. InstalledA742c371.../B30f424.../phone1.0-local-demo-20261027d unchanged, outputsENABLED_LED_ONLYnotinhibited.

PIR/battery/relaysremain disconnected. AfterLEDpass: boundeduserapproved3.3VPIRsupply-onlycheck withOUTinitiallyisolated, validitylimits; exactmodulerating/board5Vinpathunresolved/no9Vconnectionauthorized. CompletePIRmotion/AUTOvacancy/manualoverride onlywithactualobservations.

---

# CURRENT — cleanupOFF/OFF verified#5; fan wiring pending physical lightOFF confirmation,27October2026

LightLEDON userexplicitlyobserved/correct10kseriespathconfirmed/fanLEDdisconnected. ExtraONcommandsbeforeACK#3mean notcontrolledsingletrial; preservecount/provenancelimits. LightOFF#4 appliedACK/freshreadbackPASS. InitialcleanupcheckranbeforefanOFFarrived and correctlyfailedOFF/OFFassertion (no commandretry). UserthenreportedFanOFFREPORTED; latestread-onlycheck confirms **boot44/fanOFF#5 phase2/status0/oneattempt/RTT18756us/processing65us/freshOFF/OFFlast5/DHTOK**. Noagentactuation/replay. Physical lightLEDout confirmation stillpending; do notinventitfromlogicalOFF.

Nexthumansteprequested: ifphysical lightLEDnowOFF, unplugBOTHUSB and addfan3V3→ownsecond10k→anode/cathode→printedGPIO5, workinglightGPIO4/DHTGPIO7unchanged/PIRbatteryrelaysdisconnected; keepOFFandack. If lightstilllitstopnofanwiring. Candidate/installedA742c371... LEDoutputsenabledinternally/B30f424.../APK1.0-local-demo-20261027d unchanged. Nextafterfanwiringackpoweredboth safeOFFcheck then onefanON/OFFproposalConfirmACKreadback+humanvisibility, dwell3s. PriorsteadybothONfailurepreserved; onebranchrebuildlightpasssuggestsactualcircuitconnectionissuebutrootcausenotindependentlyproven; noGPIOpatch/polarityflipdone.

Evidence light-control/{after-user-ON.json,cleanup-after-extra-commands.json,after-user-cleanup.json,fan-OFF-reported-check.json,light-stage-result.json}. Allserialhandlesclosed afterfinitechecks. PIRsupplyteststillpending/9Vbatteryunused.

---

# CURRENT — extra app commands observed; light/fanON cleanup pending,27October2026

After user replied ambiguously “done i am getting also i have wires in3.3v and4” to the ONE-lightON request, read-only gatewaycheck shows boot44/lastcommand3/appliedACK#3phase2status0/lighttrue/fantrue; freshsample217998ms/age201ms, DHTstatusOK/profileLED_INDICATORS_10K_ACTIVE_LOW/outputsENABLED. Earlierobserverwindow hadlast0; **threecommands occurred, not the single requestedtrial**. Prior#1/#2 payloads/provenance notcaptured inthischeck; don'tclaimcontrolledone-commandpass. PhysicalLEDvisibility/resistorpath and secondLEDdisconnected status requireuserclarification. No commandreplayed byagent. Evidence light-control/after-user-ON.json.

Requested STOPON/AUTO/voice; returnviaapp lightOFF→review→Confirm→REPORTED, thenfanOFF→review→Confirm→REPORTED onceeach. Requested actualexternalLEDlitafterON/nowOFF and10kseriespath; noIDsneeded. Finite read-onlyLEDobservercleanup-after-extra-commands75sstarted; no agentactuation/replay. Awaituserandgatewaycleanupobservations; lastverifiedlogicalstate is ON/ON, NOTOFF. Ifrejecttimeout/mismatchstop, don'tretry orclaimphysicalsuccess. All wiringchangespowerOFF; PIRbatteryrelaysremainunused.

---

# CURRENT — lightON app trial pending on boot44; observer read-only,27October2026

User connectedB after singlelight passiveOFFcheck. FreshROMMACs match ACOM10/e0:72:a1:f5:bc:b0/CH3435C37271524 and BCOM9/e0:72:a1:f6:a9:68/5C37271527. NormalidentityresetsadvancedAboot44. Freshknown/authenticatedOFF/OFF/manual3/profileLED_INDICATORS_10K_ACTIVE_LOW/physical_outputs_enabledtrue/DHT28.8°COK/sensor+gatewaywatchdogs verified. No firmwarewrite/build/recognitionchange.

Finite scripts/observe_led_control.py added: GETstate/transaction/rtosonly, requires exactLEDprofile/freshness/sameboot, stoponrejecttimeout/profilechange; no commands/replay. Evidence physical-led-20261027/light-control/{board-identities.json,before-light-ON.json,light-ON.json/.log};90sread-onlyobserver running. Userasked ONE BedroomlightONproposal→explicitConfirm→REPORTED+humanLEDlit/notlit. Awaitresult; do not duplicateON merelyforID. SecondLEDfullydisconnected/DHTunchanged/PIRbatteryrelaysdisconnected. NextaftercorrectONvisibility/ACKnewreport: cleanupOFF after3sdwell, observehumanOFF. If LEDmismatchorerrorstop/diagnosebeforefanstage. InternallyenabledLEDprofile—notinhibited; no applianceclaim.

**Light observer window ended:**90sread-onlywindow completed/errornone/serialclosed; no newcommandobserved, finalboot44/lastcommand0/OFF/OFF/LEDprofileenabled/DHTstatusOK. PhoneAPclientcount0atobservation. This is NOT a failedONtransaction or a physicalONpass; useraction/resultstillpending. Instructed reconnectClawGridWiFi/freshappthenONEONproposal/Confirm; do notrepeatifalreadyattempted. Evidence light-control/observer-window-result.json. Later finaltransaction can be read without reissuingcommand.

---

# CURRENT — single-light passive OFF check passes; controlled ON/OFF pending,27October2026

User completed knownreference circuit3V3→own10k→oneLEDanode/cathode→GND: **LEDlit**. Then AUSBunplugged, movedONLYcathodereturn fromGND toprintedGPIO4, poweredONLYA, waited10seconds/noappcommands: **LEDoff**. This is USER-OBSERVED passive electricalOFF, not a confirmed commandedON/OFF transaction. SecondLEDfullydisconnected/DHT3V3/GPIO7/GNDunchanged; BPIRbatteryrelaysdisconnected. EarlierbothLEDsteadyON failure/currentpath remains unexplained and preserved; no firmwarepatch/polaritychange made. NativeIOMUXsourceobservation is not causalproof.

InstalledALED742c371... still outputENABLED throughSafetyKernel, not inhibited; B30f424... lastverified/phone1.0-local-demo-20261027d unchanged. Current hostenumeration onlyA-associatedCOM10/CH3435C37271524. Next physicalstep requested: leaveA/light/DHTunchanged, connectONLYBUSB, noactuatorcommands yet; then freshMACverification/freshlogicalOFF+profile/savedholds/DHThealth, followedby one lightON/explicitConfirm/ACK+freshreadback/humanLEDobservation, cleanupOFF after3sdwell. Fanbranchwillbeadded/tested separately afterlightpass. Exactevidence physical-led-20261027/single-light-reference-results.json. No build/flash/ONcommand in thisdiagnostic.

PIRpower/exactmodulerating stillunresolved; userapproved bounded3.3Vcheck later, not9Vbatterywiring. Donotassume5Vinbroken. CompleteLEDstagefirst, preserveDHT andSafetyKernel.

---

# CURRENT — LED branches removed; one known-GND reference circuit pending,27October2026

User acknowledges unplug/completeLEDbranchremoval and says priorconnectionwasgood, but does not explicitly distinguish previousGPIO5versusloosecathode position. Record as user wiringassessment, not independentverification/rootcause. Current requested POWER-OFF stage: onlyoneLED3V3→own10k→anode/cathode→printedGND onA, noGPIO. SecondLEDfullydisconnected, DHTunchanged, BPIRbatteryrelaysdisconnected. Await wiringack; keepUSBsoff until separatepowercheck. Purpose is knownONreference then later movingONLYreturnfromGND to sameprintedGPIO4 forOFF comparison, withpoweroff betweenchanges. No firmware/polarity changes or actuator commands. LEDprofile742c371... remainsinstalled/enabledinternally; physicalLEDonGPIOcontrolfailed/unresolved. PIR3.3Vtrialnotstarted.

---

# CURRENT HARDWARE STOP — actual LED setup differs from isolation test,27October2026

User reports BOTH ESPs powered/BOTH externalLEDs lit instead of intended onlyA/singlelight/fancathodeisolated setup. Whether fan cathode was reconnected is unknown; cannot conclude software or wiringcause. NoONcommands/firmwarepatch/thresholdchange since initialsafe-bootfailure. **Requested unplugBOTHUSB and disconnect bothLEDbranches completely from supply/GPIO/breadboard**, retain verifiedDHT3V3/GPIO7/GND onA; PIRbatteryrelaysdisconnected. Await acknowledgment and fan-cathode clarification; powerOFF. Do not proceed to actuation or callphysicalOFFverified.

User proposes tryingPIR3.3V before another supply. A reversible/bounded3.3Vsupply-only diagnostic may be considered after LEDfaultisolated, withOUTinitiallydisconnected and readingschecked; under-voltagefailure would NOT provePIRfault or successful/reliablemotionoperation. No9Vbatterywiringauthorized. ActualHC-SR501-stylefamily normally expects~5Vinput; exactmodel/ratingremainsunverified. NoPIRtrialstarted. ResolveLEDbranchcurrentpath first, onepower-disconnectedstep atatime; preserveDHT andinstalledLEDprofile742c371... (internallyenabled, not inhibited).

---

# ACTIVE — finish physical LEDs and PIR; awaiting single-light observation,27October2026

User asks to finish LED indicators/PIR now. Preserve DHT11 and native app; no voice-model investigation/relay/appliance wiring. Current installedALEDprofile742c371bc3904e60eb300deee5ce7d976615405fdb8aa5f290339cff80a28275 is OUTPUT-ENABLED internally through SafetyKernel; currentB30f42429.../phone1.0-local-demo-20261027d remain last verified. No activePython/esptool/build workers found; fresh Windows enumeration shows only A-associatedCH343 COM10/5C37271524 plusBluetooth; physical role must be MAC-reverified before further diagnostics/deployment, not assigned byLEDcolour.

Confirmed latest human observations: bothLEDs originally stayedlit afterboot with lastlogicalOFF/OFF, not juststartupflash; afterGPIOcathodesdisconnected/isolated, bothLEDsOFF; user confirms BOTHprintedGPIO4and5 measure~3.3V-toGND in that isolated condition. User then unpluggedA and reconnectedONLYlightcathode to SAMEmeasuredprintedGPIO4, fanisolated/DHTunchanged/BPIRbatteryrelaysdisconnected. Singlelight10secondpowered result was requested but not yet answered. NoONcommands/firmwarepatch/polarityflip sincefailedcheck; IOMUXsourceomission is not establishedcause. Actualwiringcurrentpath remainsunresolved.

Next pendinghumaninfo: actualcurrentsetup and singlelight OFF/briefFLASH/steadyON after10seconds (do not movepoweredwires); exactPIRmodel/inputrating or readablemodule/regulatorphoto.9V6F22battery NOTconnected/authorized; N16R8notPCBpoweridentity; no5Vin damage assumption. If singlelightsteadyON, powerOFF and isolate branch, then verify actual rail/eachLEDterminal voltage and printedpin mapping before anyactuation. If steadyOFF, freshMAC/healthyreports + boundedlightON/OFF with confirmation/ACK/readback and humanvisibility, then fanseparately; returnOFF/OFF beforePIRwork. Never convertlogicalACK into physicalsuccess.

Master checklist: implementedDHTrealtelemetry/manualfan/manual-untilAUTO/persistentmanualheldOFF/explicitAUTO capability and guardedLEDprofile; hosttests77JVM+59focusedpassed; boardmanualinhibited#1lightOFF/#2fanON/#3fanOFFallPASS; actualDHTA→BstatusOK~29°C and user-observedAndroid29°C; LEDsafe-bootphysicalcheckFAILED/unresolved; PIRsupply/motion/AUTOinactivityphysical/recovery/finaldemo remainpending. Existingfullrollbackdualbackups/privatecredentials/models/NVS/partitionspreserved. Earliercheckpointsbelowretainhistory.

---

# CURRENT LED DIAGNOSIS — reported GPIO~3.3V; light-only reconnection pending,27October2026

User reports “around3.3V” in response to measuring printedGPIO4/5-toGND with cathodesisolated, onlyApowered. This is consistent with expected activeLOWOFF, not support for flippingpolarity/flashingaGPIOpatch. Asked confirmation BOTHpins were measured and whether originalBOTHON was steady or a briefstartupflash. Previous nativeIOMUX omission remains a source observation, **not proven causal**; no GPIOpatch/build/deploy performed.

Next one physicalstep queued: unplugAUSB; reconnectONLYlightLEDcathode to the SAMEmeasured printedGPIO4, keep3V3→own10k→anode, fancathode loose/isolated; DHTunchanged/BPIRbatteryrelaysdisconnected. Await wiringacknowledgment; keep powerOFF until deliberate light-only bootcheck. NoONcommands, no physicalswitching pass. InstalledALED742c371... remains internallyenabled; electricalmeasurement/userobservation limits retained.

---

# CURRENT LED DIAGNOSIS — isolated cathodes bothOFF; voltage measurements pending,27October2026

User reports BOTH externalLEDsOFF when onlyAUSBpowered and LEDcathode wires disconnected from GPIO4/5/isolated; DHT unchanged, B/PIR/battery/relays disconnected. Previous failed physicalsafe-boot BOTHON with GPIOconnections is preserved, not dismissed. This narrows the current path but does not establish software polarity/pin-selection/wiring cause. No appONcommand has been sent. Current next requested observation: DCvoltage each printedGPIO4/5-toGND with LEDcathodes still isolated; activeLOWOFF expects~3.3V. Do not reconnect LEDs until cause/electrical levels resolved.

PinnedIDFsource review: gpio_set_direction enables output/matrixroute but does not select GPIOIOMUX; gpio_config explicitly selects it. native_hal.pinMode presently lacks fullGPIOconfiguration. This is a concrete initialization omission, possible contributor, NOT confirmed rootcause of the two litLEDs. No patch/build/flash since reportedfailure. InstalledALED742c371... remains enabled internally, physically isolated from cathodes; no inhibited claim. Readingspending before corrective deployment. All serial handles remainclosed.

---

# STOPPED LED ACTUATION TEST — physical safe-boot mismatch,27October2026

User reports BOTH externalLEDsON after LED-profile deployment while last authenticated board samples boot39 report OFF/OFF/manual3/lastcommand0. **Safe-boot physical check FAIL; no ON/actuator command was sent.** Immediately asked unplug both USBs; user confirmed unplugged/LEDsout. Then user confirmed only LEDcathode wires removed from GPIO4/5, loose/isolated; DHT unchanged, PIR/battery/relays disconnected. Current pending isolation check: powerONLYAUSB with LEDcathodes separately isolated and observe whether eitherLEDlights; no commands/wire movement. B remainsunplugged. Do not resume ON tests or flip polarity to force success.

A remains flashed LED-enabled742c371...; powerdisconnection/removedcathodes is the current physical stop boundary, not an inhibited firmware claim. Rollback75d668... and fullapplication dualbackup preserved. Cause unresolved. Source review found native_hal.pinMode only calls gpio_set_direction, which in pinned IDF4.4.7 does NOT select GPIOIOMUX whereas gpio_config does; this is a real initialization omission/possible contributor, not yet causal proof. No source fix/build/flash after this failure yet. First distinguish an unintended breadboard current path from pin routing/output level with isolated branches. BothLEDon cannot be accepted as logicalOFF or switching success.

Evidence physical-led-20261027/safe-boot-physical-failure.json; prior enabled-profile readbacks/systemchecks remain valid historical artifacts but do not prove physicalOFF. Preserve failed result.

---

# CURRENT — LED-only outputs ENABLED on A, safe-boot human check pending;27October2026

**Output inhibition status changed by explicit user-authorized LED stage:** controllerA now runs guarded native_voice_uart_led_bench SHA256742c371bc3904e60eb300deee5ce7d976615405fdb8aa5f290339cff80a28275 (689456B), physical_outputs_enabled=true, profileLED_INDICATORS_10K_ACTIVE_LOW, activeLOWGPIO4/5/defaultfaults. Do not call current outputs inhibited. Only LEDs/separate10k resistors are user-confirmed connected; relays/PIR/battery unused/disconnected; DHT3V3/GPIO7/GND preserved. Independent wiring photo/electrical inspection was declined and remains unverified.

Fresh MACs ACOM10/e0:72:a1:f5:bc:b0 and BCOM9/e0:72:a1:f6:a9:68 matched before hardware actions. Before enabling: boot36/OFF/OFF/inhibited/savedmanual3/DHT29.0°C/statusOK; user bothLEDoff on inhibited image. Full current inhibitedAapp0 read/board-verified and preserved twice in benchmarks/backups/physical-led-20261027 and Documents/ClawGrid-private-backups/physical-led-20261027, systemverified. Existing guarded script with explicit --role A --uart-console --app-only --bench --led-indicators wrote app0 only; afterward applicationSHA/system readbacksPASS. No B write/model/NVS/table/key changes. B remains30f42429c18876819a02d7c3ac0e74f682695e25402755d6772d2c67f189d828; phone remains installed1.0-local-demo-20261027d/SHAfb1a8d13... . Rollback inhibited artifact75d668... remains independently saved.

Postdeployment board observations boot39/samples3998→6998→9998ms/controljobs788→1389→1987/sensorjobs1→2→4; sensor/gatewaywatchdogtrue, gateway0sampledmisses, DHT29.0°C/statusOK. **Logical lightOFF/fanOFF/manualflags3/lastcommand0**; no ON/actuator command sent since deployment. These are board-state observations, not physical LED proof. Current user question queued: confirm BOTH external LEDsOFF before any ON test. Stop if unexpectedly lit; no further command until answer. All owned serial handles closed after finite checks.

Evidence benchmarks/review/physical-led-20261027/{board-identities.json,before-enable-runtime.json,deployment/preflight.json,deployment/deployed.json,after-enable-runtime.json,enabled-profile-summary.json}. Prior three inhibited manual actions allPASS#1OFF/#2fanON/#3fanOFF, no physical claim. PIR exactinput/boardpower unresolved;9V6F22 onlyidentified, battery notconnected/authorized; no5Vin damage assumption. Next safe-boot humanLEDcheck, then one lightON→humanvisibility+ACK/freshreadback→OFF, then fan similarly, respecting3sdwell and no automaticreplay.

---

# CURRENT — both LED circuits user-confirmed; inhibited power-up pending, 27 October 2026 IST

User acknowledged light and fan activeLOW circuits on printedGPIO4/5, each with its own10k resistor, and explicitly declined a photo. Proceed based on USER_REPORTED pin/polarity/resistor wiring; **independent visual/electrical wiring inspection is unverified**, not completed. Respect no-photo preference; no additional photo approval gate. DHT3V3/GPIO7/GND remains unchanged; PIR/battery/relays disconnected.

Queued physical action: connect both UART/CH343 USBs without moving wires, observe/report either externalLED lit and stop/unplug for heating/repeatedUSBdropouts. Current port enumeration shows noCH343 (still unplugged). Must fresh-MAC verify and check normal inhibited OFF/OFF/DHT state and stored manual holds before enabled-profile deployment. Built/sealed LED candidate742c371bc3904e60eb300deee5ce7d976615405fdb8aa5f290339cff80a28275 remains **not installed**; normalA75d668.../B30f424... remain last verified installed. Authorization to enable LED-only comes from user's explicit physical-LED request; no relay/appliance loads or model/credential changes. Report human LED observations separately from ACK/board-report evidence. Earlier photo-gate wording below is superseded by this explicit user preference.

---

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

## Physical LED milestone started —27October2026

All three captured app actions independently reviewed PASS on boot34: lightOFF#1,fanON#2,fanOFF#3 each phase2/status0 ACK plus fresh sameboot board report/lastcommand/output matches; see manual-three-actions-verdict.json. FinalOFF/OFF/inhibited; these do not establish LED operation.

User confirmed both USBs unplugged, controllerA printedGPIO4/5 identified (not header indices), LED anode/cathode identified; DHT wires unchanged/PIR,battery,relays disconnected. Current **pending single wiring step**:3V3→one10k resistor→lightLEDanode; lightLEDcathode→printedGPIO4. FanLED remains disconnected; power remainsOFF. Await acknowledgment then guide fan separately and inspect photo/wiring before enablement.

LED profile build attempt1 FAILED53.567s at native_hal compile-time UART guard (new _led_bench environment absent from CMake's explicit UART defaults regex), not a flash failure. Updated native/CMakeLists.txt regex to include only intended _led_bench; saved/moved failed generated SDK configs to benchmarks/review/physical-led-20261027/*.failed-console, no deletion of baselines. Six selector checks PASS. Rebuild build-led-profile-uart.log in progress; do not claim candidate built/installed until success and guards. Current inhibited A/B bin+ELF saved independently under physical-led-20261027/inhibited-baseline because PlatformIO can purge sibling environment caches.

No output-enabled flash, diagnostic-fault image, actuator command, model/NVS/partition/credential change in this physical-preparation step. Exact ESPPCB and PIRinput rating remain unresolved; no9Vbattery connection authorized and no5Vin damage diagnosis.

**LED candidate build complete, not deployed:** native_voice_uart_led_bench SUCCESS120.259s after explicit CMake UART-default correction;689456B/SHA256742c371bc3904e60eb300deee5ce7d976615405fdb8aa5f290339cff80a28275; linkedRAM69396B (build size, not runtime measurement). Enabled/default-fault/board-pair/UART0/explicitLEDselector guards PASS. Candidate bin/ELF sealed separately in physical-led-20261027/candidate; candidate-identity.json deployed=false. No COM board currently visible (both unplugged as requested), no flash write/serial handle. Await user's light-only activeLOW circuit acknowledgment; then fan circuit/photo inspection before power/deploy. Installed inhibited policy A75d668.../B30f424... unchanged.

**Light wiring acknowledged:** User confirms the requested power-disconnected light circuit (A3V3→own10k→LEDanode/cathode→printedGPIO4). This is USER_REPORTED wiring, not independent visual/electrical verification. Next requested single step: fan circuit A3V3→second10k→fanLEDanode/cathode→printedGPIO5, bothUSBsOFF, DHT/light unchanged; PIR/battery/relays disconnected. Await acknowledgment and clear photo of both circuits/pin labels before power or LED-profile deployment. Candidate742c371... still built-only/not flashed; no physical LED result.

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

# CURRENT — DHT wiring resolved; inhibited deployment preflight in progress, 27 October 2026 IST

User confirmed DATA is on the pin **printed7/IO7/GPIO7**, resolving physical-header-index ambiguity, then confirmed both USB boards powered. DHT VCC3V3/DATAGPIO7/GND on physically MAC-identified A; PIR/LEDs/relays disconnected. DHT power indicator is user-reported power only, not sensing proof. Fresh MAC verification ACOM10/e0:72:a1:f5:bc:b0, BCOM9/e0:72:a1:f6:a9:68. Before deployment: boot28/fresh authenticated OFF/OFF/outputsINHIBITED/channel1/watchdogtrue/0 sampled misses, lastcommand0. Evidence before-deploy.json.

**Phone update now installed:**1.0-dht11-20261027b/code1; installed base pulled SHA25659b6b2423992bc3c1f6cfd2b080da8d6abd0a173b437a94f0db72cd8dee12fac matches new APK. No uninstall/data clear/recording; prior a preserved. Token/manual proof will need private restoration after firmware/wiring stabilizes.

Normal inhibited candidate images remain A2b59eab9.../Be549037e...; app-only guarded B-first/A deployment is authorized and pending completion of baseline/protected-region/full-A-backup preflight. Two original full private B backup copies reverified. No output-enabled/diagnostic/model/NVS/partition write. Do not claim candidate firmware installed until receipts/readback succeed. All source/tests/build evidence in docs/DHT11_AND_LED_WIRING_20261027.md and benchmarks/review/dht11-20261027/.

**PIR remains blocked separately:** bare-board5Vin reading0.67 and working3V3 reported by user; units/exact meter setup/board power path unverified. This does not establish a damaged5Vin pin. Board model/5Vin-area photo requested; do not feed HC-SR501 from3V3 or wire OUT by assumption. DHT3V3 stage proceeds independently. Prior hardware-hold notes below are history superseded for DHT only.

---

# CURRENT HARDWARE CHECK — bare-board5Vin measurement pending, 27 October 2026 IST

User confirms controllerAUSB unplugged/PIR supply wires removed/all components disconnected/B unplugged. Reports0.01 and0.00 and says original supply connection was labelled5Vin/GND. Exact units/meter setting and whether these latest readings were taken powered are still unconfirmed; near-zero is expected if measured after the requested unplug, so do not diagnose a defective rail/sensor from this report.

Queued next physical action: with every component disconnected and B unplugged, set meter DCV/20V or autorange (COM and VΩ leads), connect only AUSB, probe GND→5Vin without bridging adjacent pins; report reading/units and powerLED. No sensor signal wiring or output enablement until supply/logic checks pass. No serial probe/build/test/flash for this clarification; driver candidates remain built-only/not deployed, phone remains last verified local-led-a. Prior readings and context remain history below.

---

# HARDWARE HOLD — PIR voltage report, 27 October 2026 IST

User reports approximately0.67 and0.6 for requested PIR VCC/GND and HIGH OUT/GND checks. Units/range and exact measurement mapping are not yet confirmed; this is USER_REPORTED, not independently measured or proof of a faulty module. If voltsDC in requested order, supply is far below expected5V. PIR OUT remains disconnected; do not connect GPIO6, DHT or LEDs, enable outputs, flash or infer motion success from these readings.

Immediate single physical step requested: unplug controllerAUSB and remove both PIR supply wires, B unplugged/all components disconnected; await acknowledgment before repowering. Also requested meter DC range/display units and exact source pin label5V/VBUS versus5/GPIO5. Check meter setup and bare-board rail at the next deliberate stage; don't assume a resistor, voltage or module fault or move VCC to another rail blindly. All source/build artifacts and installed baseline remain unchanged. Latest code candidates remain built-only/not deployed; last verified installed phone1.0-local-led-20261027a. No new host/board tests, serial connection, firmware write or output enablement for this follow-up.

---

# CURRENT HANDOFF — DHT11 candidates built, not deployed; 27 October 2026 IST

Read master current top and docs/DHT11_AND_LED_WIRING_20261027.md. User supplied actual sensor labels PIR VCC/OUT/GND; DHT VCC/DATA/GND; affirmatively answered requested GPIO7/free-board-label/DHT11/colour-code check. Current resistor inventory TWO10kΩ (brown-black-orange-gold if four-band), not earlier10MΩ inference. Labels checked by user, no new close-up/electrical measurements independently captured. No relays/appliance loads.

- **Physical step pending:** both USBs unplugged; connect only PIR VCC→controllerA5V/GND→AGND; leave OUT/DHT/LEDs disconnected. Asked via async and awaiting acknowledgment. Do not advance on silence. Next guide supply/OUT voltage check before connecting GPIO6, powered-off for wiring changes. A GPIO4light/5fan/6PIR, newly configured7DHT DATA. DHT supply3V3 to keep DATA pullup safe. Planned LED circuit deliberately activeLOW:3V3→own10k→anode, cathodeGPIO4or5, later inspected LED-only stage; don't connect all stages now.
- Added dht11_decode.h/dht11_gpio.h: bounded100edge capture, checksum/length/pulse/range, SensorTaskpriority1/core0/static mailbox/20ms start sleep+10ms capture sleep/2500ms reads/2s settling/watchdog. Only short edge bookkeeping critical sections, not entire frame; ControlTask samples completed values only. Official legacy IDF4.4.7/S3 RMT RX issue13419 led to abandoning unbuilt RMT sketch; onboard RGB TX0 untouched. Temperature failure unavailable, not control failure/automation input.
- state_wire schema4/56bytes carries actual temperature/humidity/status/age; newB accepts old3/44bytes during B-first rollout, no stale value leaks. Validity10s and control-report freshness required end-to-end. JSON null/error status on unavailable; Android hardware Bedroom renders valid DHT temperature. All sources/files/tests listed in report. Voice engine/model/parser/.7gate/confirmation untouched; current voice gate still inhibits when outputs enabled—do not claim LED-enabled voice yet.
- HOST:45actual firmware cases PASS40.45s incl4new DHT cases +existing41. Initial pytest defaulttemp WinError5 generated45setup errors/no assertions; retained, then fresh Documents/ClawGrid-test-dht11-20261027 --basetemp PASS.72actual-source JVM cases PASS1.156s. A native_voice_uart SUCCESS69.443s (linkedRAM69324B/flash686913B, not runtime); Bspeech_intelligence_uart SUCCESS24.466s. Artifact mode/pair/UART guards PASS for both normal inhibited/default-fault candidates. No new board testing/firmware write.
- CandidateA687280B/SHA2b59eab9af0b05367d0e503682998d868ea0df1c9c6e98102b720ce2eb6ed64b; B1214352B/SHAe549037e0093829bfa02882c0141ca1c7db0eac05cafed1f986334649e29d7b8. Saved unchanged installed baseline A8f028bd9.../B97daa84e... bin/ELF in benchmarks/review/dht11-20261027/baseline/, candidates separately candidate/; full original private B model/system/rollback backups retained. Do not overwrite backups or restore old NVS/counters.
- New APK deliverables/ClawGrid-DHT11-debug-20261027b.apk/version1.0-dht11-20261027b/code1/100112964B/SHA59b6b2423992bc3c1f6cfd2b080da8d6abd0a173b437a94f0db72cd8dee12fac; build SUCCESS1m36s/83identical snapshot files C:/Users/hares/Documents/ClawGrid-Android-build-dht11-20261027b. Offline model14hashes/native4ABI/16KB/signature/client-key boundary PASS. **Not installed:** ADB phone unavailable at attempt, no install command issued. Last verified phone still1.0-local-led-20261027a/SHA365ee5e6... . Avoid claiming b installed.
- No ESP ports now (power-off wiring stage), no serial handles/active flash. Last actual board observation boot24/OFF/OFF/outputsINHIBITED/advancing tasks/channel1/AP192.168.4.1/watchdog active is earlier baseline, not a fresh driver result. Zero new actuator commands/recordings/cloud changes/model writes.

**Next after user physical acknowledgment:** power/measurement in deliberate stages; fresh enumeration/MAC roles (don't assumeCOM10/9), confirm setup/inhibition. Deploy guarded app-only inhibited B first then A, verify candidate identities/current baseline/protected regions/readbacks, never erase/model/NVS/table update; rollback saved normal apps if failure. Then schema4 unavailable-sensor/control-health verification, b install/hash verification when phone visible, powered-off DHT connection and actual readings A→B→Android. Keep manual proof/token setup for stable wiring/boot context. Later remaining manual/AUTO policy, LED-only output gate/recovery and bounded unchanged phone voice checks; no appliance result claim.

**Physical identity checkpoint:** User acknowledged the PIR supply-only step, then asked whether blue- or green-blinking board is A/B. Physical association of the wired board is therefore unresolved; do not record these wires as definitely on A. Blink colours are activity indicators, not a role identifier. Fresh Windows enumeration shows no CH343 ports. Requested connecting ONLY the board carrying PIR VCC/GND via its UART/CH343 socket, other board unplugged/OUT disconnected; await acknowledgment, then enumerate and ROM-MAC verify it and label physically before further wiring. No flash/serial probe/new test for this clarification.

**Physical A identified:** After user connected only the PIR-supply board, fresh enumeration found only CH343 COM10/adapter5C37271524. ROM MACe0:72:a1:f5:bc:b0 matched provisioned controllerA; normal reset, no write. Evidence benchmarks/review/dht11-20261027/physical-single-board-identity.json and .log. The user-reported PIR VCC/GND wires are therefore associated with A; requested physical A label. B is unplugged, must freshly verify when connected. Blink colour is not used. Next queued action is DC-voltage measurement at PIR VCC/GND and HIGH OUT/GND with OUT still disconnected; await readings or explicit no-meter report before signal wiring. No new firmware deployed, sensor/LED readings not yet observed.

---

# CURRENT HANDOFF — ACTIVE, local LED prototype due 27 October 2026

Read master top and docs/LOCAL_LED_PROTOTYPE_20261027.md. User superseded study pause: complete one room today. Existing native UI/offline phone Vosk/manual baseline preserved. External LEDs represent light/fan; **no relays/appliance loads**. No new voice/model campaign. Cloud/extra rooms/redesign deferred.

- Fresh MAC/role checks: A COM10/e0:72:a1:f5:bc:b0/CH3435C37271524, B COM9/e0:72:a1:f6:a9:68/5C37271527. Re-enumerate again after physical power changes. Read-only application verify-flash PASS: A8f028bd90f65a55a72b09f4b886db7a3fa851f8a2108c6b79659136db8713cdf(683008B), B97daa84e9865a0df09c550387051d401b6d7cc98f547c808bd6734b13a35cbc4(1213232B). No firmware writes/builds. Normal loader resets advanced A to boot24. No model/NVS/table/key writes.
- Fresh inhibited OFF/OFF/lastcommand0, authenticated samples78997→81997ms; ControlTask15672→16295jobs, gatewayLink14480→15080; AP192.168.4.1/channel1/watchdogtrue/0 sampled misses. Serial handles closed. Not physical sensor/LED results.
- Source GPIOs controller A: light4/fan5/PIR6; PIR INPUT_PULLDOWN/warmup60s/LOWbaseline200ms/filter200ms. Outputs activeLOW assumption and pin-1 when inhibited. Temperature DATA pin/driver/telemetry absent. Do not invent readings.
- Photos received: Downloads WhatsApp Image 2026-10-27 at 5.51.07 PM.jpeg,5.51.08 PM (1).jpeg,5.51.08 PM.jpeg. Likely HC-SR501-style PIR and DHT11-style breakout; header/model/supply/ESP labels are not legible enough for connector order. Requested power-disconnected, jumper-removed header/back/ESP close-ups. User subsequently reports resistor bands brown-black-blue-gold: nominal10MΩ±5%, not ohmmeter-verified, unsuitable for visible LEDs. Confirm colour/value and obtain two appropriate resistors (target1kΩ brown-black-red-gold). Keep LEDs disconnected. Relays unused. No final pin-by-pin wiring instructions until labels identified; never infer wire colours.
- Android fan implementation completed/host-tested, no firmware/API changes: HardwareRepository device binding across proposal/Confirm/ACK+fresh readback; light cannot satisfy fan, unsupported device rejects, no replay, fan cannot replace manual-light voice proof. UI fan enabled only fresh/known/no active request; full target in confirmation/status; all voice engine/model/.7/parser/preview unchanged. Files listed in report. AUTO still disabled, no silent five-minute policy change.
-70actual-source Kotlin tests PASS1.288s +3privacy PASS0.66s. Build SUCCESS1m53s/83 snapshot files. Offline model/native4ABI/16KB/client-boundary0 keys/signature checks PASS. Initial apksigner unsetJAVA_HOME failure preserved; corrected using existing JDK21. New laptop APK deliverables/ClawGrid-Local-LED-debug-20261027a.apk,version1.0-local-led-20261027a/code1/100112968B/SHA365ee5e60193b6bd70ce6aa237ba0587a64b495ba09393f2f398cd517ce53ebe. Snapshot C:/Users/hares/Documents/ClawGrid-Android-build-local-led-20261027a. Debug cert71ce1bf6... unchanged/release unchanged.
- **APK installed/verified:** initially no device, then unauthorized; after user authorization adb install-r SUCCESS. Pulled installed base SHA MATCH365ee5e6...; dumpsys1.0-local-led-20261027a/code1 verified; app opened. No uninstall/data clear. Prior previewc preserved. Token/manual-light proof are memory-only and cleared by update; reapply privately after wiring/power context stabilizes, then test inhibited manual fan. No raw transcript/UI dump or recording.
- Evidence benchmarks/review/local-led-20261027/: before/,firmware-source-before.json,board-identities.json,installed-firmware-verification.json,fresh-runtime.json,JVM/privacy/build/artifact logs. No phone recordings, actuator commands, firmware flash, cloud change or publication today at checkpoint.

**Immediate next:** confirm labels/resistors, provide final GPIO/power table, guide PIR only with both ESP USB power disconnected, then observe actual warmup/filtered motion on identified A. Implement confirmed temperature driver/versioned validity/error/age telemetry outside critical control tasks and deploy compatible inhibited pair with guarded app-only rollback. New fan APK installed/hash-verified; fresh manual-light proof and manual fan inhibited proof pending. Then explicitly resolve manual-untilAUTO vs5min/HOME-AWAY/per-deviceResume semantics, inspect LED circuit/polarity before output-enabled LED test, demonstrate local policy/recovery, brief unchanged offline voiceOFF/ON/unrelated check and package actual results. No appliance switching claim; no guessed sensor wiring. More detail/remaining priorities in report. Existing baseline, credentials and dual private backups preserved.

---

# PAUSED FOR LAB FINALS — 25 October 2026 IST

**Do not work until the user returns/resumes Tuesday, October27. Target complete one-room local prototype Wednesday, October28, India time. No automatic resume, tests, recordings, builds, investigations, flashing or wiring while paused.** Only pause documentation was changed. Host process inspection found no flash/build/serial worker; owned serial handles were already closed. No active flash needed finishing. Safe to unplug both USB-powered ESPs. No new board probe was started for the pause.

## Installed baseline and last verified state

- **Phone/laptop APK:**deliverables/ClawGrid-Phone-Preview-debug-20261025c.apk;1.0-phone-preview-20261025c/code1/packagecom.aistudio.clawgrid.v2edge;100,112,968B;SHA256 **29c295701e6ba7235f4265b960f45602915968da2e7efafa27185f6cbb876a87**. adb installed-base pull/hash MATCH; same debug certificate/release signing preserved. Source snapshot C:/Users/hares/Documents/ClawGrid-Android-build-preview-20261025c; JDK21/SDK36.1/buildtools36.1.0/wrapper9.3.1.
- **A:**MACe0:72:a1:f5:bc:b0, lastCOM10/CH3435C37271524, last observed boot21. Last recorded installed normal inhibited imageSHA **8f028bd90f65a55a72b09f4b886db7a3fa851f8a2108c6b79659136db8713cdf**, unchanged by current phone work; historical identity, not newly read during pause.
- **B:**MACe0:72:a1:f6:a9:68, lastCOM9/CH3435C37271527, normal inhibited/default-fault UART appSHA **97daa84e9865a0df09c550387051d401b6d7cc98f547c808bd6734b13a35cbc4**,1,213,232B. Full original modelSHA **49b9215a315a06e900e1a2f4538b2513611253cc30a243802a5a58aedf9dbc7e**, systemd28eefba296d74ff2d0980f05fed1e5ee717f6161ff6be3a365ee297c55a5d01 last verified before/after prior app-only deploy. No firmware/model write in preview/pause work. ESP speech-model investigation CLOSED; preserve evidence/source/models.
- **Network/local speech:**ClawGrid-Assistant/http://192.168.4.1/channel1 matching ESP-NOW, editable URL. Token memory-only/re-enter privately after app restart. Vosk0.3.75/JNA5.18.1/bundled small-en-us0.15/archiveSHA30f26242...; unrestricted graph/0.7each-word gate; no Android default/cloud/PC recognition. Recognition now on phone, embedded control remains on ESPs.
- **Last verified outputs, not a fresh pause measurement:**boot21/lastcommand4/sample1,621,000ms/age459ms/lightOFF/fanOFF/**physical_outputs_enabledfalse**; ACK4applied/status0/RTT19,047us/processing35us. LinkTask324697jobs/watchdogtrue/0sampled misses/AP192.168.4.1/channel1/1client. GPIOs INHIBITED, sensors/relays/loads disconnected. Physical switching NOT tested.
- **Results:**manual OFF1 and cleanup OFF4 matched ACK/fresh readback; user-correlated voice ON included explicit user Confirm plus ACK3/fresh ON report. Informal repeated speech attempts/unknown count, command2 and nonce→request trace unverified. Recognition substitutions remain; no accuracy/controlled ON-OFF/negative-control pass. All6canonical phrases parse and native model word lookups succeed; no normalization/stale-list/OOV mismatch demonstrated. Do not add recognition-error aliases/lower0.7 or restart ESP/model experiments.
- **Preview/privacy/host evidence:**exact final text, actual normalization, phrase list, rejection reason and Dismiss in existing panel; foreground RAM only/clears on Dismiss/new recording/context/background/expiry, redactedtoString/FLAG_SECURE/pipe-only UI inspection. Never save raw transcripts/UI XML/screenshots to disk/log/cloud. Automated raw/normalized pair wasn't captured while visible.66actual-source Android cases+3privacy cases PASS; build2m12s/model/signature/key/alignment checks PASS. Evidence/report: benchmarks/review/phone-preview-20261025/ and docs/PHONE_TRANSCRIPT_PREVIEW_20261025.md.

## Resume order and October27–28 target

Read the detailed prioritized two-day table in the **master pause block** first; it supersedes the older next-step text below.

1. **Tuesday P0:**on explicit resume, re-enumerate and MAC-verify both boards (COMs/boot may change), verify fresh inhibited OFF/OFF/advancing tasks/AP/channel/watchdog and installed app. Discard old requests; never replay ON. Inventory actual hardware/photos before choosing driver/pins. Do not repeat a command merely to get its ID.
2. **Tuesday P1:**finish fanON/OFF and independent per-device AUTO through proposal/Confirm/ACK+fresh readback. Explicitly resolve existing five-minute manual hold/HOME/AWAY against user's until-selected-device-AUTO requirement; document persistent/reboot semantics before changing policy. Preserve safe bootOFF/no oldON restoration, dwell/max-run/fault guards and unrelated devices.
3. **Tuesday P2, bounded:**review shared room/device command grammar/unknown handling without changing models or inventing aliases. Preserve rollback/0.7/confirmation/boot/expiry. Only after configuration review, freeze a separately reviewed finite acceptance set (max8:OFFcancel/OFFconfirm/ONconfirm/OFFconfirm/silence/noncommand/negation/conflict), first-failure stop/no retries/tuning. This pause plan does not authorize new recordings by itself. Do not let another recognition loop consume the entire schedule; mark a failed voice gate honestly and continue independent software.
4. **Tuesday P3/P4:**timers/scenes through SafetyKernel with real partial/dwell/timeout reporting; reconnect/stale/expiry/reboot cancellation for every type, no ON replay. After exact sensorID, actual temperature driver +versioned validity/freshness telemetryA→B→Android; temperature monitoring only. Reuse existing PIR startup/filtering. Prepare confirmed low-voltage BOM/pin/power map. Focused tests/builds only after relevant changes.
5. **Wednesday P0:**power-off wiring one component at a time:identified PIR→temperature→relay interface. Verify rails/logic/pins/startup/fault reports before combined operation. Remain inhibited with loads disconnected through initial interface/polarity review. No mains.
6. **Wednesday P1:**only after actual relay/supply/low-voltage load validation and explicit agreement on the guarded output-enabled bench phase, test light then fan physically. Never enable outputs just to meet the deadline. Otherwise deliver inhibited software and mark physical switching pending.
7. **Wednesday P2/P3:**integrated offline motion AUTO/manual-until-AUTO/temp-only monitoring/timers/scenes/approved voice and disconnect/reboot/expiry/no-replay/RTOS-watchdog/bounded queue checks; controller must automate with app/Internet absent. EndOFF/OFF; package real installed APK/source/BOM/wiring/setup/results/remaining blockers. The two-day target depends on parts and safety gates; never label unobserved features complete. Supabase/home-scoped cloud and additional rooms stay later backlog/local operation independent; no deployedDB changes for local preparation.

## Hardware information still required — collect on return, not during finals

- PIR exact module/model/photos/pin labels/supply and OUT voltage/retrigger-delay settings.
- Temperature exact sensor/module/photos/interface/address/supply/pullups; bare part versus breakout. Do not assume a model or fabricate readings.
- Relay/interface exact model/photos/labels/channels/module and coil supply/trigger3.3V compatibility/active polarity/opto-JD-VCC arrangement/startup behavior. Existing active-low assumption is unverified; determine grounding/isolation from the actual module.
- Light/fan **low-voltage** ratings, running/startup current, polarity/connectors and regulated power supplies; actual driver/protection components required. No mains AC stage.
- Available multimeter, suitable power rails, wiring/terminals/resistors/level-shifter/driver parts; reconcile saved pin map with physical ESP board GPIO labels and reserved flash/PSRAM/strap/USB/UART pins. Current runtime console uses CH343/UART; identify byMAC, not connector/cable position.

## Preservation and pause boundary

Credentials/provisioning/NVS/partitions/models/signing/working source/baselines/failed evidence remain in place. Two private verified backups: benchmarks/backups/mn7-20261025T072239Z and C:/Users/hares/Documents/ClawGrid-private-backups/mn7-20261025T072239Z. Preserve both; app-only rollback exists. Do not erase, regenerate keys, overwrite sole backups, migrate/train models or mutate cloud to meet the target. app api_token/AP ap_password values stay private.

**Next executable action is Tuesday P0 after human resume. Until then STOP.** Both ESPs may be unplugged; no task/reminder/automatic resume was scheduled. Older checkpoints below retain historical results and are superseded by this pause block.

---

# Previous handoff — transcript preview installed; parser audit consistent, 25 October 2026 IST

Read master top and docs/PHONE_TRANSCRIPT_PREVIEW_20261025.md before changes. User authorized ONE transcript diagnostic, no ESP firmware/model experiments. Implemented preview and installedc. User then made multiple informal voice ON/OFF attempts, so the controlled single-recording procedure was not completed. Exact count/prompts unverified; do not call an accuracy/formal acceptance pass. Stop further recordings. All outputs INHIBITED; sensors/relays/loads disconnected, wiring deferred.

- **Installed/built APK:**deliverables/ClawGrid-Phone-Preview-debug-20261025c.apk,1.0-phone-preview-20261025c/code1,100,112,968B/SHA29c295701e6ba7235f4265b960f45602915968da2e7efafa27185f6cbb876a87. adb install-r SUCCESS/pulled-base hash MATCH. Source snapshot C:/Users/hares/Documents/ClawGrid-Android-build-preview-20261025c (83files byte-identical). Prior builds a/b and ESP source/model/failed evidence/dual backups preserved; no firmware build or flash this turn.
- **Preview/privacy:**PhoneVoicePolicy returns a redacted-toString foreground memory object with exact final segments, actual normalized text, reason enum/static detail. PhonePushToTalk renders it plus all6allowed Bedroom/light phrases from the same parser table and Dismiss. Clear on Dismiss/new recording/room/tab/mode/gateway/background/stale/reboot/60s expiry. No SavedStateHandle/Room/preferences/disk/log/cloud transcript writes. FLAG_SECURE protects this activity's ordinary screenshots/recents capture. No audio retention/upload/default recognizer/cloud/PC speech fallback.
- **Inspection privacy:**Use scripts/phone_ui_memory.py (adb exec-out uiautomator dump /proc/self/fd/1, RAM pipe ONLY) and inspect_phone_preview_memory.py, which emits only non-transcript comparisons/public vocabulary IDs/typed command metadata. Existing phone_offline_snapshot.py now also uses the pipe. NEVER create an XML dump/screenshot/recording or put raw/normalized transcripts in logs/master/chat. User voluntarily described errors; do not reproduce their verbatim words in files. Current actual RAM raw/normalized comparison wasn't captured while visible; don't pretend it was.
- **Configuration audit:**Vosk Android0.3.75/JNA5.18.1/small-en-us0.15 unchanged, unrestricted Recognizer(model,16000f),setWords(true),all final segments/final flush,0.7per-word gate. No separate configured six-phrase grammar to drift. Documented C API vosk_model_find_word on the actual phone's loaded model returned bedroom101349/light4326/off107349/on106591/the152188/turn58832. All6UI phrases displayed and all6canonical mappings host-tested. Word support is not acoustic-accuracy proof. No OOV/stale-list/normalization defect demonstrated; no aliases/threshold/model/grammar changes.
- **Checks:**66actual-source JVM tests PASS1.431s;3synthetic privacy/pipe cases PASS0.65s. Android build SUCCESS2m12s/25tasks+14cache; model14file/native4ABI/64bit16KBELF+ZIP/signature/private-value0/privileged-field0 PASS. Same debug certificate71ce1bf6..., release signing untouched. No unchanged firmware test/build campaign.
- **Hardware identities:**fresh CH343+ROM matches A COM10/e0:72:a1:f5:bc:b0/serial5C37271524; B COM9/e0:72:a1:f6:a9:68/serial5C37271527. Re-enumerate after reconnection; don't assume ports. Normal read-mac resets advanced A boot20→21. No firmware write. B remains normal inhibited app97daa84e... and original model49b9215a... from prior verified flash; no fresh model dump claimed. Both firmware core source hashes/private credentials/header/partition hashes unchanged.
- **Actual phone/board observations:**private token re-entered after update; manual OFF1 ACK+fresh OFF readback on boot21 restored gate. User reports multiple recognition substitutions/intermittent success. Latest captured metadata ACCEPTED/capture2822226236/boot21/2words/displayed minimum confidence1.000. User explicitly says ON came from voice and they tapped Confirm. Gateway appliedACK3/status0/lighttrue/oneattempt/RTT17,696us/processing34us plus fresh boot21 sample1,328,000ms/age769ms/last3/lightON/outputsfalse. User correlation, not captured nonce→request trace; command2/counts unknown. Cleanup OFF4 appliedACK/status0/RTT19,047us/processing35us plus final sample1,621,000ms/age459ms/boot21/last4/OFF/OFF/outputsfalse. LinkTask324697jobs/watchdogtrue/0misses/AP192.168.4.1/channel1/1client; mobiledata0/APClawGrid observed. Physical load NOT measured.
- **Finding:**reported raw-word substitutions are ASR errors; strict rejection of unlisted text is expected parser behavior. No normalization mismatch established. Informal voiceON+explicitConfirm+ACK/readback chain observed; reliability/controlled ON-OFF/negative and negation behavior remain unverified. Don't infer accuracy from retries, turn sound-alikes into actuator aliases or lower0.7. Existing manual-hold/HOME/AWAY mismatch remains unchanged/pending.

**Next narrow source review if continued:** consider an opt-in room/device command grammar generated from the same canonical list with explicit unknown/non-command handling, preserving unrestricted rollback and defining negative/negation safety checks before activation. Not implemented or authorized as a new recording/model campaign here. No more speech, firmware or wiring action queued. Evidence benchmarks/review/phone-preview-20261025/; frozen experiments/phone-transcript-preview-v1.json plus execution.json distinguish requested1 from actual informal multiple. Serial observers closed/no worker. No deployed database or publication changes.

---

# Historical pause — 25 October 2026, 15:30 IST (superseded)

Both ESPs will be disconnected for about3hours. **Wait for the user to return; no automatic/timed resume.** Safe point reached: no active flash write, Python/esptool worker or owned serial connection. Source/documents/evidence saved locally; no new build/test/probe/flash started for the pause. This pause does not reopen the closed phrase experiment.

Last completed state below: exact B baseline appf30b10cf.../fullmodel49b9215a.../system verified restored; A boot18/OFF/OFF/last_command0/outputs inhibited at last observation. LIGHT ON reference failed/no accepted ID/error0/PCM MATCH; OFF/silence/noncommand NOT RUN; no phone/proposal/control command. Private backups, credentials, NVS/model assets and all failed results preserved. No live hardware claim after unplugging.

Resume only on user input. First read master/handoff/report; before future authorized hardware activity re-enumerate ports, verify MACs and fresh inhibited/OFF telemetry, and discard previous boot/request context. Do not assume COM9/COM10 persist or replay old ON. Remaining architecture/one-room software review is pending; no speech/model/wiring campaign authorized by return alone.

---

# Last completed handoff: LIGHT ON/OFF preflight FAILED; closed, baseline restored — 25 October 2026 IST

Read the current master top and [LIGHT_REFERENCE_PREFLIGHT_20261025.md](docs/LIGHT_REFERENCE_PREFLIGHT_20261025.md). Latest authorization was ONLY4reference clips, not40phone recordings. Actual run stopped after first LIGHT ON failure:TIMEOUT198/no accepted candidate, error_code0/engine_errors0, PCM23,280samples/CRC1936402414 MATCH. Raw result count0/candidate_id0; accepted score unavailable, not a measured0likelihood. Untrusted diagnostic_text LIGHT ON was not treated as accepted recognition. LIGHT OFF/silence/non-command NOT RUN. No proposals/commands/phone clips/fixture replacement/threshold change/decoder investigation. Close this phrase experiment; no further speech work queued.

Frozen narrow manifest experiments/light-on-off-reference-preflight-v1.json SHA256ccf13855ed05da117b3d4d8ecbe92cb89007db454bd9ec92b0aef000b4fe9104. ID1LIGHT ON/ID2LIGHT OFF, existingMN6/raw/0.7 unchanged; fixed System.Speech Zira/rate0/volume100/16kHz mono PCM16 positive fixtures prepared once, earlier exact negative files reused. Explicit Bedroom selected in CLI and bound to A boot18/capture1/screen_onlytrue. This was not Android room-selector/phone validation. Candidate endpoint/firmware contract permits only4fixed fingerprints in order, no fifth/reference replay/nonreference audio; proposals/control dispatch blocked.

Fresh ROM identities: B COM9/CH3435C37271527/MACe0:72:a1:f6:a9:68; A COM10/5C37271524/MACe0:72:a1:f5:bc:b0, both match provisioning. Identity reads normal-reset apps; A now boot18, not flashed/commanded. B model/system/baseline app verified before write. All boards remain user-reported bare, loads/sensors disconnected and outputs inhibited.

HOST:10focused checks PASS47.65s; candidate build SUCCESS180.883s. Candidate image1e912904ac14db2c7b699f5f0bc60e7d71c2ae813e81d29e02e16832618e94ce app-only normal inhibited/default-faults. Guarded deployPASS manifest benchmarks/raw/hardware/flash_speech_intelligence_uart_light_preflight_20261025T082648Z.json; restorePASS flash_speech_intelligence_uart_20261025T082737Z.json. No model/NVS/table/key writes. Cache-independent original bin/ELF restored before guarded app flash. Final ROM verify-flash: original application prefixf30b10cf.../fullmodel49b9215a.../system match. Both earlier complete private backup copies remain valid;62pinned/private file hashes unchanged; sealed source/config/fixture hashes unchanged.

Final baseline runtime exit0: authenticated progressing A boot18/sample634,996ms/age880ms/lightOFF/fanOFF/last_command0/inhibited. B AP192.168.4.1/channel1/watchdogtrue/zero sampled misses/originalMN6ready16commands/job0/error0/APclients0. During clip A control/link113,870→114,706, B Link2,537→3,318; transactionid0/phase0 unchanged. All owned serial handles closed; laptop Wi-Fi restored, owned profile/XML removed. No physical switching measured. No new Android source/build/install/publication; historical phone build remains1.0-speech-capture-20261024b, not reverified today.

Evidence benchmarks/review/light-on-off-preflight-20261025/{campaign.json,preflight.json,preservation-final.json,sealed-source-and-build.json,restored-flash-verification/}; campaign deploy0/preflight1/restore0/baseline-runtime0 and BASELINE_FLASH_AND_RUNTIME_VERIFIED. Original broad proposal SHA85488688... preserved; separate status marks it CLOSED_FAILED_REFERENCE_PREFLIGHT. Do not run its40+5phone stages.

Remaining choices: native Android/manual/local-automation software with voice explicitly blocked; keep ESP-only recognition blocked pending an independent supported solution; or explicitly reconsider local inference placement as a separate architecture decision. No new training/model migration/alternate words/host-cloud substitution or experiment initiated. Next is review of remaining one-room software with exact restored baseline; fan/AUTO/policy/timer/scene/sensor/cloud/wiring milestones remain pending.

---

# Historical fixture/G2P review and proposal-only handoff — 25 October 2026 IST

Read [VOICE_PHRASE_AUDIT_20261025.md](docs/VOICE_PHRASE_AUDIT_20261025.md) and the current master top. User asked to preserve baseline, review fixture/G2P evidence and propose ONE conventional pair, without training or another migration. No board access/reset/flash, new audio recognition/recording, firmware/Android change or migration occurred. Previous baseline/hardware receipts below remain historical last-verified evidence; no new fresh telemetry claimed.

User played the exact retained LIGHT-DISABLE.wav and confirmed “Complete intended phrase is audible.” Assistant direct audio input is unsupported and was not claimed. Host checks:16kHz/mono/S16LE,27,200samples/1.7s,PCM SHA462f3302696f7833e01dd96b37dff01fd229067310162837d8d0a03cce5b3bcb/CRC3370348365,zero clipped samples,last nonzero0.9698125s/~0.73s trailing zeros. Matches both retained B model runs; no resynthesis/cropping/gain change.

Unchanged official pinned multinet_g2p.py Gitblob26b0bff07cd65ae113220d5774e98008b663ac6e ran locally in isolated audit tools. DISABLE LiT DgSdBcL and ENABLE LiT fNdBcL exactly match retained MN7 registration. LIGHT ON tooloutput LiT nN; LIGHT OFF LiT eF. Tool source/API confirms grapheme input/internal FLITE/command update, not evidence of a missing or wrong ID2 registration. Local G2P is not product speech recognition/training. Evidence benchmarks/review/voice-phrase-audit-20261025/{official-g2p-results.json,phoneme-comparison.json,fixture-audit.json,exact-phrase-search.json}.

Decision: complete audible fixture and matching phonemes do not prove why DISABLE is rejected. Same Zira clip was reused on MN6/raw and MN7/AFE, so these are not independent pronunciation trials. Controls pass; historical actual-phone job8 with a different full-room phrase also fails. Neither phrase/fixture defect nor universal model limitation is established. No DISABLE repeat/decoder loop or confidence relaxation.

Exact-phrase search:3,760retained text/result/source files and separate100,670,524Bflash log; no retained test found for exact LIGHT ON / LIGHT OFF. Manual payloads, host parsers, bedroom light on PC/Vosk fixtures and TURN OFF BEDROOM LIGHT are different evidence; do not count as exact-phrase ESP/phone passes.

ONE proposal: LIGHT ON→Bedroom/light/ON; LIGHT OFF→Bedroom/light/OFF, explicit selected Bedroom frozen with boot/nonce at PTT start. Use current restored MN6/model/raw frontend/0.7; no model migration. Future reversible app-only grammar and Android explicit-room/bounded ledger changes are needed and **not implemented**. Proposal experiments/light-on-off-proposal-v1.json SHA85488688e7cfbb03d37f3b78c44e2abba2df598bd031ef8af7024fa50f7174aa, PROPOSED_NOT_IMPLEMENTED_NOT_RUN.

Fixed scope if subsequently selected:4reference clips(two correct positives>=0.7 +existing silence/noncommand negatives), stop first failure; only if pass,40actual-phone clips(10each positive across2sessions +20fixed negatives),pass>=9/10each and zero wrong/negative accepts. Stop wrong/negative acceptance,second miss perphrase,transport/engine/health/output/reboot error,hard cap40/no repeats/tuning. Only after screen pass,at most5normal-app clips:OFF→ON→OFF with correct visible target/explicit confirmation/matching ACK/fresh readback/>=3s dwell plus silence/negation no actionable proposal.49total max. No usability claim without actual phone/negative/control-chain pass; outputs inhibited/loads disconnected. No current audio campaign queued.

Next is that single proposal rather than DISABLE investigation; manual Android remains the working base. Preserve all previous failed plans/results and exact rollback. No extra model/training/migration, source publication or hardware wiring implied. No serial/build/flash worker active.

---

# Historical bounded MN7 handoff and exact baseline restoration — 25 October 2026 IST

Read the current master top and [MN7_EVALUATION_20261025.md](docs/MN7_EVALUATION_20261025.md). The user approved this separate evaluation. It is **closed**: official reference0.75179/ID17 and LIGHT ENABLE0.85891/ID1 passed; mandatory LIGHT DISABLE TIMEOUT198/no accepted candidate/error0 failed. Exactly3of5maximum target reference jobs once; source/ESP samples+CRC matched. First-failure stop means silence/non-command controls and all phone/proposal/confirmation/ACK tests were NOT RUN. No negative false-accept/phone accuracy/physical-switching claim. Do not start another recognition-debugging or recording loop.

Implemented separate MN7 assets/AFE runtime plus guarded B-only backup/deploy/rollback. Official pinned1.7.0/commit07b0a4d0... support verified. Public API takes lowercase graphemes and performs internal FLITE G2P; captured actual phonemes ID1 LiT fNdBcL, ID2 LiT DgSdBcL. Independent MN7 threshold audit found setter unit/clamping bug. Resolved before target data via constructor-native JSON det_threshold=ln(0.7), same-length JSON-only artifact change; acoustic/index/outside-JSON bytes unchanged, final gate0.7. No MN6 header/private offset access/binary patch/threshold-setter call. Frozen plan experiments/mn7-eval-v1.json SHA3039cf68c3f6e6d15260f37cab2432e9e499cd32719e3634e6e6fa9ebc671b7d. Original blocked preparation plan preserved. No tuning after failure.

Current ports: B COM9/CH3435C37271527/fresh ROM MACe0:72:a1:f6:a9:68 matched. A COM10/5C37271524, existing provisioned authenticated e0:72:a1:f5:bc:b0 link, boot17. A was NOT reset, flashed or commanded. Re-enumerate after reconnect. Two bare boards; sensors/relays/loads user-reported disconnected, outputs inhibited.

**Current B restored original baseline**, prefixf30b10cfb74a4dd79ff28588de2d3be9f6c760c934b48cf1475bf6f7c271b9a4; full app0fdabd4bc40c6cbb2ae3fe20daad638c23c83105f16cb2c09226281104053e249; full6MiB model49b9215a315a06e900e1a2f4538b2513611253cc30a243802a5a58aedf9dbc7e. Full board verify-flash after rollback PASS; final runtime exit0 sample3,083,998ms/age265ms/A boot17/OFF/OFF/last_command0/inhibited; B AP192.168.4.1/channel1/watchdogtrue/zero sampled misses/MN6ready16commands/job0/error0/APclients0. Model7 app23c223b9... and packf07b20e7... are NOT installed. Prefix/full-app hashes differ because the3MiB backup includes unused tail.

Verified private backup copies: benchmarks/backups/mn7-20261025T072239Z and C:\Users\hares\Documents\ClawGrid-private-backups\mn7-20261025T072239Z. Includes full system/table/NVS/OTA/app0/model plus private provisioning snapshots. Never publish/delete/overwrite them. Preferred co-pack6,539,556B exceeds unchanged6MiB bank by248,100B; MN7-only pack2,758,776B fits, so deployed using explicit dual-backup/single-bank strategy, then restored. Guard checks fresh B MAC/current table/system/app/model, bounds writes to app/model, verifies readback and protected0..0x10000 bytes. Original blank-model guard intact. No erase-chip, NVS/table/bootloader/OTA write or credential regeneration. Scripts preserve current normal boot counters. Both backups and all62pinned/private-input hashes verified after rollback.

HOST:16focused checks PASS1.62s (initial14pass before2added recovery/path checks). Build first FAILED99.150s on misleading indentation; corrected build SUCCESS114.474s. Actual target AFE+recognition job times2.282180/1.324723/3.703556s, max model frame24.889ms; internal minimum41,367B/largest36,864B, post-job PSRAM5,289,375B, stackreserve2,672B. Peak AFE PSRAM not retained. A control/link580,537→582,472, B Link3,136→4,989/watchdog/zero sampled deadline misses. Bounded observations, no soak/performance guarantee.

Evidence benchmarks/review/mn7-eval-v1-20261025/campaign/evaluation.json: deploy0/preflight1/rollback0/baseline-runtime0, FAILED_STOP_NO_PHONE_COLLECTION, FLASH_AND_BASELINE_RUNTIME_VERIFIED, serial_closedtrue. Backup subdirs deploy-20261025T073700081471Z and rollback-20261025T074217410430Z contain exact operation logs. Current deployment-candidate.json is CLOSED_FAILED_PREFLIGHT; immutable as-deployed receipt retained separately. Serial/Wi-Fi cleanup complete; no active build/flash/test worker.

Android unchanged; no APK build/install or phone recording today. Last independently verified installed24October1.0-speech-capture-20261024b/SHA5c932d62... remains historical, not freshly reverified. No new GitHub/Downloads publication. Both MN6 experiments/results preserved and closed. New explicit-room/screening Android UI remains unimplemented because mandatory preflight never passed.

**Next:** retain manual native Android control and review remaining one-room software with voice blocked; only reopen speech for a separate bounded evidence-backed/vendor-supported decision. No repeat clip, new phrase/model search, threshold change, host/cloud substitution or bypass. Policy/manual-AUTO/HOME/AWAY mismatch, fan/timers/scenes, sensor models/wiring, routing/cloud milestones remain pending. Wiring remains deferred.

---

# Historical short-v1 handoff before approved MN7 evaluation — 25 October 2026 IST

Read the current top of CLAWGRID_MASTER_INTEGRATION.md and [VOICE_SHORT_V1_PREFLIGHT_20261025.md](docs/VOICE_SHORT_V1_PREFLIGHT_20261025.md) first. The user approved the exact short-command experiment. Its mandatory reference preflight failed: ENABLE accepted0.73673/ID1; DISABLE TIMEOUT198/no accepted candidate; silence and fixed non-command rejected. Exactly4synthetic/reference jobs once on ESP, source/ESP sample+CRC MATCH; no phone clips, proposals, confirmations or commands. Do not repeat the full-room investigation, tune threshold/phrases, or restart this failed experiment.

Implemented only the firmware/preflight stage before the early stop: explicit speech_intelligence_uart_short_v1 environment; normal inhibited/default-fault marker; --voice-experiment short-v1 flasher guard; two fixed IDs; explicit Bedroom/boot/capture/expiry checks; screening jobs barred from proposal/confirmation; frozen40-screen+5post-pass plan and non-authorizing4fixture harness. Android explicit-room/screening UI/ledger was **not implemented/built/installed** because preflight failed first. No end-to-end phone-voice success.

Fresh identities: A COM10/CH3435C37271524/MACe0:72:a1:f5:bc:b0; B COM9/5C37271527/MACe0:72:a1:f6:a9:68. Verify again after reconnect. A now boot17, unchanged last-known inhibited image8f028bd9.... B restored exact baselinef30b10cfb74a4dd79ff28588de2d3be9f6c760c934b48cf1475bf6f7c271b9a4 via fresh-MAC-guarded app-only flashT070448Z. Experimental437917d7.../T070249Z is NOT installed now. Source snapshots/bin/ELF and experiment image retained under benchmarks/review/voice-short-v1-20261025/. No NVS/model/partition/key/database writes;62pinned component hashes and private-input hashes unchanged.

Final read-only probe exit0: authenticated advancing sample768,998ms/age124ms, boot17/lightOFF/fanOFF/last_command0/outputs inhibited; AP192.168.4.1/channel1/watchdogtrue/zero sampled misses/speechready/job0/error0/APclients0. Both bare boards only; sensors/relays/loads user-reported disconnected. All owned serial handles closed; laptop Wi-Fi restored. No physical sensor/load result.

Checks: firmware SUCCESS165.775s after fixing exact-name CMake UART-default selection;8focused host cases PASS31.73s after selecting a workspace pytest temp root. Initial build82.322s FAIL and6pass/2temp-root setup errors retained. First rollback stopped before USB/write because PlatformIO removed the old build-cache image; restored the saved hash-verified original bin/ELF and guarded flash PASS. No extra recognition campaign after failure. See detailed report for exact commands/logs.

Android unchanged: last independently verified installed24October1.0-speech-capture-20261024b/SHA5c932d62.... ADB today showed no phone. Do not confuse a saved laptop APK with fresh installation evidence. No APK rebuild, new phone clip or GitHub/Downloads publication.

**Next:** a separately reviewed mn7_en readiness/migration audit, not phone trials or Milestone2/physical wiring. Pinned1.7.0/S3 support verified; required assets absent. Exact requirements in report: separate pinned assets/G2P/phoneme registration, official frontend and frame checks, independent threshold-unit verification without MN6 private-layout accesses, measured resource/RTOS budgets, fresh6MiB model-partition backup/hash and reversible guarded migration plan. Do not weaken existing nonblank-model guard or silently install another model. This task stops at the failed prerequisite as the user required. Remaining one-room policy/fan/AUTO/timers/scenes, sensor models/wiring and cloud work stay pending. No active build/flash/test/serial worker.

---

# Historical decision before the approved short-v1 experiment — 25 October 2026 IST

The user requested a finite decision, not continued recognition research. The bounded BEDROOM investigation is complete. **No evidence-backed implementation fix is supported. Treat MN6 with the current spoken full-room phrases as unsuitable for this prototype's intended voice experience.** This is a provisional engineering acceptance decision, not a general model/accuracy or accent conclusion. No further full-phrase instrumentation, repeated phone recordings or parameter search is scheduled.

Decision, confirmed/ruled-out/uncertain findings, tradeoffs and a finite proposed experiment: [VOICE_DECISION_AND_BOUNDED_EXPERIMENT_20261025.md](docs/VOICE_DECISION_AND_BOUNDED_EXPERIMENT_20261025.md). The alternative is a proposal only; room-selection architecture, vocabulary and model assets have not been changed.

## Current concise milestone checklist

| Category | Actual status |
|---|---|
| Implemented | Existing native Android/local PCM/ESP recognition/proposal-confirmation/SafetyKernel pipeline and bounded diagnostic baseline preserved. No new persistent product-code or architecture change in this bounded decision task. |
| Host/build verified | One temporary dense-sampler build SUCCESS47.989s; planned trace capacity<=48 and actual17/17frames per window verified. Original source/image/ELF hashes restored; private inputs and all62pinned component hashes unchanged. Prior10portable/49Android results are historical and were not rerun unnecessarily. |
| Board verified | Exactly2saved clips replayed once: phone8 still no detection; Zira accepted0.86229. Dense frames show phone TURN OFF0.915→BE0.633→BEDRO0.377; full path0.2945 fails0.7. Guarded baseline restoration succeeded; A boot16/OFF/OFF/last_command0/inhibited, advancing authenticated telemetry/channel1/watchdog active/zero sampled misses. |
| Awaiting sensor/load connection | Only two ESP boards connected. No physical sensor/relay/load switching or additional-room/cloud result claimed. Hardware wiring deferred. |
| Blocked / decision | Current full-room MN6 voice is not accepted for the prototype. Voice-confirmation chain, ON→OFF and phone negative UI check remain unverified. Next is review of one finite alternative experiment, not continuation of the old debugging loop. Other software/policy milestones remain pending. |

## Findings and baseline receipt

Confirmed: earlier probability/log-unit defect was repaired without changing the0.7 gate; reference controls pass. Actual phone8 has matching complete PCM and the correct completed command2 FST token sequence. The model's low score is the measured rejection mechanism. Dense capture localizes the loss at BE and subsequent BEDROOM expansion; scores are cumulative path scores, not phoneme posteriors. Captured competitors provide no supported registration/configuration fix. Transport/format loss, missing FST/command binding, merely longer inference, fresh-model reset, startup indication, tested AFE/smaller grammar and BED ROOM spelling do not sufficiently explain/fix this retained failure. Detailed caveats and remaining acoustic/model uncertainties are in the decision report; no accuracy claim from a few clips.

Bounded run evidence: benchmarks/review/speech-decision-20261025/bedroom-token-analysis.json, phone8-dense.json, zira-dense.json, run-status.json and preservation-final.json. Exactly phone64–80 and control40–56 were traced at every frame; no new phone clip, proposal, confirmation or control POST. Temporary source sampler was restored byte-for-byte. Dense image and logs remain private review evidence.

**Restored installed B baseline** normal inhibited/UART0 SHA256f30b10cfb74a4dd79ff28588de2d3be9f6c760c934b48cf1475bf6f7c271b9a4, fresh-MAC-guarded app-only restoration manifest benchmarks/raw/hardware/flash_speech_intelligence_uart_20261025T063431Z.json; temporary dense manifestT063337Z. B MACe0:72:a1:f6:a9:68/COM9. A unchanged, last-known image8f028bd90f65a55a72b09f4b886db7a3fa851f8a2108c6b79659136db8713cdf/MACe0:72:a1:f5:bc:b0/COM10, boot16. No model/NVS/partition/credential/database writes or output-enabled/diagnostic-fault image. Last readback sample3,823,997ms/age78ms/lightOFF/fanOFF/last_command0/outputs inhibited; AP192.168.4.1/channel1/watchdog/zero sampled misses/speech ready/job0/error0/AP clients0. Serial handles closed; laptop Wi-Fi restored. Physical switching untested.

APK unchanged: last independently verified installed1.0-speech-capture-20261024b/SHA2565c932d6299a3e8bb76f4098fc685b8273f553894a91fadcf561794db00d174b4. No APK build/install or new phone-version observation in this task. Original baseline/evidence, models and existing GitHub/Downloads publication preserved.

## Proposed next experiment — not started

Recommend explicit Bedroom selection in the Android app plus a frozen two-phrase vocabulary: LIGHT ENABLE / LIGHT DISABLE. This intentionally changes how the room is supplied and must be reviewed before implementation. Selected room/controller boot must be frozen at PTT start and included in the complete validated proposal; missing/changed/stale context cancels. Existing explicit user confirmation, job/boot/expiry binding, matching ACK/fresh readback, dwell and no automatic ON replay remain mandatory. Only the provisioned Bedroom is eligible; additional rooms stay unverified.

Freeze vocabulary/settings/criteria before collecting fresh data; no cropping/tuning to phone8. If preflight fails, stop before phone collection. One recognition-only batch:10fresh examples per phrase across2sessions and20fixed silence/background/non-command negatives. Pass>=9/10per phrase, zero wrong accepted targets/directions, zero accepted negatives at unchanged0.7. Stop immediately on any wrong acceptance/false positive or when a second miss makes a phrase's9/10unreachable; maximum40clips, no retuning or retries. If it passes, at most5normal-app clips for OFF→ON→OFF with explicit confirmation/ACK/readback and two negative UI checks, always inhibited and obeying>=3s dwell. Any error stops; no automatic replay. Counts demonstrate limited feasibility, not production accuracy.

If the experiment fails, close MN6 short-command feasibility rather than starting another phrase search. Supported mn7_en in pinned ESP-SR1.7.0/ESP32-S3 is a separate candidate experiment, not a promised improvement/drop-in replacement: verify phoneme registration, threshold semantics, resources and reversible asset deployment while preserving the current model pack. Never reuse the MN6 private-layout adapter blindly. No alternative model or room-selection architecture has been implemented, deployed or started. The old next-step token-debug instruction is superseded by this decision.

---



# Current result: reference fixed; actual phone recognition still blocked — 24 October 2026 IST

**End-to-end phone voice has NOT passed.** The pinned MN6 threshold-unit repair is verified with the official reference and normal synthetic OFF control. Actual phone job6 failed on APK69d95b55; retained phone job8 failed on the new installed APK5c932d62 despite complete matching PCM and the user's clear full-phrase playback report. Replaying that exact saved clip through raw, ESP AFE and two-command diagnostic grammars still yields TIMEOUT/no accepted candidate/error0. Known Zira OFF control continues to return command2/confidence0.86229. None of these diagnostic replays issued proposals/confirmations/control. Do not request another phone repetition without a tested correction; do not lower0.7 or claim accuracy/physical switching.

Full actual results, files, exact checks and next step: [phone clip diagnosis](docs/PHONE_CLIP_DIAGNOSIS_20261024.md). [Earlier threshold repair evidence](docs/VOICE_THRESHOLD_FIX_AND_PHONE_TEST_20261024.md) remains preserved.

**Verified separately:** manual OFF request1 and, after the necessary APK update, request2 each received matching ACK plus fresh inhibited OFF readback. IDs were captured automatically. Latest manual2 ACK status0/attempts1/RTT17,275us; A boot14/last_command2/OFF. Phone job8:32,000samples at16kHz mono S16LE, CRC679739021/SHA78d4ceff..., source/ESP MATCH, first buffer127ms, peak17,472/no clipping, TIMEOUT198frames/no proposal. Job7 completion evidence was not retained; do not invent it. Host local Vosk diagnostic text is not product recognition or authorization. The old startup indicator was corrected, but this did not resolve recognition.

**Installed phone APK:** version1.0-speech-capture-20261024b, absolute delivery path C:\Users\hares\OneDrive\Desktop\ESD-imp\ClawGrid-physical-integration-prep\deliverables\ClawGrid-Speech-Capture-debug-20261024b.apk,18,364,818bytes,SHA2565c932d6299a3e8bb76f4098fc685b8273f553894a91fadcf561794db00d174b4. adb install -r SUCCESS; pulled installed base APK hash independently matches. Debug signer71ce1bf6... unchanged; no uninstall/data clearing/release-signing change. Debug-only last-clip private retention is disclosed, bounded and does not export audio; release saving is disabled. Original d12778de, intermediate69d95b55 and current5c932d62 phone evidence remain distinct.

**Latest firmware:** A COM10/MACe0:72:a1:f5:bc:b0 remains normal inhibited8f028bd9.../boot14. B COM9/MACe0:72:a1:f6:a9:68 freshly matched by the guarded app-only flasher, latest normal inhibited/UART0 SHA256b2ae88b9e93a1a37987b9d2047143f0d543271007994302304d45c5d78b3b7cf; manifest benchmarks/raw/hardware/flash_speech_intelligence_uart_20261024T172229Z.json. Only application0x10000 written. No NVS/models/partition/provisioning/credentials/database changes, diagnostic-fault or output-enabled image. Final bounded probe: A sample6,080,999ms/age569ms/OFF/OFF/last_command2/inhibited; B AP192.168.4.1/channel1/watchdog/zero sampled misses. AP clients0 at final observation. B counters reset during deliberate gateway updates: latest job2 is a host synthetic control, not phone8; gateway transaction0 is after reset, not evidence erasing manual ACK2 or permission to replay. All serial handles released. Loads/relays/sensors remain user-reported disconnected; physical switching untested.

**Source/checks:** first-buffer readiness plus debug private clip retention in Android recorder/viewmodel/PTT/new PhoneVoiceDiagnostic.kt and4 tests; debug version suffix changed. Firmware adds exact-fingerprint non-authorizing AFE/two-command diagnostics, restoring normal16 grammar afterward; normal phone path remains raw because AFE did not improve this clip. Host scripts support correct actual-phone input labeling, explicit isolated APK scans and validated fresh build directories. HOST_MEASURED49Android control/voice/retention cases PASS1.142s; latest9portable cases PASS5.23s; final firmware build SUCCESS57.988s, APK SUCCESS16s/39tasks. Earlier build/cache/Windows-lock/JAVA_HOME failures are preserved with fixes; full Robolectric remains blocked. Signature/private-value scan passes; all62pinned component hashes and private credential/header/partition hashes unchanged. Exact commands/logs in the detailed report. No new GitHub/Downloads publication.

**Remaining blocker / next executable step:** identify why pinned MN6 does not accept the clear saved phone clip, using read-only token/FST alignment and best-path qualification tracing at the unchanged0.7 gate, compared with the preserved successful Zira clip. Use retained PCM under benchmarks/review/speech-fst-20261024/phone-last-clip; no new phone audio required. A correction must pass saved-clip/reference/negative target checks before a new phone trial. Voice-recognized proposal/user confirmation/matching ACK/fresh readback, voice ON→OFF and phone silence/non-command UI check remain unverified. Never inject host text, map unverified words, relax thresholds, replay old ON or enable outputs to finish the test. Historical sections below do not override this result.

---


# Current result: ESP reference fixed; new phone job6 failed recognition — 24 October 2026 IST

**Reference recognition now passes on gateway B.** The same pinned audio/model is DETECTED at frame69/probability0.90041 through both the literal upstream and portable PCM paths. Normal-path local synthetic `TURN OFF BEDROOM LIGHT` is accepted as command2/confidence0.86229. Host silence and a non-command clip return command0. These are recognition-only target checks, not a new phone or command-confirmation pass.

Identified root cause: the pinned ESP-SR1.7.0 MN6 setter wrote positive probability0.7 into a field consumed as mean log probability. The hash-guarded private-ABI adapter stores equivalent ln(0.7), preserving the final probability>=0.7 gate. No model/phrase confidence relaxation or confirmation bypass. See [repair, exact checks and evidence](docs/VOICE_THRESHOLD_FIX_AND_PHONE_TEST_20261024.md).

Latest installed B normal OUTPUT-INHIBITED/UART0 SHA256: `f7b31d41910156965d6fe38355501962072aa7c415bfff322e5f78ed348a75cd`; guarded app-only manifest `benchmarks/raw/hardware/flash_speech_intelligence_uart_20261024T163210Z.json`, B MAC e0:72:a1:f6:a9:68/COM9 freshly matched. A MAC e0:72:a1:f5:bc:b0/COM10, unchanged8f028bd9... image, boot14. Latest read-only preflight: advancing samples3,282,999→3,284,999ms/age366ms, OFF/OFF,last_command0, outputs inhibited. AP192.168.4.1/channel1 matches A, watchdog active/zero sampled misses. This is a bounded observation; refresh before phone testing. Sensors/relays/loads remain user-reported disconnected; physical switching untested. Provisioning, credentials, NVS, partitions and model assets preserved.

HOST_MEASURED: final B build SUCCESS51.224s; 9focused portable/adapter/capture cases PASS4.91s; actual Android control/voice adapter45cases PASS1.003s. Full Robolectric blocker remains. All earlier failed reference and real phone results are retained below and in the review directory; they are not relabeled successful.

Latest diagnostic APK remains `deliverables/ClawGrid-Speech-Diagnostics-debug-20261024.apk`, version `1.0-speech-diag-20261024`, SHA256 `69d95b5552f1ece7c0d11f41710cd3ffe202d92e95dc97b778877cb4716efa8e`. The user now confirms installation and displayed version1.0-speech-diag-20261024. ADB is now authorized; the pulled installed base APK hash independently matches the delivery APK (installed-phone-identity.json). No uninstall/data clearing or ADB install occurred. Last confirmed failed phone trials used d12778de...; do not conflate built and installed versions.

The app connection is now fresh after the user reapplied gateway settings. Read-only diagnostics already show **manual OFF request1, matching ACK/status0/phase2/attempts1/RTT11,631us, and fresh boot14 OFF readback/last_command1/outputs inhibited**. The phone UI independently shows REPORTED. Voice unlocked and a new actual phone job6 was observed before another recording was requested. **It failed recognition:** 26,560 samples (1.660s), AudioRecord16000Hz/monoPCM16, phone and ESP CRC0x82b5f84e=2192963662 MATCH;198frames/TIMEOUT/command0/confidence0/error0, peak16,317/RMS3,738/no clipping. Diagnostic text DO OF BEOM LATE is untrusted and did not create a proposal. UI says No confident ESP recognition/nothing proposed/no automatic retry or confirmation. Source arrays were zeroed by existing finally blocks; original PCM was not retained, so intelligibility/onset cannot be established. See benchmarks/review/speech-fst-20261024/phone-job6-result.json and phone-job6-ui.json; raw read-only probe phone-reconnected-preflight-20261024.json. User wording/timing clarification requested without another recording. No voice confirmation, ON or phone negative trial is claimed. Do not request another manual proof or identical clip merely to repeat the failure. Next: diagnose the actual phone path before another justified recording. Physical outputs remain inhibited; switching untested.


---

# Voice FST follow-up IN PROGRESS - 24 October 2026 IST

This supersedes the earlier boot13/current-image notes below while work continues. Fresh ROM identities both matched provisioning again: COM10 A e0:72:a1:f5:bc:b0, COM9 B e0:72:a1:f6:a9:68. A is now boot14 after identity read, OFF/OFF/inhibited. No control commands or phone recordings have been requested in this follow-up. Both user-reported loads/sensors disconnected. No model/NVS/partition/provisioning write.

New evidence directory: benchmarks/review/speech-fst-20261024. Original image41df... and source/master/handoff snapshots backed up before changes. Six installed-pack constituent files match pinned sources individually (installed-pack-files.json). Pinned MN6 interface ELF proves open_log slot is NULL (mn6-interface-slots.json). Initial diagnostic called it and captured InstrFetchProhibited/PC0 panic; call is now guarded. The failed capture then hit cp1252 UnicodeEncodeError, interrupting aggregate save; UART panic and child stderr were retained. UTF8 journal/text logging and finally cleanup fixed, with two focused failure-injection cases. Eight host cases pass; latest2.80s. No claim that older unexplained restart had this same cause.

Fresh-factory same-audio/model paired comparison: literal pinned memcpy loop vs portable PCM loop each601frames/no accepted candidates,88recorded fingerprints/state traces identical and all match host expected frames (frame-comparison.json). These controlled differences did not resolve reference recognition. sdk-heap16384 experiment:601frames/10,504,138us/no detection; all-PSRAM loader: switch successful/freePSRAM4,066,347bytes,601frames/10,978,800us/no detection. Raw speech_decoder_safe_compare_20261024.json, speech_sdk_heap_20261024.json, speech_psram_loader_20261024.json. Authenticated exact-reference jobs always command_id0; normal16 vocabulary and original heap preference restored after each. Threshold0.7 remains unchanged; watchdog/RTOS/security preserved.

Current experiment: fixed non-clipping1.5x gain on public reference and independent Zira Desktop local TTS TELL ME A JOKE (padded44,829samples,CRC2905759910,SHAac05fd695174e1919fd2a8fa0e7f8c6cdbf02a5fbbed518130a049f4c6ff9035); also generated a normal TURN OFF BEDROOM LIGHT fixture. Fixtures are SIMULATED, target processing is HARDWARE_MEASURED. No new phone recording. Source adds isolated gain15/synthetic modes, keeps reference input fingerprints separate and cannot authorize control. Normal phone raw path unchanged. New build24.727s SUCCESS; guarded normal app-only B update currently pending/completing, see flash-B-acoustic-reference.log. Do not claim any recognition pass yet. Next run gain15 public-reference, synthetic fixed-reference, then normal Zira OFF through actual AP HTTP and preserve all results before considering phone testing.

Latest new APK remains69d95b55.../version1.0-speech-diag-20261024, host-built only; installation unverified. No Android source change this follow-up yet. End-to-end phone recognition/confirmation/ACK/readback and silence check remain untested until reference success; old manual/phone evidence remains historical. See detailed earlier master sections below for SDK/APK/backup paths. No subagents unless explicitly requested. Never lower threshold, bypass confirmation, enable outputs, erase or regenerate credentials.

---

# Current handoff: USB recovered; ESP reference recognition still fails - 24 October 2026 IST

**No new phone trial is requested. Recognition has not passed.** This section supersedes earlier pending USB-recovery, boot9/12 and not-deployed notes below. User changed the cable/wiring; ROM communication recovered. Fresh MAC reads matched existing provisioning: **A COM10 e0:72:a1:f5:bc:b0**, **B COM9 e0:72:a1:f6:a9:68**. A is now **boot13** after identity reads. Sensors, relays and loads are user-reported disconnected; physical switching remains untested. Native Android supersedes the old browser-only AGENTS rule. No subagents were used.

Full source changes, official pinned references, earlier failures and exact checks are recorded in [the speech diagnosis](docs/SPEECH_PIPELINE_DIAGNOSIS_20261024.md). Existing evidence is preserved. Original workspace is not Git; previous GitHub/Downloads publication remains unchanged and this new work has not been republished.

## Hardware and installed firmware

Latest B is normal OUTPUT-INHIBITED/UART0, **41dfec0a3c0adb4637722fb566737dd696582254f9e0147b40c3cbb918ce44a6**,1,203,920bytes; guarded app-only manifest **benchmarks/raw/hardware/flash_speech_intelligence_uart_20261024T153041Z.json**. Guard freshly matched B MAC before writing **0x10000 application only**. Earlier successful normal app-only variants/manifests remain in raw hardware evidence. No diagnostic-fault or output-enabled image was deployed. A remains **8f028bd90f65a55a72b09f4b886db7a3fa851f8a2108c6b79659136db8713cdf**. NVS, partition table, speech models, credentials and deployed database were not written or regenerated. Installed model/partition readback matched the pinned assets earlier today; final local private-input hashes and all62pinned component file hashes still match.

Final HARDWARE_MEASURED read-only probe: A boot13 **OFF/OFF,last_command0,physical_outputs_enabled=false**, fresh authenticated sample **1,322,996ms/age989ms**; samples/task counters advance. B AP **192.168.4.1/channel1**, same as A, watchdog active/no sampled deadline misses/reset_reason1. No reboot in the final full-window/AFE campaigns. Earlier unexplained B restart has no retained panic/watchdog cause; cable replacement does not retrospectively establish its cause. B restored the normal16-command registry after diagnostic jobs (registration/update/cache verification succeeded); API command_count1 describes the last reference job's vocabulary. No proposal, confirmation or control POST was issued today. Serial handles are released.

## Recognition evidence and corrections

Pinned ESP-SR1.7.0 commit07b0a4d0f6b5de755874d5405150ee0a8eec9865 and installed mn6_en assets verified. Model reports English/16000Hz/chunk512. Normal16uppercase phrases/IDs remain unchanged. Registration now verifies update and every cached ID; redundant command allocation/free around model-owned lifecycle removed. Clean/reset-first/fresh-model/factory-vocabulary variants were tested without accepted detection. Threshold remains **0.7**, explicit proposal/confirmation, boot/expiry/freshness, SafetyKernel, no-replay and output inhibition preserved.

Identified premature termination corrected: original phone clips stopped97/157frames; initial extension195still preceded observed198-frame timeout. Normal source now allows601calls and handles terminal state. Isolated pinned reference mode follows the upstream loop through TIMEOUT and its partial-frame behavior. These changes were built/deployed/target-tested; **they did not resolve recognition**.

| HARDWARE_MEASURED final reference/normal checks | Actual result |
|---|---|
| Pinned public clip through factory49-vocabulary601-call loop |44,829samples/CRC330285049 match;601frames/10,395,154us; no detection. |
| Same clip, custom17reference vocabulary/terminal-state loop |198frames/3,207,426us; TIMEOUT, diagnostic TELL ME A JOKEK, no accepted candidate. |
| Normal synthetic02 after window correction |34,880samples/CRC491911642 match;198frames/2,963,736us; diagnostic TURN OFF BED ROOM LIFE, command0/confidence0/error0. |
| First isolated AFE experiment |Stopped before preprocessing (error17); new format diagnostic proved pinned AFE feed160/fetch512. This exposed an assumption in the new experiment, not a proven original phone defect. Failure retained. |
| Corrected AFE160-to512 public reference |511feed frames,151fetch frames,77,312processed samples including lead/tail padding; outputCRC3451230078/peak16,583/RMS1676. Model198frames/3,223,588us/TIMEOUT/error0, diagnostic TELL ME A JOKEK, no accepted result. |
| Raw reference immediately after corrected AFE |198frames/3,222,738us/TIMEOUT/error0, same diagnostic/no detection. |

All these actual-Wi-Fi uploads matched the complete original input counts/CRC. AFE output CRC is a board measurement of transformed data, not an independent expected checksum. Source public clip is mono signed PCM16LE/16kHz, Git-blob/PCM-SHA verified. Raw reference already fails without phone capture, so further identical phone utterances are not justified. Original phone PCM was not retained: phone source CRC, acoustic quality and intelligibility cannot be retrospectively established. Existing source/ESP level and independent local Vosk diagnostic results are in the detailed report; they never supplied commands. Diagnostic text is untrusted and is not successful recognition. AFE and longer window are **not sufficient fixes**; underlying decoder/model/configuration cause remains unidentified.

Evidence: benchmarks/raw/hardware/speech_pinned_final_20261024.json, speech_synthetic_full_window_20261024.json, speech_afe_reference_20261024.json, speech_afe_init_20261024.json, speech_afe160_reference_20261024.json. Final runtime: benchmarks/review/physical-phone-20261023/speech-final-afe-state-20261024.json and speech-pipeline review/final-afe-runtime.log. Actual-AP helpers restored laptop Wi-Fi and deleted only their temporary private profiles/XML. UART large-clip upload failure and all failed hypotheses remain recorded separately.

## Files and host checks

Firmware: firmware/common/runtime/{speech_feed.h,speech_runtime.h,pcm16.h}, speech/main/speech_runtime.cpp, new **speech/main/speech_reference_afe.h**, firmware/node_intelligence/src/main.cpp. Authenticated reference endpoint accepts only exact pinned public PCM while A is fresh/inhibited; reference IDs always remain command_id0. The bounded AFE is **reference-only**; normal phone path remains raw PCM because no improvement was demonstrated. It uses actual160-sample feed/512-sample fetch sizes, preserves samples across leading-silence boundaries and restores normal vocabulary after failure/success. No AFE model assets were added.

Scripts: speech_pipeline_probe.py, with_gateway_ap.py, prepare_speech_reference.py, android_isolated_build.gradle. Tests: test_speech_feed.py, test_speech_probe_pcm.py and LocalVoiceTest additions. Android: app/build.gradle.kts; data/remote/LocalVoiceRepository.kt and PhonePcmRecorder.kt; ui/ClawGridViewModel.kt and ui/components/PhonePushToTalk.kt. New app diagnostics validate actual capture encoding/channels, show capture/build identity and compare phone/ESP count+CRC **before** confidence, including failed jobs; diagnostic reference results cannot propose controls. Screens/navigation and persistent policy semantics preserved.

Exact recent checks, firmware Python from handoff:

- `python -m platformio run -d speech -e speech_intelligence_uart`: pinned build45.213s; AFE full rebuild183.464s; added init-log build initially failed14.242s (NativeSerial has no println), corrected to printf and passed21.601s; final AFE160 build **SUCCESS23.652s**. Logs build-B-*.log in speech-pipeline review.
- `python -m pytest tests/unit/test_speech_feed.py tests/unit/test_speech_probe_pcm.py -q --basetemp=benchmarks/review/speech-pipeline-20261024/pytest-afe160 --junitxml=benchmarks/review/speech-pipeline-20261024/host-afe160-feed.xml`, TEMP/TMP=review/tmp: **6passed2.93s**. Includes actual160/512 planning, delayed-frame signed-LE boundaries, full reference fingerprint and input-format rejection. Earlier41existing portable firmware cases passed; initial Temp/setup/compiler-timeout failures and fixes retained.
- `python scripts/check_android_control_jvm.py`: **45adapter/voice cases passed1.066s**. No full Robolectric pass claimed; prior uncached dependency blocker remains.
- Android `.\gradlew.bat --offline --init-script ../../scripts/android_isolated_build.gradle :app:assembleDebug --console=plain`, JDK21/existing SDK/caches: **SUCCESS1m7s**,39tasks/25executed/14fromcache. Default build failed Windows BuildConfig AccessDenied; old artifacts/ACL preserved. APK signature/badging/configured-private-value scan passed; signing unchanged.
- `python scripts/flash_native.py --role B-speech --port COM9 --uart-console --app-only`: successful guarded writes documented by individual manifests. Final manifestT153041Z above; no model-install/bench/diagnostic flags.
- Reference tests used `python scripts/with_gateway_ap.py --output <session.json> -- <firmware-python> scripts/speech_pipeline_probe.py --node-a COM10 --node-b COM9 --url http://192.168.4.1 --identities benchmarks/review/speech-pipeline-20261024/board-identities.json --wav benchmarks/review/speech-pipeline-20261024/tell_me_a_joke.wav --reference --feed afe window --output benchmarks/raw/hardware/speech_afe160_reference_20261024.json`. Exit1 reflects no detection; harness errors[] and input checks match. Other exact feed variants/results above. Final `phone_probe.py --gateway COM9 --seconds 4 --name speech-final-afe-state-20261024` exit0.

## APK identity and remaining work

HOST-built delivery APK: **C:\Users\hares\OneDrive\Desktop\ESD-imp\ClawGrid-physical-integration-prep\deliverables\ClawGrid-Speech-Diagnostics-debug-20261024.apk**,18,364,814bytes,SHA256 **69d95b5552f1ece7c0d11f41710cd3ffe202d92e95dc97b778877cb4716efa8e**,version **1.0-speech-diag-20261024**. Build output: .android-tools/speech-diag-build-20261024/app/outputs/apk/debug/app-debug.apk. No agent installation/on-phone test occurred; installation is **unverified**. Last confirmed two real phone trials used d12778de7b8ecab0d141f71bf26dea4919c458a068aa7ee79453e045c98b0bfd. Yesterday's A44 laptop build is separate/installation unconfirmed. Existing debug key/signer and release signing preserved. USB/Android Studio/APK sideload instructions remain in ecosystem/android/PHONE_INSTALL_AND_LOCAL_VOICE.md. No update or repeated manual command is needed merely to recover IDs.

**Next executable step:** instrument the pinned MultiNet decoder/FST path on the exact public clip, recording per-window candidates/terminal transitions and active model vocabulary, while preserving0.7 and isolated command_id0. Compare runtime/library configuration against the pinned official test; do not change models, regenerate assets or assume AFE solved recognition. Obtain an accepted reference result before another phone trial. Current USB/telemetry is working; remaining blocker is unaccepted ESP recognition, not phone association or Supabase. No recognition-root-cause fix or voice-confirmation pass is claimed. Voice confirmation, physical switching, manual-until-AUTO versus five-minute hold, temperature transport and full Gradle suite remain unresolved/out of scope as previously recorded.

---

Firmware Python: `C:\Users\hares\Documents\Codex\2026-10-21\i-h\work\clawgrid-env\Scripts\python.exe`. PLATFORMIO_CORE_DIR=`C:\Users\hares\Documents\Codex\2026-10-21\i-h\work\pio-core`. Workspace is ClawGrid-physical-integration-prep (not Git); no own serial connection remains. Do not spawn agents unless explicitly requested. Historical records below remain intact.

---

# Current handoff: inhibited manual PASS; phone recognition FAIL; ID-display APK built — 23 October 2026 IST

Publication follow-up: user requested push to the existing repo and a Downloads copy. Existing repo is `https://github.com/Haresh-Selvakumar/ClawGrid`, default main, baseline79ea76f0dc030477f138927b9a20faca5b79a3d1. A checked source snapshot is prepared in Downloads `ClawGrid-integration-2026-10-23` with history retained. See `DISTRIBUTION.md` and the completed local receipt for actual commit/ZIP/APK hashes. Private files/keys/backups and generated caches are excluded; original workspace remains intact. Source credential scan passed. Selected original evidence is preserved verbatim under `benchmarks/raw/phone_integration_20261023/`; README/.gitignore updated for current native app and private-file exclusions. No new hardware tests, output changes, phone install or recognition success is implied.

Read the new top section of `CLAWGRID_MASTER_INTEGRATION.md` first. Older sections below are preserved history, including questions and captures that have now completed. No serial observer is still running. Do not spawn agents unless explicitly requested. The user retains native Android; older browser-only AGENTS rule is superseded.

**Current independent board state:** A boot9, fresh authenticated sample11,068,999ms/age282ms, light/fan OFF, last_command12, physical_outputs_enabled=false. B transaction12 phase2/status0/outcome_known/attempts1/RTT16,724us is OFF. User's historical OFF11/ON12 screenshot IDs cannot be correlated to captures; never represent them as proof of a later OFF. Do not repeatedly ask the user for IDs or repeated commands. No new OFF is necessary now.

**Pair unchanged:** A COM10 MACe0:72:a1:f5:bc:b0 image8f028bd90f65a55a72b09f4b886db7a3fa851f8a2108c6b79659136db8713cdf; B COM9 MACe0:72:a1:f6:a9:68 imagea225740aa6af9445f9959994141dfc5723944c7f59daa957d039638cafbaf2b2. Prior ROM/guard evidence preserved; no reset/flash in this follow-up. Both normal inhibited/UART0/ESP-NOWchannel1; AP192.168.4.1; watchdog/task progress pass. Relays/loads remain user-reported disconnected, physical switching untested. Supabase CONNECTING does not block local tests. Credentials stay private, unchanged; models/NVS/partitions/security/thresholds/policy unchanged.

**Real phone voice result:** app manual gate unlocked without enabling outputs. User performed two recognition-only trials prompted TURN OFF BEDROOM LIGHT; both reported voice stopped/no confident ESP recognition/nothing proposed/no automatic retry/confirmation. Actual ESP jobs1/2 completed phase4/error0/engine_errors0 but command0/confidence0/candidate0/empty decoder text/DETECTING state. Job1:33,280 samples/2.08s/peak15,349/CRC3561804158/97frames/inference1,438,407us. Job2:full64,000 samples/4s/peak15,392/CRC1757513135/157frames/inference2,382,320us. Both unclipped, chunk512/padded16,384; full-length timing check did not resolve failure. Real capture/upload/ESP processing observed; recognition FAILED, speech proposal/confirmation/control NOT_EXERCISED. No raw phone PCM retained or independent CRC/transcription; no ADB logs. Do not invent microphone quality, an engine error, a successful recognition, or a passed accuracy evaluation. A boot9/progress/output inhibition persisted; final state remains OFF12.

Both passive captures completed with no error and released COM9/COM10 before diagnostics. Latest exact read-only check: firmware Python `benchmarks/review/physical-phone-20261023/phone_probe.py --gateway COM9 --seconds 4 --name voice2-diagnostics` (exit0/error=null). Raw aggregates `benchmarks/raw/android_phone_voice_trial1_20261023.json`, `android_phone_voice_trial2_20261023.json`, detailed captures/probes under physical-phone review. No host audio/control POST in these observers.

**User-requested ID visibility implemented:** new `ui/components/HardwareRequestStatus.kt`, integrated below hardware banner in `MainActivity.kt` across hardware tabs; Bedroom request text in `RoomsAndRelaysScreen.kt` includes ID. Prominent request ID/phase/desired direction, separate proposal ID and fresh board last-command/light/boot/output state. No semantics/navigation change. Before-source snapshots and installed test APK preserved. HOST_MEASURED: Gradle debug build SUCCESS1m35s;42 control/voice JVM tests +3 wire tests pass; apksigner v2/one existing signer PASS; configured-private-value APK boundary scan PASS. Existing local debug key and private-input hashes unchanged; release signing untouched. Full Robolectric remains blocked by uncached nativeruntime-dist-compat1.0.18, not rerun.

New APK **not installed or on-phone layout-tested**: `C:\Users\hares\OneDrive\Desktop\ESD-imp\ClawGrid-physical-integration-prep\ecosystem\android\app\build\outputs\apk\debug\app-debug.apk`,18,570,202B,SHA256a44b9cc4dc3571c71b334b95049d4ab2221af10f1da8b04f6f1b13d1a4366119. All physical phone trials used prior installed APKd12778de...; don't attribute them to new UI. Existing signer71ce1bf6... unchanged. Source/build metadata `benchmarks/raw/android_request_id_build_20261023.json`; exact logs in master. Phone installation/voice docs updated. Process restart/update clears in-memory token/manual gate; don't force an install/repeated manual command merely for historical IDs.

**Next executable step:** investigate current speech runtime against pinned ESP-SR1.7.0 reference PCM/model/decoder pipeline with existing labelled diagnostic clips and a bounded recognition-only harness; isolate a defect before any normal inhibited firmware change. No more identical phone utterances without a new diagnostic reason. Threshold0.7 and explicit validation/confirmation remain; AFE only a hypothesis. No blind flash, physical output enabling, credential regeneration, raw audio retention or successful transcript injection. Recognition blocker prevents real voice-confirmation test. Manual-until-AUTO/five-minute hold mismatch and temperature transport remain separate unresolved requirements. New request-ID APK is ready for user-controlled installation, not yet phone-verified.

Firmware Python: `C:\Users\hares\Documents\Codex\2026-10-21\i-h\work\clawgrid-env\Scripts\python.exe`. Current workspace `C:\Users\hares\OneDrive\Desktop\ESD-imp\ClawGrid-physical-integration-prep` (not Git). Preserve previous backups and evidence.

---

# Historical: App manual ON/OFF milestone PASS; voice recognition-only stage — 23 October 2026 IST

**Native Android → B → authenticated ESP-NOW → A → matching ACK + fresh readback passed for both manual directions, with outputs inhibited.** User confirms OFF#5 and ON#6 REPORTED; independent gateway pairs match phase2/status0/outcome_known/attempts1, same A boot9 and output inhibition. OFF#5: sample9,900,999ms/age91ms/RTT19,665us/lightfalse. ON#6: sample9,908,999ms/age341ms/RTT18,311us/lighttrue. Earlier app-owned OFF#4 also passed. These are radio/software logical results, not physical switching.

Additional observed commands #7 OFF and #8 ON have matching ACK/readback; #9 ACK/actuation reported ON. Do not silently describe the earlier OFF as the final state. User's subsequent OFF reply had a placeholder ID, so actual state was queried independently: **latest command#12 is OFF**, successful ACK/status0/outcome_known/attempts1/RTT16,724us/processing37us, matched fresh **boot9 sample10,300,999ms, age94ms, last_command12/lightfalse/physical_outputs_enabled=false**. IDs10/11 were not captured in the bounded gap; no invented reconstruction. Agent sent no command POST in these phone stages. Explicit final OFF is now established in board evidence. User reports loads disconnected throughout.

Current firmware remains normal inhibited A COM10/base MAC e0:72:a1:f5:bc:b0/image8f028bd..., B COM9/base MAC e0:72:a1:f6:a9:68/image**a225740aa6af9445f9959994141dfc5723944c7f59daa957d039638cafbaf2b2**. A boot9 unchanged. B AP192.168.4.1/channel1, task/watchdog health and speech ready/zero errors preserved. No NVS/model/credential/security/threshold/policy changes in these tests. The Supabase CONNECTING label does not participate in local control and is not a blocker.

## Current staged voice test

The app-owned inhibited REPORTED result satisfies the manual-verification condition without enabling physical GPIOs. Actual visible voice readiness and microphone permission are now requested: stop/report if “First verify one manual light command” remains. Otherwise grant microphone While using app, close the Chrome gateway tab, remain on ClawGrid AP/app foreground, hold push-to-talk and say **TURN OFF BEDROOM LIGHT** once within4s, release. This **first clip is recognition-only: CANCEL any speech proposal, do not Confirm**. User should report permission and exact voice message/job/error. Keep recognition result separate from any later confirmation/command result; no cloud speech, fixture audio or threshold reduction.

A passive capture is active: firmware Python `benchmarks/review/physical-phone-20261023/passive_phone_capture.py --name phone-voice-recognition-1 --event speech_job --min-id 1 --seconds 240`, exec session58485 at dispatch. It occupies COM10/COM9 until the first speech_job completes or timeout; **poll/wait it before opening another serial client**. It does not issue HTTP/control/audio requests; A q health queries and passive A/B task/link/speech logs permit observation without competing with phone audio for B's two HTTP sockets. It saves `phone-voice-recognition-1.json`, `-uart.log` and stdout log. Then read `/api/speech`, `/api/state`, `/api/rtos` once over the normal bridge to capture actual job, samples/CRC/peak/confidence/command/errors and continuing authenticated fresh inhibited telemetry. No recognition/capture pass is claimed yet. Permission is user-observed unless a real upload corroborates it.

If recognition succeeds for the intended target at unchanged0.7 threshold with valid job/CRC, a **fresh separate** spoken clip can test user-reviewed confirmation and matching ACK/readback; first cancelled proposal must not be replayed. If recognition or transport fails, record the actual engine/PCM/error evidence and keep outputs inhibited. No new firmware/APK change is needed merely to unlock voice. Keep app settings/process unchanged; applying connection settings or restarting process resets manualVerifiedBoot and requires a new app manual proof.

Evidence: aggregate `benchmarks/raw/android_phone_manual_20261023.json`; `phone-manual-5-to-8-verified.json`, `phone-OFF-4-verified.json`, `phone-return-off.json` (ACK#9 ON), and `return-off-current.json/log` (final OFF#12) under physical-phone review. Phone guide/root-page fixes and original verified evidence remain below. Remaining: actual first microphone/recognition result, subsequent voice confirmation path, physical switching (out of scope/inhibited), persistent manual-until-AUTO mismatch and temperature transport.

---

# Phone manual OFF #4 verified — 23 October 2026 IST

**First app-owned inhibited command PASS.** User reviewed/confirmed Bedroom/light/OFF in native Android and reports “Matching ACK + fresh board report #4. Physical load not measured”, Requested Light OFF · REPORTED, outputs inhibited and board-reported OFF. Gateway capture independently matches transaction **id4/phase2/status0/outcome_known=true/attempts1**, RTT16,875 us/processing28 us; same-boot9 advancing report sample **9,552,999 ms**, age90 ms, **last_command4/light=false/physical_outputs_enabled=false**. This is logical inhibited actuation only; relays/loads remain user-reported disconnected and physical switching untested.

The app-owned manual gate can be satisfied by inhibited ACK/readback without enabling outputs. Its actual visible Hold-to-talk readiness/permission remains to be checked after manual ON/OFF; no microphone/upload/recognition trial has occurred. Keep the app process/settings and current boot; applying connection settings again resets manualVerifiedBoot and requires another app-owned manual pass. Supabase CONNECTING remains optional and is not involved in direct control.

Next staged test: user verifies Outputs inhibited, taps Bedroom Ceiling Light **ON**, inspects Bedroom/light/ON proposal and Confirms once, waits for REPORTED/ACK+readback and replies with id/state. Stop on rejection/timeout; do not replay. Read-only `phone_probe.py --gateway COM9 --seconds 240 --name phone-manual-on-off` is capturing; no agent control POST is sent. After matched ON, wait at least3s and guide a new explicit OFF to leave logical output OFF. Then check app voice gate and grant permission/record real PCM for ESP recognition with threshold0.7 and explicit proposal confirmation intact. No further firmware or APK changes needed for this staged test.

Evidence: `benchmarks/review/physical-phone-20261023/phone-OFF-4-verified.json`, complete `phone-manual-off.json/log`, running ON/OFF observer and aggregate `benchmarks/raw/android_phone_manual_20261023.json`. App UNKNOWN was resolved by applying the existing token and retaining AP connection; current app/browser reachability is confirmed. Earlier records are historical below.

---

# Native app now fresh; inhibited manual OFF test staged — 23 October 2026 IST

The user applied the existing API token and kept the phone on ClawGrid-Assistant. Android now reports **Fresh board report; physical load not measured**, Fresh report health, Loads0/2/Relays Guarded, occupancy UNKNOWN and temperature/lux unavailable. This resolves the prior app UNKNOWN blocker at this stage; no Android source/APK change was needed. Prior token/AP-retention contributions were not isolated individually. Supabase CONNECTING is an optional display status and is not used by the direct gateway control path; it must not block local tests.

Fresh read-only preflight (`phone_probe.py --gateway COM9 --seconds 5 --name app-fresh-preflight`, exit0) verifies A **boot9**, sample **9,476,999 → 9,481,999 ms**, maximum age339 ms, all known/authenticated and **physical_outputs_enabled=false**, light/fan logically OFF, B AP clients1/channel1, watchdog active and ESP speech ready. Occupancy UNKNOWN is distinct from unavailable board telemetry; do not fabricate sensor state. No outputs were enabled.

Started read-only `phone_probe.py --gateway COM9 --seconds 180 --name phone-manual-off`. User is instructed to request **Bedroom Ceiling Light OFF**, inspect proposal Bedroom/light/OFF, Confirm once, and wait for matching ACK plus fresh report/REPORTED. Baseline controller last_command=3 and B transaction idle after its page-fix restart. No agent POST/control was sent in this step. Await phone request ID/status and corresponding gateway ACK/readback before claiming an app command pass or voice unlock. Keep all loads disconnected. Next after verified manual command: validate app manual gate and real mic capture/PCM upload/ESP recognition at unchanged threshold0.7. No physical switching claim and no cloud dependency.

Evidence: `benchmarks/raw/android_phone_manual_20261023.json`, detailed preflight/observer under `benchmarks/review/physical-phone-20261023/`. Earlier records remain historical below.

---

# Native app connection follow-up — 23 October 2026 IST

During the read-only `phone-manual-session` observer, B's AP client count changed **1 → 0**, while A remained fresh/inhibited and B speech stayed ready/job0. The laptop had already returned to home Wi-Fi. Thus the phone was no longer associated at the end of this capture; this is evidence to investigate no-Internet AP retention, **not proof of an Android parser/firmware defect or a demonstrated root cause at the exact earlier app failure time**. Asked user to reconnect, choose Stay connected/Use without Internet, disable automatic switching to better Wi-Fi/mobile data temporarily, and keep native app foreground20s. A fresh `phone-app-connection` read-only observer is running. Exact app status/HTTP scheme/API-token-field confirmation remains requested; values stay private. No further flash or speculative Android change was made, and no app control/audio result exists yet.

---

# Phone browser fixed; native app UNKNOWN — 23 October 2026 IST

User confirms corrected gateway root shows **fresh hardware telemetry / outputs INHIBITED** on phone. Chrome had upgraded HTTP to HTTPS; guided local HTTP Continue-to-site/settings per official Chrome docs. Legacy root had virtual_rooms/generated demo metrics; replaced root only with read-only hardware status and no-store, kept explicit /simulation and native Android UI. Normal inhibited B app-only update passed guard/hash verification, current B **a225740aa6af9445f9959994141dfc5723944c7f59daa957d039638cafbaf2b2**, COM9 e0:72:a1:f6:a9:68; A remains COM10/base MAC ending bc:b0/image8f028b..., boot9. Models/NVS/credentials unchanged; no threshold/security/policy change. Host JS freshness/reconnect/reboot tests and frontend checks pass; B build53.270s; actual AP root byte-match5864B/no-store + APIs HTTP200; healthy speech ready. Full exact evidence in current master / android_gateway_root_fix_20261023.json.

**Current blocker:** native Android app reports UNKNOWN/controls unavailable. Exact Bedroom status, applied http:// URL (not https://) and API-token-versus-Wi-Fi-password selection requested; never ask values. ADB devices list empty. Read-only phone-manual-session observer started; no app-owned manual or phone audio result yet. Inspect actual settings/transport before changing Android or firmware blindly. After app fresh/inhibited, user manual light OFF → proposal/Confirm → matching ACK+fresh readback, verify app voice gate, then real microphone PCM/recognition at threshold0.7. Keep relays disconnected and outputs inhibited; never enable outputs to unlock voice. Root-page success does not set app manualVerifiedBoot. Prior records below are historical and retained.

---

# Wi-Fi HTTP diagnosis — 23 October 2026 IST

Laptop joined ClawGrid-Assistant with existing credentials and received DHCP **192.168.4.3/24**, gateway/server192.168.4.1. Actual TCP80 and GET `/` (155,404 bytes), `/api/state`, `/api/rtos` all pass HTTP200 over Wi-Fi. B AP BSSID e0:72:a1:f6:a9:69/channel1; two clients measured while laptop and the other client associated. Original laptop Wi-Fi restored; temporary profile/private XML removed. No flash/reset/provisioning/output changes. Fresh A/B self-tests pass; A boot9 reports advance/inhibited, B watchdog active and speech supported/ready/errors0. No reboot/panic in bounded capture; historical gaps remain unknown.

User phone browser still times out. Phone DHCP address/gateway/subnet and VPN status are requested, not yet received. One/two client counts support association, not proof of phone DHCP lease. Do not claim a phone fix or continue control/voice until a direct phone `http://192.168.4.1/api/state` GET works. No gateway reflash is warranted by actual laptop results. Read the new master section and `benchmarks/raw/android_gateway_reachability_20261023.json`; detailed logs/helper under physical-phone review. Preserve secrets and outputs inhibited.

---

# Credential locator verified — 23 October 2026 IST

User could not find the Wi-Fi password. Existing root `local_credentials.json`: **ap_password (line 3)** for ClawGrid-Assistant Wi-Fi; **api_token (line 4)** for the Android gateway settings. User explicitly requested opening it locally, and editor open at line 3 is queued. No values printed or credentials changed. SSID/password/token match private header + current B image; image matches successful final UART flash manifest. Fresh running B authenticates the existing token and confirms healthy inhibited state, AP 192.168.4.1/channel1. Evidence: `benchmarks/review/physical-phone-20261023/credentials-verification.json`. Next: user joins AP with existing password; then resume phone-owned manual OFF/ACK/readback and gated real microphone test. No reflash or provisioning change required.

---

# Physical pair deployed; phone interaction pending — 23 October 2026 IST

Read the current master first. Both current CH343/UART0 connections are freshly ROM-MAC verified: **A COM10 e0:72:a1:f5:bc:b0**, **B COM9 e0:72:a1:f6:a9:68**. Do not infer role from an old COM number. Normal inhibited/fault-default A `native_voice_uart` and B `speech_intelligence_uart` were app-only flashed and verified. Current hashes, guarded flash manifests, exact commands/results and full private backup locations are in [the master](CLAWGRID_MASTER_INTEGRATION.md). NVS/partition/credentials were not erased or regenerated. B's original model region was entirely blank; guarded fresh table/whole-region reads permitted installing pinned ESP-SR assets there. Final B UART deployment preserved those models.

Both runtime self-tests pass, actual A/B channels=1, measured B AP=**192.168.4.1**, advancing authenticated A boot=9 reports, watchdog/task progress and inhibited outputs verified. ESP-SR now supported=true/phase=1/errors=0. **Real firmware API over USB HTTP tunnel passed manual OFF #1 → ON #2 → OFF #3, each proposal/confirmation/matching ACK plus advancing readback. This is inhibited logical actuation, not physical switching.** Host tests: 76 firmware/watchdog, final 41 flash/UART guards (overlapping suites). No thresholds or persistent policy semantics changed.

User reports APK installed. Phone joining/configuration question is pending; 90 s observer saw AP clients=0 and speech job=0. Phone association/app-command/manual voice-unlock/mic capture/upload/recognition remain untested. The inhibited API result can satisfy the app's gate, but **only the app's own current-boot manual request reaching REPORTED sets manualVerifiedBoot**. Host commands do not unlock it. Never enable outputs to unlock voice.

Next: use the existing normal pair; no new flash/cable change is necessary. Run the read-only `benchmarks/review/physical-phone-20261023/phone_probe.py --gateway COM9 --seconds 90 --name phone-session-2` with the firmware Python identified in the master. User joins ClawGrid-Assistant without Internet, Hardware settings URL http://192.168.4.1 and existing token privately. Observe AP clients and fresh inhibited state, then user does app manual OFF → Confirm → ACK/readback. Only then grant mic, hold/release real phone speech, record actual job/confidence/errors/result, retain 0.7 threshold and explicit command confirmation. No synthetic-input substitute or old ON replay. Keep relays/loads disconnected. Saved HOME/light-auto/fan-manual/grace120000 and five-minute manual-hold mismatch remain unchanged.

Evidence: `benchmarks/raw/android_physical_20261023.json`; detailed logs/helper under `benchmarks/review/physical-phone-20261023/`. Earlier Android/firmware records below remain historical and verbatim.

---

# Phone install / local ESP push-to-talk — 23 October 2026 IST

The user chose the phone microphone and direct ClawGrid AP **http://192.168.4.1**; URL remains editable. Actual raw AudioRecord PCM submission and ESP job-bound speech proposals are implemented, with explicit confirmation and ACK/readback preserved. Voice unlocks only after current-boot manual light ACK + advancing readback. Wi-Fi sockets are used without Internet/Supabase/cellular fallback. Source AP/A/peer channel is 1 throughout; no fresh RF test.

Actual Gradle wrapper/checksums and existing debug signing verified; release signing preserved. Debug APK builds (46 s), signature verifies, 42 control/voice cases + 3 wire cases + 32 host audio/proposal regressions pass. No phone installation, mic/recognition/board test, flash/NVS/key/database change. A read-only gateway probe timed out while laptop was on home Wi-Fi. Use [the phone install/local voice guide](ecosystem/android/PHONE_INSTALL_AND_LOCAL_VOICE.md) and [current master](CLAWGRID_MASTER_INTEGRATION.md) for exact paths/checks/limits and staged next step. Earlier records below remain historical.

---

# Current Android integration update — 23 October 2026 IST

Implemented native app M1/one-room manual light ON/OFF with explicit proposal confirmation and matching ACK + advancing board readback. Five-tab UI retained. Hardware no longer runs simulation policies/counters/charts; requests do not optimistically change board state. No ON replay on reconnect/reboot/timeout. Privileged Android key support and .env APK imports removed; launcher cloud integration explicitly blocked pending authenticated home-scoped access. Persistent firmware manual/AUTO semantics remain unchanged and mismatched with until-AUTO intent.

Fresh **host-only** evidence: debug APK assembled; 28 control/HTTP Kotlin cases, 3 existing wire cases and 90 focused firmware/API regression tests passed. Full Gradle unit suite blocked by missing cached Robolectric native runtime. No phone/emulator, hardware port, flash, NVS, credentials or deployed database changed. Start from [the updated master implementation](CLAWGRID_MASTER_INTEGRATION.md) for exact commands, files, results, APK hash, blockers and next step. Earlier dated hardware evidence below is preserved history.

---

# ClawGrid physical integration handoff — 22 October 2026

**Latest ecosystem integration (22 October 2026):** Supabase encrypted Broadcast now carries real A reports and measured B diagnostics to the shared live webpage and Android source. Independent public-key client verification passed virtual ON/OFF through Supabase → B → A, with matching ACK and state. Physical GPIOs remain inhibited; missing temperature/lux and A RTOS/heap are unavailable. See [ecosystem integration and exact field mapping](docs/ECOSYSTEM_INTEGRATION.md). Current B USB is unavailable (Windows device error 31); the gateway reports UNKNOWN until that connection recovers. Earlier dated deployment notes below are retained history.

**Onboard traffic LED update:** New native A/B sources and images add RGB traffic,
command reception and matched application ACK pulses. Default RGB pin is GPIO48
(GPIO38 on official DevKitC v1.1). These new images have not been flashed by the
agent; user uploads both. Physical relay GPIOs remain inhibited. Earlier live
radio measurements describe the previous installed firmware. See [LED/upload
instructions](docs/TRAFFIC_LED.md).

**Role-swap deployment now verified:** User uploaded A COM8 and B COM7 successfully.
Both flash manifests match the current compiled application hashes. Read-only
`/api/state` through B shows fresh authenticated A reports, zero reported invalid/
replayed frames, and outputs inhibited. PC offline recognizer is ready, microphone
inactive. Evidence: `benchmarks/raw/hardware/role_swap_link_20261022.json`.
No human microphone or physical relay/load test has been performed in this check.


**Latest user-confirmed role correction:** Relay/PIR A is COM8, MAC
`e0:72:a1:f5:bc:b0`; bare PC bridge B is COM7, MAC `e0:72:a1:f6:a9:68`.
Local credentials retain the same keys/token; only board assignments changed.
Both radio-peer configurations must be rebuilt and both boards uploaded.
Earlier hardware PASS results below apply to the previous assignment, not this
new deployment. The user is doing the uploads; no swapped images were flashed
by the agent. Physical outputs remain inhibited.


**Current deployment: PC offline voice -> USB B -> ESP-NOW remote A.**
B uses `native_intelligence`; A uses `native_voice`. Both flash layouts have been
corrected to 16 MB with NVS preserved. The PC recognition/virtual ON-OFF path has
passed synthetic-input testing with real radio ACK/state observations. This is
not a human microphone or physical relay/load test. Physical GPIO outputs remain
inhibited pending electrical interface checks. A only needs remote power; B stays
connected to the PC. [Code/upload paths](docs/UPLOAD_CODE.md) ·
[PC voice operation](docs/PC_OFFLINE_VOICE.md).

Verification: 205 host tests passed; both frontend checks passed; 13/13 synthetic
recognizer/parser cases passed. Evidence:
`benchmarks/raw/hardware/pc_voice_end_to_end_20261022T051049Z.json`.
The entries below are earlier commissioning history.


**Latest commissioning update supersedes the earlier A-unavailable notes:** A is
now programmed on native USB COM7. Its old 4 MB bootloader/partition layout was
backed up and corrected to the verified physical 16 MB chip, preserving NVS.
A self-test, task progress and 16 MB/8 MB memory checks pass. B was already
programmed, but its USB application transport is currently being recovered after
reconnection. See [current status](docs/CURRENT_INTEGRATION.md) and
[wiring/checklist](docs/PROGRAM_AND_WIRE.md).

**Later 22 October deployment update:** B is now freshly built, app-only flashed
and runtime-verified on COM8. A's USB connection failed (Windows Code 43); A's
native USB and UART images are built but not deployed. Continue from
[current integration status](docs/CURRENT_INTEGRATION.md) and
[ESP wiring](docs/ESP_CONNECTIONS.md). The preparation evidence below is historical.
The supplied text-command sketch was subsequently integrated alongside the
dashboard/speech firmware and deployed to B: 196 host tests passed. See
[text-command usage](docs/SERIAL_TEXT_COMMANDS.md). The robotic-arm paste is out
of scope; the original broadcast/direct-GPIO sketch was adapted through the
existing authenticated proposal/confirmation and SafetyKernel path.

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

