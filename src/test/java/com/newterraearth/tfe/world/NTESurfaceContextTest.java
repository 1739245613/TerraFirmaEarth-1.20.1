package com.newterraearth.tfe.world;

import org.junit.jupiter.api.Test;

import static net.dries007.tfc.world.TFCChunkGenerator.SEA_LEVEL_Y;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NTESurfaceContextTest
{
    private static final float EPSILON = 0.001f;

    @Test
    void baseGroundwaterFadesLinearlyOverTwentyFiveBlocks()
    {
        final float startingWater = 250f;

        assertEquals(250f, NTESurfaceContext.modifyBaseGroundwaterPoint(SEA_LEVEL_Y + 10, startingWater), EPSILON);
        assertEquals(240f, NTESurfaceContext.modifyBaseGroundwaterPoint(SEA_LEVEL_Y + 11, startingWater), EPSILON);
        assertEquals(10f, NTESurfaceContext.modifyBaseGroundwaterPoint(SEA_LEVEL_Y + 34, startingWater), EPSILON);
        assertEquals(0f, NTESurfaceContext.modifyBaseGroundwaterPoint(SEA_LEVEL_Y + 35, startingWater), EPSILON);
        assertEquals(0f, NTESurfaceContext.modifyBaseGroundwaterPoint(SEA_LEVEL_Y + 20, Float.NEGATIVE_INFINITY), EPSILON);
    }
}
