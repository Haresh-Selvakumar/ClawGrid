# Text commands from the supplied two-board sketch

The supplied sketch's text commands are integrated into the maintained ClawGrid
firmware alongside the dashboard and ESP-SR speech engine. The robotic-arm paste
is out of scope; no arm project or firmware was created.

Node B parses text into an immutable proposal. Confirmation submits it over the
existing authenticated ESP-NOW transaction path. Node A remains the only physical
controller and applies SafetyKernel policy. No GPIO writes occur in the text
parser or Wi-Fi receive callback. No credentials or radio pairing were changed.

## Use in the dashboard

1. Start the USB bridge on B's current port and open `/simulation`.
2. Enter the control token privately, then select **Virtual rooms** or
   **Physical bedroom** under Control target.
3. Type a command in Assistant and click **Prepare text command**.
4. Inspect the resolved target, room, device and action. Click
   **Confirm manual proposal** to submit it. A proposal alone does not actuate.

Examples: `turn on bedroom light`, `bed room lamp off`, `hall fan on`,
`living room pan of`, `all fun onn`.

| Text | Meaning |
|---|---|
| bedroom / bed room | Bedroom |
| hall / living / living room | Hall, mapped to virtual living room by default |
| all | Acoustic text fallback for **hall**, not an all-room scene |
| light / lights / lamp / bulb | Light |
| fan / pan / fun | Fan |
| on / onn | ON |
| off / of | OFF |

Matching is case-insensitive and uses complete words. Common fillers such as
`please`, `turn`, `switch`, `the`, and `in` are supported. Unknown words,
negations, multiple rooms/devices/states, binary input and oversized payloads
are rejected. Thus `hall light only` cannot accidentally mean ON, and
`do not turn bedroom light on` cannot actuate.

## Shared relay-pair bench mode

The supplied `SINGLE_BENCH_RELAY=true` behavior is available by selecting
**Physical bedroom** and checking **Bench: hall uses the bedroom relay pair**.
Hall and bedroom then deliberately address the same two physical slots. The
proposal reports `requested_room`, the actual `room`, and `bench_alias` so this
alias is visible before confirmation. This does not represent two physical rooms
or require a two-channel PCB: the user's two separate one-channel modules work
as the one shared pair.

With that box unchecked, a Hall command cannot address the physical bedroom.
Use Virtual rooms for Hall/living-room controls. Node B does not gain relay
outputs, and the PIR still covers only A's assigned bedroom zone.

## API / USB transport

Authenticated `POST /api/text` accepts 1–127 ASCII bytes including the scope:

```text
physical_bedroom turn on bedroom light
virtual_rooms hall pan of
bench_pair all fun on
```

The response contains a proposal ID, exact target/room/device/action,
`acoustic_fallback`, `bench_alias`, and a 15 s expiry. It sets `authorized=false`.
Use the existing authenticated `POST /api/confirm` with that exact returned target:

```text
<proposal_id> physical_bedroom bedroom fan ON
```

`/api/transaction` reports the software ACK or unknown outcome on timeout. Both
prepare and confirm require fresh controller telemetry. A missing/stale A returns
409; invalid text returns 400; missing/incorrect authentication returns 401.
The USB browser bridge transports these requests to B, where parsing runs.

This text parser does not perform speech-to-text or change ESP-SR recognition
accuracy. The submitted sketch itself only received already-transcribed text and
logged PIR edges; its comments did not implement speech recognition or a safety
kernel. The maintained speech/PIR/SafetyKernel behavior remains in place.

## Wiring and deployment

Connections are unchanged: A GPIO4 = light, GPIO5 = fan, GPIO6 = HW-416B OUT.
Use the [full wiring guide](ESP_CONNECTIONS.md) for power, interface checks and
low-voltage DC load contacts. Actual image mode, not text-parser availability,
determines whether GPIO4/5 are enabled. Outputs remain inhibited until the
relay-module interface and electrical startup behavior are verified.

Build B with `python -m platformio run -d speech -e speech_intelligence`, then use
the MAC-guarded app-only flasher on B's observed port. A's controller wire protocol
is unchanged, so it does not require a new image for the text adapter. Its pending
physical-integration update and USB blocker are tracked in
[current integration status](CURRENT_INTEGRATION.md).
