package com.example.busmod.client;

import org.lwjgl.glfw.GLFW;

import com.example.busmod.block.BusStopBlockEntity;
import com.example.busmod.menu.BusStopMenu;
import com.example.busmod.network.SetBusStopInfoPayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Lightweight screen with two text fields used to rename a bus stop and to
 * assign it a stop id. It is not a slot based container screen, so the
 * background is drawn with simple rectangles.
 */
public class BusStopScreen extends Screen implements MenuAccess<BusStopMenu> {
    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 150;

    private final BusStopMenu menu;
    private EditBox nameBox;
    private EditBox idBox;
    private int leftPos;
    private int topPos;

    public BusStopScreen(BusStopMenu menu, Inventory playerInventory, Component title) {
        super(title);
        this.menu = menu;
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - PANEL_WIDTH) / 2;
        this.topPos = (this.height - PANEL_HEIGHT) / 2;

        this.nameBox = new EditBox(this.font, this.leftPos + 20, this.topPos + 44, 180, 20,
                Component.translatable("gui.busmod.bus_stop.name"));
        this.nameBox.setMaxLength(BusStopBlockEntity.MAX_NAME_LENGTH);
        this.nameBox.setValue(this.menu.getStopName());
        this.nameBox.setHint(Component.translatable("gui.busmod.bus_stop.name_hint"));
        this.addRenderableWidget(this.nameBox);

        this.idBox = new EditBox(this.font, this.leftPos + 20, this.topPos + 82, 180, 20,
                Component.translatable("gui.busmod.bus_stop.id"));
        this.idBox.setMaxLength(BusStopBlockEntity.MAX_ID_LENGTH);
        this.idBox.setValue(this.menu.getStopId());
        this.idBox.setHint(Component.translatable("gui.busmod.bus_stop.id_hint"));
        this.addRenderableWidget(this.idBox);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.busmod.bus_stop.save"),
                button -> this.save())
                .bounds(this.leftPos + 20, this.topPos + 118, 86, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"),
                button -> this.onClose())
                .bounds(this.leftPos + 114, this.topPos + 118, 86, 20)
                .build());

        this.setInitialFocus(this.nameBox);
    }

    private void save() {
        PacketDistributor.sendToServer(new SetBusStopInfoPayload(
                this.menu.getPos(),
                this.nameBox.getValue().strip(),
                this.idBox.getValue().strip()));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        int x = this.leftPos;
        int y = this.topPos;
        graphics.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, 0xFF2B2B2B);
        graphics.fill(x + 2, y + 2, x + PANEL_WIDTH - 2, y + PANEL_HEIGHT - 2, 0xFFF3EFE6);
        graphics.fill(x + 2, y + 2, x + PANEL_WIDTH - 2, y + 28, 0xFF15509B);
        graphics.drawString(this.font, this.title, x + 10, y + 10, 0xFFFFFFFF, false);
        graphics.drawString(this.font, Component.translatable("gui.busmod.bus_stop.name"), x + 20, y + 34,
                0xFF3A3A3A, false);
        graphics.drawString(this.font, Component.translatable("gui.busmod.bus_stop.id"), x + 20, y + 72,
                0xFF3A3A3A, false);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            this.save();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.closeContainer();
        }
        super.onClose();
    }

    @Override
    public BusStopMenu getMenu() {
        return this.menu;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
