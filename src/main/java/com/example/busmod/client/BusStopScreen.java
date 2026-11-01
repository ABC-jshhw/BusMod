package com.example.busmod.client;

import org.lwjgl.glfw.GLFW;

import com.example.busmod.block.BusStopBlockEntity;
import com.example.busmod.menu.BusStopMenu;
import com.example.busmod.network.SetBusStopInfoPayload;
import com.example.busmod.village.VillageUtil;

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
    private static final int PANEL_WIDTH = 240;
    private static final int PANEL_HEIGHT = 196;
    private static final int FIELD_WIDTH = 200;
    private static final int FIELD_HEIGHT = 20;
    private static final int FIELD_X = (PANEL_WIDTH - FIELD_WIDTH) / 2;
    private static final int BUTTON_WIDTH = 96;
    private static final int BUTTON_HEIGHT = 20;

    // Palette
    private static final int COLOR_SHADOW = 0x66000000;
    private static final int COLOR_BORDER = 0xFF1B1B1B;
    private static final int COLOR_PANEL = 0xFFF4F1EA;
    private static final int COLOR_HEADER = 0xFF15509B;
    private static final int COLOR_HEADER_ACCENT = 0xFF2E7BD6;
    private static final int COLOR_TEXT = 0xFF2B2B2B;
    private static final int COLOR_TEXT_MUTED = 0xFF6B6B6B;

    private final BusStopMenu menu;
    private EditBox nameBox;
    private EditBox idBox;
    private Button villageButton;
    private boolean bindVillage;
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

        int fieldX = this.leftPos + FIELD_X;

        this.nameBox = new EditBox(this.font, fieldX, this.topPos + 48, FIELD_WIDTH, FIELD_HEIGHT,
                Component.translatable("gui.busmod.bus_stop.name"));
        this.nameBox.setMaxLength(BusStopBlockEntity.MAX_NAME_LENGTH);
        this.nameBox.setValue(this.menu.getStopName());
        this.nameBox.setHint(Component.translatable("gui.busmod.bus_stop.name_hint"));
        this.addRenderableWidget(this.nameBox);

        this.idBox = new EditBox(this.font, fieldX, this.topPos + 90, FIELD_WIDTH, FIELD_HEIGHT,
                Component.translatable("gui.busmod.bus_stop.id"));
        this.idBox.setMaxLength(BusStopBlockEntity.MAX_ID_LENGTH);
        this.idBox.setValue(this.menu.getStopId());
        this.idBox.setHint(Component.translatable("gui.busmod.bus_stop.id_hint"));
        this.addRenderableWidget(this.idBox);

        this.bindVillage = this.menu.isVillageBound();
        this.villageButton = Button.builder(villageLabel(), button -> {
            this.bindVillage = !this.bindVillage;
            button.setMessage(villageLabel());
        }).bounds(fieldX, this.topPos + 132, FIELD_WIDTH, FIELD_HEIGHT).build();
        this.addRenderableWidget(this.villageButton);

        int buttonY = this.topPos + PANEL_HEIGHT - 32;
        int gap = 8;
        int totalWidth = BUTTON_WIDTH * 2 + gap;
        int buttonX = this.leftPos + (PANEL_WIDTH - totalWidth) / 2;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.busmod.bus_stop.save"),
                button -> this.save())
                .bounds(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"),
                button -> this.onClose())
                .bounds(buttonX + BUTTON_WIDTH + gap, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        this.setInitialFocus(this.nameBox);
    }

    private Component villageLabel() {
        String village = this.menu.getVillageName();
        Component state = Component.translatable(this.bindVillage
                ? "gui.busmod.bus_stop.village.on"
                : "gui.busmod.bus_stop.village.off");
        if (village.isEmpty()) {
            return Component.translatable("gui.busmod.bus_stop.village", state);
        }
        return Component.translatable("gui.busmod.bus_stop.village_named", state, village);
    }

    private void save() {
        PacketDistributor.sendToServer(new SetBusStopInfoPayload(
                this.menu.getPos(),
                this.nameBox.getValue().strip(),
                this.idBox.getValue().strip(),
                this.bindVillage));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        int x = this.leftPos;
        int y = this.topPos;
        int right = x + PANEL_WIDTH;
        int bottom = y + PANEL_HEIGHT;

        // Drop shadow
        graphics.fill(x + 4, y + 4, right + 4, bottom + 4, COLOR_SHADOW);
        // Border + panel
        graphics.fill(x, y, right, bottom, COLOR_BORDER);
        graphics.fill(x + 1, y + 1, right - 1, bottom - 1, COLOR_PANEL);
        // Header bar with a lighter accent stripe at the bottom
        graphics.fill(x + 1, y + 1, right - 1, y + 30, COLOR_HEADER);
        graphics.fill(x + 1, y + 28, right - 1, y + 30, COLOR_HEADER_ACCENT);
        graphics.drawString(this.font, this.title, x + 12, y + 11, 0xFFFFFFFF, false);

        // Field labels
        graphics.drawString(this.font, Component.translatable("gui.busmod.bus_stop.name"),
                x + FIELD_X, y + 38, COLOR_TEXT, false);
        graphics.drawString(this.font, Component.translatable("gui.busmod.bus_stop.id"),
                x + FIELD_X, y + 80, COLOR_TEXT, false);

        // Character counters, right aligned with the fields
        drawCounter(graphics, this.nameBox, BusStopBlockEntity.MAX_NAME_LENGTH,
                x + FIELD_X + FIELD_WIDTH, y + 38);
        drawCounter(graphics, this.idBox, BusStopBlockEntity.MAX_ID_LENGTH,
                x + FIELD_X + FIELD_WIDTH, y + 80);

        // Footer hint
        graphics.drawString(this.font, Component.translatable("gui.busmod.bus_stop.hint"),
                x + FIELD_X, bottom - 14, COLOR_TEXT_MUTED, false);

        // Village binding hint, drawn under the toggle button
        Component villageHint = this.menu.getVillageName().isEmpty()
                ? Component.translatable("gui.busmod.bus_stop.village_hint_none")
                : Component.translatable("gui.busmod.bus_stop.village_hint",
                        VillageUtil.displayName(this.menu.getVillageName()));
        graphics.drawString(this.font, villageHint, x + FIELD_X, y + 156, COLOR_TEXT_MUTED, false);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawCounter(GuiGraphics graphics, EditBox box, int maxLength, int right, int y) {
        String counter = box.getValue().length() + "/" + maxLength;
        graphics.drawString(this.font, counter, right - this.font.width(counter), y, COLOR_TEXT_MUTED, false);
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
