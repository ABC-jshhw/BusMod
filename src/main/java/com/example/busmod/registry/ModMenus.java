package com.example.busmod.registry;

import com.example.busmod.BusMod;
import com.example.busmod.menu.BusStopMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, BusMod.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<BusStopMenu>> BUS_STOP =
            MENUS.register("bus_stop", () -> IMenuTypeExtension.create(BusStopMenu::new));

    private ModMenus() {
    }
}
