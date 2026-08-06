package com.github.tacowasa059.playercorpseblock.client;

import com.github.tacowasa059.playercorpseblock.block.CorpseData;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Draws a single body lying on the ground, inside the block it belongs to.
 * <p>
 * Only two models are kept (wide arms and slim arms); they are posed right before every draw call, so any
 * number of corpses share the same two model instances.
 */
public class CorpseModelRenderer {

    /** A player model is two blocks tall, this shrinks a body down to roughly its own block. */
    private static final float SCALE = 0.9F;
    /** Half the thickness of the torso, the arms and the legs, outer skin layer included, in pixels. */
    private static final float BODY_HALF_THICKNESS = 2.25F;
    /** Half the thickness of the head, outer skin layer included, in pixels. */
    private static final float HEAD_HALF_THICKNESS = 4.5F;
    /** Lifts a body so the torso and the limbs rest exactly on the floor instead of floating over it. */
    private static final double LIFT = (BODY_HALF_THICKNESS / 16.0D) * SCALE;
    /**
     * The head is thicker than the body, so on its own it would sink into the floor. Pushing it away from
     * the floor by the difference makes it rest on the ground like the rest of the body.
     */
    private static final float HEAD_OFFSET = HEAD_HALF_THICKNESS - BODY_HALF_THICKNESS;
    /**
     * Moves the model along its own length so the body is centred on the block instead of the feet.
     * The model runs from 0 (feet) to 2 (top of the head), so half of that puts the middle on the block.
     */
    private static final double LENGTH_OFFSET = -1.0D;
    /** Height of one body slot; the second body in a block lies on top of the first one. */
    public static final double LAYER_HEIGHT = 0.5D;

    private final PlayerModel<AbstractClientPlayer> wideModel;
    private final PlayerModel<AbstractClientPlayer> slimModel;
    private final ModelPart wideRoot;
    private final ModelPart slimRoot;

    public CorpseModelRenderer(EntityModelSet models) {

        this.wideRoot = models.bakeLayer(ModelLayers.PLAYER);
        this.slimRoot = models.bakeLayer(ModelLayers.PLAYER_SLIM);
        this.wideModel = new PlayerModel<>(this.wideRoot, false);
        this.slimModel = new PlayerModel<>(this.slimRoot, true);
    }

    /**
     * @param detailed whether to draw the outer skin layer as well; dropping it halves the amount of
     *                 geometry built for a body and is not noticeable from a distance
     */
    public void render(CorpseData corpse, double height, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay, boolean detailed) {

        final ResourceLocation skin = resolveSkin(corpse);
        final boolean slim = corpse.hasSlimArms();
        final PlayerModel<AbstractClientPlayer> model = slim ? this.slimModel : this.wideModel;
        final ModelPart root = slim ? this.slimRoot : this.wideRoot;

        final int variant = corpse.getVariant();
        final boolean faceUp = (variant & 1) == 0;

        applyPose(root, variant, faceUp);
        setOuterLayerVisible(root, detailed);
        model.young = false;
        model.crouching = false;
        model.riding = false;
        model.attackTime = 0.0F;

        poseStack.pushPose();
        poseStack.translate(0.5D, LIFT + height, 0.5D);
        // Lays the body out along the direction the player was looking, head first.
        poseStack.mulPose(Axis.YP.rotationDegrees(-corpse.getYaw()));
        // Tip the standing model over; which way decides whether the corpse ends up face up or face down.
        poseStack.mulPose(Axis.XP.rotationDegrees(faceUp ? -90.0F : 90.0F));
        poseStack.scale(SCALE, SCALE, SCALE);
        poseStack.translate(0.0D, LENGTH_OFFSET, 0.0D);
        // From here on this is the regular entity model setup used by LivingEntityRenderer.
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0D, -1.501D, 0.0D);

        // Cutout instead of the translucent type players use: a corpse is never see through, and this
        // keeps hundreds of them out of the translucency pass. Vanilla draws player heads the same way.
        final VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(skin));
        model.renderToBuffer(poseStack, consumer, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);

        poseStack.popPose();
    }

    /** The outer skin layer is six extra cubes per body, so far away corpses are drawn without it. */
    private static void setOuterLayerVisible(ModelPart root, boolean visible) {

        root.getChild("hat").visible = visible;
        root.getChild("jacket").visible = visible;
        root.getChild("right_sleeve").visible = visible;
        root.getChild("left_sleeve").visible = visible;
        root.getChild("right_pants").visible = visible;
        root.getChild("left_pants").visible = visible;
    }

    /**
     * Poses the body. Limbs are always folded away from the ground, so the same variants work for a
     * corpse lying on its back and one lying on its face.
     */
    private static void applyPose(ModelPart root, int variant, boolean faceUp) {

        // Negative x rotations fold a limb towards the front of the model, positive ones towards the back.
        final float fold = faceUp ? -1.0F : 1.0F;

        final ModelPart head = root.getChild("head");
        final ModelPart body = root.getChild("body");
        final ModelPart rightArm = root.getChild("right_arm");
        final ModelPart leftArm = root.getChild("left_arm");
        final ModelPart rightLeg = root.getChild("right_leg");
        final ModelPart leftLeg = root.getChild("left_leg");

        setRotation(body, 0.0F, 0.0F, 0.0F);

        // The model's own z axis points at the floor once the body is tipped over, so this raises the head.
        head.z = faceUp ? HEAD_OFFSET : -HEAD_OFFSET;

        switch (variant) {
            case 0 -> { // sprawled out
                setRotation(head, fold * 0.25F, 0.35F, 0.0F);
                setRotation(rightArm, fold * 0.35F, 0.0F, -1.35F);
                setRotation(leftArm, fold * 0.15F, 0.0F, 1.15F);
                setRotation(rightLeg, fold * 0.45F, 0.0F, -0.25F);
                setRotation(leftLeg, fold * 0.9F, 0.0F, 0.3F);
            }
            case 1 -> { // curled up
                setRotation(head, fold * 0.55F, -0.3F, 0.0F);
                setRotation(rightArm, fold * 1.4F, 0.0F, -0.45F);
                setRotation(leftArm, fold * 1.2F, 0.0F, 0.6F);
                setRotation(rightLeg, fold * 1.5F, 0.0F, -0.15F);
                setRotation(leftLeg, fold * 1.3F, 0.0F, 0.2F);
            }
            case 2 -> { // one arm thrown out
                setRotation(head, fold * 0.15F, -0.55F, 0.0F);
                setRotation(rightArm, fold * 0.2F, 0.0F, -1.6F);
                setRotation(leftArm, fold * 1.1F, 0.0F, 0.35F);
                setRotation(rightLeg, fold * 1.1F, 0.0F, -0.35F);
                setRotation(leftLeg, fold * 0.5F, 0.0F, 0.15F);
            }
            default -> { // knees pulled up
                setRotation(head, fold * 0.4F, 0.2F, 0.0F);
                setRotation(rightArm, fold * 0.9F, 0.0F, -0.8F);
                setRotation(leftArm, fold * 0.7F, 0.0F, 0.9F);
                setRotation(rightLeg, fold * 1.55F, 0.0F, 0.1F);
                setRotation(leftLeg, fold * 1.45F, 0.0F, -0.1F);
            }
        }

        // The outer skin layer is made of separate parts, they have to follow the parts they sit on.
        root.getChild("hat").copyFrom(head);
        root.getChild("jacket").copyFrom(body);
        root.getChild("right_sleeve").copyFrom(rightArm);
        root.getChild("left_sleeve").copyFrom(leftArm);
        root.getChild("right_pants").copyFrom(rightLeg);
        root.getChild("left_pants").copyFrom(leftLeg);
    }

    private static void setRotation(ModelPart part, float xRot, float yRot, float zRot) {

        part.xRot = xRot;
        part.yRot = yRot;
        part.zRot = zRot;
    }

    /**
     * Resolves the skin of the dead player once and caches it. The profile that was captured on death
     * already carries the texture data, so this never has to look anything up online.
     */
    private static ResourceLocation resolveSkin(CorpseData corpse) {

        final ResourceLocation cached = corpse.getCachedSkin();
        if (corpse.isSkinResolved() && cached != null) {
            return cached;
        }

        final GameProfile owner = corpse.getOwner();
        if (owner == null || owner.getId() == null) {
            final ResourceLocation fallback = DefaultPlayerSkin.getDefaultSkin();
            corpse.cacheSkin(fallback, false);
            return fallback;
        }

        final UUID uuid = owner.getId();
        final SkinManager skins = Minecraft.getInstance().getSkinManager();
        final MinecraftProfileTexture texture = skins.getInsecureSkinInformation(owner).get(MinecraftProfileTexture.Type.SKIN);

        final ResourceLocation skin = skins.getInsecureSkinLocation(owner);
        final boolean slim = texture != null
                ? "slim".equals(texture.getMetadata("model"))
                : "slim".equals(DefaultPlayerSkin.getSkinModelName(uuid));

        corpse.cacheSkin(skin, slim);
        return skin;
    }
}
