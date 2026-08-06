package com.github.tacowasa059.playercorpseblock;

import com.github.tacowasa059.playercorpseblock.client.PlayerCorpseItemRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** Client only glue between the Forge item extensions and the shared corpse item renderer. */
final class ForgeCorpseItemExtensions {

    private ForgeCorpseItemExtensions() {
    }

    static IClientItemExtensions create() {

        final Minecraft minecraft = Minecraft.getInstance();
        final BlockEntityWithoutLevelRenderer renderer =
                new BlockEntityWithoutLevelRenderer(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels()) {

                    @Override
                    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {

                        PlayerCorpseItemRenderer.render(stack, poseStack, buffers, light, overlay);
                    }
                };

        return new IClientItemExtensions() {

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {

                return renderer;
            }
        };
    }
}
