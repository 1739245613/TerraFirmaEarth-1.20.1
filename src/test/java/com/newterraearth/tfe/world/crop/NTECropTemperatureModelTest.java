package com.newterraearth.tfe.world.crop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NTECropTemperatureModelTest
{
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
}
