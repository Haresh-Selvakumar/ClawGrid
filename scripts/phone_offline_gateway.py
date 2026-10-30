"""Guarded B app-only deployment/rollback; model, NVS and partition table are never write targets."""
import argparse
import hashlib
import json
import shutil
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from scripts.mn7_migration import check_backup
from scripts.flash_native import read_identity, validate_image_mode, validate_pair_identity

def main(args):
    backup = ROOT / 'benchmarks/backups/mn7-20261025T072239Z'
    saved = check_backup(backup)
    config = json.loads((ROOT / 'local_credentials.json').read_text())
    evidence = ROOT / 'benchmarks/review/phone-offline-v1-20261025'
    stamp = datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')
    logdir = evidence / (args.action + '-' + stamp)
    logdir.mkdir(exist_ok=False)
    build = ROOT / 'speech/.pio/build/speech_intelligence_uart'
    candidate = evidence / 'phone-gateway.bin'
    if args.action == 'deploy':
        image = (build / 'firmware.bin').read_bytes()
        validate_image_mode(image, False, False); validate_pair_identity(image, config)
        assert len(image) < 0x300000
        shutil.copyfile(build / 'firmware.bin', candidate)
        shutil.copyfile(build / 'firmware.elf', evidence / 'phone-gateway.elf')
    else:
        # Full saved app0 restores unused tail bytes too. Never write the model during rollback.
        image = (backup / 'app0.bin').read_bytes()
        validate_image_mode(image, False, False); validate_pair_identity(image, config)
        shutil.copyfile(backup / 'app0.bin', build / 'firmware.bin')
        shutil.copyfile(evidence / 'baseline-firmware.elf', build / 'firmware.elf')
    command = [sys.executable, '-m', 'esptool', '--chip', 'esp32s3', '--port', args.port, '--baud', '460800']
    def run(name, tail):
        result = subprocess.run(command + tail, capture_output=True, text=True, timeout=150)
        (logdir / (name + '.log')).write_text(result.stdout + result.stderr)
        if result.returncode: raise RuntimeError(name + ' failed; see private log; no automatic retry')
        return result.stdout
    mac = read_identity(run('identity', ['--after','no-reset','read-mac']))
    if mac != config['node_b_mac'].lower(): raise RuntimeError('Board identity mismatch; no flash written')
    # Original baseline and its two copies are verified before the first candidate write.
    if args.action == 'deploy':
        run('baseline-app-verify', ['--after','no-reset','verify-flash','0x10000',str(backup/'app0.bin')])
    for name, address in [('system','0x0'),('model','0x610000')]:
        run('before-'+name, ['--after','no-reset','verify-flash',address,str(backup/(name+'.bin'))])
    with (logdir / 'guarded-flash.log').open('w') as output:
        subprocess.run([sys.executable, str(ROOT/'scripts/flash_native.py'), '--role','B-speech',
            '--port',args.port,'--uart-console','--app-only'], stdout=output, stderr=subprocess.STDOUT,
            check=True, timeout=210, cwd=ROOT)
    # Verify code and all unchanged protected image regions, then return B to normal execution.
    run('app-verify', ['--after','no-reset','verify-flash','0x10000',str(build/'firmware.bin')])
    run('after-system', ['--after','no-reset','verify-flash','0x0',str(backup/'system.bin')])
    run('after-model', ['--after','hard-reset','verify-flash','0x610000',str(backup/'model.bin')])
    receipt = dict(source='HARDWARE_MEASURED', action=args.action, mac=mac, port=args.port,
        app_sha256=hashlib.sha256(image).hexdigest(), bytes=len(image),
        outputs='inhibited', faults='default', write_ranges=[['0x10000',len(image)]],
        model_sha256=saved['regions']['model']['sha256'], system_sha256=saved['regions']['system']['sha256'],
        model_system_before_after_verify=True, backup_copies_verified=2,
        nvs_written=False, partition_layout_changed=False, receipt=str(logdir/'receipt.json'))
    (logdir/'receipt.json').write_text(json.dumps(receipt, indent=2)+'\n')
    print(json.dumps(receipt, indent=2))

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['deploy','rollback'])
    parser.add_argument('--port', required=True, help='Freshly enumerated and identity-verified B port')
    main(parser.parse_args())
