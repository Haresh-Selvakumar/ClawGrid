"""Seal exactly the user-approved four reference clips. No inference or hardware."""
from pathlib import Path
import hashlib
import json
import struct
import wave
import zlib

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'benchmarks/review/light-on-off-preflight-20261025'
PLAN=ROOT/'experiments/light-on-off-reference-preflight-v1.json'

def main():
    if PLAN.exists():raise RuntimeError('Existing frozen preflight preserved; no replacement fixtures')
    proposal=(ROOT/'experiments/light-on-off-proposal-v1.json').read_bytes()
    if hashlib.sha256(proposal).hexdigest()!='85488688e7cfbb03d37f3b78c44e2abba2df598bd031ef8af7024fa50f7174aa':
        raise RuntimeError('Proposal changed')
    old=json.loads(proposal);fixtures=[]
    specs=[('LIGHT ON',OUT/'LIGHT-ON.wav',1),('LIGHT OFF',OUT/'LIGHT-OFF.wav',2)]
    specs += [(x['name'],ROOT/x['path'],0) for x in old['preflight']['reuse_negative_fixtures']]
    for name,path,expected in specs:
        with wave.open(str(path),'rb') as w:
            if (w.getframerate(),w.getnchannels(),w.getsampwidth(),w.getcomptype())!=(16000,1,2,'NONE'):
                raise RuntimeError('Wrong reference format')
            pcm=w.readframes(w.getnframes())
        samples=struct.unpack('<'+'h'*(len(pcm)//2),pcm)
        fixtures.append(dict(name=name,path=str(path.relative_to(ROOT)).replace('\\','/'),expected_id=expected,
            samples=len(pcm)//2,crc32=zlib.crc32(pcm),sha256=hashlib.sha256(pcm).hexdigest(),
            wav_sha256=hashlib.sha256(path.read_bytes()).hexdigest(),audio_source='SIMULATED',
            peak=max(abs(x) for x in samples),clipped_samples=sum(x in (-32768,32767) for x in samples)))
    for frozen,new in zip(old['preflight']['reuse_negative_fixtures'],fixtures[2:]):
        for key in ('samples','crc32','sha256'):
            if frozen[key]!=new[key]:raise RuntimeError('Predefined negative changed')
    header=['#pragma once','#include <stddef.h>','#include <stdint.h>',
        'struct LightPreflightFixture { size_t bytes; uint32_t crc; uint8_t expected; };',
        'static constexpr LightPreflightFixture kLightPreflightFixtures[] = {']
    header += [f'    {{{x["samples"]*2},{x["crc32"]}u,{x["expected_id"]}}},' for x in fixtures]
    header += ['};',
        'inline bool light_preflight_size(uint32_t job,size_t bytes) {',
        '    return job>=1 && job<=4 && kLightPreflightFixtures[job-1].bytes==bytes;',
        '}',
        'inline bool light_preflight_matches(uint32_t job,size_t bytes,uint32_t crc) {',
        '    return light_preflight_size(job,bytes) && kLightPreflightFixtures[job-1].crc==crc;',
        '}','']
    h=ROOT/'firmware/common/runtime/light_preflight_contract.h';h.write_text('\n'.join(header))
    plan=dict(experiment='light-on-off-reference-preflight-v1',approval='ONLY_FOUR_REFERENCE_CLIPS',
        selected_room='bedroom',mapping=old['mapping'],phrases=old['phrases'],
        probability_threshold=.7,frontend=old['frontend'],model='mn6_en',
        audio_preparation=dict(generator='Microsoft Zira Desktop/System.Speech',rate=0,volume=100,
            wav='16000Hz mono PCM16',positive_syntheses=2,replacements_allowed=False,edits='none'),
        fixtures=fixtures,maximum_jobs=4,phone_recordings_authorized=False,
        proposals_allowed=False,actuator_commands_allowed=False,physical_outputs='INHIBITED',
        positive_pass='Exact accepted ID at>=0.7; original PCM matches; error0/engine_errors0',
        negative_pass='No detection/candidate/result; original PCM matches; error0/engine_errors0',
        stop='First recognition/delivery/engine/link/output/reboot failure; remaining clips NOT RUN; no retry',
        restore='Exact saved baseline application after either outcome; never write model/NVS/table',
        header_sha256=hashlib.sha256(h.read_bytes()).hexdigest(),
        baseline_app_sha256='f30b10cfb74a4dd79ff28588de2d3be9f6c760c934b48cf1475bf6f7c271b9a4',
        baseline_model_sha256='49b9215a315a06e900e1a2f4538b2513611253cc30a243802a5a58aedf9dbc7e')
    PLAN.write_text(json.dumps(plan,indent=2)+'\n')
    digest=hashlib.sha256(PLAN.read_bytes()).hexdigest();(OUT/'frozen-plan.sha256').write_text(digest+'\n')
    print('Frozen manifest SHA256',digest)
    print(json.dumps(fixtures,indent=2))

if __name__=='__main__':main()
