package com.github.tacowasa059.playercorpseblock.corpse;

import com.github.tacowasa059.playercorpseblock.Constants;
import com.github.tacowasa059.playercorpseblock.block.CorpseData;
import com.github.tacowasa059.playercorpseblock.block.PlayerCorpseBlock;
import com.github.tacowasa059.playercorpseblock.block.PlayerCorpseBlockEntity;
import com.github.tacowasa059.playercorpseblock.config.CorpseConfig;
import com.github.tacowasa059.playercorpseblock.corpse.CorpsePileSearch.Occupancy;
import com.github.tacowasa059.playercorpseblock.corpse.CorpsePileSearch.Spot;
import com.github.tacowasa059.playercorpseblock.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

/**
 * Puts the corpse of a dying player into the world. Where it ends up is decided by {@link CorpsePileSearch},
 * this class only translates between the level and that search.
 */
public final class CorpsePlacer {

    /** Degrees of random rotation added to the direction a body is laid out in. */
    private static final float YAW_JITTER = 70.0F;

    private CorpsePlacer() {
    }

    public static void onPlayerDeath(ServerPlayer player) {

        final CorpseConfig config = CorpseConfig.get();
        if (!config.enabled() || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        final BlockPos deathPos = findDeathPos(level, player);
        final Spot spot = CorpsePileSearch.find(new LevelSpace(level, player),
                deathPos.getX(), deathPos.getY(), deathPos.getZ(),
                config.pileRadius(), config.maxPileHeight());

        if (spot == null) {
            Constants.LOG.debug("No free spot for the corpse of {} near {}.", player.getGameProfile().getName(), deathPos);
            return;
        }

        // Bodies keep the direction the player was looking, jittered a little so a pile of players who
        // all died facing the same way does not end up looking like a stack of clones.
        final float yaw = player.getYRot() + (level.random.nextFloat() - 0.5F) * YAW_JITTER;

        placeCorpse(level, spot, new CorpseData(player.getGameProfile(),
                level.getGameTime(),
                yaw,
                level.random.nextInt(CorpseData.VARIANT_COUNT)));
    }

    private static void placeCorpse(ServerLevel level, Spot spot, CorpseData corpseData) {

        final BlockPos pos = new BlockPos(spot.x(), spot.y(), spot.z());

        if (spot.stackOnExisting()) {

            if (level.getBlockEntity(pos) instanceof PlayerCorpseBlockEntity corpse && corpse.addCorpse(corpseData)) {
                // Only a property changes, so the block entity and the body already in it survive this.
                level.setBlock(pos, level.getBlockState(pos).setValue(PlayerCorpseBlock.CORPSES, corpse.getCorpseCount()), Block.UPDATE_ALL);
            }
            return;
        }

        final BlockState state = ModContent.playerCorpse().defaultBlockState().setValue(PlayerCorpseBlock.CORPSES, 1);

        if (level.setBlock(pos, state, Block.UPDATE_ALL) && level.getBlockEntity(pos) instanceof PlayerCorpseBlockEntity corpse) {
            corpse.addCorpse(corpseData);
        }
    }

    /** Deaths in the void would place the corpse outside the world, so those bodies land on the surface. */
    private static BlockPos findDeathPos(ServerLevel level, ServerPlayer player) {

        final BlockPos pos = player.blockPosition();

        if (pos.getY() < level.getMinBuildHeight()) {
            return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos);
        }

        return new BlockPos(pos.getX(), Math.min(pos.getY(), level.getMaxBuildHeight() - 1), pos.getZ());
    }

    /** The level seen through the eyes of the pile search. */
    private record LevelSpace(ServerLevel level, ServerPlayer dying) implements CorpsePileSearch.Space {

        @Override
        public Occupancy occupancyAt(int x, int y, int z) {

            final BlockPos pos = new BlockPos(x, y, z);
            final BlockState state = this.level.getBlockState(pos);

            if (state.is(ModContent.playerCorpse())) {
                return state.getValue(PlayerCorpseBlock.CORPSES) < PlayerCorpseBlockEntity.MAX_CORPSES ? Occupancy.HALF_FULL : Occupancy.BLOCKED;
            }

            // Grass, snow layers, water and lava all give way to a body.
            if (state.isAir() || state.canBeReplaced() || !state.getFluidState().isEmpty()) {
                return Occupancy.FREE;
            }

            return Occupancy.BLOCKED;
        }

        /** Never bury a living player or mob; the dying player is standing in the spot themselves. */
        @Override
        public boolean isBlockedByEntity(int x, int y, int z) {

            final AABB box = new AABB(new BlockPos(x, y, z));
            return !this.level.getEntitiesOfClass(LivingEntity.class, box, entity -> entity != this.dying && entity.isAlive()).isEmpty();
        }

        @Override
        public int minY() {

            return this.level.getMinBuildHeight();
        }

        @Override
        public int maxY() {

            return this.level.getMaxBuildHeight() - 1;
        }
    }
}
