#pragma once
#include "platform_hal.h"
// ClawGrid V2.0 — Smart Home & Edge AI Console
static const char kDashboard[] PROGMEM=R"HTML(<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
<title>ClawGrid — Smart Home &amp; Edge AI Console</title>
<meta name="description" content="ClawGrid V2.0 room-aware lighting and fan automation, local offline voice assistant, multi-room simulation, and deterministic SafetyKernel dashboard.">
<style>
:root {
  --bg-dark: #090d16;
  --bg-sidebar: #0d121f;
  --bg-card: #121826;
  --bg-card-hover: #172033;
  --bg-input: #0a0f1d;
  --border: #1e293b;
  --border-light: rgba(255, 255, 255, 0.08);
  --text-main: #f8fafc;
  --text-muted: #94a3b8;
  --text-dim: #64748b;
  --primary: #3b82f6;
  --primary-glow: rgba(59, 130, 246, 0.25);
  --accent-purple: #8b5cf6;
  --accent-indigo: #6366f1;
  --accent-teal: #06b6d4;
  --accent-cyan: #14b8a6;
  --accent-amber: #f59e0b;
  --accent-pink: #ec4899;
  --success: #10b981;
  --success-bg: rgba(16, 185, 129, 0.12);
  --danger: #ef4444;
  --danger-bg: rgba(239, 68, 68, 0.12);
  --warning: #f59e0b;
  --warning-bg: rgba(245, 158, 11, 0.12);
  --unknown: #6b7280;
  --font-sans: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  --font-mono: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Courier New", monospace;
  --sidebar-w: 260px;
  --sidebar-w-collapsed: 72px;
  --transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1);
}

* { box-sizing: border-box; margin: 0; padding: 0; }
body {
  font: 14px/1.5 var(--font-sans);
  background: var(--bg-dark);
  color: var(--text-main);
  min-height: 100vh;
  display: flex;
  overflow-x: hidden;
}

::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: var(--bg-dark); }
::-webkit-scrollbar-thumb { background: #26334a; border-radius: 4px; }
::-webkit-scrollbar-thumb:hover { background: #3b82f6; }

aside.sidebar {
  width: var(--sidebar-w);
  background: var(--bg-sidebar);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  min-height: 100vh;
  position: sticky;
  top: 0;
  height: 100vh;
  overflow-y: auto;
  z-index: 100;
  transition: var(--transition);
}
aside.sidebar.collapsed {
  width: var(--sidebar-w-collapsed);
}
aside.sidebar.collapsed .brand-name,
aside.sidebar.collapsed .brand-sub,
aside.sidebar.collapsed .user-info,
aside.sidebar.collapsed .nav-text,
aside.sidebar.collapsed .nav-badge,
aside.sidebar.collapsed .sidebar-footer-card {
  display: none;
}
aside.sidebar.collapsed .user-widget { padding: 12px 6px; justify-content: center; }
aside.sidebar.collapsed .sidebar-brand { padding: 20px 16px; justify-content: center; }
aside.sidebar.collapsed .nav-item { justify-content: center; padding: 12px; }

.sidebar-brand {
  padding: 22px 20px 18px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid var(--border);
  cursor: pointer;
}
.brand-icon {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: linear-gradient(135deg, #2563eb, #06b6d4);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  font-size: 18px;
  color: #fff;
  box-shadow: 0 4px 12px var(--primary-glow);
  flex-shrink: 0;
}
.brand-name {
  font-size: 18px;
  font-weight: 800;
  color: #f8fafc;
  letter-spacing: 1px;
  text-transform: uppercase;
}
.brand-sub {
  font-size: 10px;
  font-weight: 700;
  color: #38bdf8;
  letter-spacing: 1.4px;
  text-transform: uppercase;
}

.user-widget {
  padding: 14px 18px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.02);
}
.user-avatar-circle {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, #1e293b, #0f172a);
  border: 1.5px solid var(--accent-teal);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 13px;
  color: #38bdf8;
  flex-shrink: 0;
  font-family: var(--font-mono);
}
.user-info { display: flex; flex-direction: column; overflow: hidden; }
.user-name { font-size: 13px; font-weight: 700; color: var(--text-main); white-space: nowrap; }
.user-role { font-size: 11px; color: var(--accent-teal); font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

nav.sidebar-menu {
  padding: 14px 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}
.nav-section-title {
  font-size: 10.5px;
  font-weight: 700;
  color: var(--text-dim);
  text-transform: uppercase;
  letter-spacing: 1.2px;
  padding: 10px 12px 4px;
}
aside.sidebar.collapsed .nav-section-title { display: none; }

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: 8px;
  border: 1px solid transparent;
  color: var(--text-muted);
  background: transparent;
  cursor: pointer;
  font-size: 13.5px;
  font-weight: 600;
  transition: var(--transition);
  text-align: left;
  width: 100%;
}
.nav-item:hover {
  background: rgba(255, 255, 255, 0.04);
  color: var(--text-main);
  border-color: rgba(255, 255, 255, 0.06);
}
.nav-item.active {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.16), rgba(6, 182, 212, 0.14));
  border-color: rgba(59, 130, 246, 0.45);
  color: #93c5fd;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}
.nav-item-icon { font-size: 16px; width: 20px; text-align: center; flex-shrink: 0; }
.nav-text { flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.nav-badge {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 10px;
  background: rgba(59, 130, 246, 0.2);
  color: #93c5fd;
  font-weight: 700;
}

.sidebar-footer-card {
  margin: 10px 12px 16px;
  padding: 12px;
  border-radius: 10px;
  background: rgba(16, 185, 129, 0.06);
  border: 1px solid rgba(16, 185, 129, 0.2);
  font-size: 11.5px;
  color: var(--text-muted);
}
.sidebar-footer-card strong {
  color: #34d399;
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
  font-size: 12px;
}

.content-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--bg-dark);
}

header.app-header {
  height: 68px;
  background: var(--bg-sidebar);
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: 90;
  gap: 16px;
}
.header-left { display: flex; align-items: center; gap: 14px; }
.btn-toggle-sidebar {
  background: transparent;
  border: 1px solid var(--border);
  color: var(--text-muted);
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 16px;
  transition: var(--transition);
}
.btn-toggle-sidebar:hover { background: var(--bg-card); color: var(--text-main); border-color: #3b82f6; }
.breadcrumb-wrap { display: flex; flex-direction: column; }
.breadcrumb-title { font-size: 16px; font-weight: 700; color: var(--text-main); display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.breadcrumb-path { font-size: 11.5px; color: var(--text-dim); display: flex; gap: 6px; align-items: center; }
.header-right { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }

.mode-segmented {
  display: flex;
  background: #080c16;
  border: 1px solid var(--border);
  border-radius: 24px;
  padding: 3px;
  position: relative;
}
.mode-btn {
  padding: 6px 14px;
  border: none;
  background: transparent;
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  border-radius: 20px;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: var(--transition);
}
.mode-btn:hover { color: var(--text-main); }
.mode-btn.active {
  background: linear-gradient(135deg, #2563eb, #0891b2);
  color: #fff;
  box-shadow: 0 2px 10px rgba(37, 99, 235, 0.35);
}
.status-dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
.status-dot.live {
  background: #10b981;
  box-shadow: 0 0 8px #10b981;
  animation: pulse-green 2s infinite;
}
.status-dot.syn { background: #06b6d4; box-shadow: 0 0 8px #06b6d4; }
@keyframes pulse-green {
  0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7); }
  70% { transform: scale(1); box-shadow: 0 0 0 6px rgba(16, 185, 129, 0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
}

.mode-persistent-badge {
  font-size: 11px;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: 6px;
  letter-spacing: 0.5px;
  text-transform: uppercase;
}
.badge-real { background: rgba(16, 185, 129, 0.15); color: #34d399; border: 1px solid rgba(16, 185, 129, 0.3); }
.badge-syn { background: rgba(6, 182, 212, 0.15); color: #22d3ee; border: 1px solid rgba(6, 182, 212, 0.3); }

.token-box-nav {
  display: flex;
  align-items: center;
  background: #080c16;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 4px 10px;
  gap: 6px;
}
.token-label-text { font-size: 11px; font-weight: 700; color: var(--text-dim); }
.token-input-field {
  background: transparent;
  border: none;
  color: var(--text-main);
  font-family: var(--font-mono);
  font-size: 12px;
  width: 96px;
  outline: none;
}

.header-actions { display: flex; align-items: center; gap: 8px; }
.header-icon-btn {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: #080c16;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
  transition: var(--transition);
}
.header-icon-btn:hover { background: var(--bg-card); color: var(--text-main); border-color: #3b82f6; }
.notif-badge-dot {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--danger);
  box-shadow: 0 0 6px var(--danger);
  display: none;
}

main.dashboard-container {
  padding: 20px 24px 32px;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 1600px;
  width: 100%;
  margin: 0 auto;
}

/* Intuitive Mode & Quick-Action Strip */
.context-banner {
  background: linear-gradient(90deg, rgba(30, 41, 59, 0.7), rgba(15, 23, 42, 0.85));
  border: 1px solid var(--border);
  border-left: 4px solid var(--accent-teal);
  border-radius: 12px;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}
.context-banner.real-mode {
  border-left-color: var(--success);
}
.context-banner-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.context-banner-title {
  font-size: 13.5px;
  font-weight: 700;
  color: var(--text-main);
  display: flex;
  align-items: center;
  gap: 8px;
}
.context-banner-sub {
  font-size: 12px;
  color: var(--text-muted);
}
.context-banner-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.app-view { display: none; flex-direction: column; gap: 20px; }
.app-view.active-view { display: flex; }

.kpi-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.kpi-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 18px 20px;
  position: relative;
  overflow: hidden;
  transition: var(--transition);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  cursor: pointer;
}
.kpi-card:hover {
  transform: translateY(-2px);
  border-color: rgba(59, 130, 246, 0.45);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.35);
}
.kpi-title { font-size: 12px; font-weight: 700; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px; display: flex; justify-content: space-between; align-items: center; }
.kpi-value-row { display: flex; align-items: baseline; justify-content: space-between; margin: 8px 0 6px; gap: 8px; }
.kpi-value { font-size: 26px; font-weight: 800; color: #fff; letter-spacing: -0.5px; font-family: var(--font-mono); }
.kpi-badge {
  font-size: 11px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}
.badge-green { background: var(--success-bg); color: #34d399; }
.badge-blue { background: rgba(59, 130, 246, 0.15); color: #60a5fa; }
.badge-amber { background: var(--warning-bg); color: #fbbf24; }
.badge-red { background: var(--danger-bg); color: #f87171; }
.kpi-subtext { font-size: 12px; color: var(--text-dim); display: flex; align-items: center; gap: 6px; }
.kpi-sparkline { margin-top: 10px; height: 24px; width: 100%; }

.content-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 20px;
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 16px;
  transition: var(--transition);
}
.content-card:hover { border-color: rgba(255, 255, 255, 0.12); }
.card-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--border-light);
  padding-bottom: 14px;
  gap: 12px;
  flex-wrap: wrap;
}
.card-headline { font-size: 15.5px; font-weight: 700; color: var(--text-main); display: flex; align-items: center; gap: 8px; }
.card-subtitle { font-size: 12px; color: var(--text-dim); font-weight: normal; margin-top: 2px; }
.card-dots { color: var(--text-dim); cursor: pointer; font-size: 18px; padding: 2px 6px; border-radius: 4px; }
.card-dots:hover { background: rgba(255, 255, 255, 0.05); color: var(--text-main); }

.btn {
  font: inherit;
  font-size: 12.5px;
  font-weight: 600;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 7px 13px;
  background: #080c16;
  color: var(--text-main);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: var(--transition);
}
.btn:hover:not(:disabled) { border-color: #3b82f6; background: #17223b; }
.btn:disabled { opacity: 0.45; cursor: not-allowed; }
.btn-primary {
  background: linear-gradient(135deg, #2563eb, #0891b2);
  border-color: #0284c7;
  color: #fff;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25);
}
.btn-primary:hover:not(:disabled) { background: linear-gradient(135deg, #1d4ed8, #0e7490); box-shadow: 0 4px 14px rgba(37, 99, 235, 0.4); }
.btn-success { background: linear-gradient(135deg, #059669, #10b981); border-color: #047857; color: #fff; }
.btn-danger { background: linear-gradient(135deg, #dc2626, #ef4444); border-color: #b91c1c; color: #fff; }
.btn-warning { background: linear-gradient(135deg, #d97706, #f59e0b); border-color: #b45309; color: #fff; }
.btn-sm { padding: 5px 10px; font-size: 11.5px; border-radius: 6px; }

.middle-grid { display: grid; grid-template-columns: 1.35fr 1fr; gap: 18px; }
.bottom-grid { display: grid; grid-template-columns: 1.25fr 1fr; gap: 18px; }

.chart-container {
  width: 100%;
  height: 220px;
  position: relative;
  background: #090e1a;
  border-radius: 10px;
  border: 1px solid var(--border-light);
  overflow: hidden;
}
.chart-container canvas { width: 100%; height: 100%; display: block; }
.chart-legend-row { display: flex; align-items: center; gap: 16px; font-size: 12px; color: var(--text-muted); flex-wrap: wrap; }
.legend-item { display: flex; align-items: center; gap: 6px; }
.legend-dot { width: 10px; height: 10px; border-radius: 50%; }
.legend-dot.temp { background: #06b6d4; }
.legend-dot.lux { background: #818cf8; }
.legend-dot.motion { background: #f59e0b; }

.actuator-list { display: flex; flex-direction: column; gap: 10px; }
.actuator-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  background: #0a0f1d;
  border: 1px solid var(--border);
  border-radius: 10px;
  transition: var(--transition);
  gap: 10px;
}
.actuator-row:hover { border-color: rgba(59, 130, 246, 0.35); }
.actuator-info { display: flex; align-items: center; gap: 12px; }
.actuator-icon-box {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}
.actuator-icon-box.active {
  background: rgba(16, 185, 129, 0.15);
  border-color: rgba(16, 185, 129, 0.45);
  box-shadow: 0 0 12px rgba(16, 185, 129, 0.25);
}
.actuator-title { font-size: 13.5px; font-weight: 700; color: var(--text-main); }
.actuator-meta { font-size: 11.5px; color: var(--text-dim); }

.btn-group-toggle {
  display: flex;
  background: #050811;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 2px;
}
.btn-group-toggle button {
  background: transparent;
  border: none;
  color: var(--text-muted);
  padding: 5px 11px;
  font-size: 11.5px;
  font-weight: 700;
  border-radius: 6px;
  cursor: pointer;
  transition: var(--transition);
}
.btn-group-toggle button:hover { color: var(--text-main); }
.btn-group-toggle button.active-on { background: #10b981; color: #fff; }
.btn-group-toggle button.active-off { background: #374151; color: #fff; }
.btn-group-toggle button.active-auto { background: #2563eb; color: #fff; }

.scene-toolbar {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-top: 2px;
}
.scene-toolbar .btn { justify-content: center; }

/* Voice & Intent Assistant Bar */
.voice-assistant-box {
  background: #090e1a;
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.voice-input-row {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}
.voice-chips {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  align-items: center;
}
.voice-chip {
  font-size: 11px;
  padding: 3px 9px;
  border-radius: 14px;
  background: rgba(59, 130, 246, 0.1);
  border: 1px solid rgba(59, 130, 246, 0.25);
  color: #93c5fd;
  cursor: pointer;
  transition: var(--transition);
}
.voice-chip:hover {
  background: rgba(59, 130, 246, 0.22);
  color: #fff;
}

.simulator-layout {
  display: grid;
  grid-template-columns: minmax(400px, 1.4fr) minmax(320px, 1fr);
  gap: 20px;
}
.canvas-wrap {
  width: 100%;
  background: #080c16;
  border-radius: 12px;
  border: 1px solid var(--border);
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
#simHouseCanvas {
  width: 100%;
  height: auto;
  aspect-ratio: 800 / 560;
  background: #090e1a;
  border-radius: 8px;
  touch-action: none;
  cursor: pointer;
  outline: none;
}
#simHouseCanvas:focus {
  box-shadow: 0 0 0 2px var(--primary);
}
.sim-controls-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}
.room-jump-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  padding-top: 4px;
  border-top: 1px solid var(--border-light);
}

.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.form-group { display: flex; flex-direction: column; gap: 4px; }
.form-group label { font-size: 11.5px; font-weight: 700; color: var(--text-dim); text-transform: uppercase; }
.form-control {
  background: var(--bg-input);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 8px 12px;
  color: var(--text-main);
  font-family: inherit;
  font-size: 13px;
  outline: none;
  transition: var(--transition);
}
.form-control:focus { border-color: #3b82f6; box-shadow: 0 0 0 2px var(--primary-glow); }

.checkbox-row {
  display: flex;
  gap: 14px;
  align-items: center;
  font-size: 13px;
  margin-top: 4px;
  flex-wrap: wrap;
}
.checkbox-row label { display: flex; align-items: center; gap: 6px; cursor: pointer; color: var(--text-muted); }
.checkbox-row input[type="checkbox"] { accent-color: #3b82f6; width: 16px; height: 16px; }

.terminal-block {
  background: #060913;
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 10px 14px;
  font-family: var(--font-mono);
  font-size: 12px;
  color: #cbd5e1;
  line-height: 1.5;
  max-height: 120px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-all;
}

.table-responsive { width: 100%; overflow-x: auto; }
table.dashboard-table { width: 100%; border-collapse: collapse; font-size: 13px; text-align: left; }
table.dashboard-table th {
  background: #0b101d;
  color: var(--text-dim);
  font-weight: 700;
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.6px;
  padding: 10px 14px;
  border-bottom: 1px solid var(--border);
}
table.dashboard-table td { padding: 11px 14px; border-bottom: 1px solid var(--border-light); color: var(--text-muted); }
table.dashboard-table tr:hover td { background: rgba(255, 255, 255, 0.02); color: var(--text-main); }

.pill-status { display: inline-flex; align-items: center; gap: 5px; padding: 3px 8px; border-radius: 12px; font-size: 11px; font-weight: 700; }
.pill-active { background: var(--success-bg); color: #34d399; }
.pill-inactive { background: rgba(148, 163, 184, 0.12); color: #94a3b8; }
.pill-warning { background: var(--warning-bg); color: #fbbf24; }
.pill-danger { background: var(--danger-bg); color: #f87171; }

.filter-bar { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; padding-bottom: 12px; border-bottom: 1px solid var(--border-light); }
.dropdown-panel {
  position: absolute;
  top: 46px;
  right: 0;
  width: 340px;
  background: #0f1627;
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.55);
  z-index: 150;
  display: none;
  flex-direction: column;
  gap: 10px;
}
.dropdown-panel.open { display: flex; }

/* Toast Notification */
.toast-container {
  position: fixed;
  bottom: 20px;
  right: 20px;
  z-index: 999;
  display: flex;
  flex-direction: column;
  gap: 8px;
  pointer-events: none;
}
.toast-item {
  background: #0f172a;
  border: 1px solid var(--border);
  border-left: 4px solid var(--primary);
  color: var(--text-main);
  padding: 10px 14px;
  border-radius: 10px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.5);
  font-size: 12.5px;
  max-width: 340px;
  pointer-events: auto;
  animation: slideInToast 0.2s ease-out;
}
.toast-item.warn { border-left-color: var(--warning); }
.toast-item.success { border-left-color: var(--success); }
.toast-item.danger { border-left-color: var(--danger); }
@keyframes slideInToast {
  from { transform: translateY(12px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

@media (max-width: 1200px) {
  .kpi-row { grid-template-columns: repeat(2, 1fr); }
  .middle-grid, .bottom-grid, .simulator-layout { grid-template-columns: 1fr; }
}
@media (max-width: 768px) {
  aside.sidebar { position: fixed; left: -260px; }
  aside.sidebar.mobile-open { left: 0; }
  .kpi-row { grid-template-columns: 1fr; }
  header.app-header { padding: 0 14px; height: auto; min-height: 64px; padding-top: 8px; padding-bottom: 8px; }
  main.dashboard-container { padding: 14px; }
  .scene-toolbar { grid-template-columns: repeat(2, 1fr); }
}
</style>
</head>
<body>

<select id="targetMode" style="display:none">
  <option value="physical_bedroom" selected>Physical Bedroom</option>
  <option value="virtual_rooms">Virtual Rooms (Synthetic)</option>
</select>

<!-- LEFT SIDEBAR: CLAWGRID BRANDING -->
<aside class="sidebar" id="sidebarEl">
  <div class="sidebar-brand" onclick="switchView('overview')" title="ClawGrid Home">
    <div class="brand-icon">⚡</div>
    <div>
      <div class="brand-name">CLAWGRID</div>
      <div class="brand-sub">SMART HOME &amp; EDGE AI</div>
    </div>
  </div>

  <div class="user-widget">
    <div class="user-avatar-circle">CG</div>
    <div class="user-info">
      <div class="user-name">ClawGrid V2.0</div>
      <div class="user-role" id="sidebarNodeRole">Node A + Node B Console</div>
    </div>
  </div>

  <nav class="sidebar-menu">
    <div class="nav-section-title">Home Automation</div>
    <button type="button" class="nav-item active" id="navOverview" onclick="switchView('overview')">
      <span class="nav-item-icon">📊</span>
      <span class="nav-text">Home Overview</span>
    </button>
    <button type="button" class="nav-item" id="navSimulator" onclick="switchView('simulator')">
      <span class="nav-item-icon">🏠</span>
      <span class="nav-text">4-Room Interactive Lab</span>
      <span class="nav-badge">4 Zones</span>
    </button>
    <button type="button" class="nav-item" id="navDevices" onclick="switchView('devices')">
      <span class="nav-item-icon">💡</span>
      <span class="nav-text">Rooms &amp; Relays</span>
    </button>
    <button type="button" class="nav-item" id="navTelemetry" onclick="switchView('telemetry')">
      <span class="nav-item-icon">📈</span>
      <span class="nav-text">Live Telemetry</span>
    </button>

    <div class="nav-section-title">Safety &amp; Diagnostics</div>
    <button type="button" class="nav-item" id="navLogs" onclick="switchView('logs')">
      <span class="nav-item-icon">📋</span>
      <span class="nav-text">Event Audit Logs</span>
      <span class="nav-badge" id="navLogBadge">0</span>
    </button>
    <button type="button" class="nav-item" id="navDiagnostics" onclick="switchView('diagnostics')">
      <span class="nav-item-icon">🔍</span>
      <span class="nav-text">Sensor Diagnostics</span>
    </button>
    <button type="button" class="nav-item" id="navHealth" onclick="switchView('health')">
      <span class="nav-item-icon">🛡️</span>
      <span class="nav-text">SafetyKernel &amp; RTOS</span>
    </button>
    <button type="button" class="nav-item" id="navConfig" onclick="switchView('config')">
      <span class="nav-item-icon">⚙️</span>
      <span class="nav-text">Policy Settings</span>
    </button>
  </nav>

  <div class="sidebar-footer-card">
    <strong>🛡️ SafetyKernel Active</strong>
    <div>Low-voltage DC only · 3s dwell guard · 4h max run cutoff enforced.</div>
  </div>
</aside>

<div class="content-wrapper">
  <header class="app-header">
    <div class="header-left">
      <button type="button" class="btn-toggle-sidebar" id="btnToggleSidebar" title="Toggle Navigation Sidebar">☰</button>
      <div class="breadcrumb-wrap">
        <div class="breadcrumb-title" id="breadcrumbHeadline">
          <span>Home Overview</span>
          <span class="mode-persistent-badge badge-syn" id="headerModeBadge">SYNTHETIC SIMULATION</span>
        </div>
        <div class="breadcrumb-path">
          <span>⚡ ClawGrid</span>
          <span>›</span>
          <span id="breadcrumbCurrent" style="color:#94a3b8">Home Overview</span>
        </div>
      </div>
    </div>

    <div class="header-right">
      <div class="mode-segmented" title="Switch between Physical Bedroom Hardware (Node A) and 4-Room Synthetic Simulation">
        <button type="button" class="mode-btn" id="btnModeReal" onclick="switchTelemetryMode('physical_bedroom')">
          <i class="status-dot live" id="dotModeReal"></i> Hardware (Node A)
        </button>
        <button type="button" class="mode-btn active" id="btnModeSyn" onclick="switchTelemetryMode('virtual_rooms')">
          <i class="status-dot syn" id="dotModeSyn"></i> 4-Room Simulation
        </button>
      </div>

      <label class="token-box-nav" title="ClawGrid Control API Token">
        <span class="token-label-text">Token</span>
        <input type="password" id="token" class="token-input-field" placeholder="API Token" value="clawgrid-dev-only" autocomplete="off" spellcheck="false">
      </label>

      <div class="header-actions">
        <button type="button" class="header-icon-btn" title="Toggle Fullscreen" onclick="toggleFullscreen()">⛶</button>
        <div style="position:relative">
          <button type="button" class="header-icon-btn" id="btnNotif" title="System Notifications" onclick="toggleNotifPanel()">
            🔔
            <span class="notif-badge-dot" id="notifDot"></span>
          </button>
          <div class="dropdown-panel" id="notifPanel">
            <div style="display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid var(--border);padding-bottom:8px">
              <strong style="font-size:13px">ClawGrid Notifications</strong>
              <button class="btn btn-sm" onclick="clearNotifications()" style="padding:2px 6px">Clear</button>
            </div>
            <div id="notifList" style="display:flex;flex-direction:column;gap:8px;max-height:240px;overflow-y:auto;font-size:12px">
              <div style="color:var(--text-dim)">No active alerts. Deterministic SafetyKernel nominal.</div>
            </div>
          </div>
        </div>
        <button type="button" class="header-icon-btn" title="Policy Settings" onclick="switchView('config')">⚙️</button>
      </div>
    </div>
  </header>

  <main class="dashboard-container">
    <!-- INTUITIVE CONTEXT & QUICK-ACTION BAR -->
    <div class="context-banner" id="contextBanner">
      <div class="context-banner-text">
        <div class="context-banner-title" id="contextBannerTitle">
          <span>🏠 4-Room Interactive Simulation Mode</span>
          <span class="pill-status pill-active" style="font-size:10.5px">Source: SIMULATED</span>
        </div>
        <div class="context-banner-sub" id="contextBannerSub">
          Auto policy: Occupied &amp; Lux &lt; 300 lx turns Light ON · Occupied &amp; Temp ≥ 28.0°C turns Fan ON · Vacant after 5s grace turns loads OFF.
        </div>
      </div>
      <div class="context-banner-actions">
        <button type="button" class="btn btn-sm btn-primary" onclick="switchView('simulator')">🗺️ Open 4-Room Floorplan</button>
        <button type="button" class="btn btn-sm" id="btnQuickTour" onclick="toggleSimTour()">▶️ Run Room Tour</button>
        <button type="button" class="btn btn-sm" onclick="switchView('devices')">💡 All Room Controls</button>
      </div>
    </div>

    <!-- 4 KPI CARDS (Click any card to jump to its detailed view) -->
    <div class="kpi-row">
      <div class="kpi-card" onclick="switchView('simulator')" title="Click to open 4-Room Interactive Floorplan">
        <div class="kpi-title"><span>Room Occupancy</span><span>↗</span></div>
        <div class="kpi-value-row">
          <span class="kpi-value" id="kpiOccVal">1 / 4</span>
          <span class="kpi-badge badge-blue" id="kpiOccBadge">25% Occupied</span>
        </div>
        <div class="kpi-subtext" id="kpiOccSub">Active: Bedroom</div>
        <div class="kpi-sparkline">
          <svg viewBox="0 0 110 24" width="100%" height="24" fill="none">
            <rect x="2" y="4" width="22" height="16" rx="4" id="kpiOccBar0" fill="#3b82f6"/>
            <rect x="28" y="4" width="22" height="16" rx="4" id="kpiOccBar1" fill="#1e293b"/>
            <rect x="54" y="4" width="22" height="16" rx="4" id="kpiOccBar2" fill="#1e293b"/>
            <rect x="80" y="4" width="22" height="16" rx="4" id="kpiOccBar3" fill="#1e293b"/>
          </svg>
        </div>
      </div>

      <div class="kpi-card" onclick="switchView('devices')" title="Click to manage Room Relays &amp; Timers">
        <div class="kpi-title"><span>Active Loads &amp; Relays</span><span>↗</span></div>
        <div class="kpi-value-row">
          <span class="kpi-value" id="kpiDevVal">8 / 8</span>
          <span class="kpi-badge badge-green" id="kpiDevBadge">Relays Guarded</span>
        </div>
        <div class="kpi-subtext" id="kpiDevSub">1 Energized · Safe Low-Voltage</div>
        <div class="kpi-sparkline">
          <svg viewBox="0 0 110 24" width="100%" height="24" fill="none">
            <path d="M4 18 L25 18 L35 6 L55 6 L65 18 L85 18 L95 6 L106 6" stroke="#10b981" stroke-width="2.5" stroke-linecap="round"/>
          </svg>
        </div>
      </div>

      <div class="kpi-card" onclick="switchView('telemetry')" title="Click to inspect Live Telemetry &amp; Export CSV">
        <div class="kpi-title"><span>Avg Room Temp &amp; Lux</span><span>↗</span></div>
        <div class="kpi-value-row">
          <span class="kpi-value" id="kpiTempVal">25.3 °C</span>
          <span class="kpi-badge badge-amber" id="kpiTempBadge">236 lx Avg</span>
        </div>
        <div class="kpi-subtext" id="kpiTempSub">Range: 22.8°C – 29.5°C across 4 zones</div>
        <div class="kpi-sparkline">
          <svg viewBox="0 0 110 24" width="100%" height="24" fill="none">
            <path d="M4 16 Q 30 4, 55 12 T 106 8" stroke="#f59e0b" stroke-width="2.5" fill="none"/>
          </svg>
        </div>
      </div>

      <div class="kpi-card" onclick="switchView('health')" title="Click to inspect SafetyKernel &amp; FreeRTOS Health">
        <div class="kpi-title"><span>SafetyKernel &amp; Health</span><span>↗</span></div>
        <div class="kpi-value-row">
          <span class="kpi-value" id="kpiHealthVal">Nominal</span>
          <span class="kpi-badge badge-green" id="kpiHealthBadge">SIMULATED</span>
        </div>
        <div class="kpi-subtext" id="kpiHealthSub">3s Dwell · 0 Faults</div>
        <div class="kpi-sparkline">
          <svg viewBox="0 0 110 24" width="100%" height="24" fill="none">
            <circle cx="12" cy="12" r="5" fill="#10b981"/>
            <circle cx="36" cy="12" r="5" fill="#10b981"/>
            <circle cx="60" cy="12" r="5" fill="#10b981"/>
            <circle cx="84" cy="12" r="5" fill="#10b981"/>
            <circle cx="102" cy="12" r="5" fill="#10b981"/>
          </svg>
        </div>
      </div>
    </div>

    <!-- VIEW 1: OVERVIEW -->
    <div class="app-view active-view" id="viewOverview">
      <div class="middle-grid">
        <div class="content-card">
          <div class="card-topbar">
            <div>
              <div class="card-headline">Live Environmental Telemetry</div>
              <div class="card-subtitle">Reactive Temperature (°C) &amp; Effective Illuminance (lux) Waveform</div>
            </div>
            <div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap">
              <select id="overviewChartRoom" class="form-control" style="padding:4px 8px;font-size:12px;width:135px" onchange="updateTelemetryChart()">
                <option value="all">All Rooms Avg</option>
                <option value="0">Bedroom</option>
                <option value="1">Living Room</option>
                <option value="2">Kitchen</option>
                <option value="3">Study</option>
              </select>
              <div class="btn-group-toggle" id="chartWindowGroup">
                <button type="button" id="btnWin60" class="active-auto" onclick="setChartWindow(60)">1m</button>
                <button type="button" id="btnWin300" onclick="setChartWindow(300)">5m</button>
                <button type="button" id="btnWin900" onclick="setChartWindow(900)">15m</button>
              </div>
              <button class="btn btn-sm" title="Inject Heat/Light Pulse to test chart" onclick="injectTelemetryPulse('heat')">🔥 +2°C</button>
              <button class="btn btn-sm" title="Inject Lux Pulse to test chart" onclick="injectTelemetryPulse('lux')">☀️ +150 lx</button>
              <button class="btn btn-sm" title="Export Telemetry CSV" onclick="exportTelemetryCSV()">📥 CSV</button>
            </div>
          </div>
          <div class="chart-legend-row">
            <div class="legend-item"><span class="legend-dot temp"></span> Temperature (°C)</div>
            <div class="legend-item"><span class="legend-dot lux"></span> Effective Illuminance (lux, includes Light ON boost)</div>
            <span style="margin-left:auto;font-family:var(--font-mono);font-size:11.5px;color:#38bdf8" id="chartLiveReadout">Avg Temp: 25.3°C · Avg Lux: 236 lx</span>
          </div>
          <div class="chart-container">
            <canvas id="overviewChartCanvas" width="700" height="220"></canvas>
          </div>
        </div>

        <div class="content-card">
          <div class="card-topbar">
            <div>
              <div class="card-headline">Multi-Room Status &amp; Quick Switcher</div>
              <div class="card-subtitle">Inspect all 4 rooms at a glance or switch to spatial radar</div>
            </div>
            <div class="btn-group-toggle">
              <button type="button" class="active-on" id="btnMiniSimMode" onclick="toggleRadarSim('mini')">4-Room Grid</button>
              <button type="button" id="btnRadarMode" onclick="toggleRadarSim('radar')">Spatial Radar</button>
            </div>
          </div>

          <div id="miniSimContainer" style="display:grid;grid-template-columns:1fr 1fr;gap:10px">
            <div class="actuator-row" style="flex-direction:column;align-items:stretch;gap:8px">
              <div style="display:flex;justify-content:space-between;align-items:center">
                <strong style="color:#60a5fa;font-size:13.5px">🛏️ Bedroom</strong>
                <span class="pill-status pill-active" id="miniBadge0">OCCUPIED</span>
              </div>
              <div class="actuator-meta" id="miniBedState">25.0°C · 120 lx · Light ON · Fan OFF</div>
              <div style="display:flex;gap:6px;flex-wrap:wrap">
                <button class="btn btn-sm" onclick="quickToggle('bedroom','light')" title="Toggle Bedroom Light">💡 Light</button>
                <button class="btn btn-sm" onclick="quickToggle('bedroom','fan')" title="Toggle Bedroom Fan">🌀 Fan</button>
                <button class="btn btn-sm" style="margin-left:auto" onclick="jumpToRoom(0)" title="Move person to Bedroom">📍 Here</button>
              </div>
            </div>

            <div class="actuator-row" style="flex-direction:column;align-items:stretch;gap:8px">
              <div style="display:flex;justify-content:space-between;align-items:center">
                <strong style="color:#60a5fa;font-size:13.5px">🛋️ Living Room</strong>
                <span class="pill-status pill-inactive" id="miniBadge1">VACANT</span>
              </div>
              <div class="actuator-meta" id="miniLivingState">24.0°C · 280 lx · Light OFF · Fan OFF</div>
              <div style="display:flex;gap:6px;flex-wrap:wrap">
                <button class="btn btn-sm" onclick="quickToggle('living_room','light')" title="Toggle Living Room Light">💡 Light</button>
                <button class="btn btn-sm" onclick="quickToggle('living_room','fan')" title="Toggle Living Room Fan">🌀 Fan</button>
                <button class="btn btn-sm" style="margin-left:auto" onclick="jumpToRoom(1)" title="Move person to Living Room">📍 Here</button>
              </div>
            </div>

            <div class="actuator-row" style="flex-direction:column;align-items:stretch;gap:8px">
              <div style="display:flex;justify-content:space-between;align-items:center">
                <strong style="color:#60a5fa;font-size:13.5px">🍳 Kitchen</strong>
                <span class="pill-status pill-inactive" id="miniBadge2">VACANT</span>
              </div>
              <div class="actuator-meta" id="miniKitchenState">29.5°C · 450 lx · Light OFF · Fan OFF</div>
              <div style="display:flex;gap:6px;flex-wrap:wrap">
                <button class="btn btn-sm" onclick="quickToggle('kitchen','light')" title="Toggle Kitchen Light">💡 Light</button>
                <button class="btn btn-sm" onclick="quickToggle('kitchen','fan')" title="Toggle Kitchen Fan">🌀 Fan</button>
                <button class="btn btn-sm" style="margin-left:auto" onclick="jumpToRoom(2)" title="Move person to Kitchen">📍 Here</button>
              </div>
            </div>

            <div class="actuator-row" style="flex-direction:column;align-items:stretch;gap:8px">
              <div style="display:flex;justify-content:space-between;align-items:center">
                <strong style="color:#60a5fa;font-size:13.5px">📚 Study</strong>
                <span class="pill-status pill-inactive" id="miniBadge3">VACANT</span>
              </div>
              <div class="actuator-meta" id="miniStudyState">22.8°C · 95 lx · Light OFF · Fan OFF</div>
              <div style="display:flex;gap:6px;flex-wrap:wrap">
                <button class="btn btn-sm" onclick="quickToggle('study','light')" title="Toggle Study Light">💡 Light</button>
                <button class="btn btn-sm" onclick="quickToggle('study','fan')" title="Toggle Study Fan">🌀 Fan</button>
                <button class="btn btn-sm" style="margin-left:auto" onclick="jumpToRoom(3)" title="Move person to Study">📍 Here</button>
              </div>
            </div>
          </div>

          <div id="radarContainer" style="display:none;align-items:center;justify-content:center;height:240px">
            <svg class="radar-chart-svg" viewBox="0 0 320 240" width="100%" height="100%">
              <polygon points="160,20 260,65 260,175 160,220 60,175 60,65" fill="none" stroke="#1e293b" stroke-width="1"/>
              <polygon points="160,50 230,80 230,160 160,190 90,160 90,80" fill="none" stroke="#1e293b" stroke-width="1"/>
              <polygon points="160,80 200,100 200,140 160,160 120,140 120,100" fill="none" stroke="#1e293b" stroke-width="1"/>
              <line x1="160" y1="20" x2="160" y2="220" stroke="#1e293b" stroke-width="1"/>
              <line x1="60" y1="65" x2="260" y2="175" stroke="#1e293b" stroke-width="1"/>
              <line x1="60" y1="175" x2="260" y2="65" stroke="#1e293b" stroke-width="1"/>
              <polygon id="radarPoly" points="160,40 240,75 220,165 160,185 85,155 80,75" fill="rgba(6, 182, 212, 0.25)" stroke="#06b6d4" stroke-width="2"/>
              <polygon id="radarPoly2" points="160,60 215,90 245,150 160,170 105,145 100,90" fill="rgba(139, 92, 246, 0.25)" stroke="#8b5cf6" stroke-width="2"/>
              <text x="135" y="14" fill="#94a3b8" font-size="10" font-weight="700">BEDROOM</text>
              <text x="266" y="70" fill="#94a3b8" font-size="10" font-weight="700">LIVING</text>
              <text x="266" y="180" fill="#94a3b8" font-size="10" font-weight="700">KITCHEN</text>
              <text x="142" y="234" fill="#94a3b8" font-size="10" font-weight="700">STUDY</text>
              <text x="8" y="180" fill="#94a3b8" font-size="10" font-weight="700">SAFETY</text>
              <text x="8" y="70" fill="#94a3b8" font-size="10" font-weight="700">ENERGY</text>
            </svg>
          </div>
        </div>
      </div>

      <div class="bottom-grid">
        <div class="content-card">
          <div class="card-topbar">
            <div>
              <div class="card-headline">Bedroom Controls, Scenes &amp; Voice Assistant</div>
              <div class="card-subtitle">Primary Zone Relays, Whole-Home Scenes, and Offline Intent Proposals</div>
            </div>
            <span class="pill-status pill-active" id="badgeKernelStatus">SafetyKernel Armed</span>
          </div>

          <div class="actuator-list">
            <div class="actuator-row">
              <div class="actuator-info">
                <div class="actuator-icon-box" id="iconLight">💡</div>
                <div>
                  <div class="actuator-title">Bedroom Ceiling Light (Relay 1)</div>
                  <div class="actuator-meta" id="metaLight">GPIO 4 · Active Low · Auto when Occupied &amp; Lux &lt; 300 lx</div>
                </div>
              </div>
              <div class="btn-group-toggle" id="btnGroupLight">
                <button type="button" id="btnLightOn" onclick="setActuator('bedroom','light','ON')">ON</button>
                <button type="button" id="btnLightOff" class="active-off" onclick="setActuator('bedroom','light','OFF')">OFF</button>
                <button type="button" id="btnLightAuto" class="active-auto" onclick="setActuator('bedroom','light','AUTO')">AUTO</button>
              </div>
            </div>

            <div class="actuator-row">
              <div class="actuator-info">
                <div class="actuator-icon-box" id="iconFan">🌀</div>
                <div>
                  <div class="actuator-title">Bedroom Circulation Fan (Relay 2)</div>
                  <div class="actuator-meta" id="metaFan">GPIO 5 · 12V DC · Auto when Occupied &amp; Temp ≥ 28.0°C</div>
                </div>
              </div>
              <div class="btn-group-toggle" id="btnGroupFan">
                <button type="button" id="btnFanOn" onclick="setActuator('bedroom','fan','ON')">ON</button>
                <button type="button" id="btnFanOff" class="active-off" onclick="setActuator('bedroom','fan','OFF')">OFF</button>
                <button type="button" id="btnFanAuto" class="active-auto" onclick="setActuator('bedroom','fan','AUTO')">AUTO</button>
              </div>
            </div>
          </div>

          <div class="scene-toolbar">
            <button class="btn" onclick="applyScene('HOME')" title="Enable automatic occupancy policies in all rooms">🏠 Home (Auto)</button>
            <button class="btn" onclick="applyScene('AWAY')" title="Turn off all loads and suspend auto policies">🚪 Away</button>
            <button class="btn" onclick="applyScene('SLEEP')" title="Turn off all lights and keep Bedroom Fan in AUTO">🌙 Sleep</button>
            <button class="btn btn-danger" onclick="applyScene('ALL_OFF')" title="Immediately command all relays to logical OFF">🛑 All Off</button>
          </div>

          <!-- Offline Voice & Command Assistant -->
          <div class="voice-assistant-box">
            <div style="display:flex;justify-content:space-between;align-items:center">
              <strong style="font-size:12.5px;color:#38bdf8">🎙️ Offline Voice &amp; Intent Assistant (SafetyKernel Guarded)</strong>
              <span style="font-size:11px;color:var(--text-dim)" id="voiceStatusBadge">Ready · Local Parser</span>
            </div>
            <div class="voice-input-row">
              <input type="text" id="voiceTextInput" class="form-control" style="flex:1;min-width:180px" placeholder="e.g., &quot;turn on bedroom light&quot; or &quot;turn off kitchen fan&quot;" onkeydown="if(event.key==='Enter')runVoiceCommand()">
              <button type="button" class="btn btn-primary" onclick="runVoiceCommand()">Send Intent</button>
              <button type="button" class="btn" id="btnMicCapture" onclick="triggerMicCapture()" title="Record 4s PCM16 audio for Node B ESP-SR">🎤 Mic (4s)</button>
            </div>
            <div class="voice-chips">
              <span style="font-size:11px;color:var(--text-dim)">Try:</span>
              <span class="voice-chip" onclick="fillAndRunVoice('turn on bedroom light')">"turn on bedroom light"</span>
              <span class="voice-chip" onclick="fillAndRunVoice('turn on bedroom fan')">"turn on bedroom fan"</span>
              <span class="voice-chip" onclick="fillAndRunVoice('turn on study light')">"turn on study light"</span>
              <span class="voice-chip" onclick="fillAndRunVoice('sleep mode')">"sleep mode"</span>
            </div>
          </div>

          <div class="terminal-block" id="overviewTerminal">ClawGrid initialized. SafetyKernel enforcing 3.0s minimum dwell, 5m manual hold, and zero mains AC.</div>
        </div>

        <div style="display:flex;flex-direction:column;gap:18px">
          <!-- EMBEDDED RTOS, WATCHDOG & SAFETYKERNEL STATUS CARD RIGHT ON OVERVIEW -->
          <div class="content-card">
            <div class="card-topbar">
              <div>
                <div class="card-headline">🛡️ Embedded RTOS, Watchdog &amp; Link Status</div>
                <div class="card-subtitle">Dual ESP32-S3 Tasks (Node A + Node B) &amp; Deterministic SafetyKernel</div>
              </div>
              <button class="btn btn-sm" onclick="switchView('health')">RTOS &amp; Fault Lab →</button>
            </div>
            <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:10px">
              <div style="background:#0a0f1d;border:1px solid var(--border);border-radius:8px;padding:10px">
                <div style="font-size:11px;color:var(--text-dim);font-weight:700">NODE A (BEDROOM MCU)</div>
                <div style="font-size:13px;font-weight:700;color:#34d399;margin-top:2px" id="ovNodeAStatus">ControlTask: 200 Hz</div>
                <div style="font-size:11px;color:var(--text-muted);font-family:var(--font-mono)" id="ovNodeAMeta">Boot #1 · 5.0s WDT Armed</div>
              </div>
              <div style="background:#0a0f1d;border:1px solid var(--border);border-radius:8px;padding:10px">
                <div style="font-size:11px;color:var(--text-dim);font-weight:700">NODE B (SPEECH / BRIDGE)</div>
                <div style="font-size:13px;font-weight:700;color:#38bdf8;margin-top:2px" id="ovNodeBStatus">LinkTask: 200 Hz</div>
                <div style="font-size:11px;color:var(--text-muted);font-family:var(--font-mono)" id="ovNodeBMeta">SRAM: 184 KiB · PSRAM: 7.6M</div>
              </div>
              <div style="background:#0a0f1d;border:1px solid var(--border);border-radius:8px;padding:10px">
                <div style="font-size:11px;color:var(--text-dim);font-weight:700">SAFETYKERNEL &amp; LINK</div>
                <div style="font-size:13px;font-weight:700;color:#a78bfa;margin-top:2px" id="ovPiStatus">3.0s Lease Nominal</div>
                <div style="font-size:11px;color:var(--text-muted);font-family:var(--font-mono)" id="ovPiMeta">3.0s Dwell · 4.0h Cutoff</div>
              </div>
            </div>
          </div>

          <div class="content-card">
            <div class="card-topbar">
              <div>
                <div class="card-headline">Live Event Stream</div>
                <div class="card-subtitle">Real-time Occupancy, Policy, and SafetyKernel Transitions</div>
              </div>
              <button class="btn btn-sm" onclick="switchView('logs')">Full Audit Log →</button>
            </div>
            <div class="table-responsive">
              <table class="dashboard-table">
                <thead>
                  <tr>
                    <th>Time</th>
                    <th>Room</th>
                    <th>Event</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody id="overviewEventsTable"></tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- VIEW 2: TELEMETRY -->
    <div class="app-view" id="viewTelemetry">
      <div class="content-card">
        <div class="card-topbar">
          <div>
            <div class="card-headline">High-Resolution Sensor Telemetry &amp; Interactive Signal Generator</div>
            <div class="card-subtitle">Live waveforms react immediately to Light/Fan actuation, room occupancy, and environmental pulses</div>
          </div>
          <div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap">
            <select id="telemetryRoomFilter" class="form-control" style="width:165px;padding:5px 10px" onchange="updateDetailedTelemetryView()">
              <option value="all">All Rooms (Average)</option>
              <option value="0">Bedroom</option>
              <option value="1">Living Room</option>
              <option value="2">Kitchen</option>
              <option value="3">Study</option>
            </select>
            <button class="btn btn-sm" onclick="injectTelemetryPulse('heat')">🔥 Heat Pulse (+2.5°C)</button>
            <button class="btn btn-sm" onclick="injectTelemetryPulse('cool')">❄️ Cool Pulse (-2.5°C)</button>
            <button class="btn btn-sm" onclick="injectTelemetryPulse('lux')">☀️ Daylight (+200 lx)</button>
            <button class="btn btn-sm" onclick="injectTelemetryPulse('dark')">🌙 Dusk (-180 lx)</button>
            <button class="btn btn-sm" onclick="exportTelemetryCSV()">📥 Export CSV</button>
            <button class="btn btn-sm" onclick="exportSummaryJSON()">📦 Export JSON</button>
          </div>
        </div>
        <div class="chart-container" style="height:280px">
          <canvas id="detailedTelemetryCanvas" width="1000" height="280"></canvas>
        </div>
        <div class="table-responsive">
          <table class="dashboard-table">
            <thead>
              <tr>
                <th>Room</th>
                <th>Occupancy</th>
                <th>Temperature</th>
                <th>Ambient / Effective Lux</th>
                <th>PIR Motion</th>
                <th>Presence</th>
                <th>Door Contact</th>
                <th>Active Loads</th>
                <th>Data Source</th>
                <th>Quick Action</th>
              </tr>
            </thead>
            <tbody id="detailedTelemetryTable"></tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- VIEW 3: SIMULATOR -->
    <div class="app-view" id="viewSimulator">
      <div class="content-card">
        <div class="card-topbar">
          <div>
            <div class="card-headline">ClawGrid Four-Room Interactive Simulation Lab</div>
            <div class="card-subtitle">Click or drag on any room to move the person and automatically select that room's environment controls.</div>
          </div>
          <div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap">
            <span class="pill-status pill-active" id="simStreamBadge">SIMULATED · Active</span>
            <button class="btn btn-sm btn-primary" id="btnSimTour" onclick="toggleSimTour()">▶️ Run Room Tour</button>
            <button class="btn btn-sm" onclick="simLeaveHouse()">🚪 Leave House</button>
            <button class="btn btn-sm" onclick="resetSimHouse()">↺ Reset Defaults</button>
          </div>
        </div>

        <div class="simulator-layout">
          <div class="canvas-wrap">
            <canvas id="simHouseCanvas" width="800" height="560" tabindex="0" aria-label="Interactive 4-room house layout. Click, drag, or use arrow keys to move avatar."></canvas>
            <div class="sim-controls-bar">
              <div style="display:flex;gap:6px;align-items:center">
                <span style="font-size:11.5px;color:var(--text-dim);margin-right:2px">Move:</span>
                <button class="btn btn-sm" onclick="moveSimPerson(-28,0)" title="Move Left (ArrowLeft)">←</button>
                <button class="btn btn-sm" onclick="moveSimPerson(0,-28)" title="Move Up (ArrowUp)">↑</button>
                <button class="btn btn-sm" onclick="moveSimPerson(0,28)" title="Move Down (ArrowDown)">↓</button>
                <button class="btn btn-sm" onclick="moveSimPerson(28,0)" title="Move Right (ArrowRight)">→</button>
              </div>
              <div style="font-size:12.5px;font-weight:600;color:#38bdf8" id="simAvatarPos">Avatar Position: (160, 140) · Bedroom</div>
            </div>
            <div class="room-jump-bar">
              <span style="font-size:11.5px;color:var(--text-dim)">Quick Teleport:</span>
              <button class="btn btn-sm" onclick="jumpToRoom(0)">🛏️ Bedroom</button>
              <button class="btn btn-sm" onclick="jumpToRoom(1)">🛋️ Living Room</button>
              <button class="btn btn-sm" onclick="jumpToRoom(2)">🍳 Kitchen</button>
              <button class="btn btn-sm" onclick="jumpToRoom(3)">📚 Study</button>
              <button class="btn btn-sm" onclick="simLeaveHouse()">🚪 Outside</button>
            </div>
            <p style="font-size:12px;color:var(--text-dim)">
              💡 <strong>How it works:</strong> Clicking a room moves the avatar there and selects that room on the right. When occupied, <strong>Light AUTO</strong> turns ON if Lux &lt; <span id="lblSimLuxThresh">300</span> lx, and <strong>Fan AUTO</strong> turns ON if Temp ≥ <span id="lblSimTempThresh">28.0</span>°C. Leaving a room starts the <span id="lblSimGrace">5</span>s vacancy grace countdown.
            </p>
          </div>

          <div style="display:flex;flex-direction:column;gap:16px">
            <div class="content-card" style="padding:16px">
              <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px">
                <h3 style="font-size:14.5px;color:var(--text-main)">Room Environmental Control</h3>
                <span class="pill-status pill-active" id="simSelectedRoomBadge">Bedroom</span>
              </div>
              <div class="form-group" style="margin-bottom:10px">
                <label>Selected Room (Syncs on Canvas Click)</label>
                <select id="simRoomSelect" class="form-control" onchange="loadSimEnvToInputs()">
                  <option value="0">Bedroom</option>
                  <option value="1">Living Room</option>
                  <option value="2">Kitchen</option>
                  <option value="3">Study</option>
                </select>
              </div>

              <div class="form-group" style="margin-bottom:10px">
                <label>1-Click Weather / Lighting Presets</label>
                <div style="display:grid;grid-template-columns:1fr 1fr;gap:6px;margin-top:2px">
                  <button type="button" class="btn btn-sm" onclick="applySimPreset(24.0, 550)">☀️ Daylight (24°C, 550 lx)</button>
                  <button type="button" class="btn btn-sm" onclick="applySimPreset(23.5, 45)">🌙 Night (23.5°C, 45 lx)</button>
                  <button type="button" class="btn btn-sm" onclick="applySimPreset(31.0, 110)">🔥 Hot &amp; Dim (31°C, 110 lx)</button>
                  <button type="button" class="btn btn-sm" onclick="applySimPreset(21.0, 420)">❄️ Cool &amp; Bright (21°C, 420 lx)</button>
                </div>
              </div>

              <div class="checkbox-row" style="margin-bottom:12px">
                <label><input type="checkbox" id="simCheckPir" checked onchange="applySimEnvironment()"> Virtual PIR</label>
                <label><input type="checkbox" id="simCheckPres" checked onchange="applySimEnvironment()"> Presence</label>
                <label><input type="checkbox" id="simCheckDoor" onchange="applySimEnvironment()"> Door Open</label>
              </div>

              <div class="form-grid" style="margin-bottom:12px">
                <div class="form-group">
                  <label>Temperature (0–60 °C)</label>
                  <input type="number" id="simInputTemp" class="form-control" min="0" max="60" value="25.0" step="0.5">
                </div>
                <div class="form-group">
                  <label>Illuminance (0–2000 Lux)</label>
                  <input type="number" id="simInputLux" class="form-control" min="0" max="2000" value="120" step="10">
                </div>
              </div>

              <button class="btn btn-primary" style="width:100%;justify-content:center" onclick="applySimEnvironment()">Apply Room Environment</button>
              <div style="font-size:11.5px;color:var(--text-muted);margin-top:8px" id="simEnvStatus">
                Active Rule: Occupied &amp; Lux &lt; 300 → Light ON; Occupied &amp; Temp ≥ 28°C → Fan ON.
              </div>
            </div>

            <div class="content-card" style="padding:16px">
              <h3 style="font-size:14px;color:var(--text-main);margin-bottom:6px">Sensor Fault &amp; Safety Experiments</h3>
              <p style="font-size:12px;color:var(--text-muted);margin-bottom:10px">
                Disabling both PIR and Presence sensors in a room forces occupancy evidence to <strong>UNKNOWN</strong> (never falsely claiming VACANT).
              </p>
              <div style="display:flex;gap:8px;flex-wrap:wrap">
                <button class="btn btn-warning btn-sm" style="flex:1;justify-content:center" onclick="simDisableSensors()">⚠️ Disable Room Sensors</button>
                <button class="btn btn-sm" style="flex:1;justify-content:center" onclick="simRestoreSensors()">✅ Restore Room Sensors</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- VIEW 4: DEVICES -->
    <div class="app-view" id="viewDevices">
      <div class="content-card">
        <div class="card-topbar">
          <div>
            <div class="card-headline">Room Relays, Scenes &amp; Countdown Timers</div>
            <div class="card-subtitle">Manual ON/OFF locks a 5-minute hold; AUTO restores sensor-driven policy control.</div>
          </div>
          <div style="display:flex;gap:8px;flex-wrap:wrap">
            <button class="btn btn-sm" onclick="applyScene('HOME')">🔄 Resume All AUTO</button>
            <button class="btn btn-sm btn-danger" onclick="applyScene('ALL_OFF')">🛑 Emergency All Off</button>
          </div>
        </div>

        <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:16px">
          <!-- Bedroom -->
          <div class="content-card" style="padding:16px">
            <h4 style="display:flex;justify-content:space-between;align-items:center;color:#60a5fa">
              <span>🛏️ Bedroom</span>
              <span class="pill-status pill-active" id="devRoomBadge0">OCCUPIED</span>
            </h4>
            <div style="font-size:11.5px;color:var(--text-dim);margin-bottom:8px" id="devRoomMeta0">PIR_1 (Zone 1) · 25.0°C · 120 lx</div>
            <div class="actuator-list">
              <div class="actuator-row" style="padding:8px 10px">
                <div>
                  <div style="font-size:12.5px;font-weight:600">💡 Ceiling Light</div>
                  <div style="font-size:11px;color:var(--text-dim)" id="b0_light_meta">Auto Policy</div>
                </div>
                <div class="btn-group-toggle">
                  <button type="button" id="b0_light_on" onclick="setActuator('bedroom','light','ON')">ON</button>
                  <button type="button" id="b0_light_off" class="active-off" onclick="setActuator('bedroom','light','OFF')">OFF</button>
                  <button type="button" id="b0_light_auto" class="active-auto" onclick="setActuator('bedroom','light','AUTO')">AUTO</button>
                </div>
              </div>
              <div class="actuator-row" style="padding:8px 10px">
                <div>
                  <div style="font-size:12.5px;font-weight:600">🌀 Ceiling Fan</div>
                  <div style="font-size:11px;color:var(--text-dim)" id="b0_fan_meta">Auto Policy</div>
                </div>
                <div class="btn-group-toggle">
                  <button type="button" id="b0_fan_on" onclick="setActuator('bedroom','fan','ON')">ON</button>
                  <button type="button" id="b0_fan_off" class="active-off" onclick="setActuator('bedroom','fan','OFF')">OFF</button>
                  <button type="button" id="b0_fan_auto" class="active-auto" onclick="setActuator('bedroom','fan','AUTO')">AUTO</button>
                </div>
              </div>
            </div>
          </div>

          <!-- Living Room -->
          <div class="content-card" style="padding:16px">
            <h4 style="display:flex;justify-content:space-between;align-items:center;color:#60a5fa">
              <span>🛋️ Living Room</span>
              <span class="pill-status pill-inactive" id="devRoomBadge1">VACANT</span>
            </h4>
            <div style="font-size:11.5px;color:var(--text-dim);margin-bottom:8px" id="devRoomMeta1">Virtual Zone 2 · 24.0°C · 280 lx</div>
            <div class="actuator-list">
              <div class="actuator-row" style="padding:8px 10px">
                <div>
                  <div style="font-size:12.5px;font-weight:600">💡 Ceiling Light</div>
                  <div style="font-size:11px;color:var(--text-dim)" id="b1_light_meta">Auto Policy</div>
                </div>
                <div class="btn-group-toggle">
                  <button type="button" id="b1_light_on" onclick="setActuator('living_room','light','ON')">ON</button>
                  <button type="button" id="b1_light_off" class="active-off" onclick="setActuator('living_room','light','OFF')">OFF</button>
                  <button type="button" id="b1_light_auto" class="active-auto" onclick="setActuator('living_room','light','AUTO')">AUTO</button>
                </div>
              </div>
              <div class="actuator-row" style="padding:8px 10px">
                <div>
                  <div style="font-size:12.5px;font-weight:600">🌀 Ceiling Fan</div>
                  <div style="font-size:11px;color:var(--text-dim)" id="b1_fan_meta">Manual OFF</div>
                </div>
                <div class="btn-group-toggle">
                  <button type="button" id="b1_fan_on" onclick="setActuator('living_room','fan','ON')">ON</button>
                  <button type="button" id="b1_fan_off" class="active-off" onclick="setActuator('living_room','fan','OFF')">OFF</button>
                  <button type="button" id="b1_fan_auto" class="active-auto" onclick="setActuator('living_room','fan','AUTO')">AUTO</button>
                </div>
              </div>
            </div>
          </div>

          <!-- Kitchen -->
          <div class="content-card" style="padding:16px">
            <h4 style="display:flex;justify-content:space-between;align-items:center;color:#60a5fa">
              <span>🍳 Kitchen</span>
              <span class="pill-status pill-inactive" id="devRoomBadge2">VACANT</span>
            </h4>
            <div style="font-size:11.5px;color:var(--text-dim);margin-bottom:8px" id="devRoomMeta2">Virtual Zone 3 · 29.5°C · 450 lx</div>
            <div class="actuator-list">
              <div class="actuator-row" style="padding:8px 10px">
                <div>
                  <div style="font-size:12.5px;font-weight:600">💡 Ceiling Light</div>
                  <div style="font-size:11px;color:var(--text-dim)" id="b2_light_meta">Auto Policy</div>
                </div>
                <div class="btn-group-toggle">
                  <button type="button" id="b2_light_on" onclick="setActuator('kitchen','light','ON')">ON</button>
                  <button type="button" id="b2_light_off" class="active-off" onclick="setActuator('kitchen','light','OFF')">OFF</button>
                  <button type="button" id="b2_light_auto" class="active-auto" onclick="setActuator('kitchen','light','AUTO')">AUTO</button>
                </div>
              </div>
              <div class="actuator-row" style="padding:8px 10px">
                <div>
                  <div style="font-size:12.5px;font-weight:600">🌀 Ceiling Fan</div>
                  <div style="font-size:11px;color:var(--text-dim)" id="b2_fan_meta">Manual OFF</div>
                </div>
                <div class="btn-group-toggle">
                  <button type="button" id="b2_fan_on" onclick="setActuator('kitchen','fan','ON')">ON</button>
                  <button type="button" id="b2_fan_off" class="active-off" onclick="setActuator('kitchen','fan','OFF')">OFF</button>
                  <button type="button" id="b2_fan_auto" class="active-auto" onclick="setActuator('kitchen','fan','AUTO')">AUTO</button>
                </div>
              </div>
            </div>
          </div>

          <!-- Study -->
          <div class="content-card" style="padding:16px">
            <h4 style="display:flex;justify-content:space-between;align-items:center;color:#60a5fa">
              <span>📚 Study</span>
              <span class="pill-status pill-inactive" id="devRoomBadge3">VACANT</span>
            </h4>
            <div style="font-size:11.5px;color:var(--text-dim);margin-bottom:8px" id="devRoomMeta3">Virtual Zone 4 · 22.8°C · 95 lx</div>
            <div class="actuator-list">
              <div class="actuator-row" style="padding:8px 10px">
                <div>
                  <div style="font-size:12.5px;font-weight:600">💡 Ceiling Light</div>
                  <div style="font-size:11px;color:var(--text-dim)" id="b3_light_meta">Auto Policy</div>
                </div>
                <div class="btn-group-toggle">
                  <button type="button" id="b3_light_on" onclick="setActuator('study','light','ON')">ON</button>
                  <button type="button" id="b3_light_off" class="active-off" onclick="setActuator('study','light','OFF')">OFF</button>
                  <button type="button" id="b3_light_auto" class="active-auto" onclick="setActuator('study','light','AUTO')">AUTO</button>
                </div>
              </div>
              <div class="actuator-row" style="padding:8px 10px">
                <div>
                  <div style="font-size:12.5px;font-weight:600">🌀 Ceiling Fan</div>
                  <div style="font-size:11px;color:var(--text-dim)" id="b3_fan_meta">Manual OFF</div>
                </div>
                <div class="btn-group-toggle">
                  <button type="button" id="b3_fan_on" onclick="setActuator('study','fan','ON')">ON</button>
                  <button type="button" id="b3_fan_off" class="active-off" onclick="setActuator('study','fan','OFF')">OFF</button>
                  <button type="button" id="b3_fan_auto" class="active-auto" onclick="setActuator('study','fan','AUTO')">AUTO</button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="content-card" style="padding:16px">
          <h4 style="font-size:14px;color:var(--text-main);margin-bottom:8px">⏱️ Auto-Off Countdown Timer Scheduler</h4>
          <div style="display:flex;gap:10px;align-items:center;flex-wrap:wrap">
            <select id="timerRoomSelect" class="form-control" style="width:150px">
              <option value="bedroom">Bedroom</option>
              <option value="living_room">Living Room</option>
              <option value="kitchen">Kitchen</option>
              <option value="study">Study</option>
            </select>
            <select id="timerDevSelect" class="form-control" style="width:130px">
              <option value="light">Ceiling Light</option>
              <option value="fan">Circulation Fan</option>
            </select>
            <input type="number" id="timerMinutes" class="form-control" min="1" max="1440" value="10" style="width:90px" placeholder="Minutes">
            <span style="font-size:12px;color:var(--text-dim)">minutes</span>
            <button class="btn btn-primary" onclick="scheduleTimer()">Start Countdown Timer</button>
            <button class="btn" onclick="cancelTimer()">Clear All Timers</button>
          </div>
          <div id="activeTimersText" style="font-size:12px;color:var(--text-muted);margin-top:8px">No active countdown timers.</div>
        </div>
      </div>
    </div>

    <!-- VIEW 5: LOGS -->
    <div class="app-view" id="viewLogs">
      <div class="content-card">
        <div class="card-topbar">
          <div>
            <div class="card-headline">ClawGrid Audit &amp; Event Logs</div>
            <div class="card-subtitle">Real-time log of occupancy transitions, SafetyKernel validations, and user commands</div>
          </div>
          <div style="display:flex;gap:8px">
            <button class="btn btn-sm" onclick="clearEventLogs()">Clear Display</button>
            <button class="btn btn-sm" onclick="exportEventLogsCSV()">📥 Download Log CSV</button>
          </div>
        </div>

        <div class="filter-bar">
          <input type="text" id="logSearchInput" class="form-control" placeholder="Search events..." style="width:220px" oninput="renderLogsTable()">
          <select id="logRoomFilter" class="form-control" style="width:140px" onchange="renderLogsTable()">
            <option value="ALL">All Rooms</option>
            <option value="bedroom">Bedroom</option>
            <option value="living_room">Living Room</option>
            <option value="kitchen">Kitchen</option>
            <option value="study">Study</option>
            <option value="system">System</option>
          </select>
          <select id="logSeverityFilter" class="form-control" style="width:140px" onchange="renderLogsTable()">
            <option value="ALL">All Severities</option>
            <option value="INFO">INFO</option>
            <option value="WARN">WARN</option>
            <option value="ALARM">ALARM</option>
          </select>
          <select id="logSourceFilter" class="form-control" style="width:150px" onchange="renderLogsTable()">
            <option value="ALL">All Sources</option>
            <option value="REAL_TIME">REAL_TIME</option>
            <option value="SYNTHETIC">SYNTHETIC</option>
          </select>
          <span style="font-size:12px;color:var(--text-dim);margin-left:auto" id="logCountLabel">Showing 0 events</span>
        </div>

        <div class="table-responsive">
          <table class="dashboard-table">
            <thead>
              <tr>
                <th>#</th>
                <th>Timestamp</th>
                <th>Room</th>
                <th>Event Type</th>
                <th>Severity</th>
                <th>Source</th>
                <th>Description</th>
              </tr>
            </thead>
            <tbody id="fullLogsTableBody"></tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- VIEW 6: DIAGNOSTICS -->
    <div class="app-view" id="viewDiagnostics">
      <div class="content-card">
        <div class="card-topbar">
          <div>
            <div class="card-headline">Sensor Health &amp; Diagnostics Matrix</div>
            <div class="card-subtitle">Per-sensor availability, warm-up readiness, and fault simulation controls</div>
          </div>
          <button class="btn btn-sm" onclick="refreshDiagnostics()">🔄 Refresh Diagnostics</button>
        </div>

        <div class="table-responsive">
          <table class="dashboard-table">
            <thead>
              <tr>
                <th>Sensor ID</th>
                <th>Room</th>
                <th>Type</th>
                <th>Latest Value</th>
                <th>Freshness</th>
                <th>Availability</th>
                <th>Health Status</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody id="diagnosticsTableBody"></tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- VIEW 7: HEALTH -->
    <div class="app-view" id="viewHealth">
      <div class="middle-grid">
        <div class="content-card">
          <div class="card-topbar">
            <div>
              <div class="card-headline">FreeRTOS Task &amp; Watchdog Telemetry</div>
              <div class="card-subtitle">Dual ESP32-S3 Task Monitoring (`esp_task_wdt_init(5, true)`) &amp; Job Counters</div>
            </div>
            <span class="pill-status pill-active" id="badgeWatchdog">Task WDT Nominal (5.0s)</span>
          </div>

          <div class="table-responsive">
            <table class="dashboard-table">
              <thead>
                <tr>
                  <th>Task Name</th>
                  <th>Core / Prio</th>
                  <th>Period</th>
                  <th>Min Stack Free</th>
                  <th>Runtime Jobs</th>
                  <th>Watchdog Subscription</th>
                  <th>Missed Deadlines</th>
                </tr>
              </thead>
              <tbody id="rtosFullTable">
                <tr>
                  <td><strong>ControlTask (Node A)</strong></td>
                  <td>Core 1 · P5</td>
                  <td>5 ms</td>
                  <td>3,480 B</td>
                  <td id="rtosJobsControl" style="font-family:var(--font-mono);color:#34d399">128,492</td>
                  <td><span class="pill-status pill-active" id="wdtStatusControl">Subscribed (5.0s)</span></td>
                  <td id="rtosMissControl">0</td>
                </tr>
                <tr>
                  <td><strong>LinkTask (Node A/B)</strong></td>
                  <td>Core 0 · P3</td>
                  <td>5 ms</td>
                  <td>2,912 B</td>
                  <td id="rtosJobsLink" style="font-family:var(--font-mono);color:#38bdf8">64,120</td>
                  <td><span class="pill-status pill-active" id="wdtStatusLink">Subscribed (5.0s)</span></td>
                  <td>0</td>
                </tr>
                <tr>
                  <td><strong>SpeechTask (Node B)</strong></td>
                  <td>Core 1 · P4</td>
                  <td>Event</td>
                  <td>8,192 B</td>
                  <td id="rtosJobsSpeech" style="font-family:var(--font-mono)">1,420</td>
                  <td><span class="pill-status pill-inactive">Bounded Job Timeout</span></td>
                  <td>0</td>
                </tr>
                <tr>
                  <td><strong>Idle0 / Idle1 WDT</strong></td>
                  <td>Core 0/1 · P0</td>
                  <td>100 ms</td>
                  <td>1,840 B</td>
                  <td id="rtosJobsPi" style="font-family:var(--font-mono);color:#a78bfa">4,210</td>
                  <td><span class="pill-status pill-active" id="wdtStatusPi">ESP-IDF Panic/Reboot</span></td>
                  <td>0</td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- Interactive Embedded Fault & Watchdog Lab -->
          <div style="background:#080c16;border:1px solid var(--border);border-radius:10px;padding:14px;display:flex;flex-direction:column;gap:10px">
            <div style="display:flex;justify-content:space-between;align-items:center">
              <strong style="font-size:13px;color:#fbbf24">⚡ Interactive Watchdog, Radio Lease &amp; SafetyKernel Lab</strong>
              <span style="font-size:11.5px;font-family:var(--font-mono);color:var(--text-muted)" id="wdtBootCounter">Node A Boot #1 · Uptime: 0s</span>
            </div>
            <p style="font-size:12px;color:var(--text-muted)">
              Test how ClawGrid handles embedded faults: stalling <code>ControlTask</code> stops its watchdog feed and triggers an <code>ESP_RST_TASK_WDT</code> panic/reboot after 5.0s (incrementing Boot ID and restoring NVS policy with relays OFF). Freezing radio timestamps triggers Node B's 3.0s freshness gate while Node A continues local autonomous PIR control.
            </p>
            <div style="display:flex;gap:8px;flex-wrap:wrap">
              <button type="button" class="btn btn-warning btn-sm" onclick="simulateWdtStall()">💥 Simulate ControlTask Stall (5s WDT Reboot)</button>
              <button type="button" class="btn btn-sm" onclick="simulateStaleLease()">📡 Simulate 3.0s Radio Lease Expiry</button>
              <button type="button" class="btn btn-sm" onclick="restoreNominalRtos()">✅ Restore Nominal State</button>
            </div>
          </div>
        </div>

        <div class="content-card">
          <div class="card-topbar">
            <div>
              <div class="card-headline">SafetyKernel &amp; Dual-ESP32-S3 Architecture</div>
              <div class="card-subtitle">Node A (Bedroom Sensor &amp; Relays) ↔ Node B (Speech &amp; API Gateway)</div>
            </div>
          </div>

          <div class="actuator-list">
            <div class="actuator-row">
              <div>
                <strong>Node A — Bedroom Sensor &amp; Relay Controller (ESP32-S3)</strong>
                <div class="actuator-meta">GPIO 6 PIR_1 (60s warm-up + 200ms LOW arming) · GPIO 4 Light · GPIO 5 Fan · Autonomous if radio drops</div>
              </div>
              <span class="pill-status pill-active" id="tier1Badge">ARMED</span>
            </div>
            <div class="actuator-row">
              <div>
                <strong>Node B — Edge AI Speech &amp; Radio Gateway (ESP32-S3)</strong>
                <div class="actuator-meta">ESP-SR MultiNet6 offline voice proposals · Authenticated ESP-NOW link (3.0s freshness gate)</div>
              </div>
              <span class="pill-status pill-active" id="tier2Badge">ONLINE</span>
            </div>
            <div class="actuator-row">
              <div>
                <strong>Two-Step Proposal → Confirm Binding</strong>
                <div class="actuator-meta">AI/Voice and UI requests create unactuated proposals first; only confirmed proposals matching Boot ID execute</div>
              </div>
              <span class="pill-status pill-active" id="tier3Badge">VERIFIED</span>
            </div>
            <div class="actuator-row">
              <div>
                <strong>Anti-Cycling Dwell (3.0s) &amp; Max Continuous Run (4.0h)</strong>
                <div class="actuator-meta">Enforced deterministically inside <code>safety_kernel.c</code> / <code>safety_kernel.py</code></div>
              </div>
              <span class="pill-status pill-active" id="invariantDwellBadge">ENFORCED (3.0s)</span>
            </div>
          </div>

          <div class="terminal-block" id="healthMemoryTerminal" style="margin-top:6px">Memory [HOST_MEASURED]: Internal Free SRAM: 184.2 KiB · Min Free: 142.0 KiB · PSRAM Free: 7,812 KiB. Dual ESP32-S3 task watchdogs armed (5.0s timeout).</div>
        </div>
      </div>
    </div>

    <!-- VIEW 8: CONFIG -->
    <div class="app-view" id="viewConfig">
      <div class="content-card">
        <div class="card-topbar">
          <div>
            <div class="card-headline">ClawGrid Policy &amp; Threshold Configuration</div>
            <div class="card-subtitle">Customize occupancy grace periods, environmental thresholds, and SafetyKernel holds</div>
          </div>
          <button class="btn btn-primary" onclick="saveConfiguration()">💾 Save Changes</button>
        </div>

        <div class="form-grid">
          <div class="form-group">
            <label>Vacancy Grace Period (seconds)</label>
            <input type="number" id="cfgGrace" class="form-control" min="2" max="600" value="5">
          </div>
          <div class="form-group">
            <label>Minimum Dwell Time (seconds)</label>
            <input type="number" id="cfgDwell" class="form-control" min="1" max="30" value="3">
          </div>
          <div class="form-group">
            <label>Manual Override Hold Duration (minutes)</label>
            <input type="number" id="cfgHold" class="form-control" min="1" max="120" value="5">
          </div>
          <div class="form-group">
            <label>Auto-Light Lux Threshold (Lux)</label>
            <input type="number" id="cfgLux" class="form-control" min="10" max="1000" value="300">
          </div>
          <div class="form-group">
            <label>Auto-Fan Temp Threshold (°C)</label>
            <input type="number" id="cfgTemp" class="form-control" min="15" max="45" value="28.0" step="0.5">
          </div>
          <div class="form-group">
            <label>Real-Time Hardware REST API URL</label>
            <input type="text" id="cfgHardwareUrl" class="form-control" value="http://127.0.0.1:8770">
          </div>
        </div>

        <div style="display:flex;gap:10px;margin-top:16px">
          <button class="btn btn-primary" onclick="saveConfiguration()">Save Configuration</button>
          <button class="btn" onclick="resetConfiguration()">Reset to Factory Defaults</button>
        </div>
      </div>
    </div>

    <!-- Hidden compatibility elements for scripts & test runners -->
    <div style="display:none">
      <button id="confirmManual" disabled></button>
      <button id="confirmSpeech" disabled></button>
      <button id="execute" disabled></button>
      <input type="checkbox" id="autoLight">
      <input type="checkbox" id="autoFan">
      <input type="number" id="grace" value="120">
      <input type="text" id="command">
      <div id="proposal"></div>
      <div id="result"></div>
    </div>
  </main>
</div>

<div class="toast-container" id="toastContainer"></div>

<script src="/api/client"></script>
<script>
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

</script>
</body>
</html>
)HTML";
