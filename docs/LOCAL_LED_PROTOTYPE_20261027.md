**Superseding no-PIR scope:** [final LED demonstration](VOICE_LED_DEMONSTRATION_20261028.md). Active-HIGH lightGPIO4/fanGPIO5, own series resistors/cathodesGND, DHT11GPIO7; PIR acquisition/physicalAUTO disabled. Earlier wiring/status blocks below are historical.

**Current 28 October update:** [GPIO8 active-HIGH LED checkpoint](INTEGRATED_LED_CHECKPOINT_20261028.md) supersedes earlier deployment/status statements below. PIR OUT currently requires an electrical compatibility check; keep it isolated.

# One-room local LED prototype — 27 October 2026 IST

User resumed and moved the deadline to today. Two external LEDs represent light/fan; relays and appliance loads are unused. Preserve existing native Android UI, bundled offline phone recognition, provisioning, models and manual-control baseline. Cloud, extra rooms, redesign and speech-model experiments are deferred.

## Verified baseline

- Fresh Windows enumeration initially showed Bluetooth only; subsequently two CH343 adapters appeared. ROM MACs matched provisioning: A COM10 / e0:72:a1:f5:bc:b0 / adapter 5C37271524; B COM9 / e0:72:a1:f6:a9:68 / adapter 5C37271527. Roles assigned only after MAC match.
- Read-only esptool `verify-flash 0x10000 <existing firmware.bin>` matched the complete application image prefixes on both boards. A native_voice_uart: 683,008 bytes, SHA256 8f028bd90f65a55a72b09f4b886db7a3fa851f8a2108c6b79659136db8713cdf. B speech_intelligence_uart: 1,213,232 bytes, SHA256 97daa84e9865a0df09c550387051d401b6d7cc98f547c808bd6734b13a35cbc4. Inhibited/default-fault and provisioning-pair artifact guards passed. No flash write, erase, model, partition, key or NVS edit. Normal ROM reads reset the applications and advance the boot counter.
- After verification: controller boot24, authenticated fresh OFF/OFF, physical_outputs_enabled=false. Samples 78,997→81,997 ms; controller ControlTask jobs15,672→16,295; gateway LinkTask14,480→15,080; channel1/AP192.168.4.1/watchdogtrue/zero sampled deadline misses. No actuator command issued in this baseline check. Serial connections closed by finally handlers.
- Evidence: benchmarks/review/local-led-20261027/board-identities.json, installed-firmware-verification.json, fresh-runtime.json, read-only logs. These observations are HARDWARE_MEASURED; they are not physical LED/sensor success.

## Actual source GPIO map and hardware gate

Source: firmware/common/config/clawgrid_config.h and node_voice/src/main.cpp. Only controller A receives sensor/indicator wiring; B remains the gateway.

| Function | Source assignment | Current status |
|---|---|---|
| Light output | GPIO4 | Registered as pin -1 in inhibited image; no GPIO actuation |
| Fan output | GPIO5 | Registered as pin -1 in inhibited image; no GPIO actuation |
| PIR OUT | GPIO8 | INPUT_PULLDOWN, 60s warmup +200ms LOW baseline, 200ms input filter |
| Temperature DATA | None | No physical temperature driver or schema field in this baseline; must implement after identification |
| Output polarity | active_low=true | Existing interface assumption; LED circuit must deliberately match polarity or firmware must explicitly change it |
| Runtime console | UART0 via CH343 | Preserve GPIO43/44 and USB19/20; don't choose flash/PSRAM/strap pins by assumption |

Photos supplied from Downloads (5.51.07/08 PM files) show two relay modules (unused), a blue perforated DHT11-style 3-pin breakout, and an HC-SR501-style PIR board with two trimmers. Exact sensor model markings and header labels are not legible/are obscured by jumpers; ESP header silkscreen is blurred. These establish likely families, not verified connector order. No left/right pin assignments or wire-colour inferences were made. Requested unplugged-header/reverse-side and ESP-label close-ups before a final pin-by-pin wiring table. Resistor values remain to be confirmed explicitly. No component wiring authorized from a guessed photo pinout.

Reference specifications are family cross-checks, not identification of an unlabelled clone: [Joy-IT HC-SR501 manual](https://joy-it.net/files/files/Produkte/SEN-HC-SR501/SEN-HC-SR501_Manual_2024-04-16.pdf), [Espressif ESP32-S3 datasheet](https://documentation.espressif.com/esp32_s3_datasheet_en.pdf). ESP GPIOs use 3.3V logic; do not attach a 5V sensor signal/pullup directly. Each LED needs its own series resistor (requested two 1kΩ); final circuit and output polarity remain gated on confirmed components/board labels. Relays stay unused. Power-disconnected, one-component-at-a-time wiring follows identification, starting with PIR.

## Implemented Android manual-fan slice

HardwareRepository now carries the immutable selected device (light/fan) through proposal, confirmation, matching ACK and authoritative same-boot newer readback. Fan HTTP acceptance never changes the displayed reported state. Light ACK/readback cannot complete a fan request. Unsupported devices fail before a POST; target mismatch rejects; disconnect/reboot invalidates without replay. Manual-light proof alone continues to unlock the existing voice gate; a fan result cannot substitute for it.

Existing Bedroom fan controls enabled only for fresh/known state with no active request. Confirmation and request-status panels show the actual device and both reported outputs. Screens/navigation and all voice phrases/parser/model/confidence/preview privacy remain unchanged. AUTO is still disabled in hardware mode pending the separate policy milestone; no hidden policy change.

Changed files:

- ecosystem/android/app/src/main/java/com/example/data/remote/HardwareRepository.kt
- ecosystem/android/app/src/main/java/com/example/ui/ClawGridViewModel.kt
- ecosystem/android/app/src/main/java/com/example/MainActivity.kt
- ecosystem/android/app/src/main/java/com/example/ui/components/HardwareRequestStatus.kt
- ecosystem/android/app/src/main/java/com/example/ui/screens/RoomsAndRelaysScreen.kt
- ecosystem/android/app/src/test/java/com/example/HardwareRepositoryTest.kt
- ecosystem/android/app/build.gradle.kts (debug version suffix only; release signing preserved)

Prior app sources/master/handoff were copied under review/local-led-20261027/before; firmware source hashes sealed before work. Existing installed preview APK and source snapshots remain rollback.

## Focused checks and built/installed distinction

| Check | Actual result |
|---|---|
| Python scripts/check_android_control_jvm.py | PASS70 actual-source Kotlin/JUnit cases,1.288s runner; four new fan contract/readback/target/reboot cases plus prior66 |
| pytest tests/unit/test_phone_preview_privacy.py -q | PASS3,0.66s |
| build_phone_outside_onedrive.py --name local-led-20261027a --evidence benchmarks/review/local-led-20261027 --build-folder local-led-build-20261027 --apk-name ClawGrid-Local-LED-debug-20261027a.apk --gradle-cache C:/Users/hares/Documents/ClawGrid-Android-build-20261025b/gradle-user | SUCCESS1m53s;83 byte-identical source files;25 tasks executed/14 cached |
| check_phone_offline_apk.py deliverables/ClawGrid-Local-LED-debug-20261027a.apk | PASS bundled model14 file hashes, Vosk/JNA4ABIs,64-bit16KB ELF loads and unchanged active local recognition boundary |
| apksigner verify --print-certs | Initial shell lacked JAVA_HOME and did not run verification. Repeated with explicit existing JDK21: PASS; same debug certificate71ce1bf6f415db7a355e7c63d941c53e7b52fba6a4c33c25042582af0a464bf9. Both logs retained |
| zipalign -c -P16 4 and check_android_client_boundary.py --apk <new APK> --build-config <snapshot generated BuildConfig.java> | PASS alignment; configured private matches0/privileged fields0 |
| ADB installation | Initially no phone, then unauthorized. After human authorization: adb install -r SUCCESS; installed base pulled and SHA256 MATCH; dumpsys version1.0-local-led-20261027a/code1 verified; existing app opened, no uninstall/data clear. installed-receipt.json retained. No phone voice/control trial yet |

New laptop APK: deliverables/ClawGrid-Local-LED-debug-20261027a.apk; version1.0-local-led-20261027a/code1/packagecom.aistudio.clawgrid.v2edge;100,112,968bytes; SHA256365ee5e60193b6bd70ce6aa237ba0587a64b495ba09393f2f398cd517ce53ebe. Snapshot C:/Users/hares/Documents/ClawGrid-Android-build-local-led-20261027a. The new APK is now also verified installed on the physical phone. Prior installed previewc/SHA29c295701e6ba7235f4265b960f45602915968da2e7efafa27185f6cbb876a87 remains rollback. Updating cleared the in-memory token/manual proof; reapply privately and restore fresh manual-light proof when board wiring/power context is stable. No phone recording or new firmware build/flash this milestone.

## Remaining work in execution order

1. Confirm sensor model/header labels/ESP GPIO labels and actual resistor values. Give final pin-by-pin table and guide PIR wiring with both USB supplies disconnected, then inspect real motion/warmup/filter reports.
2. Implement identified temperature driver on a confirmed free GPIO with bounded acquisition outside ControlTask; version validity/error/age through A→B→Android. Deploy compatible inhibited pair with guarded app-only rollback, preserving models/NVS. Verify real readings and invalid/disconnected behavior before calling sensor integration complete.
3. Fan APK installed/hash-verified; after wiring power context is stable, restore token privately and fresh OFF/light proof. Test fan ON/OFF through proposal/Confirm/ACK+readback while still inhibited before physical LED stage.
4. Resolve five-minute manual hold vs user's manual-until-selected-device-AUTO requirement explicitly in code/docs. Existing HOME removes AWAY and releases both manual holds; AWAY suppresses automatic motion; fan auto defaults false; per-device Resume currently only releases its hold and does not enable a disabled automatic policy. Existing grace120s/first30s occupied depends on filtered PIR output. This does not yet satisfy requested semantics; retain safe OFF reboot/no old ON restore, dwell3s/max-run4h, independent devices and truthful mode readback.
5. After confirmed LED circuit/polarity/resistors, guarded LED-only output-enabled controller test. Demonstrate light/fan separately, then motion AUTO/vacancy/manual/AUTO and recovery. Relays/appliance loads remain unused/untested; do not equate LED demonstration with appliance switching.
6. Brief finite OFF/ON/unrelated-speech check using existing offline phone engine only after the physical/control baseline. Explicit confirmation/ACK/fresh report and no transcript persistence remain mandatory. First failed result is captured, not followed by uncontrolled retries/model experiments.
7. Package final actual installed APK/firmware hashes/confirmed wiring/demo instructions, report gaps. Cloud/extra rooms deferred. Never promote host tests or old observations to new physical success.

Preservation closure: source-preservation.json confirms all51 firmware source files unchanged and all83 Android build snapshot files still byte-identical. No new ESP image/model/control-command/phone recording in this milestone. Resistors explicitly reported as unknown values; identify by isolated ohmmeter reading (or readable colour bands) before connection. Sensor-header/ESP labels remain the immediate hardware blocker.

Resistor follow-up: user reports brown-black-blue-gold. Under the four-band code this is10×10^6Ω=10MΩ,±5%, conditional on accurate band reading/orientation (gold last), not an electrical measurement. Unsuitable for the intended visible LED indicators. Target two separate1kΩ series resistors (brown-black-red-gold); verify actual parts before wiring. User's later USB authorization acknowledgment requires no reinstall: install/hash verification already completed above. No new hardware probe, build, test, recording or actuation for this follow-up; outputs remain at last verified inhibited OFF/OFF state.
