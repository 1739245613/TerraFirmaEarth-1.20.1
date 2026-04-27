package com.newterraearth.tfe.world.feature;

import java.util.ArrayList;
import java.util.List;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.world.ChunkGeneratorExtension;
import net.dries007.tfc.world.chunkdata.ChunkDataProvider;
import net.dries007.tfc.world.chunkdata.RockData;
import net.dries007.tfc.world.noise.Metaballs2D;
import net.dries007.tfc.world.settings.RockLayerSettings;
import net.dries007.tfc.world.settings.RockSettings;

public class SeaStacksFeature extends Feature<NoneFeatureConfiguration>
{
    public SeaStacksFeature(Codec<NoneFeatureConfiguration> codec)
    {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context)
    {
        final WorldGenLevel level = context.level();
        final BlockPos pos = context.origin();
        final RandomSource random = context.random();
        final int seaLevel = context.chunkGenerator().getSeaLevel();

        if (level.getFluidState(pos).isEmpty() || pos.getY() > seaLevel)
        {
            return false;
        }

        final BlockState rock = resolveRock(level, pos, seaLevel, context);
        if (rock == null)
        {
            return false;
        }

        final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos().set(pos);
        final int upOffset = Mth.nextInt(random, 2, 6);
        cursor.move(0, upOffset, 0);

        final int radius = Mth.nextInt(random, 3, 6);
        placeStack(level, cursor.immutable(), cursor, rock, radius, Mth.nextInt(random, 5, 15), random, false);
        cursor.set(pos).move(0, upOffset - 1, 0);
        placeStack(level, cursor.immutable(), cursor, rock, Math.max(1, radius / 2), upOffset, random, true);
        return true;
    }

    private void placeStack(WorldGenLevel level, BlockPos origin, BlockPos.MutableBlockPos cursor, BlockState state, int radius, int height, RandomSource random, boolean inverted)
    {
        final List<Long> acceptedPositions = new ArrayList<>();
        if (inverted)
        {
            for (int y = -height; y <= 0; y++)
            {
                placeStackLayer(level, origin, cursor, state, radius, height, random, y, acceptedPositions);
            }
        }
        else
        {
            for (int y = height - 1; y >= 0; y--)
            {
                placeStackLayer(level, origin, cursor, state, radius, height, random, y, acceptedPositions);
            }
        }
    }

    private void placeStackLayer(WorldGenLevel level, BlockPos origin, BlockPos.MutableBlockPos cursor, BlockState state, int radius, int height, RandomSource random, int y, List<Long> acceptedPositions)
    {
        final int actualRadius = Math.max(1, (int) (radius * Mth.abs(height - y) / (float) height));
        final Metaballs2D noise = Metaballs2D.simple(Helpers.fork(random), actualRadius);
        for (int x = origin.getX() - radius; x <= origin.getX() + radius; x++)
        {
            for (int z = origin.getZ() - radius; z <= origin.getZ() + radius; z++)
            {
                final int relX = x - origin.getX();
                final int relZ = z - origin.getZ();
                cursor.set(x, 0, z);
                final long code = cursor.asLong();
                final boolean accepted = acceptedPositions.contains(code);
                cursor.setY(origin.getY() + y);

                if ((noise.inside(relX, relZ) || accepted) && level.getBlockState(cursor).canBeReplaced())
                {
                    setBlock(level, cursor, state);
                    if (!accepted)
                    {
                        acceptedPositions.add(code);
                    }
                    if (actualRadius == 1 && random.nextFloat() < 0.4f)
                    {
                        return;
                    }
                }
            }
        }
    }

    @Nullable
    private BlockState resolveRock(WorldGenLevel level, BlockPos pos, int seaLevel, FeaturePlaceContext<NoneFeatureConfiguration> context)
    {
        try
        {
            // In 1.20 worldgen, ChunkData must be accessed through the provider rather than ChunkData.get(level, pos).
            final RockData rockData = ChunkDataProvider.get(context.chunkGenerator()).get(level, pos).getRockData();
            return rockData.getSurfaceRock(pos.getX(), pos.getZ()).hardened().defaultBlockState();
        }
        catch (AssertionError | IllegalStateException | NullPointerException ignored)
        {
            // Fall back to the already placed sea floor blocks for older runtime variants where chunk data is unavailable here.
        }

        if (context.chunkGenerator() instanceof ChunkGeneratorExtension extension)
        {
            final RockLayerSettings rockLayerSettings = extension.rockLayerSettings();
            final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int y = Math.min(seaLevel, pos.getY()); y >= context.chunkGenerator().getMinY(); y--)
            {
                cursor.set(pos.getX(), y, pos.getZ());
                final BlockState state = level.getBlockState(cursor);
                if (state.canBeReplaced() || !state.getFluidState().isEmpty())
                {
                    continue;
                }

                final RockSettings rock = rockLayerSettings.getRock(state.getBlock());
                if (rock != null)
                {
                    return rock.hardened().defaultBlockState();
                }
            }
        }

        return null;
    }
}
