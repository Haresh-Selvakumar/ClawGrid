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

# Latest acceptance progress — both-room schedules, 2 November 2026 IST

Read `benchmarks/review/three-board-acceptance-20261102/CHECKPOINT.json` for active status. Firmware unchanged this campaign. Current APK is1.0-three-room-20261102b SHA256a442f55812be72c943bac8dd87d33d3d8e8de432606c37110be4e9241e7ffada; fixed room/node/device schedule-cap review title,95JVM/build/signature/model/alignment/privacy checks PASS, installed hashmatched. Phone review Room2/NodeC/light verified and cancelled without authorization.

Scheduled OFF both rooms PASS: Room2 Cboot4 configuration6, WiFiOFF duringON-to-OFF capture, outcomeExecuted/sourceSchedule, independent uservisible result. Room1 Aboot72 configuration15 in corrected480second window,48ON samples afterWiFiOFFbeforeOFF, executedOFF withWiFiOFF, useronlyAOFF/all4OFF/appfresh. EarlierAconfiguration13 attempt had fixture timeout and onlyposteventcontinuation; explicitlyINCONCLUSIVE for offline timing and retained. See `room1-offline-scheduled-off-assessment.json`, then `room1-offline-off-corrected-verdict.json` for successful repeat. No firmware rebuilt/reflashed to resume.

Android WiFi radio enable alone did not rejoin ClawGrid AP; user joined existingAP/Stayconnected. App returnedSourceBOARDfresh. One serialobserver ended early onmissingresponse; retained gap and freshpostgapreadback showed sameboots/counters/no acceptedONreplay. Do not equate radioenabled with APassociation or claim HTTPattempt-level capture.

Room2 replacement PASS: armON15s replacedOFF25s, bothconfigurationACK/readbacks andnewOFFexecution, oldONprevented, all4OFF, userappdisplaysmatch (`room2-schedule-replacement.json`). Room2 cancellation PASS: armON15s thenexplicitcancel, olddued passedallOFF, userCancelled/Nonematch (`room2-schedule-cancellation.json`). ExistingbootboundAPI used, noGPIO bypass. C21taskhealthsamples boot4 control/link/sensoradvance, sensorwatchdogtrue, nostall (`room2-schedule-health.json`).

Current manualpriority attempt `room2-manual-cancels-fan-rule.json` did NOTPASS: usermanualOFFdidnotarrivebeforeexpiryguard; onehostcleanupcancel accepted beforeONdue. AlloutputsOFF, noarmedrules, ownedserialclosed. Active question: can user seeRoom2fan manualOFF onthe roomcard, rather than planningActionOFF? Position/ready first, thenarm/retryonlythiscase. Do not falsely count hostcleanup as successfulmanualcancellation.

Remaining original campaign: manualcancels pendingrule bothrooms; Room1replacement/cancel; hardwaredwellblocked/no-replay plusdocumentedlatepolicy; one-minuteOFFcaps bothrooms/disableindependence/labelledestimates; currentroomtargetedVosk4positivecommands each +separatecancellation/negation/unsupported/multiple/roomnegative captures; roomreboot/disconnect/pendingreviewinvalid/no replay. Sensors are reporting only; no daylight/temp/PIR automation. C has intermittent INVALID readings beyond its historic35/35window: baselineunavailablethenrecovery retained; no sensorcampaign repeated.

---

# Continued acceptance campaign — 2 November 2026 IST

Current evidence and live status: `benchmarks/review/three-board-acceptance-20261102/CHECKPOINT.json`. Source and firmware unchanged on resume; no rebuild/reflash merely for resumption. USB roles re-enumerated by adapter serial numbers tied to verified MACs. Baseline both authenticated/fresh/OFF, no pending transaction/schedule/cap; user independently confirmed fourOFF. C had one INVALID/null temperature sample then recovered; continuous temperature reliability is not established by the earlier90second window. No repeated sensor campaign.

Capture-priority regression evidence reused: C build +82 actual decoder/safety/wire/provision/flash-guard tests +35/35 measured sensor frames; host tests do not emulate interrupt latency. Source hashes match the deployed capture-priority plan.

Room2 light ON1/OFF2 on boot4 captured ACK/readback; the same window contains intentional user A6ON/A7OFF. User clarified both rooms were intentionally operated. That mixed window is not strict isolation proof. Isolated Room2 fan ON3/OFF4 PASS: A staysOFF/cmd7, separate user observation onlyCfan switched/all4OFF.

Room1 scheduledOFF PASS: manualON8, configuration9 accepted with countdown, later result2/source2/OFF without new command. User confirmed automaticOFF, correct app priority/outcome/source displays, all4OFF. User intentionally repeated ON10/schedule11; second executionOFF also captured. Phone-disconnected execution NOTRUN: first due action fired before that instruction. No undocumented automatic replay inferred from intentional repeats.

Source review found Room2 schedule/cap confirmation title hardcoded Bedroom. Preserved source evidence `schedule-review-label-defect.json`; corrected room/node/device title and OFF-cap priority description without changing protocol/guards. Current installed APK1.0-three-room-20261102b SHA256 `a442f55812be72c943bac8dd87d33d3d8e8de432606c37110be4e9241e7ffada`; isolated build and95 control/planning/isolation JVM tests PASS. Signature/model/alignment/privacy PASS, installed APK pulled/hashmatched. Existing signing identity/model/provisioning/credentials preserved. No firmware redeployment. On-device Room2 OFF review open/cancel verification and private memory-only gateway token restoration in progress. Do not authorize while reviewing label.

Last rooms A boot72/cmd11, C boot4/cmd4, all outputsOFF; no armed rules. A bounded read-only observer may own A/B serial during the app reconnect/review check; close before next owner. Remaining: isolatedRoom2light if needed, allRoom2schedule tests, phone-disconnected schedules both rooms, replacement/cancel/manual-priority/blocked-late policy checks, caps, room-targeted voicepositive/negative and recovery. Continue one explained test at a time; user visible observations separate from ACKs; no sensor automation/raw audio.

---

# Latest sensor checkpoint — PASS in observed windows, 2 November 2026 IST

Current authoritative sensor results: `benchmarks/review/three-board-20261102/SENSOR_FINAL_CHECKPOINT.json`. Earlier failed samples below are historical evidence, not the current result.

Room2 DHT11: after the C-only GPIO ISR level3 update,35/35 physical frames valid over90seconds;41/41 gateway samples fresh/authenticated/statusOK with real temperature/humidity. C node3/boot4. Decoder thresholds, checksum and range checks unchanged. Earlier captures showed distorted timing and intermittent invalid/timeouts; all retained. This supports the capture-priority repair in the observed window, not calibrated accuracy or long endurance.

Current C image SHA256 `1bbcd5b971233ae02bdd9a65635ab9d0f029a49b8d48c97333aa875cfb611e57`, guarded app-only offset0x100000; application readback and full-flash protected preservation PASS (`c-dht-priority/deployment-C.json`). Build44.15s PASS,82 affected host checks PASS. A/B unchanged. All original and intermediate dual backups preserved. C bounded pulse diagnostics remain at most3 invalid frames per boot; no decoder relaxation.

Room1 LM393: covered/darkHIGH4/4; direct torch/brightLOW4/4 (`LM393-polarity-verdict.json`). Pot unchanged, no lux or daylight automation. Firmware polarity metadata remainsUNVERIFIED; observed mapping documented here. A node1/boot72.

Phone: user replied working done to explicit Room2 temperature BOARD/fresh plus all-fourLEDsOFF check. Both rooms remainedOFF throughout final92second API capture. C last_command0, A command5; no oldON execution in observed C restarts. Software state and user LED observation recorded separately. All owned serial sessions closed. No raw microphone audio retained.

Current APK remains1.0-three-room-20261102a, SHA256 `1bad8be832e2f35bb091f02afa494c4731853f526de0ff38b4bed51bb68fcdb9`; installed hash/signer/alignment/model/privacy verified. Sensor firmware work required no further Android changes.

Full original three-board acceptance remains INCOMPLETE: isolated per-action Room2 physical/software capture; schedules/replacement/cancel/manual priority/phone-disconnected execution; OFF caps and labelled estimates; room-targeted Vosk confirmation/cancel; full room disconnect/stale/recovery coverage. Continue those guided tests without rebuilding/reflashing unchanged artifacts. No measured energy savings or new speech accuracy claim.

---

# Three-board deployment and acceptance in progress — 2 November 2026 IST



This supersedes the 1 November source-only checkpoint. Acceptance remains incomplete. Do not repeat deployment merely to resume.



## Verified hardware and deployed applications



| Role | UART | Actual chip / MAC | Flash | Current application SHA256 |

|---|---|---|---|---|

| Room 1 A | CH343 COM10 | ESP32-S3 rev0.2 / e0:72:a1:f5:bc:b0 | 16 MB | e7ffd52a0dd351a40d45070a592b0096f9e15105aee55419dc9f88f125f7cd06 |

| Gateway B | CH343 COM9 | ESP32-S3 rev0.2 / e0:72:a1:f6:a9:68 | 16 MB | d82e8fa66e72a35e0e6a56e86ab88cbd209b0d1e89ab7c54af2eba1ae25f0273 |

| Room 2 C | CH9102 COM11 | ESP32-D0WD-V3 rev3.1 / 80:f3:da:63:58:fc | 4 MB | 1bbcd5b971233ae02bdd9a65635ab9d0f029a49b8d48c97333aa875cfb611e57 |



A/B MACs match expected identities. C requires unplug/hold BOOT/reconnect and no-reset ROM connection; automatic reset fails. Never blindly retry. Both sensors are user-confirmed DHT11. A wiring: DHT7, LM393 DO6, LEDs4/5. C: DHT21, LEDs18/19. All LEDs active HIGH, separate 10k resistors, no relays. PIR disabled; C has no light sensor.



All three original full-flash backups have independently verified on-device MD5 and two local copies under `benchmarks/backups/three-board-20261102` and `output/private-rollback-three-board-20261102`. These contain private material; do not upload. Original credentials, signer, Vosk assets and backups preserved. New C provisioning is separate; no A/B credential regeneration.



C retains its existing ESP-AT legacy MD5-less table: phy_init 0xf000/0x1000; otadata 0x10000/0x2000; nvs 0x12000/0xe000; at_customize 0x20000/0xe0000; ota_0 0x100000/0x180000; ota_1 0x280000/0x180000. CRC-valid OTA metadata selects ota_0. No partition/NVS erase or layout change. C's initial deployed application could not open NVS because its SDK required a table MD5. Only C was rebuilt with CONFIG_PARTITION_TABLE_MD5 disabled to accept its preserved table. External guard still verifies actual table/layout. Current C boots normal tasks.



Deployment used `scripts/deploy_three_board.py`: gateway first, then A/C; B was updated again before C's compatibility repair to permit diagnostic room queries. Each current image passed application readback plus independent full-flash MD5 against the exact expected result, preserving all bytes outside the application including the final-sector tail. Mutable NVS/OTA are freshly read before writing; normal application NVS updates still occur. Old failed startup evidence remains intact.



Current A receipt: `benchmarks/review/three-board-20261102/deployment-A.json`. Current B/C receipts: `benchmarks/review/three-board-20261102/c-legacy-runtime/deployment-{B,C}.json`. Sealed candidates, source provenance, plans and backup receipts are alongside these. Initial superseded images must not be called current.



## Android and validation



Installed APK: `deliverables/ClawGrid-Three-Room-debug-20261101d.apk`, version 1.0-three-room-20261101d, SHA256 d4b18b300d1b430bd175a75e80468fed1a8bfef70558392430b40b88e49e55c4. Installed APK was pulled and hash-matched. Existing signer SHA256 71ce1bf6f415db7a355e7c63d941c53e7b52fba6a4c33c25042582af0a464bf9. Signature, alignment, bundled model and privacy checks passed. All 75 source/build/model provenance entries match the existing APK snapshot. Install used -r and preserved app data; existing local connection token was restored privately without recording its value.



Vosk small English 0.15 model ZIP SHA256 30f26242c4eb449f948e42cb302dd7a686cb29a3423a8367f99ff41780942498; Vosk0.3.75/JNA5.18.1, fixed0.7 word gate. Recognizer/model unchanged. No raw microphone audio retained or new accuracy claim.



Fresh validation: 132 host tests PASS; 95 JVM tests PASS. Later focused legacy-table/guard/provision/room-gateway checks: 32 PASS (overlap, not additive). A/B/C builds passed, then repaired C and B bridge builds passed. Evidence: `host-final.log`, `jvm-final.log`, `legacy-final-tests.log`, `build-C-legacy-compatible.log`, `build-B-room-bridge.log` in the 2 November review folder.



## Acceptance checkpoint



Both rooms report authenticated BOARD/fresh telemetry. Observed controller boot identities: A72, C1. A DHT11 reports real values around 29.7C. C DHT11 currently reports TIMEOUT with null temperature/humidity: real Room2 DHT acceptance BLOCKED pending diagnosis. C tasks and link advance. Room1 LM393 is available/raw HIGH, polarity UNVERIFIED; no lux or daylight automation claim.



User confirmed baseline all four LEDs OFF. Room1 light and fan ON/OFF tests: user observed only intended LED switch, all four OFF afterward. Hardware evidence confirms node1/boot72 applied ACK and fresh matching readback: light ON#1/OFF#2, fan ON#3/final OFF#5. Fan OFF#4 was dwell-rejected and must not be labelled applied. Room2 node3/boot1 remained OFF with command0 throughout. Evidence `manual-room1-light.json` contains both sequential tests; derived checks `manual-room1-verdict.json`. Physical observations are separately recorded in `USER_OBSERVATIONS.json`.



Room2 light trial is the next guided step, currently being observed using bounded read-only `observe_three_rooms.py`. It sends no actuator commands and closes its handles in finally. Before starting another observer, ensure the preceding one has closed. Observer stop file `<phase>.stop` ends a current new observer early and retains evidence.



Remaining: Room2 manual light/fan and reverse isolation; diagnose Room2 DHT and test unavailable/stale/recovery; establish Room1 LM393 bright/dark raw polarity by observation; independent schedules/replacement/cancel/manual priority and phone-disconnected execution; OFF caps/labelled assumed-watt runtime estimates; room-targeted Vosk preview/confirm/cancel; room reboot/disconnection/no replay. Guide one short step at a time. Never infer visible LED state from ACKs. Finish with fresh software OFF plus user four-LED OFF observation and close owned serial sessions.



## Android acceptance repair — current APK supersedes 1 November APK



The first Room2 manual trial was blocked by a real UI defect: room-card enabled checks still restricted physical control to bedroom and used the selected global repository for every card. Room2 commands were not dispatched. Fixed per-card repository binding/provisioned capability, Room2 status text and selected-room KPI labels. Controls retain fresh-state and active-request guards; unprovisioned cards remain disabled and all actuation still uses proposal/confirmation/SafetyKernel.



Current installed APK is `deliverables/ClawGrid-Three-Room-debug-20261102a.apk`, version 1.0-three-room-20261102a, SHA256 `1bad8be832e2f35bb091f02afa494c4731853f526de0ff38b4bed51bb68fcdb9`. Build passed; 95 control/room-isolation JVM tests passed again. Existing signer, APK alignment, exact bundled Vosk model and privacy checks passed; installed APK pulled and hash matched. Evidence `benchmarks/review/three-board-20261102/android-room-controls-final/`. The first attempted build used an empty cache and was stopped; the successful final snapshot reuses the existing verified dependency cache. No firmware update accompanied this repair. Existing gateway token restored privately to the memory-only UI field after restart. On-phone enabled-state verification and renewed guided Room2 trial pending.


## Latest sensor results: C DHT recovered with intermittent dropouts

User reports Room2 ON/OFF controls now work; detailed isolated physical trials were not individually captured. Current C diagnostic app SHA256 `8a411d00d95d402f0a89a591d4af2e07a0b2c97caebe57d5fb0172565127e96b`; `c-dht-timing/deployment-C.json` proves app readback and protected-region preservation at0x100000. A/B current images unchanged; matching existing gateway-first receipt reused. C-only diagnostic build passed and82 firmware/sensor/safety/wire/provision/guard cases passed. Decoder limits/checksum are unchanged. The diagnostic logs at most3 invalid timing frames per boot; no raw microphone data.

Before user wiring reseat: C TIMEOUT/1edge. After reseat: INVALID/83edges. After guarded diagnostic update/restart: C boot2 reports real valid83edge frames. `DHT-recovered-room-state.json` shows fresh authenticated node3 approximately30.9-31.0C/64-65%RH, light/fanOFF and last_command0. A boot72 unchanged/OFF, command5. This observed reboot did not execute old ON commands. Exact cause of the preceding INVALID readings is not isolated; do not claim a decoder fix or calibration.

C passive diagnostic capture `c-dht-timing/runtime.json` contains9 valid samples and2 TIMEOUTs, then recovery (11 total). Timeout diagnostic has only one edge, with durations10/14us. Its COM11 session closed. Therefore real-reading recovery PASS, continuous sensor reliability unresolved. New capture under covered light-sensor condition shows C statusOK in4/4 gateway samples. No masking of invalid readings: unavailable/null handling remains.

Room1 LM393: user covered the sensor; `LM393-covered.json` records4/4 available rawHIGH. Illuminated comparison in progress; no polarity yet assigned. Both rooms reportedOFF throughout that covered capture.

## LM393 polarity accepted; DHT reliability still under diagnosis

`LM393-polarity-verdict.json`: user-covered4/4HIGH, user-torch-illuminated4/4LOW; available/fresh node1 boot72; both room outputsOFF throughout. Pot unchanged. Observed darkHIGH/brightLOW is established; firmware polarity metadata remainsUNVERIFIED and no lux/daylight automation is implemented.

User confirms C DHT11 smallPCB/3pin module. `C-DHT-stability.json`:45seconds,18samples,17OK/1INVALID followed by recovery. Do not report continuous stability PASS. Fresh room API samples also record availability and null handling independently. One controlled C restart with passive pulse timing/isolation capture is pending/in progress to diagnose the invalid frame; C diagnostic quota is3 invalid frames per boot.
