package com.newterraearth.tfe.client;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;

import net.dries007.tfc.common.items.ProspectResult;

import com.newterraearth.tfe.world.prospecting.NTEProspectingRules.MineralResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

        assertEquals(NTEProspectingHud.MIN_ARC_FRACTION, far, EPSILON);
        assertEquals(NTEProspectingHud.MIN_ARC_FRACTION, NTEProspectingHud.arcFraction(100D), EPSILON);
        assertTrue(far < middle);
        assertTrue(middle < near);
        assertTrue(near < 1D);

        double previous = NTEProspectingHud.arcFraction(NTEProspectingHud.MAX_NAVIGATION_DISTANCE);
        for (double distance = NTEProspectingHud.MAX_NAVIGATION_DISTANCE - 0.25D; distance > NTEProspectingHud.FULL_RING_DISTANCE; distance -= 0.25D)
        {
            final double current = NTEProspectingHud.arcFraction(distance);
            assertTrue(current > previous);
            previous = current;
        }
    }

    @Test
    void shortArcsConcentrateTheirVisualWeightAndTaperTowardBothEnds()
    {
        final double far = NTEProspectingHud.ringCenterThickness(NTEProspectingHud.MIN_ARC_FRACTION);
        final double middle = NTEProspectingHud.ringCenterThickness(0.5D);
        final double near = NTEProspectingHud.ringCenterThickness(1D);

        assertEquals(NTEProspectingHud.MAX_RING_CENTER_THICKNESS, far, EPSILON);
        assertEquals(NTEProspectingHud.MIN_RING_CENTER_THICKNESS, near, EPSILON);
        assertTrue(far > middle);
        assertTrue(middle > near);

        final double center = NTEProspectingHud.ringThickness(0.25D, 0D);
        final double shoulder = NTEProspectingHud.ringThickness(0.25D, 0.5D);
        final double end = NTEProspectingHud.ringThickness(0.25D, 1D);
        assertTrue(center > shoulder);
        assertTrue(shoulder > end);
        assertEquals(0.5D, end, EPSILON);
    }

    @Test
    void mineralColorsUseAllDetectedTypesIncludingHiddenOnes()
    {
        assertEquals(0, NTEProspectingHud.mineralColorTier(0, 0));
        assertEquals(1, NTEProspectingHud.mineralColorTier(1, 0));
        assertEquals(2, NTEProspectingHud.mineralColorTier(2, 0));
        assertEquals(3, NTEProspectingHud.mineralColorTier(1, 2));
        assertEquals(4, NTEProspectingHud.mineralColorTier(4, 0));
        assertEquals(4, NTEProspectingHud.mineralColorTier(1, Integer.MAX_VALUE));
    }

    @Test
    void identicalGuideDependsOnlyOnItsRenderedTargetAndSaturatedColorTier()
    {
        final BlockPos target = new BlockPos(12, 45, -7);
        final NTEProspectingHud.GuideSignature fourMinerals = new NTEProspectingHud.GuideSignature(
            target,
            NTEProspectingHud.mineralColorTier(4, 0)
        );
        final NTEProspectingHud.GuideSignature sixMinerals = new NTEProspectingHud.GuideSignature(
            target,
            NTEProspectingHud.mineralColorTier(1, 5)
        );

        assertEquals(fourMinerals, sixMinerals);
        assertFalse(fourMinerals.equals(new NTEProspectingHud.GuideSignature(target.offset(1, 0, 0), 4)));
        assertFalse(fourMinerals.equals(new NTEProspectingHud.GuideSignature(target, 3)));
    }

    @Test
    void guideTransitionUsesAClampedSmoothStep()
    {
        final long started = 1_000_000_000L;
        assertEquals(0D, NTEProspectingHud.guideTransitionProgress(started, started), EPSILON);
        assertEquals(0.5D, NTEProspectingHud.guideTransitionProgress(started + NTEProspectingHud.GUIDE_TRANSITION_NANOS / 2L, started), EPSILON);
        assertEquals(1D, NTEProspectingHud.guideTransitionProgress(started + NTEProspectingHud.GUIDE_TRANSITION_NANOS, started), EPSILON);
        assertEquals(1D, NTEProspectingHud.guideTransitionProgress(started, 0L), EPSILON);
    }

    @Test
    void guideColorsBlendPerChannelWithoutOvershoot()
    {
        assertEquals(0x123456, NTEProspectingHud.lerpRgb(0x123456, 0xABCDEF, -1D));
        assertEquals(0x5F81A3, NTEProspectingHud.lerpRgb(0x123456, 0xABCDEF, 0.5D));
        assertEquals(0xABCDEF, NTEProspectingHud.lerpRgb(0x123456, 0xABCDEF, 2D));
    }

    @Test
    void guideDirectionTransitionTakesTheShortestPathAcrossTheWrapBoundary()
    {
        final double from = Math.toRadians(170D);
        final double to = Math.toRadians(-170D);

        assertEquals(Math.PI, NTEProspectingHud.lerpRadians(from, to, 0.5D), EPSILON);
        assertEquals(from, NTEProspectingHud.lerpRadians(from, to, -1D), EPSILON);
        assertEquals(Math.toRadians(190D), NTEProspectingHud.lerpRadians(from, to, 2D), EPSILON);
    }

    @Test
    void verticalMarkerShapeOnlyChangesOutsideTheNeutralBand()
    {
        assertEquals(NTEProspectingHud.MarkerShape.DOWN, NTEProspectingHud.markerShape(-1.01D));
        assertEquals(NTEProspectingHud.MarkerShape.DIAMOND, NTEProspectingHud.markerShape(-1D));
        assertEquals(NTEProspectingHud.MarkerShape.DIAMOND, NTEProspectingHud.markerShape(1D));
        assertEquals(NTEProspectingHud.MarkerShape.UP, NTEProspectingHud.markerShape(1.01D));
    }

    @Test
    void markerAnimationCycleChangesFromOneToThreeSecondsWithDistance()
    {
        assertEquals(NTEProspectingHud.MIN_MARKER_CYCLE_NANOS, NTEProspectingHud.markerCycleNanos(0D));
        assertEquals(NTEProspectingHud.MIN_MARKER_CYCLE_NANOS, NTEProspectingHud.markerCycleNanos(2D));
        assertEquals(2_000_000_000L, NTEProspectingHud.markerCycleNanos(17.5D));
        assertEquals(NTEProspectingHud.MAX_MARKER_CYCLE_NANOS, NTEProspectingHud.markerCycleNanos(33D));
        assertEquals(NTEProspectingHud.MAX_MARKER_CYCLE_NANOS, NTEProspectingHud.markerCycleNanos(100D));
        assertTrue(NTEProspectingHud.DIRECTION_DISPLAY_NANOS >= NTEProspectingHud.MAX_MARKER_CYCLE_NANOS);
        assertEquals(3_000_000_000L, NTEProspectingHud.RESULT_TEXT_DISPLAY_NANOS);
    }

    @Test
    void enclosedRingGapsAreFilledWithoutGrowingOpenEdges()
    {
        final int side = 5;
        final int[] enclosed = new int[side * side];
        final int[] resolved = new int[side * side];
        for (int y = 1; y <= 3; y++)
        {
            for (int x = 1; x <= 3; x++)
            {
                enclosed[y * side + x] = 180;
            }
        }
        enclosed[2 * side + 2] = 0;
        NTEProspectingHud.fillEnclosedRingGaps(enclosed, resolved, side);
        assertEquals(180, resolved[2 * side + 2]);

        final int[] openEdge = new int[side * side];
        openEdge[1 * side + 2] = 180;
        openEdge[2 * side + 1] = 180;
        openEdge[2 * side + 3] = 180;
        openEdge[3 * side + 2] = 180;
        NTEProspectingHud.fillEnclosedRingGaps(openEdge, resolved, side);
        assertEquals(0, resolved[2 * side + 2]);
    }

    @Test
    void markerAnimationSpendsHalfTheCycleLightingAndHalfClearing()
    {
        assertEquals(new NTEProspectingHud.MarkerAnimation(0D, true), NTEProspectingHud.markerAnimation(0D));
        assertEquals(new NTEProspectingHud.MarkerAnimation(0.5D, true), NTEProspectingHud.markerAnimation(0.25D));
        assertEquals(new NTEProspectingHud.MarkerAnimation(1D, true), NTEProspectingHud.markerAnimation(0.5D));
        assertEquals(new NTEProspectingHud.MarkerAnimation(0.5D, false), NTEProspectingHud.markerAnimation(0.75D));
        assertEquals(new NTEProspectingHud.MarkerAnimation(0D, true), NTEProspectingHud.markerAnimation(1D));
    }

    @Test
    void markerBorderActivationFollowsEachMarkersVisualDirection()
    {
        assertEquals(0D, NTEProspectingHud.markerBorderActivation(NTEProspectingHud.MarkerShape.DOWN, -10), EPSILON);
        assertEquals(1D, NTEProspectingHud.markerBorderActivation(NTEProspectingHud.MarkerShape.DOWN, 13), EPSILON);

        assertEquals(0D, NTEProspectingHud.markerBorderActivation(NTEProspectingHud.MarkerShape.UP, 10), EPSILON);
        assertEquals(1D, NTEProspectingHud.markerBorderActivation(NTEProspectingHud.MarkerShape.UP, -13), EPSILON);

        assertEquals(0D, NTEProspectingHud.markerBorderActivation(NTEProspectingHud.MarkerShape.DIAMOND, -7), EPSILON);
        assertEquals(0D, NTEProspectingHud.markerBorderActivation(NTEProspectingHud.MarkerShape.DIAMOND, 7), EPSILON);
        assertEquals(1D, NTEProspectingHud.markerBorderActivation(NTEProspectingHud.MarkerShape.DIAMOND, 0), EPSILON);
        assertEquals(
            NTEProspectingHud.markerBorderActivation(NTEProspectingHud.MarkerShape.DIAMOND, -4),
            NTEProspectingHud.markerBorderActivation(NTEProspectingHud.MarkerShape.DIAMOND, 4),
            EPSILON
        );
    }

    @Test
    void markerBorderLightsAndClearsInTheSameVisualOrder()
    {
        final NTEProspectingHud.MarkerAnimation halfLit = new NTEProspectingHud.MarkerAnimation(0.5D, true);
        final NTEProspectingHud.MarkerAnimation halfCleared = new NTEProspectingHud.MarkerAnimation(0.5D, false);

        assertTrue(NTEProspectingHud.markerBorderActive(NTEProspectingHud.MarkerShape.DOWN, -10, halfLit));
        assertFalse(NTEProspectingHud.markerBorderActive(NTEProspectingHud.MarkerShape.DOWN, 13, halfLit));
        assertFalse(NTEProspectingHud.markerBorderActive(NTEProspectingHud.MarkerShape.DOWN, -10, halfCleared));
        assertTrue(NTEProspectingHud.markerBorderActive(NTEProspectingHud.MarkerShape.DOWN, 13, halfCleared));

        assertTrue(NTEProspectingHud.markerBorderActive(NTEProspectingHud.MarkerShape.UP, 10, halfLit));
        assertFalse(NTEProspectingHud.markerBorderActive(NTEProspectingHud.MarkerShape.UP, -13, halfLit));
        assertFalse(NTEProspectingHud.markerBorderActive(NTEProspectingHud.MarkerShape.UP, 10, halfCleared));
        assertTrue(NTEProspectingHud.markerBorderActive(NTEProspectingHud.MarkerShape.UP, -13, halfCleared));

        assertTrue(NTEProspectingHud.markerBorderActive(NTEProspectingHud.MarkerShape.DIAMOND, -7, halfLit));
        assertFalse(NTEProspectingHud.markerBorderActive(NTEProspectingHud.MarkerShape.DIAMOND, 0, halfLit));
        assertFalse(NTEProspectingHud.markerBorderActive(NTEProspectingHud.MarkerShape.DIAMOND, -7, halfCleared));
        assertTrue(NTEProspectingHud.markerBorderActive(NTEProspectingHud.MarkerShape.DIAMOND, 0, halfCleared));
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
