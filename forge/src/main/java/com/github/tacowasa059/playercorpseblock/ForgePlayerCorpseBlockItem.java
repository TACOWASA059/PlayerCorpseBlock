package com.github.tacowasa059.playercorpseblock;

import com.github.tacowasa059.playercorpseblock.block.PlayerCorpseBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/**
 * Same item as on the other loaders, plus the Forge hook that lets the corpse be drawn as an actual body
 * in inventories and in hand.
 */
public class ForgePlayerCorpseBlockItem extends PlayerCorpseBlockItem {

    public ForgePlayerCorpseBlockItem(Block block, Properties properties) {

        super(block, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {

        // Only ever called on the client, so the client only class is not loaded on a server.
        consumer.accept(ForgeCorpseItemExtensions.create());
    }
}
