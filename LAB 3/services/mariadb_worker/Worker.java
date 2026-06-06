import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Worker {
    static String DB_HOST = getenv("DB_HOST", "mariadb");
    static String DB_NAME = getenv("DB_NAME", "prestamos_finanzas");
    static String DB_USER = getenv("DB_USER", "appuser");
    static String DB_PASSWORD = getenv("DB_PASSWORD", "apppass");
    static String RABBIT_HOST = getenv("RABBIT_HOST", "192.168.0.137");
    static int RABBIT_PORT = Integer.parseInt(getenv("RABBIT_PORT", "15672"));
    static String RABBIT_USER = getenv("RABBIT_USER", "admin");
    static String RABBIT_PASS = getenv("RABBIT_PASS", "adminpass");
    static int HTTP_PORT = Integer.parseInt(getenv("HTTP_PORT", "8003"));
    static AtomicInteger profiles = new AtomicInteger(0);
    static AtomicInteger decisions = new AtomicInteger(0);
    static AtomicInteger errors = new AtomicInteger(0);
    static Rabbit rabbit = new Rabbit(RABBIT_HOST, RABBIT_PORT, RABBIT_USER, RABBIT_PASS);

    static String getenv(String k, String d) { String v = System.getenv(k); return v == null || v.isBlank() ? d : v; }
    static String esc(String s) { return s == null ? "" : s.replace("\\", "\\\\").replace("'", "''"); }
    static String jesc(String s) { return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", ""); }
    static String jsonPair(String k, String v) { return "\"" + k + "\":\"" + jesc(v) + "\""; }
    static String field(String json, String key) {
        String needle = "\"" + key + "\"";
        int i = json.indexOf(needle); if (i < 0) return "";
        int c = json.indexOf(':', i + needle.length()); if (c < 0) return "";
        int p = c + 1; while (p < json.length() && Character.isWhitespace(json.charAt(p))) p++;
        if (p < json.length() && json.charAt(p) == '"') {
            StringBuilder sb = new StringBuilder(); p++;
            boolean esc = false;
            while (p < json.length()) {
                char ch = json.charAt(p++);
                if (esc) { if (ch == 'n') sb.append('\n'); else sb.append(ch); esc = false; }
                else if (ch == '\\') esc = true;
                else if (ch == '"') break;
                else sb.append(ch);
            }
            return sb.toString();
        } else {
            int e = p; while (e < json.length() && ",}\n\r\t ".indexOf(json.charAt(e)) < 0) e++;
            return json.substring(p, e).trim();
        }
    }
    static String db(String sql) throws Exception {
        List<String> cmd = Arrays.asList("mariadb", "--default-character-set=utf8mb4", "-h", DB_HOST, "-u" + DB_USER, "-p" + DB_PASSWORD, DB_NAME, "-N", "-B", "-e", sql);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        Process p = pb.start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String err = new String(p.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        int code = p.waitFor();
        if (code != 0) throw new RuntimeException(err);
        return out.trim();
    }
    static void waitDb() throws Exception {
        while (true) {
            try { db("SELECT 1;"); System.out.println("[mariadb-worker] MariaDB disponible"); return; }
            catch (Exception e) { System.out.println("[mariadb-worker] Esperando MariaDB: " + e.getMessage()); Thread.sleep(2000); }
        }
    }
    static void queryProfile(String msg) throws Exception {
        String requestId = field(msg, "request_id");
        int idUsuario = Integer.parseInt(field(msg, "id_usuario"));
        String q = "SELECT u.id_usuario,u.nombre," +
            "COALESCE((SELECT SUM(saldo) FROM cuentas c WHERE c.id_usuario=u.id_usuario),0)," +
            "COALESCE((SELECT COUNT(*) FROM prestamos p WHERE p.id_usuario=u.id_usuario AND p.estado='ACTIVO'),0)," +
            "COALESCE((SELECT SUM(monto) FROM prestamos p WHERE p.id_usuario=u.id_usuario AND p.estado='ACTIVO'),0)," +
            "COALESCE((SELECT SUM(cuotas_no_pagadas) FROM prestamos p WHERE p.id_usuario=u.id_usuario AND p.estado='ACTIVO'),0)," +
            "COALESCE((SELECT MAX(dias_mora) FROM prestamos p WHERE p.id_usuario=u.id_usuario AND p.estado='ACTIVO'),0) " +
            "FROM usuarios u WHERE u.id_usuario=" + idUsuario + " LIMIT 1;";
        String out = db(q);
        if (out.isBlank()) {
            rabbit.publish("q.response.pc1", "{\"ok\":false,\"action\":\"mariadb.profile\",\"request_id\":\"" + jesc(requestId) + "\",\"error\":\"usuario no encontrado\"}");
            return;
        }
        String[] a = out.split("\\t", -1);
        String profile = "{"
            + jsonPair("id_usuario", a[0]) + ","
            + jsonPair("nombre", a.length > 1 ? a[1] : "") + ","
            + jsonPair("saldo_total", a.length > 2 ? a[2] : "0") + ","
            + jsonPair("prestamos_activos", a.length > 3 ? a[3] : "0") + ","
            + jsonPair("deuda_total", a.length > 4 ? a[4] : "0") + ","
            + jsonPair("cuotas_vencidas", a.length > 5 ? a[5] : "0") + ","
            + jsonPair("dias_mora_max", a.length > 6 ? a[6] : "0")
            + "}";
        profiles.incrementAndGet();
        rabbit.publish("q.response.pc1", "{\"ok\":true,\"action\":\"mariadb.profile\",\"request_id\":\"" + jesc(requestId) + "\",\"profile\":" + profile + "}");
    }
    static void saveDecision(String msg) throws Exception {
        String requestId = field(msg, "request_id");
        String source = field(msg, "source");
        String idOrigen = field(msg, "id_origen"); if (idOrigen.isBlank()) idOrigen = "0";
        String idUsuario = field(msg, "id_usuario");
        String intencion = field(msg, "intencion");
        String monto = field(msg, "monto"); if (monto.isBlank()) monto = "0";
        String decision = field(msg, "decision");
        String riesgo = field(msg, "riesgo"); if (riesgo.isBlank()) riesgo = "0";
        String motivo = field(msg, "motivo");
        String q = "INSERT INTO decisiones_prestamo(request_id,source,id_origen,id_usuario,intencion,monto_solicitado,decision,riesgo,motivo) VALUES('" + esc(requestId) + "','" + esc(source) + "'," + idOrigen + "," + idUsuario + ",'" + esc(intencion) + "'," + monto + ",'" + esc(decision) + "'," + riesgo + ",'" + esc(motivo) + "'); SELECT LAST_INSERT_ID();";
        String out = db(q);
        String idDecision = out.lines().reduce((first, second) -> second).orElse("0");
        decisions.incrementAndGet();
        rabbit.publish("q.response.pc1", "{\"ok\":true,\"action\":\"mariadb.decision_saved\",\"request_id\":\"" + jesc(requestId) + "\",\"id_decision\":" + idDecision + "}");
    }
    static void loop() throws Exception {
        waitDb(); rabbit.waitOverview();
        for (String q : new String[]{"q.mariadb.query_profile", "q.mariadb.persist_decision", "q.response.pc1"}) rabbit.ensureQueue(q);
        while (true) {
            try {
                boolean did = false;
                String msg = rabbit.getOne("q.mariadb.query_profile");
                if (msg != null) { did = true; queryProfile(msg); }
                msg = rabbit.getOne("q.mariadb.persist_decision");
                if (msg != null) { did = true; saveDecision(msg); }
                if (!did) Thread.sleep(250);
            } catch (Exception e) { errors.incrementAndGet(); System.out.println("[mariadb-worker] ERROR: " + e.getMessage()); Thread.sleep(1000); }
        }
    }
    static void startHttp() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", HTTP_PORT), 0);
        server.createContext("/health", ex -> {
            String body = "{\"node\":\"PC4 MariaDB Worker Java\",\"profiles\":" + profiles.get() + ",\"decisions\":" + decisions.get() + ",\"errors\":" + errors.get() + ",\"time\":\"" + Instant.now().toString() + "\"}";
            ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            byte[] b = body.getBytes(StandardCharsets.UTF_8); ex.sendResponseHeaders(200, b.length); ex.getResponseBody().write(b); ex.close();
        });
        server.createContext("/recent", ex -> {
            String body;
            try { body = "{\"rows\":\"" + jesc(db("SELECT id_decision,request_id,source,id_usuario,decision,riesgo FROM decisiones_prestamo ORDER BY id_decision DESC LIMIT 10;")) + "\"}"; }
            catch (Exception e) { body = "{\"ok\":false,\"error\":\"" + jesc(e.getMessage()) + "\"}"; }
            ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8"); ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*"); byte[] b = body.getBytes(StandardCharsets.UTF_8); ex.sendResponseHeaders(200, b.length); ex.getResponseBody().write(b); ex.close();
        });
        server.setExecutor(Executors.newCachedThreadPool()); server.start();
        System.out.println("[mariadb-worker] HTTP en " + HTTP_PORT);
    }
    public static void main(String[] args) throws Exception { startHttp(); loop(); }

    static class Rabbit {
        String base, auth;
        HttpClient client = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).build();
        Rabbit(String host, int port, String user, String pass) {
            base = "http://" + host + ":" + port + "/api";
            auth = "Basic " + Base64.getEncoder().encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));
        }
        String call(String method, String path, String body) throws Exception {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path)).timeout(java.time.Duration.ofSeconds(5)).header("Authorization", auth).header("Content-Type", "application/json");
            if (body == null) b.method(method, HttpRequest.BodyPublishers.noBody()); else b.method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            HttpResponse<String> r = client.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (r.statusCode() >= 300) throw new RuntimeException("HTTP " + r.statusCode() + " " + r.body());
            return r.body();
        }
        void waitOverview() throws Exception { while (true) { try { call("GET", "/overview", null); System.out.println("[mariadb-worker] RabbitMQ disponible"); return; } catch (Exception e) { System.out.println("[mariadb-worker] Esperando RabbitMQ: " + e.getMessage()); Thread.sleep(2000); } } }
        void ensureQueue(String q) throws Exception { call("PUT", "/queues/%2f/" + URLEncoder.encode(q, StandardCharsets.UTF_8), "{\"durable\":true,\"auto_delete\":false,\"arguments\":{}}"); }
        void publish(String q, String msgJson) throws Exception { String body = "{\"properties\":{\"delivery_mode\":2,\"content_type\":\"application/json\"},\"routing_key\":\"" + jesc(q) + "\",\"payload\":\"" + jesc(msgJson) + "\",\"payload_encoding\":\"string\"}"; call("POST", "/exchanges/%2f/amq.default/publish", body); }
        String getOne(String q) throws Exception { String r = call("POST", "/queues/%2f/" + URLEncoder.encode(q, StandardCharsets.UTF_8) + "/get", "{\"count\":1,\"ackmode\":\"ack_requeue_false\",\"encoding\":\"auto\",\"truncate\":100000}"); if (r.equals("[]") || r.isBlank()) return null; String p = field(r, "payload"); return p.isBlank() ? null : p; }
    }
}
