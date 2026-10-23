# ClawGrid embedded home control and research prototype

ClawGrid uses two ESP32-S3 N16R8 boards with native ESP-IDF/FreeRTOS. A owns deterministic control and SafetyKernel; B owns the browser API and ESP-SR speech model. Their memories are separate. The four-room house interface supplies explicitly synthetic sensors; output policy and recognition run on MCUs.

**Current software work, 21 October 2026:** no hardware is available. Default outputs remain inhibited. Confirmed physical-bedroom/virtual routing, portable PCM handling, explicit host development mode, and dataset provenance repairs are implemented. Real recognition and physical behavior require fresh target validation. The newest retained hardware evidence is the returned-pair campaign on 20 October, not the older absent-A descriptions.

Start the host-only development dashboard after installing `requirements-dev.txt`:

```powershell
python scripts/browser_bridge.py --host-dev --port 8770
```

Open http://127.0.0.1:8770/simulation. Use the persistent simulation banner for disconnect/rejection/timeout and labeled speech fixtures. No recognition engine or hardware measurement runs on the host. See [the current software handoff](NEXT_CHAT_HANDOFF.md) for exact validation, remaining checks, and the next three hardware actions.

## Current work

The new deep-review response implements a fixed-memory evidence monitor, fault tests and energy-trace analysis. The monitor is shadow-only and never drives GPIO. Its research candidate is a measured study of fault observability and freshness/availability, with explicit limits; novelty is not established.

- [Response to the supplied analysis](docs/DEEP_REVIEW_RESPONSE.md)
- [30-issue resolution table](benchmarks/reports/DEEP_REVIEW_RESOLUTION.csv)
- [Novelty assessment and experiments](docs/NOVELTY_CASE.md)
- [Monitor contract](docs/EVIDENCE_MONITOR_SPEC.md)
- [Current validation summary](benchmarks/reports/PROJECT_VALIDATION_SUMMARY.md)
- [Self-contained next-chat handoff](NEXT_CHAT_HANDOFF.md)

## Retained hardware and earlier host evidence

| Scope | Result | Limit |
|---|---|---|
| Current host tests | 83 passed | HOST_MEASURED, actual C/C++ and analysis/preview tests |
| Current B API/shadow checks | 32/32 passed | MCU execution on synthetic feedback; no actuation |
| Host fault matrix | 4096 designed conditions | Zero necessary-contract violations; conservative refusals and jointly lying-sensor counterexamples retained |
| Historical paired controller | 81/81 rows;1000/1000 commands | Earlier image hashes; latest pair needs regression |
| Current synthetic speech | 0/16 command matches;0/2 negative false accepts | Functional failure; no human accuracy claim |

## Browser

The offline [layout preview](http://127.0.0.1:8765/simulation) reports missing processing. The [live B interface](http://127.0.0.1:8766/simulation) transports API/PCM through USB; enter the local token privately. A-dependent control is unavailable until A returns. No phone changes or browser/cloud speech recognition are used. The byte bridge performs no automation or inference.

```powershell
python -m pytest tests -q --junitxml=benchmarks/raw/host_tests.xml
python scripts/evidence_matrix.py
python scripts/single_board_hil.py --url http://127.0.0.1:8766
python scripts/audio_bench.py --speech-only --url http://127.0.0.1:8766
```

## Build and resume

```powershell
python -m pip install -r requirements-dev.txt
python scripts/provision_local.py
python scripts/fetch_speech_dependencies.py
python -m platformio run -d native -e native_voice
python -m platformio run -d speech -e speech_intelligence
# Requires both returned boards; deploys both reviewed images before testing:
python scripts/resume_validation.py --deploy-staged
```

Build sequentially. Never open a serial port from two tools at once. Provisioning preserves existing private credentials; binaries embed keys and should not be published. Flashing verifies assigned MAC and preserves NVS. Mains switching is prohibited. One real PIR covers only its assigned zone.

[Submitted proposal comparison](docs/PROPOSAL_VS_IMPLEMENTATION.md) · [RTOS design](docs/EMBEDDED_SYSTEM_DESIGN.md) · [Simulation/API guide](docs/SIMULATION_LAB.md) · [Publication test matrix](docs/PUBLICATION_TEST_MATRIX.md).
