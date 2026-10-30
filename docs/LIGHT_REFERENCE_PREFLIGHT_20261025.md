# Approved four-clip LIGHT ON/OFF preflight — closed after first failure

**25 October 2026 IST: FAIL_STOP_NO_PHONE_COLLECTION.** Only the first approved clip ran. LIGHT ON did not return an accepted command at the unchanged0.7 gate, so the strict first-failure rule stopped the sequence. LIGHT OFF, silence and the predefined non-command were **NOT RUN**. The exact saved baseline app was restored; baseline app, full model and system regions were verified against flash, then baseline runtime was checked. No phone recording, proposal, actuator command, threshold/fixture replacement or decoder investigation followed.

## Frozen scope and actual per-clip results

Approval was **only four reference clips**, not the proposed40phone screen. The original proposal remains unchanged, SHA85488688e7cfbb03d37f3b78c44e2abba2df598bd031ef8af7024fa50f7174aa. A separate narrower manifest `experiments/light-on-off-reference-preflight-v1.json` was sealed before target inference, SHA **ccf13855ed05da117b3d4d8ecbe92cb89007db454bd9ec92b0aef000b4fe9104**.

ID1 LIGHT ON→Bedroom/light/ON; ID2 LIGHT OFF→Bedroom/light/OFF. Existing pinned MN6/model assets/raw16kHz mono signed PCM16LE frontend,512-sample frames,6s model window/601-call bound and the existing probability-to-log unit repair were unchanged. Final acceptance0.7; no AFE/gain/rate/threshold tuning. Microsoft Zira Desktop generated each new positive once using System.Speech, default rate0/volume100, native16kHz mono PCM16 WAV; no edits, resynthesis or replacement. Silence/non-command are the exact earlier predefined files and hashes. Source inputs are labelled SIMULATED; target results are HARDWARE_MEASURED.

Explicit selection was `--room bedroom` in the recognition-only harness, bound in HTTP headers and the gateway snapshot to controller boot18/capture1. It was not inferred from speech. No Android room-selection UI or phone functionality was implemented/tested in this approved preflight.

| Order / clip | Actual result | Accepted command / score | PCM integrity | Engine error |
|---|---|---|---|---|
|1 LIGHT ON|TIMEOUT at198frames; failed gate|No accepted ID; candidate_id0/raw_result_count0; accepted score unavailable|23,280samples MATCH; CRC1936402414 MATCH|error_code0; engine_errors0|
|2 LIGHT OFF|NOT RUN: stop after1|Not observed|Source23,040samples/CRC2336777373 frozen; not uploaded|Not observed|
|3 Silence|NOT RUN: stop after1|Not observed|Source32,000samples/CRC2109903275 frozen; not uploaded|Not observed|
|4 Predefined non-command|NOT RUN: stop after1|Not observed|Source33,760samples/CRC3492560341 frozen; not uploaded|Not observed|

The first result's untrusted `diagnostic_text` was **LIGHT ON**, but `reference_detected:false`, `last_state:2`, `raw_result_count:0`, `command_id:0`, `authorized:false`. Matching diagnostic text does **not** satisfy the required accepted-ID/confidence contract and was not mapped to an intent or proposal. Zero-valued candidate/confidence fields mean no returned accepted candidate here, not an independently measured zero likelihood. No further score/FST instrumentation was performed.

Original ON PCM SHA256 **b87b69dae4b3ead2f1b02236c1ae384ac0ffdfca09dd92a0ecba1ec56420c720**; OFF **dcd2baf80de3ca95365e459f2a8781eaa0f33df85eb01f2b8d0f99d3cf132593**. ON actual peak23,727/RMS2,491/no clipped samples; processing3,090,006us, max model frame28,147us, speech stack reserve4,648bytes. The generated text inputs were fixed as above; independent human audition of these new ON/OFF fixtures was not recorded, and no direct assistant listening is claimed. Source transport/levels and diagnostic text are not pronunciation-quality proof. Shared `mn_internal_*` fields were unpopulated zeros in this MN6 runtime; they must not be interpreted as real zero heap measurements.

Full results: `benchmarks/review/light-on-off-preflight-20261025/preflight.json`, source WAVs and frozen manifest. OFF/negative behavior and actual phone speech remain unverified. Do not describe this as all four clips evaluated, OFF failure or a negative false-accept test.

## Recognition-only implementation and safeguards

An explicit `speech_intelligence_uart_light_preflight` environment uses the existing MN6 runtime and model bank. Its separate image marker is guarded by `--voice-experiment light-preflight-v1 --app-only`; model installation is refused. It is a normal **outputs-inhibited/default-faults** image, not an output-enabled or diagnostic-fault image.

The candidate disables command dispatch/proposal/text-confirmation and simulated-input mutation. Only the reference endpoint accepts these four frozen PCM fingerprints in order, bound to selected Bedroom/current boot/capture nonce with screen-only intent. The firmware refuses a fifth reference job and non-reference uploads. The host has an explicit read/reference-only HTTP allowlist, saves a persistent no-replay ledger before uploading and refuses to overwrite prior results. Any missing/wrong positive, negative detection, PCM/context mismatch or engine/health error stops; no automatic recording retry. Existing SafetyKernel/RTOS/watchdog code remains intact.

Frozen source/image hashes in `sealed-source-and-build.json` remained unchanged through the run. Candidate image SHA256 **1e912904ac14db2c7b699f5f0bc60e7d71c2ae813e81d29e02e16832618e94ce** is preserved but is **not installed now**. No default policy semantics were changed.

## Identity, health and rollback evidence

Enumerated current CH343 adapters and read both ROM MACs before assigning roles: B COM9/serial5C37271527/MACe0:72:a1:f6:a9:68; A COM10/5C37271524/MACe0:72:a1:f5:bc:b0. Both matched existing provisioning. Identity reads deliberately resumed the applications with normal resets; A advanced from boot17 to **boot18**. A was not flashed or commanded. No NVS erase/restore was performed; normal firmware boot-counter maintenance is distinct from a tool writing NVS.

Before deployment, ROM verify-flash confirmed B's baseline application, full6MiB model and system against saved files. Both complete private rollback copies from the earlier MN7 preparation passed size/hash checks and remain intact. No new model migration, partition change, credential generation or model/NVS write.

During the single reference: A sample569,996→573,996ms and Control/Link counters113,870→114,706 advanced, boot18 unchanged; OFF/OFF,last_command0,outputs inhibited. B Link jobs2,537→3,318/watchdogtrue/channel1/zero sampled deadline misses. Gateway transaction remained id0/phase0 before/after. Room/boot/capture/screen-only metadata matched. These are bounded observations, not physical switching or a soak test.

Guarded candidate application-only flash manifest: `benchmarks/raw/hardware/flash_speech_intelligence_uart_light_preflight_20261025T082648Z.json`, PASS. Restore manifest: `flash_speech_intelligence_uart_20261025T082737Z.json`, PASS. Only application0x10000 was written on B. The runner restored the saved original bin/ELF into the expected cache path before invoking the guard, preserving rollback despite PlatformIO environment-cache cleanup. No substitute baseline rebuild.

Final ROM verification checked baseline app prefix **f30b10cfb74a4dd79ff28588de2d3be9f6c760c934b48cf1475bf6f7c271b9a4**, original full model **49b9215a315a06e900e1a2f4538b2513611253cc30a243802a5a58aedf9dbc7e**, and saved system. Logs: `restored-flash-verification/`. Then read-only baseline probe exit0 showed fresh authenticated A boot18, sample634,996ms/age880ms, lightOFF/fanOFF/last_command0/outputs inhibited; B AP192.168.4.1/channel1/watchdogtrue/zero sampled misses, original MN6 ready/job0/16commands/error0, APclients0. Physical loads remain disconnected by the user's report; no physical sensing/relay switching was tested.

All62pinned component hashes and local_credentials/private header/partition CSV hashes match; the tested source/frozen manifest hashes match the sealed copies. Original MN6/MN7 failed evidence and model assets preserved. Laptop Wi-Fi restored, owned temporary profile/XML removed, all owned serial handles closed. No Android source/build/install or new GitHub/Downloads publication.

## Checks run and source files

Working directory `C:\Users\hares\OneDrive\Desktop\ESD-imp\ClawGrid-physical-integration-prep`. Firmware Python `C:\Users\hares\Documents\Codex\2026-10-21\i-h\work\clawgrid-env\Scripts\python.exe`; PlatformIO core `C:\Users\hares\Documents\Codex\2026-10-21\i-h\work\pio-core`.

- `scripts/prepare_light_preflight_manifest.py`: sealed the fixed mapping/audio/header/configuration; no inference. Existing source/proposal manifests preserved.
- `-m platformio run -d speech -e speech_intelligence_uart_light_preflight`: **SUCCESS180.883s**, `build.log`/`build-exit.txt`.
- `-m pytest tests/unit/test_light_reference_preflight.py tests/unit/test_flash_identity.py tests/unit/test_speech_feed.py -q --basetemp=benchmarks/review/light-on-off-preflight-20261025/pytest-1 --junitxml=benchmarks/review/light-on-off-preflight-20261025/host.xml`: **10passed47.65s**. Compiled contract checks reject modified/out-of-order/fifth clips; simulated host cases prove first-positive-failure stop, negative-detection stop, exactly4maximum/no proposal POSTs and no ledger replay. These host simulations are not extra ESP recognition clips.
- `scripts/run_light_reference_preflight.py --room bedroom --identities benchmarks/review/light-on-off-preflight-20261025/board-identities.json --output benchmarks/review/light-on-off-preflight-20261025`: **exit1**, the failed gate. deploy0/preflight1/restore0/baseline-runtime0; `campaign.json` records BASELINE_FLASH_AND_RUNTIME_VERIFIED, phone0/proposals0/commands0/serial_closedtrue.
- Its one AP child ran `light_reference_preflight.py` once, attempted exactly1audio job, and listed the other3NOT RUN. No second reference pass, alternative fixture or decoder investigation.
- Automatic final read-only `phone_probe.py --gateway COM9 --seconds 3 --name light-reference-restored-20261025`: exit0, results above. No additional recognition inference.

Affected source: speech `platformio.ini`/CMake; new `light_preflight_contract.h`; shared grammar header and MN6 runtime compile-guarded pair/quota/fingerprint handling; gateway compile-guarded recognition-only/context routing; guarded flasher explicit app-only environment; new `prepare_light_preflight_manifest.py`, `light_reference_preflight.py`, `run_light_reference_preflight.py` and focused host tests. Defaults and previous experiments retained. Master/handoff/report/status files updated. No Android implementation or SafetyKernel change.

## Decision and remaining architecture choices

**Close the LIGHT ON/OFF phrase experiment as failed at its first mandatory positive. Do not start the40phone recordings, retry these fixtures, test more synonyms, lower thresholds, use diagnostic text as an accepted command, or open another decoder investigation.** The OFF/negative/phone outcomes remain unknown; this failure is not a universal model/accuracy conclusion.

Remaining choices, none started:

1. **Continue ClawGrid with native Android manual control and voice explicitly unavailable.** This is the evidence-backed current path. Complete remaining fan/AUTO/policy/timers/scenes/software and later sensor/load verification through SafetyKernel; do not claim those milestones are already complete.
2. **Keep the ESP-only local-voice requirement but leave it blocked.** Reopening it would require a separately supported, evidence-backed vendor/model/configuration decision. No new training, model/SDK migration or additional phrase experiment is authorized by this result.
3. **Explicitly reconsider where local recognition runs** if voice is mandatory. A validated offline phone or another local processor would change the approved phone-PCM→ESP recognition architecture and needs a separate user decision and evidence. No host/cloud transcript substitution, product choice, installation or test was made here; cloud speech would conflict with the local-recognition requirement.

**Next executable step:** retain the restored baseline and review moving to the remaining one-room software with voice marked blocked. There is no pending speech experiment or phone-test instruction to execute from this failed preflight. Wiring remains deferred; sensor models still need identification when that stage is reached.
