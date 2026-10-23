# Local push to talk assistant

Updated 21 October 2026. Recognition runs only on Node B's pinned ESP-SR 1.7.0 MultiNet6 English build. A owns SafetyKernel. No hardware is available in this software session, so target recognition is still unverified after these repairs. The last retained target corpus detected 0/16 synthetic commands despite 18/18 matching PCM checksums; successful host tests do not replace that result.

The browser captures only after the record button is pressed. Its actual AudioContext rate is used to convert captured mono audio to signed PCM16 little-endian at 16 kHz; 44.1/48 kHz devices no longer cause immediate rejection. Conversion averages input intervals when downsampling, clips amplitude to the PCM range, and caps output at 64,000 samples (four seconds). This bounded baseline is host-tested; it is not a claim of an optimized acoustic resampler. Recording can truncate a long utterance at the visible four-second cap. Prefer a short phrase with a brief lead-in.

B accepts 3,200–128,000 bytes, rejects odd lengths, reserves its single clip buffer while uploading/inferencing, and clears audio before making the buffer available again. A portable helper explicitly decodes little-endian samples and zero-pads the final model-sized frame. The existing one-second trailing silence remains; no threshold was lowered and no transcript was substituted.

The code already registered and updated the 16 uppercase command phrases before this repair. The exact pinned headers confirm command_id and prob fields, model frame length in 16-bit samples, and DETECTING/DETECTED/TIMEOUT states. New diagnostics expose initialization error, chunk size, sample peak/clipped/nonzero counts, terminal state, candidate ID/score, frame count and padding. Startup prints the fixed active vocabulary once; each completed job prints one bounded summary. No token or raw PCM is logged. Invalid candidates/states are engine errors, not successful recognitions.

Error codes: 1 insufficient memory; 2 clip allocation; 3 model partition load; 4 English model selection; 5 interface; 6 model/sample rate; 7 command allocation/clear; 8 command add; 9 command update; 10 frame size; 11 malformed detected result; 12 unknown engine state; 13 speech worker creation. Phase 5 means initialization failed. Phase 4 means the job finished; error_code and engine_errors must also be checked.

The text assistant remains a deterministic preview parser. A spoken result remains an untrusted proposal. Select physical bedroom or virtual rooms, prepare the proposal, then confirm its exact target within 15 seconds. Changing selection invalidates the pending browser proposal. The server independently validates the tuple, current speech job, controller boot/freshness, and one-shot confirmation. Physical mode accepts only bedroom light/fan; other rooms require virtual mode. Confirmed requests use the existing authenticated ordered command path and A SafetyKernel.

## Host development

Run `python scripts/browser_bridge.py --host-dev --port 8770`, then open http://127.0.0.1:8770/simulation. This compiles/runs the actual portable routing and kernel with mock pins. It requires the pinned ziglang compiler but no credentials, network, or boards after dependencies are installed. The development token is deliberately public and valid only in the loopback host simulation. It is never injected into a real board connection.

Use the labeled speech fixture control to inject a command ID for testing routing; no uploaded audio is recognized in this mode. PCM uploads return 501. Choose connected, stale, disconnected, rejection, or ACK-timeout scenarios from the persistent host banner. The real bridge and offline preview never silently fall back to this mode. Host events stay HOST_MEASURED/SIMULATED and must not enter hardware accuracy/latency reports.

## Audio dataset workflow

Use `benchmarks/templates/audio_manifest.csv`. Each row names a relative WAV file and expected_id 1–16, or 0 for a negative. Include audio_source SYNTHETIC, HUMAN_RECORDING, or UNKNOWN; speaker, session, conditions, and split development/held_out/diagnostic/unspecified. Missing identities remain empty. Legacy SIMULATED maps explicitly to SYNTHETIC; absent provenance maps to UNKNOWN. Existing synthetic fixtures are diagnostic, not held-out human evaluation.

Validate without boards:

```powershell
python scripts/audio_bench.py --validate-only --dataset benchmarks/data/synthetic_speech
```

After target deployment, run B-only evaluation through the live bridge, using the private local token file:

```powershell
python scripts/audio_bench.py --speech-only --url http://127.0.0.1:8766 --dataset PATH_TO_DATASET --split held_out
```

When no split filter is used, the runner preserves the older extra synthetic silence/noise diagnostics unless `--no-generated-negatives` is passed. Split evaluation never adds these fixtures to a held-out set. Per-clip source metadata is retained. Output JSON/CSV distinguish correct command, wrong command, no detection, negative detection/rejection, engine error, and transport error. Accuracy reports the actual evaluable positive denominator alongside attempted counts and errors; failures are not quietly excluded from the record. Each command and negatives get separate counts. A timed-out control ACK is reported separately from recognition.

No human recordings are supplied. Human accuracy remains pending. Split by speaker/session before tuning, include silence and non-command speech, and retain per-command results. Do not tune on the held-out set. Matching PCM CRCs establish byte transport, not correct acoustic capture or useful recognition.

## Remaining target diagnosis

Use the same independently labeled real recording in the application and a reference pipeline built from the pinned version. Inspect levels and complete command/decoder diagnostics. The sampled synthetic corpus is already correctly formatted at 16 kHz/mono/16-bit; the first three clips are about 2.1–2.2 seconds with peaks around 29,700–31,000, so a claim that all archived inputs were silent or truncated is unsupported.

AFE is still a hypothesis. No speculative API migration, additional model, continuous listening, I2S driver, or WakeNet was added. Check exact v1.7.0 AFE interfaces/assets and measured memory before that experiment. No improvement in target recognition is claimed until fresh hardware results exist.
