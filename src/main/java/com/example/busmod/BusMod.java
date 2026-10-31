package com.example.busmod;

import org.slf4j.Logger;

import com.example.busmod.network.ModPayloads;
import com.example.busmod.registry.ModBlockEntities;
import com.example.busmod.registry.ModBlocks;
import com.example.busmod.registry.ModCreativeTabs;
import com.example.busmod.registry.ModItems;
import com.example.busmod.registry.ModMenus;
import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/**
 * Main entry point of the Bus Mod.
 *
 * <p>The mod currently adds a single decorative bus stop block. Every bus stop
 * stores its own stop name and stop id, which can be edited by right-clicking
 * the block.</p>
 */
@Mod(BusMod.MODID)
public class BusMod {
    public static final String MODID = "busmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final ResourceKey<CreativeModeTab> FUNCTIONAL_BLOCKS = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB, ResourceLocation.withDefaultNamespace("functional_blocks"));

    public BusMod(IEventBus modEventBus) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        modEventBus.addListener(ModPayloads::register);
        modEventBus.addListener(this::addCreative);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(FUNCTIONAL_BLOCKS)) {
            event.accept(ModItems.BUS_STOP.get());
        }
    }
}
