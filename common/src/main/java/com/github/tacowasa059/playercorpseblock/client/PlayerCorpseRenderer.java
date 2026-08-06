package com.github.tacowasa059.playercorpseblock.client;

import com.github.tacowasa059.playercorpseblock.block.CorpseData;
import com.github.tacowasa059.playercorpseblock.block.PlayerCorpseBlockEntity;
import com.github.tacowasa059.playercorpseblock.config.CorpseConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Draws the dead players of one corpse block, one body per half block.
 * <p>
 * Bodies are built from scratch every frame like any other block entity, so a big pile is kept affordable
 * by drawing as little as possible: nothing past {@link #getViewDistance()}, and no outer skin layer past
 * the configured detail distance.
 */
public class PlayerCorpseRenderer implements BlockEntityRenderer<PlayerCorpseBlockEntity> {

    private final CorpseModelRenderer bodies;
    private final int viewDistance;
    private final double detailDistanceSq;
    private final boolean cullHidden;

    public PlayerCorpseRenderer(BlockEntityRendererProvider.Context context) {

        final CorpseConfig config = CorpseConfig.get();

        this.bodies = new CorpseModelRenderer(context.getModelSet());
        this.viewDistance = config.renderDistance();
        this.detailDistanceSq = (double) config.detailDistance() * config.detailDistance();
        this.cullHidden = config.cullHiddenCorpses();
    }

    @Override
    public int getViewDistance() {

        return this.viewDistance;
    }

    /**
     * Vanilla only culls whole chunk sections, so the bodies buried inside a mound would still be drawn.
     * Skipping the ones that are covered on every side is what keeps a big pile affordable.
     */
    @Override
    public boolean shouldRender(PlayerCorpseBlockEntity blockEntity, Vec3 cameraPos) {

        if (!Vec3.atCenterOf(blockEntity.getBlockPos()).closerThan(cameraPos, this.viewDistance)) {
            return false;
        }

        return !this.cullHidden || !blockEntity.isHiddenByNeighbours();
    }

    @Override
    public void render(PlayerCorpseBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {

        final List<CorpseData> corpses = blockEntity.getCorpses();
        if (corpses.isEmpty()) {
            return;
        }

        final boolean detailed = isCloseToCamera(blockEntity.getBlockPos());

        for (int layer = 0; layer < corpses.size(); layer++) {
            this.bodies.render(corpses.get(layer), layer * CorpseModelRenderer.LAYER_HEIGHT, poseStack, buffers, light, overlay, detailed);
        }
    }

    private boolean isCloseToCamera(BlockPos pos) {

        final Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        return camera.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) < this.detailDistanceSq;
    }
}
