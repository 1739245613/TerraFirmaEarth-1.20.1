package com.newterraearth.tfe.world.prospecting;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.dries007.tfc.common.items.PropickItem;
import net.dries007.tfc.common.items.ProspectResult;

/** Tier, range and result-selection rules for the enhanced TFC propick. */
public final class NTEProspectingRules
{
    private NTEProspectingRules()
    {
    }

    /** Compresses TFC black steel (5) and colored steel (6) into reveal tier 5. */
    public static int revealTier(int toolLevel)
    {
        return Math.max(1, Math.min(5, toolLevel));
    }

    public static int scanRadius(int toolLevel)
    {
        return switch (Math.max(1, Math.min(6, toolLevel)))
        {
            case 1 -> PropickItem.RADIUS;
            case 2 -> 13;
            case 3 -> 14;
            case 4, 5 -> 15;
            default -> 16;
        };
    }

    public static int revealedMinerals(int toolLevel)
    {
        return switch (revealTier(toolLevel))
        {
            case 1, 2 -> 1;
            case 3 -> 2;
            case 4 -> 3;
            default -> 4;
        };
    }

    public static ProspectResult resultForCount(int count)
    {
        if (count < 10) return ProspectResult.TRACES;
        if (count < 20) return ProspectResult.SMALL;
        if (count < 40) return ProspectResult.MEDIUM;
        if (count < 80) return ProspectResult.LARGE;
        return ProspectResult.VERY_LARGE;
    }

    /**
     * Keeps TFC's selected mineral first, then orders additional minerals by
     * decreasing amount and registry id so the extra results are deterministic.
     */
    public static List<MineralResult> selectResults(Object2IntMap<Block> counts, Block selected, int toolLevel)
    {
        if (counts.isEmpty())
        {
            return List.of();
        }

        final Block representative = PropickItem.getRepresentative(selected);
        final List<Block> blocks = new ArrayList<>(counts.keySet());
        blocks.sort((left, right) -> {
            if (left == representative && right != representative) return -1;
            if (right == representative && left != representative) return 1;
            final int amountOrder = Integer.compare(counts.getInt(right), counts.getInt(left));
            if (amountOrder != 0) return amountOrder;
            return String.valueOf(BuiltInRegistries.BLOCK.getKey(left))
                .compareTo(String.valueOf(BuiltInRegistries.BLOCK.getKey(right)));
        });

        final int visible = Math.min(revealedMinerals(toolLevel), blocks.size());
        final List<MineralResult> results = new ArrayList<>(visible);
        for (int i = 0; i < visible; i++)
        {
            final Block block = blocks.get(i);
            results.add(new MineralResult(block, resultForCount(counts.getInt(block))));
        }
        return List.copyOf(results);
    }

    public record MineralResult(Block block, ProspectResult result)
    {
    }
}
