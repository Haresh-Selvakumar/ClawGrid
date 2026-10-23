#pragma once
static const char kDashboardClient[] = R"JS(
'use strict';
globalThis.ClawGrid = (() => {
  const Mode=Object.freeze({physical:'physical_bedroom',virtual:'virtual_rooms'});
  const rooms=['bedroom','living_room','kitchen','study'];
  let pending=null, revision=0, capturing=false, voice=null;
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
    if(voice)await stopListening('Stopped for manual control');
    clear(); validate(t);const mine=revision;
    const value=await request('/api/proposal',`${source} ${t.target} ${t.room} ${t.device} ${t.action} ${job}`);
    if(revision!==mine)throw Error('Selection changed; prepare again');
    pending=Object.freeze({...t,proposal_id:value.proposal_id});return {...pending};
  }
  async function confirm(t){
    validate(t);if(!pending||!same(pending,t))throw Error('Proposal or target changed; prepare again');
    if(t.target!==selectedMode())throw Error('Selected target changed; prepare again');
    const p=pending;clear();
    if(document.getElementById('cloudMode')?.checked){
      const value=await request('/local/cloud/commands',JSON.stringify({id:crypto.randomUUID(),target:p.target,room:p.room,device:p.device,action:p.action}));
      return {...value,id:value.cloud_id};
    }
    return request('/api/confirm',`${p.proposal_id} ${p.target} ${p.room} ${p.device} ${p.action}`);
  }
  async function prepareText(scope,text,session=null){
    if(voice && voice!==session)await stopListening('Stopped for manual text control');
    clear();const mine=revision;
    const expected=scope==='bench_pair'?Mode.physical:scope;
    if(!Object.values(Mode).includes(expected)||expected!==selectedMode())throw Error('Select the matching control target first');
    const value=await request('/api/text',`${scope} ${text}`);
    if(revision!==mine)throw Error('Selection changed; prepare again');
    const t={target:value.target,room:value.room,device:value.device,action:value.action};validate(t);
    if(t.target!==expected)throw Error('Unexpected text target; prepare again');
    pending=Object.freeze({...t,proposal_id:value.proposal_id});return {...value};
  }
  async function result(id,show){
    if(typeof id==='string'){
      show(`Queued in Supabase #${id}; waiting for A's software ACK. Physical state: not measured.`);
      for(let i=0;i<50;++i){
        await new Promise(resolve=>setTimeout(resolve,250));
        const entry=await request('/local/cloud/result',JSON.stringify({id}));
        if(entry.result){const t=entry.result;
          if(t.phase===2){show(`${t.status===0?'ACK: controller accepted through Supabase':'Controller rejected cloud command'} #${t.id}. Physical state: not measured.`);return t;}
          throw Error(t.error||'Cloud command outcome unknown; do not automatically resubmit');
        }
      }
      throw Error('Cloud ACK not observed; outcome unknown; command was not resubmitted');
    }
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
    if(capturing||voice)throw Error('Recording already active');capturing=true;let media,context,processor,source,timer,done=false;const chunks=[[]];let length=0;
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
  async function localSpeech(path,body,session,sequence,signal){
    const headers={'Content-Type':'application/octet-stream','X-ClawGrid-Token':document.getElementById('token')?.value||''};
    if(session)headers['X-ClawGrid-Speech-Session']=session;
    if(sequence)headers['X-ClawGrid-Speech-Sequence']=String(sequence);
    const response=await fetch('/local/speech/'+path,{method:path==='status'?'GET':'POST',headers,body,signal});
    const value=await response.json();if(!response.ok)throw Error(value.error||`HTTP ${response.status}`);return value;
  }
  async function stopListening(message='Listening stopped'){
    const s=voice;if(!s)return;voice=null;s.queue.length=0;s.controller?.abort();
    s.processor?.disconnect();s.source?.disconnect();s.mute?.disconnect();s.media?.getTracks().forEach(t=>t.stop());
    if(s.context&&s.context.state!=='closed')await s.context.close();clear();
    if(s.session)await localSpeech('stop',undefined,s.session).catch(()=>{});
    s.status(message);
  }
  async function submitVoice(text,s){
    if(voice!==s||s.commandBusy)return;
    s.commandBusy=true;const heardAt=performance.now();
    try{
      s.status('Heard: '+text);
      const p=await prepareText(s.scope,text,s);
      if(voice!==s||selectedMode()!==s.mode)return;
      if(performance.now()-heardAt>2000)throw Error('Voice command became stale; please repeat');
      const target={target:p.target,room:p.room,device:p.device,action:p.action};
      const queued=await confirm(target);
      const ack=await result(queued.id,m=>{if(voice===s)s.status('Heard: '+text+' · '+m);});
      if(ack.status!==0)throw Error('Controller refused the command; repeat after checking its state');
    }catch(error){
      if(voice===s){clear();s.status('Heard: '+text+' · '+error.message);}
    }finally{s.commandBusy=false;}
  }
  async function startListening(status){
    if(voice||capturing)throw Error('Recording already active');
    if(!isSecureContext||!navigator.mediaDevices)throw Error('Microphone requires localhost or HTTPS');
    const mode=selectedMode();if(!mode)throw Error('Select a control target');
    const alias=!!document.getElementById('benchAlias')?.checked;
    if(alias&&mode!==Mode.physical)throw Error('Bench alias requires Physical bedroom');
    clear();
    const s={mode,scope:alias?'bench_pair':mode,status,queue:[],sequence:0,lastFinal:0,commandBusy:false,sending:false};voice=s;
    const active=()=>voice===s;
    const drain=async()=>{
      if(s.sending||!active())return;s.sending=true;
      try{
        while(active()&&s.queue.length){
          const pcm=s.queue.shift();s.controller=new AbortController();
          const timeout=setTimeout(()=>s.controller.abort(),2500);let answer;
          try{answer=await localSpeech('audio',pcm,s.session,++s.sequence,s.controller.signal);}finally{clearTimeout(timeout);}
          if(!active())break;
          if(answer.rejected)status(answer.rejected+'; please repeat');
          if(answer.final&&answer.final_id>s.lastFinal){
            s.lastFinal=answer.final_id;
            if(answer.text){if(s.commandBusy)status('Previous command is pending; please repeat');else void submitVoice(answer.text,s);}
          }else if(answer.partial&&!s.commandBusy)status('Listening: '+answer.partial);
        }
      }catch(error){if(active())await stopListening('Listening stopped: '+error.message);}
      finally{s.sending=false;}
    };
    try{
      const controller=await request('/api/state');if(!active())return;if(!controller.known)throw Error('Node A unavailable; reconnect before starting voice control');
      s.media=await navigator.mediaDevices.getUserMedia({audio:{channelCount:1,echoCancellation:true,noiseSuppression:true,autoGainControl:true}});
      if(!active()){s.media.getTracks().forEach(t=>t.stop());return;}
      const opened=await localSpeech('start');s.session=opened.session;
      if(!active()){await localSpeech('stop',undefined,s.session).catch(()=>{});return;}
      s.context=new AudioContext();await s.context.resume();
      s.source=s.context.createMediaStreamSource(s.media);s.processor=s.context.createScriptProcessor(1024,1,1);s.mute=s.context.createGain();s.mute.gain.value=0;
      s.source.connect(s.processor);s.processor.connect(s.mute);s.mute.connect(s.context.destination);
      const samples=Math.ceil(s.context.sampleRate/4);let chunk=new Float32Array(samples),used=0;
      s.processor.onaudioprocess=e=>{
        if(!active())return;const input=e.inputBuffer.getChannelData(0);let at=0;
        while(at<input.length){
          const n=Math.min(input.length-at,samples-used);chunk.set(input.subarray(at,at+n),used);used+=n;at+=n;
          if(used===samples){
            if(s.queue.length>=4){void stopListening('Audio backlog: listening stopped; start again');return;}
            s.queue.push(pcm16([chunk],s.context.sampleRate).bytes);chunk=new Float32Array(samples);used=0;void drain();
          }
        }
      };
      s.media.getTracks().forEach(t=>t.addEventListener('ended',()=>{if(active())void stopListening('Microphone disconnected');}));
      status('Listening continuously on this PC. Say bedroom light on or bedroom lights off.');
    }catch(error){if(active())await stopListening('Listening stopped: '+error.message);throw error;}
  }
  if(typeof document!=='undefined')document.addEventListener('change',e=>{if(['targetMode','room','device','token','timerDevice','benchAlias','text','cloudMode'].includes(e.target.id)){clear();if(voice)void stopListening('Selection changed; start listening again');}});
  if(typeof document!=='undefined')document.addEventListener('input',e=>{if(e.target.id==='text')clear();});
  if(typeof globalThis.addEventListener==='function')globalThis.addEventListener('pagehide',()=>{void stopListening('Page closed');});
  return {Mode,rooms,request,clear,prepare,prepareText,confirm,result,selectedMode,pcm16,capture,localSpeech,startListening,stopListening,get listening(){return !!voice;},get capturing(){return capturing;}};
})();
)JS";
