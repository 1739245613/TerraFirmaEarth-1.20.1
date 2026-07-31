package com.newterraearth.tfe.mixin;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.dries007.tfc.world.feature.BoulderConfig;
import net.dries007.tfc.world.feature.BouldersFeature;

/** Prevent both full and baby boulders from starting in river or volcanic biomes. */
@Mixin(BouldersFeature.class)
public abstract class BouldersFeatureMixin
{
    @Unique private static final TagKey<Biome> TFE$IS_RIVER = TagKey.create(
        Registries.BIOME,
        new ResourceLocation("tfc", "is_river")
    );
    @Unique private static final TagKey<Biome> TFE$IS_VOLCANIC = TagKey.create(
        Registries.BIOME,
        new ResourceLocation("tfc", "is_volcanic")
    );

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void tfe$rejectExcludedBiome(
        FeaturePlaceContext<BoulderConfig> context,
        CallbackInfoReturnable<Boolean> cir
    )
    {
        final net.minecraft.core.Holder<Biome> biome = context.level().getBiome(context.origin());
        if (biome.is(TFE$IS_RIVER) || biome.is(TFE$IS_VOLCANIC))
        {
            cir.setReturnValue(false);
        }
    }
}
