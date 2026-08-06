package com.github.tacowasa059.playercorpseblock.block;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * One dead player. A corpse block holds up to {@link PlayerCorpseBlockEntity#MAX_CORPSES} of these,
 * which is what makes the pile grow in half block steps.
 */
public class CorpseData {

    public static final int VARIANT_COUNT = 4;

    private static final String TAG_OWNER = "Owner";
    private static final String TAG_DEATH_TIME = "DeathTime";
    private static final String TAG_YAW = "Yaw";
    private static final String TAG_VARIANT = "Variant";

    @Nullable
    private final GameProfile owner;
    private final long deathTime;
    private final float yaw;
    private final int variant;

    /** Client side only, resolved lazily by the renderer and never saved. */
    @Nullable
    private ResourceLocation cachedSkin;
    private boolean cachedSlimArms;
    private boolean skinResolved;

    public CorpseData(@Nullable GameProfile owner, long deathTime, float yaw, int variant) {

        this.owner = owner == null ? null : stripSignature(owner);
        this.deathTime = deathTime;
        this.yaw = yaw;
        this.variant = Math.floorMod(variant, VARIANT_COUNT);
    }

    /**
     * The skin is loaded in insecure mode, so the signature of the texture property is never used.
     * Dropping it roughly halves the amount of data that is saved and sent to clients per corpse.
     */
    private static GameProfile stripSignature(GameProfile profile) {

        final GameProfile stripped = new GameProfile(profile.getId(), profile.getName());
        for (Property property : profile.getProperties().get("textures")) {
            stripped.getProperties().put("textures", new Property("textures", property.getValue()));
        }
        return stripped;
    }

    public CompoundTag save() {

        final CompoundTag tag = new CompoundTag();
        if (this.owner != null) {
            tag.put(TAG_OWNER, NbtUtils.writeGameProfile(new CompoundTag(), this.owner));
        }
        tag.putLong(TAG_DEATH_TIME, this.deathTime);
        tag.putFloat(TAG_YAW, this.yaw);
        tag.putInt(TAG_VARIANT, this.variant);
        return tag;
    }

    public static CorpseData load(CompoundTag tag) {

        final GameProfile owner = tag.contains(TAG_OWNER, Tag.TAG_COMPOUND) ? NbtUtils.readGameProfile(tag.getCompound(TAG_OWNER)) : null;
        return new CorpseData(owner, tag.getLong(TAG_DEATH_TIME), tag.getFloat(TAG_YAW), tag.getInt(TAG_VARIANT));
    }

    @Nullable
    public GameProfile getOwner() {

        return this.owner;
    }

    public long getDeathTime() {

        return this.deathTime;
    }

    public float getYaw() {

        return this.yaw;
    }

    public int getVariant() {

        return this.variant;
    }

    public boolean isSkinResolved() {

        return this.skinResolved;
    }

    @Nullable
    public ResourceLocation getCachedSkin() {

        return this.cachedSkin;
    }

    public boolean hasSlimArms() {

        return this.cachedSlimArms;
    }

    public void cacheSkin(ResourceLocation skin, boolean slimArms) {

        this.cachedSkin = skin;
        this.cachedSlimArms = slimArms;
        this.skinResolved = true;
    }
}
