package com.newterraearth.tfe.client;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;

import net.dries007.tfc.common.items.ProspectResult;

import com.newterraearth.tfe.world.prospecting.NTEProspectingRules.MineralResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NTEProspectingHudTest
{
    private static final double EPSILON = 1.0E-9D;

    @BeforeAll
    static void bootstrapMinecraft()
    {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void twoBlocksOrLessLightsTheWholeRing()
    {
        assertEquals(1D, NTEProspectingHud.arcFraction(0D), EPSILON);
        assertEquals(1D, NTEProspectingHud.arcFraction(2D), EPSILON);
        assertTrue(NTEProspectingHud.arcFraction(2.01D) < 1D);
    }

    @Test
    void activeArcGrowsContinuouslyAsThePlayerApproaches()
    {
        final double far = NTEProspectingHud.arcFraction(33D);
        final double middle = NTEProspectingHud.arcFraction(16D);
        final double near = NTEProspectingHud.arcFraction(4D);

        assertEquals(0.08D, far, EPSILON);
        assertTrue(far < middle);
        assertTrue(middle < near);
        assertTrue(near < 1D);
    }

    @Test
    void twoMineralsStayOnOneLineAndTheThirdStartsTheSecondLine()
    {
        final List<MineralResult> two = List.of(
            new MineralResult(Blocks.IRON_ORE, ProspectResult.LARGE),
            new MineralResult(Blocks.GOLD_ORE, ProspectResult.SMALL)
        );
        assertEquals(1, NTEProspectingHud.buildResultLines(Blocks.STONE, ProspectResult.LARGE, two, 0).size());

        final List<MineralResult> three = List.of(
            two.get(0),
            two.get(1),
            new MineralResult(Blocks.COAL_ORE, ProspectResult.VERY_LARGE)
        );
        assertEquals(2, NTEProspectingHud.buildResultLines(Blocks.STONE, ProspectResult.LARGE, three, 2).size());
    }

    @Test
    void compassRingIsRelativeToThePlayersCurrentFacing()
    {
        assertEquals(0D, NTEProspectingHud.relativeDirection(0D, 0D, 0F, 0D, 10D), EPSILON);
        assertEquals(-Math.PI / 2D, NTEProspectingHud.relativeDirection(0D, 0D, 0F, 10D, 0D), EPSILON);
        assertEquals(Math.PI / 2D, NTEProspectingHud.relativeDirection(0D, 0D, 0F, -10D, 0D), EPSILON);
        assertEquals(0D, NTEProspectingHud.relativeDirection(0D, 0D, 90F, -10D, 0D), EPSILON);
        assertEquals(-Math.PI, NTEProspectingHud.relativeDirection(0D, 0D, 0F, 0D, -10D), EPSILON);
    }
}
