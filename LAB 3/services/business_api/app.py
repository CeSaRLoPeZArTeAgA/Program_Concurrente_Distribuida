import base64
import json
import os
import re
import threading
import time
import uuid
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib import request, error, parse

RABBIT_HOST = os.getenv("RABBIT_HOST", "localhost")
RABBIT_PORT = int(os.getenv("RABBIT_PORT", "15672"))
RABBIT_USER = os.getenv("RABBIT_USER", "admin")
RABBIT_PASS = os.getenv("RABBIT_PASS", "adminpass")
API_PORT = int(os.getenv("API_PORT", "8080"))
DATA_FILE = os.getenv("DATA_FILE", "/data/requests.json")

QUEUES = [
    "q.mysql.persist_text",
    "q.mysql.update_result",
    "q.postgres.persist_email",
    "q.postgres.update_result",
    "q.mariadb.query_profile",
    "q.mariadb.persist_decision",
    "q.response.pc1",
]

state_lock = threading.Lock()
response_lock = threading.Lock()
requests_state = {}
response_pool = {}
metrics = {
    "received_http": 0,
    "sms_requests": 0,
    "email_requests": 0,
    "processed": 0,
    "errors": 0,
    "rabbit_errors": 0,
    "start_time": datetime.now(timezone.utc).isoformat(),
}


def now_iso():
    return datetime.now(timezone.utc).isoformat()


def json_dumps(obj):
    return json.dumps(obj, ensure_ascii=False, separators=(",", ":"))


class RabbitHTTP:
    def __init__(self, host, port, user, password):
        self.base = f"http://{host}:{port}/api"
        token = base64.b64encode(f"{user}:{password}".encode()).decode()
        self.headers = {"Authorization": f"Basic {token}", "Content-Type": "application/json"}

    def call(self, method, path, payload=None, timeout=5):
        url = self.base + path
        data = None if payload is None else json_dumps(payload).encode("utf-8")
        req = request.Request(url, data=data, method=method, headers=self.headers)
        with request.urlopen(req, timeout=timeout) as resp:
            raw = resp.read().decode("utf-8")
            if not raw:
                return {}
            return json.loads(raw)

    def wait(self, retries=120, delay=1):
        last = None
        for i in range(retries):
            try:
                self.call("GET", "/overview", timeout=3)
                print("[business-api] RabbitMQ disponible", flush=True)
                return True
            except Exception as exc:
                last = exc
                print(f"[business-api] Esperando RabbitMQ ({i+1}/{retries}): {exc}", flush=True)
                time.sleep(delay)
        raise RuntimeError(f"RabbitMQ no disponible: {last}")

    def ensure_queue(self, queue):
        body = {"durable": True, "auto_delete": False, "arguments": {}}
        return self.call("PUT", "/queues/%2f/" + parse.quote(queue, safe=""), body)

    def publish(self, queue, message):
        payload = {
            "properties": {"delivery_mode": 2, "content_type": "application/json"},
            "routing_key": queue,
            "payload": json_dumps(message),
            "payload_encoding": "string",
        }
        return self.call("POST", "/exchanges/%2f/amq.default/publish", payload)

    def get_one(self, queue):
        body = {"count": 1, "ackmode": "ack_requeue_false", "encoding": "auto", "truncate": 100000}
        data = self.call("POST", "/queues/%2f/" + parse.quote(queue, safe="") + "/get", body)
        if not data:
            return None
        item = data[0]
        payload = item.get("payload")
        if isinstance(payload, str):
            return json.loads(payload)
        return payload

rabbit = RabbitHTTP(RABBIT_HOST, RABBIT_PORT, RABBIT_USER, RABBIT_PASS)


def save_state():
    os.makedirs(os.path.dirname(DATA_FILE), exist_ok=True)
    tmp = DATA_FILE + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        json.dump(requests_state, f, ensure_ascii=False, indent=2)
    os.replace(tmp, DATA_FILE)


def load_state():
    global requests_state
    try:
        with open(DATA_FILE, "r", encoding="utf-8") as f:
            requests_state = json.load(f)
    except FileNotFoundError:
        requests_state = {}


def set_request(request_id, data):
    with state_lock:
        current = requests_state.get(request_id, {})
        current.update(data)
        current["updated_at"] = now_iso()
        requests_state[request_id] = current
        save_state()


def get_stashed_response(request_id, action):
    with response_lock:
        arr = response_pool.get(request_id, [])
        for i, msg in enumerate(arr):
            if msg.get("action") == action:
                return arr.pop(i)
    return None


def stash_response(msg):
    rid = msg.get("request_id", "")
    if not rid:
        return
    with response_lock:
        response_pool.setdefault(rid, []).append(msg)


def wait_response(request_id, action, timeout=15):
    stashed = get_stashed_response(request_id, action)
    if stashed:
        return stashed
    deadline = time.time() + timeout
    while time.time() < deadline:
        try:
            msg = rabbit.get_one("q.response.pc1")
        except Exception as exc:
            metrics["rabbit_errors"] += 1
            time.sleep(0.3)
            continue
        if not msg:
            time.sleep(0.25)
            continue
        if msg.get("request_id") == request_id and msg.get("action") == action:
            return msg
        stash_response(msg)
    return {"ok": False, "action": action, "request_id": request_id, "error": "timeout"}


def ensure_queues():
    rabbit.wait()
    for q in QUEUES:
        rabbit.ensure_queue(q)
    print("[business-api] Colas declaradas", QUEUES, flush=True)


def read_body(handler):
    length = int(handler.headers.get("Content-Length", "0") or 0)
    raw = handler.rfile.read(length).decode("utf-8") if length else "{}"
    if not raw.strip():
        return {}
    return json.loads(raw)


def amount_from_text(text, explicit=None):
    if explicit is not None and str(explicit).strip() != "":
        try:
            return float(explicit)
        except Exception:
            pass
    nums = re.findall(r"\b\d+(?:[\.,]\d+)?\b", text or "")
    vals = []
    for n in nums:
        try:
            vals.append(float(n.replace(",", ".")))
        except Exception:
            pass
    return max(vals) if vals else 0.0


def detect_intent(text, tipo=""):
    s = f"{tipo} {text}".lower()
    if "refinanc" in s:
        return "refinanciamiento"
    if "consulta" in s or "estado" in s:
        return "consulta"
    if "atras" in s or "mora" in s or "deuda" in s:
        return "solicitud_riesgosa"
    return "solicitud"


def compute_risk(payload, profile):
    text = (payload.get("contenido") or payload.get("cuerpo") or "").lower()
    monto = amount_from_text(text, payload.get("monto"))
    saldo = float(profile.get("saldo_total") or 0)
    deuda = float(profile.get("deuda_total") or 0)
    activos = int(float(profile.get("prestamos_activos") or 0))
    cuotas = int(float(profile.get("cuotas_vencidas") or 0))
    mora = int(float(profile.get("dias_mora_max") or 0))

    risk = 20
    reasons = []
    if monto > 0 and saldo > 0:
        ratio = monto / max(saldo, 1.0)
        if ratio > 2:
            risk += 25; reasons.append("monto solicitado alto respecto al saldo")
        elif ratio > 1:
            risk += 12; reasons.append("monto solicitado moderado respecto al saldo")
        else:
            risk -= 5; reasons.append("monto compatible con saldo")
    elif monto > 8000:
        risk += 18; reasons.append("monto solicitado alto")

    if deuda > 0 and saldo > 0 and deuda / max(saldo, 1.0) > 2:
        risk += 18; reasons.append("deuda superior al saldo disponible")
    if activos >= 2:
        risk += 12; reasons.append("varios prestamos activos")
    elif activos == 1:
        risk += 5; reasons.append("prestamo activo vigente")
    if cuotas > 0:
        risk += min(30, cuotas * 9); reasons.append(f"{cuotas} cuotas vencidas")
    if mora > 30:
        risk += 22; reasons.append("mora mayor a 30 dias")
    elif mora > 0:
        risk += 8; reasons.append("mora reciente")
    if any(k in text for k in ["urgente", "mora", "atras", "deuda", "pendiente"]):
        risk += 10; reasons.append("texto contiene indicadores de riesgo")
    if any(k in text for k in ["sueldo estable", "trabajo formal", "ingreso fijo", "planilla"]):
        risk -= 12; reasons.append("texto indica ingresos estables")
    if "refinanc" in text:
        risk = max(risk, 45); reasons.append("solicitud de refinanciamiento")

    risk = max(0, min(100, int(round(risk))))
    if detect_intent(text, payload.get("tipo", "")) == "consulta":
        decision = "CONSULTA_REGISTRADA"
        risk = min(risk, 20)
    elif "refinanc" in text or payload.get("tipo") == "refinanciamiento":
        decision = "REFINANCIAMIENTO_PROPUESTO" if risk <= 75 else "REFINANCIAMIENTO_OBSERVADO"
    elif risk <= 35:
        decision = "PRESTAMO_ACEPTADO"
    elif risk <= 65:
        decision = "REVISION_MANUAL"
    else:
        decision = "PRESTAMO_RECHAZADO"
    motivo = "; ".join(reasons) if reasons else "evaluacion sin alertas relevantes"
    return {"monto": monto, "riesgo": risk, "decision": decision, "motivo": motivo}


def process_sms(payload):
    request_id = "REQ-" + datetime.now(timezone.utc).strftime("%Y%m%d%H%M%S") + "-" + uuid.uuid4().hex[:8]
    id_usuario = int(payload.get("id_usuario"))
    contenido = str(payload.get("contenido", "")).strip()
    if not contenido:
        raise ValueError("contenido vacio")
    tipo = payload.get("tipo", "solicitud")
    canal = payload.get("canal", "WhatsApp")
    base = {"request_id": request_id, "source": "mysql", "id_usuario": id_usuario, "contenido": contenido, "tipo": tipo, "canal": canal, "fecha_evento": now_iso()}
    set_request(request_id, {"estado": "RECIBIDO", "source": "mysql", "id_usuario": id_usuario, "contenido": contenido})

    rabbit.publish("q.mysql.persist_text", base)
    ack_mysql = wait_response(request_id, "mysql.persisted", timeout=20)
    if not ack_mysql.get("ok"):
        raise RuntimeError("MySQL no confirmo persistencia: " + json_dumps(ack_mysql))

    rabbit.publish("q.mariadb.query_profile", {"request_id": request_id, "id_usuario": id_usuario})
    profile_msg = wait_response(request_id, "mariadb.profile", timeout=20)
    if not profile_msg.get("ok"):
        raise RuntimeError("MariaDB no devolvio perfil: " + json_dumps(profile_msg))
    profile = profile_msg.get("profile", {})

    eval_res = compute_risk({**base, "monto": payload.get("monto")}, profile)
    intent = detect_intent(contenido, tipo)
    final_payload = {
        "request_id": request_id,
        "source": "mysql",
        "id_origen": ack_mysql.get("id_origen"),
        "id_usuario": id_usuario,
        "intencion": intent,
        "monto": eval_res["monto"],
        "decision": eval_res["decision"],
        "riesgo": eval_res["riesgo"],
        "motivo": eval_res["motivo"],
    }
    rabbit.publish("q.mariadb.persist_decision", final_payload)
    wait_response(request_id, "mariadb.decision_saved", timeout=10)
    rabbit.publish("q.mysql.update_result", final_payload)
    wait_response(request_id, "mysql.updated", timeout=10)
    result = {**final_payload, "estado": "PROCESADO", "profile": profile}
    set_request(request_id, result)
    metrics["processed"] += 1
    return result


def process_email(payload):
    request_id = "REQ-" + datetime.now(timezone.utc).strftime("%Y%m%d%H%M%S") + "-" + uuid.uuid4().hex[:8]
    id_usuario = int(payload.get("id_usuario"))
    asunto = str(payload.get("asunto", "")).strip()
    cuerpo = str(payload.get("cuerpo", "")).strip()
    if not asunto or not cuerpo:
        raise ValueError("asunto y cuerpo son obligatorios")
    tipo = payload.get("tipo", "solicitud")
    prioridad = payload.get("prioridad", "media")
    base = {"request_id": request_id, "source": "postgres", "id_usuario": id_usuario, "asunto": asunto, "cuerpo": cuerpo, "tipo": tipo, "prioridad": prioridad, "fecha_evento": now_iso()}
    set_request(request_id, {"estado": "RECIBIDO", "source": "postgres", "id_usuario": id_usuario, "asunto": asunto})

    rabbit.publish("q.postgres.persist_email", base)
    ack_pg = wait_response(request_id, "postgres.persisted", timeout=20)
    if not ack_pg.get("ok"):
        raise RuntimeError("PostgreSQL no confirmo persistencia: " + json_dumps(ack_pg))

    rabbit.publish("q.mariadb.query_profile", {"request_id": request_id, "id_usuario": id_usuario})
    profile_msg = wait_response(request_id, "mariadb.profile", timeout=20)
    if not profile_msg.get("ok"):
        raise RuntimeError("MariaDB no devolvio perfil: " + json_dumps(profile_msg))
    profile = profile_msg.get("profile", {})

    eval_res = compute_risk({**base, "contenido": asunto + " " + cuerpo, "monto": payload.get("monto")}, profile)
    intent = detect_intent(asunto + " " + cuerpo, tipo)
    final_payload = {
        "request_id": request_id,
        "source": "postgres",
        "id_origen": ack_pg.get("id_origen"),
        "id_usuario": id_usuario,
        "intencion": intent,
        "monto": eval_res["monto"],
        "decision": eval_res["decision"],
        "riesgo": eval_res["riesgo"],
        "motivo": eval_res["motivo"],
    }
    rabbit.publish("q.mariadb.persist_decision", final_payload)
    wait_response(request_id, "mariadb.decision_saved", timeout=10)
    rabbit.publish("q.postgres.update_result", final_payload)
    wait_response(request_id, "postgres.updated", timeout=10)
    result = {**final_payload, "estado": "PROCESADO", "profile": profile}
    set_request(request_id, result)
    metrics["processed"] += 1
    return result


class Handler(BaseHTTPRequestHandler):
    def log_message(self, fmt, *args):
        print("[business-api]", self.address_string(), fmt % args, flush=True)

    def cors(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET,POST,OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")

    def send_json(self, code, obj):
        raw = json_dumps(obj).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(raw)))
        self.cors()
        self.end_headers()
        self.wfile.write(raw)

    def do_OPTIONS(self):
        self.send_response(204)
        self.cors()
        self.end_headers()

    def do_GET(self):
        if self.path == "/api/health":
            self.send_json(200, {"ok": True, "node": "PC1 Business API", "rabbit_host": RABBIT_HOST, "time": now_iso()})
        elif self.path == "/api/metrics":
            self.send_json(200, metrics)
        elif self.path == "/api/requests":
            with state_lock:
                data = list(requests_state.values())[-50:]
            self.send_json(200, {"items": data})
        elif self.path.startswith("/api/requests/"):
            rid = self.path.rsplit("/", 1)[-1]
            with state_lock:
                item = requests_state.get(rid)
            self.send_json(200 if item else 404, item or {"ok": False, "error": "request_id no encontrado"})
        elif self.path == "/api/config":
            self.send_json(200, {"pc1_api": "8080", "rabbit_mgmt": "15672", "queues": QUEUES})
        else:
            self.send_json(404, {"ok": False, "error": "endpoint no encontrado"})

    def do_POST(self):
        metrics["received_http"] += 1
        try:
            payload = read_body(self)
            if self.path == "/api/solicitud":
                metrics["sms_requests"] += 1
                result = process_sms(payload)
                self.send_json(200, {"ok": True, "result": result})
            elif self.path == "/api/correo":
                metrics["email_requests"] += 1
                result = process_email(payload)
                self.send_json(200, {"ok": True, "result": result})
            elif self.path == "/api/demo":
                demo = {"id_usuario": 1, "canal": "WhatsApp", "tipo": "solicitud", "contenido": "Solicito un prestamo de 5500 soles, tengo sueldo estable"}
                result = process_sms(demo)
                self.send_json(200, {"ok": True, "result": result})
            else:
                self.send_json(404, {"ok": False, "error": "endpoint no encontrado"})
        except Exception as exc:
            metrics["errors"] += 1
            self.send_json(500, {"ok": False, "error": str(exc)})


def main():
    load_state()
    ensure_queues()
    server = ThreadingHTTPServer(("0.0.0.0", API_PORT), Handler)
    print(f"[business-api] Escuchando en 0.0.0.0:{API_PORT}", flush=True)
    server.serve_forever()


if __name__ == "__main__":
    main()
