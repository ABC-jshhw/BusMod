package com.example.busmod.village;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * Helpers to find the village a bus stop belongs to.
 * <p>
 * A village is detected through the {@code minecraft:village_*} structure
 * starts that contain the stop position. The structure start is used instead
 * of the POI based village manager because it is cheap to query, works even
 * when no villager is loaded and gives us a stable bounding box that can be
 * used to spawn villagers.
 */
public final class VillageUtil {
    /** Maximum distance (in blocks) between a stop and a village center. */
    public static final int SEARCH_RADIUS = 64;
    /** Maximum distance (in blocks) between a stop and a village center. */
    public static final int MAX_CENTER_DISTANCE = 96;

    private static final String[] VILLAGE_STRUCTURES = {
            "minecraft:village_plains",
            "minecraft:village_desert",
            "minecraft:village_savanna",
            "minecraft:village_snowy",
            "minecraft:village_taiga"
    };

    /**
     * Result of a village lookup.
     *
     * @param name     display name of the village (structure name)
     * @param center   center of the village bounding box
     * @param bounds   bounding box of the village
     */
    public record VillageInfo(String name, BlockPos center, BoundingBox bounds) {
    }

    /**
     * Looks for a village that contains {@code pos}. Returns an empty optional
     * when the position is not inside a village.
     */
    public static Optional<VillageInfo> findVillage(ServerLevel level, BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        for (String structureId : VILLAGE_STRUCTURES) {
            Optional<StructureStart> start = level.structureManager()
                    .getStartForStructure(SectionPos.of(chunkPos, level.getMinSection()), structureId, level);
            if (start.isEmpty()) {
                continue;
            }
            StructureStart structureStart = start.get();
            BoundingBox bounds = structureStart.getBoundingBox();
            if (!bounds.isInside(pos)) {
                continue;
            }
            Structure structure = structureStart.getStructure();
            Holder<Structure> holder = level.registryAccess()
                    .registryOrThrow(Registries.STRUCTURE)
                    .wrapAsHolder(structure);
            String name = holder.unwrapKey()
                    .map(key -> key.location().getPath())
                    .orElse(structureId);
            BlockPos center = new BlockPos(bounds.getCenter());
            return Optional.of(new VillageInfo(name, center, bounds));
        }
        return Optional.empty();
    }

    /**
     * Returns the display name of a village structure id, e.g.
     * {@code village_plains} becomes {@code Plains Village}.
     */
    public static Component displayName(String structureName) {
        String path = structureName.contains(":") ? structureName.substring(structureName.indexOf(':') + 1)
                : structureName;
        String key = "village.busmod." + path;
        return Component.translatable(key);
    }

    /**
     * Returns the number of villagers that live in the village around
     * {@code center}. Used to decide how many villagers may walk to a stop.
     */
    public static int countVillagers(ServerLevel level, BoundingBox bounds) {
        return level.getEntitiesOfClass(net.minecraft.world.entity.npc.Villager.class,
                new net.minecraft.world.phys.AABB(bounds.minX(), bounds.minY(), bounds.minZ(),
                        bounds.maxX() + 1, bounds.maxY() + 1, bounds.maxZ() + 1)).size();
    }

    /**
     * Finds a safe standing position near {@code target} inside the village
     * bounds, or {@code null} when none could be found.
     */
    public static BlockPos findSpawnPos(ServerLevel level, BlockPos target, BoundingBox bounds) {
        for (int attempt = 0; attempt < 16; attempt++) {
            int dx = level.random.nextInt(9) - 4;
            int dz = level.random.nextInt(9) - 4;
            BlockPos candidate = target.offset(dx, 0, dz);
            if (!bounds.isInside(candidate)) {
                continue;
            }
            BlockPos ground = findGround(level, candidate);
            if (ground != null) {
                return ground;
            }
        }
        return null;
    }

    private static BlockPos findGround(ServerLevel level, BlockPos pos) {
        for (int dy = 3; dy >= -3; dy--) {
            BlockPos feet = pos.offset(0, dy, 0);
            if (level.getBlockState(feet.below()).isSolid()
                    && level.getBlockState(feet).isAir()
                    && level.getBlockState(feet.above()).isAir()) {
                return feet;
            }
        }
        return null;
    }

    /** Returns {@code true} when the position is a village POI (bed, workstation...). */
    public static boolean isVillagePoi(ServerLevel level, BlockPos pos) {
        return level.getPoiManager()
                .getType(pos)
                .map(holder -> holder.is(PoiTypes.HOME) || holder.is(PoiTypes.MEETING))
                .orElse(false);
    }

    /** Returns the POI manager of the level, exposed for debugging commands. */
    public static PoiManager poiManager(ServerLevel level) {
        return level.getPoiManager();
    }

    private VillageUtil() {
    }
}
