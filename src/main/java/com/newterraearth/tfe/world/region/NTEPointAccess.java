package com.newterraearth.tfe.world.region;

public interface NTEPointAccess
{
    byte nte$getHotSpotAge();

    void nte$setHotSpotAge(byte age);

    byte nte$getDistanceToWestCoast();

    void nte$setDistanceToWestCoast(byte distanceToWestCoast);

    float nte$getRainfallVariance();

    void nte$setRainfallVariance(float rainfallVariance);

    boolean nte$isSurfaceRockKarst();

    void nte$setSurfaceRockKarst(boolean karst);
}
