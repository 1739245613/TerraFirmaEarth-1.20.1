package com.newterraearth.tfe.world.surface;

import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.builder.SurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilderFactory;

import com.newterraearth.tfe.world.NTEBiomeNoise;

import static net.dries007.tfc.world.TFCChunkGenerator.SEA_LEVEL_Y;

public class IceSheetShieldVolcanoSurfaceBuilder implements SurfaceBuilder
{
    public static final SurfaceBuilderFactory ICE_SHEET = seed -> new IceSheetShieldVolcanoSurfaceBuilder(
        seed,
        NTEBiomeNoise.glaciatedShieldVolcano(seed),
        max(NTEBiomeNoise.iceSheetSurfaceHeight(seed), NTEBiomeNoise.shieldVolcanoIceSheetSurface(seed)),
        false,
        true,
        SEA_LEVEL_Y
    );
    public static final SurfaceBuilderFactory GLACIATED = seed -> new IceSheetShieldVolcanoSurfaceBuilder(
        seed,
        NTEBiomeNoise.glaciatedShieldVolcano(seed),
        max(NTEBiomeNoise.iceSheetSurfaceHeight(seed), NTEBiomeNoise.shieldVolcanoGlacierSurface(seed)),
        false,
        true,
        SEA_LEVEL_Y + 30
    );

    private final Noise2D baseNoise;
    private final Noise2D iceSurfaceNoise;
    private final boolean hasMoraines;
    private final boolean hasStonyPeaks;
    private final int minFreezingHeight;
    private final SurfaceBuilder baseVolcanoSurfaceBuilder;
    private final SurfaceBuilder shoreSurfaceBuilder;

    public IceSheetShieldVolcanoSurfaceBuilder(long seed, Noise2D baseNoise, Noise2D iceSurfaceNoise, boolean hasMoraines, boolean hasStonyPeaks, int minFreezingHeight)
    {
        this.baseNoise = baseNoise;
        this.iceSurfaceNoise = iceSurfaceNoise;
        this.hasMoraines = hasMoraines;
        this.hasStonyPeaks = hasStonyPeaks;
        this.minFreezingHeight = minFreezingHeight;
        this.baseVolcanoSurfaceBuilder = ShieldVolcanoSurfaceBuilder.DORMANT.apply(seed);
        this.shoreSurfaceBuilder = ShorelineSurfaceBuilder.OLD_SHIELD_VOLCANO.apply(seed);
    }

    @Override
    public void buildSurface(SurfaceBuilderContext context, int startY, int endY)
    {
        final int x = context.pos().getX();
        final int z = context.pos().getZ();

        final int glacierBaseHeight = (int) Math.ceil(baseNoise.noise(x, z));
        final int glacierSurfaceHeight = (int) Math.ceil(iceSurfaceNoise.noise(x, z));

        final int iceDepth;
        if (hasMoraines && IceSheetSurfaceBuilder.getBaseGroundwater(context) <= 20f)
        {
            final double moraineCrestHeight = Math.min(0.5 * (glacierSurfaceHeight + glacierBaseHeight), glacierBaseHeight + 18);
            iceDepth = Math.max((int) ((startY - moraineCrestHeight) * 2), 0);
        }
        else
        {
            iceDepth = 35;
        }

        final int seaLevel = context.getSeaLevel();
        if (startY <= seaLevel)
        {
            shoreSurfaceBuilder.buildSurface(context, startY, endY);
        }
        else if (startY < minFreezingHeight || (hasStonyPeaks && startY > glacierSurfaceHeight + 2.5) || startY < glacierBaseHeight - 1.5)
        {
            baseVolcanoSurfaceBuilder.buildSurface(context, startY, endY);
        }
        else
        {
            IceSheetSurfaceBuilder.placeIceSurface(
                context,
                startY,
                endY,
                glacierBaseHeight,
                glacierSurfaceHeight,
                seaLevel,
                iceDepth,
                NTESurfaceStates.SNOW,
                NTESurfaceStates.PACKED_ICE,
                NTESurfaceStates.BLUE_ICE,
                NTESurfaceStates.SNOWY_BASALT_MORAINE,
                NTESurfaceStates.BASALT_MORAINE,
                NTESurfaceStates.BASALT
            );
        }
    }

    private static Noise2D max(Noise2D first, Noise2D second)
    {
        return (x, z) -> Math.max(first.noise(x, z), second.noise(x, z));
    }
}
