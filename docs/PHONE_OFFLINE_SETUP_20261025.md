# Native Android offline voice setup

**Current build:**1.0-phone-preview-20261025c is installed/hash verified, deliverables/ClawGrid-Phone-Preview-debug-20261025c.apk/SHA29c29570... . It adds foreground memory-only transcript preview/shared phrases/rejection reason. Actual parser/vocabulary audit agrees; user made informal repeated speech attempts and confirmed one voiceON, then cleanupOFF4 verified. Recognition consistency remains unresolved; no further recording is queued. See PHONE_TRANSCRIPT_PREVIEW_20261025.md. Historical setup/test receipts below are not a new test authorization.

**Current installed/test status:** 1.0-phone-offline-20261025b is adb-installed and pulled-hash verified38c89a14... . Original phone batch stopped after1attempt/no proposal. The separately approved one-shot diagnostic is also COMPLETE/STOPPED: UNSUPPORTED_WORDS/2words/displayed minimum confidence1.000; no proposal/command. Do not run the historical8trial instructions automatically or make another recording. See the implementation report for remaining blocker.

This is **on-phone recognition with embedded control**. Vosk runs as a native library in the existing Android app; gateway B receives only structured intents over local Wi-Fi. B/A still own confirmation validation, SafetyKernel, authenticated ESP-NOW, automation, boot/expiry checks, ACKs and authoritative reports. ESP speech experiments are closed and retained for rollback/evidence. Their model partition is untouched.

## Engine and device requirements

Pinned official Android demo dependencies: `com.alphacephei:vosk-android:0.3.75@aar` and `net.java.dev.jna:jna:5.18.1@aar`. API/model use Apache-2.0; JNA offers Apache-2.0/LGPL dual licensing (Apache option). License/attribution is bundled in `assets/offline-voice/NOTICE.txt`. Official sources checked 25 October 2026: [Android demo](https://github.com/alphacep/vosk-android-demo/blob/master/app/build.gradle), [Android integration](https://alphacephei.com/vosk/android), [models](https://alphacephei.com/vosk/models), [API license](https://github.com/alphacep/vosk-api/blob/master/COPYING), [JNA license](https://github.com/java-native-access/jna/blob/master/LICENSE).

The bundled small English 0.15 archive is **41,205,931 bytes**, extracting **70,898,967 bytes / 14 files**. SHA256 `30f26242c4eb449f948e42cb302dd7a686cb29a3423a8367f99ff41780942498`. Upstream lists about 300 MB runtime memory for small models; this is a vendor estimate, not this phone's measured RAM. The debug APK is about 100 MB. Allow at least 300 MB free storage for APK, extraction staging and Android overhead; actual installed size/RAM/startup time remain to be measured. API supports Android 21+, this app requires **Android 7/API24+**. APK contains ARM32, ARM64, x86 and x86_64 native libraries. Actual 64-bit ELF load alignment and APK 16 KB ZIP alignment were checked; this is not a compatibility observation on every device.

First use verifies/extracts the bundled archive into private `noBackupFilesDir` and loads it off the UI thread. No first-use network download. Model corruption or native/memory failure disables voice with an error; manual controls remain available. `SpeechRecognizer` and cloud/PC recognition are not used. The app retains Internet permission for existing local HTTP/cloud features; that permission is not proof of online speech. The voice implementation has no network calls, and the phone test below deliberately removes Internet connectivity.

## Build and install

Use Android Studio's bundled JDK21, SDK36.1/build-tools36.1.0, Gradle wrapper9.3.1. Release signing is unchanged. Existing local debug keystore/certificate is reused; install as an update, **do not uninstall or clear app data**.

PowerShell from the project root, if rebuilding source/model assets is needed:

```powershell
$py = 'C:\Users\hares\Documents\Codex\2026-10-21\i-h\work\clawgrid-env\Scripts\python.exe'
& $py scripts/prepare_phone_model.py
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = 'C:\Users\hares\AppData\Local\Android\Sdk'
$env:GRADLE_USER_HOME = (Resolve-Path .android-tools/gradle-cache)
Set-Location ecosystem/android
./gradlew.bat --no-daemon --console=plain --init-script ../../scripts/android_isolated_build.gradle -PclawgridBuildDirectory=phone-offline-build-20261025 :app:assembleDebug
```

Android Studio: open `ecosystem/android`, select the existing **app/debug** configuration, connect the phone with a data cable, enable Developer options → USB debugging, unlock and authorize this computer. Select that physical device and Run. This updates the same package. CLI alternative from project root:

```powershell
$adb = 'C:\Users\hares\AppData\Local\Android\Sdk\platform-tools\adb.exe'
& $adb devices -l
& $adb install -r 'C:\Users\hares\OneDrive\Desktop\ESD-imp\ClawGrid-physical-integration-prep\deliverables\ClawGrid-Phone-Offline-debug-20261025b.apk'
```

Sideload: transfer that exact APK using USB/File Explorer, open it on the phone, allow installs from that local file manager if prompted and choose Update. Verify **1.0-phone-offline-20261025b** in its PTT panel. A label/user confirmation is different evidence from an ADB-pulled APK hash. No model or Internet download is needed after installation.

## Local connection and historical frozen test (STOPPED)

Keep sensors, relays and loads disconnected and outputs **INHIBITED**. Turn mobile data off, disable VPN and automatic network switching, connect only to **ClawGrid-Assistant**, choose Stay connected / Use without Internet. In app Hardware → Safety, RTOS & Cloud set **http://192.168.4.1** and enter existing `api_token` privately from local configuration; Apply Gateway Connection. The token stays in memory and must be re-entered after app restart/update. `ap_password` is the Wi-Fi password, not the token. Never paste either value in chat. Gateway URL stays editable. Cloud status does not gate local control.

Do not record until the app shows a ready bundled model, fresh inhibited reports and a successful manual Bedroom light OFF proposal → explicit Confirm → matching ACK + fresh board report for this boot. Select **Bedroom** explicitly in the voice panel. Grant microphone While using the app. Unselected room must disable PTT; denial must leave manual controls usable.

Frozen plan: [phone-offline-v1.json](../experiments/phone-offline-v1.json). Eight PTT trials only, one at a time under observation:

1. LIGHT OFF → complete Bedroom/light/OFF proposal → **Cancel**. No command is sent.
2. LIGHT OFF → correct proposal → Confirm once → matching ACK + fresh OFF readback.
3. After at least three seconds dwell, LIGHT ON → correct proposal → Confirm once → matching ACK + fresh ON readback.
4. After dwell, LIGHT OFF → correct proposal → Confirm once → matching ACK + fresh OFF readback.
5. Two seconds silence → no proposal.
6. WHAT TIME IS IT → no proposal.
7. DO NOT TURN ON THE LIGHT → no proposal.
8. LIGHT ON AND LIGHT OFF → no proposal.

Hold PTT, wait for Speak now, speak, release; maximum four seconds. PTT recognizes locally, it never confirms. Inspect complete room/device/action before every confirmation. Stop at the first wrong target, rejection, timeout, stale/reboot/disconnect, negative proposal or engine error. Cancel a wrong proposal. Do not repeat/tune failed speech or lower the frozen 0.7 minimum word-confidence gate. If an ON was confirmed before a later failure, inspect real state and arrange one explicit manual OFF if necessary; no automatic replay. Matching ACK/readback proves reported logical switching with GPIOs inhibited, **not physical switching**. Eight passing trials would be a limited smoke test, not recognition accuracy evidence.

PCM/transcripts are not saved or uploaded by this path, even in debug. Structured target/capture/proposal/request IDs and actual ACK/readback may be retained as test evidence. Old ESP recordings/cache utility/source remain historical evidence and are not called by the active voice path.

## Gateway recovery

Only B's normal inhibited application was changed. A, NVS/credentials, model assets and partition layout are retained. Two verified private baseline backups remain available. Re-enumerate/MAC-check current ports before any deployment; never assume COM numbers. Close serial observers first.

Guarded **app-only** rollback, if specifically needed, from root (substitute freshly verified B port):

```powershell
& $py scripts/phone_offline_gateway.py rollback --port COM9
```

It verifies both backup copies and B's MAC/model/system, restores the entire original app0 including unused tail through the existing inhibited-image guard, then verifies app/model/system. It never writes model/NVS/table. Baseline APK remains separately retained; the new phone path will reject a gateway lacking the phone protocol. Rollback is prepared, not newly executed as a test here.

OneDrive build issue/recovery: follow-up Gradle initially failed52s because cloud reparse-point metadata was not seen as a regular file. The verified83file-identical snapshot at `C:/Users/hares/Documents/ClawGrid-Android-build-20261025b` built successfully4m37s using the exact existing debug key; original caches/source/evidence were not deleted. Follow-up APK100,096,584bytes/SHA25638c89a14fdc46914c1c71aaed058dd74e0bc13cc87b6a95e024f26971137fd86, version1.0-phone-offline-20261025b, subsequently installed and hash-verified for the approved one-shot diagnostic. That diagnostic is complete and stopped; these setup commands are documentation, not authorization to repeat it.

For this current source revision the byte-identical outside-OneDrive build is available at C:/Users/hares/Documents/ClawGrid-Android-build-20261025b/ecosystem/android. Its GRADLE_USER_HOME is C:/Users/hares/Documents/ClawGrid-Android-build-20261025b/gradle-user and isolated output property is phone-offline-outcomes-build-20261025. Use the verified deliverable rather than rebuilding unchanged code. Create a fresh reviewed snapshot if source changes; do not silently build an outdated snapshot.
