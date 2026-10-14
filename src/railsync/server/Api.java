package railsync.server;

import railsync.RailData;
import railsync.engine.DPAlgos;
import railsync.engine.FlowNetwork;
import railsync.engine.NPApprox;
import railsync.engine.Randomized;
import railsync.engine.StringAlgos;
import railsync.engine.SuffixStructures;
import railsync.engine.Util;
import railsync.engine.Util.IntList;

/** Maps dashboard requests to engine calls and returns hand-built JSON. */
final class Api {
    private Api() {}

    private static final StringAlgos.AhoCorasick AC = new StringAlgos.AhoCorasick(RailData.KEYWORDS);

    static String handle(String path, Params p) throws Exception {
        switch (path) {
            case "/api/meta": return meta();
            case "/api/search": return search(p);
            case "/api/alerts": return alerts(p);
            case "/api/patterns": return patterns(p);
            case "/api/docs/similar": return docsSimilar(p);
            case "/api/docs/search": return docsSearch(p);
            case "/api/correct": return correct(p);
            case "/api/route": return route(p);
            case "/api/matrix": return matrix(p);
            case "/api/obst": return obst();
            case "/api/flow": return flow(p);
            case "/api/platforms": return platforms();
            case "/api/sat": return sat(p);
            case "/api/vc": return vc();
            case "/api/sort": return sort(p);
            case "/api/reservoir": return reservoir(p);
            case "/api/prime": return prime(p);
            case "/api/scan": return scan();
            default: return err("Unknown endpoint " + path);
        }
    }

    // ---------- tiny JSON helpers ----------
    private static String q(String s) { return "\"" + Util.esc(s) + "\""; }
    private static String err(String m) { return "{\"error\":" + q(m) + "}"; }

    private static String strArr(String[] a) {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < a.length; i++) {
            if (i > 0) b.append(',');
            b.append(q(a[i]));
        }
        return b.append(']').toString();
    }

    private static String boolArr(boolean[] a) {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < a.length; i++) {
            if (i > 0) b.append(',');
            b.append(a[i]);
        }
        return b.append(']').toString();
    }

    private static double avgUs(long nanos, int reps) {
        return Math.round(nanos / (double) reps / 100.0) / 10.0;
    }

    private static int[] parseInts(String s) {
        IntList l = new IntList();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '-' || (c >= '0' && c <= '9')) {
                int sign = 1;
                if (c == '-') {
                    sign = -1;
                    i++;
                }
                long v = 0;
                boolean any = false;
                while (i < s.length() && s.charAt(i) >= '0' && s.charAt(i) <= '9') {
                    v = v * 10 + (s.charAt(i) - '0');
                    if (v > 1_000_000_000L) v = 1_000_000_000L;
                    i++;
                    any = true;
                }
                if (any) l.add((int) (sign * v));
            } else {
                i++;
            }
        }
        return l.toArray();
    }

    private static String xName(int v) { return "x" + v; }

    // ---------- meta ----------
    private static String meta() {
        return "{\"stations\":" + strArr(RailData.STATION_NAMES)
            + ",\"docs\":" + strArr(RailData.DOC_TITLES)
            + ",\"trains\":" + RailData.TRAIN_NO.length
            + ",\"tickets\":" + RailData.TICKETS.length
            + ",\"alerts\":" + RailData.ALERTS.length
            + ",\"tracks\":" + RailData.TRACKS.length + "}";
    }

    // ================= M1 =================
    private static String search(Params p) {
        String qs = p.get("q", "").trim().toLowerCase();
        String scope = p.get("scope", "all");
        String algo = p.get("algo", "kmp");
        if (qs.length() == 0) return err("Type something to search for.");
        int n = RailData.CORPUS_TEXT.length;
        String[] low = new String[n];
        boolean[] in = new boolean[n];
        for (int i = 0; i < n; i++) {
            low[i] = RailData.CORPUS_TEXT[i].toLowerCase();
            in[i] = scope.equals("all") || scope.equals(RailData.CORPUS_TYPE[i]);
        }
        StringBuilder recs = new StringBuilder();
        int total = 0, records = 0;
        boolean consistent = true;
        for (int i = 0; i < n; i++) {
            if (!in[i]) continue;
            IntList a = StringAlgos.kmp(low[i], qs);
            IntList b = StringAlgos.zSearch(low[i], qs);
            IntList c = StringAlgos.rabinKarp(low[i], qs);
            if (a.size() != b.size() || a.size() != c.size()) consistent = false;
            IntList ch = algo.equals("z") ? b : (algo.equals("rk") ? c : a);
            if (ch.size() > 0) {
                if (records > 0) recs.append(',');
                recs.append("{\"type\":").append(q(RailData.CORPUS_TYPE[i]))
                    .append(",\"text\":").append(q(RailData.CORPUS_TEXT[i]))
                    .append(",\"pos\":").append(Util.ints(ch.toArray())).append('}');
                records++;
                total += ch.size();
            }
        }
        int reps = 200, sink = 0;
        long t0 = System.nanoTime();
        for (int r = 0; r < reps; r++)
            for (int i = 0; i < n; i++) if (in[i]) sink += StringAlgos.kmp(low[i], qs).size();
        long kmpNs = System.nanoTime() - t0;
        t0 = System.nanoTime();
        for (int r = 0; r < reps; r++)
            for (int i = 0; i < n; i++) if (in[i]) sink += StringAlgos.zSearch(low[i], qs).size();
        long zNs = System.nanoTime() - t0;
        t0 = System.nanoTime();
        for (int r = 0; r < reps; r++)
            for (int i = 0; i < n; i++) if (in[i]) sink += StringAlgos.rabinKarp(low[i], qs).size();
        long rkNs = System.nanoTime() - t0;
        return "{\"query\":" + q(qs) + ",\"algo\":" + q(algo) + ",\"patternLen\":" + qs.length()
            + ",\"matches\":" + total + ",\"records\":[" + recs + "],\"consistent\":" + consistent
            + ",\"sink\":" + (sink & 1)
            + ",\"timings\":{\"kmp\":" + avgUs(kmpNs, reps) + ",\"z\":" + avgUs(zNs, reps)
            + ",\"rk\":" + avgUs(rkNs, reps) + "},\"failure\":"
            + Util.ints(StringAlgos.failure(qs)) + "}";
    }

    private static String alerts(Params p) {
        String text = p.get("text", "").trim().toLowerCase();
        String[] items = text.length() == 0 ? RailData.ALERTS : new String[] {text};
        int[] counts = new int[AC.keywordCount()];
        StringBuilder b = new StringBuilder("{\"alerts\":[");
        for (int i = 0; i < items.length; i++) {
            IntList hits = AC.search(items[i].toLowerCase());
            if (i > 0) b.append(',');
            b.append("{\"text\":").append(q(items[i])).append(",\"hits\":[");
            for (int h = 0; h < hits.size(); h += 2) {
                if (h > 0) b.append(',');
                int kw = hits.get(h + 1);
                counts[kw]++;
                b.append("{\"keyword\":").append(q(AC.keyword(kw))).append(",\"pos\":").append(hits.get(h)).append('}');
            }
            b.append("]}");
        }
        b.append("],\"keywords\":").append(strArr(RailData.KEYWORDS)).append(",\"counts\":").append(Util.ints(counts)).append('}');
        return b.toString();
    }

    private static String patterns(Params p) {
        String s = p.get("s", RailData.DEFAULT_PATTERN).trim();
        if (s.length() == 0) s = RailData.DEFAULT_PATTERN;
        if (s.length() > 2000) return err("Pattern too long (max 2000 characters).");
        int[] z = StringAlgos.zFunction(s);
        int period = StringAlgos.smallestPeriod(z);
        IntList borders = new IntList();
        for (int i = 1; i < s.length(); i++) if (i + z[i] == s.length()) borders.add(z[i]);
        return "{\"s\":" + q(s) + ",\"z\":" + Util.ints(z) + ",\"period\":" + period
            + ",\"periodic\":" + (period < s.length())
            + ",\"unit\":" + q(s.substring(0, period))
            + ",\"repeats\":" + (s.length() / period)
            + ",\"borders\":" + Util.ints(borders.toArray()) + "}";
    }

    // ================= M2 =================
    private static int docIdx(Params p, String key, int def) {
        int v = p.getInt(key, def);
        if (v < 0) v = 0;
        if (v >= RailData.DOC_TEXT.length) v = RailData.DOC_TEXT.length - 1;
        return v;
    }

    private static String docsSimilar(Params p) {
        int a = docIdx(p, "a", 0), b = docIdx(p, "b", 1);
        int nd = RailData.DOC_TEXT.length;
        String ta = RailData.DOC_TEXT[a], tb = RailData.DOC_TEXT[b];
        SuffixStructures.Common lcs = SuffixStructures.longestCommonSubstring(ta, tb);
        int minLen = Math.min(ta.length(), tb.length());
        int pct = (int) Math.round(100.0 * lcs.length / minLen);
        SuffixStructures.Common rep = SuffixStructures.longestRepeated(ta);
        SuffixStructures.SuffixAutomaton sam = new SuffixStructures.SuffixAutomaton(ta);
        int[] sa = SuffixStructures.suffixArray(ta);
        int[] lcp = SuffixStructures.kasai(ta, sa);
        StringBuilder m = new StringBuilder("[");
        for (int i = 0; i < nd; i++) {
            if (i > 0) m.append(',');
            m.append('[');
            for (int j = 0; j < nd; j++) {
                if (j > 0) m.append(',');
                if (i == j) m.append(100);
                else {
                    SuffixStructures.Common c = SuffixStructures.longestCommonSubstring(RailData.DOC_TEXT[i], RailData.DOC_TEXT[j]);
                    int ml = Math.min(RailData.DOC_TEXT[i].length(), RailData.DOC_TEXT[j].length());
                    m.append((int) Math.round(100.0 * c.length / ml));
                }
            }
            m.append(']');
        }
        m.append(']');
        int maxLcp = 0;
        for (int i = 0; i < lcp.length; i++) if (lcp[i] > maxLcp) maxLcp = lcp[i];
        return "{\"a\":" + q(RailData.DOC_TITLES[a]) + ",\"b\":" + q(RailData.DOC_TITLES[b])
            + ",\"lenA\":" + ta.length() + ",\"lenB\":" + tb.length()
            + ",\"lcsLen\":" + lcs.length + ",\"lcs\":" + q(lcs.text) + ",\"similarity\":" + pct
            + ",\"repeatedLen\":" + rep.length + ",\"repeated\":" + q(rep.text)
            + ",\"distinct\":" + sam.distinctSubstrings() + ",\"states\":" + sam.states()
            + ",\"maxLcp\":" + maxLcp
            + ",\"titles\":" + strArr(RailData.DOC_TITLES) + ",\"matrix\":" + m + "}";
    }

    private static String docsSearch(Params p) {
        String qs = p.get("q", "").trim().toLowerCase();
        if (qs.length() == 0) return err("Type a phrase to look for in the service documents.");
        StringBuilder b = new StringBuilder("{\"query\":" + q(qs) + ",\"docs\":[");
        int total = 0;
        for (int d = 0; d < RailData.DOC_TEXT.length; d++) {
            String t = RailData.DOC_TEXT[d].toLowerCase();
            int[] sa = SuffixStructures.suffixArray(t);
            IntList hits = SuffixStructures.search(t, sa, qs);
            int[] pos = hits.toArray();
            // sort positions ascending (insertion sort)
            for (int i = 1; i < pos.length; i++) {
                int v = pos[i], j = i - 1;
                while (j >= 0 && pos[j] > v) {
                    pos[j + 1] = pos[j];
                    j--;
                }
                pos[j + 1] = v;
            }
            boolean viaSam = new SuffixStructures.SuffixAutomaton(t).contains(qs);
            total += pos.length;
            if (d > 0) b.append(',');
            b.append("{\"title\":").append(q(RailData.DOC_TITLES[d])).append(",\"text\":").append(q(RailData.DOC_TEXT[d]))
                .append(",\"pos\":").append(Util.ints(pos)).append(",\"automatonAgrees\":")
                .append(viaSam == (pos.length > 0)).append('}');
        }
        return b.append("],\"total\":").append(total).append(",\"len\":").append(qs.length()).append('}').toString();
    }

    // ================= M3 =================
    private static String correct(Params p) {
        String w = p.get("word", "").trim().toLowerCase();
        if (w.length() == 0) return err("Type a (mis-spelt) station name.");
        int n = RailData.STATION_NAMES.length;
        int[] lev = new int[n], dam = new int[n], order = new int[n];
        for (int i = 0; i < n; i++) {
            String s = RailData.STATION_NAMES[i].toLowerCase();
            lev[i] = DPAlgos.levenshtein(w, s);
            dam[i] = DPAlgos.damerau(w, s);
            order[i] = i;
        }
        for (int i = 1; i < n; i++) {
            int v = order[i], j = i - 1;
            while (j >= 0 && (dam[order[j]] > dam[v] || (dam[order[j]] == dam[v] && lev[order[j]] > lev[v]))) {
                order[j + 1] = order[j];
                j--;
            }
            order[j + 1] = v;
        }
        StringBuilder b = new StringBuilder("{\"word\":" + q(w) + ",\"suggestions\":[");
        for (int k = 0; k < 5 && k < n; k++) {
            int i = order[k];
            if (k > 0) b.append(',');
            b.append("{\"station\":").append(q(RailData.STATION_NAMES[i])).append(",\"levenshtein\":").append(lev[i])
                .append(",\"damerau\":").append(dam[i]).append('}');
        }
        return b.append("]}").toString();
    }

    private static String route(Params p) {
        int[] raw = parseInts(p.get("stations", "0,1,2,3,5,9"));
        int[] sel = new int[raw.length];
        int cnt = 0;
        for (int i = 0; i < raw.length; i++) {
            int v = raw[i];
            if (v < 0 || v >= RailData.STATION_NAMES.length) continue;
            boolean dup = false;
            for (int j = 0; j < cnt; j++) if (sel[j] == v) dup = true;
            if (!dup) sel[cnt++] = v;
        }
        if (cnt < 2) return err("Pick at least two different stations.");
        if (cnt > 11) return err("Bitmask DP is exponential: pick at most 11 stations.");
        int[][] d = new int[cnt][cnt];
        for (int i = 0; i < cnt; i++)
            for (int j = 0; j < cnt; j++) d[i][j] = RailData.distanceKm(sel[i], sel[j]);
        long t0 = System.nanoTime();
        DPAlgos.Tour tour = DPAlgos.bitmaskTsp(d);
        long us = (System.nanoTime() - t0) / 1000;
        int naive = 0;
        for (int i = 0; i < cnt; i++) naive += d[i][(i + 1) % cnt];
        StringBuilder b = new StringBuilder("{\"count\":" + cnt + ",\"order\":[");
        for (int i = 0; i < cnt; i++) {
            if (i > 0) b.append(',');
            b.append(q(RailData.STATION_NAMES[sel[tour.order[i]]]));
        }
        b.append("],\"cost\":").append(tour.cost).append(",\"naive\":").append(naive)
            .append(",\"states\":").append((1L << cnt) * cnt).append(",\"micros\":").append(us).append('}');
        return b.toString();
    }

    private static String matrix(Params p) {
        int[] dims = parseInts(p.get("dims", "40,20,30,10,30"));
        if (dims.length < 3) return err("Enter at least three dimensions, e.g. 40,20,30,10,30.");
        if (dims.length > 40) return err("Too many dimensions (max 40).");
        for (int i = 0; i < dims.length; i++) if (dims[i] < 1 || dims[i] > 1000) return err("Each dimension must be between 1 and 1000.");
        DPAlgos.Chain c = DPAlgos.matrixChain(dims);
        long naive = 0;
        for (int k = 1; k < dims.length - 1; k++) naive += (long) dims[0] * dims[k] * dims[k + 1];
        return "{\"dims\":" + Util.ints(dims) + ",\"matrices\":" + (dims.length - 1) + ",\"cost\":" + c.cost
            + ",\"naive\":" + naive + ",\"paren\":" + q(c.paren) + "}";
    }

    private static long balancedCost(int[] f, int lo, int hi, int depth) {
        if (lo > hi) return 0;
        int mid = (lo + hi) >>> 1;
        return (long) f[mid] * depth + balancedCost(f, lo, mid - 1, depth + 1) + balancedCost(f, mid + 1, hi, depth + 1);
    }

    private static String obst() {
        int n = RailData.STATION_NAMES.length;
        int[] order = new int[n];
        for (int i = 0; i < n; i++) order[i] = i;
        for (int i = 1; i < n; i++) {
            int v = order[i], j = i - 1;
            while (j >= 0 && RailData.STATION_NAMES[order[j]].compareTo(RailData.STATION_NAMES[v]) > 0) {
                order[j + 1] = order[j];
                j--;
            }
            order[j + 1] = v;
        }
        int[] f = new int[n];
        long sum = 0;
        for (int i = 0; i < n; i++) {
            f[i] = RailData.STATION_FREQ[order[i]];
            sum += f[i];
        }
        DPAlgos.Obst r = DPAlgos.optimalBst(f);
        long bal = balancedCost(f, 0, n - 1, 1);
        StringBuilder b = new StringBuilder("{\"nodes\":[");
        for (int i = 0; i < n; i++) {
            if (i > 0) b.append(',');
            b.append("{\"station\":").append(q(RailData.STATION_NAMES[order[i]])).append(",\"freq\":").append(f[i])
                .append(",\"depth\":").append(r.depth[i]).append('}');
        }
        b.append("],\"cost\":").append(r.cost).append(",\"balanced\":").append(bal)
            .append(",\"avg\":").append(Math.round(100.0 * r.cost / sum) / 100.0)
            .append(",\"avgBalanced\":").append(Math.round(100.0 * bal / sum) / 100.0).append('}');
        return b.toString();
    }

    // ================= M4 =================
    private static String flow(Params p) {
        int ns = RailData.STATION_NAMES.length;
        int s = p.getInt("source", 9), t = p.getInt("sink", 8);
        if (s < 0 || s >= ns || t < 0 || t >= ns) return err("Unknown station.");
        if (s == t) return err("Source and sink must be different stations.");
        int[][] tr = RailData.TRACKS;
        FlowNetwork g = new FlowNetwork(ns, tr.length * 2);
        int[] fwd = new int[tr.length], bwd = new int[tr.length];
        for (int i = 0; i < tr.length; i++) {
            fwd[i] = g.addEdge(tr[i][0], tr[i][1], tr[i][2]);
            bwd[i] = g.addEdge(tr[i][1], tr[i][0], tr[i][2]);
        }
        int reps = 300;
        int f1 = 0, f2 = 0, f3 = 0, a1 = 0, a2 = 0, a3 = 0, ph = 0;
        long t0 = System.nanoTime();
        for (int r = 0; r < reps; r++) { f1 = g.fordFulkerson(s, t); a1 = g.augmentations; }
        long n1 = System.nanoTime() - t0;
        t0 = System.nanoTime();
        for (int r = 0; r < reps; r++) { f2 = g.edmondsKarp(s, t); a2 = g.augmentations; }
        long n2 = System.nanoTime() - t0;
        t0 = System.nanoTime();
        for (int r = 0; r < reps; r++) { f3 = g.dinic(s, t); a3 = g.augmentations; ph = g.phases; }
        long n3 = System.nanoTime() - t0;
        boolean[] reach = g.reachable(s);
        StringBuilder edges = new StringBuilder();
        for (int i = 0; i < tr.length; i++) {
            int net = g.flowOn(fwd[i]) - g.flowOn(bwd[i]);
            int u = tr[i][0], v = tr[i][1];
            if (net < 0) {
                int x = u; u = v; v = x; net = -net;
            }
            if (i > 0) edges.append(',');
            edges.append("{\"from\":").append(q(RailData.STATION_NAMES[u])).append(",\"to\":").append(q(RailData.STATION_NAMES[v]))
                .append(",\"cap\":").append(tr[i][2]).append(",\"flow\":").append(net).append('}');
        }
        StringBuilder cut = new StringBuilder();
        int cutCap = 0, cc = 0;
        for (int e = 0; e < g.edgeCount(); e += 2) {
            int u = g.from(e), v = g.to(e);
            if (reach[u] && !reach[v]) {
                if (cc++ > 0) cut.append(',');
                cut.append("{\"from\":").append(q(RailData.STATION_NAMES[u])).append(",\"to\":").append(q(RailData.STATION_NAMES[v]))
                    .append(",\"cap\":").append(g.capacity(e)).append('}');
                cutCap += g.capacity(e);
            }
        }
        return "{\"source\":" + q(RailData.STATION_NAMES[s]) + ",\"sink\":" + q(RailData.STATION_NAMES[t])
            + ",\"flow\":" + f3 + ",\"agree\":" + (f1 == f2 && f2 == f3)
            + ",\"algos\":[{\"name\":\"Ford-Fulkerson (DFS)\",\"flow\":" + f1 + ",\"steps\":" + a1 + ",\"stepLabel\":\"augmentations\",\"us\":" + avgUs(n1, reps) + "},"
            + "{\"name\":\"Edmonds-Karp (BFS)\",\"flow\":" + f2 + ",\"steps\":" + a2 + ",\"stepLabel\":\"augmentations\",\"us\":" + avgUs(n2, reps) + "},"
            + "{\"name\":\"Dinic (level graph)\",\"flow\":" + f3 + ",\"steps\":" + ph + ",\"stepLabel\":\"phases (" + a3 + " blocking paths)\",\"us\":" + avgUs(n3, reps) + "}],"
            + "\"edges\":[" + edges + "],\"cut\":[" + cut + "],\"cutCap\":" + cutCap + "}";
    }

    private static String platforms() {
        int nt = RailData.ELIGIBLE.length;
        int pc = 0;
        for (int i = 0; i < nt; i++) pc += RailData.ELIGIBLE[i].length;
        int[][] pairs = new int[pc][2];
        int k = 0;
        for (int i = 0; i < nt; i++)
            for (int j = 0; j < RailData.ELIGIBLE[i].length; j++) {
                pairs[k][0] = i;
                pairs[k][1] = RailData.ELIGIBLE[i][j];
                k++;
            }
        FlowNetwork.Matching m = FlowNetwork.bipartite(nt, RailData.PLATFORMS, pairs);
        StringBuilder asg = new StringBuilder();
        int cover = 0;
        for (int i = 0; i < nt; i++) {
            if (i > 0) asg.append(',');
            StringBuilder el = new StringBuilder();
            for (int j = 0; j < RailData.ELIGIBLE[i].length; j++) {
                if (j > 0) el.append(", ");
                el.append('P').append(RailData.ELIGIBLE[i][j] + 1);
            }
            asg.append("{\"train\":").append(q(RailData.TRAIN_NO[i] + " " + RailData.TRAIN_NAME[i]))
                .append(",\"eligible\":").append(q(el.toString()))
                .append(",\"platform\":").append(m.matchOfLeft[i] < 0 ? "null" : q("P" + (m.matchOfLeft[i] + 1)))
                .append(",\"inCover\":").append(m.coverLeft[i]).append('}');
            if (m.coverLeft[i]) cover++;
        }
        StringBuilder pl = new StringBuilder("[");
        for (int j = 0; j < RailData.PLATFORMS; j++) {
            if (j > 0) pl.append(',');
            pl.append("{\"name\":\"P").append(j + 1).append("\",\"inCover\":").append(m.coverRight[j]).append('}');
            if (m.coverRight[j]) cover++;
        }
        pl.append(']');
        return "{\"trains\":" + nt + ",\"platforms\":" + RailData.PLATFORMS + ",\"matching\":" + m.size
            + ",\"perfect\":" + (m.size == nt) + ",\"assignments\":[" + asg + "],\"platformCover\":" + pl
            + ",\"coverSize\":" + cover + ",\"konig\":" + (cover == m.size) + "}";
    }

    // ================= M5 =================
    private static String lit(int l) { return (l < 0 ? "¬x" : "x") + Math.abs(l); }

    private static String sat(Params p) {
        String raw = p.get("clauses", "1,2,3;-1,2,4;1,-2,-4;-2,3,4;-1,-3,4");
        if (p.get("preset", "").equals("unsat")) raw = "1,1,1;-1,-1,-1";
        String[] parts = raw.split(";");
        if (parts.length == 0 || parts.length > 10) return err("Use between 1 and 10 clauses separated by ';'.");
        int[][] cl = new int[parts.length][];
        int nv = 0;
        for (int i = 0; i < parts.length; i++) {
            cl[i] = parseInts(parts[i]);
            if (cl[i].length == 0 || cl[i].length > 3) return err("Clause " + (i + 1) + " must have 1 to 3 literals.");
            for (int j = 0; j < cl[i].length; j++) {
                if (cl[i][j] == 0) return err("Literal 0 is not allowed (variables start at 1).");
                nv = Math.max(nv, Math.abs(cl[i][j]));
            }
        }
        if (nv > 20) return err("Use at most 20 variables.");
        NPApprox.SatResult r = NPApprox.solveSat(nv, cl);
        boolean verified = r.sat && NPApprox.verify(cl, r.assignment);
        NPApprox.Graph g = NPApprox.sat3ToClique(cl);
        int edgeCount = 0;
        for (int i = 0; i < g.n; i++)
            for (int j = i + 1; j < g.n; j++) if (g.adj[i][j]) edgeCount++;
        String[] labels = new String[g.n];
        for (int i = 0; i < g.n; i++) labels[i] = "C" + (g.clauseOf[i] + 1) + ":" + lit(g.litOf[i]);
        int k = cl.length;
        int[] clique = NPApprox.findClique(g.adj, k);
        boolean[][] comp = NPApprox.complement(g.adj);
        boolean isOk = false;
        boolean[] back = new boolean[nv];
        boolean backOk = false;
        StringBuilder cq = new StringBuilder("[");
        StringBuilder vcv = new StringBuilder("[");
        if (clique != null) {
            isOk = true;
            for (int i = 0; i < clique.length; i++)
                for (int j = i + 1; j < clique.length; j++) if (comp[clique[i]][clique[j]]) isOk = false;
            boolean[] inIs = new boolean[g.n];
            for (int i = 0; i < clique.length; i++) {
                inIs[clique[i]] = true;
                int l = g.litOf[clique[i]];
                back[Math.abs(l) - 1] = l > 0;
                if (i > 0) cq.append(',');
                cq.append(q(labels[clique[i]]));
            }
            backOk = NPApprox.verify(cl, back);
            int c = 0;
            for (int v = 0; v < g.n; v++) {
                if (!inIs[v]) {
                    if (c++ > 0) vcv.append(',');
                    vcv.append(q(labels[v]));
                }
            }
        }
        cq.append(']');
        vcv.append(']');
        StringBuilder cs = new StringBuilder("[");
        for (int i = 0; i < cl.length; i++) {
            if (i > 0) cs.append(',');
            StringBuilder one = new StringBuilder("(");
            for (int j = 0; j < cl[i].length; j++) {
                if (j > 0) one.append(" ∨ ");
                one.append(lit(cl[i][j]));
            }
            cs.append(q(one.append(')').toString()));
        }
        cs.append(']');
        return "{\"vars\":" + nv + ",\"clauses\":" + cs + ",\"sat\":" + r.sat + ",\"assignment\":" + boolArr(r.assignment)
            + ",\"nodes\":" + r.nodes + ",\"verified\":" + verified
            + ",\"graph\":{\"vertices\":" + g.n + ",\"edges\":" + edgeCount + ",\"k\":" + k + ",\"labels\":" + strArr(labels) + "}"
            + ",\"clique\":" + (clique == null ? "null" : cq.toString())
            + ",\"equivalent\":" + ((clique != null) == r.sat)
            + ",\"independentSet\":" + (clique == null ? "null" : String.valueOf(k)) + ",\"independentOk\":" + isOk
            + ",\"vertexCover\":" + (clique == null ? "null" : String.valueOf(g.n - k)) + ",\"coverVertices\":" + vcv
            + ",\"fromClique\":" + boolArr(back) + ",\"fromCliqueOk\":" + backOk + "}";
    }

    private static String vc() {
        int ns = RailData.STATION_NAMES.length;
        int[][] tr = RailData.TRACKS;
        int[][] edges = new int[tr.length][2];
        for (int i = 0; i < tr.length; i++) {
            edges[i][0] = tr[i][0];
            edges[i][1] = tr[i][1];
        }
        // Hub-first heuristic: scan busy tracks (high combined station degree) first.
        int[] deg = new int[ns];
        for (int i = 0; i < edges.length; i++) {
            deg[edges[i][0]]++;
            deg[edges[i][1]]++;
        }
        for (int i = 1; i < edges.length; i++) {
            int[] cur = edges[i];
            int w = deg[cur[0]] + deg[cur[1]];
            int j = i - 1;
            while (j >= 0 && deg[edges[j][0]] + deg[edges[j][1]] < w) {
                edges[j + 1] = edges[j];
                j--;
            }
            edges[j + 1] = cur;
        }
        NPApprox.VcResult ap = NPApprox.vcApprox(ns, edges);
        boolean[] ex = NPApprox.vcExact(ns, edges);
        int exSize = 0;
        for (int i = 0; i < ns; i++) if (ex[i]) exSize++;
        StringBuilder a = new StringBuilder("["), e = new StringBuilder("["), m = new StringBuilder("[");
        int ca = 0, ce = 0;
        for (int i = 0; i < ns; i++) {
            if (ap.inCover[i]) {
                if (ca++ > 0) a.append(',');
                a.append(q(RailData.STATION_NAMES[i]));
            }
            if (ex[i]) {
                if (ce++ > 0) e.append(',');
                e.append(q(RailData.STATION_NAMES[i]));
            }
        }
        for (int i = 0; i < ap.matchingEdges.length; i++) {
            int[] ed = edges[ap.matchingEdges[i]];
            if (i > 0) m.append(',');
            m.append(q(RailData.STATION_NAMES[ed[0]] + " – " + RailData.STATION_NAMES[ed[1]]));
        }
        return "{\"stations\":" + ns + ",\"tracks\":" + tr.length + ",\"approxSize\":" + ap.size + ",\"approx\":" + a.append(']')
            + ",\"matching\":" + m.append(']') + ",\"exactSize\":" + exSize + ",\"exact\":" + e.append(']')
            + ",\"ratio\":" + Math.round(100.0 * ap.size / exSize) / 100.0 + ",\"within2\":" + (ap.size <= 2 * exSize) + "}";
    }

    // ================= M6 =================
    private static String sort(Params p) {
        int k = Math.max(1, Math.min(RailData.TRAIN_NO.length, p.getInt("k", 5)));
        int nt = RailData.TRAIN_NO.length;
        int[] idx = new int[nt];
        for (int i = 0; i < nt; i++) idx[i] = i;
        Randomized.randomizedQuickSort(RailData.TRAIN_DELAY, idx, new Util.Rng(System.nanoTime() | 1));
        StringBuilder top = new StringBuilder("[");
        for (int i = 0; i < k; i++) {
            int t = idx[nt - 1 - i];
            if (i > 0) top.append(',');
            top.append("{\"no\":").append(q(RailData.TRAIN_NO[t])).append(",\"name\":").append(q(RailData.TRAIN_NAME[t]))
                .append(",\"delay\":").append(RailData.TRAIN_DELAY[t]).append('}');
        }
        top.append(']');
        int n = 20000;
        int[] keys = new int[n], id2 = new int[n];
        Util.Rng gen = new Util.Rng(12345);
        for (int i = 0; i < n; i++) {
            keys[i] = gen.nextInt(181);
            id2[i] = i;
        }
        long t0 = System.nanoTime();
        long cmp = Randomized.randomizedQuickSort(keys, id2, new Util.Rng(System.nanoTime() | 1));
        long ms = (System.nanoTime() - t0) / 1_000_000;
        boolean sorted = true;
        for (int i = 1; i < n; i++) if (keys[id2[i - 1]] > keys[id2[i]]) sorted = false;
        double bound = 2.0 * n * Math.log(n);
        return "{\"top\":" + top + ",\"n\":" + n + ",\"comparisons\":" + cmp + ",\"bound\":" + Math.round(bound)
            + ",\"nlogn\":" + Math.round(n * Math.log(n) / Math.log(2)) + ",\"sorted\":" + sorted + ",\"ms\":" + ms + "}";
    }

    private static String reservoir(Params p) {
        int k = Math.max(1, Math.min(20, p.getInt("k", 5)));
        int n = 5000;
        String[] types = {"ARR", "DEP", "LATE", "HALT"};
        Util.Rng rng = new Util.Rng(System.nanoTime() | 1);
        int[] pick = Randomized.reservoir(n, k, rng);
        StringBuilder b = new StringBuilder("{\"streamSize\":" + n + ",\"k\":" + k + ",\"sample\":[");
        for (int i = 0; i < pick.length; i++) {
            int e = pick[i];
            if (i > 0) b.append(',');
            String ev = "EVT-" + (10000 + e) + "  train " + RailData.TRAIN_NO[e % RailData.TRAIN_NO.length]
                + "  " + types[(e * 7 + e / 3) % 4];
            b.append(q(ev));
        }
        int trials = 20000, size = 10, kk = 3;
        int[] counts = new int[size];
        Util.Rng r2 = new Util.Rng(987654321L);
        for (int t = 0; t < trials; t++) {
            int[] s = Randomized.reservoir(size, kk, r2);
            for (int i = 0; i < s.length; i++) counts[s[i]]++;
        }
        b.append("],\"trials\":").append(trials).append(",\"uniform\":").append(Util.ints(counts))
            .append(",\"expected\":").append(trials * kk / size).append('}');
        return b.toString();
    }

    private static String prime(Params p) {
        String raw = p.get("n", "").trim();
        long n;
        try {
            n = Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return err("Enter a whole number (up to 4,611,686,018,427,387,903).");
        }
        if (n >= (1L << 62)) return err("Please keep n below 2^62 = 4,611,686,018,427,387,904.");
        long t0 = System.nanoTime();
        Randomized.PrimeResult r = Randomized.millerRabin(n);
        long us = (System.nanoTime() - t0) / 1000;
        String check = "null";
        if (n < 1_000_000_000_000L) {
            boolean tp = n >= 2;
            for (long i = 2; i * i <= n && tp; i++) if (n % i == 0) tp = false;
            check = String.valueOf(tp == r.prime);
        }
        return "{\"n\":" + q(raw) + ",\"prime\":" + r.prime + ",\"witness\":" + r.witness + ",\"rounds\":" + r.rounds
            + ",\"note\":" + q(r.note) + ",\"trialAgrees\":" + check + ",\"micros\":" + us + "}";
    }

    private static String scan() throws InterruptedException {
        int n = RailData.HOURLY.length;
        long[] a = new long[n];
        for (int i = 0; i < n; i++) a[i] = RailData.HOURLY[i];
        long[] ex = Randomized.blellochExclusive(a);
        long[] inc = new long[n];
        for (int i = 0; i < n; i++) inc[i] = ex[i] + a[i];
        long par = Randomized.parallelSum(a, 4);
        long seq = 0;
        for (int i = 0; i < n; i++) seq += a[i];
        int sz = 1, lg = 0;
        while (sz < n) {
            sz <<= 1;
            lg++;
        }
        long work = 2L * (sz - 1), span = 2L * lg;
        int[] ps = {1, 2, 4, 8, 16};
        StringBuilder br = new StringBuilder("[");
        for (int i = 0; i < ps.length; i++) {
            double tp = Randomized.brentBound(work, span, ps[i]);
            if (i > 0) br.append(',');
            br.append("{\"p\":").append(ps[i]).append(",\"bound\":").append(Math.round(tp * 100.0) / 100.0)
                .append(",\"speedup\":").append(Math.round(100.0 * work / tp) / 100.0).append('}');
        }
        br.append(']');
        return "{\"hourly\":" + Util.longs(a) + ",\"inclusive\":" + Util.longs(inc) + ",\"exclusive\":" + Util.longs(ex)
            + ",\"parallelSum\":" + par + ",\"sequentialSum\":" + seq + ",\"agree\":" + (par == seq)
            + ",\"work\":" + work + ",\"span\":" + span + ",\"paddedTo\":" + sz + ",\"brent\":" + br + "}";
    }
}
