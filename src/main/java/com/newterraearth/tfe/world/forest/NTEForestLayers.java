package com.newterraearth.tfe.world.forest;

import java.util.Random;

import net.dries007.tfc.util.IArtist;
import net.dries007.tfc.world.layer.ZoomLayer;
import net.dries007.tfc.world.layer.framework.AreaFactory;
import net.dries007.tfc.world.noise.OpenSimplex2D;

public final class NTEForestLayers
{
    private NTEForestLayers()
    {
    }

    public static AreaFactory createOverworldForestLayer(long seed, IArtist<AreaFactory> artist)
    {
        final Random random = new Random(seed);

        AreaFactory layer = new NTEForestInitLayer(new OpenSimplex2D(random.nextInt()).spread(0.25f)).apply(random.nextLong());
        artist.draw("forest", 1, layer);
        layer = NTEForestRandomizeLayer.INSTANCE.apply(random.nextLong(), layer);
        artist.draw("forest", 2, layer);
        layer = ZoomLayer.FUZZY.apply(random.nextLong(), layer);
        artist.draw("forest", 3, layer);
        layer = NTEForestRandomizeLayer.INSTANCE.apply(random.nextLong(), layer);
        artist.draw("forest", 4, layer);
        layer = ZoomLayer.FUZZY.apply(random.nextLong(), layer);
        artist.draw("forest", 5, layer);
        layer = ZoomLayer.NORMAL.apply(random.nextLong(), layer);
        artist.draw("forest", 6, layer);
        layer = NTEForestEdgeLayer.INSTANCE.apply(random.nextLong(), layer);
        artist.draw("forest", 7, layer);
        layer = NTEForestRandomizeSmallLayer.INSTANCE.apply(random.nextLong(), layer);
        artist.draw("forest", 8, layer);

        for (int i = 0; i < 2; i++)
        {
            layer = ZoomLayer.NORMAL.apply(random.nextLong(), layer);
            artist.draw("forest", 9 + i, layer);
        }

        return layer;
    }
}
