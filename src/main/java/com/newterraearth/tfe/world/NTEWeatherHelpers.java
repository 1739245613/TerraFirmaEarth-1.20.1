package com.newterraearth.tfe.world;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blocks.IcePileBlock;
import net.dries007.tfc.common.blocks.IcicleBlock;
import net.dries007.tfc.common.blocks.SnowPileBlock;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.ThinSpikeBlock;
import net.dries007.tfc.common.blocks.plant.KrummholzBlock;
import net.dries007.tfc.config.TFCConfig;
import net.dries007.tfc.util.EnvironmentHelpers;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.climate.Climate;
import net.dries007.tfc.util.climate.ClimateModel;
import net.dries007.tfc.util.climate.OverworldClimateModel;
import net.dries007.tfc.util.tracker.WorldTracker;
import net.dries007.tfc.world.chunkdata.ChunkData;
import net.dries007.tfc.util.calendar.ICalendar;
import net.dries007.tfc.util.calendar.Month;
import net.dries007.tfc.world.ChunkGeneratorExtension;

import com.newterraearth.tfe.config.NTECommonConfig;
import com.newterraearth.tfe.mixin.WorldTrackerAccessor;

public final class NTEWeatherHelpers
{
    private static final long UNINITIALIZED_RANDOM_TICK = Integer.MIN_VALUE;
    private static final long UNINITIALIZED_CALENDAR_TICK = Long.MIN_VALUE;

    private NTEWeatherHelpers()
    {
    }

    public static boolean shouldUseCatchUpSnow(ServerLevel level)
    {
        return !TFCConfig.SERVER.enableVanillaWeatherEffects.get()
            && level.dimension() == Level.OVERWORLD
            && level.getChunkSource().getGenerator() instanceof ChunkGeneratorExtension;
    }

    public static void onTickChunk(ServerLevel level, LevelChunk chunk)
    {
        if (!shouldUseCatchUpSnow(level))
        {
            return;
        }

        final ChunkPos chunkPos = chunk.getPos();
        final NTEChunkWeatherBridge bridge = getChunkWeatherBridge(chunk);
        if (bridge == null)
        {
            return;
        }
        final long currentTick = Calendars.SERVER.getTicks();
        final long currentCalendarTick = Calendars.SERVER.getCalendarTicks();
        final int ticksPerSnowAccumulation = getTicksPerSnowAccumulation();
        final int daysInMonth = Calendars.SERVER.getCalendarDaysInMonth();
        final long lastRandomTick = bridge.tfe$getLastRandomTick();
        final long lastCalendarTick = bridge.tfe$getLastCalendarTick();
        final boolean missingCalendarHistory = lastCalendarTick == UNINITIALIZED_CALENDAR_TICK;
        if (lastRandomTick == UNINITIALIZED_RANDOM_TICK)
        {
            if (missingCalendarHistory && chunkMayContainFrozenState(chunk))
            {
                normalizeChunkSnowForCurrentClimate(level, chunk, currentCalendarTick, daysInMonth);
            }
            if (missingCalendarHistory)
            {
                bridge.tfe$setLastCalendarTick(chunk, currentCalendarTick);
            }
            // Fresh / legacy chunks do not have a meaningful runtime weather history yet.
            // Seed them into the update schedule instead of replaying a full month of catch-up immediately.
            bridge.tfe$setLastRandomTick(chunk, getSeededInitialRandomTick(chunkPos, currentTick, ticksPerSnowAccumulation));
            return;
        }

        final long timeSinceTick = currentTick - lastRandomTick;
        final long calendarTimeSinceTick = lastCalendarTick == UNINITIALIZED_CALENDAR_TICK
            ? 0L
            : Math.max(0L, currentCalendarTick - lastCalendarTick);
        if (!missingCalendarHistory && timeSinceTick < ticksPerSnowAccumulation && calendarTimeSinceTick < 4_000L)
        {
            return;
        }

        final boolean monthChanged = didCalendarMonthChange(lastCalendarTick, currentCalendarTick, daysInMonth);
        final BlockPos snowPlacementSurfacePos = getSequentialSurfacePos(level, chunkPos, chunk, bridge, false);
        final BlockPos climateCheckSurfacePos = getRandomSurfacePos(level, chunkPos);
        final int updatesPerSnowAccumulationSkip = getUpdatesPerSnowAccumulationSkip(ticksPerSnowAccumulation);
        final int updatesPerSnowMeltSkip = getUpdatesPerSnowMeltSkip(ticksPerSnowAccumulation);
        final int maxUpdatesPerTick = getSnowMaxAccumulationOnUpdate();

        if (missingCalendarHistory
            || monthChanged
            || (timeSinceTick > 4_000L
                && chunkMayContainFrozenState(chunk)
                && isWarmNormalizationCandidate(level, snowPlacementSurfacePos, climateCheckSurfacePos, currentCalendarTick, daysInMonth)))
        {
            normalizeChunkSnowForCurrentClimate(level, chunk, currentCalendarTick, daysInMonth);
        }

        final int seasonalMeltUpdates;
        if (missingCalendarHistory)
        {
            seasonalMeltUpdates = getLegacyWarmChunkMeltUpdates(level, chunk, snowPlacementSurfacePos, climateCheckSurfacePos);
        }
        else if (calendarTimeSinceTick > 4_000L)
        {
            seasonalMeltUpdates = getSeasonalCatchUpMeltUpdates(level, snowPlacementSurfacePos, climateCheckSurfacePos, lastCalendarTick, currentCalendarTick, daysInMonth);
        }
        else
        {
            seasonalMeltUpdates = 0;
        }

        if (timeSinceTick > 4_000L)
        {
            final long simulatedWindow = Math.min(192_000L, Math.max(timeSinceTick, 0L));
            long simulatedTick = currentTick - simulatedWindow;
            long simulatedCalendarTick = currentCalendarTick - simulatedWindow;
            int netChangeInSnow = 0;

            while (simulatedTick < currentTick)
            {
                simulatedTick += 4_000L;
                simulatedCalendarTick += 4_000L;

                final float estimatedTemperature = Math.max(
                    Climate.getTemperature(level, climateCheckSurfacePos, simulatedCalendarTick, daysInMonth),
                    Climate.getTemperature(level, snowPlacementSurfacePos, simulatedCalendarTick, daysInMonth)
                );

                if (estimatedTemperature > 2f)
                {
                    netChangeInSnow -= updatesPerSnowMeltSkip;
                }
                else if (estimatedTemperature < -2f && isPrecipitating(level, climateCheckSurfacePos, simulatedTick, simulatedCalendarTick, daysInMonth))
                {
                    final float fuzz = Mth.clampedMap(estimatedTemperature, -2f, -12f, 0.5f, 1f);
                    netChangeInSnow += (int) (updatesPerSnowAccumulationSkip * fuzz);
                }
            }

            if (netChangeInSnow > 0)
            {
                netChangeInSnow = Math.min(maxUpdatesPerTick, Math.min(256 - countExistingFrozenStates(chunk, maxUpdatesPerTick), netChangeInSnow));
                for (int i = 0; i < netChangeInSnow; i++)
                {
                    handleSnowAccumulation(level, getSequentialSurfacePos(level, chunkPos, chunk, bridge, true));
                }
            }
            else if (netChangeInSnow < 0)
            {
                final int meltFactor = (int) Math.max(timeSinceTick / 192_000L, 1L);
                final int meltUpdates = Math.max(
                    seasonalMeltUpdates,
                    Math.min(maxUpdatesPerTick, -netChangeInSnow * meltFactor)
                );
                handleSnowMelting(level, chunk, meltUpdates);
            }
        }
        else if (seasonalMeltUpdates > 0)
        {
            handleSnowMelting(level, chunk, seasonalMeltUpdates);
        }
        else if (level.random.nextInt(ticksPerSnowAccumulation) == 0)
        {
            final float realTemperature = Climate.getTemperature(level, snowPlacementSurfacePos);
            if (realTemperature < -2f && isPrecipitating(level, climateCheckSurfacePos, currentTick, currentCalendarTick, daysInMonth))
            {
                handleSnowAccumulation(level, snowPlacementSurfacePos);
                bridge.tfe$iterateSnowPos(chunk);
            }
            else if (Climate.getTemperature(level, climateCheckSurfacePos) > 2f && level.random.nextInt(getSnowMeltMultiplier()) == 0)
            {
                handleSnowMelting(level, chunk, 1);
            }
        }

        bridge.tfe$setLastRandomTick(chunk, currentTick);
        bridge.tfe$setLastCalendarTick(chunk, currentCalendarTick);
    }
    private static long getSeededInitialRandomTick(ChunkPos chunkPos, long currentTick, int ticksPerSnowAccumulation)
    {
        final int phase = Math.floorMod(Helpers.hash(0x7F4A7C15D2E9B11L, chunkPos.getMinBlockX(), 0, chunkPos.getMinBlockZ()), ticksPerSnowAccumulation);
        return currentTick - phase;
    }

    @Nullable
    private static NTEChunkWeatherBridge getChunkWeatherBridge(LevelChunk chunk)
    {
        final ChunkData data = NTEChunkDataHelpers.get(chunk);
        return data != ChunkData.EMPTY ? (NTEChunkWeatherBridge) (Object) data : null;
    }

    private static int getSnowMaxAccumulationOnUpdate()
    {
        return NTECommonConfig.getSnowMaxAccumulationOnUpdate();
    }

    private static int getTicksPerSnowAccumulation()
    {
        return NTECommonConfig.getTicksPerSnowAccumulation();
    }

    private static int getSnowMeltMultiplier()
    {
        return NTECommonConfig.getSnowMeltMultiplier();
    }

    private static int getUpdatesPerSnowAccumulationSkip(int ticksPerSnowAccumulation)
    {
        return 1 + 4_000 / ticksPerSnowAccumulation;
    }

    private static int getUpdatesPerSnowMeltSkip(int ticksPerSnowAccumulation)
    {
        final long meltInterval = Math.max(1L, (long) ticksPerSnowAccumulation * getSnowMeltMultiplier());
        return 1 + (int) (4_000L / meltInterval);
    }

    private static BlockPos getSequentialSurfacePos(ServerLevel level, ChunkPos chunkPos, LevelChunk chunk, NTEChunkWeatherBridge bridge, boolean updateChunk)
    {
        final BlockPos pos = bridge.tfe$getNextSnowPos(chunkPos);
        if (updateChunk)
        {
            bridge.tfe$iterateSnowPos(chunk);
        }
        return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos);
    }

    private static BlockPos getRandomSurfacePos(ServerLevel level, ChunkPos chunkPos)
    {
        final BlockPos randomPos = level.getBlockRandomPos(chunkPos.getMinBlockX(), 0, chunkPos.getMinBlockZ(), 15);
        return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, randomPos);
    }

    private static boolean isPrecipitating(ServerLevel level, BlockPos pos, long tick, long calendarTick, int daysInMonth)
    {
        final WorldTracker tracker = WorldTracker.get(level);
        final WorldTrackerAccessor accessor = (WorldTrackerAccessor) tracker;
        if (tick < accessor.tfe$getRainStartTick() || tick > accessor.tfe$getRainEndTick())
        {
            return false;
        }

        final float rainfall = NTESeasonalHelpers.getInstantRainfall(level, pos, calendarTick, daysInMonth);
        final float threshold = Mth.clampedMap(rainfall, ClimateModel.MINIMUM_RAINFALL, ClimateModel.MAXIMUM_RAINFALL, 1f, 0f);
        return accessor.tfe$invokeExactRainfallIntensity(tick) > threshold;
    }

    private static boolean didCalendarMonthChange(long lastCalendarTick, long currentCalendarTick, int daysInMonth)
    {
        if (lastCalendarTick == UNINITIALIZED_CALENDAR_TICK)
        {
            return false;
        }
        final Month lastMonth = ICalendar.getMonthOfYear(lastCalendarTick, daysInMonth);
        final Month currentMonth = ICalendar.getMonthOfYear(currentCalendarTick, daysInMonth);
        return lastMonth != currentMonth;
    }

    private static boolean chunkMayContainFrozenState(LevelChunk chunk)
    {
        for (LevelChunkSection section : chunk.getSections())
        {
            if (!section.hasOnlyAir() && section.maybeHas(NTEWeatherHelpers::isPotentiallyMeltableFrozenState))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean isPotentiallyMeltableFrozenState(BlockState state)
    {
        return isSnowState(state)
            || isIceState(state)
            || state.getBlock() == TFCBlocks.ICICLE.get()
            || state.getBlock() instanceof KrummholzBlock;
    }

    private static boolean isWarmNormalizationCandidate(ServerLevel level, BlockPos snowPlacementSurfacePos, BlockPos climateCheckSurfacePos, long calendarTick, int daysInMonth)
    {
        return Math.max(
            getSeasonalMeltTemperature(level, climateCheckSurfacePos, calendarTick, daysInMonth),
            getSeasonalMeltTemperature(level, snowPlacementSurfacePos, calendarTick, daysInMonth)
        ) > 2f;
    }

    private static void normalizeChunkSnowForCurrentClimate(ServerLevel level, LevelChunk chunk, long calendarTick, int daysInMonth)
    {
        final ChunkPos chunkPos = chunk.getPos();
        final LevelChunkSection[] sections = chunk.getSections();
        final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++)
        {
            final LevelChunkSection section = sections[sectionIndex];
            if (section.hasOnlyAir() || !section.maybeHas(NTEWeatherHelpers::isPotentiallyMeltableFrozenState))
            {
                continue;
            }

            final int minY = chunk.getSectionYFromSectionIndex(sectionIndex) << 4;
            for (int localY = 0; localY < 16; localY++)
            {
                for (int localZ = 0; localZ < 16; localZ++)
                {
                    for (int localX = 0; localX < 16; localX++)
                    {
                        final BlockState state = section.getBlockState(localX, localY, localZ);
                        if (!isPotentiallyMeltableFrozenState(state))
                        {
                            continue;
                        }

                        mutablePos.set(chunkPos.getMinBlockX() + localX, minY + localY, chunkPos.getMinBlockZ() + localZ);
                        if (getSeasonalMeltTemperature(level, mutablePos, calendarTick, daysInMonth) > 2f)
                        {
                            removeAllFrozenStateAtExactPos(level, mutablePos);
                        }
                    }
                }
            }
        }
    }

    private static float getSeasonalMeltTemperature(ServerLevel level, BlockPos pos, long calendarTick, int daysInMonth)
    {
        final OverworldClimateModel model = OverworldClimateModel.getIfPresent(level);
        if (model == null)
        {
            return Climate.getTemperature(level, pos, calendarTick, daysInMonth);
        }

        final Month currentMonth = ICalendar.getMonthOfYear(calendarTick, daysInMonth);
        final float monthDelta = ICalendar.getFractionOfMonth(calendarTick, daysInMonth);
        final float monthFactor = Mth.lerp(monthDelta, currentMonth.getTemperatureModifier(), currentMonth.next().getTemperatureModifier());
        final ChunkData chunkData = NTEChunkDataHelpers.get(level, pos);
        return model.getAverageMonthlyTemperature(pos.getZ(), pos.getY(), chunkData.getAverageTemp(pos), monthFactor);
    }

    private static int getSeasonalCatchUpMeltUpdates(ServerLevel level, BlockPos snowPlacementSurfacePos, BlockPos climateCheckSurfacePos, long lastCalendarTick, long currentCalendarTick, int daysInMonth)
    {
        if (lastCalendarTick == UNINITIALIZED_CALENDAR_TICK || currentCalendarTick <= lastCalendarTick)
        {
            return 0;
        }

        final float currentTemperature = Math.max(
            Climate.getTemperature(level, climateCheckSurfacePos),
            Climate.getTemperature(level, snowPlacementSurfacePos)
        );
        if (currentTemperature <= 2f)
        {
            return 0;
        }

        final long calendarWindow = Math.min(192_000L, currentCalendarTick - lastCalendarTick);
        long simulatedCalendarTick = currentCalendarTick - calendarWindow;
        int meltUpdates = 0;
        final int updatesPerSnowMeltSkip = getUpdatesPerSnowMeltSkip(getTicksPerSnowAccumulation());

        while (simulatedCalendarTick < currentCalendarTick)
        {
            simulatedCalendarTick += 4_000L;

            final float estimatedTemperature = Math.max(
                Climate.getTemperature(level, climateCheckSurfacePos, simulatedCalendarTick, daysInMonth),
                Climate.getTemperature(level, snowPlacementSurfacePos, simulatedCalendarTick, daysInMonth)
            );
            if (estimatedTemperature > 2f)
            {
                meltUpdates += updatesPerSnowMeltSkip;
            }
        }

        if (meltUpdates <= 0)
        {
            return 0;
        }

        final int meltFactor = (int) Math.max((currentCalendarTick - lastCalendarTick) / 192_000L, 1L);
        return Math.min(256, meltUpdates * meltFactor);
    }

    private static int getLegacyWarmChunkMeltUpdates(ServerLevel level, LevelChunk chunk, BlockPos snowPlacementSurfacePos, BlockPos climateCheckSurfacePos)
    {
        final float currentTemperature = Math.max(
            Climate.getTemperature(level, climateCheckSurfacePos),
            Climate.getTemperature(level, snowPlacementSurfacePos)
        );
        if (currentTemperature <= 2f)
        {
            return 0;
        }
        return countExistingFrozenStates(chunk, 256);
    }

    private static void removeAllFrozenStateAtExactPos(ServerLevel level, BlockPos pos)
    {
        for (int i = 0; i < 16; i++)
        {
            if (!removeSnowState(level, pos, level.getBlockState(pos)))
            {
                return;
            }
        }
    }

    private static int countExistingFrozenStates(LevelChunk chunk, int maxUpdatesPerTick)
    {
        int total = 0;
        final int saturationThreshold = Math.max(1, 257 - maxUpdatesPerTick);
        final LevelChunkSection[] sections = chunk.getSections();

        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++)
        {
            final LevelChunkSection section = sections[sectionIndex];
            if (section.hasOnlyAir() || !section.maybeHas(NTEWeatherHelpers::isPotentiallyMeltableFrozenState))
            {
                continue;
            }

            for (int localY = 0; localY < 16; localY++)
            {
                for (int localZ = 0; localZ < 16; localZ++)
                {
                    for (int localX = 0; localX < 16; localX++)
                    {
                        if (isPotentiallyMeltableFrozenState(section.getBlockState(localX, localY, localZ)))
                        {
                            total++;
                            if (total >= saturationThreshold)
                            {
                                return 256;
                            }
                        }
                    }
                }
            }
        }
        return total;
    }

    private static void handleSnowAccumulation(ServerLevel level, BlockPos surfacePos)
    {
        final float temperature = Climate.getTemperature(level, surfacePos);
        final int expectedLayers = (int) EnvironmentHelpers.getExpectedSnowLayerHeight(temperature);
        BlockPos groundPos;
        BlockPos belowGroundPos;

        if (placeSnowOrSnowPile(level, surfacePos, expectedLayers)) return;
        if (placeSnowOrSnowPile(level, groundPos = surfacePos.below(), expectedLayers)) return;
        if (placeSnowOrSnowPile(level, belowGroundPos = surfacePos.below(2), expectedLayers)) return;

        BlockState groundState = level.getBlockState(groundPos);
        if (isIceState(groundState))
        {
            return;
        }
        if (groundState.getFluidState().getType() != Fluids.WATER)
        {
            groundPos = belowGroundPos;
            groundState = level.getBlockState(groundPos);
        }

        IcePileBlock.placeIcePileOrIce(level, groundPos, groundState, false);

        if (level.random.nextInt(16) == 0)
        {
            final BlockPos iciclePos = findIcicleLocation(level, surfacePos);
            if (iciclePos != null)
            {
                final BlockPos posAbove = iciclePos.above();
                final BlockState stateAbove = level.getBlockState(posAbove);
                if (Helpers.isBlock(stateAbove, BlockTags.ICE))
                {
                    return;
                }
                if (Helpers.isBlock(stateAbove, TFCBlocks.ICICLE.get()))
                {
                    level.setBlock(posAbove, stateAbove.setValue(ThinSpikeBlock.TIP, false), Block.UPDATE_ALL | 16);
                }
                level.setBlock(iciclePos, TFCBlocks.ICICLE.get().defaultBlockState().setValue(ThinSpikeBlock.TIP, true), Block.UPDATE_ALL);
            }
        }
    }

    private static boolean placeSnowOrSnowPile(ServerLevel level, BlockPos initialPos, int expectedLayers)
    {
        if (expectedLayers < 1)
        {
            return false;
        }

        final BlockPos pos = findOptimalSnowLocation(level, initialPos, level.getBlockState(initialPos));
        final BlockState state = level.getBlockState(pos);
        if (initialPos.equals(pos) && !level.canSeeSky(pos))
        {
            return false;
        }
        return placeSnowOrSnowPileAt(level, pos, state, expectedLayers);
    }

    private static boolean placeSnowOrSnowPileAt(ServerLevel level, BlockPos pos, BlockState state, int expectedLayers)
    {
        if (isSnowState(state) && state.getBlock() == Blocks.SNOW && state.getValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS) < 7)
        {
            final int currentLayers = state.getValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS);
            final BlockState newState = state.setValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS, currentLayers + 1);
            if (newState.canSurvive(level, pos) && level.random.nextInt(1 + 3 * currentLayers) == 0 && expectedLayers > currentLayers)
            {
                level.setBlock(pos, newState, Block.UPDATE_ALL);
            }
            return true;
        }
        if (SnowPileBlock.canPlaceSnowPile(level, pos, state))
        {
            SnowPileBlock.placeSnowPile(level, pos, state, false);
            return true;
        }
        if (state.getBlock() instanceof KrummholzBlock)
        {
            KrummholzBlock.updateFreezingInColumn(level, pos, true);
        }
        else if (state.isAir() && Blocks.SNOW.defaultBlockState().canSurvive(level, pos))
        {
            level.setBlock(pos, Blocks.SNOW.defaultBlockState(), Block.UPDATE_ALL);
            return true;
        }
        else
        {
            state.getBlock().handlePrecipitation(state, level, pos, Biome.Precipitation.SNOW);
        }
        return false;
    }

    private static BlockPos findOptimalSnowLocation(ServerLevel level, BlockPos pos, BlockState state)
    {
        BlockPos targetPos = null;
        int found = 0;
        if (isSnowState(state))
        {
            final int currentLayers = state.hasProperty(net.minecraft.world.level.block.SnowLayerBlock.LAYERS)
                ? state.getValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS)
                : 1;

            for (Direction direction : Direction.Plane.HORIZONTAL)
            {
                final BlockPos adjPos = pos.relative(direction);
                final BlockState adjState = level.getBlockState(adjPos);
                if ((isSnowState(adjState) && adjState.hasProperty(net.minecraft.world.level.block.SnowLayerBlock.LAYERS) && adjState.getValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS) < currentLayers)
                    || ((adjState.isAir() || Helpers.isBlock(adjState.getBlock(), TFCTags.Blocks.CAN_BE_SNOW_PILED)) && Blocks.SNOW.defaultBlockState().canSurvive(level, adjPos)))
                {
                    found++;
                    if (targetPos == null || level.random.nextInt(found) == 0)
                    {
                        targetPos = adjPos;
                    }
                }
            }
        }
        return targetPos != null ? targetPos : pos;
    }

    @Nullable
    private static BlockPos findIcicleLocation(ServerLevel level, BlockPos pos)
    {
        final Direction side = Direction.Plane.HORIZONTAL.getRandomDirection(level.random);
        BlockPos adjacentPos = pos.relative(side);
        final int adjacentHeight = level.getHeight(Heightmap.Types.MOTION_BLOCKING, adjacentPos.getX(), adjacentPos.getZ());
        BlockPos foundPos = null;
        int found = 0;

        for (int y = 0; y < adjacentHeight; y++)
        {
            final BlockState stateAt = level.getBlockState(adjacentPos);
            final BlockPos posAbove = adjacentPos.above();
            final BlockState stateAbove = level.getBlockState(posAbove);
            if (stateAt.isAir() && (stateAbove.getBlock() == TFCBlocks.ICICLE.get() || stateAbove.isFaceSturdy(level, posAbove, Direction.DOWN)))
            {
                found++;
                if (foundPos == null || level.random.nextInt(found) == 0)
                {
                    foundPos = adjacentPos;
                }
            }
            adjacentPos = posAbove;
        }

        if (foundPos == null)
        {
            return null;
        }

        final int maxLength = 1 + (Helpers.hash(7189237951231L, pos.getX(), 0, pos.getZ()) % 3);
        if (level.getBlockState(foundPos.above(maxLength)).getBlock() == TFCBlocks.ICICLE.get())
        {
            return null;
        }
        return foundPos;
    }

    private static void handleSnowMelting(ServerLevel level, LevelChunk chunk, int amount)
    {
        if (amount <= 0)
        {
            return;
        }

        final ChunkPos chunkPos = chunk.getPos();
        final LevelChunkSection[] sections = chunk.getSections();
        final List<BlockPos> sample = new ArrayList<>(Math.min(amount, 256));
        int seen = 0;

        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++)
        {
            final LevelChunkSection section = sections[sectionIndex];
            if (section.hasOnlyAir() || !section.maybeHas(NTEWeatherHelpers::isPotentiallyMeltableFrozenState))
            {
                continue;
            }

            final int minY = chunk.getSectionYFromSectionIndex(sectionIndex) << 4;
            for (int localY = 0; localY < 16; localY++)
            {
                for (int localZ = 0; localZ < 16; localZ++)
                {
                    for (int localX = 0; localX < 16; localX++)
                    {
                        if (!isPotentiallyMeltableFrozenState(section.getBlockState(localX, localY, localZ)))
                        {
                            continue;
                        }

                        seen++;
                        final BlockPos exactPos = new BlockPos(chunkPos.getMinBlockX() + localX, minY + localY, chunkPos.getMinBlockZ() + localZ);
                        if (sample.size() < amount)
                        {
                            sample.add(exactPos);
                        }
                        else
                        {
                            final int replaceIndex = level.random.nextInt(seen);
                            if (replaceIndex < amount)
                            {
                                sample.set(replaceIndex, exactPos);
                            }
                        }
                    }
                }
            }
        }

        for (BlockPos pos : sample)
        {
            removeSnowState(level, pos, level.getBlockState(pos));
        }
    }

    private static boolean removeSnowState(ServerLevel level, BlockPos pos, BlockState state)
    {
        if (isSnowState(state))
        {
            SnowPileBlock.removePileOrSnow(level, pos, state);
            return true;
        }
        if (state.getBlock() instanceof KrummholzBlock)
        {
            KrummholzBlock.updateFreezingInColumn(level, pos, false);
            return true;
        }
        if (isIceState(state))
        {
            IcePileBlock.removeIcePileOrIce(level, pos, state);
            return true;
        }
        if (state.getBlock() == TFCBlocks.ICICLE.get())
        {
            final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            cursor.setWithOffset(pos, Direction.DOWN);
            BlockState belowState = level.getBlockState(cursor);
            while (belowState.getBlock() == TFCBlocks.ICICLE.get())
            {
                cursor.move(Direction.DOWN);
                belowState = level.getBlockState(cursor);
            }

            cursor.move(Direction.UP);
            level.removeBlock(cursor, false);
            cursor.move(Direction.UP);

            final BlockState stateAbove = level.getBlockState(cursor);
            if (stateAbove.getBlock() == TFCBlocks.ICICLE.get())
            {
                level.setBlock(cursor, stateAbove.setValue(IcicleBlock.TIP, true), Block.UPDATE_ALL);
            }
            return true;
        }
        return false;
    }

    private static boolean isSnowState(BlockState state)
    {
        return state.getBlock() == Blocks.SNOW || state.getBlock() == TFCBlocks.SNOW_PILE.get();
    }

    private static boolean isIceState(BlockState state)
    {
        return state.getBlock() == Blocks.ICE || state.getBlock() == TFCBlocks.ICE_PILE.get() || state.getBlock() == TFCBlocks.SEA_ICE.get();
    }
}
