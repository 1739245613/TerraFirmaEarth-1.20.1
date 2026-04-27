package com.newterraearth.tfe.world.forest;

import net.dries007.tfc.world.layer.framework.AreaContext;
import net.dries007.tfc.world.layer.framework.CenterTransformLayer;

enum NTEForestRandomizeSmallLayer implements CenterTransformLayer
{
    INSTANCE;

    @Override
    public int apply(AreaContext context, int value)
    {
        final NTEForestType current = NTEForestType.valueOf(value);
        if (current.isPrimary() || current.isSecondary())
        {
            final int random = context.random().nextInt(current.isSecondary() ? 40 : 25);
            if (random == 0)
            {
                return NTEForestType.CLEARING.ordinal();
            }
            if (random == 1)
            {
                return NTEForestType.SPARSE.ordinal();
            }
            if (random == 3)
            {
                return NTEForestType.getDeadForestType(context.random());
            }
        }
        else if (current.isSavanna() || current.isNone())
        {
            final int random = context.random().nextInt(30);
            if (random == 0)
            {
                return current.isSavanna() ? NTEForestType.getSecondaryForestType(context.random()) : NTEForestType.getEdgeForestType(context.random());
            }
        }
        if (context.random().nextInt(10) == 0)
        {
            return current.getAlternate().ordinal();
        }
        return value;
    }
}
