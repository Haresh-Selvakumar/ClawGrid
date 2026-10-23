"""Exercise the actual portable firmware through the explicit host API adapter."""
import pytest
from simulator.dev_adapter import DevAdapter,DEV_TOKEN

@pytest.fixture
def dev():
    adapter=DevAdapter()
    yield adapter
    adapter.close()

def call(dev,path,body=None,token=DEV_TOKEN):
    return dev.request('GET' if body is None else 'POST',path,None if body is None else body.encode(),token)

def prepared(dev,target='physical_bedroom',room='bedroom',device='light',action='ON',source='manual',job=0):
    status,value=call(dev,'/api/proposal',f'{source} {target} {room} {device} {action} {job}')
    assert status==200,value
    return value['proposal_id'],f'{target} {room} {device} {action}'

@pytest.mark.parametrize('device',['light','fan'])
@pytest.mark.parametrize('action',['ON','OFF'])
def test_physical_bedroom_reaches_actual_kernel(dev,device,action):
    if action=='OFF':
        first,initial_target=prepared(dev,device=device,action='ON')
        assert call(dev,'/api/confirm',f'{first} {initial_target}')[0]==202
        # Deterministic host clock fixture crosses the real kernel dwell rule.
        # This command is deliberately not exposed as a dashboard/API endpoint.
        with dev.lock:
            dev.process.stdin.write('advance 4000\n');dev.process.stdin.flush();dev.process.stdout.readline()
    proposal,target=prepared(dev,device=device,action=action)
    before=call(dev,'/api/state')[1]
    assert before[device] is (action=='OFF')  # Preparing never actuates.
    status,result=call(dev,'/api/confirm',f'{proposal} {target}')
    assert status==202
    state=call(dev,'/api/state')[1]
    assert state[device] is (action=='ON')
    assert state['physical_outputs_enabled'] is False
    assert state['processing_source']=='HOST_MEASURED'
    assert call(dev,'/api/confirm',f'{proposal} {target}')[0]==409

def test_virtual_bedroom_never_drives_physical_slots(dev):
    proposal,target=prepared(dev,target='virtual_rooms')
    assert call(dev,'/api/confirm',f'{proposal} {target}')[0]==202
    assert call(dev,'/api/state')[1]['light'] is False
    assert call(dev,'/api/simulation')[1]['outputs'] & 1

@pytest.mark.parametrize('room',['living_room','kitchen','study'])
def test_other_rooms_remain_virtual(dev,room):
    assert call(dev,'/api/proposal',f'manual physical_bedroom {room} light ON 0')[0]==400
    proposal,target=prepared(dev,target='virtual_rooms',room=room)
    assert call(dev,'/api/confirm',f'{proposal} {target}')[0]==202
    state=call(dev,'/api/state')[1]
    assert state['light'] is False and state['fan'] is False

@pytest.mark.parametrize('body',['manual physical_bedroom bedroom heater ON 0','manual physical_bedroom bedroom light DIM 0','manual physical_bedroom roof light ON 0','manual invalid bedroom light ON 0','manual physical_bedroom bedroom light ON -1','manual physical_bedroom bedroom light ON 0 extra'])
def test_invalid_target(dev,body):assert call(dev,'/api/proposal',body)[0]==400

def test_confirmation_auth_target_binding_and_missing_proposal(dev):
    assert call(dev,'/api/confirm','1 physical_bedroom bedroom light ON')[0]==409
    proposal,target=prepared(dev)
    assert call(dev,'/api/confirm',f'{proposal} {target}',token='wrong')[0]==401
    assert call(dev,'/api/confirm',f'{proposal} virtual_rooms bedroom light ON')[0]==409
    assert call(dev,'/api/confirm',f'{proposal+1} {target}')[0]==409
    assert call(dev,'/api/state')[1]['light'] is False

@pytest.mark.parametrize('scenario',['stale','disconnected'])
def test_unavailable_controller(dev,scenario):
    proposal,target=prepared(dev)
    call(dev,'/api/dev/scenario',scenario)
    assert call(dev,'/api/confirm',f'{proposal} {target}')[0]==409
    assert call(dev,'/api/proposal','manual physical_bedroom bedroom light ON 0')[0]==409
    assert call(dev,'/api/state')[1]['known'] is False

def test_speech_fixture_requires_matching_id_job_and_confirmation(dev):
    job=call(dev,'/api/dev/speech','3')[1]['job']
    assert call(dev,'/api/proposal',f'speech physical_bedroom bedroom light ON {job}')[0]==409
    proposal,target=prepared(dev,device='fan',source='speech',job=job)
    assert call(dev,'/api/state')[1]['fan'] is False
    assert call(dev,'/api/confirm',f'{proposal} {target}')[0]==202
    assert call(dev,'/api/state')[1]['fan'] is True
    assert call(dev,'/api/speech')[1]['supported'] is False  # Never claim host inference.

@pytest.mark.parametrize('scenario,phase,status',[('reject',2,1),('timeout',3,2)])
def test_fault_scenarios_do_not_actuate(dev,scenario,phase,status):
    call(dev,'/api/dev/scenario',scenario)
    proposal,target=prepared(dev)
    assert call(dev,'/api/confirm',f'{proposal} {target}')[0]==202
    result=call(dev,'/api/transaction')[1]
    assert result['phase']==phase
    if scenario=='reject':assert result['status']==status
    assert call(dev,'/api/state')[1]['light'] is False

def test_scenario_survives_dashboard_reload(dev):
    call(dev,'/api/dev/scenario','stale')
    assert call(dev,'/api/dev/scenario')[1]['scenario']=='stale'
    assert call(dev,'/api/state')[1]['known'] is False
