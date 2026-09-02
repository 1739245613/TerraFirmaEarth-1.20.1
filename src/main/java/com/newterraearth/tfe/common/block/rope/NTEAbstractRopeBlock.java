package com.newterraearth.tfe.common.block.rope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.items.ItemHandlerHelper;

import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.IForgeBlockExtension;
import net.dries007.tfc.util.Helpers;

import com.newterraearth.tfe.common.entity.NTEItems;

public abstract class NTEAbstractRopeBlock extends HorizontalDirectionalBlock implements IForgeBlockExtension
{
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final VoxelShape SHAPE_X = box(7, 0, 0, 9, 2, 16);
    public static final VoxelShape SHAPE_Z = Helpers.rotateShape(Direction.EAST, 7, 0, 0, 9, 2, 16);

    private final ExtendedProperties properties;

    protected NTEAbstractRopeBlock(ExtendedProperties properties)
    {
        super(properties.properties());
        this.properties = properties;
    }

    public static void recallRope(LevelAccessor level, BlockPos pos, BlockState state, Player player, Direction dir)
    {
        if (!(state.getBlock() instanceof NTEAbstractRopeBlock))
            return;

        final List<BlockPos> positions = new ArrayList<>(32);
        while (true)
        {
            if (state.getBlock() instanceof NTEHangingRopeBlock)
            {
                pos = pos.below();
                state = level.getBlockState(pos);
                if (continuesLine(state, dir)) positions.add(pos);
                else break;
            }
            else
            {
                pos = pos.relative(dir);
                state = level.getBlockState(pos);
                if (!continuesLine(state, dir))
                {
                    pos = pos.below();
                    state = level.getBlockState(pos);
                    if (continuesLine(state, dir)) positions.add(pos);
                    else break;
                }
                else positions.add(pos);
            }
        }

        Collections.reverse(positions);
        for (BlockPos ropePos : positions)
        {
            level.destroyBlock(ropePos, false);
            if (!player.isCreative())
                ItemHandlerHelper.giveItemToPlayer(player, NTEItems.ROPE.get().getDefaultInstance());
        }
    }

    private static boolean continuesLine(BlockState state, Direction dir)
    {
        return state.getBlock() instanceof NTEAbstractRopeBlock && state.getValue(FACING) == dir.getOpposite();
    }

    protected boolean isBlockBelowSturdy(LevelReader level, BlockPos pos)
    {
        return !level.getBlockState(pos.below()).canBeReplaced();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder.add(FACING));
    }

    @Override
    public ExtendedProperties getExtendedProperties()
    {
        return properties;
    }
}
