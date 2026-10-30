# Update25October: exact rejection now measured

Phone8 reaches the correct completed OFF FST path at0.2945118, below unchanged0.7; no accepted recognition. Main sampled loss is around BEDROOM. Equivalent word-spacing experiment worsened both phone/control scores and was rejected. Final reference/Zira/negative controls pass;10host cases pass; A boot16/OFF/inhibited, B firmwaref30b10cf... . No new phone trial or APK build/install. See [new alignment evidence and next step](VOICE_DECODER_ALIGNMENT_20261025.md). Older sections below remain historical.

---

# Actual phone clip diagnosis — 24 October 2026 IST

**End-to-end phone voice is not verified.** The earlier pinned MN6 threshold-unit repair passes reference and synthetic controls, but the retained actual phone clip still produces no accepted command. No further phone recording is justified by the tested startup, AFE or grammar changes. All outputs remain inhibited; physical switching is untested.

## Actual phone and manual results

Manual OFF request1 passed separately on diagnostic APK69d95b55: matching ACK/status0, RTT11,631us and fresh boot14 OFF readback. Phone job6 then reached ESP inference but failed:26,560samples/CRC2192963662,198frames/TIMEOUT/command0/confidence0/error0. ADB UI independently showed PCM MATCH and no proposal. The user confirms exact TURN OFF BEDROOM LIGHT after the old Recording indicator. Job6 PCM was zeroed by the prior app and cannot be reconstructed.

After updating the app, manual OFF request2 passed: matching ACK/status0/attempts1/RTT17,275us/processing29us, fresh boot14 sample5,080,999ms/age534ms/lightOFF/last_command2/inhibited. Passive UART, API and displayed REPORTED corroborate this necessary app-session voice-unlock proof; IDs were captured automatically. No repeated manual command was requested merely to obtain an ID.

The speech counter advanced6→8 before a new trial was requested. Only completed job8 diagnostics and the last clip were retained; job7's result is unknown. Job8 was an actual phone capture on **1.0-speech-capture-20261024b**, not a host-generated fixture:

- 16,000Hz, one channel, signed PCM16 little-endian;32,000samples/64,000bytes/2.000s.
- First nonempty microphone buffer after127ms. The user played the saved WAV and reports the entire intended phrase is clear, including beginning/ending.
- Phone-private PCM CRC32 **679739021** (`2883fe8d`), SHA256 **78d4ceff766e0fa2f6cdc068447e018b2f7866a854b1bf3bc9508f0b9e03d1fd**; matches ESP sample count/CRC exactly.
- Peak17,472, RMS3784.2, DC mean0.14, clipped samples0. These measurements do not themselves prove intelligibility.
- ESP job8:198frames/TIMEOUT, command0/confidence0, initialization/engine errors0; diagnostic text `TURNN OF WITHOMLTE`. No actionable proposal or voice confirmation.

Private retained data: `benchmarks/review/speech-fst-20261024/phone-last-clip/{last.pcm,last.wav,last.json,analysis.json}`. These remain in the ignored local review area, not a public source publication. Separate unconstrained **local host Vosk** diagnostic returned `turn off bedroom late`; it never supplied a product command or replaced ESP recognition. Original audio and all failed target reports are retained.

## Same saved audio, controlled target comparisons

All below are HARDWARE_MEASURED inference. Replays upload saved bytes, request no new phone recording and never call proposal/confirmation/control. Input counts/CRC match in every case; harness errors are empty. Each no-detection probe exits1, not a pass. Wi-Fi wrapper exit0 means orchestration completed, not recognition success.

| Check | Actual result |
|---|---|
| Known local Zira OFF immediately before phone replay, normal16-command model | Job9, command2/confidence0.86229/frame58; input34,880samples/CRC2182999710. |
| Same actual phone8 PCM, normal path on threshold-fixed image | Job10,198frames/3,017,843us/TIMEOUT/no accepted candidate. |
| Same actual phone8 PCM, freshly restarted gateway/normal16 grammar | Job1,198frames/3,057,636us/TIMEOUT/no accepted candidate. |
| Same phone8 PCM through pinned ESP AFE, normal16 grammar, isolated diagnostic | Job2,431feed frames of160 samples,126fetch frames of512;64,512processed samples/CRC1395972200/peak12,689/RMS2100;198MultiNet frames/3,066,501us/TIMEOUT/no candidate/error0. |
| Same phone8 PCM, isolated Bedroom light ON/OFF two-command grammar | Job1 on latest image,198frames/3,015,586us/TIMEOUT/no candidate/error0. Normal16 grammar restored afterward. |
| Known Zira OFF after the two-command experiment, restored normal path | Job2, command2/confidence0.86229/frame58/inference863,490us; no proposal/control. |

Raw reports under `benchmarks/raw/hardware/`: `speech_post_phone_zira_control_20261024.json`, `speech_phone8_replay_20261024.json`, `speech_phone8_raw_fresh_20261024.json`, `speech_phone8_afe_20261024.json`, `speech_phone8_bedroom_20261024.json`, `speech_final_zira_control_20261024.json`. The original phone-replay report inherited the tool's incorrect synthetic-input default label; original evidence is preserved and the correction is recorded in `phone8-replay-label-correction.json`. Later probes explicitly label actual phone replays.

The pinned [ESP-SR documentation](https://raw.githubusercontent.com/espressif/esp-sr/07b0a4d0f6b5de755874d5405150ee0a8eec9865/docs/en/speech_command_recognition/README.rst) describes AFE output and MultiNet input as16kHz/16bit/mono and recommends AFE. The correct160→512 AFE experiment above did not improve this clip, so it was **not promoted into normal phone processing**. Neither startup indication nor a smaller grammar resolves the observed phone failure. Its underlying acoustic/decoder alignment cause remains unidentified; this does not establish a general accent or model-accuracy conclusion.

## Source changes and boundaries

Android: `PhonePcmRecorder.kt` now reports readiness only after the first positive read and includes startup delay; `ClawGridViewModel.kt`/`PhonePushToTalk.kt` distinguish starting from Speak now. New `PhoneVoiceDiagnostic.kt` saves one submitted clip/manifest in app-private cache only with BuildConfig.DEBUG, overwrites the previous clip, enforces the existing4s bound, and exports nothing over the network. The debug UI discloses retention. In-memory buffers still clear in finally; release saving is disabled. New `PhoneVoiceDiagnosticTest.kt` covers retention after caller erasure, bounded overwrite, disabled saving and invalid-input preservation.

Firmware diagnostic additions: `speech_feed.h`, `speech_runtime.cpp`, `speech_reference_afe.h`, `node_intelligence/src/main.cpp`. Authenticated phone-afe/phone-bedroom reference modes accept only the exact retained clip length/CRC; the host also verifies full SHA256. They require fresh inhibited A, publish only diagnostic candidates with command_id0/authorizedfalse, restore normal16 vocabulary, and cannot propose controls. The normal API, confidence0.7, boot/expiry checks, user confirmation, SafetyKernel, RTOS/watchdog and ESP-NOW security remain intact. No policy semantics changed.

Host scripts: `speech_pipeline_probe.py` labels phone captures explicitly and supports the isolated comparisons; `check_android_control_jvm.py` includes new tests; `check_android_client_boundary.py` accepts explicit isolated APK/BuildConfig paths and scans configured passwords; `android_isolated_build.gradle` accepts a validated output-directory name. The local passive observer supports graceful stop-file cleanup.

## Actual builds, installation and checks

- Existing firmware Python: `C:\Users\hares\Documents\Codex\2026-10-21\i-h\work\clawgrid-env\Scripts\python.exe`; existing PlatformIO core from handoff. `python -m platformio run -d speech -e speech_intelligence_uart`: AFE diagnostic SUCCESS97.221s; final Bedroom diagnostic **SUCCESS57.988s**. Guarded `python scripts/flash_native.py --role B-speech --port COM9 --uart-console --app-only` matched B MAC and wrote only application0x10000, both exit0.
- `python -m pytest tests/unit/test_speech_feed.py tests/unit/test_speech_probe_pcm.py tests/unit/test_speech_probe_capture.py tests/unit/test_mn6_threshold.py -q --basetemp=benchmarks/review/speech-fst-20261024/pytest-phone-bedroom`: **9passed5.23s**. Previous expanded AFE run9passed45.23s, preserved separately; these are repeated suites, not18different cases.
- `python scripts/check_android_control_jvm.py`: **49passed1.142s**; original45 control/voice cases plus4 new retention cases. Full Robolectric suite still blocked by prior uncached dependency; no full-suite pass claimed.
- From `ecosystem/android`, existing JDK21/SDK and project Gradle/Android caches: `.\gradlew.bat --offline --init-script ../../scripts/android_isolated_build.gradle -PclawgridBuildDirectory=speech-capture-build-20261024b :app:assembleDebug --console=plain`: **SUCCESS16s**,39tasks/15executed/24fromcache. Earlier missing default-cache foojay plugin63s and Windows packageDebug zip-cache lock78s failures are retained; no locked artifacts deleted.
- Explicit newly built APK/config boundary scan: zero exact configured private-value matches/privileged BuildConfig fields. SDK36.1.0 apksigner with JDK21: v2 verified, unchanged debug signer71ce1bf6...; initial invocation without JAVA_HOME failed and is retained. Release signing unchanged.
- `adb install -r <delivery APK>`: SUCCESS. ADB measured version and pulled installed base APK hash **5c932d6299a3e8bb76f4098fc685b8273f553894a91fadcf561794db00d174b4**, exactly matching delivery. No uninstall/data clearing. Absolute APK: `C:\Users\hares\OneDrive\Desktop\ESD-imp\ClawGrid-physical-integration-prep\deliverables\ClawGrid-Speech-Capture-debug-20261024b.apk`,18,364,818bytes. Previous69d95b55 APK was also independently verified installed for job6; original d12778de APK belongs to earlier failed trials.
- Final checks: private credentials/header/partition hashes unchanged; all62 pinned upstream component hashes match. `final-phone-artifact-status.json`. Wi-Fi wrappers restore the previous laptop network/delete only their temporary private profile/XML. Serial handles released.

## Latest installed hardware and remaining blocker

A COM10 MACe0:72:a1:f5:bc:b0, unchanged normal inhibited SHA8f028bd90f65a55a72b09f4b886db7a3fa851f8a2108c6b79659136db8713cdf. B COM9 MACe0:72:a1:f6:a9:68 was freshly guarded again; latest normal inhibited/UART0 SHA **b2ae88b9e93a1a37987b9d2047143f0d543271007994302304d45c5d78b3b7cf**. Latest manifest `flash_speech_intelligence_uart_20261024T172229Z.json`; prior AFE diagnostic SHA16727bc6.../manifestT171658Z retained. No NVS/model/partition/provisioning/credential/database writes or diagnostic-fault/output-enabled image.

Final bounded read-only probe `phone-diagnosis-final-20261024.json`: A boot14, advancing sample6,080,999ms/age569ms, OFF/OFF,last_command2, output inhibition; B AP192.168.4.1/channel1 matches A, watchdog active/zero sampled deadline misses. AP clients0 at final observation, so do not claim current phone association. Gateway job counters/transaction reset after deliberate app-only B updates: latest job2 is the synthetic positive control, **not phone job8**; transaction0 does not erase retained historical manual ACK2 evidence and does not authorize replay.

Remaining blocker is accepted MN6 decoding of the clear retained phone clip at unchanged0.7. Audio delivery, actual format, manual ACK/readback and normal reference controls have been verified separately. Voice-recognized proposal/confirmation/ACK/readback, voice ON→OFF and the phone silence/non-command UI check remain **unverified**. No physical output switching/load measurement was tested. No recognition accuracy estimate is claimed.

Next executable step: use the retained PCM and pinned binary to trace token/FST alignment and best-path qualification scores **read-only at the existing0.7 gate**, comparing with the preserved successful Zira clip. Any proposed acoustic/configuration correction must first pass saved-clip/reference/negative checks on ESP before another phone trial. Do not substitute host text, invent a recognized command, add an unverified synonym, relax thresholds, automatically replay requests or enable outputs. Current source work has not been republished to GitHub/Downloads; existing publication evidence remains unchanged.
