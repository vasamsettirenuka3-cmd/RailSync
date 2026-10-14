package railsync.engine;

/** M3 - Advanced DP: edit distance, bitmask TSP, matrix-chain, optimal BST. */
public final class DPAlgos {
    private DPAlgos() {}

    private static int min3(int a, int b, int c) {
        int m = a < b ? a : b;
        return m < c ? m : c;
    }

    /** Wagner-Fischer Levenshtein distance. */
    public static int levenshtein(String a, String b) {
        int n = a.length(), m = b.length();
        int[][] d = new int[n + 1][m + 1];
        for (int i = 0; i <= n; i++) d[i][0] = i;
        for (int j = 0; j <= m; j++) d[0][j] = j;
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                int c = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                d[i][j] = min3(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + c);
            }
        }
        return d[n][m];
    }

    /** Damerau-Levenshtein (optimal string alignment): adds adjacent transposition. */
    public static int damerau(String a, String b) {
        int n = a.length(), m = b.length();
        int[][] d = new int[n + 1][m + 1];
        for (int i = 0; i <= n; i++) d[i][0] = i;
        for (int j = 0; j <= m; j++) d[0][j] = j;
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                int c = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                d[i][j] = min3(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + c);
                if (i > 1 && j > 1 && a.charAt(i - 1) == b.charAt(j - 2) && a.charAt(i - 2) == b.charAt(j - 1)) {
                    d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + 1);
                }
            }
        }
        return d[n][m];
    }

    // ---------- Bitmask DP: travelling salesman in O(2^n * n^2) ----------
    public static final class Tour {
        public int cost;
        public int[] order;
    }

    public static Tour bitmaskTsp(int[][] dist) {
        int n = dist.length;
        Tour t = new Tour();
        if (n == 0) {
            t.order = new int[0];
            return t;
        }
        if (n == 1) {
            t.order = new int[] {0};
            return t;
        }
        final int INF = Integer.MAX_VALUE / 4;
        int full = 1 << n;
        int[][] dp = new int[full][n];
        int[][] par = new int[full][n];
        for (int m = 0; m < full; m++)
            for (int i = 0; i < n; i++) dp[m][i] = INF;
        dp[1][0] = 0;
        for (int mask = 1; mask < full; mask++) {
            if ((mask & 1) == 0) continue;
            for (int last = 0; last < n; last++) {
                if (((mask >> last) & 1) == 0 || dp[mask][last] >= INF) continue;
                for (int nxt = 0; nxt < n; nxt++) {
                    if (((mask >> nxt) & 1) != 0) continue;
                    int nm = mask | (1 << nxt);
                    int c = dp[mask][last] + dist[last][nxt];
                    if (c < dp[nm][nxt]) {
                        dp[nm][nxt] = c;
                        par[nm][nxt] = last;
                    }
                }
            }
        }
        int best = INF, bestLast = 1;
        for (int last = 1; last < n; last++) {
            int c = dp[full - 1][last] + dist[last][0];
            if (c < best) {
                best = c;
                bestLast = last;
            }
        }
        t.cost = best;
        t.order = new int[n];
        int mask = full - 1, cur = bestLast;
        for (int k = n - 1; k >= 1; k--) {
            t.order[k] = cur;
            int prev = par[mask][cur];
            mask ^= (1 << cur);
            cur = prev;
        }
        t.order[0] = 0;
        return t;
    }

    // ---------- Interval DP: matrix-chain multiplication, O(n^3) ----------
    public static final class Chain {
        public long cost;
        public String paren = "";
    }

    public static Chain matrixChain(int[] p) {
        int n = p.length - 1;
        Chain res = new Chain();
        if (n < 1) return res;
        long[][] m = new long[n][n];
        int[][] s = new int[n][n];
        for (int len = 2; len <= n; len++) {
            for (int i = 0; i + len - 1 < n; i++) {
                int j = i + len - 1;
                m[i][j] = Long.MAX_VALUE;
                for (int k = i; k < j; k++) {
                    long c = m[i][k] + m[k + 1][j] + (long) p[i] * p[k + 1] * p[j + 1];
                    if (c < m[i][j]) {
                        m[i][j] = c;
                        s[i][j] = k;
                    }
                }
            }
        }
        res.cost = m[0][n - 1];
        StringBuilder sb = new StringBuilder();
        paren(0, n - 1, s, sb);
        res.paren = sb.toString();
        return res;
    }

    private static void paren(int i, int j, int[][] s, StringBuilder sb) {
        if (i == j) {
            sb.append('A').append(i + 1);
            return;
        }
        sb.append('(');
        paren(i, s[i][j], s, sb);
        sb.append(" x ");
        paren(s[i][j] + 1, j, s, sb);
        sb.append(')');
    }

    // ---------- Interval DP: optimal binary search tree (keys sorted, access frequencies) ----------
    public static final class Obst {
        public long cost;
        public int[] depth; // depth of key i in the optimal tree (root = 1)
    }

    public static Obst optimalBst(int[] f) {
        int n = f.length;
        Obst res = new Obst();
        res.depth = new int[n];
        if (n == 0) return res;
        long[] pre = new long[n + 1];
        for (int i = 0; i < n; i++) pre[i + 1] = pre[i] + f[i];
        long[][] c = new long[n + 2][n + 2];
        int[][] root = new int[n + 2][n + 2];
        for (int len = 1; len <= n; len++) {
            for (int i = 1; i + len - 1 <= n; i++) {
                int j = i + len - 1;
                c[i][j] = Long.MAX_VALUE;
                long sum = pre[j] - pre[i - 1];
                for (int r = i; r <= j; r++) {
                    long t = c[i][r - 1] + c[r + 1][j] + sum;
                    if (t < c[i][j]) {
                        c[i][j] = t;
                        root[i][j] = r;
                    }
                }
            }
        }
        res.cost = c[1][n];
        assign(1, n, 1, root, res.depth);
        return res;
    }

    private static void assign(int i, int j, int d, int[][] root, int[] depth) {
        if (i > j) return;
        int r = root[i][j];
        depth[r - 1] = d;
        assign(i, r - 1, d + 1, root, depth);
        assign(r + 1, j, d + 1, root, depth);
    }
}
