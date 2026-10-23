# USB-only paired bring-up — 21 October 2026

Current campaign: **20261021T092223Z**, HARDWARE_MEASURED diagnostics, HOST_MEASURED builds. Both updated inhibited roles deployed through `scripts/flash_native.py` with explicit ports and MAC checks. **17/17 bounded bring-up checks passed.** This supersedes the 20 October returned-pair report for the current deployment only; it does not repeat the old watchdog or speech accuracy campaigns.

| Port | MAC | Assigned role | Firmware identity | Boot result |
|---|---|---|---|---|
| COM7 | e0:72:a1:f6:a9:68 | Node A: controller/SafetyKernel | native_voice / clawgrid_native v1; binary SHA256 `990db651460832081e3fd2d086391c8ff5fa348f331f81c77a0674e64e9a5d55` | Normal USB reset boot; A self-test and RTOS ready; outputs inhibited |
| COM8 | e0:72:a1:f5:bc:b0 | Node B: API/speech | speech_intelligence / clawgrid_speech v1; binary SHA256 `79fda2a3000f63f09654f2278eb728113ce9a26ccc6b571276bab7dba099dcb9` | Normal USB reset boot; B self-test, HTTP/radio and MultiNet6 ready |

## Identity and preservation

Both boards report ESP32-S3 QFN56 revision v0.2, 240 MHz capability, 40 MHz crystal, native USB Serial/JTAG, embedded 8 MB AP_3v3 PSRAM and 16 MB flash (manufacturer 0x68, device 0x4018, quad, 3.3 V). Runtime memory checks agree with N16R8 configuration. Physical module markings/SKU cannot be established electronically. Security information was read only; no eFuse/security settings changed.

MACs match existing provisioning and the flasher's expected roles. Historical port defaults A=COM5/B=COM7 are stale; **use explicit A=COM7/B=COM8**. Roles were assigned by MAC, not by port number. No serial owner was found: the running port-8770 bridge is explicitly host-only and opens neither board. All bring-up serial handles are now closed.

The ZIP intentionally omitted credentials. The earlier development credentials did **not** match the installed boards, so those images were not used. Original constants were recovered from uniquely MAC-anchored application layouts; PMK/LMK/HMAC agree exactly across independent A/B images. B's original API token was verified against unchanged firmware (invalid token 401, recovered token 200), then verified again after deployment. No keys or passwords were regenerated. Local credentials/header were restored in ignored files, without overwriting any existing file. Private backups remain under ignored `benchmarks/backups/usb-bringup`; they contain secrets and are not copied into this public evidence set.

Partition bytes match the new builds exactly. Flashed artifacts exclude NVS `[0x9000,0xe000)`; no bulk erase or NVS erase was invoked. The saved HOME policy remains grace 120000 ms, light automation on, fan automation off. A boot counter advances from 13 before deployment to 15 after deployment and one normal capture reset. This is expected startup persistence, not byte-identical NVS. Boot logs show normal USB reset, app0 startup and successful policy loading.

## Communication results

- Both firmware targets built successfully with ESP-IDF 4.4.7; B retains pinned ESP-SR 1.7.0. Both identity-checked flashes returned 0 and esptool verified written hashes, including B's model partition.
- A and B role/self-tests and RTOS readiness passed. A boot and policy diagnostics explicitly report physical_outputs_enabled=false.
- B accepted authenticated **schema-3** room reports from A. The new decoder accepts schema 3 only; fresh state includes A's inhibited mode and independent physical_state="not measured". Four samples had report counts [55, 56, 58, 59] and ages [12, 512, 12, 737] ms; invalid/replayed counters remained zero in the capture.
- A confirmed **virtual bedroom light OFF** proposal traveled B→A→B and returned exact-command ACK status 0, phase 2, one attempt. This one observation reported 17274 µs RTT and 26 µs A processing; it is not a latency benchmark. Repeated confirmation returned 409. No physical-target command was submitted; physical commanded light/fan remained OFF.
- B initialized mn6_en, 16 command IDs, 16000 Hz, 512-sample frames, threshold 0.7. This verifies initialization only. No audio inference/accuracy evaluation ran.

## Commands and results

Interpreter: `C:\Users\hares\Documents\Codex\2026-10-21\i-h\work\clawgrid-env\Scripts\python.exe` (abbreviated `$python` below). The campaign scripts and full command arrays remain in `benchmarks/review/usb-bringup`; immutable copies of evidence are linked below.

| Command | Exit |
|---|---:|
| `$python benchmarks/review/usb-bringup/inspect_pair.py` — flash-id, get-security-info and read-flash backups for both explicit ports | 0 |
| `$python benchmarks/review/usb-bringup/recover_installed_credentials.py` — bounded recovery; no key generation | 0 |
| `$python -m esptool --chip esp32s3 --port COM7 run` and same on COM8 — unchanged firmware normal reset | 0 each |
| `$python benchmarks/review/usb-bringup/capture_pair.py --label before --seconds 18` | 0 |
| `$python scripts/provision_local.py` — existing recovered file used, no generation | 0 |
| `$python -m platformio run -d native -e native_voice` | 0 |
| `$python -m platformio run -d speech -e speech_intelligence` | 0 |
| `$python scripts/flash_native.py --role A --port COM7` | 0 |
| `$python scripts/flash_native.py --role B-speech --port COM8` | 0 |
| `$python benchmarks/review/usb-bringup/capture_pair.py --label updated-boot --seconds 22 --reset` | 0 |
| `$python benchmarks/review/usb-bringup/verify_pair.py` | 0; 17/17 |

Full build commands/logs, chip/security logs, boot logs, guards, read-only API responses, check rows and file hashes: [campaign evidence](../raw/hardware/usb_bringup_20261021T092223Z/manifest.json), [verification](../raw/hardware/usb_bringup_20261021T092223Z/verification.json). Flash provenance manifests remain in `benchmarks/raw/hardware/flash_*_20261021*.json`. No secret values are included in these evidence files.

## Stop boundary and blockers

No blocker remains for this USB-only milestone. Deliberate watchdog injection, recovery stress, physical GPIO enablement, PIR accuracy, relay polarity/startup electrical validation and real load switching were **not performed**. No claim of independent physical state is made. Useful real speech accuracy remains unverified; initialization does not overturn the historical 0/16 synthetic result. The opt-in bench build was not deployed. Both boards are left running inhibited firmware; both serial ports were released.
