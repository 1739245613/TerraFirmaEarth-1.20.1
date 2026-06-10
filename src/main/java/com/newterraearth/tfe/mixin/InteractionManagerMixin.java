package com.newterraearth.tfe.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blockentities.LogPileBlockEntity;
import net.dries007.tfc.common.blockentities.TFCBlockEntities;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.util.BlockItemPlacement;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.InteractionManager;

import com.newterraearth.tfe.common.NTELogPileHelpers;

@Mixin(value = InteractionManager.class, remap = false)
public abstract class InteractionManagerMixin
{
    /**
     * Port the 1.21 log-pile interaction before the old 1.20 registered action can place
     * a side-adjacent pile from a full clicked pile.
     */
    @Inject(method = "onItemUse", at = @At("HEAD"), cancellable = true)
    private static void tfe$use121LogPileInteraction(ItemStack stack, UseOnContext context, boolean isTargetingAir, CallbackInfoReturnable<Optional<InteractionResult>> cir)
    {
        final Player player = context.getPlayer();
        if (isTargetingAir || player == null || !player.mayBuild() || !player.isShiftKeyDown() || !Helpers.isItem(stack.getItem(), TFCTags.Items.LOG_PILE_LOGS))
        {
            return;
        }

        final Level level = context.getLevel();
        final Direction direction = context.getClickedFace();
        final BlockPos clickedPos = context.getClickedPos();
        final BlockState clickedState = level.getBlockState(clickedPos);
        final BlockPos relativePos = clickedPos.relative(direction);

        if (Helpers.isBlock(clickedState, TFCBlocks.LOG_PILE.get()))
        {
            if (!level.isClientSide() && level.getBlockEntity(clickedPos) instanceof LogPileBlockEntity logPile)
            {
                NTELogPileHelpers.insertAndPushUp(stack, clickedState, level, clickedPos, logPile, true);
            }
            cir.setReturnValue(Optional.of(InteractionResult.sidedSuccess(level.isClientSide)));
        }
        else if (level.getBlockState(relativePos.below()).isFaceSturdy(level, relativePos.below(), Direction.UP))
        {
            final ItemStack stackBefore = stack.copy();
            final BlockPos actualPlacedPos = new BlockPlaceContext(context).getClickedPos();
            final InteractionResult result = new BlockItemPlacement(() -> Items.AIR, TFCBlocks.LOG_PILE).onItemUse(stack, context);
            if (result.consumesAction())
            {
                Helpers.insertOne(level, actualPlacedPos, TFCBlockEntities.LOG_PILE.get(), stackBefore);
            }
            cir.setReturnValue(Optional.of(result));
        }
    }
}
