import json
from pathlib import Path
from research_hil import Bench

bench=Bench(['COM10','COM9'],native=True)
results=[]
try:
    bench.poll(2)
    for suffix in ['', '?room=2']:
        reply=bench.http('GET','/api/state'+suffix,timeout=5)
        if not reply or reply.get('status')==502:
            bench.poll(1)
            reply=bench.http('GET','/api/state'+suffix,timeout=5)
        results.append(reply)
    Path('benchmarks/review/three-board-20261102/rooms-restored/live-readback.json').write_text(json.dumps(results,indent=2))
    for reply in results:
        state=reply.get('body',{}) if reply else {}
        print(json.dumps({key:state.get(key) for key in ['room','node_id','known','age_ms','boot_id','light','fan','temperature_status']}))
finally:
    bench.stack.close()
