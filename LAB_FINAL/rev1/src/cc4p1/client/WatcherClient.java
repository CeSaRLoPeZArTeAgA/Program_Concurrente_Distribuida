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

/** Cliente vigilante: visualiza registros confirmados por Raft. */
public final class WatcherClient {
    public static void main(String[] args) throws Exception {
        Args a = new Args(args);
        String raft = a.get("raft", "127.0.0.1:7001");
        String[] hp = raft.split(":");
        Socket s = new Socket(hp[0], Integer.parseInt(hp[1]));
        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8")), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"));
        out.println("SUBSCRIBE"); out.flush();
        System.out.println("Cliente vigilante conectado a " + raft);
        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("%-6s %-22s %-10s %-18s %-10s %s%n", "idx", "fecha", "camara", "objeto", "conf", "imagen");
        System.out.println("--------------------------------------------------------------------------------");
        String line;
        while ((line = in.readLine()) != null) {
            String type = Wire.type(line);
            if ("CSV".equals(type)) printCsv(Wire.get(Wire.parse(line), "line", ""));
            else if ("EVENT_COMMITTED".equals(type)) printEvent(Wire.parse(line));
            else if ("SNAPSHOT_BEGIN".equals(type)) System.out.println("Snapshot inicial...");
            else if ("SNAPSHOT_END".equals(type)) System.out.println("--- Fin snapshot; esperando eventos nuevos ---");
            else System.out.println(line);
        }
        s.close();
    }

    private static void printEvent(Map<String, String> ev) {
        System.out.printf("%-6s %-22s %-10s %-18s %-10s %s%n",
                Wire.get(ev, "index", "?"),
                Wire.get(ev, "timestamp", ""),
                Wire.get(ev, "camera", ""),
                Wire.get(ev, "label", ""),
                Wire.get(ev, "confidence", ""),
                Wire.get(ev, "image", ""));
    }

    private static void printCsv(String csv) {
        // csv generado por el nodo: index,term,timestamp,camera,label,confidence,image,node
        String[] p = csv.split(",", -1);
        if (p.length >= 8) {
            System.out.printf("%-6s %-22s %-10s %-18s %-10s %s%n", p[0], p[2], p[3], p[4], p[5], p[6]);
        } else {
            System.out.println(csv);
        }
    }
}
