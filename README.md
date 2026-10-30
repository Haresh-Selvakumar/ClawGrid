# ClawGrid Embedded Home Automation

The current physical prototype uses two ESP32-S3 boards, a native Android app, two LED indicators and a DHT11. The verified integration checkpoint is dated 28 October 2026.

## Current implementation

- Controller A owns SafetyKernel and output transitions: active-HIGH light GPIO4 and fan GPIO5. Each LED has its own 10 kΩ series resistor; cathodes connect to ground.
- DHT11 DATA connects to GPIO7. Validity and freshness are reported; failed or stale readings are unavailable.
- Gateway B provides the local Android API and authenticated ESP-NOW transport to A.
- The existing bundled offline Vosk phone model produces proposals. Explicit confirmation is required before dispatch; completion requires matching controller ACK and fresh authoritative state.
- PIR is removed. Physical occupancy and occupancy-driven AUTO are unavailable. Simulation remains separately labelled. Relays and appliance loads are unused.
- Startup holds indicators OFF. Watchdogs, bounded queues, minimum dwell, run limits and boot-context checks remain enabled.

## Verified results and limits

See [demonstration results and installed hashes](docs/VOICE_LED_DEMONSTRATION_20261028.md), [embedded task review](docs/EMBEDDED_LED_REVIEW_20261028.md), [master integration](CLAWGRID_MASTER_INTEGRATION.md), and [handoff](NEXT_CHAT_HANDOFF.md).

Manual LED operation and eight positive microphone trials have user-reported visible observations with retained controller ACK/readback evidence. The negative/cancellation capture contains additional deliberate commands and is not an isolated zero-dispatch proof for each utterance. The final APK changes recovery status messaging; voice trials were on its preceding version with unchanged recognition/parser sources. Short task observations do not establish hard real-time guarantees or a recognition accuracy rate.

## Source and build entry points

- `ecosystem/android/`: native Android project and bundled offline model with notices.
- `native/platformio.ini`: controller build profiles, including the explicit active-HIGH LED-only profile.
- `speech/platformio.ini`: gateway build profiles.
- `firmware/common/`: shared transport, safety and runtime implementation.
- `tests/` and `scripts/`: host verification and guarded deployment utilities.

Provision private configuration locally from the checked-in examples. Credentials, signing keys, board dumps, compiled firmware, APKs, private diagnostics and build caches are excluded from publication. Never erase provisioning or model partitions as part of a routine update. Verify board identities rather than assuming COM-port roles.

The obsolete duplicate web-baseline tree and superseded delivery drafts have been removed from the current branch. Historical documents elsewhere remain dated supporting evidence, not the current hardware specification. No completed final submission report is included in this source update.
