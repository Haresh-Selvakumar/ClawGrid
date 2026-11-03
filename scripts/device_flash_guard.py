"""Device-specific app-only guard. No erase, partition/NVS or model writes.

All offsets come from a preserved, checksum-validated on-device partition
table, compared with the candidate's table. Unknown boot selection refuses.
Pure validation routines are usable without connecting a board.
"""
import binascii
import hashlib
import json
import struct
from pathlib import Path

SECTOR=4096

def sha(data): return hashlib.sha256(data).hexdigest()

def parse_table(table,flash_size,require_checksum=True):
    rows=[]; seen=set(); md5=False
    for at in range(0,min(len(table),0xc00),32):
        entry=table[at:at+32]
        if len(entry)!=32: raise ValueError('Truncated partition table')
        if entry[:2]==b'\xeb\xeb':
            if entry[16:]!=hashlib.md5(table[:at]).digest(): raise ValueError('Partition MD5 mismatch')
            md5=True;break
        if entry==b'\xff'*32: break
        if entry[:2]!=b'\xaa\x50': raise ValueError('Invalid partition entry magic')
        _,typ,sub,offset,size,label,flags=struct.unpack('<HBBII16sI',entry)
        label=label.split(b'\0')[0].decode('ascii')
        if not label or label in seen: raise ValueError('Duplicate/empty partition label')
        if flags or offset%SECTOR or not size or size%SECTOR or offset+size>flash_size:
            raise ValueError('Encrypted/unaligned/out-of-flash partition')
        if typ==0 and offset%0x10000: raise ValueError('Application alignment invalid')
        if any(offset<r['offset']+r['size'] and r['offset']<offset+size for r in rows):
            raise ValueError('Overlapping partitions')
        rows.append(dict(type=typ,subtype=sub,offset=offset,size=size,label=label,flags=flags));seen.add(label)
    if not rows or (require_checksum and not md5): raise ValueError('A checksum-validated partition table is required')
    if not md5 and table[32*len(rows):0xc00]!=b'\xff'*(0xc00-32*len(rows)):
        raise ValueError('Legacy table must have an unambiguous erased terminator')
    return rows

def inspect_layout(full_flash,allow_verified_legacy=False):
    """Discover the table in the system area; do not import the S3 CSV for C."""
    found=[]
    for offset in range(SECTOR,min(0x10000,len(full_flash)),SECTOR):
        if full_flash[offset:offset+2]!=b'\xaa\x50': continue
        try: rows=parse_table(full_flash[offset:offset+SECTOR],len(full_flash),not allow_verified_legacy)
        except ValueError: continue
        if any(r['offset']<offset+SECTOR for r in rows): continue
        found.append(dict(table_offset=offset,partitions=rows))
    if len(found)!=1: raise ValueError('Exactly one unambiguous checksum-valid partition table required')
    return found[0]

def selected_app(full_flash,layout):
    rows=layout['partitions']; apps=[r for r in rows if r['type']==0]
    factory=[r for r in apps if r['subtype']==0]
    ota=sorted((r for r in apps if 0x10<=r['subtype']<=0x1f),key=lambda r:r['subtype'])
    metadata=[r for r in rows if r['type']==1 and r['subtype']==0]
    if len(factory)>1 or len(metadata)>1: raise ValueError('Ambiguous boot layout')
    if ota and [r['subtype'] for r in ota]!=list(range(0x10,0x10+len(ota))):
        raise ValueError('OTA slots must be contiguous')
    valid=[]
    if metadata:
        m=metadata[0]
        if m['size']<2*SECTOR: raise ValueError('Truncated OTA metadata')
        for i in range(2):
            at=m['offset']+i*SECTOR; entry=full_flash[at:at+32]
            seq=struct.unpack_from('<I',entry)[0];state,crc=struct.unpack_from('<II',entry,24)
            if seq==0xffffffff: continue
            if crc!=(binascii.crc32(entry[:4],0xffffffff)&0xffffffff):
                raise ValueError('Corrupt nonempty OTA selector; manual recovery review required')
            # NEW/PENDING_VERIFY can mutate boot metadata or roll back. Never
            # guess which application will actually run after an in-place write.
            if state in (0,1): raise ValueError('Unconfirmed OTA image; settle rollback state first')
            if state in (3,4): continue
            if state not in (2,0xffffffff) or seq==0: raise ValueError('Unsupported OTA state/sequence')
            valid.append(seq)
    if valid:
        if not ota: raise ValueError('OTA metadata has no corresponding app slots')
        selected=ota[(max(valid)-1)%len(ota)]
    elif factory: selected=factory[0]
    elif ota and metadata: selected=ota[0] # IDF empty-selector default, unchanged metadata.
    else: raise ValueError('No determinable application boot slot')
    if full_flash[selected['offset']]!=0xe9:
        raise ValueError('Selected boot slot lacks an existing ESP application; initial provisioning is separate')
    return selected

def image_header(image):
    if len(image)<288 or image[0]!=0xe9: raise ValueError('Invalid ESP application image')
    # Extended ESP image header chip_id and app descriptor project_name.
    chip=struct.unpack_from('<H',image,12)[0]
    if image[32:36]!=b'\x32\x54\xcd\xab': raise ValueError('Missing ESP-IDF application descriptor')
    project=image[80:112].split(b'\0')[0].decode('ascii')
    size_code=image[3]>>4
    if size_code>7: raise ValueError('Invalid declared flash size')
    return dict(chip_id=chip,declared_flash_bytes=(1<<size_code)*1024*1024,project=project)

def plan_application(full_flash,identity,candidate_image,candidate_table,candidate_sdk,role,board_verified_legacy=False):
    if role not in ('A','B','C'): raise ValueError('Unknown role')
    expected_chip='esp32' if role=='C' else 'esp32s3'
    if identity.get('chip')!=expected_chip or not identity.get('rom_verified'):
        raise ValueError('Fresh compatible ROM chip identity required')
    if identity.get('secure_boot') is not False or identity.get('flash_encryption') is not False:
        raise ValueError('Verified disabled secure boot/encryption required by this development workflow')
    if len(full_flash)!=identity.get('flash_bytes'): raise ValueError('Full backup does not match detected flash size')
    layout=inspect_layout(full_flash,allow_verified_legacy=board_verified_legacy)
    if layout['table_offset']!=0x8000:
        raise ValueError('Candidate build uses default table offset; detected table differs. No layout rewrite permitted')
    if parse_table(candidate_table,len(full_flash),require_checksum=not (role=='C' and board_verified_legacy))!=layout['partitions']:
        raise ValueError('Candidate/on-device partition layout mismatch; rebuild for existing map, never rewrite it')
    installed_table=full_flash[layout['table_offset']:layout['table_offset']+SECTOR]
    try:parse_table(installed_table,len(full_flash))
    except ValueError:
        if role!='C' or not board_verified_legacy or 'CONFIG_PARTITION_TABLE_MD5=y' in candidate_sdk.splitlines():
            raise ValueError('Legacy C table needs its verified compatibility build; runtime must not require absent MD5')
    app=selected_app(full_flash,layout)
    header=image_header(candidate_image)
    if header['chip_id']!=(0 if role=='C' else 9): raise ValueError('Candidate chip family mismatch')
    if header['declared_flash_bytes']>len(full_flash): raise ValueError('Candidate flash size exceeds detected hardware')
    if not 0<len(candidate_image)<=app['size']: raise ValueError('Candidate exceeds selected application partition')
    if 'CONFIG_IDF_TARGET="'+expected_chip+'"' not in candidate_sdk.splitlines():
        raise ValueError('SDK target differs from detected device')
    if role=='C' and not identity.get('dual_core'): raise ValueError('Current C scheduler requires a dual-core classic ESP32')
    from flash_native import validate_image_mode,validate_led_profile,validate_console_mode
    validate_image_mode(candidate_image,False,role!='B')
    validate_led_profile(candidate_image,role!='B',role!='B')
    validate_console_mode(candidate_image,candidate_sdk,True)
    if role!='B' and b'ClawGrid:pir=unavailable\0' not in candidate_image:
        raise ValueError('Controller PIR must be unavailable')
    return dict(role=role,chip=expected_chip,mac=identity['mac'],flash_bytes=len(full_flash),
                offset=app['offset'],partition_size=app['size'],partition_label=app['label'],
                candidate_bytes=len(candidate_image),candidate_sha256=sha(candidate_image),
                table_sha256=sha(full_flash[layout['table_offset']:layout['table_offset']+SECTOR]),
                backup_sha256=sha(full_flash),layout=layout,
                initial_provisioning=False,allowed_write_ranges=[dict(offset=app['offset'],bytes=len(candidate_image))])

def protected_digest(full_flash,plan):
    """Hash every byte outside the one permitted application image write range."""
    start=plan['offset'];end=start+plan['candidate_bytes']
    return sha(full_flash[:start]+full_flash[end:])

def verify_backup_pair(primary,secondary,expected_sha):
    if Path(primary).resolve()==Path(secondary).resolve(): raise ValueError('Distinct preservation paths required')
    data=Path(primary).read_bytes()
    if sha(data)!=expected_sha or sha(Path(secondary).read_bytes())!=expected_sha:
        raise ValueError('Backup copies missing or changed')
    return data

def validate_boot_only_drift(original,current,plan):
    """Boot counters may change after a backup resets the application.

    Accept only NVS/OTA partition drift, preserve it afresh while held in ROM,
    and require unchanged boot selection. Never restore old replay counters.
    """
    if len(current)!=len(original): raise ValueError('Live flash size changed')
    spans=sorted((p['offset'],p['offset']+p['size']) for p in plan['layout']['partitions']
                 if p['type']==1 and p['subtype'] in (0,2))
    last=0
    for start,end in spans:
        if original[last:start]!=current[last:start]: raise ValueError('Live firmware/system/model drift outside NVS/OTA')
        last=end
    if original[last:]!=current[last:]: raise ValueError('Live firmware/system/model drift outside NVS/OTA')
    layout=inspect_layout(current,allow_verified_legacy=True)
    app=selected_app(current,layout)
    if layout!=plan['layout'] or app['offset']!=plan['offset'] or app['size']!=plan['partition_size']:
        raise ValueError('Boot selection or layout changed; re-review without writing')

def transfer_application(transport,plan,identity,primary,secondary,candidate_path,receipt_path,rollback_path):
    """Injected transport supplies identity/read/write/verify; exactly one rollback on failure.

    Before transport.write, full live flash must match the preserved backup.
    Full readback proves every protected byte, not only selected system ranges.
    The caller must keep the MCU in ROM between operations; no automatic retry.
    """
    receipt_path=Path(receipt_path)
    if receipt_path.exists() or Path(rollback_path).exists(): raise ValueError('Preserve existing operation receipts')
    backup=verify_backup_pair(primary,secondary,plan['backup_sha256'])
    image=Path(candidate_path).read_bytes()
    if sha(image)!=plan['candidate_sha256'] or len(image)!=plan['candidate_bytes']:
        raise ValueError('Candidate changed after validation')
    if transport.identity()!=identity: raise ValueError('Live chip/MAC/flash/security identity changed')
    if transport.read(0,len(backup))!=backup: raise ValueError('Live flash drifted since backup; preserve new state before writing')
    start=plan['offset'];end=start+len(image)
    if start%SECTOR or (end+SECTOR-1)//SECTOR*SECTOR>start+plan['partition_size']:
        raise ValueError('Application erase-sector range exceeds selected partition')
    # Include the original tail of the last erase sector. esptool otherwise
    # erases those protected bytes while writing a non-sector-sized image.
    aligned_end=(end+SECTOR-1)//SECTOR*SECTOR
    payload=image+backup[end:aligned_end]
    result=dict(role=plan['role'],complete=False,write_attempted=False,rollback='NOT_NEEDED')
    try:
        result['write_attempted']=True
        transport.write(start,payload)
        transport.verify(start,payload)
        expected=backup[:start]+image+backup[end:]
        if hasattr(transport,'verify_full'):
            transport.verify_full(expected)
        else:
            observed=transport.read(0,len(backup))
            if observed[start:end]!=image or protected_digest(observed,plan)!=protected_digest(backup,plan):
                raise RuntimeError('Application readback or protected-region preservation failed')
        result.update(complete=True,application_readback=True,protected_region_preservation=True)
    except Exception as error:
        result['error']=str(error)
        # Restore only the selected application partition, never system/NVS/OTA.
        # If protection was damaged, stop for explicit recovery; do not erase or
        # overwrite unrelated regions to conceal that failure.
        try:
            original=backup[start:start+plan['partition_size']]
            Path(rollback_path).write_bytes(original)
            transport.write(start,original);transport.verify(start,original)
            if hasattr(transport,'verify_full'):transport.verify_full(backup)
            elif transport.read(0,len(backup))!=backup: raise RuntimeError('Rollback/protected flash mismatch')
            result['rollback']='APP_AND_PROTECTED_BYTES_VERIFIED'
        except Exception as failure:
            result['rollback']='FAILED: '+str(failure)
        raise RuntimeError('Deployment stopped; '+result['rollback']) from error
    finally:
        with receipt_path.open('x',encoding='utf-8') as f:json.dump(result,f,indent=2)
    return result
