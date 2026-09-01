package com.newterraearth.tfe.world.surface;

import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.builder.SurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilderFactory;
import net.minecraft.world.level.levelgen.Heightmap;

import com.newterraearth.tfe.world.NTEBiomeExtensionAccess;
import com.newterraearth.tfe.world.NTESurfaceContext;
import com.newterraearth.tfe.world.NTESeed;
import com.newterraearth.tfe.world.volcano.NTECenteredFeatureBlendType;
import com.newterraearth.tfe.world.volcano.NTECenteredFeatureNoise;
import com.newterraearth.tfe.world.volcano.NTECenteredFeatureNoiseSampler;
import com.newterraearth.tfe.world.volcano.NTEVolcanoVariant;

/** Applies the selected 4.2.9 stratovolcano body and surface variant. */
public final class NTEStratovolcanoSurfaceBuilder implements SurfaceBuilder
{
    public static SurfaceBuilderFactory create(SurfaceBuilderFactory parent)
    {
        return seed -> new NTEStratovolcanoSurfaceBuilder(parent.apply(seed), seed);
    }

    private final SurfaceBuilder parent;
    private final NTECenteredFeatureNoiseSampler sampler;

    private NTEStratovolcanoSurfaceBuilder(SurfaceBuilder parent, long seed)
    {
        this.parent = parent;
        this.sampler = NTECenteredFeatureNoise.stratovolcano(NTESeed.of(seed));
    }

    @Override
    public void buildSurface(SurfaceBuilderContext context, int startY, int endY)
    {
        final NTESurfaceContext.Context surfaceContext = NTESurfaceContext.current();
        final BiomeExtension biome = surfaceContext.stratovolcanoBiome();
        if (((NTEBiomeExtensionAccess) (Object) biome).tfe$getCenteredFeatureBlendType() != NTECenteredFeatureBlendType.STRATOVOLCANO)
        {
            parent.buildSurface(context, startY, endY);
            return;
        }

        final var cellular = sampler.getCellularNoise();
        final NTEVolcanoVariant variant = sampler.getVolcanoVariant(cellular.cell(context.pos().getX(), context.pos().getZ()));
        final int oceanFloorHeight = context.chunk().getHeight(Heightmap.Types.OCEAN_FLOOR_WG, context.pos().getX() & 15, context.pos().getZ() & 15);
        if (variant.buildSurface(context, oceanFloorHeight, surfaceContext.preVolcanicHeight(context.pos()), sampler))
        {
            return;
        }
        parent.buildSurface(context, startY, endY);
    }
}
