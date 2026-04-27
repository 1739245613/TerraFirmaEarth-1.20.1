package com.newterraearth.tfe.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blocks.crop.WildCropBlock;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.calendar.Month;

import com.newterraearth.tfe.world.NTESeasonalHelpers;

@Mixin(value = WildCropBlock.class, remap = false)
public abstract class WildCropBlockMixin
{
    /**
     * @author Codex
     * @reason Wild crop placement needs hemisphere-aware maturity.
     */
    @Nullable
    @Overwrite(remap = true)
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        final int month = NTESeasonalHelpers.getHemispheralCalendarMonthOfYear(context.getLevel(), context.getClickedPos()).ordinal();
        final boolean mature = month >= Month.JUNE.ordinal() && month <= Month.OCTOBER.ordinal();
        return ((WildCropBlock) (Object) this).defaultBlockState().setValue(WildCropBlock.MATURE, mature);
    }

    /**
     * @author Codex
     * @reason Wild crop maturity should flip by local hemisphere month.
     */
    @Overwrite(remap = true)
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand)
    {
        final int month = NTESeasonalHelpers.getHemispheralCalendarMonthOfYear(level, pos).ordinal();
        final boolean mature = month >= Month.JUNE.ordinal() && month <= Month.OCTOBER.ordinal();
        if (state.getValue(WildCropBlock.MATURE) != mature)
        {
            level.setBlockAndUpdate(pos, state.setValue(WildCropBlock.MATURE, mature));
        }
    }

    /**
     * @author Codex
     * @reason Use the dedicated wild crop survival tag that the 1.21 branch expects.
     */
    @Overwrite(remap = true)
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos)
    {
        return Helpers.isBlock(level.getBlockState(pos), TFCTags.Blocks.WILD_CROP_GROWS_ON);
    }
}
