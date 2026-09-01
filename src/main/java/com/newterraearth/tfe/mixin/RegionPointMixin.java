package com.newterraearth.tfe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.dries007.tfc.world.region.Region;

import com.newterraearth.tfe.world.region.NTEPointAccess;

@Mixin(value = Region.Point.class, remap = false)
public abstract class RegionPointMixin implements NTEPointAccess
{
    @Unique private byte tfe$hotSpotAge;
    @Unique private byte tfe$distanceToWestCoast;
    @Unique private float tfe$rainfallVariance;
    @Unique private boolean tfe$surfaceRockKarst;
    @Unique private double tfe$divergence;
    @Unique private byte tfe$distanceToDeepOcean;
    @Unique private byte tfe$distanceToLand;
    @Unique private byte tfe$oceanDepth;
    @Unique private boolean tfe$volcanic;
    @Unique private boolean tfe$barrierIsland;

    @Override
    public byte nte$getHotSpotAge()
    {
        return tfe$hotSpotAge;
    }

    @Override
    public void nte$setHotSpotAge(byte age)
    {
        this.tfe$hotSpotAge = age;
    }

    @Override
    public byte nte$getDistanceToWestCoast()
    {
        return tfe$distanceToWestCoast;
    }

    @Override
    public void nte$setDistanceToWestCoast(byte distanceToWestCoast)
    {
        this.tfe$distanceToWestCoast = distanceToWestCoast;
    }

    @Override
    public float nte$getRainfallVariance()
    {
        return tfe$rainfallVariance;
    }

    @Override
    public void nte$setRainfallVariance(float rainfallVariance)
    {
        this.tfe$rainfallVariance = rainfallVariance;
    }

    @Override
    public boolean nte$isSurfaceRockKarst()
    {
        return tfe$surfaceRockKarst;
    }

    @Override
    public void nte$setSurfaceRockKarst(boolean karst)
    {
        this.tfe$surfaceRockKarst = karst;
    }

    @Override
    public double nte$getDivergence()
    {
        return tfe$divergence;
    }

    @Override
    public void nte$setDivergence(double divergence)
    {
        this.tfe$divergence = divergence;
    }

    @Override
    public byte nte$getDistanceToDeepOcean()
    {
        return tfe$distanceToDeepOcean;
    }

    @Override
    public void nte$setDistanceToDeepOcean(byte distance)
    {
        this.tfe$distanceToDeepOcean = distance;
    }

    @Override
    public byte nte$getDistanceToLand()
    {
        return tfe$distanceToLand;
    }

    @Override
    public void nte$setDistanceToLand(byte distance)
    {
        this.tfe$distanceToLand = distance;
    }

    @Override
    public byte nte$getOceanDepth()
    {
        return tfe$oceanDepth;
    }

    @Override
    public void nte$setOceanDepth(byte depth)
    {
        this.tfe$oceanDepth = depth;
    }

    @Override
    public boolean nte$isVolcanic()
    {
        return tfe$volcanic;
    }

    @Override
    public void nte$setVolcanic(boolean volcanic)
    {
        this.tfe$volcanic = volcanic;
    }

    @Override
    public boolean nte$isBarrierIsland()
    {
        return tfe$barrierIsland;
    }

    @Override
    public void nte$setBarrierIsland(boolean barrierIsland)
    {
        this.tfe$barrierIsland = barrierIsland;
    }
}
