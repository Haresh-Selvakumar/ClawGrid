# Voice and stability resume — 2 November 2026 IST

This supersedes older voice NOT_RUN and unfinished diagnostic statements in the root handoff. Machine status: `benchmarks/review/three-board-acceptance-20261102/CHECKPOINT.json`. Historical evidence is preserved. The user ended further tests with “working bro no more tests req go next.” Do not restart campaigns or request more trials without new user direction.

## Results and evidence

All paths below are relative to `benchmarks/review/three-board-acceptance-20261102/`.

| Feature / room | Result | Evidence |
|---|---|---|
| Room 1 voice readiness | PASS, existing guard satisfied normally | `resume-voice-gate-assessment.json`, `resume-room1-off-proof.json` |
| Room 1 spoken LIGHT ON/OFF | PASS in observed trials | `resume-room1-voice-light-on.json`, `resume-voice-results.json`, `USER_OBSERVATIONS.json` |
| Room 1 spoken FAN ON/OFF | PASS in observed trials | `resume-room1-voice-fan.json`, `resume-voice-results.json`, `USER_OBSERVATIONS.json` |
| Room 1 cancellation | PASS in isolated observed window | `resume-room1-voice-cancel.json`, `resume-voice-results.json` |
| Room 1 remaining negative speech | NOT_RUN, user requested no more tests | `resume-voice-results.json` |
| Room 2 stability / ON acceptance | BLOCKED, earlier watchdog cause unresolved | `resume-C-stability-assessment.json`, `C-boot5-reset-reason.json`, `C-watchdog-followup.json`, `resume-C-health.json` |
| Room 2 voice | BLOCKED by stability | `resume-voice-results.json` |
| Phone version / keep-awake restoration | PASS | `resume-phone-runtime.json` |

### Voice gate and positive trials

HOST_MEASURED source audit: Hold to talk requires microphone permission, ready bundled model, explicit selected room matching its repository, fresh supported output profile, no active transaction, and that repository's manual light ACK plus fresh readback for the current controller boot. Fan intents additionally require the corresponding fan proof. Relevant source: `PhonePushToTalk.kt`, `ClawGridViewModel.kt`, and `HardwareRepository.kt` under `ecosystem/android/app/src/main/java/com/example/`.

HARDWARE_MEASURED phone UI initially reported “No request sent by this app session” and the manual-ACK guard despite fresh A telemetry. Permission was granted; model-ready branch and Room 1 selection were visible. The missing current-session manual proof was resolved using the normal Room 1 OFF proposal/confirmation dialogs: light #20 and fan #21 on boot72 both reached matching ACK and fresh readback. Hold to talk then displayed ready instructions. No forced enable, historical-proof injection, transcript injection, source change, app restart or build was used.

HARDWARE_MEASURED: A boot72 light ON#22/OFF#23 and fan ON#24/OFF#25 each have applied ACK, matching fresh readback and intended software state. C remained boot5/command0/OFF throughout both windows. Controller control-source metadata uses the same manual category for phone manual and voice controls; it does not independently prove microphone origin.

USER_REPORTED: the user answered the room-targeted preview / intended LED questions with “room 1 working go to next” and “working bro go to next,” and explicitly answered “Yes, both OFF commands were spoken.” The user performed OFF in each positive window beyond the ON-only instruction; this provenance clarification is preserved. Individual preview text and speech were not retained. These are observed microphone acceptance trials, not speech accuracy statistics.

Cancellation was isolated from positive commands: 36 samples, A command25 and C command0 unchanged, both rooms OFF throughout. The user reported cancellation working in response to the cancel-preview / all-four-OFF instruction. Negation, unsupported device, ambiguous/multiple actions and unsupported room were not run after the user's no-more-tests instruction. No raw audio was retained.

### C watchdog investigation

C is classic ESP32-D0WD-V3, not ESP32-S3. The actual source prints `esp_reset_reason()`. The pinned ESP-IDF4.4.7 enum identifies 7 as `ESP_RST_WDT` (other watchdogs), while task watchdog is 6 and interrupt watchdog is 5. The actual ESP32 implementation maps several raw reset causes to 7; this value cannot identify an offending task or establish DHT causation. Verified against installed SDK sources and [Espressif's versioned source](https://github.com/espressif/esp-idf/blob/v4.4.7/components/esp_system/port/soc/esp32/reset_reason.c).

The previously unfinished `C-watchdog-followup.json` is complete: 84 records spanning about178.077 seconds, boot5, progressing control/link/sensor jobs, watchdog registrations true, no critical boot lines, serial closed. Retained ControlTask and LinkTask stack minima are5412/4548 bytes, respectively. These do not prove SensorTask or Diagnostics stack margins. The fresh read-only16second C capture again reported boot5/reset7, progressing tasks and no panic lines. Neither quiet window explains boot4→5.

The retained raw-journal search is saved in `resume-retained-reset-search.json`; its scope lacks a contemporaneous C panic/backtrace. The deployed C configuration disables core dumps, enables panic print/reboot, and retains task5second and interrupt300ms watchdogs. No raw reset-register detail or persisted dump is available in this evidence. No demonstrated source defect justifies a repair. Firmware, watchdog feeding, priorities, decoder thresholds, timeouts and flash/NVS were unchanged. Room 2 ON and real-microphone acceptance remain BLOCKED.

### Phone and final state

USB adapters re-enumerated by verified serial identity: A5C37271524→COM10, B5C37271527→COM9, C5A7B059772→COM11. MAC binding comes from existing verified ROM/deployment evidence; no resetting ROM probe was used. Detailed process inspection found only the existing adb server before hardware capture; no old observer was found. All captures created here closed their handles; no Python observer/test process remained at closure.

HARDWARE_MEASURED phone: APK1.0-three-room-20261102c, process7497, installed100178504byte base APK SHA256 `125eccf8d4cf5bc988e3476cf5e74846bc870d20a0c6b6e09d9ac891c813ba99` freshly hash-matched. Phone associated with `ClawGrid-Assistant`, RECORD_AUDIO granted. Keep-awake setting was already restored and freshly read as original0; no temporary keep-awake change was made here. Keys, credentials, bundled offline Vosk model and installed app were preserved.

Latest retained final state: 2026-11-02T06:52:56.930020Z (12:22:56 IST), Aboot72/command25 and Cboot5/command0, all outputs OFF, no armed schedules or countdowns. A light continuous cap60000ms remains configured; A fan and both C caps are0. Do not assume A's cap cleared because C rebooted. USER_REPORTED cancellation worked in response to the all-four-OFF check. Software state and visible observations remain separately labelled.

Historical schedules, sensor evidence, dual timing and APK boundary checks were reused, not repeated. Existing dual-timing validation remains103 Android JVM tests and87 firmware assertions; no new implementation tests were needed because source was unchanged. Countdown authorization/cancellation while OFF still proves configuration/UI behavior only. Exact energized automatic-OFF timing, clean manual-priority cancellation, late/dwell and reboot/reconnect gaps remain as previously documented. Both timing controls remain independent; the earlier OFF rule wins. The project is INCOMPLETE while C stability, Room 2 voice and remaining acceptance gaps are open.
