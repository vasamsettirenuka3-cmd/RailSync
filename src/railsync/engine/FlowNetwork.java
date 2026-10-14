package railsync.engine;

/** M4 - Network flow: Ford-Fulkerson, Edmonds-Karp, Dinic, min-cut, bipartite matching, Konig cover. */
public final class FlowNetwork {
    private static final int INF = 1 << 29;

    public final int n;
    private final int[] head;
    private final int[] to, cap, orig, nxt;
    private int m;
    public int augmentations;
    public int phases;

    public FlowNetwork(int n, int maxPairs) {
        this.n = n;
        head = new int[n];
        for (int i = 0; i < n; i++) head[i] = -1;
        int e = maxPairs * 2 + 2;
        to = new int[e];
        cap = new int[e];
        orig = new int[e];
        nxt = new int[e];
    }

    /** Adds u->v with capacity c plus the residual twin; returns the forward edge id (even). */
    public int addEdge(int u, int v, int c) {
        int id = m;
        to[m] = v; cap[m] = c; orig[m] = c; nxt[m] = head[u]; head[u] = m++;
        to[m] = u; cap[m] = 0; orig[m] = 0; nxt[m] = head[v]; head[v] = m++;
        return id;
    }

    public int edgeCount() { return m; }
    public int from(int e) { return to[e ^ 1]; }
    public int to(int e) { return to[e]; }
    public int capacity(int e) { return orig[e]; }
    public int flowOn(int e) { return orig[e] - cap[e]; }

    public void reset() {
        for (int e = 0; e < m; e++) cap[e] = orig[e];
        augmentations = 0;
        phases = 0;
    }

    // ---------- Ford-Fulkerson (DFS augmenting paths) ----------
    public int fordFulkerson(int s, int t) {
        reset();
        int flow = 0;
        while (true) {
            boolean[] vis = new boolean[n];
            int d = dfsAug(s, t, INF, vis);
            if (d == 0) break;
            flow += d;
            augmentations++;
        }
        return flow;
    }

    private int dfsAug(int u, int t, int f, boolean[] vis) {
        if (u == t) return f;
        vis[u] = true;
        for (int e = head[u]; e != -1; e = nxt[e]) {
            if (cap[e] > 0 && !vis[to[e]]) {
                int d = dfsAug(to[e], t, Math.min(f, cap[e]), vis);
                if (d > 0) {
                    cap[e] -= d;
                    cap[e ^ 1] += d;
                    return d;
                }
            }
        }
        return 0;
    }

    // ---------- Edmonds-Karp (BFS shortest augmenting path), O(V E^2) ----------
    public int edmondsKarp(int s, int t) {
        reset();
        int flow = 0;
        int[] parE = new int[n];
        int[] q = new int[n];
        while (true) {
            for (int i = 0; i < n; i++) parE[i] = -1;
            int qh = 0, qt = 0;
            q[qt++] = s;
            parE[s] = -2;
            while (qh < qt && parE[t] == -1) {
                int u = q[qh++];
                for (int e = head[u]; e != -1; e = nxt[e]) {
                    int v = to[e];
                    if (cap[e] > 0 && parE[v] == -1) {
                        parE[v] = e;
                        q[qt++] = v;
                    }
                }
            }
            if (parE[t] == -1) break;
            int bott = INF;
            for (int v = t; v != s; v = to[parE[v] ^ 1]) bott = Math.min(bott, cap[parE[v]]);
            for (int v = t; v != s; v = to[parE[v] ^ 1]) {
                cap[parE[v]] -= bott;
                cap[parE[v] ^ 1] += bott;
            }
            flow += bott;
            augmentations++;
        }
        return flow;
    }

    // ---------- Dinic (level graph + blocking flow), O(V^2 E) ----------
    public int dinic(int s, int t) {
        reset();
        int flow = 0;
        int[] level = new int[n];
        int[] it = new int[n];
        int[] q = new int[n];
        while (bfsLevel(s, t, level, q)) {
            phases++;
            for (int i = 0; i < n; i++) it[i] = head[i];
            int f;
            while ((f = dfsBlock(s, t, INF, level, it)) > 0) {
                flow += f;
                augmentations++;
            }
        }
        return flow;
    }

    private boolean bfsLevel(int s, int t, int[] level, int[] q) {
        for (int i = 0; i < n; i++) level[i] = -1;
        int qh = 0, qt = 0;
        q[qt++] = s;
        level[s] = 0;
        while (qh < qt) {
            int u = q[qh++];
            for (int e = head[u]; e != -1; e = nxt[e]) {
                if (cap[e] > 0 && level[to[e]] < 0) {
                    level[to[e]] = level[u] + 1;
                    q[qt++] = to[e];
                }
            }
        }
        return level[t] >= 0;
    }

    private int dfsBlock(int u, int t, int f, int[] level, int[] it) {
        if (u == t) return f;
        for (; it[u] != -1; it[u] = nxt[it[u]]) {
            int e = it[u];
            int v = to[e];
            if (cap[e] > 0 && level[v] == level[u] + 1) {
                int d = dfsBlock(v, t, Math.min(f, cap[e]), level, it);
                if (d > 0) {
                    cap[e] -= d;
                    cap[e ^ 1] += d;
                    return d;
                }
            }
        }
        return 0;
    }

    // ---------- Min-cut: vertices reachable from s in the final residual graph ----------
    public boolean[] reachable(int s) {
        boolean[] seen = new boolean[n];
        int[] q = new int[n];
        int qh = 0, qt = 0;
        q[qt++] = s;
        seen[s] = true;
        while (qh < qt) {
            int u = q[qh++];
            for (int e = head[u]; e != -1; e = nxt[e]) {
                if (cap[e] > 0 && !seen[to[e]]) {
                    seen[to[e]] = true;
                    q[qt++] = to[e];
                }
            }
        }
        return seen;
    }

    // ---------- Bipartite matching via max-flow + Konig's theorem ----------
    public static final class Matching {
        public int size;
        public int[] matchOfLeft;
        public boolean[] coverLeft;
        public boolean[] coverRight;
    }

    public static Matching bipartite(int left, int right, int[][] pairs) {
        FlowNetwork g = new FlowNetwork(left + right + 2, left + right + pairs.length);
        int s = 0, t = left + right + 1;
        for (int i = 0; i < left; i++) g.addEdge(s, 1 + i, 1);
        for (int j = 0; j < right; j++) g.addEdge(1 + left + j, t, 1);
        int[] eid = new int[pairs.length];
        for (int k = 0; k < pairs.length; k++) eid[k] = g.addEdge(1 + pairs[k][0], 1 + left + pairs[k][1], 1);
        Matching r = new Matching();
        r.size = g.dinic(s, t);
        r.matchOfLeft = new int[left];
        for (int i = 0; i < left; i++) r.matchOfLeft[i] = -1;
        for (int k = 0; k < pairs.length; k++) {
            if (g.flowOn(eid[k]) == 1) r.matchOfLeft[pairs[k][0]] = pairs[k][1];
        }
        boolean[] reach = g.reachable(s);
        r.coverLeft = new boolean[left];
        r.coverRight = new boolean[right];
        for (int i = 0; i < left; i++) r.coverLeft[i] = !reach[1 + i];
        for (int j = 0; j < right; j++) r.coverRight[j] = reach[1 + left + j];
        return r;
    }
}
