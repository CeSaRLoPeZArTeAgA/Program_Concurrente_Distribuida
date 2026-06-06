const http = require('http');
const { execFileSync } = require('child_process');

const DB_HOST = process.env.DB_HOST || 'postgres';
const DB_NAME = process.env.DB_NAME || 'prestamos_correo';
const DB_USER = process.env.DB_USER || 'appuser';
const DB_PASSWORD = process.env.DB_PASSWORD || 'apppass';
const RABBIT_HOST = process.env.RABBIT_HOST || '192.168.0.137';
const RABBIT_PORT = Number(process.env.RABBIT_PORT || 15672);
const RABBIT_USER = process.env.RABBIT_USER || 'admin';
const RABBIT_PASS = process.env.RABBIT_PASS || 'adminpass';
const HTTP_PORT = Number(process.env.HTTP_PORT || 8002);

const metrics = { node: 'PC3 PostgreSQL Worker Node.js', persisted: 0, updated: 0, errors: 0, rabbit_errors: 0, start_time: new Date().toISOString() };
function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }
function sql(s) { return String(s ?? '').replaceAll("'", "''"); }

class Rabbit {
  constructor() {
    this.base = `http://${RABBIT_HOST}:${RABBIT_PORT}/api`;
    this.auth = 'Basic ' + Buffer.from(`${RABBIT_USER}:${RABBIT_PASS}`).toString('base64');
  }
  async call(method, path, payload, timeoutMs = 5000) {
    const ctrl = new AbortController();
    const t = setTimeout(() => ctrl.abort(), timeoutMs);
    try {
      const res = await fetch(this.base + path, { method, headers: { 'Authorization': this.auth, 'Content-Type': 'application/json' }, body: payload ? JSON.stringify(payload) : undefined, signal: ctrl.signal });
      const txt = await res.text();
      if (!res.ok) throw new Error(`${res.status} ${txt}`);
      return txt ? JSON.parse(txt) : {};
    } finally { clearTimeout(t); }
  }
  async wait() {
    while (true) {
      try { await this.call('GET', '/overview', null, 3000); console.log('[postgres-worker] RabbitMQ disponible'); return; }
      catch (e) { console.log('[postgres-worker] Esperando RabbitMQ:', e.message); await sleep(2000); }
    }
  }
  async ensureQueue(q) { return this.call('PUT', '/queues/%2f/' + encodeURIComponent(q), { durable: true, auto_delete: false, arguments: {} }); }
  async publish(q, msg) {
    return this.call('POST', '/exchanges/%2f/amq.default/publish', { properties: { delivery_mode: 2, content_type: 'application/json' }, routing_key: q, payload: JSON.stringify(msg), payload_encoding: 'string' });
  }
  async getOne(q) {
    const data = await this.call('POST', '/queues/%2f/' + encodeURIComponent(q) + '/get', { count: 1, ackmode: 'ack_requeue_false', encoding: 'auto', truncate: 100000 });
    if (!data || data.length === 0) return null;
    return JSON.parse(data[0].payload || '{}');
  }
}
const rabbit = new Rabbit();

function psql(sqlText) {
  const env = { ...process.env, PGPASSWORD: DB_PASSWORD };
  const args = ['-h', DB_HOST, '-U', DB_USER, '-d', DB_NAME, '-t', '-A', '-c', sqlText];
  return execFileSync('psql', args, { env, encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] }).trim();
}
async function waitPostgres() {
  while (true) {
    try { psql('SELECT 1;'); console.log('[postgres-worker] PostgreSQL disponible'); return; }
    catch (e) { console.log('[postgres-worker] Esperando PostgreSQL:', String(e.stderr || e.message).trim()); await sleep(2000); }
  }
}
async function persistEmail(msg) {
  const q = `INSERT INTO mensajes_correo(request_id,id_usuario,asunto,cuerpo,tipo_correo,prioridad,estado_proceso)
VALUES('${sql(msg.request_id)}',${Number(msg.id_usuario)},'${sql(msg.asunto)}','${sql(msg.cuerpo)}','${sql(msg.tipo || 'solicitud')}','${sql(msg.prioridad || 'media')}','RECIBIDO')
ON CONFLICT (request_id) DO UPDATE SET asunto=EXCLUDED.asunto, cuerpo=EXCLUDED.cuerpo, estado_proceso='RECIBIDO'
RETURNING id_correo;`;
  const id = Number(psql(q).split(/\r?\n/).pop());
  metrics.persisted++;
  await rabbit.publish('q.response.pc1', { ok: true, action: 'postgres.persisted', request_id: msg.request_id, id_origen: id });
}
async function updateResult(msg) {
  const q = `UPDATE mensajes_correo SET estado_proceso='PROCESADO', decision='${sql(msg.decision)}', riesgo_calculado=${Number(msg.riesgo || 0)}, respuesta_ia='${sql(msg.motivo)}' WHERE request_id='${sql(msg.request_id)}';`;
  psql(q); metrics.updated++;
  await rabbit.publish('q.response.pc1', { ok: true, action: 'postgres.updated', request_id: msg.request_id });
}
async function workerLoop() {
  await waitPostgres(); await rabbit.wait();
  for (const q of ['q.postgres.persist_email', 'q.postgres.update_result', 'q.response.pc1']) await rabbit.ensureQueue(q);
  while (true) {
    try {
      let did = false;
      let msg = await rabbit.getOne('q.postgres.persist_email');
      if (msg) { did = true; await persistEmail(msg); }
      msg = await rabbit.getOne('q.postgres.update_result');
      if (msg) { did = true; await updateResult(msg); }
      if (!did) await sleep(250);
    } catch (e) { metrics.errors++; console.log('[postgres-worker] ERROR:', e.message); await sleep(1000); }
  }
}

http.createServer((req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  if (req.url === '/health') { res.setHeader('Content-Type', 'application/json'); res.end(JSON.stringify(metrics)); }
  else if (req.url === '/recent') {
    try { const out = psql("SELECT id_correo,request_id,id_usuario,estado_proceso,COALESCE(decision,''),COALESCE(riesgo_calculado,0) FROM mensajes_correo ORDER BY id_correo DESC LIMIT 10;"); res.setHeader('Content-Type', 'application/json'); res.end(JSON.stringify({ rows: out.split(/\r?\n/).filter(Boolean) })); }
    catch (e) { res.statusCode = 500; res.end(JSON.stringify({ ok: false, error: e.message })); }
  }
  else { res.statusCode = 404; res.end(JSON.stringify({ ok: false, error: 'not found' })); }
}).listen(HTTP_PORT, '0.0.0.0', () => console.log(`[postgres-worker] HTTP en ${HTTP_PORT}`));

workerLoop();
