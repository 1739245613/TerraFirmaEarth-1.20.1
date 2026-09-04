package com.newterraearth.tfe.world.crop;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NTECropTemperatureModelTest
{
    @Test
    void alfalfaIsFoodWithoutChangingItsExistingItemId() throws IOException
    {
        final String cropSource = Files.readString(
            Path.of("src/main/java/com/newterraearth/tfe/world/crop/NTECrop.java"),
            StandardCharsets.UTF_8
        );
        assertTrue(cropSource.contains("ALFALFA(FarmlandBlockEntity.NutrientType.NITROGEN, true, false, \"alfalfa\")"));

        final JsonObject food = resource("data/tfe/tfc/food_items/alfalfa.json");
        assertEquals("tfe:alfalfa", food.getAsJsonObject("ingredient").get("item").getAsString());
        assertEquals(4, food.get("hunger").getAsInt());
        assertEquals(0.5F, food.get("saturation").getAsFloat());
        assertEquals(0.2F, food.get("grain").getAsFloat());
        assertEquals(0.3F, food.get("vegetables").getAsFloat());

        final float decayModifier = food.get("decay_modifier").getAsFloat();
        assertEquals(0.275F, decayModifier);
        assertEquals(80F, 22F / decayModifier, 0.001F);

        assertTagContains("data/tfc/tags/items/foods.json", "tfe:alfalfa");
        assertTagContains("data/tfc/tags/items/foods/vegetables.json", "tfe:alfalfa");
    }

    @Test
    void climateStressAccumulatesAndRecovers()
    {
        assertEquals(1, NTECropTemperatureModel.updateStress(0, false));
        assertEquals(15, NTECropTemperatureModel.updateStress(14, false));
        assertEquals(20, NTECropTemperatureModel.updateStress(19, false));
        assertEquals(20, NTECropTemperatureModel.updateStress(20, false));
        assertEquals(20, NTECropTemperatureModel.updateStress(20, true));
        assertEquals(13, NTECropTemperatureModel.updateStress(14, true));
        assertEquals(0, NTECropTemperatureModel.updateStress(0, true));
        assertFalse(NTECropTemperatureModel.hasFatalStress(19));
        assertTrue(NTECropTemperatureModel.hasFatalStress(20));
        assertEquals(100, NTECropTemperatureModel.healthPercent(0));
        assertEquals(95, NTECropTemperatureModel.healthPercent(1));
        assertEquals(5, NTECropTemperatureModel.healthPercent(19));
        assertEquals(0, NTECropTemperatureModel.healthPercent(20));
    }

    @Test
    void deathRangeExtendsSuitableTemperatureByTenDegrees()
    {
        assertTrue(NTECropTemperatureModel.withinDeathRange(-4f, 22f, -14f));
        assertTrue(NTECropTemperatureModel.withinDeathRange(-4f, 22f, 32f));
        assertFalse(NTECropTemperatureModel.withinDeathRange(-4f, 22f, -14.01f));
        assertFalse(NTECropTemperatureModel.withinDeathRange(-4f, 22f, 32.01f));
    }

    private static void assertTagContains(String path, String item) throws IOException
    {
        final JsonArray values = resource(path).getAsJsonArray("values");
        assertTrue(values.asList().stream().anyMatch(value -> item.equals(value.getAsString())));
    }

    private static JsonObject resource(String path) throws IOException
    {
        try (var reader = Files.newBufferedReader(Path.of("src/main/resources").resolve(path), StandardCharsets.UTF_8))
        {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
