# Current phone test result and updated APK — 23 October 2026 IST

The already installed native Android APK completed inhibited manual light ON/OFF with matching ACK and fresh readback. Its manual gate unlocked real phone audio capture/upload to ESP. **Both recognition-only phone trials failed with no confident detection and no proposal.** Job1 contained33,280 samples; job2 contained the full64,000 samples. Both completed ESP processing with no reported engine error and no accepted command. Voice confirmation/actuation has not been tested. Do not repeat speech or manual commands merely to obtain IDs. Current independent readback is light/fan OFF, last_command12, outputs inhibited. Loads remain disconnected; physical switching is untested. Supabase connectivity is optional and does not block this local path. Full observations/limitations are in [the master](../../CLAWGRID_MASTER_INTEGRATION.md).

An updated debug APK now makes **REQUEST ID #N** and phase prominent below the hardware banner, separately from proposal ID and fresh board-reported command/light/boot. Source compiled and debug signing verified; the updated APK has **not** been installed or layout-tested on the phone. The phone trials used the prior installed APK. New APK:

`C:\Users\hares\OneDrive\Desktop\ESD-imp\ClawGrid-physical-integration-prep\ecosystem\android\app\build\outputs\apk\debug\app-debug.apk`

Size18,570,202 bytes; SHA256`a44b9cc4dc3571c71b334b95049d4ab2221af10f1da8b04f6f1b13d1a4366119`. Same existing debug signer, release signing unchanged. Gradle build SUCCESS1m35s,42 focused control/voice JVM tests and3 wire JVM tests passed; APK boundary scan passed. Full Robolectric suite remains blocked by its previously uncached dependency; no full-suite pass is claimed.

Use Android Studio USB Run, `adb install -r`, or the APK sideload instructions below when ready to update. Do not uninstall or clear app data to obtain IDs. An update/process restart clears the in-memory token/manual gate: re-enter the existing api_token privately when needed; old manual verification is not replayed. Do not update solely to recover historical screenshot IDs. There is no new manual command required now. Further recognition tests should follow an identified pipeline diagnostic, not repeated identical utterances or lower thresholds.

Sections below retain original installation/setup instructions and historical preparation results. Their earlier “not yet phone-tested” statements describe that preparation, not the current two failed recognition trials. Audio stays in RAM and is cleared; no original phone recording was saved for independent acoustic/CRC comparison.

---

# Gateway commissioning note — 23 October 2026 IST

The current ESP gateway root `http://192.168.4.1/` is a **read-only hardware status page**, not the legacy simulated dashboard. It displays only actual `/api/state` and `/api/rtos` responses, marks unknown/stale/disconnected controller state, leaves temperature unavailable, and cannot issue controls. The explicitly labelled simulation lab is separate at `/simulation`. The native Android app remains the product UI and does not need rebuilding for this gateway page fix.

If Android Chrome changes the URL to `https://192.168.4.1`, use **Continue to site** on its HTTP warning and retain the explicit **http://** URL. Chrome Settings → Privacy and security → Always use secure connections controls these upgrades; where offered, the configuration that warns for public sites permits private HTTP sites. Do not change the app gateway URL to HTTPS: this gateway serves HTTP on port80. See [Google's Android Chrome guidance](https://support.google.com/chrome/answer/10468685?co=GENIE.Platform%3DAndroid&hl=en). No certificate, credential or provisioning regeneration is needed.

B may briefly drop the AP when its application is deliberately restarted. Rejoin ClawGrid-Assistant and choose Use without Internet. DHCP client addresses can change; use 192.168.4.1 for the gateway, not an old client address. Once the hardware page shows fresh state and outputs inhibited, resume the app's own manual OFF/confirmation/ACK/readback test before phone push-to-talk. Browser-page success alone does not unlock app voice. Physical switching remains untested.

---

# Install ClawGrid on your Android phone

Prepared 23 October 2026 (India time). Source/build and host tests are verified; installation, microphone capture and ESP recognition on your phone are **not yet hardware-tested**. No phone was modified and no ESP firmware/NVS or credentials were changed during preparation.

## Open and build in Android Studio

Open this folder, not the enclosing firmware folder:

`C:\Users\hares\OneDrive\Desktop\ESD-imp\ClawGrid-physical-integration-prep\ecosystem\android`

Use **JDK 21** (installed Android Studio `jbr`, verified 21.0.10) as the Gradle JDK. In SDK Manager, install **Android SDK Platform 36.1**, **Build Tools 36.1.0**, and **Platform Tools**. The phone itself needs Android **7.0/API 24 or newer**; targetSdk 36 does not require Android 16 on the phone. Wrapper Gradle 9.3.1, AGP 9.1.1, app Kotlin 2.2.10 and KSP 2.3.5 are pinned. Gradle's own bundled Kotlin version is separate.

The wrapper JAR matches Gradle's official SHA256 `b3a875ddc1f044746e1b1a55f645584505f4a10438c1afea9f15e92a7c42ec13`. The distribution checksum is pinned in `gradle/wrapper/gradle-wrapper.properties`; the existing distribution ZIP also matched its official SHA256. `gradlew.bat --version` was run successfully, followed by a debug build through that wrapper.

Select build variant **debug**. In this workspace, both Android Studio and CLI reuse the existing key:

`C:\Users\hares\OneDrive\Desktop\ESD-imp\ClawGrid-physical-integration-prep\.android-tools\android-user\debug.keystore`

The app build explicitly selects it when present, otherwise retaining Android's standard user debug key location. Debug certificate SHA256: `71:CE:1B:F6:F4:15:DB:7A:35:5E:7C:63:D9:41:C5:3E:7B:52:FB:A6:A4:C3:3C:25:04:25:82:AF:0A:46:4B:F9`. Do not delete/regenerate it to solve an install error. Release signing configuration, keystore selection/environment variables and release assignment remain unchanged; no release build was performed.

Complete Gradle sync/build while dependencies are available, before connecting to the ESP's Internet-free AP. No `.env`, Gemini key, Supabase key or cloud speech account is needed. From PowerShell:

```powershell
Set-Location 'C:\Users\hares\OneDrive\Desktop\ESD-imp\ClawGrid-physical-integration-prep\ecosystem\android'
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:GRADLE_USER_HOME = (Resolve-Path '..\..\.android-tools\gradle-cache').Path
$env:ANDROID_USER_HOME = (Resolve-Path '..\..\.android-tools\android-user').Path
.\gradlew.bat --offline :app:assembleDebug :app:signingReport --console=plain
```

The initial wrapper bootstrap needs network access if its distribution is not cached. With ordinary new Android Studio caches, omit `--offline` for the first dependency download. The separate full Gradle unit-test task still needs an uncached Robolectric native runtime; that does not block `assembleDebug`.

APK output:

`C:\Users\hares\OneDrive\Desktop\ESD-imp\ClawGrid-physical-integration-prep\ecosystem\android\app\build\outputs\apk\debug\app-debug.apk`

## USB installation through Android Studio

1. On the phone, enable Developer options (usually tap Build number seven times in About phone), then **USB debugging**.
2. Connect a USB data cable, unlock the phone and approve the computer's USB-debugging RSA prompt. Install the phone manufacturer's Windows ADB driver if needed. The ESP COM port is not an Android deployment target.
3. In Android Studio select the `app` configuration, **debug** build variant and your physical phone in the device selector. Click **Run**. This builds and installs the Android app; it does not flash the ESP boards.
4. Open ClawGrid. Connect to the gateway Wi-Fi and follow the local test below. Grant microphone permission only when using the explicit microphone permission button.

Optional CLI equivalent (select your phone's ADB serial when more than one device is listed):

```powershell
$adb = 'C:\Users\hares\AppData\Local\Android\Sdk\platform-tools\adb.exe'
& $adb devices
& $adb -s PHONE_SERIAL install -r 'C:\Users\hares\OneDrive\Desktop\ESD-imp\ClawGrid-physical-integration-prep\ecosystem\android\app\build\outputs\apk\debug\app-debug.apk'
```

If the device is `unauthorized`, unlock it and approve the RSA prompt. If updates fail with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, the installed copy has a different signer: use its original signing identity or deliberately export data and decide whether to remove that copy. Do not automatically uninstall, reset phone data or replace release keys. If the APK is incompatible, check Android >=7.0 and the exact installer/ADB error first.

## APK sideloading without Android Studio

Copy the APK above to the phone using USB File Transfer or another transfer method you control. Open it in the phone's file manager and allow that file manager to **Install unknown apps / Allow from this source** when prompted (Android 7 uses its older Unknown sources setting). Tap Install, then Open. You can turn that file manager's install permission off afterward. No USB debugging is needed for this route. A same-package/different-signer copy can still prevent an update; follow the signer guidance above.

## Direct ESP Wi-Fi test — no Internet or Supabase

The user selected the ESP32 gateway access point for initial testing. The app is configured with **`http://192.168.4.1`**, the maintained firmware's AP address. **Gateway URL remains editable** in Safety, RTOS & Cloud for future local IPv4 networks/hotspots. A URL edit does not provision the ESP onto a router.

1. Power the existing A room controller and B gateway in their verified normal/inhibited deployment. This guide does not authorize a flash, NVS erase, credential regeneration, or enabling relay outputs.
2. On the phone join the existing ClawGrid gateway Wi-Fi (documented SSID `ClawGrid-Assistant`; use the current configured SSID/password). Use the preserved credentials; do not regenerate them. Choose **Stay connected / Use this network without Internet**. Disable the phone's automatic switch away from this AP if necessary.
3. Open ClawGrid → Hardware → Safety, RTOS & Cloud. Keep `http://192.168.4.1`, enter the **existing gateway API token**, then Apply Gateway Connection. Token and pending requests remain in memory, so process restart requires re-entry. Local requests are explicitly bound to the Wi-Fi network, including an AP without validated Internet; there is no cellular or Supabase fallback.
4. Wait for known/authenticated Bedroom state with an advancing source sample. Verify board boot, real age, board-reported light/fan and output inhibition. A disconnected room must remain UNKNOWN. The laptop was on home Wi-Fi during preparation and its read-only gateway probe timed out; this is **not** evidence that the phone has already reached the AP.
5. First complete manual light OFF (or ON only with verified output inhibition), explicit proposal confirmation, matching ACK and advancing same-boot `last_command` readback. Respect the firmware's three-second dwell. Physical load state remains unmeasured. **Phone voice is gated until this manual verification succeeds on the current controller boot.** Reboot or applying a new connection clears that gate.
6. After manual verification, tap Allow microphone, then **hold** the push-to-talk control on Rooms & Relays or Home Overview. Say **TURN ON BEDROOM LIGHT** or **TURN OFF BEDROOM LIGHT**. Release to submit; leaving the app/tab, a cancelled gesture or Cancel voice discards pending voice work. Capture stops at four seconds; if still holding, release sends that capped clip. Audio stays in RAM and is never saved/logged.
7. The app records raw audio directly with AudioRecord. It requests 16 kHz mono PCM16, falls back to actual 48/44.1 kHz mono capture if needed, and area-averages to **16 kHz signed PCM16 little-endian**, clipping/rounding and bounding to 64,000 samples. It never launches Android SpeechRecognizer, a browser recognizer, cloud speech or the PC Vosk endpoint.
8. It sends a single `POST /api/speech/audio` with `Content-Type: application/octet-stream`, existing token and **3,200–128,000 raw bytes** (even count, 0.1–4 seconds). There is no WAV header, AAC, MP3, JSON/base64 audio, streaming framing or automatic upload retry.
9. Recognition runs on B's existing **ESP-SR 1.7.0 MultiNet6 English** build. The app checks `GET /api/speech` reports supported=true before upload. An installed non-speech `native_intelligence` build/model failure cannot recognize phone audio; app code must report unavailable. Deployment/model correction is a separate firmware task, not a reason to bypass checks or flash from these instructions.
10. Audio HTTP 202 means a job was queued. Polling requires the exact job, supported hardware source, finished phase, zero engine errors, confidence >=0.7, matching sample count and PCM CRC32. Only command IDs 1/2 (Bedroom light ON/OFF) are integrated here. Other rooms/fan/no-command/errors never become physical light proposals.
11. A recognized result produces `speech physical_bedroom bedroom light ON|OFF <job>` at `/api/proposal`. **Review the target and explicitly Confirm** in the existing dialog. The firmware rechecks the speech job, target, proposal lease, controller boot/freshness and SafetyKernel. HTTP acceptance still does not mean switched; matching ACK plus advancing board report is required. No old audio, proposal or ON request is replayed on reconnect.

Do not configure localhost/127.0.0.1 or the emulator alias 10.0.2.2 as a laptop/ESP address on your phone. The app now rejects those values and public hostnames; enter the actual private Wi-Fi IPv4 origin instead. The unchanged PC bridge remains loopback-only and is not required for this direct-AP phone path.

## AP channel and ESP-NOW coexistence

Source inspection confirms **channel 1** for all three: shared `CLAWGRID_WIFI_CHANNEL`, gateway `WiFi.softAP(..., CLAWGRID_WIFI_CHANNEL, ...)`, and room A `esp_wifi_set_channel(CLAWGRID_WIFI_CHANNEL, ...)`; encrypted ESP-NOW peer entries also use that constant. The native HAL passes the AP channel into `wifi_config_t.ap.channel`. Neither role calls `WiFi.begin` to join a router. Phone association to this AP does not change these firmware settings. No radio/channel firmware was edited.

This establishes compatible source configuration, not a new RF measurement. With the phone associated, verify advancing authenticated room reports and manual ACK/readback before recording. Gateway AP is configured for at most two Wi-Fi clients. Future gateway router-STA/home-hotspot integration must keep its actual Wi-Fi channel compatible with the room peers (or implement coordinated channel changes); changing only the phone URL cannot do that.

## Validation limits and troubleshooting

This preparation includes an APK build/signature check and host tests for manual gating, exact speech job/CRC/target checks, wrong room/fan, model failure, low confidence, cancellation, stale/reboot handling, PCM bounds/byte order/resampling and no retries. It does **not** establish phone microphone quality, acoustic accuracy, radio coexistence under speech load, physical relay movement or on-phone layout. Historical recognition failures remain in the existing voice evidence; no threshold was lowered.

If state stays UNKNOWN, check that the phone stayed on the ClawGrid AP, URL/token match, both verified boards are powered and current reports advance. If speech is unavailable, inspect `supported`, phase and error_code on B; do not treat typed commands or synthetic test fixtures as recognition. An ACK timeout remains outcome unknown: read fresh state and make a new explicit request instead of replaying ON. Temperature transport and manual-until-AUTO policy remain separate blockers.

Official references: [AudioRecord](https://developer.android.com/reference/android/media/AudioRecord), [Android Studio USB deployment](https://developer.android.com/studio/run/device), [local network permissions](https://developer.android.com/privacy-and-security/local-network-permission). This app still targets SDK 36; default local access uses its INTERNET permission, and microphone access is requested at runtime. No opt-in local-network restriction or target-SDK migration was introduced.
