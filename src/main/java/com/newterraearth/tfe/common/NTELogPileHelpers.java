package com.newterraearth.tfe.common;

import java.lang.reflect.Field;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;

import net.dries007.tfc.common.blockentities.InventoryBlockEntity;
import net.dries007.tfc.common.blockentities.LogPileBlockEntity;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.capabilities.InventoryItemHandler;
import net.dries007.tfc.common.blocks.devices.LogPileBlock;
import net.dries007.tfc.util.Helpers;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_AXIS;

public final class NTELogPileHelpers
{
    public static final int MAX_LOGS = 16;
    public static final IntegerProperty LOG_PILE_COUNT = IntegerProperty.create("count", 1, MAX_LOGS);

    private static final Field INVENTORY_FIELD = findInventoryField();

    private static final VoxelShape[][] SHAPES_BY_AXIS_BY_COUNT = Util.make(new VoxelShape[2][MAX_LOGS], shapes -> {
        for (int i = 0; i < MAX_LOGS; i++)
        {
            final int layer = i / 4;
            final int row = i % 4 + 1;
            final double[] box2 = new double[] {0, 4 * layer, 0, 16, 4 * layer + 4, 4 * row};
            final double[] box1 = new double[] {0, 0, 0, 16, 4 * layer, 16};

            for (int dir = 0; dir < 2; dir++)
            {
                final Direction direction = dir == 0 ? Direction.SOUTH : Direction.EAST;
                final VoxelShape base = Helpers.rotateShape(direction, box1[0], box1[1], box1[2], box1[3], box1[4], box1[5]);
                final VoxelShape rowShape = Helpers.rotateShape(direction, box2[0], box2[1], box2[2], box2[3], box2[4], box2[5]);
                shapes[dir][i] = Shapes.or(base, rowShape);
            }
        }
    });

    private NTELogPileHelpers() {}

    public static VoxelShape getShape(Direction.Axis axis, int count)
    {
        final int index = Math.max(1, Math.min(MAX_LOGS, count)) - 1;
        return SHAPES_BY_AXIS_BY_COUNT[axis == Direction.Axis.X ? 0 : 1][index];
    }

    public static int getVisibleLogCount(BlockState state)
    {
        return state.hasProperty(LOG_PILE_COUNT) ? state.getValue(LOG_PILE_COUNT) : MAX_LOGS;
    }

    public static BlockState withLogCount(BlockState state, int count)
    {
        return state.hasProperty(LOG_PILE_COUNT)
            ? state.setValue(LOG_PILE_COUNT, Math.max(1, Math.min(MAX_LOGS, count)))
            : state;
    }

    public static void expandAndDisperse(LogPileBlockEntity pile)
    {
        final IItemHandlerModifiable inventory = getInventory(pile);
        if (inventory instanceof InventoryItemHandler handler && (handler.getSlots() != MAX_LOGS || hasStackedLogs(handler)))
        {
            final NonNullList<ItemStack> expandedStacks = NonNullList.withSize(MAX_LOGS, ItemStack.EMPTY);
            int nextSlot = 0;
            for (int i = 0; i < handler.getSlots() && nextSlot < MAX_LOGS; i++)
            {
                final ItemStack stack = handler.getStackInSlot(i).copy();
                while (!stack.isEmpty() && nextSlot < MAX_LOGS)
                {
                    expandedStacks.set(nextSlot++, stack.split(1));
                }
            }
            handler.setSize(MAX_LOGS);
            final NonNullList<ItemStack> internalStacks = handler.getInternalStacks();
            for (int i = 0; i < MAX_LOGS; i++)
            {
                internalStacks.set(i, expandedStacks.get(i));
            }
        }
    }

    public static IItemHandlerModifiable getInventory(LogPileBlockEntity pile)
    {
        try
        {
            return (IItemHandlerModifiable) INVENTORY_FIELD.get(pile);
        }
        catch (IllegalAccessException e)
        {
            throw new IllegalStateException("TFC log pile inventory field is unavailable", e);
        }
    }

    public static int logCount(LogPileBlockEntity pile)
    {
        int count = 0;
        final IItemHandlerModifiable inventory = getInventory(pile);
        for (int i = 0; i < inventory.getSlots(); i++)
        {
            count += inventory.getStackInSlot(i).getCount();
        }
        return Math.min(count, MAX_LOGS);
    }

    public static boolean isEmpty(LogPileBlockEntity pile)
    {
        final IItemHandlerModifiable inventory = getInventory(pile);
        for (int i = 0; i < inventory.getSlots(); i++)
        {
            if (!inventory.getStackInSlot(i).isEmpty())
            {
                return false;
            }
        }
        return true;
    }

    public static void setAndUpdateSlots(LogPileBlockEntity pile, int slot)
    {
        if (pile.getLevel() != null && !pile.getLevel().isClientSide())
        {
            expandAndDisperse(pile);
            suckLogsFromAbove(pile);
            final Level level = pile.getLevel();
            final BlockPos pos = pile.getBlockPos();
            final BlockState state = level.getBlockState(pos);
            if (isEmpty(pile))
            {
                level.removeBlock(pos, false);
            }
            else if (state.is(TFCBlocks.LOG_PILE.get()))
            {
                level.setBlockAndUpdate(pos, withLogCount(state, logCount(pile)));
            }
        }
        ((BlockEntity) pile).setChanged();
    }

    public static void suckLogsFromAbove(LogPileBlockEntity pile)
    {
        final Level level = pile.getLevel();
        if (level == null || level.isClientSide())
        {
            return;
        }

        if (!(level.getBlockEntity(pile.getBlockPos().above()) instanceof LogPileBlockEntity pileAbove) || isEmpty(pileAbove))
        {
            return;
        }

        expandAndDisperse(pileAbove);
        final IItemHandlerModifiable inventory = getInventory(pile);
        final IItemHandlerModifiable inventoryAbove = getInventory(pileAbove);
        if (!(inventory instanceof InventoryItemHandler lowerHandler) || !(inventoryAbove instanceof InventoryItemHandler upperHandler))
        {
            return;
        }

        boolean moved = false;
        final NonNullList<ItemStack> lowerStacks = lowerHandler.getInternalStacks();
        final NonNullList<ItemStack> upperStacks = upperHandler.getInternalStacks();
        for (int upperSlot = 0; upperSlot < MAX_LOGS; upperSlot++)
        {
            final ItemStack upperStack = upperStacks.get(upperSlot);
            while (!upperStack.isEmpty())
            {
                final int lowerSlot = firstEmptySlot(lowerStacks);
                if (lowerSlot == -1)
                {
                    if (moved)
                    {
                        pileAbove.setAndUpdateSlots(-1);
                    }
                    return;
                }
                lowerStacks.set(lowerSlot, upperStack.split(1));
                moved = true;
            }
        }
        if (moved)
        {
            pileAbove.setAndUpdateSlots(-1);
        }
    }

    public static boolean insertAndPushUp(ItemStack stack, BlockState state, Level level, BlockPos pos, LogPileBlockEntity logPile, boolean all)
    {
        expandAndDisperse(logPile);
        if (insert(stack, state, level, pos, logPile, all) && !all)
        {
            return true;
        }

        final BlockPos abovePos = pos.above();
        if (level.getBlockState(abovePos).isAir() && logPile.logCount() == MAX_LOGS && !stack.isEmpty())
        {
            level.setBlockAndUpdate(abovePos, TFCBlocks.LOG_PILE.get().defaultBlockState()
                .setValue(HORIZONTAL_AXIS, state.getValue(HORIZONTAL_AXIS))
                .setValue(LOG_PILE_COUNT, 1));
            if (level.getBlockEntity(abovePos) instanceof LogPileBlockEntity pileAbove)
            {
                final BlockState stateAbove = level.getBlockState(abovePos);
                if (insert(stack, stateAbove, level, abovePos, pileAbove, all))
                {
                    return true;
                }
                level.removeBlock(abovePos, false);
            }
        }

        if (level.getBlockState(abovePos).getBlock() instanceof LogPileBlock && logPile.logCount() == MAX_LOGS && level.getBlockEntity(abovePos) instanceof LogPileBlockEntity pileAbove)
        {
            final BlockState stateAbove = level.getBlockState(abovePos);
            return insertAndPushUp(stack, stateAbove, level, abovePos, pileAbove, all);
        }
        return false;
    }

    public static void extractFromTop(Level level, BlockPos pos, Player player, boolean all)
    {
        if (level.getBlockState(pos.above()).is(TFCBlocks.LOG_PILE.get()))
        {
            extractFromTop(level, pos.above(), player, all);
        }
        else if (level.getBlockEntity(pos) instanceof LogPileBlockEntity logPile)
        {
            expandAndDisperse(logPile);
            final IItemHandlerModifiable inventory = getInventory(logPile);
            for (int i = 0; i < inventory.getSlots(); i++)
            {
                final ItemStack extracted = inventory.extractItem(i, 1, false);
                if (!extracted.isEmpty())
                {
                    ItemHandlerHelper.giveItemToPlayer(player, extracted);
                    logPile.setAndUpdateSlots(-1);
                    if (!all)
                    {
                        break;
                    }
                }
            }
        }
    }

    public static void updateBurningCount(Level level, BlockPos pos, int logs)
    {
        final BlockState state = level.getBlockState(pos);
        if (state.is(TFCBlocks.BURNING_LOG_PILE.get()))
        {
            level.setBlockAndUpdate(pos, withLogCount(state, logs));
        }
    }

    private static boolean insert(ItemStack stack, BlockState state, Level level, BlockPos pos, LogPileBlockEntity logPile, boolean all)
    {
        if (all)
        {
            final ItemStack inserted = stack.copy();
            final ItemStack remainder = insertAll(logPile, inserted);
            if (remainder.getCount() < stack.getCount())
            {
                Helpers.playPlaceSound(level, pos, SoundType.WOOD);
                stack.setCount(remainder.getCount());
                logPile.setAndUpdateSlots(-1);
                return true;
            }
        }
        else if (insertOne(logPile, stack))
        {
            Helpers.playPlaceSound(level, pos, state);
            stack.shrink(1);
            logPile.setAndUpdateSlots(-1);
            return true;
        }
        return false;
    }

    private static ItemStack insertAll(LogPileBlockEntity logPile, ItemStack stack)
    {
        return Helpers.insertAllSlots(getInventory(logPile), stack);
    }

    private static boolean insertOne(LogPileBlockEntity logPile, ItemStack stack)
    {
        final ItemStack inserted = stack.copy();
        inserted.setCount(1);
        return Helpers.insertAllSlots(getInventory(logPile), inserted).isEmpty();
    }

    private static boolean hasStackedLogs(IItemHandler inventory)
    {
        for (int i = 0; i < inventory.getSlots(); i++)
        {
            if (inventory.getStackInSlot(i).getCount() > 1)
            {
                return true;
            }
        }
        return false;
    }

    private static int firstEmptySlot(NonNullList<ItemStack> stacks)
    {
        for (int i = 0; i < stacks.size(); i++)
        {
            if (stacks.get(i).isEmpty())
            {
                return i;
            }
        }
        return -1;
    }

    private static Field findInventoryField()
    {
        try
        {
            final Field field = InventoryBlockEntity.class.getDeclaredField("inventory");
            field.setAccessible(true);
            return field;
        }
        catch (NoSuchFieldException e)
        {
            throw new IllegalStateException("TFC inventory field name changed", e);
        }
    }

}
