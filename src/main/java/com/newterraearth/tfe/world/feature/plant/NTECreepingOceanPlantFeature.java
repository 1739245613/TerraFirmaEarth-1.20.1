package com.newterraearth.tfe.world.feature.plant;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.material.Fluid;

import net.dries007.tfc.common.fluids.FluidHelpers;
import net.dries007.tfc.common.fluids.TFCFluids;
import net.dries007.tfc.util.EnvironmentHelpers;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.OpenSimplex2D;
import net.dries007.tfc.world.TFCChunkGenerator;

import com.newterraearth.tfe.common.block.plant.NTECreepingWaterPlantBlock;

public class NTECreepingOceanPlantFeature extends Feature<NTECreepingPlantConfig>
{
    public NTECreepingOceanPlantFeature(Codec<NTECreepingPlantConfig> codec)
    {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NTECreepingPlantConfig> context)
    {
        final WorldGenLevel level = context.level();
        final Noise2D tideHeight = highTideNoise(level.getSeed());
        final BlockPos origin = context.origin();
        final BlockState state = context.config().block().defaultBlockState();
        final int radius = context.config().radius();
        final int height = context.config().height();
        final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        boolean placedAny = false;

        for (int x = -radius; x <= radius; x++)
        {
            for (int z = -radius; z <= radius; z++)
            {
                for (int y = 0; y < height; y++)
                {
                    if (x * x + z * z < radius * radius && context.random().nextFloat() < context.config().integrity())
                    {
                        cursor.setWithOffset(origin, x, y, z);
                        if (EnvironmentHelpers.isWorldgenReplaceable(level, cursor) && cursor.getY() <= context.config().heightAboveTide() + tideHeight.noise(cursor.getX(), cursor.getZ()))
                        {
                            final BlockState newState = NTECreepingWaterPlantBlock.updateStateFromSides(level, cursor, state);
                            if (!newState.isAir())
                            {
                                final Fluid fluid = level.getFluidState(cursor).getType();
                                final BlockState fluidState = FluidHelpers.fillWithFluid(newState, fluid);
                                if (fluidState != null)
                                {
                                    final boolean open = fluid.isSame(TFCFluids.SALT_WATER.getSource());
                                    setBlock(level, cursor, fluidState.setValue(NTECreepingWaterPlantBlock.OPEN, open));
                                    placedAny = true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return placedAny;
    }

    private static Noise2D highTideNoise(long seed)
    {
        return new OpenSimplex2D(seed)
            .octaves(3)
            .spread(0.005f)
            .scaled(TFCChunkGenerator.SEA_LEVEL_Y - 6, TFCChunkGenerator.SEA_LEVEL_Y + 6)
            .clamped(TFCChunkGenerator.SEA_LEVEL_Y, TFCChunkGenerator.SEA_LEVEL_Y + 4)
            .add(new OpenSimplex2D(seed).spread(0.03));
    }
}
