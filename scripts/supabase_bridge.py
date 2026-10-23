"""Backend-only Supabase adapter for the existing ClawGrid schema.

The current localhost website uses its existing control token. No service key
is sent to a browser or ESP. Commands are signed, expire, are journaled before
execution, and still pass through B's immutable proposal and A's SafetyKernel.
"""
import hashlib
import hmac
import json
import math
import sqlite3
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from datetime import datetime, timezone
from pathlib import Path


def utc(at=None):
    return datetime.fromtimestamp(time.time() if at is None else at, timezone.utc).isoformat()


def canonical(value):
    return json.dumps(value, sort_keys=True, separators=(',', ':'), allow_nan=False).encode()


def target_fields(value):
    names = ('target', 'room', 'device', 'action')
    target = {key: value.get(key) for key in names}
    if target['target'] not in ('physical_bedroom', 'virtual_rooms'):
        raise ValueError('Invalid cloud target')
    if target['room'] not in ('bedroom', 'living_room', 'kitchen', 'study'):
        raise ValueError('Invalid cloud room')
    if target['target'] == 'physical_bedroom' and target['room'] != 'bedroom':
        raise ValueError('Physical commands support bedroom only')
    if target['device'] not in ('light', 'fan') or target['action'] not in ('ON', 'OFF'):
        raise ValueError('Cloud commands require light/fan and explicit ON/OFF')
    return target


def signed_command(value, home, gateway, key, now):
    target = target_fields(value)
    command = dict(target, id=str(uuid.UUID(value['id'])), home_id=home,
                   gateway_id=gateway, issued_at=now, expires_at=now + 10, version=1)
    return dict(command=command, signature=hmac.new(key.encode(), canonical(command), hashlib.sha256).hexdigest())


def verify_command(envelope, event_id, home, gateway, key, now, online_since):
    if not isinstance(envelope, dict) or not isinstance(envelope.get('command'), dict):
        raise ValueError('Invalid command envelope')
    command = envelope['command']
    signature = hmac.new(key.encode(), canonical(command), hashlib.sha256).hexdigest()
    if not hmac.compare_digest(signature, envelope.get('signature', '')):
        raise ValueError('Command signature rejected')
    if command.get('version') != 1 or command.get('id') != event_id:
        raise ValueError('Command ID/version mismatch')
    uuid.UUID(event_id)
    if command.get('home_id') != home or command.get('gateway_id') != gateway:
        raise ValueError('Command belongs to another home/gateway')
    issued, expires = command.get('issued_at'), command.get('expires_at')
    if any(type(v) not in (int, float) or not math.isfinite(v) for v in (issued, expires)):
        raise ValueError('Invalid command time')
    if not 0 < expires - issued <= 10 or issued > now + 1 or now >= expires or issued < online_since:
        raise ValueError('Expired, future or pre-reconnect command rejected')
    return target_fields(command)


def telemetry(state, home, now):
    known = state.get('known') is True and state.get('authenticated') is True
    age = state.get('age_ms')
    if type(age) is not int or age < 0 or age >= 3000:
        known = False
    return dict(id=str(uuid.uuid4()), home_id=home, room_id='bedroom',
                source='HARDWARE_MEASURED', processing_source='HARDWARE_MEASURED',
                observed_at=utc(now - age / 1000 if known else now), known=known,
                age_ms=age if type(age) is int and age >= 0 else None,
                occupancy=state.get('occupancy', 'UNKNOWN') if known else 'UNKNOWN',
                light=state.get('light') if known else None,
                fan=state.get('fan') if known else None,
                motion=state.get('motion') if known else None,
                outputs_enabled=state.get('physical_outputs_enabled') is True,
                temperature_c=None, illuminance_lux=None)


class CloudError(Exception):
    pass


class Journal:
    def __init__(self, path):
        self.path = Path(path)
        self.path.parent.mkdir(parents=True, exist_ok=True)
        with sqlite3.connect(self.path) as connection:
            connection.execute('CREATE TABLE IF NOT EXISTS commands (id TEXT PRIMARY KEY, status TEXT NOT NULL, result TEXT)')
            if 'audited' not in [row[1] for row in connection.execute('PRAGMA table_info(commands)')]:
                connection.execute('ALTER TABLE commands ADD COLUMN audited INTEGER NOT NULL DEFAULT 0')
            connection.execute("UPDATE commands SET status='unknown', result=? WHERE status='processing'",
                               (json.dumps(dict(error='Gateway restarted; outcome unknown; not replayed')),))

    def claim(self, identity):
        with sqlite3.connect(self.path) as connection:
            return connection.execute("INSERT OR IGNORE INTO commands(id,status) VALUES(?,'processing')", (identity,)).rowcount == 1

    def finish(self, identity, result):
        with sqlite3.connect(self.path) as connection:
            connection.execute('UPDATE commands SET status=?,result=? WHERE id=?',
                               ('acknowledged' if result.get('phase') == 2 else 'unknown', json.dumps(result), identity))

    def result(self, identity):
        with sqlite3.connect(self.path) as connection:
            row = connection.execute('SELECT status,result FROM commands WHERE id=?', (identity,)).fetchone()
        return dict(cloud_id=identity, phase=row[0] if row else 'pending',
                    result=json.loads(row[1]) if row and row[1] else None)

    def pending_audits(self):
        with sqlite3.connect(self.path) as connection:
            return [(identity,json.loads(result)) for identity,result in connection.execute(
                'SELECT id,result FROM commands WHERE audited=0 AND result IS NOT NULL LIMIT 5')]

    def mark_audited(self, identity):
        with sqlite3.connect(self.path) as connection:
            connection.execute('UPDATE commands SET audited=1 WHERE id=?',(identity,))


class Supabase:
    def __init__(self, config):
        self.url = config['url'].rstrip('/')
        parsed = urllib.parse.urlsplit(self.url)
        if parsed.scheme != 'https' or not parsed.hostname or not parsed.hostname.endswith('.supabase.co') or parsed.path or parsed.query or parsed.username:
            raise ValueError('Use the HTTPS project URL from Supabase')
        self.key = config['service_role_key']

    def request(self, path, data=None):
        headers = {'apikey': self.key, 'Authorization': 'Bearer ' + self.key,
                   'Content-Type': 'application/json', 'Prefer': 'return=minimal'}
        request = urllib.request.Request(self.url + '/rest/v1/' + path,
                                         data=None if data is None else canonical(data), headers=headers)
        try:
            with urllib.request.urlopen(request, timeout=5) as response:
                body = response.read(262145)
                if len(body) > 262144:
                    raise CloudError('Supabase response exceeds limit')
                return json.loads(body) if body else None
        except urllib.error.HTTPError as error:
            # Never log request headers, credentials or arbitrary server bodies.
            try: code=json.loads(error.read(8192)).get('code','unknown')
            except (ValueError,OSError): code='unknown'
            raise CloudError(f'Supabase HTTP {error.code} ({code}) on {path.split("?")[0]}') from None
        except (OSError, ValueError):
            raise CloudError('Supabase unavailable; no command replay') from None


class CloudBridge:
    def __init__(self, path, token, board):
        self.path = Path(path)
        self.config = json.loads(self.path.read_text())
        if not self.config.get('enabled'):
            raise ValueError('Supabase is disabled in its private config')
        self.home = str(uuid.UUID(self.config['home_id']))
        self.gateway = str(uuid.UUID(self.config['gateway_id']))
        self.token, self.board = token, board
        self.cloud = Supabase(self.config)
        self.producer = 'dashboard'  # Existing schema's allowed publisher; gateway ID is signed in the envelope.
        self.journal = Journal(self.path.parent / 'benchmarks/backups/cloud_commands.sqlite3')
        self.realtime = None
        self.stop = threading.Event()
        self.lock = threading.Lock()
        self.online_since = None
        self.last_poll = self.last_upload = 0
        self.error = 'Connecting to Supabase'
        self.thread = threading.Thread(target=self.run, name='SupabaseBridge', daemon=True)

    def status(self):
        with self.lock:
            return dict(enabled=True, ready=self.online_since is not None and time.monotonic() - self.last_poll < 6,
                        home_id=self.home, gateway_id=self.gateway, project_url=self.cloud.url,
                        last_upload_at=utc(self.last_upload) if self.last_upload else None,
                        error=self.error, command_ttl_seconds=10, physical_state='not independently measured',
                        realtime=self.realtime.status() if self.realtime else None)

    def enqueue(self, value, issued_at=None, expires_at=None, immediate=False):
        if not self.status()['ready']:
            raise CloudError('Supabase gateway offline/busy; command was not queued')
        issued=time.time() if issued_at is None else issued_at
        envelope = signed_command(value, self.home, self.gateway, self.token, issued)
        if expires_at is not None:
            if not issued < expires_at <= issued+10 or time.time() >= expires_at: raise CloudError('Expired realtime command')
            envelope['command']['expires_at']=expires_at
            envelope['signature']=hmac.new(self.token.encode(),canonical(envelope['command']),hashlib.sha256).hexdigest()
        self.event('CLOUD_COMMAND_REQUESTED', envelope, identity=envelope['command']['id'])
        if immediate:
            self.execute(envelope, envelope['command']['id'])
        return dict(cloud_id=envelope['command']['id'], queued=True, authorized=False)

    def event(self, kind, value, identity=None, hardware=False):
        # The existing schema pairs producer=dashboard with HOST_MEASURED.
        # A hardware ACK remains labelled inside the immutable receipt payload.
        if hardware:
            value = dict(value, evidence_source='HARDWARE_MEASURED')
        row = dict(id=identity or str(uuid.uuid4()), home_id=self.home, room_id='bedroom',
                   source='HOST_MEASURED', producer=self.producer,
                   event_type=kind, severity='INFO', description=canonical(value).decode(), observed_at=utc())
        self.cloud.request('clawgrid_events', row)

    def persist_result(self, event_id, result):
        receipt_id = str(uuid.uuid5(uuid.UUID(event_id), 'ClawGridResultV1'))
        try:
            self.event('CLOUD_COMMAND_RESULT', dict(cloud_id=event_id, result=result, physical_state='not measured'),
                       identity=receipt_id, hardware=result.get('phase') == 2)
        except CloudError as error:
            if 'HTTP 409' not in str(error):
                raise
            rows=self.cloud.request('clawgrid_events?id=eq.'+receipt_id+'&select=event_type,description')
            if not rows or rows[0]['event_type']!='CLOUD_COMMAND_RESULT' or json.loads(rows[0]['description']).get('cloud_id')!=event_id:
                raise CloudError('Conflicting cloud audit receipt') from None
        self.journal.mark_audited(event_id)

    def execute(self, envelope, event_id):
        with self.lock:
            since = self.online_since
        if since is None:
            return
        try:
            target = verify_command(envelope, event_id, self.home, self.gateway, self.token, time.time(), since)
        except (ValueError, KeyError, TypeError):
            return  # An invalid cloud record cannot reach a board.
        if not self.journal.claim(event_id):
            return
        expires = envelope['command']['expires_at']
        try:
            state = self.board('/api/state', deadline=expires)
            if (state.get('known') is not True or state.get('authenticated') is not True
                    or type(state.get('age_ms')) is not int or not 0 <= state['age_ms'] < 3000):
                raise CloudError('A is unavailable; command discarded')
            fields = ' '.join(target[k] for k in ('target', 'room', 'device', 'action'))
            proposal = self.board('/api/proposal', 'manual ' + fields + ' 0', deadline=expires)
            if any(proposal.get(k) != target[k] for k in target):
                raise CloudError('B returned a different target; command discarded')
            if time.time() >= expires:
                raise CloudError('Cloud command expired before confirmation')
            queued = self.board('/api/confirm', str(proposal['proposal_id']) + ' ' + fields, deadline=expires)
            result = None
            for _ in range(20):
                transaction = self.board('/api/transaction')
                if transaction.get('id') == queued['id'] and transaction.get('phase') in (2, 3):
                    result = transaction
                    break
                if self.stop.wait(.1):
                    break
            if result is None:
                raise CloudError('ACK not observed; outcome unknown; command will not be retried')
        except (CloudError, OSError, ValueError, KeyError) as error:
            result = dict(error=str(error), phase=3, outcome_known=False)
        self.journal.finish(event_id, result)
        if self.realtime: self.realtime.result(event_id,result)
        # This is a software ACK, never a claim of measured relay/load state.
        try:
            self.persist_result(event_id,result)
        except CloudError:
            pass  # Retry the stored receipt later, never the actuation.

    def run_once(self):
        query = urllib.parse.urlencode(dict(home_id='eq.' + self.home, producer='eq.' + self.producer,
                    event_type='eq.CLOUD_COMMAND_REQUESTED', received_at='gte.' + utc(time.time() - 15),
                    select='id,description', order='received_at.asc', limit='100'))
        events = self.cloud.request('clawgrid_events?' + query)
        with self.lock:
            if self.online_since is None:
                self.online_since = time.time()  # Never drain offline/reconnect backlog.
            self.last_poll = time.monotonic()
            self.error = None
        if time.time() - self.last_upload >= 10:
            try:
                state = self.board('/api/state')
            except (OSError, CloudError):
                state = dict(known=False)
            row = telemetry(state, self.home, time.time())
            self.cloud.request('clawgrid_telemetry', row)
            with self.lock:
                self.last_upload = time.time()
        for event in events:
            try:
                envelope = json.loads(event['description'])
            except (ValueError, KeyError, TypeError):
                continue
            self.execute(envelope, event['id'])
        for event_id,result in self.journal.pending_audits():
            self.persist_result(event_id,result)

    def run(self):
        while not self.stop.is_set():
            try:
                self.run_once()
            except (CloudError, OSError, ValueError, KeyError) as error:
                with self.lock:
                    self.online_since = None
                    self.error = str(error)
            self.stop.wait(1)
