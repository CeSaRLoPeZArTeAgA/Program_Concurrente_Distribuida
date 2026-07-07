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

public final class WatcherClient {
    public static void main(String[] args) throws Exception {
        Args a = new Args(args);
        String host = a.get("host", "127.0.0.1");
        int port = a.getInt("port", 7001);
        Socket s = new Socket(host, port);
        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8")), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"));
        out.println("SUBSCRIBE");
        System.out.println("Escuchando eventos committed desde " + host + ":" + port);
        String line;
        while ((line = in.readLine()) != null) {
            if (line.startsWith("EVENT_COMMITTED|")) {
                Map<String, String> m = Wire.parse(line);
                System.out.println("COMMIT index=" + Wire.get(m, "index", "") +
                        " cam=" + Wire.get(m, "camera", "") +
                        " label=" + Wire.get(m, "label", "") +
                        " conf=" + Wire.get(m, "confidence", "") +
                        " ts=" + Wire.get(m, "timestamp", "") +
                        " image=" + Wire.get(m, "image", ""));
            } else if (line.startsWith("CSV|")) {
                // snapshot inicial
            } else {
                System.out.println(line);
            }
        }
        s.close();
    }
}
