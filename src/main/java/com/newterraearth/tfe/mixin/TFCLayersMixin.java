package com.newterraearth.tfe.mixin;

import java.util.Arrays;
import java.util.function.Supplier;

import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.layer.MoreShoresLayer;
import net.dries007.tfc.world.layer.RegionBiomeLayer;
import net.dries007.tfc.world.layer.RegionEdgeBiomeLayer;
import net.dries007.tfc.world.layer.RegionLayer;
import net.dries007.tfc.world.layer.SmoothLayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.dries007.tfc.world.layer.ZoomLayer;
import net.dries007.tfc.world.layer.framework.AreaFactory;
import net.dries007.tfc.world.layer.framework.TypedAreaFactory;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;

import com.newterraearth.tfe.world.NTEBiomeCache;
import com.newterraearth.tfe.world.NTEBiomeExtensions;
import com.newterraearth.tfe.world.NTELayerIds;
import com.newterraearth.tfe.world.NTESeed;
import com.newterraearth.tfe.world.layer.NTEIceSheetEdgeLayer;
import com.newterraearth.tfe.world.layer.NTERiverShoreLayer;

@Mixin(value = TFCLayers.class, remap = false)
public abstract class TFCLayersMixin
{
    @Shadow @Final @Mutable private static BiomeExtension[] BIOME_LAYERS;

    @Shadow
    public static int register(Supplier<BiomeExtension> variants)
    {
        throw new AssertionError();
    }

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void tfe$registerExtraLayers(CallbackInfo ci)
    {
        if (BIOME_LAYERS.length < 128)
        {
            BIOME_LAYERS = Arrays.copyOf(BIOME_LAYERS, 128);
        }

        NTELayerIds.COLLISIONAL_MOUNTAINS = registerCached("collisional_mountains", NTEBiomeExtensions::collisionalMountains);
        NTELayerIds.OCEANIC_VOLCANIC_ARC = registerCached("oceanic_volcanic_arc", NTEBiomeExtensions::oceanicVolcanicArc);
        NTELayerIds.OCEAN_ATOLLS = registerCached("ocean_atolls", NTEBiomeExtensions::oceanAtolls);
        NTELayerIds.DEEP_OCEAN_ATOLLS = registerCached("deep_ocean_atolls", NTEBiomeExtensions::deepOceanAtolls);
        NTELayerIds.OCEAN_RIDGE = registerCached("ocean_ridge", NTEBiomeExtensions::oceanRidge);
        NTELayerIds.RIFT_VALLEY = registerCached("rift_valley", NTEBiomeExtensions::riftValley);
        NTELayerIds.RIFT_LAKE = registerCached("rift_lake", NTEBiomeExtensions::riftLake);
        NTELayerIds.RIVER_VALLEY = registerCached("river_valley", NTEBiomeExtensions::riverValley);
        NTELayerIds.VOLCANIC_MOUNTAIN_ISLANDS = registerCached("volcanic_mountain_islands", NTEBiomeExtensions::volcanicMountainIslands);
        NTELayerIds.VOLCANIC_ISLAND = registerCached("volcanic_island", NTEBiomeExtensions::volcanicIsland);
        NTELayerIds.PLATEAU_WIDE = registerCached("plateau_wide", NTEBiomeExtensions::plateauWide);
        NTELayerIds.GUANO_ISLAND = registerCached("guano_island", NTEBiomeExtensions::guanoIsland);
        NTELayerIds.SEA_STACKS = registerCached("sea_stacks", NTEBiomeExtensions::seaStacks);
        NTELayerIds.TERRACE_UPPER = registerCached("terrace_upper", NTEBiomeExtensions::terraceUpper);
        NTELayerIds.TERRACE_LOWER = registerCached("terrace_lower", NTEBiomeExtensions::terraceLower);
        NTELayerIds.SETBACK_CLIFFS = registerCached("setback_cliffs", NTEBiomeExtensions::setbackCliffs);
        NTELayerIds.COASTAL_DUNES = registerCached("coastal_dunes", NTEBiomeExtensions::coastalDunes);
        NTELayerIds.ROCKY_SHORES = registerCached("rocky_shores", NTEBiomeExtensions::rockyShores);
        NTELayerIds.EMBAYMENTS = registerCached("embayments", NTEBiomeExtensions::embayments);
        NTELayerIds.MUD_FLATS = registerCached("mud_flats", NTEBiomeExtensions::mudFlats);
        NTELayerIds.SALT_FLATS = registerCached("salt_flats", NTEBiomeExtensions::saltFlats);
        NTELayerIds.DUNE_SEA = registerCached("dune_sea", NTEBiomeExtensions::duneSea);
        NTELayerIds.GRASSY_DUNES = registerCached("grassy_dunes", NTEBiomeExtensions::grassyDunes);
        NTELayerIds.WHORLED_CANYONS = registerCached("whorled_canyons", NTEBiomeExtensions::whorledCanyons);
        NTELayerIds.STAIR_STEP_CANYONS = registerCached("stair_step_canyons", NTEBiomeExtensions::stairStepCanyons);
        NTELayerIds.MESAS = registerCached("mesas", NTEBiomeExtensions::mesas);
        NTELayerIds.BUTTES = registerCached("buttes", NTEBiomeExtensions::buttes);
        NTELayerIds.HOODOOS = registerCached("hoodoos", NTEBiomeExtensions::hoodoos);
        NTELayerIds.ROCKY_PLATEAU = registerCached("rocky_plateau", NTEBiomeExtensions::rockyPlateau);
        NTELayerIds.TOWER_KARST_PLAINS = registerCached("tower_karst_plains", NTEBiomeExtensions::towerKarstPlains);
        NTELayerIds.TOWER_KARST_CANYONS = registerCached("tower_karst_canyons", NTEBiomeExtensions::towerKarstCanyons);
        NTELayerIds.TOWER_KARST_HILLS = registerCached("tower_karst_hills", NTEBiomeExtensions::towerKarstHills);
        NTELayerIds.TOWER_KARST_HIGHLANDS = registerCached("tower_karst_highlands", NTEBiomeExtensions::towerKarstHighlands);
        NTELayerIds.TOWER_KARST_LAKE = registerCached("tower_karst_lake", NTEBiomeExtensions::towerKarstLake);
        NTELayerIds.TOWER_KARST_BAY = registerCached("tower_karst_bay", NTEBiomeExtensions::towerKarstBay);
        NTELayerIds.BURREN_PLATEAU = registerCached("burren_plateau", NTEBiomeExtensions::burrenPlateau);
        NTELayerIds.BURREN_BADLANDS = registerCached("burren_badlands", NTEBiomeExtensions::burrenBadlands);
        NTELayerIds.BURREN_BADLANDS_TALL = registerCached("burren_badlands_tall", NTEBiomeExtensions::burrenBadlandsTall);
        NTELayerIds.BURREN_ROCHE_MOUTONEE = registerCached("burren_roche_moutonee", NTEBiomeExtensions::burrenRocheMoutonee);
        NTELayerIds.BURREN_PLAINS = registerCached("burren_plains", NTEBiomeExtensions::burrenPlains);
        NTELayerIds.SHILIN_PLAINS = registerCached("shilin_plains", NTEBiomeExtensions::shilinPlains);
        NTELayerIds.SHILIN_CANYONS = registerCached("shilin_canyons", NTEBiomeExtensions::shilinCanyons);
        NTELayerIds.SHILIN_HILLS = registerCached("shilin_hills", NTEBiomeExtensions::shilinHills);
        NTELayerIds.SHILIN_HIGHLANDS = registerCached("shilin_highlands", NTEBiomeExtensions::shilinHighlands);
        NTELayerIds.SHILIN_PLATEAU = registerCached("shilin_plateau", NTEBiomeExtensions::shilinPlateau);
        NTELayerIds.DOLINE_PLAINS = registerCached("doline_plains", NTEBiomeExtensions::dolinePlains);
        NTELayerIds.DOLINE_HILLS = registerCached("doline_hills", NTEBiomeExtensions::dolineHills);
        NTELayerIds.DOLINE_ROLLING_HILLS = registerCached("doline_rolling_hills", NTEBiomeExtensions::dolineRollingHills);
        NTELayerIds.DOLINE_HIGHLANDS = registerCached("doline_highlands", NTEBiomeExtensions::dolineHighlands);
        NTELayerIds.DOLINE_PLATEAU = registerCached("doline_plateau", NTEBiomeExtensions::dolinePlateau);
        NTELayerIds.DOLINE_CANYONS = registerCached("doline_canyons", NTEBiomeExtensions::dolineCanyons);
        NTELayerIds.CENOTE_PLAINS = registerCached("cenote_plains", NTEBiomeExtensions::cenotePlains);
        NTELayerIds.CENOTE_HILLS = registerCached("cenote_hills", NTEBiomeExtensions::cenoteHills);
        NTELayerIds.CENOTE_ROLLING_HILLS = registerCached("cenote_rolling_hills", NTEBiomeExtensions::cenoteRollingHills);
        NTELayerIds.CENOTE_CANYONS = registerCached("cenote_canyons", NTEBiomeExtensions::cenoteCanyons);
        NTELayerIds.CENOTE_HIGHLANDS = registerCached("cenote_highlands", NTEBiomeExtensions::cenoteHighlands);
        NTELayerIds.CENOTE_PLATEAU = registerCached("cenote_plateau", NTEBiomeExtensions::cenotePlateau);
        NTELayerIds.EXTREME_DOLINE_PLATEAU = registerCached("extreme_doline_plateau", NTEBiomeExtensions::extremeDolinePlateau);
        NTELayerIds.EXTREME_DOLINE_MOUNTAINS = registerCached("extreme_doline_mountains", NTEBiomeExtensions::extremeDolineMountains);
        NTELayerIds.ACTIVE_SHIELD_VOLCANO = registerCached("active_shield_volcano", NTEBiomeExtensions::activeShieldVolcano);
        NTELayerIds.DORMANT_SHIELD_VOLCANO = registerCached("dormant_shield_volcano", NTEBiomeExtensions::dormantShieldVolcano);
        NTELayerIds.EXTINCT_SHIELD_VOLCANO = registerCached("extinct_shield_volcano", NTEBiomeExtensions::extinctShieldVolcano);
        NTELayerIds.ANCIENT_SHIELD_VOLCANO = registerCached("ancient_shield_volcano", NTEBiomeExtensions::ancientShieldVolcano);
        NTELayerIds.SUNKEN_SHIELD_VOLCANO = registerCached("sunken_shield_volcano", NTEBiomeExtensions::sunkenShieldVolcano);
        NTELayerIds.SHIELD_VOLCANO_SHORE = registerCached("shield_volcano_shore", NTEBiomeExtensions::shieldVolcanoShore);
        NTELayerIds.OLD_SHIELD_VOLCANO_SHORE = registerCached("old_shield_volcano_shore", NTEBiomeExtensions::oldShieldVolcanoShore);
        NTELayerIds.ICE_SHEET = registerCached("ice_sheet", NTEBiomeExtensions::iceSheet);
        NTELayerIds.ICE_SHEET_MOUNTAINS = registerCached("ice_sheet_mountains", NTEBiomeExtensions::iceSheetMountains);
        NTELayerIds.ICE_SHEET_OCEANIC_MOUNTAINS = registerCached("ice_sheet_oceanic_mountains", NTEBiomeExtensions::iceSheetOceanicMountains);
        NTELayerIds.ICE_SHEET_VOLCANIC_MOUNTAINS = registerCached("ice_sheet_volcanic_mountains", NTEBiomeExtensions::iceSheetVolcanicMountains);
        NTELayerIds.ICE_SHEET_VOLCANIC_OCEANIC_MOUNTAINS = registerCached("ice_sheet_volcanic_oceanic_mountains", NTEBiomeExtensions::iceSheetVolcanicOceanicMountains);
        NTELayerIds.ICE_SHEET_SHIELD_VOLCANO = registerCached("ice_sheet_shield_volcano", NTEBiomeExtensions::iceSheetShieldVolcano);
        NTELayerIds.ICE_SHEET_TUYAS = registerCached("ice_sheet_tuyas", NTEBiomeExtensions::iceSheetTuyas);
        NTELayerIds.SUBGLACIAL_LAKE = registerCached("subglacial_lake", NTEBiomeExtensions::subglacialLake);
        NTELayerIds.ICE_SHEET_EDGE = registerCached("ice_sheet_edge", NTEBiomeExtensions::iceSheetEdge);
        NTELayerIds.ICE_SHEET_TUYAS_EDGE = registerCached("ice_sheet_tuyas_edge", NTEBiomeExtensions::iceSheetTuyasEdge);
        NTELayerIds.ICE_SHEET_OCEANIC = registerCached("ice_sheet_oceanic", NTEBiomeExtensions::iceSheetOceanic);
        NTELayerIds.ICE_SHEET_OCEANIC_MOUNTAINS_EDGE = registerCached("ice_sheet_oceanic_mountains_edge", NTEBiomeExtensions::iceSheetOceanicMountainsEdge);
        NTELayerIds.ICE_SHEET_MOUNTAINS_EDGE = registerCached("ice_sheet_mountains_edge", NTEBiomeExtensions::iceSheetMountainsEdge);
        NTELayerIds.GLACIATED_MOUNTAINS = registerCached("glaciated_mountains", NTEBiomeExtensions::glaciatedMountains);
        NTELayerIds.GLACIATED_OCEANIC_MOUNTAINS = registerCached("glaciated_oceanic_mountains", NTEBiomeExtensions::glaciatedOceanicMountains);
        NTELayerIds.GLACIATED_VOLCANIC_MOUNTAINS = registerCached("glaciated_volcanic_mountains", NTEBiomeExtensions::glaciatedVolcanicMountains);
        NTELayerIds.GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS = registerCached("glaciated_volcanic_oceanic_mountains", NTEBiomeExtensions::glaciatedVolcanicOceanicMountains);
        NTELayerIds.MELTWATER_LAKE = registerCached("meltwater_lake", NTEBiomeExtensions::meltwaterLake);
        NTELayerIds.GLACIATED_SHIELD_VOLCANO = registerCached("glaciated_shield_volcano", NTEBiomeExtensions::glaciatedShieldVolcano);
        NTELayerIds.ICE_SHEET_SHORE = registerCached("ice_sheet_shore", NTEBiomeExtensions::iceSheetShore);
        NTELayerIds.GLACIALLY_CARVED_MOUNTAINS = registerCached("glacially_carved_mountains", NTEBiomeExtensions::glaciallyCarvedMountains);
        NTELayerIds.GLACIALLY_CARVED_OCEANIC_MOUNTAINS = registerCached("glacially_carved_oceanic_mountains", NTEBiomeExtensions::glaciallyCarvedOceanicMountains);
        NTELayerIds.GLACIALLY_CARVED_VOLCANIC_MOUNTAINS = registerCached("glacially_carved_volcanic_mountains", NTEBiomeExtensions::glaciallyCarvedVolcanicMountains);
        NTELayerIds.GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS = registerCached("glacially_carved_volcanic_oceanic_mountains", NTEBiomeExtensions::glaciallyCarvedVolcanicOceanicMountains);
        NTELayerIds.DRUMLINS = registerCached("drumlins", NTEBiomeExtensions::drumlins);
        NTELayerIds.TUYAS = registerCached("tuyas", NTEBiomeExtensions::tuyas);
        NTELayerIds.KNOB_AND_KETTLE = registerCached("knob_and_kettle", NTEBiomeExtensions::knobAndKettle);
        NTELayerIds.PATTERNED_GROUND = registerCached("patterned_ground", NTEBiomeExtensions::patternedGround);
        NTELayerIds.INVERTED_PATTERNED_GROUND = registerCached("inverted_patterned_ground", NTEBiomeExtensions::invertedPatternedGround);
        NTELayerIds.STONE_CIRCLES = registerCached("stone_circles", NTEBiomeExtensions::stoneCircles);
    }

    private static int registerCached(String name, Supplier<BiomeExtension> variants)
    {
        return register(() -> NTEBiomeCache.get(name, variants));
    }

    /**
     * @author Codex
     * @reason Insert the 1.21 ice-sheet edge pass into the 1.20 biome layer chain.
     */
    @Overwrite(remap = false)
    public static AreaFactory createRegionBiomeLayer(RegionGenerator generator, long seed)
    {
        final NTESeed layerSeed = NTESeed.of(seed);
        final TypedAreaFactory<Region.Point> regionLayer = new RegionLayer(generator).apply(layerSeed.next());

        AreaFactory mainLayer = RegionBiomeLayer.INSTANCE.apply(regionLayer);

        mainLayer = RegionEdgeBiomeLayer.INSTANCE.apply(layerSeed.next(), mainLayer);
        mainLayer = ZoomLayer.NORMAL.apply(layerSeed.next(), mainLayer);

        mainLayer = NTERiverShoreLayer.INSTANCE.apply(layerSeed.next(), mainLayer);
        mainLayer = MoreShoresLayer.INSTANCE.apply(layerSeed.next(), mainLayer);
        mainLayer = NTEIceSheetEdgeLayer.INSTANCE.apply(layerSeed.next(), mainLayer);
        mainLayer = ZoomLayer.NORMAL.apply(layerSeed.next(), mainLayer);
        mainLayer = ZoomLayer.NORMAL.apply(layerSeed.next(), mainLayer);

        mainLayer = ZoomLayer.NORMAL.apply(layerSeed.next(), mainLayer);
        mainLayer = ZoomLayer.NORMAL.apply(layerSeed.next(), mainLayer);

        mainLayer = SmoothLayer.INSTANCE.apply(layerSeed.next(), mainLayer);

        return mainLayer;
    }

    /**
     * @author Codex
     * @reason Align shore exclusions to the local 1.21 TFCLayers implementation.
     */
    @Overwrite(remap = false)
    public static boolean hasShore(int value)
    {
        return !TFCLayers.isOcean(value)
            && value != TFCLayers.LOW_CANYONS
            && value != TFCLayers.CANYONS
            && value != TFCLayers.OCEANIC_MOUNTAINS
            && value != TFCLayers.VOLCANIC_OCEANIC_MOUNTAINS
            && value != NTELayerIds.TOWER_KARST_BAY
            && value != NTELayerIds.SUNKEN_SHIELD_VOLCANO
            && value != NTELayerIds.GLACIALLY_CARVED_OCEANIC_MOUNTAINS
            && value != NTELayerIds.GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS
            && value != NTELayerIds.GLACIATED_OCEANIC_MOUNTAINS
            && value != NTELayerIds.ICE_SHEET_OCEANIC_MOUNTAINS_EDGE
            && value != NTELayerIds.ICE_SHEET_SHIELD_VOLCANO
            && value != NTELayerIds.GLACIATED_SHIELD_VOLCANO
            && value != NTELayerIds.GUANO_ISLAND
            && value != NTELayerIds.VOLCANIC_MOUNTAIN_ISLANDS;
    }

    /**
     * @author Codex
     * @reason Align shore routing to the local 1.21 TFCLayers implementation.
     */
    @Overwrite(remap = false)
    public static int shoreFor(int value)
    {
        if (value == TFCLayers.LOWLANDS || value == TFCLayers.SALT_MARSH)
        {
            return TFCLayers.SALT_MARSH;
        }
        if (value == TFCLayers.MOUNTAINS || value == NTELayerIds.COLLISIONAL_MOUNTAINS)
        {
            return TFCLayers.OCEANIC_MOUNTAINS;
        }
        if (value == TFCLayers.VOLCANIC_MOUNTAINS)
        {
            return TFCLayers.VOLCANIC_OCEANIC_MOUNTAINS;
        }
        if (value == NTELayerIds.TOWER_KARST_LAKE)
        {
            return NTELayerIds.TOWER_KARST_BAY;
        }
        if (value == NTELayerIds.ACTIVE_SHIELD_VOLCANO)
        {
            return NTELayerIds.SHIELD_VOLCANO_SHORE;
        }
        if (value == NTELayerIds.DORMANT_SHIELD_VOLCANO || value == NTELayerIds.EXTINCT_SHIELD_VOLCANO || value == NTELayerIds.ANCIENT_SHIELD_VOLCANO)
        {
            return NTELayerIds.OLD_SHIELD_VOLCANO_SHORE;
        }
        if (isFlatIceSheet(value) || value == NTELayerIds.ICE_SHEET_EDGE || value == NTELayerIds.ICE_SHEET_OCEANIC)
        {
            return NTELayerIds.ICE_SHEET_SHORE;
        }
        if (value == NTELayerIds.ICE_SHEET_OCEANIC_MOUNTAINS || value == NTELayerIds.ICE_SHEET_VOLCANIC_OCEANIC_MOUNTAINS)
        {
            return NTELayerIds.ICE_SHEET_OCEANIC_MOUNTAINS_EDGE;
        }
        if (value == NTELayerIds.GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS || value == NTELayerIds.GLACIATED_VOLCANIC_MOUNTAINS || value == NTELayerIds.GLACIALLY_CARVED_VOLCANIC_MOUNTAINS)
        {
            return NTELayerIds.GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS;
        }
        if (value == NTELayerIds.GLACIATED_OCEANIC_MOUNTAINS || value == NTELayerIds.GLACIATED_MOUNTAINS || value == NTELayerIds.GLACIALLY_CARVED_MOUNTAINS)
        {
            return NTELayerIds.GLACIATED_OCEANIC_MOUNTAINS;
        }
        if (value == TFCLayers.OLD_MOUNTAINS || value == NTELayerIds.EXTREME_DOLINE_MOUNTAINS)
        {
            return NTELayerIds.TERRACE_LOWER;
        }
        if (value == TFCLayers.PLATEAU || value == NTELayerIds.EXTREME_DOLINE_PLATEAU || value == NTELayerIds.BURREN_PLATEAU || value == NTELayerIds.SHILIN_PLATEAU)
        {
            return NTELayerIds.SEA_STACKS;
        }
        if (value == NTELayerIds.PLATEAU_WIDE || value == NTELayerIds.ROCKY_PLATEAU || value == NTELayerIds.DOLINE_PLATEAU)
        {
            return NTELayerIds.SETBACK_CLIFFS;
        }
        if (value == TFCLayers.HIGHLANDS || value == NTELayerIds.CENOTE_HIGHLANDS || value == NTELayerIds.DOLINE_HIGHLANDS || value == NTELayerIds.SHILIN_HIGHLANDS || value == NTELayerIds.TOWER_KARST_HIGHLANDS)
        {
            return NTELayerIds.ROCKY_SHORES;
        }
        if (value == TFCLayers.ROLLING_HILLS || value == NTELayerIds.DOLINE_ROLLING_HILLS || value == NTELayerIds.CENOTE_ROLLING_HILLS)
        {
            return NTELayerIds.EMBAYMENTS;
        }
        if (value == TFCLayers.HILLS || value == NTELayerIds.CENOTE_HILLS || value == NTELayerIds.DOLINE_HILLS || value == NTELayerIds.SHILIN_HILLS || value == NTELayerIds.TOWER_KARST_HILLS || value == NTELayerIds.GRASSY_DUNES || value == NTELayerIds.DUNE_SEA)
        {
            return NTELayerIds.COASTAL_DUNES;
        }
        return TFCLayers.TIDAL_FLATS;
    }

    /**
     * @author Codex
     * @reason Align lake exclusions to the local 1.21 TFCLayers implementation.
     */
    @Overwrite(remap = false)
    public static boolean hasLake(int value)
    {
        return !TFCLayers.isOcean(value)
            && value != TFCLayers.LAKE
            && value != TFCLayers.MOUNTAIN_LAKE
            && value != TFCLayers.VOLCANIC_MOUNTAIN_LAKE
            && value != TFCLayers.OLD_MOUNTAIN_LAKE
            && value != TFCLayers.OCEANIC_MOUNTAIN_LAKE
            && value != TFCLayers.VOLCANIC_OCEANIC_MOUNTAIN_LAKE
            && value != TFCLayers.PLATEAU_LAKE
            && value != TFCLayers.BADLANDS
            && value != NTELayerIds.SALT_FLATS
            && value != NTELayerIds.MUD_FLATS
            && value != NTELayerIds.ACTIVE_SHIELD_VOLCANO
            && value != NTELayerIds.DORMANT_SHIELD_VOLCANO
            && value != NTELayerIds.EXTINCT_SHIELD_VOLCANO
            && value != NTELayerIds.ANCIENT_SHIELD_VOLCANO
            && value != NTELayerIds.ICE_SHEET_MOUNTAINS
            && value != NTELayerIds.ICE_SHEET_MOUNTAINS_EDGE
            && value != NTELayerIds.ICE_SHEET_OCEANIC_MOUNTAINS
            && value != NTELayerIds.ICE_SHEET_OCEANIC_MOUNTAINS_EDGE
            && value != NTELayerIds.ICE_SHEET_SHIELD_VOLCANO
            && value != NTELayerIds.ICE_SHEET_SHORE
            && value != NTELayerIds.GLACIATED_SHIELD_VOLCANO
            && value != NTELayerIds.GLACIATED_MOUNTAINS
            && value != NTELayerIds.GLACIATED_OCEANIC_MOUNTAINS
            && value != NTELayerIds.GLACIATED_VOLCANIC_MOUNTAINS
            && value != NTELayerIds.GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS
            && value != NTELayerIds.GLACIALLY_CARVED_MOUNTAINS
            && value != NTELayerIds.GLACIALLY_CARVED_OCEANIC_MOUNTAINS
            && value != NTELayerIds.GLACIALLY_CARVED_VOLCANIC_MOUNTAINS
            && value != NTELayerIds.GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS
            && value != NTELayerIds.RIFT_LAKE
            && value != NTELayerIds.TOWER_KARST_LAKE
            && value != NTELayerIds.SUBGLACIAL_LAKE
            && value != NTELayerIds.MELTWATER_LAKE;
    }

    /**
     * @author Codex
     * @reason Align lake routing to the local 1.21 TFCLayers implementation.
     */
    @Overwrite(remap = false)
    public static int lakeFor(int value)
    {
        if (value == TFCLayers.MOUNTAINS || value == NTELayerIds.COLLISIONAL_MOUNTAINS)
        {
            return TFCLayers.MOUNTAIN_LAKE;
        }
        if (value == TFCLayers.VOLCANIC_MOUNTAINS)
        {
            return TFCLayers.VOLCANIC_MOUNTAIN_LAKE;
        }
        if (value == TFCLayers.OLD_MOUNTAINS || value == NTELayerIds.EXTREME_DOLINE_MOUNTAINS)
        {
            return TFCLayers.OLD_MOUNTAIN_LAKE;
        }
        if (value == TFCLayers.OCEANIC_MOUNTAINS)
        {
            return TFCLayers.OCEANIC_MOUNTAIN_LAKE;
        }
        if (value == TFCLayers.VOLCANIC_OCEANIC_MOUNTAINS)
        {
            return TFCLayers.VOLCANIC_OCEANIC_MOUNTAIN_LAKE;
        }
        if (value == TFCLayers.PLATEAU
            || value == NTELayerIds.PLATEAU_WIDE
            || value == NTELayerIds.ROCKY_PLATEAU
            || value == NTELayerIds.BURREN_PLATEAU
            || value == NTELayerIds.SHILIN_PLATEAU
            || value == NTELayerIds.DOLINE_PLATEAU
            || value == NTELayerIds.CENOTE_PLATEAU
            || value == NTELayerIds.EXTREME_DOLINE_PLATEAU)
        {
            return TFCLayers.PLATEAU_LAKE;
        }
        if (isFlatIceSheet(value))
        {
            return NTELayerIds.SUBGLACIAL_LAKE;
        }
        if (value == NTELayerIds.ICE_SHEET_EDGE)
        {
            return NTELayerIds.MELTWATER_LAKE;
        }
        // Keep the 4.2.9 tower-karst lake branch intact when the 1.20
        // river task converts a karst biome into a nearby lake. Without
        // these cases it falls through to the generic LAKE heightmap,
        // replacing the fenglin mountain profile with a sea-level profile.
        if (value == NTELayerIds.TOWER_KARST_CANYONS
            || value == NTELayerIds.TOWER_KARST_HIGHLANDS
            || value == NTELayerIds.TOWER_KARST_HILLS
            || value == NTELayerIds.TOWER_KARST_PLAINS)
        {
            return NTELayerIds.TOWER_KARST_LAKE;
        }
        return TFCLayers.LAKE;
    }

    /**
     * @author Codex
     * @reason Align low-terrain smoothing categories to the local 1.21 TFCLayers implementation.
     */
    @Overwrite(remap = false)
    public static boolean isLow(int value)
    {
        return value == TFCLayers.PLAINS
            || value == TFCLayers.HILLS
            || value == TFCLayers.LOW_CANYONS
            || value == TFCLayers.LOWLANDS
            || value == TFCLayers.SALT_MARSH
            || value == NTELayerIds.MUD_FLATS
            || value == NTELayerIds.SALT_FLATS
            || value == NTELayerIds.DUNE_SEA
            || value == NTELayerIds.RIFT_VALLEY;
    }

    /**
     * @author Codex
     * @reason Align mountain smoothing categories to the local 1.21 TFCLayers implementation.
     */
    @Overwrite(remap = false)
    public static boolean isMountains(int value)
    {
        return value == TFCLayers.MOUNTAINS
            || value == TFCLayers.OCEANIC_MOUNTAINS
            || value == TFCLayers.OLD_MOUNTAINS
            || value == TFCLayers.VOLCANIC_MOUNTAINS
            || value == TFCLayers.VOLCANIC_OCEANIC_MOUNTAINS
            || value == NTELayerIds.VOLCANIC_MOUNTAIN_ISLANDS
            || value == NTELayerIds.COLLISIONAL_MOUNTAINS;
    }

    /**
     * @author Codex
     * @reason Keep newly registered ocean families in the shoreline/lake layer contract.
     */
    @Overwrite(remap = false)
    public static boolean isOcean(int value)
    {
        return value == TFCLayers.OCEAN
            || value == TFCLayers.DEEP_OCEAN
            || value == TFCLayers.DEEP_OCEAN_TRENCH
            || value == TFCLayers.OCEAN_REEF
            || value == NTELayerIds.OCEANIC_VOLCANIC_ARC
            || value == NTELayerIds.OCEAN_ATOLLS
            || value == NTELayerIds.DEEP_OCEAN_ATOLLS
            || value == NTELayerIds.OCEAN_RIDGE;
    }

    private static boolean isFlatIceSheet(int value)
    {
        return value == NTELayerIds.ICE_SHEET
            || value == NTELayerIds.ICE_SHEET_TUYAS
            || value == NTELayerIds.SUBGLACIAL_LAKE;
    }
}
