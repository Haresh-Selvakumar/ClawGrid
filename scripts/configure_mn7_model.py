"""Configure the pinned MN7 constructor's JSON det_threshold, not a runtime ABI.

Retain exact index/file lengths and every acoustic byte outside the one JSON
document. Never alter upstream assets or call the broken public threshold setter.
"""
import hashlib
import json
import math
from pathlib import Path

PINNED_DATA_SHA='f9d646f6301041d22de3dc3b1ae842ce9b314af6410b35814087d8042410a8e6'

def configure(data,probability=.7):
    if probability!=.7:raise ValueError('Frozen probability0.7 only')
    if hashlib.sha256(data).hexdigest()!=PINNED_DATA_SHA:raise ValueError('Unverified or already configured pinned model data')
    found=[];start=0
    while True:
        start=data.find(b'{',start)
        if start<0:break
        end=data.find(b'\0',start)
        if 0<end-start<=20000:
            try:
                obj=json.loads(data[start:end].decode('ascii'))
                if isinstance(obj,dict) and all(k in obj for k in ('conv2d_layer','chunk_size','phoneme_label','language')):found.append((start,end,obj))
            except (ValueError,UnicodeError):pass
        start+=1
    if len(found)!=1:raise ValueError('Require one complete MN7 JSON configuration')
    start,end,old=found[0]
    if old.get('language')!='en' or old.get('phoneme_label')!=1 or 'det_threshold' in old:raise ValueError('Unverified or already configured model')
    new=dict(old,det_threshold=math.log(probability))
    encoded=json.dumps(new,separators=(',',':'),ensure_ascii=True).encode('ascii')
    if len(encoded)>end-start:raise ValueError('No space in original JSON span; never move acoustic data')
    result=data[:start]+encoded+b' '*(end-start-len(encoded))+data[end:]
    if len(result)!=len(data) or result[:start]!=data[:start] or result[end:]!=data[end:]:raise AssertionError('Acoustic bytes changed')
    if json.loads(result[start:end])!=new:raise AssertionError('Configuration round-trip failed')
    return result,dict(json_start=start,json_bytes=end-start,probability=.7,constructor_log_threshold=math.log(.7),outside_json_unchanged=True,index_unchanged=True,runtime_private_abi_access=False,public_setter_called=False,original_sha256=hashlib.sha256(data).hexdigest(),configured_sha256=hashlib.sha256(result).hexdigest())
