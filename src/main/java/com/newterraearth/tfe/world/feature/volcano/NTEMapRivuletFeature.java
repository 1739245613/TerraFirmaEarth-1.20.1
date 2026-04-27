package com.newterraearth.tfe.world.feature.volcano;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.NotNull;

import net.dries007.tfc.common.fluids.FluidHelpers;

public class NTEMapRivuletFeature extends Feature<NTEBlockStateMapConfig>
{
    public NTEMapRivuletFeature(Codec<NTEBlockStateMapConfig> codec)
    {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NTEBlockStateMapConfig> context)
    {
        final WorldGenLevel world = context.level();
        final BlockPos pos = context.origin();
        final RandomSource rand = context.random();
        final NTEBlockStateMapConfig config = context.config();

        final ChunkPos chunkPos = new ChunkPos(pos);
        final BoundingBox box = new BoundingBox(chunkPos.getMinBlockX() - 14, Integer.MIN_VALUE, chunkPos.getMinBlockZ() - 14, chunkPos.getMaxBlockX() + 14, Integer.MAX_VALUE, chunkPos.getMaxBlockZ() + 14);

        final Set<BlockPos> chosen = new HashSet<>();
        final LinkedList<BlockPos> branches = new LinkedList<>();

        final BlockPos startPos = new BlockPos(pos.getX(), world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, pos.getX(), pos.getZ()), pos.getZ());
        if (!world.getFluidState(startPos.below()).isEmpty()) return false;
        final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        branches.add(startPos);

        boolean mainBranch = true;

        while (!branches.isEmpty())
        {
            final BlockPos branchStartPos = branches.removeFirst();
            int maxLength = 5 + rand.nextInt(8);
            if (mainBranch)
            {
                mainBranch = false;
                maxLength += 12;
            }

            BlockPos lastPos = branchStartPos;

            for (int i = 0; i < maxLength; i++)
            {
                chosen.add(branchStartPos);

                Direction chosenDirection = null;
                int chosenHeight = 0;
                int possibleDirections = 0;
                for (Direction direction : Direction.Plane.HORIZONTAL)
                {
                    mutablePos.setWithOffset(lastPos, direction);
                    if (!box.isInside(mutablePos) || !world.getFluidState(mutablePos.below()).isEmpty())
                    {
                        continue;
                    }

                    final int height = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, mutablePos.getX(), mutablePos.getZ());
                    if (height <= mutablePos.getY())
                    {
                        mutablePos.setY(height - 1);
                        if (world.getBlockState(mutablePos).isSolid())
                        {
                            possibleDirections++;
                            if (chosenDirection == null || rand.nextInt(possibleDirections) == 0)
                            {
                                chosenHeight = height;
                                chosenDirection = direction;
                            }
                        }
                    }
                }

                if (possibleDirections == 0)
                {
                    break;
                }

                if (possibleDirections > 1 && rand.nextInt(3) == 0)
                {
                    branches.add(lastPos);
                }

                mutablePos.setWithOffset(lastPos, chosenDirection).setY(chosenHeight);
                if (!chosen.contains(mutablePos) && world.getFluidState(mutablePos.below()).isEmpty())
                {
                    lastPos = mutablePos.immutable();
                    chosen.add(lastPos);
                }
                else
                {
                    break;
                }
            }
        }

        if (chosen.isEmpty())
        {
            return false;
        }

        for (BlockPos chosenPos : chosen)
        {
            mutablePos.set(chosenPos);
            while (!FluidHelpers.isAirOrEmptyFluid(world.getBlockState(mutablePos)))
            {
                setBlock(world, mutablePos, getReplaceState(world, mutablePos));
                mutablePos.move(Direction.UP);
            }

            mutablePos.setWithOffset(chosenPos, Direction.DOWN);
            setBlock(world, mutablePos, getReplaceState(world, mutablePos));
            mutablePos.move(Direction.DOWN);

            final BlockState stateAt = world.getBlockState(mutablePos);
            final BlockState placementState = config.getState(stateAt);
            if (placementState != null)
            {
                setBlock(world, mutablePos, placementState);
                mutablePos.move(Direction.DOWN);
                setBlock(world, mutablePos, placementState);
            }
        }
        return true;
    }

    @NotNull
    private BlockState getReplaceState(WorldGenLevel level, BlockPos.MutableBlockPos pos)
    {
        for (Direction direction : Direction.Plane.HORIZONTAL)
        {
            pos.move(direction);
            final FluidState fluidState = level.getFluidState(pos);
            if (!fluidState.isEmpty())
            {
                pos.move(direction.getOpposite());
                return fluidState.createLegacyBlock();
            }
            pos.move(direction.getOpposite());
        }
        return Blocks.AIR.defaultBlockState();
    }
}
