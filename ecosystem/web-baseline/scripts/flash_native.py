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
    if uart_console and role!='A': raise RuntimeError('UART console variant is supported only for Node A')
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


def main(args):
    speech=args.role=='B-speech'; a=args.role=='A'
    port=args.port or ('COM5' if a else 'COM7')
    expected='e0:72:a1:f6:a9:68' if a else 'e0:72:a1:f5:bc:b0'
    project=ROOT/('speech' if speech else 'native')
    env=build_environment(args.role,args.uart_console,args.diagnostic,args.bench)
    if args.bench: print('Explicit BENCH selection: this image enables Node A relay GPIOs. Verify the low-voltage interface first.',flush=True)
    build=project/'.pio/build'/env
    # CMake compile_commands omits PlatformIO's application build_flags. Read
    # the retained application marker instead of mistaking that file for proof.
    validate_image_mode((build/'firmware.bin').read_bytes(),args.diagnostic,args.bench)
    sdk=(project/('sdkconfig.'+env)).read_text()
    validate_console_mode((build/'firmware.bin').read_bytes(),sdk,args.uart_console)
    required=['CONFIG_ESPTOOLPY_FLASHSIZE_16MB=y','CONFIG_ESP32S3_SPIRAM_SUPPORT=y','CONFIG_SPIRAM_MODE_OCT=y']
    if any(line not in sdk.splitlines() for line in required): raise RuntimeError('Expected N16R8 SDK configuration is missing')
    files=[(0,build/'bootloader.bin'),(0x8000,build/'partitions.bin'),(0xe000,build/'ota_data_initial.bin'),(0x10000,build/'firmware.bin')]
    if args.app_only:
        files=[(0x10000,build/'firmware.bin')]
    if speech and not args.app_only:
        subprocess.run([sys.executable,str(project/'components/esp-sr/model/movemodel.py'),'-d1',str(project/('sdkconfig.'+env)),'-d2',str(project/'components/esp-sr'),'-d3',str(build)],check=True)
        model=build/'srmodels/srmodels.bin'
        if model.stat().st_size>0x600000: raise RuntimeError('Speech models exceed reserved partition')
        files.append((0x610000,model))
    if not all(path.is_file() for _,path in files): raise RuntimeError('Build artifacts missing; compile first')
    command=[sys.executable,'-m','esptool','--chip','esp32s3','--port',port,'--baud','460800']
    identity=subprocess.run(command+['--after','no-reset','read-mac'],capture_output=True,text=True,timeout=30)
    if identity.returncode or expected not in identity.stdout.lower(): raise RuntimeError('Board MAC identity did not match its assigned role')
    stamp=datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')
    log=ROOT/f'benchmarks/raw/hardware/flash_{env}_{stamp}.log'; log.parent.mkdir(parents=True,exist_ok=True)
    arguments=['write-flash','--flash-mode','dio','--flash-freq','80m','--flash-size','16MB']
    for address,path in files: arguments += [hex(address),str(path)]
    with log.open('w') as output:
        output.write(identity.stdout); output.flush()
        result=subprocess.run(command+arguments,stdout=output,stderr=subprocess.STDOUT,timeout=180)
    manifest=dict(role=args.role,port=port,mac=expected,environment=env,physical_outputs_enabled=args.bench,diagnostic_faults_enabled=args.diagnostic,tool='esptool 5.4.0',exit_code=result.returncode,
                  artifacts=[dict(address=hex(address),path=str(path.relative_to(ROOT)),sha256=hashlib.sha256(path.read_bytes()).hexdigest()) for address,path in files])
    log.with_suffix('.json').write_text(json.dumps(manifest,indent=2)+'\n')
    print(f'Flash {"PASS" if result.returncode==0 else "FAIL"}: {args.role}; log {log}',flush=True)
    return result.returncode


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__); parser.add_argument('--role',choices=['A','B','B-speech'],required=True); parser.add_argument('--port')
    parser.add_argument('--app-only',action='store_true',help='Development app0 rewrite; requires the existing verified partition/boot/model images')
    mode=parser.add_mutually_exclusive_group()
    mode.add_argument('--diagnostic',action='store_true',help='Explicit inhibited USB fault/replay build; restore default firmware after the campaign')
    mode.add_argument('--bench',action='store_true',help='EXPLICIT OUTPUT-ENABLED Node A bench image; requires verified low-voltage interface/polarity; never use for USB-only work')
    parser.add_argument('--uart-console',action='store_true',help='Node A variant using its verified UART0 USB adapter')
    raise SystemExit(main(parser.parse_args()))
