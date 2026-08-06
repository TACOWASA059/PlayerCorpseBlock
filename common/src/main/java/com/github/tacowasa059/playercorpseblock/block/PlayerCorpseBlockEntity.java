package com.github.tacowasa059.playercorpseblock.block;

import com.github.tacowasa059.playercorpseblock.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds the bodies stacked in one block. There is deliberately no ticker: the block entity only does
 * work when it is placed, loaded or rendered.
 */
public class PlayerCorpseBlockEntity extends BlockEntity {

    /** Two bodies per block, one in each half. */
    public static final int MAX_CORPSES = 2;

    private static final String TAG_CORPSES = "Corpses";

    /** Ticks between two checks of whether this block is buried; spread out so a pile never checks at once. */
    private static final int HIDDEN_CHECK_INTERVAL = 10;

    private final List<CorpseData> corpses = new ArrayList<>(MAX_CORPSES);

    /** Client side only: every side is covered, so nothing of these bodies can be seen. */
    private boolean hiddenByNeighbours;
    private long nextHiddenCheck;

    public PlayerCorpseBlockEntity(BlockPos pos, BlockState state) {

        super(ModContent.playerCorpseEntity(), pos, state);
    }

    public boolean addCorpse(CorpseData corpse) {

        if (this.corpses.size() >= MAX_CORPSES) {
            return false;
        }

        this.corpses.add(corpse);
        this.setChanged();
        this.sendUpdate();
        return true;
    }

    /** Drops every corpse that is older than the given lifetime. Returns true when something was removed. */
    public boolean removeExpired(long gameTime, long lifetimeTicks) {

        final boolean removed = this.corpses.removeIf(corpse -> gameTime - corpse.getDeathTime() >= lifetimeTicks);

        if (removed) {
            this.setChanged();
            this.sendUpdate();
        }

        return removed;
    }

    private void sendUpdate() {

        // What is visible may have changed, so do not trust the cached answer any more.
        this.nextHiddenCheck = 0L;

        if (this.level != null) {
            final BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(this.getBlockPos(), state, state, 3);
        }
    }

    public List<CorpseData> getCorpses() {

        return this.corpses;
    }

    /**
     * True when all six sides are covered by full corpse blocks or solid blocks. Bodies inside a mound are
     * invisible but would still be built from scratch every frame, so the renderer skips them.
     * <p>
     * The answer is cached for a moment: a pile changes rarely, and asking the level six times per block
     * per frame would defeat the point.
     */
    public boolean isHiddenByNeighbours() {

        if (this.level == null) {
            return false;
        }

        final long gameTime = this.level.getGameTime();
        if (gameTime >= this.nextHiddenCheck) {
            this.hiddenByNeighbours = isBuried();
            this.nextHiddenCheck = gameTime + HIDDEN_CHECK_INTERVAL + Math.floorMod(this.getBlockPos().hashCode(), HIDDEN_CHECK_INTERVAL);
        }

        return this.hiddenByNeighbours;
    }

    private boolean isBuried() {

        final BlockPos pos = this.getBlockPos();

        for (Direction direction : Direction.values()) {
            if (!covers(this.level, pos.relative(direction))) {
                return false;
            }
        }

        return true;
    }

    private static boolean covers(Level level, BlockPos pos) {

        final BlockState state = level.getBlockState(pos);

        if (state.is(ModContent.playerCorpse())) {
            return state.getValue(PlayerCorpseBlock.CORPSES) >= MAX_CORPSES;
        }

        return state.isSolidRender(level, pos);
    }

    public int getCorpseCount() {

        return this.corpses.size();
    }

    public boolean isEmpty() {

        return this.corpses.isEmpty();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {

        super.saveAdditional(tag);

        final ListTag list = new ListTag();
        for (CorpseData corpse : this.corpses) {
            list.add(corpse.save());
        }
        tag.put(TAG_CORPSES, list);
    }

    @Override
    public void load(CompoundTag tag) {

        super.load(tag);

        this.corpses.clear();
        this.corpses.addAll(readCorpses(tag));
        this.nextHiddenCheck = 0L;
    }

    /** Reads the bodies out of block entity data, also used to unpack the NBT of a corpse item. */
    public static List<CorpseData> readCorpses(CompoundTag tag) {

        final ListTag list = tag.getList(TAG_CORPSES, Tag.TAG_COMPOUND);
        final List<CorpseData> corpses = new ArrayList<>(MAX_CORPSES);

        for (int i = 0; i < list.size() && i < MAX_CORPSES; i++) {
            corpses.add(CorpseData.load(list.getCompound(i)));
        }

        return corpses;
    }

    @Override
    public CompoundTag getUpdateTag() {

        return this.saveWithoutMetadata();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {

        return ClientboundBlockEntityDataPacket.create(this);
    }
}
