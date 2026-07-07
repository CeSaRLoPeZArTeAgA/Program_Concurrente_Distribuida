package cc4p1.client;

import cc4p1.common.Args;
import cc4p1.common.Tcp;

public final class StatusClient {
    public static void main(String[] args) throws Exception {
        Args a = new Args(args);
        String raft = a.get("raft", "127.0.0.1:7001");
        String[] hp = raft.split(":");
        System.out.println(Tcp.request(hp[0], Integer.parseInt(hp[1]), "STATUS", 5000));
    }
}
