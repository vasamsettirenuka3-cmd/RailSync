package railsync.engine;

/** M1 - String algorithms: KMP, Z-function, Rabin-Karp (double hash), Aho-Corasick. */
public final class StringAlgos {
    private StringAlgos() {}

    // ---------- KMP ----------
    public static int[] failure(String p) {
        int m = p.length();
        int[] f = new int[m];
        int k = 0;
        for (int i = 1; i < m; i++) {
            while (k > 0 && p.charAt(i) != p.charAt(k)) k = f[k - 1];
            if (p.charAt(i) == p.charAt(k)) k++;
            f[i] = k;
        }
        return f;
    }

    public static Util.IntList kmp(String t, String p) {
        Util.IntList res = new Util.IntList();
        int m = p.length();
        if (m == 0) return res;
        int[] f = failure(p);
        int k = 0;
        for (int i = 0; i < t.length(); i++) {
            while (k > 0 && t.charAt(i) != p.charAt(k)) k = f[k - 1];
            if (t.charAt(i) == p.charAt(k)) k++;
            if (k == m) {
                res.add(i - m + 1);
                k = f[k - 1];
            }
        }
        return res;
    }

    // ---------- Z-function ----------
    public static int[] zFunction(String s) {
        int n = s.length();
        int[] z = new int[n];
        if (n == 0) return z;
        z[0] = n;
        int l = 0, r = 0;
        for (int i = 1; i < n; i++) {
            if (i < r) z[i] = Math.min(r - i, z[i - l]);
            while (i + z[i] < n && s.charAt(z[i]) == s.charAt(i + z[i])) z[i]++;
            if (i + z[i] > r) {
                l = i;
                r = i + z[i];
            }
        }
        return z;
    }

    public static Util.IntList zSearch(String t, String p) {
        Util.IntList res = new Util.IntList();
        int m = p.length();
        if (m == 0) return res;
        String s = p + '\u0001' + t;
        int[] z = zFunction(s);
        for (int i = m + 1; i < s.length(); i++) {
            if (z[i] >= m) res.add(i - m - 1);
        }
        return res;
    }

    /** Smallest period p with p + z[p] == n, or n when the string is not periodic. */
    public static int smallestPeriod(int[] z) {
        int n = z.length;
        for (int p = 1; p < n; p++) {
            if (p + z[p] == n) return p;
        }
        return n;
    }

    // ---------- Rabin-Karp with double hashing ----------
    public static Util.IntList rabinKarp(String t, String p) {
        final long M1 = 1_000_000_007L, M2 = 998_244_353L, B = 257L;
        int n = t.length(), m = p.length();
        Util.IntList res = new Util.IntList();
        if (m == 0 || m > n) return res;
        long h1 = 0, h2 = 0, p1 = 0, p2 = 0, pw1 = 1, pw2 = 1;
        for (int i = 0; i < m - 1; i++) {
            pw1 = pw1 * B % M1;
            pw2 = pw2 * B % M2;
        }
        for (int i = 0; i < m; i++) {
            p1 = (p1 * B + p.charAt(i)) % M1;
            p2 = (p2 * B + p.charAt(i)) % M2;
            h1 = (h1 * B + t.charAt(i)) % M1;
            h2 = (h2 * B + t.charAt(i)) % M2;
        }
        for (int i = 0; i + m <= n; i++) {
            if (h1 == p1 && h2 == p2 && t.regionMatches(i, p, 0, m)) res.add(i);
            if (i + m < n) {
                h1 = ((h1 - t.charAt(i) * pw1 % M1 + M1) * B + t.charAt(i + m)) % M1;
                h2 = ((h2 - t.charAt(i) * pw2 % M2 + M2) * B + t.charAt(i + m)) % M2;
            }
        }
        return res;
    }

    // ---------- Aho-Corasick ----------
    public static final class AhoCorasick {
        private static final int A = 256;
        private final String[] kws;
        private final int[] next;
        private final int[] fail;
        private final int[] word;
        private final int[] dict;
        private int nodes = 1;

        public AhoCorasick(String[] keywords) {
            kws = keywords;
            int total = 1;
            for (int i = 0; i < keywords.length; i++) total += keywords[i].length();
            next = new int[total * A];
            for (int i = 0; i < next.length; i++) next[i] = -1;
            fail = new int[total];
            word = new int[total];
            dict = new int[total];
            for (int i = 0; i < total; i++) {
                word[i] = -1;
                dict[i] = -1;
            }
            // 1) trie
            for (int w = 0; w < keywords.length; w++) {
                int cur = 0;
                String k = keywords[w];
                for (int i = 0; i < k.length(); i++) {
                    int c = k.charAt(i) & 255;
                    if (next[cur * A + c] == -1) next[cur * A + c] = nodes++;
                    cur = next[cur * A + c];
                }
                word[cur] = w;
            }
            // 2) failure links by BFS
            int[] q = new int[nodes];
            int qh = 0, qt = 0;
            for (int c = 0; c < A; c++) {
                int v = next[c];
                if (v == -1) next[c] = 0;
                else {
                    fail[v] = 0;
                    q[qt++] = v;
                }
            }
            while (qh < qt) {
                int u = q[qh++];
                dict[u] = (word[fail[u]] >= 0) ? fail[u] : dict[fail[u]];
                for (int c = 0; c < A; c++) {
                    int v = next[u * A + c];
                    if (v == -1) next[u * A + c] = next[fail[u] * A + c];
                    else {
                        fail[v] = next[fail[u] * A + c];
                        q[qt++] = v;
                    }
                }
            }
        }

        /** Returns pairs (startIndex, keywordIndex) flattened in an IntList. */
        public Util.IntList search(String text) {
            Util.IntList res = new Util.IntList();
            int cur = 0;
            for (int i = 0; i < text.length(); i++) {
                cur = next[cur * A + (text.charAt(i) & 255)];
                int t = (word[cur] >= 0) ? cur : dict[cur];
                while (t > 0) {
                    res.add(i - kws[word[t]].length() + 1);
                    res.add(word[t]);
                    t = dict[t];
                }
            }
            return res;
        }

        public String keyword(int i) { return kws[i]; }
        public int keywordCount() { return kws.length; }
    }
}
