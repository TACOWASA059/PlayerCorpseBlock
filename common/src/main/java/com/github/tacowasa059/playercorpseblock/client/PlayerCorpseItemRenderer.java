package com.github.tacowasa059.playercorpseblock.client;

import com.github.tacowasa059.playercorpseblock.block.CorpseData;
import com.github.tacowasa059.playercorpseblock.block.PlayerCorpseBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Draws the corpse item as the bodies it actually carries, skins included, instead of a flat icon.
 * Both loaders hook their own item renderer up to this.
 */
public final class PlayerCorpseItemRenderer {

    /** Shown for an item that carries no bodies yet, for example the one from the creative menu. */
    private static final CorpseData EMPTY_PREVIEW = new CorpseData(null, 0L, 0.0F, 0);

    @Nullable
    private static CorpseModelRenderer bodies;

    private PlayerCorpseItemRenderer() {
    }

    public static void render(ItemStack stack, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {

        if (bodies == null) {
            bodies = new CorpseModelRenderer(Minecraft.getInstance().getEntityModels());
        }

        final List<CorpseData> corpses = corpsesIn(stack);

        for (int layer = 0; layer < corpses.size(); layer++) {
            bodies.render(corpses.get(layer), layer * CorpseModelRenderer.LAYER_HEIGHT, poseStack, buffers, light, overlay, true);
        }
    }

    private static List<CorpseData> corpsesIn(ItemStack stack) {

        final CompoundTag data = BlockItem.getBlockEntityData(stack);
        if (data == null) {
            return List.of(EMPTY_PREVIEW);
        }

        final List<CorpseData> corpses = PlayerCorpseBlockEntity.readCorpses(data);
        return corpses.isEmpty() ? List.of(EMPTY_PREVIEW) : corpses;
    }
}
