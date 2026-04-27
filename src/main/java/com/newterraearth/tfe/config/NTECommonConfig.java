package com.newterraearth.tfe.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;

import com.newterraearth.tfe.world.crop.NTECrop;

public final class NTECommonConfig
{
    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue WILD_CROP_ALFALFA;
    private static final ForgeConfigSpec.BooleanValue WILD_CROP_CANOLA;
    private static final ForgeConfigSpec.BooleanValue WILD_CROP_CASSAVA;
    private static final ForgeConfigSpec.BooleanValue WILD_CROP_LENTIL;
    private static final ForgeConfigSpec.BooleanValue WILD_CROP_PEANUT;
    private static final ForgeConfigSpec.BooleanValue WILD_CROP_RADISH;

    private static final ForgeConfigSpec.BooleanValue FRUIT_TREE_CHERRY;
    private static final ForgeConfigSpec.BooleanValue FRUIT_TREE_GREEN_APPLE;
    private static final ForgeConfigSpec.BooleanValue FRUIT_TREE_LEMON;
    private static final ForgeConfigSpec.BooleanValue FRUIT_TREE_OLIVE;
    private static final ForgeConfigSpec.BooleanValue FRUIT_TREE_ORANGE;
    private static final ForgeConfigSpec.BooleanValue FRUIT_TREE_PEACH;
    private static final ForgeConfigSpec.BooleanValue FRUIT_TREE_PLUM;
    private static final ForgeConfigSpec.BooleanValue FRUIT_TREE_RED_APPLE;
    private static final ForgeConfigSpec.BooleanValue BAMBOO;
    private static final ForgeConfigSpec.BooleanValue GOLDEN_BAMBOO;
    private static final ForgeConfigSpec.BooleanValue ENTITY_SPAWN_BISON;
    private static final ForgeConfigSpec.BooleanValue ENTITY_SPAWN_LEOPARD_SEAL;
    private static final ForgeConfigSpec.BooleanValue ENTITY_SPAWN_LEMMING;
    private static final ForgeConfigSpec.BooleanValue ENTITY_SPAWN_MONGOOSE;
    private static final ForgeConfigSpec.BooleanValue ENTITY_SPAWN_JERBOA;
    private static final ForgeConfigSpec.IntValue SNOW_MAX_ACCUMULATION_ON_UPDATE;
    private static final ForgeConfigSpec.IntValue TICKS_PER_SNOW_ACCUMULATION;
    private static final ForgeConfigSpec.IntValue SNOW_MELT_MULTIPLIER;
    private static final ForgeConfigSpec.BooleanValue CROP_USE_CURRENT_RAINFALL_HYDRATION;
    private static final ForgeConfigSpec.BooleanValue FRUIT_USE_CURRENT_RAINFALL_HYDRATION;

    static
    {
        final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Only controls natural generation for addon-managed plantable content. It does not disable blocks, items, or planting logic.");
        builder.push("wild_crops");
        WILD_CROP_ALFALFA = builder.define("alfalfa", true);
        WILD_CROP_CANOLA = builder.define("canola", true);
        WILD_CROP_CASSAVA = builder.define("cassava", true);
        WILD_CROP_LENTIL = builder.define("lentil", true);
        WILD_CROP_PEANUT = builder.define("peanut", true);
        WILD_CROP_RADISH = builder.define("radish", true);
        builder.pop();

        builder.push("fruit_trees");
        FRUIT_TREE_CHERRY = builder.define("cherry", true);
        FRUIT_TREE_GREEN_APPLE = builder.define("green_apple", true);
        FRUIT_TREE_LEMON = builder.define("lemon", true);
        FRUIT_TREE_OLIVE = builder.define("olive", true);
        FRUIT_TREE_ORANGE = builder.define("orange", true);
        FRUIT_TREE_PEACH = builder.define("peach", true);
        FRUIT_TREE_PLUM = builder.define("plum", true);
        FRUIT_TREE_RED_APPLE = builder.define("red_apple", true);
        builder.pop();

        builder.push("bamboo");
        BAMBOO = builder.define("bamboo", true);
        GOLDEN_BAMBOO = builder.define("golden_bamboo", true);
        builder.pop();

        builder.comment("Controls natural spawn selection only, including biome spawns and infestation-selected pests. Disabled entities remain registered and usable by spawn eggs, commands, or custom scripts.");
        builder.push("entity_spawns");
        ENTITY_SPAWN_BISON = builder.define("bison", true);
        ENTITY_SPAWN_LEOPARD_SEAL = builder.define("leopard_seal", true);
        ENTITY_SPAWN_LEMMING = builder.define("lemming", true);
        ENTITY_SPAWN_MONGOOSE = builder.define("mongoose", true);
        ENTITY_SPAWN_JERBOA = builder.define("jerboa", true);
        builder.pop();

        builder.comment("Controls the 1.21-style runtime snow catch-up backend. Vanilla 1.20 rain/thunder scheduling stays unchanged.");
        builder.push("weather_runtime");
        SNOW_MAX_ACCUMULATION_ON_UPDATE = builder
            .comment("Maximum snow/ice/icicle updates applied when re-entering an unloaded chunk. Lower values match nearby loaded chunks more closely.")
            .defineInRange("snow_max_accumulation_on_update", 64, 0, 256);
        TICKS_PER_SNOW_ACCUMULATION = builder
            .comment("Game ticks between snow accumulation attempts. Lower values accumulate snow faster, but cost more runtime work.")
            .defineInRange("ticks_per_snow_accumulation", 80, 1, Integer.MAX_VALUE);
        SNOW_MELT_MULTIPLIER = builder
            .comment("Multiplier applied to the accumulation interval when scheduling melt attempts. Default matches 1.21 TFC: 3.")
            .defineInRange("snow_melt_multiplier", 3, 1, Integer.MAX_VALUE);
        builder.pop();

        builder.comment("Controls whether crops and fruiting plants use current seasonal rainfall or legacy annual-average hydration.");
        builder.push("plant_hydration");
        CROP_USE_CURRENT_RAINFALL_HYDRATION = builder
            .comment("When true, crop growth and crop tooltips use the current seasonal rainfall-derived hydration.")
            .define("crop_use_current_rainfall_hydration", true);
        FRUIT_USE_CURRENT_RAINFALL_HYDRATION = builder
            .comment("When true, fruit trees, berry bushes, and banana plants use current seasonal rainfall-derived hydration.")
            .define("fruit_use_current_rainfall_hydration", true);
        builder.pop();

        SPEC = builder.build();
    }

    private NTECommonConfig()
    {
    }

    public static boolean isWildCropEnabled(NTECrop crop)
    {
        return switch (crop)
        {
            case ALFALFA -> WILD_CROP_ALFALFA.get();
            case CANOLA -> WILD_CROP_CANOLA.get();
            case CASSAVA -> WILD_CROP_CASSAVA.get();
            case LENTIL -> WILD_CROP_LENTIL.get();
            case PEANUT -> WILD_CROP_PEANUT.get();
            case RADISH -> WILD_CROP_RADISH.get();
        };
    }

    public static boolean isWildCropEnabled(ResourceLocation blockId)
    {
        if (blockId == null || !"tfe".equals(blockId.getNamespace()))
        {
            return true;
        }

        final String path = blockId.getPath();
        if (!path.startsWith("wild_crop/"))
        {
            return true;
        }

        final NTECrop crop = NTECrop.byName(path.substring("wild_crop/".length()));
        return crop == null || isWildCropEnabled(crop);
    }

    public static boolean isFruitTreeEnabled(String treeName)
    {
        return switch (treeName)
        {
            case "cherry" -> FRUIT_TREE_CHERRY.get();
            case "green_apple" -> FRUIT_TREE_GREEN_APPLE.get();
            case "lemon" -> FRUIT_TREE_LEMON.get();
            case "olive" -> FRUIT_TREE_OLIVE.get();
            case "orange" -> FRUIT_TREE_ORANGE.get();
            case "peach" -> FRUIT_TREE_PEACH.get();
            case "plum" -> FRUIT_TREE_PLUM.get();
            case "red_apple" -> FRUIT_TREE_RED_APPLE.get();
            default -> true;
        };
    }

    public static boolean isFruitTreeEnabled(ResourceLocation blockId)
    {
        if (blockId == null || !"tfc".equals(blockId.getNamespace()))
        {
            return true;
        }

        final String path = blockId.getPath();
        if (!path.startsWith("plant/") || !path.endsWith("_growing_branch"))
        {
            return true;
        }

        return isFruitTreeEnabled(path.substring("plant/".length(), path.length() - "_growing_branch".length()));
    }

    public static boolean isBambooEnabled(String bambooName)
    {
        return switch (bambooName)
        {
            case "bamboo" -> BAMBOO.get();
            case "golden_bamboo" -> GOLDEN_BAMBOO.get();
            default -> true;
        };
    }

    public static boolean isBambooEnabled(ResourceLocation blockId)
    {
        if (blockId == null)
        {
            return true;
        }

        if ("minecraft".equals(blockId.getNamespace()) && "bamboo".equals(blockId.getPath()))
        {
            return BAMBOO.get();
        }

        if ("tfe".equals(blockId.getNamespace()) && "plant/golden_bamboo".equals(blockId.getPath()))
        {
            return GOLDEN_BAMBOO.get();
        }

        return true;
    }

    public static boolean isBambooEnabledByBlock(net.minecraft.world.level.block.Block block)
    {
        return isBambooEnabled(ForgeRegistries.BLOCKS.getKey(block));
    }

    public static boolean isPlacedPlantFeatureEnabled(ResourceLocation featureId)
    {
        if (featureId == null || !"tfc".equals(featureId.getNamespace()))
        {
            return true;
        }

        final String path = featureId.getPath();
        if ("bamboo".equals(path))
        {
            return BAMBOO.get();
        }
        if ("bamboo_golden".equals(path))
        {
            return GOLDEN_BAMBOO.get();
        }
        if (!path.startsWith("plant/"))
        {
            return true;
        }

        final String plantPath = path.substring("plant/".length());
        if (plantPath.startsWith("wild_crop/"))
        {
            String cropName = plantPath.substring("wild_crop/".length());
            if (cropName.endsWith("_patch"))
            {
                cropName = cropName.substring(0, cropName.length() - "_patch".length());
            }
            final NTECrop crop = NTECrop.byName(cropName);
            return crop == null || isWildCropEnabled(crop);
        }
        return isFruitTreeEnabled(plantPath);
    }

    public static boolean hasDisabledConfiguredPlantFeatures()
    {
        return !WILD_CROP_ALFALFA.get()
            || !WILD_CROP_CANOLA.get()
            || !WILD_CROP_CASSAVA.get()
            || !WILD_CROP_LENTIL.get()
            || !WILD_CROP_PEANUT.get()
            || !WILD_CROP_RADISH.get()
            || !FRUIT_TREE_CHERRY.get()
            || !FRUIT_TREE_GREEN_APPLE.get()
            || !FRUIT_TREE_LEMON.get()
            || !FRUIT_TREE_OLIVE.get()
            || !FRUIT_TREE_ORANGE.get()
            || !FRUIT_TREE_PEACH.get()
            || !FRUIT_TREE_PLUM.get()
            || !FRUIT_TREE_RED_APPLE.get()
            || !BAMBOO.get()
            || !GOLDEN_BAMBOO.get();
    }

    public static boolean isEntitySpawnEnabled(ResourceLocation entityId)
    {
        if (entityId == null || !"tfc".equals(entityId.getNamespace()))
        {
            return true;
        }

        return switch (entityId.getPath())
        {
            case "bison" -> ENTITY_SPAWN_BISON.get();
            case "leopard_seal" -> ENTITY_SPAWN_LEOPARD_SEAL.get();
            case "lemming" -> ENTITY_SPAWN_LEMMING.get();
            case "mongoose" -> ENTITY_SPAWN_MONGOOSE.get();
            case "jerboa" -> ENTITY_SPAWN_JERBOA.get();
            default -> true;
        };
    }

    public static boolean hasDisabledConfiguredEntitySpawns()
    {
        return !ENTITY_SPAWN_BISON.get()
            || !ENTITY_SPAWN_LEOPARD_SEAL.get()
            || !ENTITY_SPAWN_LEMMING.get()
            || !ENTITY_SPAWN_MONGOOSE.get()
            || !ENTITY_SPAWN_JERBOA.get();
    }

    public static int getConfiguredEntitySpawnMask()
    {
        int mask = 0;
        if (ENTITY_SPAWN_BISON.get())
        {
            mask |= 1;
        }
        if (ENTITY_SPAWN_LEOPARD_SEAL.get())
        {
            mask |= 1 << 1;
        }
        if (ENTITY_SPAWN_LEMMING.get())
        {
            mask |= 1 << 2;
        }
        if (ENTITY_SPAWN_MONGOOSE.get())
        {
            mask |= 1 << 3;
        }
        if (ENTITY_SPAWN_JERBOA.get())
        {
            mask |= 1 << 4;
        }
        return mask;
    }

    public static int getSnowMaxAccumulationOnUpdate()
    {
        return SNOW_MAX_ACCUMULATION_ON_UPDATE.get();
    }

    public static int getTicksPerSnowAccumulation()
    {
        return TICKS_PER_SNOW_ACCUMULATION.get();
    }

    public static int getSnowMeltMultiplier()
    {
        return SNOW_MELT_MULTIPLIER.get();
    }

    public static boolean useCurrentRainfallForCrops()
    {
        return CROP_USE_CURRENT_RAINFALL_HYDRATION.get();
    }

    public static boolean useCurrentRainfallForFruit()
    {
        return FRUIT_USE_CURRENT_RAINFALL_HYDRATION.get();
    }
}
