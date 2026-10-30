# ClawGrid voice-controlled LED demonstration — 28 October 2026 IST

The declared physical scope is two ESP32-S3 boards, controller A light/fan indicators and DHT11. PIR and occupancy-driven physical AUTO are **excluded**, not remaining hardware requirements. Relays/appliance loads are unused. Native Android UI and existing bundled offline Vosk model are retained.

## Installed identities

| Item | Role / configuration | SHA256 |
|---|---|---|
| Controller A | MAC e0:72:a1:f5:bc:b0; CH343 serial 5C37271524; native_voice_uart_led_high_bench; active-HIGH enabled LED-only; PIR unavailable | `2fad039a863ba65484e09cf761b8f1d3e4b598fb37a63bf95517922fb0ae9955` |
| Gateway B | MAC e0:72:a1:f6:a9:68; serial 5C37271527; speech_intelligence_uart | `60da71c68dce57a5e0f663ff25c53e7b0911008ac20562ec2dc3ce7063b4881c` |
| Android | `1.0-voice-led-no-pir-20261028j`; code1; existing debug signing key | `c349a275575dd683b76be69db31ce99fde5900888db7adaae89fec4d276f9c92` |

Ports at fresh ROM verification A COM10/B COM9; never infer roles from old COM names. Guarded application-only updates and app readback/protected-system checks passed. B model partition verified unchanged. Existing dual rollback backups checked intact. NVS, provisioning, credentials and models preserved; normal boot counter/policy writes remain application behavior. Final APK pulled from phone and hash-matched. Native16KB/ZIP alignment, signature match, bundled-model contents and configured-private-value checks passed.

APK i demonstrated the voice path. Final j changes only recovery-status message/version plus a regression test: recognizer, model archive, parser, ViewModel and push-to-talk source verified byte-identical between the two snapshots. Final85 actual-source JVM tests passed; earlier65 focused native/GPIO/proposal/privacy cases and final51 native firmware cases passed. A/B final builds SUCCESS9.790s/19.604s after the scoped change; APK build logs/receipts retained.

## Wiring and electrical evidence

| Signal | Physical wiring | Observation |
|---|---|---|
| Light | A GPIO4 -> own 10kΩ series resistor -> LED anode; cathode -> GND | User confirmed series path,0V whileOFF, correct independent visible ON/OFF |
| Fan | A GPIO5 -> own 10kΩ series resistor -> LED anode; cathode -> GND | User confirmed series path,0V whileOFF, correct independent visible ON/OFF |
| DHT11 | DATA A GPIO7; original working3V3/GND arrangement | Real valid checksum telemetry, sensor failure and recovery observed |
| PIR | Removed | Acquisition disabled; no disconnected input sampling; physical occupancy/AUTO unavailable |

The intended explicit output-enabled active-HIGH LED profile is deployed. SafetyKernel remains in control. GPIO commanded state and software ACK are not electrical feedback; visibleLED/meter observations are labelled USER_REPORTED separately. BootOFF observations do not establish oscilloscope-level absence of every reset transient.

## Embedded implementation and verification

See [task/queue/synchronization review](EMBEDDED_LED_REVIEW_20261028.md). A ControlTask owns physical output transitions after startup, core 1 / priority 5 / 5 ms; A/B link tasks core 0 / priority 3 / 5 ms; A SensorTask core 0 / priority 1. ESP-NOW callbacks only bounded-copy into depth 8 static queues, processing deferred. Nonblocking queues either return explicit drop/counters or overwrite latest-value mailboxes. No indefinite sensor/control waits. Diagnostic overflows counted. No live queue-saturation endurance campaign is claimed.

DHT11 uses20ms host startLOW and10ms bounded edge capture with at most 100 recorded edges, then decode outside ISR;2.5 s spacing between reads and2s startup settle. TIMEOUT/invalid/checksum failure replaces the previous sample as unavailable. Freshness is checked at controller/gateway/phone; a stale valid reading is not shown as current.

Observed sensor failure: boot 62 fresh controller telemetry, temperature/humidity/age null and statusTIMEOUT. A control/link jobs36377->37264, sensor attempts72->73, sensor/gateway watchdogs active. OFF commands succeeded during this condition. Android showed Unavailable/DHT11/TIMEOUT. After user restored DATA, same-boot valid24.4C readings returned, tasks continued, both outputs stayedOFF. This demonstrates bounded failure handling and recovery; it does not calibrate temperature.

Representative sampled task metrics: A ControlTask max_exec293us/max_late6us, A LinkTask818us/6us, B LinkTask1483us/1552us; sampled deadline_misses0, watchdogtrue. RX/TX/command/diagnostic drops0 in retained final records. These are measured observations, **not hard real-time guarantees** or unmeasured worst-case bounds.

Physical PIR unavailable and automation_availablefalse are carried in authenticated reports; gateway motionnull, occupancyUNAVAILABLE, auto_flags0. Android hardware AUTO disabled/explained. Separately labelled simulation preserved. Legacy saved auto policy bits retained for format compatibility but not executed/advertised as physical automation. Persistence format unchanged: manual-mode flags restore heldOFF; ON level not restored.3s dwell and4h maximum continuous run retained and actual-source host tested. No 4 h physical endurance claim.

## Actual end-to-end results

| Check | Result and evidence |
|---|---|
| Manual light/fan ON/OFF on no-PIR build | USER_REPORTED correct independent visibleLEDs, ACK/readback and DHT. Timed first manual capture expired before actions; later boardOFF/OFF / cumulative ON counters/last command5 observed. Earlier controlled manual IDs1-4 on preceding compatible build are historical evidence. |
| Microphone round1 | User verified correct recognized proposals, explicit confirmation, ACK/app readback and correctLED: boot 61 light ON#6/OFF#7, fan ON#9/OFF#10, all matching appliedACK/fresh state. ExtraOFF#8 retained; initiating action unconfirmed. |
| Microphone round2 | User verified once-per-phrase: light ON#11/OFF#12, fan ON#13/OFF#14; matching appliedACK/fresh state; both OFF. |
| Four negative utterances | User completed/reported working: what time is it; don't turn the light on; light and fan on; turn the television on. One captured preview metadata was UNSUPPORTED_WORDS/no interpreted target. Window contains extra intentional light ON#15/OFF#16, so it is **not an isolated automated zero-dispatch proof for every negative**. |
| Cancellation | User reported working. Initially ambiguous answer was explicitly corrected: #15/#16 were deliberate ON/OFF, not unconfirmed dispatch. No confirmation-safety defect established. Host cancellation/no-replay tests and real unconfirmed-phone-proposal check pass; mixed voice-window limitation retained. |
| Transport rejection | Real API oldboot/wrongroom/zero nonce/wrongtarget rejected; light/fan OFF queued once then matching appliedACK/fresh state; repeatConfirm409/no second command; expired proposal409/no dispatch; unconfirmed phone fan OFF proposal no dispatch. `transport-final.json`. |
| Restart | boot 61->62, last command resets0, heldOFF/manual_flags3, no oldON replay; user confirmed visibleOFF. Protected identities unchanged. |
| Reconnect | Final phone UNKNOWN while Wi-Fi absent, freshBOARD/readback on return; no automatic actuator requests. Final receipt retained when check finishes. |
| Sensor availability | Actual TIMEOUT/null reading and Android unavailable followed by valid24.4C recovery, control/link/sensor progress and watchdogs maintained. |

Earlier unrestricted-recognition failure is retained in its authorized local diagnostic, not replaced with successful trials. Decoder correction uses same dynamic-graph Vosk English0.15 model and fixed0.7 per-word gate: explicit phrase patterns plus negative paths and[unk]. Finalized whole text only. Case, supported punctuation and repeated whitespace normalized conservatively; no filler deletion, fuzzy/homophone mapping or ON/OFF guessing. Memory-only preview displays exact final/normalized words, target/action and reason; clearing/privacy behavior retained. No raw audio saved/uploaded. Routine trial evidence stores structured decisions/comparisons rather than transcripts.

## Completed and bounded limitations

- Implemented/deployed: active-HIGH LED-only GPIO, explicit PIR/AUTO unavailability, bounded real DHT acquisition, existing RTOS/queue/watchdog protections, strict confirmed light/fan voice integration, correct ACK/freshness semantics and stale-reconnect-message fix.
- Observed: independent manualLEDs, two reported microphone trials per canonical command with correlated board results, sensor failure/recovery, restartOFF/no replay, transport negatives, local AP operation with mobile data OFF.
- Evidence limitations: mixed extra deliberate actions in negative/cancellation window; per-negative microphone rejection/visibility partly user-reported. No oscilloscope transient, long endurance, forced queue-overload or calibration claim. Host tests are not physical observations.
- This document concerns the declared low-voltage LED demonstration, not completion of cloud/multi-room/appliance features. PIR/physicalAUTO excluded by request.

## Short demonstration

1. Join ClawGrid-Assistant, mobile data OFF; apply existing gateway URL/token privately. Require Source BOARD/fresh state, exact enabledLED profile, both OFF; Telemetry shows real DHT11 and PIR/AUTO unavailable. Show live/retained RTOS task counters/watchdog diagnostics alongside the app; A task counters are honestly not ingested in the Android screen.
2. Bedroom light ON/Confirm -> matching ACK+fresh readback -> correct visibleLED. Wait3s, light OFF/Confirm. Repeat fan. EndOFF.
3. Select Bedroom; ensure current-boot manual light/fanACK proof. Hold until Speak now, say LIGHT ON/OFF or FAN ON/OFF, release, inspect exact words/target, explicitly Confirm. Cancellation/negative speech never authorizes actuation. Never confirm a wrong proposal.
4. Restart with both OFF, rejoinAP, require freshBOOT/readback and no previousON replay. Sensor can be power-disconnected/reconnected only with wiring power off; TIMEOUT must display unavailable. End both manuallyOFF.

Evidence directory: `benchmarks/review/voice-led-no-pir-20261028/`. Test results, installed hashes and journals stay separate from human observations. Final endpoint snapshot and saved checkpoint are authoritative for the ending state.

Final saved endpoint: boot 62, last command2, authenticatedfreshOFF/OFF, DHT1124.3C/statusOK. FinalAPKj reconnect checkPASS (UNKNOWN while disconnected, BOARD on recovery, no actuator requests). SeeFINAL_CHECKPOINT.json.
