package com.newterraearth.tfe.world;

import com.newterraearth.tfe.world.river.NTERiverBlendType;
import com.newterraearth.tfe.world.shore.NTEShoreBlendType;
import com.newterraearth.tfe.world.volcano.NTECenteredFeatureBlendType;

public interface NTEBiomeExtensionAccess
{
    NTERiverBlendType tfe$getRiverBlendType();

    void tfe$setRiverBlendType(NTERiverBlendType blendType);

    NTEShoreBlendType tfe$getShoreBlendType();

    void tfe$setShoreBlendType(NTEShoreBlendType blendType);

    int tfe$getShoreBaseHeight();

    void tfe$setShoreBaseHeight(int shoreBaseHeight);

    NTECenteredFeatureBlendType tfe$getCenteredFeatureBlendType();

    void tfe$setCenteredFeatureBlendType(NTECenteredFeatureBlendType blendType);

    int tfe$getCenteredFeatureRarity();

    void tfe$setCenteredFeatureRarity(int rarity);

    float tfe$getCenteredFeatureFrequency();

    void tfe$setCenteredFeatureFrequency(float frequency);

    int tfe$getCenteredFeatureRockHeight();

    void tfe$setCenteredFeatureRockHeight(int rockHeight);

    int tfe$getCenteredFeatureBaseHeight();

    void tfe$setCenteredFeatureBaseHeight(int baseHeight);

    int tfe$getCenteredFeatureScaleHeight();

    void tfe$setCenteredFeatureScaleHeight(int scaleHeight);

    boolean tfe$getCenteredFeatureIce();

    void tfe$setCenteredFeatureIce(boolean icy);
}
