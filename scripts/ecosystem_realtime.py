"""Encrypted Supabase Broadcast snapshots shared by web and Android clients."""
import base64
import hashlib
import json
import os
import threading
import time
import uuid
from concurrent.futures import ThreadPoolExecutor

from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from websockets.sync.client import connect
from websockets.exceptions import ConnectionClosed


class WireCipher:
    def __init__(self, token, home, gateway):
        self.home, self.gateway = home, gateway
        self.aad = f'ClawGridRealtimeV1|{home}|{gateway}'.encode()
        self.aes = AESGCM(hashlib.sha256(('ClawGridRealtimeV1:' + token).encode()).digest())

    def encrypt(self, payload):
        iv = os.urandom(12)
        raw = json.dumps(payload, separators=(',', ':'), allow_nan=False).encode()
        if len(raw) > 32768:
            raise ValueError('Realtime payload too large')
        ciphertext = self.aes.encrypt(iv, raw, self.aad)
        return dict(v=1, home_id=self.home, gateway_id=self.gateway,
                    iv=base64.b64encode(iv).decode(), ct=base64.b64encode(ciphertext).decode())

    def decrypt(self, envelope):
        if envelope.get('v') != 1 or envelope.get('home_id') != self.home or envelope.get('gateway_id') != self.gateway:
            raise ValueError('Realtime scope mismatch')
        iv = base64.b64decode(envelope['iv'], validate=True)
        ciphertext = base64.b64decode(envelope['ct'], validate=True)
        if len(iv) != 12 or not 16 <= len(ciphertext) <= 32784:
            raise ValueError('Invalid encrypted frame size')
        value = json.loads(self.aes.decrypt(iv, ciphertext, self.aad))
        if not isinstance(value, dict):
            raise ValueError('Invalid realtime object')
        return value


class RealtimeLink:
    def __init__(self, cloud, board, token, serial_port, identities):
        self.cloud, self.board = cloud, board
        self.serial_port, self.identities = serial_port, identities
        self.cipher = WireCipher(token, cloud.home, cloud.gateway)
        self.topic = 'realtime:clawgrid_live'
        self.session, self.sequence, self.reference = str(uuid.uuid4()), 0, 10
        self.socket = None
        self.send_lock = threading.Lock()
        self.status_lock = threading.Lock()
        self.connected = False
        self.last_publish = None
        self.last_snapshot = None
        self.received_frames = self.invalid_frames = self.received_commands = 0
        self.last_ingress_error = None
        self.stop = threading.Event()
        self.executor = ThreadPoolExecutor(max_workers=1, thread_name_prefix='CloudIngress')
        self.command_slot = threading.BoundedSemaphore(2)
        self.thread = threading.Thread(target=self.run, daemon=True, name='EcosystemRealtime')

    def public_config(self):
        return dict(url=self.cloud.cloud.url, anon_key=self.cloud.config['anon_key'],
                    home_id=self.cloud.home, gateway_id=self.cloud.gateway,
                    channel='clawgrid_live', protocol='ClawGridRealtimeV1')

    def status(self):
        with self.status_lock:
            return dict(connected=self.connected, last_publish_at_ms=self.last_publish,
                        snapshot_interval_ms=1000, diagnostics_interval_ms=5000,
                        encrypted=True, session_id=self.session, received_frames=self.received_frames,
                        invalid_frames=self.invalid_frames, received_commands=self.received_commands,
                        last_ingress_error=self.last_ingress_error)

    def frame(self, event, payload, reference=None):
        with self.send_lock:
            if self.socket is None:
                raise OSError('Realtime disconnected')
            self.reference += 1
            self.socket.send(json.dumps(dict(topic=self.topic, event=event, payload=payload,
                                             ref=reference or str(self.reference))))

    def broadcast(self, payload):
        self.frame('broadcast', dict(type='broadcast', event='clawgrid_secure_v1', payload=self.cipher.encrypt(payload)))

    def result(self, cloud_id, result):
        try:
            self.broadcast(dict(kind='command_ack', cloud_id=cloud_id, result=result,
                                sent_at_ms=int(time.time() * 1000), physical_state='not measured'))
        except (OSError, ValueError, ConnectionClosed):
            pass  # Journal/cloud receipt is retained; never re-execute a command.

    def incoming_command(self, value):
        identity = value.get('id')
        try:
            now = time.time()
            issued, expires = value['issued_at'], value['expires_at']
            if (type(issued) not in (int, float) or type(expires) not in (int, float)
                    or not 0 < expires - issued <= 10 or issued > now + 1 or now >= expires):
                raise ValueError('Expired or future command')
            # Execute after durable storage without waiting for the history
            # poll/upload cycle. Journal.claim prevents duplicate execution.
            envelope = self.cloud.enqueue(value, issued_at=issued, expires_at=expires, immediate=True)
            self.broadcast(dict(kind='command_queued', cloud_id=envelope['cloud_id'],
                                sent_at_ms=int(time.time() * 1000)))
        except Exception as error:
            self.last_ingress_error = type(error).__name__
            self.result(identity, dict(phase=3, outcome_known=False, error='Command not accepted by cloud gateway; no automatic retry'))
        finally:
            self.command_slot.release()

    def snapshot(self, memory, rtos, diagnostic_at):
        try:
            state = self.board('/api/state')
        except Exception:
            state = dict(known=False, authenticated=False, occupancy='UNKNOWN', physical_state='not measured')
        self.sequence += 1
        return dict(kind='snapshot', version=1, home_id=self.cloud.home, gateway_id=self.cloud.gateway,
                    session_id=self.session, sequence=self.sequence, sent_at_ms=int(time.time() * 1000),
                    source='HARDWARE_MEASURED',
                    node_a=dict(state, expected_mac=self.identities['node_a_mac'], memory=None, rtos=None),
                    node_b=dict(expected_mac=self.identities['node_b_mac'], usb_port=self.serial_port,
                                memory=memory, rtos=rtos, diagnostics_at_ms=diagnostic_at),
                    sensors=dict(temperature_c=None, illuminance_lux=None, pir_room='bedroom'),
                    limitations=['Relay contacts/load state not independently measured',
                                 'Node A heap/task diagnostics unavailable over current radio protocol',
                                 'Only bedroom has a physical PIR; other rooms have no physical sensors'])

    def run(self):
        url = self.cloud.cloud.url.replace('https://', 'wss://') + '/realtime/v1/websocket?apikey=' + self.cloud.config['anon_key'] + '&vsn=1.0.0'
        while not self.stop.is_set():
            try:
                with connect(url, open_timeout=8, close_timeout=2, max_size=65536, ping_interval=20, ping_timeout=20) as socket:
                    self.socket = socket
                    join_ref = 'join-' + self.session
                    self.frame('phx_join', dict(config=dict(broadcast=dict(ack=False, self=False),
                               presence=dict(enabled=False), postgres_changes=[], private=False),
                               access_token=self.cloud.config['service_role_key']), join_ref)
                    joined = False
                    next_snapshot = next_diagnostics = 0
                    heartbeat = time.monotonic()
                    memory = rtos = diagnostic_at = None
                    while not self.stop.is_set():
                        try:
                            frame = json.loads(socket.recv(timeout=.1))
                            if frame.get('event') == 'phx_reply' and frame.get('ref') == join_ref:
                                joined = frame.get('payload', {}).get('status') == 'ok'
                                if not joined:
                                    raise ValueError('Realtime join rejected')
                                with self.status_lock:
                                    self.connected = True
                            if frame.get('event') == 'broadcast' and frame.get('payload', {}).get('event') == 'clawgrid_secure_v1':
                                self.received_frames += 1
                                try:
                                    value = self.cipher.decrypt(frame['payload']['payload'])
                                    if value.get('kind') == 'command' and self.command_slot.acquire(blocking=False):
                                        self.received_commands += 1
                                        self.executor.submit(self.incoming_command, value)
                                except Exception:
                                    self.invalid_frames += 1
                                    pass  # Unauthenticated/malformed broadcasts have no authority.
                        except TimeoutError:
                            pass
                        now = time.monotonic()
                        if now - heartbeat >= 20:
                            with self.send_lock:
                                socket.send(json.dumps(dict(topic='phoenix', event='heartbeat', payload={}, ref='heartbeat-' + str(now))))
                            heartbeat = now
                        if joined and now >= next_snapshot:
                            if now >= next_diagnostics:
                                try:
                                    memory = self.board('/api/memory')
                                    rtos = self.board('/api/rtos')
                                    diagnostic_at = int(time.time() * 1000)
                                except Exception:
                                    memory = rtos = diagnostic_at = None
                                next_diagnostics = time.monotonic() + 5
                            snapshot = self.snapshot(memory, rtos, diagnostic_at)
                            self.broadcast(snapshot)
                            with self.status_lock:
                                self.last_publish = snapshot['sent_at_ms']
                                self.last_snapshot = snapshot
                            next_snapshot = time.monotonic() + 1
            except Exception:
                # URLs contain credentials; never log an exception/URL verbatim.
                pass
            finally:
                self.socket = None
                with self.status_lock:
                    self.connected = False
            self.stop.wait(2)
