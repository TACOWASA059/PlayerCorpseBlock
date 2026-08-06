package com.github.tacowasa059.playercorpseblock;

import com.github.tacowasa059.playercorpseblock.client.PlayerCorpseItemRenderer;
import com.github.tacowasa059.playercorpseblock.client.PlayerCorpseRenderer;
import com.github.tacowasa059.playercorpseblock.registry.ModContent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;

public class PlayerCorpseBlockFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        BlockEntityRendererRegistry.register(ModContent.playerCorpseEntity(), PlayerCorpseRenderer::new);

        BuiltinItemRendererRegistry.INSTANCE.register(ModContent.playerCorpseItem(),
                (stack, mode, poseStack, buffers, light, overlay) -> PlayerCorpseItemRenderer.render(stack, poseStack, buffers, light, overlay));
    }
}
