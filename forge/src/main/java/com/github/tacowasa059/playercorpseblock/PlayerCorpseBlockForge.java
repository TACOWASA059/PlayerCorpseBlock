package com.github.tacowasa059.playercorpseblock;

import com.github.tacowasa059.playercorpseblock.block.PlayerCorpseBlockEntity;
import com.github.tacowasa059.playercorpseblock.client.PlayerCorpseRenderer;
import com.github.tacowasa059.playercorpseblock.corpse.CorpsePlacer;
import com.github.tacowasa059.playercorpseblock.registry.ModContent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@Mod(Constants.MOD_ID)
public class PlayerCorpseBlockForge {

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Constants.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Constants.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Constants.MOD_ID);

    static {
        // All of them are built by the common project, but only while Forge has the matching registry open.
        BLOCKS.register(ModContent.PLAYER_CORPSE_ID.getPath(), ModContent::createPlayerCorpseBlock);
        ITEMS.register(ModContent.PLAYER_CORPSE_ID.getPath(),
                () -> ModContent.setPlayerCorpseItem(new ForgePlayerCorpseBlockItem(ModContent.playerCorpse(), new Item.Properties())));
        BLOCK_ENTITIES.register(ModContent.PLAYER_CORPSE_ID.getPath(), () -> {
            final BlockEntityType<PlayerCorpseBlockEntity> type =
                    BlockEntityType.Builder.of(PlayerCorpseBlockEntity::new, ModContent.playerCorpse()).build(null);
            ModContent.setPlayerCorpseEntityType(type);
            return type;
        });
    }

    public PlayerCorpseBlockForge() {

        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        modEventBus.addListener(PlayerCorpseBlockForge::addCreative);

        PlayerCorpseBlockMod.init();

        // Lowest priority and no cancelled events, so a mod that saves the player from dying also stops the corpse.
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingDeathEvent.class, PlayerCorpseBlockForge::onLivingDeath);
    }

    private static void addCreative(BuildCreativeModeTabContentsEvent event) {

        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModContent.playerCorpseItem());
        }
    }

    private static void onLivingDeath(LivingDeathEvent event) {

        if (event.getEntity() instanceof ServerPlayer player && !(player instanceof FakePlayer)) {
            CorpsePlacer.onPlayerDeath(player);
        }
    }

    @Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {

            event.registerBlockEntityRenderer(ModContent.playerCorpseEntity(), PlayerCorpseRenderer::new);
        }
    }
}
