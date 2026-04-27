package com.newterraearth.tfe.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.dries007.tfc.world.feature.plant.FruitTreeFeature;

import com.newterraearth.tfe.config.NTECommonConfig;

@Mixin(FruitTreeFeature.class)
public abstract class FruitTreeFeatureMixin
{
    // TFC's feature override is remapped to a runtime obfuscated name in release jars.
    @Inject(method = "place", at = @At("HEAD"), cancellable = true, require = 0)
    private void tfe$disableConfiguredFruitTrees(FeaturePlaceContext<BlockStateConfiguration> context, CallbackInfoReturnable<Boolean> cir)
    {
        final ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(context.config().state.getBlock());
        if (!NTECommonConfig.isFruitTreeEnabled(blockId))
        {
            cir.setReturnValue(false);
        }
    }
}
