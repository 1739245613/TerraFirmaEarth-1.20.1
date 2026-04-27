package com.newterraearth.tfe.client.model;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.IDynamicBakedModel;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;

import net.dries007.tfc.client.ClientHelpers;
import net.dries007.tfc.client.RenderHelpers;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.calendar.ICalendar;

import com.newterraearth.tfe.client.NTEClimateRenderHelpers;

public class NTEPlantBlockModel implements IDynamicBakedModel, IUnbakedGeometry<NTEPlantBlockModel>
{
    private static final float DEFAULT_BLOOM_OFFSET = 0.4f;
    private static final float DEFAULT_BLOOMING_END = 0.6f;
    private static final float DEFAULT_SEEDING_END = 0.75f;
    private static final float DEFAULT_DYING_END = 0.9f;
    private static final float DEFAULT_DORMANT_END = 1.1f;
    private static final float DEFAULT_SPROUTING_END = 1.25f;
    private static final long POSITION_SALT = 836494186029734123L;
    private static final int MAX_POSITION_CACHE_SIZE = 16_384;
    private static final Map<Long, PositionClimateData> POSITION_CACHE = new ConcurrentHashMap<>();

    @Nullable private static volatile Level cachedLevel;

    private final BlockModel dormant;
    private final BlockModel sprouting;
    private final BlockModel budding;
    private final BlockModel blooming;
    private final BlockModel seeding;
    private final BlockModel dying;
    private final SeasonalConfig seasonalConfig;

    @Nullable private BakedModel dormantBakedModel;
    @Nullable private BakedModel sproutingBakedModel;
    @Nullable private BakedModel buddingBakedModel;
    @Nullable private BakedModel bloomingBakedModel;
    @Nullable private BakedModel seedingBakedModel;
    @Nullable private BakedModel dyingBakedModel;

    public NTEPlantBlockModel(
        BlockModel dormant,
        BlockModel sprouting,
        BlockModel budding,
        BlockModel blooming,
        BlockModel seeding,
        BlockModel dying,
        SeasonalConfig seasonalConfig
    ) {
        this.dormant = dormant;
        this.sprouting = sprouting;
        this.budding = budding;
        this.blooming = blooming;
        this.seeding = seeding;
        this.dying = dying;
        this.seasonalConfig = seasonalConfig;
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data)
    {
        return data.derive().with(BakedModelData.PROPERTY, new BakedModelData(getModelForBlockPos(pos))).build();
    }

    private BakedModel getModelForBlockPos(@Nullable BlockPos pos)
    {
        if (pos == null)
        {
            return getFallbackModel();
        }

        final Level level = ClientHelpers.getLevel();
        if (level == null)
        {
            return getFallbackModel();
        }

        final PositionClimateData positionData = getPositionClimateData(level, pos);
        final float start = getSeasonStart(positionData);
        final float endBlooming = start + seasonalConfig.bloomingEnd();
        final float endSeeding = start + seasonalConfig.seedingEnd();
        final float endDying = start + seasonalConfig.dyingEnd();
        final float endDormant = start + seasonalConfig.dormantEnd();
        final float endSprouting = start + seasonalConfig.sproutingEnd();
        final float randomScale = positionData != null ? positionData.randomScale() : 0.03f;
        return getModelFromCalendar(
            start,
            endBlooming,
            endSeeding,
            endDying,
            endDormant,
            endSprouting,
            seasonalConfig.startTime(),
            seasonalConfig.endTime(),
            randomScale > 0.25f
        );
    }

    private float getSeasonStart(@Nullable PositionClimateData positionData)
    {
        float start = seasonalConfig.bloomOffset();
        if (positionData == null)
        {
            return normalizeSeason(start);
        }

        start += positionData.seasonOffset();
        start += Mth.lerp(positionData.randomUnit(), -positionData.randomScale(), positionData.randomScale());
        return normalizeSeason(start);
    }

    @Nullable
    private PositionClimateData getPositionClimateData(Level level, BlockPos pos)
    {
        preparePositionCache(level);

        if (seasonalConfig.wetSeasonBlooming())
        {
            return computeWetSeasonPositionClimateData(level, pos);
        }

        final long key = BlockPos.asLong(pos.getX(), 0, pos.getZ());
        final PositionClimateData cached = POSITION_CACHE.get(key);
        if (cached != null)
        {
            return cached;
        }

        final PositionClimateData computed = computeTemperatePositionClimateData(level, pos);
        final PositionClimateData present = POSITION_CACHE.putIfAbsent(key, computed);
        if (POSITION_CACHE.size() > MAX_POSITION_CACHE_SIZE)
        {
            POSITION_CACHE.clear();
        }
        return present != null ? present : computed;
    }

    private static void preparePositionCache(Level level)
    {
        if (cachedLevel != level)
        {
            synchronized (NTEPlantBlockModel.class)
            {
                if (cachedLevel != level)
                {
                    cachedLevel = level;
                    POSITION_CACHE.clear();
                }
            }
        }
    }

    @Nullable
    private static PositionClimateData computeWetSeasonPositionClimateData(Level level, BlockPos pos)
    {
        final float rainVariance = NTEClimateRenderHelpers.getRainVarianceOrNaN(level, pos);
        if (Float.isNaN(rainVariance))
        {
            return null;
        }

        final Random random = new Random(Helpers.hash(POSITION_SALT, pos.getX(), 0, pos.getZ()));
        final float randomScale = Mth.clampedMap(Math.abs(rainVariance), 0f, 0.3f, 0.5f, 0.03f);
        final float seasonOffset = rainVariance < 0f ? 1f : 1.5f;
        return new PositionClimateData(randomScale, random.nextFloat(), seasonOffset);
    }

    private static PositionClimateData computeTemperatePositionClimateData(Level level, BlockPos pos)
    {
        final Random random = new Random(Helpers.hash(POSITION_SALT, pos.getX(), 0, pos.getZ()));
        final float randomScale = Mth.clampedMap(NTEClimateRenderHelpers.getAdjustedAverageTemperature(level, pos), 16f, 26f, 0.03f, 0.5f);
        final float seasonOffset = NTEClimateRenderHelpers.isNorthernHemisphere(level, pos) ? 1.5f : 1f;
        return new PositionClimateData(randomScale, random.nextFloat(), seasonOffset);
    }

    private BakedModel getFallbackModel()
    {
        return getModelFromCalendar(
            DEFAULT_BLOOM_OFFSET,
            DEFAULT_BLOOMING_END,
            DEFAULT_SEEDING_END,
            DEFAULT_DYING_END,
            DEFAULT_DORMANT_END,
            DEFAULT_SPROUTING_END,
            0,
            0,
            false
        );
    }

    private BakedModel getModelFromCalendar(
        float bloomingStart,
        float bloomingEnd,
        float seedingEnd,
        float dyingEnd,
        float dormantEnd,
        float sproutingEnd,
        int startTime,
        int endTime,
        boolean nonDormant
    ) {
        final float timeOfYear = Calendars.CLIENT.getCalendarFractionOfYear();
        final float adjustedTimeOfYear = timeOfYear < bloomingStart ? timeOfYear + 1f : timeOfYear;

        if (adjustedTimeOfYear < bloomingEnd)
        {
            if (startTime != endTime)
            {
                return getModelByDayTime(startTime, endTime);
            }
            return requireModel(bloomingBakedModel);
        }
        if (adjustedTimeOfYear < seedingEnd)
        {
            return requireModel(seedingBakedModel);
        }
        if (adjustedTimeOfYear < dyingEnd)
        {
            return nonDormant ? requireModel(buddingBakedModel) : requireModel(dyingBakedModel);
        }
        if (adjustedTimeOfYear < dormantEnd)
        {
            if (nonDormant)
            {
                if (startTime != endTime)
                {
                    return getModelByDayTime(startTime, endTime);
                }
                return requireModel(bloomingBakedModel);
            }
            return requireModel(dormantBakedModel);
        }
        if (adjustedTimeOfYear < sproutingEnd)
        {
            return nonDormant ? requireModel(seedingBakedModel) : requireModel(sproutingBakedModel);
        }
        return requireModel(buddingBakedModel);
    }

    private BakedModel getModelByDayTime(int startTime, int endTime)
    {
        final Level level = ClientHelpers.getLevel();
        if (level != null)
        {
            final long dayTime = level.getDayTime() % ICalendar.TICKS_IN_DAY;
            if ((endTime < dayTime && dayTime < startTime) || (startTime < endTime && (dayTime < startTime || endTime < dayTime)))
            {
                return requireModel(buddingBakedModel);
            }
        }
        return requireModel(bloomingBakedModel);
    }

    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> atlas, ModelState modelState, ItemOverrides overrides, ResourceLocation modelLocation)
    {
        dormantBakedModel = dormant.bake(baker, dormant, atlas, modelState, modelLocation, false);
        sproutingBakedModel = sprouting.bake(baker, sprouting, atlas, modelState, modelLocation, false);
        buddingBakedModel = budding.bake(baker, budding, atlas, modelState, modelLocation, false);
        bloomingBakedModel = blooming.bake(baker, blooming, atlas, modelState, modelLocation, false);
        seedingBakedModel = seeding.bake(baker, seeding, atlas, modelState, modelLocation, false);
        dyingBakedModel = dying.bake(baker, dying, atlas, modelState, modelLocation, false);
        return this;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random, ModelData modelData, @Nullable RenderType renderType)
    {
        final BakedModelData bakedData = modelData.get(BakedModelData.PROPERTY);
        if (bakedData != null)
        {
            return bakedData.toRender().getQuads(state, direction, random, modelData, renderType);
        }
        return getFallbackModel().getQuads(state, direction, random, modelData, renderType);
    }

    @Override
    public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context)
    {
        dormant.resolveParents(modelGetter);
        sprouting.resolveParents(modelGetter);
        budding.resolveParents(modelGetter);
        blooming.resolveParents(modelGetter);
        seeding.resolveParents(modelGetter);
        dying.resolveParents(modelGetter);
    }

    @Override
    public boolean useAmbientOcclusion()
    {
        return true;
    }

    @Override
    public boolean isGui3d()
    {
        return false;
    }

    @Override
    public boolean usesBlockLight()
    {
        return true;
    }

    @Override
    public boolean isCustomRenderer()
    {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public TextureAtlasSprite getParticleIcon()
    {
        return bloomingBakedModel != null ? bloomingBakedModel.getParticleIcon() : RenderHelpers.missingTexture();
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data)
    {
        final BakedModelData bakedData = data.get(BakedModelData.PROPERTY);
        return bakedData != null ? bakedData.toRender().getParticleIcon(data) : RenderHelpers.missingTexture();
    }

    @Override
    public ItemOverrides getOverrides()
    {
        return ItemOverrides.EMPTY;
    }

    private static float normalizeSeason(float value)
    {
        float normalized = value % 1f;
        return normalized < 0f ? normalized + 1f : normalized;
    }

    private static BakedModel requireModel(@Nullable BakedModel model)
    {
        if (model == null)
        {
            throw new IllegalStateException("Plant model was requested before it finished baking");
        }
        return model;
    }

    record BakedModelData(BakedModel toRender)
    {
        public static final ModelProperty<BakedModelData> PROPERTY = new ModelProperty<>();
    }

    record PositionClimateData(float randomScale, float randomUnit, float seasonOffset) {}

    record SeasonalConfig(
        boolean wetSeasonBlooming,
        float bloomOffset,
        float bloomingEnd,
        float seedingEnd,
        float dyingEnd,
        float dormantEnd,
        float sproutingEnd,
        int startTime,
        int endTime
    ) {}

    public static class Loader implements IGeometryLoader<NTEPlantBlockModel>
    {
        private static final SeasonalConfig DEFAULT_PLANT = new SeasonalConfig(false, -0.124f, 0.25f, 0.42f, 0.5f, 0.75f, 0.88f, 0, 0);
        private static final SeasonalConfig AZALEA = new SeasonalConfig(false, -0.25f, 0.2f, 0.35f, 0.5f, 0.75f, 0.9f, 0, 0);
        private static final SeasonalConfig BEAR_GRASS = new SeasonalConfig(false, 0.05f, 0.25f, 0.35f, 0.4f, 0.7f, 0.8f, 0, 0);
        private static final SeasonalConfig BUTTERCUP = new SeasonalConfig(false, -0.2f, 0.35f, 0.45f, 0.55f, 0.65f, 0.9f, 0, 0);
        private static final SeasonalConfig CORNFLOWER = new SeasonalConfig(false, -0.1f, 0.28f, 0.46f, 0.57f, 0.71f, 0.93f, 0, 0);
        private static final SeasonalConfig CORDGRASS = new SeasonalConfig(false, 0f, 0.27f, 0.39f, 0.5f, 0.666f, 0.92f, 0, 0);
        private static final SeasonalConfig EDELWEISS = new SeasonalConfig(false, -0.09f, 0.2f, 0.35f, 0.5f, 0.75f, 0.9f, 0, 0);
        private static final SeasonalConfig FLAME_VINE = new SeasonalConfig(true, 0.49f, 0.4f, 0.5f, 0.6f, 0.8f, 0.9f, 0, 0);
        private static final SeasonalConfig KINNIKINNICK = new SeasonalConfig(false, -0.11f, 0.065f, 0.14f, 0.39f, 0.85f, 0.95f, 0, 0);
        private static final SeasonalConfig LOTUS = new SeasonalConfig(true, -0.12f, 0.25f, 0.35f, 0.5f, 0.75f, 0.92f, 1000, 13000);
        private static final SeasonalConfig MOSS_CAMPION = new SeasonalConfig(false, 0.05f, 0.15f, 0.25f, 0.45f, 0.85f, 0.92f, 0, 0);
        private static final SeasonalConfig PALASH = new SeasonalConfig(true, 0.55f, 0.2f, 0.35f, 0.7f, 0.8f, 0.9f, 0, 0);
        private static final SeasonalConfig PENWORTEL = new SeasonalConfig(true, 0.3f, 0.25f, 0.35f, 0.45f, 0.6f, 0.8f, 0, 0);
        private static final SeasonalConfig PRICKLY_PEAR = new SeasonalConfig(true, -0.13f, 0.16f, 0.32f, 0.5f, 0.75f, 0.92f, 0, 0);
        private static final SeasonalConfig QANTU = new SeasonalConfig(false, -0.33f, 0.25f, 0.35f, 0.5f, 0.65f, 0.9f, 0, 0);
        private static final SeasonalConfig RAMUNDA = new SeasonalConfig(false, -0.12f, 0.22f, 0.3f, 0.45f, 0.7f, 0.9f, 0, 0);
        private static final SeasonalConfig RED_OAT_GRASS = new SeasonalConfig(true, -0.1f, 0.33f, 0.45f, 0.55f, 0.75f, 0.85f, 0, 0);
        private static final SeasonalConfig SEA_LAVENDER = new SeasonalConfig(false, 0.08f, 0.18f, 0.31f, 0.4f, 0.7f, 0.91f, 0, 0);
        private static final SeasonalConfig SHAWIASH = new SeasonalConfig(false, -0.09f, 0.07f, 0.14f, 0.4f, 0.85f, 0.95f, 0, 0);
        private static final SeasonalConfig SILKEN_PINCUSHION_CACTUS = new SeasonalConfig(true, -0.2f, 0.25f, 0.4f, 0.6f, 0.75f, 0.9f, 0, 0);
        private static final SeasonalConfig SUNFLOWER = new SeasonalConfig(false, 0.12f, 0.12f, 0.21f, 0.34f, 0.63f, 0.88f, 0, 0);
        private static final SeasonalConfig WHITE_WATER_LILY = new SeasonalConfig(false, -0.11f, 0.33f, 0.42f, 0.5f, 0.8f, 0.92f, 0, 0);
        private static final SeasonalConfig PURPLE_WATER_LILY = new SeasonalConfig(false, -0.05f, 0.32f, 0.41f, 0.5f, 0.8f, 0.92f, 0, 0);
        private static final SeasonalConfig YELLOW_WATER_LILY = new SeasonalConfig(false, -0.083f, 0.32f, 0.41f, 0.5f, 0.8f, 0.92f, 0, 0);
        private static final SeasonalConfig YELLOW_SAXIFRAGE = new SeasonalConfig(false, -0.12f, 0.31f, 0.4f, 0.5f, 0.65f, 0.9f, 0, 0);

        public static final Loader INSTANCE = new Loader();

        private Loader()
        {
        }

        @Override
        public NTEPlantBlockModel read(JsonObject json, JsonDeserializationContext context) throws JsonParseException
        {
            return new NTEPlantBlockModel(
                context.deserialize(json.get("dormant"), BlockModel.class),
                context.deserialize(json.get("sprouting"), BlockModel.class),
                context.deserialize(json.get("budding"), BlockModel.class),
                context.deserialize(json.get("blooming"), BlockModel.class),
                context.deserialize(json.get("seeding"), BlockModel.class),
                context.deserialize(json.get("dying"), BlockModel.class),
                readSeasonalConfig(json)
            );
        }

        private SeasonalConfig readSeasonalConfig(JsonObject json)
        {
            if (json.has("bloom_offset"))
            {
                return new SeasonalConfig(
                    GsonHelper.getAsBoolean(json, "wet_season_blooming", false),
                    GsonHelper.getAsFloat(json, "bloom_offset"),
                    GsonHelper.getAsFloat(json, "blooming_end"),
                    GsonHelper.getAsFloat(json, "seeding_end"),
                    GsonHelper.getAsFloat(json, "dying_end"),
                    GsonHelper.getAsFloat(json, "dormant_end"),
                    GsonHelper.getAsFloat(json, "sprouting_end"),
                    GsonHelper.getAsInt(json, "start_time", 0),
                    GsonHelper.getAsInt(json, "end_time", 0)
                );
            }

            final String parent = GsonHelper.getAsString(GsonHelper.getAsJsonObject(json, "blooming"), "parent");
            final String modelName = parent.substring(parent.lastIndexOf('/') + 1);
            final String plantName = modelName.endsWith("_0") || modelName.endsWith("_1") ? modelName.substring(0, modelName.length() - 2) : modelName;

            return seasonalConfigForModel(plantName);
        }

        private SeasonalConfig seasonalConfigForModel(String modelName)
        {
            if (modelName.startsWith("azalea"))
            {
                return AZALEA;
            }
            if (modelName.startsWith("bear_grass"))
            {
                return BEAR_GRASS;
            }
            if (modelName.startsWith("buttercup"))
            {
                return BUTTERCUP;
            }
            if (modelName.startsWith("cornflower"))
            {
                return CORNFLOWER;
            }
            if (modelName.startsWith("cordgrass"))
            {
                return CORDGRASS;
            }
            if (modelName.startsWith("edelweiss"))
            {
                return EDELWEISS;
            }
            if (modelName.startsWith("flame_vine"))
            {
                return FLAME_VINE;
            }
            if (modelName.startsWith("kinnikinnick"))
            {
                return KINNIKINNICK;
            }
            if (modelName.startsWith("lotus"))
            {
                return LOTUS;
            }
            if (modelName.startsWith("moss_campion"))
            {
                return MOSS_CAMPION;
            }
            if (modelName.startsWith("palash"))
            {
                return PALASH;
            }
            if (modelName.startsWith("penwortel"))
            {
                return PENWORTEL;
            }
            if (modelName.startsWith("prickly_pear") || modelName.startsWith("purple_prickly_pear"))
            {
                return PRICKLY_PEAR;
            }
            if (modelName.startsWith("qantu"))
            {
                return QANTU;
            }
            if (modelName.startsWith("ramunda"))
            {
                return RAMUNDA;
            }
            if (modelName.startsWith("red_oat_grass"))
            {
                return RED_OAT_GRASS;
            }
            if (modelName.startsWith("sea_lavender"))
            {
                return SEA_LAVENDER;
            }
            if (modelName.startsWith("shawiash"))
            {
                return SHAWIASH;
            }
            if (modelName.startsWith("silken_pincushion_cactus"))
            {
                return SILKEN_PINCUSHION_CACTUS;
            }
            if (modelName.startsWith("sunflower"))
            {
                return SUNFLOWER;
            }
            if (modelName.startsWith("water_lily_0_white"))
            {
                return WHITE_WATER_LILY;
            }
            if (modelName.startsWith("water_lily_0_purple"))
            {
                return PURPLE_WATER_LILY;
            }
            if (modelName.startsWith("water_lily_0_yellow"))
            {
                return YELLOW_WATER_LILY;
            }
            if (modelName.startsWith("yellow_saxifrage"))
            {
                return YELLOW_SAXIFRAGE;
            }
            return DEFAULT_PLANT;
        }
    }
}
