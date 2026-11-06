package com.example.busmod.network;

import com.example.busmod.BusMod;
import com.example.busmod.block.BusStopBlockEntity;
import com.example.busmod.registry.ModBlocks;
import com.example.busmod.village.VillageUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent by the client when the player clicks "Save" in the bus stop screen.
 * The server validates the block, the interaction distance and the player's
 * permission before the values are applied.
 *
 * <p>{@code bindVillage} asks the server to bind the stop to the village that
 * contains it. The village name is never sent by the client: it is resolved
 * server side so a modified client cannot fake a village.</p>
 */
public record SetBusStopInfoPayload(BlockPos pos, String name, String id, boolean bindVillage)
        implements CustomPacketPayload {
    /** Maximum distance between player and block for editing, in blocks. */
    public static final double MAX_EDIT_DISTANCE = 4.0D;

    public static final Type<SetBusStopInfoPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BusMod.MODID, "set_bus_stop_info"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetBusStopInfoPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SetBusStopInfoPayload::pos,
                    ByteBufCodecs.stringUtf8(BusStopBlockEntity.MAX_NAME_LENGTH), SetBusStopInfoPayload::name,
                    ByteBufCodecs.stringUtf8(BusStopBlockEntity.MAX_ID_LENGTH), SetBusStopInfoPayload::id,
                    ByteBufCodecs.BOOL, SetBusStopInfoPayload::bindVillage,
                    SetBusStopInfoPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetBusStopInfoPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.level().isClientSide()) {
                return;
            }

            BlockPos pos = payload.pos();
            BlockState state = player.level().getBlockState(pos);
            if (!state.is(ModBlocks.BUS_STOP.get())) {
                return;
            }
            if (!player.canInteractWithBlock(pos, MAX_EDIT_DISTANCE)) {
                return;
            }
            if (!canEdit(player)) {
                player.displayClientMessage(Component.translatable("message.busmod.bus_stop.no_permission"), true);
                return;
            }

            if (player.level().getBlockEntity(pos) instanceof BusStopBlockEntity blockEntity) {
                blockEntity.setStopInfo(payload.name(), payload.id());
                applyVillageBinding(player, pos, blockEntity, payload.bindVillage());
                player.displayClientMessage(Component.translatable("message.busmod.bus_stop.saved"), true);
            }
        });
    }

    /**
     * Binds or unbinds the stop from the village that contains it. When the
     * player asks for a binding but no village was found, the stop stays
     * unbound and the player is told why.
     */
    private static void applyVillageBinding(Player player, BlockPos pos, BusStopBlockEntity blockEntity,
            boolean bindVillage) {
        if (!bindVillage) {
            if (blockEntity.isVillageBound()) {
                blockEntity.setVillageName("");
            }
            return;
        }

        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        VillageUtil.findVillage(serverLevel, pos, VillageUtil.SEARCH_RADIUS).ifPresentOrElse(
                village -> blockEntity.setVillageName(village.name()),
                () -> player.displayClientMessage(
                        Component.translatable("message.busmod.bus_stop.no_village"), true));
    }

    /**
     * Only creative-mode players and server operators may rename a stop.
     */
    private static boolean canEdit(Player player) {
        if (player.isSpectator()) {
            return false;
        }
        if (player.canUseGameMasterBlocks()) {
            return true;
        }
        return player instanceof ServerPlayer serverPlayer && serverPlayer.hasPermissions(2);
    }
}
