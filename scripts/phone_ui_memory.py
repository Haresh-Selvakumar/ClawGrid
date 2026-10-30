"""Read UI XML through a pipe only. Never create a UI dump/screenshot file."""
import subprocess
import xml.etree.ElementTree as ET

def read_ui(adb):
    result = subprocess.run([str(adb), 'exec-out', 'uiautomator', 'dump', '/proc/self/fd/1'],
        capture_output=True, timeout=25)
    if result.returncode: raise RuntimeError('In-memory UI inspection unavailable; no dump file fallback')
    raw = result.stdout
    start = raw.find(b'<?xml')
    end = raw.rfind(b'</hierarchy>')
    if start < 0 or end < start: raise RuntimeError('In-memory UI XML unavailable; no disk fallback')
    return ET.fromstring(raw[start:end+len(b'</hierarchy>')])
