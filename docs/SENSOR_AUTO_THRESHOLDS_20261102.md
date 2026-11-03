# Sensor AUTO and app temperature thresholds — 2 November 2026

App path: Rooms & Relays → select Room 1 / Room 2 → select fan in the room's timing/settings panel → Edit AUTO settings. Fan ON and OFF temperatures accept tenths of a degree over 0–50°C with OFF strictly below ON. Draft defaults are ON28°C/OFF26°C. Review settings, then Authorize; read the separately labelled accepted values. Applying settings does not enable AUTO. Explicitly review/enable AUTO to use them. Each room and device has its own configuration.

Controller behavior:

- Valid temperature ≥ON sets the fan sensor demand ON; ≤OFF sets it OFF. Inside the band retain SENSOR demand, independently of the actual LED or scheduled lease. Explicit AUTO entry inside the band initializes demand OFF.
- Room1 light uses the physically verified filtered LM393 DO mapping: dark HIGH→ON, bright LOW→OFF. This is bright/dark, not lux. Room2 light has no sensor and supports schedule-only AUTO; Sensors first is unavailable.
- Priority is per device: Schedules first lets a live lease win against valid sensor demand; Sensors first uses the sensor and reports conflicting scheduled requests as overridden. The finite lease is3–3600 whole seconds, starting at the scheduled due action's controller acceptance. Expiry reevaluates the sensor; neither polling, reconnect nor configuration edits renews it.
- Invalid/stale required input clears sensor demand to OFF, discards an active lease, and requests OFF through SafetyKernel, subject to dwell. No last-good value or implicit schedule fallback. Recovery uses fresh input; a fresh demand can operate again, but the discarded lease is not replayed. A sensor fault is distinct from a forced protection hold.
- Successful manual/confirmed voice commands hold that device MANUAL and cancel pending clock schedules and active overrides. Arming a schedule or changing priority does not switch MANUAL to AUTO. Explicit AUTO entry clears pending schedules/overrides and releases that device's hold. MANUAL-mode selection freezes the current output; use manual OFF to turn it OFF.
- Dwell and maximum-run protection remain. Continuous ON cap counts from the actual ON transition; OFF countdown starts at controller configuration acceptance; the schedule lease controls source ownership only. Protection OFF cancels automatic work and holds OFF until an explicit manual action or AUTO release. No automatic re-energizing from continuing demand after protection cutoff.
- Boot always starts new sensor-rule devices MANUAL/OFF with no active lease, countdown, cap or pending clock schedule. Accepted priority/threshold/lease-duration settings persist under a new CRC-protected optional NVS key. Existing policy/manual/provisioning keys are preserved. AUTO does not restart on install/reboot.

Protocol and app:

- Report schema8 (108bytes), features_version2; gateway decodes historical schemas3–7. Op9 atomically configures per-device thresholds/priority/lease length; Op10 selects MANUAL/AUTO. New configuration commands require explicit room/controller boot and exact applied ACK plus newer matching authoritative readback. Values are rejected rather than silently clamped. HTTP202 alone is not completion.
- New gateway must be installed before new controllers. Old controller reports remain readable but cannot enable the new settings UI. New controllers cannot be used with an old gateway that rejects schema8. C remains on its existing capture-priority image while stability is unresolved; its new image is only a built/guard-checked candidate.
- App shows accepted mode, priority, ON/OFF thresholds, sensor demand, active lease/remaining time, winning source and blocking reason. Stale/missing reports show unavailable. Draft values are not presented as accepted configuration. Model/parser/voice thresholds/signing/credentials remain unchanged; PIR remains excluded.

Validation and artifact hashes: `benchmarks/review/sensor-auto-20261102/validation.json` and `ARTIFACTS.json`. Pre-change source snapshots and failed/intermediate checks are retained in that folder. Guarded deployment plans/receipts are in `benchmarks/review/three-board-20261102/sensor-auto/`.

Hardware acceptance and final installed-device status are recorded in the current campaign checkpoint; this implementation description does not imply hardware acceptance. The unresolved C watchdog and outstanding negative speech coverage remain open.

## Final observed acceptance and ending state

Authoritative summary: `benchmarks/review/sensor-auto-20261102/FINAL_RESULTS.json`; separate visible observations: `USER_OBSERVATIONS.json` there. Host/JVM/build details and retained failures are in `validation.json`.

| Room / device / feature | Result | Evidence within sensor-auto-20261102 |
|---|---|---|
| Room1 light sensor AUTO | PASS in observed darkHIGH/brightLOW window, user confirmed cover/illumination | `threshold-verdict.json` (raw phone capture referenced there) |
| Room1 fan editable thresholds | PASS: accepted27/26 at real27.5C→ON; accepted30/29→OFF; user observed both | `threshold-verdict.json` |
| Room1 manual priority | PASS appliedmanualOFF#10 cancelled pendingON#9 before fallback; olddue passed;10 freshsensorON-demand samples heldOFF | `manual-priority.json` |
| Room1 fan Schedules first | PASS15slease ON against valid sensorOFF, expiry returnedOFF; user intendedLEDonly | `schedules-first-isolated.json` |
| Room1 fan Sensors first | PASS scheduledON reportedoverridden, noON, leaseexpired; userallfourOFF | `sensors-first.json` |
| Room1 fan OFF countdown | PASS fresh60scountdown accepted afterON, earlier than120scap, automaticOFF + forcedhold | `off-countdown.json` |
| Room1 fan continuous ON cap | PASS corrected60scap, earlier than120scountdown, automaticOFF + forcedhold | `continuous-cap-corrected.json` |
| Room2 light/fan new AUTO | BLOCKED live deployment/acceptance; correctCtarget build and guards PASS only | `ARTIFACTS.json`, guard plans below |
| C stability and voice | BLOCKED earlier watchdog cause unresolved; noCfix/deployment/hostONtrial | earlier `resume-C-stability-assessment.json` under three-board-acceptance-20261102 |

The successful countdown and corrected cap each bracket the controller software-state OFF transition within2000ms, with unchanged command counter. Countdown OFF left the120000ms cap configured; corrected cap OFF left62740ms on the later countdown. The corrected cap's cumulative software runtime addition was60000ms; countdown case added64000ms (ON preceded authorization). Cap had9postOFF samples with no re-energizing; countdown had7. These are controller state/accounting and independent USER_REPORTED LED observations, not direct electrical or oscilloscope timing.

Both priority cases captured decreasing lease remaining time without polling renewal. Manual priority was checked independently of thresholds: while manual heldOFF, real input requestedON and winning source remainedManual/blockingManualhold. The first fan nonactivation report was an actualMANUAL hold; explicitAUTO#4 then correctly stayedOFF at27.4C inside the28/26band. No source defect was found in that interaction.

Retained non-passes: `schedules-first.json` preflight expectedMANUAL after newuserAUTO actions; `schedules-first-ready.json` setup ACK was superseded by another usermanualOFF. Neither armed scheduledON. The first cap capture `continuous-cap.json` lost a diagnostic response before deadline and isINCOMPLETE, not a timingPASS. Read-only postgap/cleanup is retained in `cap-gap-cleanup.json` and the referenced three-board-acceptance observer. Corrected capture has no read gaps. The fixture now permits one recorded read-only retry on absent/502 diagnostic response; it never retries actuator/configuration POSTs. No firmware or watchdog change was used to conceal that capture gap.

User intentionally operated C lightON#1/OFF#2 in the earlier mixed phone-settings window. This is recorded separately, not unexplained cross-room actuation and not an isolated whole-window isolationPASS. Subsequent isolated fixtures held Cboot5/command2/OFF unchanged, with no host C writes.

Latest final read-only runtime evidence: `final-runtime.json`. Aboot73/command78; Cboot5/command2; allfour outputsOFF; Aauto_flags0/manual_flags3; noarmed schedules, active leases, countdowns orcaps. Fan preferredON28/OFF26, Schedules first, override60s restored; AUTO remains disabled. User confirmed finalallfourOFF. Acontrol/link/sensor jobs advanced, sensorwatchdogtrue, diagnostic fault injectionfalse. All owned serial handles are closed; no observer/test worker remained. Phone keep-awake remains original0.

Remaining physical coverage is explicit: these priority/timing fixtures used Room1fan, not separate light-specific conflict/timer cases; no reboot of edited/persisted settings, induced sensorfault whileAUTO active, full late/dwell/reconnect campaign orlong endurance is established. Host regressions cover those implemented logic branches. Prior voice acceptance on Aboot72 is historical; newboot73/currentAPK microphone trials were not repeated. Remaining negative speech and C stability/voice remainopen. No calibration, electrical feedback, measured appliance savings or new speech accuracy claim.

## Exact artifacts and rollout

B first, then A app-only deployment/readback/full-flash preservation PASS: `benchmarks/review/three-board-20261102/sensor-auto/deployment-B.json` and `deployment-A.json`. FreshNVS/OTA and every byte outside the application, including erase-sector tail, were preserved. C was not connected to ROM or flashed. Dual expected-current and predeploy backups are private and preserved. Current C remains capture-priority SHA256 `1bbcd5b971233ae02bdd9a65635ab9d0f029a49b8d48c97333aa875cfb611e57`.

- A, `native_voice_uart_led_high_bench`: SHA256 `fffc4245a10c77ee9342e6a1f7c4a5189e7b8b5b18e5bf03213fa65778702bd9`; PASS application readback and full-flash preservation.
- B, `native_gateway_three_room`: SHA256 `eeab1c9338c2d8429717a85b5f35611faf423f1abf84ecc77110d6db2c86b289`; PASS application readback and full-flash preservation.
- C, `room_c_esp32`: SHA256 `3866aeec7cf4ca35215fd835278db65b6bc5d26053c4cd8b59e2e482c32167b6`; NOT DEPLOYED; stability blocker; candidate only.
- Installed APK `1.0-sensor-auto-20261102d`: SHA256 `bf91cf4445d3420d4f63d89aa9d0ef09a7afd1a6b808b7094853b04f0057107e`; installed base freshly hashmatched.

Existing signer SHA256 `71ce1bf6f415db7a355e7c63d941c53e7b52fba6a4c33c25042582af0a464bf9`; alignment16KB/privacy checks PASS. Bundled model archive and manifest exactly match previousAPK-c (`model-preservation.json`), modelZIP SHA256 `30f26242c4eb449f948e42cb302dd7a686cb29a3423a8367f99ff41780942498`.

HOST_MEASURED:109 Android JVM tests;75newAUTO assertions;87existing timing assertions. The affected106unique host tests passed across initial run and targeted recheck. Initial fixture and cold-compiler failures are retained with their resolutions, not omitted. All3correctfirmwaretargets and theAPK built. NewC is a sealed candidate only and must not be deployed as a supposed watchdogfix.
