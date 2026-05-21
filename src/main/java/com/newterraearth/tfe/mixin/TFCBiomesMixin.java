package com.newterraearth.tfe.mixin;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.CommonLevelAccessor;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.biome.TFCBiomes;

import com.newterraearth.tfe.world.NTEBiomeCache;
import com.newterraearth.tfe.world.NTEBiomeExtensions;

@Mixin(value = TFCBiomes.class, remap = false)
public abstract class TFCBiomesMixin
{
    @Unique private static final Logger TFE_LOGGER = LogManager.getLogger();
    @Unique private static final Set<String> tfe$warnedMissingBiomeExtensions = ConcurrentHashMap.newKeySet();
    @Unique private static final Map<ResourceLocation, BiomeExtension> tfe$extensionsByLocation = new ConcurrentHashMap<>();

    @Shadow @Final private static Map<ResourceKey<Biome>, BiomeExtension> EXTENSIONS;

    @Shadow @Final @Mutable public static BiomeExtension OCEAN;
    @Shadow @Final @Mutable public static BiomeExtension OCEAN_REEF;
    @Shadow @Final @Mutable public static BiomeExtension DEEP_OCEAN;
    @Shadow @Final @Mutable public static BiomeExtension DEEP_OCEAN_TRENCH;
    @Shadow @Final @Mutable public static BiomeExtension PLAINS;
    @Shadow @Final @Mutable public static BiomeExtension HILLS;
    @Shadow @Final @Mutable public static BiomeExtension LOWLANDS;
    @Shadow @Final @Mutable public static BiomeExtension SALT_MARSH;
    @Shadow @Final @Mutable public static BiomeExtension LOW_CANYONS;
    @Shadow @Final @Mutable public static BiomeExtension SHORE;
    @Shadow @Final @Mutable public static BiomeExtension HIGHLANDS;
    @Shadow @Final @Mutable public static BiomeExtension BADLANDS;
    @Shadow @Final @Mutable public static BiomeExtension PLATEAU;
    @Shadow @Final @Mutable public static BiomeExtension CANYONS;
    @Shadow @Final @Mutable public static BiomeExtension MOUNTAINS;
    @Shadow @Final @Mutable public static BiomeExtension OLD_MOUNTAINS;
    @Shadow @Final @Mutable public static BiomeExtension OCEANIC_MOUNTAINS;
    @Shadow @Final @Mutable public static BiomeExtension VOLCANIC_MOUNTAINS;
    @Shadow @Final @Mutable public static BiomeExtension VOLCANIC_OCEANIC_MOUNTAINS;
    @Shadow @Final @Mutable public static BiomeExtension TIDAL_FLATS;
    @Shadow @Final @Mutable public static BiomeExtension RIVER;
    @Shadow @Final @Mutable public static BiomeExtension MOUNTAIN_LAKE;
    @Shadow @Final @Mutable public static BiomeExtension OLD_MOUNTAIN_LAKE;
    @Shadow @Final @Mutable public static BiomeExtension OCEANIC_MOUNTAIN_LAKE;
    @Shadow @Final @Mutable public static BiomeExtension VOLCANIC_MOUNTAIN_LAKE;
    @Shadow @Final @Mutable public static BiomeExtension VOLCANIC_OCEANIC_MOUNTAIN_LAKE;
    @Shadow @Final @Mutable public static BiomeExtension PLATEAU_LAKE;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void tfe$replaceBiomeExtensions(CallbackInfo ci)
    {
        OCEAN = replace("ocean", NTEBiomeExtensions.ocean());
        OCEAN_REEF = replace("ocean_reef", NTEBiomeExtensions.oceanReef());
        DEEP_OCEAN = replace("deep_ocean", NTEBiomeExtensions.deepOcean());
        DEEP_OCEAN_TRENCH = replace("deep_ocean_trench", NTEBiomeExtensions.deepOceanTrench());
        PLAINS = replace("plains", NTEBiomeExtensions.plains());
        HILLS = replace("hills", NTEBiomeExtensions.hills());
        LOWLANDS = replace("lowlands", NTEBiomeExtensions.lowlands());
        SALT_MARSH = replace("salt_marsh", NTEBiomeExtensions.saltMarsh());
        LOW_CANYONS = replace("low_canyons", NTEBiomeExtensions.lowCanyons());
        SHORE = replace("shore", NTEBiomeExtensions.shore());
        HIGHLANDS = replace("highlands", NTEBiomeExtensions.highlands());
        BADLANDS = replace("badlands", NTEBiomeExtensions.badlands());
        PLATEAU = replace("plateau", NTEBiomeExtensions.plateau());
        CANYONS = replace("canyons", NTEBiomeExtensions.canyons());
        MOUNTAINS = replace("mountains", NTEBiomeExtensions.mountains());
        OLD_MOUNTAINS = replace("old_mountains", NTEBiomeExtensions.oldMountains());
        OCEANIC_MOUNTAINS = replace("oceanic_mountains", NTEBiomeExtensions.oceanicMountains());
        VOLCANIC_MOUNTAINS = replace("volcanic_mountains", NTEBiomeExtensions.volcanicMountains());
        VOLCANIC_OCEANIC_MOUNTAINS = replace("volcanic_oceanic_mountains", NTEBiomeExtensions.volcanicOceanicMountains());
        TIDAL_FLATS = replace("tidal_flats", NTEBiomeExtensions.tidalFlats());
        RIVER = replace("river", NTEBiomeExtensions.river());
        MOUNTAIN_LAKE = replace("mountain_lake", NTEBiomeExtensions.mountainLake());
        OLD_MOUNTAIN_LAKE = replace("old_mountain_lake", NTEBiomeExtensions.oldMountainLake());
        OCEANIC_MOUNTAIN_LAKE = replace("oceanic_mountain_lake", NTEBiomeExtensions.oceanicMountainLake());
        VOLCANIC_MOUNTAIN_LAKE = replace("volcanic_mountain_lake", NTEBiomeExtensions.volcanicMountainLake());
        VOLCANIC_OCEANIC_MOUNTAIN_LAKE = replace("volcanic_oceanic_mountain_lake", NTEBiomeExtensions.volcanicOceanicMountainLake());
        PLATEAU_LAKE = replace("plateau_lake", NTEBiomeExtensions.plateauLake());

        replace("plateau_wide", NTEBiomeExtensions.plateauWide());
        replace("guano_island", NTEBiomeExtensions.guanoIsland());
        replace("sea_stacks", NTEBiomeExtensions.seaStacks());
        replace("terrace_upper", NTEBiomeExtensions.terraceUpper());
        replace("terrace_lower", NTEBiomeExtensions.terraceLower());
        replace("setback_cliffs", NTEBiomeExtensions.setbackCliffs());
        replace("coastal_dunes", NTEBiomeExtensions.coastalDunes());
        replace("rocky_shores", NTEBiomeExtensions.rockyShores());
        replace("embayments", NTEBiomeExtensions.embayments());
        replace("mud_flats", NTEBiomeExtensions.mudFlats());
        replace("salt_flats", NTEBiomeExtensions.saltFlats());
        replace("dune_sea", NTEBiomeExtensions.duneSea());
        replace("grassy_dunes", NTEBiomeExtensions.grassyDunes());
        replace("whorled_canyons", NTEBiomeExtensions.whorledCanyons());
        replace("stair_step_canyons", NTEBiomeExtensions.stairStepCanyons());
        replace("mesas", NTEBiomeExtensions.mesas());
        replace("buttes", NTEBiomeExtensions.buttes());
        replace("hoodoos", NTEBiomeExtensions.hoodoos());
        replace("rocky_plateau", NTEBiomeExtensions.rockyPlateau());
        replace("tower_karst_plains", NTEBiomeExtensions.towerKarstPlains());
        replace("tower_karst_canyons", NTEBiomeExtensions.towerKarstCanyons());
        replace("tower_karst_hills", NTEBiomeExtensions.towerKarstHills());
        replace("tower_karst_highlands", NTEBiomeExtensions.towerKarstHighlands());
        replace("tower_karst_lake", NTEBiomeExtensions.towerKarstLake());
        replace("tower_karst_bay", NTEBiomeExtensions.towerKarstBay());
        replace("burren_plateau", NTEBiomeExtensions.burrenPlateau());
        replace("burren_badlands", NTEBiomeExtensions.burrenBadlands());
        replace("burren_badlands_tall", NTEBiomeExtensions.burrenBadlandsTall());
        replace("burren_roche_moutonee", NTEBiomeExtensions.burrenRocheMoutonee());
        replace("burren_plains", NTEBiomeExtensions.burrenPlains());
        replace("shilin_plains", NTEBiomeExtensions.shilinPlains());
        replace("shilin_canyons", NTEBiomeExtensions.shilinCanyons());
        replace("shilin_hills", NTEBiomeExtensions.shilinHills());
        replace("shilin_highlands", NTEBiomeExtensions.shilinHighlands());
        replace("shilin_plateau", NTEBiomeExtensions.shilinPlateau());
        replace("doline_plains", NTEBiomeExtensions.dolinePlains());
        replace("doline_hills", NTEBiomeExtensions.dolineHills());
        replace("doline_rolling_hills", NTEBiomeExtensions.dolineRollingHills());
        replace("doline_highlands", NTEBiomeExtensions.dolineHighlands());
        replace("doline_plateau", NTEBiomeExtensions.dolinePlateau());
        replace("doline_canyons", NTEBiomeExtensions.dolineCanyons());
        replace("cenote_plains", NTEBiomeExtensions.cenotePlains());
        replace("cenote_hills", NTEBiomeExtensions.cenoteHills());
        replace("cenote_rolling_hills", NTEBiomeExtensions.cenoteRollingHills());
        replace("cenote_canyons", NTEBiomeExtensions.cenoteCanyons());
        replace("cenote_highlands", NTEBiomeExtensions.cenoteHighlands());
        replace("cenote_plateau", NTEBiomeExtensions.cenotePlateau());
        replace("extreme_doline_plateau", NTEBiomeExtensions.extremeDolinePlateau());
        replace("extreme_doline_mountains", NTEBiomeExtensions.extremeDolineMountains());
        replace("active_shield_volcano", NTEBiomeExtensions.activeShieldVolcano());
        replace("dormant_shield_volcano", NTEBiomeExtensions.dormantShieldVolcano());
        replace("extinct_shield_volcano", NTEBiomeExtensions.extinctShieldVolcano());
        replace("ancient_shield_volcano", NTEBiomeExtensions.ancientShieldVolcano());
        replace("sunken_shield_volcano", NTEBiomeExtensions.sunkenShieldVolcano());
        replace("shield_volcano_shore", NTEBiomeExtensions.shieldVolcanoShore());
        replace("old_shield_volcano_shore", NTEBiomeExtensions.oldShieldVolcanoShore());
        replace("ice_sheet", NTEBiomeExtensions.iceSheet());
        replace("ice_sheet_mountains", NTEBiomeExtensions.iceSheetMountains());
        replace("ice_sheet_oceanic_mountains", NTEBiomeExtensions.iceSheetOceanicMountains());
        replace("ice_sheet_shield_volcano", NTEBiomeExtensions.iceSheetShieldVolcano());
        replace("ice_sheet_tuyas", NTEBiomeExtensions.iceSheetTuyas());
        replace("subglacial_lake", NTEBiomeExtensions.subglacialLake());
        replace("ice_sheet_edge", NTEBiomeExtensions.iceSheetEdge());
        replace("ice_sheet_tuyas_edge", NTEBiomeExtensions.iceSheetTuyasEdge());
        replace("ice_sheet_oceanic", NTEBiomeExtensions.iceSheetOceanic());
        replace("ice_sheet_oceanic_mountains_edge", NTEBiomeExtensions.iceSheetOceanicMountainsEdge());
        replace("ice_sheet_mountains_edge", NTEBiomeExtensions.iceSheetMountainsEdge());
        replace("glaciated_mountains", NTEBiomeExtensions.glaciatedMountains());
        replace("glaciated_oceanic_mountains", NTEBiomeExtensions.glaciatedOceanicMountains());
        replace("meltwater_lake", NTEBiomeExtensions.meltwaterLake());
        replace("glaciated_shield_volcano", NTEBiomeExtensions.glaciatedShieldVolcano());
        replace("ice_sheet_shore", NTEBiomeExtensions.iceSheetShore());
        replace("glacially_carved_mountains", NTEBiomeExtensions.glaciallyCarvedMountains());
        replace("glacially_carved_oceanic_mountains", NTEBiomeExtensions.glaciallyCarvedOceanicMountains());
        replace("drumlins", NTEBiomeExtensions.drumlins());
        replace("tuyas", NTEBiomeExtensions.tuyas());
        replace("knob_and_kettle", NTEBiomeExtensions.knobAndKettle());
        replace("patterned_ground", NTEBiomeExtensions.patternedGround());
        replace("inverted_patterned_ground", NTEBiomeExtensions.invertedPatternedGround());
        replace("stone_circles", NTEBiomeExtensions.stoneCircles());
    }

    /**
     * @author Codex
     * @reason Avoid stale BiomeBridge extension caches and IdentityHashMap key identity misses after addon biome extension replacement.
     */
    @Overwrite(remap = false)
    public static BiomeExtension getExtension(CommonLevelAccessor level, Biome biome)
    {
        return tfe$getDirectExtension(level, biome);
    }

    /**
     * @author Codex
     * @reason External datapacks can place adjacent biomes without TFC extensions; chunk decoration should not hard-crash on them.
     */
    @Overwrite(remap = false)
    public static BiomeExtension getExtensionOrThrow(LevelAccessor level, Biome biome)
    {
        final BiomeExtension extension = tfe$getDirectExtension(level, biome);
        if (extension != null)
        {
            return extension;
        }

        final String biomeId = tfe$getBiomeId(level, biome);
        if (tfe$warnedMissingBiomeExtensions.add(biomeId))
        {
            TFE_LOGGER.warn("Missing TFC biome extension for {} during chunk decoration; using tfc:plains climate fallback and skipping unindexed decoration features.", biomeId);
        }
        return PLAINS;
    }

    /**
     * @author Codex
     * @reason Match biome extensions by location as well as by ResourceKey identity.
     */
    @Overwrite(remap = false)
    public static BiomeExtension getById(ResourceLocation id)
    {
        return tfe$getExtensionByLocation(id, null);
    }

    @Nullable
    @Unique
    private static BiomeExtension tfe$getDirectExtension(CommonLevelAccessor level, Biome biome)
    {
        final Registry<Biome> registry = level.registryAccess().registryOrThrow(Registries.BIOME);
        return registry.getResourceKey(biome)
            .map(key -> tfe$getExtensionByLocation(key.location(), key))
            .orElse(null);
    }

    @Nullable
    @Unique
    private static BiomeExtension tfe$getExtensionByLocation(ResourceLocation id, @Nullable ResourceKey<Biome> preferredKey)
    {
        final BiomeExtension cached = tfe$extensionsByLocation.get(id);
        if (cached != null)
        {
            return cached;
        }

        if (preferredKey != null)
        {
            final BiomeExtension extension = EXTENSIONS.get(preferredKey);
            if (extension != null)
            {
                tfe$extensionsByLocation.putIfAbsent(id, extension);
                return extension;
            }
        }

        for (Map.Entry<ResourceKey<Biome>, BiomeExtension> entry : EXTENSIONS.entrySet())
        {
            if (entry.getKey().location().equals(id))
            {
                tfe$extensionsByLocation.putIfAbsent(id, entry.getValue());
                return entry.getValue();
            }
        }
        return null;
    }

    @Unique
    private static String tfe$getBiomeId(CommonLevelAccessor level, Biome biome)
    {
        final Registry<Biome> registry = level.registryAccess().registryOrThrow(Registries.BIOME);
        return registry.getResourceKey(biome)
            .map(key -> key.location().toString())
            .orElse("registry_id=" + registry.getId(biome));
    }

    private static BiomeExtension replace(String name, BiomeExtension extension)
    {
        final BiomeExtension cached = NTEBiomeCache.get(name, () -> extension);
        EXTENSIONS.put(ResourceKey.create(Registries.BIOME, Helpers.identifier(name)), cached);
        return cached;
    }
}
