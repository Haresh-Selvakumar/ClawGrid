# ClawGrid Embedded Home Automation

Current source snapshot: 3 November 2026. ClawGrid uses a native Android app, a local ESP-NOW gateway and deterministic controller firmware for low-voltage light/fan LED demonstrations.

Room 1 supports DHT11 temperature and LM393 digital-light sensor AUTO, configurable fan hysteresis, sensor evaluation order, schedule-versus-sensor priority, manual holds, schedules, countdowns and continuous ON caps. Room 2 retains its existing manual and voice controls; it receives no new sensor AUTO firmware. Occupancy and measured lux are unavailable. The four-room lab is explicitly simulated.

The latest app provides reviewed physical scene commands. Multi-room execution is sequential and can partially apply; matching command ACK and fresh authoritative readback determine success. Physical scene actuation has not been acceptance-tested. Existing Room 2 watchdog limitations remain documented; restored connectivity does not establish a watchdog repair.

Offline Vosk recognition produces a room-bound intent preview requiring explicit authorization. The latest app removes the earlier per-boot manual-proof voice prerequisite while retaining freshness, output-profile, confirmation and ACK/readback checks. Local operation does not require Internet or cloud speech.

## Current records

- [Latest physical app controls and limitations](docs/PHYSICAL_APP_CONTROLS_20261102.md)
- [Room 1 sensor evaluation order](docs/ROOM1_SENSOR_ORDER_20261102.md)
- [Sensor AUTO thresholds and observed acceptance](docs/SENSOR_AUTO_THRESHOLDS_20261102.md)
- [Publication verification](docs/PUBLICATION_20261103.md)
- [Implementation history](CLAWGRID_MASTER_INTEGRATION.md) and [retained handoff](NEXT_CHAT_HANDOFF.md)

The physical app controls document supersedes the older Room1-only UI scope in the retained handoff. Dated records describe their own checkpoint; historical test results do not establish that every current-source test passes.

## Source and build entry points

- `ecosystem/android/`: native Android app, tests and pinned offline Vosk model with notices.
- `native/platformio.ini`: controller profiles, including the active-HIGH LED profile and retained Room 2 target.
- `speech/platformio.ini`: gateway profiles.
- `firmware/common/`: safety, protocol, scheduling and sensor policy.
- `scripts/` and `tests/`: host checks and guarded deployment tools.

Controller A Room 1 wiring: light GPIO4, fan GPIO5, DHT11 DATA GPIO7 and LM393 DO GPIO6. Each LED uses its own series resistor; indicators are active-HIGH. SafetyKernel remains authoritative. Startup restores outputs OFF; private provisioning and protected flash regions must be preserved. Verify actual board identities before deployment. Mains switching is outside this prototype.

Private credentials, signing identities, board dumps, compiled firmware, APKs, local diagnostics and build caches are excluded. Provision private configuration locally using the checked-in examples and tools. The pinned offline model archive is included and verified against its existing manifest.

This update replaces the earlier source on `main`. Superseded delivery drafts and the obsolete duplicate web-baseline remain excluded; dated supporting evidence and the existing Room 2 implementation are retained.
