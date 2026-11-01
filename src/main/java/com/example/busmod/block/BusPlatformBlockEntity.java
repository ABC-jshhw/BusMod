package com.example.busmod.block;

import com.example.busmod.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Data of a single platform. A platform is always bound to a bus stop and
 * stores the suffix that makes its id unique within that stop.
 *
 * <p>The full platform id is {@code <stopId>-<suffix>}, e.g. {@code 001-A}.
 * The stop id part is not stored here: it is read from the bound stop so
 * renaming a stop automatically updates every platform id.</p>
 */
public class BusPlatformBlockEntity extends BlockEntity {
    private static final String TAG_STOP_POS = "StopPos";
    private static final String TAG_SUFFIX = "Suffix";

    private BlockPos stopPos;
    private String suffix = "";

    public BusPlatformBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BUS_PLATFORM.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.stopPos != null) {
            tag.putLong(TAG_STOP_POS, this.stopPos.asLong());
        }
        tag.putString(TAG_SUFFIX, this.suffix);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.stopPos = tag.contains(TAG_STOP_POS) ? BlockPos.of(tag.getLong(TAG_STOP_POS)) : null;
        this.suffix = tag.getString(TAG_SUFFIX);
    }

    /** Position of the bound bus stop, or {@code null} when unbound. */
    public BlockPos getStopPos() {
        return this.stopPos;
    }

    public String getSuffix() {
        return this.suffix;
    }

    public boolean isBound() {
        return this.stopPos != null && !this.suffix.isEmpty();
    }

    /** Binds this platform to a stop with the given suffix. */
    public void bind(BlockPos stopPos, String suffix) {
        this.stopPos = stopPos.immutable();
        this.suffix = suffix;
        this.setChanged();
        this.sync();
    }

    /** Removes the binding, used when the platform is broken. */
    public void unbind() {
        this.stopPos = null;
        this.suffix = "";
        this.setChanged();
        this.sync();
    }

    /** Returns the bound stop block entity, or {@code null}. */
    public BusStopBlockEntity getStop() {
        if (this.stopPos == null || this.level == null) {
            return null;
        }
        return this.level.getBlockEntity(this.stopPos) instanceof BusStopBlockEntity stop ? stop : null;
    }

    /**
     * Full platform id, e.g. {@code 001-A}. Falls back to the suffix alone
     * when the stop is missing or has no id yet.
     */
    public String getPlatformId() {
        BusStopBlockEntity stop = this.getStop();
        String stopId = stop == null ? "" : stop.getStopId();
        if (stopId.isEmpty()) {
            return this.suffix;
        }
        return stopId + "-" + this.suffix;
    }

    /** Display name of the bound stop, or an empty string. */
    public String getStopName() {
        BusStopBlockEntity stop = this.getStop();
        return stop == null ? "" : stop.getStopName();
    }

    /** {@code true} when the bound stop is itself bound to a village. */
    public boolean isVillageBound() {
        BusStopBlockEntity stop = this.getStop();
        return stop != null && stop.isVillageBound();
    }

    /**
     * Releases this platform from its stop: the suffix is returned to the
     * stop's free pool and the local binding is cleared. Called when the
     * platform block is removed.
     */
    public void releaseFromStop() {
        BusStopBlockEntity stop = this.getStop();
        if (stop != null && !this.suffix.isEmpty()) {
            stop.releasePlatformSuffix(this.suffix);
        }
        this.unbind();
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
}
