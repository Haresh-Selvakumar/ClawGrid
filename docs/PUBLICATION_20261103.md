# Source publication verification — 3 November 2026

This replaces the 28 October source snapshot on `main` with the current local implementation. The latest scope is documented in [Physical app controls](PHYSICAL_APP_CONTROLS_20261102.md): Room 1 sensor AUTO and existing Room 2 manual/voice controls. Historical handoff sections retain earlier scopes.

## Checks performed for this publication

All results below are HOST_MEASURED. No hardware was flashed, no APK was installed and no actuator commands were sent during publication.

| Check | Result |
| --- | --- |
| Selected firmware, protocol, planning, sensor order/AUTO, provisioning, safety and proposal tests | 22 passed |
| Current Android control JVM suite | 114 run; 100 passed; 14 failed |
| Configured private-value, private-key/token-pattern and privileged-JWT scan, including nested archives | PASS |
| Offline Vosk model archive SHA256 against existing manifest | PASS |
| Git whitespace/error check | PASS |

The host test command was:

```powershell
python -m pytest tests/unit/test_sensor_order_cpp.py tests/unit/test_sensor_auto_cpp.py tests/unit/test_planning_cpp.py tests/unit/test_room_gateway_cpp.py tests/unit/test_room2_provision.py tests/unit/test_safety_kernel.py tests/unit/test_phone_proposal.py tests/protocol/test_wire_contract.py -q -p no:cacheprovider
python scripts/check_android_control_jvm.py
```

The firmware tests used the existing pinned Zig 0.13.0 host compiler. Initial sandbox runs could not compile the C/C++ tests and prevented four local HTTP tests; the results in the table come from reruns with those restrictions removed.

## Outstanding Android failures

The current suite fails two cases in `HardwareRepositoryTest`, eleven in `LocalVoiceTest` and one in `PhoneOfflineVoiceTest`. The failing cases cover fan-specific manual/ACK expectations, prior per-boot manual verification, local speech delivery/recognition/proposal handling and fan manual-proof gating. Their cause has not been fully investigated in this publication task. Several assertions concern prerequisites changed by the latest app scope; that observation does not establish that every failure is a stale test.

This is the requested current-source publication, with unresolved test failures disclosed. The older checkpoint's 114-pass result must not be attributed to this later source. No new complete firmware/Android build or physical acceptance campaign was performed here. Physical scene actuation remains untested, and historical Room 2 watchdog limitations remain unresolved.

## Publication boundary

Private credentials, signing keys, board dumps, compiled firmware, APKs, local SDK configuration, build caches and raw private diagnostics are excluded. The existing offline Vosk archive is included at `ecosystem/android/app/src/main/assets/offline-voice/model.zip`; its SHA256 is `30f26242c4eb449f948e42cb302dd7a686cb29a3423a8367f99ff41780942498`. Attribution and its manifest remain alongside it.

The scan checks configured private values and selected known secret patterns; it is not a general security audit. The obsolete duplicate web-baseline and superseded delivery drafts remain excluded. Supporting dated documents and the Room 2 implementation are retained. Existing Git history remains available.
