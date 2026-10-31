package com.example.busmod.client;

import org.joml.Matrix4f;

import com.example.busmod.block.BusStopBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;

/**
 * Renders the stop name and stop id as a floating label above the bus stop.
 */
public class BusStopRenderer implements BlockEntityRenderer<BusStopBlockEntity> {
    private static final float TEXT_SCALE = 0.025F;
    private static final int BACKGROUND_COLOR = 0x40000000;

    public BusStopRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(BusStopBlockEntity blockEntity, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        String stopName = blockEntity.getStopName();
        String stopId = blockEntity.getStopId();
        Font font = Minecraft.getInstance().font;

        Component nameComponent = stopName.isEmpty()
                ? Component.translatable("block.busmod.bus_stop")
                : Component.literal(stopName);
        Component idComponent = stopId.isEmpty() ? null : Component.literal("[" + stopId + "]");

        poseStack.pushPose();
        poseStack.translate(0.5D, 1.02D, 0.5D);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);
        Matrix4f matrix = poseStack.last().pose();

        float nameY = idComponent == null ? 0.0F : -10.0F;
        font.drawInBatch(nameComponent, -font.width(nameComponent) / 2.0F, nameY, 0xFFFFFFFF, false, matrix,
                bufferSource, Font.DisplayMode.SEE_THROUGH, BACKGROUND_COLOR, packedLight);

        if (idComponent != null) {
            font.drawInBatch(idComponent, -font.width(idComponent) / 2.0F, 0.0F, 0xFFFFD54F, false, matrix,
                    bufferSource, Font.DisplayMode.SEE_THROUGH, BACKGROUND_COLOR, packedLight);
        }

        poseStack.popPose();
    }
}
