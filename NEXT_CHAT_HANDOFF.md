# ClawGrid software handoff

**Latest hardware update, 21 October 2026:** [controlled watchdog validation completed](benchmarks/reports/WATCHDOG_VALIDATION_20261021.md): **3/3 A watchdog trials, 69/69 campaign checks and 16/16 restoration checks passed; 144 host tests pass**. The connector blocker was resolved using a pinned-SDK UART0 console: verified A is COM9 through CH343, B remains native USB COM8. Final deployed images are inhibited `native_voice_uart` and `speech_intelligence`, both with fault/replay hooks disabled. Original credentials and saved policy are preserved; no NVS erase or physical-target command occurred. Boot IDs 20→21→22→23 were task-watchdog resets (cause 6); final restoration boot 25 is a separate normal flash reset. Control/link task progress, fresh schema-3 telemetry and old-boot command/confirmation rejection passed. Cached reports can keep B's `known` flag true while A's control task is stalled; use task/boot evidence for health. All serial handles are released. Earlier hardware/software milestones below remain historical.

**Earlier USB bring-up, 21 October 2026:** both updated default inhibited images were deployed with original installed credentials and NVS policy preserved. At that earlier milestone, MAC-verified A was COM7 and B COM8; 17/17 bounded checks passed. See [the earlier bring-up record](benchmarks/reports/USB_BRINGUP_20261021.md). The software-only milestone below predates these hardware sessions.

Updated 21 October 2026. No hardware was available or accessed. **122 host tests pass**, frontend checks pass, all 16 supplied WAVs pass format validation, and Node A, Node B speech/API, and the opt-in bench image compile. Real recognition remains unverified after these repairs; the last retained target result was 0/16 synthetic commands.

ESP-IDF 4.4.7 and ESP-SR 1.7.0 remain pinned. No flashing, serial discovery, deployment, eFuse changes, external push, or Raspberry Pi migration occurred. Default outputs remain inhibited. The original archive was retained; a local baseline commit and software-demo-repairs branch preserve reviewability. No pre-existing uncommitted project changes or credentials were overwritten.

## Final status

| Area | Implemented change | Validation performed | Remaining hardware check | Status |
|---|---|---|---|---|
| Baseline and routing | Named physical/virtual targets; exact proposal, target, speech-job and boot binding; one-shot 15-second confirmation | Actual portable C++/SafetyKernel tests including light/fan ON-to-OFF, virtual separation, invalid inputs, missing/mismatched confirmation, stale controller and expiry | Current-image paired API/radio operation | Completed and host-tested |
| Dashboard and host development | Explicit targets, pending/submitted/ACK/rejection/unknown states; independent physical state not measured; persistent host banner and fault fixtures | Node/HTTP checks; browser-observed pending proposal, confirmed ACK, commanded ON with inhibited outputs, and disconnected refusal | Browser microphone and actual B/A runtime | Completed and host-tested |
| Audio evaluation | Per-clip origin, speaker/session/conditions/split; separate outcomes and actual denominators | 16 WAV validations, metadata/summary tests and synthetic API contract fixtures; no host recognition | Labeled human recordings and target results | Completed and host-tested |
| Default firmware A/B | Inhibited A; B speech/API integration, PCM handling and bounded diagnostics; schema 3 mode telemetry | Both original pinned firmware environments compile | Boot, radio/API, memory, watchdog and concurrent speech/control | Compiled but not hardware-tested |
| Opt-in bench and policy | Separate bench build; actual output mode/effective policy visible; default inhibition retained | Bench compiles; portable default/CRC/policy-restore tests and inhibition assertion | GPIO4/5/6 compatibility, actual polarity, startup levels, real PIR/DC loads and NVS recovery | Compiled but not hardware-tested |
| Useful real speech recognition | Browser conversion, portable framing and diagnostics repaired; existing model and 0.7 threshold retained | Portable portions verified; ESP32 recognizer not executed | Repeat original clips and real recordings against pinned reference pipeline | Implemented but unverified |
| Physical and acoustic campaigns | Pending, with no invented measurements | Not executed | Boards, human recordings and validated low-voltage setup | Blocked |
| AFE/WakeNet/I2S, Pi, four physical rooms, metering, UPS and production rollout | Outside this deadline's milestone | No speculative changes | Separate scope/evidence | Deferred |

## Changes and reasons

All repository paths below refer to the project root in the source bundle.

- Routing: `command_target.h` implements bounded named targets and the immutable proposal store. Physical bedroom uses Device; virtual rooms use Simulation. Other-room physical requests refuse explicitly. `control_wire.h` shares the established strict command parser. B adds authenticated prepare/confirm handlers; queueing retains the exact A boot, 3-second freshness, authenticated radio, command TTL, ordered IDs and duplicate protection. All actual requests still reach A SafetyKernel.
- State: A reports its actual enabled/inhibited mode and active-low configuration assumption in room telemetry schema 3, still 44 payload bytes. Old schema 2 is deliberately rejected; deploy both reviewed roles together later. The envelope, command and exact-command ACK formats are unchanged.
- Dashboard: shared client logic binds confirmation to the actual proposal and selected target. Selection changes invalidate pending UI confirmation; a missing/invalid target selector never defaults to a physical device. Physical/virtual controls remain separate. ACKs describe software processing, never verified electrical switching. Root scenes are labeled bedroom scenes.
- Speech: capture uses the actual AudioContext rate and converts 16/44.1/48 kHz audio to bounded mono PCM16LE with clipping and a visible four-second cap. Portable firmware helpers decode byte order, check sizes/levels and zero-pad final model frames. Existing command registration/update, frame querying, 0.7 threshold and one-second appended silence were verified rather than duplicated. Init/job logs are bounded and contain no tokens/raw audio. Invalid candidates/states and worker creation failure report errors. No cloud recognizer or invented transcript was added.
- Evaluation: manifest origin/speaker/session/conditions/split are preserved. Legacy SIMULATED explicitly maps to SYNTHETIC; absent metadata stays UNKNOWN/empty. Expected ID 0 means negative. Results distinguish correct command, wrong command, no detection, negative detection/rejection, engine error and transport error; report attempted/evaluable denominators, per-command counts and source groups. Control ACK failures remain separate. Generated negatives do not contaminate filtered held-out sets. The new human manifest template has a header only, with no invented recordings.
- Host mode: the existing stdlib bridge explicitly compiles/runs actual portable routing, Automation, HouseSimulation and SafetyKernel with mock pins. It has connected/stale/disconnected/reject/timeout scenarios and labeled command-ID fixtures. PCM upload returns 501 because no host recognizer exists. Events are HOST_MEASURED/SIMULATED and never write hardware reports. The real bridge and offline preview never silently fall back to it. Content-addressed host executables avoid replacing a running Windows binary, and compiler subprocesses have time bounds.
- Bench: only `native_voice_bench` opts into outputs. The unchanged pin assumptions are light GPIO4, fan GPIO5, PIR GPIO6, active-low relays; compatibility is unverified. Factory automatic light is ON, automatic fan OFF, grace 120 seconds. Preserved NVS can override these values; diagnostics show effective policy. No transient energized output/timer/hold is restored by policy loading.
- Validation/docs: a single software validator records commands/logs/exit codes. Tests cover routing/auth/freshness, real kernel transitions, PCM, metadata/error outcomes and loopback pages. Existing architecture, protocol, voice, safety, hardware status and README now explain the current software path. Older CURRENT_EVIDENCE.json is annotated as superseded rather than relabeled as fresh evidence.

## Exact commands and results

Final campaign UTC: 20261021T084502Z. All six commands below returned 0. The distribution includes `validation/software-validation.json`, corresponding stdout logs and host JUnit XML. The working copy retains its source record in `benchmarks/review/`.

| Exact command | Exit code | Log in working project |
|---|---:|---|
| `C:\Users\hares\Documents\Codex\2026-10-21\i-h\work\clawgrid-env\Scripts\python.exe -m pytest tests -q --basetemp=C:\Users\hares\Documents\Codex\2026-10-21\i-h\work\source\AUTOHOME-main\benchmarks\review\pytest-20261021T084502Z --junitxml=C:\Users\hares\Documents\Codex\2026-10-21\i-h\work\source\AUTOHOME-main\benchmarks\review\host-20261021T084502Z.xml` | 0 | `benchmarks\review\host-20261021T084502Z.log` |
| `"C:\Program Files\nodejs\node.EXE" scripts/check_frontend.cjs` | 0 | `benchmarks\review\frontend-20261021T084502Z.log` |
| `C:\Users\hares\Documents\Codex\2026-10-21\i-h\work\clawgrid-env\Scripts\python.exe scripts/audio_bench.py --validate-only --output C:\Users\hares\Documents\Codex\2026-10-21\i-h\work\source\AUTOHOME-main\benchmarks\review\dataset-20261021T084502Z.json` | 0 | `benchmarks\review\dataset-20261021T084502Z.log` |
| `C:\Users\hares\Documents\Codex\2026-10-21\i-h\work\clawgrid-env\Scripts\python.exe -m platformio run -d native -e native_voice` | 0 | `benchmarks\review\node-a-20261021T084502Z.log` |
| `C:\Users\hares\Documents\Codex\2026-10-21\i-h\work\clawgrid-env\Scripts\python.exe -m platformio run -d speech -e speech_intelligence` | 0 | `benchmarks\review\node-b-20261021T084502Z.log` |
| `C:\Users\hares\Documents\Codex\2026-10-21\i-h\work\clawgrid-env\Scripts\python.exe -m platformio run -d native -e native_voice_bench` | 0 | `benchmarks\review\bench-compile-only-20261021T084502Z.log` |

The final host result is **122 passed, zero failures/errors**. Frontend checks cover shipped-page/shared-script syntax, rates, stereo conversion, byte order, clipping/caps and confirmation/selection binding. Dataset validation checked 16 clips and ran no recognizer. All firmware targets linked and produced images; this is not target runtime validation.

Environment: Python 3.13.7, Node 24.18.0; direct requirements remain pytest 9.0.3, pyserial 3.5, PlatformIO 6.2.0, matplotlib 3.10.8, ziglang 0.13.0, esptool 5.4.0. ESP-SR is commit 07b0a4d0f6b5de755874d5405150ee0a8eec9865. Its existing fetch workflow verified 62 files / 23,515,359 bytes. Workspace-local PlatformIO, temporary and Zig caches were used. `pip check` and `git diff --check` also passed; distributable source was scanned for the actual private credential values, with no matches.

## Preserved failures

`validation/earlier-failures/` retains available setup/failure logs. Initial sandbox venv ensurepip returned 1; the isolated interpreter was bootstrapped through system pip's --python option. A sandbox install then returned 1 due to DNS; the permitted network retry installed the original pins.

Early maintained-suite runs returned 1 with 48 passed/35 setup errors and 51 passed/32 setup errors, due to dependency/temporary/compiler/loopback restrictions. Workspace caches and required execution permissions restored the original 83 tests. A later host compiler campaign stalled and was stopped (exit -1); its partial output is not counted as validation. Subsequent complete runs passed after the host artifact/timeout changes.

Browser testing caught the shared script's content-policy omission, now repaired and regression-tested. A frontend fixture initialization error was corrected before the final passing checks. Some PlatformIO mirrors had certificate hostname failures; TLS verification stayed enabled and alternate mirrors succeeded. The first A build took approximately 47 minutes including setup. A redundant direct RISC-V archive download was registry-SHA256 verified and left as scratch data; pins were not changed.

The ZIP contained no local credential file. Provisioning created an ignored development pair once; it did not rotate an existing file. Preserve your own working credentials before provisioning. The bundle excludes generated credentials, private headers, caches, fetched dependencies and firmware binaries that embed keys. No remote was pushed.

## Launch and repeat validation

From the extracted project root:

```powershell
py -m venv .venv
py -m pip --python .venv/Scripts/python.exe install -r requirements-dev.txt
.\.venv\Scripts\python.exe scripts/browser_bridge.py --host-dev --port 8770
```

Open http://127.0.0.1:8770/simulation for rooms and fixture-driven speech routing, or http://127.0.0.1:8770/ for bedroom controls. The persistent host banner supplies a public development-only token, valid only in this loopback simulation. No credentials/speech assets are needed for host mode after compiler/Python dependencies are installed.

Choose physical bedroom or virtual rooms. A manual action prepares a proposal; confirm within 15 seconds. Inject a labeled speech fixture to exercise the speech route without recognition. Try stale/disconnected/rejection/ACK-timeout scenarios. Commanded state can change while outputs remain inhibited; independent physical state remains not measured. Unsupported physical rooms refuse explicitly.

The adapter does not emulate NVS, actual radio, electrical startup, microphone acoustics or MCU timing. Its clock fixtures and events are not hardware observations. Normal preview still returns 503 for board APIs. Host PCM upload returns 501.

To rebuild/validate the firmware after retaining or creating local credentials:

```powershell
.\.venv\Scripts\python.exe scripts/provision_local.py
.\.venv\Scripts\python.exe scripts/fetch_speech_dependencies.py
.\.venv\Scripts\python.exe scripts/validate_software.py --build --bench
```

Provisioning preserves an existing credential file; --bench compiles only. The source bundle includes `changes.patch` against the retained baseline. Use `git apply --check changes.patch` before applying it to another checkout, preserving that checkout's uncommitted work and credentials.

## Remaining uncertainty and next three hardware actions

The retained 0/16 command failure is not disproven by compilation or portable tests. Sampled archived clips already have correct 16 kHz/mono/PCM16 format and useful amplitude. Browser resampling repairs another input path; it is not established as the cause of the archived failure. CRCs prove transfer bytes, not recognition quality. AFE remains a hypothesis: exact pinned interfaces were inspected, but no speculative AFE/model/threshold migration was made. No human recordings are supplied; held-out human evaluation is pending.

1. Restore intended private credentials and verify board identities when available. Deploy both reviewed **inhibited** roles together for schema 3, preserve NVS, then rerun paired API/routing/radio/recovery. Older hardware results do not validate these new images.
2. Keep loads disconnected. Evaluate the original corpus and independently labeled real captures on B, retaining new levels/candidates/states/CRC diagnostics. Compare identical audio with the pinned reference pipeline; reserve held-out speakers/sessions and negatives before making accuracy claims.
3. Follow `docs/SAFETY.md`: verify actual low-voltage modules/rails/polarity, test PIR first, then one DC load/channel and cold-boot/reset/disconnect behavior. Use the opt-in image only after that review. Fit independent feedback before claiming electrical switching verification.

## Complete changed file list

- `NEXT_CHAT_HANDOFF.md`
- `README.md`
- `benchmarks/data/synthetic_speech/manifest.csv`
- `benchmarks/reports/CURRENT_EVIDENCE.json`
- `benchmarks/templates/audio_manifest.csv`
- `docs/ARCHITECTURE.md`
- `docs/HARDWARE_STATUS.md`
- `docs/PROTOCOL.md`
- `docs/SAFETY.md`
- `docs/VOICE_ASSISTANT.md`
- `firmware/common/runtime/command_target.h`
- `firmware/common/runtime/control_wire.h`
- `firmware/common/runtime/pcm16.h`
- `firmware/common/runtime/speech_runtime.h`
- `firmware/common/runtime/state_wire.h`
- `firmware/node_intelligence/src/dashboard.h`
- `firmware/node_intelligence/src/dashboard_client.h`
- `firmware/node_intelligence/src/main.cpp`
- `firmware/node_intelligence/src/simulation_dashboard.h`
- `firmware/node_voice/src/main.cpp`
- `native/platformio.ini`
- `scripts/audio_bench.py`
- `scripts/audio_dataset.py`
- `scripts/browser_bridge.py`
- `scripts/check_frontend.cjs`
- `scripts/host_build.py`
- `scripts/validate_software.py`
- `simulator/dev_adapter.py`
- `speech/main/speech_runtime.cpp`
- `tests/integration/test_host_development.py`
- `tests/integration/test_host_http.py`
- `tests/native/dev_controller.cpp`
- `tests/native/firmware_checks.cpp`
- `tests/native/software_selftest.h`
- `tests/unit/test_audio_bench_runner.py`
- `tests/unit/test_audio_dataset.py`
- `tests/unit/test_firmware_cpp.py`
