package com.newterraearth.tfe.debug;

import com.mojang.logging.LogUtils;
import net.minecraft.core.SectionPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

public final class NTEAquaticSpawnTraceHooks
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean installed = false;

    private NTEAquaticSpawnTraceHooks()
    {
    }

    public static void init()
    {
        if (installed || !AquaticSpawnTrace.isEnabled())
        {
            return;
        }
        installed = true;
        MinecraftForge.EVENT_BUS.addListener(NTEAquaticSpawnTraceHooks::onServerStarted);
    }

    private static void onServerStarted(ServerStartedEvent event)
    {
        final AquaticSpawnTrace.Config config = AquaticSpawnTrace.config();
        final MinecraftServer server = event.getServer();
        final ServerLevel level = server.overworld();
        final int centerChunkX = SectionPos.blockToSectionCoord(config.blockX());
        final int centerChunkZ = SectionPos.blockToSectionCoord(config.blockZ());
        final int chunkRadius = Math.max(0, (config.blockRadius() + 15) >> 4);

        LOGGER.info(
            "[TFE][AquaticTrace] loading chunks around target=({}, {}) chunkRadius={} blockRadius={}",
            config.blockX(),
            config.blockZ(),
            chunkRadius,
            config.blockRadius()
        );
        for (int dz = -chunkRadius; dz <= chunkRadius; dz++)
        {
            for (int dx = -chunkRadius; dx <= chunkRadius; dx++)
            {
                level.getChunk(centerChunkX + dx, centerChunkZ + dz);
            }
        }

        AquaticSpawnTrace.dump(LOGGER, level);
        if (config.stopAfterTrace())
        {
            LOGGER.info("[TFE][AquaticTrace] stopping dedicated server after trace dump");
            server.halt(false);
        }
    }
}
