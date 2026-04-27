package com.newterraearth.tfe.mixin;

import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.newterraearth.tfe.common.entity.NTEConfiguredSpawnFilter;

@Mixin(MobSpawnSettings.class)
public abstract class MobSpawnSettingsMixin
{
    @Inject(method = "getMobs", at = @At("RETURN"), cancellable = true)
    private void tfe$filterDisabledConfiguredEntities(MobCategory category, CallbackInfoReturnable<WeightedRandomList<MobSpawnSettings.SpawnerData>> cir)
    {
        cir.setReturnValue(NTEConfiguredSpawnFilter.filter(cir.getReturnValue()));
    }
}
