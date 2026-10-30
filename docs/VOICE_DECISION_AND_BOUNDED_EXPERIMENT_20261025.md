# Voice decision and one bounded alternative experiment — 25 October 2026

**Latest status:** the user subsequently approved the exact short-v1 experiment. Its mandatory reference preflight **failed for LIGHT DISABLE** at the unchanged0.7 gate; no phone collection or Android room-selection implementation followed. ENABLE reference0.73673 passed and two negative controls were rejected. B baseline restored; stop MN6 short-v1 as predefined. See [actual implementation, results and supported MN7 migration requirements](VOICE_SHORT_V1_PREFLIGHT_20261025.md). The proposal below is preserved historical rationale, not an instruction to restart the failed experiment.

**Decision: no evidence-backed implementation fix is supported. Stop the current recognition-debugging line. Treat the current MN6 English/full-room-command configuration as unsuitable for the intended prototype voice experience until a different, predefined experiment passes.** This is an engineering acceptance decision based on failures with the intended user's speech, not a universal claim about MN6, accents or population accuracy.

The remaining BEDROOM investigation is complete. Exactly two saved clips were replayed once with dense read-only sampling: phone frames64–80 and Zira frames40–56, all17 frames in each window. No new phone recording, threshold change, vocabulary change or actuator command. The temporary sampler was removed and the saved source, firmware.bin, firmware.elf and installed B firmware were restored exactly to the baseline.

## Findings that support the decision

**Confirmed causes/findings**

- The earlier pinned MN6 threshold setter had a probability/log-unit mismatch. The existing hash-guarded repair preserves probability0.7 and fixes reference recognition. It does not solve real-phone recognition.
- Phone8 reaches the correct completed command2 FST path, with the exact registered TURN OFF BEDROOM LIGHT token sequence. Its normalized path score0.2945118 fails the unchanged0.7 gate. This is an actual score rejection, not a successful recognized command.
- The finer trace localizes poor matching during the room word. These are cumulative path scores, not isolated phoneme probabilities:

| Expansion | Phone8 | Successful Zira |
|---|---|---|
| TURN OFF | Frame64:0.914904 | Frame40:0.747910 |
| TURN OFF BE | Frame66:0.632696 | Frame45:0.808196 |
| TURN OFF BEDRO | Frame68:0.377143 | Frame47:0.850806 |
| TURN OFF BEDROOM | Frame69:0.420460; frame80:0.173605 | Frame48:0.866135 |
| Accepted/full completed result | Completed path0.294512, rejected | Accepted0.86229 |

The captured competing paths do not reveal a high-scoring correct alternative or a registration repair. This trace covers the first two stored FST paths and the specified windows; it is not an exhaustive unconstrained acoustic-posterior dump. No further instrumentation loop is proposed.

**Ruled out as sufficient explanations for the retained clip**

- Lost/wrong-format transport: original phone/ESP sample count and CRC match; input is16kHz/mono/signed PCM16LE. The saved clip is complete and the user reports it clear. This does not retrospectively prove the quality of earlier recordings whose PCM was not retained.
- Missing or incorrectly bound command/FST: the correct completed command2 path exists and decodes to the exact intended phrase.
- Merely ending inference early, a stuck previous job, or absence of the tested AFE: proper terminal handling/fresh model/raw/AFE runs still fail, while positive controls work before/after failures. This does not prove that every conceivable frontend configuration is correct.
- Smaller two-command grammar, startup indicator correction, or equivalent BED ROOM spelling as a solution: all were tested and insufficient. Spelling worsened phone score0.2945→0.1683 and broke the synthetic control at0.4924; it was not promoted.
- No observed engine error, watchdog failure, unexpected reboot or output enablement explains these retained inference failures. Captured link/task progress remained healthy; this is bounded observation, not proof of all possible runtime behavior.

**Uncertainties retained**

We cannot attribute the low likelihood specifically to training coverage, pronunciation, room acoustics, phone processing or another acoustic/frontend interaction. We have no representative multi-speaker/noise evaluation and cannot estimate general recognition accuracy. A working code fix must not be invented from the diagnostic text, a lowered threshold or one manipulated recording.

## Recommended next experiment — proposal only, not implemented

Test **explicit room selection in Android plus a frozen short light-command vocabulary** on the same local ESP recognizer. Proposed fixed phrases: **LIGHT ENABLE → light ON** and **LIGHT DISABLE → light OFF**. These are candidate experiment phrases, not a supported fix or a change to the installed baseline.

Rationale/tradeoff: omit the measured weak room word while retaining device/action in speech; avoid replacing the ESP recognizer. Users must select and verify the room on screen instead of speaking it. This is a visible interaction/architecture change requiring review before implementation. It does not establish multi-room routing: initially only the provisioned Bedroom is eligible, with all additional rooms disabled/unverified.

Safety contract: capture selected room/controller boot when PTT starts; reject missing/unsupported room; cancel on context change, disconnect, expiry or reboot. The gateway must bind the recognized job and full room/device/action to the existing proposal. The user sees the complete target and explicitly confirms once. Existing token authorization, fresh readback, SafetyKernel, dwell, no automatic replay, RTOS/watchdog and output inhibition remain intact. No default/inferred room, cloud recognition, Android default recognizer or PC transcript substitution.

**Freeze before collecting data:** two phrases, phrase→ID mapping, mic/frontend settings, acceptance0.7, room binding, negative list and criteria. Do not crop phone8 or tune to it. Use existing synthetic/reference/negative fixtures only for preflight regression; synthetic success is not user-speech success. If either proposed phrase cannot pass that preflight at the existing threshold, stop before requesting phone recordings; do not search synonyms in this experiment.

**One finite primary-user screening batch:**20 fresh positive clips (10per phrase, split across two predefined sessions) and20 negative clips (5silence,5background/non-speech,10fixed non-command/near-miss sentences). Preserve each privately with source/ESP count+CRC, score, target and result. Evaluate exactly once; no retry of a failed utterance or adjustment after inspecting results. Stage1 is recognition-only and cannot operate a load.

**Pass/fail gate, selected before the batch:**

- At least9/10 correct accepted recognitions for each phrase at confidence>=0.7, with zero accepted wrong directions/targets.
- Zero accepted command detections on all20negatives; no uncommanded proposal.
- No PCM mismatch, unexpected reboot/output enablement or task/watchdog regression. Transport/engine failure stops the run as inconclusive; it does not count as recognition success.
- Stop immediately on any accepted wrong direction/target or negative false acceptance, or when a second miss on either phrase makes9/10 impossible. Otherwise stop at40clips. **No additional recordings, threshold changes, new phrases or configuration search within this experiment.** Failure closes MN6/short-command feasibility for this prototype configuration.

Only if that gate passes, perform one bounded normal-app check: OFF, ON, return OFF, with correct visible target, explicit confirmation, matching ACK and fresh board readback for each; respect>=3s dwell. Add one silence and one non-command app check that must create no actionable proposal. Stop on the first error; never replay an old ON. Outputs remain inhibited and loads disconnected. Thus the entire experiment is capped at40recognition-screen clips plus5post-pass clips, not an ongoing retry loop. Raw counts demonstrate limited feasibility only, not production accuracy or physical switching.

If it fails, retain manual app control and move the model decision to a separate reviewed experiment; do not resume the current full-phrase debugging loop.

## Supported model alternative and tradeoffs

The pinned ESP-SR1.7.0 sources offer **mn7_en for ESP32-S3** (`CONFIG_SR_MN_EN_MULTINET7_QUANT`, model packer branch). Its English command representation uses phonemes, whereas MN6 uses graphemes. [Pinned command documentation](https://raw.githubusercontent.com/espressif/esp-sr/07b0a4d0f6b5de755874d5405150ee0a8eec9865/docs/en/speech_command_recognition/README.rst), [pinned Kconfig](https://github.com/espressif/esp-sr/blob/07b0a4d0f6b5de755874d5405150ee0a8eec9865/Kconfig.projbuild).

This is a supported candidate, not proof it will recognize this user's speech or fit the current runtime budget. It requires separate model assets, phoneme/registration verification, measured RAM/CPU/latency, and a reversible deployment plan preserving the current model pack. The MN6-specific private-layout threshold repair must **not** be copied to MN7. Verify that model's threshold semantics independently while preserving the same acceptance requirement. Do not overwrite existing models, weaken the blank-model flash guard or silently swap the installed model. Short-command testing is the recommended first experiment because it avoids a model/partition deployment change; neither alternative is started by this decision.

## Bounded investigation receipt and preserved baseline

Evidence: `benchmarks/review/speech-decision-20261025/{bedroom-token-analysis.json,phone8-dense.json,zira-dense.json,run-status.json,preservation-final.json}`. One temporary source-line sampler change only; build **SUCCESS47.989s**. The two known saved clips respectively returned no detection and accepted control0.86229; all17dense frames per window captured. These were diagnostic replays, not new phone trials or an accuracy evaluation. Existing10portable/49Android tests were not unnecessarily rerun for a temporary sampler that was restored.

Guarded normal inhibited app-only dense deployment manifestT063337Z and baseline restorationT063431Z both exit0, fresh B MAC matched. Baseline source hash and image/ELF hashes match saved originals. Final installed B remains **f30b10cfb74a4dd79ff28588de2d3be9f6c760c934b48cf1475bf6f7c271b9a4**, A unchanged/boot16. No model/partition/NVS/credential/database writes; private inputs and all62pinned component hashes match.

Restored readback: authenticated advancing controller sample3,823,997ms/age78ms, lightOFF/fanOFF/last_command0, physical_outputs_enabled=false. Gateway AP192.168.4.1/channel1, watchdog active/zero sampled deadline misses, speech ready/job0/error0, no phone associated. Serial connections closed; laptop Wi-Fi restored and owned temporary profile/XML removed. No APK or baseline architecture change. Last independently verified phone build remains1.0-speech-capture-20261024b/SHA5c932d62..., not newly verified on-phone in this run. No live voice confirmation chain or physical switching success is claimed.
