package com.newterraearth.tfe.world.feature;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Test
    void roseUsesTheTfc429RarityClimateAndSiteFilters() throws IOException
    {
        final JsonArray patchPlacement = placedFeature("plant/rose_patch.json").getAsJsonArray("placement");
        assertEquals("minecraft:rarity_filter", patchPlacement.get(0).getAsJsonObject().get("type").getAsString());
        assertEquals(10, patchPlacement.get(0).getAsJsonObject().get("chance").getAsInt());

        final JsonObject climate = patchPlacement.get(3).getAsJsonObject();
        assertEquals(-0.4F, climate.get("min_temperature").getAsFloat());
        assertEquals(1, climate.get("min_forest").getAsInt());
        assertEquals(3, climate.get("max_forest").getAsInt());

        final JsonArray plantPlacement = placedFeature("plant/rose.json").getAsJsonArray("placement");
        assertEquals("tfc:air_or_empty_fluid", plantPlacement.get(1).getAsJsonObject().getAsJsonObject("predicate").get("type").getAsString());
        assertEquals("tfc:no_solid_neighbors", plantPlacement.get(3).getAsJsonObject().get("type").getAsString());
        assertEquals("tfc:intertidal", plantPlacement.get(4).getAsJsonObject().get("type").getAsString());
    }

    @Test
    void bambooUsesTheTfc429NoiseDensityAndNonChunkAlignedOffset() throws IOException
    {
        assertBambooPlacement("bamboo.json", 110);
        assertBambooPlacement("bamboo_golden.json", 110);
        assertBambooPlacement("rare_bamboo.json", 80);
        assertBambooPlacement("rare_bamboo_golden.json", 80);
    }

    @Test
    void portedReplaceablePlantsExtendTheTfc120MergeTag() throws IOException
    {
        final Set<String> expected = Set.of(
            "tfc:plant/anemone_green", "tfc:plant/anemone_large_orange", "tfc:plant/anemone_large_purple",
            "tfc:plant/anemone_purple", "tfc:plant/barnacles", "tfc:plant/mussels", "tfc:plant/starfish",
            "tfe:plant/azalea", "tfe:plant/bear_grass", "tfe:plant/bird_nest_fern", "tfe:plant/buttercup",
            "tfe:plant/cornflower", "tfe:plant/dry_grass", "tfe:plant/edelweiss",
            "tfe:plant/elegant_sunburst_lichen", "tfe:plant/fan_palm", "tfe:plant/kinnikinnick",
            "tfe:plant/moss_campion", "tfe:plant/mountain_hullwort", "tfe:plant/palash", "tfe:plant/penwortel",
            "tfe:plant/prickly_pear", "tfe:plant/prickly_pear_purple", "tfe:plant/purple_water_lily",
            "tfe:plant/qantu", "tfe:plant/ramirezella", "tfe:plant/ramunda", "tfe:plant/red_oat_grass",
            "tfe:plant/shawiash", "tfe:plant/silver_bromeliad", "tfe:plant/sunflower",
            "tfe:plant/tank_bromeliad", "tfe:plant/white_water_lily", "tfe:plant/yellow_saxifrage",
            "tfe:plant/yellow_water_lily"
        );
        final JsonObject tag = resource("data/tfc/tags/blocks/replaceable_plants.json");
        final JsonArray values = tag.getAsJsonArray("values");
        final Set<String> actual = values.asList().stream()
            .map(value -> value.getAsString())
            .collect(Collectors.toSet());

        assertEquals(expected.size(), values.size());
        assertEquals(expected, actual);
    }

    private static void assertBambooPlacement(String name, int expectedNoiseRatio) throws IOException
    {
        final JsonArray placement = placedFeature(name).getAsJsonArray("placement");
        final JsonObject noise = findPlacement(placement, "minecraft:noise_based_count");
        assertEquals(expectedNoiseRatio, noise.get("noise_to_count_ratio").getAsInt());

        final JsonObject offset = findPlacement(placement, "tfe:horizontal_clamped_normal_offset");
        assertEquals(-8, offset.get("min_inclusive").getAsInt());
        assertEquals(15, offset.get("max_inclusive").getAsInt());
        assertEquals(3, offset.get("mean").getAsInt());
        assertEquals(4, offset.get("deviation").getAsInt());
    }

    private static JsonObject findPlacement(JsonArray placement, String type)
    {
        return placement.asList().stream()
            .map(element -> element.getAsJsonObject())
            .filter(element -> type.equals(element.get("type").getAsString()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing placement modifier " + type));
    }

    private static JsonObject placedFeature(String path) throws IOException
    {
        return resource("data/tfc/worldgen/placed_feature/" + path);
    }

    private static JsonObject resource(String path) throws IOException
    {
        try (var reader = Files.newBufferedReader(Path.of("src/main/resources").resolve(path), StandardCharsets.UTF_8))
        {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
