# Integration status — 22 October 2026, IST

**Latest ecosystem integration (22 October 2026):** Supabase encrypted Broadcast now carries real A reports and measured B diagnostics to the shared live webpage and Android source. Independent public-key client verification passed virtual ON/OFF through Supabase → B → A, with matching ACK and state. Physical GPIOs remain inhibited; missing temperature/lux and A RTOS/heap are unavailable. See [ecosystem integration and exact field mapping](ECOSYSTEM_INTEGRATION.md). Current B USB is unavailable (Windows device error 31); the gateway reports UNKNOWN until that connection recovers. Earlier dated deployment notes below are retained history.

**Onboard traffic LED update:** New native A/B sources and images add RGB traffic,
command reception and matched application ACK pulses. Default RGB pin is GPIO48
(GPIO38 on official DevKitC v1.1). These new images have not been flashed by the
agent; user uploads both. Physical relay GPIOs remain inhibited. Earlier live
radio measurements describe the previous installed firmware. See [LED/upload
instructions](TRAFFIC_LED.md).

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
connected to the PC. [Code/upload paths](UPLOAD_CODE.md) ·
[PC voice operation](PC_OFFLINE_VOICE.md).

Verification: 205 host tests passed; both frontend checks passed; 13/13 synthetic
recognizer/parser cases passed. Evidence:
`benchmarks/raw/hardware/pc_voice_end_to_end_20261022T051049Z.json`.
The entries below are earlier commissioning history.


Both boards have now been programmed. A is runtime-verified on COM7; B's updated
firmware was verified earlier, but its USB application transport needs recovery
after reconnection. Physical relay/load tests have not run. Use
[programming, wiring and the supplied validation checklist](PROGRAM_AND_WIRE.md)
for the bench hookup.

## Latest: A programmed and flash layout corrected

A returned on native USB COM7 with MAC `e0:72:a1:f6:a9:68`. Its first app-only
flash succeeded, but the runtime reported 4 MB flash and failed the memory-config
check. A ROM flash-ID read verified the physical chip is 16 MB. A readback showed
an old 4 MB bootloader header and old 4 MB partition table, so app-only flashing
could not repair the mismatch. Both layouts place NVS at `0x9000..0xe000`.

The complete 16 MB installed flash was backed up locally/ignored, then the guarded
flasher installed A's prepared bootloader, partition table, initial OTA selection
and application. The manifest's write ranges exclude NVS. A now passes its
self-test, RTOS readiness, advancing control/link job counters and memory config:
16 MB flash and 8 MB PSRAM. The saved HOME policy, 120000 ms grace and auto flags
1 (light ON, fan OFF) survived. Relay outputs remain inhibited and diagnostic
fault hooks disabled.

Evidence: [A full flash manifest](../benchmarks/raw/hardware/flash_native_voice_20261022T040151Z.json),
[A commissioning / pending paired checks](../benchmarks/raw/hardware/commission_pair_20261022.json).
The initial failing 4 MB runtime capture is retained as
`benchmarks/raw/hardware/commission_a_20261022.json`; do not label it a pass.
The old flash backup stays in ignored `benchmarks/backups/boot_check_20261022/`.

B reappeared on COM8 with its assigned MAC, but its USB application data timed
out after reconnect. A ROM identity/reset succeeded; the subsequent capture
`20261022T040356Z.json` contained no telemetry and failed verification. A physical
unplug/replug and RESET (BOOT released) was requested. The latest bridge launcher
14308 was stopped for this investigation; earlier launcher PIDs below are history.
Paired state/command verification is pending recovery of B's USB application path.

## Earlier session history

**Subsequent reboot attempt:** B's MAC was reverified on COM8 and esptool performed
a hard reset without writing flash. Its serial self-test and memory checks passed
again; see [reboot evidence](../benchmarks/raw/hardware/reboot_20261022.json) and
[serial capture](../benchmarks/raw/hardware/20261022T033602Z.json). A still has no
serial port and the failed USB device reports Code 43. A Windows device-restart
attempt returned Access is denied. A could not be remotely reset. The dashboard
bridge was restored at port 8770; latest launcher PID at creation is **12376**,
superseding the older PIDs below.

## Latest update: supplied text-command sketch

The user confirmed adding the supplied sketch's text interface alongside the
existing firmware. Its room/device/state aliases now run on B through authenticated
`POST /api/text`, proposal preview, explicit confirmation and A's existing
SafetyKernel transaction path. Whole-word matching rejects ambiguous, negative,
binary and substring-only input. Hall can address the shared physical pair only
through the explicit `bench_pair` scope/dashboard checkbox; normal Hall routing is
virtual living room. See [text command usage](SERIAL_TEXT_COMMANDS.md).

**HOST_MEASURED:** 196 tests passed in 26.45 s, including 29 new text-command tests.
Frontend syntax and text-target confirmation checks passed. B's updated image
built and was app-only flashed successfully on COM8. New application SHA256:
`acdb1aaf1489da864d14db9e3377ca8ff4face972ebe5d9d349419de0c5199ec`.
The previous B image/hash and its telemetry below are retained historical evidence.

**HARDWARE_MEASURED:** the new endpoint returned 401 without a token, 400 for
malformed/negative/ambiguous text and invalid physical Hall routing, and 409 for
valid prepare requests with A unavailable. No commands were confirmed. B's memory
configuration, speech-model readiness, 23 synthetic shadow cases and increasing
LinkTask job counters passed. Positive text-to-actuation paths are host-tested;
paired target execution and physical relay behavior are still unverified.

Latest evidence:
[host report](../benchmarks/raw/host_tests_text.xml),
[B flash manifest](../benchmarks/raw/hardware/flash_speech_intelligence_20261022T033051Z.json),
[live endpoint checks](../benchmarks/raw/hardware/text_api_20261022.json).
Build/test logs: `build-speech-text.log`, `test-text.log`. The USB bridge was
restarted on the same URL/COM8; current launcher PID at creation is **24864**
(superseding 11220 below). A remains unavailable and unflashed; outputs remain
inhibited. The arm paste was discarded and no arm firmware/project was created.

Current workspace: `C:\Users\hares\OneDrive\Desktop\ESD-imp\ClawGrid-physical-integration-prep`.
The source was extracted from the supplied ZIP. Existing paired credentials and
the pinned ESP-SR dependency files were recovered from the previous local project;
no credentials were generated or rotated. No NVS erase was performed.

## Completed

- **HOST_MEASURED:** 167 tests passed in 29.33 s; frontend checks passed.
  The first sandbox run could not invoke Zig correctly. The first permitted run
  passed 166 tests with one cold compiler-cache setup timeout; the full rerun passed.
- Fresh builds passed: A `native_voice`, A `native_voice_uart`, and B
  `speech_intelligence`. Actual image output/fault markers and SDK/console settings
  were checked: all are **output-inhibited, diagnostic faults disabled**.
- **HARDWARE_MEASURED:** B's MAC was verified as `e0:72:a1:f5:bc:b0` on COM8.
  The guarded flasher successfully rewrote app0 only at `0x10000`, preserving
  the installed boot/partition/model images and NVS.
- B's runtime self-test and memory configuration passed. A 12 s capture detected
  16 MB flash and 8 MB PSRAM. Three memory samples had minimum observed internal
  free heap 107679 B and largest block 59392 B. These are bounded observations,
  not long-run stability results.
- B's authenticated state/memory/RTOS/speech/evidence APIs returned HTTP 200.
  Its 23 shadow monitor cases passed on synthetic inputs. The speech model reports
  initialized/ready; no fresh recognition-accuracy result is claimed.
- B correctly reports the bedroom state UNKNOWN with A unavailable.
- [Wiring and first-run guide](ESP_CONNECTIONS.md) added for the identified
  HW-416B PIR, the two relay modules and DC load contacts.

## USB blocker and next action

A initially appeared on native USB as COM7 with serial identity
`E0:72:A1:F6:A9:68`. Opening it failed with Windows device/baud configuration errors.
After reconnect attempts, A had no COM port and Windows listed an Unknown USB
Device, Device Descriptor Request Failed, Code 43. B's connection worked.
The user cannot change the cable now. A's current running image/state could not
be verified. No new application was written to A.

Next: use a known-working data cable directly to A's **UART/COM (CH343)** connector.
Enumerate the new COM port and verify A's MAC `e0:72:a1:f6:a9:68`; do not infer
identity from a COM number. With relay power/loads disconnected, deploy the
prepared `native_voice_uart` image using the guarded flasher:

```powershell
# Use the detected, MAC-verified UART port in place of COM9:
python scripts/flash_native.py --role A --port COM9 --uart-console --app-only
```

The local pinned Python is
`C:\Users\hares\Documents\Codex\2026-10-21\i-h\work\clawgrid-env\Scripts\python.exe`.
The system Python does not contain the complete toolchain. If rebuilding, use
that Python and the preserved PlatformIO core directory
`C:\Users\hares\Documents\Codex\2026-10-21\i-h\work\pio-core`.

Then verify A's `t`, `P` and repeated `q` responses, increasing control/link job
counters, inhibited GPIO mode, PIR warm-up and advancing authenticated state on B.
Do not run the legacy resume script with its hard-coded historical COM5/COM7 ports.

## Dashboard left running

The local USB bridge uses B COM8 at
[http://127.0.0.1:8770/simulation](http://127.0.0.1:8770/simulation).
Launcher process ID at creation: 11220. Logs: `bridge-current.log` and
`bridge-current-error.log`. Enter the API token privately from the ignored
`local_credentials.json`. The bridge does not inject it or perform inference.
Close the bridge before opening B with a serial monitor/flasher.

Restart if needed:

```powershell
python scripts/browser_bridge.py --serial COM8 --port 8770
```

## Physical integration still pending

The user confirmed relay power disconnected. The PIR is HW-416B, nominally
5 V supply and 3.3 V OUT; its labeled terminal order/output still needs physical
verification. The relay marking `SRD-05VDC-SL-C` identifies the 5 V coil component,
not the module's IN interface. Direct GPIO compatibility, polarity and electrical
OFF bias/reset behavior remain unverified. GPIO4/5 remain inhibited in every
image built/deployed here. No bench image was flashed. Low-voltage DC only.

After the interface checks, follow the guide's explicit bench build/flash path
and test contacts unloaded before connecting a lamp or fan. B disappearing does
not force A OFF; A's documented local policy continues. One PIR covers one zone.

## Evidence

- [Machine-readable session record and image hashes](../benchmarks/raw/integration_20261022.json)
- [Host test report](../benchmarks/raw/host_tests_current_verified.xml)
- [B flash manifest](../benchmarks/raw/hardware/flash_speech_intelligence_20261022T031226Z.json)
- [B serial telemetry](../benchmarks/raw/hardware/20261022T031308Z.json)
- [B API snapshot](../benchmarks/raw/hardware/api_initial_20261022.json)

Build logs are `build-native.log`, `build-native-uart.log`, `build-speech.log`;
test logs include `test-current.log`, `test-current-full.log` and
`test-current-verified.log`. Keep binaries and paired credentials local/ignored.
