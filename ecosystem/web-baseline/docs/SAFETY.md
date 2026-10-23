# Safety boundary and limitations

Updated 21 October 2026. This is a low-voltage DC prototype. No mains switching is permitted. Default native builds inhibit physical outputs and register all virtual devices with pin -1. USB-only board bring-up has been performed; PIR, relays, loads and electrical switching remain unvalidated.

Node A's SafetyKernel is the only component that changes registered output state. It validates room/device/action, limits rapid cycling with a three-second dwell, limits continuous ON to four hours, and gives manual actions a five-minute hold over automatic policy. Maximum-run cutoff overrides a manual hold. The registry rejects duplicate identities/GPIOs and overlong names.

The firmware sets the inactive output latch before enabling each pin as an output. Hardware reset/power-up levels, relay polarity, brownouts and connected-load transients have not been measured. Software initialization does not prove absence of electrical pulses during power-up, and a crash does not guarantee an external relay de-energizes. Physical fail-safe behavior requires suitable pull resistors, drivers and independent electrical validation.

Network proposals require paired peer identity, a mandatory application authentication tag, the current persistent actuator boot identity, next request ID and valid command issuance/TTL. An identical retry returns the cached outcome. No network message directly writes a GPIO. The HTTP control endpoint requires its own token on the protected AP; the token is not a substitute for secure production provisioning.

Scenes call the same kernel for each output. If one action is denied, a scene may partially apply, and its ACK reports denial and actual states. All-off obeys dwell and is not a physical emergency stop. Auto-off timers retry a denied OFF; they do not bypass the kernel.

Local automation uses bedroom PIR evidence only. Its VACANT policy state means the grace timer expired, not verified human absence. The four virtual rooms have explicitly synthetic inputs, processed separately on A; these are not physical occupancy evidence. Fan automation is optional and motion-based; it is not thermal protection.

Boot self-tests use mock pins and run before real devices are registered. USB control commands still pass through the kernel. Fault/ACK-drop/replay hooks now require the explicit `CLAWGRID_DIAGNOSTIC_FAULTS=1` build flag; default images disable them. Compilation rejects combining diagnostic faults with enabled physical outputs. Diagnostic builds are only for disconnected bare boards; production fault behavior still requires independent electrical validation.

## Controlled watchdog diagnostics

`native_voice_diagnostic` and `speech_intelligence_diagnostic` are explicit inhibited USB diagnostic environments. For A's verified CH343 UART0 connection, use `native_voice_uart_diagnostic`, then restore `native_voice_uart`; these retain the same kernel/watchdog with the pinned SDK UART console. The flasher requires `--diagnostic` for fault images and `--uart-console` for A's UART variant. B restores to normal `speech_intelligence`. Normal images keep fault hooks disabled.

`scripts/recovery_hil.py --node-a COM9 --node-b COM8 --trials 3` uses the current MAC-verified ports and diagnostic firmware, stops on the first failed check, and never manually resets a board to obtain a passing result. The existing `X` mechanism stalls only A's ControlTask by sleeping without feeding the task watchdog. Passive `q` diagnostics show frozen control progress, live link progress and the fault configuration. An automatic reboot clears the RAM-only stall. The script preserves policy without editing it and uses virtual OFF commands only to validate stale command/confirmation rejection and normal recovery. Reconfirm MACs if port aliases change.

Task watchdog settings stay at five seconds with panic and print/reboot enabled. Proof requires the watchdog panic naming ControlTask, `ESP_RST_TASK_WDT` (6), an advancing persistent boot identity and restored fresh schema-3 telemetry. A ROM `RTC_SW_CPU_RST` line alone does not establish cause: the panic handler itself uses a software CPU reset. Recovery measurements are explicitly host-observed injection-to-fresh-state intervals, including boot, radio and probe delivery.

The completed three-trial campaign is recorded in [the watchdog report](../benchmarks/reports/WATCHDOG_VALIDATION_20261021.md). During a control stall, LinkTask may resend its cached report, keeping B's packet-age `known` flag true. That flag alone is not proof of control-task progress. Use task counters, boot identity and an actual current-boot ACK for recovery diagnostics; physical state still requires independent feedback.

The offline interface preview performs no automation or inference: APIs explicitly return503 and device states stayunknown. A loaded speech model produces proposals only. Missing sensors/expired input leases cannot authorize unlimited automatic occupancy; manual holds and maximum-run cutoff retain their own documented semantics.

## Later low voltage bench validation

Default `native_voice` and speech/API builds keep `CLAWGRID_PHYSICAL_OUTPUTS=0`. The separate `native_voice_bench` environment is an explicit opt-in compile target for A; it does not validate or flash hardware. All virtual devices still use GPIO -1. The pin assumptions remain GPIO4 light, GPIO5 fan, GPIO6 PIR, active-low relay inputs. Actual module compatibility and polarity are unverified.

Before using that bench image:

- Confirm exact ESP32 board and relay/driver module specifications, GPIO input compatibility, supply rails, trigger polarity, protection and grounding.
- Use only appropriate low-voltage DC demonstration loads with a current-limited supply and a physical load-power disconnect. Do not drive a relay coil or fan directly from GPIO.
- Observe outputs electrically during cold boot, reset, brownout and USB removal; software initialization alone does not prove absence of a pulse.
- Wire the PIR first and verify warm-up, HIGH/LOW voltage and motion events; one PIR covers only its assigned zone.
- Test one load/channel at a time, then holds, timers, vacancy grace and communication loss. All-off obeys dwell and is not an emergency disconnect.
- Check effective policy after restoring preserved NVS. Factory defaults are automatic light ON, automatic fan OFF, grace 120 seconds. A saved policy can override those defaults. The API shows actual effective flags/grace and A's output-enabled mode.
- Keep physical state labeled not measured unless independent feedback has actually been fitted and validated. The B shadow monitor is not such feedback.
- Run inhibited HIL/recovery campaigns on the inhibited image; do not reuse their output-mode assertions on connected loads. The maximum-run cutoff is not a permanent latched lockout against later requests.

`--host-dev` explicitly runs portable firmware on the laptop with mock pins. Its persistent banner and HOST_MEASURED/SIMULATED provenance are mandatory. It never activates as a fallback to a failed board connection and performs no speech inference. The offline `--preview-only` mode continues to return 503 for board APIs.
