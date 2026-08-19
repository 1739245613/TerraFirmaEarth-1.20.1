package com.newterraearth.tfe.world;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NTEWorldTrackerRainMixinTest
{
    @Test
    void commonMixinConfigRegistersTheSeasonalWorldTrackerRedirect() throws IOException
    {
        final String mixinConfig = readProjectFile("src/main/resources/tfe.mixins.json");
        assertTrue(mixinConfig.contains("\"WorldTrackerMixin\""));
        assertTrue(mixinConfig.indexOf("\"WorldTrackerAccessor\"") < mixinConfig.indexOf("\"WorldTrackerMixin\""));
    }

    @Test
    void worldTrackerMixinTargetsOnlyTheLocationAwareRainOverload() throws IOException
    {
        final String source = readProjectFile("src/main/java/com/newterraearth/tfe/mixin/WorldTrackerMixin.java");
        assertTrue(source.contains("method = \"isRaining(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z\""));
        assertTrue(source.contains("target = \"Lnet/dries007/tfc/util/climate/Climate;getRainfall(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)F\""));
        assertTrue(source.contains("NTESeasonalHelpers.getInstantRainfall(level, pos)"));
    }

    private static String readProjectFile(String path) throws IOException
    {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }

}
