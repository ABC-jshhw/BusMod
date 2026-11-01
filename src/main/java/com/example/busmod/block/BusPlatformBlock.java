package com.example.busmod.block;

import java.util.EnumMap;
import java.util.Map;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A platform that belongs to a bus stop.
 *
 * <p>A platform can only be placed when the held item carries a
 * {@link PlatformBinding}. The binding is created by right-clicking a bus stop
 * while holding an unbound platform item. Breaking the stop clears the
 * binding of every platform that pointed at it.</p>
 *
 * <p>{@link #VILLAGE} mirrors the village binding of the stop so the model can
 * show a small village decoration without a block entity renderer.</p>
 */
public class BusPlatformBlock extends BaseEntityBlock {
    public static final MapCodec<BusPlatformBlock> CODEC = simpleCodec(BusPlatformBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty VILLAGE = BooleanProperty.create("village");

    private static final Map<Direction, VoxelShape> SHAPES = createShapes();

    public BusPlatformBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(VILLAGE, Boolean.FALSE));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, VILLAGE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BusPlatformBlockEntity(pos, state);
    }

    /**
     * Copies the binding of the placed item onto the new block entity and
     * mirrors the village flag of the stop into the block state.
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof BusPlatformBlockEntity platform)) {
            return;
        }

        BlockPos stopPos = PlatformBinding.getStopPos(stack);
        String suffix = PlatformBinding.getSuffix(stack);
        if (stopPos == null || suffix.isEmpty()) {
            return;
        }

        platform.bind(stopPos, suffix);
        if (platform.isVillageBound() && !state.getValue(VILLAGE)) {
            level.setBlock(pos, state.setValue(VILLAGE, Boolean.TRUE), Block.UPDATE_ALL);
        }
    }

    /**
     * Releases the suffix on the stop when the platform is removed, so the
     * letter can be reused by another platform of the same stop.
     */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof BusPlatformBlockEntity platform) {
            platform.releaseFromStop();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * Drops a platform item that still carries the binding, so picking the
     * platform up and placing it again keeps its id.
     */
    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity,
            ItemStack tool) {
        if (!level.isClientSide && !player.isCreative()
                && blockEntity instanceof BusPlatformBlockEntity platform && platform.isBound()) {
            ItemStack drop = new ItemStack(this);
            PlatformBinding.bind(drop, platform.getStopPos(), platform.getSuffix());
            Block.popResource(level, pos, drop);
        }
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }

    /** Right-clicking a platform shows its full id in the action bar. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof BusPlatformBlockEntity platform) {
            player.displayClientMessage(describe(platform), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof BusPlatformBlockEntity platform) {
            player.displayClientMessage(describe(platform), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static Component describe(BusPlatformBlockEntity platform) {
        if (!platform.isBound()) {
            return Component.translatable("message.busmod.platform.unbound");
        }
        String id = platform.getPlatformId();
        String stopName = platform.getStopName();
        if (stopName.isEmpty()) {
            return Component.translatable("message.busmod.platform.info", id);
        }
        return Component.translatable("message.busmod.platform.info_named", id, stopName);
    }

    private static Map<Direction, VoxelShape> createShapes() {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            shapes.put(direction, createShape(direction));
        }
        return Map.copyOf(shapes);
    }

    /**
     * Mirrors the element list of {@code models/block/bus_platform.json}: a
     * low slab with a raised kerb, a sign post and a small roof.
     */
    private static VoxelShape createShape(Direction facing) {
        return Shapes.or(
                box(facing, 0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D), // slab
                box(facing, 0.0D, 2.0D, 0.0D, 16.0D, 3.0D, 1.0D), // kerb (front)
                box(facing, 0.5D, 3.0D, 0.5D, 1.5D, 15.0D, 1.5D), // sign post
                box(facing, 1.0D, 12.0D, 0.0D, 15.0D, 15.0D, 0.5D)); // sign board
    }

    /**
     * Builds a box using coordinates of the north-facing model and rotates it
     * together with the block state's facing value.
     */
    private static VoxelShape box(Direction facing, double x1, double y1, double z1, double x2, double y2,
            double z2) {
        return switch (facing) {
            case EAST -> Block.box(16.0D - z2, y1, x1, 16.0D - z1, y2, x2);
            case SOUTH -> Block.box(16.0D - x2, y1, 16.0D - z2, 16.0D - x1, y2, 16.0D - z1);
            case WEST -> Block.box(z1, y1, 16.0D - x2, z2, y2, 16.0D - x1);
            default -> Block.box(x1, y1, z1, x2, y2, z2);
        };
    }
}

