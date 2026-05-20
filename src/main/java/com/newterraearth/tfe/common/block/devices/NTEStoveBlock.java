package com.newterraearth.tfe.common.block.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.common.blockentities.AbstractFirepitBlockEntity;
import net.dries007.tfc.common.blockentities.FirepitBlockEntity;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.devices.FirepitBlock;
import net.dries007.tfc.common.items.TFCItems;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.advancements.TFCAdvancements;

import com.newterraearth.tfe.common.NTEDevices;

public class NTEStoveBlock extends FirepitBlock
{
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final VoxelShape BASE_SHAPE = box(1, 0, 1, 15, 12, 15);

    public NTEStoveBlock(ExtendedProperties properties)
    {
        super(properties, BASE_SHAPE);
        registerDefaultState(getStateDefinition().any().setValue(LIT, false).setValue(SMOKE_LEVEL, 0).setValue(AXIS, Direction.Axis.X).setValue(FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        final BlockState state = super.getStateForPlacement(context);
        if (state != null)
        {
            return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(AXIS, context.getHorizontalDirection().getAxis());
        }
        return null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(FACING);
        super.createBlockStateDefinition(builder);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
    {
        // stove doesn't ignite nearby blocks
    }

    @Nullable
    @Override
    public BlockState getStateToDraw(Level level, Player player, BlockState lookState, Direction direction, BlockPos pos, double x, double y, double z, ItemStack item)
    {
        if (Helpers.isItem(item, TFCItems.POT.get()))
        {
            return NTEDevices.STOVE_POT.get().defaultBlockState()
                .setValue(LIT, lookState.getValue(LIT))
                .setValue(NTEStovePotBlock.FACING, level.getBlockState(pos).getValue(FACING));
        }
        return null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
        if (level.getBlockEntity(pos) instanceof FirepitBlockEntity firepit)
        {
            final ItemStack stack = player.getItemInHand(hand);
            // disallow grill on stove (kept for balance like 1.21)
            if (stack.getItem() == TFCItems.WROUGHT_IRON_GRILL.get())
            {
                return InteractionResult.FAIL;
            }
            // pot placement converts stove -> stove_pot (preserve facing via getStateToDraw later)
            if (stack.getItem() == TFCItems.POT.get())
            {
                if (!level.isClientSide)
                {
                    final Block newBlock = NTEDevices.STOVE_POT.get();
                    AbstractFirepitBlockEntity.convertTo(level, pos, state, firepit, newBlock);
                    if (player instanceof ServerPlayer serverPlayer)
                    {
                        TFCAdvancements.FIREPIT_CREATED.trigger(serverPlayer, newBlock.defaultBlockState());
                    }
                    if (!player.isCreative())
                    {
                        stack.shrink(1);
                    }
                }
                return InteractionResult.SUCCESS;
            }
        }
        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState rotate(BlockState state, Rotation rot)
    {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirror)
    {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
