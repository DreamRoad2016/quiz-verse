package net.quizverse.historystream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YearRangeTest {

    @Test
    void intersectsInclusiveEdges() {
        assertTrue(YearRange.intersects(960, 1127, 1127, 1279));
        assertTrue(YearRange.intersects(386, 534, 420, 589));
        assertFalse(YearRange.intersects(220, 280, 317, 420));
    }

    @Test
    void yearInRange() {
        assertTrue(YearRange.yearInRange(1949, 1912, 1949));
        assertFalse(YearRange.yearInRange(1950, 1912, 1949));
        assertTrue(YearRange.yearInRange(-221, -2070, -221));
    }

    @Test
    void ordinalInRangeBceSafe() {
        org.junit.jupiter.api.Assertions.assertEquals(23, YearRange.ordinalInRange(-119, -141, -87));
        org.junit.jupiter.api.Assertions.assertEquals(1, YearRange.ordinalInRange(1522, 1522, 1566));
        org.junit.jupiter.api.Assertions.assertEquals(0, YearRange.ordinalInRange(1500, 1522, 1566));
    }
}
