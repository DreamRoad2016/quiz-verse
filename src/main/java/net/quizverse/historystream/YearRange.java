package net.quizverse.historystream;

final class YearRange {

    private YearRange() {
    }

    /** Inclusive year ranges intersect. */
    static boolean intersects(int aFrom, int aTo, int bFrom, int bTo) {
        return aFrom <= bTo && aTo >= bFrom;
    }

    static boolean yearInRange(int year, int from, int to) {
        return year >= from && year <= to;
    }

    /** 1-based ordinal of year within an inclusive range (BCE-safe). */
    static int ordinalInRange(int year, int from, int to) {
        if (!yearInRange(year, from, to)) {
            return 0;
        }
        return year - from + 1;
    }
}
