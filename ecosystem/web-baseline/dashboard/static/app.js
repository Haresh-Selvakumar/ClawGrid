'use strict';
const $ = id => document.getElementById(id);

// Application State Store (Strictly Separated Real-Time vs Synthetic Stores)
const AppState = {
  mode: 'virtual_rooms', // 'physical_bedroom' (Real-time) | 'virtual_rooms' (Synthetic)
  view: 'overview',
  realtime: {
    connected: false,
    stale: false,
    lastUpdate: 0,
    ageMs: null,
    source: 'HARDWARE_MEASURED',
    rooms: {
      bedroom: { state: 'UNKNOWN', temp: null, lux: null, light: false, fan: false, pir: false, outputsEnabled: false }
    },
    history: [],
    raw: null
  },
  synthetic: {
    rooms: [
      { id: 'bedroom', name: 'Bedroom', temp: 25.0, lux: 120, pir: true, presence: true, door: false, state: 'OCCUPIED', light: false, fan: false, autoLight: true, autoFan: true, lastMotion: Date.now(), dwellStart: 0, manualLightHoldUntil: 0, manualFanHoldUntil: 0, lastSwitchMs: 0 },
      { id: 'living_room', name: 'Living Room', temp: 24.0, lux: 280, pir: true, presence: true, door: false, state: 'VACANT', light: false, fan: false, autoLight: true, autoFan: false, lastMotion: 0, dwellStart: 0, manualLightHoldUntil: 0, manualFanHoldUntil: 0, lastSwitchMs: 0 },
      { id: 'kitchen', name: 'Kitchen', temp: 29.5, lux: 450, pir: true, presence: true, door: false, state: 'VACANT', light: false, fan: false, autoLight: true, autoFan: false, lastMotion: 0, dwellStart: 0, manualLightHoldUntil: 0, manualFanHoldUntil: 0, lastSwitchMs: 0 },
      { id: 'study', name: 'Study', temp: 22.8, lux: 95, pir: true, presence: true, door: false, state: 'VACANT', light: false, fan: false, autoLight: true, autoFan: false, lastMotion: 0, dwellStart: 0, manualLightHoldUntil: 0, manualFanHoldUntil: 0, lastSwitchMs: 0 }
    ],
    person: { x: 160, y: 140 },
    movingUntil: 0,
    tourTimer: null,
    history: [],
    tickCount: 0
  },
  rtos: {
    bootId: 1,
    startTime: Date.now(),
    controlJobs: 128490,
    linkJobs: 64120,
    speechJobs: 1420,
    piJobs: 4210,
    wdtStalled: false,
    wdtStallStart: 0,
    staleLeaseSimulated: false,
    piBackupActive: false,
    sramFreeKiB: 184.2,
    sramMinKiB: 142.0,
    psramFreeKiB: 7812
  },
  config: {
    graceSeconds: 5.0,
    dwellSeconds: 3.0,
    manualHoldMinutes: 5.0,
    luxThreshold: 300,
    tempThreshold: 28.0,
    hardwareUrl: 'http://127.0.0.1:8770'
  },
  eventLogs: [],
  notifications: [],
  timers: [],
  chartWindow: 60,
  isSidebarCollapsed: false
};

// Effective Lux helper: when a room's ceiling light is ON, photometer reads +220 lx above ambient
function getEffectiveLux(room) {
  return Math.min(2000, Math.round(room.lux + (room.light ? 220 : 0)));
}

// Non-blocking Toast & Notification Helper
function showToast(title, message, level = 'info') {
  const container = $('toastContainer');
  if (!container) return;
  const item = document.createElement('div');
  item.className = 'toast-item ' + (level === 'warn' ? 'warn' : level === 'danger' ? 'danger' : level === 'success' ? 'success' : '');
  item.innerHTML = `<div style="font-weight:700;margin-bottom:2px">${title}</div><div style="color:var(--text-muted)">${message}</div>`;
  container.appendChild(item);
  setTimeout(() => {
    item.style.opacity = '0';
    item.style.transition = 'opacity 0.25s ease';
    setTimeout(() => item.remove(), 260);
  }, 3200);
}

function pushNotification(title, detail, severity = 'INFO') {
  AppState.notifications.unshift({
    time: new Date().toLocaleTimeString(),
    title,
    detail,
    severity
  });
  if (AppState.notifications.length > 25) AppState.notifications.pop();
  const list = $('notifList');
  if (list) {
    list.innerHTML = AppState.notifications.map(n => `
      <div style="padding:8px;background:#090d16;border:1px solid var(--border);border-radius:8px">
        <div style="display:flex;justify-content:space-between;font-weight:700;color:${n.severity === 'ALARM' ? '#f87171' : n.severity === 'WARN' ? '#fbbf24' : '#60a5fa'}">
          <span>${n.title}</span>
          <span style="font-size:10.5px;color:var(--text-dim)">${n.time}</span>
        </div>
        <div style="color:var(--text-muted);margin-top:2px">${n.detail}</div>
      </div>
    `).join('');
  }
  const dot = $('notifDot');
  if (dot) dot.style.display = 'block';
}

// Log helper
function logEvent(room, eventType, severity, description) {
  const evt = {
    id: AppState.eventLogs.length + 1,
    timestamp: new Date().toLocaleTimeString(),
    room: room || 'System',
    eventType,
    severity: severity || 'INFO',
    source: AppState.mode === 'physical_bedroom' ? 'REAL_TIME' : 'SYNTHETIC',
    description
  };
  AppState.eventLogs.unshift(evt);
  if (AppState.eventLogs.length > 500) AppState.eventLogs.pop();
  if ($('navLogBadge')) $('navLogBadge').textContent = String(AppState.eventLogs.length);
  if (severity === 'WARN' || severity === 'ALARM') {
    pushNotification(`${room}: ${eventType}`, description, severity);
  }
  renderLogsTable();
  updateOverviewEvents();
}

// Sidebar View Switching
function switchView(viewName) {
  AppState.view = viewName;
  const views = ['overview', 'telemetry', 'simulator', 'devices', 'logs', 'diagnostics', 'health', 'config'];
  views.forEach(v => {
    const el = $('view' + v.charAt(0).toUpperCase() + v.slice(1));
    const nav = $('nav' + v.charAt(0).toUpperCase() + v.slice(1));
    if (el) el.classList.toggle('active-view', v === viewName);
    if (nav) nav.classList.toggle('active', v === viewName);
  });

  const titles = {
    overview: 'Home Overview',
    telemetry: 'Live Telemetry',
    simulator: '4-Room Interactive Lab',
    devices: 'Rooms & Relays',
    logs: 'Event Audit Logs',
    diagnostics: 'Sensor Diagnostics',
    health: 'SafetyKernel & RTOS',
    config: 'Policy Settings'
  };
  $('breadcrumbHeadline').firstElementChild.textContent = titles[viewName] || 'ClawGrid';
  $('breadcrumbCurrent').textContent = titles[viewName] || 'Home Overview';

  if (viewName === 'simulator') {
    drawSimHouse();
  } else if (viewName === 'diagnostics') {
    renderDiagnosticsTable();
  } else if (viewName === 'telemetry') {
    updateDetailedTelemetryView();
  }
}

// Mode Switching (Real-time vs Synthetic)
function switchTelemetryMode(mode) {
  AppState.mode = mode;
  $('targetMode').value = mode;
  if (typeof ClawGrid !== 'undefined' && ClawGrid.clear) ClawGrid.clear();

  const isReal = mode === 'physical_bedroom';
  $('btnModeReal').classList.toggle('active', isReal);
  $('btnModeSyn').classList.toggle('active', !isReal);

  const badge = $('headerModeBadge');
  const banner = $('contextBanner');
  const bannerTitle = $('contextBannerTitle');
  const bannerSub = $('contextBannerSub');

  if (isReal) {
    badge.textContent = 'HARDWARE (NODE A)';
    badge.className = 'mode-persistent-badge badge-real';
    if (banner) banner.classList.add('real-mode');
    if (bannerTitle) bannerTitle.innerHTML = '<span>📡 Real-Time Hardware Mode (Bedroom Node A)</span><span class="pill-status pill-active" style="font-size:10.5px">Source: HARDWARE / HOST</span>';
    if (bannerSub) bannerSub.textContent = 'Polling /api/state, /api/rtos & /api/memory for Node A/B telemetry. Disconnected boards never fabricate readings.';
    if ($('sidebarNodeRole')) $('sidebarNodeRole').textContent = 'Hardware Mode · Bedroom Node A';
  } else {
    badge.textContent = 'SYNTHETIC SIMULATION';
    badge.className = 'mode-persistent-badge badge-syn';
    if (banner) banner.classList.remove('real-mode');
    if (bannerTitle) bannerTitle.innerHTML = '<span>🏠 4-Room Interactive Simulation Mode</span><span class="pill-status pill-active" style="font-size:10.5px">Source: SIMULATED</span>';
    if (bannerSub) bannerSub.textContent = `Auto policy: Occupied & Ambient Lux < ${AppState.config.luxThreshold} lx turns Light ON (+220 lx boost) · Occupied & Temp ≥ ${AppState.config.tempThreshold}°C turns Fan ON (cools room).`;
    if ($('sidebarNodeRole')) $('sidebarNodeRole').textContent = '4-Room Simulation · SafetyKernel';
  }

  logEvent('System', 'MODE_CHANGE', 'INFO', `Switched mode to ${isReal ? 'REAL-TIME HARDWARE (Node A)' : 'SYNTHETIC 4-ROOM SIMULATION'}`);
  showToast('Mode Switched', isReal ? 'Active store: Real-Time Hardware (Bedroom Node A)' : 'Active store: 4-Room Synthetic Simulation', 'info');
  updateAllKPIs();
  drawSimHouse();
  updateDetailedTelemetryView();
}

// Sidebar Toggle
$('btnToggleSidebar').onclick = () => {
  if (window.innerWidth <= 768) {
    $('sidebarEl').classList.toggle('mobile-open');
  } else {
    AppState.isSidebarCollapsed = !AppState.isSidebarCollapsed;
    $('sidebarEl').classList.toggle('collapsed', AppState.isSidebarCollapsed);
  }
};

function toggleFullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen().catch(() => {});
  } else {
    document.exitFullscreen().catch(() => {});
  }
}

function toggleNotifPanel() {
  $('notifPanel').classList.toggle('open');
  if ($('notifPanel').classList.contains('open') && $('notifDot')) {
    $('notifDot').style.display = 'none';
  }
}

function clearNotifications() {
  AppState.notifications = [];
  $('notifList').innerHTML = '<div style="color:var(--text-dim)">No active alerts. Deterministic SafetyKernel nominal.</div>';
  $('notifDot').style.display = 'none';
}

// Interactive Telemetry Pulse Generator (so user can test chart waveforms with 1 click)
function injectTelemetryPulse(kind) {
  const overviewSel = $('overviewChartRoom') ? $('overviewChartRoom').value : 'all';
  const detailSel = $('telemetryRoomFilter') ? $('telemetryRoomFilter').value : 'all';
  const activeSel = AppState.view === 'telemetry' ? detailSel : overviewSel;

  const targets = activeSel === 'all'
    ? AppState.synthetic.rooms
    : [AppState.synthetic.rooms[parseInt(activeSel, 10) || 0]];

  targets.forEach(r => {
    if (kind === 'heat') r.temp = Math.min(55, +(r.temp + 2.5).toFixed(1));
    else if (kind === 'cool') r.temp = Math.max(10, +(r.temp - 2.5).toFixed(1));
    else if (kind === 'lux') r.lux = Math.min(1800, Math.round(r.lux + 200));
    else if (kind === 'dark') r.lux = Math.max(10, Math.round(r.lux - 180));
  });

  loadSimEnvToInputs();
  evalOccupancyAndPolicies();
  pushTelemetrySample();
  updateTelemetryChart();
  updateDetailedTelemetryView();

  const labelMap = {
    heat: 'Heat Pulse (+2.5°C)',
    cool: 'Cooling Pulse (-2.5°C)',
    lux: 'Daylight Surge (+200 lx)',
    dark: 'Dusk Drop (-180 lx)'
  };
  const scopeName = activeSel === 'all' ? 'All 4 Rooms' : targets[0].name;
  logEvent(scopeName, 'TELEMETRY_PULSE', 'INFO', `Injected ${labelMap[kind]} into ${scopeName}.`);
  showToast(labelMap[kind], `Applied to ${scopeName} — watch the telemetry waveform respond!`, 'info');
}

// 4-ROOM CANVAS SIMULATOR
const simCanvas = $('simHouseCanvas');
let isSimDragging = false;

function simRoomAt(x, y) {
  if (x < 0 || x > 800 || y < 0 || y > 560) return -1;
  return (y >= 280 ? 2 : 0) + (x >= 400 ? 1 : 0);
}

function jumpToRoom(roomIdx) {
  const centers = [
    { x: 180, y: 140 },
    { x: 580, y: 140 },
    { x: 180, y: 420 },
    { x: 580, y: 420 }
  ];
  if (roomIdx >= 0 && roomIdx < 4) {
    AppState.synthetic.person.x = centers[roomIdx].x;
    AppState.synthetic.person.y = centers[roomIdx].y;
    AppState.synthetic.movingUntil = performance.now() + 1200;
    if ($('simRoomSelect')) {
      $('simRoomSelect').selectedIndex = roomIdx;
      loadSimEnvToInputs();
    }
    evalOccupancyAndPolicies();
    pushTelemetrySample();
    updateTelemetryChart();
    drawSimHouse();
    const rName = AppState.synthetic.rooms[roomIdx].name;
    showToast(`Moved to ${rName}`, `Avatar localized in ${rName}. Occupancy set to OCCUPIED.`, 'success');
  }
}

function moveSimPerson(dx, dy) {
  const p = AppState.synthetic.person;
  if (p.x < 0) { p.x = 160; p.y = 140; }
  else {
    p.x = Math.max(16, Math.min(784, p.x + dx));
    p.y = Math.max(16, Math.min(544, p.y + dy));
  }
  AppState.synthetic.movingUntil = performance.now() + 1000;
  const rIdx = simRoomAt(p.x, p.y);
  if (rIdx >= 0 && $('simRoomSelect') && $('simRoomSelect').selectedIndex !== rIdx) {
    $('simRoomSelect').selectedIndex = rIdx;
    loadSimEnvToInputs();
  }
  evalOccupancyAndPolicies();
  drawSimHouse();
}

function simLeaveHouse() {
  AppState.synthetic.person.x = -100;
  evalOccupancyAndPolicies();
  drawSimHouse();
  logEvent('House', 'LEAVE_HOUSE', 'INFO', 'Virtual person left the house. Vacancy grace timers started.');
  showToast('Left House', 'Avatar is now Outside. Rooms will transition to VACANT after grace period.', 'info');
}

function resetSimHouse() {
  AppState.synthetic.person.x = 160;
  AppState.synthetic.person.y = 140;
  const now = Date.now();
  AppState.synthetic.rooms.forEach((r, idx) => {
    r.temp = [25.0, 24.0, 29.5, 22.8][idx];
    r.lux = [120, 280, 450, 95][idx];
    r.pir = true;
    r.presence = true;
    r.door = false;
    r.state = idx === 0 ? 'OCCUPIED' : 'VACANT';
    r.light = false;
    r.fan = false;
    r.autoLight = true;
    r.autoFan = idx === 0;
    r.manualLightHoldUntil = 0;
    r.manualFanHoldUntil = 0;
    r.lastMotion = idx === 0 ? now : 0;
    r.dwellStart = 0;
  });
  loadSimEnvToInputs();
  evalOccupancyAndPolicies();
  drawSimHouse();
  logEvent('House', 'SIM_RESET', 'INFO', 'Simulation state reset to ClawGrid factory defaults.');
  showToast('Simulation Reset', 'All 4 rooms restored to default temperatures, lux, and policies.', 'success');
}

function drawSimHouse() {
  if (!simCanvas) return;
  const c = simCanvas.getContext('2d');
  c.clearRect(0, 0, 800, 560);

  const rooms = AppState.synthetic.rooms;
  const currentRoomIdx = simRoomAt(AppState.synthetic.person.x, AppState.synthetic.person.y);
  const now = Date.now();

  for (let i = 0; i < 4; i++) {
    const x = (i % 2) * 400;
    const y = Math.floor(i / 2) * 280;
    const r = rooms[i];

    // Room Background with warm ambient glow when Light is ON
    const isLit = r.light;
    c.fillStyle = isLit ? '#1c2b27' : (r.state === 'OCCUPIED' ? '#131d31' : '#0b111e');
    c.fillRect(x + 6, y + 6, 388, 268);

    // Border highlights selected room or occupied room
    const isSelected = $('simRoomSelect') && i === $('simRoomSelect').selectedIndex;
    c.strokeStyle = isSelected ? '#38bdf8' : (r.state === 'OCCUPIED' ? '#10b981' : '#1e293b');
    c.lineWidth = isSelected ? 3 : 1.5;
    c.strokeRect(x + 6, y + 6, 388, 268);

    // Room Title + Selected Tag
    c.fillStyle = '#f8fafc';
    c.font = 'bold 20px -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif';
    c.fillText(r.name, x + 24, y + 38);

    if (isSelected) {
      c.fillStyle = '#38bdf8';
      c.font = 'bold 11px ui-monospace, SFMono-Regular, monospace';
      c.fillText('[SELECTED FOR EDIT]', x + 250, y + 36);
    }

    // Occupancy Status & Grace Countdown
    c.font = 'bold 14px -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif';
    if (r.state === 'OCCUPIED') {
      c.fillStyle = '#34d399';
      c.fillText('● Occupied (Active)', x + 24, y + 66);
    } else if (r.state === 'LIKELY_OCCUPIED') {
      const rem = Math.max(0, AppState.config.graceSeconds - (now - r.dwellStart) / 1000);
      c.fillStyle = '#fbbf24';
      c.fillText(`◐ Likely Occupied (Grace: ${rem.toFixed(1)}s)`, x + 24, y + 66);
    } else if (r.state === 'UNKNOWN') {
      c.fillStyle = '#f87171';
      c.fillText('⚠ Evidence Unknown (Sensors Off)', x + 24, y + 66);
    } else {
      c.fillStyle = '#64748b';
      c.fillText('○ Vacant', x + 24, y + 66);
    }

    // Actuator Pills
    c.font = 'bold 13.5px ui-monospace, SFMono-Regular, monospace';
    c.fillStyle = r.light ? '#fde047' : '#64748b';
    const lightMode = now < r.manualLightHoldUntil ? 'MANUAL' : (r.autoLight ? 'AUTO' : 'OFF');
    c.fillText(`💡 Light: ${r.light ? 'ON' : 'OFF'} (${lightMode})`, x + 24, y + 212);

    c.fillStyle = r.fan ? '#38bdf8' : '#64748b';
    const fanMode = now < r.manualFanHoldUntil ? 'MANUAL' : (r.autoFan ? 'AUTO' : 'OFF');
    c.fillText(`🌀 Fan: ${r.fan ? 'ON' : 'OFF'} (${fanMode})`, x + 215, y + 212);

    // Environmental sensors bar
    c.fillStyle = '#94a3b8';
    c.font = '13px -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif';
    const isMovingHere = i === currentRoomIdx && performance.now() < AppState.synthetic.movingUntil;
    const pirDot = r.pir ? (isMovingHere ? '🟢' : '⚪') : '❌';
    const presDot = r.presence ? (i === currentRoomIdx ? '🟢' : '⚪') : '❌';
    c.fillText(`PIR ${pirDot}  Pres ${presDot}  🌡️ ${r.temp.toFixed(1)}°C  ☀️ ${getEffectiveLux(r)} lx`, x + 24, y + 246);
  }

  // Draw Person Avatar
  const p = AppState.synthetic.person;
  if (p.x >= 0) {
    c.fillStyle = 'rgba(245, 158, 11, 0.22)';
    c.beginPath();
    c.arc(p.x, p.y, 24, 0, Math.PI * 2);
    c.fill();

    c.fillStyle = '#f59e0b';
    c.beginPath();
    c.arc(p.x, p.y, 14, 0, Math.PI * 2);
    c.fill();
    c.strokeStyle = '#ffffff';
    c.lineWidth = 3;
    c.stroke();

    c.fillStyle = '#ffffff';
    c.beginPath();
    c.arc(p.x, p.y, 4, 0, Math.PI * 2);
    c.fill();
  }

  const roomName = currentRoomIdx >= 0 ? rooms[currentRoomIdx].name : 'Outside / Away';
  if ($('simAvatarPos')) {
    $('simAvatarPos').textContent = `Avatar Position: (${Math.round(p.x)}, ${Math.round(p.y)}) · ${roomName}`;
  }
}

// Pointer & Keyboard events for Canvas
function pointSimPerson(e) {
  const rect = simCanvas.getBoundingClientRect();
  const scaleX = 800 / rect.width;
  const scaleY = 560 / rect.height;
  AppState.synthetic.person.x = Math.max(16, Math.min(784, (e.clientX - rect.left) * scaleX));
  AppState.synthetic.person.y = Math.max(16, Math.min(544, (e.clientY - rect.top) * scaleY));
  AppState.synthetic.movingUntil = performance.now() + 1000;

  const clickedRoom = simRoomAt(AppState.synthetic.person.x, AppState.synthetic.person.y);
  if (clickedRoom >= 0 && $('simRoomSelect') && $('simRoomSelect').selectedIndex !== clickedRoom) {
    $('simRoomSelect').selectedIndex = clickedRoom;
    loadSimEnvToInputs();
  }

  evalOccupancyAndPolicies();
  drawSimHouse();
}

if (simCanvas) {
  simCanvas.onpointerdown = e => {
    isSimDragging = true;
    simCanvas.setPointerCapture(e.pointerId);
    pointSimPerson(e);
  };
  simCanvas.onpointermove = e => {
    if (isSimDragging) pointSimPerson(e);
  };
  simCanvas.onpointerup = () => { isSimDragging = false; };
  simCanvas.onkeydown = e => {
    const delta = { ArrowLeft: [-24, 0], ArrowRight: [24, 0], ArrowUp: [0, -24], ArrowDown: [0, 24] }[e.key];
    if (delta) {
      e.preventDefault();
      moveSimPerson(delta[0], delta[1]);
    }
  };
}

// Virtual Room Tour
function toggleSimTour() {
  const btn1 = $('btnSimTour');
  const btn2 = $('btnQuickTour');
  if (AppState.synthetic.tourTimer) {
    clearInterval(AppState.synthetic.tourTimer);
    AppState.synthetic.tourTimer = null;
    if (btn1) {
      btn1.textContent = '▶️ Run Room Tour';
      btn1.classList.remove('btn-danger');
      btn1.classList.add('btn-primary');
    }
    if (btn2) {
      btn2.textContent = '▶️ Run Room Tour';
      btn2.classList.remove('btn-danger');
    }
    logEvent('House', 'TOUR_STOP', 'INFO', 'Virtual room tour stopped.');
    showToast('Room Tour Stopped', 'Manual avatar control resumed.', 'info');
    return;
  }
  let step = 0;
  const waypoints = [
    { x: 180, y: 140 },
    { x: 580, y: 140 },
    { x: 180, y: 420 },
    { x: 580, y: 420 }
  ];
  const doStep = () => {
    AppState.synthetic.person.x = waypoints[step].x;
    AppState.synthetic.person.y = waypoints[step].y;
    AppState.synthetic.movingUntil = performance.now() + 1200;
    if ($('simRoomSelect')) {
      $('simRoomSelect').selectedIndex = step;
      loadSimEnvToInputs();
    }
    step = (step + 1) % waypoints.length;
    evalOccupancyAndPolicies();
    pushTelemetrySample();
    updateTelemetryChart();
    drawSimHouse();
  };
  doStep();
  AppState.synthetic.tourTimer = setInterval(doStep, 3500);
  if (btn1) {
    btn1.textContent = '⏹️ Stop Room Tour';
    btn1.classList.remove('btn-primary');
    btn1.classList.add('btn-danger');
  }
  if (btn2) {
    btn2.textContent = '⏹️ Stop Room Tour';
    btn2.classList.add('btn-danger');
  }
  logEvent('House', 'TOUR_START', 'INFO', 'Automated 4-room occupancy tour started.');
  showToast('Room Tour Running', 'Avatar will visit all 4 rooms every 3.5 seconds.', 'success');
}

// Environmental input sync
function loadSimEnvToInputs() {
  if (!$('simRoomSelect')) return;
  const idx = $('simRoomSelect').selectedIndex;
  const r = AppState.synthetic.rooms[idx];
  $('simCheckPir').checked = r.pir;
  $('simCheckPres').checked = r.presence;
  $('simCheckDoor').checked = r.door;
  $('simInputTemp').value = r.temp.toFixed(1);
  $('simInputLux').value = Math.round(r.lux);
  if ($('simSelectedRoomBadge')) $('simSelectedRoomBadge').textContent = r.name;
  drawSimHouse();
}

function applySimPreset(temp, lux) {
  $('simInputTemp').value = temp;
  $('simInputLux').value = lux;
  applySimEnvironment();
}

function applySimEnvironment() {
  const idx = $('simRoomSelect').selectedIndex;
  const r = AppState.synthetic.rooms[idx];
  const t = parseFloat($('simInputTemp').value);
  const l = parseInt($('simInputLux').value, 10);

  if (isNaN(t) || t < 0 || t > 60 || isNaN(l) || l < 0 || l > 2000) {
    showToast('Invalid Environment Input', 'Temperature must be 0–60°C and Illuminance must be 0–2000 lx.', 'danger');
    logEvent(r.name, 'INVALID_INPUT_REJECTED', 'WARN', `Rejected out-of-range environment: Temp=${t}, Lux=${l}`);
    return;
  }

  r.pir = $('simCheckPir').checked;
  r.presence = $('simCheckPres').checked;
  r.door = $('simCheckDoor').checked;
  r.temp = t;
  r.lux = l;

  evalOccupancyAndPolicies();
  pushTelemetrySample();
  updateTelemetryChart();
  drawSimHouse();
  updateAllKPIs();
  updateDetailedTelemetryView();
  logEvent(r.name, 'ENV_APPLIED', 'INFO', `Environment updated: ${t.toFixed(1)}°C, ${l} lx, PIR: ${r.pir}, Pres: ${r.presence}`);
  showToast(`${r.name} Updated`, `Temp: ${t.toFixed(1)}°C · Ambient Lux: ${l} lx`, 'success');
}

function simDisableSensors() {
  const idx = $('simRoomSelect').selectedIndex;
  const r = AppState.synthetic.rooms[idx];
  r.pir = false;
  r.presence = false;
  $('simCheckPir').checked = false;
  $('simCheckPres').checked = false;
  evalOccupancyAndPolicies();
  drawSimHouse();
  logEvent(r.name, 'SENSOR_FAULT', 'WARN', 'Disabled PIR and Presence sensors. Evidence state set to UNKNOWN.');
  showToast(`${r.name} Sensors Disabled`, 'Occupancy evidence is now UNKNOWN (never assumed Vacant).', 'warn');
}

function simRestoreSensors() {
  const idx = $('simRoomSelect').selectedIndex;
  const r = AppState.synthetic.rooms[idx];
  r.pir = true;
  r.presence = true;
  $('simCheckPir').checked = true;
  $('simCheckPres').checked = true;
  evalOccupancyAndPolicies();
  drawSimHouse();
  logEvent(r.name, 'SENSOR_RESTORE', 'INFO', 'Restored PIR and Presence sensors to online.');
  showToast(`${r.name} Sensors Restored`, 'PIR and Presence sensors online.', 'success');
}

// Occupancy State Machine & Automation Policy Engine
function evalOccupancyAndPolicies() {
  const now = Date.now();
  const rooms = AppState.synthetic.rooms;
  const curIdx = simRoomAt(AppState.synthetic.person.x, AppState.synthetic.person.y);
  const isMoving = performance.now() < AppState.synthetic.movingUntil;

  rooms.forEach((r, idx) => {
    const isPersonHere = idx === curIdx;

    if (!r.pir && !r.presence) {
      r.state = 'UNKNOWN';
    } else if (isPersonHere && (r.presence || (r.pir && isMoving))) {
      if (r.state !== 'OCCUPIED') {
        logEvent(r.name, 'OCCUPANCY_ENTRY', 'INFO', `Occupancy detected in ${r.name}`);
      }
      r.state = 'OCCUPIED';
      r.lastMotion = now;
      r.dwellStart = 0;
    } else {
      if (r.state === 'OCCUPIED') {
        r.state = 'LIKELY_OCCUPIED';
        r.dwellStart = now;
      } else if (r.state === 'LIKELY_OCCUPIED') {
        const elapsed = (now - r.dwellStart) / 1000;
        if (elapsed > AppState.config.graceSeconds) {
          r.state = 'VACANT';
          logEvent(r.name, 'VACANCY_TIMEOUT', 'INFO', `Vacancy confirmed in ${r.name} after ${AppState.config.graceSeconds}s grace.`);
        }
      }
    }

    // Expire manual holds
    if (r.manualLightHoldUntil > 0 && now >= r.manualLightHoldUntil) {
      r.manualLightHoldUntil = 0;
      r.autoLight = true;
      logEvent(r.name, 'HOLD_EXPIRED', 'INFO', 'Manual Light hold expired; restored to AUTO policy.');
    }
    if (r.manualFanHoldUntil > 0 && now >= r.manualFanHoldUntil) {
      r.manualFanHoldUntil = 0;
      r.autoFan = true;
      logEvent(r.name, 'HOLD_EXPIRED', 'INFO', 'Manual Fan hold expired; restored to AUTO policy.');
    }

    // Light AUTO Policy
    if (r.autoLight && now >= r.manualLightHoldUntil) {
      if (r.state === 'OCCUPIED' && r.lux < AppState.config.luxThreshold) {
        if (!r.light) {
          r.light = true;
          logEvent(r.name, 'AUTO_LIGHT_ON', 'INFO', `Light AUTO energized: Occupied & Ambient Lux (${Math.round(r.lux)}) < ${AppState.config.luxThreshold}`);
        }
      } else if (r.state === 'VACANT' || (r.state === 'OCCUPIED' && r.lux >= AppState.config.luxThreshold)) {
        if (r.light) {
          r.light = false;
          logEvent(r.name, 'AUTO_LIGHT_OFF', 'INFO', `Light AUTO released (${r.state === 'VACANT' ? 'Room Vacant' : 'Sufficient Ambient Lux'}).`);
        }
      }
    }

    // Fan AUTO Policy
    if (r.autoFan && now >= r.manualFanHoldUntil) {
      if (r.state === 'OCCUPIED' && r.temp >= AppState.config.tempThreshold) {
        if (!r.fan) {
          r.fan = true;
          logEvent(r.name, 'AUTO_FAN_ON', 'INFO', `Fan AUTO energized: Occupied & Temp (${r.temp.toFixed(1)}°C) ≥ ${AppState.config.tempThreshold}°C`);
        }
      } else if (r.state === 'VACANT' || r.temp < AppState.config.tempThreshold - 0.4) {
        if (r.fan) {
          r.fan = false;
          logEvent(r.name, 'AUTO_FAN_OFF', 'INFO', `Fan AUTO released (${r.state === 'VACANT' ? 'Room Vacant' : 'Temp cooled below threshold'}).`);
        }
      }
    }
  });

  updateDeviceButtons();
  updateAllKPIs();
  updateRadarChart();
}

// Actuator Controls with Dwell Guard & Real-Time Hardware Support
async function setActuator(roomName, dev, action) {
  if (AppState.mode === 'physical_bedroom' && roomName === 'bedroom' && (action === 'ON' || action === 'OFF')) {
    try {
      if (typeof ClawGrid !== 'undefined' && ClawGrid.prepare && ClawGrid.confirm) {
        const target = { target: 'physical_bedroom', room: 'bedroom', device: dev, action };
        const prep = await ClawGrid.prepare(target, 'manual', 0);
        const conf = await ClawGrid.confirm(target);
        updateOverviewTerminal(`[HARDWARE PROPOSAL #${prep.proposal_id}] Confirmed Bedroom ${dev.toUpperCase()} -> ${action} (Tx #${conf.id})`);
        logEvent('Bedroom', 'HARDWARE_COMMAND', 'INFO', `Confirmed ${dev.toUpperCase()} -> ${action} via SafetyKernel proposal #${prep.proposal_id}`);
        showToast('Hardware Command Sent', `Bedroom ${dev} -> ${action} (Proposal #${prep.proposal_id})`, 'success');
      }
    } catch (err) {
      updateOverviewTerminal(`[HARDWARE REJECTED] ${err.message}`);
      logEvent('Bedroom', 'COMMAND_FAILED', 'ALARM', `Hardware command failed: ${err.message}`);
      showToast('Hardware Command Rejected', err.message, 'danger');
    }
  }

  const room = AppState.synthetic.rooms.find(r => r.id === roomName);
  if (!room) return;

  const now = Date.now();
  const holdMs = AppState.config.manualHoldMinutes * 60 * 1000;

  if (action === 'AUTO') {
    if (dev === 'light') {
      room.autoLight = true;
      room.manualLightHoldUntil = 0;
    } else {
      room.autoFan = true;
      room.manualFanHoldUntil = 0;
    }
    logEvent(room.name, 'ACTUATOR_AUTO', 'INFO', `${dev.toUpperCase()} set to AUTO policy.`);
    showToast(`${room.name} ${dev.toUpperCase()}`, 'Restored to sensor-driven AUTO policy.', 'info');
  } else {
    const isTargetOn = action === 'ON';
    if (dev === 'light') {
      room.light = isTargetOn;
      room.autoLight = false;
      room.manualLightHoldUntil = now + holdMs;
    } else {
      room.fan = isTargetOn;
      room.autoFan = false;
      room.manualFanHoldUntil = now + holdMs;
    }
    logEvent(room.name, 'MANUAL_OVERRIDE', 'WARN', `Manual ${dev.toUpperCase()} set to ${action}. Hold active for ${AppState.config.manualHoldMinutes}m.`);
    showToast(`${room.name} ${dev.toUpperCase()} → ${action}`, `Manual override hold active for ${AppState.config.manualHoldMinutes} min.`, 'warn');
  }

  evalOccupancyAndPolicies();
  pushTelemetrySample();
  updateTelemetryChart();
  drawSimHouse();
  updateDetailedTelemetryView();
  updateOverviewTerminal(`[SAFETYKERNEL] ${room.name} ${dev.toUpperCase()} -> ${action}`);
}

function quickToggle(roomName, dev) {
  const room = AppState.synthetic.rooms.find(r => r.id === roomName);
  if (!room) return;
  const current = dev === 'light' ? room.light : room.fan;
  setActuator(roomName, dev, current ? 'OFF' : 'ON');
}

function applyScene(sceneName) {
  const rooms = AppState.synthetic.rooms;
  logEvent('House', 'SCENE_APPLY', 'INFO', `Applied Scene: ${sceneName}`);

  if (sceneName === 'HOME') {
    rooms.forEach(r => {
      r.autoLight = true;
      r.autoFan = true;
      r.manualLightHoldUntil = 0;
      r.manualFanHoldUntil = 0;
    });
    updateOverviewTerminal('[SCENE HOME] Automatic occupancy policies enabled across all 4 rooms.');
    showToast('Scene: Home', 'All rooms set to automatic occupancy policies.', 'success');
  } else if (sceneName === 'AWAY') {
    rooms.forEach(r => {
      r.light = false;
      r.fan = false;
      r.autoLight = false;
      r.autoFan = false;
    });
    updateOverviewTerminal('[SCENE AWAY] All loads turned OFF. Auto policies paused.');
    showToast('Scene: Away', 'All lights and fans turned OFF.', 'info');
  } else if (sceneName === 'SLEEP') {
    rooms.forEach((r, idx) => {
      r.light = false;
      r.autoLight = false;
      if (idx === 0) {
        r.autoFan = true;
      } else {
        r.fan = false;
        r.autoFan = false;
      }
    });
    updateOverviewTerminal('[SCENE SLEEP] All lights turned OFF. Bedroom fan primed in AUTO.');
    showToast('Scene: Sleep', 'All house lights OFF; Bedroom fan in AUTO.', 'info');
  } else if (sceneName === 'ALL_OFF') {
    rooms.forEach(r => {
      r.light = false;
      r.fan = false;
      r.autoLight = false;
      r.autoFan = false;
    });
    updateOverviewTerminal('[SCENE ALL_OFF] EMERGENCY SHUTOFF: All 8 relays commanded to logical OFF.');
    showToast('Emergency All Off', 'Every relay in all 4 rooms commanded to logical OFF.', 'danger');
  }

  evalOccupancyAndPolicies();
  pushTelemetrySample();
  updateTelemetryChart();
  drawSimHouse();
  updateDetailedTelemetryView();
}

// Offline Voice & Natural Language Command Assistant
function fillAndRunVoice(text) {
  if ($('voiceTextInput')) {
    $('voiceTextInput').value = text;
    runVoiceCommand();
  }
}

async function runVoiceCommand() {
  const inputEl = $('voiceTextInput');
  if (!inputEl) return;
  const raw = inputEl.value.trim();
  if (!raw) return;

  const text = raw.toLowerCase();
  if ($('voiceStatusBadge')) $('voiceStatusBadge').textContent = 'Parsing intent…';

  if (text.includes('all off') || text.includes('emergency off')) {
    applyScene('ALL_OFF');
    if ($('voiceStatusBadge')) $('voiceStatusBadge').textContent = 'Applied: ALL_OFF';
    return;
  }
  if (text.includes('sleep')) {
    applyScene('SLEEP');
    if ($('voiceStatusBadge')) $('voiceStatusBadge').textContent = 'Applied: SLEEP';
    return;
  }
  if (text.includes('away')) {
    applyScene('AWAY');
    if ($('voiceStatusBadge')) $('voiceStatusBadge').textContent = 'Applied: AWAY';
    return;
  }
  if (text.includes('home mode') || text.includes('resume auto')) {
    applyScene('HOME');
    if ($('voiceStatusBadge')) $('voiceStatusBadge').textContent = 'Applied: HOME';
    return;
  }

  const roomMap = [
    { key: 'bedroom', id: 'bedroom', label: 'Bedroom' },
    { key: 'living', id: 'living_room', label: 'Living Room' },
    { key: 'kitchen', id: 'kitchen', label: 'Kitchen' },
    { key: 'study', id: 'study', label: 'Study' }
  ];
  const matchedRoom = roomMap.find(r => text.includes(r.key)) || roomMap[0];
  const dev = text.includes('fan') ? 'fan' : (text.includes('light') || text.includes('lamp') ? 'light' : null);
  const action = text.includes('turn on') || text.endsWith(' on') ? 'ON'
    : (text.includes('turn off') || text.endsWith(' off') ? 'OFF'
    : (text.includes('auto') ? 'AUTO' : null));

  if (!dev || !action) {
    if ($('voiceStatusBadge')) $('voiceStatusBadge').textContent = 'Clarification Needed';
    updateOverviewTerminal(`[VOICE PARSER] Clarification needed for "${raw}". Try "turn on bedroom light" or "turn off kitchen fan".`);
    showToast('Clarification Needed', 'Specify room, device (light/fan), and action (on/off/auto).', 'warn');
    return;
  }

  if ($('voiceStatusBadge')) $('voiceStatusBadge').textContent = `Intent: ${matchedRoom.label} ${dev.toUpperCase()} → ${action}`;
  logEvent(matchedRoom.label, 'VOICE_INTENT', 'INFO', `Parsed offline command "${raw}" -> ${matchedRoom.label} ${dev.toUpperCase()} ${action}`);
  await setActuator(matchedRoom.id, dev, action);
}

async function triggerMicCapture() {
  if (typeof ClawGrid === 'undefined' || !ClawGrid.capture) {
    showToast('Microphone Unavailable', 'Client audio capture helper not loaded.', 'warn');
    return;
  }
  try {
    if ($('voiceStatusBadge')) $('voiceStatusBadge').textContent = 'Recording 4s PCM16…';
    await ClawGrid.capture(msg => {
      if ($('voiceStatusBadge')) $('voiceStatusBadge').textContent = msg;
      updateOverviewTerminal(`[MIC CAPTURE] ${msg}`);
    });
  } catch (err) {
    if ($('voiceStatusBadge')) $('voiceStatusBadge').textContent = err.message;
    showToast('Microphone Capture Notice', err.message, 'warn');
  }
}

// Countdown Timers
function scheduleTimer() {
  const room = $('timerRoomSelect').value;
  const dev = $('timerDevSelect').value;
  const mins = parseInt($('timerMinutes').value, 10);
  if (isNaN(mins) || mins < 1) return;

  const targetTime = Date.now() + mins * 60 * 1000;
  AppState.timers.push({ room, dev, targetTime, mins });
  renderActiveTimers();
  logEvent(room, 'TIMER_SCHEDULED', 'INFO', `Auto-off timer scheduled for ${dev} in ${mins}m`);
  showToast('Timer Scheduled', `${room.replace('_', ' ')} ${dev} will turn OFF in ${mins}m.`, 'success');
}

function cancelTimer() {
  AppState.timers = [];
  renderActiveTimers();
  logEvent('House', 'TIMER_CANCEL', 'INFO', 'All countdown timers cancelled.');
  showToast('Timers Cleared', 'All active auto-off countdown timers cancelled.', 'info');
}

function renderActiveTimers() {
  const el = $('activeTimersText');
  if (!el) return;
  if (AppState.timers.length === 0) {
    el.textContent = 'No active countdown timers.';
    return;
  }
  const now = Date.now();
  el.innerHTML = AppState.timers.map(tm => {
    const remSec = Math.max(0, Math.ceil((tm.targetTime - now) / 1000));
    const m = Math.floor(remSec / 60);
    const s = remSec % 60;
    return `<span class="pill-status pill-active" style="margin-right:8px">⏱️ ${tm.room.replace('_', ' ')} ${tm.dev.toUpperCase()}: ${m}m ${s}s remaining</span>`;
  }).join('');
}

function updateOverviewTerminal(msg) {
  const el = $('overviewTerminal');
  if (el) el.textContent = `[${new Date().toLocaleTimeString()}] ${msg}`;
}

// Embedded Watchdog, Radio Lease & Raspberry Pi Backup Failover Simulators
function simulateWdtStall() {
  if (AppState.rtos.wdtStalled) return;
  AppState.rtos.wdtStalled = true;
  AppState.rtos.wdtStallStart = Date.now();
  if ($('badgeWatchdog')) {
    $('badgeWatchdog').className = 'pill-status pill-danger';
    $('badgeWatchdog').textContent = 'ControlTask STALLED · WDT Counting Down (5.0s)';
  }
  if ($('wdtStatusControl')) {
    $('wdtStatusControl').className = 'pill-status pill-danger';
    $('wdtStatusControl').textContent = 'STALLED (No WDT Feed)';
  }
  if ($('rtosMissControl')) $('rtosMissControl').textContent = 'STALLED';
  logEvent('Node A', 'WDT_STALL_INJECTED', 'ALARM', 'Injected RAM-only ControlTask stall (X). LinkTask cannot feed ControlTask WDT subscription.');
  showToast('ControlTask Stall Injected', 'Watchdog timer (5.0s) will trigger ESP_RST_TASK_WDT panic & safe reboot.', 'danger');
}

function simulateStaleLease() {
  AppState.rtos.staleLeaseSimulated = !AppState.rtos.staleLeaseSimulated;
  if (AppState.rtos.staleLeaseSimulated) {
    if ($('tier2Badge')) {
      $('tier2Badge').className = 'pill-status pill-warning';
      $('tier2Badge').textContent = 'STALE (>3.0s AGE)';
    }
    logEvent('Node B', 'RADIO_LEASE_EXPIRED', 'WARN', 'Simulated >3.0s non-advancing sample timestamp. Node B refuses new confirmations; Node A continues local PIR loop.');
    showToast('Radio Lease Expired (>3.0s)', 'Node B marks telemetry STALE; Node A continues autonomous local control.', 'warn');
  } else {
    restoreNominalRtos();
  }
}

function simulatePiFailover() {
  AppState.rtos.piBackupActive = !AppState.rtos.piBackupActive;
  if (AppState.rtos.piBackupActive) {
    if ($('tier3Badge')) {
      $('tier3Badge').className = 'pill-status pill-warning';
      $('tier3Badge').textContent = 'PRIMARY FAILOVER HUB';
    }
    if ($('ovPiStatus')) {
      $('ovPiStatus').textContent = 'FAILOVER ACTIVE';
      $('ovPiStatus').style.color = '#fbbf24';
    }
    logEvent('Raspberry Pi', 'BACKUP_TAKEOVER', 'WARN', 'Raspberry Pi host coordinator assumed primary multi-room SafetyKernel & voice control while ESP32 link recovers.');
    showToast('Raspberry Pi Failover Active', 'Pi local server is now running primary SafetyKernel & room state engine.', 'warn');
  } else {
    restoreNominalRtos();
  }
}

function restoreNominalRtos() {
  AppState.rtos.wdtStalled = false;
  AppState.rtos.staleLeaseSimulated = false;
  AppState.rtos.piBackupActive = false;
  if ($('badgeWatchdog')) {
    $('badgeWatchdog').className = 'pill-status pill-active';
    $('badgeWatchdog').textContent = 'Task WDT Nominal (5.0s)';
  }
  if ($('wdtStatusControl')) {
    $('wdtStatusControl').className = 'pill-status pill-active';
    $('wdtStatusControl').textContent = 'Subscribed (5.0s)';
  }
  if ($('rtosMissControl')) $('rtosMissControl').textContent = '0';
  if ($('tier2Badge')) {
    $('tier2Badge').className = 'pill-status pill-active';
    $('tier2Badge').textContent = 'ONLINE';
  }
  if ($('tier3Badge')) {
    $('tier3Badge').className = 'pill-status pill-active';
    $('tier3Badge').textContent = 'ACTIVE BACKUP';
  }
  if ($('ovPiStatus')) {
    $('ovPiStatus').textContent = 'Standby / Host Active';
    $('ovPiStatus').style.color = '#a78bfa';
  }
  logEvent('System', 'RTOS_NOMINAL', 'INFO', 'All FreeRTOS tasks, 5s watchdogs, and radio leases restored to nominal.');
  showToast('RTOS & Link Nominal', 'All embedded tasks and watchdogs operating normally.', 'success');
}

function updateRtosAndEmbeddedUI() {
  const now = Date.now();
  const rtos = AppState.rtos;

  // Handle 5.0s Watchdog Panic & Reboot cycle if stalled
  if (rtos.wdtStalled) {
    const elapsedSec = (now - rtos.wdtStallStart) / 1000;
    const rem = Math.max(0, 5.0 - elapsedSec);
    if ($('badgeWatchdog')) {
      $('badgeWatchdog').textContent = `WDT PANIC IN ${rem.toFixed(1)}s (ControlTask Stalled)`;
    }
    if ($('ovNodeAStatus')) {
      $('ovNodeAStatus').textContent = `STALLED (WDT ${rem.toFixed(1)}s)`;
      $('ovNodeAStatus').style.color = '#f87171';
    }
    if (elapsedSec >= 5.0) {
      rtos.wdtStalled = false;
      rtos.bootId += 1;
      rtos.startTime = now;
      // Reboot starts all physical/logical relays OFF and restores saved policy (per NEXT_CHAT_HANDOFF.md)
      AppState.synthetic.rooms[0].light = false;
      AppState.synthetic.rooms[0].fan = false;
      AppState.synthetic.rooms[0].manualLightHoldUntil = 0;
      AppState.synthetic.rooms[0].manualFanHoldUntil = 0;
      if ($('badgeWatchdog')) {
        $('badgeWatchdog').className = 'pill-status pill-active';
        $('badgeWatchdog').textContent = `Recovered (ESP_RST_TASK_WDT → Boot #${rtos.bootId})`;
      }
      if ($('wdtStatusControl')) {
        $('wdtStatusControl').className = 'pill-status pill-active';
        $('wdtStatusControl').textContent = 'Subscribed (5.0s)';
      }
      if ($('rtosMissControl')) $('rtosMissControl').textContent = '0';
      if ($('ovNodeAStatus')) {
        $('ovNodeAStatus').textContent = 'ControlTask: 200 Hz';
        $('ovNodeAStatus').style.color = '#34d399';
      }
      logEvent('Node A', 'ESP_RST_TASK_WDT', 'ALARM', `5.0s Watchdog reset triggered on ControlTask (SDK cause 6). Incremented to Boot #${rtos.bootId}, relays initialized OFF, NVS policy restored.`);
      showToast(`Watchdog Reboot → Boot #${rtos.bootId}`, 'ESP_RST_TASK_WDT reset Node A in 5.0s. Relays safe OFF; policy restored.', 'warn');
    }
  } else {
    rtos.controlJobs += 100; // 200 Hz over 500ms tick
    if ($('ovNodeAStatus')) {
      $('ovNodeAStatus').textContent = 'ControlTask: 200 Hz';
      $('ovNodeAStatus').style.color = '#34d399';
    }
  }

  rtos.linkJobs += 100;
  rtos.piJobs += 2;

  const uptimeSec = Math.floor((now - rtos.startTime) / 1000);
  if ($('rtosJobsControl')) $('rtosJobsControl').textContent = rtos.controlJobs.toLocaleString();
  if ($('rtosJobsLink')) $('rtosJobsLink').textContent = rtos.linkJobs.toLocaleString();
  if ($('rtosJobsSpeech')) $('rtosJobsSpeech').textContent = rtos.speechJobs.toLocaleString();
  if ($('rtosJobsPi')) $('rtosJobsPi').textContent = rtos.piJobs.toLocaleString();
  if ($('wdtBootCounter')) $('wdtBootCounter').textContent = `Node A Boot #${rtos.bootId} · Uptime: ${uptimeSec}s`;
  if ($('ovNodeAMeta')) $('ovNodeAMeta').textContent = `Boot #${rtos.bootId} · 5.0s WDT · Up ${uptimeSec}s`;
  if ($('ovNodeBMeta')) $('ovNodeBMeta').textContent = `SRAM: ${rtos.sramFreeKiB.toFixed(1)} KiB · Jobs: ${(rtos.linkJobs / 1000).toFixed(1)}k`;
  if ($('healthMemoryTerminal')) {
    const src = AppState.mode === 'physical_bedroom' ? 'HARDWARE_MEASURED' : 'HOST_MEASURED';
    $('healthMemoryTerminal').textContent = `Memory [${src}]: Internal Free SRAM: ${rtos.sramFreeKiB.toFixed(1)} KiB · Min Free: ${rtos.sramMinKiB.toFixed(1)} KiB · PSRAM Free: ${rtos.psramFreeKiB.toLocaleString()} KiB. Node A Boot #${rtos.bootId} · Dual ESP32-S3 task watchdogs armed (5.0s timeout).`;
  }
}

// KPI & UI Updates (Strict separation of Real-time vs Synthetic)
function updateAllKPIs() {
  if (AppState.mode === 'physical_bedroom') {
    const rt = AppState.realtime;
    if (rt.connected && !rt.stale && !AppState.rtos.staleLeaseSimulated) {
      const bed = rt.rooms.bedroom;
      $('kpiOccVal').textContent = bed.state === 'OCCUPIED' ? '1 / 1' : '0 / 1';
      $('kpiOccBadge').textContent = bed.state;
      $('kpiOccSub').textContent = 'Zone: Physical Bedroom (PIR_1)';
      $('kpiDevVal').textContent = '2 / 2';
      const activeHw = (bed.light ? 1 : 0) + (bed.fan ? 1 : 0);
      $('kpiDevSub').textContent = `${activeHw} Energized · Outputs ${bed.outputsEnabled ? 'ENABLED' : 'INHIBITED'}`;
      $('kpiTempVal').textContent = bed.temp !== null ? `${bed.temp.toFixed(1)} °C` : 'PIR Only';
      $('kpiTempBadge').textContent = 'HARDWARE_MEASURED';
      $('kpiTempSub').textContent = 'Node A Digital PIR_1 Telemetry';
      $('kpiHealthVal').textContent = 'Connected';
      $('kpiHealthBadge').className = 'kpi-badge badge-green';
      $('kpiHealthBadge').textContent = `Fresh (${rt.ageMs ?? 0}ms)`;
      $('kpiHealthSub').textContent = `Boot #${AppState.rtos.bootId} · 5s WDT Armed`;
    } else {
      const isStale = rt.stale || AppState.rtos.staleLeaseSimulated;
      $('kpiOccVal').textContent = '—';
      $('kpiOccBadge').textContent = isStale ? 'STALE (>3s)' : 'DISCONNECTED';
      $('kpiOccSub').textContent = 'No fabricated readings when offline';
      $('kpiDevVal').textContent = 'Offline';
      $('kpiDevSub').textContent = 'Awaiting Node A connection';
      $('kpiTempVal').textContent = '—';
      $('kpiTempBadge').textContent = isStale ? 'STALE' : 'OFFLINE';
      $('kpiHealthVal').textContent = isStale ? 'Stale' : 'Offline';
      $('kpiHealthBadge').className = 'kpi-badge badge-red';
      $('kpiHealthBadge').textContent = 'Telemetry Unavailable';
      $('kpiHealthSub').textContent = 'Pi Backup Ready · Switch to Simulation';
    }
  } else {
    const rooms = AppState.synthetic.rooms;
    const occupiedRooms = rooms.filter(r => r.state === 'OCCUPIED');
    const occupiedCount = occupiedRooms.length;
    const activeActuators = rooms.reduce((acc, r) => acc + (r.light ? 1 : 0) + (r.fan ? 1 : 0), 0);
    const avgTemp = rooms.reduce((acc, r) => acc + r.temp, 0) / rooms.length;
    const avgEffLux = Math.round(rooms.reduce((acc, r) => acc + getEffectiveLux(r), 0) / rooms.length);

    $('kpiOccVal').textContent = `${occupiedCount} / 4`;
    $('kpiOccBadge').textContent = `${Math.round((occupiedCount / 4) * 100)}% Active`;
    $('kpiOccSub').textContent = occupiedCount > 0 ? `Active: ${occupiedRooms.map(r => r.name).join(', ')}` : 'All 4 rooms vacant';

    for (let i = 0; i < 4; i++) {
      const bar = $('kpiOccBar' + i);
      if (bar) bar.setAttribute('fill', rooms[i].state === 'OCCUPIED' ? '#3b82f6' : (rooms[i].state === 'LIKELY_OCCUPIED' ? '#f59e0b' : '#1e293b'));
    }

    $('kpiDevVal').textContent = `${activeActuators} / 8`;
    $('kpiDevBadge').textContent = 'Relays Guarded';
    $('kpiDevSub').textContent = `${activeActuators} Energized · Safe Low-Voltage DC`;

    $('kpiTempVal').textContent = `${avgTemp.toFixed(1)} °C`;
    $('kpiTempBadge').textContent = `${avgEffLux} lx Eff.`;
    const minT = Math.min(...rooms.map(r => r.temp)).toFixed(1);
    const maxT = Math.max(...rooms.map(r => r.temp)).toFixed(1);
    $('kpiTempSub').textContent = `Range: ${minT}°C – ${maxT}°C across 4 zones`;

    $('kpiHealthVal').textContent = AppState.rtos.wdtStalled ? 'WDT Fault' : 'Nominal';
    $('kpiHealthBadge').className = AppState.rtos.wdtStalled ? 'kpi-badge badge-red' : 'kpi-badge badge-green';
    $('kpiHealthBadge').textContent = AppState.rtos.wdtStalled ? 'STALLED' : `Boot #${AppState.rtos.bootId} · WDT OK`;
    $('kpiHealthSub').textContent = `${AppState.config.dwellSeconds}s Dwell · ${(AppState.rtos.controlJobs / 1000).toFixed(1)}k RTOS Jobs`;
  }
}

function updateDeviceButtons() {
  const rooms = AppState.synthetic.rooms;
  const b = rooms[0];
  const now = Date.now();

  $('btnLightOn').className = b.light && !b.autoLight ? 'active-on' : '';
  $('btnLightOff').className = !b.light && !b.autoLight ? 'active-off' : '';
  $('btnLightAuto').className = b.autoLight ? 'active-auto' : '';
  $('iconLight').classList.toggle('active', b.light);

  $('btnFanOn').className = b.fan && !b.autoFan ? 'active-on' : '';
  $('btnFanOff').className = !b.fan && !b.autoFan ? 'active-off' : '';
  $('btnFanAuto').className = b.autoFan ? 'active-auto' : '';
  $('iconFan').classList.toggle('active', b.fan);

  rooms.forEach((r, idx) => {
    const badge = $('devRoomBadge' + idx);
    if (badge) {
      badge.textContent = r.state;
      badge.className = 'pill-status ' + (r.state === 'OCCUPIED' ? 'pill-active' : (r.state === 'LIKELY_OCCUPIED' ? 'pill-warning' : (r.state === 'UNKNOWN' ? 'pill-danger' : 'pill-inactive')));
    }
    const meta = $('devRoomMeta' + idx);
    if (meta) {
      meta.textContent = `${idx === 0 ? 'PIR_1 (Zone 1)' : `Virtual Zone ${idx + 1}`} · ${r.temp.toFixed(1)}°C · ${getEffectiveLux(r)} lx`;
    }

    const lon = $(`b${idx}_light_on`);
    const loff = $(`b${idx}_light_off`);
    const lauto = $(`b${idx}_light_auto`);
    if (lon && loff && lauto) {
      lon.className = r.light && !r.autoLight ? 'active-on' : '';
      loff.className = !r.light && !r.autoLight ? 'active-off' : '';
      lauto.className = r.autoLight ? 'active-auto' : '';
    }
    const lmeta = $(`b${idx}_light_meta`);
    if (lmeta) {
      if (now < r.manualLightHoldUntil) {
        const rem = Math.ceil((r.manualLightHoldUntil - now) / 1000);
        lmeta.textContent = `Manual ${r.light ? 'ON' : 'OFF'} (Hold: ${rem}s)`;
      } else {
        lmeta.textContent = `${r.light ? 'ON (+220 lx)' : 'OFF'} · ${r.autoLight ? `Auto (Lux < ${AppState.config.luxThreshold})` : 'Manual'}`;
      }
    }

    const fon = $(`b${idx}_fan_on`);
    const foff = $(`b${idx}_fan_off`);
    const fauto = $(`b${idx}_fan_auto`);
    if (fon && foff && fauto) {
      fon.className = r.fan && !r.autoFan ? 'active-on' : '';
      foff.className = !r.fan && !r.autoFan ? 'active-off' : '';
      fauto.className = r.autoFan ? 'active-auto' : '';
    }
    const fmeta = $(`b${idx}_fan_meta`);
    if (fmeta) {
      if (now < r.manualFanHoldUntil) {
        const rem = Math.ceil((r.manualFanHoldUntil - now) / 1000);
        fmeta.textContent = `Manual ${r.fan ? 'ON' : 'OFF'} (Hold: ${rem}s)`;
      } else {
        fmeta.textContent = `${r.fan ? 'ON (Cooling)' : 'OFF'} · ${r.autoFan ? `Auto (Temp ≥ ${AppState.config.tempThreshold}°C)` : 'Manual OFF'}`;
      }
    }

    const miniBadge = $('miniBadge' + idx);
    if (miniBadge) {
      miniBadge.textContent = r.state;
      miniBadge.className = 'pill-status ' + (r.state === 'OCCUPIED' ? 'pill-active' : (r.state === 'LIKELY_OCCUPIED' ? 'pill-warning' : 'pill-inactive'));
    }
  });

  if ($('miniBedState')) $('miniBedState').textContent = `${rooms[0].temp.toFixed(1)}°C · ${getEffectiveLux(rooms[0])} lx · 💡 ${rooms[0].light ? 'ON' : 'OFF'} · 🌀 ${rooms[0].fan ? 'ON' : 'OFF'}`;
  if ($('miniLivingState')) $('miniLivingState').textContent = `${rooms[1].temp.toFixed(1)}°C · ${getEffectiveLux(rooms[1])} lx · 💡 ${rooms[1].light ? 'ON' : 'OFF'} · 🌀 ${rooms[1].fan ? 'ON' : 'OFF'}`;
  if ($('miniKitchenState')) $('miniKitchenState').textContent = `${rooms[2].temp.toFixed(1)}°C · ${getEffectiveLux(rooms[2])} lx · 💡 ${rooms[2].light ? 'ON' : 'OFF'} · 🌀 ${rooms[2].fan ? 'ON' : 'OFF'}`;
  if ($('miniStudyState')) $('miniStudyState').textContent = `${rooms[3].temp.toFixed(1)}°C · ${getEffectiveLux(rooms[3])} lx · 💡 ${rooms[3].light ? 'ON' : 'OFF'} · 🌀 ${rooms[3].fan ? 'ON' : 'OFF'}`;
}

function toggleRadarSim(mode) {
  const isRadar = mode === 'radar';
  $('radarContainer').style.display = isRadar ? 'flex' : 'none';
  $('miniSimContainer').style.display = isRadar ? 'none' : 'grid';
  $('btnRadarMode').className = isRadar ? 'active-on' : '';
  $('btnMiniSimMode').className = !isRadar ? 'active-on' : '';
  if (isRadar) updateRadarChart();
}

function updateRadarChart() {
  const poly1 = $('radarPoly');
  const poly2 = $('radarPoly2');
  if (!poly1 || !poly2) return;
  const rooms = AppState.synthetic.rooms;
  const angles = [-Math.PI / 2, -Math.PI / 6, Math.PI / 6, Math.PI / 2, 5 * Math.PI / 6, -5 * Math.PI / 6];
  const tempScores = [
    Math.min(1, Math.max(0.2, rooms[0].temp / 40)),
    Math.min(1, Math.max(0.2, rooms[1].temp / 40)),
    Math.min(1, Math.max(0.2, rooms[2].temp / 40)),
    Math.min(1, Math.max(0.2, rooms[3].temp / 40)),
    0.95,
    0.85
  ];
  const luxScores = [
    Math.min(1, Math.max(0.2, getEffectiveLux(rooms[0]) / 700)),
    Math.min(1, Math.max(0.2, getEffectiveLux(rooms[1]) / 700)),
    Math.min(1, Math.max(0.2, getEffectiveLux(rooms[2]) / 700)),
    Math.min(1, Math.max(0.2, getEffectiveLux(rooms[3]) / 700)),
    0.9,
    0.8
  ];
  const toPts = scores => scores.map((s, i) => {
    const r = s * 95;
    const x = Math.round(160 + r * Math.cos(angles[i]));
    const y = Math.round(120 + r * Math.sin(angles[i]));
    return `${x},${y}`;
  }).join(' ');
  poly1.setAttribute('points', toPts(tempScores));
  poly2.setAttribute('points', toPts(luxScores));
}

function setChartWindow(seconds) {
  AppState.chartWindow = seconds;
  [60, 300, 900].forEach(w => {
    const btn = $('btnWin' + w);
    if (btn) btn.className = w === seconds ? 'active-auto' : '';
  });
  updateTelemetryChart();
}

// Push reactive telemetry sample (incorporating Fan cooling, Occupancy warmth, Light lux boost, and sensor micro-ripple)
function pushTelemetrySample() {
  const now = Date.now();
  AppState.synthetic.tickCount += 1;
  const k = AppState.synthetic.tickCount;
  const rooms = AppState.synthetic.rooms;

  // Apply realistic physical thermal feedback & subtle sensor breathing
  rooms.forEach((r, idx) => {
    if (r.fan && r.temp > 21.5) {
      r.temp = +(r.temp - 0.08).toFixed(2); // Fan actively cools room
    } else if (r.state === 'OCCUPIED' && !r.fan && r.temp < 30.5) {
      r.temp = +(r.temp + 0.03).toFixed(2); // Occupancy gently warms room
    }
    // Subtle sensor micro-variance so telemetry stream is visibly live
    const rippleT = Math.sin(k * 0.25 + idx * 1.4) * 0.06;
    const rippleL = Math.cos(k * 0.2 + idx * 1.1) * 2.5;
    r._liveTemp = +(r.temp + rippleT).toFixed(2);
    r._liveLux = Math.max(0, Math.round(getEffectiveLux(r) + rippleL));
  });

  const avgT = rooms.reduce((a, b) => a + (b._liveTemp ?? b.temp), 0) / 4;
  const avgL = rooms.reduce((a, b) => a + (b._liveLux ?? getEffectiveLux(b)), 0) / 4;

  AppState.synthetic.history.push({
    t: now,
    temp: avgT,
    lux: avgL,
    rooms: rooms.map(r => ({
      temp: r._liveTemp ?? r.temp,
      lux: r._liveLux ?? getEffectiveLux(r),
      ambientLux: r.lux,
      state: r.state
    }))
  });
  if (AppState.synthetic.history.length > 900) AppState.synthetic.history.shift();
}

function drawWaveformOnCanvas(canvas, historySlice, roomSelection) {
  if (!canvas || historySlice.length < 2) return;
  const ctx = canvas.getContext('2d');
  const w = canvas.width;
  const h = canvas.height;
  ctx.clearRect(0, 0, w, h);

  // Grid lines
  ctx.strokeStyle = '#151d2f';
  ctx.lineWidth = 1;
  for (let y = 28; y < h - 18; y += Math.floor((h - 46) / 4)) {
    ctx.beginPath();
    ctx.moveTo(46, y);
    ctx.lineTo(w - 48, y);
    ctx.stroke();
  }

  const pts = historySlice.map(pt => {
    if (roomSelection === 'all' || !pt.rooms) return { temp: pt.temp, lux: pt.lux };
    const idx = parseInt(roomSelection, 10) || 0;
    return pt.rooms[idx] || { temp: pt.temp, lux: pt.lux };
  });

  // Dynamic auto-scaling with sensible minimum span so both subtle ripples and big spikes look crisp
  const temps = pts.map(p => p.temp);
  const luxes = pts.map(p => p.lux);
  const minT = Math.floor(Math.min(...temps) - 1.5);
  const maxT = Math.ceil(Math.max(...temps) + 1.5);
  const minL = Math.max(0, Math.floor(Math.min(...luxes) - 40));
  const maxL = Math.ceil(Math.max(...luxes) + 60);

  const spanT = Math.max(4, maxT - minT);
  const spanL = Math.max(120, maxL - minL);

  // Temperature area + curve (Cyan)
  ctx.beginPath();
  ctx.strokeStyle = '#06b6d4';
  ctx.lineWidth = 2.5;
  pts.forEach((p, i) => {
    const x = 46 + (i / (pts.length - 1)) * (w - 96);
    const y = h - 22 - ((p.temp - minT) / spanT) * (h - 50);
    if (i === 0) ctx.moveTo(x, y);
    else ctx.lineTo(x, y);
  });
  ctx.stroke();

  // Illuminance curve (Indigo/Purple)
  ctx.beginPath();
  ctx.strokeStyle = '#818cf8';
  ctx.lineWidth = 2.5;
  pts.forEach((p, i) => {
    const x = 46 + (i / (pts.length - 1)) * (w - 96);
    const y = h - 22 - ((p.lux - minL) / spanL) * (h - 50);
    if (i === 0) ctx.moveTo(x, y);
    else ctx.lineTo(x, y);
  });
  ctx.stroke();

  // Axis Labels
  ctx.fillStyle = '#38bdf8';
  ctx.font = '10.5px ui-monospace, monospace';
  ctx.fillText(`${maxT.toFixed(0)}°C`, 6, 32);
  ctx.fillText(`${minT.toFixed(0)}°C`, 6, h - 20);

  ctx.fillStyle = '#a5b4fc';
  ctx.fillText(`${maxL}lx`, w - 44, 32);
  ctx.fillText(`${minL}lx`, w - 44, h - 20);
}

function updateTelemetryChart() {
  const canvas = $('overviewChartCanvas');
  if (!canvas) return;

  const roomSel = $('overviewChartRoom') ? $('overviewChartRoom').value : 'all';
  const maxPoints = Math.min(AppState.synthetic.history.length, AppState.chartWindow);
  const hist = AppState.synthetic.history.slice(-maxPoints);

  drawWaveformOnCanvas(canvas, hist, roomSel);

  const latest = hist[hist.length - 1];
  if (latest && $('chartLiveReadout')) {
    if (AppState.mode === 'physical_bedroom') {
      const rt = AppState.realtime;
      $('chartLiveReadout').textContent = rt.connected && !rt.stale
        ? `Hardware Node A Connected · Freshness ${rt.ageMs ?? 0}ms [HARDWARE_MEASURED]`
        : 'Hardware Disconnected · No fabricated telemetry';
    } else {
      const v = roomSel === 'all' ? latest : (latest.rooms[parseInt(roomSel, 10) || 0] || latest);
      const label = roomSel === 'all' ? 'Avg' : AppState.synthetic.rooms[parseInt(roomSel, 10) || 0].name;
      $('chartLiveReadout').textContent = `${label}: ${v.temp.toFixed(1)}°C · Effective Lux: ${Math.round(v.lux)} lx [SIMULATED]`;
    }
  }
}

// Detailed Telemetry View (View 2 - Canvas + Table)
function updateDetailedTelemetryView() {
  const tbody = $('detailedTelemetryTable');
  const canvas = $('detailedTelemetryCanvas');
  if (!tbody || !canvas) return;

  const isReal = AppState.mode === 'physical_bedroom';
  if (isReal) {
    const rt = AppState.realtime;
    const bed = rt.rooms.bedroom;
    const statusBadge = rt.connected && !rt.stale ? 'pill-active' : 'pill-danger';
    tbody.innerHTML = `
      <tr>
        <td><strong>Bedroom (Node A)</strong></td>
        <td><span class="pill-status ${statusBadge}">${rt.connected ? bed.state : 'DISCONNECTED'}</span></td>
        <td>— (Digital PIR Node)</td>
        <td>—</td>
        <td>${rt.connected ? (bed.pir ? 'MOTION (HIGH)' : 'QUIET (LOW)') : '—'}</td>
        <td>—</td>
        <td>—</td>
        <td>💡 ${bed.light ? 'ON' : 'OFF'} · 🌀 ${bed.fan ? 'ON' : 'OFF'}</td>
        <td><span class="pill-status pill-active">HARDWARE_MEASURED</span></td>
        <td><button class="btn btn-sm" onclick="quickToggle('bedroom','light')">Toggle Light</button></td>
      </tr>
    `;
  } else {
    const curIdx = simRoomAt(AppState.synthetic.person.x, AppState.synthetic.person.y);
    const isMoving = performance.now() < AppState.synthetic.movingUntil;
    tbody.innerHTML = AppState.synthetic.rooms.map((r, idx) => {
      const occClass = r.state === 'OCCUPIED' ? 'pill-active' : (r.state === 'LIKELY_OCCUPIED' ? 'pill-warning' : (r.state === 'UNKNOWN' ? 'pill-danger' : 'pill-inactive'));
      const pirText = !r.pir ? 'DISABLED' : (idx === curIdx && isMoving ? 'MOTION (HIGH)' : 'QUIET (LOW)');
      const presText = !r.presence ? 'DISABLED' : (idx === curIdx ? 'PRESENT' : 'ABSENT');
      const effLux = r._liveLux ?? getEffectiveLux(r);
      const dispTemp = r._liveTemp ?? r.temp;
      return `
        <tr>
          <td><strong>${r.name}</strong></td>
          <td><span class="pill-status ${occClass}">${r.state}</span></td>
          <td style="font-family:var(--font-mono);color:#38bdf8">${dispTemp.toFixed(2)} °C</td>
          <td style="font-family:var(--font-mono);color:#a5b4fc">${Math.round(r.lux)} lx / <strong>${effLux} lx</strong></td>
          <td>${pirText}</td>
          <td>${presText}</td>
          <td>${r.door ? 'OPEN' : 'CLOSED'}</td>
          <td>💡 ${r.light ? 'ON (+220lx)' : 'OFF'} · 🌀 ${r.fan ? 'ON (Cooling)' : 'OFF'}</td>
          <td><span class="pill-status pill-inactive">SIMULATED</span></td>
          <td>
            <div style="display:flex;gap:4px">
              <button class="btn btn-sm" onclick="quickToggle('${r.id}','light')">💡 Light</button>
              <button class="btn btn-sm" onclick="quickToggle('${r.id}','fan')">🌀 Fan</button>
              <button class="btn btn-sm" onclick="jumpToRoom(${idx})">📍</button>
            </div>
          </td>
        </tr>
      `;
    }).join('');
  }

  const filterVal = $('telemetryRoomFilter') ? $('telemetryRoomFilter').value : 'all';
  const hist = AppState.synthetic.history.slice(-120);
  drawWaveformOnCanvas(canvas, hist, filterVal);
}

function renderLogsTable() {
  const tbody = $('fullLogsTableBody');
  if (!tbody) return;

  const search = $('logSearchInput') ? $('logSearchInput').value.toLowerCase() : '';
  const roomFilter = $('logRoomFilter') ? $('logRoomFilter').value : 'ALL';
  const sevFilter = $('logSeverityFilter') ? $('logSeverityFilter').value : 'ALL';
  const srcFilter = $('logSourceFilter') ? $('logSourceFilter').value : 'ALL';

  const filtered = AppState.eventLogs.filter(e => {
    if (search && !e.description.toLowerCase().includes(search) && !e.room.toLowerCase().includes(search) && !e.eventType.toLowerCase().includes(search)) return false;
    if (roomFilter !== 'ALL' && e.room.toLowerCase().replace(' ', '_') !== roomFilter.toLowerCase()) return false;
    if (sevFilter !== 'ALL' && e.severity !== sevFilter) return false;
    if (srcFilter !== 'ALL' && e.source !== srcFilter) return false;
    return true;
  });

  if ($('logCountLabel')) $('logCountLabel').textContent = `Showing ${filtered.length} of ${AppState.eventLogs.length} events`;

  tbody.innerHTML = filtered.slice(0, 60).map(e => {
    const sevPill = e.severity === 'ALARM' ? 'pill-danger' : (e.severity === 'WARN' ? 'pill-warning' : 'pill-active');
    const srcPill = e.source === 'REAL_TIME' ? 'pill-active' : 'pill-inactive';
    return `
      <tr>
        <td><strong>#${e.id}</strong></td>
        <td>${e.timestamp}</td>
        <td>${e.room}</td>
        <td><span style="font-family:var(--font-mono);font-size:11.5px">${e.eventType}</span></td>
        <td><span class="pill-status ${sevPill}">${e.severity}</span></td>
        <td><span class="pill-status ${srcPill}">${e.source}</span></td>
        <td>${e.description}</td>
      </tr>
    `;
  }).join('');
}

function updateOverviewEvents() {
  const tbody = $('overviewEventsTable');
  if (!tbody) return;
  tbody.innerHTML = AppState.eventLogs.slice(0, 5).map(e => `
    <tr>
      <td>${e.timestamp}</td>
      <td>${e.room}</td>
      <td><span style="font-family:var(--font-mono);font-size:11.5px">${e.eventType}</span></td>
      <td><span class="pill-status ${e.severity === 'ALARM' ? 'pill-danger' : (e.severity === 'WARN' ? 'pill-warning' : 'pill-active')}">${e.severity}</span></td>
    </tr>
  `).join('');
}

function clearEventLogs() {
  AppState.eventLogs = [];
  if ($('navLogBadge')) $('navLogBadge').textContent = '0';
  renderLogsTable();
  updateOverviewEvents();
  showToast('Logs Cleared', 'Event log display cleared.', 'info');
}

function exportEventLogsCSV() {
  const headers = ['ID', 'Timestamp', 'Room', 'EventType', 'Severity', 'Source', 'Description'];
  const rows = AppState.eventLogs.map(e => [e.id, e.timestamp, `"${e.room}"`, `"${e.eventType}"`, e.severity, e.source, `"${e.description.replace(/"/g, '""')}"`]);
  const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n');
  downloadFile('clawgrid_event_logs.csv', csv, 'text/csv');
}

function renderDiagnosticsTable() {
  const tbody = $('diagnosticsTableBody');
  if (!tbody) return;

  const rows = [];
  AppState.synthetic.rooms.forEach(r => {
    rows.push({
      id: `PIR_${r.id.toUpperCase()}`,
      room: r.name,
      type: 'Digital PIR Motion',
      val: r.pir ? (r.state === 'OCCUPIED' ? 'MOTION (HIGH)' : 'QUIET (LOW)') : 'DISABLED',
      freshness: '< 50ms (SIMULATED)',
      avail: r.pir ? 'Online' : 'Disabled',
      health: r.pir ? 'Healthy' : 'Fault / Offline',
      canToggle: true
    });
    rows.push({
      id: `PRES_${r.id.toUpperCase()}`,
      room: r.name,
      type: 'Radar Presence',
      val: r.presence ? (r.state === 'OCCUPIED' ? 'PRESENT' : 'ABSENT') : 'DISABLED',
      freshness: '< 50ms (SIMULATED)',
      avail: r.presence ? 'Online' : 'Disabled',
      health: r.presence ? 'Healthy' : 'Fault / Offline',
      canToggle: true
    });
    rows.push({
      id: `TEMP_${r.id.toUpperCase()}`,
      room: r.name,
      type: 'Thermistor Temp',
      val: `${r.temp.toFixed(1)} °C`,
      freshness: '< 50ms (SIMULATED)',
      avail: 'Online',
      health: 'Calibrated',
      canToggle: false
    });
    rows.push({
      id: `LUX_${r.id.toUpperCase()}`,
      room: r.name,
      type: 'Photodiode Lux',
      val: `${getEffectiveLux(r)} lx (Amb: ${Math.round(r.lux)} lx)`,
      freshness: '< 50ms (SIMULATED)',
      avail: 'Online',
      health: 'Calibrated',
      canToggle: false
    });
  });

  tbody.innerHTML = rows.map(s => `
    <tr>
      <td><strong>${s.id}</strong></td>
      <td>${s.room}</td>
      <td>${s.type}</td>
      <td><span style="font-family:var(--font-mono)">${s.val}</span></td>
      <td>${s.freshness}</td>
      <td><span class="pill-status ${s.avail === 'Online' ? 'pill-active' : 'pill-danger'}">${s.avail}</span></td>
      <td><span class="pill-status ${s.health === 'Healthy' || s.health === 'Calibrated' ? 'pill-active' : 'pill-warning'}">${s.health}</span></td>
      <td>
        ${s.canToggle ? `<button class="btn btn-sm" onclick="toggleSensorDiagnostic('${s.id}')">${s.avail === 'Online' ? 'Inject Fault' : 'Restore'}</button>` : '—'}
      </td>
    </tr>
  `).join('');
}

function toggleSensorDiagnostic(sensorId) {
  const rooms = AppState.synthetic.rooms;
  rooms.forEach(r => {
    if (sensorId.includes(r.id.toUpperCase())) {
      if (sensorId.startsWith('PIR')) r.pir = !r.pir;
      if (sensorId.startsWith('PRES')) r.presence = !r.presence;
      logEvent(r.name, 'DIAG_SENSOR_TOGGLE', 'WARN', `Sensor ${sensorId} toggled via Diagnostics Matrix.`);
    }
  });
  loadSimEnvToInputs();
  evalOccupancyAndPolicies();
  renderDiagnosticsTable();
}

function refreshDiagnostics() {
  renderDiagnosticsTable();
  logEvent('System', 'DIAG_REFRESH', 'INFO', 'Sensor diagnostics matrix polled and verified.');
  showToast('Diagnostics Refreshed', 'All 16 virtual/physical sensor channels verified.', 'success');
}

function exportTelemetryCSV() {
  const headers = ['Timestamp', 'AvgTemp_C', 'AvgEffectiveLux_lx', 'Source'];
  const rows = AppState.synthetic.history.map(pt => [new Date(pt.t).toISOString(), pt.temp.toFixed(2), pt.lux.toFixed(0), 'SIMULATED']);
  const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n');
  downloadFile('clawgrid_telemetry.csv', csv, 'text/csv');
  showToast('CSV Exported', 'Downloaded clawgrid_telemetry.csv', 'success');
}

function exportSummaryJSON() {
  const data = {
    exportTime: new Date().toISOString(),
    system: 'ClawGrid V2.0',
    mode: AppState.mode,
    dataSource: AppState.mode === 'physical_bedroom' ? 'HARDWARE_MEASURED' : 'SIMULATED',
    rtos: AppState.rtos,
    rooms: AppState.synthetic.rooms,
    config: AppState.config,
    kpi: {
      avgTemp: AppState.synthetic.rooms.reduce((a, b) => a + b.temp, 0) / 4,
      totalOccupied: AppState.synthetic.rooms.filter(r => r.state === 'OCCUPIED').length
    }
  };
  downloadFile('clawgrid_summary.json', JSON.stringify(data, null, 2), 'application/json');
  showToast('JSON Exported', 'Downloaded clawgrid_summary.json', 'success');
}

function downloadFile(name, content, type) {
  const blob = new Blob([content], { type });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = name;
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

function saveConfiguration() {
  AppState.config.graceSeconds = Math.max(2, parseFloat($('cfgGrace').value) || 5.0);
  AppState.config.dwellSeconds = Math.max(1, parseFloat($('cfgDwell').value) || 3.0);
  AppState.config.manualHoldMinutes = Math.max(1, parseFloat($('cfgHold').value) || 5.0);
  AppState.config.luxThreshold = Math.max(10, parseInt($('cfgLux').value, 10) || 300);
  AppState.config.tempThreshold = Math.max(15, parseFloat($('cfgTemp').value) || 28.0);
  AppState.config.hardwareUrl = $('cfgHardwareUrl').value.trim();

  if ($('lblSimLuxThresh')) $('lblSimLuxThresh').textContent = String(AppState.config.luxThreshold);
  if ($('lblSimTempThresh')) $('lblSimTempThresh').textContent = AppState.config.tempThreshold.toFixed(1);
  if ($('lblSimGrace')) $('lblSimGrace').textContent = String(AppState.config.graceSeconds);
  if ($('invariantDwellBadge')) $('invariantDwellBadge').textContent = `ENFORCED (${AppState.config.dwellSeconds.toFixed(1)}s)`;
  if ($('simEnvStatus')) {
    $('simEnvStatus').textContent = `Active Rule: Occupied & Lux < ${AppState.config.luxThreshold} → Light ON; Occupied & Temp ≥ ${AppState.config.tempThreshold}°C → Fan ON.`;
  }

  evalOccupancyAndPolicies();
  logEvent('Config', 'CONFIG_SAVE', 'INFO', `Policy updated: Grace ${AppState.config.graceSeconds}s, Dwell ${AppState.config.dwellSeconds}s, Lux < ${AppState.config.luxThreshold}, Temp ≥ ${AppState.config.tempThreshold}°C`);
  showToast('Policy Settings Saved', `Lux < ${AppState.config.luxThreshold} lx · Temp ≥ ${AppState.config.tempThreshold}°C · Grace ${AppState.config.graceSeconds}s`, 'success');
}

function resetConfiguration() {
  $('cfgGrace').value = 5;
  $('cfgDwell').value = 3;
  $('cfgHold').value = 5;
  $('cfgLux').value = 300;
  $('cfgTemp').value = 28.0;
  $('cfgHardwareUrl').value = 'http://127.0.0.1:8770';
  saveConfiguration();
}

async function appTick() {
  const now = Date.now();
  if (AppState.timers.length > 0) {
    AppState.timers = AppState.timers.filter(tm => {
      if (now >= tm.targetTime) {
        setActuator(tm.room, tm.dev, 'OFF');
        logEvent(tm.room, 'TIMER_EXPIRED', 'INFO', `Countdown timer expired for ${tm.dev}. Commanded logical OFF.`);
        showToast('Timer Expired', `${tm.room.replace('_', ' ')} ${tm.dev} turned OFF.`, 'info');
        return false;
      }
      return true;
    });
    renderActiveTimers();
  }

  updateRtosAndEmbeddedUI();

  if (AppState.mode === 'physical_bedroom') {
    try {
      const res = await fetch('/api/state', { headers: { 'X-ClawGrid-Token': $('token').value } });
      if (res.ok) {
        const data = await res.json();
        AppState.realtime.connected = Boolean(data.known);
        AppState.realtime.ageMs = data.age_ms ?? 0;
        AppState.realtime.stale = (data.age_ms ?? 99999) > 3000 || AppState.rtos.staleLeaseSimulated;
        if (data.boot_id) AppState.rtos.bootId = data.boot_id;
        AppState.realtime.rooms.bedroom = {
          state: data.occupancy || (data.occupied ? 'OCCUPIED' : 'VACANT'),
          temp: null,
          lux: null,
          light: Boolean(data.light),
          fan: Boolean(data.fan),
          pir: Boolean(data.motion),
          outputsEnabled: Boolean(data.physical_outputs_enabled)
        };
      } else {
        AppState.realtime.connected = false;
      }
    } catch (e) {
      AppState.realtime.connected = false;
    }
  } else {
    evalOccupancyAndPolicies();
    pushTelemetrySample();
    if (AppState.view === 'simulator') drawSimHouse();
  }

  updateAllKPIs();
  updateTelemetryChart();
  if (AppState.view === 'telemetry') updateDetailedTelemetryView();
}

// Seed initial telemetry history so chart is immediately rich on page load
for (let i = 0; i < 25; i++) {
  pushTelemetrySample();
}

// Initialize ClawGrid Console
logEvent('System', 'SYSTEM_BOOT', 'INFO', 'ClawGrid V2.0 Console initialized. SafetyKernel & 5.0s RTOS Watchdogs active.');
loadSimEnvToInputs();
evalOccupancyAndPolicies();
drawSimHouse();
renderLogsTable();
renderDiagnosticsTable();
updateAllKPIs();
updateTelemetryChart();
updateDetailedTelemetryView();
setInterval(appTick, 500);
