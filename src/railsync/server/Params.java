package railsync.server;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/** Minimal query-string parser (no java.util collections). */
final class Params {
    private final String[] keys = new String[32];
    private final String[] vals = new String[32];
    private int n;

    Params(String raw) {
        if (raw == null || raw.length() == 0) return;
        int i = 0;
        while (i < raw.length() && n < keys.length) {
            int amp = raw.indexOf('&', i);
            if (amp < 0) amp = raw.length();
            String pair = raw.substring(i, amp);
            int eq = pair.indexOf('=');
            String k = eq < 0 ? pair : pair.substring(0, eq);
            String v = eq < 0 ? "" : pair.substring(eq + 1);
            keys[n] = decode(k);
            vals[n] = decode(v);
            n++;
            i = amp + 1;
        }
    }

    private static String decode(String s) {
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return s;
        }
    }

    String get(String key, String def) {
        for (int i = 0; i < n; i++) if (keys[i].equals(key)) return vals[i];
        return def;
    }

    int getInt(String key, int def) {
        try {
            return Integer.parseInt(get(key, "").trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
