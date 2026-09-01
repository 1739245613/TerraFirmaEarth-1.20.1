package com.newterraearth.tfe.debug;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.biome.BiomeSourceExtension;
import net.dries007.tfc.world.region.RegionPartition;

import com.newterraearth.tfe.world.NTEBiomeNoise;
import net.dries007.tfc.world.noise.Noise2D;

public final class NTERuntimeTrace
{
    private static final String MODE_TERRAIN_CUT = "terrain_cut";
    private static final String MODE_BLOCK_PROBE = "block_probe";
    private static final String MODE_REGION_SCAN = "region_scan";
    private static final String MODE_RIFT_PROFILE = "rift_profile";
    private static final boolean ENABLED = Boolean.getBoolean("tfe.debug.runtimeTrace");
    private static final String MODE = System.getProperty("tfe.debug.traceMode", MODE_TERRAIN_CUT).trim();
    private static final long EXPECTED_SEED = Long.getLong("tfe.debug.traceSeed", Long.MIN_VALUE);
    private static final int TARGET_X = Integer.getInteger("tfe.debug.traceX", 2695);
    private static final int TARGET_Y = Integer.getInteger("tfe.debug.traceY", 64);
    private static final int TARGET_Z = Integer.getInteger("tfe.debug.traceZ", 6738);
    private static final int RADIUS = Math.max(0, Integer.getInteger("tfe.debug.traceRadius", 8));
    private static final int SCAN_RADIUS = Math.max(128, Integer.getInteger("tfe.debug.traceScanRadius", 32768));
    private static final int SCAN_STEP = Math.max(4, Integer.getInteger("tfe.debug.traceScanStep", 128));
    private static final String AXIS = System.getProperty("tfe.debug.traceAxis", "x");
    private static int ticks;
    private static boolean requested;
    private static boolean printed;
    private static ServerLevel traceLevel;

    private NTERuntimeTrace() {}

    public static void init()
    {
        if (ENABLED)
        {
            MinecraftForge.EVENT_BUS.register(NTERuntimeTrace.class);
        }
    }

    public static boolean enabled()
    {
        return ENABLED;
    }

    public static boolean terrainCutEnabled()
    {
        return ENABLED && MODE_TERRAIN_CUT.equals(MODE);
    }

    public static boolean blockProbeEnabled()
    {
        return ENABLED && MODE_BLOCK_PROBE.equals(MODE);
    }

    public static boolean regionScanEnabled()
    {
        return ENABLED && MODE_REGION_SCAN.equals(MODE);
    }

    public static boolean riftProfileEnabled()
    {
        return ENABLED && MODE_RIFT_PROFILE.equals(MODE);
    }

    public static boolean isTargetColumn(int x, int z)
    {
        return terrainCutEnabled() && (traceZAxis()
            ? x == TARGET_X && z >= TARGET_Z - RADIUS && z <= TARGET_Z + RADIUS
            : z == TARGET_Z && x >= TARGET_X - RADIUS && x <= TARGET_X + RADIUS);
    }

    private static boolean traceZAxis()
    {
        return "z".equalsIgnoreCase(AXIS);
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event)
    {
        ticks = 0;
        requested = false;
        printed = false;
        traceLevel = null;

        final MinecraftServer server = event.getServer();
        final long actualSeed = server.getWorldData().worldGenOptions().seed();
        System.out.printf("[TFE][RuntimeTrace] started mode=%s seed=%d target=(%d,%d,%d) radius=%d axis=%s%n", MODE, actualSeed, TARGET_X, TARGET_Y, TARGET_Z, RADIUS, AXIS);
        if (EXPECTED_SEED != Long.MIN_VALUE && EXPECTED_SEED != actualSeed)
        {
            System.out.printf("[TFE][RuntimeTrace] expected seed %d but server seed is %d%n", EXPECTED_SEED, actualSeed);
        }
        final ServerLevel level = server.getLevel(Level.OVERWORLD);
        if (level == null)
        {
            System.out.println("[TFE][RuntimeTrace] missing overworld; stopping");
            server.halt(false);
            return;
        }

        if (regionScanEnabled())
        {
            printRegionScan(level);
            System.out.println("[TFE][RuntimeTrace] region scan finished; stopping");
            server.halt(false);
            return;
        }

        if (riftProfileEnabled())
        {
            printRiftProfile(level);
            System.out.println("[TFE][RuntimeTrace] rift profile finished; stopping");
            server.halt(false);
            return;
        }

        final ChunkPos center = new ChunkPos(TARGET_X >> 4, TARGET_Z >> 4);
        final int chunkRadius = Math.max(2, (RADIUS + 15) / 16 + 1);
        System.out.printf("[TFE][RuntimeTrace] loading chunk square center=(%d,%d) chunkRadius=%d%n", center.x, center.z, chunkRadius);
        for (int dz = -chunkRadius; dz <= chunkRadius; dz++)
        {
            for (int dx = -chunkRadius; dx <= chunkRadius; dx++)
            {
                final int chunkX = center.x + dx;
                final int chunkZ = center.z + dz;
                level.getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.START, new ChunkPos(chunkX, chunkZ), 1, net.minecraft.util.Unit.INSTANCE);
                level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);
            }
        }
        traceLevel = level;
        requested = true;
        if (blockProbeEnabled())
        {
            printBlockProbeSnapshot(level, "pre_tick");
        }
    }

    private static void printRegionScan(ServerLevel level)
    {
        final BiomeSource biomeSource = level.getChunkSource().getGenerator().getBiomeSource();
        if (!(biomeSource instanceof BiomeSourceExtension source))
        {
            System.out.printf("[TFE][RuntimeTrace][region_scan] unsupported biome source=%s%n", biomeSource.getClass().getName());
            return;
        }

        final Map<String, Integer> counts = new HashMap<>();
        final int minX = TARGET_X - SCAN_RADIUS;
        final int maxX = TARGET_X + SCAN_RADIUS;
        final int minZ = TARGET_Z - SCAN_RADIUS;
        final int maxZ = TARGET_Z + SCAN_RADIUS;
        int samples = 0;
        int riftHits = 0;
        int riftLakeHits = 0;
        int printedRiftValley = 0;
        int printedRiftLake = 0;
        long nearestRiftValleyDistance = Long.MAX_VALUE;
        int nearestRiftValleyX = 0;
        int nearestRiftValleyZ = 0;
        long nearestRiftLakeDistance = Long.MAX_VALUE;
        int nearestRiftLakeX = 0;
        int nearestRiftLakeZ = 0;

        System.out.printf("[TFE][RuntimeTrace][region_scan] range=(%d..%d,%d..%d) step=%d%n", minX, maxX, minZ, maxZ, SCAN_STEP);
        for (int z = minZ; z <= maxZ; z += SCAN_STEP)
        {
            for (int x = minX; x <= maxX; x += SCAN_STEP)
            {
                final BiomeExtension extension = source.getBiomeExtensionNoRiver(QuartPos.fromBlock(x), QuartPos.fromBlock(z));
                final String name = extension.key().location().toString();
                counts.merge(name, 1, Integer::sum);
                samples++;
                if (name.endsWith(":rift_valley") || name.endsWith(":rift_lake"))
                {
                    final boolean isRiftLake = name.endsWith(":rift_lake");
                    if (isRiftLake) riftLakeHits++;
                    else riftHits++;
                    final long distance = (long) x * x + (long) z * z;
                    if (isRiftLake && distance < nearestRiftLakeDistance)
                    {
                        nearestRiftLakeDistance = distance;
                        nearestRiftLakeX = x;
                        nearestRiftLakeZ = z;
                    }
                    else if (!isRiftLake && distance < nearestRiftValleyDistance)
                    {
                        nearestRiftValleyDistance = distance;
                        nearestRiftValleyX = x;
                        nearestRiftValleyZ = z;
                    }
                    final boolean printValley = !isRiftLake && printedRiftValley < 16;
                    final boolean printLake = isRiftLake && printedRiftLake < 16;
                    if (printValley || printLake)
                    {
                        final RegionPartition.Point point = source.getPartition(x, z);
                        System.out.printf(
                            "[TFE][RuntimeTrace][region_scan][rift] x=%d z=%d biome=%s partitionRivers=%d%n",
                            x, z, name, point.rivers().size());
                        if (isRiftLake) printedRiftLake++;
                        else printedRiftValley++;
                    }
                }
            }
        }
        System.out.printf("[TFE][RuntimeTrace][region_scan] samples=%d rift_valley=%d rift_lake=%d%n", samples, riftHits, riftLakeHits);
        if (nearestRiftValleyDistance != Long.MAX_VALUE)
        {
            System.out.printf("[TFE][RuntimeTrace][region_scan][nearest] biome=rift_valley x=%d z=%d distanceSq=%d%n",
                nearestRiftValleyX, nearestRiftValleyZ, nearestRiftValleyDistance);
        }
        if (nearestRiftLakeDistance != Long.MAX_VALUE)
        {
            System.out.printf("[TFE][RuntimeTrace][region_scan][nearest] biome=rift_lake x=%d z=%d distanceSq=%d%n",
                nearestRiftLakeX, nearestRiftLakeZ, nearestRiftLakeDistance);
        }
        counts.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .forEach(entry -> System.out.printf("[TFE][RuntimeTrace][region_scan][count] biome=%s count=%d%n", entry.getKey(), entry.getValue()));
    }

    private static void printRiftProfile(ServerLevel level)
    {
        final BiomeSource biomeSource = level.getChunkSource().getGenerator().getBiomeSource();
        if (!(biomeSource instanceof BiomeSourceExtension source))
        {
            System.out.printf("[TFE][RuntimeTrace][rift_profile] unsupported biome source=%s%n", biomeSource.getClass().getName());
            return;
        }

        final long seed = level.getServer().getWorldData().worldGenOptions().seed();
        final Noise2D lake = NTEBiomeNoise.riftValley(seed, -10, 25, true);
        final Noise2D valley = NTEBiomeNoise.riftValley(seed, 2, 25, false);
        final int minX = TARGET_X - RADIUS;
        final int maxX = TARGET_X + RADIUS;
        final int minZ = TARGET_Z - RADIUS;
        final int maxZ = TARGET_Z + RADIUS;
        int samples = 0;
        int finalLake = 0;
        int finalValley = 0;
        int finalLakeBelowSea = 0;
        double minLakeHeight = Double.POSITIVE_INFINITY;
        int minLakeX = 0;
        int minLakeZ = 0;
        String minLakeBiome = "unknown";
        double minFinalLakeHeight = Double.POSITIVE_INFINITY;
        int minFinalLakeX = 0;
        int minFinalLakeZ = 0;
        double minValleyHeight = Double.POSITIVE_INFINITY;
        int minValleyX = 0;
        int minValleyZ = 0;
        int lowLakeOutsideFinalLake = 0;
        System.out.printf("[TFE][RuntimeTrace][rift_profile] range=(%d..%d,%d..%d) step=%d seed=%d%n",
            minX, maxX, minZ, maxZ, SCAN_STEP, seed);
        for (int z = minZ; z <= maxZ; z += SCAN_STEP)
        {
            for (int x = minX; x <= maxX; x += SCAN_STEP)
            {
                final double lakeHeight = lake.noise(x, z);
                final double valleyHeight = valley.noise(x, z);
                final String biome = source.getBiomeExtensionNoRiver(QuartPos.fromBlock(x), QuartPos.fromBlock(z)).key().location().toString();
                samples++;
                if (biome.endsWith(":rift_lake"))
                {
                    finalLake++;
                    if (lakeHeight < 63d) finalLakeBelowSea++;
                    if (lakeHeight < minFinalLakeHeight)
                    {
                        minFinalLakeHeight = lakeHeight;
                        minFinalLakeX = x;
                        minFinalLakeZ = z;
                    }
                }
                else if (biome.endsWith(":rift_valley"))
                {
                    finalValley++;
                }
                if (lakeHeight < 63d && !biome.endsWith(":rift_lake"))
                {
                    lowLakeOutsideFinalLake++;
                }
                if (lakeHeight < minLakeHeight)
                {
                    minLakeHeight = lakeHeight;
                    minLakeX = x;
                    minLakeZ = z;
                    minLakeBiome = biome;
                }
                if (valleyHeight < minValleyHeight)
                {
                    minValleyHeight = valleyHeight;
                    minValleyX = x;
                    minValleyZ = z;
                }
            }
        }
        final double targetLakeHeight = lake.noise(TARGET_X, TARGET_Z);
        final double targetValleyHeight = valley.noise(TARGET_X, TARGET_Z);
        final String targetBiome = source.getBiomeExtensionNoRiver(QuartPos.fromBlock(TARGET_X), QuartPos.fromBlock(TARGET_Z)).key().location().toString();
        System.out.printf("[TFE][RuntimeTrace][rift_profile] samples=%d finalLake=%d finalValley=%d finalLakeBelowSea=%d%n",
            samples, finalLake, finalValley, finalLakeBelowSea);
        System.out.printf("[TFE][RuntimeTrace][rift_profile][min] lake=(x=%d,z=%d,h=%.3f,biome=%s) finalLake=(x=%d,z=%d,h=%.3f) valley=(x=%d,z=%d,h=%.3f)%n",
            minLakeX, minLakeZ, minLakeHeight, minLakeBiome, minFinalLakeX, minFinalLakeZ, minFinalLakeHeight,
            minValleyX, minValleyZ, minValleyHeight);
        System.out.printf("[TFE][RuntimeTrace][rift_profile][alignment] lowLakeOutsideFinalLake=%d%n", lowLakeOutsideFinalLake);
        System.out.printf("[TFE][RuntimeTrace][rift_profile][target] x=%d z=%d biome=%s lakeHeight=%.3f valleyHeight=%.3f%n",
            TARGET_X, TARGET_Z, targetBiome, targetLakeHeight, targetValleyHeight);
    }

    private static void printHeightmap(ServerLevel level)
    {
        if (traceZAxis())
        {
            for (int z = TARGET_Z - RADIUS; z <= TARGET_Z + RADIUS; z++)
            {
                final int worldSurface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, TARGET_X, z);
                final int oceanFloor = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, TARGET_X, z);
                System.out.printf("[TFE][RuntimeTrace][terrain_cut][heightmap] x=%d z=%d worldSurfaceWG=%d oceanFloorWG=%d%n", TARGET_X, z, worldSurface, oceanFloor);
            }
            return;
        }

        for (int x = TARGET_X - RADIUS; x <= TARGET_X + RADIUS; x++)
        {
            final int worldSurface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, TARGET_Z);
            final int oceanFloor = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, TARGET_Z);
            System.out.printf("[TFE][RuntimeTrace][terrain_cut][heightmap] x=%d z=%d worldSurfaceWG=%d oceanFloorWG=%d%n", x, TARGET_Z, worldSurface, oceanFloor);
        }
    }

    private static void printBlockProbeSnapshot(ServerLevel level, String phase)
    {
        System.out.printf("[TFE][RuntimeTrace][block_probe][snapshot] phase=%s%n", phase);
        printBlockProbe(level);
    }

    private static void printBlockProbe(ServerLevel level)
    {
        final int minY = Math.max(level.getMinBuildHeight(), TARGET_Y - Math.max(8, RADIUS));
        final int maxY = Math.min(level.getMaxBuildHeight() - 1, TARGET_Y + Math.max(32, RADIUS));
        final int worldSurface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, TARGET_X, TARGET_Z);
        final int oceanFloor = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, TARGET_X, TARGET_Z);
        final BlockPos target = new BlockPos(TARGET_X, TARGET_Y, TARGET_Z);
        System.out.printf(
            "[TFE][RuntimeTrace][block_probe][target] x=%d y=%d z=%d biome=%s block=%s fluid=%s worldSurfaceWG=%d oceanFloorWG=%d%n",
            TARGET_X,
            TARGET_Y,
            TARGET_Z,
            level.getBiome(target).unwrapKey().map(key -> key.location().toString()).orElse("unknown"),
            level.getBlockState(target).getBlock(),
            level.getFluidState(target).getType(),
            worldSurface,
            oceanFloor
        );
        for (int y = maxY; y >= minY; y--)
        {
            final BlockPos pos = new BlockPos(TARGET_X, y, TARGET_Z);
            System.out.printf(
                "[TFE][RuntimeTrace][block_probe][column] x=%d y=%d z=%d block=%s fluid=%s%n",
                TARGET_X,
                y,
                TARGET_Z,
                level.getBlockState(pos).getBlock(),
                level.getFluidState(pos).getType()
            );
        }
        for (int dz = -1; dz <= 1; dz++)
        {
            for (int dx = -1; dx <= 1; dx++)
            {
                final int x = TARGET_X + dx;
                final int z = TARGET_Z + dz;
                final BlockPos pos = new BlockPos(x, TARGET_Y, z);
                System.out.printf(
                    "[TFE][RuntimeTrace][block_probe][plane] x=%d y=%d z=%d biome=%s block=%s fluid=%s worldSurfaceWG=%d oceanFloorWG=%d%n",
                    x,
                    TARGET_Y,
                    z,
                    level.getBiome(pos).unwrapKey().map(key -> key.location().toString()).orElse("unknown"),
                    level.getBlockState(pos).getBlock(),
                    level.getFluidState(pos).getType(),
                    level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z),
                    level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z)
                );
            }
        }
        if (traceZAxis())
        {
            for (int z = TARGET_Z - RADIUS; z <= TARGET_Z + RADIUS; z++)
            {
                printBlockProbeRow(level, TARGET_X, TARGET_Y, z);
            }
        }
        else
        {
            for (int x = TARGET_X - RADIUS; x <= TARGET_X + RADIUS; x++)
            {
                printBlockProbeRow(level, x, TARGET_Y, TARGET_Z);
            }
        }
        printBlockProbeSurfaceRows(level);
    }

    private static void printBlockProbeRow(ServerLevel level, int x, int y, int z)
    {
        final BlockPos pos = new BlockPos(x, y, z);
        System.out.printf(
            "[TFE][RuntimeTrace][block_probe][row] x=%d y=%d z=%d biome=%s block=%s fluid=%s worldSurfaceWG=%d oceanFloorWG=%d%n",
            x,
            y,
            z,
            level.getBiome(pos).unwrapKey().map(key -> key.location().toString()).orElse("unknown"),
            level.getBlockState(pos).getBlock(),
            level.getFluidState(pos).getType(),
            level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z),
            level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z)
        );
    }

    private static void printBlockProbeSurfaceRows(ServerLevel level)
    {
        final int minY = Math.max(level.getMinBuildHeight(), TARGET_Y - Math.max(8, RADIUS));
        final int maxY = Math.min(level.getMaxBuildHeight() - 1, TARGET_Y + Math.max(32, RADIUS));
        for (int y = maxY; y >= minY; y--)
        {
            if (traceZAxis())
            {
                printBlockProbeSurfaceRow(level, TARGET_X, y, TARGET_Z - RADIUS, TARGET_Z + RADIUS, true);
            }
            else
            {
                printBlockProbeSurfaceRow(level, TARGET_Z, y, TARGET_X - RADIUS, TARGET_X + RADIUS, false);
            }
        }
    }

    private static void printBlockProbeSurfaceRow(ServerLevel level, int fixed, int y, int start, int end, boolean zAxis)
    {
        final StringBuilder row = new StringBuilder();
        int firstSolid = Integer.MIN_VALUE;
        int lastSolid = Integer.MIN_VALUE;
        for (int moving = start; moving <= end; moving++)
        {
            final int x = zAxis ? fixed : moving;
            final int z = zAxis ? moving : fixed;
            final BlockPos pos = new BlockPos(x, y, z);
            final boolean solid = !level.getBlockState(pos).isAir() && level.getFluidState(pos).isEmpty();
            row.append(solid ? '#' : '.');
            if (solid)
            {
                if (firstSolid == Integer.MIN_VALUE)
                {
                    firstSolid = moving;
                }
                lastSolid = moving;
            }
        }
        System.out.printf(
            "[TFE][RuntimeTrace][block_probe][surface_row] %s=%d y=%d range=%d..%d solidFirst=%s solidLast=%s pattern=%s%n",
            zAxis ? "x" : "z",
            fixed,
            y,
            start,
            end,
            firstSolid == Integer.MIN_VALUE ? "none" : Integer.toString(firstSolid),
            lastSolid == Integer.MIN_VALUE ? "none" : Integer.toString(lastSolid),
            row
        );
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || !requested)
        {
            return;
        }

        ticks++;
        if (!printed && ticks >= 20)
        {
            if (terrainCutEnabled() && traceLevel != null)
            {
                printHeightmap(traceLevel);
            }
            else if (blockProbeEnabled() && traceLevel != null)
            {
                printBlockProbeSnapshot(traceLevel, "post_tick_20");
            }
            printed = true;
        }
        if (ticks >= 40)
        {
            System.out.println("[TFE][RuntimeTrace] finished; stopping");
            event.getServer().halt(false);
        }
    }
}
