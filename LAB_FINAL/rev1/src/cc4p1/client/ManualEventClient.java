package cc4p1.client;

import cc4p1.common.Args;
import cc4p1.common.Tcp;
import cc4p1.common.Wire;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/** Cliente de prueba: inserta un registro manual sin camara ni YOLO. */
public final class ManualEventClient {
    public static void main(String[] args) throws Exception {
        Args a = new Args(args);
        String raft = a.get("raft", "127.0.0.1:7001");
        String[] hp = raft.split(":");
        Map<String, String> ev = new LinkedHashMap<String, String>();
        ev.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()));
        ev.put("camera", a.get("camera", "manual"));
        ev.put("label", a.get("label", "objeto_prueba"));
        ev.put("confidence", a.get("confidence", "1.0"));
        ev.put("bbox", "0,0,0,0");
        ev.put("image", "manual.jpg");
        String resp = Tcp.request(hp[0], Integer.parseInt(hp[1]), Wire.line("EVENT", ev), 8000);
        System.out.println(resp);
    }
}
