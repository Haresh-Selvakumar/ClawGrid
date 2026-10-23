"""Loopback USB transport with explicitly enabled offline-PC speech recognition.

The optional local recognizer returns text only. B routes proposed commands and
A owns SafetyKernel/GPIO. No cloud recognition or automatic token injection.
"""
import argparse
import http.client
import hmac
import ipaddress
import json
import math
import uuid
import urllib.request
import threading
import time
from pathlib import Path
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlsplit


def run(host, port, serial_port=None, preview_only=False,host_dev=False,offline_model=None,supabase_config=None):
    board = str(ipaddress.IPv4Address(host))
    try:
        from .offline_speech import OfflineSpeech, SpeechError
        from .serial_gate import SerialGate, SerialBusyError
        from .supabase_bridge import CloudBridge, CloudError
    except ImportError:
        from offline_speech import OfflineSpeech, SpeechError
        from serial_gate import SerialGate, SerialBusyError
        from supabase_bridge import CloudBridge, CloudError
    speech=OfflineSpeech(offline_model) if offline_model else None
    speech_token=''
    if speech or supabase_config:
        speech_token=json.loads((Path(__file__).resolve().parents[1]/'local_credentials.json').read_text())['api_token']
    cloud=None
    development=None
    if host_dev:
        import sys
        sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
        from simulator.dev_adapter import DevAdapter
        development=DevAdapter()
    connection=None; serial_gate=SerialGate(); request_id=0
    if serial_port:
        import serial
        connection=serial.Serial(port=None,baudrate=115200,timeout=.1,write_timeout=6)
        connection.dtr=False; connection.rts=False; connection.port=serial_port
        try:
            connection.open()
        except OSError:
            print('USB not ready; data remains unavailable until a new read can reconnect.',flush=True)

    def send_serial(method,path,body,token,deadline_at=None):
        nonlocal request_id
        urgent=path in ('/api/text','/api/proposal','/api/confirm','/api/control','/api/transaction','/api/state')
        with serial_gate.turn(urgent):
            if deadline_at is not None and time.time() >= deadline_at:
                raise ValueError('Cloud command expired before USB transmission')
            try:
                if not connection.is_open:
                    connection.open()
                if deadline_at is not None and time.time() >= deadline_at: raise ValueError('Cloud command expired while reconnecting USB')
                request_id=(request_id%0xffffffff)+1
                token=token or '-'
                if len(token)>63 or any(c.isspace() for c in token): raise ValueError('Invalid token format')
                packet=f'@{request_id} {"G" if method=="GET" else "P"} {path} {len(body or b"")} {token}\n'.encode()
                connection.write(packet); connection.flush()
                if body: connection.write(body); connection.flush()
                deadline=time.monotonic()+12; buffered=bytearray(); seen=0; last_kind='none'
                while time.monotonic()<deadline:
                    buffered.extend(connection.read(max(1,min(4096,connection.in_waiting))))
                    while b'\n' in buffered:
                        line,_,rest=buffered.partition(b'\n'); buffered=bytearray(rest)
                        seen+=len(line)+1
                        try: response=json.loads(line)
                        except (ValueError,UnicodeError): continue
                        if isinstance(response,dict) and response.get('kind')=='bridge' and response.get('id')==request_id:
                            return response['status'],json.dumps(response['body'],separators=(',',':')).encode()
                        if isinstance(response,dict): last_kind=str(response.get('kind','none'))[:32]
                    if len(buffered)>16384: buffered.clear()
                print(f'USB response timeout id={request_id}, received={seen+len(buffered)}, last_kind={last_kind}',flush=True)
                raise TimeoutError('No complete response from ESP32')
            except OSError:
                connection.close()
                raise

    class Handler(BaseHTTPRequestHandler):
        protocol_version = 'HTTP/1.1'

        def local_speech(self,path,body):
            if not speech:
                status,value=503,{'error':'Offline PC speech is not enabled; start the bridge with --offline-speech MODEL_DIRECTORY','ready':False}
            elif not hmac.compare_digest((self.headers.get('X-ClawGrid-Token') or '').encode(),speech_token.encode()):
                status,value=401,{'error':'Control token required'}
            else:
                status=200
                try:
                    session=self.headers.get('X-ClawGrid-Speech-Session','')
                    if self.command=='GET' and path=='/local/speech/status': value=speech.status()
                    elif self.command=='POST' and path=='/local/speech/start' and not body: value=speech.start()
                    elif self.command=='POST' and path=='/local/speech/stop' and not body: value=speech.stop(session)
                    elif self.command=='POST' and path=='/local/speech/audio':
                        sequence=self.headers.get('X-ClawGrid-Speech-Sequence','')
                        if not sequence.isdigit() or len(sequence)>10: raise SpeechError(400,'Invalid audio sequence')
                        value=speech.feed(session,int(sequence),body)
                    else: status,value=400,{'error':'Invalid local speech request'}
                except SpeechError as error:
                    status,value=error.status,{'error':str(error)}
            payload=json.dumps(value,separators=(',',':')).encode()
            self.send_response(status);self.send_header('Content-Type','application/json')
            self.send_header('Content-Length',str(len(payload)));self.send_header('Cache-Control','no-store')
            self.send_header('X-Content-Type-Options','nosniff');self.end_headers();self.wfile.write(payload)

        def forward(self):
            # Reject DNS rebinding and cross-origin control requests.
            if self.headers.get('Host') not in (f'127.0.0.1:{port}', f'localhost:{port}'):
                self.send_error(403); return
            origin = self.headers.get('Origin')
            if origin and origin not in (f'http://127.0.0.1:{port}', f'http://localhost:{port}'):
                self.send_error(403); return
            path = urlsplit(self.path)
            if path.scheme or path.netloc or path.query or path.fragment or not (
                path.path in ('/', '/simulation','/voice','/ecosystem') or path.path.startswith('/api/') or path.path.startswith('/local/speech/') or path.path.startswith('/local/cloud/')
            ):
                self.send_error(404); return
            if self.headers.get('Transfer-Encoding'):
                self.send_error(400); return
            try:
                size = int(self.headers.get('Content-Length', '0'))
                if not 0 <= size <= 131072:
                    self.send_error(413); return
            except ValueError:
                self.send_error(400); return
            self.connection.settimeout(3)
            try:
                body = self.rfile.read(size) if size else None
                if body is not None and len(body) != size:
                    self.send_error(400); return
                if self.command=='GET' and path.path in ('/ecosystem','/api/ecosystem-client','/api/ecosystem-config'):
                    if path.path=='/api/ecosystem-config':
                        if cloud is None or cloud.realtime is None: self.send_error(503,'Realtime gateway not configured');return
                        payload=json.dumps(cloud.realtime.public_config()).encode();content_type='application/json'
                    else:
                        filename='ecosystem.html' if path.path=='/ecosystem' else 'ecosystem-client.js'
                        payload=(Path(__file__).resolve().parents[1]/'dashboard'/filename).read_bytes()
                        content_type='text/html; charset=utf-8' if filename.endswith('.html') else 'application/javascript'
                    self.send_response(200);self.send_header('Content-Type',content_type);self.send_header('Content-Length',str(len(payload)))
                    self.send_header('Cache-Control','no-store');self.send_header('X-Content-Type-Options','nosniff')
                    self.end_headers();self.wfile.write(payload);return
                if path.path.startswith('/local/cloud/'):
                    status=200
                    if not hmac.compare_digest((self.headers.get('X-ClawGrid-Token') or '').encode(),speech_token.encode()) or not speech_token:
                        status,value=401,{'error':'Control token required'}
                    elif cloud is None:
                        status,value=503,{'error':'Supabase is not enabled','enabled':False}
                    else:
                        try:
                            if self.command=='GET' and path.path=='/local/cloud/status': value=cloud.status()
                            elif self.command=='POST' and path.path=='/local/cloud/commands' and body and len(body)<=2048: value=cloud.enqueue(json.loads(body),immediate=True);status=202
                            elif self.command=='POST' and path.path=='/local/cloud/result' and body and len(body)<=128:
                                value=cloud.journal.result(str(uuid.UUID(json.loads(body)['id'])))
                            else: status,value=400,{'error':'Invalid cloud request'}
                        except (ValueError,KeyError,TypeError): status,value=400,{'error':'Invalid cloud command fields'}
                        except CloudError as error: status,value=503,{'error':str(error)}
                    payload=json.dumps(value,separators=(',',':')).encode()
                    self.send_response(status);self.send_header('Content-Type','application/json');self.send_header('Content-Length',str(len(payload)))
                    self.send_header('Cache-Control','no-store');self.end_headers();self.wfile.write(payload);return
                if path.path.startswith('/local/speech/'):
                    self.local_speech(path.path,body);return
                if self.command=='GET' and path.path=='/voice':
                    payload=(Path(__file__).resolve().parents[1]/'dashboard/voice.html').read_bytes()
                    self.send_response(200);self.send_header('Content-Type','text/html; charset=utf-8')
                    self.send_header('Content-Length',str(len(payload)));self.send_header('Cache-Control','no-store')
                    self.send_header('Content-Security-Policy',"default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'unsafe-inline'; connect-src 'self'; frame-ancestors 'none'")
                    self.end_headers();self.wfile.write(payload);return
                if connection is not None or preview_only or development is not None:
                    if self.command=='GET' and path.path=='/api/client':
                        source=(Path(__file__).resolve().parents[1]/'firmware/node_intelligence/src/dashboard_client.h').read_text(encoding='utf-8')
                        payload=source.split('R"JS(',1)[1].split(')JS"',1)[0].encode();status=200;content_type='application/javascript'
                    elif self.command=='GET' and path.path in ('/','/simulation'):
                        name='simulation_dashboard.h' if path.path=='/simulation' else 'dashboard.h'
                        source=(Path(__file__).resolve().parents[1]/'firmware/node_intelligence/src'/name).read_text(encoding='utf-8')
                        payload=source.split('R"HTML(',1)[1].split(')HTML"',1)[0].encode()
                        if development is not None:
                            from simulator.dev_adapter import inject
                            payload=inject(payload.decode()).encode()
                        if preview_only:
                            notice=b'<aside role="status" style="padding:16px;background:#593e18;color:white;font:16px system-ui">OFFLINE LAYOUT PREVIEW: boards disconnected. Person and environment controls are synthetic inputs only. ESP32 inference, decisions and device outputs are unavailable.</aside>'
                            payload=payload.replace(b'<main>',notice+b'<main>',1)
                            payload=payload.replace(b'</html>',b'<script>document.getElementById("metrics")?.replaceChildren(document.createTextNode("No measurements: boards disconnected"));document.getElementById("speechStatus")?.replaceChildren(document.createTextNode("Speech unavailable until Node B reconnects"));</script></html>')
                        status=200; content_type='text/html; charset=utf-8'
                    elif development is not None:
                        status,value=development.request(self.command,path.path,body,self.headers.get('X-ClawGrid-Token'))
                        payload=json.dumps(value,separators=(',',':')).encode();content_type='application/json'
                    elif preview_only:
                        status=503; payload=b'{"error":"Boards disconnected; interface preview only","known":false,"supported":false}'
                        content_type='application/json'
                    else:
                        command_deadline=self.headers.get('X-ClawGrid-Command-Deadline')
                        command_deadline=float(command_deadline) if command_deadline else None
                        if command_deadline is not None and (not math.isfinite(command_deadline) or command_deadline > time.time()+10): raise ValueError('Invalid command deadline')
                        status,payload=send_serial(self.command,path.path,body,self.headers.get('X-ClawGrid-Token'),command_deadline)
                        content_type='application/json'
                    self.send_response(status); self.send_header('Content-Type',content_type)
                    self.send_header('Content-Length',str(len(payload))); self.send_header('Cache-Control','no-store')
                    self.send_header('X-Content-Type-Options','nosniff')
                    self.send_header('Content-Security-Policy',"default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'unsafe-inline'; connect-src 'self'; media-src blob:; frame-ancestors 'none'")
                    self.end_headers(); self.wfile.write(payload); return
                headers = {'Connection': 'close', 'Content-Type': self.headers.get('Content-Type', 'text/plain')}
                token = self.headers.get('X-ClawGrid-Token')
                if token: headers['X-ClawGrid-Token'] = token
                http_connection = http.client.HTTPConnection(board, timeout=5)
                try:
                    http_connection.request(self.command, path.path, body, headers)
                    response = http_connection.getresponse()
                    payload = response.read(262145)
                    if len(payload) > 262144: raise ValueError('Board response exceeds limit')
                    self.send_response(response.status)
                    self.send_header('Content-Type', response.getheader('Content-Type', 'application/json'))
                    self.send_header('Content-Length', str(len(payload)))
                    self.send_header('Cache-Control', 'no-store')
                    self.send_header('X-Content-Type-Options', 'nosniff')
                    self.send_header('Content-Security-Policy', "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'unsafe-inline'; connect-src 'self'; media-src blob:; frame-ancestors 'none'")
                    self.end_headers(); self.wfile.write(payload)
                finally:
                    http_connection.close()
            except SerialBusyError:
                # Queue congestion is not a missing-board response. The expired
                # request was discarded, and commands are never retried here.
                payload=b'{"error":"USB busy; request not sent. Try again when the current request completes.","stage":"queue","retryable":true}'
                self.send_response(429)
                self.send_header('Content-Type','application/json')
                self.send_header('Content-Length',str(len(payload)))
                self.send_header('Cache-Control','no-store')
                self.send_header('Retry-After','1')
                self.end_headers();self.wfile.write(payload)
            except (OSError, ValueError, http.client.HTTPException) as error:
                print(f'Board transport failure: {self.command} {path.path}: {type(error).__name__}',flush=True)
                self.send_error(502, 'ESP32 unavailable; no simulated fallback')

        do_GET = forward
        do_POST = forward

        def log_message(self, fmt, *args):
            # Avoid request paths, headers, text or microphone data in logs.
            pass

    server = ThreadingHTTPServer(('127.0.0.1', port), Handler)
    server.daemon_threads = True
    print(f'Browser: http://127.0.0.1:{port}/simulation; '+('HOST-ONLY SIMULATION' if host_dev else 'OFFLINE INTERFACE PREVIEW' if preview_only else f'ESP32 transport {serial_port or board}'), flush=True)
    if speech: print('PC offline speech enabled; model loaded from disk; audio is not saved.',flush=True)
    if supabase_config:
        if not serial_port or preview_only or host_dev:
            server.server_close()
            raise ValueError('Supabase live integration requires the physical USB bridge')
        def board_request(path,body=None,deadline=None):
            headers={'X-ClawGrid-Token':speech_token,'Content-Type':'text/plain'}
            if deadline is not None: headers['X-ClawGrid-Command-Deadline']=str(deadline)
            request=urllib.request.Request(f'http://127.0.0.1:{port}'+path,data=body.encode() if body is not None else None,headers=headers)
            try:
                with urllib.request.urlopen(request,timeout=6) as response: return json.load(response)
            except OSError:
                raise CloudError('USB/board request failed; outcome may be unknown; no automatic command retry') from None
        cloud=CloudBridge(supabase_config,speech_token,board_request)
        try:
            from .ecosystem_realtime import RealtimeLink
        except ImportError:
            from ecosystem_realtime import RealtimeLink
        identities=json.loads((Path(__file__).resolve().parents[1]/'local_credentials.json').read_text())
        cloud.realtime=RealtimeLink(cloud,board_request,speech_token,serial_port,identities)
        cloud.thread.start()
        cloud.realtime.thread.start()
        print('Supabase backend sync enabled; service key stays on PC; commands expire and are not replayed.',flush=True)
    try: server.serve_forever()
    except KeyboardInterrupt: pass
    finally:
        if cloud is not None:
            cloud.stop.set()
            if cloud.realtime:
                cloud.realtime.stop.set()
                cloud.realtime.executor.shutdown(wait=False,cancel_futures=True)
        server.server_close()
        if connection is not None: connection.close()
        if development is not None: development.close()


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--board', default='192.168.4.1')
    parser.add_argument('--port', type=int, default=8765)
    parser.add_argument('--serial',help='Use the board USB HTTP tunnel without changing laptop Wi-Fi')
    parser.add_argument('--preview-only',action='store_true',help='Show the interface without boards; all API calls explicitly return 503')
    parser.add_argument('--host-dev',action='store_true',help='Explicit HOST-ONLY SIMULATION using portable firmware; no ESP32 speech or physical measurements')
    parser.add_argument('--offline-speech',type=Path,metavar='MODEL_DIRECTORY',help='Explicit offline PC speech recognition using a local Vosk model')
    parser.add_argument('--supabase-config',type=Path,help='Backend-only ignored Supabase configuration')
    args = parser.parse_args()
    if sum((bool(args.preview_only),bool(args.serial),bool(args.host_dev)))>1: parser.error('Choose only one of --preview-only, --serial, --host-dev')
    run(args.board, args.port, args.serial,args.preview_only,args.host_dev,args.offline_speech,args.supabase_config)
