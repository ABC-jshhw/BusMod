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
 *
 * <p>The "bind village" toggle is only shown when the server detected a
 * village around the stop. Without a village the stop cannot be bound, so the
 * toggle would be misleading.</p>
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
    private static final int COLOR_DIM = 0x99000000;
    private static final int COLOR_SHADOW = 0x66000000;
    private static final int COLOR_BORDER = 0xFF1B1B1B;
    private static final int COLOR_PANEL = 0xFFF4F1EA;
    private static final int COLOR_PANEL_INNER = 0xFFFBF9F4;
    private static final int COLOR_HEADER = 0xFF15509B;
    private static final int COLOR_HEADER_ACCENT = 0xFF2E7BD6;
    private static final int COLOR_TEXT = 0xFF2B2B2B;
    private static final int COLOR_TEXT_MUTED = 0xFF6B6B6B;
    private static final int COLOR_TEXT_OK = 0xFF1B7F3B;
    private static final int COLOR_TEXT_WARN = 0xFF9A5B00;

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

        // The toggle only exists when a village was detected around the stop.
        this.bindVillage = this.menu.isVillageBound();
        if (this.menu.hasNearbyVillage()) {
            this.villageButton = Button.builder(villageLabel(), button -> {
                this.bindVillage = !this.bindVillage;
                button.setMessage(villageLabel());
            }).bounds(fieldX, this.topPos + 132, FIELD_WIDTH, FIELD_HEIGHT).build();
            this.addRenderableWidget(this.villageButton);
        } else {
            this.bindVillage = false;
        }

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
        Component state = Component.translatable(this.bindVillage
                ? "gui.busmod.bus_stop.village.on"
                : "gui.busmod.bus_stop.village.off");
        return Component.translatable("gui.busmod.bus_stop.village", state);
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
        // Do NOT call renderBackground(): in 1.21 it applies a full screen
        // gaussian blur, which bleeds through the semi transparent panel and
        // makes every label look smeared. A plain dark overlay keeps the text
        // crisp and is cheaper to draw.
        graphics.fill(0, 0, this.width, this.height, COLOR_DIM);

        int x = this.leftPos;
        int y = this.topPos;
        int right = x + PANEL_WIDTH;
        int bottom = y + PANEL_HEIGHT;

        // Drop shadow
        graphics.fill(x + 4, y + 4, right + 4, bottom + 4, COLOR_SHADOW);
        // Border + opaque panel. The panel must be fully opaque: a translucent
        // fill lets the blurred world show through and the text loses contrast.
        graphics.fill(x, y, right, bottom, COLOR_BORDER);
        graphics.fill(x + 1, y + 1, right - 1, bottom - 1, COLOR_PANEL);
        graphics.fill(x + 2, y + 32, right - 2, bottom - 2, COLOR_PANEL_INNER);
        // Header bar with a lighter accent stripe at the bottom
        graphics.fill(x + 1, y + 1, right - 1, y + 30, COLOR_HEADER);
        graphics.fill(x + 1, y + 28, right - 1, y + 30, COLOR_HEADER_ACCENT);

        // Widgets first, text afterwards: the text is then drawn on top of the
        // edit boxes and buttons instead of being blended with them, which is
        // what made the labels look blurry.
        super.render(graphics, mouseX, mouseY, partialTick);

        // Header title, drawn with a shadow so it stays crisp on the blue bar
        graphics.drawString(this.font, this.title, x + 12, y + 11, 0xFFFFFFFF, true);

        // Field labels, drawn with a shadow so they stay readable on the
        // light panel background.
        graphics.drawString(this.font, Component.translatable("gui.busmod.bus_stop.name"),
                x + FIELD_X, y + 38, COLOR_TEXT, true);
        graphics.drawString(this.font, Component.translatable("gui.busmod.bus_stop.id"),
                x + FIELD_X, y + 80, COLOR_TEXT, true);

        // Character counters, right aligned with the fields
        drawCounter(graphics, this.nameBox, BusStopBlockEntity.MAX_NAME_LENGTH,
                x + FIELD_X + FIELD_WIDTH, y + 38);
        drawCounter(graphics, this.idBox, BusStopBlockEntity.MAX_ID_LENGTH,
                x + FIELD_X + FIELD_WIDTH, y + 80);

        // Village status line, drawn under the toggle (or in its place)
        drawVillageStatus(graphics, x + FIELD_X, y + 156);

        // Footer hint
        graphics.drawString(this.font, Component.translatable("gui.busmod.bus_stop.hint"),
                x + FIELD_X, bottom - 14, COLOR_TEXT_MUTED, true);
    }

    /**
     * Draws the village detection result. The line is drawn with a shadow so
     * it stays readable on the light panel background.
     */
    private void drawVillageStatus(GuiGraphics graphics, int x, int y) {
        if (!this.menu.hasNearbyVillage()) {
            graphics.drawString(this.font, Component.translatable("gui.busmod.bus_stop.village_hint_none"),
                    x, y, COLOR_TEXT_WARN, true);
            return;
        }

        Component name = VillageUtil.displayName(this.menu.getNearbyVillage());
        if (this.menu.isVillageBound()) {
            graphics.drawString(this.font,
                    Component.translatable("gui.busmod.bus_stop.village_hint_bound", name),
                    x, y, COLOR_TEXT_OK, true);
        } else {
            graphics.drawString(this.font,
                    Component.translatable("gui.busmod.bus_stop.village_hint", name),
                    x, y, COLOR_TEXT_OK, true);
        }
    }

    private void drawCounter(GuiGraphics graphics, EditBox box, int maxLength, int right, int y) {
        String counter = box.getValue().length() + "/" + maxLength;
        graphics.drawString(this.font, counter, right - this.font.width(counter), y, COLOR_TEXT_MUTED, true);
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
