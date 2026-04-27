package com.newterraearth.tfe.mixin;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.dries007.tfc.common.blocks.plant.fruit.Lifecycle;
import net.dries007.tfc.common.blocks.plant.fruit.SeasonalPlantBlock;
import net.dries007.tfc.util.calendar.Month;
import net.dries007.tfc.util.climate.ClimateRange;

@Mixin(value = SeasonalPlantBlock.class, remap = false)
public interface SeasonalPlantBlockAccessor
{
    @Accessor("climateRange")
    Supplier<ClimateRange> tfe$getClimateRange();

    @Invoker("getLifecycleForMonth")
    Lifecycle tfe$invokeGetLifecycleForMonth(Month month);
}
