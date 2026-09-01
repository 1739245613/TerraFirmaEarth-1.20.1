package com.newterraearth.tfe.world;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import net.dries007.tfc.world.BiomeNoiseSampler;
import net.dries007.tfc.world.noise.Cellular2D;
import net.dries007.tfc.world.noise.Noise2D;

import com.newterraearth.tfe.world.noise.NTECellular2D;
import com.newterraearth.tfe.world.volcano.NTEVolcanoVariant;
import com.newterraearth.tfe.world.volcano.NTEVolcanoVariants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static net.dries007.tfc.world.TFCChunkGenerator.SEA_LEVEL_Y;

class NTEBiomeNoiseTest
{
    @Test
    void cellularNoiseKeepsTheSharedTfcFields()
    {
        final long seed = 1357913579L;
        final Cellular2D reference = new Cellular2D(seed).spread(0.03125f);
        final NTECellular2D port = new NTECellular2D(seed).spread(0.03125f);

        for (int x = -2048; x <= 2048; x += 64)
        {
            for (int z = -2048; z <= 2048; z += 64)
            {
                final Cellular2D.Cell expected = reference.cell(x, z);
                final NTECellular2D.Cell actual = port.cell(x, z);
                assertEquals(expected.x(), actual.x(), 0d);
                assertEquals(expected.y(), actual.y(), 0d);
                assertEquals(expected.cx(), actual.cx());
                assertEquals(expected.cy(), actual.cy());
                assertEquals(expected.f1(), actual.f1(), 0d);
                assertEquals(expected.f2(), actual.f2(), 0d);
                assertEquals(expected.noise(), actual.noise(), 0d);
            }
        }
    }

    @Test
    void ridgeMountainsIsDeterministicAndBounded()
    {
        final Noise2D first = NTEBiomeNoise.ridgeMountains(123456789L, 10, 90, 0.4f, 140, 40);
        final Noise2D second = NTEBiomeNoise.ridgeMountains(123456789L, 10, 90, 0.4f, 140, 40);

        for (int x = -2048; x <= 2048; x += 64)
        {
            for (int z = -2048; z <= 2048; z += 64)
            {
                final double firstValue = first.noise(x, z);
                final double secondValue = second.noise(x, z);
                assertTrue(Double.isFinite(firstValue));
                assertTrue(firstValue <= 300d);
                assertEquals(firstValue, secondValue, 0d);
            }
        }
    }

    @Test
    void fenglinPlainsNeverInvertsKarstHeight()
    {
        final long seed = 9171574372565474963L;
        final Noise2D[] profiles = {
            NTEBiomeNoise.towerKarstHills(seed),
            NTEBiomeNoise.towerKarstLake(seed),
            NTEBiomeNoise.towerKarstBay(seed)
        };
        final double[] minimumHeights = {
            SEA_LEVEL_Y,
            SEA_LEVEL_Y - 8d,
            SEA_LEVEL_Y - 12d
        };

        for (int profile = 0; profile < profiles.length; profile++)
        {
            for (int x = -4096; x <= 4096; x += 64)
            {
                for (int z = -4096; z <= 4096; z += 64)
                {
                    final double height = profiles[profile].noise(x, z);
                    assertTrue(Double.isFinite(height));
                    assertTrue(
                        height >= minimumHeights[profile],
                        "tower karst profile inverted at " + x + "," + z + ": " + height
                    );
                }
            }
        }
    }

    @Test
    void undergroundLakesSamplerProducesNormalizedDensity()
    {
        final BiomeNoiseSampler sampler = NTEBiomeNoise.undergroundLakes(987654321L, (x, z) -> 100d);
        sampler.setColumn(512, -768);

        for (int y = 40; y <= 180; y += 5)
        {
            final double density = sampler.noise(y);
            assertTrue(Double.isFinite(density));
            assertTrue(density >= 0d && density <= 1d);
        }
    }

    @Test
    void oceanTrenchNoiseUsesFinite4_2_9Profile()
    {
        final Noise2D trench = NTEBiomeNoise.oceanTrench(246813579L, -60, -46);
        final Noise2D deepOcean = net.dries007.tfc.world.biome.BiomeNoise.ocean(246813579L, -46, -30);

        for (int x = -1024; x <= 1024; x += 64)
        {
            for (int z = -1024; z <= 1024; z += 64)
            {
                final double trenchHeight = trench.noise(x, z);
                final double deepOceanHeight = deepOcean.noise(x, z);
                assertTrue(Double.isFinite(trenchHeight));
                assertTrue(Double.isFinite(deepOceanHeight));
                assertTrue(trenchHeight <= 64d);
            }
        }
    }

    @Test
    void cenotesSamplerProducesFiniteDensity()
    {
        final BiomeNoiseSampler sampler = NTEBiomeNoise.cenotes(135792468L, (x, z) -> 100d);
        sampler.setColumn(384, -512);

        for (int y = 40; y <= 140; y += 5)
        {
            assertTrue(Double.isFinite(sampler.noise(y)));
        }
    }

    @Test
    void oceanRidgeNoiseIsFiniteAndBounded()
    {
        final Noise2D ridge = NTEBiomeNoise.oceanRidge(11223344L);
        for (int x = -4096; x <= 4096; x += 128)
        {
            for (int z = -4096; z <= 4096; z += 128)
            {
                final double height = ridge.noise(x, z);
                assertTrue(Double.isFinite(height));
                assertTrue(height <= 64d);
            }
        }
    }

    @Test
    void riftValleyNoiseIsFiniteAndBounded()
    {
        final Noise2D valley = NTEBiomeNoise.riftValley(55667788L, 2, 25, false);
        for (int x = -4096; x <= 4096; x += 128)
        {
            for (int z = -4096; z <= 4096; z += 128)
            {
                final double height = valley.noise(x, z);
                assertTrue(Double.isFinite(height));
                // The 4.2.9 profile reaches edgeHeight + 32, with the
                // independent roughness pass adding up to eight blocks.
                assertTrue(height <= 128d);
            }
        }
    }

    @Test
    void stratovolcanoVariantsAreDeterministicAndReachable()
    {
        final NTECellular2D cells = new NTECellular2D(424242L, 2).spread(0.0024f);
        final Set<String> names = new HashSet<>();
        for (int x = -4096; x <= 4096; x += 64)
        {
            for (int z = -4096; z <= 4096; z += 64)
            {
                names.add(NTEVolcanoVariants.forCell(NTESeed.of(424242L), cells.cell(x, z), cells).name());
            }
        }
        assertTrue(names.size() >= 4, "Expected the 4.2.9 variant selector to reach multiple profiles");

        final NTECellular2D.Cell sample = new NTECellular2D.Cell(0, 0, 0, 0, 0, 0, 0.02, 0.3, 0.1, 0);
        final NTEVolcanoVariant[] variants = {
            NTEVolcanoVariants.fuji(NTESeed.of(1L)),
            NTEVolcanoVariants.craterLake(NTESeed.of(1L)),
            NTEVolcanoVariants.tahoma(NTESeed.of(1L)),
            NTEVolcanoVariants.batholith(NTESeed.of(1L)),
            NTEVolcanoVariants.kelimutu(NTESeed.of(1L))
        };
        for (NTEVolcanoVariant variant : variants)
        {
            assertTrue(Double.isFinite(variant.getHeight(64, 0, 0, 1, 200, 0, sample)), variant.name());
        }
    }

}
