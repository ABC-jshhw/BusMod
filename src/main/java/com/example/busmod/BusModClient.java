package com.example.busmod;

import com.example.busmod.client.BusStopRenderer;
import com.example.busmod.client.BusStopScreen;
import com.example.busmod.registry.ModBlockEntities;
import com.example.busmod.registry.ModMenus;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Client-only hooks: the bus stop configuration screen and the floating
 * label renderer.
 */
@Mod(value = BusMod.MODID, dist = Dist.CLIENT)
public final class BusModClient {
    public BusModClient(IEventBus modEventBus) {
        modEventBus.addListener(BusModClient::registerScreens);
        modEventBus.addListener(BusModClient::registerBlockEntityRenderers);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.BUS_STOP.get(), BusStopScreen::new);
    }

    private static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.BUS_STOP.get(), BusStopRenderer::new);
    }
}
