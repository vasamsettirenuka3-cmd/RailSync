package railsync.engine;

/** M5 - NP-completeness: 3-SAT solver and verifier, reduction chain, vertex-cover 2-approximation. */
public final class NPApprox {
    private NPApprox() {}

    // ---------- NP verifier: polynomial-time certificate check ----------
    public static boolean verify(int[][] clauses, boolean[] a) {
        for (int c = 0; c < clauses.length; c++) {
            boolean ok = false;
            for (int l = 0; l < clauses[c].length; l++) {
                int lit = clauses[c][l];
                boolean v = a[Math.abs(lit) - 1];
                if (lit < 0) v = !v;
                if (v) {
                    ok = true;
                    break;
                }
            }
            if (!ok) return false;
        }
        return true;
    }

    // ---------- Exponential 3-SAT solver (backtracking with early conflict pruning) ----------
    public static final class SatResult {
        public boolean sat;
        public boolean[] assignment;
        public long nodes;
    }

    public static SatResult solveSat(int nVars, int[][] clauses) {
        final int[] val = new int[nVars];
        for (int i = 0; i < nVars; i++) val[i] = -1;
        final SatResult res = new SatResult();
        res.assignment = new boolean[nVars];
        res.sat = dfs(0, nVars, clauses, val, res);
        if (res.sat) {
            for (int i = 0; i < nVars; i++) res.assignment[i] = val[i] == 1;
        }
        return res;
    }

    private static boolean dfs(int idx, int n, int[][] clauses, int[] val, SatResult res) {
        res.nodes++;
        if (conflict(clauses, val)) return false;
        if (idx == n) return true;
        for (int v = 1; v >= 0; v--) {
            val[idx] = v;
            if (dfs(idx + 1, n, clauses, val, res)) return true;
        }
        val[idx] = -1;
        return false;
    }

    private static boolean conflict(int[][] clauses, int[] val) {
        for (int c = 0; c < clauses.length; c++) {
            boolean allFalse = true;
            for (int l = 0; l < clauses[c].length; l++) {
                int lit = clauses[c][l];
                int x = val[Math.abs(lit) - 1];
                if (x == -1) {
                    allFalse = false;
                    break;
                }
                boolean truth = (x == 1) == (lit > 0);
                if (truth) {
                    allFalse = false;
                    break;
                }
            }
            if (allFalse) return true;
        }
        return false;
    }

    // ---------- Reduction: 3-SAT -> CLIQUE ----------
    public static final class Graph {
        public int n;
        public boolean[][] adj;
        public int[] clauseOf;
        public int[] litOf;
    }

    public static Graph sat3ToClique(int[][] clauses) {
        Graph g = new Graph();
        int total = 0;
        for (int c = 0; c < clauses.length; c++) total += clauses[c].length;
        g.n = total;
        g.clauseOf = new int[total];
        g.litOf = new int[total];
        int k = 0;
        for (int c = 0; c < clauses.length; c++) {
            for (int l = 0; l < clauses[c].length; l++) {
                g.clauseOf[k] = c;
                g.litOf[k] = clauses[c][l];
                k++;
            }
        }
        g.adj = new boolean[total][total];
        for (int i = 0; i < total; i++) {
            for (int j = 0; j < total; j++) {
                g.adj[i][j] = g.clauseOf[i] != g.clauseOf[j] && g.litOf[i] != -g.litOf[j];
            }
        }
        return g;
    }

    /** CLIQUE <-> INDEPENDENT-SET: same vertices, complemented edge set. */
    public static boolean[][] complement(boolean[][] g) {
        int n = g.length;
        boolean[][] c = new boolean[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) c[i][j] = i != j && !g[i][j];
        return c;
    }

    /** Backtracking k-clique search; returns the vertices or null. */
    public static int[] findClique(boolean[][] g, int k) {
        if (k <= 0) return new int[0];
        int[] cur = new int[k];
        if (extend(g, cur, 0, 0, k)) return cur;
        return null;
    }

    private static boolean extend(boolean[][] g, int[] cur, int size, int start, int k) {
        for (int v = start; v < g.length; v++) {
            boolean ok = true;
            for (int i = 0; i < size; i++) {
                if (!g[cur[i]][v]) {
                    ok = false;
                    break;
                }
            }
            if (!ok) continue;
            cur[size] = v;
            if (size + 1 == k) return true;
            if (extend(g, cur, size + 1, v + 1, k)) return true;
        }
        return false;
    }

    // ---------- Vertex cover: 2-approximation via maximal matching ----------
    public static final class VcResult {
        public boolean[] inCover;
        public int[] matchingEdges;
        public int size;
    }

    public static VcResult vcApprox(int n, int[][] edges) {
        VcResult r = new VcResult();
        r.inCover = new boolean[n];
        Util.IntList chosen = new Util.IntList();
        for (int e = 0; e < edges.length; e++) {
            int u = edges[e][0], v = edges[e][1];
            if (!r.inCover[u] && !r.inCover[v]) {
                r.inCover[u] = true;
                r.inCover[v] = true;
                chosen.add(e);
                r.size += 2;
            }
        }
        r.matchingEdges = chosen.toArray();
        return r;
    }

    /** Exact minimum vertex cover by exhaustive subsets (only for small n). */
    public static boolean[] vcExact(int n, int[][] edges) {
        int bestMask = (1 << n) - 1;
        int bestSize = n;
        for (int mask = 0; mask < (1 << n); mask++) {
            int size = Integer.bitCount(mask);
            if (size >= bestSize) continue;
            boolean ok = true;
            for (int e = 0; e < edges.length; e++) {
                if (((mask >> edges[e][0]) & 1) == 0 && ((mask >> edges[e][1]) & 1) == 0) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                bestSize = size;
                bestMask = mask;
            }
        }
        boolean[] r = new boolean[n];
        for (int i = 0; i < n; i++) r[i] = ((bestMask >> i) & 1) == 1;
        return r;
    }
}
