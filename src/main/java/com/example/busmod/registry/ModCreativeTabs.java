package com.example.busmod.registry;

import com.example.busmod.BusMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BusMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BUS_TAB = CREATIVE_MODE_TABS.register(
            "bus",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.busmod"))
                    .icon(() -> new ItemStack(ModItems.BUS_STOP.get()))
                    .displayItems((parameters, output) -> output.accept(ModItems.BUS_STOP.get()))
                    .build());

    private ModCreativeTabs() {
    }
}
