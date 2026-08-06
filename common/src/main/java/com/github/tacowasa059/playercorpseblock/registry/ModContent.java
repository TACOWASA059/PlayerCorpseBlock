package com.github.tacowasa059.playercorpseblock.registry;

import com.github.tacowasa059.playercorpseblock.Constants;
import com.github.tacowasa059.playercorpseblock.block.PlayerCorpseBlock;
import com.github.tacowasa059.playercorpseblock.block.PlayerCorpseBlockEntity;
import com.github.tacowasa059.playercorpseblock.block.PlayerCorpseBlockItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.Objects;

/**
 * Holds the content of the mod for the common code.
 * <p>
 * Nothing is created in a static initializer on purpose: on Forge a block may only be constructed while its
 * registry is open, so both the block and the block entity type are built from the loader specific
 * registration code and handed back here.
 */
public final class ModContent {

    public static final ResourceLocation PLAYER_CORPSE_ID = Constants.id("player_corpse");

    private static PlayerCorpseBlock playerCorpse;
    private static BlockItem playerCorpseItem;
    private static BlockEntityType<PlayerCorpseBlockEntity> playerCorpseEntity;

    private ModContent() {
    }

    /** Creates the corpse block and remembers it. Call this from the loader while the block registry is open. */
    public static PlayerCorpseBlock createPlayerCorpseBlock() {

        playerCorpse = new PlayerCorpseBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .strength(0.6F)
                .sound(SoundType.WOOL)
                .noOcclusion()
                .noLootTable()
                .randomTicks()
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false));

        return playerCorpse;
    }

    public static PlayerCorpseBlock playerCorpse() {

        return Objects.requireNonNull(playerCorpse, "The player corpse block has not been registered yet.");
    }

    /**
     * The item form carries the bodies in its NBT, so a corpse picked in creative can be placed back
     * with the same players in it.
     */
    public static BlockItem createPlayerCorpseItem() {

        return setPlayerCorpseItem(new PlayerCorpseBlockItem(playerCorpse(), new Item.Properties()));
    }

    /** Forge needs its own subclass to hook up the item renderer, so the instance can be handed in. */
    public static BlockItem setPlayerCorpseItem(BlockItem item) {

        playerCorpseItem = item;
        return item;
    }

    public static BlockItem playerCorpseItem() {

        return Objects.requireNonNull(playerCorpseItem, "The player corpse item has not been registered yet.");
    }

    /**
     * {@code BlockEntityType.BlockEntitySupplier} is not public in vanilla, so the type itself is built by
     * the loader (Forge patches the interface, Fabric has its own builder) and handed back here.
     */
    public static void setPlayerCorpseEntityType(BlockEntityType<PlayerCorpseBlockEntity> type) {

        playerCorpseEntity = type;
    }

    public static BlockEntityType<PlayerCorpseBlockEntity> playerCorpseEntity() {

        return Objects.requireNonNull(playerCorpseEntity, "The player corpse block entity type has not been registered yet.");
    }
}
