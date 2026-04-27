package com.newterraearth.tfe.world.forest;

import net.dries007.tfc.world.layer.framework.AdjacentTransformLayer;
import net.dries007.tfc.world.layer.framework.AreaContext;

enum NTEForestEdgeLayer implements AdjacentTransformLayer
{
    INSTANCE;

    @Override
    public int apply(AreaContext context, int north, int east, int south, int west, int center)
    {
        if (isFullForest(center) && (!isFullForest(north) || !isFullForest(east) || !isFullForest(south) || !isFullForest(west)))
        {
            return NTEForestType.getEdgeForestType(context.random());
        }
        return center;
    }

    private boolean isFullForest(int value)
    {
        final NTEForestType type = NTEForestType.valueOf(value);
        return type.isPrimary() || type.isSecondary();
    }
}
