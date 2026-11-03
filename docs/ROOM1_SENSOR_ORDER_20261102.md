# Final physical demonstration: Room1 only — 2 November 2026

This supersedes the earlier two-room final acceptance scope. Room2 stability, deployment and physical acceptance work is stopped. Its source, provisioning, backups and historical evidence remain preserved. Room2 is unavailable/unverified in the physical UI and outside final physical acceptance; no fresh Room2 state or command acknowledgement is inferred. No C target is built or deployed for this change.

## Independent settings

Room1 sensor-evaluation priority is one room-wide setting: Light sensor first or Temperature sensor first. It selects evaluation order within the existing bounded ControlTask cycle. It does not select the winning control source, change FreeRTOS priorities, insert delays, enableAUTO, release holds or renew leases.

Per-device source priority remains independent: Schedules first / Sensors first (vs schedules), with finite schedule overrides. Fan hysteresis, LM393 filtering, manual holds, countdowns, continuous ON caps and SafetyKernel remain in effect.

## Snapshot and controller evidence

ControlTask reads the filtered LM393 state once and copies the latest DHT11 mailbox entry once before commands/arbitration. The same immutable input snapshot is used by both rules and published with the resulting output/control state. This is a coherent software snapshot of independently acquired samples, not simultaneous physical acquisition. The DHT11 age is independent of the filtered light sample age; latest acquisition-attempt age is exposed even when temperature is invalid. Invalid/stale input is unavailable and requests the existing fault-OFF policy; no previous good temperature is substituted.

Each cycle evaluates both sensor/device rules once in the selected order. A return/block in the first rule does not bypass the second. Fan demand retains its own hysteresis memory independently of LED/schedule state. Both rule evaluations record their actual ordinal, requested action, result, controller start microsecond counter and elapsed microseconds. A cycle counter and input snapshot uptime bind the pair. Timing uses the existing wrapping32-bit ESP timer; host tests use a separately labelled controlled clock. Unavailable timing is explicitly flagged, never manufactured. Live telemetry is a bounded latest-cycle snapshot at up to4Hz, not a lossless log of all200Hz controller cycles.

Rule result codes:0 not evaluated;1 request accepted/satisfied (including no-op);2 manual hold;3 safety/OFF protection hold;4 SafetyKernel dwell;5 AWAY;6 invalid input, faultOFF accepted;7 schedule source has control. Input values/ages and per-device source/blocking details are visible together in the app. LED appearance is not used to infer order.

## Transactions, compatibility and scope guard

Op11 configures Room1 sensor order, with explicit boot, exact applied ACK and newer matching readback. It rejects other rooms/devices, invalid values and unsupported firmware. Order persists in a separate CRC-protected optional NVS key; missing key defaults Light first. Boot always restores MANUAL/OFF and no active automatic work. Order changes themselves do not affect mode/output or source-priority configuration.

Report schema9 is152bytes with Room1 features_version3. The gateway retains decoding of older report schemas. New gateway must precede new A. Existing per-device configuration/features2 remains compatible. Gateway scope flag `CLAWGRID_ROOM1_DEMO_ONLY` rejects Room2 HTTP routes/dispatch and ignores Room2 reports. The app constructs/polls only the Room1 hardware repository; Room2 controls/voice selection are disabled and marked unavailable/unverified. Retained Room2 implementation is not deleted.

Only A/B targets and APK-e are built. Use `scripts/deploy_three_board.py --room1-only` with the checked A/B revision; this scope refuses C and checks A/B readiness plus gateway-first receipt while retaining all application-only/protected-flash guards.

Validation and hashes: `benchmarks/review/room1-sensor-order-20261102/`. Host92tests (including39newordering assertions,75existingAUTO assertions and87timing assertions) and114JVMtests pass. An initial reused-report manual-flag accumulation issue was fixed by clearing report flags before publishing current registry values. The old monitoring-only wording expectation was updated; earlier failed logs are retained. No watchdog timeout, task feeding or task priority was changed.

A/B deployment PASS with application readback and protected full-flash preservation; APK-e installed and hashmatched. Fresh read-only baseline: A boot74/cmd0, both Room1 outputs OFF, MANUAL flags3/AUTO0, no active timing work. Both readings are fresh and both rules report real microsecond timing and Light step1/Fan step2; this baseline alone is not AUTO or changed-order acceptance. Short live phone demonstration is pending user ready; read the campaign CHECKPOINT before any hardware access. Historical passed Room1 evidence under sensor-auto-20261102 remains preserved, not rerun wholesale.
