import json
from pathlib import Path
import deploy_three_board as rollout
from device_flash_guard import sha, transfer_application

root=rollout.ROOT
stage=root/'benchmarks/review/three-board-20261102/rooms-restored'
stage.mkdir(exist_ok=True)
previous=root/'benchmarks/review/three-board-20261102/room1-order'
backup=json.loads((previous/'backup-B.json').read_text())
receipt=json.loads((previous/'deployment-B.json').read_text())
old_image=(root/'benchmarks/review/room1-sensor-order-20261102/candidate/B/firmware.bin').read_bytes()
assert receipt['complete'] and receipt['candidate_sha256']==sha(old_image)
for key,folder in [('primary',root/'benchmarks/backups/three-board-20261102/B-rooms-restored'),('second',root/'output/private-rollback-three-board-20261102/B-rooms-restored')]:
    folder.mkdir(exist_ok=True)
    data=bytearray(Path(backup[key]).with_name('predeploy-full-flash.bin').read_bytes())
    data[65536:65536+len(old_image)]=old_image
    target=folder/'full-flash.bin'
    if target.exists(): assert target.read_bytes()==data
    else: target.write_bytes(data)
    backup[key]=str(target)
backup['full_flash_sha256']=sha(data)
plan=rollout.prepared('B',backup)
(stage/'plan-B.json').write_text(json.dumps(plan,indent=2))
transport=None
try:
    transport=rollout.LiveTransport('COM9',False,plan['identity'])
    assert transport.identity()==plan['identity']
    current=transport.current_snapshot(bytes(data),plan)
    for key in ['primary','second']:
        Path(backup[key]).with_name('predeploy-full-flash.bin').write_bytes(current)
    transport.prewrite_cache=current
    plan['backup_sha256']=sha(current)
    result=transfer_application(transport,plan,plan['identity'],Path(backup['primary']).with_name('predeploy-full-flash.bin'),Path(backup['second']).with_name('predeploy-full-flash.bin'),plan['candidate_path'],stage/'deployment-B.json',stage/'rollback-app-B.bin')
    result['candidate_sha256']=plan['candidate_sha256']
    (stage/'deployment-B.json').write_text(json.dumps(result,indent=2))
    print(json.dumps(result))
finally:
    if transport: transport.close()
