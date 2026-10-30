# Separate, reversible MN7 evaluation assets

**Closed after failed mandatory LIGHT DISABLE reference.** Exact baseline application/model restored and runtime verified; MN7 is not installed now. No phone batch. See [actual results](../../docs/MN7_EVALUATION_20261025.md). The current deployment manifest is CLOSED_FAILED_PREFLIGHT; do not redeploy for another unbounded attempt.

Pinned ESP-SR1.7.0 commit `07b0a4d0f6b5de755874d5405150ee0a8eec9865`. `upstream/` contains verified official assets, docs, G2P tool and licence; `upstream-manifest.json` records their Git blob IDs and SHA256. Existing `speech/components/esp-sr` and its62-file receipt remain unchanged. Reproduce downloads with `scripts/prepare_mn7_assets.py`.

This is a candidate evaluation, not a delivered phone-recognition feature. Current observed outcome and installed firmware belong in the master/handoff and dated evaluation report; historical intermediate manifests do not prove installation.

- `candidate/`: unconfigured preparation pack with lowercase `light enable` and `light disable` in the factory command file. The initial public-threshold blocker is retained in `candidate-manifest.json`.
- `copack-size-check/`: measured MN6+MN7+FST pack **6,539,556bytes**, exceeding the unchanged6MiB model partition by248,100bytes. It must never be flashed there.
- `candidate-configured/`: MN7-only+FST pack **2,758,776bytes**. Only native JSON model configuration changes; source acoustic weights, vocabulary/index and all bytes outside the original JSON document remain identical. It does not retain MN6 in the live partition; two verified host rollback copies are mandatory before deployment.
- `model-configuration-receipt.json`: exact original/configured hashes, JSON span and equivalent log threshold. The configured pack is a ClawGrid configuration variant, not falsely labelled an unmodified official binary.

The independently audited MN7 public setter stores its positive probability argument without converting it, while its RNNT qualification uses mean log probability and results use exp(mean). Passing0.7 through that setter is not a valid0.7 log-domain configuration, and passing ln(0.7) is clamped positive. The MN7 constructor independently supports `det_threshold` in its parsed model JSON. `scripts/configure_mn7_model.py` adds that native field as **ln(0.7)**, compacts/pads the same-length JSON span, verifies all other keys unchanged and changes no acoustic byte. No runtime model offset read/write, MN6 header, binary code patch, public-setter workaround or default/lower-threshold fallback is used. The final returned-probability gate remains0.7.

Registration is the pinned **public API with lowercase graphemes**. `esp_mn_commands_add` performs internal FLITE G2P under `CONFIG_SR_MN_EN_MULTINET7_QUANT`; the runtime verifies/logs nonempty public phrase phonemes and IDs before `esp_mn_commands_update`. Do not feed externally generated phonemes into this grapheme API. Internal G2P is an explicit frozen choice; the external recommended converter is preserved as reference and was not used to tune these commands.

Frozen command set, AFE settings, resource floors,5-job preflight limit and conditional40+5 phone limit: `experiments/mn7-eval-v1.json`. The final plan was sealed before any MN7 target inference. Original blocked preparation plan remains in the dated private review directory; the later JSON-path correction was identified before data collection, not fitted to a recording.

Guarded operations:

```powershell
# B only. Discover its current port; do not assume these historical ports.
& <firmware-python> scripts/mn7_migration.py backup --port <B-port> --receipt <private-receipt.json>
# Deploy only an explicit READY manifest whose artifacts/MAC/baseline hashes match.
& <firmware-python> scripts/mn7_migration.py deploy --port <B-port> --backup <primary-backup> --candidate <ready-manifest.json>
# Recover exact original full app0/model regions, preserving current NVS/OTA.
& <firmware-python> scripts/mn7_migration.py rollback --port <B-port> --backup <primary-backup>
```

The guard never writes bootloader, partitions, NVS or OTA data. Before/after ROM reads verify these protected regions. No erase-chip/erase-flash operation exists. Both copies must pass size/hash checks. A partially failed deployment triggers one rollback attempt; failure to restore stops and requires physical USB/BOOT recovery. The model partition is a single bank, so power-loss recovery relies on the verified off-board copies and unchanged ROM/UART boot path; this is not atomic OTA or a second on-board model bank. Do not overwrite, publish or delete private backups; they contain credentials.
