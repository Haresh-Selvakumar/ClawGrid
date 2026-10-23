// HOST-only: verify cloud routing and failure behavior without a microphone/ESP.
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const source=fs.readFileSync(require('node:path').join(__dirname,'../firmware/node_intelligence/src/dashboard_client.h'),'utf8').split('R"JS(')[1].split(')JS"')[0];
async function check(fail){
 const calls=[],elements=new Map([['targetMode',{value:'virtual_rooms'}],['token',{value:'HOST_TEST_TOKEN'}],['cloudMode',{checked:true}]]);
 const target={target:'virtual_rooms',room:'bedroom',device:'light',action:'ON'};
 const context=vm.createContext({console,crypto:{randomUUID:()=> 'c75ed365-6ca4-481d-94b7-3e7038e39fb4'},setTimeout:fn=>{fn();return 1;},clearTimeout,
  document:{addEventListener(){},getElementById:id=>elements.get(id)},
  fetch:async(path,options)=>{calls.push([path,options.body]);
   if(path==='/api/proposal')return {ok:true,json:async()=>({...target,proposal_id:7})};
   if(path==='/local/cloud/commands')return {ok:!fail,json:async()=>fail?{error:'Supabase offline; not queued'}:{cloud_id:'c75ed365-6ca4-481d-94b7-3e7038e39fb4',queued:true,authorized:false}};
   if(path==='/local/cloud/result')return {ok:true,json:async()=>({phase:'acknowledged',result:{id:8,phase:2,status:0}})};
   throw Error('Unexpected request '+path);
  }});
 vm.runInContext(source,context);const client=context.ClawGrid;
 await client.prepare(target);
 if(fail){await assert.rejects(client.confirm(target),/Supabase offline/);}
 else{const queued=await client.confirm(target);const messages=[];const ack=await client.result(queued.id,m=>messages.push(m));assert.equal(ack.status,0);assert.ok(messages.some(m=>m.includes('accepted through Supabase')));}
 assert.ok(!calls.some(([path])=>path==='/api/confirm'));
 const submitted=JSON.parse(calls.find(([path])=>path==='/local/cloud/commands')[1]);
 for(const key of Object.keys(target))assert.equal(submitted[key],target[key]);
 assert.equal(submitted.id,'c75ed365-6ca4-481d-94b7-3e7038e39fb4');
 assert.ok(!calls.some(([,body])=>body&&body.includes('service_role')));
}
(async()=>{await check(false);await check(true);console.log('Cloud frontend: immutable target, cloud ACK observation, no direct-confirm fallback and no service key passed.');})().catch(error=>{console.error(error);process.exitCode=1;});
