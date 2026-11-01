package com.example.busmod.block;

import java.util.EnumMap;
import java.util.Map;

import com.mojang.serialization.MapCodec;

import com.example.busmod.registry.ModBlockEntities;
import com.example.busmod.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A small bus stop shelter. {@link #FACING} describes the side the bus stop
 * sign is facing. Right-clicking the block opens the stop configuration
 * screen on the server.
 */
public class BusStopBlock extends BaseEntityBlock {
    public static final MapCodec<BusStopBlock> CODEC = simpleCodec(BusStopBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final Map<Direction, VoxelShape> SHAPES = createShapes();

    public BusStopBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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
        return new BusStopBlockEntity(pos, state);
    }

    @Override
    protected <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(blockEntityType, ModBlockEntities.BUS_STOP.get(),
                BusStopBlockEntity::serverTick);
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof BusStopBlockEntity blockEntity ? blockEntity : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof BusStopBlockEntity blockEntity) {
            serverPlayer.openMenu(blockEntity, buffer -> {
                buffer.writeBlockPos(pos);
                buffer.writeUtf(blockEntity.getStopName(), BusStopBlockEntity.MAX_NAME_LENGTH);
                buffer.writeUtf(blockEntity.getStopId(), BusStopBlockEntity.MAX_ID_LENGTH);
                buffer.writeUtf(blockEntity.getVillageName(), BusStopBlockEntity.MAX_VILLAGE_NAME_LENGTH);
                buffer.writeBoolean(blockEntity.isVillageBound());
            });
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Right-clicking a stop while holding an unbound platform item binds the
     * item to this stop: a free suffix is reserved on the stop and written to
     * the item's data component. The platform can then be placed.
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(stack.getItem() instanceof BlockItem blockItem)
                || !blockItem.getBlock().equals(ModBlocks.BUS_PLATFORM.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof BusStopBlockEntity stop)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (PlatformBinding.isBound(stack)) {
            player.displayClientMessage(Component.translatable("message.busmod.platform.already_bound"), true);
            return ItemInteractionResult.SUCCESS;
        }

        String suffix = stop.reservePlatformSuffix();
        if (suffix == null) {
            player.displayClientMessage(Component.translatable("message.busmod.platform.no_suffix"), true);
            return ItemInteractionResult.SUCCESS;
        }

        PlatformBinding.bind(stack, pos, suffix);
        String stopId = stop.getStopId();
        String platformId = stopId.isEmpty() ? suffix : stopId + "-" + suffix;
        player.displayClientMessage(
                Component.translatable("message.busmod.platform.bound", platformId), true);
        return ItemInteractionResult.SUCCESS;
    }

    private static Map<Direction, VoxelShape> createShapes() {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            shapes.put(direction, createShape(direction));
        }
        return Map.copyOf(shapes);
    }

    /**
     * Mirrors the element list of {@code models/block/bus_stop.json} so the
     * collision and selection boxes match what the player sees.
     */
    private static VoxelShape createShape(Direction facing) {
        return Shapes.or(
                box(facing, 0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D), // base
                box(facing, 0.0D, 12.0D, 0.0D, 16.0D, 13.0D, 16.0D), // roof trim
                box(facing, 0.0D, 13.0D, 0.0D, 16.0D, 15.0D, 16.0D), // roof
                box(facing, 0.0D, 15.0D, 0.0D, 16.0D, 16.0D, 16.0D), // roof cap
                box(facing, 0.5D, 1.0D, 0.5D, 1.5D, 13.0D, 1.5D), // posts
                box(facing, 14.5D, 1.0D, 0.5D, 15.5D, 13.0D, 1.5D),
                box(facing, 0.5D, 1.0D, 14.5D, 1.5D, 13.0D, 15.5D),
                box(facing, 14.5D, 1.0D, 14.5D, 15.5D, 13.0D, 15.5D),
                box(facing, 1.5D, 1.0D, 14.5D, 14.5D, 3.0D, 15.5D), // back wall (lower)
                box(facing, 1.5D, 10.0D, 14.5D, 14.5D, 12.0D, 15.5D), // back wall (upper)
                box(facing, 1.5D, 3.0D, 14.5D, 4.0D, 10.0D, 15.5D), // back wall (left)
                box(facing, 12.0D, 3.0D, 14.5D, 14.5D, 10.0D, 15.5D), // back wall (right)
                box(facing, 7.75D, 3.0D, 14.5D, 8.25D, 10.0D, 15.5D), // back wall (mullion)
                box(facing, 3.0D, 7.0D, 0.0D, 13.0D, 12.0D, 0.5D), // sign
                box(facing, 3.0D, 4.0D, 11.5D, 13.0D, 4.5D, 14.0D), // bench seat
                box(facing, 3.5D, 1.0D, 12.0D, 4.5D, 4.0D, 13.0D), // bench legs
                box(facing, 11.5D, 1.0D, 12.0D, 12.5D, 4.0D, 13.0D));
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
