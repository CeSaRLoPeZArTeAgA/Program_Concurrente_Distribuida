package cc4p1.common;

import java.util.HashMap;
import java.util.Map;

public final class Args {
    private final Map<String, String> m = new HashMap<String, String>();

    public Args(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.startsWith("--")) {
                String key = a.substring(2);
                String val = "true";
                int eq = key.indexOf('=');
                if (eq >= 0) {
                    val = key.substring(eq + 1);
                    key = key.substring(0, eq);
                } else if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    val = args[++i];
                }
                m.put(key, val);
            }
        }
    }

    public String get(String k, String def) { String v = m.get(k); return v == null ? def : v; }
    public int getInt(String k, int def) { try { return Integer.parseInt(get(k, String.valueOf(def))); } catch (Exception e) { return def; } }
    public long getLong(String k, long def) { try { return Long.parseLong(get(k, String.valueOf(def))); } catch (Exception e) { return def; } }
    public double getDouble(String k, double def) { try { return Double.parseDouble(get(k, String.valueOf(def))); } catch (Exception e) { return def; } }
    public boolean has(String k) { return m.containsKey(k); }
}
