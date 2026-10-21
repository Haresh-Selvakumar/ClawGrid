# Hardware status

Latest watchdog follow-up, 21 October: [controlled validation completed](../benchmarks/reports/WATCHDOG_VALIDATION_20261021.md): **3/3 A task-watchdog trials, 69/69 campaign checks and 16/16 restoration checks passed; 144 host tests pass**. A's expected MAC is verified on COM9 through CH343 USB UART0, B remains COM8 native USB. A UART-console variant resolved the connector blocker without changing watchdog settings. Both final images are inhibited and fault/replay hooks are disabled; saved policy and credentials are preserved. Fresh schema-3 telemetry and normal task/command progress were verified after recovery and restoration. Cached packets can keep `known=true` during a control stall; the report records this limitation and uses task counters/boot identities for recovery proof. Both serial ports are released. No PIR, relays or loads were connected.

Earlier USB-only bring-up on 21 October 2026: **both updated inhibited roles were deployed and 17/17 checks passed**. That milestone used A COM7 (`e0:72:a1:f6:a9:68`) and B COM8 (`e0:72:a1:f5:bc:b0`). Both report ESP32-S3 revision 0.2, 16 MB flash and 8 MB PSRAM. See [the earlier bring-up record](../benchmarks/reports/USB_BRINGUP_20261021.md) for its hashes, commands, boot logs and evidence. The watchdog follow-up above records the current A COM9 UART deployment.

Original installed credentials were recovered and verified before rebuilding; no keys were regenerated. Flash writes excluded NVS, saved policy was preserved, and fresh authenticated schema-3 telemetry confirms output inhibition. One confirmed virtual OFF command received an ACK. Both serial ports are released. No watchdog fault injection, physical-target command, bench image deployment or physical output enablement occurred. Speech initialized, but recognition accuracy, PIR accuracy and real switching remain unverified.

Historical port defaults in deployment/resume scripts are stale. This campaign used explicit `--port COM7` for A and `--port COM8` for B-speech. The broader resume campaign includes recovery/fault work and was not run in this bounded session.

## Earlier software-only milestone

Earlier work on 21 October 2026 had **no hardware available**. Default builds inhibit outputs; the separate bench configuration remains opt-in until the electrical checklist is completed.

The previous retained hardware campaign is [the returned pair addendum](../benchmarks/reports/RETURNED_PAIR_20261020.md): both boards were present in that 20 October session; paired checks 81/81, software OFF outcomes 1000/1000, and recovery 12/12. It superseded the earlier absent-A snapshot below. Those historical port assignments do not establish current connections. Speech remained 0/16 synthetic matches and no wired physical campaign exists.

Use [the current software handoff](../NEXT_CHAT_HANDOFF.md) for fresh host/build validation and later hardware actions. No discovery, flashing, deployment, eFuse changes, or phone changes were performed during the software work.

## Earlier 20 October snapshot

Node B has returned on COM7 (MAC e0:72:a1:f5:bc:b0), ESP32-S3 N16R8. It is flashed with the current native speech/API/observation-monitor image. Node A COM5 (MAC e0:72:a1:f6:a9:68) remains unavailable; its updated native image compiled but has not been flashed. Nothing is wired. Physical outputs remain inhibited. No phone settings or Wi-Fi network changes were made.

Current B-only campaign:32/32 API/shadow rows passed, including23 monitor cases. Raw trace: `benchmarks/raw/hardware/single_b_20261020T080754Z.json`. Current host regression:83 passed. A separate 18-input B speech run completed with matching PCM CRCs but no hits on16 TTS commands; two negatives gave no command. Useful recognition is still an open functional issue, and a real-voice diagnostic sample has been requested.

The historical paired run passed81/81 controller checks and1000/1000 commands on older images. Those data are retained and do not certify the latest pair. Recovery final verification, physical feedback, energy, UPS, power cycling, RF coexistence and long-duration tests remain incomplete or unmeasured.

Port8765 is the offline interface preview. Port8766 is the active serial byte bridge to B. Stop only that owned serial bridge before any COM7 flash/serial test. When A returns, `python scripts/resume_validation.py --deploy-staged` deploys both reviewed images with MAC checks and preserved NVS, then runs paired tests. It does not enable physical outputs. Expect audio failure until recognition is repaired.

[Current evidence](../benchmarks/reports/PROJECT_VALIDATION_SUMMARY.md) · [Review response](DEEP_REVIEW_RESPONSE.md) · [Handoff](../NEXT_CHAT_HANDOFF.md).
