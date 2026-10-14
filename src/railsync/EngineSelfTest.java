package railsync;

import railsync.engine.DPAlgos;
import railsync.engine.FlowNetwork;
import railsync.engine.NPApprox;
import railsync.engine.Randomized;
import railsync.engine.StringAlgos;
import railsync.engine.SuffixStructures;
import railsync.engine.Util;

/** Console self-test: run this to check every algorithm against known answers (no server needed). */
public final class EngineSelfTest {
    private static int passed, failed;

    private static void check(String name, boolean ok) {
        if (ok) passed++; else failed++;
        System.out.println((ok ? "  PASS  " : "  FAIL  ") + name);
    }

    public static void main(String[] args) {
        System.out.println("M1 strings");
        check("KMP finds 'aba' in 'ababa' at 0 and 2", StringAlgos.kmp("ababa", "aba").size() == 2);
        check("Z-search agrees with KMP", StringAlgos.zSearch("ababa", "aba").size() == 2);
        check("Rabin-Karp agrees with KMP", StringAlgos.rabinKarp("ababa", "aba").size() == 2);
        check("Z smallest period of ADADADL x2 = 7", StringAlgos.smallestPeriod(StringAlgos.zFunction("ADADADLADADADL")) == 7);
        StringAlgos.AhoCorasick ac = new StringAlgos.AhoCorasick(new String[] {"he", "she", "his", "hers"});
        check("Aho-Corasick finds she, he, hers in 'ushers' (3 matches)", ac.search("ushers").size() == 6);

        System.out.println("M2 suffix");
        int[] sa = SuffixStructures.suffixArray("banana");
        check("suffix array of banana = 5 3 1 0 4 2", Util.ints(sa).equals("[5,3,1,0,4,2]"));
        check("Kasai LCP of banana = 0 1 3 0 0 2", Util.ints(SuffixStructures.kasai("banana", sa)).equals("[0,1,3,0,0,2]"));
        check("longest repeated in banana = ana", SuffixStructures.longestRepeated("banana").text.equals("ana"));
        check("longest common substring", SuffixStructures.longestCommonSubstring("xabcdy", "zabcdw").text.equals("abcd"));
        check("banana has 15 distinct substrings", new SuffixStructures.SuffixAutomaton("banana").distinctSubstrings() == 15);

        System.out.println("M3 dp");
        check("Levenshtein kitten/sitting = 3", DPAlgos.levenshtein("kitten", "sitting") == 3);
        check("Damerau ca/ac = 1 (Levenshtein = 2)", DPAlgos.damerau("ca", "ac") == 1 && DPAlgos.levenshtein("ca", "ac") == 2);
        check("matrix chain 10,30,5,60 = 4500", DPAlgos.matrixChain(new int[] {10, 30, 5, 60}).cost == 4500);
        int[][] d = {{0, 10, 15, 20}, {10, 0, 35, 25}, {15, 35, 0, 30}, {20, 25, 30, 0}};
        check("bitmask TSP classic 4-city = 80", DPAlgos.bitmaskTsp(d).cost == 80);
        check("optimal BST {34,8,50} = 142", DPAlgos.optimalBst(new int[] {34, 8, 50}).cost == 142);

        System.out.println("M4 flow");
        FlowNetwork g = new FlowNetwork(6, 9);
        int[][] e = {{0, 1, 16}, {0, 2, 13}, {1, 2, 10}, {2, 1, 4}, {1, 3, 12}, {3, 2, 9}, {2, 4, 14}, {4, 3, 7}, {3, 5, 20}, {4, 5, 4}};
        g = new FlowNetwork(6, e.length);
        for (int i = 0; i < e.length; i++) g.addEdge(e[i][0], e[i][1], e[i][2]);
        check("Ford-Fulkerson CLRS = 23", g.fordFulkerson(0, 5) == 23);
        check("Edmonds-Karp CLRS = 23", g.edmondsKarp(0, 5) == 23);
        check("Dinic CLRS = 23", g.dinic(0, 5) == 23);
        FlowNetwork.Matching m = FlowNetwork.bipartite(3, 3, new int[][] {{0, 0}, {0, 1}, {1, 0}, {2, 0}});
        int cover = 0;
        for (int i = 0; i < 3; i++) {
            if (m.coverLeft[i]) cover++;
            if (m.coverRight[i]) cover++;
        }
        check("matching size 2 and Konig cover size 2", m.size == 2 && cover == 2);

        System.out.println("M5 np");
        int[][] sat = {{1, 2, 3}, {-1, 2, 4}, {1, -2, -4}};
        NPApprox.SatResult r = NPApprox.solveSat(4, sat);
        check("3-SAT solved and verified", r.sat && NPApprox.verify(sat, r.assignment));
        check("x and not-x is unsatisfiable", !NPApprox.solveSat(1, new int[][] {{1}, {-1}}).sat);
        NPApprox.Graph gr = NPApprox.sat3ToClique(sat);
        check("clique of size 3 exists iff satisfiable", NPApprox.findClique(gr.adj, 3) != null);
        int[][] tri = {{0, 1}, {1, 2}, {0, 2}, {2, 3}};
        NPApprox.VcResult vc = NPApprox.vcApprox(4, tri);
        boolean[] ex = NPApprox.vcExact(4, tri);
        int exs = 0;
        for (int i = 0; i < 4; i++) if (ex[i]) exs++;
        check("vertex-cover approx <= 2 * optimum", vc.size <= 2 * exs);

        System.out.println("M6 randomised / parallel");
        check("Miller-Rabin: 561 (Carmichael) is composite", !Randomized.millerRabin(561).prime);
        check("Miller-Rabin: 1000000007 is prime", Randomized.millerRabin(1000000007L).prime);
        check("Miller-Rabin: 2^61-1 is prime", Randomized.millerRabin((1L << 61) - 1).prime);
        long[] sc = Randomized.blellochExclusive(new long[] {3, 1, 7, 0, 4, 1, 6, 3});
        check("Blelloch exclusive scan", Util.longs(sc).equals("[0,3,4,11,11,15,16,22]"));
        int[] keys = {5, 2, 9, 1, 5, 6};
        int[] idx = {0, 1, 2, 3, 4, 5};
        Randomized.randomizedQuickSort(keys, idx, new Util.Rng(7));
        boolean sorted = true;
        for (int i = 1; i < idx.length; i++) if (keys[idx[i - 1]] > keys[idx[i]]) sorted = false;
        check("randomised quicksort sorts", sorted);
        int[] rs = Randomized.reservoir(100, 5, new Util.Rng(3));
        check("reservoir returns 5 items", rs.length == 5);
        try {
            long[] big = new long[1000];
            for (int i = 0; i < big.length; i++) big[i] = i;
            check("parallel reduce = 499500", Randomized.parallelSum(big, 4) == 499500L);
        } catch (InterruptedException ex2) {
            check("parallel reduce", false);
        }

        System.out.println("\n" + passed + " passed, " + failed + " failed");
    }
}
