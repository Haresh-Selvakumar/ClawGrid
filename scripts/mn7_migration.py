"""Recoverable B-only model evaluation transport. Never erase or write NVS/table.

Backup verifies fresh ROM identity, two independent local copies and on-board
flash hashes. Deploy requires an independently approved READY candidate manifest.
The present MN7 preparation may deliberately remain BLOCKED at its threshold gate.
"""
import argparse
import hashlib
import json
import shutil
import struct
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT))
from scripts.flash_native import read_identity,validate_image_mode,validate_pair_identity

REGIONS={'system':(0,0x9000),'nvs':(0x9000,0x5000),'otadata':(0xe000,0x2000),
         'app0':(0x10000,0x300000),'model':(0x610000,0x600000)}
BASELINE_SHA='f30b10cfb74a4dd79ff28588de2d3be9f6c760c934b48cf1475bf6f7c271b9a4'

def sha(data):return hashlib.sha256(data).hexdigest()

def validate_table(data):
    rows={}
    for n in range(0x8000,0x8c00,32):
        b=data[n:n+32]
        if b[:2]!=b'\xaa\x50':break
        magic,typ,sub,offset,size,label,flags=struct.unpack('<HBBII16sI',b)
        rows[label.split(b'\0')[0].decode()]=(offset,size)
    expected={'nvs':(0x9000,0x5000),'otadata':(0xe000,0x2000),'app0':(0x10000,0x300000),
              'app1':(0x310000,0x300000),'model':(0x610000,0x600000),'storage':(0xc10000,0x3f0000)}
    if rows!=expected:raise RuntimeError('Installed partition layout differs; stop without writes')

def check_backup(primary):
    m=json.loads((primary/'backup.json').read_text());other=Path(m['second_copy'])
    if primary.resolve()==other.resolve():raise RuntimeError('Independent backup paths required')
    if set(m['regions'])!=set(REGIONS):raise RuntimeError('Missing or unexpected backup regions')
    for name,row in m['regions'].items():
        if (row['address'],row['size'])!=REGIONS[name]:raise RuntimeError('Unverified backup range')
        for folder in (primary,other):
            p=folder/(name+'.bin')
            if not p.is_file() or p.stat().st_size!=row['size'] or sha(p.read_bytes())!=row['sha256']:
                raise RuntimeError('Backup missing/changed: '+str(p))
    for row in m['private_files']:
        for folder in (primary,other):
            p=folder/'private'/row['name']
            if sha(p.read_bytes())!=row['sha256']:raise RuntimeError('Private backup changed')
    validate_table((primary/'system.bin').read_bytes())
    return m

def validate_candidate(candidate,backup):
    c=json.loads(candidate.read_text())
    if c.get('state')!='READY' or c.get('threshold_verified') is not True or c.get('private_abi_access') is not False:
        raise RuntimeError('Candidate is not READY with independently verified threshold/no-private-ABI gate')
    if c.get('baseline_model_sha256')!=backup['regions']['model']['sha256'] or c.get('mac')!=backup['mac']:
        raise RuntimeError('Candidate does not bind this backup/model/MAC')
    rows=[]
    for name,address,maximum in [('app',0x10000,0x300000),('model',0x610000,0x600000)]:
        row=c[name];p=Path(row['path']);data=p.read_bytes()
        if not 0<len(data)<=maximum or sha(data)!=row['sha256']:raise RuntimeError('Candidate artifact changed or exceeds existing partition')
        rows.append((address,p))
    if c.get('retains_mn6') is not True and c.get('deployment_strategy')!='single-bank-dual-verified-backup':
        raise RuntimeError('A model replacement requires the explicit dual-backup single-bank strategy')
    if b'ClawGrid:voice=mn7-eval-v1\0' not in rows[0][1].read_bytes():raise RuntimeError('Wrong candidate image')
    return c,rows

class Rom:
    def __init__(self,port,log):
        self.base=[sys.executable,'-m','esptool','--chip','esp32s3','--port',port,'--baud','460800']
        self.log=log;self.index=0
    def run(self,parts,after='no-reset',timeout=420):
        self.index+=1
        p=subprocess.run(self.base+['--after',after]+parts,capture_output=True,text=True,encoding='utf-8',errors='replace',timeout=timeout)
        (self.log/f'{self.index:02d}-{parts[0]}.log').write_text(p.stdout+'\n'+p.stderr,encoding='utf-8')
        if p.returncode:raise RuntimeError('ROM operation failed: '+parts[0]+'; see private log; no automatic retry')
        return p.stdout
    def identity(self,expected):
        mac=read_identity(self.run(['read-mac']))
        if mac!=expected:raise RuntimeError('STOP: board MAC mismatch; no write allowed')
        return mac

def backup(args,cfg):
    stamp=datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')
    primary=ROOT/'benchmarks/backups'/('mn7-'+stamp)
    second=Path.home()/'Documents/ClawGrid-private-backups'/('mn7-'+stamp)
    primary.mkdir(parents=True,exist_ok=False);second.mkdir(parents=True,exist_ok=False)
    rom=Rom(args.port,primary)
    m=dict(created_utc=stamp,role='B',mac=cfg['node_b_mac'].lower(),port=args.port,
           second_copy=str(second),regions={},private_files=[],complete=False)
    matched=False
    try:
        rom.identity(m['mac']);matched=True;print('Gateway B ROM MAC MATCH; reading backup only',flush=True)
        for name,(address,size) in REGIONS.items():
            p=primary/(name+'.bin')
            rom.run(['read-flash',hex(address),hex(size),str(p)])
            if p.stat().st_size!=size:raise RuntimeError('Short backup read')
            rom.run(['verify-flash',hex(address),str(p)])
            shutil.copy2(p,second/p.name)
            m['regions'][name]=dict(address=address,size=size,sha256=sha(p.read_bytes()))
            print(name,'read/board verification/second copy complete',flush=True)
        validate_table((primary/'system.bin').read_bytes())
        baseline=ROOT/'benchmarks/review/voice-short-v1-20261025/baseline-firmware.bin'
        b=baseline.read_bytes()
        if sha(b)!=BASELINE_SHA or (primary/'app0.bin').read_bytes()[:len(b)]!=b:raise RuntimeError('Installed application differs from saved baseline')
        for rel in ['local_credentials.json','firmware/common/config/private_config.h','partitions_n16r8.csv']:
            p=ROOT/rel
            for folder in (primary,second):
                dst=folder/'private'/p.name;dst.parent.mkdir(exist_ok=True);shutil.copy2(p,dst)
            m['private_files'].append(dict(name=p.name,sha256=sha(p.read_bytes())))
        m.update(complete=True,baseline_image_sha256=BASELINE_SHA,baseline_image_bytes=len(b),board_flash_verified=True)
        (primary/'backup.json').write_text(json.dumps(m,indent=2)+'\n');shutil.copy2(primary/'backup.json',second/'backup.json')
        check_backup(primary)
        print('Backup VERIFIED in two local locations:',primary,flush=True)
        if args.receipt:args.receipt.write_text(json.dumps(dict(primary=str(primary),second=str(second),complete=True),indent=2)+'\n')
    finally:
        # Read-mac is read-only; a hard-reset resumes B, A is never touched.
        if matched:rom.run(['read-mac'],after='hard-reset',timeout=30)

def transfer(args,cfg):
    primary=args.backup.resolve();m=check_backup(primary)
    if not m.get('complete') or m['mac']!=cfg['node_b_mac'].lower():raise RuntimeError('Complete matching B backup required')
    c=None
    if args.mode=='deploy':c,rows=validate_candidate(args.candidate,m)
    else:rows=[(REGIONS['model'][0],primary/'model.bin'),(REGIONS['app0'][0],primary/'app0.bin')]
    for address,p in rows:
        if address not in (0x10000,0x610000):raise RuntimeError('Write outside app/model forbidden')
    stamp=datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
    log=primary/(args.mode+'-'+stamp);log.mkdir(exist_ok=False);rom=Rom(args.port,log);wrote=False
    result=dict(operation=args.mode,complete=False,write_ranges=[dict(address=a,bytes=p.stat().st_size) for a,p in rows])
    matched=False;failure=None
    try:
        rom.identity(m['mac']);matched=True
        # Bind unchanged table and preserve *current* NVS/otadata, including normal boot counters.
        rom.run(['read-flash','0','0x10000',str(log/'current-system-nvs-ota.bin')])
        current=(log/'current-system-nvs-ota.bin').read_bytes();validate_table(current)
        if current[:0x9000]!=(primary/'system.bin').read_bytes():raise RuntimeError('System/table changed; stop')
        if args.mode=='deploy':
            rom.run(['verify-flash','0x10000',str(primary/'app0.bin')])
            rom.run(['read-flash','0x610000','0x600000',str(log/'current-model.bin')])
            if sha((log/'current-model.bin').read_bytes())!=m['regions']['model']['sha256']:raise RuntimeError('Current model changed; stop before deployment')
            validate_image_mode(rows[0][1].read_bytes(),False,False);validate_pair_identity(rows[0][1].read_bytes(),cfg)
        parts=['write-flash','--flash-mode','dio','--flash-freq','80m','--flash-size','16MB']
        for address,p in rows:parts += [hex(address),str(p)]
        wrote=True;rom.run(parts)
        for address,p in rows:rom.run(['verify-flash',hex(address),str(p)])
        rom.run(['read-flash','0','0x10000',str(log/'after-system-nvs-ota.bin')])
        if (log/'after-system-nvs-ota.bin').read_bytes()!=current:raise RuntimeError('STOP: system/NVS/OTA unexpectedly changed')
        result['complete']=True
    except Exception as e:
        failure=e;result['error']=str(e)
    finally:
        result['write_attempted']=wrote;(log/'result.json').write_text(json.dumps(result,indent=2)+'\n')
        if matched:
            try:rom.run(['read-mac'],after='hard-reset',timeout=30)
            except Exception as e:failure=failure or e
    if failure:
        if args.mode=='deploy' and wrote:
            # Exactly one rollback attempt, never a loop or recognition retry.
            try:
                transfer(argparse.Namespace(mode='rollback',backup=primary,port=args.port,candidate=None),cfg)
                result['rollback']='verified flash restoration; runtime check still required'
            except Exception as e:
                result['rollback']='FAILED: '+str(e)+'; stop and request necessary physical recovery'
            (log/'result.json').write_text(json.dumps(result,indent=2)+'\n')
        raise RuntimeError('Migration stopped: '+str(failure)+'; '+result.get('rollback','no rollback write needed')) from failure
    print(args.mode,'verified; runtime health must be checked separately. Log:',log,flush=True)

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('mode',choices=['backup','deploy','rollback']);p.add_argument('--port',required=True);p.add_argument('--receipt',type=Path);p.add_argument('--backup',type=Path);p.add_argument('--candidate',type=Path)
    a=p.parse_args();cfg=json.loads((ROOT/'local_credentials.json').read_text())
    if a.mode=='backup':backup(a,cfg)
    else:
        if a.backup is None or (a.mode=='deploy' and a.candidate is None):p.error('--backup and, for deploy, --candidate required')
        transfer(a,cfg)
