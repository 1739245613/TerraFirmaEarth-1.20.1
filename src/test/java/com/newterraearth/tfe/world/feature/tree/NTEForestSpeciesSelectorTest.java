package com.newterraearth.tfe.world.feature.tree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NTEForestSpeciesSelectorTest
{
    @Test
    void tenCandidatesMatchRequestedWeightExample()
    {
        final int[] weights = new int[10];
        for (int rank = 0; rank < weights.length; rank++)
        {
            weights[rank] = NTEForestSpeciesSelector.rankWeight(weights.length, rank);
        }

        assertArrayEquals(new int[] {15, 14, 13, 12, 11, 9, 8, 7, 6, 5}, weights);
    }

    @Test
    void oddCandidateCountsKeepOneUniformMiddleWeight()
    {
        final int[] weights = new int[5];
        for (int rank = 0; rank < weights.length; rank++)
        {
            weights[rank] = NTEForestSpeciesSelector.rankWeight(weights.length, rank);
        }

        assertArrayEquals(new int[] {6, 5, 4, 3, 2}, weights);
    }

    @Test
    void rankWeightsRemainPositiveAndPreserveTheUniformTotal()
    {
        for (int candidateCount = 1; candidateCount <= 100; candidateCount++)
        {
            int total = 0;
            for (int rank = 0; rank < candidateCount; rank++)
            {
                final int weight = NTEForestSpeciesSelector.rankWeight(candidateCount, rank);
                assertTrue(weight > 0);
                total += weight;
            }
            final int expectedTotal = candidateCount == 1
                ? 1
                : candidateCount * (candidateCount % 2 == 0 ? candidateCount : candidateCount - 1);
            assertEquals(expectedTotal, total);
        }
    }

    @Test
    void selectionUsesGenericKWithoutReplacement()
    {
        final List<Integer> candidates = new ArrayList<>();
        for (int value = 0; value < 10; value++)
        {
            candidates.add(value);
        }

        final List<Integer> selected = NTEForestSpeciesSelector.selectWithoutReplacement(candidates, 6, RandomSource.create(42L));
        assertEquals(6, selected.size());
        assertEquals(6, new HashSet<>(selected).size());
    }

    @Test
    void selectionCapsKAtTheAvailableCandidateCount()
    {
        final List<Integer> candidates = List.of(0, 1, 2, 3);
        assertEquals(candidates, NTEForestSpeciesSelector.selectWithoutReplacement(candidates, 20, RandomSource.create(42L)));
        assertTrue(NTEForestSpeciesSelector.selectWithoutReplacement(candidates, 0, RandomSource.create(42L)).isEmpty());
        assertTrue(NTEForestSpeciesSelector.selectWithoutReplacement(candidates, -1, RandomSource.create(42L)).isEmpty());
    }

    @Test
    void selectionIsDeterministicForTheSameSeed()
    {
        final List<Integer> candidates = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        assertEquals(
            NTEForestSpeciesSelector.selectWithoutReplacement(candidates, 4, RandomSource.create(1297981456774045812L)),
            NTEForestSpeciesSelector.selectWithoutReplacement(candidates, 4, RandomSource.create(1297981456774045812L))
        );
    }

    @Test
    void everyRankRemainsReachableWhileBetterRanksStayMoreCommon()
    {
        final List<Integer> candidates = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        final int[] selectedCounts = new int[candidates.size()];
        final RandomSource random = RandomSource.create(42L);
        for (int sample = 0; sample < 10_000; sample++)
        {
            final int selected = NTEForestSpeciesSelector.selectWithoutReplacement(candidates, 1, random).get(0);
            selectedCounts[selected]++;
        }

        for (int selectedCount : selectedCounts)
        {
            assertTrue(selectedCount > 0);
        }
        assertTrue(selectedCounts[0] > selectedCounts[selectedCounts.length - 1]);
    }

    @Test
    void placementSelectionUsesTheSampledPoolForKTwoAndThree()
    {
        final List<Integer> candidates = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        for (int requestedCount = 2; requestedCount <= 3; requestedCount++)
        {
            final long seed = 42L + requestedCount;
            final RandomSource expectedRandom = RandomSource.create(seed);
            final List<Integer> selected = NTEForestSpeciesSelector.selectWithoutReplacement(candidates, requestedCount, expectedRandom);
            final int expected = selected.get(expectedRandom.nextInt(selected.size()));

            assertEquals(expected, NTEForestSpeciesSelector.selectForPlacement(candidates, requestedCount, RandomSource.create(seed)));
        }
        assertNull(NTEForestSpeciesSelector.selectForPlacement(candidates, 0, RandomSource.create(42L)));
        assertNull(NTEForestSpeciesSelector.selectForPlacement(List.of(), 3, RandomSource.create(42L)));
    }

    @Test
    void singleSelectedCandidateDoesNotConsumeAnExtraRandomValue()
    {
        final List<Integer> candidates = List.of(0, 1, 2, 3);
        final RandomSource expectedRandom = RandomSource.create(42L);
        final int expected = NTEForestSpeciesSelector.selectWithoutReplacement(candidates, 1, expectedRandom).get(0);
        final int expectedNextValue = expectedRandom.nextInt();

        final RandomSource actualRandom = RandomSource.create(42L);
        assertEquals(expected, NTEForestSpeciesSelector.selectForPlacement(candidates, 1, actualRandom));
        assertEquals(expectedNextValue, actualRandom.nextInt());
    }

    @Test
    void normalizedDistanceUsesTheActualIntervalCenter()
    {
        assertEquals(0d, NTEForestSpeciesSelector.normalizedIntervalDistance(15d, 10d, 20d));
        assertEquals(1d, NTEForestSpeciesSelector.normalizedIntervalDistance(10d, 10d, 20d));
        assertEquals(1d, NTEForestSpeciesSelector.normalizedIntervalDistance(20d, 10d, 20d));
        assertEquals(0.5d, NTEForestSpeciesSelector.normalizedIntervalDistance(12.5d, 10d, 20d));
    }

    @Test
    void normalizedDistanceHandlesDegenerateAndInvalidIntervalsSafely()
    {
        assertEquals(0d, NTEForestSpeciesSelector.normalizedIntervalDistance(10d, 10d, 10d));
        assertEquals(Double.POSITIVE_INFINITY, NTEForestSpeciesSelector.normalizedIntervalDistance(11d, 10d, 10d));
        assertEquals(Double.POSITIVE_INFINITY, NTEForestSpeciesSelector.normalizedIntervalDistance(20d, 20d, 10d));
    }
}
