package railsync.engine;

/** M2 - Suffix structures: suffix array (prefix doubling), Kasai LCP, suffix automaton. */
public final class SuffixStructures {
    private SuffixStructures() {}

    // ---------- Suffix array: prefix doubling, O(n log^2 n) ----------
    public static int[] suffixArray(String s) {
        final int n = s.length();
        int[] sa = new int[n];
        int[] rk = new int[n];
        int[] tmp = new int[n];
        int[] buf = new int[n];
        for (int i = 0; i < n; i++) {
            sa[i] = i;
            rk[i] = s.charAt(i);
        }
        if (n <= 1) return sa;
        for (int k = 1; ; k <<= 1) {
            mergeSort(sa, buf, 0, n, rk, k, n);
            tmp[sa[0]] = 0;
            for (int i = 1; i < n; i++) {
                tmp[sa[i]] = tmp[sa[i - 1]] + (cmp(sa[i - 1], sa[i], rk, k, n) < 0 ? 1 : 0);
            }
            for (int i = 0; i < n; i++) rk[i] = tmp[i];
            if (rk[sa[n - 1]] == n - 1) break;
        }
        return sa;
    }

    private static int cmp(int a, int b, int[] rk, int k, int n) {
        if (rk[a] != rk[b]) return rk[a] < rk[b] ? -1 : 1;
        int ra = a + k < n ? rk[a + k] : -1;
        int rb = b + k < n ? rk[b + k] : -1;
        return ra == rb ? 0 : (ra < rb ? -1 : 1);
    }

    private static void mergeSort(int[] a, int[] buf, int lo, int hi, int[] rk, int k, int n) {
        if (hi - lo < 2) return;
        int mid = (lo + hi) >>> 1;
        mergeSort(a, buf, lo, mid, rk, k, n);
        mergeSort(a, buf, mid, hi, rk, k, n);
        int i = lo, j = mid, o = lo;
        while (i < mid && j < hi) {
            if (cmp(a[i], a[j], rk, k, n) <= 0) buf[o++] = a[i++];
            else buf[o++] = a[j++];
        }
        while (i < mid) buf[o++] = a[i++];
        while (j < hi) buf[o++] = a[j++];
        for (int x = lo; x < hi; x++) a[x] = buf[x];
    }

    // ---------- Kasai LCP in O(n); lcp[i] = LCP(suffix sa[i-1], suffix sa[i]) ----------
    public static int[] kasai(String s, int[] sa) {
        int n = s.length();
        int[] rank = new int[n];
        int[] lcp = new int[n];
        for (int i = 0; i < n; i++) rank[sa[i]] = i;
        int h = 0;
        for (int i = 0; i < n; i++) {
            if (rank[i] > 0) {
                int j = sa[rank[i] - 1];
                while (i + h < n && j + h < n && s.charAt(i + h) == s.charAt(j + h)) h++;
                lcp[rank[i]] = h;
                if (h > 0) h--;
            } else {
                h = 0;
            }
        }
        return lcp;
    }

    // ---------- Pattern search with binary search over the suffix array ----------
    public static Util.IntList search(String text, int[] sa, String p) {
        Util.IntList res = new Util.IntList();
        int n = text.length(), m = p.length();
        if (m == 0 || n == 0) return res;
        int lo = 0, hi = n;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (compareSuffix(text, sa[mid], p) < 0) lo = mid + 1; else hi = mid;
        }
        int i = lo;
        while (i < n && text.regionMatches(sa[i], p, 0, m)) {
            res.add(sa[i]);
            i++;
        }
        return res;
    }

    private static int compareSuffix(String t, int start, String p) {
        int m = p.length();
        for (int i = 0; i < m; i++) {
            if (start + i >= t.length()) return -1;
            char a = t.charAt(start + i), b = p.charAt(i);
            if (a != b) return a < b ? -1 : 1;
        }
        return 0;
    }

    // ---------- Applications ----------
    public static final class Common {
        public int length;
        public String text = "";
    }

    /** Longest common substring of A and B via suffix array of A + sep + B. */
    public static Common longestCommonSubstring(String a, String b) {
        String s = a + '\u0001' + b;
        int la = a.length();
        int[] sa = suffixArray(s);
        int[] lcp = kasai(s, sa);
        Common best = new Common();
        for (int i = 1; i < s.length(); i++) {
            int x = sa[i - 1], y = sa[i];
            if (x == la || y == la) continue;
            if ((x < la) != (y < la) && lcp[i] > best.length) {
                best.length = lcp[i];
                best.text = s.substring(y, y + lcp[i]);
            }
        }
        return best;
    }

    /** Longest repeated substring inside one text = max of the LCP array. */
    public static Common longestRepeated(String s) {
        Common best = new Common();
        if (s.length() < 2) return best;
        int[] sa = suffixArray(s);
        int[] lcp = kasai(s, sa);
        for (int i = 1; i < s.length(); i++) {
            if (lcp[i] > best.length) {
                best.length = lcp[i];
                best.text = s.substring(sa[i], sa[i] + lcp[i]);
            }
        }
        return best;
    }

    // ---------- Suffix automaton ----------
    public static final class SuffixAutomaton {
        private static final int A = 256;
        private final int[] len, link, next;
        private int size = 1, last = 0;

        public SuffixAutomaton(String s) {
            int cap = Math.max(2, 2 * s.length() + 2);
            len = new int[cap];
            link = new int[cap];
            next = new int[cap * A];
            for (int i = 0; i < next.length; i++) next[i] = -1;
            link[0] = -1;
            for (int i = 0; i < s.length(); i++) extend(s.charAt(i) & 255);
        }

        private void extend(int c) {
            int cur = size++;
            len[cur] = len[last] + 1;
            int p = last;
            while (p != -1 && next[p * A + c] == -1) {
                next[p * A + c] = cur;
                p = link[p];
            }
            if (p == -1) {
                link[cur] = 0;
            } else {
                int q = next[p * A + c];
                if (len[p] + 1 == len[q]) {
                    link[cur] = q;
                } else {
                    int clone = size++;
                    len[clone] = len[p] + 1;
                    for (int x = 0; x < A; x++) next[clone * A + x] = next[q * A + x];
                    link[clone] = link[q];
                    while (p != -1 && next[p * A + c] == q) {
                        next[p * A + c] = clone;
                        p = link[p];
                    }
                    link[q] = clone;
                    link[cur] = clone;
                }
            }
            last = cur;
        }

        public boolean contains(String p) {
            int v = 0;
            for (int i = 0; i < p.length(); i++) {
                v = next[v * A + (p.charAt(i) & 255)];
                if (v == -1) return false;
            }
            return true;
        }

        public long distinctSubstrings() {
            long total = 0;
            for (int v = 1; v < size; v++) total += len[v] - len[link[v]];
            return total;
        }

        public int states() { return size; }
    }
}
