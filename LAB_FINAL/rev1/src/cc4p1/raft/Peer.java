package cc4p1.raft;

public final class Peer {
    public final String id;
    public final String host;
    public final int raftPort;
    public final int clientPort;

    public Peer(String id, String host, int raftPort, int clientPort) {
        this.id = id; this.host = host; this.raftPort = raftPort; this.clientPort = clientPort;
    }

    public static Peer parse(String s) {
        // formato: id@host:raftPort:clientPort
        String[] a = s.split("@", 2);
        if (a.length != 2) throw new IllegalArgumentException("Peer invalido: " + s);
        String[] b = a[1].split(":");
        if (b.length < 3) throw new IllegalArgumentException("Peer invalido, use id@host:raftPort:clientPort: " + s);
        return new Peer(a[0], b[0], Integer.parseInt(b[1]), Integer.parseInt(b[2]));
    }

    public String toString() { return id + "@" + host + ":" + raftPort + ":" + clientPort; }
}
