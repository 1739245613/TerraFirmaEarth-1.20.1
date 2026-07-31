package com.newterraearth.tfe.world.feature;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NTEBoulderWorldgenDataTest
{
    @Test
    void allLargeAndSmallBoulderVariantsUseOneInFiftyRarity() throws IOException
    {
        for (String variant : new String[] {"raw", "cobble", "mossy"})
        {
            for (String suffix : new String[] {"_boulder.json", "_boulder_small_patch.json"})
            {
                final JsonObject root = resource("data/tfc/worldgen/placed_feature/" + variant + suffix);
                final JsonArray placement = root.getAsJsonArray("placement");
                assertEquals("minecraft:rarity_filter", placement.get(0).getAsJsonObject().get("type").getAsString());
                assertEquals(50, placement.get(0).getAsJsonObject().get("chance").getAsInt());
            }
        }
    }

    @Test
    void tfcBiomeTagsCoverRiverAndReportedVolcanicCanyon() throws IOException
    {
        final JsonArray rivers = resource("data/tfc/tags/worldgen/biome/is_river.json").getAsJsonArray("values");
        final JsonArray volcanic = resource("data/tfc/tags/worldgen/biome/is_volcanic.json").getAsJsonArray("values");

        assertTrue(rivers.asList().stream().anyMatch(value -> "tfc:river".equals(value.getAsString())));
        assertTrue(volcanic.asList().stream().anyMatch(value -> "tfc:canyons".equals(value.getAsString())));
    }

    private static JsonObject resource(String path) throws IOException
    {
        try (var reader = Files.newBufferedReader(Path.of("src/main/resources").resolve(path), StandardCharsets.UTF_8))
        {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
