# Controlled watchdog validation — 21 October 2026

**Completed: 3/3 controlled trials, 69/69 campaign check rows and 16/16 restoration checks passed.** The maintained host suite passed 144 tests. This is fresh HARDWARE_MEASURED watchdog evidence from the updated inhibited application, separate from historical recovery results.

Node A is MAC-verified `e0:72:a1:f6:a9:68` on **COM9 through its CH343 USB UART0 adapter**. Node B remains `e0:72:a1:f5:bc:b0` on **COM8 native USB**. No additional USB socket or hardware wiring was required. All PIR, relay and load connections remained disconnected.

## Configuration reported before injection

A explicitly monitors ControlTask (core 1, priority 5, 5 ms period) and LinkTask (core 0, priority 3, 5 ms period). B explicitly monitors LinkTask. Both CPU idle tasks are monitored on each node. Diagnostics and SpeechTask are not separately subscribed. `RuntimeHealth::begin()` calls `esp_task_wdt_init(5,true)`; the SDK confirms a **5-second task watchdog**, panic enabled and **print/reboot** behavior. The separate interrupt watchdog remains 300 ms and bootloader watchdog 9000 ms. No watchdog timeout, framework pin, threshold or restart policy was changed.

The existing diagnostic `X` hook sets a RAM-only stall flag. ControlTask sleeps without feeding its watchdog; LinkTask and Diagnostics continue. Fault/replay hooks require an explicit diagnostic image and compilation rejects combining them with physical outputs enabled. Actual image markers, console settings and board MACs were checked before app-only flashing.

## Per-trial results

Recovery time below is **host-observed X-write to the first observed fresh post-reset schema-3 API state**, including UART delivery, boot, radio and probe time. It is not the configured timeout or a precise electrical reset interval. Every trial required watchdog panic text naming ControlTask, SDK reset reason 6, one boot increment, restored policy, live task counters, fresh telemetry and subsequent authenticated virtual-command ACK.

Trial 1:

| Injected fault | Watchdog evidence | Reset reason | Recovery time | Telemetry restored | Policy restored | Stale command rejected |
|---|---|---|---|---|---|---|
| X stalls only ControlTask | task_wdt panic names ControlTask, CPU 1; observed at 5.026 s | 6, ESP_RST_TASK_WDT; boot 20→21 | 6.267 s | Yes, schema 3 and inhibited | Yes, all saved fields | Yes: exact old command status 4; old confirmation HTTP 409 |

Trial 2:

| Injected fault | Watchdog evidence | Reset reason | Recovery time | Telemetry restored | Policy restored | Stale command rejected |
|---|---|---|---|---|---|---|
| X stalls only ControlTask | task_wdt panic names ControlTask, CPU 1; observed at 5.029 s | 6, ESP_RST_TASK_WDT; boot 21→22 | 6.267 s | Yes, schema 3 and inhibited | Yes, all saved fields | Yes: exact old command status 4; old confirmation HTTP 409 |

Trial 3:

| Injected fault | Watchdog evidence | Reset reason | Recovery time | Telemetry restored | Policy restored | Stale command rejected |
|---|---|---|---|---|---|---|
| X stalls only ControlTask | task_wdt panic names ControlTask, CPU 1; observed at 5.020 s | 6, ESP_RST_TASK_WDT; boot 22→23 | 6.270 s | Yes, schema 3 and inhibited | Yes, all saved fields | Yes: exact old command status 4; old confirmation HTTP 409 |

A's task counters prove unrelated activity survived the fault: control counts stayed fixed while link counts advanced. Samples were 18240→18240 versus 18296→18432 in trial 1; 1133→1133 versus 1187→1325 in trial 2; and 1131→1131 versus 1186→1323 in trial 3. Diagnostics answered both samples and B's HTTP/LinkTask stayed responsive. After each reset, both A task counts advanced again and a new confirmed virtual OFF command received status-0 ACK.

Each pre-reset pending proposal was rejected before expiry because its stored boot no longer matched the controller, with HTTP 409 `Controller unavailable or stale`. Replaying the exact earlier authenticated command produced A's **WrongBoot (4)** result, before TTL evaluation or command-gate commit; command sequence remained zero until a new command was submitted. Only virtual OFF targets were used; no physical-target command was sent.

## Watchdog versus requested restart

The three UART traces contain `Task watchdog got triggered`, `ControlTask (CPU 1)`, `Aborting`, ROM `rst:0xc (RTC_SW_CPU_RST)`, then application `boot_recovery` with **reset_reason=6** and boot IDs 21, 22 and 23. The ROM software-CPU-reset line is the panic handler's reset implementation; it is not sufficient by itself. The preceding task-watchdog panic, SDK cause and boot progression establish watchdog-initiated recovery. No esptool invocation, DTR/RTS reset sequence, esp_restart request or manual reset occurred inside a trial. Identity/flashing resets occurred only before and after the campaign and are retained separately. Baseline and final post-flash resets reported cause 1, distinct from the three task-watchdog causes.

## Restoration and final state

Both fault-disabled images were identity-checked and flashed after the three trials. A remains on the inhibited **native_voice_uart** image so diagnostics are accessible through the verified CH343 adapter; B runs **speech_intelligence** on native USB. These are normal images with diagnostic faults off. A's final boot is 25; boots after 23 belong to the explicit restoration phase, not to additional watchdog trials.

All 16 restoration checks passed. A rejects X/d/O and B rejects F/R/W/V as disabled without arming a fault. After a disabled X request, A's boot remained unchanged beyond 6.2 seconds, control/link counters advanced, fresh inhibited schema-3 telemetry advanced and both self-tests passed. All serial handles were released.

The original saved policy was **never edited**: HOME, grace 120000 ms, light automation on, fan automation off. Every recovered policy and final policy matched all stored fields. Credentials match the preserved installed-pair backup exactly; no key/password regeneration occurred. Every deployment wrote only app0 at 0x10000, outside NVS; no bulk/NVS erase, model rewrite, eFuse or security change occurred. The NVS boot counter advances normally, so NVS is not expected to remain byte-identical.

| Final firmware | Port | Application SHA256 |
|---|---|---|
| native_voice_uart | COM9 | `82f3450186dae4bbf92b649059d58b4c341bfffe9fd6332d2a01219a31a5b4f2` |
| speech_intelligence | COM8 | `e7bedef3a79ee225122b80290a9704e450541d13f97c2351f5b426e1586d075a` |

## Commands and software changes

All commands used the existing interpreter `C:\Users\hares\Documents\Codex\2026-10-21\i-h\work\clawgrid-env\Scripts\python.exe`, abbreviated `$python`. Commands below returned **0**. Exact build command arrays/logs, manifests, serial records and host JUnit XML are retained in the evidence directory.

- `$python benchmarks/review/watchdog/build_uart.py`: built native_voice_uart_diagnostic, native_voice_uart, speech_intelligence_diagnostic and speech_intelligence with pinned ESP-IDF 4.4.7 / ESP-SR 1.7.0.
- `$python -m pytest tests -q --basetemp=benchmarks/review/watchdog/pytest-uart --junitxml=benchmarks/review/watchdog/host-uart.xml`: **144 passed**.
- `$python benchmarks/review/watchdog/deploy_mode.py --diagnostic --uart-a --node-a COM9 --node-b COM8`: MAC-checked diagnostic pair, app-only, original credentials/partitions/model verified.
- Read-only preflight recorded original policy, task health, self-tests and schema-3 state before injection.
- `$python scripts/recovery_hil.py --node-a COM9 --node-b COM8 --trials 3`: **69/69 check rows**, three completed trials, no errors.
- `$python benchmarks/review/watchdog/deploy_mode.py --uart-a --node-a COM9 --node-b COM8`: restored inhibited normal images.
- `$python benchmarks/review/watchdog/verify_restored.py --node-a COM9 --node-b COM8`: **16/16** restoration checks.

Maintained source changes: native/main/native_hal.{h,cpp} selects the pinned SDK console transport and supplies nonblocking UART receive through the already verified ROM UART0 route. native/sdkconfig.uart.defaults and native/platformio.ini add explicit UART normal/diagnostic variants while leaving the native USB environments available. The SDK expressly supports application console selection differing from the retained bootloader, so no bootloader replacement was needed. Firmware A/B print retained build-mode and console markers; A supplies passive q task-health diagnostics. clawgrid_config.h defaults fault injection off and rejects faults with enabled loads. A/B hooks honor that gate. flash_native.py checks actual binary mode/console and requires explicit --diagnostic/--uart-console selections. recovery_hil.py preserves policy, timestamps evidence and stops at the first failure. test_watchdog_campaign.py adds cause/policy/unsafe-build/image-console mismatch tests.

## Limits and retained diagnostics

During all three stalls, LinkTask retransmitted its cached A report and B's `known` flag stayed true. **Recent packet receipt alone is not proof of control-task progress.** This campaign recorded that behavior and used frozen/live task counters, the new persistent boot, actual watchdog cause, fresh post-boot reports and new-command ACKs to establish recovery. The cached-report freshness behavior remains a software limitation; it was not hidden or changed to obtain passing tests.

Initial native-USB connection blockers and failed preflight guards are retained. The expected A MAC was verified through COM9, then the console transport was adapted using the exact pinned SDK. A regenerated model cache was SHA256-compared with the prior verified model; no model migration occurred. CMake compile_commands omits PlatformIO application flags, so guards now check retained markers in the actual firmware rather than treating that database as proof.

Not performed: B watchdog fault injection, power-cut/brownout/cold-start campaigns, physical GPIO/relay switching, PIR accuracy, real-load fail-safe behavior, long-duration recovery stress or speech-accuracy evaluation. Independent physical output state remains **not measured**. Three USB-only trials establish this controlled software recovery path, not electrical safety or long-term reliability.

[Evidence manifest](../raw/hardware/watchdog_20261021T101022Z/manifest.json) · [Three-trial records](../raw/hardware/watchdog_20261021T101022Z/results.json) · [Restoration records](../raw/hardware/watchdog_20261021T101022Z/restored-verification.json).
