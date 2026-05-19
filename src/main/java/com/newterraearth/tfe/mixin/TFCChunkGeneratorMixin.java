package com.newterraearth.tfe.mixin;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import net.minecraft.Util;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.dries007.tfc.mixin.accessor.ChunkAccessAccessor;
import net.dries007.tfc.world.BiomeNoiseSampler;
import net.dries007.tfc.world.ChunkBaseBlockSource;
import net.dries007.tfc.world.ChunkBiomeSampler;
import net.dries007.tfc.world.ChunkGeneratorExtension;
import net.dries007.tfc.world.ChunkHeightFiller;
import net.dries007.tfc.world.ChunkNoiseFiller;
import net.dries007.tfc.world.FastConcurrentCache;
import net.dries007.tfc.world.TFCAquifer;
import net.dries007.tfc.world.TFCChunkGenerator;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.biome.BiomeSourceExtension;
import net.dries007.tfc.world.biome.TFCBiomes;
import net.dries007.tfc.world.chunkdata.ChunkData;
import net.dries007.tfc.world.chunkdata.ChunkDataProvider;
import net.dries007.tfc.world.chunkdata.RockData;
import net.dries007.tfc.world.layer.framework.ConcurrentArea;
import net.dries007.tfc.world.noise.ChunkNoiseSamplingSettings;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.NoiseSampler;
import net.dries007.tfc.world.noise.OpenSimplex2D;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.river.RiverBlendType;
import net.dries007.tfc.world.river.RiverNoiseSampler;
import net.dries007.tfc.world.surface.SurfaceManager;

import com.newterraearth.tfe.debug.VolcanoRuntimeTrace;
import com.newterraearth.tfe.world.NTEChunkShoreContext;
import com.newterraearth.tfe.world.NTESeed;
import com.newterraearth.tfe.world.NTESurfaceContext;
import com.newterraearth.tfe.world.forest.NTE121ForestHelpers;
import com.newterraearth.tfe.world.region.NTERegionGeneratorAccess;
import com.newterraearth.tfe.world.river.NTERiverBlendType;
import com.newterraearth.tfe.world.river.NTERiverNoiseSampler;
import com.newterraearth.tfe.world.shore.NTEShoreBlendType;
import com.newterraearth.tfe.world.shore.NTEShoreNoiseHelpers;
import com.newterraearth.tfe.world.shore.NTEShoreNoiseSampler;
import com.newterraearth.tfe.world.terrain.NTETerrainUpliftSampler;
import com.newterraearth.tfe.world.volcano.NTECenteredFeatureBlendType;
import com.newterraearth.tfe.world.volcano.NTECenteredFeatureNoiseSampler;

@Mixin(TFCChunkGenerator.class)
public abstract class TFCChunkGeneratorMixin
{
    @Shadow(remap = false) @Final private Holder<NoiseGeneratorSettings> noiseSettings;
    @Shadow(remap = false) @Final private FastConcurrentCache<TFCAquifer> aquiferCache;
    @Shadow(remap = false) private BiomeSourceExtension customBiomeSource;
    @Shadow(remap = false) private long noiseSamplerSeed;
    @Shadow(remap = false) private SurfaceManager surfaceManager;
    @Shadow(remap = false) private NoiseSampler noiseSampler;
    @Shadow(remap = false) private ChunkDataProvider chunkDataProvider;
    @Unique private Noise2D tfe$tideHeightNoise;
    @Unique private Noise2D tfe$legacyShoreNoise;

    @Redirect(
        method = "initRandomState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/world/biome/BiomeSourceExtension;initRandomState(Lnet/dries007/tfc/world/region/RegionGenerator;Lnet/dries007/tfc/world/layer/framework/ConcurrentArea;)V"
        ),
        remap = false,
        require = 0
    )
    private void tfe$alignExtraRegionNoiseToRootSeed(
        BiomeSourceExtension biomeSource,
        RegionGenerator regionGenerator,
        ConcurrentArea<BiomeExtension> biomeLayer,
        net.minecraft.server.level.ChunkMap chunkMap,
        net.minecraft.server.level.ServerLevel level
    )
    {
        ((NTERegionGeneratorAccess) regionGenerator).nte$setRootLevelSeed(level.getSeed());
        biomeSource.initRandomState(regionGenerator, biomeLayer);
    }

    /**
     * @author Codex
     * @reason Inject 1.21-style shoreline samplers and tide noise into height sampling.
     */
    @Overwrite(remap = false)
    public ChunkHeightFiller createHeightFillerForChunk(ChunkPos pos)
    {
        final Object2DoubleMap<BiomeExtension>[] biomeWeights = ChunkBiomeSampler.sampleBiomes(pos, this::tfe$sampleBiomeNoRiver, BiomeExtension::biomeBlendType);
        try (NTEChunkShoreContext.Scope ignored = NTEChunkShoreContext.open(
            tfe$createShoreSamplersForChunk(),
            tfe$createTideHeightNoise(),
            tfe$createExactRiverSamplersForChunk(),
            tfe$createCenteredFeatureSamplersForChunk(),
            tfe$createTerrainUpliftSampler()))
        {
            return new ChunkHeightFiller(
                biomeWeights,
                customBiomeSource,
                tfe$createBiomeSamplersForChunk(null),
                tfe$createRiverSamplersForChunk(),
                tfe$createLegacyShoreSamplerForChunk(),
                ((TFCChunkGenerator) (Object) this).getSeaLevel()
            );
        }
    }

    /**
     * @author Codex
     * @reason Inject 1.21 shoreline runtime into the 1.20 terrain fill path without changing TFC constructor signatures.
     */
    @Overwrite
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor mainExecutor, Blender oldTerrainBlender, RandomState rawState, StructureManager structureFeatureManager, ChunkAccess chunk)
    {
        final ChunkNoiseSamplingSettings settings = tfe$createNoiseSamplingSettingsForChunk(chunk);
        final LevelAccessor actualLevel = (LevelAccessor) ((ChunkAccessAccessor) chunk).accessor$getLevelHeightAccessor();
        final ChunkPos chunkPos = chunk.getPos();
        final RandomSource random = new XoroshiroRandomSource(chunkPos.x * 1842639486192314L, chunkPos.z * 579238196380231L);
        final ChunkData chunkData = chunkDataProvider.get(chunk);
        tfe$syncForestType121(chunkData);
        final Object2DoubleMap<BiomeExtension>[] biomeWeights = ChunkBiomeSampler.sampleBiomes(chunkPos, this::tfe$sampleBiomeNoRiver, BiomeExtension::biomeBlendType);
        final Beardifier beardifier = Beardifier.forStructuresInChunk(structureFeatureManager, chunkPos);

        final Set<LevelChunkSection> sections = new HashSet<>();
        for (LevelChunkSection section : chunk.getSections())
        {
            section.acquire();
            sections.add(section);
        }

        final ChunkNoiseFiller[] fillerHolder = new ChunkNoiseFiller[1];
        final BiomeExtension[] cinderConeBiomeHolder = new BiomeExtension[1];
        final BiomeExtension[] tuffRingBiomeHolder = new BiomeExtension[1];
        final BiomeExtension[] tuyaBiomeHolder = new BiomeExtension[1];

        return CompletableFuture.supplyAsync(() -> {
            final ChunkBaseBlockSource baseBlockSource = tfe$createBaseBlockSourceForChunk(chunk, chunkData);
            final Map<NTEShoreBlendType, NTEShoreNoiseSampler> exactShoreSamplers = tfe$createShoreSamplersForChunk();
            final Noise2D tideHeightNoise = tfe$createTideHeightNoise();
            final Map<NTERiverBlendType, NTERiverNoiseSampler> exactRiverSamplers = tfe$createExactRiverSamplersForChunk();
            final Map<NTECenteredFeatureBlendType, NTECenteredFeatureNoiseSampler> centeredFeatureSamplers = tfe$createCenteredFeatureSamplersForChunk();
            final NTETerrainUpliftSampler terrainUpliftSampler = tfe$createTerrainUpliftSampler();

            final ChunkNoiseFiller filler;
            try (NTEChunkShoreContext.Scope ignored = NTEChunkShoreContext.open(exactShoreSamplers, tideHeightNoise, exactRiverSamplers, centeredFeatureSamplers, terrainUpliftSampler))
            {
                filler = new ChunkNoiseFiller(
                    (ProtoChunk) chunk,
                    biomeWeights,
                    customBiomeSource,
                    tfe$createBiomeSamplersForChunk(chunk),
                    tfe$createRiverSamplersForChunk(),
                    tfe$createLegacyShoreSamplerForChunk(),
                    noiseSampler,
                    baseBlockSource,
                    settings,
                    ((TFCChunkGenerator) (Object) this).getSeaLevel(),
                    beardifier
                );
            }

            fillerHolder[0] = filler;
            cinderConeBiomeHolder[0] = tfe$getCenteredFeatureBiome(centeredFeatureSamplers.get(NTECenteredFeatureBlendType.CINDER_CONE), chunkPos);
            tuffRingBiomeHolder[0] = tfe$getCenteredFeatureBiome(centeredFeatureSamplers.get(NTECenteredFeatureBlendType.TUFF_RING), chunkPos);
            tuyaBiomeHolder[0] = tfe$getCenteredFeatureBiome(centeredFeatureSamplers.get(NTECenteredFeatureBlendType.TUYA), chunkPos);

            filler.sampleAquiferSurfaceHeight(this::tfe$sampleBiomeNoRiver);
            chunkData.generateFull(filler.surfaceHeight(), filler.aquifer().surfaceHeights());
            chunkData.getRockData().useCache(chunkPos);
            VolcanoRuntimeTrace.recordChunkPhase("before_fill", chunkPos);
            filler.fillFromNoise();
            VolcanoRuntimeTrace.recordChunkPhase("after_fill", chunkPos);

            aquiferCache.set(chunkPos.x, chunkPos.z, filler.aquifer());
            return chunk;
        }, Util.backgroundExecutor()).whenCompleteAsync((ret, error) -> {
            sections.forEach(LevelChunkSection::release);

            final ChunkNoiseFiller filler = fillerHolder[0];
            if (error != null || filler == null)
            {
                return;
            }

            try (NTESurfaceContext.Scope ignored = NTESurfaceContext.open((TFCChunkGenerator) (Object) this, chunkData, filler.surfaceHeight(), chunkPos, cinderConeBiomeHolder[0], tuffRingBiomeHolder[0], tuyaBiomeHolder[0]))
            {
                surfaceManager.buildSurface(
                    actualLevel,
                    chunk,
                    ((ChunkGeneratorExtension) (Object) this).rockLayerSettings(),
                    chunkData,
                    filler.localBiomes(),
                    filler.localBiomesNoRivers(),
                    filler.localBiomeWeights(),
                    filler.createSlopeMap(),
                    random,
                    ((TFCChunkGenerator) (Object) this).getSeaLevel(),
                    settings.minY()
                );
                VolcanoRuntimeTrace.recordChunkPhase("after_surface", chunkPos);
            }
        }, mainExecutor);
    }

    @Unique
    private BiomeExtension tfe$sampleBiomeNoRiver(int blockX, int blockZ)
    {
        return customBiomeSource.getBiomeExtensionNoRiver(QuartPos.fromBlock(blockX), QuartPos.fromBlock(blockZ));
    }

    @Unique
    private ChunkBaseBlockSource tfe$createBaseBlockSourceForChunk(ChunkAccess chunk)
    {
        final RockData rockData = chunkDataProvider.get(chunk).getRockData();
        return new ChunkBaseBlockSource(rockData, this::tfe$sampleBiomeNoRiver);
    }

    @Unique
    private ChunkBaseBlockSource tfe$createBaseBlockSourceForChunk(ChunkAccess chunk, ChunkData chunkData)
    {
        return new ChunkBaseBlockSource(chunkData.getRockData(), this::tfe$sampleBiomeNoRiver);
    }

    @Unique
    private ChunkNoiseSamplingSettings tfe$createNoiseSamplingSettingsForChunk(ChunkAccess chunk)
    {
        return tfe$createNoiseSamplingSettingsForChunk(chunk.getPos(), chunk.getHeightAccessorForGeneration());
    }

    @Unique
    private ChunkNoiseSamplingSettings tfe$createNoiseSamplingSettingsForChunk(ChunkPos pos, LevelHeightAccessor level)
    {
        final NoiseSettings noiseSettings = this.noiseSettings.value().noiseSettings();

        final int cellWidth = noiseSettings.getCellWidth();
        final int cellHeight = noiseSettings.getCellHeight();

        final int minY = Math.max(noiseSettings.minY(), level.getMinBuildHeight());
        final int maxY = Math.min(noiseSettings.minY() + noiseSettings.height(), level.getMaxBuildHeight());

        final int cellCountY = Math.floorDiv(maxY - minY, noiseSettings.getCellHeight());

        final int firstCellX = Math.floorDiv(pos.getMinBlockX(), cellWidth);
        final int firstCellY = Math.floorDiv(minY, cellHeight);
        final int firstCellZ = Math.floorDiv(pos.getMinBlockZ(), cellWidth);

        return new ChunkNoiseSamplingSettings(minY, 16 / cellWidth, cellCountY, cellWidth, cellHeight, firstCellX, firstCellY, firstCellZ);
    }

    @Unique
    private Map<BiomeExtension, BiomeNoiseSampler> tfe$createBiomeSamplersForChunk(@Nullable ChunkAccess chunk)
    {
        final ImmutableMap.Builder<BiomeExtension, BiomeNoiseSampler> builder = ImmutableMap.builder();
        for (BiomeExtension extension : TFCBiomes.getExtensions())
        {
            final BiomeNoiseSampler sampler = extension.createNoiseSampler(noiseSamplerSeed);
            if (sampler != null)
            {
                sampler.prepare((TFCChunkGenerator) (Object) this, chunk);
                builder.put(extension, sampler);
            }
        }
        return builder.build();
    }

    @Unique
    private Map<RiverBlendType, RiverNoiseSampler> tfe$createRiverSamplersForChunk()
    {
        final EnumMap<RiverBlendType, RiverNoiseSampler> builder = new EnumMap<>(RiverBlendType.class);
        for (RiverBlendType blendType : RiverBlendType.ALL)
        {
            builder.put(blendType, blendType.createNoiseSampler(noiseSamplerSeed));
        }
        return builder;
    }

    @Unique
    private Map<NTERiverBlendType, NTERiverNoiseSampler> tfe$createExactRiverSamplersForChunk()
    {
        final NTESeed seed = NTESeed.of(noiseSamplerSeed);
        final EnumMap<NTERiverBlendType, NTERiverNoiseSampler> builder = new EnumMap<>(NTERiverBlendType.class);
        for (NTERiverBlendType blendType : NTERiverBlendType.ALL)
        {
            builder.put(blendType, blendType.createNoiseSampler(seed));
        }
        return builder;
    }

    @Unique
    private Map<NTEShoreBlendType, NTEShoreNoiseSampler> tfe$createShoreSamplersForChunk()
    {
        final NTESeed seed = NTESeed.unsafeOf(noiseSamplerSeed);
        final EnumMap<NTEShoreBlendType, NTEShoreNoiseSampler> builder = new EnumMap<>(NTEShoreBlendType.class);
        for (NTEShoreBlendType blendType : NTEShoreBlendType.ALL)
        {
            builder.put(blendType, blendType.createNoiseSampler(seed));
        }
        return builder;
    }

    @Unique
    private Noise2D tfe$createTideHeightNoise()
    {
        Noise2D tideHeightNoise = tfe$tideHeightNoise;
        if (tideHeightNoise == null)
        {
            tideHeightNoise = NTEShoreNoiseHelpers.shoreTideLevelNoise(NTESeed.unsafeOf(noiseSamplerSeed));
            tfe$tideHeightNoise = tideHeightNoise;
        }
        return tideHeightNoise;
    }

    @Unique
    private Noise2D tfe$createLegacyShoreSamplerForChunk()
    {
        Noise2D legacyShoreNoise = tfe$legacyShoreNoise;
        if (legacyShoreNoise == null)
        {
            legacyShoreNoise = new OpenSimplex2D(noiseSamplerSeed)
                .octaves(2)
                .spread(0.003f)
                .scaled(-0.1, 1.1);
            tfe$legacyShoreNoise = legacyShoreNoise;
        }
        return legacyShoreNoise;
    }

    @Unique
    private Map<NTECenteredFeatureBlendType, NTECenteredFeatureNoiseSampler> tfe$createCenteredFeatureSamplersForChunk()
    {
        final NTESeed seed = NTESeed.of(noiseSamplerSeed);
        final EnumMap<NTECenteredFeatureBlendType, NTECenteredFeatureNoiseSampler> builder = new EnumMap<>(NTECenteredFeatureBlendType.class);
        for (NTECenteredFeatureBlendType blendType : NTECenteredFeatureBlendType.ALL)
        {
            builder.put(blendType, blendType.createNoiseSampler(seed));
        }
        return builder;
    }

    @Unique
    private NTETerrainUpliftSampler tfe$createTerrainUpliftSampler()
    {
        return new NTETerrainUpliftSampler(noiseSamplerSeed, customBiomeSource);
    }

    @Unique
    private BiomeExtension tfe$getCenteredFeatureBiome(@Nullable NTECenteredFeatureNoiseSampler sampler, ChunkPos chunkPos)
    {
        if (sampler == null)
        {
            return null;
        }
        return sampler.getCenterBiome(chunkPos.getBlockX(8), chunkPos.getBlockZ(8), customBiomeSource);
    }

    @Unique
    private void tfe$syncForestType121(ChunkData chunkData)
    {
        final ChunkPos chunkPos = chunkData.getPos();
        ((ChunkDataAccessor) chunkData).tfe$setForestType(
            NTE121ForestHelpers.toLegacyForestType(
                NTE121ForestHelpers.getForestType(noiseSamplerSeed, chunkPos.getMinBlockX(), chunkPos.getMinBlockZ())
            )
        );
    }
}
