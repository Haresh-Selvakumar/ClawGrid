# Current update: reference recognition repaired — 24 October 2026 IST

Gateway B now accepts the pinned public reference through both comparison paths (probability0.90041) and normal synthetic Bedroom/light/OFF (0.86229). The identified pinned MN6 probability/log-unit mismatch is repaired without relaxing the0.7 policy. See [repair and actual target evidence](VOICE_THRESHOLD_FIX_AND_PHONE_TEST_20261024.md). Current B SHAf7b31d41..., A boot14/OFF/OFF/inhibited; refresh before live testing. Silence/non-command host checks returned command0. New phone recognition/confirmation and phone negative tests are pending. Diagnostic APK version1.0-speech-diag-20261024/SHA69d95b55... is laptop-built; installation remains unverified while user updates by sideload. Outputs stay inhibited, loads disconnected, local HTTP192.168.4.1/channel1; Supabase/Internet optional. Older status sections below are preserved history.

---

# Current result: USB recovered; ESP reference recognition still fails - 24 October 2026 IST

**No new phone trial is requested. Recognition has not passed.** This section supersedes earlier pending USB-recovery, boot9/12 and not-deployed notes below. User changed the cable/wiring; ROM communication recovered. Fresh MAC reads matched existing provisioning: **A COM10 e0:72:a1:f5:bc:b0**, **B COM9 e0:72:a1:f6:a9:68**. A is now **boot13** after identity reads. Sensors, relays and loads are user-reported disconnected; physical switching remains untested. Native Android supersedes the old browser-only AGENTS rule. No subagents were used.

Full source changes, official pinned references, earlier failures and exact checks are recorded here and in the preserved detailed record below. Existing evidence is preserved. Original workspace is not Git; previous GitHub/Downloads publication remains unchanged and this new work has not been republished.

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

## Earlier detailed investigation record (superseded by final results above)

# Speech pipeline diagnosis — 24 October 2026 IST

Status: **recognition remains unresolved**. No further phone trial has been requested. All target jobs below are recognition-only, with no proposal, confirmation or control POST. No physical switching was tested. Sensors, relays and loads are user-reported disconnected. Existing failed phone and synthetic results remain valid evidence.

## Reference and installed assets

The dependency manifest pins ESP-SR 1.7.0 commit `07b0a4d0f6b5de755874d5405150ee0a8eec9865`. All 62 existing component files checked against that manifest matched. Reference sources downloaded for this investigation were verified against their Git blob IDs; retained copies and hashes are in `benchmarks/review/speech-pipeline-20261024/reference-manifest.json`.

Primary references: [pinned MultiNet tests](https://github.com/espressif/esp-sr/blob/07b0a4d0f6b5de755874d5405150ee0a8eec9865/test_apps/esp-sr/main/test_multinet.c), [pinned command recognition documentation](https://github.com/espressif/esp-sr/blob/07b0a4d0f6b5de755874d5405150ee0a8eec9865/docs/en/speech_command_recognition/README.rst), and [pinned AFE tests](https://github.com/espressif/esp-sr/blob/07b0a4d0f6b5de755874d5405150ee0a8eec9865/test_apps/esp-sr/main/test_afe.c). The CPU test feeds raw PCM without AFE, creates a 6000ms recognizer, feeds zeros after complete clip frames and stops on detection or after 601 calls. Its seven-frame CPU-accounting subtraction does not specify a decoder window. It does not stop on TIMEOUT. Documentation recommends the AFE path, but its absence alone is not established as the failure cause. No AFE was added in this investigation.

Fresh ROM reads matched existing provisioning: A COM10 `e0:72:a1:f5:bc:b0`; B COM9 `e0:72:a1:f6:a9:68`. CH343 serial numbers are respectively `5C37271524` and `5C37271527`; port numbers must still be enumerated before future work. Records: `board-identities.json` and `COM*-read-mac-retry1.log` in the review directory. An earlier AccessDenied failure was preserved; the successful retry followed closing serial monitors. Identity reads reset applications; today's A boot is 12, superseding yesterday's boot9. No control command was issued today.

Read-only installed-app verification matched the original B image `a225740aa6af9445f9959994141dfc5723944c7f59daa957d039638cafbaf2b2`. Installed model bytes at `0x610000`, length 3,788,231, matched pinned pack SHA256 `b46a65befe47187f793ffb8108f74ab091338060060a54d23b69c385c3213d80`. Installed partition table matched the existing build. See `installed-model-readback-verified.json`; private readbacks remain under `benchmarks/backups/speech-pipeline-20261024/`. Failed no-reset loader reads are preserved. No model, partition, NVS or credential write was made.

Actual loaded model reports `mn6_en`, English, 16000Hz, chunk512. Normal commands are the unchanged 16 uppercase phrases: TURN ON/OFF plus BEDROOM, LIVING ROOM, KITCHEN or STUDY and LIGHT/FAN; IDs1/2 are Bedroom light ON/OFF. Registration now checks every public command-cache entry and update result. MN6 owns command allocation through create/destroy; redundant external allocation/free was removed. Clean-before-job, reset-before-registration and fresh-model variants were compared. None established a successful recognition. Threshold remains 0.7.

## PCM and acoustic evidence

Phone capture source is AudioRecord mono PCM16; actual capture rate is used by the existing bounded conversion to 16kHz signed PCM16 little-endian, max64,000 samples. HTTP sends raw bytes without a WAV header. ESP explicitly decodes signed little-endian samples into 512-sample frames, pads the final partial frame, and clears clip memory after inference. New phone diagnostics check actual capture channels/encoding and compare encoded sample count and CRC32 with the completed ESP job **before** checking confidence. Diagnostic reference results cannot create app proposals.

Original phone PCM from the two failed jobs was not retained. Their board counts/CRC are recorded in the existing master, but there is no independent source-byte comparison or acoustic/intelligibility inspection for those phone clips. Peak/nonzero/clipping counts alone do not prove intelligibility. The new APK's capture and failed-job delivery diagnostics have not run on the phone.

| HOST_MEASURED clip | Pinned tell_me_a_joke | Existing synthetic command02 |
|---|---|---|
| Format | mono signed PCM16LE / 16000Hz | same |
| Samples / duration | 44,829 / 2.8018125s | 34,880 / 2.18s |
| CRC32 | 330285049 | 491911642 |
| Peak / RMS / DC | 16,421 / 2252.84 / 1.73 | 31,000 / 3166.73 / 387.00 |
| Clipped / exact-zero fraction | 0 / 0.001941 | 0 / 0.371818 |
| Active 10ms windows, RMS>=300 | 0.42–2.40s | 0.11–1.34s |
| Independent local Vosk diagnostic | “tell me a joke” | “turn off bedroom life” |

This independent local recognizer was used only to inspect existing public/synthetic audio; it never supplied a device command or ESP transcript. These results are not human listening or an ESP recognition pass. Clip analysis and independent results are retained in `clip-analysis.json` and `independent-clip-transcription.json`. Pinned PCM SHA256 is `6277bddf1b8e5b44cf2c453b9763e013c2817f5011320d42128eb732d5ac4ac4`; preparation checks upstream blob and exact bytes.

## Actual target results

All Wi-Fi rows below matched complete sample count and CRC32 end-to-end. They isolate these runs from phone capture and transport, but do not prove phone delivery or identify the remaining decoder cause.

| HARDWARE_MEASURED run | Actual outcome |
|---|---|
| Original image, synthetic02 over UART HTTP bridge | Upload failed502, only59,921/69,760 bytes received; no inference.115200-baud bridge is too slow for this clip under HTTP timeout. Preserved as transport failure. |
| Original image, same synthetic02 over actual AP HTTP | Job2,100frames,1,545,188us; command0/confidence0, DETECTING. Full bytes/CRC matched. |
| Reference legacy feed | 119frames,1,840,146us; no detection. |
| Reference extended195-frame feed, two jobs | 3,174,025us and3,170,932us; both ended DETECTING, no candidate. |
| Reset-first and fresh-model195-frame reference | No detection;3,134,348us and3,153,195us. |
| First fresh factory-reference attempt | HTTP accepted, then timeout and later gateway job0/ready indicated a restart. No retained panic/watchdog cause; restart cause is **unknown**. A remained boot12. |
| Factory49-command reference on later ownership-cleanup image | 198frames,3,265,865us, TIMEOUT, no accepted command/candidate. Untrusted decoder text `TELL ME A JOKEK` is diagnostic only. |
| Custom reference / reset-first / fresh-model repeats on that image | Each stopped at195frames, no detection;3,149,348 /3,123,770 /3,134,213us. Normal16-command registry restored after each job. |

Raw results: `benchmarks/raw/hardware/speech_baseline_20261024.json`, `speech_baseline_wifi_20261024.json`, `speech_official_reference_20261024.json`, `speech_reference_reset_20261024.json`, `speech_factory_reference_20261024.json`. The last campaign had no harness errors and A boot12 progressed sample1,958,998→1,975,998ms, with fresh authenticated OFF/OFF, last_command0 and outputs inhibited. B AP192.168.4.1/channel1 matched A; watchdog was active. A subsequent failed-guard runtime probe still observed A normal inhibited/channel1, but B's UART selftest was unavailable. Do not claim current B health from older telemetry.

Three guarded, normal inhibited, app-only B updates succeeded, at address0x10000 only: manifests `flash_speech_intelligence_uart_20261024T143212Z.json` (image `0f0c6544ea374f43cc78ed8c761ca46a4cdd8471024d52c3e78e817cd2aaa6d6`), `...T144011Z.json` (`9b3a322ba7f5b5bdc9234f69a782adfc4682934fbd7ee9f3b9b17915bbf84fd6`), and latest successful `...T144749Z.json` (**`a22de8b3ebebfe5afe322fd90d09f807637960df7cc84ecbfc35451958f6f0f9`**). A remains `8f028bd90f65a55a72b09f4b886db7a3fa851f8a2108c6b79659136db8713cdf`. No diagnostic-fault or output-enabled image was deployed.

## Source corrections and focused checks

An identified framing defect was premature inference termination: original phone jobs stopped at97/157frames; the first extension still stopped at195, whereas the factory decoder actually timed out at198. Application/reference-window jobs now allow up to601 calls, stopping on a terminal state. A separate isolated `pinned` reference mode follows the upstream loop through TIMEOUT and drops its last partial frame as upstream does. Normal phone jobs retain **all** input samples. The model threshold is unchanged even in reference modes, so this is not a claim of exact factory-default-threshold equivalence. The latest full-window/pinned source is built but **not deployed or target-tested**: guarded reads failed before writing, including once after the user tapped RESET. B remained visible as COM9 but neither ROM nor configured UART runtime responded. Manual BOOT+RESET entry has been requested. No blind repeated flash is justified.

Files changed: `firmware/common/runtime/{speech_feed.h,speech_runtime.h,pcm16.h}`, `speech/main/speech_runtime.cpp`, `firmware/node_intelligence/src/main.cpp`; new `scripts/{speech_pipeline_probe.py,with_gateway_ap.py,prepare_speech_reference.py,android_isolated_build.gradle}`; new `tests/unit/{test_speech_feed.py,test_speech_probe_pcm.py}`. Reference endpoint is authenticated, gated on fresh inhibited A, validates exact public clip size/CRC, and always keeps command_id0. Factory diagnostic ID1 is never a normal light command.

Android changes: `ecosystem/android/app/build.gradle.kts`, `app/src/main/java/com/example/data/remote/LocalVoiceRepository.kt`, `data/remote/PhonePcmRecorder.kt`, `ui/ClawGridViewModel.kt`, `ui/components/PhonePushToTalk.kt`, and existing `LocalVoiceTest` cases. They display build identity, capture details and source/ESP delivery comparison including failed recognition, and reject diagnostic jobs. Existing requested/reported state, boot/expiry, proposal/confirmation, SafetyKernel, local-only speech and no-replay boundaries remain. No persistent policy change: the existing five-minute manual-hold mismatch with manual-until-AUTO remains unresolved.

Commands run from project root using existing firmware Python (full path in handoff), unless stated:

| HOST_MEASURED check | Actual result / review log |
|---|---|
| `python -m platformio run -d speech -e speech_intelligence_uart` | First compile failed undeclared non-public commands_num API; corrected to public cache APIs. Later successful builds55.376s,45.353s,43.902s,43.104s,50.434s; latest pinned-mode build45.213s (`build-B-pinned-reference.log`). |
| `python -m pytest tests/unit/test_speech_feed.py tests/unit/test_speech_probe_pcm.py -q --basetemp=benchmarks/review/speech-pipeline-20261024/pytest-pinned --junitxml=benchmarks/review/speech-pipeline-20261024/host-pinned-feed.xml` with TEMP/TMP at review/tmp | Latest6passed2.68s, `host-pinned-feed.log`. Includes signed LE/frame boundaries, final partial preservation, reference isolation/CRC and pinned-loop differences. |
| Earlier combined portable firmware run, review basetemp | Existing41cases passed; new compiler case timed out120s. Adding matching Zig -O2 fixed the new fixture; subsequent runs passed. Initial default pytest Temp run failed setup AccessDenied. Failed logs retained. |
| `python scripts/check_android_control_jvm.py` |45actual adapter/voice tests passed, JUnit1.066s; `android-control-tests.log`. Includes failed-job CRC reporting and reference rejection. |
| Android `.\gradlew.bat --offline :app:assembleDebug --console=plain` | Failed54s Windows AccessDenied on existing BuildConfig directory with deny-delete ACL. Existing outputs preserved; no ACL changed. |
| Android `.\gradlew.bat --offline --init-script ../../scripts/android_isolated_build.gradle :app:assembleDebug --console=plain` with JDK21, existing SDK/caches | SUCCESS1m7s,39tasks/25executed/14fromcache; `android-build-isolated.log`. Full Robolectric not run: earlier uncached nativeruntime-dist-compat1.0.18 blocker remains. |
| SDK36.1.0 apksigner verify and aapt2 dump badging on new APK | v2 valid, existing signer unchanged, debug version identity verified; `apk-signature.log`, `apk-badging.log`. Initial signing check needed JAVA_HOME set to existing JBR; subsequent check passed. Configured-private-value/nested APK scan passed. |

## Laptop APK versus installed phone

New HOST_MEASURED APK: `C:\Users\hares\OneDrive\Desktop\ESD-imp\ClawGrid-physical-integration-prep\deliverables\ClawGrid-Speech-Diagnostics-debug-20261024.apk`,18,364,814bytes,SHA256 `69d95b5552f1ece7c0d11f41710cd3ffe202d92e95dc97b778877cb4716efa8e`,versionName **1.0-speech-diag-20261024**,versionCode1. Built output is `.android-tools/speech-diag-build-20261024/app/outputs/apk/debug/app-debug.apk`. Existing debug signer `71ce1bf6f415db7a355e7c63d941c53e7b52fba6a4c33c25042582af0a464bf9` and key unchanged; release signing untouched. Metadata: `benchmarks/raw/android_speech_diagnostics_build_20261024.json`.

This APK was **not installed or phone-tested**. The last confirmed APK used for the two physical phone trials was `d12778de7b8ecab0d141f71bf26dea4919c458a068aa7ee79453e045c98b0bfd`. Yesterday's ID-display build `a44b9cc4dc3571c71b334b95049d4ab2221af10f1da8b04f6f1b13d1a4366119` is a separate laptop artifact and remains preserved; its installation was not confirmed. Current installed phone version cannot be asserted without new evidence. Existing Android Studio/USB/sideload instructions remain in the phone guide; no update is required merely to repeat the failed phrase.

## Blocker and next executable step

Wait for B's manual bootloader entry; enumerate current ports, run guarded MAC verification against provisioning, stop on mismatch. Once verified, normal inhibited app-only deployment command is `python scripts/flash_native.py --role B-speech --port COM9 --uart-console --app-only` (use actual newly verified port). Then verify runtime/AP/channel/inhibition and run bounded actual-Wi-Fi reference `--feed pinned window` followed by normal synthetic02, using `scripts/speech_pipeline_probe.py` and existing AP wrapper. Do not claim the newly built601-frame correction works before those results. If the exact reference still fails, retain it and investigate pinned preprocessing/model configuration next; AFE remains a controlled hypothesis. Do not lower thresholds, inject transcripts, replay ON or enable outputs. No new phone trial until a concrete target-tested correction or discriminating hypothesis warrants one. Recognition and voice confirmation remain separate; physical switching is untested.
