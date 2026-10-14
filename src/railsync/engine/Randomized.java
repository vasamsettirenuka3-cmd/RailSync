package railsync.engine;

/** M6 - Randomised and parallel algorithms. */
public final class Randomized {
    private Randomized() {}

    // ---------- Randomised quicksort (3-way partition) on keys, sorting an index array ----------
    private static long cmp;

    public static long randomizedQuickSort(int[] keys, int[] idx, Util.Rng rng) {
        cmp = 0;
        qs(keys, idx, 0, idx.length - 1, rng);
        return cmp;
    }

    private static void qs(int[] key, int[] idx, int lo, int hi, Util.Rng rng) {
        if (lo >= hi) return;
        int p = lo + rng.nextInt(hi - lo + 1);
        int pivot = key[idx[p]];
        int lt = lo, gt = hi, i = lo;
        while (i <= gt) {
            cmp++;
            int k = key[idx[i]];
            if (k < pivot) swap(idx, lt++, i++);
            else if (k > pivot) swap(idx, i, gt--);
            else i++;
        }
        qs(key, idx, lo, lt - 1, rng);
        qs(key, idx, gt + 1, hi, rng);
    }

    private static void swap(int[] a, int i, int j) {
        int t = a[i];
        a[i] = a[j];
        a[j] = t;
    }

    // ---------- Reservoir sampling (Algorithm R): uniform k-sample of a stream of n items ----------
    public static int[] reservoir(int n, int k, Util.Rng rng) {
        int size = Math.min(k, n);
        int[] r = new int[size];
        for (int i = 0; i < size; i++) r[i] = i;
        for (int i = size; i < n; i++) {
            int j = rng.nextInt(i + 1);
            if (j < size) r[j] = i;
        }
        return r;
    }

    // ---------- Miller-Rabin (valid for n < 2^62) ----------
    public static long mulmod(long a, long b, long m) {
        long r = 0;
        a %= m;
        while (b > 0) {
            if ((b & 1) == 1) {
                r += a;
                if (r >= m) r -= m;
            }
            a <<= 1;
            if (a >= m) a -= m;
            b >>= 1;
        }
        return r;
    }

    public static long powmod(long b, long e, long m) {
        long r = 1;
        b %= m;
        while (e > 0) {
            if ((e & 1) == 1) r = mulmod(r, b, m);
            b = mulmod(b, b, m);
            e >>= 1;
        }
        return r;
    }

    public static final class PrimeResult {
        public boolean prime;
        public long witness = -1;
        public int rounds;
        public String note = "";
    }

    private static final long[] BASES = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37};

    public static PrimeResult millerRabin(long n) {
        PrimeResult res = new PrimeResult();
        if (n < 2) {
            res.note = "n < 2 is not prime";
            return res;
        }
        for (int i = 0; i < BASES.length; i++) {
            if (n == BASES[i]) {
                res.prime = true;
                res.note = "small prime";
                return res;
            }
        }
        long d = n - 1;
        int r = 0;
        while ((d & 1) == 0) {
            d >>= 1;
            r++;
        }
        for (int b = 0; b < BASES.length; b++) {
            long a = BASES[b];
            long x = powmod(a, d, n);
            res.rounds++;
            if (x == 1 || x == n - 1) continue;
            boolean composite = true;
            for (int i = 1; i < r; i++) {
                x = mulmod(x, x, n);
                if (x == n - 1) {
                    composite = false;
                    break;
                }
            }
            if (composite) {
                res.witness = a;
                res.note = "base " + a + " is a Miller-Rabin witness of compositeness";
                return res;
            }
        }
        res.prime = true;
        res.note = "passed all " + BASES.length + " bases (deterministic for n < 3.3e24)";
        return res;
    }

    // ---------- Blelloch work-efficient exclusive scan ----------
    public static long[] blellochExclusive(long[] a) {
        int n = a.length;
        if (n == 0) return new long[0];
        int sz = 1;
        while (sz < n) sz <<= 1;
        long[] t = new long[sz];
        for (int i = 0; i < n; i++) t[i] = a[i];
        for (int d = 1; d < sz; d <<= 1) { // up-sweep (reduce)
            for (int i = 0; i < sz; i += 2 * d) t[i + 2 * d - 1] += t[i + d - 1];
        }
        t[sz - 1] = 0;
        for (int d = sz >> 1; d >= 1; d >>= 1) { // down-sweep
            for (int i = 0; i < sz; i += 2 * d) {
                long x = t[i + d - 1];
                t[i + d - 1] = t[i + 2 * d - 1];
                t[i + 2 * d - 1] += x;
            }
        }
        long[] r = new long[n];
        for (int i = 0; i < n; i++) r[i] = t[i];
        return r;
    }

    // ---------- Parallel reduce with real threads ----------
    public static long parallelSum(final long[] a, int threads) throws InterruptedException {
        final long[] part = new long[threads];
        Thread[] ts = new Thread[threads];
        final int chunk = (a.length + threads - 1) / threads;
        for (int t = 0; t < threads; t++) {
            final int id = t;
            ts[t] = new Thread(new Runnable() {
                public void run() {
                    int lo = id * chunk;
                    int hi = Math.min(a.length, lo + chunk);
                    long s = 0;
                    for (int i = lo; i < hi; i++) s += a[i];
                    part[id] = s;
                }
            });
            ts[t].start();
        }
        long total = 0;
        for (int t = 0; t < threads; t++) {
            ts[t].join();
            total += part[t];
        }
        return total;
    }

    /** Brent's theorem upper bound on p-processor time: T_p <= T1/p + Tinf. */
    public static double brentBound(long t1, long tInf, int p) {
        return (double) t1 / p + tInf;
    }
}
