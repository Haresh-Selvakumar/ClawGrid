# Code and manual upload

Project root: `C:\Users\hares\OneDrive\Desktop\ESD-imp\ClawGrid-physical-integration-prep`

- Remote A: `firmware/node_voice/src/main.cpp`; build environment `native_voice`.
- PC-connected B: `firmware/node_intelligence/src/main.cpp`; build environment `native_intelligence`.
- Active PlatformIO project: `native/platformio.ini` (native ESP-IDF).
- Entry point: `native/main/app_entry.cpp`; selects the role-specific source above.
- Shared code: `firmware/common/`. Keep the whole project together.
- Webpage: `dashboard/voice.html`.
- PC recognition: `scripts/offline_speech.py`.

These are multi-file PlatformIO ESP-IDF projects, not standalone Arduino .ino sketches.
Open the `native` folder in PlatformIO. Do not use the old root Arduino environments.
Do not upload the same board image to both ESPs.

Current user-confirmed wiring: **A (relays/PIR) is COM8**, MAC `e0:72:a1:f5:bc:b0`; **B (PC bridge) is COM7**, MAC `e0:72:a1:f6:a9:68`.

Both role assignments and compiled radio peers were updated together. Earlier successful hardware tests used the opposite physical board assignments and do not validate the new deployment until both boards are uploaded and checked.

## Use the existing compiled images

From the project root in PowerShell, close the running voice bridge/serial monitor
before flashing B (Ctrl+C in its terminal). Discover the current COM port.
The A command requires A temporarily connected by data USB; afterwards it can
return to remote power only. Existing programmed boards do not require reflashing
just because A is remote.

```powershell
$py = 'C:\Users\hares\Documents\Codex\2026-10-21\i-h\work\clawgrid-env\Scripts\python.exe'
$env:PLATFORMIO_CORE_DIR = 'C:\Users\hares\Documents\Codex\2026-10-21\i-h\work\pio-core'
# Replace these example COM ports with the currently detected ports.
& $py scripts/flash_native.py --role A --port COM8
& $py scripts/flash_native.py --role B --port COM7
```

The script checks each assigned MAC and preserves NVS. It uploads bootloader at
0x0, partitions at 0x8000, OTA selection at 0xe000, application at 0x10000.
Compiled artifacts are in `native/.pio/build/native_voice/` and
`native/.pio/build/native_intelligence/`. Do not upload firmware.bin at address 0.

To rebuild before flashing:

```powershell
& $py -m platformio run -d native -e native_voice
& $py -m platformio run -d native -e native_intelligence
```

## Run voice

```powershell
powershell -ExecutionPolicy Bypass -File .\Start-Voice.ps1 -SerialPort COM7
```

Open http://127.0.0.1:8770/voice, enter the private local API token and Start listening.
PC microphone -> offline PC recognizer -> USB B -> ESP-NOW A -> SafetyKernel.

**The normal A image inhibits physical relay outputs.** Real relay switching
requires verified input voltage/interface, polarity and startup behavior before
using an output-enabled bench build. Wiring: [PROGRAM_AND_WIRE.md](PROGRAM_AND_WIRE.md).
Use low-voltage DC loads only. GPIO4 is light, GPIO5 fan, GPIO6 PIR OUT.
SRD-05VDC-SL-C alone does not establish 3.3 V input compatibility.

Onboard radio/command indicator is now implemented: [LED pattern and upload](TRAFFIC_LED.md).
