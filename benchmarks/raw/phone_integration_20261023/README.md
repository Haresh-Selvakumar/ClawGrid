# Preserved phone integration evidence — 23 October 2026

These files are verbatim copies of selected existing review evidence, made for the source distribution. Their original timestamps/results were not regenerated as new tests. Original private review files remain in the working project. `evidence-manifest.json` records the copied bytes and SHA256 values.

`phone-voice-recognition-1.json` and `phone-voice-recognition-2.json` contain passive UART observations; `voice1-diagnostics.json` and `voice2-diagnostics.json` contain read-only actual firmware API snapshots. They are HARDWARE_MEASURED. Phone UI messages are user-reported in the separate `android_phone_voice_trial1_20261023.json` and trial2 aggregate one directory above. Both trials failed recognition; no speech proposal or confirmation occurred. Physical outputs remained inhibited; physical switching was not measured.

`android-request-id-build.log`, JVM test logs, APK signature log and client boundary JSON are HOST_MEASURED. They do not establish on-phone layout or recognition. The current master records exact commands, failures and limitations. No token, raw microphone recording, signing key, NVS or firmware image is included.
