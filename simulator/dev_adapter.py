"""Explicit host-only adapter. Runs portable firmware; never claims MCU inference."""
import json
import subprocess
import threading
from scripts.host_build import build

DEV_TOKEN='clawgrid-dev-only'

class DevAdapter:
    def __init__(self):
        self.process=subprocess.Popen([str(build())],stdin=subprocess.PIPE,stdout=subprocess.PIPE,text=True,bufsize=1)
        self.lock=threading.Lock()
        self.scenario='connected'

    def close(self):
        self.process.terminate()
        self.process.wait(timeout=5)

    def request(self,method,path,body,token):
        if path=='/api/dev/scenario' and method=='GET':
            return 200,dict(scenario=self.scenario,processing_source='HOST_MEASURED',input_source='SIMULATED')
        if path=='/api/speech/audio':
            return (501,{'error':'Host mode has no recognition engine; use explicitly labeled fixtures'}) if token==DEV_TOKEN else (401,{'error':'Host development token required'})
        data=(body or b'').decode('ascii',errors='strict')
        if any(ord(c)<32 or ord(c)>126 for c in data):return 400,{'error':'ASCII body required'}
        if method=='POST' or path in ('/api/transaction','/api/speech'):
            if token!=DEV_TOKEN:return 401,{'error':'Host development token required'}
        commands={'/api/state':'state','/api/simulation':'simstate','/api/simulation/input':'siminput',
                  '/api/proposal':'proposal','/api/confirm':'confirm','/api/control':'control','/api/text':'text',
                  '/api/transaction':'transaction','/api/speech':'speech','/api/dev/scenario':'scenario',
                  '/api/dev/speech':'speechfixture'}
        expected='POST' if path in ('/api/simulation/input','/api/proposal','/api/confirm','/api/control','/api/text','/api/dev/scenario','/api/dev/speech','/api/speech/audio','/api/voice') else 'GET'
        if method!=expected:return 405,{'error':'Method not allowed'}
        if path=='/api/dev/scenario' and data not in ('connected','stale','disconnected','reject','timeout'):return 400,{'error':'Invalid host scenario'}
        if path=='/api/dev/scenario':self.scenario=data
        if path=='/api/speech/audio':return 501,{'error':'Host mode has no recognition engine; use explicitly labeled fixtures'}
        if path=='/api/memory':return 200,{'source':'HOST_MEASURED','internal_free':None,'internal_largest':None,'psram_free':None,'stack_free_bytes':None}
        if path=='/api/voice':
            # Reuse the established preview parser only; this never submits commands.
            import re
            match=re.fullmatch(r'turn (on|off) (bedroom|living room|kitchen|study) (light|fan)',data.strip().lower())
            if not match:return 200,{'requires_clarification':True,'authorized':False}
            return 200,dict(requires_clarification=False,authorized=False,action=match[1].upper(),room=match[2].replace(' ','_'),device=match[3],processing_source='HOST_MEASURED')
        if path not in commands:return 404,{'error':'Unknown host-development endpoint'}
        with self.lock:
            self.process.stdin.write(commands[path]+(' '+data if data else '')+'\n');self.process.stdin.flush()
            line=self.process.stdout.readline()
        if not line:return 503,{'error':'Host firmware adapter stopped'}
        value=json.loads(line);status=value.pop('http_status',200)
        value.update(processing_source='HOST_MEASURED',input_source='SIMULATED',execution_mode='HOST_ONLY',physical_state='not measured')
        return status,value

def banner():
    return '''<aside style="position:sticky;top:0;z-index:20;background:#704512;color:white;padding:12px" role="status">HOST-ONLY SIMULATION — no ESP32, no recognition, no physical measurements. <label>Scenario <select id="devScenario"><option>connected</option><option>stale</option><option>disconnected</option><option>reject</option><option>timeout</option></select></label><label>Speech fixture ID <input id="devSpeech" type="number" min="0" max="16" value="1"></label><button id="injectFixture">Inject labeled fixture</button><span id="devResult"></span></aside>'''

def inject(page):
    notice=banner()
    page=page.replace('Synthetic sensors · ESP32 decisions','Synthetic inputs · portable firmware on host').replace('Measurements from the boards','Host development observations').replace('Measured ESP32 simulation processing time','No MCU timing in host mode')
    page=page.replace('<main>',notice+'<main>',1) if '<main>' in page else notice+page
    script='''<script>document.getElementById('token').value='clawgrid-dev-only';ClawGrid.request('/api/dev/scenario').then(r=>document.getElementById('devScenario').value=r.scenario);document.getElementById('devScenario').onchange=async e=>{ClawGrid.clear();try{await ClawGrid.request('/api/dev/scenario',e.target.value);document.getElementById('devResult').textContent='Host scenario: '+e.target.value;}catch(err){document.getElementById('devResult').textContent=err.message;}};document.getElementById('injectFixture').onclick=async()=>{try{const r=await ClawGrid.request('/api/dev/speech',document.getElementById('devSpeech').value);document.getElementById('devResult').textContent='SIMULATED command fixture '+r.command_id+'; no audio recognition';}catch(err){document.getElementById('devResult').textContent=err.message;}};</script>'''
    return page.replace('</html>',script+'</html>')
