"""Run a bounded local test on the existing ESP AP, then restore Windows Wi-Fi."""
import argparse
import json
import re
import subprocess
import sys
import time
import urllib.request
import urllib.error
from pathlib import Path
from xml.sax.saxutils import escape

ROOT=Path(__file__).resolve().parents[1]


def run(args):
    args.output.parent.mkdir(parents=True,exist_ok=True)
    def cmd(parts): return subprocess.run(parts,capture_output=True,text=True,timeout=15)
    before=cmd(['netsh','wlan','show','interfaces'])
    match=re.search(r'^\s*Profile\s*:\s*(.+)$',before.stdout,re.M)
    if not match: raise RuntimeError('No existing connected Wi-Fi profile to restore; stopped')
    previous=match[1].strip(); profile='ClawGrid-Speech-Reference-Temporary'
    if previous==profile or cmd(['netsh','wlan','show','profile','name='+profile]).returncode==0:
        raise RuntimeError('Temporary profile already exists; preserve it and stop')
    config=json.loads((ROOT/'local_credentials.json').read_text())
    private=ROOT/'benchmarks/review/speech-pipeline-20261024/temporary-ap.xml'
    private.parent.mkdir(parents=True,exist_ok=True)
    if private.exists():raise RuntimeError('Temporary private XML already exists; stop')
    xml=f'''<?xml version="1.0"?><WLANProfile xmlns="http://www.microsoft.com/networking/WLAN/profile/v1"><name>{profile}</name><SSIDConfig><SSID><name>{escape(config['ap_ssid'])}</name></SSID></SSIDConfig><connectionType>ESS</connectionType><connectionMode>manual</connectionMode><MSM><security><authEncryption><authentication>WPA2PSK</authentication><encryption>AES</encryption><useOneX>false</useOneX></authEncryption><sharedKey><keyType>passPhrase</keyType><protected>false</protected><keyMaterial>{escape(config['ap_password'])}</keyMaterial></sharedKey></security></MSM></WLANProfile>'''
    report={'source':'HARDWARE_MEASURED','purpose':'bounded actual AP HTTP recognition test; existing credentials reused','child_exit_code':None};created=False
    try:
        private.write_text(xml,encoding='utf-8')
        added=cmd(['netsh','wlan','add','profile','filename='+str(private),'user=current'])
        if added.returncode:raise RuntimeError('Temporary WLAN profile import failed')
        created=True
        connected=cmd(['netsh','wlan','connect','name='+profile,'ssid='+config['ap_ssid'],'interface=Wi-Fi'])
        if connected.returncode:raise RuntimeError('WLAN connect request failed')
        deadline=time.monotonic()+18
        while time.monotonic()<deadline:
            status=cmd(['netsh','wlan','show','interfaces'])
            if re.search(r'^\s*SSID\s*:\s*'+re.escape(config['ap_ssid'])+r'\s*$',status.stdout,re.M):break
            time.sleep(1)
        else:raise RuntimeError('AP association timeout')
        report['AP_associated']=True
        # Association can precede DHCP/route readiness. Confirm actual local
        # HTTP availability before launching any audio test; never change IPs.
        opener=urllib.request.build_opener(urllib.request.ProxyHandler({}))
        ready_deadline=time.monotonic()+25
        report['gateway_http_ready']=False
        while time.monotonic()<ready_deadline:
            try:
                with opener.open('http://192.168.4.1/api/state',timeout=2) as response:
                    state=json.loads(response.read(16384))
                if state.get('physical_outputs_enabled') is True:
                    raise RuntimeError('STOP: unexpected output enablement before campaign')
                if state.get('known') is True and state.get('authenticated') is True and state.get('physical_outputs_enabled') is False and state.get('age_ms',99999)<3000:
                    report['gateway_http_ready']=True;break
            except (urllib.error.URLError,OSError,ValueError) as error:
                report['last_readiness_error']=str(error)
            time.sleep(1)
        if not report['gateway_http_ready']:
            report['wifi_ipv4_diagnostic']=cmd(['powershell','-NoProfile','-Command',"Get-NetIPAddress -InterfaceAlias 'Wi-Fi' -AddressFamily IPv4 | Select-Object IPAddress,PrefixLength,AddressState | ConvertTo-Json -Compress"]).stdout.strip()
            raise RuntimeError('AP associated but fresh inhibited gateway HTTP did not become reachable; no audio test started')
        child=args.command
        if child and child[0]=='--':child=child[1:]
        if not child:raise ValueError('A bounded test command is required after --')
        result=subprocess.run(child,cwd=ROOT,capture_output=True,text=True,encoding='utf-8',errors='replace',timeout=65)
        report['child_exit_code']=result.returncode
        args.output.with_suffix('.child.log').write_text(result.stdout+'\n'+result.stderr,encoding='utf-8')
    finally:
        if created:
            report['restore_request_exit']=cmd(['netsh','wlan','connect','name='+previous,'interface=Wi-Fi']).returncode
            report['temporary_profile_delete_exit']=cmd(['netsh','wlan','delete','profile','name='+profile,'interface=Wi-Fi']).returncode
        if private.exists():private.unlink()
        deadline=time.monotonic()+18
        report['previous_profile_restored']=False
        while time.monotonic()<deadline:
            after=cmd(['netsh','wlan','show','interfaces'])
            if re.search(r'^\s*Profile\s*:\s*'+re.escape(previous)+r'\s*$',after.stdout,re.M):
                report['previous_profile_restored']=True;break
            time.sleep(1)
        report['temporary_private_XML_removed']=not private.exists()
        args.output.parent.mkdir(parents=True,exist_ok=True);args.output.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(report,indent=2))
    return report['child_exit_code'] if report['previous_profile_restored'] else 2


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',type=Path,required=True);p.add_argument('command',nargs=argparse.REMAINDER)
    raise SystemExit(run(p.parse_args()))
