package railsync;

/** Sample railway dataset used by the dashboard (all fictional). */
public final class RailData {
    private RailData() {}

    public static final String[] STATION_NAMES = {
        "Vijayawada Jn", "Guntur Jn", "Visakhapatnam", "Rajahmundry", "Tenali Jn", "Eluru",
        "Nellore", "Tirupati", "Chennai Central", "Hyderabad Deccan", "Warangal", "Ongole"
    };
    public static final double[] LAT = {16.52, 16.30, 17.69, 17.00, 16.24, 16.71, 14.44, 13.63, 13.08, 17.39, 17.97, 15.50};
    public static final double[] LON = {80.62, 80.44, 83.22, 81.78, 80.64, 81.10, 79.99, 79.42, 80.27, 78.47, 79.60, 80.05};
    public static final int[] STATION_FREQ = {95, 60, 85, 40, 25, 20, 35, 55, 90, 80, 30, 28};

    /** {stationA, stationB, passengers-per-hour capacity}, each track usable in both directions. */
    public static final int[][] TRACKS = {
        {0, 5, 900}, {5, 3, 800}, {3, 2, 850}, {0, 1, 750}, {1, 4, 500}, {0, 4, 700},
        {4, 11, 650}, {11, 6, 700}, {6, 7, 600}, {7, 8, 750}, {6, 8, 500}, {0, 10, 800},
        {10, 9, 900}, {1, 9, 400}, {1, 11, 450}, {0, 11, 400}
    };

    public static final String[] TRAIN_NO = {
        "12711", "12841", "12727", "12615", "17644", "12759", "12797", "12805", "12723", "12737", "12713", "22203"
    };
    public static final String[] TRAIN_NAME = {
        "Pinakini Express", "Coromandel Express", "Godavari Express", "Grand Trunk Express", "Kakinada Express",
        "Charminar Express", "Venkatadri Express", "Janmabhoomi Express", "Telangana Express", "Gautami Express",
        "Satavahana Express", "Duronto Express"
    };
    public static final int[] TRAIN_DELAY = {12, 35, 0, 48, 7, 22, 15, 5, 61, 9, 27, 3};

    /** Platforms each of the first 8 trains may use (platform indices 0..5 = P1..P6). */
    public static final int[][] ELIGIBLE = {
        {0, 1}, {0}, {0, 1}, {1, 2}, {2, 3}, {3}, {3, 4}, {4, 5}
    };
    public static final int PLATFORMS = 6;

    public static final String[] TICKETS = {
        "PNR2841957301 | Ravi Kumar | 12711 | S4-23 | VJA-MAS",
        "PNR2841957302 | Anitha Rao | 12711 | S4-24 | VJA-MAS",
        "PNR7710293847 | Suresh Babu | 12841 | B2-11 | VSKP-MAS",
        "PNR5520018834 | Lakshmi Devi | 12727 | A1-05 | VSKP-SC",
        "PNR9032746120 | Imran Khan | 12615 | S9-41 | MAS-NZM",
        "PNR2847701923 | Kavya Reddy | 12759 | CC-07 | SC-MAS",
        "PNR6618204455 | Mohan Raju | 12797 | S2-18 | TPTY-SC",
        "PNR3305519872 | Priya Sharma | 12805 | S6-33 | VSKP-SC",
        "PNR1184450036 | Arjun Das | 12723 | S1-02 | SC-NDLS",
        "PNR8809134412 | Sneha Patel | 12737 | CC-12 | SC-HWH"
    };

    public static final String[] ALERTS = {
        "Train 12841 delayed by 35 minutes due to dense fog near Rajahmundry",
        "Signal failure reported at Tenali Jn, services running late",
        "Track maintenance between Nellore and Ongole from 22:00 to 04:00",
        "Heavy flood warning: Godavari bridge speed restricted, delay expected",
        "Train 12615 cancelled due to staff strike at Chennai Central",
        "Overcrowding at Vijayawada Jn platform 4; passengers advised to use alternate gates",
        "Fire alarm reported in pantry car of 12759, train halted at Warangal",
        "Minor derailment of goods wagon cleared near Guntur Jn",
        "Platform change: 12727 will arrive on platform 2",
        "Dense fog expected, delay likely on the Hyderabad corridor"
    };

    public static final String[] KEYWORDS = {
        "delay", "cancel", "fog", "flood", "signal failure", "derail", "fire", "strike",
        "overcrowd", "maintenance", "platform change"
    };

    public static final String[] DOC_TITLES = {
        "Pinakini Express service sheet", "Venkatadri Express service sheet",
        "Freight service note", "Godavari Express service sheet"
    };
    public static final String[] DOC_TEXT = {
        "Pinakini Express runs daily between Chennai Central and Vijayawada Jn with stops at Nellore, Ongole and Tenali. "
            + "Air conditioned chair car and sleeper coaches are available. Pantry car serves meals on board.",
        "Venkatadri Express runs daily between Tirupati and Hyderabad Deccan with stops at Nellore, Ongole and Vijayawada. "
            + "Sleeper coaches and chair car are available. Pantry car serves meals on board.",
        "Goods freight service between Visakhapatnam port and Vijayawada Jn operates at night. "
            + "No passenger coaches are attached.",
        "Godavari Express runs daily between Visakhapatnam and Hyderabad Deccan with stops at Rajahmundry, Vijayawada and Warangal. "
            + "Air conditioned chair car and sleeper coaches are available. Pantry car serves meals on board."
    };

    public static final int[] HOURLY = {
        120, 80, 60, 50, 90, 300, 720, 1100, 1450, 980, 640, 600,
        700, 720, 650, 680, 900, 1380, 1520, 1100, 760, 480, 300, 180
    };

    public static final String DEFAULT_PATTERN = "ADADADLADADADL";

    public static final String[] CORPUS_TYPE;
    public static final String[] CORPUS_TEXT;

    static {
        int n = TRAIN_NO.length + STATION_NAMES.length + TICKETS.length + ALERTS.length;
        CORPUS_TYPE = new String[n];
        CORPUS_TEXT = new String[n];
        int k = 0;
        for (int i = 0; i < TRAIN_NO.length; i++) {
            CORPUS_TYPE[k] = "train";
            CORPUS_TEXT[k++] = TRAIN_NO[i] + " " + TRAIN_NAME[i];
        }
        for (int i = 0; i < STATION_NAMES.length; i++) {
            CORPUS_TYPE[k] = "station";
            CORPUS_TEXT[k++] = STATION_NAMES[i];
        }
        for (int i = 0; i < TICKETS.length; i++) {
            CORPUS_TYPE[k] = "ticket";
            CORPUS_TEXT[k++] = TICKETS[i];
        }
        for (int i = 0; i < ALERTS.length; i++) {
            CORPUS_TYPE[k] = "alert";
            CORPUS_TEXT[k++] = ALERTS[i];
        }
    }

    /** Approximate rail distance in km between two stations from their coordinates. */
    public static int distanceKm(int a, int b) {
        double dy = (LAT[a] - LAT[b]) * 111.0;
        double dx = (LON[a] - LON[b]) * 106.0;
        return (int) Math.round(Math.sqrt(dx * dx + dy * dy) * 1.2);
    }
}
