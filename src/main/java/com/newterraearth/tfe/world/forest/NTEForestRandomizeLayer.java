package com.newterraearth.tfe.world.forest;

import net.minecraft.util.RandomSource;

import net.dries007.tfc.world.layer.framework.AreaContext;
import net.dries007.tfc.world.layer.framework.CenterTransformLayer;

enum NTEForestRandomizeLayer implements CenterTransformLayer
{
    INSTANCE;

    @Override
    public int apply(AreaContext context, int value)
    {
        final NTEForestType current = NTEForestType.valueOf(value);
        final RandomSource random = context.random();
        if (current.isNone())
        {
            final int next = random.nextInt(20);
            if (next <= 2)
            {
                return NTEForestType.SHRUBLAND.ordinal();
            }
            if (next <= 5)
            {
                return NTEForestType.getSavannaForestType(random);
            }
            if (next == 6)
            {
                return NTEForestType.getSecondaryForestType(random);
            }
        }
        else if (current.isSavanna())
        {
            final int next = random.nextInt(16);
            if (next <= 2)
            {
                return NTEForestType.getSecondaryForestType(random);
            }
            if (next <= 6)
            {
                return NTEForestType.SPARSE.ordinal();
            }
            if (next <= 8)
            {
                return NTEForestType.SHRUBLAND.ordinal();
            }
        }
        else if (current.isPrimary() || current.isSecondary())
        {
            final int next = random.nextInt(24);
            if (next == 0)
            {
                return NTEForestType.SECONDARY_BAMBOO.ordinal();
            }
            if (next == 1 && !current.isSecondary())
            {
                return NTEForestType.getSavannaForestType(random);
            }
            if (next == 2)
            {
                return NTEForestType.SHRUBLAND.ordinal();
            }
            if (next <= 6)
            {
                return NTEForestType.getPrimaryForestType(random);
            }
        }
        return value;
    }
}
