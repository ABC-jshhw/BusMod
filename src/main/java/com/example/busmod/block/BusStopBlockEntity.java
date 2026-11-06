package com.example.busmod.block;

import java.util.ArrayList;
import java.util.List;

import com.example.busmod.menu.BusStopMenu;
import com.example.busmod.registry.ModBlockEntities;
import com.example.busmod.registry.ModBlocks;
import com.example.busmod.village.VillagePlatformBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stores the editable data of a bus stop. The data is saved to the chunk and
 * synchronized to clients so the floating label and the configuration screen
 * show the current values.
 *
 * <p>Besides the name and id, a stop keeps the id of the village it is bound
 * to and the list of platform suffixes that are currently in use. The
 * suffixes are stored on the stop so two platforms of the same stop can never
 * share the same letter.</p>
 */
public class BusStopBlockEntity extends BlockEntity implements MenuProvider {
    public static final int MAX_NAME_LENGTH = 32;
    public static final int MAX_ID_LENGTH = 32;
    public static final int MAX_VILLAGE_NAME_LENGTH = 64;

    private static final String TAG_STOP_NAME = "StopName";
    private static final String TAG_STOP_ID = "StopId";
    private static final String TAG_VILLAGE = "Village";
    private static final String TAG_PLATFORMS = "Platforms";

    private String stopName = "";
    private String stopId = "";
    private String villageName = "";
    private final List<String> platformSuffixes = new ArrayList<>();

    public BusStopBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BUS_STOP.get(), pos, state);
    }

    /**
     * Server ticker: lets villagers of the bound village walk to a platform of
     * this stop from time to time. See {@link VillagePlatformBehaviour}.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, BusStopBlockEntity blockEntity) {
        if (level instanceof ServerLevel serverLevel) {
            VillagePlatformBehaviour.tickStop(serverLevel, pos, blockEntity);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString(TAG_STOP_NAME, this.stopName);
        tag.putString(TAG_STOP_ID, this.stopId);
        tag.putString(TAG_VILLAGE, this.villageName);

        ListTag platforms = new ListTag();
        for (String suffix : this.platformSuffixes) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Suffix", suffix);
            platforms.add(entry);
        }
        tag.put(TAG_PLATFORMS, platforms);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.stopName = sanitize(tag.getString(TAG_STOP_NAME), MAX_NAME_LENGTH);
        this.stopId = sanitize(tag.getString(TAG_STOP_ID), MAX_ID_LENGTH);
        this.villageName = sanitize(tag.getString(TAG_VILLAGE), MAX_VILLAGE_NAME_LENGTH);

        this.platformSuffixes.clear();
        ListTag platforms = tag.getList(TAG_PLATFORMS, Tag.TAG_COMPOUND);
        for (int i = 0; i < platforms.size(); i++) {
            String suffix = platforms.getCompound(i).getString("Suffix");
            if (!suffix.isEmpty()) {
                this.platformSuffixes.add(suffix);
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.busmod.bus_stop");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BusStopMenu(containerId, playerInventory, this.worldPosition, this.stopName, this.stopId,
                this.villageName, this.isVillageBound(), "");
    }

    public String getStopName() {
        return this.stopName;
    }

    public String getStopId() {
        return this.stopId;
    }

    /** Name of the bound village, or an empty string when unbound. */
    public String getVillageName() {
        return this.villageName;
    }

    public boolean isVillageBound() {
        return !this.villageName.isEmpty();
    }

    /** Suffixes of all platforms bound to this stop. */
    public List<String> getPlatformSuffixes() {
        return List.copyOf(this.platformSuffixes);
    }

    /**
     * Updates the stop data, marks the chunk dirty and sends the new data to
     * every client that has the block loaded.
     */
    public void setStopInfo(String name, String id) {
        this.stopName = sanitize(name, MAX_NAME_LENGTH);
        this.stopId = sanitize(id, MAX_ID_LENGTH);
        this.setChanged();
        this.sync();
    }

    /** Binds or unbinds the stop from a village. */
    public void setVillageName(String village) {
        this.villageName = sanitize(village, MAX_VILLAGE_NAME_LENGTH);
        this.setChanged();
        this.sync();
        this.refreshPlatforms();
    }

    /**
     * Mirrors the village flag of this stop into the {@code VILLAGE} block
     * state of every platform bound to it, so the model variant follows the
     * binding without a block entity renderer.
     */
    private void refreshPlatforms() {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return;
        }
        boolean bound = this.isVillageBound();
        for (BlockPos platformPos : VillagePlatformBehaviour.findPlatforms(serverLevel, this.worldPosition)) {
            BlockState platformState = serverLevel.getBlockState(platformPos);
            if (platformState.is(ModBlocks.BUS_PLATFORM.get())
                    && platformState.getValue(BusPlatformBlock.VILLAGE) != bound) {
                serverLevel.setBlock(platformPos, platformState.setValue(BusPlatformBlock.VILLAGE, bound),
                        Block.UPDATE_ALL);
            }
        }
    }

    /**
     * Reserves a free platform suffix for this stop.
     *
     * @return the reserved suffix, or {@code null} when all 26 letters are used
     */
    public String reservePlatformSuffix() {
        for (char c = 'A'; c <= 'Z'; c++) {
            String suffix = String.valueOf(c);
            if (!this.platformSuffixes.contains(suffix)) {
                this.platformSuffixes.add(suffix);
                this.setChanged();
                this.sync();
                return suffix;
            }
        }
        return null;
    }

    /** Releases a suffix after the platform was removed. */
    public void releasePlatformSuffix(String suffix) {
        if (this.platformSuffixes.remove(suffix)) {
            this.setChanged();
            this.sync();
        }
    }

    /** Returns {@code true} when the suffix is already used by this stop. */
    public boolean hasPlatformSuffix(String suffix) {
        return this.platformSuffixes.contains(suffix);
    }

    private void sync() {
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
