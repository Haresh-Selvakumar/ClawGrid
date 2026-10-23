"""Exercise the submitted sketch's text aliases through real portable firmware."""
import pytest
from simulator.dev_adapter import DevAdapter, DEV_TOKEN


@pytest.fixture
def dev():
    adapter = DevAdapter()
    yield adapter
    adapter.close()


def call(dev, path, body=None, token=DEV_TOKEN):
    return dev.request('GET' if body is None else 'POST', path,
                       None if body is None else body.encode('ascii'), token)


@pytest.mark.parametrize('text,device,action,fallback', [
    ('turn on bedroom light', 'light', 'ON', False),
    ('PLEASE SWITCH THE BED ROOM LAMP OFF', 'light', 'OFF', False),
    ('bedroom bulb on', 'light', 'ON', False),
    ('bedroom fan off', 'fan', 'OFF', False),
    ('bedroom pan on', 'fan', 'ON', True),
    ('bedroom fun of', 'fan', 'OFF', True),
    ('bedroom fan onn', 'fan', 'ON', True),
])
def test_text_is_only_a_proposal_until_confirmed(dev, text, device, action, fallback):
    status, p = call(dev, '/api/text', 'physical_bedroom ' + text)
    assert status == 200
    assert (p['room'], p['device'], p['action']) == ('bedroom', device, action)
    assert p['acoustic_fallback'] is fallback
    assert p['authorized'] is False
    assert call(dev, '/api/state')[1]['light'] is False
    assert call(dev, '/api/state')[1]['fan'] is False
    body = f"{p['proposal_id']} {p['target']} {p['room']} {device} {action}"
    assert call(dev, '/api/confirm', body)[0] == 202
    assert call(dev, '/api/state')[1][device] is (action == 'ON')
    assert call(dev, '/api/confirm', body)[0] == 409


@pytest.mark.parametrize('room', ['hall', 'living', 'living room', 'all'])
def test_hall_is_virtual_unless_explicit_shared_bench_target(dev, room):
    assert call(dev, '/api/text', f'physical_bedroom {room} fan on')[0] == 400
    status, virtual = call(dev, '/api/text', f'virtual_rooms {room} fan on')
    assert status == 200
    assert (virtual['target'], virtual['room'], virtual['bench_alias']) == ('virtual_rooms', 'living_room', False)
    status, bench = call(dev, '/api/text', f'bench_pair {room} fan on')
    assert status == 200
    assert (bench['target'], bench['room'], bench['bench_alias']) == ('physical_bedroom', 'bedroom', True)
    body = f"{bench['proposal_id']} virtual_rooms living_room fan ON"
    assert call(dev, '/api/confirm', body)[0] == 409
    body = f"{bench['proposal_id']} physical_bedroom bedroom fan ON"
    assert call(dev, '/api/confirm', body)[0] == 202
    assert call(dev, '/api/state')[1]['fan'] is True


@pytest.mark.parametrize('text', [
    '', 'hall light only', 'small light on', 'hall pantry on',
    'bedroom fan on off', 'bedroom light fan on', 'bedroom hall light on',
    'bedroom bedroom light on', 'do not turn bedroom light on',
    "don't turn bedroom light on", 'never turn bedroom light on',
    'bedroom fan on yesterday', 'bedroom lights on and fan off',
    'bedroom\x00light on', 'bedroom\nlight on', 'bedroom light on ' + 'x' * 128,
])
def test_ambiguous_negative_binary_and_substring_commands_are_rejected(dev, text):
    assert call(dev, '/api/text', 'bench_pair ' + text)[0] == 400
    assert call(dev, '/api/state')[1]['light'] is False
    assert call(dev, '/api/state')[1]['fan'] is False


def test_text_auth_expiry_and_stale_controller(dev):
    body = 'physical_bedroom bedroom light on'
    assert call(dev, '/api/text', body, token='wrong')[0] == 401
    p = call(dev, '/api/text', body)[1]
    with dev.lock:
        dev.process.stdin.write('advance 15000\n')
        dev.process.stdin.flush()
        dev.process.stdout.readline()
    confirm = f"{p['proposal_id']} physical_bedroom bedroom light ON"
    assert call(dev, '/api/confirm', confirm)[0] == 409
    assert call(dev, '/api/dev/scenario', 'stale')[0] == 200
    assert call(dev, '/api/text', body)[0] == 409


def test_bad_text_invalidates_previous_proposal(dev):
    p = call(dev, '/api/text', 'physical_bedroom bedroom light on')[1]
    assert call(dev, '/api/text', 'physical_bedroom bedroom light on off')[0] == 400
    assert call(dev, '/api/confirm', f"{p['proposal_id']} physical_bedroom bedroom light ON")[0] == 409
