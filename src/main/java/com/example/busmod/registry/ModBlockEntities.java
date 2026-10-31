package com.example.busmod.registry;

import com.example.busmod.BusMod;
import com.example.busmod.block.BusStopBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BusMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BusStopBlockEntity>> BUS_STOP =
            BLOCK_ENTITIES.register("bus_stop", () -> BlockEntityType.Builder
                    .of(BusStopBlockEntity::new, ModBlocks.BUS_STOP.get())
                    .build(null));

    private ModBlockEntities() {
    }
}
