package com.github.tacowasa059.playercorpseblock.block;

import com.github.tacowasa059.playercorpseblock.config.CorpseConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * A corpse is a plain block with a block entity instead of an entity, so a big pile of them costs
 * about as much as a pile of chests and nothing at all while no player is looking at it.
 * <p>
 * One block holds up to two bodies: the first fills the lower half like a slab, the second one fills
 * the upper half and turns the block into a full cube.
 */
public class PlayerCorpseBlock extends BaseEntityBlock {

    public static final IntegerProperty CORPSES = IntegerProperty.create("corpses", 1, PlayerCorpseBlockEntity.MAX_CORPSES);

    private static final VoxelShape HALF_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);

    public PlayerCorpseBlock(Properties properties) {

        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(CORPSES, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {

        builder.add(CORPSES);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {

        // The bodies are drawn by the block entity renderer, there is no block model to draw.
        return RenderShape.INVISIBLE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {

        return state.getValue(CORPSES) >= PlayerCorpseBlockEntity.MAX_CORPSES ? Shapes.block() : HALF_SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {

        return new PlayerCorpseBlockEntity(pos, state);
    }

    /** Stacks like a slab: a corpse item used on a block that holds one body fills the upper half. */
    @SuppressWarnings("deprecation")
    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {

        if (state.getValue(CORPSES) >= PlayerCorpseBlockEntity.MAX_CORPSES || !context.getItemInHand().is(this.asItem())) {
            return false;
        }

        if (!context.replacingClickedOnBlock()) {
            return true;
        }

        final boolean upperHalf = context.getClickLocation().y - context.getClickedPos().getY() > 0.5D;
        final Direction face = context.getClickedFace();

        return face == Direction.UP || (upperHalf && face.getAxis().isHorizontal());
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {

        final BlockState existing = context.getLevel().getBlockState(context.getClickedPos());

        if (existing.is(this)) {
            return existing.setValue(CORPSES, Math.min(PlayerCorpseBlockEntity.MAX_CORPSES, existing.getValue(CORPSES) + 1));
        }

        return this.defaultBlockState().setValue(CORPSES, 1);
    }

    /** Corpses have no loot table, but the item keeps the bodies in its NBT so it can be placed back. */
    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {

        final ItemStack stack = new ItemStack(this);
        final BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity instanceof PlayerCorpseBlockEntity corpse && !corpse.isEmpty()) {
            blockEntity.saveToItem(stack);
        }

        return stack;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {

        super.setPlacedBy(level, pos, state, placer, stack);

        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof PlayerCorpseBlockEntity corpse)) {
            return;
        }

        // A corpse item without any NBT (creative menu) becomes the body of whoever placed it.
        if (corpse.isEmpty()) {
            corpse.addCorpse(new CorpseData(placer instanceof Player player ? player.getGameProfile() : null,
                    level.getGameTime(),
                    placer == null ? 0.0F : placer.getYRot(),
                    level.random.nextInt(CorpseData.VARIANT_COUNT)));
        }

        // The item may carry two bodies, so the shape has to follow what the block entity actually holds.
        if (state.getValue(CORPSES) != corpse.getCorpseCount()) {
            level.setBlock(pos, state.setValue(CORPSES, corpse.getCorpseCount()), Block.UPDATE_ALL);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {

        final int despawnSeconds = CorpseConfig.get().despawnSeconds();
        if (despawnSeconds <= 0 || !(level.getBlockEntity(pos) instanceof PlayerCorpseBlockEntity corpse)) {
            return;
        }

        if (!corpse.removeExpired(level.getGameTime(), despawnSeconds * 20L)) {
            return;
        }

        if (corpse.isEmpty()) {
            level.removeBlock(pos, false);
            level.levelEvent(2001, pos, Block.getId(state));
        } else {
            level.setBlock(pos, state.setValue(CORPSES, corpse.getCorpseCount()), Block.UPDATE_ALL);
        }
    }
}
