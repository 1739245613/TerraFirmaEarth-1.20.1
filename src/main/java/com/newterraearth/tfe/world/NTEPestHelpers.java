package com.newterraearth.tfe.world;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.world.chunkdata.ChunkData;

import com.newterraearth.tfe.config.NTECommonConfig;

public final class NTEPestHelpers
{
    private static final TagKey<EntityType<?>> UNIVERSAL_PESTS = createTag("universal_pests");
    private static final TagKey<EntityType<?>> COLD_PESTS = createTag("cold_pests");
    private static final TagKey<EntityType<?>> DESERT_PESTS = createTag("desert_pests");
    private static final TagKey<EntityType<?>> TROPICAL_PESTS = createTag("tropical_pests");

    private NTEPestHelpers()
    {
    }

    public static Optional<EntityType<?>> choosePest(Level level, BlockPos pos)
    {
        final ChunkData data = ChunkData.get(level, pos);
        if (data == ChunkData.EMPTY)
        {
            return pickWithFallback(level.random, UNIVERSAL_PESTS, TFCTags.Entities.PESTS);
        }

        final float temperature = data.getAverageTemp(pos);
        if (temperature <= -12f)
        {
            return Optional.empty();
        }
        if (temperature < -3f)
        {
            return pickWithFallback(level.random, COLD_PESTS, UNIVERSAL_PESTS, TFCTags.Entities.PESTS);
        }
        if (level.random.nextFloat() <= 0.7f)
        {
            final float rainfall = data.getRainfall(pos);
            if (rainfall < 160f)
            {
                return pickWithFallback(level.random, DESERT_PESTS, UNIVERSAL_PESTS, TFCTags.Entities.PESTS);
            }
            if (temperature > 12f)
            {
                return pickWithFallback(level.random, TROPICAL_PESTS, UNIVERSAL_PESTS, TFCTags.Entities.PESTS);
            }
        }
        return pickWithFallback(level.random, UNIVERSAL_PESTS, TFCTags.Entities.PESTS);
    }

    @SafeVarargs
    private static Optional<EntityType<?>> pickWithFallback(RandomSource random, TagKey<EntityType<?>>... tags)
    {
        for (TagKey<EntityType<?>> tag : tags)
        {
            final Optional<EntityType<?>> candidate = randomEnabledEntity(tag, random);
            if (candidate.isPresent())
            {
                return candidate;
            }
        }
        return Optional.empty();
    }

    private static Optional<EntityType<?>> randomEnabledEntity(TagKey<EntityType<?>> tag, RandomSource random)
    {
        final List<EntityType<?>> candidates = BuiltInRegistries.ENTITY_TYPE.getTag(tag)
            .stream()
            .flatMap(named -> named.stream())
            .map(Holder::value)
            .filter(NTEPestHelpers::isEnabled)
            .toList();
        if (candidates.isEmpty())
        {
            return Optional.empty();
        }
        return Optional.of(candidates.get(random.nextInt(candidates.size())));
    }

    private static boolean isEnabled(EntityType<?> entityType)
    {
        return NTECommonConfig.isEntitySpawnEnabled(BuiltInRegistries.ENTITY_TYPE.getKey(entityType));
    }

    private static TagKey<EntityType<?>> createTag(String path)
    {
        return TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("tfc", path));
    }
}
