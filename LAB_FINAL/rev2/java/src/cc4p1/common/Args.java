package cc4p1.common;

import java.util.HashMap;
import java.util.Map;

public final class Args {
    private final Map<String, String> values = new HashMap<String, String>();

    public Args(String[] raw) {
        for (int i = 0; i < raw.length; i++) {
            String a = raw[i];
            if (!a.startsWith("--")) continue;
            String key = a.substring(2);
            String value = "true";
            int eq = key.indexOf('=');
            if (eq >= 0) {
                value = key.substring(eq + 1);
                key = key.substring(0, eq);
            } else if (i + 1 < raw.length && !raw[i + 1].startsWith("--")) {
                value = raw[++i];
            }
            values.put(key, value);
        }
    }

    public String get(String key, String def) {
        String v = values.get(key);
        return v == null ? def : v;
    }

    public int getInt(String key, int def) {
        try { return Integer.parseInt(get(key, String.valueOf(def))); }
        catch (Exception e) { return def; }
    }

    public long getLong(String key, long def) {
        try { return Long.parseLong(get(key, String.valueOf(def))); }
        catch (Exception e) { return def; }
    }

    public boolean getBoolean(String key, boolean def) {
        String v = values.get(key);
        if (v == null) return def;
        return "true".equalsIgnoreCase(v) || "1".equals(v) || "yes".equalsIgnoreCase(v);
    }
}
