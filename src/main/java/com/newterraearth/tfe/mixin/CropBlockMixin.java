package com.newterraearth.tfe.mixin;

import java.util.List;
import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.common.blockentities.CropBlockEntity;
import net.dries007.tfc.common.blockentities.IFarmland;
import net.dries007.tfc.common.blocks.crop.CropBlock;
import net.dries007.tfc.common.blocks.soil.FarmlandBlock;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.climate.ClimateRange;

import com.newterraearth.tfe.config.NTECommonConfig;
import com.newterraearth.tfe.world.NTESeasonalHelpers;

@Mixin(value = CropBlock.class, remap = false)
public abstract class CropBlockMixin
{
    @Shadow @Final protected Supplier<ClimateRange> climateRange;

    /**
     * @author Codex
     * @reason Crop overlays should match the configured rainfall hydration mode.
     */
    @Overwrite(remap = false)
    public void addHoeOverlayInfo(Level level, BlockPos pos, BlockState state, List<Component> text, boolean isDebug)
    {
        final ClimateRange range = climateRange.get();
        final BlockPos sourcePos = pos.below();

        text.add(FarmlandBlock.getTemperatureTooltip(level, pos, range, false));
        text.add(FarmlandBlock.getHydrationTooltip(level, sourcePos, range, false, NTESeasonalHelpers.getConfiguredCropHydration(level, sourcePos)));
        NTESeasonalHelpers.addAverageHydrationTooltipIfNeeded(text, level, sourcePos, range, false, NTECommonConfig.useCurrentRainfallForCrops());

        IFarmland farmland = null;
        if (level.getBlockEntity(sourcePos) instanceof IFarmland found)
        {
            farmland = found;
        }
        else if (level.getBlockEntity(sourcePos.below()) instanceof IFarmland found)
        {
            farmland = found;
        }
        if (farmland != null)
        {
            farmland.addTooltipInfo(text);
        }

        if (level.getBlockEntity(pos) instanceof CropBlockEntity crop)
        {
            if (isDebug)
            {
                text.add(Component.literal(String.format("[Debug] Growth = %.4f Yield = %.4f Expiry = %.4f Last Tick = %d Delta = %d", crop.getGrowth(), crop.getYield(), crop.getExpiry(), crop.getLastGrowthTick(), Calendars.get(level).getTicks() - crop.getLastGrowthTick())));
            }
            if (crop.getGrowth() >= 1)
            {
                text.add(Component.translatable("tfc.tooltip.farmland.mature"));
            }
        }
    }
}
