import json
import hashlib
from pathlib import Path
import deploy_three_board as rollout
from esptool.cmds import read_flash
stage=rollout.ROOT/'benchmarks/review/three-board-20261102/rooms-restored'
plan=json.loads((stage/'plan-B.json').read_text())
backup=Path(plan['primary']).with_name('predeploy-full-flash.bin').read_bytes()
image=Path(plan['candidate_path']).read_bytes()
transport=rollout.LiveTransport('COM9',False,plan['identity'])
try:
    transport.esp.change_baud(115200)
    start=plan['offset']; end=(start+len(image)+4095)//4096*4096
    assert rollout.sha(image)==plan['candidate_sha256']
    for offset, data in [(0,backup[:start]),(end,backup[end:])]:
        digest=transport.esp.flash_md5sum(offset,len(data))
        if isinstance(digest,bytes): digest=digest.decode('ascii')
        assert digest.lower()==hashlib.md5(data).hexdigest()
    payload=image+backup[start+len(image):end]
    transport.write(start,payload)
    transport.verify(start,payload)
    transport.verify_full(backup[:start]+image+backup[start+len(image):])
    (stage/'recovery.json').write_text(json.dumps(dict(complete=True,application_readback=True,protected_region_preservation=True,candidate_sha256=rollout.sha(image))))
    print('RECOVERY PASS')
finally:
    transport.close()
