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
    void streamSamplingRejectsLowAmbientColumnsInsteadOfBuildingAWaterWall()
    {
        final NTEHeadwaterNetwork.TestStream stream = slopedValley(11223344L);
        assertTrue(stream.valid());

        final List<NTEHeadwaterNetwork.DiagnosticPoint> points = stream.points();
        final NTEHeadwaterNetwork.DiagnosticPoint middle = points.get(points.size() / 2);
        final int x = (int) Math.floor(middle.x());
        final int z = (int) Math.floor(middle.z());

        assertNotNull(stream.sample(x, z, middle.terrainY()));
        assertNull(stream.sample(x, z, middle.waterY() + 0.25d));
        assertNull(stream.sample(x, z + 96, middle.terrainY()));
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
            (x, z) -> 65d + Math.max(0, x) * 0.04d + Math.abs(z) * 0.08d
        );

        assertTrue(feeder.valid());
        final List<NTEHeadwaterNetwork.DiagnosticPoint> points = feeder.points();
        assertTrue(points.get(0).x() >= 368d);
        assertTrue(points.get(0).radius() <= 0.7d);
        assertEquals(320d, points.get(points.size() - 1).x(), 1.0e-9d);
        assertEquals(0d, points.get(points.size() - 1).z(), 1.0e-9d);
        assertTrue(points.get(points.size() - 1).radius() <= 2d);
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
                new NTEHeadwaterNetwork.Vec(-48d, 0d),
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
        assertEquals(48d, end.z(), 1.0e-6d);

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
        assertEquals(0d, NTEHeadwaterNetwork.mouthFanIncision(9d, 0d), 1.0e-9d);
        assertEquals(0d, NTEHeadwaterNetwork.mouthFanIncision(8d, 0d), 1.0e-9d);
        assertEquals(1d, NTEHeadwaterNetwork.mouthFanIncision(4d, 0d), 1.0e-9d);
        assertEquals(2d, NTEHeadwaterNetwork.mouthFanIncision(0d, 0d), 1.0e-9d);
        assertEquals(0d, NTEHeadwaterNetwork.mouthFanIncision(4d, 1d), 1.0e-9d);
        assertEquals(1d, NTEHeadwaterNetwork.mouthFanIncision(4d, 0.25d), 1.0e-9d);
        assertEquals(1d, NTEHeadwaterNetwork.mouthFanIncision(4d, 0.72d), 1.0e-9d,
            "the complete ordinary channel core must receive the planned mouth incision");
        assertTrue(NTEHeadwaterNetwork.mouthFanIncision(4d, 0.783d) > 0.9d,
            "the outer mouth shoulder must not lose nearly all incision before the bank edge");
        assertTrue(NTEHeadwaterNetwork.mouthFanIncision(4d, 0.95d)
            < NTEHeadwaterNetwork.mouthFanIncision(4d, 0.783d));

        double previous = 0d;
        for (int distanceToOutlet = 7; distanceToOutlet >= 0; distanceToOutlet--)
        {
            final double current = NTEHeadwaterNetwork.mouthFanIncision(distanceToOutlet, 0d);
            assertTrue(current >= previous, "the mouth bed may not rise again before reaching the receiver");
            previous = current;
        }
        assertEquals(8d, NTEHeadwaterNetwork.alignedMouthCutLength(2.5d), 1.0e-9d,
            "the complete fan stays cut-only and cannot build a mouth platform");
        assertEquals(1.5d, NTEHeadwaterNetwork.alignedMouthWaterCutLength(2.5d), 1.0e-9d,
            "water must continue through the cut-only fan until the restored TFC core takes ownership");
        assertEquals(
            NTERiverHydrology.SUPPLEMENTAL_WATER_CORE_RADIUS_SQ,
            NTEHeadwaterNetwork.mouthWaterCoreRadiusSq(8d),
            1.0e-9d
        );
        assertTrue(NTEHeadwaterNetwork.mouthWaterCoreRadiusSq(7d) < NTERiverHydrology.SUPPLEMENTAL_WATER_CORE_RADIUS_SQ);
        assertEquals(0.12d, NTEHeadwaterNetwork.mouthWaterCoreRadiusSq(5d), 1.0e-9d,
            "most of the cut-only mouth must carry only a narrow source-water connector");
        assertEquals(0.12d, NTEHeadwaterNetwork.mouthWaterCoreRadiusSq(0d), 1.0e-9d);
    }

    @Test
    void alignedMouthUsesOneContinuousBankAndWaterTransition()
    {
        assertEquals(1d, NTEHeadwaterNetwork.mouthBankFillWeight(24d), 1.0e-9d);
        assertEquals(0.5d, NTEHeadwaterNetwork.mouthBankFillWeight(16d), 1.0e-9d);
        assertEquals(0d, NTEHeadwaterNetwork.mouthBankFillWeight(8d), 1.0e-9d);
        assertEquals(0d, NTEHeadwaterNetwork.mouthBankFillWeight(0d), 1.0e-9d);

        double previous = 1d;
        for (int distanceToOutlet = 23; distanceToOutlet >= 0; distanceToOutlet--)
        {
            final double current = NTEHeadwaterNetwork.mouthBankFillWeight(distanceToOutlet);
            assertTrue(current <= previous, "bank construction must fade monotonically toward cut-only ownership");
            previous = current;
        }

        assertEquals(0d, NTEHeadwaterNetwork.mouthWaterDrop(8d), 1.0e-9d);
        assertEquals(1d, NTEHeadwaterNetwork.mouthWaterDrop(4d), 1.0e-9d);
        assertEquals(2d, NTEHeadwaterNetwork.mouthWaterDrop(0d), 1.0e-9d);
        assertEquals(0.4d, NTEHeadwaterNetwork.mouthWaterDrop(4d, 65.4d, 65d), 1.0e-9d,
            "a flat confluence may not lower supplemental water below the receiver surface");
        assertEquals(0d, NTEHeadwaterNetwork.mouthWaterDrop(0d, 65d, 65d), 1.0e-9d);
        assertEquals(0d, NTERiverHydrology.wideShapeWeight(3d), 1.0e-9d);
        assertEquals(0.5d, NTERiverHydrology.wideShapeWeight(3.5d), 1.0e-9d);
        assertEquals(1d, NTERiverHydrology.wideShapeWeight(4d), 1.0e-9d);
    }

    @Test
    void mouthIncisionIsNotAttenuatedTwiceByTheChannelCrossSection()
    {
        assertEquals(1d, NTERiverHydrology.supplementalLocalDepth(2.5d, 0d, 0.8d), 1.0e-9d,
            "ordinary outer-bank depth stays unchanged when no mouth incision exists");
        assertEquals(1.75d, NTERiverHydrology.supplementalLocalDepth(2.5d, 0.75d, 0.8d), 1.0e-9d,
            "the already feathered fan incision must be applied once after the base cross-section");
        assertEquals(3.25d, NTERiverHydrology.supplementalLocalDepth(2.5d, 0.75d, 0d), 1.0e-9d);

        final double routeWaterY = 78d;
        final double waterDrop = NTEHeadwaterNetwork.mouthWaterDrop(4d);
        final double centerIncision = NTEHeadwaterNetwork.mouthFanIncision(4d, 0d);
        final double centerBedY = routeWaterY
            - NTERiverHydrology.supplementalLocalDepth(2.5d, centerIncision, 0d);
        assertEquals(2.5d, routeWaterY - waterDrop - centerBedY, 1.0e-9d,
            "mouth erosion must lower source water with the bed instead of increasing visible depth");

        final double edgeIncision = NTEHeadwaterNetwork.mouthFanIncision(
            4d,
            NTERiverHydrology.SUPPLEMENTAL_WATER_CORE_RADIUS_SQ
        );
        final double edgeBedY = routeWaterY
            - NTERiverHydrology.supplementalLocalDepth(
                2.5d,
                edgeIncision,
                NTERiverHydrology.SUPPLEMENTAL_WATER_CORE_RADIUS_SQ
            );
        assertEquals(1d, routeWaterY - waterDrop - edgeBedY, 1.0e-9d,
            "the full source-water core must retain its ordinary tapered depth");
    }

    @Test
    void mouthDistanceFieldTurnsFromTheCreekTowardTheReceiver()
    {
        assertEquals(0.2d, NTEHeadwaterNetwork.mouthGeometryNormalizedDistanceSq(0.2d, 4d, 8d), 1.0e-9d);
        final double halfway = NTEHeadwaterNetwork.mouthGeometryNormalizedDistanceSq(0.2d, 4d, 4d);
        assertTrue(halfway > 0.2d && halfway < 4d);
        assertEquals(4d, NTEHeadwaterNetwork.mouthGeometryNormalizedDistanceSq(0.2d, 4d, 0d), 1.0e-9d,
            "a column extending along the old creek direction must be outside at the receiver");
        assertEquals(0.2d, NTEHeadwaterNetwork.mouthGeometryNormalizedDistanceSq(4d, 0.2d, 0d), 1.0e-9d,
            "the receiver-aligned side of a high-angle mouth must be opened instead");
    }

    @Test
    void cutOnlyMouthStillCarvesAndCarriesWaterWithoutTerrainFill()
    {
        final NTERiverHydrology.ColumnProfile mouth = new NTERiverHydrology.ColumnProfile(
            78d,
            76d,
            76.5d,
            0.08d,
            2.5d,
            0d,
            0.35d,
            0d,
            0.12d,
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
        assertTrue(mouth.inWaterCore());
        assertFalse(mouth.inSourceWaterCore(),
            "the generated flowing fringe remains water without becoming a static source column");
        assertTrue(NTERiverHydrology.shouldUseSupplemental(mouth, null));
        assertTrue(NTERiverHydrology.shouldUseSupplemental(mouth, riverAt(0.5d)),
            "the receiver-aligned fan must keep ownership long enough to blend its banks and bed");
        assertTrue(NTERiverHydrology.protectsBedAt(mouth, mouth.bedBlockY()));
        assertEquals(78d, mouth.applyBankFillTransition(78d, 82d), 1.0e-9d,
            "cut-only ownership may not retain any creek-built bank above the original terrain");
        assertEquals(76d, mouth.applyBankFillTransition(78d, 76d), 1.0e-9d,
            "bank fading must never attenuate an actual terrain cut");
        assertEquals(77.65d, mouth.terrainCutCeiling(78d), 1.0e-9d,
            "cut-only terrain must realize the planned mouth incision despite river-shape bank noise");
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
