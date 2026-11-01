package com.example.busmod.client;

import org.joml.Matrix4f;

import com.example.busmod.block.BusPlatformBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/**
 * Renders the platform id above a platform block.
 *
 * <p>The id is {@code <stopId>-<suffix>}. When the suffix is already used by
 * another platform of the same stop (which can only happen after manual NBT
 * edits) the label is drawn in red so the conflict is visible in world.</p>
 */
public class BusPlatformRenderer implements BlockEntityRenderer<BusPlatformBlockEntity> {
    private static final float TEXT_SCALE = 0.022F;
    private static final int BACKGROUND_COLOR = 0x66000000;
    private static final int ID_COLOR = 0xFFFFFFFF;
    private static final int CONFLICT_COLOR = 0xFFFF5555;
    private static final double LABEL_HEIGHT = 1.15D;
    private static final double MAX_RENDER_DISTANCE = 24.0D;
    private static final double FULL_SIZE_DISTANCE = 6.0D;
    private static final float MIN_SCALE_FACTOR = 0.55F;

    public BusPlatformRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(BusPlatformBlockEntity blockEntity, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!blockEntity.isBound()) {
            return;
        }

        String id = blockEntity.getPlatformId();
        if (id.isEmpty()) {
            return;
        }

        boolean conflict = blockEntity.getStop() != null
                && blockEntity.getStop().getPlatformSuffixes().stream()
                        .filter(suffix -> suffix.equals(blockEntity.getSuffix()))
                        .count() > 1;

        Font font = Minecraft.getInstance().font;
        Component label = Component.literal(id);

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

        font.drawInBatch(label, -font.width(label) / 2.0F, 0.0F,
                conflict ? CONFLICT_COLOR : ID_COLOR, false, matrix,
                bufferSource, Font.DisplayMode.NORMAL, BACKGROUND_COLOR, packedLight);

        poseStack.popPose();
    }

    @Override
    public int getViewDistance() {
        return (int) MAX_RENDER_DISTANCE;
    }
}
