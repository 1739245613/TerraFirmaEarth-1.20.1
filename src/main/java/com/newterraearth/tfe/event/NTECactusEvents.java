package com.newterraearth.tfe.event;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.plant.Plant;

import com.newterraearth.tfe.common.NTEBlocks;

public final class NTECactusEvents
{
    private static final TagKey<Item> SHARP_TOOLS = ItemTags.create(new ResourceLocation("tfc", "sharp_tools"));
    private static final TagKey<Item> AXES = ItemTags.create(new ResourceLocation("tfc", "axes"));
    private static final int MAX_CONNECTED_SAGUARO_BLOCKS = 2048;

    private NTECactusEvents() {}

    public static void init()
    {
        MinecraftForge.EVENT_BUS.register(NTECactusEvents.class);
    }

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event)
    {
        if (!(event.getLevel() instanceof ServerLevel level))
        {
            return;
        }

        final Player player = event.getPlayer();
        if (player.isCreative())
        {
            return;
        }

        final BlockState state = event.getState();
        final ItemStack tool = player.getMainHandItem();
        if (!canHarvestSaguaro(tool))
        {
            return;
        }

        final BlockPos pos = event.getPos();
        if (isSaguaroBranch(state))
        {
            final SaguaroHarvest harvest = collectSaguaro(level, pos);
            if (!harvest.isEmpty())
            {
                event.setCanceled(true);
                removeSaguaroBlocks(level, harvest);
                dropStacks(level, pos, NTEBlocks.CACTUS_WOOD.get(), harvest.branches().size());
                dropStacks(level, pos, saguaroFruitItem(), harvest.fruits().size());
                tool.mineBlock(level, state, pos, player);
            }
        }
        else if (isSaguaroFruit(state))
        {
            event.setCanceled(true);
            level.destroyBlock(pos, false);
            dropStacks(level, pos, saguaroFruitItem(), 1);
            tool.mineBlock(level, state, pos, player);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event)
    {
        final ItemStack stack = event.getItemStack();
        if (!stack.is(saguaroFruitItem()))
        {
            return;
        }

        final Level level = event.getLevel();
        final Player player = event.getEntity();
        final ItemStack plantingStack = new ItemStack(saguaroItem(), 1);
        final InteractionResult result = plantingStack.useOn(new UseOnContext(level, player, event.getHand(), plantingStack, event.getHitVec()));
        if (result.consumesAction())
        {
            event.setCanceled(true);
            event.setCancellationResult(result);
            if (!level.isClientSide)
            {
                if (!player.getAbilities().instabuild)
                {
                    stack.shrink(1);
                }
                if (player instanceof ServerPlayer serverPlayer)
                {
                    CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(serverPlayer, event.getPos(), stack);
                }
            }
        }
    }

    private static boolean canHarvestSaguaro(ItemStack stack)
    {
        return stack.is(SHARP_TOOLS) || stack.is(AXES) || stack.is(TFCTags.Items.AXES_THAT_LOG) || stack.is(TFCTags.Items.HOES) || stack.is(TFCTags.Items.KNIVES);
    }

    private static SaguaroHarvest collectSaguaro(ServerLevel level, BlockPos start)
    {
        final Set<BlockPos> branches = new HashSet<>();
        final Set<BlockPos> fruits = new HashSet<>();
        final ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start.immutable());

        while (!queue.isEmpty() && branches.size() < MAX_CONNECTED_SAGUARO_BLOCKS)
        {
            final BlockPos pos = queue.removeFirst();
            if (!level.isLoaded(pos) || !isSaguaroBranch(level.getBlockState(pos)) || !branches.add(pos))
            {
                continue;
            }

            for (Direction direction : Direction.values())
            {
                final BlockPos neighbor = pos.relative(direction);
                if (!level.isLoaded(neighbor))
                {
                    continue;
                }

                final BlockState neighborState = level.getBlockState(neighbor);
                if (isSaguaroBranch(neighborState) && !branches.contains(neighbor))
                {
                    queue.add(neighbor.immutable());
                }
                else if (isSaguaroFruit(neighborState))
                {
                    fruits.add(neighbor.immutable());
                }
            }
        }

        return new SaguaroHarvest(branches, fruits);
    }

    private static void removeSaguaroBlocks(ServerLevel level, SaguaroHarvest harvest)
    {
        for (BlockPos fruit : harvest.fruits())
        {
            level.destroyBlock(fruit, false);
        }
        for (BlockPos branch : harvest.branches())
        {
            level.destroyBlock(branch, false);
        }
    }

    private static void dropStacks(ServerLevel level, BlockPos pos, Item item, int count)
    {
        final int maxStackSize = new ItemStack(item).getMaxStackSize();
        while (count > 0)
        {
            final int dropped = Math.min(count, maxStackSize);
            Block.popResource(level, pos, new ItemStack(item, dropped));
            count -= dropped;
        }
    }

    private static boolean isSaguaroBranch(BlockState state)
    {
        final Block block = state.getBlock();
        return block == TFCBlocks.PLANTS.get(Plant.SAGUARO).get() || block == TFCBlocks.PLANTS.get(Plant.SAGUARO_PLANT).get();
    }

    private static boolean isSaguaroFruit(BlockState state)
    {
        return state.getBlock() == TFCBlocks.PLANTS.get(Plant.SAGUARO_FRUIT).get();
    }

    private static Item saguaroItem()
    {
        return TFCBlocks.PLANTS.get(Plant.SAGUARO).get().asItem();
    }

    private static Item saguaroFruitItem()
    {
        return TFCBlocks.PLANTS.get(Plant.SAGUARO_FRUIT).get().asItem();
    }

    private record SaguaroHarvest(Set<BlockPos> branches, Set<BlockPos> fruits)
    {
        boolean isEmpty()
        {
            return branches.isEmpty() && fruits.isEmpty();
        }
    }
}
