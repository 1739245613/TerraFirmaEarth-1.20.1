package com.newterraearth.tfe.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.dries007.tfc.common.blockentities.CropBlockEntity;
import net.dries007.tfc.common.blockentities.FarmlandBlockEntity;
import net.dries007.tfc.common.blockentities.IFarmland;
import net.dries007.tfc.common.blocks.crop.CropHelpers;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.calendar.ICalendar;
import net.dries007.tfc.util.Fertilizer;

import com.newterraearth.tfe.world.NTESoilFertility;
import com.newterraearth.tfe.world.NTESeasonalHelpers;

@Mixin(value = CropHelpers.class, remap = false, priority = 900)
public abstract class CropHelpersMixin
{
    @Redirect(
        method = "growthTickStep",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/common/blocks/soil/FarmlandBlock;getHydration(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;)I"
        ),
        remap = false,
        require = 0
    )
    private static int tfe$useConfiguredCropHydration(
        LevelAccessor levelAccessor,
        BlockPos sourcePos,
        Level level,
        BlockPos pos,
        BlockState state,
        RandomSource random,
        long fromTick,
        long toTick,
        CropBlockEntity crop
    )
    {
        final var calendar = Calendars.get(level);
        return NTESeasonalHelpers.getConfiguredCropHydration(level, sourcePos, calendar.ticksToCalendarTicks(toTick), calendar.getCalendarDaysInMonth());
    }

    @Redirect(
        method = "growthTickStep",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/util/climate/Climate;getTemperature(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/dries007/tfc/util/calendar/ICalendar;J)F"
        ),
        remap = false,
        require = 0
    )
    private static float tfe$usePlantTemperatureForCropGrowth(Level level, BlockPos pos, ICalendar calendar, long calendarTick)
    {
        return NTESeasonalHelpers.getPlantTemperature(level, pos, calendarTick, calendar.getCalendarDaysInMonth());
    }

    @Redirect(
        method = "growthTickStep",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/common/blockentities/IFarmland;getNutrient(Lnet/dries007/tfc/common/blockentities/FarmlandBlockEntity$NutrientType;)F"
        ),
        remap = false,
        require = 0
    )
    private static float tfe$scaleAvailableNutrients(
        IFarmland farmland,
        FarmlandBlockEntity.NutrientType type,
        Level level,
        BlockPos pos,
        BlockState state,
        RandomSource random,
        long fromTick,
        long toTick,
        CropBlockEntity crop
    )
    {
        return farmland.getNutrient(type) * modifierAt(level, pos.below());
    }

    @Redirect(
        method = "growthTickStep",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/common/blockentities/IFarmland;consumeNutrientAndResupplyOthers(Lnet/dries007/tfc/common/blockentities/FarmlandBlockEntity$NutrientType;F)F"
        ),
        remap = false,
        require = 0
    )
    private static float tfe$scaleConsumedNutrients(
        IFarmland farmland,
        FarmlandBlockEntity.NutrientType type,
        float amount,
        Level level,
        BlockPos pos,
        BlockState state,
        RandomSource random,
        long fromTick,
        long toTick,
        CropBlockEntity crop
    )
    {
        return farmland.consumeNutrientAndResupplyOthers(type, amount) * modifierAt(level, pos.below());
    }

    @Redirect(
        method = "useFertilizer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/common/blocks/crop/CropHelpers;minAmountRequiredToNextFillBar(Lnet/dries007/tfc/common/blockentities/IFarmland;Lnet/dries007/tfc/util/Fertilizer;Lnet/dries007/tfc/common/blockentities/FarmlandBlockEntity$NutrientType;I)I"
        ),
        remap = false,
        require = 0
    )
    private static int tfe$scaleShiftFertilizerFill(
        IFarmland farmland,
        Fertilizer fertilizer,
        FarmlandBlockEntity.NutrientType type,
        int prevValue,
        Level level,
        Player player,
        InteractionHand hand,
        BlockPos farmlandPos
    )
    {
        final float amount = fertilizer.getNutrient(type) * modifierAt(level, farmlandPos);
        if (amount > 0 && farmland.getNutrient(type) < 1)
        {
            final int requiredValue = Mth.ceil((1 - farmland.getNutrient(type)) / amount);
            if (prevValue == -1 || requiredValue < prevValue)
            {
                return requiredValue;
            }
        }
        return prevValue;
    }

    @Redirect(
        method = "useFertilizer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/common/blockentities/IFarmland;addNutrients(Lnet/dries007/tfc/util/Fertilizer;F)V"
        ),
        remap = false,
        require = 0
    )
    private static void tfe$scaleAppliedFertilizer(
        IFarmland farmland,
        Fertilizer fertilizer,
        float multiplier,
        Level level,
        Player player,
        InteractionHand hand,
        BlockPos farmlandPos
    )
    {
        farmland.addNutrients(fertilizer, multiplier * modifierAt(level, farmlandPos));
    }

    private static float modifierAt(Level level, BlockPos pos)
    {
        return NTESoilFertility.getModifier(level.getBlockState(pos));
    }
}
