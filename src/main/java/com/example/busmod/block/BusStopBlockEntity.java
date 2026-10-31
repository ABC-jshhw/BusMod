package com.example.busmod.block;

import com.example.busmod.menu.BusStopMenu;
import com.example.busmod.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stores the editable data of a bus stop. The data is saved to the chunk and
 * synchronized to clients so the floating label and the configuration screen
 * show the current values.
 */
public class BusStopBlockEntity extends BlockEntity implements MenuProvider {
    public static final int MAX_NAME_LENGTH = 32;
    public static final int MAX_ID_LENGTH = 32;

    private static final String TAG_STOP_NAME = "StopName";
    private static final String TAG_STOP_ID = "StopId";

    private String stopName = "";
    private String stopId = "";

    public BusStopBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BUS_STOP.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString(TAG_STOP_NAME, this.stopName);
        tag.putString(TAG_STOP_ID, this.stopId);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.stopName = sanitize(tag.getString(TAG_STOP_NAME), MAX_NAME_LENGTH);
        this.stopId = sanitize(tag.getString(TAG_STOP_ID), MAX_ID_LENGTH);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.busmod.bus_stop");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BusStopMenu(containerId, playerInventory, this.worldPosition, this.stopName, this.stopId);
    }

    public String getStopName() {
        return this.stopName;
    }

    public String getStopId() {
        return this.stopId;
    }

    /**
     * Updates the stop data, marks the chunk dirty and sends the new data to
     * every client that has the block loaded.
     */
    public void setStopInfo(String name, String id) {
        this.stopName = sanitize(name, MAX_NAME_LENGTH);
        this.stopId = sanitize(id, MAX_ID_LENGTH);
        this.setChanged();

        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(),
                    Block.UPDATE_ALL);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private static String sanitize(String value, int maxLength) {
        if (value == null) {
            return "";
        }

        String cleaned = value.replaceAll("\\p{Cntrl}", "").replace("§", "").strip();
        return cleaned.length() > maxLength ? cleaned.substring(0, maxLength) : cleaned;
    }
}
