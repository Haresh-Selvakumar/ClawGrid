# Bounded MN7 evaluation: failed OFF reference, exact baseline restored

**25 October 2026 — FAILED_STOP_NO_PHONE_COLLECTION.** MN6 short-v1 remains closed with its results preserved. The separately approved MN7 evaluation completed preparation, verified backups, guarded deployment and three target reference jobs. The mandatory LIGHT DISABLE reference failed, activating the frozen first-failure stop. The guard restored B's original application and full model region and verified the running baseline. **No phone recording, new APK, speech proposal/confirmation, actuator command or physical switching occurred.** Do not restart a word/model/frontend search from this result.

## Pinned support, registration and threshold configuration

Verified MN7 English/S3 support in ESP-SR1.7.0 commit `07b0a4d0f6b5de755874d5405150ee0a8eec9865`: Kconfig `CONFIG_SR_MN_EN_MULTINET7_QUANT`, model-packer `mn7_en`/FST selection, public command API implementation, model interface and official command/AFE documentation. [Pinned Kconfig](https://github.com/espressif/esp-sr/blob/07b0a4d0f6b5de755874d5405150ee0a8eec9865/Kconfig.projbuild), [pinned command documentation](https://raw.githubusercontent.com/espressif/esp-sr/07b0a4d0f6b5de755874d5405150ee0a8eec9865/docs/en/speech_command_recognition/README.rst).

Separate official assets under `models/mn7-eval-v1/upstream/` were verified by Git blob and SHA256;8files/2,785,934bytes including model files, licence, G2P tool and docs. Existing62pinned component files remain byte-identical. No original MN6 model file was overwritten off-board.

Frozen intended commands: ID1 **LIGHT ENABLE → Bedroom/light/ON**; ID2 **LIGHT DISABLE → Bedroom/light/OFF**. Registration supplies lowercase graphemes to `esp_mn_commands_add`; the pinned MN7 implementation performs internal FLITE G2P under its MN7 configuration. Runtime checks public phrase IDs/nonempty phonemes before update. It actually logged:

| ID | Graphemes | Registered phonemes |
|---|---|---|
|1|light enable|LiT fNdBcL|
|2|light disable|LiT DgSdBcL|

This is the supported internal-conversion path, explicitly frozen rather than fitted to audio. The external G2P tool was preserved as reference, not substituted after failures. No MN6 grapheme registration or runtime private-ABI adapter was reused.

An independent audit of the pinned **MN7** object found that its public setter clamps to positive0.1..0.9999, stores that raw value, and its RNNT consumer compares a normalized log score; result conversion uses exp(mean). Thus the setter cannot express equivalent probability0.7 correctly. Passing ln(0.7) to it also clamps positive. We did **not** use that setter, default/lower-threshold fallback, model-memory offsets or binary-code patches.

Before target inference, the same independent audit identified the constructor's native optional JSON `det_threshold` field. The separate configured model adds **ln(0.7)=-0.35667494393873245** to that model configuration. `configure_mn7_model.py` pins the original model hash, finds one valid complete English/phoneme JSON object, compacts/pads its original6,016-byte span, verifies identical other JSON keys, and preserves the index, total data length and every byte outside that JSON. Acoustic weights are unchanged. The final returned-probability gate also remains0.7. This configured artifact is labelled a ClawGrid model-configuration variant, not an unmodified upstream model.

Evidence: `mn7-threshold-audit.txt`, independently extracted `multinet7_quantized.c.obj`, `mn7-complete-disassembly.txt`, constructor strings/literals and `model-configuration-receipt.json` in the dated review/assets directories. Offsets in host disassembly are observational evidence only; the MN7 runtime performs no private model read/write. An initially blocked preparation plan/manifest is preserved; the native-JSON configuration was resolved **before collecting any MN7 target inference**, not tuned after a failed recording.

The separate MN7 runtime uses ESP-SR AFE, mono16kHz signed PCM16LE, SSP enhancement, VAD_MODE_3, AGC mode2, gain1, no AEC/wakenet, PSRAM-preferred allocation, AFE core1/priority2 and speech priority2. A fresh AFE instance and recognizer clean separate jobs. PCM delivery/levels and processed AFE samples/CRC are distinct observations. Model frame512,6s window,601-call bound and15s job deadline were frozen. Existing LinkTask, RTOS/watchdog, authorization and SafetyKernel remain intact.

## Recoverability and actual deployment

Fresh B ROM MAC matched provisioning: **e0:72:a1:f6:a9:68**, currently COM9/CH343 serial5C37271527. A remained on COM10/5C37271524 and was verified through the existing authenticated provisioned link/runtime. **No A ROM reset, flash, policy or command operation** was performed; A stayed boot17.

Read and verified against on-board flash: system/partition0..0x9000, NVS0x9000/0x5000, OTA0xe000/0x2000, full app0x10000/0x300000 and full model0x610000/0x600000. The installed application prefix matched saved baselinef30b10cf.... Local provisioning/header/CSV copies and hashes were preserved privately. Every region and private file exists in two verified local directories:

- `C:\Users\hares\OneDrive\Desktop\ESD-imp\ClawGrid-physical-integration-prep\benchmarks\backups\mn7-20261025T072239Z`
- `C:\Users\hares\Documents\ClawGrid-private-backups\mn7-20261025T072239Z`

These are two copies on this computer, not a claim of separate physical-disk disaster recovery. They contain credentials; do not publish/delete/overwrite them. Both copies were checked again after rollback.

The preferred MN6+MN7+FST co-pack measured **6,539,556bytes**, exceeding the unchanged6MiB model partition by248,100bytes. The separate MN7+FST candidate measured **2,758,776bytes** and fits. The authorized deployment therefore used the explicit single-bank/dual-verified-backup strategy. ROM/UART recovery remains available through the unchanged bootloader/table; this is not atomic model OTA or a second on-board model bank.

The new guard validates backup sizes/hashes/copies, B's fresh MAC, unchanged table/system, baseline application and current full model hash. Only explicit READY artifact hashes in existing app/model ranges can be deployed. Protected system/NVS/OTA bytes are compared before/after writes while held in ROM. Rollback restores the original full app/model files, preserves current NVS/OTA/normal boot counters, and verifies them against the board. A partial deployment failure has one rollback attempt; rollback failure stops for physical recovery, not a retry loop. The normal blank-model guard was not weakened.

| Artifact | SHA256 |
|---|---|
|Experimental MN7 app,1,823,232bytes|23c223b9ebe62e0c172107038a300201ce06a24b1b1709eac85254ced10b8416|
|Configured MN7 pack,2,758,776bytes|f07b20e797fd6e4eafe4d9189c8f5598381d95c00babf46ade9ffc0514b8b997|
|Original/restored model,full6MiB|49b9215a315a06e900e1a2f4538b2513611253cc30a243802a5a58aedf9dbc7e|
|Original/restored full app0,3MiB including unused tail|fdabd4bc40c6cbb2ae3fe20daad638c23c83105f16cb2c09226281104053e249|
|Restored baseline application image prefix|f30b10cfb74a4dd79ff28588de2d3be9f6c760c934b48cf1475bf6f7c271b9a4|

Write/readback receipts: backup `deploy-20261025T073700081471Z/result.json` PASS and `rollback-20261025T074217410430Z/result.json` PASS, with individual esptool verify logs. No erase-chip command, NVS/table/bootloader write, credential regeneration, output-enabled image, diagnostic-fault image or deployed database change. Protected-region comparisons passed. Firmware's own normal boot-counter maintenance is distinct from a tool writing/restoring NVS.

## Frozen criteria and actual results

Final plan `experiments/mn7-eval-v1.json`, SHA256 **3039cf68c3f6e6d15260f37cab2432e9e499cd32719e3634e6e6fa9ebc671b7d**, was sealed before target inference. Same two positive synthetic fixtures as MN6, plus the official compatibility reference and fixed silence/non-command controls; maximum5jobs. **Stop at the first failed positive, wrong/negative acceptance, resource/engine/transport or safety prerequisite.** Do not tune/replay that clip. Conditional phone screening retained the prior40+5 cap, >=9/10each positive, zero wrong/negative acceptances and explicit confirmation/ACK/readback; it was never reached.

Inputs are labelled SIMULATED or pinned official reference; byte/size checks are HOST_MEASURED and target inference is HARDWARE_MEASURED. All three upload/ESP sample counts and CRCs matched; no host/phone/cloud recognizer produced product commands.

| ESP diagnostic job | Input samples / CRC32 | Result | Frames | AFE+recognition processing | Max model-frame time |
|---|---|---|---:|---:|---:|
|1 official TELL ME A JOKE|44,829 /330285049|Reference ID17 accepted0.75179|78|2.282180s|20.954ms|
|2 LIGHT ENABLE|25,520 /3001788789|Reference ID1 accepted0.85891|48|1.324723s|21.638ms|
|3 LIGHT DISABLE|27,200 /3370348365|No accepted candidate; TIMEOUT|198|3.703556s|24.889ms|

All jobs had error0/engine_errors0 and returned actionable `command_id:0`, `authorized:false`, `diagnostic_only:true`. Diagnostic acceptance is not a proposed or confirmed control command. OFF raw_result_count0; displayed candidate_score0 means no returned accepted candidate, not a measured full-path probability. Untrusted diagnostic phoneme text `LiK To SdBcL` remains evidence, not a substitute recognized intent. Registered OFF target was ID2/`LiT DgSdBcL`; the more specific acoustic cause is undetermined and no new debugging loop was opened.

**Silence and non-command controls were NOT RUN**, because the earlier mandatory positive failed. Therefore this evaluation establishes no negative false-accept rate. No phone screening, microphone permission/capture evaluation, recognized proposal/confirmation, ACK/readback chain or OFF→ON→OFF was tested. Synthetic/reference successes are not phone-voice success or an accuracy estimate.

Measured resources/health during these three target jobs:

- Internal free after jobs89,835→89,607bytes; observed internal minimum41,367bytes; largest internal block36,864bytes. Speech stack high-water reserve2,672bytes. These met the frozen floors on observed jobs, not a production resource guarantee.
- PSRAM free after each job5,289,375bytes; HTTP memory snapshots showed5,289,551before/5,289,375after, total8MiB. This is post-job headroom; peak PSRAM consumption inside AFE was not retained and must not be invented. Job durations above include AFE creation/feeding/fetching and recognition, not only neural compute or upload latency.
- A boot17 unchanged; controller sample2,902,998→2,911,998ms, lightOFF/fanOFF/last_command0/inhibited. A Control/Link counters580,537→582,472, no stall. B Link jobs3,136→4,989, zero sampled deadline misses, watchdog active, max observed Link execution2,855us, AP192.168.4.1/channel1 matching A. No unexpected reboot/output enablement observed. This was a bounded observation, not a soak test.
- AFE sample/CRC pairs: official77,312/3451230078; ENABLE57,856/607669255; DISABLE59,904/491089098. Processed PCM differs intentionally from source PCM; delivery comparisons use original source CRC/count.

Detailed records: `benchmarks/review/mn7-eval-v1-20261025/campaign/{preflight.json,startup-records.json,evaluation.json,wifi.json}` and raw UART journals referenced by the harness. `evaluation.json` records deploy0/preflight1/rollback0/baseline-runtime0, final FAILED_STOP_NO_PHONE_COLLECTION, rollback FLASH_AND_BASELINE_RUNTIME_VERIFIED, serial_closedtrue. The initial Windows profile-restore request returned1; subsequent observation confirmed the previous Wi-Fi profile restored. The owned temporary profile and private XML were removed. The nonzero request result remains in wifi.json.

## Exact checks and source changes

Working directory `C:\Users\hares\OneDrive\Desktop\ESD-imp\ClawGrid-physical-integration-prep`. Firmware Python `C:\Users\hares\Documents\Codex\2026-10-21\i-h\work\clawgrid-env\Scripts\python.exe`; PlatformIO core `C:\Users\hares\Documents\Codex\2026-10-21\i-h\work\pio-core`.

1. `scripts/prepare_mn7_assets.py`:8official files/blob+SHA verified, separate path. `configure_mn7_model.configure` and pinned `pack_model.pack_models` prepared the candidate/configuration receipt and measured both packs off-board. Source/JSON/acoustic-byte checks pass.
2. `scripts/mn7_migration.py backup --port COM9 --receipt benchmarks/review/mn7-eval-v1-20261025/backup-receipt.json`: PASS, board verify-flash for every region and two-copy hash checks. B only; A not reset.
3. With `PLATFORMIO_CORE_DIR` above, `-m platformio run -d speech -e speech_intelligence_uart_mn7_eval`: first FAILED99.150s due `-Werror=misleading-indentation` in the new AFE feed loop. Added explicit line separation and moved initial memory sampling outside the snapshot lock. Corrected build **SUCCESS114.474s**; `build.log`, `build-fixed.log`, `build-exit.txt` retain exact results. No confidence/frontend change after data.
4. `-m pytest tests/unit/test_mn7_preparation.py tests/unit/test_flash_identity.py -q --basetemp=benchmarks/review/mn7-eval-v1-20261025/pytest-host-2 --junitxml=benchmarks/review/mn7-eval-v1-20261025/host-final.xml`: **16passed1.62s**. Added after initial14passed1.42s: bounded rollback failure-injection and frozen-plan/path checks. Tests cover wrong/reapplied model rejection, unchanged acoustic bytes/config semantics, fixed threshold, partition changes, two-copy tampering, unready candidate refusal, no MN6 runtime ABI access and exactly one simulated recovery with writes confined to app/model. No physical fault image or forced board failure used.
5. `scripts/run_mn7_evaluation.py --controller COM10 --gateway COM9 --backup benchmarks/backups/mn7-20261025T072239Z --candidate benchmarks/review/mn7-eval-v1-20261025/deployment-candidate.json --output benchmarks/review/mn7-eval-v1-20261025/campaign`: **exit1**, the intended failed-prerequisite stop; guarded deployment0 and rollback0. Harness ran `with_gateway_ap.py`/`mn7_preflight.py` once. Three of five maximum clips evaluated, no repeats. The used READY candidate receipt is retained as `deployment-candidate-as-deployed.json`; the current candidate is marked CLOSED_FAILED_PREFLIGHT, so the guard refuses automatic reuse.
6. Automatic final read-only `phone_probe.py --gateway COM9 --seconds 3 --name mn7-rollback-20261025`: **exit0**. Baseline sample3,083,998ms/age265ms, A boot17/lightOFF/fanOFF/last_command0/outputs inhibited; B speech ready MN6/job0/original16commands/error0, AP192.168.4.1/channel1/watchdogtrue/zero sampled misses/APclients0. All owned serial connections closed. This is baseline runtime verification, not another recognition run.
7. Post-run preservation check: both full backup copies valid; all62pinned component hashes match; local_credentials/private header/partition CSV hashes unchanged; MN6 short and final MN7 frozen plans unchanged. Receipts `preservation-final.json`. No Android source/build/signing/install change or new GitHub/Downloads publication.

Source files: new `scripts/{prepare_mn7_assets,configure_mn7_model,mn7_migration,mn7_preflight,run_mn7_evaluation}.py`; new `tests/unit/test_mn7_preparation.py`; new `speech/main/speech_runtime_mn7.cpp` and explicit `speech/sdkconfig.mn7.defaults`; modified speech CMake/main CMake/platformio and shared speech snapshot/feed/grammar headers plus gateway metadata/reference routing. `.gitignore` allows the nonsecret MN7 defaults as source. Separate model/configuration manifests, frozen experiment, asset README, master/handoff/reports record provenance. The old MN6 runtime and evidence are retained; source adds an opt-in candidate, not a new accepted product voice feature.

## Current state and next step

**B is back on the exact baseline; MN7 is not installed now.** A unchanged, bare-board outputs inhibited. User reports sensors/relays/loads disconnected; none were wired or measured. Last independently verified installed Android build remains24October `1.0-speech-capture-20261024b`, SHA5c932d6299a3e8bb76f4098fc685b8273f553894a91fadcf561794db00d174b4; no new APK or fresh on-phone inspection today.

Close this exact MN7/short-command/AFE configuration as failing its frozen feasibility gate. This does not establish that MN7 is universally unsuitable, identify a specific pronunciation cause or estimate phone accuracy. It does show that the tested configuration is not ready to justify phone collection. Do not switch models, lower thresholds, reinterpret decoder text or weaken confirmation/SafetyKernel to finish a milestone.

**Next executable step:** keep manual native Android control as the verified base and review proceeding with the remaining one-room software while voice is explicitly blocked. Reopening speech requires a separately bounded, evidence-backed change or vendor-supported model/configuration decision before any new data; no additional recognition campaign is queued. Physical wiring remains deferred. Persistent manual/AUTO policy, fan/timers/scenes, real sensor models, multi-room and cloud milestones remain unverified/pending, not silently completed by this evaluation.
