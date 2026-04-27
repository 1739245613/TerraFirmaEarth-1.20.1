package com.newterraearth.tfe.debug;

import com.mojang.logging.LogUtils;
import net.minecraft.core.SectionPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.region.Units;

import com.newterraearth.tfe.world.NTEBiomeNoise;
import com.newterraearth.tfe.world.region.NTERegionNoise;

public final class NTEVolcanoRuntimeTraceHooks
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean installed = false;

    private NTEVolcanoRuntimeTraceHooks()
    {
    }

    public static void init()
    {
        if (installed || !VolcanoRuntimeTrace.isEnabled())
        {
            return;
        }
        installed = true;
        MinecraftForge.EVENT_BUS.addListener(NTEVolcanoRuntimeTraceHooks::onServerStarted);
    }

    private static void onServerStarted(ServerStartedEvent event)
    {
        final VolcanoRuntimeTrace.Config config = VolcanoRuntimeTrace.config();
        final MinecraftServer server = event.getServer();
        final ServerLevel level = server.overworld();
        final int centerChunkX = SectionPos.blockToSectionCoord(config.blockX());
        final int centerChunkZ = SectionPos.blockToSectionCoord(config.blockZ());
        final int scanChunkRadius = config.effectiveChunkRadius();

        LOGGER.info(
            "[TFE][VolcanoTrace] forcing runtime generation seed={} target=({}, {}) chunkRadius={} effectiveChunkRadius={} blockRadius={} summaryTopN={}",
            level.getSeed(),
            config.blockX(),
            config.blockZ(),
            config.chunkRadius(),
            scanChunkRadius,
            config.blockRadius(),
            config.summaryTopN()
        );
        logTargetNoiseContext(level.getSeed(), config.blockX(), config.blockZ());

        for (int dz = -scanChunkRadius; dz <= scanChunkRadius; dz++)
        {
            for (int dx = -scanChunkRadius; dx <= scanChunkRadius; dx++)
            {
                level.getChunk(centerChunkX + dx, centerChunkZ + dz);
            }
        }

        for (int blockZ = config.blockZ() - config.blockRadius(); blockZ <= config.blockZ() + config.blockRadius(); blockZ++)
        {
            for (int blockX = config.blockX() - config.blockRadius(); blockX <= config.blockX() + config.blockRadius(); blockX++)
            {
                VolcanoRuntimeTrace.recordWorldSurface(level, blockX, blockZ);
            }
        }

        VolcanoRuntimeTrace.dumpWorldSurfaceScan(LOGGER, level);
        VolcanoRuntimeTrace.dump(LOGGER, server);
        if (config.stopAfterTrace())
        {
            LOGGER.info("[TFE][VolcanoTrace] stopping dedicated server after trace dump");
            server.halt(false);
        }
    }

    private static void logTargetNoiseContext(long seed, int blockX, int blockZ)
    {
        final int gridX = Units.blockToGrid(blockX);
        final int gridZ = Units.blockToGrid(blockZ);

        final Noise2D activeHotspots = NTERegionNoise.activeHotSpots(seed);
        final Noise2D dormantHotspots = NTERegionNoise.dormantHotSpots(seed);
        final Noise2D extinctHotspots = NTERegionNoise.extinctHotSpots(seed);
        final Noise2D ancientHotspots = NTERegionNoise.ancientHotSpots(seed);
        final Noise2D hotspotAge = NTERegionNoise.hotSpotAge(seed);
        final Noise2D hotspotIntensity = NTERegionNoise.hotSpotIntensity(seed);
        final Noise2D regionHotspotAge = hotspotAge.spread(128);
        final Noise2D regionHotspotIntensity = hotspotIntensity.spread(128);

        LOGGER.info(
            "[TFE][VolcanoTrace] target-noise block=({}, {}) grid=({}, {}) hotspotBlock={{active={}, dormant={}, extinct={}, ancient={}, intensity={}, age={}}} hotspotRegion={{intensity={}, age={}}}",
            blockX,
            blockZ,
            gridX,
            gridZ,
            fmt(activeHotspots.noise(blockX, blockZ)),
            fmt(dormantHotspots.noise(blockX, blockZ)),
            fmt(extinctHotspots.noise(blockX, blockZ)),
            fmt(ancientHotspots.noise(blockX, blockZ)),
            fmt(hotspotIntensity.noise(blockX, blockZ)),
            fmt(hotspotAge.noise(blockX, blockZ)),
            fmt(regionHotspotIntensity.noise(gridX + 0.5, gridZ + 0.5)),
            fmt(regionHotspotAge.noise(gridX + 0.5, gridZ + 0.5))
        );

        LOGGER.info(
            "[TFE][VolcanoTrace] target-noise terrainHeights block=({}, {}) shield={{active={}, dormant={}, extinct={}, ancient={}, sunken={}, glaciated={}, iceSheet={}}}",
            blockX,
            blockZ,
            fmt(NTEBiomeNoise.activeShieldVolcano(seed).noise(blockX, blockZ)),
            fmt(NTEBiomeNoise.dormantShieldVolcano(seed).noise(blockX, blockZ)),
            fmt(NTEBiomeNoise.extinctShieldVolcano(seed).noise(blockX, blockZ)),
            fmt(NTEBiomeNoise.ancientShieldVolcano(seed).noise(blockX, blockZ)),
            fmt(NTEBiomeNoise.sunkenShieldVolcano(seed).noise(blockX, blockZ)),
            fmt(NTEBiomeNoise.glaciatedShieldVolcano(seed).noise(blockX, blockZ)),
            fmt(NTEBiomeNoise.iceSheetShieldVolcanoTerrain(seed).noise(blockX, blockZ))
        );
    }

    private static String fmt(double value)
    {
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }
}
