package com.newterraearth.tfe.world.forest;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;

import net.dries007.tfc.util.IArtist;
import net.dries007.tfc.world.chunkdata.ForestType;
import net.dries007.tfc.world.layer.framework.ConcurrentArea;

import com.newterraearth.tfe.world.NTESeed;

public final class NTE121ForestHelpers
{
    private static final int FOREST_LAYER_SEED_ADVANCE = 22;
    private static final Map<Long, ConcurrentArea<NTEForestType>> FOREST_LAYERS = new ConcurrentHashMap<>();

    private NTE121ForestHelpers()
    {
    }

    public static NTEForestType getForestType(long levelSeed, BlockPos pos)
    {
        return getForestType(levelSeed, pos.getX(), pos.getZ());
    }

    public static NTEForestType getForestType(long levelSeed, int blockX, int blockZ)
    {
        return FOREST_LAYERS.computeIfAbsent(levelSeed, NTE121ForestHelpers::createForestLayer).get(blockX >> 4, blockZ >> 4);
    }

    public static NTEForestType mapLegacyForestType(ForestType type)
    {
        return switch (type)
        {
            case NONE -> NTEForestType.GRASSLAND;
            case SPARSE -> NTEForestType.SPARSE;
            case EDGE -> NTEForestType.EDGE_DIVERSE;
            case NORMAL -> NTEForestType.SECONDARY_DIVERSE;
            case OLD_GROWTH -> NTEForestType.PRIMARY_DIVERSE;
        };
    }

    public static ForestType toLegacyForestType(NTEForestType type)
    {
        return switch (type)
        {
            case GRASSLAND, CLEARING -> ForestType.NONE;
            default ->
            {
                final int density = type.getDensity();
                yield switch (density)
                {
                    case 4 -> ForestType.OLD_GROWTH;
                    case 3 -> ForestType.NORMAL;
                    case 2 -> ForestType.EDGE;
                    default -> ForestType.SPARSE;
                };
            }
        };
    }

    private static ConcurrentArea<NTEForestType> createForestLayer(long levelSeed)
    {
        return new ConcurrentArea<>(NTEForestLayers.createOverworldForestLayer(getForestLayerSeed(levelSeed), IArtist.nope()), NTEForestType::valueOf);
    }

    private static long getForestLayerSeed(long levelSeed)
    {
        final NTESeed seed = NTESeed.of(levelSeed);
        for (int i = 0; i < FOREST_LAYER_SEED_ADVANCE; i++)
        {
            seed.next();
        }
        return seed.next();
    }
}
