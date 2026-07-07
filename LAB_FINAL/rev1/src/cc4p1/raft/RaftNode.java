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
import java.io.IOException;
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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Nodo Raft didactico para CC4P1.
 * Usa solo java.net.Socket y java.lang.Thread. Implementa eleccion, heartbeats,
 * AppendEntries simplificado y replicacion por mayoria para registros de deteccion.
 */
public final class RaftNode {
    enum Role { FOLLOWER, CANDIDATE, LEADER }

    private String id;
    private String host;
    private int clientPort;
    private int raftPort;
    private File dataDir;
    private File imageDir;
    private File logFile;
    private File stateFile;
    private File csvFile;
    private final List<Peer> peers = new ArrayList<Peer>();
    private final List<LogEntry> log = new ArrayList<LogEntry>();
    private final Set<Long> applied = new HashSet<Long>();
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
        host = a.get("host", "127.0.0.1");
        clientPort = a.getInt("client-port", 7001);
        raftPort = a.getInt("raft-port", 7101);
        dataDir = new File(a.get("data", "data/" + id));
        imageDir = new File(dataDir, "images");
        logFile = new File(dataDir, "raft.log");
        stateFile = new File(dataDir, "state.properties");
        csvFile = new File(dataDir, "events.csv");
        dataDir.mkdirs(); imageDir.mkdirs();

        String peersRaw = a.get("peers", "").trim();
        if (!peersRaw.isEmpty()) {
            for (String p : peersRaw.split(",")) if (!p.trim().isEmpty()) peers.add(Peer.parse(p.trim()));
        }

        loadState();
        loadLogAndApply();

        Tcp.serve(raftPort, new Tcp.Handler() { public void handle(Socket s) throws Exception { handleRaft(s); } }, "raft-" + id);
        Tcp.serve(clientPort, new Tcp.Handler() { public void handle(Socket s) throws Exception { handleClient(s); } }, "client-" + id);

        startElectionLoop();
        startHeartbeatLoop();

        Tcp.log("Nodo " + id + " iniciado. host=" + host + " clientPort=" + clientPort + " raftPort=" + raftPort + " peers=" + peers);
        while (true) Thread.sleep(60_000L);
    }

    private synchronized void loadState() {
        if (!stateFile.exists()) return;
        try {
            List<String> lines = Files.readAllLines(stateFile.toPath(), StandardCharsets.UTF_8);
            for (String l : lines) {
                if (l.startsWith("term=")) currentTerm = Long.parseLong(l.substring(5));
                if (l.startsWith("votedFor=")) {
                    String v = l.substring(9);
                    votedFor = v.isEmpty() ? null : v;
                }
            }
        } catch (Exception e) { Tcp.log(id + " no pudo leer estado: " + e.getMessage()); }
    }

    private synchronized void saveState() {
        try {
            FileWriter fw = new FileWriter(stateFile, false);
            fw.write("term=" + currentTerm + "\n");
            fw.write("votedFor=" + (votedFor == null ? "" : votedFor) + "\n");
            fw.close();
        } catch (IOException e) { Tcp.log(id + " no pudo guardar estado: " + e.getMessage()); }
    }

    private synchronized void loadLogAndApply() {
        try {
            if (!csvFile.exists()) {
                FileWriter fw = new FileWriter(csvFile, true);
                fw.write("index,term,timestamp,camera,label,confidence,image,node\n");
                fw.close();
            }
            if (!logFile.exists()) return;
            BufferedReader br = new BufferedReader(new FileReader(logFile));
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split("\\|", 3);
                if (p.length == 3) {
                    LogEntry e = new LogEntry(Long.parseLong(p[0]), Long.parseLong(p[1]), p[2]);
                    log.add(e);
                    commitIndex = Math.max(commitIndex, e.index); // para reinicio didactico: lo persistido se re-aplica
                    apply(e);
                }
            }
            br.close();
        } catch (Exception e) { Tcp.log(id + " no pudo cargar log: " + e.getMessage()); }
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
        if (term > currentTerm) stepDown(term, null);
        boolean upToDate = candLastTerm > lastTerm() || (candLastTerm == lastTerm() && candLastIndex >= lastIndex());
        boolean grant = term == currentTerm && upToDate && (votedFor == null || votedFor.equals(candidate));
        if (grant) {
            votedFor = candidate;
            lastHeartbeatMs = System.currentTimeMillis();
            saveState();
        }
        return Wire.line("VOTE_RESPONSE", Wire.kv("term", String.valueOf(currentTerm), "voteGranted", String.valueOf(grant), "from", id));
    }

    private synchronized String onAppend(Map<String, String> m) {
        long term = Wire.getLong(m, "term", 0);
        String leader = Wire.get(m, "leader", "");
        if (term < currentTerm) return Wire.line("APPEND_RESPONSE", Wire.kv("term", String.valueOf(currentTerm), "success", "false", "from", id));
        if (term > currentTerm || role != Role.FOLLOWER) stepDown(term, leader);
        leaderId = leader;
        leaderHost = Wire.get(m, "leaderHost", leaderHost == null ? "" : leaderHost);
        leaderClientPort = Wire.getInt(m, "leaderClientPort", leaderClientPort);
        lastHeartbeatMs = System.currentTimeMillis();

        long entryIndex = Wire.getLong(m, "entryIndex", 0);
        if (entryIndex > 0) {
            long entryTerm = Wire.getLong(m, "entryTerm", term);
            String payload = Wire.get(m, "payload", "");
            boolean exists = false;
            for (LogEntry e : log) if (e.index == entryIndex) { exists = true; break; }
            if (!exists) {
                LogEntry e = new LogEntry(entryIndex, entryTerm, payload);
                log.add(e);
                persist(e);
            }
        }
        long leaderCommit = Wire.getLong(m, "commit", commitIndex);
        if (leaderCommit > commitIndex) {
            commitIndex = Math.min(leaderCommit, lastIndex());
            applyCommitted();
        }
        return Wire.line("APPEND_RESPONSE", Wire.kv("term", String.valueOf(currentTerm), "success", "true", "from", id, "matchIndex", String.valueOf(lastIndex())));
    }

    private synchronized void stepDown(long term, String newLeader) {
        currentTerm = term;
        role = Role.FOLLOWER;
        votedFor = null;
        leaderId = newLeader;
        saveState();
    }

    private void handleClient(Socket s) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"));
        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8")), true);
        String line = in.readLine();
        String type = Wire.type(line);
        Map<String, String> m = Wire.parse(line);
        if ("EVENT".equals(type)) {
            out.println(handleEvent(line));
        } else if ("STATUS".equals(type)) {
            out.println(Wire.line("STATUS_RESPONSE", Wire.kv("id", id, "term", String.valueOf(currentTerm), "role", role.name(), "leaderId", nvl(leaderId), "leaderHost", nvl(leaderHost), "leaderClientPort", String.valueOf(leaderClientPort), "lastIndex", String.valueOf(lastIndex()))));
        } else if ("SNAPSHOT".equals(type)) {
            sendSnapshot(out);
        } else if ("SUBSCRIBE".equals(type)) {
            subscribers.add(out);
            sendSnapshot(out);
            while (!s.isClosed()) Thread.sleep(5_000L);
        } else {
            out.println(Wire.line("ERROR", Wire.kv("msg", "tipo cliente no soportado: " + type)));
        }
    }

    private String handleEvent(String eventLine) {
        if (role != Role.LEADER) {
            String lh = leaderHost;
            int lp = leaderClientPort;
            if (lh != null && lh.length() > 0 && lp > 0) {
                try { return Tcp.request(lh, lp, eventLine, 5000); }
                catch (Exception e) { return Wire.line("ERROR", Wire.kv("msg", "no soy lider y no pude reenviar al lider: " + e.getMessage(), "leader", nvl(leaderId))); }
            }
            return Wire.line("NOT_LEADER", Wire.kv("leaderId", nvl(leaderId), "leaderHost", nvl(leaderHost), "leaderClientPort", String.valueOf(leaderClientPort)));
        }
        LogEntry e;
        synchronized (this) {
            e = new LogEntry(lastIndex() + 1, currentTerm, Base64.getEncoder().encodeToString(eventLine.getBytes(StandardCharsets.UTF_8)));
            log.add(e);
            persist(e);
        }
        int ok = 1;
        for (Peer p : peers) if (replicate(p, e)) ok++;
        if (ok >= majority()) {
            synchronized (this) { commitIndex = e.index; applyCommitted(); }
            broadcastCommitToPeers();
            return Wire.line("OK", Wire.kv("committed", "true", "index", String.valueOf(e.index), "acks", String.valueOf(ok), "leader", id));
        }
        return Wire.line("ERROR", Wire.kv("committed", "false", "index", String.valueOf(e.index), "acks", String.valueOf(ok), "msg", "sin mayoria"));
    }

    private boolean replicate(Peer p, LogEntry e) {
        Map<String, String> msg = new LinkedHashMap<String, String>();
        msg.put("term", String.valueOf(currentTerm));
        msg.put("leader", id);
        msg.put("leaderHost", host);
        msg.put("leaderClientPort", String.valueOf(clientPort));
        msg.put("commit", String.valueOf(commitIndex));
        msg.put("entryIndex", String.valueOf(e.index));
        msg.put("entryTerm", String.valueOf(e.term));
        msg.put("payload", e.payload);
        try {
            String resp = Tcp.request(p.host, p.raftPort, Wire.line("APPEND", msg), 5000);
            Map<String, String> r = Wire.parse(resp);
            long term = Wire.getLong(r, "term", currentTerm);
            if (term > currentTerm) { synchronized (this) { stepDown(term, null); } return false; }
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
        Thread t = new Thread(new Runnable() { public void run() {
            while (true) {
                try {
                    Thread.sleep(250L);
                    if (role == Role.LEADER) continue;
                    long elapsed = System.currentTimeMillis() - lastHeartbeatMs;
                    long timeout = 1500L + rnd.nextInt(1500);
                    if (elapsed > timeout) startElection();
                } catch (Exception e) { Tcp.log(id + " electionLoop: " + e.getMessage()); }
            }
        }}, "election-" + id);
        t.setDaemon(true); t.start();
    }

    private void startHeartbeatLoop() {
        Thread t = new Thread(new Runnable() { public void run() {
            while (true) {
                try {
                    Thread.sleep(600L);
                    if (role == Role.LEADER) for (Peer p : peers) sendHeartbeat(p);
                } catch (Exception e) { Tcp.log(id + " heartbeatLoop: " + e.getMessage()); }
            }
        }}, "heartbeat-" + id);
        t.setDaemon(true); t.start();
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
            saveState();
            term = currentTerm;
            li = lastIndex();
            lt = lastTerm();
        }
        int votes = 1;
        Tcp.log(id + " inicia eleccion term=" + term);
        for (Peer p : peers) {
            Map<String, String> msg = Wire.kv("term", String.valueOf(term), "candidate", id, "lastIndex", String.valueOf(li), "lastTerm", String.valueOf(lt));
            try {
                String resp = Tcp.request(p.host, p.raftPort, Wire.line("VOTE_REQUEST", msg), 3000);
                Map<String, String> r = Wire.parse(resp);
                long rt = Wire.getLong(r, "term", term);
                synchronized (this) {
                    if (rt > currentTerm) { stepDown(rt, null); return; }
                }
                if ("true".equals(Wire.get(r, "voteGranted", "false"))) votes++;
            } catch (Exception e) { Tcp.log(id + " no obtiene voto de " + p + ": " + e.getMessage()); }
        }
        synchronized (this) {
            if (role == Role.CANDIDATE && currentTerm == term && votes >= majority()) {
                role = Role.LEADER;
                leaderId = id;
                leaderHost = host;
                leaderClientPort = clientPort;
                lastHeartbeatMs = System.currentTimeMillis();
                Tcp.log(id + " ES LIDER term=" + term + " votos=" + votes + "/" + (peers.size() + 1));
                for (Peer p : peers) sendHeartbeat(p);
            }
        }
    }

    private void sendHeartbeat(Peer p) {
        Map<String, String> msg = Wire.kv("term", String.valueOf(currentTerm), "leader", id, "leaderHost", host, "leaderClientPort", String.valueOf(clientPort), "commit", String.valueOf(commitIndex));
        try {
            String resp = Tcp.request(p.host, p.raftPort, Wire.line("APPEND", msg), 1500);
            Map<String, String> r = Wire.parse(resp);
            long term = Wire.getLong(r, "term", currentTerm);
            if (term > currentTerm) synchronized (this) { stepDown(term, null); }
        } catch (Exception ignored) {}
    }

    private synchronized void persist(LogEntry e) {
        try {
            FileWriter fw = new FileWriter(logFile, true);
            fw.write(e.index + "|" + e.term + "|" + e.payload + "\n");
            fw.close();
        } catch (IOException ex) { Tcp.log(id + " no pudo persistir log: " + ex.getMessage()); }
    }

    private synchronized void applyCommitted() {
        List<LogEntry> copy = new ArrayList<LogEntry>(log);
        Collections.sort(copy, new java.util.Comparator<LogEntry>() { public int compare(LogEntry a, LogEntry b) { return Long.compare(a.index, b.index); }});
        for (LogEntry e : copy) if (e.index <= commitIndex) apply(e);
    }

    private synchronized void apply(LogEntry e) {
        if (applied.contains(e.index)) return;
        try {
            String eventLine = new String(Base64.getDecoder().decode(e.payload), StandardCharsets.UTF_8);
            Map<String, String> ev = Wire.parse(eventLine);
            String ts = Wire.get(ev, "timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            String camera = Wire.get(ev, "camera", "cam?");
            String label = Wire.get(ev, "label", "unknown");
            String confidence = Wire.get(ev, "confidence", "0");
            String image = Wire.get(ev, "image", "");
            String b64 = Wire.get(ev, "imageB64", "");
            if (!b64.isEmpty() && !image.isEmpty()) {
                try {
                    byte[] data = Base64.getDecoder().decode(b64);
                    FileOutputStream fos = new FileOutputStream(new File(imageDir, new File(image).getName()));
                    fos.write(data); fos.close();
                    image = "images/" + new File(image).getName();
                } catch (Exception imgEx) { Tcp.log(id + " no pudo guardar imagen: " + imgEx.getMessage()); }
            }
            FileWriter fw = new FileWriter(csvFile, true);
            fw.write(csv(e.index) + "," + csv(e.term) + "," + csv(ts) + "," + csv(camera) + "," + csv(label) + "," + csv(confidence) + "," + csv(image) + "," + csv(id) + "\n");
            fw.close();
            applied.add(e.index);
            Map<String, String> msg = new LinkedHashMap<String, String>(ev);
            msg.put("index", String.valueOf(e.index));
            msg.put("term", String.valueOf(e.term));
            msg.remove("imageB64");
            broadcast(Wire.line("EVENT_COMMITTED", msg));
            Tcp.log(id + " commit index=" + e.index + " camera=" + camera + " label=" + label + " conf=" + confidence);
        } catch (Exception ex) { Tcp.log(id + " no pudo aplicar entrada " + e.index + ": " + ex.getMessage()); }
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
            out.println(Wire.line("SNAPSHOT_BEGIN", Wire.kv("count", String.valueOf(Math.max(0, lines.size() - 1)), "node", id, "role", role.name())));
            for (int i = 1; i < lines.size(); i++) out.println(Wire.line("CSV", Wire.kv("line", lines.get(i))));
            out.println("SNAPSHOT_END");
            out.flush();
        } catch (Exception e) { out.println(Wire.line("ERROR", Wire.kv("msg", e.getMessage()))); }
    }

    private static String nvl(String s) { return s == null ? "" : s; }
}
