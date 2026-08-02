package com.newterraearth.tfe.world.forest;

import java.util.List;
import java.util.Locale;

import com.mojang.serialization.Codec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;

public enum NTEForestType implements StringRepresentable
{
    GRASSLAND(ForestSubType.NONE, 0, zero(), zero(), zero(), zero(), 2, 0f),
    CLEARING(ForestSubType.NONE, 0, zero(), zero(), zero(), zero(), 2, 0f),
    SHRUBLAND(ForestSubType.NONE, 0, zero(), value(10), range(0, 1), range(2, 7), 2, 1f),
    SPARSE(ForestSubType.NONE, 0, value(2), value(6), zero(), range(0, 2), 2, 0.08f),
    SAVANNA_MONOCULTURE(ForestSubType.SAVANNA, 1, value(3), value(6), zero(), range(0, 2), 1, 0.55f),
    SAVANNA_DIVERSE(ForestSubType.SAVANNA, 1, value(3), value(6), zero(), range(0, 2), 2, 0.65f),
    SAVANNA_ALTERNATE(ForestSubType.SAVANNA, 1, value(3), value(6), zero(), range(0, 2), 3, 0.40f),
    SAVANNA_SHRUB_MONOCULTURE(ForestSubType.SAVANNA, 1, value(1), value(6), zero(), range(3, 6), 1, 0.9f),
    SAVANNA_SHRUB_DIVERSE(ForestSubType.SAVANNA, 1, value(1), value(6), zero(), range(3, 6), 2, 1f),
    SAVANNA_SHRUB_ALTERNATE(ForestSubType.SAVANNA, 1, value(1), value(6), zero(), range(3, 6), 3, 0.8f),
    PRIMARY_MONOCULTURE(ForestSubType.PRIMARY, 3, value(5), value(25), range(0, 1), zero(), 1, 1f),
    PRIMARY_DIVERSE(ForestSubType.PRIMARY, 4, value(7), value(40), range(0, 1), range(0, 3), 2, 1f),
    PRIMARY_ALTERNATE(ForestSubType.PRIMARY, 4, value(7), value(40), range(0, 1), range(0, 3), 3, 1f),
    SECONDARY_MONOCULTURE(ForestSubType.SECONDARY, 3, value(5), value(25), zero(), range(1, 2), 1, 1f),
    SECONDARY_MONOCULTURE_TALL(ForestSubType.SECONDARY, 3, value(5), value(25), zero(), range(1, 2), 1, 1f),
    SECONDARY_DIVERSE(ForestSubType.SECONDARY, 3, value(5), value(25), zero(), range(1, 2), 2, 1f),
    SECONDARY_BAMBOO(ForestSubType.SECONDARY, 3, value(1), value(25), range(0, 1), range(0, 1), 2, 0.3f),
    SECONDARY_DIVERSE_TALL(ForestSubType.SECONDARY, 3, value(5), value(25), zero(), range(1, 2), 2, 1f),
    SECONDARY_DENSE(ForestSubType.SECONDARY, 4, value(7), value(40), range(0, 1), value(3), 2, 1f),
    SECONDARY_DENSE_TALL(ForestSubType.SECONDARY, 4, value(7), value(40), range(0, 1), value(3), 2, 1f),
    SECONDARY_ALTERNATE(ForestSubType.SECONDARY, 3, value(5), value(25), zero(), range(1, 2), 3, 1f),
    EDGE_MONOCULTURE(ForestSubType.EDGE, 2, value(2), value(10), range(0, 1), range(0, 1), 1, 1f),
    EDGE_DIVERSE(ForestSubType.EDGE, 2, value(2), value(10), range(0, 1), range(0, 1), 2, 1f),
    EDGE_ALTERNATE(ForestSubType.EDGE, 2, value(2), value(10), range(0, 1), range(0, 1), 3, 1f),
    EDGE_BAMBOO(ForestSubType.EDGE, 2, value(1), value(10), range(0, 1), range(0, 1), 1, 0.7f),
    DEAD_MONOCULTURE(ForestSubType.DEAD, 2, value(5), value(25), zero(), range(2, 4), 1, 1f),
    DEAD_DIVERSE(ForestSubType.DEAD, 2, value(5), value(25), zero(), range(2, 4), 2, 1f),
    DEAD_ALTERNATE(ForestSubType.DEAD, 3, value(4), value(40), range(0, 1), range(0, 3), 3, 1f),
    DEAD_BAMBOO(ForestSubType.DEAD, 3, value(4), value(25), range(0, 1), range(2, 4), 2, 1f);

    public static final Codec<NTEForestType> CODEC = StringRepresentable.fromEnum(NTEForestType::values);

    private static final NTEForestType[] VALUES = values();
    private static final List<NTEForestType> EDGE_DENSITY = List.of(EDGE_MONOCULTURE, EDGE_DIVERSE, EDGE_ALTERNATE, EDGE_BAMBOO);
    private static final List<NTEForestType> SECONDARY_FORESTS = List.of(SECONDARY_MONOCULTURE, SECONDARY_DENSE, SECONDARY_DIVERSE, SECONDARY_DENSE_TALL, SECONDARY_MONOCULTURE_TALL, SECONDARY_DIVERSE_TALL, SECONDARY_BAMBOO);
    private static final List<NTEForestType> PRIMARY_FORESTS = List.of(PRIMARY_DIVERSE, PRIMARY_MONOCULTURE);
    private static final List<NTEForestType> DEAD_FORESTS = List.of(DEAD_MONOCULTURE, DEAD_DIVERSE, DEAD_ALTERNATE, DEAD_BAMBOO);
    private static final List<NTEForestType> SAVANNA_FORESTS = List.of(SAVANNA_MONOCULTURE, SAVANNA_ALTERNATE, SAVANNA_DIVERSE, SAVANNA_SHRUB_MONOCULTURE, SAVANNA_SHRUB_ALTERNATE, SAVANNA_SHRUB_DIVERSE);

    public static NTEForestType valueOf(int i)
    {
        return i >= 0 && i < VALUES.length ? VALUES[i] : GRASSLAND;
    }

    public static NTEForestType byName(String name)
    {
        return valueOf(Enum.valueOf(NTEForestType.class, name.toUpperCase(Locale.ROOT)).ordinal());
    }

    public static int getEdgeForestType(RandomSource random)
    {
        return EDGE_DENSITY.get(random.nextInt(EDGE_DENSITY.size())).ordinal();
    }

    public static int getSecondaryForestType(RandomSource random)
    {
        return SECONDARY_FORESTS.get(random.nextInt(SECONDARY_FORESTS.size())).ordinal();
    }

    public static int getPrimaryForestType(RandomSource random)
    {
        return PRIMARY_FORESTS.get(random.nextInt(PRIMARY_FORESTS.size())).ordinal();
    }

    public static int getDeadForestType(RandomSource random)
    {
        return DEAD_FORESTS.get(random.nextInt(DEAD_FORESTS.size())).ordinal();
    }

    public static int getSavannaForestType(RandomSource random)
    {
        return SAVANNA_FORESTS.get(random.nextInt(SAVANNA_FORESTS.size())).ordinal();
    }

    private static IntProvider zero()
    {
        return ConstantInt.of(0);
    }

    private static IntProvider range(int min, int max)
    {
        return UniformInt.of(min, max);
    }

    private static IntProvider value(int i)
    {
        return ConstantInt.of(i);
    }

    private final ForestSubType subType;
    private final int density;
    private final IntProvider treeCount;
    private final IntProvider groundcoverCount;
    private final IntProvider leafPileCount;
    private final IntProvider bushCount;
    private final int maxTreeTypes;
    private final float perChunkChance;

    NTEForestType(ForestSubType subType, int density, IntProvider treeCount, IntProvider groundcoverCount, IntProvider leafPileCount, IntProvider bushCount, int maxTreeTypes, float perChunkChance)
    {
        this.subType = subType;
        this.density = density;
        this.treeCount = treeCount;
        this.groundcoverCount = groundcoverCount;
        this.leafPileCount = leafPileCount;
        this.bushCount = bushCount;
        this.maxTreeTypes = maxTreeTypes;
        this.perChunkChance = perChunkChance;
    }

    @Override
    public String getSerializedName()
    {
        return name().toLowerCase(Locale.ROOT);
    }

    public NTEForestType getAlternate()
    {
        if (isSavanna())
        {
            return this;
        }
        if (isEdge())
        {
            return EDGE_ALTERNATE;
        }
        if (isSecondary())
        {
            return SECONDARY_ALTERNATE;
        }
        if (isPrimary())
        {
            return PRIMARY_ALTERNATE;
        }
        if (isDead())
        {
            return DEAD_ALTERNATE;
        }
        return this;
    }

    public boolean isPrimary()
    {
        return subType == ForestSubType.PRIMARY;
    }

    public boolean isEdge()
    {
        return subType == ForestSubType.EDGE;
    }

    public boolean isSecondary()
    {
        return subType == ForestSubType.SECONDARY;
    }

    public boolean isDead()
    {
        return subType == ForestSubType.DEAD;
    }

    public boolean isNone()
    {
        return subType == ForestSubType.NONE;
    }

    public boolean isSavanna()
    {
        return subType == ForestSubType.SAVANNA;
    }

    public int sampleTrees(RandomSource random)
    {
        return treeCount.sample(random);
    }

    public int sampleGroundcover(RandomSource random)
    {
        return groundcoverCount.sample(random);
    }

    public int sampleLeafPiles(RandomSource random)
    {
        return leafPileCount.sample(random);
    }

    public int sampleBushes(RandomSource random)
    {
        return bushCount.sample(random);
    }

    public int getDensity()
    {
        return density;
    }

    public int getMaxTreeTypes()
    {
        return maxTreeTypes;
    }

    public float getPerChunkChance()
    {
        return perChunkChance;
    }

    public enum ForestSubType
    {
        NONE,
        PRIMARY,
        SECONDARY,
        EDGE,
        DEAD,
        SAVANNA
    }
}
