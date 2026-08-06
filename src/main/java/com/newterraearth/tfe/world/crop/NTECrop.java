package com.newterraearth.tfe.world.crop;

import java.util.Locale;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;

import net.dries007.tfc.common.blockentities.FarmlandBlockEntity;
import net.dries007.tfc.util.climate.ClimateRange;

import com.newterraearth.tfe.NewTerraEarthMod;

public enum NTECrop
{
    ALFALFA(FarmlandBlockEntity.NutrientType.NITROGEN, false, false),
    CANOLA(FarmlandBlockEntity.NutrientType.PHOSPHOROUS, true, false, "canola"),
    CASSAVA(FarmlandBlockEntity.NutrientType.POTASSIUM, true, true),
    LENTIL(FarmlandBlockEntity.NutrientType.NITROGEN, true, true),
    PEANUT(FarmlandBlockEntity.NutrientType.NITROGEN, true, false),
    RADISH(FarmlandBlockEntity.NutrientType.POTASSIUM, true, false);

    private final String serializedName;
    private final FarmlandBlockEntity.NutrientType primaryNutrient;
    private final boolean foodItem;
    private final boolean cookedItem;
    @Nullable private final String producePathOverride;
    private final Supplier<ClimateRange> climateRange;

    NTECrop(FarmlandBlockEntity.NutrientType primaryNutrient, boolean foodItem, boolean cookedItem)
    {
        this(primaryNutrient, foodItem, cookedItem, null);
    }

    NTECrop(FarmlandBlockEntity.NutrientType primaryNutrient, boolean foodItem, boolean cookedItem, @Nullable String producePathOverride)
    {
        this.serializedName = name().toLowerCase(Locale.ROOT);
        this.primaryNutrient = primaryNutrient;
        this.foodItem = foodItem;
        this.cookedItem = cookedItem;
        this.producePathOverride = producePathOverride;
        this.climateRange = ClimateRange.MANAGER.register(new ResourceLocation(NewTerraEarthMod.MOD_ID, "crop/" + serializedName));
    }

    public String serializedName()
    {
        return serializedName;
    }

    public String cropPath()
    {
        return "crop/" + serializedName;
    }

    public String deadCropPath()
    {
        return "dead_crop/" + serializedName;
    }

    public String wildCropPath()
    {
        return "wild_crop/" + serializedName;
    }

    public String seedPath()
    {
        return "seeds/" + serializedName;
    }

    public String producePath()
    {
        return producePathOverride != null ? producePathOverride : foodItem ? "food/" + serializedName : serializedName;
    }

    public boolean isFoodItem()
    {
        return foodItem;
    }

    public boolean hasCookedItem()
    {
        return cookedItem;
    }

    public String cookedProducePath()
    {
        if (!cookedItem)
        {
            throw new IllegalStateException("Crop does not have a cooked item: " + serializedName);
        }
        return "food/cooked_" + serializedName;
    }

    public FarmlandBlockEntity.NutrientType primaryNutrient()
    {
        return primaryNutrient;
    }

    public Supplier<ClimateRange> climateRange()
    {
        return climateRange;
    }

    @Nullable
    public static NTECrop byName(String name)
    {
        for (NTECrop crop : values())
        {
            if (crop.serializedName.equals(name))
            {
                return crop;
            }
        }
        return null;
    }
}
