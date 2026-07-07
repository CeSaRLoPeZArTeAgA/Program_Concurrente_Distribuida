package cc4p1.client;

import cc4p1.common.Args;
import cc4p1.common.Wire;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

public final class SnapshotClient {
    public static void main(String[] args) throws Exception {
        Args a = new Args(args);
        String host = a.get("host", "127.0.0.1");
        int port = a.getInt("port", 7001);
        Socket s = new Socket(host, port);
        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8")), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"));
        out.println("SNAPSHOT");
        String line;
        while ((line = in.readLine()) != null) {
            if ("SNAPSHOT_END".equals(line)) break;
            if (line.startsWith("CSV|")) {
                Map<String, String> m = Wire.parse(line);
                System.out.println(Wire.get(m, "line", ""));
            } else {
                System.out.println(line);
            }
        }
        s.close();
    }
}
