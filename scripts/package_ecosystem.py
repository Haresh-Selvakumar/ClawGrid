"""Create reviewed client deliverables; never include private gateway credentials."""
import hashlib
import json
import shutil
import zipfile
from datetime import datetime, timezone
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]


def main():
    out=ROOT/'deliverables';out.mkdir(exist_ok=True)
    secrets=[json.loads((ROOT/'local_credentials.json').read_text())['api_token'].encode(),
             json.loads((ROOT/'supabase.local.json').read_text())['service_role_key'].encode()]
    def checked(data):
        if any(secret in data for secret in secrets):raise RuntimeError('Private credential found in delivery; packaging stopped')
        return data
    assert 'BUILD SUCCESSFUL' in (ROOT/'benchmarks/raw/android-delivery-build.log').read_text()
    assert 'OK (3 tests)' in (ROOT/'benchmarks/raw/android-wire-jvm.log').read_text()
    apk=ROOT/'ecosystem/android/app/build/outputs/apk/debug/app-debug.apk'
    with zipfile.ZipFile(apk) as z:
        for name in z.namelist():checked(z.read(name))
    shutil.copyfile(apk,out/'ClawGrid-Live-debug.apk')
    android=ROOT/'ecosystem/android';count=0
    with zipfile.ZipFile(out/'ClawGrid-Android-source.zip','w',zipfile.ZIP_DEFLATED) as z:
        for path in sorted(android.rglob('*')):
            if not path.is_file():continue
            rel=path.relative_to(android)
            if any(part in ('.gradle','build','public','.idea','__pycache__') for part in rel.parts):continue
            if path.name in ('.env','local.properties') or path.suffix in ('.zip','.jks','.keystore'):continue
            z.writestr('ClawGrid-Android-Live/'+rel.as_posix(),checked(path.read_bytes()));count+=1
    web=ROOT/'ecosystem/web-baseline/dashboard/static'
    html=(ROOT/'dashboard/ecosystem.html').read_text().replace('src="/api/ecosystem-client"','src="ecosystem-client.js"')
    (web/'index.html').write_text(html,encoding='utf-8')
    (web/'ecosystem-client.js').write_bytes((ROOT/'dashboard/ecosystem-client.js').read_bytes())
    readme='ClawGrid live website\nServe with localhost or HTTPS, then open index.html. Enter your existing ClawGrid control token and Connect live. Keep the PC gateway running on COM7 and A powered. No service key belongs in this client. Missing hardware stays UNKNOWN. Read ECOSYSTEM_INTEGRATION.md for current USB status, data mapping, wiring and verification.\n'
    with zipfile.ZipFile(out/'ClawGrid-live-web.zip','w',zipfile.ZIP_DEFLATED) as z:
        for name in ('index.html','ecosystem-client.js','config.public.json'):z.writestr(name,checked((web/name).read_bytes()))
        z.writestr('README.txt',readme)
        z.writestr('ECOSYSTEM_INTEGRATION.md',checked((ROOT/'docs/ECOSYSTEM_INTEGRATION.md').read_bytes()))
    tests=ET.parse(ROOT/'benchmarks/raw/supabase_unit_20261022_final.xml').getroot()
    suite=tests if tests.tag=='testsuite' else tests.find('testsuite')
    assert suite.get('failures')=='0' and suite.get('errors')=='0'
    manifest=dict(created_at=datetime.now(timezone.utc).isoformat(),source='HOST_MEASURED',
                  topology='A PIR/relays -> ESP-NOW -> USB B COM7 -> PC gateway -> Supabase -> webpage and Android',
                  android_source_files=count,android_build='PASS',android_wire_junit_tests=3,gateway_tests=int(suite.get('tests')),
                  private_credential_scan='PASS',phone_installed=False,physical_load_tested=False,
                  current_hardware='UNAVAILABLE: B COM7 Windows device error 31; known-working data cable/port required',
                  earlier_hardware_campaign='benchmarks/raw/hardware/ecosystem_realtime_20261022T084316Z.json',
                  current_cloud_and_unknown_state_evidence='benchmarks/raw/hardware/ecosystem_history_and_disconnect_20261022.json',files=[])
    for name in ('ClawGrid-Live-debug.apk','ClawGrid-Android-source.zip','ClawGrid-live-web.zip'):
        p=out/name;manifest['files'].append(dict(name=name,bytes=p.stat().st_size,sha256=hashlib.sha256(p.read_bytes()).hexdigest()))
    (out/'delivery-manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
    print('Delivery PASS: signed-build APK, clean Android source and website. No private credentials. Current USB remains unavailable.')


if __name__=='__main__':main()
