import base64
import json
import os
import subprocess
import threading
import time
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib import request, parse

DB_HOST = os.getenv("DB_HOST", "mysql")
DB_NAME = os.getenv("DB_NAME", "prestamos_texto")
DB_USER = os.getenv("DB_USER", "appuser")
DB_PASSWORD = os.getenv("DB_PASSWORD", "apppass")
RABBIT_HOST = os.getenv("RABBIT_HOST", "192.168.0.137")
RABBIT_PORT = int(os.getenv("RABBIT_PORT", "15672"))
RABBIT_USER = os.getenv("RABBIT_USER", "admin")
RABBIT_PASS = os.getenv("RABBIT_PASS", "adminpass")
HTTP_PORT = int(os.getenv("HTTP_PORT", "8001"))

metrics = {"node": "PC2 MySQL Worker Python", "persisted": 0, "updated": 0, "errors": 0, "rabbit_errors": 0, "start_time": datetime.now(timezone.utc).isoformat()}


def jd(obj): return json.dumps(obj, ensure_ascii=False, separators=(",", ":"))
def now(): return datetime.now(timezone.utc).isoformat()
def sql(s): return str(s).replace("\\", "\\\\").replace("'", "''")


class Rabbit:
    def __init__(self):
        self.base = f"http://{RABBIT_HOST}:{RABBIT_PORT}/api"
        token = base64.b64encode(f"{RABBIT_USER}:{RABBIT_PASS}".encode()).decode()
        self.headers = {"Authorization": f"Basic {token}", "Content-Type": "application/json"}
    def call(self, method, path, payload=None, timeout=5):
        data = None if payload is None else jd(payload).encode()
        req = request.Request(self.base + path, data=data, method=method, headers=self.headers)
        with request.urlopen(req, timeout=timeout) as resp:
            raw = resp.read().decode()
            return json.loads(raw) if raw else {}
    def wait(self):
        while True:
            try:
                self.call("GET", "/overview", timeout=3)
                print("[mysql-worker] RabbitMQ disponible", flush=True)
                return
            except Exception as exc:
                print(f"[mysql-worker] Esperando RabbitMQ: {exc}", flush=True)
                time.sleep(2)
    def ensure_queue(self, q):
        return self.call("PUT", "/queues/%2f/" + parse.quote(q, safe=""), {"durable": True, "auto_delete": False, "arguments": {}})
    def publish(self, q, msg):
        payload = {"properties": {"delivery_mode": 2, "content_type": "application/json"}, "routing_key": q, "payload": jd(msg), "payload_encoding": "string"}
        return self.call("POST", "/exchanges/%2f/amq.default/publish", payload)
    def get_one(self, q):
        body = {"count": 1, "ackmode": "ack_requeue_false", "encoding": "auto", "truncate": 100000}
        data = self.call("POST", "/queues/%2f/" + parse.quote(q, safe="") + "/get", body)
        if not data: return None
        return json.loads(data[0].get("payload", "{}"))

rabbit = Rabbit()


def mysql(sql_text):
    cmd = ["mysql", "--default-character-set=utf8mb4", "-h", DB_HOST, "-u" + DB_USER, "-p" + DB_PASSWORD, "-D", DB_NAME, "-N", "-B", "-e", sql_text]
    out = subprocess.check_output(cmd, stderr=subprocess.STDOUT, text=True)
    return out.strip()


def wait_mysql():
    while True:
        try:
            mysql("SELECT 1;")
            print("[mysql-worker] MySQL disponible", flush=True)
            return
        except subprocess.CalledProcessError as exc:
            print("[mysql-worker] Esperando MySQL:", exc.output.strip(), flush=True)
            time.sleep(2)


def persist_text(msg):
    q = f"""
    INSERT INTO mensajes_texto(request_id,id_usuario,contenido,tipo_mensaje,canal,estado_proceso)
    VALUES('{sql(msg['request_id'])}',{int(msg['id_usuario'])},'{sql(msg.get('contenido',''))}','{sql(msg.get('tipo','solicitud'))}','{sql(msg.get('canal','WhatsApp'))}','RECIBIDO')
    ON DUPLICATE KEY UPDATE contenido=VALUES(contenido), estado_proceso='RECIBIDO';
    SELECT id_mensaje FROM mensajes_texto WHERE request_id='{sql(msg['request_id'])}' LIMIT 1;
    """
    out = mysql(q)
    id_origen = int(out.splitlines()[-1])
    metrics["persisted"] += 1
    rabbit.publish("q.response.pc1", {"ok": True, "action": "mysql.persisted", "request_id": msg["request_id"], "id_origen": id_origen})


def update_result(msg):
    q = f"""
    UPDATE mensajes_texto
    SET estado_proceso='PROCESADO', decision='{sql(msg.get('decision',''))}', riesgo_calculado={int(msg.get('riesgo',0))}, respuesta_ia='{sql(msg.get('motivo',''))}'
    WHERE request_id='{sql(msg['request_id'])}';
    """
    mysql(q)
    metrics["updated"] += 1
    rabbit.publish("q.response.pc1", {"ok": True, "action": "mysql.updated", "request_id": msg["request_id"]})


def worker_loop():
    wait_mysql(); rabbit.wait()
    for q in ["q.mysql.persist_text", "q.mysql.update_result", "q.response.pc1"]:
        rabbit.ensure_queue(q)
    while True:
        try:
            msg = rabbit.get_one("q.mysql.persist_text")
            if msg: persist_text(msg)
            msg = rabbit.get_one("q.mysql.update_result")
            if msg: update_result(msg)
            if not msg: time.sleep(0.2)
        except Exception as exc:
            metrics["errors"] += 1
            print("[mysql-worker] ERROR:", repr(exc), flush=True)
            time.sleep(1)


class Handler(BaseHTTPRequestHandler):
    def log_message(self, fmt, *args): print("[mysql-worker]", fmt % args, flush=True)
    def send_json(self, obj, code=200):
        raw = jd(obj).encode()
        self.send_response(code); self.send_header("Content-Type", "application/json; charset=utf-8"); self.send_header("Content-Length", str(len(raw))); self.send_header("Access-Control-Allow-Origin", "*"); self.end_headers(); self.wfile.write(raw)
    def do_GET(self):
        if self.path == "/health": self.send_json(metrics)
        elif self.path == "/recent":
            try:
                out = mysql("SELECT id_mensaje,request_id,id_usuario,estado_proceso,IFNULL(decision,''),IFNULL(riesgo_calculado,0) FROM mensajes_texto ORDER BY id_mensaje DESC LIMIT 10;")
                self.send_json({"rows": out.splitlines()})
            except Exception as exc: self.send_json({"ok": False, "error": str(exc)}, 500)
        else: self.send_json({"ok": False, "error": "not found"}, 404)


def main():
    threading.Thread(target=worker_loop, daemon=True).start()
    ThreadingHTTPServer(("0.0.0.0", HTTP_PORT), Handler).serve_forever()

if __name__ == "__main__": main()
