package cc4p1.detector;

import cc4p1.common.Args;
import cc4p1.common.Tcp;
import cc4p1.common.Wire;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servidor de testeo de objetos.
 * Orquesta camara RTSP + YOLO + envio a Raft usando solo sockets Java.
 * Nota tecnica: Java SE no carga .pt ni decodifica RTSP/H264 de forma nativa; por eso este
 * modulo invoca ffmpeg para capturar frame y un proceso Python/Ultralytics para ejecutar YOLO.
 * La red distribuida, concurrencia, persistencia y consenso estan en Java puro.
 */
public final class DetectionServer {
    private String camera;
    private String rtsp;
    private String image;
    private String raftHost;
    private int raftPort;
    private File model;
    private File outDir;
    private String python;
    private File script;
    private long intervalMs;
    private double conf;
    private String mode;
    private int maxImageBytes;

    public static void main(String[] args) throws Exception { new DetectionServer().start(args); }

    private void start(String[] raw) throws Exception {
        Args a = new Args(raw);
        camera = a.get("camera", "cam1");
        rtsp = a.get("rtsp", "");
        image = a.get("image", "");
        String raft = a.get("raft", "127.0.0.1:7001");
        String[] hp = raft.split(":");
        raftHost = hp[0]; raftPort = Integer.parseInt(hp[1]);
        model = new File(a.get("model", "models/yolo11n(4).pt"));
        outDir = new File(a.get("out", "captures/" + camera));
        python = a.get("python", System.getProperty("os.name").toLowerCase().contains("win") ? "python" : "python3");
        script = new File(a.get("script", "tools/yolo_detect.py"));
        intervalMs = a.getLong("interval-ms", 3000L);
        conf = a.getDouble("conf", 0.35);
        mode = a.get("mode", "yolo"); // yolo | mock
        maxImageBytes = a.getInt("max-image-bytes", 220000);
        outDir.mkdirs();

        Tcp.log("DetectionServer camera=" + camera + " raft=" + raftHost + ":" + raftPort + " mode=" + mode);
        Tcp.log("RTSP=" + (rtsp.isEmpty() ? "NO" : rtsp) + " image=" + (image.isEmpty() ? "NO" : image));

        int seq = 0;
        while (true) {
            try {
                File frame = new File(outDir, "frame_current.jpg");
                if (!image.isEmpty()) copyImage(new File(image), frame);
                else if (!rtsp.isEmpty()) grabRtspFrame(rtsp, frame);
                else throw new IllegalArgumentException("Debe indicar --rtsp rtsp://... o --image archivo.jpg");

                List<Detection> detections = "mock".equalsIgnoreCase(mode) ? mockDetections() : runYolo(frame);
                for (Detection d : detections) {
                    if (d.confidence < conf) continue;
                    String ts = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss_SSS").format(new Date());
                    String imgName = camera + "_" + ts + "_" + (++seq) + ".jpg";
                    File saved = new File(outDir, imgName);
                    copyImage(frame, saved);
                    Map<String, String> ev = new LinkedHashMap<String, String>();
                    ev.put("timestamp", ts.replace('_', ' '));
                    ev.put("camera", camera);
                    ev.put("label", d.label);
                    ev.put("confidence", String.format(java.util.Locale.US, "%.4f", d.confidence));
                    ev.put("bbox", d.x1 + "," + d.y1 + "," + d.x2 + "," + d.y2);
                    ev.put("image", imgName);
                    String b64 = imageB64(saved);
                    if (!b64.isEmpty()) ev.put("imageB64", b64);
                    String resp = Tcp.request(raftHost, raftPort, Wire.line("EVENT", ev), 15000);
                    Tcp.log("Enviado a Raft: " + d.label + " conf=" + d.confidence + " resp=" + resp);
                }
            } catch (Exception e) {
                Tcp.log("DetectionServer error: " + e.getMessage());
            }
            Thread.sleep(intervalMs);
        }
    }

    private void grabRtspFrame(String url, File output) throws Exception {
        List<String> cmd = new ArrayList<String>();
        cmd.add("ffmpeg");
        cmd.add("-y");
        cmd.add("-loglevel"); cmd.add("error");
        cmd.add("-rtsp_transport"); cmd.add("tcp");
        cmd.add("-i"); cmd.add(url);
        cmd.add("-frames:v"); cmd.add("1");
        cmd.add("-q:v"); cmd.add("2");
        cmd.add(output.getAbsolutePath());
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        StringBuilder sb = new StringBuilder(); String line;
        while ((line = br.readLine()) != null) sb.append(line).append('\n');
        int code = p.waitFor();
        if (code != 0 || !output.exists() || output.length() == 0) throw new RuntimeException("ffmpeg fallo code=" + code + " " + sb.toString());
    }

    private List<Detection> runYolo(File img) throws Exception {
        List<String> cmd = new ArrayList<String>();
        cmd.add(python);
        cmd.add(script.getAbsolutePath());
        cmd.add("--model"); cmd.add(model.getAbsolutePath());
        cmd.add("--image"); cmd.add(img.getAbsolutePath());
        cmd.add("--conf"); cmd.add(String.valueOf(conf));
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        List<Detection> out = new ArrayList<Detection>();
        String line;
        StringBuilder errors = new StringBuilder();
        while ((line = br.readLine()) != null) {
            if (line.startsWith("DET|")) {
                Map<String, String> m = Wire.parse(line.substring(3)); // usa el mismo formato k=v al quitar DET
                out.add(new Detection(Wire.get(m, "label", "unknown"), Wire.getDouble(m, "confidence", 0), Wire.getInt(m, "x1", 0), Wire.getInt(m, "y1", 0), Wire.getInt(m, "x2", 0), Wire.getInt(m, "y2", 0)));
            } else {
                errors.append(line).append('\n');
            }
        }
        int code = p.waitFor();
        if (code != 0) throw new RuntimeException("YOLO fallo code=" + code + " " + errors.toString());
        if (out.isEmpty()) Tcp.log("YOLO sin detecciones. " + errors.toString().trim());
        return out;
    }

    private List<Detection> mockDetections() {
        List<Detection> d = new ArrayList<Detection>();
        d.add(new Detection("objeto_demo", 0.99, 0, 0, 100, 100));
        return d;
    }

    private void copyImage(File src, File dst) throws Exception {
        if (!src.exists()) throw new IllegalArgumentException("No existe imagen: " + src.getAbsolutePath());
        Files.copy(src.toPath(), dst.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private String imageB64(File f) throws Exception {
        if (f.length() > maxImageBytes) {
            Tcp.log("Imagen no enviada por base64 porque pesa " + f.length() + " bytes. Limite=" + maxImageBytes);
            return "";
        }
        byte[] data = new byte[(int) f.length()];
        FileInputStream fis = new FileInputStream(f);
        int off = 0;
        while (off < data.length) {
            int n = fis.read(data, off, data.length - off);
            if (n < 0) break; off += n;
        }
        fis.close();
        return Base64.getEncoder().encodeToString(data);
    }

    static final class Detection {
        final String label; final double confidence; final int x1, y1, x2, y2;
        Detection(String label, double confidence, int x1, int y1, int x2, int y2) {
            this.label = label; this.confidence = confidence; this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2;
        }
    }
}
