package com.newterraearth.tfe.world.river;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import net.minecraft.util.RandomSource;

import net.dries007.tfc.world.region.RegionPartition;
import net.dries007.tfc.world.region.RiverEdge;
import net.dries007.tfc.world.river.River;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NTERiverHydrologyTest
{
    @Test
    void readOnlyHeightQueryDoesNotPlanAnUnseenLeaf()
    {
        final AtomicInteger heightSamples = new AtomicInteger();
        final RiverEdge leaf = testLeaf();
        final RegionPartition.Point partition = new RegionPartition.Point(List.of(leaf));
        final NTERiverHydrology hydrology = new NTERiverHydrology(
            918273645L,
            63,
            (x, z) -> {
                heightSamples.incrementAndGet();
                return 80d;
            },
            (x, z) -> partition
        );

        NTERiverHydrology.beginReadOnlyHeightQuery();
        try
        {
            assertTrue(hydrology.retainsTfcEdge(leaf));
            assertNull(hydrology.findGraphProfile(0, 0));
        }
        finally
        {
            NTERiverHydrology.endReadOnlyHeightQuery();
        }

        assertEquals(0, heightSamples.get(),
            "locate and height-preload queries must not sample terrain for a new creek route");
        assertFalse(NTERiverHydrology.readOnlyHeightQueryActive());
    }

    @Test
    void nestedReadOnlyHeightQueriesRestoreTheOuterScope()
    {
        assertFalse(NTERiverHydrology.readOnlyHeightQueryActive());
        NTERiverHydrology.beginReadOnlyHeightQuery();
        NTERiverHydrology.beginReadOnlyHeightQuery();
        try
        {
            assertTrue(NTERiverHydrology.readOnlyHeightQueryActive());
            NTERiverHydrology.endReadOnlyHeightQuery();
            assertTrue(NTERiverHydrology.readOnlyHeightQueryActive());
        }
        finally
        {
            if (NTERiverHydrology.readOnlyHeightQueryActive())
            {
                NTERiverHydrology.endReadOnlyHeightQuery();
            }
        }
        assertFalse(NTERiverHydrology.readOnlyHeightQueryActive());
    }

    private static RiverEdge testLeaf()
    {
        final River.Vertex source = new River.Vertex(-1d, 0d, 0d, 1d, 1);
        final River.Vertex drain = new River.Vertex(1d, 0d, 0d, 1d, 0);
        final RiverEdge edge = new RiverEdge(new River.Edge(source, drain), RandomSource.create(55667788L));
        edge.width = 10;
        return edge;
    }
}
