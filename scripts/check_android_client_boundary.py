"""Check the built debug APK without printing any configured credential values."""
import hashlib
import json
import re
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / 'ecosystem/android'


def main():
    apk = APP / 'app/build/outputs/apk/debug/app-debug.apk'
    config = (APP / 'app/build/generated/source/buildConfig/debug/com/example/BuildConfig.java').read_text()
    forbidden = ['SUPABASE_SERVICE_ROLE_KEY', 'customServiceKey', 'postHardwareCommand', '/api/command']
    for path in (APP / 'app/src/main/java').rglob('*.kt'):
        text = path.read_text(encoding='utf-8')
        assert not any(value in text for value in forbidden), f'Legacy privileged/control path in {path.name}'
    assert 'SERVICE' not in config and 'SECRET' not in config and 'TOKEN' not in config
    assert 'libs.plugins.secrets' not in (APP / 'app/build.gradle.kts').read_text()
    candidates = set()
    env = APP / '.env'
    if env.exists():
        for line in env.read_text().splitlines():
            key, sep, value = line.partition('=')
            if sep and re.search(r'SERVICE|SECRET|PASSWORD|TOKEN|API_KEY', key, re.I) and not re.search(r'ANON|PUBLISHABLE', key, re.I):
                value = value.strip().strip('"\'')
                if len(value) >= 12 and not value.startswith(('YOUR_', 'MY_')):
                    candidates.add(value.encode())
    credentials = ROOT / 'local_credentials.json'
    if credentials.exists():
        for key, value in json.loads(credentials.read_text()).items():
            if isinstance(value, str) and len(value) >= 12 and re.search(r'key|token|pmk|lmk', key, re.I):
                candidates.add(value.encode())
    with zipfile.ZipFile(apk) as archive:
        for name in archive.namelist():
            content = archive.read(name)
            assert not any(value in content for value in candidates), 'Configured private value found in APK (value withheld)'
            assert b'SUPABASE_SERVICE_ROLE_KEY' not in content, 'Privileged BuildConfig field found in APK'
    print(json.dumps(dict(source='HOST_MEASURED', apk=str(apk.relative_to(ROOT)),
                          bytes=apk.stat().st_size, sha256=hashlib.sha256(apk.read_bytes()).hexdigest(),
                          configured_private_value_matches=0, privileged_buildconfig_fields=0,
                          limit='Exact configured-value scan only; not a general secret audit or hardware validation'), indent=2))


if __name__ == '__main__':
    main()
