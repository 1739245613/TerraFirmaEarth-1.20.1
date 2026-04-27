package com.newterraearth.tfe.world.region;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.RegionTask;
import net.dries007.tfc.world.settings.RockSettings;

public enum NTEKarstSurfaceRocks implements RegionTask
{
    INSTANCE;

    @Override
    public void apply(RegionGenerator.Context context)
    {
        final Region.Point[] points = context.region.data();
        final NTERegionGeneratorAccess generator = (NTERegionGeneratorAccess) context.generator();

        for (Region.Point point : points)
        {
            if (point == null)
            {
                continue;
            }

            ((NTEPointAccess) point).nte$setSurfaceRockKarst(isKarst(generator.nte$getSurfaceRock(point)));
        }
    }

    private static boolean isKarst(RockSettings rock)
    {
        final ResourceLocation id = ForgeRegistries.BLOCKS.getKey(rock.raw());
        if (id == null)
        {
            return false;
        }

        final String path = id.getPath();
        return path.endsWith("/chalk")
            || path.endsWith("/dolomite")
            || path.endsWith("/limestone")
            || path.endsWith("/marble");
    }
}
