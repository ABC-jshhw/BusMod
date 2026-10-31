package com.example.busmod.network;

import com.example.busmod.BusMod;
import com.example.busmod.block.BusStopBlockEntity;
import com.example.busmod.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent by the client when the player clicks "Save" in the bus stop screen.
 * The server validates the block and the interaction distance before the
 * values are applied.
 */
public record SetBusStopInfoPayload(BlockPos pos, String name, String id) implements CustomPacketPayload {
    public static final Type<SetBusStopInfoPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BusMod.MODID, "set_bus_stop_info"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetBusStopInfoPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SetBusStopInfoPayload::pos,
                    ByteBufCodecs.stringUtf8(BusStopBlockEntity.MAX_NAME_LENGTH), SetBusStopInfoPayload::name,
                    ByteBufCodecs.stringUtf8(BusStopBlockEntity.MAX_ID_LENGTH), SetBusStopInfoPayload::id,
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
            if (!player.canInteractWithBlock(pos, 4.0D)) {
                return;
            }

            if (player.level().getBlockEntity(pos) instanceof BusStopBlockEntity blockEntity) {
                blockEntity.setStopInfo(payload.name(), payload.id());
                player.displayClientMessage(Component.translatable("message.busmod.bus_stop.saved"), true);
            }
        });
    }
}
