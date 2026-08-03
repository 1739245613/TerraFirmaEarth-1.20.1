package com.newterraearth.tfe.world.prospecting;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.dries007.tfc.common.items.ProspectResult;

import com.newterraearth.tfe.world.prospecting.NTEProspectingRules.MineralResult;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NTEProspectingRulesTest
{
    @BeforeAll
    static void bootstrapMinecraft()
    {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void tfcLevelsAreCompressedIntoFiveRevealTiers()
    {
        assertEquals(1, NTEProspectingRules.revealTier(0));
        assertEquals(1, NTEProspectingRules.revealTier(1));
        assertEquals(2, NTEProspectingRules.revealTier(2));
        assertEquals(3, NTEProspectingRules.revealTier(3));
        assertEquals(4, NTEProspectingRules.revealTier(4));
        assertEquals(5, NTEProspectingRules.revealTier(5));
        assertEquals(5, NTEProspectingRules.revealTier(6));
    }

    @Test
    void everyTierUsesTheRequestedRadiusAndRevealLimit()
    {
        assertEquals(12, NTEProspectingRules.scanRadius(1));
        assertEquals(13, NTEProspectingRules.scanRadius(2));
        assertEquals(14, NTEProspectingRules.scanRadius(3));
        assertEquals(15, NTEProspectingRules.scanRadius(4));
        assertEquals(15, NTEProspectingRules.scanRadius(5));
        assertEquals(16, NTEProspectingRules.scanRadius(6));

        assertEquals(1, NTEProspectingRules.revealedMinerals(1));
        assertEquals(1, NTEProspectingRules.revealedMinerals(2));
        assertEquals(2, NTEProspectingRules.revealedMinerals(3));
        assertEquals(3, NTEProspectingRules.revealedMinerals(4));
        assertEquals(4, NTEProspectingRules.revealedMinerals(5));
        assertEquals(4, NTEProspectingRules.revealedMinerals(6));
    }

    @Test
    void amountThresholdsRemainIdenticalToTfc()
    {
        assertEquals(ProspectResult.TRACES, NTEProspectingRules.resultForCount(9));
        assertEquals(ProspectResult.SMALL, NTEProspectingRules.resultForCount(10));
        assertEquals(ProspectResult.SMALL, NTEProspectingRules.resultForCount(19));
        assertEquals(ProspectResult.MEDIUM, NTEProspectingRules.resultForCount(20));
        assertEquals(ProspectResult.MEDIUM, NTEProspectingRules.resultForCount(39));
        assertEquals(ProspectResult.LARGE, NTEProspectingRules.resultForCount(40));
        assertEquals(ProspectResult.LARGE, NTEProspectingRules.resultForCount(79));
        assertEquals(ProspectResult.VERY_LARGE, NTEProspectingRules.resultForCount(80));
    }

    @Test
    void originalSelectionStaysFirstAndAdditionalMineralsAreAmountOrdered()
    {
        final Object2IntMap<Block> counts = new Object2IntOpenHashMap<>();
        counts.put(Blocks.IRON_ORE, 5);
        counts.put(Blocks.GOLD_ORE, 100);
        counts.put(Blocks.COAL_ORE, 25);
        counts.put(Blocks.COPPER_ORE, 50);
        counts.put(Blocks.DIAMOND_ORE, 12);

        final List<MineralResult> tierFour = NTEProspectingRules.selectResults(counts, Blocks.IRON_ORE, 4);
        assertEquals(3, tierFour.size());
        assertEquals(Blocks.IRON_ORE, tierFour.get(0).block());
        assertEquals(ProspectResult.TRACES, tierFour.get(0).result());
        assertEquals(Blocks.GOLD_ORE, tierFour.get(1).block());
        assertEquals(ProspectResult.VERY_LARGE, tierFour.get(1).result());
        assertEquals(Blocks.COPPER_ORE, tierFour.get(2).block());
        assertEquals(2, counts.size() - tierFour.size());

        assertEquals(1, NTEProspectingRules.selectResults(counts, Blocks.IRON_ORE, 2).size());
        assertEquals(2, NTEProspectingRules.selectResults(counts, Blocks.IRON_ORE, 3).size());
        assertEquals(4, NTEProspectingRules.selectResults(counts, Blocks.IRON_ORE, 5).size());
    }
}
