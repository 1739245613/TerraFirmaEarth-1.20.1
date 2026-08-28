package com.newterraearth.tfe.world.prospecting;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.dries007.tfc.common.items.PropickItem;
import net.dries007.tfc.common.items.ProspectResult;

/** Tier, range and result-selection rules for the enhanced TFC propick. */
public final class NTEProspectingRules
{
    public static final int OVER_AMOUNT_THRESHOLD = 200;

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

    public static int navigationRadius(int toolLevel)
    {
        return switch (Math.max(1, Math.min(6, toolLevel)))
        {
            case 1 -> 6;
            case 2 -> 8;
            case 3 -> 10;
            case 4 -> 12;
            case 5 -> 14;
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
     * Returns the stable mineral identity used by the enhanced scan.
     *
     * <p>TFC ore blocks encode grade and host rock in the registry path, for
     * example {@code ore/normal_native_copper/granite}. Prospecting identity
     * intentionally ignores both the grade prefix and host-rock suffix;
     * quantity is calculated only after all matching blocks are accumulated.
     * TFE's backported rock blocks use the same format in its own namespace and
     * intentionally share the TFC family.</p>
     */
    public static ResourceLocation mineralKey(Block block)
    {
        return mineralKey(BuiltInRegistries.BLOCK.getKey(block));
    }

    static ResourceLocation mineralKey(ResourceLocation blockId)
    {
        final String path = blockId.getPath();
        if (!path.startsWith("ore/"))
        {
            return blockId;
        }

        final String encodedMineral = path.substring("ore/".length());
        final int rockSeparator = encodedMineral.indexOf('/');
        if (rockSeparator <= 0 || rockSeparator == encodedMineral.length() - 1
            || encodedMineral.indexOf('/', rockSeparator + 1) >= 0)
        {
            return blockId;
        }

        String mineralName = encodedMineral.substring(0, rockSeparator);
        for (String gradePrefix : List.of("poor_", "normal_", "rich_"))
        {
            if (mineralName.startsWith(gradePrefix) && mineralName.length() > gradePrefix.length())
            {
                mineralName = mineralName.substring(gradePrefix.length());
                break;
            }
        }
        final String namespace = isTfcMineralNamespace(blockId.getNamespace()) ? "tfc" : blockId.getNamespace();
        return new ResourceLocation(namespace, "ore/" + mineralName);
    }

    private static boolean isTfcMineralNamespace(String namespace)
    {
        return namespace.equals("tfc") || namespace.equals("tfe");
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

        final ResourceLocation selectedKey = mineralKey(PropickItem.getRepresentative(selected));
        final List<Block> blocks = new ArrayList<>(counts.keySet());
        blocks.sort((left, right) -> {
            final boolean leftSelected = mineralKey(left).equals(selectedKey);
            final boolean rightSelected = mineralKey(right).equals(selectedKey);
            if (leftSelected && !rightSelected) return -1;
            if (rightSelected && !leftSelected) return 1;
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
            final int count = counts.getInt(block);
            results.add(new MineralResult(block, resultForCount(count), count));
        }
        return List.copyOf(results);
    }

    public record MineralResult(Block block, ProspectResult result, int count)
    {
        /** Compatibility constructor for results that do not carry an exact scan count. */
        public MineralResult(Block block, ProspectResult result)
        {
            this(block, result, -1);
        }

        public boolean isOverAmount()
        {
            return count > OVER_AMOUNT_THRESHOLD;
        }
    }
}
