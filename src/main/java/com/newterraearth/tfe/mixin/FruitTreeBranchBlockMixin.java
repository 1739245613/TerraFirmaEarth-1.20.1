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
import net.dries007.tfc.common.blocks.plant.fruit.GrowingFruitTreeBranchBlock;
import net.dries007.tfc.util.climate.ClimateRange;

import com.newterraearth.tfe.world.NTESeasonalHelpers;

@Mixin(value = FruitTreeBranchBlock.class, remap = false)
public abstract class FruitTreeBranchBlockMixin
{
    @Shadow @Final private Supplier<ClimateRange> climateRange;

    /**
     * @author Codex
     * @reason Fruit tree branch overlays should use the tree base/root climate, matching leaves and saplings.
     */
    @Overwrite(remap = false)
    public void addHoeOverlayInfo(Level level, BlockPos pos, BlockState state, List<Component> text, boolean isDebug)
    {
        final ClimateRange range = climateRange.get();
        final BlockPos stemPos = NTESeasonalHelpers.getFruitTreeStemPos(level, pos);
        final BlockPos rootPos = stemPos.below();

        NTESeasonalHelpers.addPlantClimateTooltips(text, level, stemPos, rootPos, range, NTESeasonalHelpers.getFruitBushHydrationFromRootPos(level, rootPos));
        if (FruitTreeSaplingBlock.maySplice(level, pos.above(), level.getBlockState(pos.above())))
        {
            text.add(Component.translatable("tfc.tooltip.fruit_tree.sapling_splice"));
        }
        if ((Object) this instanceof GrowingFruitTreeBranchBlock)
        {
            if (state.getValue(FruitTreeBranchBlock.STAGE) >= 3)
            {
                text.add(tfe$plantStatus("tfe.jade.plant.growth_complete"));
            }
            else
            {
                final boolean climateValid = state.getValue(GrowingFruitTreeBranchBlock.NATURAL)
                    || range.checkBoth(
                        NTESeasonalHelpers.getFruitBushHydrationFromRootPos(level, rootPos),
                        NTESeasonalHelpers.getPlantTemperature(level, stemPos),
                        false
                    );
                text.add(tfe$plantStatus(climateValid ? "tfe.jade.plant.growing" : "tfe.jade.plant.waiting_climate"));
            }
        }
        else
        {
            text.add(tfe$plantStatus("tfe.jade.plant.growth_complete"));
        }
    }

    private static Component tfe$plantStatus(String translationKey)
    {
        return Component.translatable("tfe.jade.plant_status", Component.translatable(translationKey));
    }
}
