package com.newterraearth.tfe.mixin;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.world.biome.BiomeExtension;

import com.newterraearth.tfe.world.NTEBiomeExtensionAccess;
import com.newterraearth.tfe.world.NTEConfiguredPlantFeatureFilter;
import com.newterraearth.tfe.world.river.NTERiverBlendType;
import com.newterraearth.tfe.world.shore.NTEShoreBlendType;
import com.newterraearth.tfe.world.volcano.NTECenteredFeatureBlendType;

import static net.dries007.tfc.world.TFCChunkGenerator.SEA_LEVEL_Y;

@Mixin(value = BiomeExtension.class, remap = false)
public class BiomeExtensionMixin implements NTEBiomeExtensionAccess
{
    @Shadow @Nullable private List<HolderSet<PlacedFeature>> flattenedFeatures;
    @Shadow @Nullable private Set<PlacedFeature> flattenedFeatureSet;

    @Unique private NTERiverBlendType tfe$riverBlendType;
    @Unique private NTEShoreBlendType tfe$shoreBlendType = NTEShoreBlendType.NONE;
    @Unique private int tfe$shoreBaseHeight = SEA_LEVEL_Y;
    @Unique private NTECenteredFeatureBlendType tfe$centeredFeatureBlendType = NTECenteredFeatureBlendType.NONE;
    @Unique private int tfe$centeredFeatureRarity;
    @Unique private float tfe$centeredFeatureFrequency;
    @Unique private int tfe$centeredFeatureRockHeight;
    @Unique private int tfe$centeredFeatureBaseHeight;
    @Unique private int tfe$centeredFeatureScaleHeight;
    @Unique private boolean tfe$centeredFeatureIce;

    @Override
    public NTERiverBlendType tfe$getRiverBlendType()
    {
        if (tfe$riverBlendType == null)
        {
            return NTERiverBlendType.fromLegacy(((BiomeExtension) (Object) this).riverBlendType());
        }
        return tfe$riverBlendType;
    }

    @Override
    public void tfe$setRiverBlendType(NTERiverBlendType blendType)
    {
        tfe$riverBlendType = blendType;
    }

    @Override
    public NTEShoreBlendType tfe$getShoreBlendType()
    {
        return tfe$shoreBlendType;
    }

    @Override
    public void tfe$setShoreBlendType(NTEShoreBlendType blendType)
    {
        tfe$shoreBlendType = blendType;
    }

    @Override
    public int tfe$getShoreBaseHeight()
    {
        return tfe$shoreBaseHeight;
    }

    @Override
    public void tfe$setShoreBaseHeight(int shoreBaseHeight)
    {
        tfe$shoreBaseHeight = shoreBaseHeight;
    }

    @Override
    public NTECenteredFeatureBlendType tfe$getCenteredFeatureBlendType()
    {
        return tfe$centeredFeatureBlendType;
    }

    @Override
    public void tfe$setCenteredFeatureBlendType(NTECenteredFeatureBlendType blendType)
    {
        tfe$centeredFeatureBlendType = blendType;
    }

    @Override
    public int tfe$getCenteredFeatureRarity()
    {
        return tfe$centeredFeatureRarity;
    }

    @Override
    public void tfe$setCenteredFeatureRarity(int rarity)
    {
        tfe$centeredFeatureRarity = rarity;
    }

    @Override
    public float tfe$getCenteredFeatureFrequency()
    {
        return tfe$centeredFeatureFrequency;
    }

    @Override
    public void tfe$setCenteredFeatureFrequency(float frequency)
    {
        tfe$centeredFeatureFrequency = frequency;
    }

    @Override
    public int tfe$getCenteredFeatureRockHeight()
    {
        return tfe$centeredFeatureRockHeight;
    }

    @Override
    public void tfe$setCenteredFeatureRockHeight(int rockHeight)
    {
        tfe$centeredFeatureRockHeight = rockHeight;
    }

    @Override
    public int tfe$getCenteredFeatureBaseHeight()
    {
        return tfe$centeredFeatureBaseHeight;
    }

    @Override
    public void tfe$setCenteredFeatureBaseHeight(int baseHeight)
    {
        tfe$centeredFeatureBaseHeight = baseHeight;
    }

    @Override
    public int tfe$getCenteredFeatureScaleHeight()
    {
        return tfe$centeredFeatureScaleHeight;
    }

    @Override
    public void tfe$setCenteredFeatureScaleHeight(int scaleHeight)
    {
        tfe$centeredFeatureScaleHeight = scaleHeight;
    }

    @Override
    public boolean tfe$getCenteredFeatureIce()
    {
        return tfe$centeredFeatureIce;
    }

    @Override
    public void tfe$setCenteredFeatureIce(boolean icy)
    {
        tfe$centeredFeatureIce = icy;
    }

    @Inject(method = "getFlattenedFeatures", at = @At("RETURN"), cancellable = true, require = 0)
    private void tfe$filterDisabledConfiguredPlantFeatures(Biome biome, CallbackInfoReturnable<List<HolderSet<PlacedFeature>>> cir)
    {
        final List<HolderSet<PlacedFeature>> filtered = NTEConfiguredPlantFeatureFilter.filter(cir.getReturnValue());
        if (filtered != cir.getReturnValue())
        {
            flattenedFeatures = filtered;
            flattenedFeatureSet = filtered.stream().flatMap(HolderSet::stream).map(Holder::value).collect(Collectors.toSet());
            cir.setReturnValue(filtered);
        }
    }
}
