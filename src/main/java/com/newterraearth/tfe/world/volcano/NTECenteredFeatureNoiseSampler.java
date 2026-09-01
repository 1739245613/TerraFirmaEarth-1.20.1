package com.newterraearth.tfe.world.volcano;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.biome.BiomeSourceExtension;

import com.newterraearth.tfe.world.NTEBiomeExtensionAccess;
import com.newterraearth.tfe.world.noise.NTECellular2D;

public interface NTECenteredFeatureNoiseSampler
{
    double NOT_PRESENT_RETURN = Integer.MIN_VALUE;

    NTECenteredFeatureNoiseSampler NONE = new NTECenteredFeatureNoiseSampler()
    {
        @Override
        public @Nullable BiomeExtension getCenterBiome(int x, int z, BiomeSourceExtension biomeSource)
        {
            return null;
        }

        @Override
        public boolean isValidBiome(BiomeExtension biome)
        {
            return false;
        }

        @Override
        public float calculateEasing(int x, int z, int rarity)
        {
            return 0;
        }

        @Override
        public @Nullable BlockPos calculateCenter(int x, int y, int z, int rarity)
        {
            return null;
        }
    };

    default double setColumnAndSampleHeight(double heightIn, int x, int z, BiomeSourceExtension biomeSource)
    {
        return heightIn;
    }

    @Nullable
    BiomeExtension getCenterBiome(int x, int z, BiomeSourceExtension biomeSource);

    boolean isValidBiome(BiomeExtension biome);

    default int getRarity(BiomeExtension biome)
    {
        return ((NTEBiomeExtensionAccess) (Object) biome).tfe$getCenteredFeatureRarity();
    }

    default float getFrequency(BiomeExtension biome)
    {
        final float frequency = ((NTEBiomeExtensionAccess) (Object) biome).tfe$getCenteredFeatureFrequency();
        return frequency > 0 ? frequency : (getRarity(biome) == 0 ? 0 : 1f / getRarity(biome));
    }

    default boolean checkCellFrequency(NTECellular2D.Cell cell, float frequency)
    {
        return frequency > 0 && Math.abs(cell.noise()) <= frequency;
    }

    default float calculateEasing(BlockPos pos, BiomeExtension biome)
    {
        return calculateEasing(pos.getX(), pos.getZ(), getRarity(biome));
    }

    float calculateEasing(int x, int z, int rarity);

    @Nullable
    default BlockPos calculateCenter(BlockPos pos, BiomeExtension biome)
    {
        return calculateCenter(pos.getX(), pos.getY(), pos.getZ(), getRarity(biome));
    }

    @Nullable
    BlockPos calculateCenter(int x, int y, int z, int rarity);

    /**
     * Cellular field used by the centered feature.  Only stratovolcanoes use
     * the value directly; the default keeps older feature samplers source
     * compatible.
     */
    @Nullable
    default NTECellular2D getCellularNoise()
    {
        return null;
    }

    /**
     * Returns the selected stratovolcano shape for a cell, when applicable.
     */
    @Nullable
    default NTEVolcanoVariant getVolcanoVariant(NTECellular2D.Cell cell)
    {
        return null;
    }

    default boolean checkCellRarity(NTECellular2D.Cell cell, int rarity)
    {
        if (rarity == 0)
        {
            return false;
        }
        return Math.abs(cell.noise()) <= 1.0 / rarity;
    }
}
