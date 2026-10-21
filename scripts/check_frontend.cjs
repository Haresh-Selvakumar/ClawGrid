// Dependency-free syntax, PCM, and proposal-binding checks against shipped code.
const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm'),assert=require('node:assert/strict');
const root=path.resolve(__dirname,'..','firmware','node_intelligence','src');
for(const name of ['dashboard.h','simulation_dashboard.h']){
 const html=fs.readFileSync(path.join(root,name),'utf8').split('R"HTML(')[1].split(')HTML"')[0];
 for(const match of html.matchAll(/<script(?:\s[^>]*)?>([\s\S]*?)<\/script>/g))new vm.Script(match[1],{filename:name});
}
const source=fs.readFileSync(path.join(root,'dashboard_client.h'),'utf8').split('R"JS(')[1].split(')JS"')[0];
new vm.Script(source,{filename:'dashboard_client.h'});
const calls=[],elements=new Map([['targetMode',{value:'physical_bedroom'}]]);
const context=vm.createContext({console,Uint8Array,Float32Array,DataView,setTimeout,clearTimeout,document:{addEventListener(){},getElementById(id){if(!elements.has(id))elements.set(id,{value:id==='token'?'test-token':'',disabled:false});return elements.get(id);}},fetch:async(p,o)=>{calls.push([p,o.body]);return {ok:true,json:async()=>({proposal_id:41,id:7})};}});
vm.runInContext(source,context);const client=context.ClawGrid;
const converted=client.pcm16([new Float32Array(48000).fill(.5)],48000);
assert.equal(converted.samples,16000);assert.equal(new DataView(converted.bytes.buffer).getInt16(0,true),16384);
assert.equal(client.pcm16([new Float32Array(44100)],44100).samples,16000);
assert.equal(client.pcm16([Float32Array.of(-1,1)],16000).bytes.join(','),'0,128,255,127');
assert.equal(client.pcm16([new Float32Array(10).fill(1),new Float32Array(10).fill(-1)],16000).bytes.some(x=>x!==0),false);
assert.equal(client.pcm16([new Float32Array(80000)],16000).samples,64000);
assert.equal(client.pcm16([new Float32Array(80000)],16000).truncated,true);
assert.throws(()=>client.pcm16([Float32Array.of(NaN)],16000));
assert.throws(()=>client.pcm16([new Float32Array(4)],0));
(async()=>{
 const physical={target:'physical_bedroom',room:'bedroom',device:'light',action:'ON'};
 await assert.rejects(client.confirm(physical));
 await client.prepare(physical);
 await assert.rejects(client.confirm({...physical,target:'virtual_rooms'}));
 elements.set('targetMode',{value:'virtual_rooms'});
 await assert.rejects(client.confirm(physical));
 elements.get('targetMode').value='physical_bedroom';
 await client.confirm(physical);assert.equal(calls.at(-1)[1],'41 physical_bedroom bedroom light ON');
 await assert.rejects(client.confirm(physical));
 await assert.rejects(client.prepare({...physical,room:'kitchen'}));
 console.log('Frontend syntax, 16/44.1/48 kHz PCM, stereo, clipping, caps and confirmation checks passed');
})().catch(error=>{console.error(error);process.exitCode=1;});
