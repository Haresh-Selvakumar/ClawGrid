# LIGHT DISABLE evidence review and one conventional-pair proposal

**Later authorized outcome:** the user approved ONLY the4reference clips. LIGHT ON failed its accepted-command gate; the other3were not run under the strict stop. Exact baseline restored/verified; this phrase experiment is closed and the40phone recordings are not authorized. See [actual preflight/rollback report](LIGHT_REFERENCE_PREFLIGHT_20261025.md). The audit/proposal below remains preserved history.

**Decision:** the retained DISABLE fixture is complete by the user's playback confirmation, and MN7's recorded phonemes match Espressif's pinned official tool exactly. No specific fixture truncation, wrong phrase, G2P mismatch or command-ID error is established. Its rejection remains real and unexplained at the acoustic/decoder level. This does **not** prove that the fixture is defective or that either model cannot recognize conventional light commands. Voice remains unverified/unusable under the acceptance gate. One fixed LIGHT ON / LIGHT OFF feasibility proposal is justified; **it is not implemented or run**. No model training, migration, board access, reset, flash, APK change or new recording occurred in this review.

## Verify the actual retained recording

File: `benchmarks/review/voice-short-v1-20261025/LIGHT-DISABLE.wav`. It is the exact shared Zira reference used by both MN6 and MN7, not two independently recorded utterances.

| Check | Actual result |
|---|---|
| User playback | User explicitly confirmed: “Complete intended phrase is audible,” including LIGHT and the ending of DISABLE. |
| Assistant direct listening | Unsupported by this interface; no claim that the assistant independently heard it. |
| WAV/PCM |16kHz, mono, signed16bit LE,27,200samples/1.700s. |
| PCM SHA256 |462f3302696f7833e01dd96b37dff01fd229067310162837d8d0a03cce5b3bcb|
| PCM CRC32 |3370348365, matching both retained ESP jobs. |
| Levels |Peak23,379; RMS2,529.20; zero clipped samples. |
| File boundary |First nonzero0.080s; last nonzero0.9698125s; about0.730s of trailing zero samples. Ten-ms RMS above200 from0.10–0.95s. |

These observations argue against an obviously missing/cropped ending or delivery loss. Nonzero/energy boundaries alone do not identify words or prove pronunciation quality. Human audibility does not guarantee that this TTS rendering matches either recognizer's acoustic representation. The file was not cropped, amplified, resynthesized or replaced during this audit. Full source/ESP fingerprints and the user's confirmation are in `benchmarks/review/voice-phrase-audit-20261025/fixture-audit.json`.

## Verify phonemes and registration against official tools

Ran the **unchanged pinned official** `tool/multinet_g2p.py` from ESP-SR commit `07b0a4d0f6b5de755874d5405150ee0a8eec9865`, Git blob `26b0bff07cd65ae113220d5774e98008b663ac6e`. Dependencies were isolated under the ignored review directory: g2p_en2.1.0, NLTK3.10.4, NumPy2.5.4, pandas2.2.3, with local NLTK dictionary/tagger data. This was a local text-to-phoneme audit, **not speech recognition, custom training or a new product model**. No audio was uploaded or sent to the tool.

| Text | Pinned official tool output, normalized | Retained MN7 registration |
|---|---|---|
|light disable|LiT DgSdBcL|ID2 / LiT DgSdBcL — exact match|
|light enable|LiT fNdBcL|ID1 / LiT fNdBcL — exact match|
|light on|LiT nN|No retained exact-phrase target test found; proposal only|
|light off|LiT eF|No retained exact-phrase target test found; proposal only|

The tool emits a trailing semicolon as its phrase-list delimiter; it is excluded from the phoneme comparison, not a missing phoneme. The pinned MN7 public API accepts graphemes and invokes `flite_g2p(string,1)` internally. Actual logs show the correct ID2/text/phonemes; model-ready/zero-error records and source checks establish that `esp_mn_commands_update()` succeeded. These are public registration checks, not a new exhaustive FST-path audit or proof of acoustic suitability. No unsupported phoneme strings were fed into the grapheme API. [Official pinned converter](https://raw.githubusercontent.com/espressif/esp-sr/07b0a4d0f6b5de755874d5405150ee0a8eec9865/tool/multinet_g2p.py), [pinned registration implementation](https://raw.githubusercontent.com/espressif/esp-sr/07b0a4d0f6b5de755874d5405150ee0a8eec9865/src/esp_mn_speech_commands.c).

Espressif documents both supplied phonemes and internal conversion, with potentially different results; here the compared tools produced the **same DISABLE sequence**, so the tested phrase has no demonstrated external-versus-internal G2P mismatch. The unchanged MN6 configuration uses graphemes rather than this MN7 phoneme representation. [Pinned command documentation](https://raw.githubusercontent.com/espressif/esp-sr/07b0a4d0f6b5de755874d5405150ee0a8eec9865/docs/en/speech_command_recognition/README.rst).

Evidence: `official-g2p-results.json`, `phoneme-comparison.json`, `g2p-run.log`, dependency log and retained MN7 `campaign/startup-records.json`. No ESP job was rerun for this comparison.

## What the evidence distinguishes

- **Confirmed:** the same complete, audible TTS fixture reached both tested gateway-B recognition configurations with matching PCM. MN6/raw and MN7/AFE both returned TIMEOUT198/no accepted candidate/error0 for DISABLE. Confidence/candidate zero is absence of a returned accepted candidate, not an independently measured full-path likelihood.
- **Registration checked:** MN7 ID2, phrase spelling and phonemes match the official tool; public update completed. This does not expose every decoder/FST implementation detail, but supplies no evidence-backed registration repair to make.
- **Not an all-speech failure:** the official reference and ENABLE controls did pass. The historical independent actual-phone job8 for TURN OFF BEDROOM LIGHT also failed despite complete delivery and user-confirmed audibility, so the history is not confined solely to this one DISABLE fixture.
- **Not two independent pronunciation samples:** MN6/MN7 reused the same Zira clip; their frontends differ. Rejection on both could reflect this phrase/rendering's acoustic match, a shared or different frontend/model limitation, or another interaction. There is no alternate-speaker DISABLE comparison, independent rendering comparison or representative dataset to distinguish those causes.
- **No supported generalization:** neither “DISABLE is a broken fixture” nor “the models cannot recognize the intended user's speech at all” is established. The latter cannot follow from a few failures while other references pass. No LIGHT ON/OFF phone result exists to establish whether that pair works.

The prior decisions remain correct at their stated scope: each frozen tested configuration failed its required gate. They are not population accuracy findings or universal model-unsuitability proofs. No further DISABLE repetitions or decoder instrumentation is warranted by this review.

## Check prior exact LIGHT ON / LIGHT OFF tests before proposing more

Searched3,760retained text/source/result files, comparing exact JSON/JSONL/CSV strings and quoted phrase literals case-insensitively. The100,670,524-byte flash-guard log was separately streamed/searched and contained no exact quoted pair. No retained recognition test of either **exact two-word phrase** was found. Full search scope/results are in `exact-phrase-search.json`; this is absence of retained evidence, not proof that an unrecorded experiment never happened.

Existing evidence includes manual HTTP `... light ON/OFF` payloads, host text-parser tests, PC/Vosk fixtures such as **bedroom light on**, and ESP **TURN ON/OFF BEDROOM LIGHT** or ENABLE/DISABLE jobs. These are different inputs/protocol paths. A filename such as `light_on.wav` does not establish that its spoken content was LIGHT ON. Do not relabel any of those as an exact-phrase ESP or phone pass. Reuse existing model/PCM transport/threshold/unit checks and negative fixtures as their original evidence; do not rerun unrelated successful tests or count them as new phrase recognition.

## ONE proposed frozen practical command set — not run

Proposal: **LIGHT ON → Bedroom light ON; LIGHT OFF → Bedroom light OFF.** Nothing else is accepted as a positive synonym. The user explicitly selects Bedroom; other rooms remain unavailable/unverified. Room/controller boot/capture nonce are frozen at push-to-talk start, with sticky cancellation on context change, disconnect, stale state, reboot or expiry. The complete target is shown for explicit confirmation. Existing authorization, SafetyKernel, dwell, proposal expiry, matching ACK plus advancing readback and no old-ON replay remain mandatory. Requested state stays separate from board-reported state and unmeasured physical load state.

Use the **existing restored MN6 model and its frozen raw PCM frontend/0.7 gate**, not another MN7 migration or training. A future reversible app-only grammar/UI change would be required; the installed firmware does not currently register these exact phrases. Android explicit selection and the bounded private screening ledger are also not implemented. No new code/build/flash/test is implied by saving this proposal. New positive reference audio must be checked for the intended words and hashed before target inference; a source-generation problem is a preparation failure, not a reason to search words/voices/rates.

The justification is modest: exact conventional phrases are untested, omit the unnecessary ENABLE/DISABLE lexical choice and keep the interaction practical. This is a **hypothesis worth one finite feasibility screen**, not a demonstrated fix or a promise of recognition.

Frozen proposal file: `experiments/light-on-off-proposal-v1.json`, SHA256 **85488688e7cfbb03d37f3b78c44e2abba2df598bd031ef8af7024fa50f7174aa**, status PROPOSED_NOT_IMPLEMENTED_NOT_RUN.

1. **Reference preflight, maximum4clips:** one exact positive reference per phrase plus the existing fixed silence and non-command fixtures. Require both correct accepted IDs at>=0.7, zero accepted negatives, matching PCM, fresh advancing controller/link telemetry and inhibited outputs. Stop at the first failed prerequisite, missed/wrong positive or accepted negative. No repeated utterance, gain/threshold/frontend change or synonym search.
2. **Actual intended-user phone screen, maximum40clips, only after preflight passes:** retain the previous acceptance yardstick:10ON and10OFF across two predefined sessions, plus20negatives comprising5silence,5background and10fixed sentences/near-misses. Pass>=9/10each positive at unchanged0.7, zero wrong accepted targets/directions and zero accepted negatives. Stop immediately on a wrong/negative acceptance, second miss on either phrase, or transport/engine/health/reboot/output error. No failed clip is re-recorded or replaced. The exact prompts/order are in the proposal, including ON/OFF words alone, negations and near-misses; no post-result selection.
3. **Normal-app check, maximum5more phone clips, only after screening passes:** OFF→ON→OFF, each with correct visible Bedroom/light/action, explicit confirmation, matching ACK and fresh authoritative readback; respect>=3s dwell. Then silence and one fixed negation must produce no actionable proposal. Any failure ends the experiment. Automatic diagnostics retain IDs; the user does not copy them or repeat commands to retrieve them.

Total ceiling **49audio jobs:4reference+40phone screening+5post-pass**. Keep all results, including failures. Restore the exact saved baseline app after a failed candidate; no model/NVS/partition change. If a post-pass error leaves the logical light ON, a single explicitly confirmed manual OFF through SafetyKernel is teardown, never a voice retry or old-ON replay.

**Do not call voice usable before actual phone screening and the confirmed ACK/readback chain pass, including negatives.** Even a pass would establish limited prototype feasibility for this user/context, not production accuracy, general robustness, physical relay switching or other rooms. Outputs stay inhibited and loads disconnected throughout.

## Preservation and next step

This review performed host-only file/signal analysis, pinned G2P conversion and the user's playback check. No board serial connection, reset, firmware/model/partition/NVS write, provisioning change, model training, new migration or new recording. Last hardware-verified baseline remains the restored f30b10cf.../model49b9215a... pair; this review does not claim a new fresh board observation. Both verified private rollback copies and failed experiment receipts are preserved. Android unchanged; no new APK/install or cloud operation.

**Next step is the one proposal above, not more DISABLE debugging.** It has not run. The current voice feature remains blocked; manual native Android control is the retained working base. If this sole conventional-pair screen is subsequently selected and fails its fixed gate, close it without another word/model search and continue the remaining software with voice explicitly unverified.
