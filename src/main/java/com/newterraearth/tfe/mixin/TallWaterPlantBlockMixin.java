package com.newterraearth.tfe.mixin;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.plant.TFCTallGrassBlock;
import net.dries007.tfc.common.blocks.plant.TallWaterPlantBlock;
import net.dries007.tfc.common.fluids.FluidHelpers;
import net.dries007.tfc.common.fluids.FluidProperty;
import net.dries007.tfc.common.fluids.IFluidLoggable;
import net.dries007.tfc.util.Helpers;

@Mixin(value = TallWaterPlantBlock.class, remap = false)
public abstract class TallWaterPlantBlockMixin extends TFCTallGrassBlock implements IFluidLoggable
{
    public abstract FluidProperty getFluidProperty();

    protected TallWaterPlantBlockMixin(ExtendedProperties properties)
    {
        super(properties);
    }

    /**
     * @author Codex
     * @reason Align 1.20 tall water plant survival with the 1.21 implementation used by shore vegetation.
     */
    @Overwrite(remap = true)
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
    {
        final BlockState belowState = level.getBlockState(pos.below());
        if (state.getValue(PART) == Part.LOWER)
        {
            if (state.getValue(getFluidProperty()).getFluid().getFluidType().isAir())
            {
                return false;
            }
            if (Helpers.isBlock(state, TFCTags.Blocks.HALOPHYTE))
            {
                return Helpers.isBlock(belowState, TFCTags.Blocks.HALOPHYTE_PLANTABLE_ON);
            }
            return Helpers.isBlock(belowState, TFCTags.Blocks.SEA_BUSH_PLANTABLE_ON);
        }
        if (state.getBlock() != (Object) this)
        {
            return Helpers.isBlock(belowState, TFCTags.Blocks.SEA_BUSH_PLANTABLE_ON);
        }
        return belowState.getBlock() == (Object) this && belowState.getValue(PART) == Part.LOWER;
    }

    /**
     * @author Codex
     * @reason Align 1.20 tall water plant placement checks with the 1.21 runtime required by shore vegetation.
     */
    @Overwrite(remap = true)
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        final BlockPos pos = context.getClickedPos();
        final FluidState fluidState = context.getLevel().getFluidState(pos);
        BlockState state = defaultBlockState();

        if (getFluidProperty().canContain(fluidState.getType()))
        {
            state = state.setValue(getFluidProperty(), getFluidProperty().keyFor(fluidState.getType()));
        }
        else
        {
            return null;
        }

        final FluidState aboveFluidState = context.getLevel().getFluidState(pos.above());
        if (!getFluidProperty().canContain(aboveFluidState.getType()))
        {
            return null;
        }

        return pos.getY() < context.getLevel().getMaxBuildHeight() - 1 && context.getLevel().getBlockState(pos.above()).canBeReplaced(context) ? state : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack)
    {
        final BlockState upper = FluidHelpers.fillWithFluid(defaultBlockState().setValue(PART, Part.UPPER), level.getFluidState(pos.above()).getType());
        if (upper == null)
        {
            level.destroyBlock(pos, true);
            return;
        }
        level.setBlockAndUpdate(pos.above(), upper);
    }

    /**
     * @author Codex
     * @reason Remove the 1.20 random AGE assignment so tall water plants match the 1.21 worldgen/runtime state layout.
     */
    @Overwrite(remap = false)
    public void placeTwoHalves(LevelAccessor level, BlockPos pos, int flags, RandomSource random)
    {
        final BlockPos posAbove = pos.above();
        final Fluid fluidBottom = level.getFluidState(pos).getType();
        final Fluid fluidTop = level.getFluidState(posAbove).getType();
        if (!fluidBottom.isSame(Fluids.EMPTY))
        {
            final BlockState state = FluidHelpers.fillWithFluid(defaultBlockState().setValue(PART, Part.LOWER), fluidBottom);
            final BlockState stateAbove = FluidHelpers.fillWithFluid(defaultBlockState().setValue(PART, Part.UPPER), fluidTop);
            if (state != null && stateAbove != null)
            {
                level.setBlock(pos, state, flags);
                level.setBlock(posAbove, stateAbove, flags);
            }
        }
    }
}
