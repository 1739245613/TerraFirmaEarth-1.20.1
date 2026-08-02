package com.newterraearth.tfe.world.feature.tree;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

final class NTEForestSpeciesSelector
{
    private NTEForestSpeciesSelector()
    {
    }

    /**
     * Builds the symmetric rank weight requested by the forest selection rules.
     * Mirrored candidates transfer a decreasing share from the less suitable half
     * to the more suitable half. The best candidate gains half of the uniform share
     * given up by the worst candidate, and each inner pair transfers one step less.
     */
    static int rankWeight(int candidateCount, int rank)
    {
        if (candidateCount <= 0)
        {
            throw new IllegalArgumentException("candidateCount must be positive");
        }
        if (rank < 0 || rank >= candidateCount)
        {
            throw new IllegalArgumentException("rank must be within the candidate list");
        }

        if (candidateCount == 1)
        {
            return 1;
        }

        final int half = candidateCount / 2;
        int weight = 3 * half - rank;
        if (candidateCount % 2 == 0 && rank >= half)
        {
            weight--;
        }
        return weight;
    }

    static <T> List<T> selectWithoutReplacement(List<T> rankedCandidates, int requestedCount, RandomSource random)
    {
        final int selectedCount = Math.min(Math.max(requestedCount, 0), rankedCandidates.size());
        if (selectedCount == 0)
        {
            return List.of();
        }
        if (selectedCount == rankedCandidates.size())
        {
            return List.copyOf(rankedCandidates);
        }

        final List<WeightedCandidate<T>> remaining = new ArrayList<>(rankedCandidates.size());
        for (int rank = 0; rank < rankedCandidates.size(); rank++)
        {
            remaining.add(new WeightedCandidate<>(rankedCandidates.get(rank), rankWeight(rankedCandidates.size(), rank)));
        }

        final List<T> selected = new ArrayList<>(selectedCount);
        for (int pick = 0; pick < selectedCount; pick++)
        {
            long totalWeight = 0L;
            for (WeightedCandidate<T> candidate : remaining)
            {
                totalWeight += candidate.weight();
            }

            double roll = random.nextDouble() * totalWeight;
            int selectedIndex = remaining.size() - 1;
            for (int index = 0; index < remaining.size(); index++)
            {
                roll -= remaining.get(index).weight();
                if (roll < 0d)
                {
                    selectedIndex = index;
                    break;
                }
            }
            selected.add(remaining.remove(selectedIndex).value());
        }
        return List.copyOf(selected);
    }

    @Nullable
    static <T> T selectForPlacement(List<T> rankedCandidates, int requestedCount, RandomSource random)
    {
        final List<T> selected = selectWithoutReplacement(rankedCandidates, requestedCount, random);
        if (selected.isEmpty())
        {
            return null;
        }
        return selected.size() == 1 ? selected.get(0) : selected.get(random.nextInt(selected.size()));
    }

    static double normalizedIntervalDistance(double value, double min, double max)
    {
        if (min > max)
        {
            return Double.POSITIVE_INFINITY;
        }
        final double span = max - min;
        if (span == 0d)
        {
            return Double.compare(value, min) == 0 ? 0d : Double.POSITIVE_INFINITY;
        }
        return Math.abs(value - (min + max) * 0.5d) / (span * 0.5d);
    }

    private record WeightedCandidate<T>(T value, int weight)
    {
    }
}
