package com.newterraearth.tfe.world;

import net.dries007.tfc.world.biome.BiomeExtension;

public interface NTEChunkBaseBlockSourceAccess
{
    void tfe$useAccurateBiome(int localX, int localZ, BiomeExtension biome, double weight, boolean couldBeSalty);
}
