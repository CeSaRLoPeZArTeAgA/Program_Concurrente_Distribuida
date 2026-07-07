package cc4p1.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Tcp {
    private Tcp() {}

    public interface Handler { void handle(Socket socket) throws Exception; }

    public static Thread serve(final String bindHost, final int port, final Handler handler, final String name) {
        Thread t = new Thread(new Runnable() {
            public void run() {
                ExecutorService pool = Executors.newCachedThreadPool();
                try {
                    ServerSocket ss = new ServerSocket();
                    ss.bind(new InetSocketAddress(bindHost, port));
                    log(name + " escuchando en " + bindHost + ":" + port);
                    while (true) {
                        final Socket s = ss.accept();
                        pool.submit(new Runnable() {
                            public void run() {
                                try { handler.handle(s); }
                                catch (Exception e) { log(name + " error: " + e.getMessage()); }
                                finally { try { s.close(); } catch (IOException ignored) {} }
                            }
                        });
                    }
                } catch (IOException e) {
                    log(name + " no pudo iniciar en puerto " + port + ": " + e.getMessage());
                }
            }
        }, name + "-server");
        t.setDaemon(false);
        t.start();
        return t;
    }

    public static String request(String host, int port, String line, int timeoutMs) throws IOException {
        Socket s = new Socket();
        s.connect(new InetSocketAddress(host, port), timeoutMs);
        s.setSoTimeout(timeoutMs);
        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8")), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"));
        out.println(line);
        out.flush();
        String resp = in.readLine();
        s.close();
        return resp == null ? "" : resp;
    }

    public static void log(String s) {
        System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()) + "] " + s);
    }
}
