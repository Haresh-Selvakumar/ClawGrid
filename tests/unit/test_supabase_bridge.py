import copy
import json
import time
import uuid

import pytest

from scripts.supabase_bridge import (CloudBridge, CloudError, Journal, signed_command,
                                     telemetry, verify_command)

HOME = '1df3773e-56b5-46ed-9c72-a5e216555aad'
GATEWAY = 'bd39e05e-c162-4c9d-b240-cd78e7852b09'
TOKEN = 'HOST_TEST_ONLY_SECRET'


def command(now=100):
    value = dict(id=str(uuid.uuid4()), target='virtual_rooms', room='bedroom', device='light', action='ON')
    return signed_command(value, HOME, GATEWAY, TOKEN, now)


def test_signed_command_binds_all_fields():
    envelope = command()
    target = verify_command(envelope, envelope['command']['id'], HOME, GATEWAY, TOKEN, 101, 99)
    assert target == dict(target='virtual_rooms', room='bedroom', device='light', action='ON')
    for field, value in [('action', 'OFF'), ('target', 'physical_bedroom'), ('expires_at', 120), ('room', 'study')]:
        altered = copy.deepcopy(envelope)
        altered['command'][field] = value
        with pytest.raises(ValueError, match='signature'):
            verify_command(altered, altered['command']['id'], HOME, GATEWAY, TOKEN, 101, 99)


@pytest.mark.parametrize('now,since', [(110, 99), (120, 99), (98, 90), (101, 102)])
def test_stale_future_and_reconnect_backlog_rejected(now, since):
    envelope = command()
    with pytest.raises(ValueError):
        verify_command(envelope, envelope['command']['id'], HOME, GATEWAY, TOKEN, now, since)


def test_cross_home_gateway_and_wrong_id_rejected():
    envelope = command()
    identity = envelope['command']['id']
    for home, gateway, given in [(str(uuid.uuid4()), GATEWAY, identity),
                                  (HOME, str(uuid.uuid4()), identity),
                                  (HOME, GATEWAY, str(uuid.uuid4()))]:
        with pytest.raises(ValueError):
            verify_command(envelope, given, home, gateway, TOKEN, 101, 99)


@pytest.mark.parametrize('changes', [dict(target='physical_bedroom', room='study'), dict(action='TOGGLE'),
                                     dict(device='heater'), dict(room='hall')])
def test_no_unsupported_target_or_toggle(changes):
    value = dict(id=str(uuid.uuid4()), target='virtual_rooms', room='bedroom', device='light', action='ON')
    with pytest.raises(ValueError):
        signed_command(dict(value, **changes), HOME, GATEWAY, TOKEN, 100)


def test_real_telemetry_never_fabricates_unavailable_sensors():
    state = dict(known=True, authenticated=True, age_ms=400, occupancy='UNKNOWN', light=False, fan=True,
                 motion=False, physical_outputs_enabled=False)
    row = telemetry(state, HOME, 1000)
    assert row['source'] == row['processing_source'] == 'HARDWARE_MEASURED'
    assert row['temperature_c'] is None and row['illuminance_lux'] is None
    assert row['outputs_enabled'] is False and row['known'] is True
    stale = telemetry(dict(state, age_ms=3000), HOME, 1000)
    assert stale['known'] is False and stale['light'] is None and stale['fan'] is None
    assert telemetry(dict(state, authenticated=False), HOME, 1000)['known'] is False


def test_journal_survives_restart_without_replaying(tmp_path):
    path = tmp_path / 'commands.sqlite3'
    journal = Journal(path)
    identity = str(uuid.uuid4())
    assert journal.claim(identity)
    recovered = Journal(path)
    assert not recovered.claim(identity)
    assert recovered.result(identity)['phase'] == 'unknown'


def bridge(tmp_path, *, mismatch=False, lost_ack=False):
    config = dict(enabled=True, home_id=HOME, gateway_id=GATEWAY,
                  url='https://test-project.supabase.co', service_role_key='HOST_ONLY_KEY')
    path = tmp_path / 'supabase.local.json'
    path.write_text(json.dumps(config))
    calls, events = [], []

    def board(path, body=None, deadline=None):
        calls.append((path, body))
        if path == '/api/state':
            return dict(known=True, authenticated=True, age_ms=100)
        if path == '/api/proposal':
            return dict(proposal_id=20, target='virtual_rooms', room='study' if mismatch else 'bedroom', device='light', action='ON')
        if path == '/api/confirm':
            return dict(id=21)
        if lost_ack:
            raise CloudError('HOST fixture: ACK unavailable')
        return dict(id=21, phase=2, status=0, outcome_known=True)

    worker = CloudBridge(path, TOKEN, board)
    worker.online_since = time.time() - 2
    worker.event = lambda *args, **kwargs: events.append((args, kwargs))
    return worker, calls, events


def test_exact_proposal_confirm_ack_once(tmp_path):
    worker, calls, events = bridge(tmp_path)
    envelope = command(time.time())
    identity = envelope['command']['id']
    worker.execute(envelope, identity)
    worker.execute(envelope, identity)
    assert sum(path == '/api/confirm' for path, _ in calls) == 1
    assert ('/api/confirm', '20 virtual_rooms bedroom light ON') in calls
    assert worker.journal.result(identity)['result']['status'] == 0
    assert events[0][1]['hardware'] is True


@pytest.mark.parametrize('mismatch,lost_ack', [(True, False), (False, True)])
def test_mismatch_or_lost_ack_never_resubmits(tmp_path, mismatch, lost_ack):
    worker, calls, _ = bridge(tmp_path, mismatch=mismatch, lost_ack=lost_ack)
    envelope = command(time.time())
    identity = envelope['command']['id']
    worker.execute(envelope, identity)
    worker.execute(envelope, identity)
    assert sum(path == '/api/confirm' for path, _ in calls) == (0 if mismatch else 1)
    result = worker.journal.result(identity)
    assert result['phase'] == 'unknown'
    assert result['result']['outcome_known'] is False


def test_offline_command_is_not_queued(tmp_path):
    worker, calls, events = bridge(tmp_path)
    worker.online_since = None
    with pytest.raises(CloudError, match='not queued'):
        worker.enqueue(dict(command()['command']))
    assert calls == events == []


def test_realtime_executes_after_storage_without_waiting_for_history_poll(tmp_path):
    worker, calls, events = bridge(tmp_path)
    worker.last_poll = time.monotonic()
    original = worker.event
    def stored_first(*args, **kwargs):
        if args[0] == 'CLOUD_COMMAND_REQUESTED':
            assert calls == []
        original(*args, **kwargs)
    worker.event = stored_first
    value = command()['command']
    receipt = worker.enqueue(value, immediate=True)
    assert events[0][0][0] == 'CLOUD_COMMAND_REQUESTED'
    assert worker.journal.result(receipt['cloud_id'])['result']['phase'] == 2
    assert sum(path == '/api/confirm' for path, _ in calls) == 1


def test_failed_receipt_upload_retries_only_audit_not_command(tmp_path):
    worker,calls,events=bridge(tmp_path)
    stored_event=worker.event
    def unavailable(kind,*args,**kwargs):
        if kind=='CLOUD_COMMAND_RESULT': raise CloudError('HOST fixture: cloud receipt upload lost')
        stored_event(kind,*args,**kwargs)
    worker.event=unavailable
    envelope=command(time.time()); identity=envelope['command']['id']
    worker.execute(envelope,identity)
    assert worker.journal.result(identity)['result']['phase']==2
    assert len(worker.journal.pending_audits())==1
    worker.event=stored_event
    for event_id,result in worker.journal.pending_audits(): worker.persist_result(event_id,result)
    assert worker.journal.pending_audits()==[]
    assert sum(path=='/api/confirm' for path,_ in calls)==1
