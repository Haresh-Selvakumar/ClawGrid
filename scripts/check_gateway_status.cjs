// Exercise the actual shipped landing-page JavaScript with controlled time.
const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm'),assert=require('node:assert/strict');
const html=fs.readFileSync(path.resolve(__dirname,'../firmware/node_intelligence/src/hardware_status.h'),'utf8').split('R"HTML(')[1].split(')HTML"')[0];
const source=html.match(/<script>([\s\S]*?)<\/script>/)[1];
assert.ok(!html.includes('Math.random'));assert.ok(!html.includes('/api/confirm'));assert.ok(!html.includes('/api/control'));
function page(){
 let now=0;const elements=new Map();
 const context=vm.createContext({AbortController,performance:{now:()=>now},setTimeout:()=>1,clearTimeout(){},setInterval(){},
  fetch:()=>new Promise(()=>{}),document:{getElementById(id){if(!elements.has(id))elements.set(id,{textContent:''});return elements.get(id);}}});
 vm.runInContext(source,context);
 context.ClawGridStatus.render();
 return {api:context.ClawGridStatus,text:id=>elements.get(id)?.textContent,time:n=>{now=n;context.ClawGridStatus.render();}};
}
const report={room:'bedroom',known:true,authenticated:true,boot_id:9,sample_uptime_ms:1000,last_command:3,age_ms:100,light:true,fan:false,physical_outputs_enabled:false,occupancy:'UNKNOWN'};
let p=page();assert.equal(p.text('light'),'UNKNOWN'); // Initial data stays unavailable.
assert.equal(p.api.acceptState({...report}),true);assert.equal(p.text('light'),'ON');assert.equal(p.text('outputs'),'INHIBITED');
p.time(2900);assert.equal(p.text('light'),'UNKNOWN'); // Exactly 3000 ms is stale.
assert.equal(p.api.acceptState({...report}),true);assert.equal(p.text('light'),'UNKNOWN'); // Duplicate cannot refresh age.
assert.equal(p.api.acceptState({...report,sample_uptime_ms:2000,light:false}),true);assert.equal(p.text('light'),'OFF');
assert.equal(p.api.acceptState({...report,sample_uptime_ms:1500}),false);assert.equal(p.text('light'),'UNKNOWN'); // Old sample.
p=page();p.api.acceptState({...report});assert.equal(p.api.acceptState({...report,light:false}),false);assert.equal(p.text('light'),'UNKNOWN'); // Changed duplicate.
p=page();p.api.acceptState({...report});p.api.acceptState({known:false});p.time(4000);p.api.acceptState({...report});assert.equal(p.text('light'),'UNKNOWN'); // Unknown/reconnect cannot reset duplicate age.
p=page();p.api.acceptState({...report});p.api.acceptState({...report,boot_id:10,sample_uptime_ms:0,light:false});assert.equal(p.text('light'),'OFF');
assert.equal(p.api.acceptState({...report,boot_id:9,sample_uptime_ms:3000}),false);assert.equal(p.text('light'),'UNKNOWN'); // Old boot.
p=page();assert.equal(p.api.acceptState({...report,processing_source:'SIMULATED'}),false);assert.equal(p.text('light'),'UNKNOWN');
p=page();assert.equal(p.api.acceptState({...report,light:'true'}),false);assert.equal(p.text('light'),'UNKNOWN');
console.log('Gateway status checks passed: unavailable, fresh/inhibited, expiry, duplicate aging, advancing report, regression, changed duplicate, reconnect, reboot, simulation and invalid types.');
