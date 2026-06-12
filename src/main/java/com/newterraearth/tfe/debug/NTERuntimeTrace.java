package com.newterraearth.tfe.debug;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class NTERuntimeTrace
{
    private static final String MODE_TERRAIN_CUT = "terrain_cut";
    private static final String MODE_BLOCK_PROBE = "block_probe";
    private static final boolean ENABLED = Boolean.getBoolean("tfe.debug.runtimeTrace");
    private static final String MODE = System.getProperty("tfe.debug.traceMode", MODE_TERRAIN_CUT);
    private static final long EXPECTED_SEED = Long.getLong("tfe.debug.traceSeed", Long.MIN_VALUE);
    private static final int TARGET_X = Integer.getInteger("tfe.debug.traceX", 2695);
    private static final int TARGET_Y = Integer.getInteger("tfe.debug.traceY", 64);
    private static final int TARGET_Z = Integer.getInteger("tfe.debug.traceZ", 6738);
    private static final int RADIUS = Math.max(0, Integer.getInteger("tfe.debug.traceRadius", 8));
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
                printBlockProbe(traceLevel);
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
