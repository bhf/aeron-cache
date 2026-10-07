package com.bhf.aeroncache.clustertools.archive;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the pure purge-floor arithmetic - which snapshot is retained and how its log
 * position is aligned down to an archive segment boundary - without an Aeron client or containers.
 */
class PurgePlannerTest {

    private static final int TERM = 64 * 1024;          // 65536
    private static final int SEGMENT = 64 * 1024;       // one segment == one term for simple maths
    private static final long START = 0L;

    @Test
    @DisplayName("No purge when there are fewer snapshots than the retention count")
    void noPurgeWhenNotEnoughSnapshots() {
        var plan = PurgePlanner.plan(List.of(100_000L), 2, START, TERM, SEGMENT);

        assertFalse(plan.shouldPurge());
        assertTrue(plan.reason().contains("not enough snapshots"));
    }

    @Test
    @DisplayName("No purge when the retained floor is still inside the first segment")
    void noPurgeWhenFloorWithinFirstSegment() {
        // floor = oldest of two snapshots = 50_000 < one 64KB segment, so it aligns down to START.
        var plan = PurgePlanner.plan(List.of(50_000L, 120_000L), 2, START, TERM, SEGMENT);

        assertFalse(plan.shouldPurge());
        assertTrue(plan.reason().contains("nothing to purge"));
    }

    @Test
    @DisplayName("Purges whole segments below the retained snapshot floor")
    void purgesWholeSegmentsBelowFloor() {
        // Retain the newest 1; floor = newest snapshot = 200_000. Aligns down to 3 * 65536 = 196_608.
        var plan = PurgePlanner.plan(List.of(50_000L, 200_000L), 1, START, TERM, SEGMENT);

        assertTrue(plan.shouldPurge());
        assertEquals(196_608L, plan.alignedFloor());
    }

    @Test
    @DisplayName("Retention selects the Nth most recent snapshot as the floor")
    void retentionSelectsNthMostRecent() {
        // Three snapshots, retain the newest 2 -> floor = get(size-2) = index 1 = 300_000.
        // 300_000 aligns down to 4 * 65536 = 262_144.
        var plan = PurgePlanner.plan(List.of(150_000L, 300_000L, 450_000L), 2, START, TERM, SEGMENT);

        assertTrue(plan.shouldPurge());
        assertEquals(262_144L, plan.alignedFloor());
    }

    @Test
    @DisplayName("Honours a non-zero recording start position")
    void honoursNonZeroStartPosition() {
        // Recording already starts at one segment in; floor just past two segments aligns to 2 segments.
        long start = SEGMENT; // 65536
        var plan = PurgePlanner.plan(List.of(200_000L), 1, start, TERM, SEGMENT);

        assertTrue(plan.shouldPurge());
        assertEquals(196_608L, plan.alignedFloor());
        assertTrue(plan.alignedFloor() > start);
    }

    @Test
    @DisplayName("No purge for an invalid retention count")
    void noPurgeForInvalidRetention() {
        var plan = PurgePlanner.plan(List.of(100_000L, 200_000L), 0, START, TERM, SEGMENT);

        assertFalse(plan.shouldPurge());
        assertTrue(plan.reason().contains("retention count"));
    }
}
