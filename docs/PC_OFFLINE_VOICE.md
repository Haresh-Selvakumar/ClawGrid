# Continuous offline voice: the requested two-ESP layout

The active architecture was explicitly selected by the user:

```text
PC microphone and webpage
    -> offline Vosk recognition on the PC
    -> recognized text over USB to ESP B
    -> authenticated ESP-NOW to remote ESP A
    -> A's SafetyKernel
    -> GPIO4 light / GPIO5 fan interface
```

A and B are separate ESP32-S3 N16R8 boards. B remains next to the PC with a data
USB connection. A needs power at the remote location and must remain within radio
range. No router, internet connection or TX/RX wires between the ESPs are needed
for normal operation. The initial package/model setup needs a one-time download.
The active B image is now **native_intelligence**, with PC recognition; it is not
the earlier B ESP-SR speech image. A remains **native_voice** (a legacy build name
for the controller, not a requirement for a microphone on A).

## Open and use

Open **http://127.0.0.1:8770/voice**. The microphone starts only after the user
clicks Start listening and grants browser microphone permission.

1. Enter `api_token` privately from the local ignored `local_credentials.json`.
2. Select **Physical bedroom relays** for the real pair, or Virtual rooms for a
   GPIO-free test. Leave the Hall bench alias unchecked unless deliberately sharing
   the bedroom pair with Hall commands.
3. Click **Start listening**. Speak `bedroom light on`, pause briefly, then
   `bedroom lights off`. Fan commands use `bedroom fan on` / `bedroom fan off`.
4. The microphone stays open and sends bounded PCM chunks to the local recognizer.
   A completed recognized utterance automatically prepares and confirms its exact
   target on B. No separate confirmation click is required for each spoken command.
5. Click **Stop listening** to close the microphone and discard partial audio.

Audio is processed locally and is not saved by the service. There is no cloud
speech service or wake word. The webpage sends command text to B; raw audio does
not need to pass over the ESP serial/radio link. Browser state/token are kept in
tab memory. Changing the target/token, losing the controller, audio backlog or a
transport error stops/cancels listening work; commands are not replayed on reconnect.
Stop cannot recall a command already submitted to the controller.

Commands still obey A's 3 s minimum transition dwell and other SafetyKernel
checks. Allow at least 3 s between relay ON/OFF transitions. The ACK describes
software acceptance; physical contacts/load state are not independently measured.

**Physical outputs are currently inhibited.** PC recognition and the radio path
have been tested using virtual ON/OFF. The relay module input/interface and
electrical startup checks remain necessary before deploying A's output-enabled
bench image. See [wiring](PROGRAM_AND_WIRE.md).

## Where the code is

| File | Role |
|---|---|
| `dashboard/voice.html` | Focused voice-control webpage |
| `firmware/node_intelligence/src/dashboard_client.h` | Continuous microphone capture, local audio streaming, cancellation and automatic command submission |
| `scripts/offline_speech.py` | Offline PC model, command/negative vocabulary and sequenced streaming sessions |
| `scripts/browser_bridge.py` | Local web server and USB byte transport to B |
| `scripts/serial_gate.py` | Bounded USB scheduling; command/ACK requests precede background polls |
| `firmware/node_intelligence/src/main.cpp` | B's text parsing/proposal API and ESP-NOW transmitter/ACK handling |
| `firmware/node_voice/src/main.cpp` | Remote A's control tasks, PIR and SafetyKernel integration |
| `firmware/common/runtime/serial_text_command.h` | Whole-word room/device/ON-OFF parsing and explicit Hall bench alias |
| `firmware/common/safety_kernel/safety_kernel.c` | Deterministic relay policy/GPIO owner |

## Restart later

From this project in PowerShell:

```powershell
.\Start-Voice.ps1 -SerialPort COM7 -Port 8770
```

Use the current MAC-verified B port if Windows changes COM8. The launcher reuses
an already running local voice service; otherwise it starts it. Ctrl-C stops a
service launched in that terminal. Do not open another serial monitor on B's port
while the bridge is running. A can be monitored independently on its own port.

First setup on another PC:

```powershell
python -m venv .voice-env
.\.voice-env\Scripts\python.exe -m pip install -r requirements-voice.txt
.\.voice-env\Scripts\python.exe scripts/setup_offline_speech.py
```

Retain this pair's existing private provisioning; do not generate new keys just
to restart the service. The model loader uses an explicit local path and performs
no runtime model download. Package/model files are ignored by version control.

## Verification and limits

The browser tests use simulated audio callbacks, not a real microphone. They
cover automatic exact-target confirmation, duplicate final-result suppression,
Stop during permission/preparation, target changes, missing A, and bounded backlog.
Python tests cover session ownership/expiry, strict audio sequence, malformed PCM,
and full rejection of oversized utterances. The command vocabulary includes
explicit negations and unknown-word paths rather than forcing every sound into
an affirmative command.

The command-focused model passed **13 synthetic cases** through the actual
portable C++ parser: eight positive commands, three negative/unrelated phrases,
silence and noise. An unrestricted-model baseline was 10/13 and is retained.
These are synthetic fixtures, not human-speaker accuracy or a guarantee against
false activations. Human microphone/acoustic testing remains pending.

The live pipeline also passed virtual light ON and OFF with real B->A ESP-NOW
ACKs and matching virtual state, while physical GPIOs remained inhibited. The
recorded audio for that test was synthetic; the microphone was never opened.

References: [Vosk installation](https://alphacephei.com/vosk/install),
[official models](https://alphacephei.com/vosk/models),
[official streaming example](https://github.com/alphacep/vosk-api/blob/master/python/example/test_simple.py).

## USB queue timeout correction, 22 October

Background queue expiry now returns HTTP 429 with an explicit request-not-sent
message and Retry-After header. It does not claim a board disconnect and never
replays an expired command. Real USB response failures remain reported. Simulation
polls run once per second and pause while that page is hidden. These local page/
bridge changes require no ESP reflash. Eight targeted host tests and both frontend
checks passed. Read-only live verification: B COM7 -> A COM8 fresh authenticated
state, local offline recognizer ready/inactive, congested simulation request 429.
Evidence: `benchmarks/raw/hardware/usb_busy_fix_20261022.json`.

The user reports relay VCC on 3.3 V and IN directly on GPIO4/5. The relay
component marking is 5 V. This supply/input setup is not commissioned; disconnect
relay power and IN pending module input-interface verification. Do not simply
raise relay VCC to 5 V while retaining an unverified direct GPIO connection.
Physical outputs remain inhibited; no actuator test was performed.
