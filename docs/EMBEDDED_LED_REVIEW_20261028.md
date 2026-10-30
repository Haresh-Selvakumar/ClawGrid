# Embedded LED integration review — 28 October 2026

Current physical scope: A lightGPIO4/fanGPIO5, each own10k series resistor to LED anode/cathodeGND; DHT11 DATA GPIO7/existing3V3/GND. PIR removed, relays unused. This review distinguishes source inspection, host tests and physical observations.

| Owner | Core / priority / cadence | Work and boundaries |
|---|---|---|
| A ControlTask | core1 / priority5 /5ms | Sole post-startup owner of physical SafetyKernel device state and GPIO transitions; tick max-run, consume at most2 commands/cycle, publish latest report; no serial/NVS/DHT waits |
| A LinkTask | core0 / priority3 /5ms | At most4 queued received frames/cycle; authenticate/decode then enqueue control requests; drain up to2 ACKs/cycle; authenticated fresh advancing reports |
| A SensorTask | core0 / priority1 / about2530ms |2s initial settle;20ms start LOW +10ms edge capture window; decode checksum after capture;2.5s delay between attempts; own watchdog |
| B LinkTask | core0 / priority3 /5ms | At most4 frames/cycle; authenticated state/ACK correlation; bounded same-transaction radio retries, not new application ON requests |
| B HTTP task | core1 / SDK configuration | Proposal/confirmation APIs, bounded request sizes/socket waits; queues/critical sections instead of GPIO writes |

ESP-NOW callbacks copy bounded frames into a static depth8 receive queue with zero wait then return. Queue-full/invalid input increments drops. A command/ACK queues depth8, state/temperature latest-value mailboxes depth1; producer overload drops/counters or latest overwrite explicitly. Diagnostic depth16 queue drops bounded oversized/full lines. Port critical sections protect mailbox/audit/shared state; atomics publish counters and task flags. DHT edge ISR stores at most100 pulse durations then marks overflow; processing is outside ISR and ControlTask. These bounds are source evidence, not a measured worst-case scheduling proof.

Control/link tasks are individually watchdog-registered; runtime health requires task-ready flags and no fault. SensorTask separately registers/feeds watchdog.5s panic watchdog unchanged. Stack/counters/drop/late metrics are observations; short runs with no observed misses do not establish hard real-time guarantees.

DHT attempts are separated by >2s. Timeout/checksum/invalid/driver failure publishes an invalid new sample, never carries a last good value as current. Controller uses status/range/10s age gate; gateway adds report transit age; Android additionally requires fresh (<3s) controller state and fresh (<10s) valid temperature. Host decode/age tests passed. Live sensor-disconnect behavior still needs separately labelled physical verification if performed.

PIR-unavailable build never configures or reads GPIO8. Authenticated room report carries availability=false, motion=null in HTTP, occupancy=UNAVAILABLE,auto_flags0. Physical AUTO/occupancy policy resume denied. Virtual simulation still uses separate state/control paths. Android hardware AUTO is disabled and explains PIR removal. Manual transitions still go through SafetyKernel,3s minimum dwell and4h continuous maximum run.4h limit is actual-source host-tested, not a4h physical endurance test.

Startup preloads OFF latch before output configuration. Native gpio_config selects GPIO function/direction without clearing that latch. Normal LED-only active-HIGH output-enable target is explicit; no SafetyKernel bypass. Source/board state is commanded GPIO state; it is not electrical feedback. Human visibleLED and meter observations are recorded separately.

Persistence format unchanged: boot identity commits before command eligibility; saved policy/manual-mode flags restore with device state OFF. Prior ON state is not restored. In no-PIR build historical auto bits may remain stored for compatibility but are not acquired/executed or advertised as available; no NVS erase/migration/key change. App resets session proof/proposals at boot change and never automatically replays a request.

Gateway accepts a proposal separately from dispatch. Confirm binds proposalID/full target/current boot/freshness/lease and consumes it. Controller validates secure sender/authentication, boot, expiry and monotonic command ID; duplicate identical bytes return cached ACK without a second transition. Android HTTP202 means PENDING; completion requires applied matching ACK and advancing fresh same-boot target state. Cancellation, wrong target, duplicate confirmation, stale/reboot behavior are actual-source test cases. Current84 JVM and65 focused firmware/GPIO/parser/privacy tests passed; final native51 includes no-PIR/manual bookkeeping.

Live and physical trial outcomes are in benchmarks/review/voice-led-no-pir-20261028. No microphone success is inferred from typed-parser/manual actions. Existing English0.15 Vosk model/archive unchanged; runtime decoder list evaluation uses explicit command/negative paths plus[unk], exact whole finalized text parser, fixed0.7 per-word gate and explicit confirmation. Preserve failed trials and diagnostic permission boundaries.
