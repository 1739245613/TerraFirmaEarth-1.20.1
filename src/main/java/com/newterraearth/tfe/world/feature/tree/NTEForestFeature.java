package com.newterraearth.tfe.world.feature.tree;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blocks.TFCBlockStateProperties;
import net.dries007.tfc.common.blocks.wood.FallenLeavesBlock;
import net.dries007.tfc.common.blocks.wood.ILeavesBlock;
import net.dries007.tfc.common.fluids.FluidHelpers;
import net.dries007.tfc.util.EnvironmentHelpers;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.climate.OverworldClimateModel;
import net.dries007.tfc.util.collections.IWeighted;
import net.dries007.tfc.world.chunkdata.ChunkData;
import net.dries007.tfc.world.chunkdata.ChunkDataProvider;
import net.dries007.tfc.world.feature.tree.ForestConfig;
import net.dries007.tfc.world.placement.ClimatePlacement;

import com.newterraearth.tfe.world.NTE121ClimateHelpers;
import com.newterraearth.tfe.world.forest.NTE121ForestHelpers;
import com.newterraearth.tfe.world.forest.NTEForestType;

public class NTEForestFeature extends Feature<NTEForestConfig>
{
    public NTEForestFeature(Codec<NTEForestConfig> codec)
    {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NTEForestConfig> context)
    {
        final WorldGenLevel level = context.level();
        final BlockPos pos = context.origin();
        final RandomSource random = context.random();
        final NTEForestConfig config = context.config();

        final ChunkDataProvider provider = ChunkDataProvider.get(context.chunkGenerator());
        final ChunkData data = provider.get(level, pos);
        final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        final NTEForestType forestType = NTE121ForestHelpers.getForestType(level.getSeed(), pos);

        if (random.nextFloat() > forestType.getPerChunkChance())
        {
            return false;
        }

        final long levelSeed = level.getSeed();
        final int treeCount = forestType.sampleTrees(random);
        final int bushCount = forestType.sampleBushes(random);

        boolean placedTrees = false;
        boolean placedBushes = false;

        for (int i = 0; i < treeCount; i++)
        {
            placedTrees |= placeTree(level, context.chunkGenerator(), random, pos, config, data, mutablePos, forestType, levelSeed);
        }
        for (int i = 0; i < bushCount; i++)
        {
            placedBushes |= placeBush(level, random, pos, config, data, mutablePos, forestType, context.chunkGenerator(), levelSeed);
        }
        if (placedTrees)
        {
            placeGroundcover(level, random, pos, config, data, mutablePos, forestType.sampleGroundcover(random), forestType, context.chunkGenerator(), levelSeed);
            placeLeafPile(level, random, pos, config, data, mutablePos, forestType.sampleLeafPiles(random), forestType, context.chunkGenerator(), levelSeed);
            placeFallenTree(level, random, pos, config, data, mutablePos, forestType, context.chunkGenerator(), levelSeed);
        }
        return placedTrees || placedBushes;
    }

    private boolean placeTree(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos chunkBlockPos, NTEForestConfig config, ChunkData data, BlockPos.MutableBlockPos mutablePos, NTEForestType typeConfig, long levelSeed)
    {
        final int chunkX = chunkBlockPos.getX();
        final int chunkZ = chunkBlockPos.getZ();

        mutablePos.set(chunkX + random.nextInt(16), 0, chunkZ + random.nextInt(16));
        mutablePos.setY(level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, mutablePos.getX(), mutablePos.getZ()));

        final ForestEntryHandle entry = getTree(data, random, config, mutablePos, typeConfig, generator, levelSeed);
        if (entry == null)
        {
            return false;
        }

        if (entry.floating())
        {
            mutablePos.setY(level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, mutablePos.getX(), mutablePos.getZ()) + random.nextInt(2));
        }

        final ConfiguredFeature<?, ?> feature;
        final int oldChance = entry.oldGrowthChance();
        final int deadChance = entry.deadChance();
        final float blockTemp = OverworldClimateModel.getAdjustedAverageTempByElevation(mutablePos.getY(), data.getAverageTemp(mutablePos));
        final float treeMinTemp = entry.climatePlacement().getMinTemp();
        final float krumChance = blockTemp > treeMinTemp + 5f ? 0f : Mth.clampedMap(blockTemp, treeMinTemp + 2.5f, treeMinTemp + 5f, 1f, 0f);
        if (entry.krummholz().isPresent() && random.nextFloat() < krumChance)
        {
            feature = entry.krummholz().get().value();
        }
        else if (typeConfig.isPrimary() && oldChance > 0 && random.nextInt(oldChance) == 0)
        {
            feature = entry.getOldGrowthFeature();
        }
        else if (deadChance > 0 && (random.nextInt(deadChance) == 0 || typeConfig.isDead()))
        {
            feature = entry.getDeadFeature();
        }
        else
        {
            final int spoilerChance = entry.spoilerOldGrowthChance();
            if (spoilerChance > 0 && random.nextInt(spoilerChance) == 0)
            {
                feature = entry.getOldGrowthFeature();
            }
            else
            {
                feature = entry.getFeature();
            }
        }

        final BlockPos featurePos = mutablePos.immutable();
        final boolean placed = feature.place(level, generator, random, featurePos);
        if (placed && typeConfig.getDensity() >= 3)
        {
            mutablePos.set(featurePos);
            placeSoilDisc(level, generator, random, mutablePos, entry);
        }
        return placed;
    }

    private boolean placeBush(WorldGenLevel level, RandomSource random, BlockPos chunkBlockPos, NTEForestConfig config, ChunkData data, BlockPos.MutableBlockPos mutablePos, NTEForestType type, ChunkGenerator generator, long levelSeed)
    {
        final int chunkX = chunkBlockPos.getX();
        final int chunkZ = chunkBlockPos.getZ();

        mutablePos.set(chunkX + random.nextInt(16), 0, chunkZ + random.nextInt(16));
        mutablePos.setY(level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, mutablePos.getX(), mutablePos.getZ()));

        final ForestEntryHandle entry = getTree(data, random, config, mutablePos, type, generator, levelSeed);
        if (entry != null && EnvironmentHelpers.canPlaceBushOn(level, mutablePos))
        {
            entry.bushLog().ifPresent(log -> entry.bushLeaves().ifPresent(leaves -> {
                placeBushPart(level, mutablePos, log, leaves, 1.0F, random, true);
                for (int i = 0; i < 5; i++)
                {
                    if (random.nextInt(4) == 0)
                    {
                        mutablePos.move(Direction.Plane.HORIZONTAL.getRandomDirection(random));
                        placeBushPart(level, mutablePos, leaves, leaves, 0.7F, random, false);
                        if (random.nextInt(6) == 0)
                        {
                            mutablePos.move(Direction.UP);
                            placeBushPart(level, mutablePos, leaves, leaves, 0.6F, random, false);
                            break;
                        }
                    }
                }
            }));
            return true;
        }
        return false;
    }

    private void placeBushPart(WorldGenLevel level, BlockPos.MutableBlockPos mutablePos, BlockState log, BlockState leaves, float decay, RandomSource random, boolean needsEmptyCenter)
    {
        if (EnvironmentHelpers.isWorldgenReplaceable(level, mutablePos))
        {
            setBlock(level, mutablePos, log);
        }
        else if (needsEmptyCenter)
        {
            return;
        }
        for (Direction facing : Helpers.DIRECTIONS)
        {
            if (facing != Direction.DOWN)
            {
                final BlockPos offsetPos = mutablePos.offset(facing.getStepX(), facing.getStepY(), facing.getStepZ());
                if (EnvironmentHelpers.isWorldgenReplaceable(level, offsetPos) && random.nextFloat() < decay)
                {
                    setBlock(level, offsetPos, leaves);
                }
            }
        }
    }

    private void placeGroundcover(WorldGenLevel level, RandomSource random, BlockPos chunkBlockPos, NTEForestConfig config, ChunkData data, BlockPos.MutableBlockPos mutablePos, int tries, NTEForestType type, ChunkGenerator generator, long levelSeed)
    {
        if (tries == 0)
        {
            return;
        }

        final int chunkX = chunkBlockPos.getX();
        final int chunkZ = chunkBlockPos.getZ();

        mutablePos.set(chunkX + random.nextInt(16), 0, chunkZ + random.nextInt(16));
        mutablePos.setY(level.getHeight(Heightmap.Types.OCEAN_FLOOR, mutablePos.getX(), mutablePos.getZ()));

        final ForestEntryHandle entry = getTree(data, random, config, mutablePos, type, generator, levelSeed);
        if (entry != null)
        {
            entry.groundcover().ifPresent(groundcover -> {
                for (int i = 0; i < tries; i++)
                {
                    BlockState placementState = groundcover.get(random);
                    mutablePos.set(chunkX + random.nextInt(16), 0, chunkZ + random.nextInt(16));
                    mutablePos.setY(level.getHeight(Heightmap.Types.OCEAN_FLOOR, mutablePos.getX(), mutablePos.getZ()));

                    placementState = FluidHelpers.fillWithFluid(placementState, level.getFluidState(mutablePos).getType());
                    if (placementState != null && EnvironmentHelpers.isWorldgenReplaceable(level.getBlockState(mutablePos)) && EnvironmentHelpers.isOnSturdyFace(level, mutablePos))
                    {
                        setBlock(level, mutablePos, placementState);
                    }
                }
            });
        }
    }

    private void placeLeafPile(WorldGenLevel level, RandomSource random, BlockPos chunkBlockPos, NTEForestConfig config, ChunkData data, BlockPos.MutableBlockPos mutablePos, int tries, NTEForestType type, ChunkGenerator generator, long levelSeed)
    {
        final int chunkX = chunkBlockPos.getX();
        final int chunkZ = chunkBlockPos.getZ();

        mutablePos.set(chunkX + random.nextInt(16), 0, chunkZ + random.nextInt(16));
        mutablePos.setY(level.getHeight(Heightmap.Types.OCEAN_FLOOR, mutablePos.getX(), mutablePos.getZ()));

        final ForestEntryHandle entry = getTree(data, random, config, mutablePos, type, generator, levelSeed);
        if (entry != null)
        {
            entry.fallenLeaves().ifPresent(placementState -> {
                for (int i = 0; i < tries; i++)
                {
                    mutablePos.set(chunkX + random.nextInt(16), 0, chunkZ + random.nextInt(16));
                    mutablePos.setY(level.getHeight(Heightmap.Types.OCEAN_FLOOR, mutablePos.getX(), mutablePos.getZ()));
                    final BlockPos origin = mutablePos.immutable();

                    for (int j = 0; j < 8; j++)
                    {
                        mutablePos.setWithOffset(origin, Mth.nextInt(random, -2, 2), 0, Mth.nextInt(random, -2, 2));
                        if (level.getFluidState(mutablePos).isEmpty() && EnvironmentHelpers.isOnSturdyFace(level, mutablePos) && EnvironmentHelpers.isWorldgenReplaceable(level, mutablePos))
                        {
                            final BlockState layered = placementState.setValue(FallenLeavesBlock.LAYERS, Mth.nextInt(random, 1, FallenLeavesBlock.MAX_LAYERS - 3));
                            level.setBlock(mutablePos, layered, 3);
                        }
                    }
                }
            });
        }
    }

    private void placeFallenTree(WorldGenLevel level, RandomSource random, BlockPos chunkBlockPos, NTEForestConfig config, ChunkData data, BlockPos.MutableBlockPos mutablePos, NTEForestType type, ChunkGenerator generator, long levelSeed)
    {
        final int chunkX = chunkBlockPos.getX();
        final int chunkZ = chunkBlockPos.getZ();

        mutablePos.set(chunkX + random.nextInt(16), 0, chunkZ + random.nextInt(16));
        mutablePos.setY(level.getHeight(Heightmap.Types.OCEAN_FLOOR, mutablePos.getX(), mutablePos.getZ()));

        mutablePos.move(Direction.DOWN);
        final BlockState downState = level.getBlockState(mutablePos);
        mutablePos.move(Direction.UP);
        if (Helpers.isBlock(downState, TFCTags.Blocks.BUSH_PLANTABLE_ON) || Helpers.isBlock(downState, TFCTags.Blocks.SEA_BUSH_PLANTABLE_ON))
        {
            final ForestEntryHandle entry = getTree(data, random, config, mutablePos, type, generator, levelSeed);
            if (entry != null)
            {
                final int fallChance = entry.fallenChance();
                if (fallChance > 0 && level.getRandom().nextInt(fallChance) == 0)
                {
                    BlockState log = entry.fallenLog().orElse(null);
                    if (log != null)
                    {
                        final Direction axis = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                        log = Helpers.setProperty(log, TFCBlockStateProperties.NATURAL, false);
                        log = Helpers.setProperty(log, BlockStateProperties.AXIS, axis.getAxis());

                        final int length = 4 + random.nextInt(10);
                        final BlockPos start = mutablePos.immutable();
                        final boolean[] support = new boolean[length];

                        mutablePos.set(start);
                        int valid = 0;
                        for (; valid < length; valid++)
                        {
                            final BlockState replaceState = level.getBlockState(mutablePos);
                            if (EnvironmentHelpers.isWorldgenReplaceable(replaceState) || replaceState.getBlock() instanceof ILeavesBlock)
                            {
                                mutablePos.move(Direction.DOWN);
                                support[valid] = level.getBlockState(mutablePos).isFaceSturdy(level, mutablePos, Direction.UP);
                            }
                            else
                            {
                                break;
                            }

                            mutablePos.move(Direction.UP);
                            mutablePos.move(axis);
                        }

                        int left = 0;
                        int right = valid - 1;
                        for (; left < support.length; left++)
                        {
                            if (support[left])
                            {
                                break;
                            }
                        }
                        for (; right >= 0; right--)
                        {
                            if (support[right])
                            {
                                break;
                            }
                        }

                        if (left <= valid / 2 && right >= valid / 2 && valid >= 3)
                        {
                            mutablePos.set(start);
                            for (int i = 0; i < length; i++)
                            {
                                level.setBlock(mutablePos, log, 2);
                                mutablePos.move(axis);
                            }
                        }
                    }
                }
            }
        }
    }

    private void placeSoilDisc(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos.MutableBlockPos mutablePos, ForestEntryHandle entry)
    {
        mutablePos.move(random.nextInt(4) - 2, 0, random.nextInt(4) - 2);
        mutablePos.setY(level.getHeight(Heightmap.Types.OCEAN_FLOOR, mutablePos.getX(), mutablePos.getZ()));
        entry.transitionSoilDiscFeature().ifPresent(feature -> feature.value().place(level, generator, random, mutablePos));
        entry.soilDiscFeature().ifPresent(feature -> feature.value().place(level, generator, random, mutablePos));
    }

    @Nullable
    private ForestEntryHandle getTree(ChunkData chunkData, RandomSource random, NTEForestConfig config, BlockPos pos, NTEForestType type, ChunkGenerator generator, long levelSeed)
    {
        final boolean northernHemisphere = NTE121ClimateHelpers.isNorthernHemisphere(generator, pos.getZ());
        final float rainVariance = NTE121ClimateHelpers.getRainVariance(levelSeed, generator, pos) * (northernHemisphere ? 1f : -1f);
        final float groundwater = NTE121ClimateHelpers.getAverageGroundwater(generator, pos);
        final int elevation = pos.getY();
        final float rainfall = chunkData.getRainfall(pos);
        final float averageTemperature = OverworldClimateModel.getAdjustedAverageTempByElevation(pos, chunkData);

        final List<ForestEntryHandle> entries = config.entries().stream()
            .map(configuredFeature -> configuredFeature.value().config())
            .map(NTEForestFeature::adaptEntry)
            .filter(Objects::nonNull)
            .filter(entry -> entry.isValid(averageTemperature, rainfall, groundwater, rainVariance, elevation))
            .sorted(Comparator.comparingDouble(entry -> entry.distanceFromMean(averageTemperature, rainfall, groundwater, rainVariance, elevation)))
            .collect(Collectors.toList());

        if (entries.isEmpty())
        {
            return null;
        }
        if (entries.size() == 1)
        {
            return entries.get(0);
        }

        while (entries.size() > type.getMaxTreeTypes())
        {
            entries.remove(entries.size() - 1);
        }
        int alternate = type.getAlternateSize();
        while (entries.size() > 1 && alternate > 0)
        {
            entries.remove(0);
            alternate--;
        }

        int index = 0;
        while (index < entries.size() - 1 && random.nextFloat() < 0.6f)
        {
            index++;
        }
        return entries.get(index);
    }

    @Nullable
    private static ForestEntryHandle adaptEntry(FeatureConfiguration config)
    {
        if (config instanceof NTEForestConfig.Entry entry)
        {
            return new NTEForestEntryHandle(entry);
        }
        if (config instanceof ForestConfig.Entry entry)
        {
            return new TFCForestEntryHandle(entry);
        }
        return null;
    }

    private interface ForestEntryHandle
    {
        boolean isValid(float temperature, float rainfall, float groundwater, float rainVariance, int elevation);

        double distanceFromMean(float temperature, float rainfall, float groundwater, float rainVariance, int elevation);

        ClimatePlacement climatePlacement();

        Optional<BlockState> bushLog();

        Optional<BlockState> bushLeaves();

        Optional<BlockState> fallenLog();

        Optional<BlockState> fallenLeaves();

        Optional<IWeighted<BlockState>> groundcover();

        ConfiguredFeature<?, ?> getFeature();

        ConfiguredFeature<?, ?> getDeadFeature();

        ConfiguredFeature<?, ?> getOldGrowthFeature();

        Optional<Holder<ConfiguredFeature<?, ?>>> krummholz();

        Optional<Holder<ConfiguredFeature<?, ?>>> soilDiscFeature();

        Optional<Holder<ConfiguredFeature<?, ?>>> transitionSoilDiscFeature();

        int oldGrowthChance();

        int spoilerOldGrowthChance();

        int fallenChance();

        int deadChance();

        boolean floating();
    }

    private record NTEForestEntryHandle(NTEForestConfig.Entry entry) implements ForestEntryHandle
    {
        @Override
        public boolean isValid(float temperature, float rainfall, float groundwater, float rainVariance, int elevation)
        {
            return entry.isValid(temperature, groundwater, rainVariance, elevation);
        }

        @Override
        public double distanceFromMean(float temperature, float rainfall, float groundwater, float rainVariance, int elevation)
        {
            return entry.distanceFromMean(temperature, groundwater, rainVariance, elevation);
        }

        @Override
        public ClimatePlacement climatePlacement()
        {
            return entry.getClimatePlacement();
        }

        @Override
        public Optional<BlockState> bushLog()
        {
            return entry.bushLog();
        }

        @Override
        public Optional<BlockState> bushLeaves()
        {
            return entry.bushLeaves();
        }

        @Override
        public Optional<BlockState> fallenLog()
        {
            return entry.fallenLog();
        }

        @Override
        public Optional<BlockState> fallenLeaves()
        {
            return entry.fallenLeaves();
        }

        @Override
        public Optional<IWeighted<BlockState>> groundcover()
        {
            return entry.groundcover();
        }

        @Override
        public ConfiguredFeature<?, ?> getFeature()
        {
            return entry.getFeature();
        }

        @Override
        public ConfiguredFeature<?, ?> getDeadFeature()
        {
            return entry.getDeadFeature();
        }

        @Override
        public ConfiguredFeature<?, ?> getOldGrowthFeature()
        {
            return entry.getOldGrowthFeature();
        }

        @Override
        public Optional<Holder<ConfiguredFeature<?, ?>>> krummholz()
        {
            return entry.krummholz();
        }

        @Override
        public Optional<Holder<ConfiguredFeature<?, ?>>> soilDiscFeature()
        {
            return entry.soilDiscFeature();
        }

        @Override
        public Optional<Holder<ConfiguredFeature<?, ?>>> transitionSoilDiscFeature()
        {
            return entry.transitionSoilDiscFeature();
        }

        @Override
        public int oldGrowthChance()
        {
            return entry.oldGrowthChance();
        }

        @Override
        public int spoilerOldGrowthChance()
        {
            return entry.spoilerOldGrowthChance();
        }

        @Override
        public int fallenChance()
        {
            return entry.fallenChance();
        }

        @Override
        public int deadChance()
        {
            return entry.deadChance();
        }

        @Override
        public boolean floating()
        {
            return entry.floating();
        }
    }

    private record TFCForestEntryHandle(ForestConfig.Entry entry) implements ForestEntryHandle
    {
        @Override
        public boolean isValid(float temperature, float rainfall, float groundwater, float rainVariance, int elevation)
        {
            return entry.isValid(temperature, rainfall);
        }

        @Override
        public double distanceFromMean(float temperature, float rainfall, float groundwater, float rainVariance, int elevation)
        {
            return entry.distanceFromMean(temperature, rainfall);
        }

        @Override
        public ClimatePlacement climatePlacement()
        {
            return entry.climate();
        }

        @Override
        public Optional<BlockState> bushLog()
        {
            return entry.bushLog();
        }

        @Override
        public Optional<BlockState> bushLeaves()
        {
            return entry.bushLeaves();
        }

        @Override
        public Optional<BlockState> fallenLog()
        {
            return entry.fallenLog();
        }

        @Override
        public Optional<BlockState> fallenLeaves()
        {
            return entry.fallenLeaves();
        }

        @Override
        public Optional<IWeighted<BlockState>> groundcover()
        {
            return entry.groundcover();
        }

        @Override
        public ConfiguredFeature<?, ?> getFeature()
        {
            return entry.getFeature();
        }

        @Override
        public ConfiguredFeature<?, ?> getDeadFeature()
        {
            return entry.getDeadFeature();
        }

        @Override
        public ConfiguredFeature<?, ?> getOldGrowthFeature()
        {
            return entry.getOldGrowthFeature();
        }

        @Override
        public Optional<Holder<ConfiguredFeature<?, ?>>> krummholz()
        {
            return entry.krummholz();
        }

        @Override
        public Optional<Holder<ConfiguredFeature<?, ?>>> soilDiscFeature()
        {
            return Optional.empty();
        }

        @Override
        public Optional<Holder<ConfiguredFeature<?, ?>>> transitionSoilDiscFeature()
        {
            return Optional.empty();
        }

        @Override
        public int oldGrowthChance()
        {
            return entry.oldGrowthChance();
        }

        @Override
        public int spoilerOldGrowthChance()
        {
            return entry.spoilerOldGrowthChance();
        }

        @Override
        public int fallenChance()
        {
            return entry.fallenChance();
        }

        @Override
        public int deadChance()
        {
            return entry.deadChance();
        }

        @Override
        public boolean floating()
        {
            return entry.floating();
        }
    }

    public static class Entry extends Feature<NTEForestConfig.Entry>
    {
        public Entry(Codec<NTEForestConfig.Entry> codec)
        {
            super(codec);
        }

        @Override
        public boolean place(FeaturePlaceContext<NTEForestConfig.Entry> context)
        {
            throw new IllegalArgumentException("This is not a real feature and should never be placed!");
        }
    }
}
