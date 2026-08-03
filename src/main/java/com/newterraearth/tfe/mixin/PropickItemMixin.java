package com.newterraearth.tfe.mixin;

import java.util.List;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.network.PacketDistributor;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.items.PropickItem;
import net.dries007.tfc.common.items.ProspectResult;
import net.dries007.tfc.network.PacketHandler;
import net.dries007.tfc.network.ProspectedPacket;

import com.newterraearth.tfe.network.NTEPacketHandler;
import com.newterraearth.tfe.network.NTEProspectingResultPacket;
import com.newterraearth.tfe.world.prospecting.NTEProspectingRules;
import com.newterraearth.tfe.world.prospecting.NTEProspectingRules.MineralResult;
import com.newterraearth.tfe.world.prospecting.NTEProspectingScan;

/** Adds hidden-mineral hints and one short-lived direction target to the TFC propick. */
@Mixin(PropickItem.class)
public abstract class PropickItemMixin
{
    @Unique private static final ThreadLocal<NTEProspectingScan> TFE_SCAN = new ThreadLocal<>();
    @Unique private static final ThreadLocal<UseOnContext> TFE_USE_CONTEXT = new ThreadLocal<>();

    @Inject(method = "useOn", at = @At("HEAD"))
    private void tfe$resetScanContext(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir)
    {
        TFE_SCAN.remove();
        TFE_USE_CONTEXT.set(context);
    }

    @Inject(method = "useOn", at = @At("RETURN"))
    private void tfe$clearScanContext(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir)
    {
        TFE_SCAN.remove();
        TFE_USE_CONTEXT.remove();
    }

    /** Removes false negatives from both use behavior and the advanced accuracy tooltip. */
    @Redirect(
        method = {"useOn", "appendHoverText"},
        at = @At(
            value = "FIELD",
            target = "Lnet/dries007/tfc/common/items/PropickItem;falseNegativeChance:F",
            opcode = Opcodes.GETFIELD,
            remap = false
        )
    )
    private float tfe$removeFalseNegativeChance(PropickItem item)
    {
        return 0F;
    }

    @Redirect(
        method = "useOn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/common/items/PropickItem;scanAreaFor(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ILnet/minecraft/tags/TagKey;)Lit/unimi/dsi/fastutil/objects/Object2IntMap;",
            remap = false
        )
    )
    private Object2IntMap<Block> tfe$scanWithNavigation(
        Level level,
        BlockPos center,
        int radius,
        TagKey<Block> tag
    )
    {
        final UseOnContext context = TFE_USE_CONTEXT.get();
        if (context == null || !(context.getPlayer() instanceof ServerPlayer player))
        {
            return PropickItem.scanAreaFor(level, center, radius, tag);
        }
        final NTEProspectingScan scan = NTEProspectingScan.scan(level, center, player, tfe$scanRadius(), tag);
        TFE_SCAN.set(scan);
        return scan.counts();
    }

    @Redirect(
        method = "useOn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/network/PacketHandler;send(Lnet/minecraftforge/network/PacketDistributor$PacketTarget;Ljava/lang/Object;)V",
            remap = false
        )
    )
    private void tfe$sendEnhancedResult(
        PacketDistributor.PacketTarget target,
        Object message
    )
    {
        final UseOnContext context = TFE_USE_CONTEXT.get();
        if (!(message instanceof ProspectedPacket prospected) || context == null || !(context.getPlayer() instanceof ServerPlayer player))
        {
            PacketHandler.send(target, message);
            TFE_SCAN.remove();
            TFE_USE_CONTEXT.remove();
            return;
        }

        final ProspectResult result = prospected.result();
        NTEProspectingScan scan = TFE_SCAN.get();
        if (scan == null)
        {
            scan = NTEProspectingScan.scan(
                context.getLevel(),
                context.getClickedPos(),
                player,
                tfe$scanRadius(),
                TFCTags.Blocks.PROSPECTABLE
            );
        }
        final List<MineralResult> minerals;
        final int hiddenMinerals;
        final BlockPos nearestPos;
        if (scan != null && !scan.counts().isEmpty())
        {
            minerals = NTEProspectingRules.selectResults(scan.counts(), prospected.block(), tfe$toolLevel());
            hiddenMinerals = Math.max(0, scan.mineralTypes() - minerals.size());
            nearestPos = scan.nearestPos();
        }
        else if (result == ProspectResult.FOUND)
        {
            minerals = List.of(new MineralResult(PropickItem.getRepresentative(prospected.block()), ProspectResult.FOUND));
            hiddenMinerals = 0;
            nearestPos = context.getClickedPos().immutable();
        }
        else
        {
            minerals = List.of();
            hiddenMinerals = 0;
            nearestPos = null;
        }

        NTEPacketHandler.send(target, new NTEProspectingResultPacket(
            prospected.block(),
            result,
            minerals,
            hiddenMinerals,
            nearestPos
        ));
        TFE_SCAN.remove();
        TFE_USE_CONTEXT.remove();
    }

    @Unique
    private int tfe$toolLevel()
    {
        return ((PropickItem) (Object) this).getTier().getLevel();
    }

    @Unique
    private int tfe$scanRadius()
    {
        return NTEProspectingRules.scanRadius(tfe$toolLevel());
    }
}
