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
}
