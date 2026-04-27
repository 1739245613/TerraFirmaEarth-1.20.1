package com.newterraearth.tfe.mixin;

import net.dries007.tfc.world.layer.framework.Area;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.dries007.tfc.world.region.ChooseBiomes;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;

import com.newterraearth.tfe.world.NTELayerIds;
import com.newterraearth.tfe.world.region.NTEPointAccess;

import static net.dries007.tfc.world.layer.TFCLayers.*;

@Mixin(value = ChooseBiomes.class, remap = false)
public abstract class ChooseBiomesMixin
{
    private static final int[] MOUNTAIN_ALTITUDE_BIOMES = {MOUNTAINS, MOUNTAINS, MOUNTAINS, OLD_MOUNTAINS, OLD_MOUNTAINS, PLATEAU, HIGHLANDS};
    private static final int[] OCEANIC_MOUNTAIN_ALTITUDE_BIOMES = {VOLCANIC_MOUNTAINS, VOLCANIC_OCEANIC_MOUNTAINS, VOLCANIC_OCEANIC_MOUNTAINS, OCEANIC_MOUNTAINS, OCEANIC_MOUNTAINS, ROLLING_HILLS};
    private static final int[][] ALTITUDE_BIOMES = {
        {PLAINS, PLAINS, HILLS, HILLS, ROLLING_HILLS, LOW_CANYONS, LOWLANDS, LOWLANDS},
        {PLAINS, HILLS, ROLLING_HILLS, ROLLING_HILLS, ROLLING_HILLS, HIGHLANDS, NTELayerIds.BUTTES, NTELayerIds.MESAS, BADLANDS, NTELayerIds.PLATEAU_WIDE, CANYONS, CANYONS, LOW_CANYONS},
        {HIGHLANDS, HIGHLANDS, HIGHLANDS, HIGHLANDS, ROLLING_HILLS, ROLLING_HILLS, BADLANDS, BADLANDS, NTELayerIds.STAIR_STEP_CANYONS, PLATEAU, PLATEAU, PLATEAU, NTELayerIds.PLATEAU_WIDE, OLD_MOUNTAINS, OLD_MOUNTAINS, OLD_MOUNTAINS, OLD_MOUNTAINS},
    };
    private static final int[][] ICE_SHEET_ALTITUDE_BIOMES = {
        {NTELayerIds.ICE_SHEET, NTELayerIds.ICE_SHEET, NTELayerIds.ICE_SHEET, NTELayerIds.ICE_SHEET, NTELayerIds.ICE_SHEET, NTELayerIds.ICE_SHEET_TUYAS},
        {NTELayerIds.ICE_SHEET, NTELayerIds.ICE_SHEET, NTELayerIds.ICE_SHEET, NTELayerIds.ICE_SHEET, NTELayerIds.ICE_SHEET, NTELayerIds.ICE_SHEET_TUYAS, NTELayerIds.ICE_SHEET, NTELayerIds.ICE_SHEET_TUYAS},
        {NTELayerIds.ICE_SHEET, NTELayerIds.ICE_SHEET, NTELayerIds.ICE_SHEET, NTELayerIds.ICE_SHEET, NTELayerIds.ICE_SHEET_TUYAS, NTELayerIds.ICE_SHEET_TUYAS, NTELayerIds.ICE_SHEET_MOUNTAINS, NTELayerIds.ICE_SHEET_MOUNTAINS},
    };
    private static final int[][] PALEO_ICE_SHEET_ALTITUDE_BIOMES = {
        {NTELayerIds.PATTERNED_GROUND, NTELayerIds.INVERTED_PATTERNED_GROUND, NTELayerIds.KNOB_AND_KETTLE, NTELayerIds.KNOB_AND_KETTLE, NTELayerIds.KNOB_AND_KETTLE, NTELayerIds.DRUMLINS, NTELayerIds.TUYAS, LOWLANDS, LOWLANDS},
        {NTELayerIds.PATTERNED_GROUND, NTELayerIds.KNOB_AND_KETTLE, NTELayerIds.DRUMLINS, NTELayerIds.DRUMLINS, NTELayerIds.DRUMLINS, NTELayerIds.DRUMLINS, NTELayerIds.TUYAS, NTELayerIds.TUYAS},
        {NTELayerIds.DRUMLINS, NTELayerIds.DRUMLINS, NTELayerIds.DRUMLINS, BADLANDS, BADLANDS, PLATEAU, PLATEAU, PLATEAU, NTELayerIds.PLATEAU_WIDE, NTELayerIds.ICE_SHEET_MOUNTAINS},
    };
    private static final int[][] DESERT_ALTITUDE_BIOMES = {
        {NTELayerIds.BUTTES, NTELayerIds.GRASSY_DUNES, NTELayerIds.DUNE_SEA, NTELayerIds.DUNE_SEA, NTELayerIds.DUNE_SEA, NTELayerIds.SALT_FLATS, NTELayerIds.SALT_FLATS},
        {NTELayerIds.DUNE_SEA, NTELayerIds.BUTTES, NTELayerIds.BUTTES, NTELayerIds.HOODOOS, NTELayerIds.MESAS, NTELayerIds.MESAS, NTELayerIds.STAIR_STEP_CANYONS, BADLANDS, PLATEAU, CANYONS, NTELayerIds.WHORLED_CANYONS},
        {NTELayerIds.HOODOOS, NTELayerIds.MESAS, NTELayerIds.STAIR_STEP_CANYONS, NTELayerIds.STAIR_STEP_CANYONS, NTELayerIds.ROCKY_PLATEAU, NTELayerIds.ROCKY_PLATEAU, OLD_MOUNTAINS, OLD_MOUNTAINS, OLD_MOUNTAINS, NTELayerIds.WHORLED_CANYONS},
    };
    private static final int[][] SEMI_ARID_ALTITUDE_BIOMES = {
        {PLAINS, HILLS, HILLS, NTELayerIds.GRASSY_DUNES, NTELayerIds.GRASSY_DUNES, NTELayerIds.GRASSY_DUNES, NTELayerIds.GRASSY_DUNES, LOW_CANYONS, LOWLANDS, LOWLANDS, NTELayerIds.MUD_FLATS},
        {PLAINS, HILLS, ROLLING_HILLS, ROLLING_HILLS, HIGHLANDS, HIGHLANDS, BADLANDS, BADLANDS, NTELayerIds.PLATEAU_WIDE, CANYONS, LOW_CANYONS, NTELayerIds.WHORLED_CANYONS, NTELayerIds.BUTTES, NTELayerIds.MESAS, NTELayerIds.MESAS, NTELayerIds.HOODOOS},
        {HIGHLANDS, HIGHLANDS, NTELayerIds.MESAS, NTELayerIds.HOODOOS, ROLLING_HILLS, ROLLING_HILLS, BADLANDS, BADLANDS, NTELayerIds.PLATEAU_WIDE, NTELayerIds.ROCKY_PLATEAU, NTELayerIds.STAIR_STEP_CANYONS, NTELayerIds.STAIR_STEP_CANYONS, OLD_MOUNTAINS, OLD_MOUNTAINS, NTELayerIds.WHORLED_CANYONS},
    };
    private static final int[] KNOB_AND_KETTLE_BIOMES = {NTELayerIds.KNOB_AND_KETTLE, NTELayerIds.PATTERNED_GROUND, NTELayerIds.INVERTED_PATTERNED_GROUND};
    private static final int[] ISLAND_BIOMES = {PLAINS, HILLS, ROLLING_HILLS, VOLCANIC_OCEANIC_MOUNTAINS, VOLCANIC_OCEANIC_MOUNTAINS, NTELayerIds.GUANO_ISLAND};
    private static final int[] MID_DEPTH_OCEAN_BIOMES = {DEEP_OCEAN, OCEAN, OCEAN, OCEAN_REEF, OCEAN_REEF, OCEAN_REEF};

    /**
     * @author Codex
     * @reason Backport the local 1.21 biome family chooser to 1.20, consuming pre-annotated hotspot and karst data instead of seeded approximations.
     */
    @Overwrite(remap = false)
    public void apply(RegionGenerator.Context context)
    {
        final Region region = context.region;
        final Area blobArea = context.generator().biomeArea.get();
        final long rngSeed = context.random.nextLong();
        final long climateSeed = context.random.nextLong();

        for (int x = region.minX(); x <= region.maxX(); x++)
        {
            for (int z = region.minZ(); z <= region.maxZ(); z++)
            {
                final Region.Point point = region.maybeAt(x, z);
                if (point == null)
                {
                    continue;
                }

                final NTEPointAccess pointAccess = (NTEPointAccess) point;
                final int areaSeed = blobArea.get(x, z);

                if (point.island())
                {
                    point.biome = randomSeededFrom(rngSeed, areaSeed, ISLAND_BIOMES);
                }
                else if (point.mountain())
                {
                    final float temperature = point.temperature;
                    if (point.coastalMountain())
                    {
                        final float maxIceSheetTemp = -16f + 0.006f * point.rainfall;
                        if (temperature < maxIceSheetTemp + 2f)
                        {
                            point.biome = NTELayerIds.ICE_SHEET_OCEANIC_MOUNTAINS;
                        }
                        else if (temperature < maxIceSheetTemp + 6f)
                        {
                            point.biome = NTELayerIds.GLACIATED_OCEANIC_MOUNTAINS;
                        }
                        else if (temperature < maxIceSheetTemp + 10f)
                        {
                            point.biome = NTELayerIds.GLACIALLY_CARVED_OCEANIC_MOUNTAINS;
                        }
                        else
                        {
                            point.biome = randomSeededFrom(rngSeed, areaSeed, OCEANIC_MOUNTAIN_ALTITUDE_BIOMES);
                        }
                    }
                    else
                    {
                        final float maxIceSheetTemp = -14f + 0.006f * point.rainfall;
                        if (temperature < maxIceSheetTemp)
                        {
                            point.biome = NTELayerIds.ICE_SHEET_MOUNTAINS;
                        }
                        else if (temperature < maxIceSheetTemp + 4f)
                        {
                            point.biome = NTELayerIds.GLACIATED_MOUNTAINS;
                        }
                        else if (temperature < maxIceSheetTemp + 10f)
                        {
                            point.biome = NTELayerIds.GLACIALLY_CARVED_MOUNTAINS;
                        }
                        else
                        {
                            point.biome = randomSeededFrom(rngSeed, areaSeed, MOUNTAIN_ALTITUDE_BIOMES);
                        }
                    }
                }
                else if (point.land())
                {
                    final float rainfall = point.rainfall;
                    final float temperature = point.temperature;
                    final float maxIceSheetTemp = -17f + 0.006f * rainfall;
                    if (temperature < maxIceSheetTemp)
                    {
                        int biome = randomSeededFrom(rngSeed, areaSeed, ICE_SHEET_ALTITUDE_BIOMES[point.discreteBiomeAltitude()]);
                        if (point.distanceToOcean < 3 && isFlatIceSheet(biome))
                        {
                            biome = NTELayerIds.ICE_SHEET_OCEANIC;
                        }
                        point.biome = biome;
                    }
                    else if (temperature < maxIceSheetTemp + 1f)
                    {
                        point.biome = NTELayerIds.ICE_SHEET_EDGE;
                    }
                    else if (temperature < maxIceSheetTemp + 2.5f)
                    {
                        point.biome = randomSeededFrom(rngSeed, areaSeed, KNOB_AND_KETTLE_BIOMES);
                    }
                    else if (temperature < maxIceSheetTemp + 6f)
                    {
                        point.biome = randomSeededFrom(rngSeed, areaSeed, PALEO_ICE_SHEET_ALTITUDE_BIOMES[point.discreteBiomeAltitude()]);
                    }
                    else if (rainfall < 60f)
                    {
                        point.biome = randomSeededFrom(rngSeed, areaSeed, DESERT_ALTITUDE_BIOMES[point.discreteBiomeAltitude()]);
                    }
                    else if (rainfall < 155f)
                    {
                        point.biome = randomSeededFrom(rngSeed, areaSeed, SEMI_ARID_ALTITUDE_BIOMES[point.discreteBiomeAltitude()]);
                    }
                    else
                    {
                        point.biome = randomSeededFrom(rngSeed, areaSeed, ALTITUDE_BIOMES[point.discreteBiomeAltitude()]);
                    }
                }
                else if (point.baseOceanDepth < 3)
                {
                    point.biome = OCEAN;
                }
                else if (point.baseOceanDepth > 9)
                {
                    point.biome = DEEP_OCEAN_TRENCH;
                }
                else if (point.baseOceanDepth >= 5 || point.distanceToEdge < 2)
                {
                    point.biome = DEEP_OCEAN;
                }
                else
                {
                    point.biome = randomSeededFrom(rngSeed, areaSeed, MID_DEPTH_OCEAN_BIOMES);
                }

                final byte hotSpotAge = pointAccess.nte$getHotSpotAge();
                if (hotSpotAge > 0)
                {
                    if (shouldUseSunkenShieldVolcano(hotSpotAge, point.biome))
                    {
                        point.biome = NTELayerIds.SUNKEN_SHIELD_VOLCANO;
                    }
                    else
                    {
                        point.biome = getHotSpotBiome(hotSpotAge);
                    }
                }

                final float rainfall = point.rainfall;
                final float temperature = point.temperature;
                final float minRainForLowFreshWaterBiomes = 90f + Math.floorMod(areaSeed ^ climateSeed, 40);
                if (rainfall < minRainForLowFreshWaterBiomes)
                {
                    if (rainfall <= 55f)
                    {
                        if (point.biome == LOWLANDS || point.biome == LOW_CANYONS)
                        {
                            point.biome = NTELayerIds.SALT_FLATS;
                        }
                        else if (point.biome == HILLS || point.biome == ROLLING_HILLS || point.biome == PLATEAU)
                        {
                            point.biome = NTELayerIds.DUNE_SEA;
                        }
                    }
                }

                if (rainfall < 145f && (point.biome == NTELayerIds.PATTERNED_GROUND || point.biome == NTELayerIds.INVERTED_PATTERNED_GROUND))
                {
                    point.biome = NTELayerIds.STONE_CIRCLES;
                }

                final float maxRainfallForBadlands = 420f + Math.floorMod(areaSeed ^ climateSeed, 40);
                if (rainfall > maxRainfallForBadlands && point.biome == BADLANDS)
                {
                    point.biome = HIGHLANDS;
                }

                final float maxIceSheetTemp = -14f + 0.006f * rainfall;
                if (point.land() && temperature < maxIceSheetTemp)
                {
                    if (isGlaciatableShieldVolcano(point.biome))
                    {
                        point.biome = NTELayerIds.ICE_SHEET_SHIELD_VOLCANO;
                    }
                }
                else if (temperature < maxIceSheetTemp + 4f)
                {
                    if (isGlaciatableShieldVolcano(point.biome))
                    {
                        point.biome = NTELayerIds.GLACIATED_SHIELD_VOLCANO;
                    }
                }

                if (pointAccess.nte$isSurfaceRockKarst())
                {
                    if (rainfall > 375f)
                    {
                        if (rainfall > 425f && rainfall + 10f * temperature > 500f)
                        {
                            point.biome = getTowerKarstBiome(point.biome);
                        }
                        else if (temperature > 9f)
                        {
                            point.biome = getShilinBiome(point.biome);
                        }
                        else if (temperature < 0f)
                        {
                            point.biome = getBurrenBiome(point.biome);
                        }
                        else
                        {
                            point.biome = getDolineBiome(point.biome);
                        }
                    }
                    else if (rainfall > 250f)
                    {
                        if (temperature > 5f)
                        {
                            point.biome = getCenoteBiome(point.biome);
                        }
                        else
                        {
                            point.biome = getDolineBiome(point.biome);
                        }
                    }
                }

                if (point.distanceToOcean <= 2 && point.biome == LOWLANDS && rainfall > 220f && temperature > 18f)
                {
                    point.biome = SALT_MARSH;
                }
            }
        }
    }

    private int randomSeededFrom(long rngSeed, int areaSeed, int[] choices)
    {
        return choices[Math.floorMod(rngSeed ^ areaSeed, choices.length)];
    }

    private boolean shouldUseSunkenShieldVolcano(int hotSpotAge, int biome)
    {
        return biome == DEEP_OCEAN
            || biome == OCEAN_REEF
            || biome == DEEP_OCEAN_TRENCH
            || hotSpotAge == 4 && biome == OCEAN;
    }

    private boolean isFlatIceSheet(int biome)
    {
        return biome == NTELayerIds.ICE_SHEET
            || biome == NTELayerIds.ICE_SHEET_TUYAS
            || biome == NTELayerIds.SUBGLACIAL_LAKE;
    }

    private boolean isGlaciatableShieldVolcano(int biome)
    {
        return biome == NTELayerIds.ACTIVE_SHIELD_VOLCANO
            || biome == NTELayerIds.DORMANT_SHIELD_VOLCANO
            || biome == NTELayerIds.EXTINCT_SHIELD_VOLCANO;
    }

    private int getHotSpotBiome(int age)
    {
        return switch (age)
        {
            case 4 -> NTELayerIds.ANCIENT_SHIELD_VOLCANO;
            case 3 -> NTELayerIds.EXTINCT_SHIELD_VOLCANO;
            case 2 -> NTELayerIds.DORMANT_SHIELD_VOLCANO;
            case 1 -> NTELayerIds.ACTIVE_SHIELD_VOLCANO;
            default -> PLAINS;
        };
    }

    private int getTowerKarstBiome(int biome)
    {
        if (biome == SALT_MARSH)
        {
            return NTELayerIds.TOWER_KARST_BAY;
        }
        if (biome == LOWLANDS)
        {
            return NTELayerIds.TOWER_KARST_LAKE;
        }
        if (biome == PLAINS || biome == LOW_CANYONS)
        {
            return NTELayerIds.TOWER_KARST_PLAINS;
        }
        if (biome == CANYONS)
        {
            return NTELayerIds.TOWER_KARST_CANYONS;
        }
        if (biome == HILLS || biome == ROLLING_HILLS || biome == BADLANDS)
        {
            return NTELayerIds.TOWER_KARST_HILLS;
        }
        if (biome == HIGHLANDS)
        {
            return NTELayerIds.TOWER_KARST_HIGHLANDS;
        }
        if (biome == PLATEAU || biome == NTELayerIds.PLATEAU_WIDE)
        {
            return NTELayerIds.EXTREME_DOLINE_PLATEAU;
        }
        if (biome == OLD_MOUNTAINS || biome == MOUNTAINS || biome == OCEANIC_MOUNTAINS)
        {
            return NTELayerIds.EXTREME_DOLINE_MOUNTAINS;
        }
        return biome;
    }

    private int getShilinBiome(int biome)
    {
        if (biome == PLAINS)
        {
            return NTELayerIds.SHILIN_PLAINS;
        }
        if (biome == CANYONS)
        {
            return NTELayerIds.SHILIN_CANYONS;
        }
        if (biome == ROLLING_HILLS || biome == BADLANDS)
        {
            return NTELayerIds.SHILIN_HILLS;
        }
        if (biome == PLATEAU || biome == NTELayerIds.PLATEAU_WIDE)
        {
            return NTELayerIds.SHILIN_PLATEAU;
        }
        if (biome == HIGHLANDS)
        {
            return NTELayerIds.SHILIN_HIGHLANDS;
        }
        return biome;
    }

    private int getBurrenBiome(int biome)
    {
        if (biome == PLAINS || biome == CANYONS)
        {
            return NTELayerIds.BURREN_PLAINS;
        }
        if (biome == BADLANDS || biome == HILLS || biome == ROLLING_HILLS)
        {
            return NTELayerIds.BURREN_BADLANDS;
        }
        if (biome == NTELayerIds.DRUMLINS)
        {
            return NTELayerIds.BURREN_ROCHE_MOUTONEE;
        }
        if (biome == HIGHLANDS)
        {
            return NTELayerIds.BURREN_BADLANDS_TALL;
        }
        if (biome == PLATEAU || biome == NTELayerIds.PLATEAU_WIDE)
        {
            return NTELayerIds.BURREN_PLATEAU;
        }
        return biome;
    }

    private int getDolineBiome(int biome)
    {
        if (biome == CANYONS)
        {
            return NTELayerIds.DOLINE_CANYONS;
        }
        if (biome == PLAINS || biome == LOW_CANYONS)
        {
            return NTELayerIds.DOLINE_PLAINS;
        }
        if (biome == HILLS)
        {
            return NTELayerIds.DOLINE_HILLS;
        }
        if (biome == ROLLING_HILLS)
        {
            return NTELayerIds.DOLINE_ROLLING_HILLS;
        }
        if (biome == HIGHLANDS)
        {
            return NTELayerIds.DOLINE_HIGHLANDS;
        }
        if (biome == PLATEAU || biome == NTELayerIds.PLATEAU_WIDE)
        {
            return NTELayerIds.DOLINE_PLATEAU;
        }
        return biome;
    }

    private int getCenoteBiome(int biome)
    {
        if (biome == CANYONS)
        {
            return NTELayerIds.CENOTE_CANYONS;
        }
        if (biome == PLAINS || biome == LOW_CANYONS)
        {
            return NTELayerIds.CENOTE_PLAINS;
        }
        if (biome == HILLS)
        {
            return NTELayerIds.CENOTE_HILLS;
        }
        if (biome == ROLLING_HILLS)
        {
            return NTELayerIds.CENOTE_ROLLING_HILLS;
        }
        if (biome == HIGHLANDS)
        {
            return NTELayerIds.CENOTE_HIGHLANDS;
        }
        if (biome == PLATEAU || biome == NTELayerIds.PLATEAU_WIDE)
        {
            return NTELayerIds.CENOTE_PLATEAU;
        }
        return biome;
    }
}
