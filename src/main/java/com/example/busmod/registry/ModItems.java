package com.example.busmod.registry;

import com.example.busmod.BusMod;

import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BusMod.MODID);

    public static final DeferredItem<BlockItem> BUS_STOP = ITEMS.registerSimpleBlockItem("bus_stop", ModBlocks.BUS_STOP);

    private ModItems() {
    }
}
