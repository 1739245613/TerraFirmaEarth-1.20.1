package com.newterraearth.tfe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blockentities.LogPileBlockEntity;
import net.dries007.tfc.util.Helpers;

import com.newterraearth.tfe.common.NTELogPileHelpers;

@Mixin(value = LogPileBlockEntity.class, remap = false)
public abstract class LogPileBlockEntityMixin
{
    @Inject(method = "<init>", at = @At("TAIL"))
    private void tfe$expandInitialInventory(BlockPos pos, BlockState state, CallbackInfo ci)
    {
        NTELogPileHelpers.expandAndDisperse((LogPileBlockEntity) (Object) this);
    }

    /**
     * @author Codex
     * @reason Port 1.21 log piles to one log per slot, sixteen logs per pile, and visible count state updates.
     */
    @Overwrite(remap = false)
    public void setAndUpdateSlots(int slot)
    {
        NTELogPileHelpers.setAndUpdateSlots((LogPileBlockEntity) (Object) this, slot);
    }

    /**
     * @author Codex
     * @reason 1.21 log piles expose sixteen individual log positions instead of four stacks.
     */
    @Overwrite(remap = false)
    public int getSlotStackLimit(int slot)
    {
        return 1;
    }

    /**
     * @author Codex
     * @reason Keep 1.20 item validation while allowing the 1.21 slot layout.
     */
    @Overwrite(remap = false)
    public boolean isItemValid(int slot, ItemStack stack)
    {
        return Helpers.isItem(stack.getItem(), TFCTags.Items.LOG_PILE_LOGS);
    }

    /**
     * @author Codex
     * @reason Count the expanded sixteen-slot inventory.
     */
    @Overwrite(remap = false)
    public int logCount()
    {
        return NTELogPileHelpers.logCount((LogPileBlockEntity) (Object) this);
    }

    /**
     * @author Codex
     * @reason Empty checks need to read the expanded inventory directly.
     */
    @Overwrite(remap = false)
    public boolean isEmpty()
    {
        return NTELogPileHelpers.isEmpty((LogPileBlockEntity) (Object) this);
    }

}
