package cc4p1.raft;

/** Formato: id@host:clientPort:raftPort */
public final class Peer {
    public final String id;
    public final String host;
    public final int clientPort;
    public final int raftPort;

    public Peer(String id, String host, int clientPort, int raftPort) {
        this.id = id;
        this.host = host;
        this.clientPort = clientPort;
        this.raftPort = raftPort;
    }

    public static Peer parse(String raw) {
        String[] a = raw.split("@", 2);
        String id = a[0].trim();
        String[] hp = a[1].split(":");
        if (hp.length != 3) throw new IllegalArgumentException("Peer invalido: " + raw + ". Use id@host:clientPort:raftPort");
        return new Peer(id, hp[0], Integer.parseInt(hp[1]), Integer.parseInt(hp[2]));
    }

    public String toString() {
        return id + "@" + host + ":" + clientPort + ":" + raftPort;
    }
}
