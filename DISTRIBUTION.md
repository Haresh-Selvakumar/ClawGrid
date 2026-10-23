# ClawGrid source snapshot — 23 October 2026, India time

This snapshot continues the existing [Haresh-Selvakumar/ClawGrid](https://github.com/Haresh-Selvakumar/ClawGrid) repository and preserves its history. It includes the native Android project, firmware and guarded tooling, actual benchmark records, master implementation and handoff. Existing remote-only files are retained. Read the current master before using older documentation or images.

The Android product UI, local Wi-Fi control and phone-microphone → ESP recognition path are retained. Manual light ON/OFF passed inhibited proposal/confirmation/ACK/readback testing. Two real phone recognition attempts failed with no detection; voice confirmation and physical switching are untested. Publishing these sources does not claim a successful voice recognizer or change physical outputs.

## Downloads copy

The requested local Git checkout is `C:\Users\hares\Downloads\ClawGrid-integration-2026-10-23`. Its portable ZIP is `C:\Users\hares\Downloads\ClawGrid-integration-2026-10-23.zip`. A matching debug install package is included locally under `local-artifacts/ClawGrid-debug-2026-10-23.apk`; it is excluded from Git. The local distribution receipt records the resulting commit and file hashes. The source ZIP omits `.git` history; the Downloads checkout retains that history.

The APK SHA256 is `a44b9cc4dc3571c71b334b95049d4ab2221af10f1da8b04f6f1b13d1a4366119`, size18,570,202 bytes. It uses the existing debug signer. Gradle debug build, 42 focused control/voice JVM tests, 3 wire JVM tests, APK signature check and configured-private-value boundary scan passed. This APK is not yet installed or layout-tested on the phone. See the [installation instructions](ecosystem/android/PHONE_INSTALL_AND_LOCAL_VOICE.md) for Android Studio USB Run and APK sideloading. Updating/restarting the app clears its in-memory token/manual-verification gate; no previous ON command is replayed.

## Private and generated files

The public Git snapshot and portable ZIP exclude existing credentials, private firmware configuration, cloud service keys, signing keystores, full-flash/NVS backups, credential-bearing firmware binaries, build/dependency caches, and scratch review files. Those remain unchanged in the original workspace. The local APK is scanned separately before inclusion. No signing key, password or API token is published. The existing `.gitignore` is extended to protect install archives, signing identities and local artifacts.

Do not regenerate credentials to make an extracted copy deployable. For hardware work, continue from the original provisioned workspace or restore its existing private files privately. Fetched dependencies/models and SDK caches must be restored through the project's pinned workflows; this source snapshot is not a complete offline toolchain backup. Release signing is unchanged. No hardware flashing, database mutation, output enabling or phone installation is part of this publication.

Selected phone UART/API evidence and build/test/signature logs are copied verbatim into `benchmarks/raw/phone_integration_20261023/` after a bounded credential scan. Original review files and earlier verified evidence remain in place. The scan checks exact configured private values, private key/token patterns, privileged JWT payloads and archive contents; it is not a comprehensive security audit. Git/source consistency and ZIP integrity are checked for this distribution. Tests and hardware observations retain their original host-versus-hardware classification.

Recognition no-detection, the five-minute manual hold versus manual-until-AUTO requirement, temperature transport, the full Gradle test dependency, and new APK on-phone layout remain blockers. The next engineering step remains diagnosis against the pinned ESP-SR1.7.0 reference pipeline without weakening thresholds or confirmation.
