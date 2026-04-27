package com.newterraearth.tfe.world.feature.plant;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.material.Fluid;

import net.dries007.tfc.common.fluids.FluidHelpers;
import net.dries007.tfc.common.fluids.TFCFluids;
import net.dries007.tfc.util.EnvironmentHelpers;
import net.dries007.tfc.world.feature.BlockConfig;

import com.newterraearth.tfe.common.block.plant.NTERotatableWaterPlantBlock;

public class NTERotatableWaterPlantFeature extends Feature<BlockConfig<NTERotatableWaterPlantBlock>>
{
    public static final Codec<BlockConfig<NTERotatableWaterPlantBlock>> CODEC = BlockConfig.codec(block -> block instanceof NTERotatableWaterPlantBlock plant ? plant : null, "Must be a " + NTERotatableWaterPlantBlock.class.getSimpleName());

    public NTERotatableWaterPlantFeature(Codec<BlockConfig<NTERotatableWaterPlantBlock>> codec)
    {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<BlockConfig<NTERotatableWaterPlantBlock>> context)
    {
        final RandomSource random = context.random();
        final WorldGenLevel level = context.level();
        final BlockPos pos = context.origin().offset(0, random.nextInt(12), 0);
        if (!EnvironmentHelpers.isWorldgenReplaceable(level, pos))
        {
            return false;
        }

        BlockState state = context.config().block().defaultBlockState();
        for (Direction direction : Direction.allShuffled(RandomSource.create()))
        {
            state = state.setValue(NTERotatableWaterPlantBlock.FACING, direction);
            if (state.canSurvive(level, pos))
            {
                final Fluid fluid = level.getFluidState(pos).getType();
                final BlockState fluidState = FluidHelpers.fillWithFluid(state, fluid);
                if (fluidState != null)
                {
                    final boolean open = fluid.isSame(TFCFluids.SALT_WATER.getSource());
                    setBlock(level, pos, fluidState.setValue(NTERotatableWaterPlantBlock.OPEN, open));
                    return true;
                }
            }
        }
        return false;
    }
}
