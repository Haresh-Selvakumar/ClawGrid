// No microphone/network: exercise the shipped continuous-listening controller.
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const source=fs.readFileSync(require('node:path').join(__dirname,'../firmware/node_intelligence/src/dashboard_client.h'),'utf8').split('R"JS(')[1].split(')JS"')[0];
const delay=ms=>new Promise(resolve=>setTimeout(resolve,ms));
async function until(test){for(let i=0;i<100&&!test();i++)await delay(10);assert.ok(test());}
function setup({known=true,holdProposal=false,holdMicrophone=false,stallAudio=false}={}){
 const calls=[],events={},messages=[],elements=new Map([['targetMode',{value:'physical_bedroom'}],['token',{value:'test'}],['benchAlias',{checked:false}]]);
 let processor,stopped=0,microphones=0,releaseProposal,releaseMicrophone;
 const track={stop(){stopped++;},addEventListener(){}};
 const media={getTracks:()=>[track]};
 class Context{
  constructor(){this.sampleRate=16000;this.state='running';}
  async resume(){} async close(){this.state='closed';}
  createMediaStreamSource(){return {connect(){},disconnect(){}};}
  createScriptProcessor(){return processor={connect(){},disconnect(){}};}
  createGain(){return {gain:{value:1},connect(){},disconnect(){}};}
 }
 const context=vm.createContext({console,Uint8Array,Float32Array,DataView,AbortController,performance,setTimeout,clearTimeout,isSecureContext:true,AudioContext:Context,
  navigator:{mediaDevices:{getUserMedia:async()=>{microphones++;if(holdMicrophone)return new Promise(r=>releaseMicrophone=()=>r(media));return media;}}},
  document:{addEventListener(type,fn){events[type]=fn;},getElementById(id){if(!elements.has(id))elements.set(id,{disabled:false});return elements.get(id);}},
  fetch:async(path,options)=>{
   calls.push([path,options]);let value={};
   if(path==='/api/state')value={known};
   if(path==='/local/speech/start')value={session:'session1'};
   if(path==='/local/speech/audio'){
    if(stallAudio)return new Promise((_,reject)=>options.signal.addEventListener('abort',()=>reject(Error('aborted'))));
    value={final:true,final_id:1,text:'bedroom lights off'};
   }
   if(path==='/api/text'){
    value={proposal_id:12,target:'physical_bedroom',room:'bedroom',device:'light',action:'OFF'};
    if(holdProposal)await new Promise(r=>releaseProposal=r);
   }
   if(path==='/api/confirm')value={id:9};
   if(path==='/api/transaction')value={id:9,phase:2,status:0};
   return {ok:true,json:async()=>value};
  }});
 vm.runInContext(source,context);
 return {client:context.ClawGrid,calls,events,messages,elements,
  async start(){return context.ClawGrid.startListening(m=>messages.push(m));},
  feed(count=4){for(let i=0;i<count;i++)processor.onaudioprocess({inputBuffer:{getChannelData:()=>new Float32Array(1024)}});},
  get stopped(){return stopped;},get microphones(){return microphones;},
  releaseProposal(){releaseProposal();},releaseMicrophone(){releaseMicrophone();}};
}
(async()=>{
 let s=setup();await s.start();assert.ok(s.client.listening);s.feed();
 await until(()=>s.calls.some(([p])=>p==='/api/transaction'));
 assert.equal(s.calls.find(([p])=>p==='/api/text')[1].body,'physical_bedroom bedroom lights off');
 assert.equal(s.calls.find(([p])=>p==='/api/confirm')[1].body,'12 physical_bedroom bedroom light OFF');
 s.feed(8);await delay(30);assert.equal(s.calls.filter(([p])=>p==='/api/confirm').length,1);
 await s.client.stopListening();assert.equal(s.stopped,1);assert.equal(s.client.listening,false);
 const count=s.calls.length;s.feed();await delay(10);assert.equal(s.calls.length,count);
 s=setup({holdProposal:true});await s.start();s.feed();await until(()=>s.calls.some(([p])=>p==='/api/text'));
 await s.client.stopListening();s.releaseProposal();await delay(20);assert.ok(!s.calls.some(([p])=>p==='/api/confirm'));
 s=setup({known:false});await assert.rejects(s.start());assert.equal(s.microphones,0);assert.equal(s.client.listening,false);
 s=setup({holdMicrophone:true});const starting=s.start();await until(()=>s.microphones===1);await s.client.stopListening();s.releaseMicrophone();await starting;assert.equal(s.stopped,1);assert.ok(!s.calls.some(([p])=>p==='/local/speech/start'));
 s=setup();await s.start();s.elements.get('targetMode').value='virtual_rooms';s.events.change({target:{id:'targetMode'}});await until(()=>!s.client.listening);assert.equal(s.stopped,1);
 s=setup({stallAudio:true});await s.start();s.feed(30);await until(()=>!s.client.listening);assert.equal(s.stopped,1);assert.ok(!s.calls.some(([p])=>p==='/api/confirm'));
 console.log('Continuous voice: automatic exact-target confirmation, duplicate suppression, Stop/cancellation, target changes, missing A and bounded backlog passed.');
})().catch(error=>{console.error(error);process.exitCode=1;});
