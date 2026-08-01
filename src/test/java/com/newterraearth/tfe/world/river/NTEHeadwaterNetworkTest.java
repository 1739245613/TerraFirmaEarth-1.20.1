package com.newterraearth.tfe.world.river;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.dries007.tfc.world.river.Flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NTEHeadwaterNetworkTest
{
    private static final int SEA_LEVEL = 63;

    @Test
    void validatedHeadwaterKeepsTheTfcDrainAndWidensDownstream()
    {
        final NTEHeadwaterNetwork.TestStream stream = slopedValley(918273645L);

        assertTrue(stream.valid());
        final List<NTEHeadwaterNetwork.DiagnosticPoint> points = stream.points();
        assertTrue(points.size() > 8);
        assertEquals(0d, points.get(points.size() - 1).x(), 1.0e-9d);
        assertEquals(0d, points.get(points.size() - 1).z(), 1.0e-9d);
        assertEquals(SEA_LEVEL - 1d, points.get(points.size() - 1).waterY(), 1.0e-9d);

        for (int i = 1; i < points.size(); i++)
        {
            assertTrue(points.get(i - 1).waterY() >= points.get(i).waterY(), "water may not climb downstream");
            assertTrue(points.get(i - 1).radius() <= points.get(i).radius(), "a headwater may not narrow downstream");
        }
        assertTrue(points.get(0).waterY() > points.get(points.size() - 1).waterY());
        assertTrue(points.get(0).radius() < points.get(points.size() - 1).radius());
        assertTrue(points.get(0).x() >= 368d, "the spring must extend at least 48 blocks beyond the old source");
        assertTrue(points.get(0).radius() <= 0.7d, "the extended spring must begin about one block wide");
    }

    @Test
    void highlandHeadwaterFollowsTerrainAndLimitsCascadeSlope()
    {
        final NTEHeadwaterNetwork.TestStream stream = NTEHeadwaterNetwork.planTestStream(
            246813579L,
            SEA_LEVEL,
            320d,
            0d,
            0d,
            0d,
            16,
            (x, z) -> 65d + Math.max(0, x) * 0.18d + Math.abs(z) * 0.12d
        );

        assertTrue(stream.valid());
        final List<NTEHeadwaterNetwork.DiagnosticPoint> points = stream.points();
        assertTrue(points.get(0).waterY() > 120d, "a mountain creek may not be pinned near sea level");
        for (int i = 1; i < points.size(); i++)
        {
            final NTEHeadwaterNetwork.DiagnosticPoint previous = points.get(i - 1);
            final NTEHeadwaterNetwork.DiagnosticPoint current = points.get(i);
            final double segmentLength = Math.hypot(current.x() - previous.x(), current.z() - previous.z());
            assertTrue(previous.waterY() >= current.waterY(), "water may not climb downstream");
            assertTrue(previous.waterY() - current.waterY() <= segmentLength + 1.0e-6d,
                "a cascade may drop at most one block per horizontal block");
            assertTrue(previous.terrainY() - previous.waterY() <= 6.25d,
                "normal creek sections may not become deep artificial trenches");
        }
    }

    @Test
    void selectedStreamRemainsContinuousWhenAnotherChannelAlreadyLoweredAmbientTerrain()
    {
        final NTEHeadwaterNetwork.TestStream stream = slopedValley(11223344L);
        assertTrue(stream.valid());

        final List<NTEHeadwaterNetwork.DiagnosticPoint> points = stream.points();
        final NTEHeadwaterNetwork.DiagnosticPoint middle = points.get(points.size() / 2);
        final int x = (int) Math.floor(middle.x());
        final int z = (int) Math.floor(middle.z());

        assertNotNull(stream.sample(x, z, middle.terrainY()));
        assertNotNull(stream.sample(x, z, middle.waterY() + 0.25d),
            "a selected route may not disappear because another planned creek cut the shared column first");
        assertNull(stream.sample(x, z + 96, middle.terrainY()));
    }

    @Test
    void lowestRiskCandidateStillGeneratesWhenEveryRouteExceedsThePreferredIncisionRange()
    {
        final NTEHeadwaterNetwork.TestStream stream = NTEHeadwaterNetwork.planTestStream(
            66778899L,
            SEA_LEVEL,
            320d,
            0d,
            0d,
            0d,
            16,
            (x, z) -> 95d + Math.max(0, x) * 0.02d + Math.abs(z) * 0.04d
        );

        assertTrue(stream.valid(),
            "incision is a candidate-ranking risk and may not veto the selected route after planning");
        assertTrue(stream.points().stream().anyMatch(point ->
                NTEHeadwaterNetwork.visibleIncisionDepth(point.terrainY(), point.waterY()) > 8),
            "the fixture must exercise a route beyond the former hard outlet limit");
    }

    @Test
    void retainedTfcLeafCanReceiveANarrowSafeSourceExtension()
    {
        final NTEHeadwaterNetwork.TestStream feeder = NTEHeadwaterNetwork.planTestSourceExtension(
            31415926L,
            SEA_LEVEL,
            320d,
            0d,
            0d,
            0d,
            16,
            (x, z) -> 65d + Math.max(0, x - 320) * 0.04d + Math.abs(z) * 0.08d
        );

        assertTrue(feeder.valid());
        final List<NTEHeadwaterNetwork.DiagnosticPoint> points = feeder.points();
        assertTrue(points.get(0).x() >= 368d);
        assertTrue(points.get(0).radius() <= 0.7d);
        assertEquals(320d, points.get(points.size() - 1).x(), 1.0e-9d);
        assertEquals(0d, points.get(points.size() - 1).z(), 1.0e-9d);
        assertTrue(points.get(points.size() - 1).radius() <= 2d);
        assertEquals(SEA_LEVEL - 1d, points.get(points.size() - 1).waterY(), 1.0e-9d,
            "a retained-leaf feeder must reach the native receiver water level at contact");
    }

    @Test
    void uncontainableOutletInvalidatesTheReplacementSoTfcCanFallback()
    {
        final NTEHeadwaterNetwork.TestStream stream = NTEHeadwaterNetwork.planTestStream(
            42L,
            SEA_LEVEL,
            320d,
            0d,
            0d,
            0d,
            16,
            (x, z) -> SEA_LEVEL - 1d
        );

        assertFalse(stream.valid());
        assertTrue(stream.points().isEmpty());
    }

    @Test
    void routePlanningIsDeterministicForTheWorldSeed()
    {
        final NTEHeadwaterNetwork.TestStream first = slopedValley(55667788L);
        final NTEHeadwaterNetwork.TestStream second = slopedValley(55667788L);

        assertTrue(first.valid());
        assertEquals(first.points(), second.points());
    }

    @Test
    void drainagePlannerFollowsACurvedCatchmentInsteadOfTheOldStraightCorridor()
    {
        final NTEHeadwaterNetwork.TestStream stream = NTEHeadwaterNetwork.planTestStream(
            77889911L,
            SEA_LEVEL,
            320d,
            0d,
            0d,
            0d,
            16,
            (x, z) -> {
                final double bend = Math.max(0d, Math.min(1d, (x - 64d) / 192d));
                final double valleyZ = 48d * bend;
                return 68d + Math.max(0, x) * 0.035d + Math.abs(z - valleyZ) * 0.22d;
            }
        );

        assertTrue(stream.valid());
        final List<NTEHeadwaterNetwork.DiagnosticPoint> points = stream.points();
        final NTEHeadwaterNetwork.DiagnosticPoint spring = points.get(0);
        assertTrue(Math.abs(spring.z() - 48d) <= 12d,
            "the spring must be selected from the upstream catchment instead of the old center line");

        NTEHeadwaterNetwork.DiagnosticPoint bend = points.get(0);
        for (NTEHeadwaterNetwork.DiagnosticPoint point : points)
        {
            if (Math.abs(point.x() - 224d) < Math.abs(bend.x() - 224d))
            {
                bend = point;
            }
        }
        assertTrue(bend.z() > 24d, "the planned channel must stay in the curved valley through the middle reach");
    }

    @Test
    void completeReplacementRoutesAroundAnUnrelatedRetainedRiver()
    {
        final NTEHeadwaterNetwork.TestStream stream = NTEHeadwaterNetwork.planTestStream(
            1357911L,
            SEA_LEVEL,
            320d,
            0d,
            0d,
            0d,
            16,
            (x, z) -> 70d + Math.max(0, x) * 0.035d + Math.abs(z) * 0.05d,
            (owner, x, z, clearance) -> x > 136d && x < 184d && Math.abs(z) < 22d
        );

        assertTrue(stream.valid(), "a protected native channel must reroute the candidate, not be crossed");
        for (NTEHeadwaterNetwork.DiagnosticPoint point : stream.points())
        {
            assertFalse(point.x() > 136d && point.x() < 184d && Math.abs(point.z()) < 22d,
                "the accepted replacement may not enter the unrelated retained river corridor");
        }
    }

    @Test
    void feederSourceIsNotSelectedInsideAnUnrelatedRetainedRiver()
    {
        final NTEHeadwaterNetwork.TestStream feeder = NTEHeadwaterNetwork.planTestSourceExtension(
            2468024L,
            SEA_LEVEL,
            320d,
            0d,
            0d,
            0d,
            16,
            (x, z) -> 65d + Math.max(0, x - 320) * 0.04d + Math.abs(z) * 0.08d,
            (owner, x, z, clearance) -> x >= 368d && x <= 440d && Math.abs(z) < 20d
        );

        assertTrue(feeder.valid(), "the feeder must search the unobstructed side of its upstream catchment");
        final NTEHeadwaterNetwork.DiagnosticPoint spring = feeder.points().get(0);
        assertFalse(spring.x() >= 368d && spring.x() <= 440d && Math.abs(spring.z()) < 20d,
            "the spring may not be placed in the protected retained-river water core");
    }

    @Test
    void failedPrimaryDrainageCandidateFallsBackToAnotherValidValley()
    {
        final NTEHeadwaterNetwork.HeightSampler catchment = (x, z) -> {
            final double base = 70d + Math.max(0, x) * 0.04d;
            final double branch = Math.max(0d, Math.min(1d, (x - 64d) / 128d));
            final double secondValleyZ = 64d * branch;
            final double primaryValley = base + Math.abs(z) * 0.24d;
            final double secondaryValley = base + 0.8d + Math.abs(z - secondValleyZ) * 0.24d;
            return Math.min(primaryValley, secondaryValley);
        };
        final NTEHeadwaterNetwork.TestStream baseline = NTEHeadwaterNetwork.planTestStream(
            99112233L,
            SEA_LEVEL,
            320d,
            0d,
            0d,
            0d,
            16,
            catchment
        );
        assertTrue(baseline.valid());
        assertTrue(baseline.availableDrainageCandidates() > 1);

        NTEHeadwaterNetwork.DiagnosticPoint poison = null;
        final List<NTEHeadwaterNetwork.DiagnosticPoint> baselinePoints = baseline.points();
        for (int i = 1; i < baselinePoints.size() / 3; i++)
        {
            final NTEHeadwaterNetwork.DiagnosticPoint candidate = baselinePoints.get(i);
            final int x = (int) Math.floor(candidate.x());
            final int z = (int) Math.floor(candidate.z());
            if (Math.floorMod(x, NTEHeadwaterNetwork.SAMPLE_STEP) != 0
                || Math.floorMod(z, NTEHeadwaterNetwork.SAMPLE_STEP) != 0)
            {
                poison = candidate;
                break;
            }
        }
        assertNotNull(poison);
        final int poisonX = (int) Math.floor(poison.x());
        final int poisonZ = (int) Math.floor(poison.z());

        final NTEHeadwaterNetwork.TestStream stream = NTEHeadwaterNetwork.planTestStream(
            99112233L,
            SEA_LEVEL,
            320d,
            0d,
            0d,
            0d,
            16,
            (x, z) -> x == poisonX && z == poisonZ ? 61.5d : catchment.sample(x, z)
        );

        assertTrue(stream.valid(), "a failed first candidate must not immediately remove the complete creek");
        assertTrue(stream.attemptedDrainageCandidates() > 1,
            "fine validation must reject the poisoned first route and advance to another drainage source");
    }

    @Test
    void failedSharedDrainageTrunkIsPenalizedAndRoutedAround()
    {
        final NTEHeadwaterNetwork.HeightSampler valley = (x, z) ->
            70d + Math.max(0, x) * 0.04d + Math.abs(z) * 0.08d;
        final NTEHeadwaterNetwork.TestStream baseline = NTEHeadwaterNetwork.planTestStream(
            44556677L,
            SEA_LEVEL,
            320d,
            0d,
            0d,
            0d,
            16,
            valley
        );
        assertTrue(baseline.valid());
        assertTrue(baseline.availableDrainageCandidates() > 1);

        NTEHeadwaterNetwork.DiagnosticPoint poison = null;
        final List<NTEHeadwaterNetwork.DiagnosticPoint> baselinePoints = baseline.points();
        for (int i = baselinePoints.size() / 2; i < baselinePoints.size() * 3 / 4; i++)
        {
            final NTEHeadwaterNetwork.DiagnosticPoint candidate = baselinePoints.get(i);
            final int x = (int) Math.floor(candidate.x());
            final int z = (int) Math.floor(candidate.z());
            if (Math.floorMod(x, NTEHeadwaterNetwork.SAMPLE_STEP) != 0
                || Math.floorMod(z, NTEHeadwaterNetwork.SAMPLE_STEP) != 0)
            {
                poison = candidate;
                break;
            }
        }
        assertNotNull(poison);
        final int poisonX = (int) Math.floor(poison.x());
        final int poisonZ = (int) Math.floor(poison.z());

        final NTEHeadwaterNetwork.TestStream rerouted = NTEHeadwaterNetwork.planTestStream(
            44556677L,
            SEA_LEVEL,
            320d,
            0d,
            0d,
            0d,
            16,
            (x, z) -> x == poisonX && z == poisonZ ? 61.5d : valley.sample(x, z)
        );

        assertTrue(rerouted.valid(),
            "a failure on the shared drainage trunk must trigger a genuinely different corridor");
        assertTrue(rerouted.attemptedDrainageCandidates() > 1,
            "the rejected trunk must advance to a penalized replanning pass");
        for (NTEHeadwaterNetwork.DiagnosticPoint point : rerouted.points())
        {
            assertFalse((int) Math.floor(point.x()) == poisonX && (int) Math.floor(point.z()) == poisonZ,
                "the accepted route must not reuse the failed fine-terrain column");
        }
    }

    @Test
    void crossingHeadwatersBecomeOneFourBranchJunctionWithoutDeletingEitherSource()
    {
        final NTEHeadwaterNetwork.HeightSampler crossingValleys = (x, z) -> {
            final double horizontal = 75d + Math.max(0d, x + 160d) * 0.05d + Math.abs(z) * 0.10d;
            final double vertical = 75d + Math.max(0d, z + 160d) * 0.05d + Math.abs(x) * 0.10d;
            return Math.min(horizontal, vertical);
        };
        final NTEHeadwaterNetwork.TestStream horizontal = NTEHeadwaterNetwork.planTestStream(
            11224488L,
            SEA_LEVEL,
            160d,
            0d,
            -160d,
            0d,
            16,
            crossingValleys
        );
        final NTEHeadwaterNetwork.TestStream vertical = NTEHeadwaterNetwork.planTestStream(
            22448811L,
            SEA_LEVEL,
            0d,
            160d,
            0d,
            -160d,
            16,
            crossingValleys
        );
        assertTrue(horizontal.valid());
        assertTrue(vertical.valid());

        final NTEHeadwaterNetwork.DiagnosticPoint horizontalSource = horizontal.points().get(0);
        final NTEHeadwaterNetwork.DiagnosticPoint verticalSource = vertical.points().get(0);
        assertTrue(NTEHeadwaterNetwork.coordinateTestStreams(horizontal, vertical),
            "two independently planned routes must be promoted to one coordinated junction");

        final List<NTEHeadwaterNetwork.DiagnosticPoint> horizontalPoints = horizontal.points();
        final List<NTEHeadwaterNetwork.DiagnosticPoint> verticalPoints = vertical.points();
        assertEquals(horizontalSource.x(), horizontalPoints.get(0).x(), 1.0e-9d);
        assertEquals(horizontalSource.z(), horizontalPoints.get(0).z(), 1.0e-9d);
        assertEquals(verticalSource.x(), verticalPoints.get(0).x(), 1.0e-9d);
        assertEquals(verticalSource.z(), verticalPoints.get(0).z(), 1.0e-9d);
        assertEquals(-160d, horizontalPoints.get(horizontalPoints.size() - 1).x(), 1.0e-9d);
        assertEquals(0d, horizontalPoints.get(horizontalPoints.size() - 1).z(), 1.0e-9d);
        assertEquals(0d, verticalPoints.get(verticalPoints.size() - 1).x(), 1.0e-9d);
        assertEquals(-160d, verticalPoints.get(verticalPoints.size() - 1).z(), 1.0e-9d);

        NTEHeadwaterNetwork.DiagnosticPoint sharedHorizontal = null;
        NTEHeadwaterNetwork.DiagnosticPoint sharedVertical = null;
        for (NTEHeadwaterNetwork.DiagnosticPoint left : horizontalPoints)
        {
            for (NTEHeadwaterNetwork.DiagnosticPoint right : verticalPoints)
            {
                if (Math.hypot(left.x() - right.x(), left.z() - right.z()) <= 1.0e-9d)
                {
                    sharedHorizontal = left;
                    sharedVertical = right;
                    break;
                }
            }
            if (sharedHorizontal != null)
            {
                break;
            }
        }
        assertNotNull(sharedHorizontal, "both routes must contain the same explicit junction point");
        assertNotNull(sharedVertical);
        assertEquals(sharedHorizontal.waterY(), sharedVertical.waterY(), 1.0e-9d,
            "the shared node must have one coordinated water level");
    }

    @Test
    void sustainedHighLowOverlapBecomesOneSharedLongitudinalProfile()
    {
        final NTEHeadwaterNetwork.HeightSampler sharedValley = (x, z) -> 80d;
        final List<NTEHeadwaterNetwork.Vec> higherPath = new java.util.ArrayList<>();
        final List<NTEHeadwaterNetwork.Vec> lowerPath = new java.util.ArrayList<>();
        for (int x = 80; x >= -80; x -= 4)
        {
            higherPath.add(new NTEHeadwaterNetwork.Vec(x, 0d));
            lowerPath.add(new NTEHeadwaterNetwork.Vec(x, 1d));
        }
        final NTEHeadwaterNetwork.TestStream higher = NTEHeadwaterNetwork.testStreamFromRoute(
            higherPath, 72d, 66d, sharedValley
        );
        final NTEHeadwaterNetwork.TestStream lower = NTEHeadwaterNetwork.testStreamFromRoute(
            lowerPath, 71d, 65d, sharedValley
        );
        final double[] overlap = NTEHeadwaterNetwork.overlapTestStreams(higher, lower);
        assertNotNull(overlap, "a co-directed near-overlap must be represented as a shared corridor");
        assertTrue(overlap[1] - overlap[0] >= 12d);

        assertTrue(NTEHeadwaterNetwork.coordinateTestStreams(higher, lower));
        final int sampleX = 0;
        final NTEHeadwaterNetwork.Sample higherSample = NTEHeadwaterNetwork.sampleTestStream(higher, sampleX, 1);
        final NTEHeadwaterNetwork.Sample lowerSample = NTEHeadwaterNetwork.sampleTestStream(lower, sampleX, 1);
        assertNotNull(higherSample);
        assertNotNull(lowerSample);
        assertEquals(higherSample.waterSurfaceY(), lowerSample.waterSurfaceY(), 0.08d,
            "the shared trunk may not retain a one-block high/low double water surface");
        assertEquals(higherSample.flow(), lowerSample.flow(),
            "both owners of a shared trunk must bake water in the same downstream direction");
    }

    @Test
    void physicallyTouchingWetCoresCoordinateBeyondTheOldCenterlineCutoff()
    {
        final NTEHeadwaterNetwork.HeightSampler valley = (x, z) -> 82d;
        final NTEHeadwaterNetwork.TestStream higher = NTEHeadwaterNetwork.testStreamFromRoute(
            List.of(
                new NTEHeadwaterNetwork.Vec(80d, 0d),
                new NTEHeadwaterNetwork.Vec(0d, 0d),
                new NTEHeadwaterNetwork.Vec(-80d, 0d)
            ),
            76d,
            72d,
            valley
        );
        final NTEHeadwaterNetwork.TestStream lower = NTEHeadwaterNetwork.testStreamFromRoute(
            List.of(
                new NTEHeadwaterNetwork.Vec(80d, 6.1d),
                new NTEHeadwaterNetwork.Vec(0d, 6.1d),
                new NTEHeadwaterNetwork.Vec(-80d, 6.1d)
            ),
            74d,
            70d,
            valley
        );

        assertTrue(NTEHeadwaterNetwork.coordinateTestStreams(higher, lower),
            "water cores which already touch may not remain independent high/low channels");
        final List<NTEHeadwaterNetwork.DiagnosticPoint> higherPoints = higher.points();
        final List<NTEHeadwaterNetwork.DiagnosticPoint> lowerPoints = lower.points();
        assertTrue(higherPoints.stream().anyMatch(left -> lowerPoints.stream().anyMatch(right ->
                Math.hypot(left.x() - right.x(), left.z() - right.z()) <= 1.0e-9d
                    && Math.abs(left.waterY() - right.waterY()) <= 1.0e-9d)),
            "physical contact must insert one explicit shared topology node");
        assertWaterNeverClimbs(higherPoints);
        assertWaterNeverClimbs(lowerPoints);
    }

    @Test
    void curvedRouteOnlyRejectsColumnsPastItsActualFinalSegment()
    {
        final List<NTEHeadwaterNetwork.Vec> route = List.of(
            new NTEHeadwaterNetwork.Vec(-10d, 20d),
            new NTEHeadwaterNetwork.Vec(20d, 20d),
            new NTEHeadwaterNetwork.Vec(20d, 0d),
            new NTEHeadwaterNetwork.Vec(0d, 0d)
        );

        assertFalse(NTEHeadwaterNetwork.rejectsPastOutlet(route, -10, 20),
            "an upstream meander may lie ahead of the outlet tangent without extending past the route endpoint");
        assertTrue(NTEHeadwaterNetwork.rejectsPastOutlet(route, -2, 0),
            "a column beyond the clamped end of the final segment must still be rejected");
    }

    @Test
    void columnProfileCanOnlyCutAndNeverRaiseTerrain()
    {
        final NTERiverHydrology.ColumnProfile profile = new NTERiverHydrology.ColumnProfile(
            78d,
            76d,
            76.5d,
            0d,
            2.5d,
            0d,
            0d,
            0d,
            1d,
            NTERiverHydrology.SUPPLEMENTAL_WATER_CORE_RADIUS_SQ,
            true,
            true,
            true,
            0d,
            false,
            true,
            NTERiverHydrology.ChannelKind.STREAM,
            NTERiverHydrology.ChannelMode.SURFACE,
            Flow.NONE
        );

        assertEquals(60d, profile.fillCeiling(60d), 0d);
        assertEquals(0d, profile.bankRaise(), 0d);
        assertFalse(profile.subterranean());
    }

    @Test
    void singlePassWholeRouteSmoothingSoftensGridRightAnglesWithoutMovingEndpoints()
    {
        final List<NTEHeadwaterNetwork.Vec> points = NTEHeadwaterNetwork.smoothRoute(List.of(
            new NTEHeadwaterNetwork.Vec(0d, 0d),
            new NTEHeadwaterNetwork.Vec(8d, 0d),
            new NTEHeadwaterNetwork.Vec(8d, 8d)
        ));

        double maximumTurn = 0d;
        for (int i = 1; i < points.size() - 1; i++)
        {
            final NTEHeadwaterNetwork.Vec before = points.get(i - 1);
            final NTEHeadwaterNetwork.Vec at = points.get(i);
            final NTEHeadwaterNetwork.Vec after = points.get(i + 1);
            final double incoming = Math.atan2(at.z() - before.z(), at.x() - before.x());
            final double outgoing = Math.atan2(after.z() - at.z(), after.x() - at.x());
            maximumTurn = Math.max(maximumTurn, Math.abs(Math.atan2(
                Math.sin(outgoing - incoming),
                Math.cos(outgoing - incoming)
            )));
        }

        assertTrue(maximumTurn < Math.toRadians(50d), "one stable smoothing pass must soften an 8-block grid corner");
        assertFalse(points.contains(new NTEHeadwaterNetwork.Vec(8d, 0d)), "the sharp grid corner must be replaced by an arc");
        assertEquals(new NTEHeadwaterNetwork.Vec(0d, 0d), points.get(0));
        assertEquals(new NTEHeadwaterNetwork.Vec(8d, 8d), points.get(points.size() - 1));
    }

    @Test
    void onlyActualTurnsReceiveLocalDiagonalConnectorCells()
    {
        final List<NTEHeadwaterNetwork.Vec> roundedTurn = NTEHeadwaterNetwork.smoothRoute(List.of(
            new NTEHeadwaterNetwork.Vec(0d, 0d),
            new NTEHeadwaterNetwork.Vec(8d, 0d),
            new NTEHeadwaterNetwork.Vec(8d, 8d)
        ));
        final List<NTEHeadwaterNetwork.Vec> straightDiagonal = List.of(
            new NTEHeadwaterNetwork.Vec(0d, 0d),
            new NTEHeadwaterNetwork.Vec(8d, 8d)
        );

        assertTrue(NTEHeadwaterNetwork.turnConnectorCount(roundedTurn) > 0,
            "the short diagonal transition inside a real bend must not leave a one-column hole");
        assertTrue(NTEHeadwaterNetwork.isTurnConnector(roundedTurn, 7, 2),
            "the exit half of the rounded bend must open the inside corner instead of extending the incoming ledge");
        assertEquals(0, NTEHeadwaterNetwork.turnConnectorCount(straightDiagonal),
            "a diagonal creek run must not be expanded into a global four-connected strip");
    }

    @Test
    void everyRetainedTfcLeafWidensGraduallyFromTheNarrowSourceScale()
    {
        assertEquals(0.36d, NTEHeadwaterNetwork.sourceWidthScale(-10d), 1.0e-9d);
        assertEquals(0.36d, NTEHeadwaterNetwork.sourceWidthScale(0d), 1.0e-9d);
        assertTrue(NTEHeadwaterNetwork.sourceWidthScale(32d) > 0.36d);
        assertTrue(NTEHeadwaterNetwork.sourceWidthScale(32d) < 1d);
        assertEquals(1d, NTEHeadwaterNetwork.sourceWidthScale(64d), 1.0e-9d);
        assertEquals(1d, NTEHeadwaterNetwork.sourceWidthScale(96d), 1.0e-9d);
    }

    @Test
    void headwaterContinuationTurnsTheReceiverInsteadOfCreatingAYJunction()
    {
        final List<NTEHeadwaterNetwork.Vec> aligned = NTEHeadwaterNetwork.alignTestRoute(
            List.of(
                new NTEHeadwaterNetwork.Vec(-96d, 0d),
                new NTEHeadwaterNetwork.Vec(-80d, 0d),
                new NTEHeadwaterNetwork.Vec(-64d, 0d),
                new NTEHeadwaterNetwork.Vec(-48d, 0d),
                new NTEHeadwaterNetwork.Vec(-32d, 0d),
                new NTEHeadwaterNetwork.Vec(-16d, 0d),
                new NTEHeadwaterNetwork.Vec(0d, 0d)
            ),
            List.of(
                new NTEHeadwaterNetwork.Vec(0d, 0d),
                new NTEHeadwaterNetwork.Vec(0d, 96d)
            ),
            8d
        );

        assertTrue(aligned.size() > 10);
        assertEquals(new NTEHeadwaterNetwork.Vec(-96d, 0d), aligned.get(0));
        final NTEHeadwaterNetwork.Vec end = aligned.get(aligned.size() - 1);
        assertEquals(0d, end.x(), 1.0e-6d);
        assertEquals(0d, end.z(), 1.0e-6d,
            "the aligned creek must end at the shared topology node instead of following beside the receiver");

        int firstTurn = 1;
        while (firstTurn < aligned.size() && aligned.get(firstTurn).z() == 0d)
        {
            firstTurn++;
        }
        final NTEHeadwaterNetwork.Vec transitionStart = aligned.get(Math.max(1, firstTurn - 1));
        final NTEHeadwaterNetwork.Vec transitionBefore = aligned.get(Math.max(0, firstTurn - 2));
        final NTEHeadwaterNetwork.Vec beforeEnd = aligned.get(aligned.size() - 2);
        final double earlyDirection = Math.atan2(
            transitionStart.z() - transitionBefore.z(),
            transitionStart.x() - transitionBefore.x()
        );
        final double outletDirection = Math.atan2(end.z() - beforeEnd.z(), end.x() - beforeEnd.x());
        assertTrue(Math.cos(earlyDirection) > 0.35d, "the source side must still inherit the east-flowing creek");
        assertTrue(Math.sin(outletDirection) > 0.92d, "the downstream end must align with the south-running receiver");
        final double contactDot = NTEHeadwaterNetwork.firstVisibleContactFlowDot(
            aligned,
            List.of(
                new NTEHeadwaterNetwork.Vec(0d, 0d),
                new NTEHeadwaterNetwork.Vec(0d, 96d)
            ),
            8d
        );
        assertTrue(contactDot >= 0.90d,
            "the visible merge must be within about 26 degrees of the receiver flow; actual dot=" + contactDot);
    }

    @Test
    void obtuseFlowConfluenceUsesEnoughRunwayToMeetTheReceiverSmoothly()
    {
        final List<NTEHeadwaterNetwork.Vec> receiver = List.of(
            new NTEHeadwaterNetwork.Vec(0d, 0d),
            new NTEHeadwaterNetwork.Vec(-113.137d, 113.137d)
        );
        final List<NTEHeadwaterNetwork.Vec> aligned = NTEHeadwaterNetwork.alignTestRoute(
            List.of(
                new NTEHeadwaterNetwork.Vec(-176d, 0d),
                new NTEHeadwaterNetwork.Vec(-160d, 0d),
                new NTEHeadwaterNetwork.Vec(-144d, 0d),
                new NTEHeadwaterNetwork.Vec(-128d, 0d),
                new NTEHeadwaterNetwork.Vec(-112d, 0d),
                new NTEHeadwaterNetwork.Vec(-96d, 0d),
                new NTEHeadwaterNetwork.Vec(-80d, 0d),
                new NTEHeadwaterNetwork.Vec(-64d, 0d),
                new NTEHeadwaterNetwork.Vec(-48d, 0d),
                new NTEHeadwaterNetwork.Vec(-32d, 0d),
                new NTEHeadwaterNetwork.Vec(-16d, 0d),
                new NTEHeadwaterNetwork.Vec(0d, 0d)
            ),
            receiver,
            8d
        );

        final NTEHeadwaterNetwork.Vec end = aligned.get(aligned.size() - 1);
        assertEquals(0d, end.x(), 1.0e-6d);
        assertEquals(0d, end.z(), 1.0e-6d,
            "even an obtuse approach must join at the shared topology node");
        final double contactDot = NTEHeadwaterNetwork.firstVisibleContactFlowDot(aligned, receiver, 8d);
        assertTrue(contactDot >= 0.90d,
            "the creek must be nearly parallel when its water first visibly meets the receiver; actual dot=" + contactDot);
    }

    @Test
    void receiverAlignmentAnchorsAtTheSharedTopologyNodeInsteadOfANearbyBend()
    {
        final List<NTEHeadwaterNetwork.Vec> aligned = NTEHeadwaterNetwork.alignTestRoute(
            List.of(
                new NTEHeadwaterNetwork.Vec(-96d, 0d),
                new NTEHeadwaterNetwork.Vec(-48d, 0d),
                new NTEHeadwaterNetwork.Vec(0d, 0d)
            ),
            List.of(
                new NTEHeadwaterNetwork.Vec(0d, 0d),
                new NTEHeadwaterNetwork.Vec(0d, 96d),
                new NTEHeadwaterNetwork.Vec(-40d, 96d),
                new NTEHeadwaterNetwork.Vec(-40d, 0d)
            ),
            8d
        );

        final NTEHeadwaterNetwork.Vec end = aligned.get(aligned.size() - 1);
        assertEquals(0d, end.x(), 1.0e-6d);
        assertEquals(0d, end.z(), 1.0e-6d,
            "a nearby receiver return bend must not move the shared topology endpoint");
    }

    @Test
    void receiverWidthGuidesOnlyReceiverGeometryWithoutInflatingTheCreek()
    {
        final double receiverWidth = 8d;
        final double receiverGeometryRadius = NTEHeadwaterNetwork.matchedReceiverRadius(receiverWidth);

        assertEquals(
            receiverWidth * Math.sqrt(NTERiverHydrology.TFC_WATER_CORE_RADIUS_SQ),
            receiverGeometryRadius * Math.sqrt(NTERiverHydrology.SUPPLEMENTAL_WATER_CORE_RADIUS_SQ),
            1.0e-9d,
            "receiver geometry still measures the native physical water-core radius"
        );
    }

    @Test
    void alignedMouthFanCutsMonotonicallyTowardTheReceiver()
    {
        assertEquals(8d, NTEHeadwaterNetwork.alignedMouthCutLength(2.5d), 1.0e-9d,
            "the complete fan stays cut-only and cannot build a mouth platform");
        assertEquals(1.5d, NTEHeadwaterNetwork.alignedMouthWaterCutLength(2.5d), 1.0e-9d,
            "water must continue through the cut-only fan until the restored TFC core takes ownership");
    }

    @Test
    void alignedMouthUsesOneContinuousBankAndWaterTransition()
    {
        assertEquals(1d, NTEHeadwaterNetwork.mouthBankFillWeight(24d), 1.0e-9d);
        assertEquals(0.5d, NTEHeadwaterNetwork.mouthBankFillWeight(16d), 1.0e-9d);
        assertEquals(0d, NTEHeadwaterNetwork.mouthBankFillWeight(8d), 1.0e-9d);
        assertEquals(0d, NTEHeadwaterNetwork.mouthBankFillWeight(0d), 1.0e-9d);
        assertEquals(0d, NTEHeadwaterNetwork.mouthReceiverBlendWeight(24d), 1.0e-9d);
        assertEquals(0d, NTEHeadwaterNetwork.mouthReceiverBlendWeight(8d), 1.0e-9d);
        assertEquals(0.5d, NTEHeadwaterNetwork.mouthReceiverBlendWeight(4d), 1.0e-9d);
        assertEquals(1d, NTEHeadwaterNetwork.mouthReceiverBlendWeight(0d), 1.0e-9d);
        assertEquals(0d, NTEHeadwaterNetwork.mouthOuterBankReceiverBlendWeight(24d), 1.0e-9d);
        assertEquals(0.5d, NTEHeadwaterNetwork.mouthOuterBankReceiverBlendWeight(16d), 1.0e-9d);
        assertEquals(1d, NTEHeadwaterNetwork.mouthOuterBankReceiverBlendWeight(8d), 1.0e-9d,
            "only the dry outer bank completes its handoff before the cut-only fan begins");

        double previous = 1d;
        for (int distanceToOutlet = 23; distanceToOutlet >= 0; distanceToOutlet--)
        {
            final double current = NTEHeadwaterNetwork.mouthBankFillWeight(distanceToOutlet);
            assertTrue(current <= previous, "bank construction must fade monotonically toward cut-only ownership");
            previous = current;
        }

        assertEquals(0.4d, NTEHeadwaterNetwork.mouthWaterDrop(4d, 65.4d, 65d), 1.0e-9d,
            "a flat confluence may not lower supplemental water below the receiver surface");
        assertEquals(5.9d, NTEHeadwaterNetwork.mouthWaterDrop(4d, 67.9d, 62d), 1.0e-9d,
            "the final fan must finish water-level handoff instead of retaining a raised shelf");
        assertEquals(2.95d, NTEHeadwaterNetwork.mouthWaterDrop(16d, 67.9d, 62d), 1.0e-9d,
            "water descends smoothly while the dry banks transfer to the receiver");
        assertEquals(0d, NTEHeadwaterNetwork.mouthWaterDrop(0d, 65d, 65d), 1.0e-9d);
        assertEquals(0d, NTERiverHydrology.wideShapeWeight(3d), 1.0e-9d);
        assertEquals(0.5d, NTERiverHydrology.wideShapeWeight(3.5d), 1.0e-9d);
        assertEquals(1d, NTERiverHydrology.wideShapeWeight(4d), 1.0e-9d);
    }

    @Test
    void incisionValidationUsesVisibleBlockDepthInsteadOfFractionalNoise()
    {
        assertEquals(5, NTEHeadwaterNetwork.visibleIncisionDepth(92.60d, 86.25d),
            "a fractional 5.10-height difference still exposes only five vertical blocks");
        assertEquals(7, NTEHeadwaterNetwork.visibleIncisionDepth(91.43d, 83.95d),
            "a genuinely deep fallback trench must remain rejected");
    }

    @Test
    void mouthIncisionIsNotAttenuatedTwiceByTheChannelCrossSection()
    {
        assertEquals(1d, NTERiverHydrology.supplementalLocalDepth(2.5d, 0d, 0.8d), 1.0e-9d,
            "ordinary outer-bank depth stays unchanged when no mouth incision exists");
        assertEquals(1.75d, NTERiverHydrology.supplementalLocalDepth(2.5d, 0.75d, 0.8d), 1.0e-9d,
            "the already feathered fan incision must be applied once after the base cross-section");
        assertEquals(3.25d, NTERiverHydrology.supplementalLocalDepth(2.5d, 0.75d, 0d), 1.0e-9d);

    }

    @Test
    void mouthDistanceFieldSeparatesTheWetConnectorFromTheOuterBankHandoff()
    {
        assertEquals(0.2d, NTEHeadwaterNetwork.mouthGeometryNormalizedDistanceSq(0.2d, 4d, 24d), 1.0e-9d,
            "the ordinary creek cross-section remains authoritative before bank handoff starts");
        assertEquals(0.2d, NTEHeadwaterNetwork.mouthGeometryNormalizedDistanceSq(0.2d, 4d, 10d), 1.0e-9d,
            "a wet creek-center column may not be reclassified outside the receiver before the final fan");
        final double halfway = NTEHeadwaterNetwork.mouthGeometryNormalizedDistanceSq(0.2d, 4d, 4d);
        assertTrue(halfway > 0.2d && halfway < 4d);
        assertEquals(4d, NTEHeadwaterNetwork.mouthGeometryNormalizedDistanceSq(0.2d, 4d, 0d), 1.0e-9d,
            "a column extending along the old creek direction must be outside at the receiver");
        assertEquals(0.2d, NTEHeadwaterNetwork.mouthGeometryNormalizedDistanceSq(4d, 0.2d, 0d), 1.0e-9d,
            "the receiver-aligned side of a high-angle mouth must be opened instead");
        assertTrue(NTEHeadwaterNetwork.mouthGeometryNormalizedDistanceSq(0.924d, 1.011d, 4d) > 1d,
            "the first reported dry shoulder must leave the supplemental creek bank");
        assertTrue(NTEHeadwaterNetwork.mouthGeometryNormalizedDistanceSq(0.835d, 1.327d, 6d) > 1d,
            "the second reported dry shoulder must leave the supplemental creek bank");
    }

    @Test
    void cutOnlyMouthStillCarvesAndCarriesWaterWithoutTerrainFill()
    {
        final NTERiverHydrology.ColumnProfile mouth = new NTERiverHydrology.ColumnProfile(
            78d,
            76d,
            76.5d,
            0.508d,
            2.5d,
            0d,
            0.35d,
            3d,
            0d,
            NTERiverHydrology.SUPPLEMENTAL_WATER_CORE_RADIUS_SQ,
            false,
            true,
            false,
            0.5d,
            true,
            false,
            NTERiverHydrology.ChannelKind.STREAM,
            NTERiverHydrology.ChannelMode.SURFACE,
            Flow.EEE
        );

        assertTrue(mouth.inChannel());
        assertTrue(mouth.inWaterCore(),
            "the outer half of the carved receiver blend must carry generated flowing water");
        assertFalse(mouth.inSourceWaterCore(),
            "the generated flowing fringe remains water without becoming a static source column");
        assertTrue(mouth.descendingReceiverMouth(),
            "only a cut-only receiver blend with real descent needs a stable rock lip");
        assertTrue(NTERiverHydrology.shouldUseSupplemental(mouth, null));
        assertTrue(NTERiverHydrology.shouldUseSupplemental(mouth, riverAt(0.5d)),
            "the receiver-aligned fan must keep ownership long enough to blend its banks and bed");
        assertTrue(NTERiverHydrology.protectsBedAt(mouth, mouth.bedBlockY()));
        assertTrue(NTERiverHydrology.protectsBedAt(mouth, mouth.bedBlockY() - 4),
            "the density-stage roof must cover a shallow cave beneath the creek");
        assertFalse(NTERiverHydrology.protectsBedAt(mouth, mouth.bedBlockY() - 5));
        assertEquals(59, NTERiverHydrology.effectiveBedBlockY(mouth, 59.18d));
        assertTrue(NTERiverHydrology.protectsBedAt(mouth, 59, 59.18d));
        assertFalse(NTERiverHydrology.protectsBedAt(mouth, mouth.bedBlockY(), 59.18d),
            "bed protection must follow a deeper retained receiver instead of rebuilding the creek lip");
        assertEquals(59.18d, NTERiverHydrology.clampToRetainedReceiverBed(
            mouth,
            riverAt(0.5d),
            64.95d,
            59.18d
        ), 1.0e-9d);
        assertEquals(64.95d, NTERiverHydrology.clampToRetainedReceiverBed(
            mouth,
            null,
            64.95d,
            59.18d
        ), 1.0e-9d,
            "a supplemental-only column must not borrow an unrelated receiver-bed depth");
        assertEquals(78d, mouth.applyBankFillTransition(78d, 82d), 1.0e-9d,
            "cut-only ownership may not retain any creek-built bank above the original terrain");
        assertEquals(76d, mouth.applyBankFillTransition(78d, 76d), 1.0e-9d,
            "bank fading must never attenuate an actual terrain cut");
        assertEquals(77.65d, mouth.terrainCutCeiling(78d), 1.0e-9d,
            "cut-only terrain must realize the planned mouth incision despite river-shape bank noise");
        assertTrue(NTERiverHydrology.clearsWetMouthHeadroom(mouth, mouth.waterBlockY() + 1));
        assertTrue(NTERiverHydrology.clearsWetMouthHeadroom(mouth, mouth.waterBlockY() + 2));
        assertTrue(NTERiverHydrology.clearsWetMouthHeadroom(mouth, mouth.waterBlockY() + 3));
        assertFalse(NTERiverHydrology.clearsWetMouthHeadroom(mouth, mouth.waterBlockY() + 4));
    }

    @Test
    void minimumRiverLayerSurvivesWaterfallMouthShelfCleanup()
    {
        final NTERiverHydrology.ColumnProfile minimumLanding = profileAtWater(62d, true);
        final NTERiverHydrology.ColumnProfile raisedLanding = profileAtWater(67d, true);
        final NTERiverHydrology.ColumnProfile ordinaryMouth = profileAtWater(67d, false);

        assertEquals(63, NTERiverHydrology.retainedMouthWaterClearFromY(minimumLanding, 62),
            "the fixed y=62 river-water layer may not be cleared at an underground-river join");
        assertEquals(67, NTERiverHydrology.retainedMouthWaterClearFromY(raisedLanding, 62),
            "a raised waterfall shelf is still removed so the generated fall can own it");
        assertEquals(68, NTERiverHydrology.retainedMouthWaterClearFromY(ordinaryMouth, 62));
        assertTrue(NTERiverHydrology.usesDirectionalReceiverSurfaceWater(minimumLanding, 62, 62),
            "a waterfall becomes directional TFC river water when it reaches the fixed receiver layer");
        assertFalse(NTERiverHydrology.usesDirectionalReceiverSurfaceWater(raisedLanding, 67, 62),
            "raised waterfall steps remain vanilla flowing water until receiver contact");
        assertTrue(NTERiverHydrology.blocksCaveDecoration(minimumLanding, 63),
            "late cave spikes may not rebuild a hardened shelf inside the wet corridor");
        assertTrue(NTERiverHydrology.blocksCaveColumn(minimumLanding, 48),
            "a hardened cave column growing from below may not cross the wet corridor");
        assertFalse(NTERiverHydrology.blocksCaveColumn(minimumLanding, 66),
            "an unrelated cave column beginning above the protected corridor remains available");
        assertTrue(NTERiverHydrology.blocksErosionSupport(minimumLanding, 63),
            "late erosion may not rebuild a hardened support block over receiver water");
        assertFalse(NTERiverHydrology.blocksErosionSupport(minimumLanding, minimumLanding.bedBlockY()),
            "the actual creek bed remains available to erosion stability handling");
    }

    @Test
    void sourceWaterCoreRetractsWithoutShorteningTheGeneratedWaterfall()
    {
        assertEquals(1d, NTEHeadwaterNetwork.mouthSourceWaterInset(0.2d), 0d);
        assertEquals(2d, NTEHeadwaterNetwork.mouthSourceWaterInset(1d), 0d);
        assertFalse(NTEHeadwaterNetwork.mouthSourceWaterAllowed(4.5d, 3d, 1d),
            "the two compensated cells are generated as flowing water, not source water");
        assertTrue(NTEHeadwaterNetwork.mouthSourceWaterAllowed(5.1d, 3d, 1d));
        assertFalse(NTEHeadwaterNetwork.plannedSourceWaterAllowed(0.75d),
            "a planned downstream step must start as generated flowing water even before the mouth taper");
        assertTrue(NTEHeadwaterNetwork.plannedSourceWaterAllowed(0.25d));
        assertFalse(NTEHeadwaterNetwork.plannedSourceWaterAllowed(66.1d, 66.05d, 65.95d),
            "source water must retract two cells before a fractional profile crosses a block-water level");
        assertTrue(NTEHeadwaterNetwork.plannedSourceWaterAllowed(66.9d, 66.4d, 66.05d),
            "sub-block descent which stays on one water layer may remain static");
        assertFalse(NTEHeadwaterNetwork.naturalWaterfallLanding(66.9d, 66.1d),
            "a fractional fan descent inside the same route block is not a terrain waterfall");
        assertTrue(NTEHeadwaterNetwork.naturalWaterfallLanding(67d, 66.9d),
            "a real integer route drop still starts generation-time waterfall baking");

        // The full waterfall range remains governed by the separate 7-cell
        // generation-time spill bake; this helper only changes source shape.
        for (int step = 1; step <= 7; step++)
        {
            assertEquals(new NTERiverHydrology.CardinalStep(1, 0),
                NTERiverHydrology.cardinalFlowStep(Flow.EEE, step));
        }
    }

    @Test
    void diagonalWaterfallRasterUsesOneDownstreamCardinalStepAtATime()
    {
        final NTERiverHydrology.CardinalStep first = NTERiverHydrology.cardinalFlowStep(Flow.N_E, 1);
        final NTERiverHydrology.CardinalStep second = NTERiverHydrology.cardinalFlowStep(Flow.N_E, 2);

        assertEquals(1, Math.abs(first.x()) + Math.abs(first.z()));
        assertEquals(1, Math.abs(second.x()) + Math.abs(second.z()));
        assertTrue(Flow.N_E.getVector().x * first.x() + Flow.N_E.getVector().z * first.z() > 0d);
        assertTrue(Flow.N_E.getVector().x * second.x() + Flow.N_E.getVector().z * second.z() > 0d);
        assertFalse(first.equals(second), "a diagonal flow must alternate axes instead of branching into both at once");

        for (int step = 1; step <= 7; step++)
        {
            assertEquals(new NTERiverHydrology.CardinalStep(1, 0), NTERiverHydrology.cardinalFlowStep(Flow.EEE, step));
        }
    }

    @Test
    void supplementalChannelOnlyOverridesARetainedTfcRiverInsideItsActualBed()
    {
        final NTERiverHydrology.ColumnProfile channel = profileAt(0.5d);
        final NTERiverHydrology.ColumnProfile outerBank = profileAt(1.4d);

        assertFalse(NTERiverHydrology.shouldUseSupplemental(channel, riverAt(0.20d)),
            "the retained main river owns its physical water core");
        assertTrue(NTERiverHydrology.shouldUseSupplemental(channel, riverAt(0.50d)),
            "the feeder may approach through the retained river's outer influence");
        assertFalse(NTERiverHydrology.shouldUseSupplemental(outerBank, riverAt(0.50d)));
        assertTrue(NTERiverHydrology.shouldUseSupplemental(outerBank, null));
    }

    @Test
    void receiverBlendTransfersTheCrossSectionDistanceWithTheBedAndWater()
    {
        final net.dries007.tfc.world.river.RiverInfo receiver = riverAt(0.25d);
        final NTERiverHydrology.ColumnProfile creek = profileAt(0.75d);
        final NTERiverHydrology.ColumnProfile halfway = withReceiverBlend(creek, 0.5d);
        final NTERiverHydrology.ColumnProfile receiverOwned = withReceiverBlend(creek, 1d);

        assertEquals(0.75d, NTERiverNoise.radialDistanceSq(receiver, creek), 1.0e-9d);
        assertEquals(0.5d, NTERiverNoise.radialDistanceSq(receiver, halfway), 1.0e-9d);
        assertEquals(0.25d, NTERiverNoise.radialDistanceSq(receiver, receiverOwned), 1.0e-9d,
            "receiver-owned columns must not keep a raised creek-shaped outer bank");
    }

    @Test
    void carversKeepOnlyAShallowBroadRoofAroundTheCreek()
    {
        final int outerBankMinimum = NTERiverCarverProtection.minimumProtectedY(90, 84d, 0d);
        final int waterCoreMinimum = NTERiverCarverProtection.minimumProtectedY(90, 84d, 1d);

        assertEquals(79, outerBankMinimum);
        assertEquals(73, waterCoreMinimum);
        assertTrue(waterCoreMinimum < outerBankMinimum, "the roof should thicken smoothly toward the water core");
        assertTrue(waterCoreMinimum > 60, "deep caves must remain outside the protected upper terrain band");
    }

    private static NTEHeadwaterNetwork.TestStream slopedValley(long seed)
    {
        return NTEHeadwaterNetwork.planTestStream(
            seed,
            SEA_LEVEL,
            320d,
            0d,
            0d,
            0d,
            16,
            (x, z) -> 65d + Math.max(0, x) * 0.04d + Math.abs(z) * 0.08d
        );
    }

    private static void assertWaterNeverClimbs(List<NTEHeadwaterNetwork.DiagnosticPoint> points)
    {
        for (int index = 1; index < points.size(); index++)
        {
            assertTrue(points.get(index - 1).waterY() >= points.get(index).waterY() - 1.0e-9d,
                "coordinating a junction may not create an uphill outgoing branch");
        }
    }

    private static NTERiverHydrology.ColumnProfile profileAt(double normalizedDistanceSq)
    {
        return new NTERiverHydrology.ColumnProfile(
            78d,
            76d,
            76.5d,
            normalizedDistanceSq,
            2.5d,
            0d,
            0d,
            0d,
            1d,
            NTERiverHydrology.SUPPLEMENTAL_WATER_CORE_RADIUS_SQ,
            true,
            true,
            true,
            0d,
            false,
            true,
            NTERiverHydrology.ChannelKind.STREAM,
            NTERiverHydrology.ChannelMode.SURFACE,
            Flow.NONE
        );
    }

    private static NTERiverHydrology.ColumnProfile profileAtWater(double waterY, boolean waterfallLanding)
    {
        return new NTERiverHydrology.ColumnProfile(
            waterY,
            waterY - 2d,
            waterY - 2d,
            0d,
            3d,
            0d,
            1d,
            0d,
            0d,
            NTERiverHydrology.SUPPLEMENTAL_WATER_CORE_RADIUS_SQ,
            false,
            true,
            false,
            0.5d,
            waterfallLanding,
            false,
            NTERiverHydrology.ChannelKind.STREAM,
            NTERiverHydrology.ChannelMode.SURFACE,
            Flow.EEE
        );
    }

    private static net.dries007.tfc.world.river.RiverInfo riverAt(double normalizedDistanceSq)
    {
        return new net.dries007.tfc.world.river.RiverInfo(null, Flow.NONE, normalizedDistanceSq * 100d, 100d);
    }

    private static NTERiverHydrology.ColumnProfile withReceiverBlend(
        NTERiverHydrology.ColumnProfile profile,
        double receiverBlendWeight
    )
    {
        return new NTERiverHydrology.ColumnProfile(
            profile.waterSurfaceY(),
            profile.centerBedY(),
            profile.bedY(),
            profile.normalizedDistanceSq(),
            profile.channelRadius(),
            profile.bankRaise(),
            profile.terrainIncision(),
            profile.mouthWaterDrop(),
            profile.bankFillWeight(),
            profile.waterCoreRadiusSq(),
            profile.fillAllowed(),
            profile.waterAllowed(),
            profile.sourceWaterAllowed(),
            receiverBlendWeight,
            profile.waterfallLanding(),
            profile.headwater(),
            profile.kind(),
            profile.mode(),
            profile.flow()
        );
    }

}
