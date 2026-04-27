package com.newterraearth.tfe.world.feature.plant;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import net.dries007.tfc.common.blocks.soil.DirtBlock;
import net.dries007.tfc.common.blocks.soil.IGrassBlock;

import com.newterraearth.tfe.config.NTECommonConfig;

public class NTEBambooFeature extends Feature<NTEBambooConfig>
{
    public NTEBambooFeature(Codec<NTEBambooConfig> codec)
    {
        super(codec);
    }

    private static BlockState trunk(NTEBambooConfig config)
    {
        return config.state().setValue(BambooStalkBlock.AGE, 1).setValue(BambooStalkBlock.LEAVES, BambooLeaves.NONE).setValue(BambooStalkBlock.STAGE, 0);
    }

    private static BlockState finalLarge(NTEBambooConfig config)
    {
        return trunk(config).setValue(BambooStalkBlock.LEAVES, BambooLeaves.LARGE).setValue(BambooStalkBlock.STAGE, 1);
    }

    private static BlockState topLarge(NTEBambooConfig config)
    {
        return trunk(config).setValue(BambooStalkBlock.LEAVES, BambooLeaves.LARGE);
    }

    private static BlockState topSmall(NTEBambooConfig config)
    {
        return trunk(config).setValue(BambooStalkBlock.LEAVES, BambooLeaves.SMALL);
    }

    @Override
    public boolean place(FeaturePlaceContext<NTEBambooConfig> context)
    {
        final BlockPos origin = context.origin();
        final WorldGenLevel level = context.level();
        final var random = context.random();
        final NTEBambooConfig config = context.config();
        final BlockPos.MutableBlockPos cursor = origin.mutable();
        final BlockPos.MutableBlockPos ground = origin.mutable();

        if (!NTECommonConfig.isBambooEnabledByBlock(config.state().getBlock()))
        {
            return false;
        }

        if (!level.isEmptyBlock(cursor) || !config.state().canSurvive(level, cursor))
        {
            return false;
        }

        final int trunkSize = random.nextInt(12) + 5;
        if (random.nextFloat() < config.probability())
        {
            final int radius = random.nextInt(4) + 1;
            for (int x = origin.getX() - radius; x <= origin.getX() + radius; ++x)
            {
                for (int z = origin.getZ() - radius; z <= origin.getZ() + radius; ++z)
                {
                    final int dx = x - origin.getX();
                    final int dz = z - origin.getZ();
                    if (dx * dx + dz * dz <= radius * radius)
                    {
                        ground.set(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1, z);
                        final var under = level.getBlockState(ground).getBlock();
                        if (under instanceof IGrassBlock grass && grass.getDirt().getBlock() instanceof DirtBlock dirt)
                        {
                            level.setBlock(ground, dirt.getRooted(), 2);
                        }
                        else if (under instanceof DirtBlock dirt)
                        {
                            level.setBlock(ground, dirt.getRooted(), 2);
                        }
                    }
                }
            }
        }

        final BlockState trunk = trunk(config);
        for (int i = 0; i < trunkSize && level.isEmptyBlock(cursor); ++i)
        {
            level.setBlock(cursor, trunk, 2);
            cursor.move(Direction.UP);
        }

        if (cursor.getY() - origin.getY() >= 3)
        {
            level.setBlock(cursor, finalLarge(config), 2);
            level.setBlock(cursor.move(Direction.DOWN), topLarge(config), 2);
            level.setBlock(cursor.move(Direction.DOWN), topSmall(config), 2);
        }

        return true;
    }
}
