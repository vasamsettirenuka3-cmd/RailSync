# RailSync - Intelligent Railway Operations Platform (DSA-3)

A Java project for the DSA-3 advanced-algorithms course. A hand-built algorithm engine
(no `java.util` inside `railsync.engine`, as the syllabus requires) is served through the
JDK's built-in web server to an HTML/CSS/JS dashboard.

## How to run in Eclipse

1. **File > New > Java Project**, name it `RailSync`, untick "Create module-info.java".
2. In the Package Explorer, copy the contents of this folder's `src` into the project's `src`
   (so you get `src/railsync/...`), and copy the `web` folder into the **project root**
   (same level as `src`).
3. Right-click `src/railsync/EngineSelfTest.java > Run As > Java Application`.
   You should see every algorithm PASS (quick proof that your engine is correct).
4. Right-click `src/railsync/server/Main.java > Run As > Java Application`.
   The console prints `RailSync is running -> http://localhost:8080`.
5. Open **http://localhost:8080** in your browser.

If Eclipse underlines `com.sun.net.httpserver` with "Access restriction" (older Java 8 setups):
Window > Preferences > Java > Compiler > Errors/Warnings > Deprecated and restricted API >
set "Forbidden reference (access rules)" to **Warning**. On Java 11 or newer there is no issue.

If port 8080 is busy: Run > Run Configurations > Arguments > Program arguments: `9090`.

## Project layout

```
src/railsync/engine/      the algorithms (hand-built, no java.util)
  Util.java               IntList, Rng (own random), JSON helpers
  StringAlgos.java        M1  KMP, Z-function, Rabin-Karp (double hash), Aho-Corasick
  SuffixStructures.java   M2  suffix array, Kasai LCP, suffix automaton, LCS / repeats
  DPAlgos.java            M3  Levenshtein, Damerau, bitmask TSP, matrix-chain, optimal BST
  FlowNetwork.java        M4  Ford-Fulkerson, Edmonds-Karp, Dinic, min-cut, matching, Konig
  NPApprox.java           M5  3-SAT, 3-SAT->CLIQUE->IS->VC, vertex-cover 2-approximation
  Randomized.java         M6  randomised quicksort, reservoir, Miller-Rabin, Blelloch, parallel reduce, Brent
src/railsync/RailData.java        sample railway data (stations, trains, tickets, alerts, documents)
src/railsync/EngineSelfTest.java  console tests with known answers
src/railsync/server/Main.java     starts the web server
src/railsync/server/Api.java      connects each dashboard card to the engine
src/railsync/server/Params.java   query-string parser
web/index.html, style.css, app.js the dashboard
```

## Where each problem-statement item lives

| Problem statement | Class / method | Dashboard card |
|---|---|---|
| KMP train numbers, stations, alerts | `StringAlgos.kmp` | M1 Search railway records |
| Z-function repeated patterns | `StringAlgos.zFunction`, `smallestPeriod` | M1 Repeated operation patterns |
| Rabin-Karp ticket / booking codes | `StringAlgos.rabinKarp` | M1 Search (scope: tickets) |
| Aho-Corasick alert keywords | `StringAlgos.AhoCorasick` | M1 Alert keyword scanner |
| Suffix arrays index documents | `SuffixStructures.suffixArray`, `search` | M2 Substring search |
| LCP / Kasai repeated descriptions | `SuffixStructures.kasai`, `longestRepeated` | M2 Document similarity |
| Suffix automata substring search | `SuffixStructures.SuffixAutomaton` | M2 both cards |
| Levenshtein / Damerau correction | `DPAlgos.levenshtein`, `damerau` | M3 Station name correction |
| Bitmask DP routes | `DPAlgos.bitmaskTsp` | M3 Best route |
| Matrix-chain | `DPAlgos.matrixChain` | M3 Cheapest order |
| Optimal BST | `DPAlgos.optimalBst` | M3 Optimal station index tree |
| Ford-Fulkerson / Edmonds-Karp / Dinic | `FlowNetwork.fordFulkerson`, `edmondsKarp`, `dinic` | M4 Passenger-flow capacity |
| Bipartite matching, Konig | `FlowNetwork.bipartite` | M4 Train to platform assignment |
| Max-flow min-cut bottlenecks | `FlowNetwork.reachable` | M4 Passenger-flow capacity |
| SAT / 3-SAT, 3-SAT->CLIQUE->IS->VC | `NPApprox.solveSat`, `sat3ToClique`, `complement`, `findClique` | M5 Scheduling constraints |
| Vertex-cover 2-approximation | `NPApprox.vcApprox`, `vcExact` | M5 Critical connections |
| Randomised QuickSort | `Randomized.randomizedQuickSort` | M6 Most delayed trains |
| Reservoir sampling | `Randomized.reservoir` | M6 Sample the event stream |
| Miller-Rabin | `Randomized.millerRabin` | M6 Large-number primality |
| Blelloch scan, parallel reduce, Brent | `blellochExclusive`, `parallelSum`, `brentBound` | M6 Cumulative passenger statistics |

## Notes for your viva

- The suffix array uses prefix doubling, O(n log^2 n) (the practical version in your syllabus).
  SA-IS is explained in class at concept level; it is not implemented here.
- Aho-Corasick and the suffix automaton index characters by `c & 255`, which is fine for the
  English sample data. For Indian-language (Unicode) text, switch the `A = 256` alphabet to a map.
- Miller-Rabin uses a hand-written `mulmod` (double-and-add) so it works for n < 2^62 without BigInteger.
- Every card cross-checks itself (algorithms agree, certificates verify, max-flow = min-cut,
  matching = Konig cover), which is useful evidence in a demo.
