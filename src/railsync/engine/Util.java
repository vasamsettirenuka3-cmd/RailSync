package railsync.engine;

/** Tiny hand-built helpers so the engine never touches java.util.* */
public final class Util {
    private Util() {}

    /** Growable int list (our own ArrayList replacement). */
    public static final class IntList {
        private int[] a = new int[8];
        private int size;

        public void add(int v) {
            if (size == a.length) a = copy(a, size * 2);
            a[size++] = v;
        }
        public int get(int i) { return a[i]; }
        public int size() { return size; }
        public int[] toArray() { return copy(a, size); }
    }

    /** Own xorshift random generator (replacement for java.util.Random). */
    public static final class Rng {
        private long x;
        public Rng(long seed) { x = (seed == 0) ? 88172645463325252L : seed; }
        public long nextLong() {
            x ^= x << 13;
            x ^= x >>> 7;
            x ^= x << 17;
            return x;
        }
        public int nextInt(int bound) {
            if (bound <= 0) return 0;
            long r = nextLong() >>> 1;
            return (int) (r % bound);
        }
    }

    public static int[] copy(int[] src, int newLen) {
        int[] r = new int[newLen];
        int n = src.length < newLen ? src.length : newLen;
        for (int i = 0; i < n; i++) r[i] = src[i];
        return r;
    }

    public static String esc(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n"); break;
                case '\r': break;
                case '\t': b.append("\\t"); break;
                default:
                    if (c < 0x20) b.append(' '); else b.append(c);
            }
        }
        return b.toString();
    }

    public static String ints(int[] a) {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < a.length; i++) {
            if (i > 0) b.append(',');
            b.append(a[i]);
        }
        return b.append(']').toString();
    }

    public static String longs(long[] a) {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < a.length; i++) {
            if (i > 0) b.append(',');
            b.append(a[i]);
        }
        return b.append(']').toString();
    }
}
