package com.github.tacowasa059.playercorpseblock.corpse;

import javax.annotation.Nullable;

/**
 * Works out where the next body goes. This is pure geometry and free of Minecraft types, so the shape of
 * a pile can be tested on its own.
 * <p>
 * A body belongs exactly where the player died, mid air included: it never drops to the ground. Space is
 * counted in half blocks because one corpse block holds two bodies, so dying in the same spot fills the
 * lower half, then the upper half, then the block above. Only when that whole column is taken does the
 * body move to a nearby one.
 */
public final class CorpsePileSearch {

    /** How much of a block position is taken. */
    public enum Occupancy {

        /** Nothing in the way, a body can start a new corpse block here. */
        FREE,
        /** A corpse block that still has room for a second body. */
        HALF_FULL,
        /** Anything a body cannot go into. */
        BLOCKED
    }

    /** The world as this search sees it. */
    public interface Space {

        Occupancy occupancyAt(int x, int y, int z);

        /** True when a living entity other than the dying player stands in the way. */
        boolean isBlockedByEntity(int x, int y, int z);

        int minY();

        int maxY();
    }

    /** A free half block: either an empty position, or a corpse block with room for a second body. */
    public record Spot(int x, int y, int z, boolean stackOnExisting) {

        public int halfLevel() {

            return this.y * 2 + (this.stackOnExisting ? 1 : 0);
        }
    }

    private CorpsePileSearch() {
    }

    /**
     * @param radius         how far the body may be moved aside when its own column is full
     * @param maxPileHeight  how far a body may climb over what is already stacked in a column
     * @return where the body goes, or null when the death position and everything around it is taken
     */
    @Nullable
    public static Spot find(Space space, int x, int y, int z, int radius, int maxPileHeight) {

        // The body belongs where the player died.
        final Spot atDeathPosition = findInColumn(space, x, y, z, maxPileHeight);
        if (atDeathPosition != null) {
            return atDeathPosition;
        }

        final int radiusSq = radius * radius;

        Spot best = null;
        int bestDistanceSq = Integer.MAX_VALUE;
        int bestLevel = Integer.MAX_VALUE;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {

                final int distanceSq = dx * dx + dz * dz;
                if (distanceSq > radiusSq || (dx == 0 && dz == 0)) {
                    continue;
                }

                final Spot candidate = findInColumn(space, x + dx, y, z + dz, maxPileHeight);
                if (candidate == null) {
                    continue;
                }

                // The closest column wins, ties go to the lowest free half block in it.
                if (distanceSq < bestDistanceSq || (distanceSq == bestDistanceSq && candidate.halfLevel() < bestLevel)) {
                    best = candidate;
                    bestDistanceSq = distanceSq;
                    bestLevel = candidate.halfLevel();
                }
            }
        }

        return best;
    }

    @Nullable
    static Spot findInColumn(Space space, int x, int startY, int z, int maxPileHeight) {

        int y = Math.max(space.minY(), Math.min(startY, space.maxY()));

        // Climb over whatever is already there, most often the corpses placed before this one.
        for (int climb = 0; climb < maxPileHeight && space.occupancyAt(x, y, z) == Occupancy.BLOCKED; climb++) {
            y++;
        }

        if (y > space.maxY()) {
            return null;
        }

        final Occupancy occupancy = space.occupancyAt(x, y, z);
        if (occupancy == Occupancy.BLOCKED || space.isBlockedByEntity(x, y, z)) {
            return null;
        }

        return new Spot(x, y, z, occupancy == Occupancy.HALF_FULL);
    }
}
