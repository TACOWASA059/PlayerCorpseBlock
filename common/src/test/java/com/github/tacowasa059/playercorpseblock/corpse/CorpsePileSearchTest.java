package com.github.tacowasa059.playercorpseblock.corpse;

import com.github.tacowasa059.playercorpseblock.corpse.CorpsePileSearch.Occupancy;
import com.github.tacowasa059.playercorpseblock.corpse.CorpsePileSearch.Spot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HCorpsePileSearchTest {

    private static final int GROUND_Y = 64;
    private static final int MIN_Y = 0;
    private static final int MAX_Y = 127;
    private static final int MAX_PILE = 24;

    private TestSpace space;

    @BeforeEach
    void setUp() {

        this.space = new TestSpace(GROUND_Y, MIN_Y, MAX_Y);
    }

    @Test
    void bodyIsPlacedWhereThePlayerDied() {

        final Spot spot = CorpsePileSearch.find(this.space, 0, GROUND_Y, 0, 2, MAX_PILE);

        assertEquals(new Spot(0, GROUND_Y, 0, false), spot);
    }

    @Test
    void bodyStaysInTheAirWhenThePlayerDiedThere() {

        final Spot spot = CorpsePileSearch.find(this.space, 0, GROUND_Y + 20, 0, 2, MAX_PILE);

        assertEquals(new Spot(0, GROUND_Y + 20, 0, false), spot, "a body never drops to the ground");
    }

    @Test
    void secondBodyFillsTheUpperHalfOfTheSameBlock() {

        this.space.set(0, GROUND_Y, 0, Occupancy.HALF_FULL);

        final Spot spot = CorpsePileSearch.find(this.space, 0, GROUND_Y, 0, 2, MAX_PILE);

        assertEquals(new Spot(0, GROUND_Y, 0, true), spot);
    }

    @Test
    void deathPositionWinsOverAFreeNeighbour() {

        this.space.set(0, GROUND_Y, 0, Occupancy.HALF_FULL);

        final Spot spot = CorpsePileSearch.find(this.space, 0, GROUND_Y, 0, 3, MAX_PILE);

        assertNotNull(spot);
        assertEquals(0, spot.x());
        assertEquals(0, spot.z());
        assertTrue(spot.stackOnExisting());
    }

    @Test
    void aFullBlockPushesTheBodyOneLevelUp() {

        this.space.set(0, GROUND_Y, 0, Occupancy.BLOCKED);

        final Spot spot = CorpsePileSearch.find(this.space, 0, GROUND_Y, 0, 2, MAX_PILE);

        assertEquals(new Spot(0, GROUND_Y + 1, 0, false), spot);
    }

    @Test
    void bodyClimbsOverEverythingStackedInTheColumn() {

        for (int i = 0; i < 5; i++) {
            this.space.set(0, GROUND_Y + i, 0, Occupancy.BLOCKED);
        }

        final Spot spot = CorpsePileSearch.find(this.space, 0, GROUND_Y, 0, 2, MAX_PILE);

        assertEquals(new Spot(0, GROUND_Y + 5, 0, false), spot);
    }

    @Test
    void neighbourIsUsedOnlyWhenTheColumnIsFull() {

        for (int i = 0; i <= 3; i++) {
            this.space.set(0, GROUND_Y + i, 0, Occupancy.BLOCKED);
        }

        final Spot spot = CorpsePileSearch.find(this.space, 0, GROUND_Y, 0, 2, 3);

        assertNotNull(spot);
        assertEquals(1, Math.abs(spot.x()) + Math.abs(spot.z()), "the closest column takes over");
        assertEquals(GROUND_Y, spot.y());
    }

    @Test
    void theClosestColumnWinsEvenWhenAFurtherOneHasMoreRoom() {

        for (int i = 0; i <= 3; i++) {
            this.space.set(0, GROUND_Y + i, 0, Occupancy.BLOCKED);
        }
        // The direct neighbours only have their upper half left, the columns behind them are empty.
        this.space.set(1, GROUND_Y, 0, Occupancy.HALF_FULL);
        this.space.set(-1, GROUND_Y, 0, Occupancy.HALF_FULL);
        this.space.set(0, GROUND_Y, 1, Occupancy.HALF_FULL);
        this.space.set(0, GROUND_Y, -1, Occupancy.HALF_FULL);

        final Spot spot = CorpsePileSearch.find(this.space, 0, GROUND_Y, 0, 2, 3);

        assertNotNull(spot);
        assertEquals(1, Math.abs(spot.x()) + Math.abs(spot.z()));
        assertTrue(spot.stackOnExisting());
    }

    @Test
    void climbingStopsAtTheMaximumPileHeight() {

        for (int i = 0; i < 10; i++) {
            this.space.set(0, GROUND_Y + i, 0, Occupancy.BLOCKED);
        }

        assertNull(CorpsePileSearch.find(this.space, 0, GROUND_Y, 0, 0, 3));
    }

    @Test
    void nothingIsPlacedAboveTheWorldTop() {

        final TestSpace shallow = new TestSpace(0, 0, 3);
        for (int y = 0; y <= 3; y++) {
            shallow.set(0, y, 0, Occupancy.BLOCKED);
        }

        assertNull(CorpsePileSearch.find(shallow, 0, 3, 0, 0, MAX_PILE));
    }

    @Test
    void deathPositionBelowTheWorldIsPulledBackIn() {

        final TestSpace openWorld = new TestSpace(MIN_Y, MIN_Y, MAX_Y);

        final Spot spot = CorpsePileSearch.find(openWorld, 0, MIN_Y - 20, 0, 0, MAX_PILE);

        assertNotNull(spot);
        assertEquals(MIN_Y, spot.y());
    }

    @Test
    void livingEntitiesAreNotBuriedUnderABody() {

        this.space.putEntity(0, GROUND_Y, 0);

        assertNull(CorpsePileSearch.find(this.space, 0, GROUND_Y, 0, 0, MAX_PILE));
    }

    @Test
    void aBlockedNeighbourIsSkippedForTheNextOne() {

        for (int i = 0; i <= 3; i++) {
            this.space.set(0, GROUND_Y + i, 0, Occupancy.BLOCKED);
        }
        this.space.putEntity(-1, GROUND_Y, 0);

        final Spot spot = CorpsePileSearch.find(this.space, 0, GROUND_Y, 0, 2, 3);

        assertNotNull(spot);
        assertEquals(1, Math.abs(spot.x()) + Math.abs(spot.z()));
        assertTrue(spot.x() != -1 || spot.z() != 0, "the column with the mob in it is skipped");
    }

    @Test
    void searchStaysInsideTheRadius() {

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int y = GROUND_Y; y <= MAX_Y; y++) {
                    this.space.set(dx, y, dz, Occupancy.BLOCKED);
                }
            }
        }

        assertNull(CorpsePileSearch.find(this.space, 0, GROUND_Y, 0, 1, MAX_PILE));
    }

    @Test
    void halfLevelCountsSpaceInHalfBlocks() {

        assertEquals(128, new Spot(0, 64, 0, false).halfLevel());
        assertEquals(129, new Spot(0, 64, 0, true).halfLevel());
        assertTrue(new Spot(0, 64, 0, true).halfLevel() < new Spot(0, 65, 0, false).halfLevel());
    }

    /** A world made of a flat floor plus whatever a test puts on top of it. */
    private static final class TestSpace implements CorpsePileSearch.Space {

        private final Map<String, Occupancy> cells = new HashMap<>();
        private final Set<String> entities = new HashSet<>();
        private final int groundY;
        private final int minY;
        private final int maxY;

        private TestSpace(int groundY, int minY, int maxY) {

            this.groundY = groundY;
            this.minY = minY;
            this.maxY = maxY;
        }

        void set(int x, int y, int z, Occupancy occupancy) {

            this.cells.put(key(x, y, z), occupancy);
        }

        void putEntity(int x, int y, int z) {

            this.entities.add(key(x, y, z));
        }

        private static String key(int x, int y, int z) {

            return x + "/" + y + "/" + z;
        }

        @Override
        public Occupancy occupancyAt(int x, int y, int z) {

            final Occupancy occupancy = this.cells.get(key(x, y, z));
            if (occupancy != null) {
                return occupancy;
            }
            return y < this.groundY ? Occupancy.BLOCKED : Occupancy.FREE;
        }

        @Override
        public boolean isBlockedByEntity(int x, int y, int z) {

            return this.entities.contains(key(x, y, z));
        }

        @Override
        public int minY() {

            return this.minY;
        }

        @Override
        public int maxY() {

            return this.maxY;
        }
    }
}
