# Phone transcript preview and parser audit — 25 October 2026 IST

**Implemented, built, installed and partly phone-verified.** The remaining problem is inconsistent recognition, not a demonstrated parser-normalization defect. User reports reading substituted words in the preview and making several attempts until commands were accepted. One user-correlated voice ON → explicit Confirm → gateway ACK3/fresh ON report was observed; final cleanup OFF4 has matching ACK/fresh OFF. Outputs stayed inhibited. This was informal repeated use, **not** the requested controlled one-recording test or an accuracy result.

No actual transcript is copied into this report, logs, cloud synchronization or an evidence file. The user voluntarily described recognition errors in chat; those words are not reproduced here. Instrumented raw/normalized comparison did not capture a visible preview in time, so there is no independently retained verbatim comparison. No new recording is requested.

## Configuration audit before speech

Pinned Vosk Android0.3.75 uses `Recognizer(model,16000f)`, **unrestricted default model graph**, `setWords(true)`, every bounded PCM chunk and all final segments plus final flush. No custom grammar, grammar reset, phrase aliases or alternate recognizer is configured. There is no separate recognizer phrase list that drifted from the parser. The bundled small-en-us0.15 model/archive is unchanged (SHA30f26242c4eb449f948e42cb302dd7a686cb29a3423a8367f99ff41780942498). Final confidence gate remains0.7 per word, with no acoustic/threshold/model tuning.

The UI now derives its six phrases directly from `PhoneVoicePolicy.allowedPhrases` for explicit Bedroom/light: light on; light off; turn on the light; turn off the light; turn on bedroom light; turn off bedroom light. Other room/device combinations return no phrases. Tests independently assert all six mappings, full-utterance rejection, unchanged threshold, exact text/normalization and all-segment handling.

The documented public C API `vosk_model_find_word` is exported by the pinned AAR (symbol presence checked for4ABIs) and was called on the **actual phone's loaded model**, not via host recognition. All six unique words returned positive model IDs:

| Configured word | Phone model word ID |
|---|---:|
| bedroom |101349|
| light |4326|
| off |107349|
| on |106591|
| the |152188|
| turn |58832|

The phone displayed all6phrases and this table before recording. This establishes vocabulary support and parser acceptance, **not** guaranteed acoustic recognition of every phrase. No out-of-vocabulary/stale-list configuration mismatch was found, so no speculative grammar, alias or threshold repair was made. Official primary references: [Vosk recognizer API](https://github.com/alphacep/vosk-api/blob/master/java/lib/src/main/java/org/vosk/Recognizer.java), [public vocabulary API](https://github.com/alphacep/vosk-api/blob/master/src/vosk_api.h).

## Changes and privacy boundary

- `PhoneVoicePolicy.kt`: redacted `PhoneTranscriptPreview` object carries exact final-text segments, actual normalized parser input, reason enum and specific static rejection detail. Full-text matching remains unchanged. Raw text is not placed in metadata or `toString`; invalid results explain format/confidence/word-text disagreement separately.
- `OfflinePhoneRecognizer.kt`: public vocabulary check for configured words during model preparation; original model/decoder/frontend retained. No host, cloud or ESP audio recognition path.
- `ClawGridViewModel.kt`: foreground StateFlow preview only, no SavedStateHandle/Room/preferences/serialization. Clear on Dismiss, new recording, room/tab/mode/network/background/context changes, stale state/reboot or60second expiry. Dismissing also cancels an unconfirmed phone proposal; confirmed requests retain normal ACK/readback handling. PCM is still wiped after use. Metadata remains separate from transcript.
- `PhonePushToTalk.kt`: exact final words (every segment), normalized text, shared allowed list, specific parser result/reason and Dismiss button in the existing PTT panel. Screens/navigation/product package retained.
- `MainActivity.kt`: callback wiring and `FLAG_SECURE` for this activity, protecting normal screenshot/recording/recents pixel capture. This changes capture availability, not screen layout. [Official Android flag documentation](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#FLAG_SECURE).
- `app/build.gradle.kts`: debug suffix1.0-phone-preview-20261025c; SDK/dependencies/debug identity/release signing unchanged.
- `PhoneOfflineVoiceTest.kt`: additional exact-preview, canonical-map, normalization, threshold, multi-segment and redacted-state checks. Synthetic fixtures are not phone recordings or new aliases.
- `scripts/phone_ui_memory.py`, `inspect_phone_preview_memory.py`, updated `phone_offline_snapshot.py`: UI inspection through `/proc/self/fd/1` stdout pipe only; **no temporary XML file or screenshot**. Exact/normalized values remain local in process memory; output contains only comparison booleans/token-position metadata, public configured vocabulary IDs and structured command results. No disk fallback on failure. Three synthetic privacy/pipe tests verify this boundary.
- Build/observer scripts accept fresh evidence/snapshot paths; old artifacts and identities were not overwritten. Firmware files were not edited, built or flashed.

## Exact checks and actual results

Evidence directory: `benchmarks/review/phone-preview-20261025/`. Prechange Android source/master/handoff/scripts preserved under before/. No audio/transcript/UI dump files were collected.

| Check | Actual result |
|---|---|
| Fresh CH343 enumeration, esptool ROM read-mac against provisioning |PASS A COM10/e0:72:a1:f5:bc:b0/adapter5C37271524; B COM9/e0:72:a1:f6:a9:68/adapter5C37271527. Normal identity resets advanced A boot20→21. No firmware/NVS erase/key/model/partition write.|
| `scripts/check_android_control_jvm.py` |**66PASS,1.431s JUnit**, actual Android control/parser source. Not a speech-accuracy test.|
| `pytest tests/unit/test_phone_preview_privacy.py -q` |**3PASS,0.65s**, synthetic RAM comparison/pipe/no-fallback tests.|
| `build_phone_outside_onedrive.py --name preview-20261025c --evidence benchmarks/review/phone-preview-20261025 --build-folder phone-preview-build-20261025 --apk-name ClawGrid-Phone-Preview-debug-20261025c.apk --gradle-cache C:/Users/hares/Documents/ClawGrid-Android-build-20261025b/gradle-user` |**SUCCESS2m12s**,25tasks executed/14from cache;83Android source files byte-identical in fresh outside-OneDrive snapshot. Existing debug key copied, release config unchanged. No repeated firmware build.|
| `check_phone_offline_apk.py`, `check_android_client_boundary.py`, apksigner verify, zipalign-c-P16-v4 |PASS14model hashes, native4ABIs/64bit16KBELF,16KBZIP alignment, same71ce1bf6... debug certificate/v2 signature; configured private matches0/privileged fields0.|
| adb install-r, pull installed base, SHA comparison |**SUCCESS/MATCH**1.0-phone-preview-20261025c/code1,100,112,968B, SHA29c295701e6ba7235f4265b960f45602915968da2e7efafa27185f6cbb876a87. Previous buildb preserved.|
| Phone model vocabulary/display audit |PASS six native word IDs above; all6phrases displayed. Host canonical parsing PASS. Actual acoustic success for all6phrases not tested.|
| Phone private gateway/manual proof |User reapplied memory token privately; manual OFF1 on new boot21 matched appliedACK/status0 and fresh OFF report. AP192.168.4.1/channel1; outputsfalse.|
| Requested one diagnostic |User instead reported multiple informal ON/OFF attempts, substitutions and intermittent success. Count/prompts not controlled. Exact raw/normalized comparison was not captured while visible; metadata-only inspector saw ACCEPTED capture2822226236/boot21/2words/displayed minimum confidence1.000. No accuracy/formal acceptance claim.|
| Informal voice ON/confirmation |User explicitly confirms ON came from voice and they tapped Confirm. Gateway ACK3/status0/lighttrue/oneattempt/RTT17,696us/processing34us; fresh boot21 report sample1,328,000ms/age769ms/lastcommand3/lighttrue/outputsfalse. This is user-correlated provenance, not a retained nonce→request trace. Command2 was not instrumented individually.|
| Cleanup OFF and final state |OFF4 appliedACK/status0/oneattempt/RTT19,047us/processing35us; final boot21 sample1,621,000ms/age459ms/lastcommand4/lightfalse/fanfalse/outputsfalse. LinkTask324,697jobs/watchdogtrue/0sampled misses/channel1/1APclient. **Physical load not measured.**|
| Privacy/source closure |Mobile data setting0/APClawGrid observed again; no transcript saved/emitted by inspector. Controller/gateway core source hashes match prior seal; private credentials/header/partition hashes unchanged. Serial observers close after bounded reads. No ESP build/flash, model change, cloud/database mutation or publication.|

## Decision and next executable step

The user's visible-word substitutions are **recognition errors**. Rejecting a complete unlisted phrase is the intended parser behavior, not evidence of a parser defect. Correct recognition can enter the existing proposal/explicit confirmation/ACK/readback chain; one informal ON chain is observed. No raw-to-normalized corruption or stale vocabulary/grammar list was demonstrated. Confidence1.000 is rounded metadata and does not prove intended-word accuracy.

Keep the strict parser,0.7gate, existing baseline/models and safety flow. Do not add near-sounding error forms as actuator aliases. Recognition consistency, controlled ON/OFF, silence/non-command/negation rejection and robustness are still unverified; physical switching remains untested.

**Next narrow source review**, if continued: evaluate whether a room/device-specific Vosk grammar generated from the same canonical list plus explicit unknown/non-command handling is appropriate. Preserve unrestricted decoding as rollback and define rejection/negative criteria before enabling such a change. This is a proposed recognition-configuration review, not an implemented fix or authorization for another recording campaign/model migration. No further recording is queued here. Current preview build remains installed, logical light OFF and outputs inhibited.
