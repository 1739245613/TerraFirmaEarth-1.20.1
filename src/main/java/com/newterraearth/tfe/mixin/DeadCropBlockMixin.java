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

import net.dries007.tfc.common.blocks.crop.DeadCropBlock;
import net.dries007.tfc.util.climate.ClimateRange;

import com.newterraearth.tfe.world.NTESeasonalHelpers;

@Mixin(value = DeadCropBlock.class, remap = false)
public abstract class DeadCropBlockMixin
{
    @Shadow @Final private Supplier<ClimateRange> climateRange;

    /**
     * @author Codex
     * @reason Dead crop overlays should match the configured rainfall hydration mode and TFE current temperature.
     */
    @Overwrite(remap = false)
    public void addHoeOverlayInfo(Level level, BlockPos pos, BlockState state, List<Component> text, boolean isDebug)
    {
        final ClimateRange range = climateRange.get();
        final BlockPos sourcePos = pos.below();
        NTESeasonalHelpers.addPlantClimateTooltips(text, level, pos, sourcePos, range, NTESeasonalHelpers.getConfiguredCropHydration(level, sourcePos));
        if (state.getValue(DeadCropBlock.MATURE))
        {
            text.add(Component.translatable("tfc.tooltip.farmland.mature"));
        }
    }
}
