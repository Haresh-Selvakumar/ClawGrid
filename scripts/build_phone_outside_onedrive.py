"""Build a byte-identical Android source snapshot outside OneDrive reparse-point caches."""
import hashlib
import argparse
import json
import os
import shutil
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEST = Path.home()/'Documents/ClawGrid-Android-build-20261025b'

def copy_tree(source, destination, excluded=()):
    destination.mkdir(parents=True, exist_ok=True)
    for path in source.iterdir():
        if path.name in excluded or path.name.endswith('.lock'):
            continue
        target=destination/path.name
        if path.is_dir(): copy_tree(path,target,excluded)
        else: shutil.copyfile(path,target)

def main(args):
    global DEST
    if args.name:
        import re
        if not re.fullmatch('[a-z0-9-]+',args.name): raise ValueError('Invalid snapshot name')
        DEST=Path.home()/('Documents/ClawGrid-Android-build-'+args.name)
    evidence=ROOT/args.evidence
    if not evidence.resolve().is_relative_to((ROOT/'benchmarks/review').resolve()): raise ValueError('Evidence directory outside review workspace')
    folder=args.build_folder
    if DEST.exists(): raise RuntimeError('Build snapshot exists; preserve it and inspect rather than overwriting')
    DEST.mkdir()
    source=ROOT/'ecosystem/android';snapshot=DEST/'ecosystem/android'
    copy_tree(source,snapshot,{'.gradle','.kotlin','build','.idea','.env'})
    rows=[]
    for path in snapshot.rglob('*'):
        if path.is_file():
            relative=path.relative_to(snapshot)
            digest=hashlib.sha256(path.read_bytes()).hexdigest()
            assert digest==hashlib.sha256((source/relative).read_bytes()).hexdigest()
            rows.append(dict(path=str(relative),sha256=digest))
    # Reuse exact signing identity, never regenerate a key. Release configuration is unchanged.
    debug=DEST/'.android-tools/android-user/debug.keystore';debug.parent.mkdir(parents=True)
    shutil.copyfile(ROOT/'.android-tools/android-user/debug.keystore',debug)
    helper=DEST/'scripts/android_isolated_build.gradle';helper.parent.mkdir()
    shutil.copyfile(ROOT/'scripts/android_isolated_build.gradle',helper)
    cache=Path(args.gradle_cache).resolve() if args.gradle_cache else DEST/'gradle-user'
    if not args.gradle_cache:
        old=ROOT/'.android-tools/gradle-cache'
        copy_tree(old/'wrapper',cache/'wrapper')
        copy_tree(old/'caches/modules-2',cache/'caches/modules-2')
    (DEST/'source-snapshot.json').write_text(json.dumps(dict(source=str(source),files=rows),indent=2)+'\n')
    env=os.environ.copy()
    env.update(JAVA_HOME=r'C:\Program Files\Android\Android Studio\jbr',
               ANDROID_HOME=str(Path.home()/'AppData/Local/Android/Sdk'), GRADLE_USER_HOME=str(cache))
    log=evidence/'android-build-outside-onedrive.log'
    with log.open('w') as output:
        result=subprocess.run([str(snapshot/'gradlew.bat'),'--no-daemon','--console=plain',
            '--init-script','../../scripts/android_isolated_build.gradle',
            '-PclawgridBuildDirectory='+folder,':app:assembleDebug'],
            cwd=snapshot,env=env,stdout=output,stderr=subprocess.STDOUT,timeout=600)
    if result.returncode: raise RuntimeError('Build failed; see '+str(log))
    apk=DEST/'.android-tools'/folder/'app/outputs/apk/debug/app-debug.apk'
    deliverable=ROOT/'deliverables'/args.apk_name
    if deliverable.exists(): raise RuntimeError('Preserve existing APK; choose a new deliverable name')
    shutil.copyfile(apk,deliverable)
    receipt=dict(source='HOST_MEASURED',snapshot=str(DEST),source_files_identical=len(rows),
        apk=str(deliverable),bytes=apk.stat().st_size,sha256=hashlib.sha256(apk.read_bytes()).hexdigest(),
        signing='existing debug key reused',release_signing_modified=False)
    (evidence/'outside-build-receipt.json').write_text(json.dumps(receipt,indent=2)+'\n')
    print(json.dumps(receipt,indent=2))

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--name')
    p.add_argument('--evidence',default='benchmarks/review/phone-offline-v1-20261025')
    p.add_argument('--build-folder',default='phone-offline-outcomes-build-20261025')
    p.add_argument('--apk-name',default='ClawGrid-Phone-Offline-debug-20261025b.apk')
    p.add_argument('--gradle-cache',help='Reuse a verified outside-OneDrive cache, without copying old cloud files')
    main(p.parse_args())
