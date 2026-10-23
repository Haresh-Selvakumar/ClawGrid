#pragma once
static const char kDashboardClient[] = R"JS(
'use strict';
globalThis.ClawGrid = (() => {
  const Mode=Object.freeze({physical:'physical_bedroom',virtual:'virtual_rooms'});
  const rooms=['bedroom','living_room','kitchen','study'];
  let pending=null, revision=0, capturing=false;
  const same=(a,b)=>['target','room','device','action'].every(k=>a[k]===b[k]);
  async function request(path,body){
    const response=await fetch(path,{method:body===undefined?'GET':'POST',headers:{'Content-Type':'text/plain','X-ClawGrid-Token':document.getElementById('token')?.value||''},body});
    const value=await response.json();
    if(!response.ok)throw Error(value.error||`HTTP ${response.status}`);
    return value;
  }
  function clear(){pending=null; ++revision; for(const id of ['confirmSpeech','confirmManual','execute']){const b=document.getElementById(id);if(b)b.disabled=true;}}
  function selectedMode(){const value=document.getElementById('targetMode')?.value;return Object.values(Mode).includes(value)?value:null;}
  function validate(t){
    if(!Object.values(Mode).includes(t.target)||!rooms.includes(t.room)||!['light','fan'].includes(t.device)||!['ON','OFF','TOGGLE'].includes(t.action))throw Error('Invalid target or action');
    if(t.target===Mode.physical&&t.room!=='bedroom')throw Error('Physical target supports bedroom only; select virtual rooms');
  }
  async function prepare(t,source='manual',job=0){
    clear(); validate(t);const mine=revision;
    const value=await request('/api/proposal',`${source} ${t.target} ${t.room} ${t.device} ${t.action} ${job}`);
    if(revision!==mine)throw Error('Selection changed; prepare again');
    pending=Object.freeze({...t,proposal_id:value.proposal_id});return {...pending};
  }
  async function confirm(t){
    validate(t);if(!pending||!same(pending,t))throw Error('Proposal or target changed; prepare again');
    if(t.target!==selectedMode())throw Error('Selected target changed; prepare again');
    const p=pending;clear();
    return request('/api/confirm',`${p.proposal_id} ${p.target} ${p.room} ${p.device} ${p.action}`);
  }
  async function result(id,show){
    show(`Confirmed/submitted #${id}; waiting for software ACK. Physical state: not measured.`);
    for(let i=0;i<25;++i){await new Promise(r=>setTimeout(r,120));const t=await request('/api/transaction');if(t.id!==id)continue;
      if(t.phase===2){show(`${t.status===0?'ACK: controller accepted':'Controller rejected: '+(['Applied','Denied by SafetyKernel','Invalid','Replay','Wrong boot','Disabled','Expired'][t.status]||'unknown')} #${id}. Controller-commanded state requires fresh telemetry. Physical state: not measured.`);return t;}
      if(t.phase===3)throw Error('ACK timeout; command outcome unknown. Physical state: not measured.');
    }throw Error('ACK not observed; outcome unknown.');
  }
  // Source rate may be 44.1/48 kHz even when a 16 kHz constraint was requested.
  // Area averaging preserves DC and limits aliasing when downsampling.
  function pcm16(channels,rate,maxSamples=64000){
    if(!Number.isFinite(rate)||rate<16000||rate>192000||!channels.length||channels.some(c=>c.length!==channels[0].length))throw Error('Invalid capture format');
    const input=channels[0].length, count=Math.min(maxSamples,Math.floor(input*16000/rate));
    const bytes=new Uint8Array(count*2),view=new DataView(bytes.buffer),ratio=rate/16000;
    for(let i=0;i<count;++i){let sum=0,weight=0,start=i*ratio,end=(i+1)*ratio;
      for(let j=Math.floor(start);j<Math.ceil(end)&&j<input;++j){const w=Math.min(end,j+1)-Math.max(start,j);let mono=0;for(const c of channels){if(!Number.isFinite(c[j]))throw Error('Non-finite audio');mono+=c[j]/channels.length;}sum+=mono*w;weight+=w;}
      const sample=Math.max(-1,Math.min(1,sum/weight));view.setInt16(i*2,Math.round(sample*(sample<0?32768:32767)),true);
    }return {bytes,samples:count,truncated:Math.floor(input*16000/rate)>maxSamples,sourceRate:rate};
  }
  async function capture(status){
    if(!isSecureContext||!navigator.mediaDevices)throw Error('Microphone requires localhost or HTTPS');
    if(capturing)throw Error('Recording already active');capturing=true;let media,context,processor,source,timer,done=false;const chunks=[[]];let length=0;
    const cleanup=async()=>{capturing=false;clearTimeout(timer);processor?.disconnect();source?.disconnect();media?.getTracks().forEach(t=>t.stop());if(context&&context.state!=='closed')await context.close();};
    try{
      media=await navigator.mediaDevices.getUserMedia({audio:{channelCount:1,echoCancellation:false,noiseSuppression:false,autoGainControl:false}});
      context=new AudioContext();await context.resume();source=context.createMediaStreamSource(media);processor=context.createScriptProcessor(1024,1,1);const mute=context.createGain();mute.gain.value=0;source.connect(processor);processor.connect(mute);mute.connect(context.destination);
      // Raw byte upload uses a separate request; never serialize PCM as text.
      const upload=async()=>{if(done)return;done=true;await cleanup();const mono=new Float32Array(length);let at=0;for(const c of chunks[0]){mono.set(c,at);at+=c.length;}const pcm=pcm16([mono],context.sampleRate);if(pcm.samples<1600)throw Error('Recording too short');status(`Uploading ${pcm.samples} samples, ${context.sampleRate} → 16000 Hz; capped at four seconds`);const response=await fetch('/api/speech/audio',{method:'POST',headers:{'Content-Type':'application/octet-stream','X-ClawGrid-Token':document.getElementById('token').value},body:pcm.bytes});const value=await response.json();if(!response.ok)throw Error(value.error||'Upload failed');status(`Queued audio job ${value.job}; awaiting Node B recognition`);return value;};
      processor.onaudioprocess=e=>{if(done)return;const input=e.inputBuffer.getChannelData(0),n=Math.min(input.length,Math.ceil(context.sampleRate*4)-length);if(n>0){chunks[0].push(input.slice(0,n));length+=n;}if(length>=Math.ceil(context.sampleRate*4))upload().catch(e=>status(e.message));};
      timer=setTimeout(()=>upload().catch(e=>status(e.message)),4500);status('Recording up to four seconds; all inference remains on Node B');return upload;
    }catch(error){done=true;await cleanup();throw error;}
  }
  if(typeof document!=='undefined')document.addEventListener('change',e=>{if(['targetMode','room','device','token','timerDevice'].includes(e.target.id))clear();});
  return {Mode,rooms,request,clear,prepare,confirm,result,selectedMode,pcm16,capture,get capturing(){return capturing;}};
})();
)JS";
