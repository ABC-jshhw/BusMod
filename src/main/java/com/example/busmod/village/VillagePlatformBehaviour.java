package com.example.busmod.village;

import java.util.ArrayList;
import java.util.List;

import com.example.busmod.block.BusPlatformBlockEntity;
import com.example.busmod.block.BusStopBlockEntity;
import com.example.busmod.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

/**
 * Makes villagers of a village bound to a bus stop occasionally walk to one of
 * the platforms of that stop and return home afterwards.
 *
 * <p>The logic is intentionally stateless: every tick a small random chance is
 * rolled per stop. When it fires, a villager that is currently inside the
 * village bounds and has no walk target is sent to a platform. The vanilla
 * brain then walks the villager there; once it arrives the walk target is
 * cleared and the villager's normal schedule takes it back home.</p>
 */
public final class VillagePlatformBehaviour {
    /** Chance per stop per tick that a villager is sent to a platform. */
    private static final float SEND_CHANCE = 0.002F;
    /** Maximum number of villagers that may be sent per stop per tick. */
    private static final int MAX_SENT_PER_TICK = 1;
    /** How close a villager must be to the platform to count as arrived. */
    private static final int ARRIVE_DISTANCE = 2;
    /** Search radius around the platform for candidate villagers. */
    private static final int SEARCH_RADIUS = 48;

    private VillagePlatformBehaviour() {
    }

    /**
     * Rolls the send chance for a single stop. Called by the stop block entity
     * from its own ticker so only loaded stops are considered.
     */
    public static void tickStop(ServerLevel level, BlockPos stopPos, BusStopBlockEntity stop) {
        if (!stop.isVillageBound() || level.random.nextFloat() > SEND_CHANCE) {
            return;
        }

        List<BlockPos> platforms = findPlatforms(level, stopPos);
        if (platforms.isEmpty()) {
            return;
        }

        BlockPos target = platforms.get(level.random.nextInt(platforms.size()));
        int sent = 0;
        for (Villager villager : level.getEntitiesOfClass(Villager.class,
                new AABB(target).inflate(SEARCH_RADIUS))) {
            if (sent >= MAX_SENT_PER_TICK) {
                break;
            }
            if (!canSend(villager, target)) {
                continue;
            }
            villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                    new WalkTarget(target, 0.5F, ARRIVE_DISTANCE));
            sent++;
        }
    }

    private static boolean canSend(Villager villager, BlockPos target) {
        if (villager.isBaby() || villager.isSleeping() || villager.isTrading()) {
            return false;
        }
        if (villager.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)) {
            return false;
        }
        return villager.blockPosition().distSqr(target) > (long) ARRIVE_DISTANCE * ARRIVE_DISTANCE;
    }

    /**
     * Collects the positions of all platforms that are bound to {@code stopPos}.
     * The search is limited to the chunks around the stop so it stays cheap.
     */
    public static List<BlockPos> findPlatforms(ServerLevel level, BlockPos stopPos) {
        List<BlockPos> result = new ArrayList<>();
        int radius = 16;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx += 4) {
            for (int dz = -radius; dz <= radius; dz += 4) {
                for (int dy = -4; dy <= 4; dy += 4) {
                    cursor.set(stopPos.getX() + dx, stopPos.getY() + dy, stopPos.getZ() + dz);
                    if (!level.isLoaded(cursor)) {
                        continue;
                    }
                    if (!level.getBlockState(cursor).is(ModBlocks.BUS_PLATFORM.get())) {
                        continue;
                    }
                    if (level.getBlockEntity(cursor) instanceof BusPlatformBlockEntity platform
                            && stopPos.equals(platform.getStopPos())) {
                        result.add(cursor.immutable());
                    }
                }
            }
        }
        return result;
    }

    /** Returns the village bounds of a stop, or {@code null} when unbound. */
    public static BoundingBox villageBounds(ServerLevel level, BlockPos stopPos) {
        return VillageUtil.findVillage(level, stopPos)
                .map(VillageUtil.VillageInfo::bounds)
                .orElse(null);
    }
}
