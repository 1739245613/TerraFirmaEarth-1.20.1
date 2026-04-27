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

import net.dries007.tfc.common.blocks.plant.fruit.FruitTreeBranchBlock;
import net.dries007.tfc.common.blocks.plant.fruit.FruitTreeSaplingBlock;
import net.dries007.tfc.common.blocks.soil.FarmlandBlock;
import net.dries007.tfc.util.climate.ClimateRange;

import com.newterraearth.tfe.config.NTECommonConfig;
import com.newterraearth.tfe.world.NTESeasonalHelpers;

@Mixin(value = FruitTreeBranchBlock.class, remap = false)
public abstract class FruitTreeBranchBlockMixin
{
    @Shadow @Final private Supplier<ClimateRange> climateRange;

    @Shadow public abstract void addExtraInfo(List<Component> text);

    /**
     * @author Codex
     * @reason Fruit tree branch overlays should use the tree base/root climate, matching leaves and saplings.
     */
    @Overwrite(remap = false)
    public void addHoeOverlayInfo(Level level, BlockPos pos, BlockState state, List<Component> text, boolean isDebug)
    {
        final ClimateRange range = climateRange.get();
        final BlockPos stemPos = NTESeasonalHelpers.getFruitTreeStemPos(level, pos);

        text.add(FarmlandBlock.getHydrationTooltip(level, stemPos, range, false, NTESeasonalHelpers.getFruitBushHydrationFromRootPos(level, stemPos.below())));
        NTESeasonalHelpers.addAverageHydrationTooltipIfNeeded(text, level, stemPos.below(), range, false, NTECommonConfig.useCurrentRainfallForFruit());
        text.add(FarmlandBlock.getAverageTemperatureTooltip(level, stemPos, range, false));
        if (FruitTreeSaplingBlock.maySplice(level, pos.above(), level.getBlockState(pos.above())))
        {
            text.add(Component.translatable("tfc.tooltip.fruit_tree.sapling_splice"));
        }
        addExtraInfo(text);
    }
}
