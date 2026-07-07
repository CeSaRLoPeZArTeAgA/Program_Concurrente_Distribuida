package cc4p1.client;

import cc4p1.common.Args;
import cc4p1.common.Tcp;
import cc4p1.common.Wire;

import java.util.Map;

public final class StatusClient {
    public static void main(String[] args) throws Exception {
        Args a = new Args(args);
        String host = a.get("host", "127.0.0.1");
        int port = a.getInt("port", 7001);
        String resp = Tcp.request(host, port, "STATUS", 5000);
        Map<String, String> m = Wire.parse(resp);
        System.out.println(resp);
        System.out.println("Nodo: " + Wire.get(m, "id", "?"));
        System.out.println("Rol: " + Wire.get(m, "role", "?"));
        System.out.println("Term: " + Wire.get(m, "term", "?"));
        System.out.println("Leader: " + Wire.get(m, "leaderId", "?") + " " + Wire.get(m, "leaderHost", "") + ":" + Wire.get(m, "leaderClientPort", ""));
        System.out.println("lastIndex=" + Wire.get(m, "lastIndex", "0") + " commitIndex=" + Wire.get(m, "commitIndex", "0"));
    }
}
