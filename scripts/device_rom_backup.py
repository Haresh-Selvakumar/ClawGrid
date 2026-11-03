"""One bounded ROM session: identity/security, full-flash backup, layout inspection.

No flash/NVS/eFuse writes. Each backup is device-size-derived, board-MD5
verified and copied to a separate local directory before any future deployment.
"""
import argparse
import hashlib
import json
import re
import shutil
from pathlib import Path
import esptool
from esptool.cmds import attach_flash,detect_flash_size,read_flash
from device_flash_guard import inspect_layout,selected_app,sha
ROOT=Path(__file__).resolve().parents[1]

def main(args):
    evidence=ROOT/'benchmarks/review/three-board-20261102'
    primary=ROOT/'benchmarks/backups/three-board-20261102'/args.role
    second=ROOT/'output/private-rollback-three-board-20261102'/args.role
    if primary.exists() or second.exists():raise RuntimeError('Existing backup paths preserved; no overwrite/retry')
    evidence.mkdir(parents=True,exist_ok=True)
    primary.mkdir(parents=True);second.mkdir(parents=True)
    cfg=json.loads((ROOT/'local_credentials.json').read_text())
    expected=cfg['node_a_mac' if args.role=='A' else 'node_b_mac'] if args.role!='C' else args.expected_mac
    if not expected:raise RuntimeError('A freshly observed expected MAC is required')
    rom=None;esp=None;result={'role':args.role,'port':args.port,'complete':False,'flash_write':False}
    try:
        rom=esptool.detect_chip(args.port,115200,connect_mode='no-reset' if args.no_reset else 'default-reset',connect_attempts=2)
        mac=':'.join(f'{x:02x}' for x in rom.read_mac())
        if mac.lower()!=expected.lower():raise RuntimeError('Board MAC mismatch; no backup accepted')
        chip=rom.CHIP_NAME.lower().replace('-','')
        if chip not in ('esp32','esp32s3'):raise RuntimeError('Unsupported chip; revise target before proceeding')
        if args.role in ('A','B') and chip!='esp32s3':raise RuntimeError('Existing A/B roles require S3')
        secure=bool(rom.get_secure_boot_enabled());encrypted=bool(rom.get_flash_encryption_enabled())
        description=rom.get_chip_description();features=rom.get_chip_features()
        if secure or encrypted:raise RuntimeError('Secure/encrypted target requires a separately reviewed workflow')
        esp=rom.run_stub();attach_flash(esp)
        size_name=detect_flash_size(esp)
        if not size_name or not re.fullmatch(r'(?:1|2|4|8|16|32|64)MB',size_name):raise RuntimeError('Unknown flash size')
        size=int(size_name[:-2])*1024*1024
        flash_id=esp.flash_id()
        identity=dict(role=args.role,port=args.port,rom_verified=True,chip=chip,description=description,
                      mac=mac,features=features,dual_core=any('Dual Core' in x for x in features),
                      flash_bytes=size,flash_jedec_id=f'{flash_id:06x}',secure_boot=secure,flash_encryption=encrypted)
        print(json.dumps(identity),flush=True)
        esp.change_baud(460800)
        data=read_flash(esp,0,size,no_progress=True)
        if len(data)!=size:raise RuntimeError('Truncated full-flash backup')
        on_device=esp.flash_md5sum(0,size)
        if isinstance(on_device,bytes):on_device=on_device.decode('ascii')
        if on_device.lower()!=hashlib.md5(data).hexdigest():raise RuntimeError('Board flash MD5 mismatch')
        for folder in (primary,second):
            with (folder/'full-flash.bin').open('xb') as f:f.write(data)
        if (second/'full-flash.bin').read_bytes()!=data:raise RuntimeError('Second backup mismatch')
        # Preserve complete data even when layout is unknown/blank. That state
        # is a blocker requiring explicit initial-provisioning review, not erase.
        layout=None;selection=None;layout_error=None
        try:
            layout=inspect_layout(data,allow_verified_legacy=True);selection=selected_app(data,layout)
        except ValueError as error:layout_error=str(error)
        result.update(identity=identity,complete=True,board_md5_verified=True,full_flash_sha256=sha(data),
                      primary=str(primary/'full-flash.bin'),second=str(second/'full-flash.bin'),
                      layout=layout,selected_app=selection,layout_error=layout_error,
                      compatible_for_application_review=layout is not None and selection is not None)
        for folder in (primary,second):
            with (folder/'backup.json').open('x',encoding='utf-8') as f:json.dump(result,f,indent=2)
        print(json.dumps({k:v for k,v in result.items() if k not in ('primary','second')}),flush=True)
    finally:
        connection=esp or rom
        if connection:
            try:connection.hard_reset()
            finally:connection._port.close()
        with (evidence/f'backup-{args.role}.json').open('x',encoding='utf-8') as f:json.dump(result,f,indent=2)

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--role',choices=['A','B','C'],required=True)
    p.add_argument('--port',required=True);p.add_argument('--expected-mac');p.add_argument('--no-reset',action='store_true')
    main(p.parse_args())
