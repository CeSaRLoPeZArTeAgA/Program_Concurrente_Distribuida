package cc4p1.common;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

/** Line protocol: TYPE|k=v|k=v. Values are URL encoded. */
public final class Wire {
    private Wire() {}

    public static String type(String line) {
        if (line == null) return "";
        int p = line.indexOf('|');
        return p < 0 ? line : line.substring(0, p);
    }

    public static Map<String, String> parse(String line) {
        Map<String, String> out = new LinkedHashMap<String, String>();
        if (line == null) return out;
        String[] parts = line.split("\\|", -1);
        for (int i = 1; i < parts.length; i++) {
            int eq = parts[i].indexOf('=');
            if (eq <= 0) continue;
            String k = dec(parts[i].substring(0, eq));
            String v = dec(parts[i].substring(eq + 1));
            out.put(k, v);
        }
        return out;
    }

    public static String line(String type, Map<String, String> map) {
        StringBuilder sb = new StringBuilder(type == null ? "" : type);
        if (map != null) {
            for (Map.Entry<String, String> e : map.entrySet()) {
                sb.append('|').append(enc(e.getKey())).append('=').append(enc(e.getValue() == null ? "" : e.getValue()));
            }
        }
        return sb.toString();
    }

    public static Map<String, String> kv(String... a) {
        Map<String, String> m = new LinkedHashMap<String, String>();
        for (int i = 0; i + 1 < a.length; i += 2) m.put(a[i], a[i + 1]);
        return m;
    }

    public static String get(Map<String, String> m, String k, String def) {
        String v = m.get(k);
        return v == null ? def : v;
    }

    public static int getInt(Map<String, String> m, String k, int def) {
        try { return Integer.parseInt(get(m, k, String.valueOf(def))); } catch (Exception e) { return def; }
    }

    public static long getLong(Map<String, String> m, String k, long def) {
        try { return Long.parseLong(get(m, k, String.valueOf(def))); } catch (Exception e) { return def; }
    }

    public static double getDouble(Map<String, String> m, String k, double def) {
        try { return Double.parseDouble(get(m, k, String.valueOf(def))); } catch (Exception e) { return def; }
    }

    private static String enc(String s) {
        try { return URLEncoder.encode(s == null ? "" : s, "UTF-8"); }
        catch (UnsupportedEncodingException e) { throw new RuntimeException(e); }
    }

    private static String dec(String s) {
        try { return URLDecoder.decode(s == null ? "" : s, "UTF-8"); }
        catch (UnsupportedEncodingException e) { throw new RuntimeException(e); }
    }
}
