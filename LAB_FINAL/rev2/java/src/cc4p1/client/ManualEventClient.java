package cc4p1.client;

import cc4p1.common.Args;
import cc4p1.common.Tcp;
import cc4p1.common.Wire;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class ManualEventClient {
    public static void main(String[] args) throws Exception {
        Args a = new Args(args);
        String host = a.get("host", "127.0.0.1");
        int port = a.getInt("port", 7001);
        String camera = a.get("camera", "cam-demo");
        String label = a.get("label", "objeto_demo");
        String conf = a.get("confidence", "0.9900");
        String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
        Map<String, String> ev = new LinkedHashMap<String, String>();
        ev.put("eventId", UUID.randomUUID().toString());
        ev.put("timestamp", ts);
        ev.put("camera", camera);
        ev.put("label", label);
        ev.put("confidence", conf);
        ev.put("bbox", "0,0,100,100");
        ev.put("image", "");
        String line = Wire.line("EVENT", ev);
        System.out.println(Tcp.request(host, port, line, 15000));
    }
}
