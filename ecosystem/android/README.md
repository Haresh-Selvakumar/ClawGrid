# ClawGrid native Android app

Existing Kotlin/Compose UI from `clawgrid.zip`; all five tabs/navigation retained. Hardware uses real board readback and separates pending requests; simulation remains local and isolated.

**Start here:** [Phone installation and local voice guide](PHONE_INSTALL_AND_LOCAL_VOICE.md) for Android Studio USB installation, APK sideloading, exact SDK/JDK/signing requirements and the direct ESP access-point bench sequence.

- Initial phone gateway: **http://192.168.4.1**, confirmed by the user's AP choice. The field stays editable for future private IPv4 networks.
- Local control and raw phone-audio submission use Wi-Fi only, without Internet or Supabase.
- First complete manual Bedroom light ON/OFF with matching ACK + advancing board readback; then phone push-to-talk unlocks for that boot.
- Audio is raw mono PCM16LE at 16 kHz, 0.1–4 seconds, sent once to the existing ESP `/api/speech/audio` endpoint. Recognition remains on B; every recognized command still needs explicit proposal confirmation and authoritative readback.
- SDK platform 36.1, Build Tools 36.1.0, JDK 21, wrapper Gradle 9.3.1, AGP 9.1.1, Kotlin 2.2.10/KSP 2.3.5. Standard debug build output: `app/build/outputs/apk/debug/app-debug.apk`. Existing local debug key is selected consistently; release signing is preserved.
- `scripts/check_android_control_jvm.py` (run from project root) covers control, PCM and voice proposal failures using cached JVM dependencies. `scripts/check_android_wire_jvm.py` must run after assembly. Full Gradle/Robolectric unit tests still require the previously missing native runtime dependency.

See [the master implementation](../../CLAWGRID_MASTER_INTEGRATION.md) for exact fresh results, APK hash and blockers. No phone/microphone/ESP physical result is claimed from a host build. Hardware AUTO/fan control, persistent policy changes, temperature transport and authenticated cloud synchronization remain separate work. No cloud key or `.env` import is used by the launcher.
