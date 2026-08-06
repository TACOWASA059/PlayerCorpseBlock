package com.github.tacowasa059.playercorpseblock;

import com.github.tacowasa059.playercorpseblock.block.PlayerCorpseBlock;
import com.github.tacowasa059.playercorpseblock.block.PlayerCorpseBlockEntity;
import com.github.tacowasa059.playercorpseblock.corpse.CorpsePlacer;
import com.github.tacowasa059.playercorpseblock.registry.ModContent;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class PlayerCorpseBlockFabric implements ModInitializer {

    @Override
    public void onInitialize() {

        final PlayerCorpseBlock corpseBlock = ModContent.createPlayerCorpseBlock();
        Registry.register(BuiltInRegistries.BLOCK, ModContent.PLAYER_CORPSE_ID, corpseBlock);
        Registry.register(BuiltInRegistries.ITEM, ModContent.PLAYER_CORPSE_ID, ModContent.createPlayerCorpseItem());

        final BlockEntityType<PlayerCorpseBlockEntity> corpseType =
                FabricBlockEntityTypeBuilder.create(PlayerCorpseBlockEntity::new, corpseBlock).build();
        ModContent.setPlayerCorpseEntityType(corpseType);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ModContent.PLAYER_CORPSE_ID, corpseType);

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                .register(entries -> entries.accept(ModContent.playerCorpseItem()));

        PlayerCorpseBlockMod.init();

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                CorpsePlacer.onPlayerDeath(player);
            }
        });
    }
}
