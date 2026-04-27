package com.newterraearth.tfe.mixin;

import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.dries007.tfc.world.noise.Cellular2D;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.OpenSimplex2D;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.settings.RockSettings;
import net.dries007.tfc.world.settings.Settings;

import com.newterraearth.tfe.world.NTESeed;
import com.newterraearth.tfe.world.region.NTERegionGeneratorAccess;
import com.newterraearth.tfe.world.region.NTERegionNoise;

@Mixin(value = RegionGenerator.class, remap = false)
public abstract class RegionGeneratorMixin implements NTERegionGeneratorAccess
{
    @Shadow @Final private long seed;

    @Unique private Settings tfe$settings;
    @Unique private Noise2D tfe$oceanicInfluenceNoise;
    @Unique private Noise2D tfe$rainfallVarianceNoise;
    @Unique private Noise2D tfe$hotSpotAgeNoise;
    @Unique private Noise2D tfe$hotSpotIntensityNoise;
    @Unique private Cellular2D tfe$plateRegionNoise;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void tfe$initExtraRegionNoise(Settings settings, RandomSource random, CallbackInfo ci)
    {
        this.tfe$settings = settings;
        tfe$reinitExtraRegionNoise(seed);
    }

    @Unique
    private void tfe$reinitExtraRegionNoise(long rootLevelSeed)
    {
        final NTESeed rootSeed = NTESeed.of(rootLevelSeed);
        rootSeed.next(); // cellNoise
        rootSeed.next(); // continentNoise
        rootSeed.next(); // temperatureNoise
        this.tfe$oceanicInfluenceNoise = new OpenSimplex2D(rootSeed.next())
            .spread(0.02f);
        rootSeed.next(); // rainfallNoise
        this.tfe$rainfallVarianceNoise = new OpenSimplex2D(rootSeed.next())
            .octaves(2)
            .spread(0.1f)
            .scaled(0f, 20f);
        this.tfe$hotSpotAgeNoise = NTERegionNoise.hotSpotAge(rootSeed.seed()).spread(128);
        this.tfe$hotSpotIntensityNoise = NTERegionNoise.hotSpotIntensity(rootSeed.seed()).spread(128);
        this.tfe$plateRegionNoise = NTERegionNoise.plateRegions(rootSeed.seed()).spread(128);
    }

    @Override
    public void nte$setRootLevelSeed(long rootLevelSeed)
    {
        tfe$reinitExtraRegionNoise(rootLevelSeed);
    }

    @Override
    public Noise2D nte$getHotSpotAgeNoise()
    {
        return tfe$hotSpotAgeNoise;
    }

    @Override
    public Noise2D nte$getHotSpotIntensityNoise()
    {
        return tfe$hotSpotIntensityNoise;
    }

    @Override
    public Cellular2D nte$getPlateRegionNoise()
    {
        return tfe$plateRegionNoise;
    }

    @Override
    public Noise2D nte$getOceanicInfluenceNoise()
    {
        return tfe$oceanicInfluenceNoise;
    }

    @Override
    public Noise2D nte$getRainfallVarianceNoise()
    {
        return tfe$rainfallVarianceNoise;
    }

    @Override
    public Settings nte$getSettings()
    {
        return tfe$settings;
    }

    @Override
    public float nte$continentFactor(int gridX, int gridZ)
    {
        return 1f;
    }

    @Override
    public RockSettings nte$getSurfaceRock(Region.Point point)
    {
        return tfe$settings.rockLayerSettings().sampleAtLayer(point.rock, 0);
    }
}
