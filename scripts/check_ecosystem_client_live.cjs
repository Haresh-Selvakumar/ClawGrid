// Runs the exact webpage client against Supabase without browser UI or actuator commands.
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
globalThis.crypto=require('node:crypto').webcrypto;
vm.runInThisContext(fs.readFileSync('dashboard/ecosystem-client.js','utf8'));
const config=JSON.parse(fs.readFileSync('supabase.local.json'));
const local=JSON.parse(fs.readFileSync('local_credentials.json'));
const report={source:'HOST_MEASURED',test:'actual webpage client, live Supabase WSS, read only',snapshots:[]};
let finished=false,client;
function finish(error){
 if(finished)return;finished=true;clearTimeout(limit);client?.close();
 report.passed=!error;if(error)report.error=error.message;
 fs.writeFileSync('benchmarks/raw/ecosystem_web_client_live.json',JSON.stringify(report,null,2)+'\n');
 console.log(error?'Web client verification FAIL':'Web client: encrypted Supabase snapshots and honest hardware availability PASS');process.exitCode=error?1:0;
}
const limit=setTimeout(()=>finish(Error('No authenticated gateway snapshots received within 35 seconds')),35000);
client=new ClawRealtime.Client(config,local.api_token,{snapshot:value=>{
 if(!value||finished)return;
 try{
  assert.equal(value.source,'HARDWARE_MEASURED');assert.equal(value.node_a.expected_mac,local.node_a_mac);
  assert.equal(value.node_b.expected_mac,local.node_b_mac);assert.equal(value.sensors.temperature_c,null);
  assert.equal(value.sensors.illuminance_lux,null);
  report.snapshots.push(value);
  if(report.snapshots.length===2){
   assert.ok(report.snapshots[1].sequence>report.snapshots[0].sequence);
   report.hardware_available=value.node_a.known===true&&value.node_a.authenticated===true;
   finish();
  }
 }catch(error){finish(error)}
}});
client.connect().catch(finish);
