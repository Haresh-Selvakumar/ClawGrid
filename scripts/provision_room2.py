"""ROM-identify classic ESP32 C and add only its new private peer material.

No writes to boards, existing A/B provisioning, NVS, models or signing keys.
Check-only by default. Header creation requires --apply and refuses replacement.
"""
import argparse
import hashlib
import json
import re
import secrets
import subprocess
import sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]

def identify(text,existing):
    from flash_native import read_identity
    mac=read_identity(text)
    if mac in {existing['node_a_mac'].lower(),existing['node_b_mac'].lower()}:
        raise ValueError('Refusing to reassign an existing A/B board')
    chip=re.search(r'^Chip type:\s*(.+)$',text,re.M)
    if not chip or not re.match(r'ESP32(?:-|\s|\()',chip[1]) or re.match(r'ESP32-(?:S|C|H|P)',chip[1]):
        raise ValueError('ROM identity is not the classic ESP32 required by the provisional C target')
    return dict(mac=mac,chip=chip[1].strip(),role='C',node_id=3,room_id=2,sensor='DHT11')

def main(args):
    config=json.loads((ROOT/'local_credentials.json').read_text())
    header=ROOT/'firmware/common/config/private_room2_config.h'
    if header.exists(): raise RuntimeError('Existing C provisioning is preserved; inspect it, never regenerate')
    before={p:hashlib.sha256((ROOT/p).read_bytes()).hexdigest() for p in
            ['local_credentials.json','firmware/common/config/private_config.h']}
    if args.backup_receipt:
        from device_flash_guard import verify_backup_pair,inspect_layout,selected_app
        receipt=json.loads(args.backup_receipt.read_text())
        if not receipt.get('complete') or not receipt.get('board_md5_verified') or receipt['identity'].get('role')!='C':
            raise RuntimeError('Complete board-verified C preservation receipt required')
        original=receipt['identity']
        data=verify_backup_pair(receipt['primary'],receipt['second'],receipt['full_flash_sha256'])
        selected_app(data,inspect_layout(data,allow_verified_legacy=True))
        identity=identify('Chip type: '+original['description']+'\nMAC: '+original['mac']+'\n',config)
        if not original.get('rom_verified') or not original.get('dual_core') or original.get('secure_boot') is not False or original.get('flash_encryption') is not False:
            raise RuntimeError('C chip/security prerequisites failed')
    else:
        run=subprocess.run([sys.executable,'-m','esptool','--port',args.port,'--baud','115200','read-mac'],capture_output=True,text=True,timeout=30)
        if run.returncode: raise RuntimeError('ROM identification failed; no provisioning or firmware was changed')
        identity=identify(run.stdout,config)
    if args.apply:
        def array(name,data): return f'static const uint8_t {name}[{len(data)}]={{'+','.join(f'0x{x:02x}' for x in data)+'};\n'
        text='#pragma once\n#include <stdint.h>\n// PRIVATE: new C material only; existing A/B identities and keys unchanged.\nstatic constexpr bool kRoom2Provisioned=true;\n'
        text+=array('kNodeC',bytes.fromhex(identity['mac'].replace(':','')))
        text+=array('kRoom2Lmk',secrets.token_bytes(16))+array('kRoom2HmacKey',secrets.token_bytes(32))
        text+='#define CLAWGRID_C_ID '+json.dumps('ClawGrid:node:C='+identity['mac']+';room=2')+'\n'
        with header.open('x',encoding='utf-8') as f:f.write(text)
        receipt=ROOT/'benchmarks/review/three-board-20261102/C-provisioning.json'
        receipt.parent.mkdir(parents=True,exist_ok=True)
        receipt.write_text(json.dumps({**identity,'source':'HARDWARE_MEASURED','port':args.port,'header_sha256':hashlib.sha256(header.read_bytes()).hexdigest(),'flash_layout_verified':bool(args.backup_receipt),'deployment_allowed':False},indent=2)+'\n')
    assert all(hashlib.sha256((ROOT/p).read_bytes()).hexdigest()==h for p,h in before.items())
    print(json.dumps({**identity,'applied':args.apply,'deployment_allowed':False,'next':'Preserve and verify C partition/flash rollback before any application write'}))

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--port');p.add_argument('--backup-receipt',type=Path);p.add_argument('--apply',action='store_true')
    a=p.parse_args()
    if not a.port and not a.backup_receipt:p.error('--port or --backup-receipt required')
    main(a)
