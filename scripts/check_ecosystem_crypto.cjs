const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
globalThis.crypto=require('node:crypto').webcrypto;
vm.runInThisContext(fs.readFileSync('dashboard/ecosystem-client.js','utf8'));
(async()=>{
 const f=JSON.parse(fs.readFileSync('ecosystem/android/app/src/test/resources/realtime-vector.json'));
 const cipher=await ClawRealtime.cipher(f.token,f);
 assert.deepEqual(await cipher.decrypt(f.envelope),f.payload);
 const wrong=await ClawRealtime.cipher('wrong-token',f);
 await assert.rejects(()=>wrong.decrypt(f.envelope));
 await assert.rejects(()=>cipher.decrypt({...f.envelope,gateway_id:f.home_id}));
 const altered={...f.envelope,ct:f.envelope.ct.slice(0,-8)+'AAAAAAAA'};
 await assert.rejects(()=>cipher.decrypt(altered));
 const client=new ClawRealtime.Client(f,f.token);client.lastReceived=performance.now();
 for(const age of [-1,3000,null,undefined]){
  client.lastSnapshot={node_a:{known:true,authenticated:true,age_ms:age}};
  await assert.rejects(()=>client.command({target:'virtual_rooms',room:'bedroom',device:'light',action:'ON'}),/Fresh authenticated/);
 }
 const result=await cipher.encrypt(f.payload);
 fs.writeFileSync('benchmarks/review/browser-wire-vector.json',JSON.stringify(result));
 const html=fs.readFileSync('dashboard/ecosystem.html','utf8');
 for(const match of html.matchAll(/<script>([\s\S]*?)<\/script>/g))new vm.Script(match[1]);
 console.log('HOST_MEASURED: browser decrypts Python fixture; wrong key, tamper, scope rejected; HTML scripts parse PASS');
})().catch(error=>{console.error(error);process.exitCode=1});
