# Decision update: bounded token investigation closed

No evidence-backed implementation fix emerged. Current MN6/full-room voice is not accepted for this prototype; the debugging loop is closed. Exactly two saved clips completed dense BEDROOM windows, then baseline source and installed firmwaref30b10cf... were restored. No new phone recordings, model/architecture change or threshold lowering. See [decision, uncertainties and one finite proposed alternative](VOICE_DECISION_AND_BOUNDED_EXPERIMENT_20261025.md). Previous next-step token-debug instructions below are historical and superseded.

---

# ESP decoder/FST alignment — 25 October 2026 IST

**The retained phone clip reaches the correct completed OFF path but is rejected on score. A working correction has not been established.** Its sampled completed-path score is0.2945118 against the unchanged0.7 acceptance gate. No live phone repetition, proposal, confirmation or control command was requested in this run. Physical outputs remain inhibited and wiring deferred.

## Reconnected boards and preservation

Fresh Windows enumeration found two CH343 adapters. ROM `read-mac` results matched the existing provisioning before role assignment: A COM10/USB serial5C37271524/MACe0:72:a1:f5:bc:b0; B COM9/serial5C37271527/MACe0:72:a1:f6:a9:68. No role was inferred solely from an old COM number. Reads performed ordinary application resets; A is now boot16. Preflight gave authenticated advancing telemetry, OFF/OFF/last_command0, outputs inhibited, AP192.168.4.1/channel1 and watchdog active. Evidence: `benchmarks/review/speech-decoder-20261025/board-identities.json`, COM read logs and `preflight.log`.

Initial sources, master/handoff and B image/ELF are preserved in that review folder's `before/`. Final credential/config/partition hashes match the start of this run; all62 pinned component files match the dependency manifest. No credential regeneration, NVS erase, partition/model/database write, output-enabled or diagnostic-fault image. Existing failed results and prior publication remain untouched.

## New observations and exact rejection

The hash-guarded MN6 layout was traced read-only: active/finished containers at model92/96; path tokens at0, command at8, total log score at12 and token length at16. The pinned qualification routine first compares `total_log/(length+2)` with the effective log threshold. Result conversion uses its exponential. Trace records cannot authorize commands and are distinct from accepted recognizer results.

For original phone job8, replayed as job3 in the final campaign:

- Input remains32,000samples/64,000bytes, PCM16LE/mono16kHz, CRC679739021/SHA78d4ceff766e0fa2f6cdc068447e018b2f7866a854b1bf3bc9508f0b9e03d1fd. Complete input delivery checks match.
- At sampled frame112, finished path command2 has10 tokens, total log score-14.6692362 and mean-1.2224363. Its exponential0.2945118 fails the0.7 gate (`ln(0.7)` approximately-0.356675).
- Tokens `[457,11,37,49,8,94,206,18,29,196]`, decoded using the unchanged pinned vocabulary, spell **TURN OFF BEDROOM LIGHT**. This verifies a completed correct FST path; the low score, rather than missing registration or a target-ID mismatch, explains this candidate's rejection.
- Accepted recognizer output remains command0/confidence0/TIMEOUT/error0. Diagnostic path2 was never promoted to a proposal or device command.

Sampled alignment comparison (these are internal partial-path scores, not accepted recognition or an accuracy metric):

| Aligned prefix | Actual phone8 | Successful Zira control |
|---|---|---|
| TURN OFF | Frame64,0.9149038 | Frame40,0.7479100 |
| TURN OFF BEDROOM | Frame72,0.4204598; frame80,0.1736052 | Frame48,0.8661351 |
| Full phrase | Sampled completed frame112,0.2945118; rejected | Accepted frame58,0.86229 |

The largest observed score loss is around BEDROOM's token sequence `▁BE,D,RO,OM`. These samples locate the divergence; they do not establish a phonetic, accent, microphone or model-training cause. Only the first two stored paths per group are sampled, every8frames plus terminal state, with48 snapshots maximum. No global claim about every beam/emission is made. The phone's completed group contained one path in the observed interval.

Final v4 trace status reports `layout_read_failed=false` for every case. Warm-up/terminal empty paths use negative-infinity score bits0xff800000; these are explicitly emitted as unavailable/null scores, never fabricated zero confidence. Recognized positive results can be cached after internal finished paths are cleared, so their accepted score comes from the maintained recognizer result API.

## Controlled comparisons and rejected correction

All inference below is **HARDWARE_MEASURED**. Zira/silence/non-command fixtures are **SIMULATED inputs**; phone8 is preserved real microphone PCM. All jobs used authenticated exact-fingerprint diagnostic routes requiring fresh inhibited A, returned command_id0/authorizedfalse, and issued no proposal/confirmation/control POST.

| Final v4 normal-grammar case | Actual result |
|---|---|
| Pinned official TELL ME A JOKE reference | DETECTED/frame69/candidate1/0.90041. |
| Zira TURN OFF BEDROOM LIGHT | DETECTED/frame58/candidate2/0.86229. |
| Retained actual phone8 | TIMEOUT/frame198; completed diagnostic OFF path0.2945118; no accepted command. |
| Two-second silence | No accepted command; sampled completed path score0.0267891, well below gate. |
| The weather is nice today | No accepted command; sampled completed path score0.0102169, below gate. |

Evidence: `benchmarks/raw/hardware/decoder_{reference,zira,phone8,silence,noncommand}_20261025_v4.json`, review `final-trace-summary.json` and `alignment-final.json`. All input counts/CRCs match, harness errors are empty, and watchdog/zero sampled deadline misses/channel1 are retained. Each failed phone probe exits1; wrapper/campaign completion is not recognition success.

An evidence-driven **isolated** experiment changed only the equivalent BEDROOM spelling to BED ROOM to test token segmentation, retaining command IDs, model and0.7. It replaced token94 (`RO`) with137 (`▁RO`) in this path. Phone score worsened to0.1683451 and the previously successful Zira control also failed at0.4924091. The public reference and negative controls retained expected outcomes. **This experiment was rejected and not enabled in normal operation.** Normal16-command grammar is restored after diagnostic jobs. Raw v3 results and all v2 observations are preserved.

No new phone trial is justified by a successful correction. Historical AFE, two-command grammar and startup-indicator experiments remain recorded as insufficient fixes.

## Implementation and host checks

Changed files: new `firmware/common/runtime/mn6_trace.h`; `speech_feed.h`; `speech/main/speech_runtime.cpp`; `firmware/node_intelligence/src/main.cpp`; `scripts/speech_pipeline_probe.py`; `scripts/with_gateway_ap.py`; new `tests/unit/test_mn6_trace.py`; expanded `test_speech_feed.py`; master/handoff and this report. Android code/APK, SafetyKernel, policy semantics, RTOS scheduling, ESP-NOW/provisioning and model assets were not changed.

Documentation pointers also updated: `docs/PHONE_CLIP_DIAGNOSIS_20261024.md`, `docs/VOICE_ASSISTANT.md` and `ecosystem/android/PHONE_INSTALL_AND_LOCAL_VOICE.md`. Ignored review result/checkpoint JSON records now point to the current campaign; prior raw failures remain intact.

Instrumentation uses validated RAM pointer ranges, bounded lengths/counts and the existing exact archive hash guard. Snapshots are buffered in bounded PSRAM; UART formatting occurs after timed inference. Each JSON path is printed atomically because NativeSerial deliberately prefixes a new line to each printf call. Initial split-call records were preserved verbatim and reconstructed into sidecars with original raw line locations; no values were invented. Internal unscored states no longer falsely abort remaining snapshot reads. Finite raw path scores above1 may be observed diagnostically; product confidence validity and acceptance checks are unchanged.

AP association initially preceded Windows route/DHCP readiness: v1 stopped with WinError10051 **before any audio upload**. The helper now waits up to25s for a fresh inhibited local HTTP state, without changing IPs or credentials. v2/v3/v4 recorded HTTP readiness and restored the previous laptop Wi-Fi, removed only their owned temporary profile/XML. v1 error and journal remain intact.

Exact current checks, using firmware Python from the handoff and existing PlatformIO core:

- `python -m pytest tests/unit/test_mn6_trace.py tests/unit/test_speech_feed.py tests/unit/test_speech_probe_pcm.py tests/unit/test_speech_probe_capture.py tests/unit/test_mn6_threshold.py -q --basetemp=benchmarks/review/speech-decoder-20261025/pytest-final --junitxml=benchmarks/review/speech-decoder-20261025/host-final.xml`, TEMP/TMP set to existing review temporary directory: **10passed11.09s**. Covers read-only memory, invalid pointer/length/layout rejection, score math, unavailable scores, framing/fingerprint boundaries and failure-evidence cleanup. Previous runs10passed34.01s and10passed32.19s are repeats, not additional cases. An intermediate9pass/1fail12.28s exposed an unconditional optional-feed access in the host probe; corrected to access feed only for reference jobs, failure preserved.
- `python -m platformio run -d speech -e speech_intelligence_uart`: initial trace SUCCESS96.937s; public-reference trace inclusion SUCCESS53.496s; spelling/logging experiment SUCCESS57.387s; final trace cleanup **SUCCESS54.276s**. Each rebuild had an intervening source change. No unchanged Android component was rebuilt; previous49 Android cases/APK checks remain historical.
- `python scripts/flash_native.py --role B-speech --port COM9 --uart-console --app-only`: three successful fresh-MAC-guarded normal inhibited updates. Manifests `flash_speech_intelligence_uart_20261025T053926Z.json`, `...T055203Z.json`, final `...T055835Z.json`. Only app0x10000 written.
- `python scripts/with_gateway_ap.py --output benchmarks/review/speech-decoder-20261025/wifi-v4.json -- <firmware-python> benchmarks/review/speech-decoder-20261025/campaign-v4.py`: exact bounded five-case commands are retained in that script; read-only status plus audio uploads only. Raw outcome/exit statuses above. v2 baseline and v3 spelling variant were necessary comparisons; v4 validates repaired trace serialization/unavailable-score handling and unchanged normal recognition/negative behavior.
- `phone_probe.py --gateway COM9 --seconds 4 --name decoder-final-state-20261025`: exit0. No manual-sequence option. Final artifacts/model/private-input checks in `preservation-final.json` pass. All serial handles released.

## Installed identities and remaining work

Latest B: normal inhibited/UART0 SHA256 **f30b10cfb74a4dd79ff28588de2d3be9f6c760c934b48cf1475bf6f7c271b9a4**. Final guarded manifestT055835Z above. Image and ELF are retained by hash under `benchmarks/backups/speech-decoder-20261025/`. A was not flashed; last known image remains8f028bd90f65a55a72b09f4b886db7a3fa851f8a2108c6b79659136db8713cdf, with fresh MAC and inhibited runtime confirmed. A boot16 invalidates historical boot14 manual/voice-unlock proof.

No APK was built or installed today. Last independently verified installed Android version remains **1.0-speech-capture-20261024b**, SHA256 **5c932d6299a3e8bb76f4098fc685b8273f553894a91fadcf561794db00d174b4**; actual phone8 used that build. Only ESP boards are USB-connected per user; no fresh on-phone version observation is claimed.

Final bounded readback: A authenticated/known, boot16, sample1,906,997ms/age137ms, lightOFF/fanOFF/last_command0, physical_outputs_enabled=false. Telemetry/tasks advance. B AP192.168.4.1/channel1/watchdog active/zero sampled deadline misses, AP clients0. Last gateway job5 is the replayed non-command negative control, not original phone8; transaction0 is expected because this run issued no actuator commands. Loads/sensors/relays remain disconnected; physical sensing/switching is untested.

Remaining blocker: the clear phone clip's correct full command path has insufficient acoustic/decoder score. Registration, token sequence and the exact threshold rejection are now measured. No safe correction has been validated. Voice-recognized correct proposal/user confirmation/matching ACK/fresh readback, voice ON→OFF and phone negative UI test remain unverified; no accuracy estimate is claimed.

Next executable step: compare per-token emissions/competing unconstrained tokens specifically around `_BE,D,RO,OM` for phone frames64–80 versus the preserved successful Zira frames40–56. Determine whether a supported acoustic/frontend configuration correction is indicated, using saved audio only. Do not repeat the failed spelling/AFE/smaller-grammar experiments, alter model assets, infer commands from diagnostic strings, relax0.7 or request another phone clip without a tested correction passing saved-phone/reference/negative controls. No GitHub/Downloads publication was changed.
