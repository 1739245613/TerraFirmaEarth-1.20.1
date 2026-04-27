package com.newterraearth.tfe.common.block.plant;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.plant.WaterPlantBlock;
import net.dries007.tfc.common.fluids.FluidProperty;
import net.dries007.tfc.common.fluids.TFCFluids;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.registry.RegistryPlant;

public abstract class NTERotatableWaterPlantBlock extends WaterPlantBlock
{
    private static final TagKey<Block> ANEMONE_PLANTABLE_ON = BlockTags.create(Helpers.identifier("anemone_plantable_on"));
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private static final VoxelShape NORTH_SHAPE = box(4.0, 4.0, 10.0, 12.0, 12.0, 16.0);
    private static final VoxelShape SOUTH_SHAPE = box(4.0, 4.0, 0.0, 12.0, 12.0, 6.0);
    private static final VoxelShape WEST_SHAPE = box(10.0, 4.0, 4.0, 16.0, 12.0, 12.0);
    private static final VoxelShape EAST_SHAPE = box(0.0, 4.0, 4.0, 6.0, 12.0, 12.0);
    private static final VoxelShape UP_SHAPE = box(4.0, 0.0, 4.0, 12.0, 6.0, 12.0);
    private static final VoxelShape DOWN_SHAPE = box(4.0, 10.0, 4.0, 12.0, 16.0, 12.0);
    private static final Map<Direction, VoxelShape> SHAPES = ImmutableMap.of(
        Direction.NORTH, NORTH_SHAPE,
        Direction.SOUTH, SOUTH_SHAPE,
        Direction.WEST, WEST_SHAPE,
        Direction.EAST, EAST_SHAPE,
        Direction.UP, UP_SHAPE,
        Direction.DOWN, DOWN_SHAPE
    );

    public static NTERotatableWaterPlantBlock create(RegistryPlant plant, FluidProperty fluid, ExtendedProperties properties)
    {
        return new NTERotatableWaterPlantBlock(properties)
        {
            @Override
            public RegistryPlant getPlant()
            {
                return plant;
            }

            @Override
            public FluidProperty getFluidProperty()
            {
                return fluid;
            }
        };
    }

    protected NTERotatableWaterPlantBlock(ExtendedProperties properties)
    {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.UP).setValue(OPEN, false));
    }

    public abstract FluidProperty getFluidProperty();

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving)
    {
        if (!canSurvive(state, level, pos))
        {
            level.destroyBlock(pos, false);
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos)
    {
        if (direction.getOpposite() == state.getValue(FACING) && !Helpers.isBlock(facingState, ANEMONE_PLANTABLE_ON))
        {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
    {
        return Helpers.isBlock(level.getBlockState(pos.relative(state.getValue(FACING).getOpposite())), ANEMONE_PLANTABLE_ON);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        final Direction direction = context.getClickedFace();
        final BlockPos pos = context.getClickedPos();
        final Fluid fluid = context.getLevel().getFluidState(pos).getType();
        BlockState state = defaultBlockState().setValue(FACING, direction);
        if (getFluidProperty().canContain(fluid))
        {
            state = state.setValue(getFluidProperty(), getFluidProperty().keyFor(fluid));
            if (fluid == TFCFluids.SALT_WATER.getSource())
            {
                state = state.setValue(OPEN, true);
            }
        }
        return state;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder.add(FACING).add(OPEN));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot)
    {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror)
    {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
    {
        if (level.getFluidState(pos).getType().isSame(TFCFluids.SALT_WATER.getSource()))
        {
            level.setBlock(pos, state.setValue(OPEN, true), Block.UPDATE_ALL);
        }
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity)
    {
        if (!level.isClientSide)
        {
            level.setBlock(pos, state.setValue(OPEN, false), Block.UPDATE_ALL);
            level.scheduleTick(pos, this, 150);
        }
    }
}
