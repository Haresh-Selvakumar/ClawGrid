'use strict';
globalThis.ClawRealtime = (() => {
  const encode = bytes => btoa(String.fromCharCode(...new Uint8Array(bytes)));
  const decode = text => Uint8Array.from(atob(text), c => c.charCodeAt(0));
  async function cipher(token, config) {
    const utf = new TextEncoder();
    const digest = await crypto.subtle.digest('SHA-256', utf.encode('ClawGridRealtimeV1:' + token));
    const key = await crypto.subtle.importKey('raw', digest, 'AES-GCM', false, ['encrypt', 'decrypt']);
    const aad = utf.encode(`ClawGridRealtimeV1|${config.home_id}|${config.gateway_id}`);
    return {
      async encrypt(value) {
        const iv = crypto.getRandomValues(new Uint8Array(12));
        const ct = await crypto.subtle.encrypt({name:'AES-GCM', iv, additionalData:aad}, key, utf.encode(JSON.stringify(value)));
        return {v:1,home_id:config.home_id,gateway_id:config.gateway_id,iv:encode(iv),ct:encode(ct)};
      },
      async decrypt(envelope) {
        if(envelope.v!==1||envelope.home_id!==config.home_id||envelope.gateway_id!==config.gateway_id)throw Error('Wrong realtime scope');
        const iv=decode(envelope.iv),ct=decode(envelope.ct);
        if(iv.length!==12||ct.length>32784)throw Error('Invalid encrypted frame');
        const raw=await crypto.subtle.decrypt({name:'AES-GCM',iv,additionalData:aad},key,ct);
        return JSON.parse(new TextDecoder().decode(raw));
      }
    };
  }
  class Client {
    constructor(config,token,{snapshot,status,ack}={}) {
      this.config=config;this.token=token;this.onSnapshot=snapshot||(()=>{});this.onStatus=status||(()=>{});this.onAck=ack||(()=>{});
      this.pending=new Map();this.seq=-1;this.session=null;this.lastReceived=0;this.lastSent=0;this.lastSnapshot=null;this.closed=false;this.ref=1;
    }
    async connect(){
      if(this.token.length<16)throw Error('Enter your existing ClawGrid control token');
      this.crypto=await cipher(this.token,this.config);this.open();
      this.watch=setInterval(()=>{if(this.lastReceived&&performance.now()-this.lastReceived>4000){this.lastReceived=0;this.onSnapshot(null);this.onStatus('STALE: no fresh gateway snapshot');}},500);
    }
    open(){
      if(this.closed)return;
      const url=this.config.url.replace('https://','wss://')+'/realtime/v1/websocket?apikey='+encodeURIComponent(this.config.anon_key)+'&vsn=1.0.0';
      const ws=new WebSocket(url);this.ws=ws;
      ws.onopen=()=>{
        ws.send(JSON.stringify({topic:'realtime:clawgrid_live',event:'phx_join',ref:'join',payload:{config:{broadcast:{ack:false,self:false},presence:{enabled:false},postgres_changes:[],private:false},access_token:this.config.anon_key}}));
        this.heartbeat=setInterval(()=>{if(ws.readyState===WebSocket.OPEN)ws.send(JSON.stringify({topic:'phoenix',event:'heartbeat',payload:{},ref:String(++this.ref)}));},20000);
      };
      ws.onmessage=async event=>{
        try{
          const frame=JSON.parse(event.data);
          if(frame.event==='phx_reply'&&frame.ref==='join'){
            if(frame.payload.status!=='ok')throw Error('Supabase channel rejected');
            this.onStatus('Supabase connected; waiting for authenticated ESP snapshot');return;
          }
          if(frame.event!=='broadcast'||frame.payload?.event!=='clawgrid_secure_v1')return;
          const value=await this.crypto.decrypt(frame.payload.payload);
          if(!Number.isFinite(value.sent_at_ms)||Date.now()-value.sent_at_ms>5000||value.sent_at_ms-Date.now()>2000)return;
          if(value.kind==='snapshot'){
            if(value.sent_at_ms<=this.lastSent)return;
            if(value.source!=='HARDWARE_MEASURED'||value.home_id!==this.config.home_id||value.gateway_id!==this.config.gateway_id)return;
            if(value.session_id===this.session&&value.sequence<=this.seq)return;
            this.seq=value.sequence;this.session=value.session_id;this.lastSent=value.sent_at_ms;this.lastReceived=performance.now();this.lastSnapshot=value;this.onSnapshot(value);
            this.onStatus(value.node_a.known?'Live ESP snapshots through Supabase':'Supabase connected; remote A unavailable');
          }else if(value.kind==='command_ack'){
            const pending=this.pending.get(value.cloud_id);
            if(pending){clearTimeout(pending.timer);this.pending.delete(value.cloud_id);this.onAck(value);pending.resolve(value.result);}
          }
        }catch(_){/* Invalid/wrong-token data has no authority and is never rendered. */}
      };
      ws.onerror=()=>this.onStatus('Supabase connection error; no command replay');
      ws.onclose=()=>{
        clearInterval(this.heartbeat);this.onSnapshot(null);this.onStatus('Disconnected; reconnecting without replay');
        for(const entry of this.pending.values()){clearTimeout(entry.timer);entry.reject(Error('Disconnected; command outcome unknown'));}this.pending.clear();
        if(!this.closed)this.reconnect=setTimeout(()=>this.open(),2000);
      };
    }
    async command(target){
      if(!['physical_bedroom','virtual_rooms'].includes(target.target)||!['bedroom','living_room','kitchen','study'].includes(target.room)||!['light','fan'].includes(target.device)||!['ON','OFF'].includes(target.action))throw Error('Invalid target');
      if(target.target==='physical_bedroom'&&target.room!=='bedroom')throw Error('Only bedroom has physical relays');
      const a=this.lastSnapshot?.node_a;
      if(!this.lastReceived||performance.now()-this.lastReceived>4000||!a?.known||!a.authenticated||!Number.isFinite(a.age_ms)||a.age_ms<0||a.age_ms>=3000)throw Error('Fresh authenticated A data is required');
      if(this.ws?.readyState!==WebSocket.OPEN)throw Error('Supabase disconnected');
      const id=crypto.randomUUID(),issued=Date.now()/1000;
      const envelope=await this.crypto.encrypt({kind:'command',id,...target,issued_at:issued,expires_at:issued+10});
      return new Promise((resolve,reject)=>{
        const timer=setTimeout(()=>{this.pending.delete(id);reject(Error('ACK not observed; outcome unknown. Do not automatically repeat.'));},16000);
        this.pending.set(id,{resolve,reject,timer});
        this.ws.send(JSON.stringify({topic:'realtime:clawgrid_live',event:'broadcast',ref:String(++this.ref),payload:{type:'broadcast',event:'clawgrid_secure_v1',payload:envelope}}));
      });
    }
    close(){this.closed=true;clearInterval(this.watch);clearInterval(this.heartbeat);clearTimeout(this.reconnect);this.ws?.close();this.token='';}
  }
  return {Client,cipher};
})();
