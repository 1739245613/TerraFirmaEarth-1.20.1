package com.newterraearth.tfe.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import net.dries007.tfc.common.blockentities.FarmlandBlockEntity;
import net.dries007.tfc.common.blockentities.IFarmland;

import com.newterraearth.tfe.config.NTECommonConfig;
import com.newterraearth.tfe.world.NTESeasonalHelpers;

@Mixin(value = FarmlandBlockEntity.class, remap = false)
public abstract class FarmlandBlockEntityMixin
{
    /**
     * @author Codex
     * @reason Native and addon farmland overlays should both show configured current hydration, and append average hydration when enabled.
     */
    @Overwrite(remap = false)
    public void addHoeOverlayInfo(Level level, BlockPos pos, List<Component> text, boolean includeHydration, boolean includeNutrients)
    {
        if (includeHydration)
        {
            text.add(Component.translatable("tfc.tooltip.farmland.hydration", NTESeasonalHelpers.getConfiguredCropHydration(level, pos)));
            if (NTECommonConfig.useCurrentRainfallForCrops() && !NTESeasonalHelpers.useAverageHydrationInControlledGreenhouse(level, pos))
            {
                text.add(Component.translatable("tfc.tooltip.farmland.average_hydration", NTESeasonalHelpers.getAverageRainHydration(level, pos)));
            }
        }

        if (includeNutrients)
        {
            ((IFarmland) (Object) this).addTooltipInfo(text);
        }
    }
}
