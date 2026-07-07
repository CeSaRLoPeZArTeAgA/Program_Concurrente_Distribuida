package cc4p1.raft;

import cc4p1.common.Args;
import cc4p1.common.Tcp;
import cc4p1.common.Wire;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Nodo Raft didactico para CC4P1.
 *
 * Responsabilidad:
 * - Elegir lider por mayoria.
 * - Recibir eventos de deteccion ya generados por YOLO.
 * - Replicar eventos a los demas nodos.
 * - Hacer commit cuando existe mayoria.
 * - Persistir registros confirmados en CSV e imagenes.
 *
 * Nota: es una implementacion simplificada de Raft, suficiente para laboratorio.
 * No implementa snapshots formales ni reparacion avanzada de logs divergentes.
 */
public final class RaftNode {
    enum Role { FOLLOWER, CANDIDATE, LEADER }

    private String id;
    private String advertiseHost;
    private String bindHost;
    private int clientPort;
    private int raftPort;

    private File dataDir;
    private File imageDir;
    private File logFile;
    private File stateFile;
    private File commitFile;
    private File csvFile;

    private final List<Peer> peers = new ArrayList<Peer>();
    private final List<LogEntry> log = new ArrayList<LogEntry>();
    private final Set<Long> appliedIndexes = new HashSet<Long>();
    private final CopyOnWriteArrayList<PrintWriter> subscribers = new CopyOnWriteArrayList<PrintWriter>();

    private volatile Role role = Role.FOLLOWER;
    private volatile long currentTerm = 0;
    private volatile String votedFor = null;
    private volatile String leaderId = null;
    private volatile String leaderHost = null;
    private volatile int leaderClientPort = -1;
    private volatile long lastHeartbeatMs = System.currentTimeMillis();
    private volatile long commitIndex = 0;
    private final Random rnd = new Random();

    public static void main(String[] args) throws Exception {
        new RaftNode().start(args);
    }

    private void start(String[] raw) throws Exception {
        Args a = new Args(raw);
        id = a.get("id", "n1");
        advertiseHost = a.get("host", "127.0.0.1");
        bindHost = a.get("bind", "0.0.0.0");
        clientPort = a.getInt("client-port", 7001);
        raftPort = a.getInt("raft-port", 7101);

        dataDir = new File(a.get("data", "data/" + id));
        imageDir = new File(dataDir, "images");
        logFile = new File(dataDir, "raft.log");
        stateFile = new File(dataDir, "state.properties");
        commitFile = new File(dataDir, "commit.index");
        csvFile = new File(dataDir, "events.csv");
        dataDir.mkdirs();
        imageDir.mkdirs();

        String peersRaw = a.get("peers", "").trim();
        if (!peersRaw.isEmpty()) {
            for (String p : peersRaw.split(",")) {
                if (!p.trim().isEmpty()) peers.add(Peer.parse(p.trim()));
            }
        }

        loadPersistentState();
        loadCommitIndex();
        ensureCsvHeader();
        loadAppliedIndexesFromCsv();
        loadLog();
        applyCommitted();

        Tcp.serve(bindHost, raftPort, new Tcp.Handler() {
            public void handle(Socket s) throws Exception { handleRaft(s); }
        }, "raft-" + id);

        Tcp.serve(bindHost, clientPort, new Tcp.Handler() {
            public void handle(Socket s) throws Exception { handleClient(s); }
        }, "client-" + id);

        startElectionLoop();
        startHeartbeatLoop();

        Tcp.log("Nodo " + id + " iniciado. hostPublico=" + advertiseHost + " clientPort=" + clientPort + " raftPort=" + raftPort + " peers=" + peers);
        while (true) Thread.sleep(60_000L);
    }

    private synchronized void loadPersistentState() {
        if (!stateFile.exists()) return;
        try {
            List<String> lines = Files.readAllLines(stateFile.toPath(), StandardCharsets.UTF_8);
            for (String l : lines) {
                if (l.startsWith("term=")) currentTerm = Long.parseLong(l.substring(5).trim());
                if (l.startsWith("votedFor=")) {
                    String v = l.substring(9).trim();
                    votedFor = v.isEmpty() ? null : v;
                }
            }
        } catch (Exception e) {
            Tcp.log(id + " no pudo leer estado: " + e.getMessage());
        }
    }

    private synchronized void savePersistentState() {
        try {
            FileWriter fw = new FileWriter(stateFile, false);
            fw.write("term=" + currentTerm + "\n");
            fw.write("votedFor=" + (votedFor == null ? "" : votedFor) + "\n");
            fw.close();
        } catch (Exception e) {
            Tcp.log(id + " no pudo guardar estado: " + e.getMessage());
        }
    }

    private synchronized void loadCommitIndex() {
        if (!commitFile.exists()) return;
        try {
            String s = new String(Files.readAllBytes(commitFile.toPath()), StandardCharsets.UTF_8).trim();
            if (!s.isEmpty()) commitIndex = Long.parseLong(s);
        } catch (Exception e) {
            Tcp.log(id + " no pudo leer commit.index: " + e.getMessage());
        }
    }

    private synchronized void saveCommitIndex() {
        try {
            FileWriter fw = new FileWriter(commitFile, false);
            fw.write(String.valueOf(commitIndex));
            fw.write("\n");
            fw.close();
        } catch (Exception e) {
            Tcp.log(id + " no pudo guardar commit.index: " + e.getMessage());
        }
    }

    private synchronized void ensureCsvHeader() {
        try {
            if (!csvFile.exists() || csvFile.length() == 0) {
                FileWriter fw = new FileWriter(csvFile, true);
                fw.write("index,term,eventId,timestamp,camera,label,confidence,bbox,image,node\n");
                fw.close();
            }
        } catch (Exception e) {
            Tcp.log(id + " no pudo crear events.csv: " + e.getMessage());
        }
    }

    private synchronized void loadAppliedIndexesFromCsv() {
        if (!csvFile.exists()) return;
        try {
            BufferedReader br = new BufferedReader(new FileReader(csvFile));
            String line;
            boolean header = true;
            while ((line = br.readLine()) != null) {
                if (header) { header = false; continue; }
                String[] p = line.split(",", 2);
                if (p.length > 0 && !p[0].trim().isEmpty()) appliedIndexes.add(Long.parseLong(p[0].trim()));
            }
            br.close();
        } catch (Exception e) {
            Tcp.log(id + " no pudo cargar indices aplicados: " + e.getMessage());
        }
    }

    private synchronized void loadLog() {
        if (!logFile.exists()) return;
        try {
            BufferedReader br = new BufferedReader(new FileReader(logFile));
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split("\\|", 3);
                if (p.length == 3) {
                    log.add(new LogEntry(Long.parseLong(p[0]), Long.parseLong(p[1]), p[2]));
                }
            }
            br.close();
            Collections.sort(log, new Comparator<LogEntry>() {
                public int compare(LogEntry a, LogEntry b) { return Long.compare(a.index, b.index); }
            });
        } catch (Exception e) {
            Tcp.log(id + " no pudo cargar raft.log: " + e.getMessage());
        }
    }

    private synchronized long lastIndex() { return log.isEmpty() ? 0 : log.get(log.size() - 1).index; }
    private synchronized long lastTerm() { return log.isEmpty() ? 0 : log.get(log.size() - 1).term; }
    private int majority() { return (peers.size() + 1) / 2 + 1; }

    private void handleRaft(Socket s) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"));
        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8")), true);
        String line = in.readLine();
        String type = Wire.type(line);
        Map<String, String> m = Wire.parse(line);
        if ("VOTE_REQUEST".equals(type)) out.println(onVoteRequest(m));
        else if ("APPEND".equals(type)) out.println(onAppend(m));
        else if ("PING".equals(type)) out.println(Wire.line("PONG", Wire.kv("id", id, "term", String.valueOf(currentTerm), "role", role.name())));
        else out.println(Wire.line("ERROR", Wire.kv("msg", "tipo raft no soportado: " + type)));
    }

    private synchronized String onVoteRequest(Map<String, String> m) {
        long term = Wire.getLong(m, "term", 0);
        String candidate = Wire.get(m, "candidate", "");
        long candLastIndex = Wire.getLong(m, "lastIndex", 0);
        long candLastTerm = Wire.getLong(m, "lastTerm", 0);

        if (term > currentTerm) stepDown(term, null, null, -1);

        boolean candidateUpToDate = candLastTerm > lastTerm() || (candLastTerm == lastTerm() && candLastIndex >= lastIndex());
        boolean grant = term == currentTerm && candidateUpToDate && (votedFor == null || votedFor.equals(candidate));

        if (grant) {
            votedFor = candidate;
            lastHeartbeatMs = System.currentTimeMillis();
            savePersistentState();
        }
        return Wire.line("VOTE_RESPONSE", Wire.kv("term", String.valueOf(currentTerm), "voteGranted", String.valueOf(grant), "from", id));
    }

    private synchronized String onAppend(Map<String, String> m) {
        long term = Wire.getLong(m, "term", 0);
        String leader = Wire.get(m, "leader", "");
        if (term < currentTerm) {
            return Wire.line("APPEND_RESPONSE", Wire.kv("term", String.valueOf(currentTerm), "success", "false", "from", id));
        }

        String lh = Wire.get(m, "leaderHost", "");
        int lcp = Wire.getInt(m, "leaderClientPort", -1);
        if (term > currentTerm || role != Role.FOLLOWER || !leader.equals(leaderId)) {
            stepDown(term, leader, lh, lcp);
        }
        leaderId = leader;
        leaderHost = lh;
        leaderClientPort = lcp;
        lastHeartbeatMs = System.currentTimeMillis();

        long entryIndex = Wire.getLong(m, "entryIndex", 0);
        if (entryIndex > 0) {
            long entryTerm = Wire.getLong(m, "entryTerm", term);
            String payload = Wire.get(m, "payload", "");
            appendOrReplace(new LogEntry(entryIndex, entryTerm, payload));
        }

        long leaderCommit = Wire.getLong(m, "commit", commitIndex);
        if (leaderCommit > commitIndex) {
            commitIndex = Math.min(leaderCommit, lastIndex());
            saveCommitIndex();
            applyCommitted();
        }

        return Wire.line("APPEND_RESPONSE", Wire.kv(
                "term", String.valueOf(currentTerm),
                "success", "true",
                "from", id,
                "matchIndex", String.valueOf(lastIndex())));
    }

    private synchronized void appendOrReplace(LogEntry newEntry) {
        for (int i = 0; i < log.size(); i++) {
            LogEntry old = log.get(i);
            if (old.index == newEntry.index) {
                if (old.term == newEntry.term && old.payload.equals(newEntry.payload)) return;
                while (log.size() > i) log.remove(log.size() - 1);
                rewriteLogFile();
                break;
            }
        }
        log.add(newEntry);
        persistLogEntry(newEntry);
    }

    private synchronized void rewriteLogFile() {
        try {
            FileWriter fw = new FileWriter(logFile, false);
            for (LogEntry e : log) fw.write(e.index + "|" + e.term + "|" + e.payload + "\n");
            fw.close();
        } catch (Exception e) {
            Tcp.log(id + " no pudo reescribir log: " + e.getMessage());
        }
    }

    private synchronized void stepDown(long term, String newLeader, String newLeaderHost, int newLeaderClientPort) {
        currentTerm = term;
        role = Role.FOLLOWER;
        votedFor = null;
        leaderId = newLeader;
        leaderHost = newLeaderHost;
        leaderClientPort = newLeaderClientPort;
        savePersistentState();
    }

    private void handleClient(Socket s) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"));
        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8")), true);
        String line = in.readLine();
        String type = Wire.type(line);
        Map<String, String> m = Wire.parse(line);

        if ("EVENT".equals(type)) out.println(handleEvent(line));
        else if ("STATUS".equals(type)) out.println(statusLine());
        else if ("SNAPSHOT".equals(type)) sendSnapshot(out);
        else if ("GET_IMAGE".equals(type)) sendImage(out, m);
        else if ("SUBSCRIBE".equals(type)) subscribe(s, out);
        else out.println(Wire.line("ERROR", Wire.kv("msg", "tipo cliente no soportado: " + type)));
    }

    private String statusLine() {
        return Wire.line("STATUS_RESPONSE", Wire.kv(
                "id", id,
                "term", String.valueOf(currentTerm),
                "role", role.name(),
                "leaderId", nvl(leaderId),
                "leaderHost", nvl(leaderHost),
                "leaderClientPort", String.valueOf(leaderClientPort),
                "clientPort", String.valueOf(clientPort),
                "raftPort", String.valueOf(raftPort),
                "lastIndex", String.valueOf(lastIndex()),
                "commitIndex", String.valueOf(commitIndex)));
    }

    private String handleEvent(String eventLine) {
        if (role != Role.LEADER) {
            String lh = leaderHost;
            int lp = leaderClientPort;
            if (lh != null && lh.length() > 0 && lp > 0) {
                try { return Tcp.request(lh, lp, eventLine, 15000); }
                catch (Exception e) {
                    return Wire.line("ERROR", Wire.kv("msg", "no soy lider y no pude reenviar: " + e.getMessage(), "leaderId", nvl(leaderId), "leaderHost", nvl(leaderHost), "leaderClientPort", String.valueOf(leaderClientPort)));
                }
            }
            return Wire.line("NOT_LEADER", Wire.kv("leaderId", nvl(leaderId), "leaderHost", nvl(leaderHost), "leaderClientPort", String.valueOf(leaderClientPort)));
        }

        LogEntry e;
        synchronized (this) {
            String payload = Base64.getEncoder().encodeToString(eventLine.getBytes(StandardCharsets.UTF_8));
            e = new LogEntry(lastIndex() + 1, currentTerm, payload);
            log.add(e);
            persistLogEntry(e);
        }

        int ok = 1;
        for (Peer p : peers) if (replicate(p, e)) ok++;

        if (ok >= majority()) {
            synchronized (this) {
                commitIndex = e.index;
                saveCommitIndex();
                applyCommitted();
            }
            broadcastCommitToPeers();
            return Wire.line("OK", Wire.kv("committed", "true", "index", String.valueOf(e.index), "acks", String.valueOf(ok), "leader", id));
        }

        return Wire.line("ERROR", Wire.kv("committed", "false", "index", String.valueOf(e.index), "acks", String.valueOf(ok), "msg", "sin mayoria"));
    }

    private boolean replicate(Peer p, LogEntry e) {
        Map<String, String> msg = new LinkedHashMap<String, String>();
        msg.put("term", String.valueOf(currentTerm));
        msg.put("leader", id);
        msg.put("leaderHost", advertiseHost);
        msg.put("leaderClientPort", String.valueOf(clientPort));
        msg.put("commit", String.valueOf(commitIndex));
        msg.put("entryIndex", String.valueOf(e.index));
        msg.put("entryTerm", String.valueOf(e.term));
        msg.put("payload", e.payload);
        try {
            String resp = Tcp.request(p.host, p.raftPort, Wire.line("APPEND", msg), 5000);
            Map<String, String> r = Wire.parse(resp);
            long rt = Wire.getLong(r, "term", currentTerm);
            if (rt > currentTerm) {
                synchronized (this) { stepDown(rt, null, null, -1); }
                return false;
            }
            return "true".equals(Wire.get(r, "success", "false"));
        } catch (Exception ex) {
            Tcp.log(id + " no replica en " + p + ": " + ex.getMessage());
            return false;
        }
    }

    private void broadcastCommitToPeers() {
        for (Peer p : peers) sendHeartbeat(p);
    }

    private void startElectionLoop() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(250L);
                        if (role == Role.LEADER) continue;
                        long elapsed = System.currentTimeMillis() - lastHeartbeatMs;
                        long timeout = 1500L + rnd.nextInt(1500);
                        if (elapsed > timeout) startElection();
                    } catch (Exception e) {
                        Tcp.log(id + " electionLoop: " + e.getMessage());
                    }
                }
            }
        }, "election-" + id);
        t.setDaemon(true);
        t.start();
    }

    private void startHeartbeatLoop() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(600L);
                        if (role == Role.LEADER) for (Peer p : peers) sendHeartbeat(p);
                    } catch (Exception e) {
                        Tcp.log(id + " heartbeatLoop: " + e.getMessage());
                    }
                }
            }
        }, "heartbeat-" + id);
        t.setDaemon(true);
        t.start();
    }

    private void startElection() {
        long term;
        long li;
        long lt;
        synchronized (this) {
            role = Role.CANDIDATE;
            currentTerm++;
            votedFor = id;
            leaderId = null;
            leaderHost = null;
            leaderClientPort = -1;
            lastHeartbeatMs = System.currentTimeMillis();
            savePersistentState();
            term = currentTerm;
            li = lastIndex();
            lt = lastTerm();
        }

        int votes = 1;
        Tcp.log(id + " inicia eleccion term=" + term);
        for (Peer p : peers) {
            Map<String, String> msg = Wire.kv(
                    "term", String.valueOf(term),
                    "candidate", id,
                    "lastIndex", String.valueOf(li),
                    "lastTerm", String.valueOf(lt));
            try {
                String resp = Tcp.request(p.host, p.raftPort, Wire.line("VOTE_REQUEST", msg), 3000);
                Map<String, String> r = Wire.parse(resp);
                long rt = Wire.getLong(r, "term", term);
                synchronized (this) {
                    if (rt > currentTerm) { stepDown(rt, null, null, -1); return; }
                }
                if ("true".equals(Wire.get(r, "voteGranted", "false"))) votes++;
            } catch (Exception e) {
                Tcp.log(id + " no obtiene voto de " + p + ": " + e.getMessage());
            }
        }

        synchronized (this) {
            if (role == Role.CANDIDATE && currentTerm == term && votes >= majority()) {
                role = Role.LEADER;
                leaderId = id;
                leaderHost = advertiseHost;
                leaderClientPort = clientPort;
                lastHeartbeatMs = System.currentTimeMillis();
                Tcp.log(id + " ES LIDER term=" + term + " votos=" + votes + "/" + (peers.size() + 1));
                for (Peer p : peers) sendHeartbeat(p);
            }
        }
    }

    private void sendHeartbeat(Peer p) {
        Map<String, String> msg = Wire.kv(
                "term", String.valueOf(currentTerm),
                "leader", id,
                "leaderHost", advertiseHost,
                "leaderClientPort", String.valueOf(clientPort),
                "commit", String.valueOf(commitIndex));
        try {
            String resp = Tcp.request(p.host, p.raftPort, Wire.line("APPEND", msg), 1500);
            Map<String, String> r = Wire.parse(resp);
            long rt = Wire.getLong(r, "term", currentTerm);
            if (rt > currentTerm) synchronized (this) { stepDown(rt, null, null, -1); }
        } catch (Exception ignored) {}
    }

    private synchronized void persistLogEntry(LogEntry e) {
        try {
            FileWriter fw = new FileWriter(logFile, true);
            fw.write(e.index + "|" + e.term + "|" + e.payload + "\n");
            fw.close();
        } catch (Exception ex) {
            Tcp.log(id + " no pudo persistir log: " + ex.getMessage());
        }
    }

    private synchronized void applyCommitted() {
        List<LogEntry> copy = new ArrayList<LogEntry>(log);
        Collections.sort(copy, new Comparator<LogEntry>() {
            public int compare(LogEntry a, LogEntry b) { return Long.compare(a.index, b.index); }
        });
        for (LogEntry e : copy) if (e.index <= commitIndex) apply(e);
    }

    private synchronized void apply(LogEntry e) {
        if (appliedIndexes.contains(e.index)) return;
        try {
            String eventLine = new String(Base64.getDecoder().decode(e.payload), StandardCharsets.UTF_8);
            Map<String, String> ev = Wire.parse(eventLine);
            String eventId = Wire.get(ev, "eventId", "ev-" + e.index);
            String ts = Wire.get(ev, "timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()));
            String camera = Wire.get(ev, "camera", "cam?");
            String label = Wire.get(ev, "label", "unknown");
            String confidence = Wire.get(ev, "confidence", "0");
            String bbox = Wire.get(ev, "bbox", "");
            String image = Wire.get(ev, "image", "");
            String b64 = Wire.get(ev, "imageB64", "");

            if (!b64.isEmpty() && !image.isEmpty()) {
                try {
                    byte[] data = Base64.getDecoder().decode(b64);
                    String safeName = new File(image).getName();
                    File outImage = new File(imageDir, safeName);
                    FileOutputStream fos = new FileOutputStream(outImage);
                    fos.write(data);
                    fos.close();
                    image = "images/" + safeName;
                } catch (Exception imgEx) {
                    Tcp.log(id + " no pudo guardar imagen: " + imgEx.getMessage());
                }
            }

            FileWriter fw = new FileWriter(csvFile, true);
            fw.write(csv(e.index) + "," + csv(e.term) + "," + csv(eventId) + "," + csv(ts) + "," + csv(camera) + "," + csv(label) + "," + csv(confidence) + "," + csv(bbox) + "," + csv(image) + "," + csv(id) + "\n");
            fw.close();
            appliedIndexes.add(e.index);

            Map<String, String> msg = new LinkedHashMap<String, String>(ev);
            msg.put("index", String.valueOf(e.index));
            msg.put("term", String.valueOf(e.term));
            msg.remove("imageB64");
            broadcast(Wire.line("EVENT_COMMITTED", msg));

            Tcp.log(id + " COMMIT index=" + e.index + " eventId=" + eventId + " camera=" + camera + " label=" + label + " conf=" + confidence);
        } catch (Exception ex) {
            Tcp.log(id + " no pudo aplicar entrada " + e.index + ": " + ex.getMessage());
        }
    }

    private String csv(Object x) {
        String s = String.valueOf(x == null ? "" : x);
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) s = "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }

    private void broadcast(String line) {
        for (PrintWriter w : subscribers) {
            try { w.println(line); w.flush(); }
            catch (Exception e) { subscribers.remove(w); }
        }
    }

    private void sendSnapshot(PrintWriter out) {
        try {
            List<String> lines = Files.readAllLines(csvFile.toPath(), StandardCharsets.UTF_8);
            out.println(Wire.line("SNAPSHOT_BEGIN", Wire.kv("count", String.valueOf(Math.max(0, lines.size() - 1)), "node", id, "role", role.name(), "leaderId", nvl(leaderId))));
            for (int i = 1; i < lines.size(); i++) {
                out.println(Wire.line("CSV", Wire.kv("line", lines.get(i))));
            }
            out.println("SNAPSHOT_END");
            out.flush();
        } catch (Exception e) {
            out.println(Wire.line("ERROR", Wire.kv("msg", e.getMessage())));
        }
    }

    private void sendImage(PrintWriter out, Map<String, String> m) {
        try {
            String image = Wire.get(m, "image", "");
            String safe = new File(image).getName();
            File f = new File(imageDir, safe);
            if (!f.exists()) {
                out.println(Wire.line("ERROR", Wire.kv("msg", "imagen no existe: " + image)));
                return;
            }
            byte[] data = Files.readAllBytes(f.toPath());
            out.println(Wire.line("IMAGE", Wire.kv("name", safe, "imageB64", Base64.getEncoder().encodeToString(data))));
        } catch (Exception e) {
            out.println(Wire.line("ERROR", Wire.kv("msg", e.getMessage())));
        }
    }

    private void subscribe(Socket s, PrintWriter out) throws Exception {
        subscribers.add(out);
        sendSnapshot(out);
        while (!s.isClosed()) Thread.sleep(5000L);
    }

    private static String nvl(String s) { return s == null ? "" : s; }
}
