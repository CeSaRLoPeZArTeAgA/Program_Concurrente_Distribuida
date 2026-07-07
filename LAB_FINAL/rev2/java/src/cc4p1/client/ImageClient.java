package cc4p1.client;

import cc4p1.common.Args;
import cc4p1.common.Tcp;
import cc4p1.common.Wire;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Cliente simple para descargar imagenes guardadas por un nodo Raft.
 *
 * Modos de uso:
 * 1) Descargar por indice del evento:
 *    java -cp build/classes cc4p1.client.ImageClient --host 192.168.0.186 --port 7001 --index 1
 *
 * 2) Descargar por ruta de imagen:
 *    java -cp build/classes cc4p1.client.ImageClient --host 192.168.0.186 --port 7001 --image images/cam1.jpg
 *
 * 3) Descargar el ultimo evento del snapshot:
 *    java -cp build/classes cc4p1.client.ImageClient --host 192.168.0.186 --port 7001 --latest true
 */
public final class ImageClient {
    public static void main(String[] args) throws Exception {
        Args a = new Args(args);
        String host = a.get("host", "127.0.0.1");
        int port = a.getInt("port", 7001);
        String image = a.get("image", "").trim();
        long index = a.getLong("index", -1L);
        boolean latest = a.getBoolean("latest", false);
        boolean open = a.getBoolean("open", true);
        String outDir = a.get("out", "downloaded_images");

        if (image.isEmpty()) {
            List<String[]> rows = readSnapshotRows(host, port);
            if (rows.isEmpty()) {
                System.out.println("[ERROR] No hay eventos committed en el nodo " + host + ":" + port);
                System.exit(1);
            }

            String[] selected = null;

            if (latest) {
                selected = rows.get(rows.size() - 1);
            } else if (index > 0) {
                for (String[] row : rows) {
                    if (row.length > 0 && String.valueOf(index).equals(row[0])) {
                        selected = row;
                        break;
                    }
                }
            } else {
                System.out.println("[ERROR] Indica --index N, --latest true o --image images/archivo.jpg");
                printUsage();
                System.exit(1);
            }

            if (selected == null) {
                System.out.println("[ERROR] No se encontro evento con index=" + index);
                System.exit(1);
            }

            if (selected.length < 9) {
                System.out.println("[ERROR] La fila CSV no tiene columna image.");
                System.exit(1);
            }

            image = selected[8];

            System.out.println("[INFO] Evento seleccionado:");
            System.out.println("  index      : " + safeCol(selected, 0));
            System.out.println("  timestamp  : " + safeCol(selected, 3));
            System.out.println("  camera     : " + safeCol(selected, 4));
            System.out.println("  label      : " + safeCol(selected, 5));
            System.out.println("  confidence : " + safeCol(selected, 6));
            System.out.println("  image      : " + image);
        }

        File out = downloadImage(host, port, image, outDir);
        System.out.println("[OK] Imagen descargada:");
        System.out.println(out.getAbsolutePath());

        if (open) {
            tryOpen(out);
        }
    }

    private static void printUsage() {
        System.out.println("Uso:");
        System.out.println("  java -cp build/classes cc4p1.client.ImageClient --host HOST --port 7001 --index 1");
        System.out.println("  java -cp build/classes cc4p1.client.ImageClient --host HOST --port 7001 --latest true");
        System.out.println("  java -cp build/classes cc4p1.client.ImageClient --host HOST --port 7001 --image images/archivo.jpg");
        System.out.println("Opcional:");
        System.out.println("  --out downloaded_images");
        System.out.println("  --open false");
    }

    private static List<String[]> readSnapshotRows(String host, int port) throws Exception {
        List<String[]> rows = new ArrayList<String[]>();

        Socket s = new Socket(host, port);
        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8")), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"));

        out.println("SNAPSHOT");
        out.flush();

        String line;
        while ((line = in.readLine()) != null) {
            if ("SNAPSHOT_END".equals(line)) break;
            if (line.startsWith("CSV|")) {
                Map<String, String> m = Wire.parse(line);
                String csvLine = Wire.get(m, "line", "");
                rows.add(parseCsvLine(csvLine));
            }
        }

        s.close();
        return rows;
    }

    private static File downloadImage(String host, int port, String image, String outDir) throws Exception {
        String request = Wire.line("GET_IMAGE", Wire.kv("image", image));
        String response = Tcp.request(host, port, request, 15000);

        String type = Wire.type(response);
        Map<String, String> m = Wire.parse(response);

        if (!"IMAGE".equals(type)) {
            String msg = Wire.get(m, "msg", response);
            throw new RuntimeException("El nodo no devolvio imagen: " + msg);
        }

        String name = Wire.get(m, "name", new File(image).getName());
        String imageB64 = Wire.get(m, "imageB64", "");

        if (imageB64.isEmpty()) {
            throw new RuntimeException("Respuesta IMAGE sin imageB64.");
        }

        byte[] data = Base64.getDecoder().decode(imageB64);

        Path dir = Paths.get(outDir);
        Files.createDirectories(dir);

        String safeName = new File(name).getName();
        Path output = dir.resolve(safeName);
        Files.write(output, data);

        return output.toFile();
    }

    private static void tryOpen(File f) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(f);
            } else {
                System.out.println("[INFO] Desktop.open no esta soportado en este sistema.");
            }
        } catch (Exception e) {
            System.out.println("[WARN] No se pudo abrir automaticamente la imagen: " + e.getMessage());
        }
    }

    private static String safeCol(String[] row, int i) {
        return i >= 0 && i < row.length ? row[i] : "";
    }

    /**
     * Parser CSV simple con soporte para comillas dobles.
     * Necesario porque bbox viene como "x1,y1,x2,y2".
     */
    private static String[] parseCsvLine(String line) {
        List<String> cols = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                cols.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        cols.add(current.toString());
        return cols.toArray(new String[0]);
    }
}
