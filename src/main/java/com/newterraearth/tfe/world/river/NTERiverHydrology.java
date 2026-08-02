package com.newterraearth.tfe.world.river;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.world.region.RegionPartition;
import net.dries007.tfc.world.region.RiverEdge;
import net.dries007.tfc.world.region.Units;
import net.dries007.tfc.world.river.Flow;
import net.dries007.tfc.world.river.RiverInfo;

/**
 * Runtime bridge between TFC's river graph and the supplemental headwater
 * streams. Main-stem edges stay entirely in TFC's original river pipeline;
 * only a leaf edge whose terrain-aware replacement validates is suppressed.
 */
public final class NTERiverHydrology
{
    static final double TFC_WATER_CORE_RADIUS_SQ = 0.28d;
    static final double SUPPLEMENTAL_WATER_CORE_RADIUS_SQ = 0.72d;
    private static final int MAX_HEIGHT_CACHE_SIZE = 131072;
    private static final ThreadLocal<GenerationContext> ACTIVE_GENERATION = new ThreadLocal<>();

    @FunctionalInterface
    public interface TerrainHeightSampler
    {
        double sample(int blockX, int blockZ);
    }

    @FunctionalInterface
    public interface PartitionLookup
    {
        RegionPartition.Point find(int blockX, int blockZ);
    }

    public enum ChannelKind
    {
        STREAM,
        TRIBUTARY,
        RIVER
    }

    public enum ChannelMode
    {
        SURFACE
    }

    public record CardinalStep(int x, int z) {}

    public record ColumnProfile(
        double waterSurfaceY,
        double centerBedY,
        double bedY,
        double normalizedDistanceSq,
        double channelRadius,
        double bankRaise,
        double terrainIncision,
        double mouthWaterDrop,
        double bankFillWeight,
        double waterCoreRadiusSq,
        boolean fillAllowed,
        boolean waterAllowed,
        boolean sourceWaterAllowed,
        double receiverBlendWeight,
        boolean waterfallLanding,
        boolean headwater,
        ChannelKind kind,
        ChannelMode mode,
        Flow flow
    )
    {
        public boolean inChannel()
        {
            return normalizedDistanceSq <= 1d;
        }

        public boolean inWaterCore()
        {
            return waterAllowed && normalizedDistanceSq <= waterCoreRadiusSq;
        }

        public boolean inSourceWaterCore()
        {
            return sourceWaterAllowed && normalizedDistanceSq <= waterCoreRadiusSq;
        }

        public boolean surfaceVisible()
        {
            return true;
        }

        public boolean descendingReceiverMouth()
        {
            return !fillAllowed && receiverBlendWeight > 0d && terrainIncision > 0d;
        }

        /** A cut-only fallback feeder which must remain wet until native water contact. */
        public boolean retainedFeederMouth()
        {
            return inChannel() && !fillAllowed && waterAllowed && receiverBlendWeight <= 0d;
        }

        public boolean subterranean()
        {
            return false;
        }

        public int waterBlockY()
        {
            return Mth.floor(waterSurfaceY);
        }

        public int bedBlockY()
        {
            return Mth.floor(bedY);
        }

        /** Supplemental streams are cut into terrain and may never build it up. */
        public double fillCeiling(double terrainHeight)
        {
            return terrainHeight;
        }

        /**
         * Fade only the part of a BANKED sample which would build above the
         * pre-river terrain. Cutting remains fully effective throughout the
         * transition, and the weight reaches zero before cut-only ownership.
         */
        public double applyBankFillTransition(double terrainHeight, double sampledHeight)
        {
            if (sampledHeight <= terrainHeight)
            {
                return sampledHeight;
            }
            return Mth.lerp(bankFillWeight, terrainHeight, sampledHeight);
        }

        /**
         * A cut-only receiver mouth must realize its planned erosion even when
         * the selected TFC river-shape noise happens to raise an outer bank.
         * The explicit incision is retained even though source water descends
         * with the cut, so lowering the water cannot accidentally cancel the
         * cut-only terrain ceiling.
         */
        public double terrainCutCeiling(double terrainHeight)
        {
            if (fillAllowed)
            {
                return terrainHeight;
            }
            return terrainHeight - Math.max(0d, terrainIncision);
        }
    }

    public static boolean shouldUseSupplemental(@Nullable ColumnProfile profile, @Nullable RiverInfo retainedTfcRiver)
    {
        if (profile == null)
        {
            return false;
        }
        if (retainedTfcRiver == null)
        {
            return true;
        }
        if (!profile.inChannel())
        {
            return false;
        }
        // During the receiver-aligned fan the supplemental profile is the
        // transition itself. Let it continue through the retained TFC water
        // core so its bed and banks can converge over several columns instead
        // of switching ownership on one vertical slice.
        if (profile.receiverBlendWeight() > 0d)
        {
            return true;
        }
        if (profile.retainedFeederMouth())
        {
            return true;
        }
        return profile.fillAllowed() && retainedTfcRiver.normDistSq() > TFC_WATER_CORE_RADIUS_SQ;
    }

    /**
     * A receiver-aligned supplemental cross-section may rotate and fade into a
     * retained TFC river, but it may never leave a shallower bed on top of that
     * river's already excavated column.
     */
    public static boolean usesRetainedReceiverBed(
        @Nullable ColumnProfile profile,
        @Nullable RiverInfo retainedTfcRiver
    )
    {
        return profile != null
            && retainedTfcRiver != null
            && profile.inChannel()
            && profile.receiverBlendWeight() > 0d;
    }

    public static double clampToRetainedReceiverBed(
        @Nullable ColumnProfile profile,
        @Nullable RiverInfo retainedTfcRiver,
        double supplementalHeight,
        double retainedReceiverHeight
    )
    {
        return usesRetainedReceiverBed(profile, retainedTfcRiver)
            ? Math.min(supplementalHeight, retainedReceiverHeight)
            : supplementalHeight;
    }

    /** The density-stage bed follows any lower receiver-bed ceiling. */
    public static int effectiveBedBlockY(ColumnProfile profile, double terrainHeight)
    {
        return Math.min(profile.bedBlockY(), Mth.floor(terrainHeight));
    }

    /**
     * A waterfall landing above the native river may remove its old raised
     * source shelf, but the fixed minimum river layer itself must survive.
     */
    public static int retainedMouthWaterClearFromY(
        ColumnProfile profile,
        int minimumRiverWaterY
    )
    {
        final int plannedWaterY = profile.waterBlockY();
        return profile.waterfallLanding() && plannedWaterY > minimumRiverWaterY
            ? plannedWaterY
            : plannedWaterY + 1;
    }

    /** Clear the planned vertical descent above a wet connector, including cave rivers. */
    public static boolean clearsWetMouthHeadroom(ColumnProfile profile, int y)
    {
        final int clearanceCeilingY = Mth.ceil(
            profile.waterSurfaceY() + profile.mouthWaterDrop()
        );
        return profile.descendingReceiverMouth()
            && profile.inWaterCore()
            && y > profile.waterBlockY()
            && y <= clearanceCeilingY;
    }

    /**
     * Initial terrain fill needs a stable five-block shallow roof before
     * carvers run. Three blocks still left density-noise caves visibly open
     * directly beneath narrow upland streams, producing a thin floating slab.
     */
    public static boolean protectsBedAt(ColumnProfile profile, int y)
    {
        return protectsBedAt(profile, y, profile.bedY());
    }

    public static boolean protectsBedAt(ColumnProfile profile, int y, double terrainHeight)
    {
        final int bedY = effectiveBedBlockY(profile, terrainHeight);
        return profile.inWaterCore() && y <= bedY && y >= bedY - 4;
    }

    /**
     * The fixed river layer is source-like TFC river water. A descending
     * connector may use vanilla flowing water above it, but its contact layer
     * must rejoin the receiver's directional-water semantics.
     */
    public static boolean usesDirectionalReceiverSurfaceWater(
        ColumnProfile profile,
        int y,
        int minimumRiverWaterY
    )
    {
        return profile.inWaterCore()
            && y == profile.waterBlockY()
            && y <= minimumRiverWaterY;
    }

    /** Protect the realized wet corridor from late cave-spike decoration. */
    public static boolean blocksCaveDecoration(ColumnProfile profile, int featureY)
    {
        final int clearanceCeilingY = Mth.ceil(
            profile.waterSurfaceY() + profile.mouthWaterDrop()
        );
        return profile.inWaterCore()
            && featureY >= profile.bedBlockY() - 4
            && featureY <= clearanceCeilingY + 3;
    }

    /** A cave column grows upward from its origin until it reaches the roof. */
    public static boolean blocksCaveColumn(ColumnProfile profile, int featureY)
    {
        final int clearanceCeilingY = Mth.ceil(
            profile.waterSurfaceY() + profile.mouthWaterDrop()
        );
        return profile.inWaterCore() && featureY <= clearanceCeilingY + 1;
    }

    /**
     * Erosion's late stability repair may not bridge the planned wet cavity
     * with hardened support stone after the density pass has opened it.
     */
    public static boolean blocksErosionSupport(ColumnProfile profile, int y)
    {
        final int clearanceCeilingY = Mth.ceil(
            profile.waterSurfaceY() + profile.mouthWaterDrop()
        );
        return profile.inWaterCore()
            && y > profile.bedBlockY()
            && y <= clearanceCeilingY + 1;
    }

    public static void activateGeneration(
        NTERiverHydrology hydrology,
        @Nullable ColumnProfile[] localProfiles,
        int chunkMinX,
        int chunkMinZ
    )
    {
        ACTIVE_GENERATION.set(new GenerationContext(hydrology, localProfiles, chunkMinX, chunkMinZ));
    }

    public static void clearActiveGeneration()
    {
        ACTIVE_GENERATION.remove();
    }

    @Nullable
    public static ColumnProfile activeGenerationProfile(int blockX, int blockZ)
    {
        final GenerationContext context = ACTIVE_GENERATION.get();
        if (context == null)
        {
            return null;
        }
        if (context.localProfiles() != null
            && blockX >= context.chunkMinX() && blockX < context.chunkMinX() + 16
            && blockZ >= context.chunkMinZ() && blockZ < context.chunkMinZ() + 16)
        {
            return context.localProfiles()[
                blockX - context.chunkMinX() + 16 * (blockZ - context.chunkMinZ())
            ];
        }
        return context.hydrology().findGraphProfileIfPlanned(blockX, blockZ);
    }

    public static boolean hasActiveGenerationColumn(NTERiverHydrology hydrology, int blockX, int blockZ)
    {
        final GenerationContext context = ACTIVE_GENERATION.get();
        return context != null
            && context.hydrology() == hydrology
            && context.localProfiles() != null
            && blockX >= context.chunkMinX() && blockX < context.chunkMinX() + 16
            && blockZ >= context.chunkMinZ() && blockZ < context.chunkMinZ() + 16;
    }

    private record GenerationContext(
        NTERiverHydrology hydrology,
        @Nullable ColumnProfile[] localProfiles,
        int chunkMinX,
        int chunkMinZ
    ) {}

    /** One non-branching cardinal step that rasterizes the supplied flow direction. */
    public static CardinalStep cardinalFlowStep(Flow flow, int horizontalStep)
    {
        final double vectorX = flow.getVector().x;
        final double vectorZ = flow.getVector().z;
        final double absX = Math.abs(vectorX);
        final double absZ = Math.abs(vectorZ);
        final double total = absX + absZ;
        if (total <= 1.0e-9d)
        {
            return new CardinalStep(0, 0);
        }

        final int step = Math.max(1, horizontalStep);
        final double xShare = absX / total;
        final boolean advancesX = Math.round(step * xShare) > Math.round((step - 1d) * xShare);
        if (advancesX && absX > 1.0e-9d)
        {
            return new CardinalStep(vectorX > 0d ? 1 : -1, 0);
        }
        if (absZ > 1.0e-9d)
        {
            return new CardinalStep(0, vectorZ > 0d ? 1 : -1);
        }
        return new CardinalStep(vectorX > 0d ? 1 : -1, 0);
    }

    private final TerrainHeightSampler terrainHeightSampler;
    private final PartitionLookup partitionLookup;
    private final NTEHeadwaterNetwork headwaters;
    private final Map<Long, Double> heightCache = new LinkedHashMap<>();

    public NTERiverHydrology(
        long seed,
        int seaLevel,
        TerrainHeightSampler terrainHeightSampler,
        PartitionLookup partitionLookup
    )
    {
        this.terrainHeightSampler = terrainHeightSampler;
        this.partitionLookup = partitionLookup;
        this.headwaters = new NTEHeadwaterNetwork(
            seed,
            seaLevel,
            this::sampleTerrainHeight,
            this::blocksUnrelatedRetainedRiver
        );
    }

    /**
     * Planning-only mask for native downstream river edges. TFC marks an edge
     * as sourceEdge once another edge feeds it, so these are the main-stem
     * corridors that this addon always retains. The leaf being replaced and
     * its designated receiver remain legal; every other retained wet core is
     * a structural obstacle, not a low terrain valley for a new creek to use.
     * This query intentionally never asks headwaters for replacement state.
     */
    private boolean blocksUnrelatedRetainedRiver(
        @Nullable RiverEdge owner,
        double blockX,
        double blockZ,
        double clearance
    )
    {
        final RiverEdge receiver = owner == null ? null : owner.drainEdge();
        final RegionPartition.Point point = partitionLookup.find(Mth.floor(blockX), Mth.floor(blockZ));
        final double exactGridX = Units.blockToGridExact(blockX);
        final double exactGridZ = Units.blockToGridExact(blockZ);
        for (RiverEdge edge : point.rivers())
        {
            if (!edge.sourceEdge() || edge == owner || edge == receiver)
            {
                continue;
            }
            final double distanceBlocks = Math.sqrt(edge.fractal().intersectDistance(exactGridX, exactGridZ))
                * Units.GRID_WIDTH_IN_BLOCK;
            final double wetRadius = Math.sqrt(
                edge.widthSq(exactGridX, exactGridZ) * TFC_WATER_CORE_RADIUS_SQ
            );
            if (distanceBlocks <= wetRadius + clearance)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolve the nearest retained TFC edge. The height filler's raw nearest
     * edge can be a suppressed leaf even when a retained main stem is the
     * physical receiving channel at this column.
     */
    @Nullable
    public RiverInfo retainedRiverInfo(@Nullable RiverInfo nearest, int blockX, int blockZ)
    {
        planCandidateLeaves(blockX, blockZ);
        return retainedRiverInfoFromPlannedCandidates(nearest, blockX, blockZ);
    }

    @Nullable
    public RiverInfo retainedRiverInfoPrepared(@Nullable RiverInfo nearest, int blockX, int blockZ)
    {
        return retainedRiverInfoFromPlannedCandidates(nearest, blockX, blockZ);
    }

    @Nullable
    private RiverInfo retainedRiverInfoFromPlannedCandidates(@Nullable RiverInfo nearest, int blockX, int blockZ)
    {
        if (nearest != null && retainsTfcEdge(nearest.edge()))
        {
            return headwaters.suppressesRetainedAt(nearest.edge(), blockX, blockZ)
                ? null
                : adaptRetainedLeafWidth(nearest, blockX, blockZ);
        }

        final RegionPartition.Point point = partitionLookup.find(blockX, blockZ);
        final double exactGridX = Units.blockToGridExact(blockX);
        final double exactGridZ = Units.blockToGridExact(blockZ);
        final double limitDistGridSq = 50d * 50d / (Units.GRID_WIDTH_IN_BLOCK * Units.GRID_WIDTH_IN_BLOCK);
        double minimumAdjusted = Double.POSITIVE_INFINITY;
        double minimumDistanceGridSq = Double.POSITIVE_INFINITY;
        RiverEdge minimumEdge = null;
        for (RiverEdge edge : point.rivers())
        {
            if (!retainsTfcEdge(edge) || headwaters.suppressesRetainedAt(edge, blockX, blockZ))
            {
                continue;
            }
            final double distanceGridSq = edge.fractal().intersectDistance(exactGridX, exactGridZ);
            if (distanceGridSq >= limitDistGridSq)
            {
                continue;
            }
            final double adjusted = distanceGridSq / edge.widthSq();
            if (adjusted < minimumAdjusted)
            {
                minimumAdjusted = adjusted;
                minimumDistanceGridSq = distanceGridSq;
                minimumEdge = edge;
            }
        }
        if (minimumEdge == null)
        {
            return null;
        }

        final double widthSq = minimumEdge.widthSq(exactGridX, exactGridZ);
        final double distanceBlocksSq = minimumDistanceGridSq
            * Units.GRID_WIDTH_IN_BLOCK * Units.GRID_WIDTH_IN_BLOCK;
        return adaptRetainedLeafWidth(new RiverInfo(
            minimumEdge,
            minimumEdge.fractal().calculateFlow(exactGridX, exactGridZ),
            distanceBlocksSq,
            widthSq
        ), blockX, blockZ);
    }

    /** Compatibility entry used by the height filler. RiverInfo is not needed for a new stream sample. */
    @Nullable
    public ColumnProfile sample(@Nullable RiverInfo ignored, int blockX, int blockZ, double ambientHeight)
    {
        return findProfile(blockX, blockZ, ambientHeight);
    }

    @Nullable
    public ColumnProfile samplePreparedColumn(int blockX, int blockZ, double ambientHeight)
    {
        return findProfileFromPlannedCandidates(blockX, blockZ, ambientHeight);
    }

    /**
     * Keeps biome queries aligned with both generators: retained TFC edges use
     * TFC's original 0.08 intersection, while replacement streams use their
     * fill-safe water core.
     */
    public boolean isVisibleRiver(int blockX, int blockZ)
    {
        final RegionPartition.Point point = partitionLookup.find(blockX, blockZ);
        final double exactGridX = Units.blockToGridExact(blockX);
        final double exactGridZ = Units.blockToGridExact(blockZ);
        for (RiverEdge edge : point.rivers())
        {
            // Most biome queries are unrelated to a river. Reject those before
            // consulting any headwater state; planning a whole drainage route
            // here made initial structure biome searches take several minutes.
            if (!edge.fractal().intersect(exactGridX, exactGridZ, 0.08d))
            {
                continue;
            }

            final Boolean replacement = headwaters.replacementIfPlanned(edge);
            if (replacement == null)
            {
                // Until normal chunk generation reaches this leaf, retain TFC's
                // biome result. A visibility lookup is deliberately read-only.
                return true;
            }
            if (replacement)
            {
                continue;
            }

            final double widthScale = headwaters.retainedLeafWidthScaleIfPlanned(edge, blockX, blockZ);
            if (edge.fractal().intersect(exactGridX, exactGridZ, 0.08d * widthScale))
            {
                return true;
            }
        }

        final ColumnProfile profile = findPlannedGraphProfile(blockX, blockZ);
        return profile != null && profile.inWaterCore();
    }

    @Nullable
    private ColumnProfile findPlannedGraphProfile(int blockX, int blockZ)
    {
        NTEHeadwaterNetwork.Sample nearest = null;
        for (RiverEdge edge : candidateEdges(blockX, blockZ))
        {
            if (edge.sourceEdge() || !headwaters.mayInfluence(edge, blockX, blockZ))
            {
                continue;
            }
            final NTEHeadwaterNetwork.Sample sample = headwaters.sampleIfPlanned(edge, blockX, blockZ);
            if (NTEHeadwaterNetwork.samplePreferred(sample, nearest))
            {
                nearest = sample;
            }
        }
        final NTEHeadwaterNetwork.Sample spatial = headwaters.samplePlannedAt(blockX, blockZ, Double.POSITIVE_INFINITY);
        if (NTEHeadwaterNetwork.samplePreferred(spatial, nearest))
        {
            nearest = spatial;
        }
        return nearest == null ? null : createProfile(nearest);
    }

    /** Read-only late-decoration query; never initiates a new drainage search. */
    @Nullable
    public ColumnProfile findGraphProfileIfPlanned(int blockX, int blockZ)
    {
        return findPlannedGraphProfile(blockX, blockZ);
    }

    /** Query stream geometry for groundwater influence without a per-column terrain rejection. */
    @Nullable
    public ColumnProfile findGraphProfile(int blockX, int blockZ)
    {
        return findProfileFromCandidates(blockX, blockZ, Double.POSITIVE_INFINITY);
    }

    @Nullable
    public ColumnProfile findProfile(int blockX, int blockZ, double ambientHeight)
    {
        return findProfileFromCandidates(blockX, blockZ, ambientHeight);
    }

    @Nullable
    private ColumnProfile findProfileFromCandidates(
        int blockX,
        int blockZ,
        double ambientHeight
    )
    {
        planCandidateLeaves(blockX, blockZ);
        return findProfileFromPlannedCandidates(blockX, blockZ, ambientHeight);
    }

    @Nullable
    private ColumnProfile findProfileFromPlannedCandidates(
        int blockX,
        int blockZ,
        double ambientHeight
    )
    {
        NTEHeadwaterNetwork.Sample nearest = null;
        for (RiverEdge edge : candidateEdges(blockX, blockZ))
        {
            if (edge.sourceEdge() || !headwaters.mayInfluence(edge, blockX, blockZ))
            {
                continue;
            }
            final NTEHeadwaterNetwork.Sample sample = headwaters.sampleIfPlanned(
                edge,
                blockX,
                blockZ,
                ambientHeight
            );
            if (NTEHeadwaterNetwork.samplePreferred(sample, nearest))
            {
                nearest = sample;
            }
        }
        final NTEHeadwaterNetwork.Sample spatial = headwaters.samplePlannedAt(blockX, blockZ, ambientHeight);
        if (NTEHeadwaterNetwork.samplePreferred(spatial, nearest))
        {
            nearest = spatial;
        }
        return nearest == null ? null : createProfile(nearest);
    }

    /**
     * Only the local TFC partition is allowed to initiate route planning.
     * Already-planned terrain-aware routes are found through the headwater
     * spatial index, including portions that have left their original
     * partition. Broad neighboring-partition scans multiply route planning and
     * made otherwise unrelated chunk generation contend on the same cache.
     */
    private List<RiverEdge> candidateEdges(int blockX, int blockZ)
    {
        return partitionLookup.find(blockX, blockZ).rivers();
    }

    private void planCandidateLeaves(int blockX, int blockZ)
    {
        for (RiverEdge edge : candidateEdges(blockX, blockZ))
        {
            if (!edge.sourceEdge() && headwaters.mayInfluence(edge, blockX, blockZ))
            {
                headwaters.ensurePlanned(edge);
            }
        }
    }

    private ColumnProfile createProfile(NTEHeadwaterNetwork.Sample sample)
    {
        final double baseCenterDepth = baseCenterDepth(sample.channelRadius());
        final double centerDepth = baseCenterDepth + sample.extraIncision();
        final double centerBedY = sample.waterSurfaceY() - centerDepth;
        final double localDepth = supplementalLocalDepth(
            baseCenterDepth,
            sample.extraIncision(),
            sample.normalizedDistanceSq()
        );
        final double bedY = sample.waterSurfaceY() - localDepth;
        // The fan erosion and the source-water surface descend together in the
        // ordinary water core. This preserves the already validated absolute
        // bed cut while preventing the old 2 -> 3+ block depth jump and raised
        // water shelf at the ownership boundary.
        final double waterSurfaceY = sample.waterSurfaceY() - sample.mouthWaterDrop();
        final ChannelKind kind = sample.channelRadius() < 3.5d ? ChannelKind.STREAM : ChannelKind.TRIBUTARY;

        return new ColumnProfile(
            waterSurfaceY,
            centerBedY,
            bedY,
            sample.normalizedDistanceSq(),
            sample.channelRadius(),
            0d,
            sample.extraIncision(),
            sample.mouthWaterDrop(),
            sample.bankFillWeight(),
            sample.waterCoreRadiusSq(),
            sample.fillAllowed(),
            sample.waterAllowed(),
            sample.sourceWaterAllowed(),
            sample.receiverBlendWeight(),
            sample.waterfallLanding(),
            sample.headwater(),
            kind,
            ChannelMode.SURFACE,
            sample.flow()
        );
    }

    private static double baseCenterDepth(double channelRadius)
    {
        return Mth.clamp(1.1d + channelRadius * 0.22d, 1.25d, 2.75d);
    }

    /**
     * Shape the ordinary channel first, then apply the already laterally
     * feathered mouth incision once. Applying the fan incision before the
     * cross-section taper attenuated it twice and preserved a raised rim at
     * cut-only receiver mouths.
     */
    static double supplementalLocalDepth(
        double baseCenterDepth,
        double extraIncision,
        double normalizedDistanceSq
    )
    {
        final double crossSection = Mth.clamp(
            normalizedDistanceSq / SUPPLEMENTAL_WATER_CORE_RADIUS_SQ,
            0d,
            1d
        );
        final double baseLocalDepth = Math.max(1d, baseCenterDepth * (1d - crossSection * crossSection));
        return baseLocalDepth + extraIncision;
    }

    public static double wideShapeWeight(double channelRadius)
    {
        final double progress = Mth.clamp((channelRadius - 3d) / 1d, 0d, 1d);
        return progress * progress * (3d - 2d * progress);
    }

    public boolean retainsTfcEdge(RiverEdge edge)
    {
        return edge.sourceEdge() || !headwaters.replaces(edge);
    }

    private RiverInfo adaptRetainedLeafWidth(RiverInfo info, int blockX, int blockZ)
    {
        final RiverEdge edge = info.edge();
        if (edge == null || edge.sourceEdge())
        {
            return info;
        }
        final double widthScale = headwaters.retainedLeafWidthScale(edge, blockX, blockZ);
        if (widthScale >= 0.999999d)
        {
            return info;
        }
        return new RiverInfo(info.edge(), info.flow(), info.distSq(), info.widthSq() * widthScale * widthScale);
    }

    private double sampleTerrainHeight(int blockX, int blockZ)
    {
        final long key = ((long) blockX << 32) ^ (blockZ & 0xffffffffL);
        synchronized (heightCache)
        {
            final Double cached = heightCache.get(key);
            if (cached != null)
            {
                return cached;
            }
        }

        // Do not hold the cache lock while the height filler performs nested biome queries.
        final double height = terrainHeightSampler.sample(blockX, blockZ);
        synchronized (heightCache)
        {
            if (heightCache.size() >= MAX_HEIGHT_CACHE_SIZE)
            {
                heightCache.clear();
            }
            heightCache.put(key, height);
        }
        return height;
    }
}
