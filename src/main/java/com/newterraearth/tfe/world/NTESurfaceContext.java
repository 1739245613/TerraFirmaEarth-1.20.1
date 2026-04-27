package com.newterraearth.tfe.world;

import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkGenerator;

import net.dries007.tfc.world.biome.BiomeExtension;

public final class NTESurfaceContext
{
    public static final class Context
    {
        private final ChunkGenerator generator;
        private final BiomeExtension cinderConeBiome;
        private final BiomeExtension tuffRingBiome;
        private final BiomeExtension tuyaBiome;
        private final Long2FloatOpenHashMap baseGroundwaterCache;
        private final Long2FloatOpenHashMap averageGroundwaterCache;
        private final Long2FloatOpenHashMap rainVarianceCache;

        private Context(ChunkGenerator generator, BiomeExtension cinderConeBiome, BiomeExtension tuffRingBiome, BiomeExtension tuyaBiome)
        {
            this.generator = generator;
            this.cinderConeBiome = cinderConeBiome;
            this.tuffRingBiome = tuffRingBiome;
            this.tuyaBiome = tuyaBiome;
            this.baseGroundwaterCache = new Long2FloatOpenHashMap();
            this.averageGroundwaterCache = new Long2FloatOpenHashMap();
            this.rainVarianceCache = new Long2FloatOpenHashMap();
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

        public float baseGroundwater(BlockPos pos)
        {
            final long key = columnKey(pos);
            final float cached = baseGroundwaterCache.get(key);
            if (!Float.isNaN(cached))
            {
                return cached;
            }
            final float sampled = NTE121ClimateHelpers.getBaseGroundwater(generator, pos);
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
            final float sampled = NTE121ClimateHelpers.getAverageGroundwater(generator, pos);
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

    public static Scope open(ChunkGenerator generator, BiomeExtension cinderConeBiome, BiomeExtension tuffRingBiome, BiomeExtension tuyaBiome)
    {
        CURRENT.set(new Context(generator, cinderConeBiome, tuffRingBiome, tuyaBiome));
        return CURRENT::remove;
    }

    public static Context current()
    {
        return CURRENT.get();
    }
}
