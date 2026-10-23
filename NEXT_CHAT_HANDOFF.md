# Current handoff: inhibited manual PASS; phone recognition FAIL; ID-display APK built — 23 October 2026 IST

Publication follow-up: user requested push to the existing repo and a Downloads copy. Existing repo is `https://github.com/Haresh-Selvakumar/ClawGrid`, default main, baseline79ea76f0dc030477f138927b9a20faca5b79a3d1. A checked source snapshot is prepared in Downloads `ClawGrid-integration-2026-10-23` with history retained. See `DISTRIBUTION.md` and the completed local receipt for actual commit/ZIP/APK hashes. Private files/keys/backups and generated caches are excluded; original workspace remains intact. Source credential scan passed. Selected original evidence is preserved verbatim under `benchmarks/raw/phone_integration_20261023/`; README/.gitignore updated for current native app and private-file exclusions. No new hardware tests, output changes, phone install or recognition success is implied.

Read the new top section of `CLAWGRID_MASTER_INTEGRATION.md` first. Older sections below are preserved history, including questions and captures that have now completed. No serial observer is still running. Do not spawn agents unless explicitly requested. The user retains native Android; older browser-only AGENTS rule is superseded.

**Current independent board state:** A boot9, fresh authenticated sample11,068,999ms/age282ms, light/fan OFF, last_command12, physical_outputs_enabled=false. B transaction12 phase2/status0/outcome_known/attempts1/RTT16,724us is OFF. User's historical OFF11/ON12 screenshot IDs cannot be correlated to captures; never represent them as proof of a later OFF. Do not repeatedly ask the user for IDs or repeated commands. No new OFF is necessary now.

**Pair unchanged:** A COM10 MACe0:72:a1:f5:bc:b0 image8f028bd90f65a55a72b09f4b886db7a3fa851f8a2108c6b79659136db8713cdf; B COM9 MACe0:72:a1:f6:a9:68 imagea225740aa6af9445f9959994141dfc5723944c7f59daa957d039638cafbaf2b2. Prior ROM/guard evidence preserved; no reset/flash in this follow-up. Both normal inhibited/UART0/ESP-NOWchannel1; AP192.168.4.1; watchdog/task progress pass. Relays/loads remain user-reported disconnected, physical switching untested. Supabase CONNECTING does not block local tests. Credentials stay private, unchanged; models/NVS/partitions/security/thresholds/policy unchanged.

**Real phone voice result:** app manual gate unlocked without enabling outputs. User performed two recognition-only trials prompted TURN OFF BEDROOM LIGHT; both reported voice stopped/no confident ESP recognition/nothing proposed/no automatic retry/confirmation. Actual ESP jobs1/2 completed phase4/error0/engine_errors0 but command0/confidence0/candidate0/empty decoder text/DETECTING state. Job1:33,280 samples/2.08s/peak15,349/CRC3561804158/97frames/inference1,438,407us. Job2:full64,000 samples/4s/peak15,392/CRC1757513135/157frames/inference2,382,320us. Both unclipped, chunk512/padded16,384; full-length timing check did not resolve failure. Real capture/upload/ESP processing observed; recognition FAILED, speech proposal/confirmation/control NOT_EXERCISED. No raw phone PCM retained or independent CRC/transcription; no ADB logs. Do not invent microphone quality, an engine error, a successful recognition, or a passed accuracy evaluation. A boot9/progress/output inhibition persisted; final state remains OFF12.

Both passive captures completed with no error and released COM9/COM10 before diagnostics. Latest exact read-only check: firmware Python `benchmarks/review/physical-phone-20261023/phone_probe.py --gateway COM9 --seconds 4 --name voice2-diagnostics` (exit0/error=null). Raw aggregates `benchmarks/raw/android_phone_voice_trial1_20261023.json`, `android_phone_voice_trial2_20261023.json`, detailed captures/probes under physical-phone review. No host audio/control POST in these observers.

**User-requested ID visibility implemented:** new `ui/components/HardwareRequestStatus.kt`, integrated below hardware banner in `MainActivity.kt` across hardware tabs; Bedroom request text in `RoomsAndRelaysScreen.kt` includes ID. Prominent request ID/phase/desired direction, separate proposal ID and fresh board last-command/light/boot/output state. No semantics/navigation change. Before-source snapshots and installed test APK preserved. HOST_MEASURED: Gradle debug build SUCCESS1m35s;42 control/voice JVM tests +3 wire tests pass; apksigner v2/one existing signer PASS; configured-private-value APK boundary scan PASS. Existing local debug key and private-input hashes unchanged; release signing untouched. Full Robolectric remains blocked by uncached nativeruntime-dist-compat1.0.18, not rerun.

New APK **not installed or on-phone layout-tested**: `C:\Users\hares\OneDrive\Desktop\ESD-imp\ClawGrid-physical-integration-prep\ecosystem\android\app\build\outputs\apk\debug\app-debug.apk`,18,570,202B,SHA256a44b9cc4dc3571c71b334b95049d4ab2221af10f1da8b04f6f1b13d1a4366119. All physical phone trials used prior installed APKd12778de...; don't attribute them to new UI. Existing signer71ce1bf6... unchanged. Source/build metadata `benchmarks/raw/android_request_id_build_20261023.json`; exact logs in master. Phone installation/voice docs updated. Process restart/update clears in-memory token/manual gate; don't force an install/repeated manual command merely for historical IDs.

**Next executable step:** investigate current speech runtime against pinned ESP-SR1.7.0 reference PCM/model/decoder pipeline with existing labelled diagnostic clips and a bounded recognition-only harness; isolate a defect before any normal inhibited firmware change. No more identical phone utterances without a new diagnostic reason. Threshold0.7 and explicit validation/confirmation remain; AFE only a hypothesis. No blind flash, physical output enabling, credential regeneration, raw audio retention or successful transcript injection. Recognition blocker prevents real voice-confirmation test. Manual-until-AUTO/five-minute hold mismatch and temperature transport remain separate unresolved requirements. New request-ID APK is ready for user-controlled installation, not yet phone-verified.

Firmware Python: `C:\Users\hares\Documents\Codex\2026-10-21\i-h\work\clawgrid-env\Scripts\python.exe`. Current workspace `C:\Users\hares\OneDrive\Desktop\ESD-imp\ClawGrid-physical-integration-prep` (not Git). Preserve previous backups and evidence.

---

# Historical: App manual ON/OFF milestone PASS; voice recognition-only stage — 23 October 2026 IST

**Native Android → B → authenticated ESP-NOW → A → matching ACK + fresh readback passed for both manual directions, with outputs inhibited.** User confirms OFF#5 and ON#6 REPORTED; independent gateway pairs match phase2/status0/outcome_known/attempts1, same A boot9 and output inhibition. OFF#5: sample9,900,999ms/age91ms/RTT19,665us/lightfalse. ON#6: sample9,908,999ms/age341ms/RTT18,311us/lighttrue. Earlier app-owned OFF#4 also passed. These are radio/software logical results, not physical switching.

Additional observed commands #7 OFF and #8 ON have matching ACK/readback; #9 ACK/actuation reported ON. Do not silently describe the earlier OFF as the final state. User's subsequent OFF reply had a placeholder ID, so actual state was queried independently: **latest command#12 is OFF**, successful ACK/status0/outcome_known/attempts1/RTT16,724us/processing37us, matched fresh **boot9 sample10,300,999ms, age94ms, last_command12/lightfalse/physical_outputs_enabled=false**. IDs10/11 were not captured in the bounded gap; no invented reconstruction. Agent sent no command POST in these phone stages. Explicit final OFF is now established in board evidence. User reports loads disconnected throughout.

Current firmware remains normal inhibited A COM10/base MAC e0:72:a1:f5:bc:b0/image8f028bd..., B COM9/base MAC e0:72:a1:f6:a9:68/image**a225740aa6af9445f9959994141dfc5723944c7f59daa957d039638cafbaf2b2**. A boot9 unchanged. B AP192.168.4.1/channel1, task/watchdog health and speech ready/zero errors preserved. No NVS/model/credential/security/threshold/policy changes in these tests. The Supabase CONNECTING label does not participate in local control and is not a blocker.

## Current staged voice test

The app-owned inhibited REPORTED result satisfies the manual-verification condition without enabling physical GPIOs. Actual visible voice readiness and microphone permission are now requested: stop/report if “First verify one manual light command” remains. Otherwise grant microphone While using app, close the Chrome gateway tab, remain on ClawGrid AP/app foreground, hold push-to-talk and say **TURN OFF BEDROOM LIGHT** once within4s, release. This **first clip is recognition-only: CANCEL any speech proposal, do not Confirm**. User should report permission and exact voice message/job/error. Keep recognition result separate from any later confirmation/command result; no cloud speech, fixture audio or threshold reduction.

A passive capture is active: firmware Python `benchmarks/review/physical-phone-20261023/passive_phone_capture.py --name phone-voice-recognition-1 --event speech_job --min-id 1 --seconds 240`, exec session58485 at dispatch. It occupies COM10/COM9 until the first speech_job completes or timeout; **poll/wait it before opening another serial client**. It does not issue HTTP/control/audio requests; A q health queries and passive A/B task/link/speech logs permit observation without competing with phone audio for B's two HTTP sockets. It saves `phone-voice-recognition-1.json`, `-uart.log` and stdout log. Then read `/api/speech`, `/api/state`, `/api/rtos` once over the normal bridge to capture actual job, samples/CRC/peak/confidence/command/errors and continuing authenticated fresh inhibited telemetry. No recognition/capture pass is claimed yet. Permission is user-observed unless a real upload corroborates it.

If recognition succeeds for the intended target at unchanged0.7 threshold with valid job/CRC, a **fresh separate** spoken clip can test user-reviewed confirmation and matching ACK/readback; first cancelled proposal must not be replayed. If recognition or transport fails, record the actual engine/PCM/error evidence and keep outputs inhibited. No new firmware/APK change is needed merely to unlock voice. Keep app settings/process unchanged; applying connection settings or restarting process resets manualVerifiedBoot and requires a new app manual proof.

Evidence: aggregate `benchmarks/raw/android_phone_manual_20261023.json`; `phone-manual-5-to-8-verified.json`, `phone-OFF-4-verified.json`, `phone-return-off.json` (ACK#9 ON), and `return-off-current.json/log` (final OFF#12) under physical-phone review. Phone guide/root-page fixes and original verified evidence remain below. Remaining: actual first microphone/recognition result, subsequent voice confirmation path, physical switching (out of scope/inhibited), persistent manual-until-AUTO mismatch and temperature transport.

---

# Phone manual OFF #4 verified — 23 October 2026 IST

**First app-owned inhibited command PASS.** User reviewed/confirmed Bedroom/light/OFF in native Android and reports “Matching ACK + fresh board report #4. Physical load not measured”, Requested Light OFF · REPORTED, outputs inhibited and board-reported OFF. Gateway capture independently matches transaction **id4/phase2/status0/outcome_known=true/attempts1**, RTT16,875 us/processing28 us; same-boot9 advancing report sample **9,552,999 ms**, age90 ms, **last_command4/light=false/physical_outputs_enabled=false**. This is logical inhibited actuation only; relays/loads remain user-reported disconnected and physical switching untested.

The app-owned manual gate can be satisfied by inhibited ACK/readback without enabling outputs. Its actual visible Hold-to-talk readiness/permission remains to be checked after manual ON/OFF; no microphone/upload/recognition trial has occurred. Keep the app process/settings and current boot; applying connection settings again resets manualVerifiedBoot and requires another app-owned manual pass. Supabase CONNECTING remains optional and is not involved in direct control.

Next staged test: user verifies Outputs inhibited, taps Bedroom Ceiling Light **ON**, inspects Bedroom/light/ON proposal and Confirms once, waits for REPORTED/ACK+readback and replies with id/state. Stop on rejection/timeout; do not replay. Read-only `phone_probe.py --gateway COM9 --seconds 240 --name phone-manual-on-off` is capturing; no agent control POST is sent. After matched ON, wait at least3s and guide a new explicit OFF to leave logical output OFF. Then check app voice gate and grant permission/record real PCM for ESP recognition with threshold0.7 and explicit proposal confirmation intact. No further firmware or APK changes needed for this staged test.

Evidence: `benchmarks/review/physical-phone-20261023/phone-OFF-4-verified.json`, complete `phone-manual-off.json/log`, running ON/OFF observer and aggregate `benchmarks/raw/android_phone_manual_20261023.json`. App UNKNOWN was resolved by applying the existing token and retaining AP connection; current app/browser reachability is confirmed. Earlier records are historical below.

---

# Native app now fresh; inhibited manual OFF test staged — 23 October 2026 IST

The user applied the existing API token and kept the phone on ClawGrid-Assistant. Android now reports **Fresh board report; physical load not measured**, Fresh report health, Loads0/2/Relays Guarded, occupancy UNKNOWN and temperature/lux unavailable. This resolves the prior app UNKNOWN blocker at this stage; no Android source/APK change was needed. Prior token/AP-retention contributions were not isolated individually. Supabase CONNECTING is an optional display status and is not used by the direct gateway control path; it must not block local tests.

Fresh read-only preflight (`phone_probe.py --gateway COM9 --seconds 5 --name app-fresh-preflight`, exit0) verifies A **boot9**, sample **9,476,999 → 9,481,999 ms**, maximum age339 ms, all known/authenticated and **physical_outputs_enabled=false**, light/fan logically OFF, B AP clients1/channel1, watchdog active and ESP speech ready. Occupancy UNKNOWN is distinct from unavailable board telemetry; do not fabricate sensor state. No outputs were enabled.

Started read-only `phone_probe.py --gateway COM9 --seconds 180 --name phone-manual-off`. User is instructed to request **Bedroom Ceiling Light OFF**, inspect proposal Bedroom/light/OFF, Confirm once, and wait for matching ACK plus fresh report/REPORTED. Baseline controller last_command=3 and B transaction idle after its page-fix restart. No agent POST/control was sent in this step. Await phone request ID/status and corresponding gateway ACK/readback before claiming an app command pass or voice unlock. Keep all loads disconnected. Next after verified manual command: validate app manual gate and real mic capture/PCM upload/ESP recognition at unchanged threshold0.7. No physical switching claim and no cloud dependency.

Evidence: `benchmarks/raw/android_phone_manual_20261023.json`, detailed preflight/observer under `benchmarks/review/physical-phone-20261023/`. Earlier records remain historical below.

---

# Native app connection follow-up — 23 October 2026 IST

During the read-only `phone-manual-session` observer, B's AP client count changed **1 → 0**, while A remained fresh/inhibited and B speech stayed ready/job0. The laptop had already returned to home Wi-Fi. Thus the phone was no longer associated at the end of this capture; this is evidence to investigate no-Internet AP retention, **not proof of an Android parser/firmware defect or a demonstrated root cause at the exact earlier app failure time**. Asked user to reconnect, choose Stay connected/Use without Internet, disable automatic switching to better Wi-Fi/mobile data temporarily, and keep native app foreground20s. A fresh `phone-app-connection` read-only observer is running. Exact app status/HTTP scheme/API-token-field confirmation remains requested; values stay private. No further flash or speculative Android change was made, and no app control/audio result exists yet.

---

# Phone browser fixed; native app UNKNOWN — 23 October 2026 IST

User confirms corrected gateway root shows **fresh hardware telemetry / outputs INHIBITED** on phone. Chrome had upgraded HTTP to HTTPS; guided local HTTP Continue-to-site/settings per official Chrome docs. Legacy root had virtual_rooms/generated demo metrics; replaced root only with read-only hardware status and no-store, kept explicit /simulation and native Android UI. Normal inhibited B app-only update passed guard/hash verification, current B **a225740aa6af9445f9959994141dfc5723944c7f59daa957d039638cafbaf2b2**, COM9 e0:72:a1:f6:a9:68; A remains COM10/base MAC ending bc:b0/image8f028b..., boot9. Models/NVS/credentials unchanged; no threshold/security/policy change. Host JS freshness/reconnect/reboot tests and frontend checks pass; B build53.270s; actual AP root byte-match5864B/no-store + APIs HTTP200; healthy speech ready. Full exact evidence in current master / android_gateway_root_fix_20261023.json.

**Current blocker:** native Android app reports UNKNOWN/controls unavailable. Exact Bedroom status, applied http:// URL (not https://) and API-token-versus-Wi-Fi-password selection requested; never ask values. ADB devices list empty. Read-only phone-manual-session observer started; no app-owned manual or phone audio result yet. Inspect actual settings/transport before changing Android or firmware blindly. After app fresh/inhibited, user manual light OFF → proposal/Confirm → matching ACK+fresh readback, verify app voice gate, then real microphone PCM/recognition at threshold0.7. Keep relays disconnected and outputs inhibited; never enable outputs to unlock voice. Root-page success does not set app manualVerifiedBoot. Prior records below are historical and retained.

---

# Wi-Fi HTTP diagnosis — 23 October 2026 IST

Laptop joined ClawGrid-Assistant with existing credentials and received DHCP **192.168.4.3/24**, gateway/server192.168.4.1. Actual TCP80 and GET `/` (155,404 bytes), `/api/state`, `/api/rtos` all pass HTTP200 over Wi-Fi. B AP BSSID e0:72:a1:f6:a9:69/channel1; two clients measured while laptop and the other client associated. Original laptop Wi-Fi restored; temporary profile/private XML removed. No flash/reset/provisioning/output changes. Fresh A/B self-tests pass; A boot9 reports advance/inhibited, B watchdog active and speech supported/ready/errors0. No reboot/panic in bounded capture; historical gaps remain unknown.

User phone browser still times out. Phone DHCP address/gateway/subnet and VPN status are requested, not yet received. One/two client counts support association, not proof of phone DHCP lease. Do not claim a phone fix or continue control/voice until a direct phone `http://192.168.4.1/api/state` GET works. No gateway reflash is warranted by actual laptop results. Read the new master section and `benchmarks/raw/android_gateway_reachability_20261023.json`; detailed logs/helper under physical-phone review. Preserve secrets and outputs inhibited.

---

# Credential locator verified — 23 October 2026 IST

User could not find the Wi-Fi password. Existing root `local_credentials.json`: **ap_password (line 3)** for ClawGrid-Assistant Wi-Fi; **api_token (line 4)** for the Android gateway settings. User explicitly requested opening it locally, and editor open at line 3 is queued. No values printed or credentials changed. SSID/password/token match private header + current B image; image matches successful final UART flash manifest. Fresh running B authenticates the existing token and confirms healthy inhibited state, AP 192.168.4.1/channel1. Evidence: `benchmarks/review/physical-phone-20261023/credentials-verification.json`. Next: user joins AP with existing password; then resume phone-owned manual OFF/ACK/readback and gated real microphone test. No reflash or provisioning change required.

---

# Physical pair deployed; phone interaction pending — 23 October 2026 IST

Read the current master first. Both current CH343/UART0 connections are freshly ROM-MAC verified: **A COM10 e0:72:a1:f5:bc:b0**, **B COM9 e0:72:a1:f6:a9:68**. Do not infer role from an old COM number. Normal inhibited/fault-default A `native_voice_uart` and B `speech_intelligence_uart` were app-only flashed and verified. Current hashes, guarded flash manifests, exact commands/results and full private backup locations are in [the master](CLAWGRID_MASTER_INTEGRATION.md). NVS/partition/credentials were not erased or regenerated. B's original model region was entirely blank; guarded fresh table/whole-region reads permitted installing pinned ESP-SR assets there. Final B UART deployment preserved those models.

Both runtime self-tests pass, actual A/B channels=1, measured B AP=**192.168.4.1**, advancing authenticated A boot=9 reports, watchdog/task progress and inhibited outputs verified. ESP-SR now supported=true/phase=1/errors=0. **Real firmware API over USB HTTP tunnel passed manual OFF #1 → ON #2 → OFF #3, each proposal/confirmation/matching ACK plus advancing readback. This is inhibited logical actuation, not physical switching.** Host tests: 76 firmware/watchdog, final 41 flash/UART guards (overlapping suites). No thresholds or persistent policy semantics changed.

User reports APK installed. Phone joining/configuration question is pending; 90 s observer saw AP clients=0 and speech job=0. Phone association/app-command/manual voice-unlock/mic capture/upload/recognition remain untested. The inhibited API result can satisfy the app's gate, but **only the app's own current-boot manual request reaching REPORTED sets manualVerifiedBoot**. Host commands do not unlock it. Never enable outputs to unlock voice.

Next: use the existing normal pair; no new flash/cable change is necessary. Run the read-only `benchmarks/review/physical-phone-20261023/phone_probe.py --gateway COM9 --seconds 90 --name phone-session-2` with the firmware Python identified in the master. User joins ClawGrid-Assistant without Internet, Hardware settings URL http://192.168.4.1 and existing token privately. Observe AP clients and fresh inhibited state, then user does app manual OFF → Confirm → ACK/readback. Only then grant mic, hold/release real phone speech, record actual job/confidence/errors/result, retain 0.7 threshold and explicit command confirmation. No synthetic-input substitute or old ON replay. Keep relays/loads disconnected. Saved HOME/light-auto/fan-manual/grace120000 and five-minute manual-hold mismatch remain unchanged.

Evidence: `benchmarks/raw/android_physical_20261023.json`; detailed logs/helper under `benchmarks/review/physical-phone-20261023/`. Earlier Android/firmware records below remain historical and verbatim.

---

# Phone install / local ESP push-to-talk — 23 October 2026 IST

The user chose the phone microphone and direct ClawGrid AP **http://192.168.4.1**; URL remains editable. Actual raw AudioRecord PCM submission and ESP job-bound speech proposals are implemented, with explicit confirmation and ACK/readback preserved. Voice unlocks only after current-boot manual light ACK + advancing readback. Wi-Fi sockets are used without Internet/Supabase/cellular fallback. Source AP/A/peer channel is 1 throughout; no fresh RF test.

Actual Gradle wrapper/checksums and existing debug signing verified; release signing preserved. Debug APK builds (46 s), signature verifies, 42 control/voice cases + 3 wire cases + 32 host audio/proposal regressions pass. No phone installation, mic/recognition/board test, flash/NVS/key/database change. A read-only gateway probe timed out while laptop was on home Wi-Fi. Use [the phone install/local voice guide](ecosystem/android/PHONE_INSTALL_AND_LOCAL_VOICE.md) and [current master](CLAWGRID_MASTER_INTEGRATION.md) for exact paths/checks/limits and staged next step. Earlier records below remain historical.

---

# Current Android integration update — 23 October 2026 IST

Implemented native app M1/one-room manual light ON/OFF with explicit proposal confirmation and matching ACK + advancing board readback. Five-tab UI retained. Hardware no longer runs simulation policies/counters/charts; requests do not optimistically change board state. No ON replay on reconnect/reboot/timeout. Privileged Android key support and .env APK imports removed; launcher cloud integration explicitly blocked pending authenticated home-scoped access. Persistent firmware manual/AUTO semantics remain unchanged and mismatched with until-AUTO intent.

Fresh **host-only** evidence: debug APK assembled; 28 control/HTTP Kotlin cases, 3 existing wire cases and 90 focused firmware/API regression tests passed. Full Gradle unit suite blocked by missing cached Robolectric native runtime. No phone/emulator, hardware port, flash, NVS, credentials or deployed database changed. Start from [the updated master implementation](CLAWGRID_MASTER_INTEGRATION.md) for exact commands, files, results, APK hash, blockers and next step. Earlier dated hardware evidence below is preserved history.

---

# ClawGrid physical integration handoff — 22 October 2026

**Latest ecosystem integration (22 October 2026):** Supabase encrypted Broadcast now carries real A reports and measured B diagnostics to the shared live webpage and Android source. Independent public-key client verification passed virtual ON/OFF through Supabase → B → A, with matching ACK and state. Physical GPIOs remain inhibited; missing temperature/lux and A RTOS/heap are unavailable. See [ecosystem integration and exact field mapping](docs/ECOSYSTEM_INTEGRATION.md). Current B USB is unavailable (Windows device error 31); the gateway reports UNKNOWN until that connection recovers. Earlier dated deployment notes below are retained history.

**Onboard traffic LED update:** New native A/B sources and images add RGB traffic,
command reception and matched application ACK pulses. Default RGB pin is GPIO48
(GPIO38 on official DevKitC v1.1). These new images have not been flashed by the
agent; user uploads both. Physical relay GPIOs remain inhibited. Earlier live
radio measurements describe the previous installed firmware. See [LED/upload
instructions](docs/TRAFFIC_LED.md).

**Role-swap deployment now verified:** User uploaded A COM8 and B COM7 successfully.
Both flash manifests match the current compiled application hashes. Read-only
`/api/state` through B shows fresh authenticated A reports, zero reported invalid/
replayed frames, and outputs inhibited. PC offline recognizer is ready, microphone
inactive. Evidence: `benchmarks/raw/hardware/role_swap_link_20261022.json`.
No human microphone or physical relay/load test has been performed in this check.


**Latest user-confirmed role correction:** Relay/PIR A is COM8, MAC
`e0:72:a1:f5:bc:b0`; bare PC bridge B is COM7, MAC `e0:72:a1:f6:a9:68`.
Local credentials retain the same keys/token; only board assignments changed.
Both radio-peer configurations must be rebuilt and both boards uploaded.
Earlier hardware PASS results below apply to the previous assignment, not this
new deployment. The user is doing the uploads; no swapped images were flashed
by the agent. Physical outputs remain inhibited.


**Current deployment: PC offline voice -> USB B -> ESP-NOW remote A.**
B uses `native_intelligence`; A uses `native_voice`. Both flash layouts have been
corrected to 16 MB with NVS preserved. The PC recognition/virtual ON-OFF path has
passed synthetic-input testing with real radio ACK/state observations. This is
not a human microphone or physical relay/load test. Physical GPIO outputs remain
inhibited pending electrical interface checks. A only needs remote power; B stays
connected to the PC. [Code/upload paths](docs/UPLOAD_CODE.md) ·
[PC voice operation](docs/PC_OFFLINE_VOICE.md).

Verification: 205 host tests passed; both frontend checks passed; 13/13 synthetic
recognizer/parser cases passed. Evidence:
`benchmarks/raw/hardware/pc_voice_end_to_end_20261022T051049Z.json`.
The entries below are earlier commissioning history.


**Latest commissioning update supersedes the earlier A-unavailable notes:** A is
now programmed on native USB COM7. Its old 4 MB bootloader/partition layout was
backed up and corrected to the verified physical 16 MB chip, preserving NVS.
A self-test, task progress and 16 MB/8 MB memory checks pass. B was already
programmed, but its USB application transport is currently being recovered after
reconnection. See [current status](docs/CURRENT_INTEGRATION.md) and
[wiring/checklist](docs/PROGRAM_AND_WIRE.md).

**Later 22 October deployment update:** B is now freshly built, app-only flashed
and runtime-verified on COM8. A's USB connection failed (Windows Code 43); A's
native USB and UART images are built but not deployed. Continue from
[current integration status](docs/CURRENT_INTEGRATION.md) and
[ESP wiring](docs/ESP_CONNECTIONS.md). The preparation evidence below is historical.
The supplied text-command sketch was subsequently integrated alongside the
dashboard/speech firmware and deployed to B: 196 host tests passed. See
[text-command usage](docs/SERIAL_TEXT_COMMANDS.md). The robotic-arm paste is out
of scope; the original broadcast/direct-GPIO sketch was adapted through the
existing authenticated proposal/confirmation and SafetyKernel path.

Preparation started 21 October; finalized 22 October 2026 IST (21 October UTC), branch `physical-integration-prep`. This handoff supersedes the older software summary here. Historical evidence is retained unchanged in the linked records.

## Tonight's result and hardware availability

**167 host tests passed (26.81 s), frontend checks passed.** Both inhibited roles and both explicit bench variants compiled; actual output/console/watchdog checks passed. Final image hashes are recorded below. These changes have **not been deployed or exercised on S3** tonight. Serial enumeration returned only Bluetooth COM3/COM4; no ESP32 or CH343 USB board was available. No serial device was opened, no flash/NVS was written, no policy was edited, and no credentials were regenerated. Last verified on-board state remains the normal inhibited, fault-disabled pair; that state cannot be freshly confirmed while disconnected.

| Role | Identity to verify before use | Last verified port/console | Last flashed application SHA256 (historical) |
|---|---|---|---|
| A: bedroom GPIO/PIR/SafetyKernel | `e0:72:a1:f6:a9:68` | COM9, CH343/UART0; earlier COM7 native USB | `82f3450186dae4bbf92b649059d58b4c341bfffe9fd6332d2a01219a31a5b4f2` |
| B: API/dashboard/local speech | `e0:72:a1:f5:bc:b0` | COM8, native USB | `e7bedef3a79ee225122b80290a9704e450541d13f97c2351f5b426e1586d075a` |

Do not assign a role from a COM number. Stop on a different MAC; retain existing provisioning. The installed pair's ignored `local_credentials.json` and `firmware/common/config/private_config.h` must remain intact. Never run key generation or erase-flash to resolve a connection problem. The app-only workflow writes app0 at `0x10000`, retaining the verified partition/model images and NVS at `0x9000..0xe000`.

Earlier S3 evidence: [17/17 USB bring-up](benchmarks/reports/USB_BRINGUP_20261021.md), then [3/3 watchdog trials, 69/69 checks and 16/16 restoration checks](benchmarks/reports/WATCHDOG_VALIDATION_20261021.md). The earlier host count was 144, newer than the quoted 122 baseline. Those hardware results validate their recorded hashes, not tonight's binaries.

## Repairs and reviewed behavior

- Physical PIR now calls `PirTracker::begin()` with **60 s warm-up**, then requires **200 ms continuous LOW** before arming. HIGH held through warm-up stays unarmed; it is never a startup motion event. Subsequent HIGH/LOW changes each require 200 ms filtering. The 60 s duration is provisional until checked against the actual PIR module. Readiness means this software gate passed, not validated human presence. GPIO6 uses `INPUT_PULLDOWN`; an unconnected LOW stays occupancy UNKNOWN until a qualified HIGH has occurred. `q` shows `pir_ready` and remaining warm-up; zero remaining with ready=false means waiting for LOW. Events identify `PHYSICAL_DIGITAL_GPIO`. Simulation inputs remain confined to HouseSimulation and cannot arm the physical tracker.
- The historical cached-report freshness defect is repaired on both roles. A sends only advancing control samples no older than 100 ms, and B rejects same-boot non-advancing sample timestamps even in new authenticated radio envelopes. Timestamp rollover is supported without changing schema 3 (44 bytes). B's existing 3 s freshness gate now ages frozen samples out; new commands/proposals refuse when stale. API state exposes boot/sample uptime and `nonprogress`; serial link counters expose the latter. Target validation of this repair remains pending.
- Relay polarity is an explicit, overrideable **provisional GPIO-to-interface** setting. `NODE_A_RELAY_ACTIVE_LOW=true` means OFF=HIGH, ON=LOW. An inverting transistor would generally require the opposite GPIO polarity. SafetyKernel preloads OFF before setting OUTPUT. Host HAL-call tests verify order and both polarities; they do not measure pin voltage, reset pulses or module behavior.
- Default A variants still register physical devices with GPIO=-1. They do not drive relay pins; inhibition does not provide an electrical OFF bias to a connected module. Only explicit bench environments enable GPIO4/5. Startup creates OFF devices and restores configuration only, never energized state, timers, manual holds, pending motion requests or queued commands. Failed policy/storage/self-test initialization disables command/control startup without erasing NVS. GPIO driver return codes and electrical startup still require target/electrical checks.
- Factory policy is HOME, light auto ON, fan auto OFF, grace 120000 ms. Preserved NVS can change those defaults. Last verified saved policy matches that factory policy. `/api/state` is the effective policy; `P` is the saved policy. Writes coalesce for 5 s; wait for successful `persistence_commit` and matching `P` before testing restoration.
- Manual ON/OFF establishes a 5 min automation hold. Resume releases it and can apply a retained motion request on the next tick if still eligible; it does not independently force ON. Transitions obey a 3 s dwell, including OFF requests. Timers retry an OFF denied by dwell; expiry is not a guaranteed instantaneous electrical cutoff. Vacancy clears pending motion and requests OFF, respecting manual hold. Occupancy is Occupied until 30 s since last qualified HIGH, then LikelyOccupied until the configured grace, then Vacant. After a previous HIGH, unplugging the input resembles no motion and eventually vacancy; it cannot identify that disconnection. A single PIR cannot establish stationary-person absence or diagnose every disconnected/stuck/wired-wrong input.
- Maximum continuous ON time is 4 h, enforced by the periodic kernel even if a manual ON hold is refreshed. It forces logical OFF and respects dwell before another transition; it is not a permanent fault latch. A later fresh request/event may energize again.

## Watchdog, disconnect and reboot policy

No watchdog timeouts were changed. RuntimeHealth uses `esp_task_wdt_init(5,true)`; generated A/B SDK configuration enables a **5 s task watchdog, panic, print/reboot**, and both idle-task subscriptions. A explicitly subscribes ControlTask (core1, priority5, 5 ms) and LinkTask (core0, priority3, 5 ms); B subscribes LinkTask. Each feeds its own subscription only after bounded work. Diagnostics or LinkTask cannot feed a stalled ControlTask's subscription. SpeechTask is not separately subscribed; speech-job completion and a bounded processing timeout must be observed separately. No speech-liveness guarantee is inferred from B LinkTask health.

`X` is a RAM-only ControlTask stall, enabled only by explicit diagnostic builds. It leaves link/diagnostics running and stops that task's watchdog feed. Compilation rejects diagnostic faults plus enabled outputs. Normal images reject X/d/O and B F/R/W/V. Existing three trials showed panic naming ControlTask, SDK cause 6 (ESP_RST_TASK_WDT), incrementing boot IDs, policy recovery and old-boot command/confirmation refusal; host-observed recovery was 6.267–6.270 s. No repeat was possible tonight. The maintained recovery campaign now additionally requires frozen telemetry to expire and the pending confirmation to be rejected during the stall. Old diagnostic binaries must be rebuilt before running that updated campaign.

Actual local demo policy: **B disappearing does not switch A's commanded outputs OFF.** A continues local PIR automation, manual holds, timers, vacancy and the 4 h maximum-run guard while its critical tasks remain healthy. This supports local autonomous operation; do not describe network loss as an immediate load shutdown. B marks A unavailable after 3 s without an advancing accepted sample. No retries are stored to disk. A pending command has at most four sends 300 ms apart (timeout about 1.2 s), and A validates its 3 s TTL, boot, ordered ID and duplicate bytes. An ACK timeout means outcome unknown, never proof of OFF.

A reboot creates a new persisted boot before accepting commands and starts logical OFF. B invalidates pending work on observing the new A boot. Old exact commands receive WrongBoot; old proposal confirmation fails boot binding. B reboot loses RAM proposals/transactions and seeds new proposal IDs; it waits for current A state before new submissions. A itself does not turn OFF just because B reboots. Reconnect with fresh state and prepare/confirm again; do not automatically retry an old UI request. Scenes may partially apply if one transition is denied; inspect both ACK/state fields.

## Exact software validation

Project root: `C:\Users\hares\Downloads\ClawGrid-software\AUTOHOME-main`.

```powershell
Set-Location 'C:\Users\hares\Downloads\ClawGrid-software\AUTOHOME-main'
$python = 'C:\Users\hares\Documents\Codex\2026-10-21\i-h\work\clawgrid-env\Scripts\python.exe'
$env:PLATFORMIO_CORE_DIR = 'C:\Users\hares\Documents\Codex\2026-10-21\i-h\work\pio-core'
$env:ZIG_GLOBAL_CACHE_DIR = Join-Path $PWD 'benchmarks\.host_build\zig-cache'
$env:TEMP = Join-Path $PWD 'benchmarks\review\physical-integration\temporary'
$env:TMP = $env:TEMP
& $python -m pytest tests -q --basetemp=benchmarks/review/physical-integration/pytest-after-console --junitxml=benchmarks/review/physical-integration/host-after-console.xml
& 'C:\Program Files\nodejs\node.exe' scripts/check_frontend.cjs
& $python -m platformio run -d speech -e speech_intelligence
& $python -m platformio run -d native -e native_voice_uart -e native_voice -e native_voice_bench -e native_voice_uart_bench
```

Pinned: Python 3.13.7, PlatformIO 6.2.0, Zig 0.13.0, pyserial 3.5, pytest 9.0.3, esptool 5.4.0; espressif32 6.6.0 / ESP-IDF 4.4.7 and ESP-SR 1.7.0 commit `07b0a4d0f6b5de755874d5405150ee0a8eec9865`. Existing fetched assets/cache were reused. Requirements remain in `requirements-dev.txt`; speech assets use `scripts/fetch_speech_dependencies.py`. No framework/model migration or confidence-threshold reduction occurred.

All commands above returned **0**. Native builds: four environments succeeded; B: one environment succeeded. Actual binaries passed retained output/fault marker and console checks; generated SDK configurations retain 5 s panic/reboot watchdog settings. Default mode validation rejects both bench binaries. Fresh UART bench configuration was regenerated from maintained CMake defaults after preserving the incorrect USB configuration in scratch; no private SDK/provisioning data was replaced. `pip check`, 16-clip format validation and `git diff --check` also returned 0. Installed credentials match the preserved pair backup and the changed public files contain none of the scanned private values.

| Final local image (not deployed) | Output mode / console | Application SHA256 |
|---|---|---|
| native_voice | inhibited / usb_serial_jtag | `b86539ead33cf15bae00f1851209e8802a6a2d249e6412d54714f8e5417a6316` |
| native_voice_uart | inhibited / uart0 | `8f0bc04a94922abbf5650035bf804cb2be188963e64b034d81eb70e4fc6b3174` |
| speech_intelligence | inhibited / usb_serial_jtag | `69d74bac4b349e32dca6a8469927e07366731016a33d7d98a0aacc20000f7367` |
| native_voice_bench | enabled (opt-in bench only) / usb_serial_jtag | `caf16c46fcdbcd7a7774033f7b43d6ed9f79c59cefbd95648f079952905c4742` |
| native_voice_uart_bench | enabled (opt-in bench only) / uart0 | `40a36d2d4fdb06b4372954ccd06c5f651883853165a1d5dd40283ae287ceab9c` |

[Machine-readable commands, image identities and limitations](benchmarks/raw/physical_integration_20261021.json). Binary files stay local/ignored because they embed credentials.

Working logs are in ignored `benchmarks/review/physical-integration/`: `host-after-console.log/xml`, `frontend.log`, `build-speech-final.log`, `build-native-console-fixed.log`, and `ports.log`. The initial UART bench linked with an incorrect USB console; binary/SDK guards caught it before any deployment. Pinned PlatformIO ignores the INI sdkconfig-defaults option, so maintained CMake now selects UART defaults and the HAL fails compilation on environment/console mismatch. Corrected builds passed. The initial UART bench linked with an incorrect USB console; binary/SDK guards caught it before any deployment. Pinned PlatformIO ignores the INI sdkconfig-defaults option, so maintained CMake now selects UART defaults and the HAL fails compilation on environment/console mismatch. Corrected builds passed. Initial sandbox run returned 1 (80 passed, 4 failed, 70 setup errors; compiler `error: Unexpected`); the permitted full run passed 154 tests before final guards, and final run passed 167. Both failed and passing logs are preserved. No target engine, physical latency or electrical measurement is attributed to host tests. GPIO tests capture real kernel HAL calls, not voltages.

## Identity, flash and monitor commands for tomorrow

Close bridges/monitors with Ctrl-C before ownership changes. Enumerate once, set `$aPort`/`$bPort` to observed candidates, and read identity before assigning roles:

```powershell
& $python -m serial.tools.list_ports --verbose
$aPort = 'COM9' # example only: verify MAC below
$bPort = 'COM8' # example only: verify MAC below
& $python -m esptool --chip esp32s3 --port $aPort --after no-reset read-mac
& $python -m esptool --chip esp32s3 --port $bPort --after no-reset read-mac
```

These ROM identity reads enter the loader; they are not passive runtime monitoring. A different MAC is a STOP, not permission to edit provisioning. After matching A/B and retaining current NVS/partition/model evidence, deploy the **inhibited normal pair**, together:

```powershell
& $python scripts/flash_native.py --role A --port $aPort --uart-console --app-only
& $python scripts/flash_native.py --role B-speech --port $bPort --app-only
# Separate terminals; exit with Ctrl+] before another process uses a port:
& $python -m serial.tools.miniterm $aPort 115200 --dtr 0 --rts 0
& $python -m serial.tools.miniterm $bPort 115200 --dtr 0 --rts 0
```

Flasher independently rechecks MAC, exact binary output/fault mode, console, N16R8 SDK configuration and artifact existence; it records artifact SHA256 and exit code. No erase, credentials regeneration or NVS rewrite is part of this workflow. A uses UART0 only if connected through the verified CH343 route; use native_voice without `--uart-console` for a verified native USB console. Ports are always explicit.

Check boot role, mode `outputs=inhibited;faults=default`, reset cause, `P`, `q`, `t`, advancing sample uptime, current policy and fresh schema-3 state. Request `q` again after 0.5 s: both job counters must increase. `m` is an optional bounded arena/heap probe. Every 5 s, existing logs expose internal free/minimum/largest heap, PSRAM total/free/largest, RX drops/TX failures, command drops, diagnostic drops, task jobs/max execution/max lateness/missed deadlines and stack high-water margins. API `/api/memory`, `/api/rtos`, `/api/state` and `/api/transaction` expose B memory, task timing, freshness and command/ACK RTT. These counters need fresh target observation; linker RAM percentages are not heap/headroom measurements.

For a 30 s retained telemetry capture after identity checks, close monitors and run `& $python scripts/capture_hardware.py --ports $aPort $bPort --seconds 30`. This legacy collector's firmware hash lookup targets older `.pio/build/node_*` paths: use the identity-checked flash manifests above to establish actual image provenance rather than treating that optional hash field as deployed-image proof. It sends only self-test requests unless `--probe` is supplied. Do not run another process on the same serial ports simultaneously.

For USB dashboard/audio through B (close B's serial monitor first): `& $python scripts/browser_bridge.py --serial $bPort --port 8770`, then open `http://127.0.0.1:8770/simulation`. Repeated virtual OFF prepare/confirm actions exercise transport without changing physical devices. Retain transaction RTTs, task counters/heap before and after, and bounded speech-job results; ACKs describe software acceptance, independent physical state remains **not measured**. `scripts/audio_bench.py --url http://127.0.0.1:8770 --speech-only` avoids its optional physical control command during evaluation. It uses the existing private token internally; never paste tokens in reports.

Only if watchdog repetition is needed, with every external component disconnected, rebuild `native_voice_uart_diagnostic` and `speech_intelligence_diagnostic`, flash each with the same normal command plus `--diagnostic`, then run `& $python scripts/recovery_hil.py --node-a $aPort --node-b $bPort --trials 3`. Stop at any failure. Capture panic+ControlTask, cause 6, exactly one boot increment, stale/confirmation refusal, fresh telemetry and identical saved policy. Immediately reflash the normal inhibited pair and verify fault hooks disabled and increasing task counters. Never run faults on a bench/output-enabled image.

## Pin and electrical checks before any output-enabled use

Actual parts are **two separate 5 V single-channel low-level-trigger relay modules**, not a two-channel PCB. Their GPIO compatibility, input current/voltage, isolation arrangement and reset behavior are unverified. PIR pin order and output voltage are also unverified.

| A signal | GPIO | Current software assumption | Required electrical check |
|---|---:|---|---|
| Bedroom light | 4 | Active-low GPIO: OFF HIGH / ON LOW | Separate light module IN/VCC/GND, 3.3 V compatibility and any transistor inversion |
| Bedroom fan | 5 | Same provisional polarity | Separate fan module interface, supply/current and DC motor transients |
| Bedroom PIR | 6 | Digital active HIGH, input pulldown, 60 s warm-up + LOW baseline | Identify VCC/OUT/GND from the actual sensor, supply and OUT voltage; never feed 5 V into ESP GPIO |

Before power, identify terminal labels/pin order and relay NO/COM contacts. Verify grounds/isolation from actual modules; do not defeat an isolation scheme by guessing connections. Measure the control-interface levels/current and powered/unpowered/backfeed behavior. Check GPIO voltage remains within ESP32-S3 limits. A 5 V relay supply does not imply 3.3 V-safe IN. Use a suitable interface when needed, then set GPIO polarity to the **complete** interface and recompile. No final transistor circuit/polarity is assumed. Provide a defined OFF bias during reset/boot/high impedance; firmware OFF preloading starts only after application initialization. Verify reset/cold-start behavior without loads before relying on it. For the DC fan, check supply capacity, contact DC/inrush rating and inductive suppression. Low-voltage DC only; no mains work.

Bench enable is a **later explicit step after the electrical checks**, not part of tonight's deployment. Use `native_voice_uart_bench` for CH343, or `native_voice_bench` for native USB. New flasher `--bench` selects only A's exact output-enabled/default-fault image and retains MAC/console guards. `--bench` and `--diagnostic` cannot combine. Example only after the relay-without-loads stage is ready:

```powershell
& $python scripts/flash_native.py --role A --port $aPort --uart-console --bench --app-only
# Return to inhibited operation after the bench session:
& $python scripts/flash_native.py --role A --port $aPort --uart-console --app-only
```

Do not use PlatformIO's stale upload_port defaults or bypass the guarded flasher. Bench images contain private installed credentials and stay local/ignored.

## Ordered component tests and stopping criteria

| Order | Test | Expected observation | Stop and diagnose |
|---:|---|---|---|
| 1 | **PIR alone, inhibited firmware; relays/loads disconnected** | Verified pinout and safe OUT voltage; q stays unready during 60 s warm-up, then waits for 200 ms LOW; a subsequent stable HIGH creates a physical-source edge. An unconnected LOW stays occupancy UNKNOWN. | Unsafe voltage/pin uncertainty, boot-time motion accepted, unexplained resets or HIGH stuck after warm-up. Do not simulate motion on the physical route. |
| 2 | Relay modules individually, contacts unloaded | Verify safe interface/polarity/OFF bias first, then opt-in bench. Each physical bedroom ON/OFF goes through confirmation/ACK/SafetyKernel and changes only its intended module; observe coil/contact behavior independently. Check reset/power-up stays OFF. Allow at least 3 s between transitions. | Wrong channel/polarity, voltage/backfeed problem, chatter, startup energization or unintended contact state. Disconnect relay power and restore inhibited image. |
| 3 | Light, one low-voltage DC load | Correct NO/COM wiring, manual ON/OFF, confirmed independent lamp observation, inhibited restoration after test. | ACK/state disagrees with observation, unexpected heating/current, flicker or reset. |
| 4 | Fan, separately | Correct DC rating/suppression, controlled start/stop, no brownouts/chatter; other channel unaffected. | Supply droop, repeated resets, excessive inrush/heat, inconsistent OFF behavior. |
| 5 | Automation with real PIR | Warm-up/LOW arm, factory fan-auto OFF unless deliberately changed, manual hold/resume, configured vacancy grace/timer/dwell. Disconnect B: A continues documented local policy. Reboot A: OFF and original saved configuration, no old confirmation/ON replay. | Unexpected ON, auto fan contrary to effective policy, stale command accepted, failure to recover or unmeasured electrical startup. Do not wait 4 h to infer max-run proof from a short test; the boundary is host-tested only tonight. |
| 6 | Voice last, physical target explicitly selected | First check speech-only PCM/results on B, then correct proposal/target and confirmation, software ACK, independent load observation. Negative/other-room utterances must not actuate bedroom. | Wrong ID, false activation, stale target, unexplained engine error or no detection. Preserve the failure; do not lower thresholds to obtain a pass. |

At every stage stop for loss of inhibition during USB-only tests, credential/identity mismatch, non-advancing telemetry, sustained queue overflow, declining heap/stack margin or a watchdog event outside the deliberate disconnected-load diagnostic campaign. Keep physical observations separate from software ACKs and virtual events.

Speech uncertainty remains: the latest retained recognition smoke test is **0/16 synthetic commands**, with no human corpus. Byte-exact transfer/PCM host tests and successful linking do not establish recognition. Fixed-command model, framing/end silence and bounded candidate/state/level diagnostics are already present. Keep the pinned pipeline and 0.7 threshold; AFE remains a hypothesis. Use `benchmarks/templates/audio_manifest.csv` for real recordings with speaker/session/conditions and development/held_out split; report correct/wrong/no detection/engine/transport outcomes and negatives with real denominators. Human/held-out accuracy, simultaneous speech/control timing, physical sensor/relay behavior, cold-start/brownout behavior and long-run stability remain pending.

## Files changed tonight

- `firmware/common/config/clawgrid_config.h`: provisional overrideable relay polarity, real module wording and PIR warm-up duration.
- `firmware/common/runtime/pir_tracker.h`: explicit physical startup gate and readiness diagnostics.
- `firmware/common/runtime/state_wire.h`: portable wrap-safe advancing/fresh-sample rules.
- `firmware/node_voice/src/main.cpp`: activate physical startup gate, label events, expose readiness and stop cached/aged report transmission.
- `firmware/node_intelligence/src/main.cpp`: reject frozen samples without refreshing controller age; expose boot/sample/nonprogress diagnostics.
- `native/platformio.ini`, `native/CMakeLists.txt`, `native/main/native_hal.cpp`: explicit UART bench variant and reproducible clean SDK console defaults; compile-time console mismatch refusal, leaving normal outputs inhibited.
- `scripts/flash_native.py`: guarded explicit A bench selection, mutually exclusive faults, artifact mode/console/MAC checks and manifest mode.
- `scripts/recovery_hil.py`: next target campaign must prove stale-control state and confirmation refusal during fault.
- `tests/native/integration_selftest.h`, `tests/native/firmware_checks.cpp`, `tests/unit/test_firmware_cpp.py`: PIR startup/HIGH/noise/wrap/vacancy, report progress/age, dwell/timer, restored transients and max-run boundaries.
- `tests/native/gpio_fixture/{platform_hal.h,checks.c}`, `tests/unit/test_gpio_initialization.py`: real kernel HAL call order and both polarities, inhibited/invalid devices do not write GPIO.
- `tests/unit/test_watchdog_campaign.py`: bench mode/environment and unsafe-combination refusal.
- `benchmarks/raw/physical_integration_20261021.json`: HOST_MEASURED commands, exit codes, final image hashes, checks and target-validation limits (date is UTC).
- `NEXT_CHAT_HANDOFF.md`: one current integration package; historical reports remain unchanged.

The explicit host-only dashboard path remains available with `& $python scripts/browser_bridge.py --host-dev --port 8770`. It always shows its simulation banner and never replaces a failed real connection. No simulator expansion, Pi migration or external push occurred in this preparation session.
