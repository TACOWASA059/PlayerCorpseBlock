package com.github.tacowasa059.playercorpseblock.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The item form of a corpse. The bodies it carries are added to whatever is already in the block instead
 * of replacing it, which is what lets a corpse be stacked onto another one like a slab.
 */
public class PlayerCorpseBlockItem extends BlockItem {

    public PlayerCorpseBlockItem(Block block, Properties properties) {

        super(block, properties);
    }

    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, @Nullable Player player, ItemStack stack, BlockState state) {

        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof PlayerCorpseBlockEntity corpse)) {
            return false;
        }

        final CompoundTag data = BlockItem.getBlockEntityData(stack);
        if (data == null) {
            return false;
        }

        final List<CorpseData> carried = PlayerCorpseBlockEntity.readCorpses(data);
        boolean changed = false;

        for (CorpseData body : carried) {
            changed |= corpse.addCorpse(body);
        }

        return changed;
    }
}
