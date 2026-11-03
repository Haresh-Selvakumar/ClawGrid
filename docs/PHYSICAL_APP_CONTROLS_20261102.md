# Physical app controls — 2 November 2026

Latest user scope: sensor AUTO for Room 1 only. Room 2 retains its existing manual/voice controls and receives no new sensor AUTO firmware. Both room selectors and voice are available. Occupancy and measured lux remain unavailable because the sensors are absent. The four-room lab remains explicitly simulated.

APK-h connects the physical scene buttons to controller Scene commands with explicit review and authorization. Resume Room 1 AUTO releases Room 1 holds and enables its light/fan AUTO. Sleep requests both rooms' lights OFF and fans ON in MANUAL. Away requests both rooms OFF and sets AWAY, blocking AUTO. All Off requests both rooms OFF. Safety dwell applies. Multi-room execution is sequential and may partially apply; the app stops on the first unconfirmed/rejected result and tells the user to inspect each room. Success requires exact command ACK and fresh matching readback. No scene executes during build/install.

Room 1 retains editable fan thresholds, LM393 filtering, independent sensor evaluation order, schedule-versus-sensor source priority, manual holds, schedules, finite overrides, countdowns and continuous caps. These controller settings remain authoritative; app drafts do not pretend to be accepted settings.

Gateway B recovery completed with application readback and full-flash protected-region verification; see `benchmarks/review/three-board-20261102/rooms-restored/recovery.json`. Both rooms subsequently returned fresh actual telemetry. User confirmed the phone connected and working. APK-g removed the per-boot manual-proof voice prerequisite while preserving explicit room selection, fresh telemetry, profile checks, preview/authorization and ACK/readback. Historical Room1 evidence is preserved; no new C firmware or actuator tests were performed.

APK-h build/install receipt and limitations are in `benchmarks/review/three-board-20261102/rooms-restored/SCENES_CHECKPOINT.json`. Physical scene actuation is not acceptance-tested; user requested no extra trials. Existing model and signing identity are preserved. See the retained build logs at repository root.
