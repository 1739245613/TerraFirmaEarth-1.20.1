package com.newterraearth.tfe.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.dries007.tfc.client.ClientHelpers;
import net.dries007.tfc.client.IngameOverlays;
import net.dries007.tfc.common.capabilities.player.PlayerDataCapability;

@Mixin(value = IngameOverlays.class, remap = false)
public final class IngameOverlaysMixin
{
    /**
     * TFC's chisel HUD may outlive the old client player's capabilities during the death transition.
     *
     * <p>临时兼容 TFC 上游缺陷：直到官方修复后撤销修改。</p>
     */
    @SuppressWarnings("deprecation")
    @Inject(
        method = "renderChiselMode",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/common/capabilities/player/PlayerData;get(Lnet/minecraft/world/entity/player/Player;)Lnet/dries007/tfc/common/capabilities/player/PlayerData;"
        ),
        cancellable = true,
        remap = false,
        require = 0
    )
    private static void tfe$skipChiselHudWithoutPlayerData(
        ForgeGui gui,
        GuiGraphics graphics,
        float partialTicks,
        int width,
        int height,
        CallbackInfo ci
    )
    {
        final Player player = ClientHelpers.getPlayer();
        if (player == null || !player.getCapability(PlayerDataCapability.CAPABILITY).isPresent())
        {
            ci.cancel();
        }
    }
}
