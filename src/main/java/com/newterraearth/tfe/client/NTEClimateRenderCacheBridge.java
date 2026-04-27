package com.newterraearth.tfe.client;

public interface NTEClimateRenderCacheBridge
{
    float getAverageSeaLevelTemperature();

    float getInstantTemperature();

    float getAverageRainfall();

    float getRainVariance();

    float getInstantRainfall();

    float getBaseGroundwater();

    float getAverageGroundwater();

    float getInstantGroundwater();

    float getHemisphereScale();
}
