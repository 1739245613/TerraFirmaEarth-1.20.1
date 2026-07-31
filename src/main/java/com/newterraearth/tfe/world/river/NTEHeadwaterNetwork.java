package com.newterraearth.tfe.world.river;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import com.mojang.logging.LogUtils;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import net.dries007.tfc.world.region.RiverEdge;
import net.dries007.tfc.world.region.Units;
import net.dries007.tfc.world.river.Flow;

/**
 * Lazily plans a fine, terrain-sampled creek for each replaceable TFC leaf edge.
 * The TFC graph remains authoritative: the receiving edge is the outlet, while
 * only the route from the source area to its first physical contact is re-planned.
 */
final class NTEHeadwaterNetwork
{
    static final int SAMPLE_STEP = 8;
    private static final int PREFERRED_CORNER_PRECISION = 1;

    private static final int ROUTE_PADDING = 160;
    private static final int SOURCE_EXTENSION_MIN = 48;
    private static final int SOURCE_EXTENSION_MAX = 120;
    private static final int SOURCE_EXTENSION_LATERAL = 80;
    private static final double SOURCE_CHANNEL_RADIUS = 0.65d;
    private static final double CENTER_SURFACE_INSET = 1.25d;
    private static final double BANK_FREEBOARD = 0.10d;
    private static final double MAX_CASCADE_SLOPE = 1.0d;
    private static final double MAX_NORMAL_INCISION = 5.0d;
    private static final double MAX_FEEDER_INCISION = 5.5d;
    private static final double FEEDER_RECEIVER_WIDTH_SCALE = 0.36d;
    private static final double TFC_LEAF_TAPER_LENGTH = 64d;
    private static final double SOURCE_ALIGNMENT_LENGTH = 48d;
    private static final double SOURCE_ALIGNMENT_REWRITE_LENGTH = 32d;
    private static final double SOURCE_ALIGNMENT_SAMPLE_SPACING = 1.5d;
    private static final double SOURCE_ALIGNMENT_SUPPRESSION_WIDTH_SCALE = 1.35d;
    private static final double MOUTH_BANK_TRANSITION_LENGTH = 24d;
    private static final double MOUTH_FAN_LENGTH = 8d;
    private static final double MOUTH_FAN_MAX_INCISION = 2d;
    private static final double MOUTH_WATER_TAPER_LENGTH = 3d;
    private static final double MOUTH_CONNECTOR_WATER_CORE_RADIUS_SQ = 0.12d;
    private static final double OUTLET_ADAPTER_LENGTH = SOURCE_ALIGNMENT_LENGTH + 40d;
    private static final double MAX_OUTLET_ADAPTER_INCISION = 8d;
    private static final int MAX_HEADWATER_CACHE_SIZE = 512;
    private static final double MAX_INFLUENCE_SQ = 2.25d;
    private static final double SPATIAL_INDEX_MARGIN = 12d;
    private static final boolean TRACE = Boolean.getBoolean("tfe.debug.runtimeTrace");
    private static final Logger LOGGER = LogUtils.getLogger();

    @FunctionalInterface
    interface HeightSampler
    {
        double sample(int blockX, int blockZ);
    }

    record Sample(
        double waterSurfaceY,
        double normalizedDistanceSq,
        double channelRadius,
        double extraIncision,
        double mouthWaterDrop,
        double bankFillWeight,
        double waterCoreRadiusSq,
        boolean fillAllowed,
        boolean waterAllowed,
        boolean sourceWaterAllowed,
        double receiverBlendWeight,
        boolean waterfallLanding,
        boolean headwater,
        Flow flow
    ) {}

    record DiagnosticPoint(double x, double z, double terrainY, double waterY, double radius) {}

    record Vec(double x, double z) {}

    private record SearchNode(int index, double score) {}

    private record Projection(double distanceSq, double delta) {}

    private record OutletRoute(
        List<Vec> points,
        boolean receiverMouth,
        double receiverWidth,
        @Nullable ReceiverAlignment alignment
    ) {}

    private record ReceiverPath(List<Vec> points, double width) {}

    private record GridCell(int x, int z) {}

    private final long seed;
    private final int seaLevel;
    private final HeightSampler heights;
    private final Map<RiverEdge, Headwater> headwaters = new IdentityHashMap<>();
    private final Map<Long, List<Headwater>> plannedByChunk = new HashMap<>();

    NTEHeadwaterNetwork(long seed, int seaLevel, HeightSampler heights)
    {
        this.seed = seed;
        this.seaLevel = seaLevel;
        this.heights = heights;
    }

    boolean replaces(RiverEdge edge)
    {
        if (edge.sourceEdge())
        {
            return false;
        }
        final Headwater headwater = headwater(edge);
        final boolean replacement = headwater.replacesTfc();
        indexPlanned(headwater);
        return replacement;
    }

    boolean hasFeeder(RiverEdge edge)
    {
        if (edge.sourceEdge())
        {
            return false;
        }
        final Headwater headwater = headwater(edge);
        final boolean feeder = headwater.hasFeeder();
        indexPlanned(headwater);
        return feeder;
    }

    /**
     * Returns the replacement decision only after normal chunk generation has
     * planned this leaf. Biome and structure lookups must never turn a cheap
     * visibility query into a complete terrain search.
     */
    @Nullable
    Boolean replacementIfPlanned(RiverEdge edge)
    {
        if (edge.sourceEdge())
        {
            return Boolean.FALSE;
        }
        final Headwater headwater = existingHeadwater(edge);
        return headwater == null ? null : headwater.replacementIfPlanned();
    }

    double retainedLeafWidthScale(RiverEdge edge, int blockX, int blockZ)
    {
        final Headwater headwater = headwater(edge);
        if (headwater.replacesTfc())
        {
            return 1d;
        }
        indexPlanned(headwater);
        return retainedLeafWidthScaleForFeeder(edge, blockX, blockZ);
    }

    double retainedLeafWidthScaleIfPlanned(RiverEdge edge, int blockX, int blockZ)
    {
        final Headwater headwater = existingHeadwater(edge);
        if (headwater == null || !headwater.retainsTfcIfPlanned())
        {
            return 1d;
        }

        return retainedLeafWidthScaleForFeeder(edge, blockX, blockZ);
    }

    private static double retainedLeafWidthScaleForFeeder(RiverEdge edge, int blockX, int blockZ)
    {

        final double sourceX = edge.source().x() * Units.GRID_WIDTH_IN_BLOCK;
        final double sourceZ = edge.source().y() * Units.GRID_WIDTH_IN_BLOCK;
        final double drainX = edge.drain().x() * Units.GRID_WIDTH_IN_BLOCK;
        final double drainZ = edge.drain().y() * Units.GRID_WIDTH_IN_BLOCK;
        final double dx = drainX - sourceX;
        final double dz = drainZ - sourceZ;
        final double length = Math.hypot(dx, dz);
        if (length < 1.0e-6d)
        {
            return 1d;
        }
        final double along = ((blockX - sourceX) * dx + (blockZ - sourceZ) * dz) / length;
        return sourceWidthScale(along);
    }

    static double sourceWidthScale(double distanceDownstream)
    {
        final double progress = Mth.clamp(distanceDownstream / TFC_LEAF_TAPER_LENGTH, 0d, 1d);
        final double smooth = progress * progress * (3d - 2d * progress);
        return Mth.lerp(smooth, FEEDER_RECEIVER_WIDTH_SCALE, 1d);
    }

    static double matchedReceiverRadius(double receiverWidth)
    {
        return Math.max(
            SOURCE_CHANNEL_RADIUS,
            receiverWidth * Math.sqrt(
                NTERiverHydrology.TFC_WATER_CORE_RADIUS_SQ
                    / NTERiverHydrology.SUPPLEMENTAL_WATER_CORE_RADIUS_SQ
            )
        );
    }

    static double mouthBankFillWeight(double distanceToOutlet)
    {
        if (distanceToOutlet >= MOUTH_BANK_TRANSITION_LENGTH)
        {
            return 1d;
        }
        if (distanceToOutlet <= MOUTH_FAN_LENGTH)
        {
            return 0d;
        }
        return smootherStep(Mth.clamp(
            (distanceToOutlet - MOUTH_FAN_LENGTH)
                / (MOUTH_BANK_TRANSITION_LENGTH - MOUTH_FAN_LENGTH),
            0d,
            1d
        ));
    }

    static double mouthWaterDrop(double distanceToOutlet)
    {
        if (distanceToOutlet >= MOUTH_FAN_LENGTH)
        {
            return 0d;
        }
        return MOUTH_FAN_MAX_INCISION * smootherStep(Mth.clamp(
            (MOUTH_FAN_LENGTH - distanceToOutlet) / MOUTH_FAN_LENGTH,
            0d,
            1d
        ));
    }

    static double mouthWaterDrop(double distanceToOutlet, double localWaterY, double receiverWaterY)
    {
        return Math.min(
            mouthWaterDrop(distanceToOutlet),
            Math.max(0d, localWaterY - receiverWaterY)
        );
    }

    private static double mouthFanLateralWeight(double normalizedDistanceSq)
    {
        return smootherStep(Mth.clamp(
            (1d - normalizedDistanceSq) / (1d - NTERiverHydrology.SUPPLEMENTAL_WATER_CORE_RADIUS_SQ),
            0d,
            1d
        ));
    }

    static double mouthFanIncision(double distanceToOutlet, double normalizedDistanceSq)
    {
        if (distanceToOutlet >= MOUTH_FAN_LENGTH || normalizedDistanceSq >= 1d)
        {
            return 0d;
        }
        // Keep the erosion cut at full strength across the ordinary channel
        // core, then feather it through the outer bank. The old radial
        // smootherstep attenuated the incision almost to zero by radialSq
        // ~= 0.78; the profile cross-section attenuated it a second time and
        // left an uncut shoulder around an otherwise open mouth.
        return mouthWaterDrop(distanceToOutlet) * mouthFanLateralWeight(normalizedDistanceSq);
    }

    static double mouthGeometryNormalizedDistanceSq(
        double streamNormalizedDistanceSq,
        double receiverNormalizedDistanceSq,
        double distanceToOutlet
    )
    {
        return Mth.lerp(
            mouthReceiverBlendWeight(distanceToOutlet),
            streamNormalizedDistanceSq,
            receiverNormalizedDistanceSq
        );
    }

    static double mouthReceiverBlendWeight(double distanceToOutlet)
    {
        return smootherStep(Mth.clamp(
            (MOUTH_FAN_LENGTH - distanceToOutlet) / MOUTH_FAN_LENGTH,
            0d,
            1d
        ));
    }

    /**
     * Reserve one ordinary flow cell, or two cells for a descending mouth, in
     * front of the static source-water core. Those cells are still generated
     * as flowing water and participate in the full generation-time waterfall
     * bake; this is source-shape compensation, not a limit on fall length.
     */
    static double mouthSourceWaterInset(double mouthWaterDrop)
    {
        return mouthWaterDrop >= 0.75d ? 2d : 1d;
    }

    static boolean mouthSourceWaterAllowed(
        double distanceToOutlet,
        double waterCutLength,
        double mouthWaterDrop
    )
    {
        return distanceToOutlet > waterCutLength + mouthSourceWaterInset(mouthWaterDrop);
    }

    static boolean plannedSourceWaterAllowed(double downstreamWaterDrop)
    {
        return downstreamWaterDrop < 0.5d;
    }

    /**
     * Retract the static source core before a quantized water-surface step.
     * Looking two blocks ahead models the short fringe which an ordinary
     * source stair creates after fluid settling, without simulating an
     * unbounded water update during route planning.
     */
    static boolean plannedSourceWaterAllowed(
        double currentPlannedWaterY,
        double downstreamPlannedWaterY,
        double secondDownstreamPlannedWaterY
    )
    {
        final int currentBlockY = Mth.floor(currentPlannedWaterY);
        return currentBlockY <= Mth.floor(downstreamPlannedWaterY)
            && currentBlockY <= Mth.floor(secondDownstreamPlannedWaterY);
    }

    static boolean naturalWaterfallLanding(double upstreamRouteWaterY, double localRouteWaterY)
    {
        return Mth.floor(upstreamRouteWaterY) > Mth.floor(localRouteWaterY);
    }

    static double alignedMouthCutLength(double localRadius)
    {
        return Math.max(MOUTH_FAN_LENGTH, Math.max(1.25d, Math.min(3d, localRadius * 0.6d)));
    }

    static double alignedMouthWaterCutLength(double localRadius)
    {
        return Math.max(1.25d, Math.min(3d, localRadius * 0.6d));
    }

    static double mouthWaterCoreRadiusSq(double distanceToOutlet)
    {
        final double progress = smootherStep(Mth.clamp(
            (MOUTH_FAN_LENGTH - distanceToOutlet) / MOUTH_WATER_TAPER_LENGTH,
            0d,
            1d
        ));
        return Mth.lerp(
            progress,
            NTERiverHydrology.SUPPLEMENTAL_WATER_CORE_RADIUS_SQ,
            MOUTH_CONNECTOR_WATER_CORE_RADIUS_SQ
        );
    }

    private static double smootherStep(double value)
    {
        final double t = Mth.clamp(value, 0d, 1d);
        return t * t * t * (t * (t * 6d - 15d) + 10d);
    }

    void ensurePlanned(RiverEdge edge)
    {
        if (!edge.sourceEdge())
        {
            final Headwater headwater = headwater(edge);
            headwater.route();
            indexPlanned(headwater);
        }
    }

    boolean suppressesRetainedAt(RiverEdge edge, int blockX, int blockZ)
    {
        final List<Headwater> candidates;
        final long targetChunkKey = chunkKey(blockX >> 4, blockZ >> 4);
        synchronized (headwaters)
        {
            final List<Headwater> indexed = plannedByChunk.get(targetChunkKey);
            candidates = indexed == null ? List.of() : List.copyOf(indexed);
        }
        for (Headwater headwater : candidates)
        {
            final Route route = headwater.plannedRoute();
            if (route != null && route.suppresses(edge, blockX, blockZ))
            {
                return true;
            }
        }
        return false;
    }

    @Nullable
    Sample sample(RiverEdge edge, int blockX, int blockZ, double ambientHeight)
    {
        if (edge.sourceEdge())
        {
            return null;
        }
        final Headwater headwater = headwater(edge);
        final Sample sample = headwater.sample(blockX, blockZ, ambientHeight);
        indexPlanned(headwater);
        return sample;
    }

    @Nullable
    Sample sampleIfPlanned(RiverEdge edge, int blockX, int blockZ)
    {
        if (edge.sourceEdge())
        {
            return null;
        }
        final Headwater headwater = existingHeadwater(edge);
        return headwater == null ? null : headwater.sampleIfPlanned(blockX, blockZ);
    }

    @Nullable
    Sample samplePlannedAt(int blockX, int blockZ, double ambientHeight)
    {
        final List<Headwater> candidates;
        final long targetChunkKey = chunkKey(blockX >> 4, blockZ >> 4);
        synchronized (headwaters)
        {
            final List<Headwater> indexed = plannedByChunk.get(targetChunkKey);
            candidates = indexed == null ? List.of() : List.copyOf(indexed);
        }

        Sample nearest = null;
        for (Headwater headwater : candidates)
        {
            final Sample sample = headwater.sampleIfPlanned(blockX, blockZ, ambientHeight);
            if (sample != null && (nearest == null || sample.normalizedDistanceSq() < nearest.normalizedDistanceSq()))
            {
                nearest = sample;
            }
        }
        return nearest;
    }

    boolean mayInfluence(RiverEdge edge, int blockX, int blockZ)
    {
        final double distanceSqGrid = edge.fractal().intersectDistance(
            Units.blockToGridExact(blockX),
            Units.blockToGridExact(blockZ)
        );
        final double influenceGrid = (ROUTE_PADDING + 12d) / Units.GRID_WIDTH_IN_BLOCK;
        return distanceSqGrid <= influenceGrid * influenceGrid;
    }

    private Headwater headwater(RiverEdge edge)
    {
        synchronized (headwaters)
        {
            Headwater headwater = headwaters.get(edge);
            if (headwater == null)
            {
                if (headwaters.size() >= MAX_HEADWATER_CACHE_SIZE)
                {
                    headwaters.clear();
                    plannedByChunk.clear();
                }
                final int downstreamWidth = edge.drainEdge() == null ? edge.width : edge.drainEdge().width;
                headwater = new Headwater(seedFor(edge), seaLevel, downstreamWidth, heights, edge);
                headwaters.put(edge, headwater);
            }
            return headwater;
        }
    }

    @Nullable
    private Headwater existingHeadwater(RiverEdge edge)
    {
        synchronized (headwaters)
        {
            return headwaters.get(edge);
        }
    }

    private void indexPlanned(Headwater headwater)
    {
        final Route route = headwater.plannedRoute();
        if (route == null || headwater.indexed)
        {
            return;
        }
        synchronized (headwaters)
        {
            if (headwater.indexed)
            {
                return;
            }

            double minX = Double.POSITIVE_INFINITY;
            double minZ = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double maxZ = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < route.x.length; i++)
            {
                minX = Math.min(minX, route.x[i]);
                minZ = Math.min(minZ, route.z[i]);
                maxX = Math.max(maxX, route.x[i]);
                maxZ = Math.max(maxZ, route.z[i]);
            }
            final int minChunkX = Mth.floor(minX - SPATIAL_INDEX_MARGIN) >> 4;
            final int minChunkZ = Mth.floor(minZ - SPATIAL_INDEX_MARGIN) >> 4;
            final int maxChunkX = Mth.floor(maxX + SPATIAL_INDEX_MARGIN) >> 4;
            final int maxChunkZ = Mth.floor(maxZ + SPATIAL_INDEX_MARGIN) >> 4;
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++)
            {
                for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++)
                {
                    plannedByChunk.computeIfAbsent(chunkKey(chunkX, chunkZ), ignored -> new ArrayList<>()).add(headwater);
                }
            }
            headwater.indexed = true;
        }
    }

    private static long chunkKey(int chunkX, int chunkZ)
    {
        return (chunkX & 0xffffffffL) | ((chunkZ & 0xffffffffL) << 32);
    }

    private long seedFor(RiverEdge edge)
    {
        long value = seed;
        value ^= Double.doubleToLongBits(edge.source().x()) * 0x9E3779B97F4A7C15L;
        value ^= Double.doubleToLongBits(edge.source().y()) * 0xC2B2AE3D27D4EB4FL;
        value ^= Double.doubleToLongBits(edge.drain().x()) * 0x165667B19E3779F9L;
        value ^= Double.doubleToLongBits(edge.drain().y()) * 0x85EBCA77C2B2AE63L;
        return mix64(value);
    }

    static TestStream planTestStream(
        long seed,
        int seaLevel,
        double sourceX,
        double sourceZ,
        double drainX,
        double drainZ,
        int downstreamWidth,
        HeightSampler heights
    )
    {
        return new TestStream(new Headwater(
            seed,
            seaLevel,
            downstreamWidth,
            heights,
            sourceX,
            sourceZ,
            drainX,
            drainZ
        ));
    }

    static TestStream planTestSourceExtension(
        long seed,
        int seaLevel,
        double sourceX,
        double sourceZ,
        double drainX,
        double drainZ,
        int downstreamWidth,
        HeightSampler heights
    )
    {
        final Headwater headwater = new Headwater(
            seed,
            seaLevel,
            downstreamWidth,
            heights,
            sourceX,
            sourceZ,
            drainX,
            drainZ
        );
        headwater.route = headwater.planSourceExtension();
        headwater.attempted = true;
        return new TestStream(headwater);
    }

    static List<Vec> alignTestRoute(List<Vec> route, List<Vec> receiver, double receiverWidth)
    {
        final ReceiverPath receiverPath = new ReceiverPath(List.copyOf(receiver), receiverWidth);
        return Headwater.alignRoute(route, receiverPath).points();
    }

    static final class TestStream
    {
        private final Headwater headwater;

        private TestStream(Headwater headwater)
        {
            this.headwater = headwater;
        }

        boolean valid()
        {
            return headwater.route() != null;
        }

        @Nullable
        Sample sample(int blockX, int blockZ, double ambientHeight)
        {
            return headwater.sample(blockX, blockZ, ambientHeight);
        }

        List<DiagnosticPoint> points()
        {
            final Route route = headwater.route();
            return route == null ? List.of() : route.diagnostics();
        }

    }

    private static final class Headwater
    {
        private final long seed;
        private final int seaLevel;
        private final int downstreamWidth;
        private final HeightSampler heights;
        @Nullable private final RiverEdge edge;
        private final double sourceX;
        private final double sourceZ;
        private final double drainX;
        private final double drainZ;
        private final int minX;
        private final int minZ;
        private final int maxX;
        private final int maxZ;

        private volatile boolean attempted;
        @Nullable private volatile Route route;
        private volatile boolean replacement;
        private volatile boolean indexed;
        @Nullable private String failureReason;

        private Headwater(long seed, int seaLevel, int downstreamWidth, HeightSampler heights, RiverEdge edge)
        {
            this(
                seed,
                seaLevel,
                downstreamWidth,
                heights,
                edge,
                edge.source().x() * Units.GRID_WIDTH_IN_BLOCK,
                edge.source().y() * Units.GRID_WIDTH_IN_BLOCK,
                edge.drain().x() * Units.GRID_WIDTH_IN_BLOCK,
                edge.drain().y() * Units.GRID_WIDTH_IN_BLOCK
            );
        }

        private Headwater(
            long seed,
            int seaLevel,
            int downstreamWidth,
            HeightSampler heights,
            double sourceX,
            double sourceZ,
            double drainX,
            double drainZ
        )
        {
            this(seed, seaLevel, downstreamWidth, heights, null, sourceX, sourceZ, drainX, drainZ);
        }

        private Headwater(
            long seed,
            int seaLevel,
            int downstreamWidth,
            HeightSampler heights,
            @Nullable RiverEdge edge,
            double sourceX,
            double sourceZ,
            double drainX,
            double drainZ
        )
        {
            this.seed = seed;
            this.seaLevel = seaLevel;
            this.downstreamWidth = downstreamWidth;
            this.heights = heights;
            this.edge = edge;
            this.sourceX = sourceX;
            this.sourceZ = sourceZ;
            this.drainX = drainX;
            this.drainZ = drainZ;

            double minX = Math.min(sourceX, drainX);
            double minZ = Math.min(sourceZ, drainZ);
            double maxX = Math.max(sourceX, drainX);
            double maxZ = Math.max(sourceZ, drainZ);
            if (edge != null)
            {
                final double[] segments = edge.fractal().segments;
                for (int i = 0; i < segments.length; i += 2)
                {
                    final double x = segments[i] * Units.GRID_WIDTH_IN_BLOCK;
                    final double z = segments[i + 1] * Units.GRID_WIDTH_IN_BLOCK;
                    minX = Math.min(minX, x);
                    minZ = Math.min(minZ, z);
                    maxX = Math.max(maxX, x);
                    maxZ = Math.max(maxZ, z);
                }
            }
            this.minX = Mth.floor(minX) - ROUTE_PADDING;
            this.minZ = Mth.floor(minZ) - ROUTE_PADDING;
            this.maxX = Mth.ceil(maxX) + ROUTE_PADDING;
            this.maxZ = Mth.ceil(maxZ) + ROUTE_PADDING;
        }

        @Nullable
        private Sample sample(int blockX, int blockZ, double ambientHeight)
        {
            if (blockX < minX || blockX > maxX || blockZ < minZ || blockZ > maxZ)
            {
                return null;
            }
            final Route route = route();
            return route == null ? null : route.sample(blockX, blockZ, ambientHeight);
        }

        @Nullable
        private Sample sampleIfPlanned(int blockX, int blockZ)
        {
            return sampleIfPlanned(blockX, blockZ, Double.POSITIVE_INFINITY);
        }

        @Nullable
        private Sample sampleIfPlanned(int blockX, int blockZ, double ambientHeight)
        {
            if (!attempted || blockX < minX || blockX > maxX || blockZ < minZ || blockZ > maxZ)
            {
                return null;
            }
            final Route planned = route;
            return planned == null ? null : planned.sample(blockX, blockZ, ambientHeight);
        }

        @Nullable
        private Boolean replacementIfPlanned()
        {
            return attempted ? replacement : null;
        }

        private boolean hasFeederIfPlanned()
        {
            return attempted && route != null && !replacement;
        }

        private boolean retainsTfcIfPlanned()
        {
            return attempted && !replacement;
        }

        @Nullable
        private Route route()
        {
            if (!attempted)
            {
                synchronized (this)
                {
                    if (!attempted)
                    {
                        route = plan();
                        replacement = route != null;
                        if (route == null && edge != null)
                        {
                            route = planSourceExtension();
                        }
                        attempted = true;
                        if (TRACE)
                        {
                            LOGGER.info(
                                "[TFE][HeadwaterTrace] source=({}, {}) drain=({}, {}) width={} result={} points={} path={} reason={}",
                                sourceX,
                                sourceZ,
                                drainX,
                                drainZ,
                                downstreamWidth,
                                route == null ? "TFC_FALLBACK" : replacement ? "STREAM" : "TFC_WITH_FEEDER",
                                route == null ? 0 : route.x.length,
                                route == null ? "none" : route.summary(),
                                failureReason == null ? "none" : failureReason
                            );
                        }
                    }
                }
            }
            return route;
        }

        @Nullable
        private Route plannedRoute()
        {
            return attempted ? route : null;
        }

        private boolean replacesTfc()
        {
            route();
            return replacement;
        }

        private boolean hasFeeder()
        {
            final Route planned = route();
            return planned != null && !replacement;
        }

        @Nullable
        private Route plan()
        {
            return planWithSmoothing(PREFERRED_CORNER_PRECISION);
        }

        @Nullable
        private Route planWithSmoothing(int smoothingPasses)
        {
            final int minCellX = Math.floorDiv(minX, SAMPLE_STEP);
            final int minCellZ = Math.floorDiv(minZ, SAMPLE_STEP);
            final int maxCellX = Math.floorDiv(maxX, SAMPLE_STEP);
            final int maxCellZ = Math.floorDiv(maxZ, SAMPLE_STEP);
            final int width = maxCellX - minCellX + 1;
            final int depth = maxCellZ - minCellZ + 1;
            final int size = width * depth;
            if (width < 2 || depth < 2 || size > 24000)
            {
                return fail("search_bounds");
            }

            final double[] terrain = new double[size];
            for (int z = 0; z < depth; z++)
            {
                for (int x = 0; x < width; x++)
                {
                    terrain[index(x, z, width)] = heights.sample(
                        (minCellX + x) * SAMPLE_STEP,
                        (minCellZ + z) * SAMPLE_STEP
                    );
                }
            }

            final int nominalSourceX = Mth.clamp((int) Math.round(sourceX / SAMPLE_STEP) - minCellX, 0, width - 1);
            final int nominalSourceZ = Mth.clamp((int) Math.round(sourceZ / SAMPLE_STEP) - minCellZ, 0, depth - 1);
            final int start = chooseSource(nominalSourceX, nominalSourceZ, width, depth, terrain);
            final int goalX = Mth.clamp((int) Math.round(drainX / SAMPLE_STEP) - minCellX, 0, width - 1);
            final int goalZ = Mth.clamp((int) Math.round(drainZ / SAMPLE_STEP) - minCellZ, 0, depth - 1);
            final int goal = index(goalX, goalZ, width);

            final int[] parent = search(start, goal, minCellX, minCellZ, width, depth, terrain);
            if (parent == null || parent[goal] < 0)
            {
                return fail("no_route");
            }

            final List<Vec> raw = new ArrayList<>();
            int cursor = goal;
            while (cursor != -1)
            {
                final int cellX = cursor % width;
                final int cellZ = cursor / width;
                raw.add(new Vec(
                    (minCellX + cellX) * (double) SAMPLE_STEP,
                    (minCellZ + cellZ) * (double) SAMPLE_STEP
                ));
                if (cursor == start)
                {
                    break;
                }
                cursor = parent[cursor];
            }
            Collections.reverse(raw);
            if (raw.size() < 2)
            {
                return fail("short_raw_route");
            }
            raw.set(raw.size() - 1, new Vec(drainX, drainZ));

            final RiverEdge receiver = edge == null ? null : edge.drainEdge();
            final OutletRoute outlet = alignWithReceiver(
                smoothRoute(raw, smoothingPasses),
                receiver,
                downstreamWidth
            );
            if (outlet == null)
            {
                return fail("receiver_alignment");
            }
            final List<Vec> smooth = outlet.points();
            if (smooth.size() < 2)
            {
                return fail("route_entirely_inside_receiver");
            }
            final int pointCount = smooth.size();
            final double[] x = new double[pointCount];
            final double[] z = new double[pointCount];
            final double[] terrainY = new double[pointCount];
            final double[] distance = new double[pointCount];
            for (int i = 0; i < pointCount; i++)
            {
                final Vec point = smooth.get(i);
                x[i] = point.x();
                z[i] = point.z();
                terrainY[i] = heights.sample(Mth.floor(x[i]), Mth.floor(z[i]));
                if (i > 0)
                {
                    distance[i] = distance[i - 1] + Math.hypot(x[i] - x[i - 1], z[i] - z[i - 1]);
                }
            }
            final double totalLength = distance[pointCount - 1];
            if (totalLength < 96d)
            {
                return fail("route_under_96_blocks");
            }

            final double[] radius = new double[pointCount];
            final double drainRadius = Mth.clamp(outlet.receiverWidth() * 0.45d, 3.2d, 5.5d);
            for (int i = 0; i < pointCount; i++)
            {
                final double progress = distance[i] / totalLength;
                final double baseRadius = Mth.lerp(Math.pow(progress, 0.72d), SOURCE_CHANNEL_RADIUS, drainRadius);
                // Keep the terrain-planned stream width through the aligned
                // mouth. Receiver width only defines the receiver distance
                // field below and does not inflate the supplemental creek.
                radius[i] = baseRadius;
            }

            final double outletWater = seaLevel - 1d;
            final double[] capacity = new double[pointCount];
            for (int i = 0; i < pointCount; i++)
            {
                capacity[i] = Math.min(
                    terrainY[i] - CENTER_SURFACE_INSET,
                    bankCapacity(i, x, z, radius)
                );
                if (capacity[i] + 1.0e-6d < outletWater)
                {
                    return fail(String.format(
                        "route_below_outlet_water index=%d capacity=%.2f outletWater=%.2f",
                        i,
                        capacity[i],
                        outletWater
                    ));
                }
            }

            // Streams-style grade: follow the real terrain envelope while
            // never climbing downstream. A second pass caps only genuinely
            // vertical drops at one block per horizontal block, allowing
            // terraces and cascades instead of forcing a sea-level trench.
            final double[] waterY = new double[pointCount];
            waterY[0] = capacity[0];
            for (int i = 1; i < pointCount; i++)
            {
                waterY[i] = Math.max(outletWater, Math.min(waterY[i - 1], capacity[i]));
            }
            waterY[pointCount - 1] = outletWater;
            for (int i = pointCount - 2; i >= 0; i--)
            {
                final double segmentLength = Math.max(1.0e-6d, distance[i + 1] - distance[i]);
                waterY[i] = Math.min(waterY[i], waterY[i + 1] + segmentLength * MAX_CASCADE_SLOPE);
            }

            for (int i = 0; i < pointCount; i++)
            {
                final double incision = terrainY[i] - CENTER_SURFACE_INSET - waterY[i];
                final double distanceToOutlet = totalLength - distance[i];
                final double allowedIncision = distanceToOutlet <= OUTLET_ADAPTER_LENGTH
                    ? MAX_OUTLET_ADAPTER_INCISION
                    : MAX_NORMAL_INCISION;
                if (incision > allowedIncision)
                {
                    return fail(String.format(
                        "deep_incision index=%d terrain=%.2f water=%.2f incision=%.2f allowed=%.2f",
                        i,
                        terrainY[i],
                        waterY[i],
                        incision,
                        allowedIncision
                    ));
                }
            }

            return new Route(
                x,
                z,
                terrainY,
                waterY,
                radius,
                distance,
                totalLength,
                outlet.receiverMouth(),
                outlet.alignment()
            );
        }

        /**
         * If a complete leaf replacement is unsafe, retain TFC's leaf and add
         * only a narrow upstream feeder. The feeder disappears under the
         * retained TFC river influence at the join, so it cannot override the
         * main channel or recreate a suspended crossing over the unsafe area.
         */
        @Nullable
        private Route planSourceExtension()
        {
            final int minCellX = Math.floorDiv(minX, SAMPLE_STEP);
            final int minCellZ = Math.floorDiv(minZ, SAMPLE_STEP);
            final int maxCellX = Math.floorDiv(maxX, SAMPLE_STEP);
            final int maxCellZ = Math.floorDiv(maxZ, SAMPLE_STEP);
            final int width = maxCellX - minCellX + 1;
            final int depth = maxCellZ - minCellZ + 1;
            final int size = width * depth;
            if (width < 2 || depth < 2 || size > 24000)
            {
                return failFeeder("search_bounds");
            }

            final double[] terrain = new double[size];
            for (int z = 0; z < depth; z++)
            {
                for (int x = 0; x < width; x++)
                {
                    terrain[index(x, z, width)] = heights.sample(
                        (minCellX + x) * SAMPLE_STEP,
                        (minCellZ + z) * SAMPLE_STEP
                    );
                }
            }

            final int nominalSourceX = Mth.clamp((int) Math.round(sourceX / SAMPLE_STEP) - minCellX, 0, width - 1);
            final int nominalSourceZ = Mth.clamp((int) Math.round(sourceZ / SAMPLE_STEP) - minCellZ, 0, depth - 1);
            final int start = chooseSource(nominalSourceX, nominalSourceZ, width, depth, terrain);
            final int goal = index(nominalSourceX, nominalSourceZ, width);
            final int[] parent = search(start, goal, minCellX, minCellZ, width, depth, terrain);
            if (parent == null || parent[goal] < 0)
            {
                return failFeeder("no_route");
            }

            final List<Vec> raw = new ArrayList<>();
            int cursor = goal;
            while (cursor != -1)
            {
                final int cellX = cursor % width;
                final int cellZ = cursor / width;
                raw.add(new Vec(
                    (minCellX + cellX) * (double) SAMPLE_STEP,
                    (minCellZ + cellZ) * (double) SAMPLE_STEP
                ));
                if (cursor == start)
                {
                    break;
                }
                cursor = parent[cursor];
            }
            Collections.reverse(raw);
            if (raw.size() < 2)
            {
                return failFeeder("short_raw_route");
            }
            raw.set(raw.size() - 1, new Vec(sourceX, sourceZ));

            // A feeder is a conservative fallback for a retained TFC leaf. Its
            // physical channel ends at the first contact with the retained TFC
            // water core; the remaining graph overlap belongs to TFC itself.
            final OutletRoute outlet = clipAtReceiver(
                smoothRoute(raw, 1),
                edge,
                FEEDER_RECEIVER_WIDTH_SCALE,
                downstreamWidth
            );
            final List<Vec> smooth = outlet.points();
            if (smooth.size() < 2)
            {
                return failFeeder("route_entirely_inside_receiver");
            }
            final int pointCount = smooth.size();
            final double[] x = new double[pointCount];
            final double[] z = new double[pointCount];
            final double[] terrainY = new double[pointCount];
            final double[] distance = new double[pointCount];
            for (int i = 0; i < pointCount; i++)
            {
                final Vec point = smooth.get(i);
                x[i] = point.x();
                z[i] = point.z();
                terrainY[i] = heights.sample(Mth.floor(x[i]), Mth.floor(z[i]));
                if (i > 0)
                {
                    distance[i] = distance[i - 1] + Math.hypot(x[i] - x[i - 1], z[i] - z[i - 1]);
                }
            }
            final double totalLength = distance[pointCount - 1];
            if (totalLength < SOURCE_EXTENSION_MIN - SAMPLE_STEP)
            {
                return failFeeder("route_under_source_extension_minimum");
            }

            final double[] radius = new double[pointCount];
            final double joinRadius = Mth.clamp(downstreamWidth * 0.18d, 1.4d, 2.0d);
            for (int i = 0; i < pointCount; i++)
            {
                final double progress = distance[i] / totalLength;
                radius[i] = Mth.lerp(Math.pow(progress, 0.72d), SOURCE_CHANNEL_RADIUS, joinRadius);
            }

            final double minimumWater = seaLevel - 1d;
            final double[] capacity = new double[pointCount];
            for (int i = 0; i < pointCount; i++)
            {
                capacity[i] = Math.min(terrainY[i] - CENTER_SURFACE_INSET, bankCapacity(i, x, z, radius));
                if (capacity[i] + 1.0e-6d < minimumWater)
                {
                    return failFeeder(String.format(
                        "below_minimum_water index=%d capacity=%.2f minimum=%.2f",
                        i,
                        capacity[i],
                        minimumWater
                    ));
                }
            }

            final double[] waterY = new double[pointCount];
            waterY[0] = capacity[0];
            for (int i = 1; i < pointCount; i++)
            {
                waterY[i] = Math.max(minimumWater, Math.min(waterY[i - 1], capacity[i]));
            }
            for (int i = pointCount - 2; i >= 0; i--)
            {
                final double segmentLength = Math.max(1.0e-6d, distance[i + 1] - distance[i]);
                waterY[i] = Math.min(waterY[i], waterY[i + 1] + segmentLength * MAX_CASCADE_SLOPE);
            }
            for (int i = 0; i < pointCount; i++)
            {
                final double incision = terrainY[i] - CENTER_SURFACE_INSET - waterY[i];
                if (incision > MAX_FEEDER_INCISION)
                {
                    return failFeeder(String.format(
                        "deep_incision index=%d terrain=%.2f water=%.2f incision=%.2f",
                        i,
                        terrainY[i],
                        waterY[i],
                        incision
                    ));
                }
            }
            return new Route(
                x,
                z,
                terrainY,
                waterY,
                radius,
                distance,
                totalLength,
                outlet.receiverMouth(),
                null
            );
        }

        @Nullable
        private Route fail(String reason)
        {
            failureReason = reason;
            return null;
        }

        @Nullable
        private Route failFeeder(String reason)
        {
            failureReason = (failureReason == null ? "primary=unknown" : "primary=" + failureReason)
                + "; feeder=" + reason;
            return null;
        }

        @Nullable
        private static OutletRoute alignWithReceiver(
            List<Vec> route,
            @Nullable RiverEdge receiver,
            double fallbackWidth
        )
        {
            if (receiver == null)
            {
                return new OutletRoute(route, false, fallbackWidth, null);
            }
            if (route.size() < 3)
            {
                return null;
            }

            final ReceiverPath receiverPath = receiverPath(receiver);
            final OutletRoute aligned = alignRoute(route, receiverPath);
            if (aligned.alignment() == null)
            {
                return null;
            }
            final ReceiverAlignment geometry = aligned.alignment();
            final Vec anchor = aligned.points().get(aligned.points().size() - 1);
            final double receiverWidth = receiverWidthAt(receiver, anchor);
            return new OutletRoute(
                aligned.points(),
                true,
                receiverWidth,
                new ReceiverAlignment(
                    receiver,
                    geometry.receiverPath,
                    geometry.sourceAlong,
                    geometry.anchorAlong,
                    receiverWidth
                )
            );
        }

        private static OutletRoute alignRoute(List<Vec> route, ReceiverPath receiverPath)
        {
            if (receiverPath.points().size() < 2 || route.size() < 3)
            {
                return new OutletRoute(route, false, receiverPath.width(), null);
            }

            final int rewriteStart = findRewriteStart(route, SOURCE_ALIGNMENT_REWRITE_LENGTH);
            final List<Vec> prefix = new ArrayList<>(route.subList(0, rewriteStart + 1));
            final Vec start = prefix.get(prefix.size() - 1);
            final Vec incoming = normalizedDirection(route.get(Math.max(0, rewriteStart - 2)), route.get(rewriteStart));
            if (vectorLength(incoming) < 1.0e-6d)
            {
                return new OutletRoute(route, false, receiverPath.width(), null);
            }

            final double nearestAlong = nearestAlong(receiverPath.points(), start);
            final double anchorAlong = Mth.clamp(
                nearestAlong + SOURCE_ALIGNMENT_LENGTH,
                0d,
                polylineLength(receiverPath.points())
            );
            final Vec anchor = pointAlong(receiverPath.points(), anchorAlong);
            final Vec receiverDirection = directionAlong(receiverPath.points(), anchorAlong);
            final double directLength = distance(start, anchor);
            if (directLength < SOURCE_ALIGNMENT_LENGTH * 0.45d || vectorLength(receiverDirection) < 1.0e-6d)
            {
                return new OutletRoute(route, false, receiverPath.width(), null);
            }

            final double tangentLength = Math.min(SOURCE_ALIGNMENT_LENGTH * 0.72d, directLength * 0.62d);
            final Vec startControl = add(start, scale(incoming, tangentLength));
            final Vec endControl = add(anchor, scale(receiverDirection, -tangentLength));
            final int steps = Math.max(8, Mth.ceil(directLength / SOURCE_ALIGNMENT_SAMPLE_SPACING));
            for (int step = 1; step <= steps; step++)
            {
                addIfDifferent(prefix, cubicBezier(
                    start,
                    startControl,
                    endControl,
                    anchor,
                    step / (double) steps
                ));
            }

            final ReceiverAlignment alignment = new ReceiverAlignment(
                null,
                List.copyOf(receiverPath.points()),
                nearestAlong,
                anchorAlong,
                receiverPath.width()
            );
            return new OutletRoute(List.copyOf(prefix), true, receiverPath.width(), alignment);
        }

        private static OutletRoute clipAtReceiver(
            List<Vec> route,
            @Nullable RiverEdge receiver,
            double receiverWidthScale,
            double fallbackWidth
        )
        {
            if (receiver == null || route.size() < 2)
            {
                return new OutletRoute(route, false, fallbackWidth, null);
            }

            final double receiverCoreRadiusSq = NTERiverHydrology.TFC_WATER_CORE_RADIUS_SQ
                * receiverWidthScale * receiverWidthScale;
            final List<Vec> clipped = new ArrayList<>(route.size());
            Vec previous = route.get(0);
            double previousDistance = receiverDistanceSq(receiver, previous);
            clipped.add(previous);
            for (int i = 1; i < route.size(); i++)
            {
                final Vec current = route.get(i);
                final double currentDistance = receiverDistanceSq(receiver, current);
                if (currentDistance <= receiverCoreRadiusSq)
                {
                    if (previousDistance > receiverCoreRadiusSq)
                    {
                        double outside = 0d;
                        double inside = 1d;
                        for (int iteration = 0; iteration < 12; iteration++)
                        {
                            final double delta = (outside + inside) * 0.5d;
                            final Vec candidate = lerp(previous, current, delta);
                            if (receiverDistanceSq(receiver, candidate) <= receiverCoreRadiusSq)
                            {
                                inside = delta;
                            }
                            else
                            {
                                outside = delta;
                            }
                        }
                        clipped.add(lerp(previous, current, inside));
                    }
                    return new OutletRoute(
                        List.copyOf(clipped),
                        true,
                        receiverWidthAt(receiver, current),
                        null
                    );
                }
                clipped.add(current);
                previous = current;
                previousDistance = currentDistance;
            }
            return new OutletRoute(route, false, fallbackWidth, null);
        }

        private static ReceiverPath receiverPath(RiverEdge receiver)
        {
            final double[] segments = receiver.fractal().segments;
            final List<Vec> points = new ArrayList<>(segments.length / 2);
            for (int i = 0; i < segments.length; i += 2)
            {
                points.add(new Vec(
                    segments[i] * Units.GRID_WIDTH_IN_BLOCK,
                    segments[i + 1] * Units.GRID_WIDTH_IN_BLOCK
                ));
            }
            final double width = points.isEmpty() ? receiver.width : receiverWidthAt(receiver, points.get(0));
            return new ReceiverPath(List.copyOf(points), width);
        }

        private static double receiverWidthAt(RiverEdge receiver, Vec point)
        {
            return Math.sqrt(receiver.widthSq(
                Units.blockToGridExact(point.x()),
                Units.blockToGridExact(point.z())
            ));
        }

        private static double receiverDistanceSq(RiverEdge receiver, Vec point)
        {
            final double gridX = Units.blockToGridExact(point.x());
            final double gridZ = Units.blockToGridExact(point.z());
            final double distanceSqBlocks = receiver.fractal().intersectDistance(gridX, gridZ)
                * Units.GRID_WIDTH_IN_BLOCK * Units.GRID_WIDTH_IN_BLOCK;
            return distanceSqBlocks / receiver.widthSq(gridX, gridZ);
        }

        private int chooseSource(int nominalX, int nominalZ, int width, int depth, double[] terrain)
        {
            final double downstreamX = drainX - sourceX;
            final double downstreamZ = drainZ - sourceZ;
            final double downstreamLength = Math.hypot(downstreamX, downstreamZ);
            if (downstreamLength < 1.0e-6d)
            {
                return chooseNearbySource(nominalX, nominalZ, width, depth, terrain);
            }

            // Extend beyond TFC's nominal leaf source. This turns the old
            // several-block-wide endpoint into a confluence and gives the new
            // terrain-following creek room to taper to a one-block spring.
            final double upstreamX = -downstreamX / downstreamLength;
            final double upstreamZ = -downstreamZ / downstreamLength;
            int best = -1;
            double bestScore = Double.NEGATIVE_INFINITY;
            for (int z = 1; z < depth - 1; z++)
            {
                for (int x = 1; x < width - 1; x++)
                {
                    final double deltaX = (x - nominalX) * (double) SAMPLE_STEP;
                    final double deltaZ = (z - nominalZ) * (double) SAMPLE_STEP;
                    final double along = deltaX * upstreamX + deltaZ * upstreamZ;
                    if (along < SOURCE_EXTENSION_MIN || along > SOURCE_EXTENSION_MAX)
                    {
                        continue;
                    }
                    final double lateral = Math.abs(deltaX * upstreamZ - deltaZ * upstreamX);
                    if (lateral > SOURCE_EXTENSION_LATERAL)
                    {
                        continue;
                    }

                    final int candidate = index(x, z, width);
                    if (terrain[candidate] < seaLevel - 1d + CENTER_SURFACE_INSET)
                    {
                        continue;
                    }
                    final double valleyBonus = valleyBonus(x, z, width, terrain);
                    final double score = terrain[candidate]
                        + valleyBonus * 2.2d
                        + along * 0.012d
                        - lateral * 0.035d;
                    if (score > bestScore)
                    {
                        bestScore = score;
                        best = candidate;
                    }
                }
            }
            return best >= 0 ? best : chooseNearbySource(nominalX, nominalZ, width, depth, terrain);
        }

        private int chooseNearbySource(int nominalX, int nominalZ, int width, int depth, double[] terrain)
        {
            int best = index(nominalX, nominalZ, width);
            double bestScore = Double.NEGATIVE_INFINITY;
            for (int dz = -4; dz <= 4; dz++)
            {
                for (int dx = -4; dx <= 4; dx++)
                {
                    final int x = nominalX + dx;
                    final int z = nominalZ + dz;
                    if (x <= 0 || z <= 0 || x >= width - 1 || z >= depth - 1)
                    {
                        continue;
                    }
                    final int candidate = index(x, z, width);
                    final double score = terrain[candidate]
                        + valleyBonus(x, z, width, terrain) * 2.2d
                        - Math.hypot(dx, dz) * 0.35d;
                    if (score > bestScore)
                    {
                        bestScore = score;
                        best = candidate;
                    }
                }
            }
            return best;
        }

        private static double valleyBonus(int x, int z, int width, double[] terrain)
        {
            double neighborAverage = 0d;
            int count = 0;
            for (int nz = -1; nz <= 1; nz++)
            {
                for (int nx = -1; nx <= 1; nx++)
                {
                    if (nx != 0 || nz != 0)
                    {
                        neighborAverage += terrain[index(x + nx, z + nz, width)];
                        count++;
                    }
                }
            }
            return Math.max(0d, neighborAverage / count - terrain[index(x, z, width)]);
        }

        @Nullable
        private int[] search(
            int start,
            int goal,
            int minCellX,
            int minCellZ,
            int width,
            int depth,
            double[] terrain
        )
        {
            final int size = width * depth;
            final double[] cost = new double[size];
            final int[] parent = new int[size];
            final boolean[] closed = new boolean[size];
            Arrays.fill(cost, Double.POSITIVE_INFINITY);
            Arrays.fill(parent, -1);

            final int startX = start % width;
            final int startZ = start / width;
            final int goalX = goal % width;
            final int goalZ = goal / width;
            final double directDistance = Math.max(1d, Math.hypot(goalX - startX, goalZ - startZ));
            final double sourceHeight = terrain[start];
            final double goalHeight = terrain[goal];
            final PriorityQueue<SearchNode> open = new PriorityQueue<>(Comparator.comparingDouble(SearchNode::score));
            cost[start] = 0d;
            open.add(new SearchNode(start, heuristic(startX, startZ, goalX, goalZ)));

            final int[] directions = {-1, -1, 0, -1, 1, -1, -1, 0, 1, 0, -1, 1, 0, 1, 1, 1};
            while (!open.isEmpty())
            {
                final SearchNode node = open.poll();
                final int current = node.index();
                if (closed[current])
                {
                    continue;
                }
                closed[current] = true;
                if (current == goal)
                {
                    return parent;
                }

                final int currentX = current % width;
                final int currentZ = current / width;
                for (int i = 0; i < directions.length; i += 2)
                {
                    final int nextX = currentX + directions[i];
                    final int nextZ = currentZ + directions[i + 1];
                    if (nextX < 0 || nextZ < 0 || nextX >= width || nextZ >= depth)
                    {
                        continue;
                    }
                    final int next = index(nextX, nextZ, width);
                    if (closed[next])
                    {
                        continue;
                    }
                    if (next != goal && terrain[next] < seaLevel - 1d + CENTER_SURFACE_INSET)
                    {
                        continue;
                    }

                    final double step = directions[i] == 0 || directions[i + 1] == 0 ? 1d : Math.sqrt(2d);
                    final double deltaHeight = terrain[next] - terrain[current];
                    final double uphillDownstream = Math.max(0d, deltaHeight);
                    final double progress = Mth.clamp(
                        1d - Math.hypot(goalX - nextX, goalZ - nextZ) / directDistance,
                        0d,
                        1d
                    );
                    final double expectedHeight = Mth.lerp(progress, sourceHeight, goalHeight);
                    final double highGround = Math.max(0d, terrain[next] - expectedHeight);
                    final double blockX = (minCellX + nextX) * (double) SAMPLE_STEP;
                    final double blockZ = (minCellZ + nextZ) * (double) SAMPLE_STEP;
                    final double corridor = Math.max(0d, corridorDistance(blockX, blockZ) - 24d) / 64d;
                    final double meander = meanderCost(blockX, blockZ);
                    final double nextCost = cost[current] + step * (
                        1d
                            + uphillDownstream * 26d
                            + Math.abs(deltaHeight) * 0.35d
                            + highGround * 0.65d
                            + corridor * corridor * 0.9d
                            + meander
                    );
                    if (nextCost < cost[next])
                    {
                        cost[next] = nextCost;
                        parent[next] = current;
                        open.add(new SearchNode(next, nextCost + heuristic(nextX, nextZ, goalX, goalZ)));
                    }
                }
            }
            return null;
        }

        private double corridorDistance(double x, double z)
        {
            if (edge != null)
            {
                return Math.sqrt(edge.fractal().intersectDistance(
                    x / Units.GRID_WIDTH_IN_BLOCK,
                    z / Units.GRID_WIDTH_IN_BLOCK
                )) * Units.GRID_WIDTH_IN_BLOCK;
            }
            return Math.sqrt(project(sourceX, sourceZ, drainX, drainZ, x, z).distanceSq());
        }

        private double meanderCost(double x, double z)
        {
            final double phaseX = (seed & 0xffffL) * 0.0017d;
            final double phaseZ = ((seed >>> 16) & 0xffffL) * 0.0013d;
            final double wave = Math.sin(x * 0.024d + phaseX) * Math.sin(z * 0.021d - phaseZ);
            return 0.32d * (1d + wave);
        }

        private double bankCapacity(int index, double[] x, double[] z, double[] radius)
        {
            final int before = Math.max(0, index - 1);
            final int after = Math.min(x.length - 1, index + 1);
            final double dx = x[after] - x[before];
            final double dz = z[after] - z[before];
            final double length = Math.hypot(dx, dz);
            if (length < 1.0e-6d)
            {
                return Double.NEGATIVE_INFINITY;
            }
            final double offset = radius[index] * 0.9d + 0.75d;
            final double nx = -dz / length * offset;
            final double nz = dx / length * offset;
            final double left = heights.sample(Mth.floor(x[index] + nx), Mth.floor(z[index] + nz));
            final double right = heights.sample(Mth.floor(x[index] - nx), Mth.floor(z[index] - nz));
            return Math.min(left, right) - BANK_FREEBOARD;
        }
    }

    private static final class Route
    {
        private final double[] x;
        private final double[] z;
        private final double[] terrainY;
        private final double[] waterY;
        private final double[] radius;
        private final double[] distance;
        private final double totalLength;
        private final boolean receiverMouth;
        @Nullable private final ReceiverAlignment receiverAlignment;
        private final Set<Long> turnConnectorColumns;

        private Route(
            double[] x,
            double[] z,
            double[] terrainY,
            double[] waterY,
            double[] radius,
            double[] distance,
            double totalLength,
            boolean receiverMouth,
            @Nullable ReceiverAlignment receiverAlignment
        )
        {
            this.x = x;
            this.z = z;
            this.terrainY = terrainY;
            this.waterY = waterY;
            this.radius = radius;
            this.distance = distance;
            this.totalLength = totalLength;
            this.receiverMouth = receiverMouth;
            this.receiverAlignment = receiverAlignment;
            this.turnConnectorColumns = rasterizeTurnConnectors(x, z);
        }

        private boolean suppresses(RiverEdge edge, int blockX, int blockZ)
        {
            return receiverAlignment != null && receiverAlignment.suppresses(edge, blockX, blockZ);
        }

        @Nullable
        private Sample sample(int blockX, int blockZ, double ambientHeight)
        {
            double bestDistanceSq = Double.POSITIVE_INFINITY;
            int bestIndex = -1;
            double bestDelta = 0d;
            for (int i = 0; i < x.length - 1; i++)
            {
                final Projection projection = project(x[i], z[i], x[i + 1], z[i + 1], blockX, blockZ);
                if (projection.distanceSq() < bestDistanceSq)
                {
                    bestDistanceSq = projection.distanceSq();
                    bestIndex = i;
                    bestDelta = projection.delta();
                }
            }
            if (bestIndex < 0)
            {
                return null;
            }

            final double localRadius = Mth.lerp(bestDelta, radius[bestIndex], radius[bestIndex + 1]);
            final double rawNormalizedDistanceSq = bestDistanceSq / (localRadius * localRadius);
            final double localWaterY = Mth.lerp(bestDelta, waterY[bestIndex], waterY[bestIndex + 1]);
            final double dx = x[bestIndex + 1] - x[bestIndex];
            final double dz = z[bestIndex + 1] - z[bestIndex];
            final double angle = Mth.atan2(-dz, dx);
            final double along = Mth.lerp(bestDelta, distance[bestIndex], distance[bestIndex + 1]);
            final double downstreamWaterY = sampleWaterYAtAlong(along + 1d);
            final double downstreamWaterDrop = Math.max(0d, localWaterY - downstreamWaterY);
            final double distanceToOutlet = totalLength - along;
            if (receiverAlignment != null && extendsPastOutlet(blockX, blockZ))
            {
                traceTargetSample(blockX, blockZ, ambientHeight, localWaterY, rawNormalizedDistanceSq, Double.POSITIVE_INFINITY, false, "past_outlet");
                return null;
            }
            final double mouthNormalizedDistanceSq = receiverAlignment == null
                ? rawNormalizedDistanceSq
                : receiverAlignment.mouthNormalizedDistanceSq(rawNormalizedDistanceSq, blockX, blockZ, distanceToOutlet);
            // Do not four-connect every diagonal step. Only a short diagonal
            // transition between perpendicular cardinal runs receives its
            // inside-corner cells, so a real bend stays connected without
            // turning an entire one-block diagonal creek into a two-block cut.
            // The aligned mouth uses its receiver-aware distance field, so
            // connector cells must not override that shape.
            final boolean turnConnector = (receiverAlignment == null || distanceToOutlet >= MOUTH_FAN_LENGTH)
                && turnConnectorColumns.contains(columnKey(blockX, blockZ));
            final double normalizedDistanceSq = turnConnector
                ? Math.min(mouthNormalizedDistanceSq, NTERiverHydrology.SUPPLEMENTAL_WATER_CORE_RADIUS_SQ * 0.95d)
                : mouthNormalizedDistanceSq;
            if (normalizedDistanceSq > MAX_INFLUENCE_SQ)
            {
                traceTargetSample(blockX, blockZ, ambientHeight, Double.NaN, rawNormalizedDistanceSq, normalizedDistanceSq, turnConnector, "outside");
                return null;
            }
            if (Double.isFinite(ambientHeight)
                && normalizedDistanceSq <= 1.2d
                && ambientHeight < localWaterY + 0.75d)
            {
                traceTargetSample(blockX, blockZ, ambientHeight, localWaterY, rawNormalizedDistanceSq, normalizedDistanceSq, turnConnector, "low_ambient");
                return null;
            }

            final double terrainCutLength = receiverAlignment == null
                ? alignedMouthWaterCutLength(localRadius)
                : receiverAlignment.terrainCutLength(localRadius);
            final double waterCutLength = receiverAlignment == null
                ? terrainCutLength
                : receiverAlignment.waterCutLength(localRadius);
            final boolean fillAllowed = !receiverMouth || distanceToOutlet > terrainCutLength;
            final boolean waterAllowed = !receiverMouth || distanceToOutlet > waterCutLength;
            final double waterCoreRadiusSq = receiverAlignment == null
                ? NTERiverHydrology.SUPPLEMENTAL_WATER_CORE_RADIUS_SQ
                : mouthWaterCoreRadiusSq(distanceToOutlet);
            final double mouthWaterDrop = receiverAlignment == null
                ? 0d
                : mouthWaterDrop(distanceToOutlet, localWaterY, waterY[waterY.length - 1]);
            final double downstreamDistanceToOutlet = Math.max(0d, totalLength - (along + 1d));
            final double downstreamMouthWaterDrop = receiverAlignment == null
                ? 0d
                : mouthWaterDrop(
                    downstreamDistanceToOutlet,
                    downstreamWaterY,
                    waterY[waterY.length - 1]
                );
            final double secondDownstreamWaterY = sampleWaterYAtAlong(along + 2d);
            final double secondDownstreamDistanceToOutlet = Math.max(0d, totalLength - (along + 2d));
            final double secondDownstreamMouthWaterDrop = receiverAlignment == null
                ? 0d
                : mouthWaterDrop(
                    secondDownstreamDistanceToOutlet,
                    secondDownstreamWaterY,
                    waterY[waterY.length - 1]
                );
            final double plannedWaterY = localWaterY - mouthWaterDrop;
            final double downstreamPlannedWaterY = downstreamWaterY - downstreamMouthWaterDrop;
            final double secondDownstreamPlannedWaterY = secondDownstreamWaterY - secondDownstreamMouthWaterDrop;
            final double effectiveDownstreamWaterDrop = Math.max(
                0d,
                plannedWaterY - downstreamPlannedWaterY
            );
            final double upstreamAlong = Math.max(0d, along - 1d);
            final double upstreamWaterY = sampleWaterYAtAlong(upstreamAlong);
            // The small fan descent is an artificial profile adaptation, not
            // a terrain waterfall. Treating every quantized fan step as a
            // landing made generation-time spill baking restore the upper
            // water layer which the profile had deliberately lowered. Real
            // route drops still pre-bake their complete waterfalls.
            final boolean waterfallLanding = naturalWaterfallLanding(upstreamWaterY, localWaterY);
            final double extraIncision = receiverAlignment == null
                ? 0d
                : mouthWaterDrop * mouthFanLateralWeight(normalizedDistanceSq);
            final double bankFillWeight = receiverAlignment == null
                ? 1d
                : mouthBankFillWeight(distanceToOutlet);
            final double receiverBlendWeight = receiverAlignment == null
                ? 0d
                : mouthReceiverBlendWeight(distanceToOutlet);
            final boolean sourceWaterAllowed = waterAllowed
                && plannedSourceWaterAllowed(effectiveDownstreamWaterDrop)
                && plannedSourceWaterAllowed(plannedWaterY, downstreamPlannedWaterY, secondDownstreamPlannedWaterY)
                && !waterfallLanding
                && (receiverAlignment == null
                    || mouthSourceWaterAllowed(distanceToOutlet, waterCutLength, mouthWaterDrop));
            traceTargetSample(blockX, blockZ, ambientHeight, localWaterY, rawNormalizedDistanceSq, normalizedDistanceSq, turnConnector, "accepted");
            return new Sample(
                localWaterY,
                normalizedDistanceSq,
                localRadius,
                extraIncision,
                mouthWaterDrop,
                bankFillWeight,
                waterCoreRadiusSq,
                fillAllowed,
                waterAllowed,
                sourceWaterAllowed,
                receiverBlendWeight,
                waterfallLanding,
                along / totalLength < 0.15d,
                Flow.fromAngle(angle)
            );
        }

        private double sampleWaterYAtAlong(double targetAlong)
        {
            if (targetAlong <= 0d)
            {
                return waterY[0];
            }
            if (targetAlong >= totalLength)
            {
                return waterY[waterY.length - 1];
            }
            int upper = 1;
            while (upper < distance.length && distance[upper] < targetAlong)
            {
                upper++;
            }
            final int lower = upper - 1;
            final double segmentLength = distance[upper] - distance[lower];
            final double delta = segmentLength <= 1.0e-9d
                ? 0d
                : (targetAlong - distance[lower]) / segmentLength;
            return Mth.lerp(delta, waterY[lower], waterY[upper]);
        }

        private boolean extendsPastOutlet(int blockX, int blockZ)
        {
            final int last = x.length - 1;
            final double dx = x[last] - x[last - 1];
            final double dz = z[last] - z[last - 1];
            final double length = Math.hypot(dx, dz);
            if (length <= 1.0e-9d)
            {
                return false;
            }
            final double forward = ((blockX - x[last]) * dx + (blockZ - z[last]) * dz) / length;
            return forward > 0.5d;
        }

        private static void traceTargetSample(
            int blockX,
            int blockZ,
            double ambientHeight,
            double waterY,
            double rawNormalizedDistanceSq,
            double normalizedDistanceSq,
            boolean turnConnector,
            String result
        )
        {
            if (TRACE
                && blockX == Integer.getInteger("tfe.debug.traceX", Integer.MIN_VALUE)
                && blockZ == Integer.getInteger("tfe.debug.traceZ", Integer.MIN_VALUE))
            {
                LOGGER.info(
                    "[TFE][HeadwaterTargetSample] x={} z={} ambient={} water={} rawNorm={} norm={} connector={} result={}",
                    blockX,
                    blockZ,
                    ambientHeight,
                    waterY,
                    rawNormalizedDistanceSq,
                    normalizedDistanceSq,
                    turnConnector,
                    result
                );
            }
        }

        private List<DiagnosticPoint> diagnostics()
        {
            final List<DiagnosticPoint> points = new ArrayList<>(x.length);
            for (int i = 0; i < x.length; i++)
            {
                points.add(new DiagnosticPoint(x[i], z[i], terrainY[i], waterY[i], radius[i]));
            }
            return List.copyOf(points);
        }

        private String summary()
        {
            final int middle = x.length / 2;
            return String.format(
                "start=(%.1f,%.1f,water=%.1f) middle=(%.1f,%.1f,water=%.1f) end=(%.1f,%.1f,water=%.1f) turnConnectors=%d%s",
                x[0], z[0], waterY[0],
                x[middle], z[middle], waterY[middle],
                x[x.length - 1], z[z.length - 1], waterY[waterY.length - 1],
                turnConnectorColumns.size(),
                tracedGeometry()
            );
        }

        private String tracedGeometry()
        {
            if (!TRACE)
            {
                return "";
            }
            final int targetX = Integer.getInteger("tfe.debug.traceX", Integer.MIN_VALUE);
            final int targetZ = Integer.getInteger("tfe.debug.traceZ", Integer.MIN_VALUE);
            final List<String> nearby = new ArrayList<>();
            for (int i = 0; i < x.length; i++)
            {
                if (Math.abs(x[i] - targetX) <= 6d && Math.abs(z[i] - targetZ) <= 6d)
                {
                    nearby.add(String.format("%.1f/%.1f", x[i], z[i]));
                }
            }
            if (nearby.isEmpty())
            {
                return "";
            }
            final List<String> connectors = new ArrayList<>();
            for (long key : turnConnectorColumns)
            {
                final int connectorX = (int) key;
                final int connectorZ = (int) (key >>> 32);
                if (Math.abs(connectorX - targetX) <= 6 && Math.abs(connectorZ - targetZ) <= 6)
                {
                    connectors.add(connectorX + "/" + connectorZ);
                }
            }
            return " nearby=" + nearby + " nearbyConnectors=" + connectors;
        }
    }

    private static final class ReceiverAlignment
    {
        @Nullable private final RiverEdge receiver;
        private final List<Vec> receiverPath;
        private final double sourceAlong;
        private final double anchorAlong;
        private final double receiverWidth;

        private ReceiverAlignment(
            @Nullable RiverEdge receiver,
            List<Vec> receiverPath,
            double sourceAlong,
            double anchorAlong,
            double receiverWidth
        )
        {
            this.receiver = receiver;
            this.receiverPath = receiverPath;
            this.sourceAlong = sourceAlong;
            this.anchorAlong = anchorAlong;
            this.receiverWidth = receiverWidth;
        }

        private double terrainCutLength(double localRadius)
        {
            return alignedMouthCutLength(localRadius);
        }

        private double waterCutLength(double localRadius)
        {
            return alignedMouthWaterCutLength(localRadius);
        }

        private double mouthNormalizedDistanceSq(
            double streamNormalizedDistanceSq,
            int blockX,
            int blockZ,
            double distanceToOutlet
        )
        {
            final double receiverRadius = matchedReceiverRadius(receiverWidth);
            final double receiverNormalizedDistanceSq = distanceSqToPolyline(
                receiverPath,
                new Vec(blockX, blockZ)
            ) / (receiverRadius * receiverRadius);
            return mouthGeometryNormalizedDistanceSq(
                streamNormalizedDistanceSq,
                receiverNormalizedDistanceSq,
                distanceToOutlet
            );
        }

        private boolean suppresses(RiverEdge edge, int blockX, int blockZ)
        {
            if (receiver == null || edge != receiver)
            {
                return false;
            }
            final Vec point = new Vec(blockX, blockZ);
            final double along = nearestAlong(receiverPath, point);
            if (along < sourceAlong - receiverWidth || along > anchorAlong - receiverWidth * 0.35d)
            {
                return false;
            }
            final double distanceSq = distanceSqToPolyline(receiverPath, point);
            final double suppressionRadius = Math.max(
                4d,
                receiverWidth * SOURCE_ALIGNMENT_SUPPRESSION_WIDTH_SCALE
            );
            return distanceSq <= suppressionRadius * suppressionRadius;
        }
    }

    private static Set<Long> rasterizeTurnConnectors(double[] x, double[] z)
    {
        final List<GridCell> cells = new ArrayList<>();
        for (int segment = 0; segment < x.length - 1; segment++)
        {
            final double dx = x[segment + 1] - x[segment];
            final double dz = z[segment + 1] - z[segment];
            final int steps = Math.max(1, Mth.ceil(Math.max(Math.abs(dx), Math.abs(dz)) * 2d));
            for (int step = segment == 0 ? 0 : 1; step <= steps; step++)
            {
                final double delta = step / (double) steps;
                final GridCell cell = new GridCell(
                    Mth.floor(Mth.lerp(delta, x[segment], x[segment + 1]) + 0.5d),
                    Mth.floor(Mth.lerp(delta, z[segment], z[segment + 1]) + 0.5d)
                );
                if (cells.isEmpty() || !cells.get(cells.size() - 1).equals(cell))
                {
                    cells.add(cell);
                }
            }
        }

        final Set<Long> connectors = new HashSet<>();
        int edge = 0;
        while (edge < cells.size() - 1)
        {
            final int dx = cells.get(edge + 1).x() - cells.get(edge).x();
            final int dz = cells.get(edge + 1).z() - cells.get(edge).z();
            if (!isDiagonalUnitStep(dx, dz))
            {
                edge++;
                continue;
            }

            final int diagonalStart = edge;
            while (edge < cells.size() - 1)
            {
                final int runDx = cells.get(edge + 1).x() - cells.get(edge).x();
                final int runDz = cells.get(edge + 1).z() - cells.get(edge).z();
                if (!isDiagonalUnitStep(runDx, runDz))
                {
                    break;
                }
                edge++;
            }

            final int diagonalCount = edge - diagonalStart;
            if (diagonalStart == 0 || edge >= cells.size() - 1 || diagonalCount > 4)
            {
                continue;
            }

            final int beforeDx = cells.get(diagonalStart).x() - cells.get(diagonalStart - 1).x();
            final int beforeDz = cells.get(diagonalStart).z() - cells.get(diagonalStart - 1).z();
            final int afterDx = cells.get(edge + 1).x() - cells.get(edge).x();
            final int afterDz = cells.get(edge + 1).z() - cells.get(edge).z();
            if (!isCardinalUnitStep(beforeDx, beforeDz)
                || !isCardinalUnitStep(afterDx, afterDz)
                || beforeDx * afterDx + beforeDz * afterDz != 0)
            {
                continue;
            }

            for (int diagonalEdge = diagonalStart; diagonalEdge < edge; diagonalEdge++)
            {
                final GridCell from = cells.get(diagonalEdge);
                final GridCell to = cells.get(diagonalEdge + 1);
                final boolean followsIncomingAxis = (diagonalEdge - diagonalStart) * 2 < diagonalCount;
                final boolean connectorUsesXAxis = followsIncomingAxis ? beforeDx != 0 : afterDx != 0;
                final int connectorX = connectorUsesXAxis ? to.x() : from.x();
                final int connectorZ = connectorUsesXAxis ? from.z() : to.z();
                connectors.add(columnKey(connectorX, connectorZ));
            }
        }
        return Set.copyOf(connectors);
    }

    static int turnConnectorCount(List<Vec> points)
    {
        final double[] x = new double[points.size()];
        final double[] z = new double[points.size()];
        for (int i = 0; i < points.size(); i++)
        {
            x[i] = points.get(i).x();
            z[i] = points.get(i).z();
        }
        return rasterizeTurnConnectors(x, z).size();
    }

    static boolean isTurnConnector(List<Vec> points, int blockX, int blockZ)
    {
        final double[] x = new double[points.size()];
        final double[] z = new double[points.size()];
        for (int i = 0; i < points.size(); i++)
        {
            x[i] = points.get(i).x();
            z[i] = points.get(i).z();
        }
        return rasterizeTurnConnectors(x, z).contains(columnKey(blockX, blockZ));
    }

    private static boolean isDiagonalUnitStep(int dx, int dz)
    {
        return Math.abs(dx) == 1 && Math.abs(dz) == 1;
    }

    private static boolean isCardinalUnitStep(int dx, int dz)
    {
        return Math.abs(dx) + Math.abs(dz) == 1;
    }

    private static long columnKey(int x, int z)
    {
        return (x & 0xffffffffL) | ((z & 0xffffffffL) << 32);
    }

    static List<Vec> smoothRoute(List<Vec> input)
    {
        return smoothRoute(input, PREFERRED_CORNER_PRECISION);
    }

    private static int findRewriteStart(List<Vec> route, double rewriteLength)
    {
        double remaining = rewriteLength;
        for (int i = route.size() - 2; i > 0; i--)
        {
            remaining -= distance(route.get(i), route.get(i + 1));
            if (remaining <= 0d)
            {
                return i;
            }
        }
        return 1;
    }

    private static Vec cubicBezier(Vec start, Vec startControl, Vec endControl, Vec end, double delta)
    {
        final double inverse = 1d - delta;
        final double startWeight = inverse * inverse * inverse;
        final double startControlWeight = 3d * inverse * inverse * delta;
        final double endControlWeight = 3d * inverse * delta * delta;
        final double endWeight = delta * delta * delta;
        return new Vec(
            start.x() * startWeight
                + startControl.x() * startControlWeight
                + endControl.x() * endControlWeight
                + end.x() * endWeight,
            start.z() * startWeight
                + startControl.z() * startControlWeight
                + endControl.z() * endControlWeight
                + end.z() * endWeight
        );
    }

    private static Vec normalizedDirection(Vec from, Vec to)
    {
        final double dx = to.x() - from.x();
        final double dz = to.z() - from.z();
        final double length = Math.hypot(dx, dz);
        return length < 1.0e-6d ? new Vec(0d, 0d) : new Vec(dx / length, dz / length);
    }

    private static Vec add(Vec left, Vec right)
    {
        return new Vec(left.x() + right.x(), left.z() + right.z());
    }

    private static Vec scale(Vec vector, double factor)
    {
        return new Vec(vector.x() * factor, vector.z() * factor);
    }

    private static double vectorLength(Vec vector)
    {
        return Math.hypot(vector.x(), vector.z());
    }

    private static double distance(Vec left, Vec right)
    {
        return Math.hypot(right.x() - left.x(), right.z() - left.z());
    }

    private static double polylineLength(List<Vec> points)
    {
        double length = 0d;
        for (int i = 0; i < points.size() - 1; i++)
        {
            length += distance(points.get(i), points.get(i + 1));
        }
        return length;
    }

    private static double nearestAlong(List<Vec> points, Vec point)
    {
        double bestDistanceSq = Double.POSITIVE_INFINITY;
        double bestAlong = 0d;
        double traversed = 0d;
        for (int i = 0; i < points.size() - 1; i++)
        {
            final Vec from = points.get(i);
            final Vec to = points.get(i + 1);
            final Projection projection = project(from.x(), from.z(), to.x(), to.z(), point.x(), point.z());
            final double segmentLength = distance(from, to);
            if (projection.distanceSq() < bestDistanceSq)
            {
                bestDistanceSq = projection.distanceSq();
                bestAlong = traversed + projection.delta() * segmentLength;
            }
            traversed += segmentLength;
        }
        return bestAlong;
    }

    private static double distanceSqToPolyline(List<Vec> points, Vec point)
    {
        double bestDistanceSq = Double.POSITIVE_INFINITY;
        for (int i = 0; i < points.size() - 1; i++)
        {
            final Vec from = points.get(i);
            final Vec to = points.get(i + 1);
            bestDistanceSq = Math.min(bestDistanceSq, project(
                from.x(),
                from.z(),
                to.x(),
                to.z(),
                point.x(),
                point.z()
            ).distanceSq());
        }
        return bestDistanceSq;
    }

    private static Vec pointAlong(List<Vec> points, double targetAlong)
    {
        double traversed = 0d;
        for (int i = 0; i < points.size() - 1; i++)
        {
            final Vec from = points.get(i);
            final Vec to = points.get(i + 1);
            final double segmentLength = distance(from, to);
            if (traversed + segmentLength >= targetAlong)
            {
                final double delta = segmentLength < 1.0e-6d ? 0d : (targetAlong - traversed) / segmentLength;
                return lerp(from, to, delta);
            }
            traversed += segmentLength;
        }
        return points.get(points.size() - 1);
    }

    private static Vec directionAlong(List<Vec> points, double targetAlong)
    {
        double traversed = 0d;
        for (int i = 0; i < points.size() - 1; i++)
        {
            final Vec from = points.get(i);
            final Vec to = points.get(i + 1);
            final double segmentLength = distance(from, to);
            if (traversed + segmentLength >= targetAlong || i == points.size() - 2)
            {
                return normalizedDirection(from, to);
            }
            traversed += segmentLength;
        }
        return new Vec(0d, 0d);
    }

    private static List<Vec> smoothRoute(List<Vec> input, int passes)
    {
        if (input.size() < 3)
        {
            return input;
        }

        // Smooth the complete polyline without independently moving every grid
        // corner. The old per-corner fillets produced a mathematically round
        // curve whose sub-block raster was discontinuous at narrow sources.
        List<Vec> current = List.copyOf(input);
        for (int pass = 0; pass < Math.max(1, passes); pass++)
        {
            final List<Vec> rounded = new ArrayList<>(current.size() * 2);
            rounded.add(current.get(0));
            for (int i = 0; i < current.size() - 1; i++)
            {
                final Vec from = current.get(i);
                final Vec to = current.get(i + 1);
                addIfDifferent(rounded, lerp(from, to, 0.25d));
                addIfDifferent(rounded, lerp(from, to, 0.75d));
            }
            addIfDifferent(rounded, current.get(current.size() - 1));
            current = List.copyOf(rounded);
        }
        return resampleRoute(current, passes >= PREFERRED_CORNER_PRECISION ? 1.5d : 2.25d);
    }

    private static List<Vec> resampleRoute(List<Vec> input, double spacing)
    {
        final List<Vec> output = new ArrayList<>(input.size() * 2);
        output.add(input.get(0));
        for (int i = 0; i < input.size() - 1; i++)
        {
            final Vec from = input.get(i);
            final Vec to = input.get(i + 1);
            final double length = Math.hypot(to.x() - from.x(), to.z() - from.z());
            final int steps = Math.max(1, Mth.ceil(length / spacing));
            for (int step = 1; step <= steps; step++)
            {
                addIfDifferent(output, lerp(from, to, step / (double) steps));
            }
        }
        return List.copyOf(output);
    }

    private static void addIfDifferent(List<Vec> output, Vec point)
    {
        final Vec last = output.get(output.size() - 1);
        if (Math.hypot(last.x() - point.x(), last.z() - point.z()) > 1.0e-6d)
        {
            output.add(point);
        }
    }

    private static Vec lerp(Vec from, Vec to, double delta)
    {
        return new Vec(Mth.lerp(delta, from.x(), to.x()), Mth.lerp(delta, from.z(), to.z()));
    }

    private static int index(int x, int z, int width)
    {
        return x + z * width;
    }

    private static double heuristic(int x, int z, int goalX, int goalZ)
    {
        return Math.hypot(goalX - x, goalZ - z) * 0.8d;
    }

    private static Projection project(double sourceX, double sourceZ, double drainX, double drainZ, double x, double z)
    {
        final double dx = drainX - sourceX;
        final double dz = drainZ - sourceZ;
        final double lengthSq = dx * dx + dz * dz;
        final double delta = lengthSq <= 1.0e-9d
            ? 0d
            : Mth.clamp(((x - sourceX) * dx + (z - sourceZ) * dz) / lengthSq, 0d, 1d);
        final double nearestX = sourceX + dx * delta;
        final double nearestZ = sourceZ + dz * delta;
        final double offsetX = x - nearestX;
        final double offsetZ = z - nearestZ;
        return new Projection(offsetX * offsetX + offsetZ * offsetZ, delta);
    }

    private static long mix64(long value)
    {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ value >>> 31;
    }
}
