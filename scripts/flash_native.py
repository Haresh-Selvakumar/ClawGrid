"""Flash reviewed local artifacts with esptool 5.4, preserving NVS.

The PlatformIO-bundled esptool 4.5 was unreliable on this USB setup. No NVS erase,
eFuse programming, secure-boot changes or credential output is performed.
"""
import argparse
import hashlib
import json
import re
import subprocess
import sys
from datetime import datetime,timezone
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]

def validate_image_mode(image,diagnostic,bench=False):
    if bench and diagnostic: raise RuntimeError('Bench outputs and diagnostic faults cannot be combined')
    expected=b'ClawGrid:outputs='+ (b'enabled' if bench else b'inhibited')+b';faults='+ (b'diagnostic' if diagnostic else b'default')
    markers=[b'ClawGrid:outputs='+outputs+b';faults='+faults for outputs in (b'enabled',b'inhibited') for faults in (b'default',b'diagnostic')]
    if expected+b'\0' not in image or any(marker in image for marker in markers if marker!=expected):
        raise RuntimeError('Application build marker does not match the explicitly requested output/fault mode')

def build_environment(role,uart_console=False,diagnostic=False,bench=False):
    if uart_console and role not in ('A','B-speech'): raise RuntimeError('UART console variant is supported only for A and B-speech')
    if uart_console and role=='B-speech' and (diagnostic or bench): raise RuntimeError('B-speech UART console requires the normal inhibited mode')
    if bench and role!='A': raise RuntimeError('Bench outputs are supported only for Node A')
    if bench and diagnostic: raise RuntimeError('Bench outputs and diagnostic faults cannot be combined')
    if diagnostic and role not in ('A','B-speech'): raise RuntimeError('Diagnostic build supported only for A and B-speech')
    env='speech_intelligence' if role=='B-speech' else 'native_voice' if role=='A' else 'native_intelligence'
    if uart_console: env += '_uart'
    if diagnostic: env += '_diagnostic'
    if bench: env += '_bench'
    return env

def validate_console_mode(image,sdk,uart_console):
    transport=b'ClawGrid:console='+ (b'uart0' if uart_console else b'usb_serial_jtag')+b'\0'
    required='CONFIG_ESP_CONSOLE_UART_DEFAULT=y' if uart_console else 'CONFIG_ESP_CONSOLE_USB_SERIAL_JTAG=y'
    if required not in sdk.splitlines() or transport not in image:
        raise RuntimeError('Console configuration does not match the requested image transport')


def validate_pair_identity(image,config):
    marker=('ClawGrid:pair:A='+config['node_a_mac'].lower()+';B='+config['node_b_mac'].lower()).encode()+b'\0'
    if marker not in image:
        raise RuntimeError('Image board assignments are stale or unverified. Rebuild this environment before flashing.')


def read_identity(output):
    match=re.search(r'^MAC:\s*([0-9a-f]{2}(?::[0-9a-f]{2}){5})\b',output,re.I|re.M)
    if not match: raise RuntimeError('esptool returned no readable MAC identity; no flash was written.')
    return match.group(1).lower()


def validate_blank_model_partition(partition, expected_partition, model_bytes):
    if len(partition) < len(expected_partition) or partition[:len(expected_partition)] != expected_partition:
        raise RuntimeError('Installed partition table differs; preserve it and stop before model installation')
    if len(model_bytes) != 0x600000 or model_bytes != b'\xff' * 0x600000:
        raise RuntimeError('Existing speech-model region is not entirely blank; no model will be overwritten')


def prepare_pinned_model(project, build, sdk):
    # Upstream packer removes its generated srmodels directory. Resolve and bound
    # that exact directory first; never pass a computed external path to it.
    target=(build/'srmodels').resolve()
    if target.parent != build.resolve() or not target.is_relative_to(ROOT.resolve()):
        raise RuntimeError('Generated model target escapes the intended build directory')
    subprocess.run([sys.executable,str(project/'components/esp-sr/model/movemodel.py'),
        '-d1',str(sdk),'-d2',str(project/'components/esp-sr'),'-d3',str(build)],check=True)
    model=target/'srmodels.bin'
    if not model.is_file() or not 0 < model.stat().st_size <= 0x600000:
        raise RuntimeError('Packed pinned models do not fit the existing model partition')
    return model


def main(args):
    speech=args.role=='B-speech'; a=args.role=='A'
    port=args.port or ('COM8' if a else 'COM7')
    config=json.loads((ROOT/'local_credentials.json').read_text())
    expected=config['node_a_mac' if a else 'node_b_mac'].lower()
    project=ROOT/('speech' if speech else 'native')
    env=build_environment(args.role,args.uart_console,args.diagnostic,args.bench)
    missing_model=getattr(args,'install_missing_speech_model',False)
    if missing_model and (not speech or not args.app_only or args.diagnostic or args.bench):
        raise RuntimeError('Missing-model installation requires normal inhibited B-speech --app-only')
    if args.bench: print('Explicit BENCH selection: this image enables Node A relay GPIOs. Verify the low-voltage interface first.',flush=True)
    build=project/'.pio/build'/env
    # CMake compile_commands omits PlatformIO's application build_flags. Read
    # the retained application marker instead of mistaking that file for proof.
    validate_image_mode((build/'firmware.bin').read_bytes(),args.diagnostic,args.bench)
    validate_pair_identity((build/'firmware.bin').read_bytes(),config)
    sdk=(project/('sdkconfig.'+env)).read_text()
    validate_console_mode((build/'firmware.bin').read_bytes(),sdk,args.uart_console)
    required=['CONFIG_ESPTOOLPY_FLASHSIZE_16MB=y','CONFIG_ESP32S3_SPIRAM_SUPPORT=y','CONFIG_SPIRAM_MODE_OCT=y']
    if any(line not in sdk.splitlines() for line in required): raise RuntimeError('Expected N16R8 SDK configuration is missing')
    files=[(0,build/'bootloader.bin'),(0x8000,build/'partitions.bin'),(0xe000,build/'ota_data_initial.bin'),(0x10000,build/'firmware.bin')]
    if args.app_only:
        files=[(0x10000,build/'firmware.bin')]
    if speech and not args.app_only:
        model=prepare_pinned_model(project,build,project/('sdkconfig.'+env))
        files.append((0x610000,model))
    if not all(path.is_file() for _,path in files): raise RuntimeError('Build artifacts missing; compile first')
    command=[sys.executable,'-m','esptool','--chip','esp32s3','--port',port,'--baud','460800']
    identity=subprocess.run(command+['--after','no-reset','read-mac'],capture_output=True,text=True,timeout=30)
    if identity.returncode:
        raise RuntimeError('Cannot read board identity on '+port+'; no flash was written. Close the voice bridge/serial monitor and retry. esptool output: '+(identity.stdout+'\n'+identity.stderr).strip())
    actual=read_identity(identity.stdout)
    if actual!=expected:
        raise RuntimeError(f'{port} has MAC {actual}, but role {args.role} requires {expected}. No flash was written. Check the port and saved role assignment; tap RESET to resume its application.')
    stamp=datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')
    if missing_model:
        # Fresh read while the verified B is in the loader. These files may be
        # private and remain under the existing ignored backup directory.
        backup=ROOT/'benchmarks/backups'/('blank-model-'+stamp)
        backup.mkdir(parents=True,exist_ok=False)
        for address,size,name in [(0x8000,0x1000,'partitions.bin'),(0x610000,0x600000,'model-before.bin')]:
            with (backup/(name+'.log')).open('w') as output:
                subprocess.run(command+['--after','no-reset','read-flash',hex(address),hex(size),str(backup/name)],
                    stdout=output,stderr=subprocess.STDOUT,check=True,timeout=240)
        validate_blank_model_partition((backup/'partitions.bin').read_bytes(),
            (build/'partitions.bin').read_bytes(),(backup/'model-before.bin').read_bytes())
        model=prepare_pinned_model(project,build,project/('sdkconfig.'+env))
        files.append((0x610000,model))
        print('Verified existing model region is entirely blank; installing pinned assets there only. Partition table/NVS remain outside write ranges.',flush=True)
    log=ROOT/f'benchmarks/raw/hardware/flash_{env}_{stamp}.log'; log.parent.mkdir(parents=True,exist_ok=True)
    arguments=['write-flash','--flash-mode','dio','--flash-freq','80m','--flash-size','16MB']
    for address,path in files: arguments += [hex(address),str(path)]
    with log.open('w') as output:
        output.write(identity.stdout); output.flush()
        result=subprocess.run(command+arguments,stdout=output,stderr=subprocess.STDOUT,timeout=180)
    manifest=dict(role=args.role,port=port,mac=expected,environment=env,physical_outputs_enabled=args.bench,diagnostic_faults_enabled=args.diagnostic,tool='esptool 5.4.0',exit_code=result.returncode,
                  installed_missing_model=missing_model,
                  artifacts=[dict(address=hex(address),path=str(path.relative_to(ROOT)),sha256=hashlib.sha256(path.read_bytes()).hexdigest()) for address,path in files])
    log.with_suffix('.json').write_text(json.dumps(manifest,indent=2)+'\n')
    print(f'Flash {"PASS" if result.returncode==0 else "FAIL"}: {args.role}; log {log}',flush=True)
    return result.returncode


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__); parser.add_argument('--role',choices=['A','B','B-speech'],required=True); parser.add_argument('--port')
    parser.add_argument('--app-only',action='store_true',help='Development app0 rewrite; requires the existing verified partition/boot/model images')
    parser.add_argument('--install-missing-speech-model',action='store_true',help='Normal B-speech app-only: fresh-read matching partition table and require fully blank model region before installing pinned assets; never overwrite existing models')
    mode=parser.add_mutually_exclusive_group()
    mode.add_argument('--diagnostic',action='store_true',help='Explicit inhibited USB fault/replay build; restore default firmware after the campaign')
    mode.add_argument('--bench',action='store_true',help='EXPLICIT OUTPUT-ENABLED Node A bench image; requires verified low-voltage interface/polarity; never use for USB-only work')
    parser.add_argument('--uart-console',action='store_true',help='A or normal B-speech variant using its verified UART0 USB adapter')
    raise SystemExit(main(parser.parse_args()))
