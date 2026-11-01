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
import net.minecraft.world.phys.Vec3;

/**
 * Renders the stop name and stop id as a floating label above the bus stop.
 * <p>
 * The label is placed above the roof (the block is 1 block tall) so it never
 * intersects the model, and it is drawn with {@link Font.DisplayMode#NORMAL}
 * so blocks in front of it still occlude it.
 */
public class BusStopRenderer implements BlockEntityRenderer<BusStopBlockEntity> {
    private static final float TEXT_SCALE = 0.025F;
    private static final int BACKGROUND_COLOR = 0x66000000;
    private static final int NAME_COLOR = 0xFFFFFFFF;
    private static final int ID_COLOR = 0xFFFFD54F;
    /** Vertical offset above the block, in blocks. Keeps the label clear of the roof. */
    private static final double LABEL_HEIGHT = 1.35D;
    private static final double MAX_RENDER_DISTANCE = 32.0D;
    /** Distance at which the label is drawn at full size. */
    private static final double FULL_SIZE_DISTANCE = 8.0D;
    private static final float MIN_SCALE_FACTOR = 0.55F;
    private static final int LINE_HEIGHT = 10;

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

        // Shrink the label with distance so it stays readable but does not
        // cover the whole screen when the player walks away.
        float distanceFactor = 1.0F;
        if (Minecraft.getInstance().player != null) {
            double distance = Minecraft.getInstance().player.position()
                    .distanceTo(Vec3.atCenterOf(blockEntity.getBlockPos()));
            distanceFactor = (float) Math.max(MIN_SCALE_FACTOR,
                    Math.min(1.0D, FULL_SIZE_DISTANCE / Math.max(distance, 0.001D)));
        }
        float scale = TEXT_SCALE * distanceFactor;

        poseStack.pushPose();
        poseStack.translate(0.5D, LABEL_HEIGHT, 0.5D);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(scale, -scale, scale);
        Matrix4f matrix = poseStack.last().pose();

        float nameY = idComponent == null ? 0.0F : -LINE_HEIGHT;
        drawLine(font, nameComponent, nameY, NAME_COLOR, matrix, bufferSource, packedLight);
        if (idComponent != null) {
            drawLine(font, idComponent, 0.0F, ID_COLOR, matrix, bufferSource, packedLight);
        }

        poseStack.popPose();
    }

    private static void drawLine(Font font, Component text, float y, int color, Matrix4f matrix,
            MultiBufferSource bufferSource, int packedLight) {
        font.drawInBatch(text, -font.width(text) / 2.0F, y, color, false, matrix,
                bufferSource, Font.DisplayMode.NORMAL, BACKGROUND_COLOR, packedLight);
    }

    @Override
    public int getViewDistance() {
        return (int) MAX_RENDER_DISTANCE;
    }
}
