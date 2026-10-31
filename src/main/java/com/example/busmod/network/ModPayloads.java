package com.example.busmod.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ModPayloads {
    private static final String NETWORK_VERSION = "1";

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(NETWORK_VERSION).playToServer(
                SetBusStopInfoPayload.TYPE,
                SetBusStopInfoPayload.STREAM_CODEC,
                SetBusStopInfoPayload::handle);
    }

    private ModPayloads() {
    }
}
