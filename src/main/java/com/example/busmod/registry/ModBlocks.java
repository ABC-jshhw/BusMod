package com.example.busmod.registry;

import com.example.busmod.BusMod;
import com.example.busmod.block.BusStopBlock;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BusMod.MODID);

    public static final DeferredBlock<BusStopBlock> BUS_STOP = BLOCKS.registerBlock(
            "bus_stop",
            BusStopBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .requiresCorrectToolForDrops());

    private ModBlocks() {
    }
}
