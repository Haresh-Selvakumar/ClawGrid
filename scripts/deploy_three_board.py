"""Guarded sequential application-only rollout using actual device backups.

check prepares a device-bound plan without hardware. deploy accepts only a
checked plan and requires the current gateway receipt before either controller.
Partition/bootloader/NVS/model writes are never part of this procedure.
"""
import argparse
import hashlib
import json
from pathlib import Path
import esptool
from esptool.cmds import attach_flash,detect_flash_size,read_flash,write_flash,verify_flash
from device_flash_guard import plan_application,verify_backup_pair,transfer_application,sha,validate_boot_only_drift
from flash_native import validate_pair_identity
ROOT=Path(__file__).resolve().parents[1]
EVIDENCE=ROOT/'benchmarks/review/three-board-20261102'
ENVS={'A':'native_voice_uart_led_high_bench','B':'native_gateway_three_room','C':'room_c_esp32'}

def source_inventory():
    paths=[]
    for section in ['firmware','native/main']:
        paths.extend(p for p in (ROOT/section).rglob('*') if p.is_file() and p.suffix in ('.h','.cpp','.c'))
    paths.extend(ROOT/p for p in ['native/platformio.ini','native/CMakeLists.txt','native/sdkconfig.defaults',
                  'native/sdkconfig.uart.defaults','native/sdkconfig.room_c.defaults','local_credentials.json'])
    return {p.relative_to(ROOT).as_posix():sha(p.read_bytes()) for p in sorted(set(paths))}

def prepared(role,backup):
    data=verify_backup_pair(backup['primary'],backup['second'],backup['full_flash_sha256'])
    env=ENVS[role];build=ROOT/'native/.pio/build'/env
    image=build/'firmware.bin';table=build/'partitions.bin';sdk=ROOT/'native'/('sdkconfig.'+env)
    cfg=json.loads((ROOT/'local_credentials.json').read_text())
    identity=backup['identity']
    expected=cfg.get('node_a_mac' if role=='A' else 'node_b_mac') if role!='C' else identity['mac']
    if identity['role']!=role or identity['mac'].lower()!=expected.lower():raise ValueError('Role/MAC backup mismatch')
    plan=plan_application(data,identity,image.read_bytes(),table.read_bytes(),sdk.read_text(),role,
                          board_verified_legacy=backup.get('board_md5_verified') is True)
    validate_pair_identity(image.read_bytes(),cfg)
    private=ROOT/'firmware/common/config/private_room2_config.h'
    if not private.is_file():raise ValueError('C provisioning missing; no three-board rollout')
    # C assignment marker is compiled into both B and C, with one private header
    # shared at build time. Preserve its digest without printing its contents.
    cbackup=json.loads((ROOT/'benchmarks/review/three-board-20261102/backup-C.json').read_text())
    marker=('ClawGrid:node:C='+cbackup['identity']['mac']+';room=2').encode()+b'\0'
    if role in ('B','C') and marker not in image.read_bytes():raise ValueError('Candidate lacks the verified C assignment marker')
    plan.update(identity=identity,environment=env,candidate_path=str(image),candidate_table_sha256=sha(table.read_bytes()),
                sdk_sha256=sha(sdk.read_bytes()),primary=backup['primary'],secondary=backup['second'],
                provisioning_sha256=sha(private.read_bytes()),source_hashes=source_inventory(),
                deployment_permitted=True,flash_write=False)
    return plan

class LiveTransport:
    def __init__(self,port,no_reset,expected):
        self.rom=None;self.esp=None;self.expected=expected
        self.prewrite_cache=None
        try:
            self.rom=esptool.detect_chip(port,115200,connect_mode='no-reset' if no_reset else 'default-reset',connect_attempts=2)
            r=self.rom
            identity={**expected,'mac':':'.join(f'{b:02x}' for b in r.read_mac()),
                      'chip':r.CHIP_NAME.lower().replace('-',''),
                      'secure_boot':bool(r.get_secure_boot_enabled()),'flash_encryption':bool(r.get_flash_encryption_enabled()),
                      'description':r.get_chip_description(),'features':r.get_chip_features()}
            identity['dual_core']=any('Dual Core' in x for x in identity['features'])
            self.esp=r.run_stub();attach_flash(self.esp)
            size=detect_flash_size(self.esp)
            identity['flash_bytes']=int(size[:-2])*1024*1024 if size and size.endswith('MB') else 0
            identity['flash_jedec_id']=f'{self.esp.flash_id():06x}'
            self.observed=identity
            self.esp.change_baud(460800)
        except Exception:
            self.close();raise
    def identity(self):return self.observed
    def read(self,start,size):
        if self.prewrite_cache is not None and start==0 and size==len(self.prewrite_cache):
            data=self.prewrite_cache;self.prewrite_cache=None;return data
        return read_flash(self.esp,start,size,no_progress=True)
    def write(self,start,data):
        if start<0x10000:raise ValueError('System-region write forbidden')
        write_flash(self.esp,[(start,data)],flash_mode='keep',flash_freq='keep',flash_size='keep',no_progress=True)
    def verify(self,start,data):verify_flash(self.esp,[(start,data)])
    def verify_full(self,expected):
        digest=self.esp.flash_md5sum(0,len(expected))
        if isinstance(digest,bytes):digest=digest.decode('ascii')
        if digest.lower()!=hashlib.md5(expected).hexdigest():
            raise RuntimeError('Full-device board MD5 mismatch; protected-region preservation not established')
    def current_snapshot(self,original,plan):
        # The previous backup is board-verified. Read current mutable partitions,
        # then independently validate the reconstructed COMPLETE snapshot against
        # a fresh on-device full-flash MD5. Any drift elsewhere fails before write.
        data=bytearray(original)
        for p in plan['layout']['partitions']:
            if p['type']==1 and p['subtype'] in (0,2):
                data[p['offset']:p['offset']+p['size']]=read_flash(self.esp,p['offset'],p['size'],no_progress=True)
        self.verify_full(data)
        return bytes(data)
    def close(self):
        connection=self.esp or self.rom
        if connection:
            try:connection.hard_reset()
            finally:connection._port.close()

def main(args):
    if args.room1_only and args.role=='C':raise ValueError('Room2 is outside final physical acceptance; no C deployment/plan')
    EVIDENCE.mkdir(parents=True,exist_ok=True)
    backup=json.loads((EVIDENCE/f'backup-{args.role}.json').read_text())
    plan=prepared(args.role,backup)
    path=EVIDENCE/f'plan-{args.role}.json'
    if args.mode=='check':
        with path.open('x',encoding='utf-8') as f:json.dump(plan,f,indent=2)
        print(json.dumps({k:plan[k] for k in ['role','chip','mac','offset','partition_size','candidate_sha256','deployment_permitted']}));return
    previous=json.loads(path.read_text())
    if previous!=plan:raise ValueError('Source/artifact/backup drift since reviewed plan; no write allowed')
    # Check all three plans before the first gateway write. No incomplete
    # topology deployment and no unprovisioned C image may pass this gate.
    for role in (('A','B') if args.room1_only else ('A','B','C')):
        other=json.loads((EVIDENCE/f'plan-{role}.json').read_text())
        if other!=prepared(role,json.loads((EVIDENCE/f'backup-{role}.json').read_text())):
            raise ValueError('Three-board readiness changed')
    if args.role!='B':
        receipt=json.loads((EVIDENCE/'deployment-B.json').read_text())
        if not receipt.get('complete') or receipt.get('candidate_sha256')!=json.loads((EVIDENCE/'plan-B.json').read_text())['candidate_sha256']:
            raise ValueError('Matching gateway-first deployment/readback receipt required')
    transport=None
    try:
        transport=LiveTransport(args.port,args.no_reset,plan['identity'])
        if transport.identity()!=plan['identity']:raise ValueError('Live chip/MAC/flash/security identity changed')
        original=verify_backup_pair(plan['primary'],plan['secondary'],plan['backup_sha256'])
        current=transport.current_snapshot(original,plan)
        validate_boot_only_drift(original,current,plan)
        # Fresh NVS/OTA bytes are preserved, never overwritten by the older backup.
        first=Path(plan['primary']).parent/'predeploy-full-flash.bin'
        second=Path(plan['secondary']).parent/'predeploy-full-flash.bin'
        for destination in (first,second):
            with destination.open('xb') as f:f.write(current)
        verify_backup_pair(first,second,sha(current))
        current_plan={**plan,'backup_sha256':sha(current)}
        transport.prewrite_cache=current # MCU remains in the same ROM session.
        result=transfer_application(transport,current_plan,plan['identity'],first,second,plan['candidate_path'],
                                   EVIDENCE/f'deployment-{args.role}.json',EVIDENCE/f'rollback-app-{args.role}.bin')
        # Extend receipt with exact artifact binding after verified transfer.
        result.update(candidate_sha256=plan['candidate_sha256'],mac=plan['mac'],port=args.port,
                      protected_bytes='Every byte outside the candidate image write, including final-sector tail',
                      verification_method='Application verify-flash plus independent on-device full-flash MD5 against expected complete image; mutable partition reads preserve current NVS/OTA')
        (EVIDENCE/f'deployment-{args.role}.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
        print(json.dumps(result))
    finally:
        if transport:transport.close()

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('mode',choices=['check','deploy'])
    p.add_argument('--role',choices=['A','B','C'],required=True);p.add_argument('--port');p.add_argument('--no-reset',action='store_true');p.add_argument('--revision');p.add_argument('--room1-only',action='store_true')
    a=p.parse_args()
    if a.revision:
        import re
        if not re.fullmatch(r'[a-z0-9-]+',a.revision):p.error('Invalid revision label')
        EVIDENCE=EVIDENCE/a.revision
    if a.mode=='deploy' and not a.port:p.error('--port is required for deploy')
    main(a)
