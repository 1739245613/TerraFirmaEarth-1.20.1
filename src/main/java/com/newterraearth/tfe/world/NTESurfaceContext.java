package com.newterraearth.tfe.world;

import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;

import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.chunkdata.ChunkData;
import net.dries007.tfc.world.chunkdata.LerpFloatLayer;

import com.newterraearth.tfe.world.river.NTERiverHydrology;

import static net.dries007.tfc.world.TFCChunkGenerator.SEA_LEVEL_Y;

public final class NTESurfaceContext
{
    public static final class Context
    {
        private final ChunkGenerator generator;
        private final ChunkData chunkData;
        private final BiomeExtension cinderConeBiome;
        private final BiomeExtension tuffRingBiome;
        private final BiomeExtension tuyaBiome;
        private final LerpFloatLayer baseGroundwaterLayer;
        private final Long2FloatOpenHashMap baseGroundwaterCache;
        private final Long2FloatOpenHashMap averageGroundwaterCache;
        private final Long2FloatOpenHashMap rainVarianceCache;
        private final NTERiverHydrology.ColumnProfile[] riverProfiles;

        private Context(ChunkGenerator generator, ChunkData chunkData, int[] surfaceHeight, ChunkPos chunkPos, BiomeExtension cinderConeBiome, BiomeExtension tuffRingBiome, BiomeExtension tuyaBiome, NTERiverHydrology.ColumnProfile[] riverProfiles)
        {
            this.generator = generator;
            this.chunkData = chunkData;
            this.cinderConeBiome = cinderConeBiome;
            this.tuffRingBiome = tuffRingBiome;
            this.tuyaBiome = tuyaBiome;
            this.baseGroundwaterLayer = createModifiedBaseGroundwaterLayer(generator, surfaceHeight, chunkPos);
            this.baseGroundwaterCache = new Long2FloatOpenHashMap();
            this.averageGroundwaterCache = new Long2FloatOpenHashMap();
            this.rainVarianceCache = new Long2FloatOpenHashMap();
            this.riverProfiles = riverProfiles;
            this.baseGroundwaterCache.defaultReturnValue(Float.NaN);
            this.averageGroundwaterCache.defaultReturnValue(Float.NaN);
            this.rainVarianceCache.defaultReturnValue(Float.NaN);
        }

        public ChunkGenerator generator()
        {
            return generator;
        }

        public BiomeExtension cinderConeBiome()
        {
            return cinderConeBiome;
        }

        public BiomeExtension tuffRingBiome()
        {
            return tuffRingBiome;
        }

        public BiomeExtension tuyaBiome()
        {
            return tuyaBiome;
        }

        public NTERiverHydrology.ColumnProfile riverProfile(BlockPos pos)
        {
            return riverProfiles[(pos.getX() & 15) + 16 * (pos.getZ() & 15)];
        }

        public float baseGroundwater(BlockPos pos)
        {
            final long key = columnKey(pos);
            final float cached = baseGroundwaterCache.get(key);
            if (!Float.isNaN(cached))
            {
                return cached;
            }
            final float sampled = baseGroundwaterLayer.getValue((pos.getX() & 15) / 16f, (pos.getZ() & 15) / 16f);
            baseGroundwaterCache.put(key, sampled);
            return sampled;
        }

        public float averageGroundwater(BlockPos pos)
        {
            final long key = columnKey(pos);
            final float cached = averageGroundwaterCache.get(key);
            if (!Float.isNaN(cached))
            {
                return cached;
            }
            final float sampled = Math.min(baseGroundwater(pos) + chunkData.getRainfall(pos), 500f);
            averageGroundwaterCache.put(key, sampled);
            return sampled;
        }

        public float rainVariance(long levelSeed, BlockPos pos)
        {
            final long key = columnKey(pos);
            final float cached = rainVarianceCache.get(key);
            if (!Float.isNaN(cached))
            {
                return cached;
            }
            final float sampled = NTE121ClimateHelpers.getRainVariance(levelSeed, generator, pos);
            rainVarianceCache.put(key, sampled);
            return sampled;
        }

        private static long columnKey(BlockPos pos)
        {
            return ((long) pos.getX() << 32) ^ (pos.getZ() & 0xffffffffL);
        }
    }

    public interface Scope extends AutoCloseable
    {
        @Override
        void close();
    }

    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();

    private NTESurfaceContext()
    {
    }

    public static Scope open(ChunkGenerator generator, ChunkData chunkData, int[] surfaceHeight, ChunkPos chunkPos, BiomeExtension cinderConeBiome, BiomeExtension tuffRingBiome, BiomeExtension tuyaBiome, NTERiverHydrology.ColumnProfile[] riverProfiles)
    {
        CURRENT.set(new Context(generator, chunkData, surfaceHeight, chunkPos, cinderConeBiome, tuffRingBiome, tuyaBiome, riverProfiles));
        return CURRENT::remove;
    }

    public static Context current()
    {
        return CURRENT.get();
    }

    private static LerpFloatLayer createModifiedBaseGroundwaterLayer(ChunkGenerator generator, int[] surfaceHeight, ChunkPos chunkPos)
    {
        final BlockPos corner00 = new BlockPos(chunkPos.getMinBlockX(), 0, chunkPos.getMinBlockZ());
        final BlockPos corner01 = new BlockPos(chunkPos.getMinBlockX(), 0, chunkPos.getMaxBlockZ());
        final BlockPos corner10 = new BlockPos(chunkPos.getMaxBlockX(), 0, chunkPos.getMinBlockZ());
        final BlockPos corner11 = new BlockPos(chunkPos.getMaxBlockX(), 0, chunkPos.getMaxBlockZ());

        return new LerpFloatLayer(
            modifyBaseGroundwaterPoint(surfaceHeight[0], NTE121ClimateHelpers.getBaseGroundwater(generator, corner00)),
            modifyBaseGroundwaterPoint(surfaceHeight[240], NTE121ClimateHelpers.getBaseGroundwater(generator, corner01)),
            modifyBaseGroundwaterPoint(surfaceHeight[15], NTE121ClimateHelpers.getBaseGroundwater(generator, corner10)),
            modifyBaseGroundwaterPoint(surfaceHeight[255], NTE121ClimateHelpers.getBaseGroundwater(generator, corner11))
        );
    }

    private static float modifyBaseGroundwaterPoint(int height, float startingWater)
    {
        if (startingWater == Float.NEGATIVE_INFINITY)
        {
            return 0f;
        }
        return startingWater * Mth.clampedMap(height, SEA_LEVEL_Y + 10, SEA_LEVEL_Y + 25, 1f, 0f);
    }
}
