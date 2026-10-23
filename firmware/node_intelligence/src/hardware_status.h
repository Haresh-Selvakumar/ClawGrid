#pragma once
#include "platform_hal.h"

// Read-only AP landing page. The native Android app owns product controls.
static const char kHardwareStatus[] PROGMEM=R"HTML(<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>ClawGrid gateway hardware status</title><style>
body{font:16px system-ui,sans-serif;background:#101827;color:#e5edf7;max-width:760px;margin:32px auto;padding:0 18px}
h1{font-size:1.65rem}section{padding:18px;margin:16px 0;background:#192438;border-radius:10px}
dl{display:grid;grid-template-columns:1fr 1fr;gap:10px}dd{margin:0;font-weight:600}pre{white-space:pre-wrap;overflow-wrap:anywhere;font-size:.8rem}a{color:#91c9ff}.note{color:#b7c5d9}
</style></head><body>
<h1>ClawGrid gateway · hardware status</h1>
<p class="note">Live board reports only. Use the native ClawGrid Android app for control and phone push-to-talk. No Internet is required.</p>
<p>Open this gateway with <strong>http://192.168.4.1</strong>. HTTPS is not configured.</p>
<section aria-live="polite"><h2 id="connection">Waiting for board telemetry</h2><dl>
<dt>Bedroom light</dt><dd id="light">UNKNOWN</dd><dt>Bedroom fan</dt><dd id="fan">UNKNOWN</dd>
<dt>Occupancy</dt><dd id="occupancy">UNKNOWN</dd><dt>GPIO outputs</dt><dd id="outputs">UNKNOWN</dd>
<dt>Controller boot</dt><dd id="boot">Unavailable</dd><dt>Report age</dt><dd id="age">Unavailable</dd>
<dt>Temperature</dt><dd>Unavailable</dd><dt>Physical load state</dt><dd>Not measured</dd>
</dl><p>ON/OFF describes controller logic. It does not prove physical switching.</p></section>
<section><h2>Gateway measurements</h2><p id="gateway">Unavailable</p><pre id="rtos">Waiting for measured task data</pre></section>
<section><h2>Controller report snapshot</h2><pre id="state">Unavailable</pre></section>
<p class="note">This page cannot submit commands, change policy or enable outputs. <a href="/simulation">Open the explicitly labelled simulation lab</a>.</p>
<script>
(() => {
  'use strict';
  const el = id => document.getElementById(id);
  let sample = null, connected = false, lastBoot = null, lastUptime = null, gateway = null, gatewayReceived = 0;
  const uint = v => Number.isInteger(v) && v >= 0 && v <= 0xffffffff;
  const newer = (a,b) => a !== b && ((a-b) >>> 0) < 0x80000000;
  const ageAt = now => sample ? sample.age + Math.max(0, now-sample.received) : Infinity;
  function acceptState(s, now = performance.now()) {
    try {
      if (s.known !== true || s.authenticated !== true) throw Error('Unknown report');
      if (s.room !== 'bedroom' || !uint(s.boot_id) || !uint(s.sample_uptime_ms) || !uint(s.last_command) ||
          !uint(s.age_ms) || typeof s.light !== 'boolean' || typeof s.fan !== 'boolean' ||
          typeof s.physical_outputs_enabled !== 'boolean') throw Error('Invalid report');
      for (const k of ['source','processing_source','input_source'])
        if (s[k] != null && s[k] !== 'HARDWARE_MEASURED') throw Error('Not hardware telemetry');
      if (lastBoot !== null && s.boot_id < lastBoot) throw Error('Old boot');
      if (s.boot_id === lastBoot && s.sample_uptime_ms !== lastUptime && !newer(s.sample_uptime_ms,lastUptime)) throw Error('Old sample');
      const same = sample && s.boot_id === lastBoot && s.sample_uptime_ms === lastUptime;
      if (same && (s.light !== sample.data.light || s.fan !== sample.data.fan || s.last_command !== sample.data.last_command ||
          s.physical_outputs_enabled !== sample.data.physical_outputs_enabled)) throw Error('Changed duplicate');
      const age = same ? Math.max(s.age_ms,ageAt(now)) : s.age_ms;
      sample = {data:s,age,received:now}; connected = true; lastBoot = s.boot_id; lastUptime = s.sample_uptime_ms;
      render(now); return true;
    } catch (_) { connected = false; render(now); return false; }
  }
  function render(now = performance.now()) {
    const fresh = connected && sample && ageAt(now) < 3000;
    el('connection').textContent = fresh ? 'Fresh authenticated board report' : 'UNKNOWN · disconnected, invalid or stale telemetry';
    for (const id of ['light','fan']) el(id).textContent = fresh ? (sample.data[id] ? 'ON' : 'OFF') : 'UNKNOWN';
    el('occupancy').textContent = fresh ? (sample.data.occupancy || 'UNKNOWN') : 'UNKNOWN';
    el('outputs').textContent = fresh ? (sample.data.physical_outputs_enabled ? 'ENABLED · stop commissioning' : 'INHIBITED') : 'UNKNOWN';
    el('boot').textContent = sample ? String(sample.data.boot_id) : 'Unavailable';
    el('age').textContent = sample ? Math.floor(ageAt(now)) + ' ms' : 'Unavailable';
    el('state').textContent = sample ? (fresh ? '' : 'Previous snapshot; current state UNKNOWN\n') + JSON.stringify(sample.data,null,2) : 'Unavailable';
    const gatewayFresh = gateway && Math.max(0,now-gatewayReceived) < 3000;
    el('gateway').textContent = gatewayFresh ? 'Measured AP ' + (gateway.ap_ip_valid ? gateway.ap_ip : 'unknown') +
      ' · channel ' + (gateway.wifi_channel_known ? gateway.wifi_channel : 'unknown') +
      ' · clients ' + (gateway.ap_clients_known ? gateway.ap_clients : 'unknown') : 'Unavailable';
    el('rtos').textContent = gatewayFresh ? JSON.stringify(gateway,null,2) : 'Unavailable or stale';
  }
  async function get(path) {
    const abort = new AbortController(), timer = setTimeout(() => abort.abort(),1800);
    try {
      const r = await fetch(path,{cache:'no-store',signal:abort.signal});
      if (!r.ok) throw Error('HTTP rejected');
      return await r.json();
    } finally { clearTimeout(timer); }
  }
  async function poll() {
    try { acceptState(await get('/api/state')); } catch (_) { connected = false; }
    try {
      const r = await get('/api/rtos');
      gateway = r.source === 'HARDWARE_MEASURED' ? r : null; gatewayReceived = performance.now();
    } catch (_) { gateway = null; }
    render();
    setTimeout(poll,1000);
  }
  globalThis.ClawGridStatus = {acceptState,render,ageAt};
  setInterval(() => render(),250);
  poll();
})();
</script></body></html>)HTML";
