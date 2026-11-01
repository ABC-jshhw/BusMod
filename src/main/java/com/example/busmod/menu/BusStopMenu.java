package com.example.busmod.menu;

import com.example.busmod.block.BusStopBlockEntity;
import com.example.busmod.network.SetBusStopInfoPayload;
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
 * The stop name, stop id and the bound village are transmitted in the menu
 * open packet, so no data slots are required.
 */
public class BusStopMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final BlockPos pos;
    private final String stopName;
    private final String stopId;
    private final String villageName;
    private final boolean villageBound;

    /**
     * Client-side constructor invoked by {@code IMenuTypeExtension}.
     */
    public BusStopMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, buffer.readBlockPos(),
                buffer.readUtf(BusStopBlockEntity.MAX_NAME_LENGTH),
                buffer.readUtf(BusStopBlockEntity.MAX_ID_LENGTH),
                buffer.readUtf(BusStopBlockEntity.MAX_VILLAGE_NAME_LENGTH),
                buffer.readBoolean());
    }

    public BusStopMenu(int containerId, Inventory playerInventory, BlockPos pos, String stopName, String stopId,
            String villageName, boolean villageBound) {
        super(ModMenus.BUS_STOP.get(), containerId);
        this.pos = pos;
        this.stopName = stopName == null ? "" : stopName;
        this.stopId = stopId == null ? "" : stopId;
        this.villageName = villageName == null ? "" : villageName;
        this.villageBound = villageBound;
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

    /** Name of the village bound to this stop, or an empty string. */
    public String getVillageName() {
        return this.villageName;
    }

    /** {@code true} when the stop is currently bound to a village. */
    public boolean isVillageBound() {
        return this.villageBound;
    }

    /**
     * Uses the same distance as {@link SetBusStopInfoPayload#MAX_EDIT_DISTANCE}
     * so the screen closes exactly when the server would reject an update.
     */
    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate((level, blockPos) ->
                level.getBlockState(blockPos).is(ModBlocks.BUS_STOP.get())
                        && player.canInteractWithBlock(blockPos, SetBusStopInfoPayload.MAX_EDIT_DISTANCE),
                true);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
