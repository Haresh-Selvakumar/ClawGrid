# Milestone 1: short-v1 stopped at reference preflight — 25 October 2026

**Later approved work:** the separate bounded MN7 evaluation also failed its mandatory OFF reference and restored the exact baseline. [MN7 actual results/rollback](MN7_EVALUATION_20261025.md). The short-v1 receipt and proposed migration requirements below are preserved history; they do not reopen either closed experiment.

**Result: FAIL_STOP_NO_PHONE_COLLECTION.** The user approved the fixed short-command experiment. Its mandatory synthetic/reference preflight failed for LIGHT DISABLE at the unchanged probability threshold 0.7. No phone screening clips, new phone repetitions, synonym search, threshold changes, control proposals or confirmations followed. The previous full-room investigation remains closed. Do not treat this as a measured phone-recognition accuracy result.

## Frozen experiment and implemented scope

The immutable plan is `experiments/voice-short-v1.json`, SHA256 `e9369a25f9ff6583fb7023cf492326ceb187f2025642836934d126435f3cc700`. It was saved before target inference or fresh phone data. Grammar: two phrases only, ID1 LIGHT ENABLE → Bedroom/light/ON, ID2 LIGHT DISABLE → Bedroom/light/OFF. Same MN6 model, archive, probability0.7/log-unit adapter, raw mono16kHz S16LE frontend,512-sample frames,6s window/601-call bound, terminal-state handling and zero tail. No AFE/gain/model change. The existing Android MIC capture, rate fallback and resampling settings were frozen but not changed.

The plan fixes40 screening prompts:10 per positive phrase across two sessions,5 silence,5 background and10 specific non-command/near-miss sentences. Pass requires>=9/10 per positive phrase and zero wrong/negative acceptances. Wrong or negative acceptance, a second miss per phrase, or a transport/engine/reboot/output/watchdog problem stops collection; maximum40, no retries or tuning. A pass would permit only5 further normal-app clips: OFF→ON→OFF with explicit confirmation/ACK/fresh readback and>=3s dwell, plus silence/non-command checks. **None of that phone batch was started.** The synthetic preflight failure independently activates the earlier stopping gate.

Implemented and built an explicit opt-in `speech_intelligence_uart_short_v1` firmware environment. Normal environments retain their full-room grammar; the saved installed baseline is restored. The experiment has:

- Separate image marker and an explicit guarded-flasher `--voice-experiment short-v1` choice, restricted to normal inhibited B/UART0.
- Actual two-command ESP registration and a non-authorizing, recognition-only `/api/speech/reference?feed=short-v1` preflight route. Old diagnostic routes are unavailable in this experiment environment.
- Source support for explicit Bedroom/current-controller-boot/capture-ID binding at upload, echoed by `/api/speech`. A screen-only job cannot propose control. Wrong room/boot, missing capture context, stale controller, enabled outputs, diagnostic job, low/nonfinite score or30s binding expiry fails closed.
- Speech target/job/binding revalidation at both proposal and confirmation. Existing SafetyKernel submission and ACK/readback mechanisms remain in place. A new upload invalidates the old proposal; nothing automatically confirms or replays ON.

This is **firmware/preflight implementation**, not a completed Android feature. Explicit Bedroom selection, the Android finite-screening ledger/UI, and on-phone binding/confirmation have **not** been implemented or validated because the required preflight failed first. No new APK was built or installed. Do not advertise short voice as ready or use the experimental image with the old app.

## Actual target observations

Fresh ROM identities matched existing provisioning before assignment:

| Role | Port/adapter serial observed now | MAC |
|---|---|---|
| A controller | COM10 / CH343 5C37271524 | e0:72:a1:f5:bc:b0 |
| B gateway | COM9 / CH343 5C37271527 | e0:72:a1:f6:a9:68 |

Identity records and raw outputs: `benchmarks/review/voice-short-v1-20261025/board-identities.json`, `COM9-identity.log`, `COM10-identity.log`. ROM reads deliberately reset applications; A is now boot17. Only two bare boards are connected by the user's report; sensors, relays and loads remain disconnected. No physical switching is measured.

Exactly four frozen clips traversed laptop Wi-Fi → gateway HTTP → ESP recognition once. The two positive references were generated once with Microsoft Zira Desktop at16kHz mono PCM16. Existing silence and non-command fixtures were reused. Synthetic input is labelled SIMULATED; inference/results are HARDWARE_MEASURED on B. The laptop did not recognize speech or supply command text to the product.

| Fixture / ESP job | Upload/ESP samples | CRC32 | Actual ESP result |
|---|---:|---:|---|
| LIGHT ENABLE /1 |25,520 MATCH|3001788789|Accepted reference ID1, score0.73673, DETECTED frame38|
| LIGHT DISABLE /2 |27,200 MATCH|3370348365|No accepted ID, TIMEOUT frame198, raw_result_count0, no engine error|
| Silence /3 |32,000 MATCH|2109903275|No accepted command|
| Fixed non-command /4 |33,760 MATCH|3492560341|No accepted command|

All four returned `command_id:0`, `authorized:false`, `diagnostic_only:true`; reference acceptance is separate from command authorization. LIGHT DISABLE diagnostic text was `LIKE TO SABLE`, an untrusted decoder string, not an actionable command. Its displayed candidate score0 is **not** a measured completed-path confidence; no accepted/result candidate was returned. We did not start another FST/acoustic-debugging investigation to explain that failure.

Full results, levels, frame counts, timings and source/received fingerprints are retained in `benchmarks/review/voice-short-v1-20261025/preflight.json`. Frozen WAV/SHA records are in the plan. Peak values23,461/23,379 with zero clipped samples for the two references; PCM delivery matched. No cropped, amplified or otherwise tuned failed recording was substituted.

During preflight, controller boot17 sample592,998→605,998ms advanced; light/fan stayed OFF and last_command0. B LinkTask jobs3,358→5,879, watchdog active, zero deadline misses, AP192.168.4.1/channel1 matched A. These are bounded observations, not a soak test. Laptop Wi-Fi/profile was restored and the owned temporary private XML removed; see `preflight-wifi.json`.

## Build/check/deployment receipt

Working directory for commands below: `C:\Users\hares\OneDrive\Desktop\ESD-imp\ClawGrid-physical-integration-prep`.

Firmware Python: `C:\Users\hares\Documents\Codex\2026-10-21\i-h\work\clawgrid-env\Scripts\python.exe`; PlatformIO core environment `PLATFORMIO_CORE_DIR=C:\Users\hares\Documents\Codex\2026-10-21\i-h\work\pio-core`.

1. Enumerated with Python `-m serial.tools.list_ports -v`; ADB `devices` showed **no phone**. Each actual MAC was read using `-m esptool --chip esp32s3 --port COM9|COM10 --after hard-reset read-mac`; both matched.
2. `-m platformio run -d speech -e speech_intelligence_uart_short_v1`: first build FAILED82.322s because the existing CMake UART-default rule matched only the old exact environment name. Expanded that rule to the explicit experiment name, retained the failed generated config, and seeded the experiment config from the verified UART SDK config. Retried for this identified build correction: **SUCCESS165.775s**, firmware.bin1,212,496bytes/3MiB app slot (PlatformIO flash usage1,212,137bytes). Logs `build-firmware.log`, `build-firmware-fixed.log` preserve both outcomes. No recognizer tuning was involved.
3. `-m pytest tests/unit/test_short_voice.py tests/unit/test_flash_identity.py tests/unit/test_speech_feed.py -q --basetemp=benchmarks/review/voice-short-v1-20261025/pytest-temp-1 --junitxml=benchmarks/review/voice-short-v1-20261025/host-fixed.xml`: **8 passed31.73s**. Compiled C++ checks exercise mapping and fail-closed boot/room/screen/expiry/score behavior including time wrap. The earlier invocation without a workspace basetemp had6passes/2setup errors due Windows access denial in the default pytest temp root; preserved `host.log`. No failing product assertion was hidden.
4. `scripts/flash_native.py --role B-speech --port COM9 --uart-console --app-only --voice-experiment short-v1`: fresh-MAC guarded **PASS**, normal inhibited/default faults. Experimental SHA256 `437917d7f2e1bcb4e3ea178689d469697b15e07b9d1acd998b547c6dd7d87520`; manifest `benchmarks/raw/hardware/flash_speech_intelligence_uart_short_v1_20261025T070249Z.json`. Wrote application0x10000 only.
5. `scripts/with_gateway_ap.py --output benchmarks/review/voice-short-v1-20261025/preflight-wifi.json -- <firmware-python> scripts/short_voice_preflight.py --node-a COM10 --node-b COM9 --identities benchmarks/review/voice-short-v1-20261025/board-identities.json --output benchmarks/review/voice-short-v1-20261025/preflight.json`: completed the four fixed jobs; **exit1 / FAIL_STOP_NO_PHONE_COLLECTION**, not a successful voice milestone. Run logs retained. No automatic rerun.
6. Restored baseline with `scripts/flash_native.py --role B-speech --port COM9 --uart-console --app-only`: fresh-MAC guarded **PASS**, manifest `benchmarks/raw/hardware/flash_speech_intelligence_uart_20261025T070448Z.json`. The first restoration attempt stopped before USB/write because PlatformIO's environment switch had removed the old build-cache image. Copied the pre-build saved, hash-verified original bin/ELF back to its expected cache path, then ran the guard once. Both restoration logs remain. No rebuild or fabricated replacement baseline.
7. Read-only `benchmarks/review/physical-phone-20261023/phone_probe.py --gateway COM9 --seconds 3 --name short-v1-restored`: **exit0**. Captured fresh advancing sample768,998ms/age124ms, boot17, lightOFF/fanOFF/last_command0/outputs inhibited; AP192.168.4.1/channel1/watchdogtrue/zero sampled misses, speech ready/job0/error0 and original16-command baseline. All owned serial handles closed in finally. No new speech inference in this final verification.

## Rollback and installed-version distinction

Current B restored image SHA256 **f30b10cfb74a4dd79ff28588de2d3be9f6c760c934b48cf1475bf6f7c271b9a4**; ELF **6b94ac5cffa5366891ae0f2e672df2ac9fe463d2560fdba328e161a7c599baff**. A was not flashed; last-known installed inhibited image **8f028bd90f65a55a72b09f4b886db7a3fa851f8a2108c6b79659136db8713cdf**, freshly observed boot17. Do not attribute the restored B's job0 to any prior phone trial.

Baseline source copies, image/ELF and preservation receipts are under `benchmarks/review/voice-short-v1-20261025/`; the experimental image/ELF are saved separately there. Previous evidence is preserved. All62 pinned component hashes, private credential/header/partition hashes and frozen plan hash match. No model/NVS/partition/key/database write. No output-enabled or diagnostic-fault firmware deployed.

Android remains unchanged. Last independently verified installed phone build on24October is **1.0-speech-capture-20261024b**, APK SHA256 **5c932d6299a3e8bb76f4098fc685b8273f553894a91fadcf561794db00d174b4** at `deliverables/ClawGrid-Speech-Capture-debug-20261024b.apk`. That is historical installed evidence; no phone was visible to ADB or independently inspected today. No newer APK exists from this experiment. No new GitHub/Downloads publication.

## Files affected

- `experiments/voice-short-v1.json`: immutable vocabulary, frontend, fixtures, prompts, acceptance and stopping conditions.
- `firmware/common/runtime/short_voice.h`: new binding/mapping/expiry guard.
- `firmware/common/runtime/speech_runtime.h`, `speech_feed.h`: binding snapshot and isolated preflight input.
- `speech/main/speech_runtime.cpp`: opt-in two-command registration, same inference frontend, non-authorizing preflight result.
- `firmware/node_intelligence/src/main.cpp`: upload context validation, result metadata, proposal/confirmation binding checks, isolated preflight route.
- `speech/platformio.ini`, `speech/CMakeLists.txt`: explicit inhibited experiment environment and matching UART defaults.
- `scripts/flash_native.py`: explicit experiment image/environment verification; ordinary baseline flashing retained.
- `scripts/short_voice_preflight.py`: four fixed fixtures, fingerprint/delivery/safety checks, bounded persisted outcomes, finally cleanup; refuses to overwrite its result.
- `tests/unit/test_short_voice.py`: frozen-budget, flashing-mode and compiled binding tests.
- Master, handoff and decision-report status pointers updated. Android source/signing unchanged. Generated local review/config/build artifacts are not product source or public audio exports.

## Decision and exact supported migration requirements

**Close MN6/short-v1 feasibility for this prototype configuration, as predefined.** A weak positive and a failed mandatory OFF reference do not justify collecting40 phone clips or advertising speech control. The supported next candidate is **mn7_en on ESP32-S3 in the pinned ESP-SR1.7.0**, a separate reviewed migration, not a promised improvement. [Pinned Kconfig](https://github.com/espressif/esp-sr/blob/07b0a4d0f6b5de755874d5405150ee0a8eec9865/Kconfig.projbuild), [official pinned command documentation](https://raw.githubusercontent.com/espressif/esp-sr/07b0a4d0f6b5de755874d5405150ee0a8eec9865/docs/en/speech_command_recognition/README.rst).

No migration has begun. Requirements before another live trial:

1. **Acquire/verify separate assets without changing MN6.** The current selective downloader only includes `model/multinet_model/mn6_en/`; `mn7_en` is absent locally. At the same pinned commit, required `mn7_en/{_MODEL_INFO_,mn7_data,mn7_index,vocab}` totals2,751,129 raw bytes. Read-only upstream metadata/blob IDs are preserved in `mn7-readonly-asset-inventory.json`; no weights were downloaded. Add an explicit separate model selection/manifest and pinned `tool/multinet_g2p.py` plus its dependencies/licences. Do not rewrite the verified62-file receipt as if the new assets had always been installed.
2. **Use MN7 registration/preprocessing semantics.** It uses English phonemes; recommended supplied phonemes come from the pinned G2P tool, while omitted phonemes invoke a different internal conversion. Freeze one supported registration method and inspect its generated IDs/FST against the official example. Check the official AFE16kHz/16bit/mono path and the model's queried frame/channels; do not assume the current raw MN6 frontend transfers unchanged. Preserve phone PCM transport evidence and safety bindings.
3. **Audit runtime and threshold units independently.** Initialization currently explicitly requires `mn6_en`; add an exact MN7 opt-in selector, not a loose first-English-model filter. Current CMake archive hash verification must remain. The model224 MN6 private-layout/log-unit adapter and decoder tracing must never run on MN7. Establish MN7 public-setter/result probability semantics from the pinned implementation/reference, fail closed on uncertainty, and retain final probability>=0.7. Do not guess another private offset or silently reuse MN6's shim.
4. **Prepare a reversible model write rather than weakening the blank-model guard.** The existing model partition is0x610000/0x600000bytes and nonblank. The current `--install-missing-speech-model` correctly refuses overwriting it. First fresh-verify B/partition table, read and hash the complete installed6MiB region into two local rollback copies, and verify the known baseline application. Pack/measure a candidate (prefer retaining both models if the exact packed size fits), explicitly select MN7, and stop if it needs repartitioning or loses rollback assets. Any replacement needs a separately reviewed guarded migration command bound to the current MAC/table/model hashes, explicit write range and readback verification. No such write/tool was authorized by this failed experiment or executed here. NVS/credentials remain outside writes.
5. **Measure before claiming compatibility.** Verify model load, phrase IDs, full PCM delivery, heap/PSRAM/stack, bounded inference/job queues, HTTP deadlines, controller telemetry/ESP-NOW channel, RTOS/watchdog and output inhibition. Run frozen reference/negative preflight once; stop on a failed prerequisite. No model-fit/latency/recognition guarantee follows from raw file size.
6. **Only after preflight, finish Android and a new finite screen.** Implement explicit room selection, capture-start room+boot+nonce binding, sticky cancellation on context/disconnect/stale/reboot/expiry, visible complete target, no auto-confirmation/replay, and a private bounded immutable clip/result ledger. Freeze model/phrases/frontend/criteria before fresh data; keep the same40+5 cap and safety stopping rules unless the user explicitly reviews a different experiment. Distinguish recognition, proposal, ACK/readback and unmeasured physical switching.

**Next executable step:** review the MN7 readiness/migration requirements above and, if that separate direction is selected, begin an off-board asset/ABI/packing audit. Do not resume MN6 phrase searches, request phone recordings, download/install a model silently, or move to physical wiring. Milestones2–4 remain pending; sensor models still need identification when that stage is reached.
