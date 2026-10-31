package com.example.busmod.menu;

import com.example.busmod.block.BusStopBlockEntity;
import com.example.busmod.registry.ModBlocks;
import com.example.busmod.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

/**
 * Empty container menu used to open the client-side {@code BusStopScreen}.
 * The stop name and stop id are transmitted in the menu open packet, so no
 * data slots are required.
 */
public class BusStopMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final BlockPos pos;
    private final String stopName;
    private final String stopId;

    /**
     * Client-side constructor invoked by {@code IMenuTypeExtension}.
     */
    public BusStopMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, buffer.readBlockPos(),
                buffer.readUtf(BusStopBlockEntity.MAX_NAME_LENGTH),
                buffer.readUtf(BusStopBlockEntity.MAX_ID_LENGTH));
    }

    public BusStopMenu(int containerId, Inventory playerInventory, BlockPos pos, String stopName, String stopId) {
        super(ModMenus.BUS_STOP.get(), containerId);
        this.pos = pos;
        this.stopName = stopName == null ? "" : stopName;
        this.stopId = stopId == null ? "" : stopId;
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public String getStopName() {
        return this.stopName;
    }

    public String getStopId() {
        return this.stopId;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.BUS_STOP.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
