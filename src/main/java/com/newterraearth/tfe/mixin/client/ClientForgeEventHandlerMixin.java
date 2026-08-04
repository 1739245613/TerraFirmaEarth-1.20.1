package com.newterraearth.tfe.mixin.client;

import java.util.List;

import com.mojang.datafixers.util.Either;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.dries007.tfc.client.ClimateRenderCache;
import net.dries007.tfc.client.ClientForgeEventHandler;
import net.dries007.tfc.common.capabilities.player.PlayerDataCapability;
import net.dries007.tfc.common.recipes.ChiselRecipe;
import net.dries007.tfc.config.TFCConfig;

import com.newterraearth.tfe.client.NTEClimateRenderCacheBridge;

@Mixin(value = ClientForgeEventHandler.class, remap = false)
public final class ClientForgeEventHandlerMixin
{
    /**
     * TFC may render one final chisel preview after death has invalidated the old client player's capabilities.
     * Keep-inventory leaves the chisel and hammer equipped, so the unguarded upstream call otherwise throws while
     * resolving the selected chisel mode.
     *
     * <p>临时兼容 TFC 上游缺陷：直到官方修复后撤销修改。</p>
     */
    @SuppressWarnings("deprecation")
    @Redirect(
        method = "onHighlightBlockEvent",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/common/recipes/ChiselRecipe;computeResult(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/phys/BlockHitResult;Z)Lcom/mojang/datafixers/util/Either;"
        ),
        remap = false,
        require = 0
    )
    private static Either<BlockState, InteractionResult> tfe$skipChiselPreviewWithoutPlayerData(
        Player player,
        BlockState state,
        BlockHitResult hit,
        boolean informWhy
    )
    {
        if (!player.getCapability(PlayerDataCapability.CAPABILITY).isPresent())
        {
            return Either.right(InteractionResult.PASS);
        }
        return ChiselRecipe.computeResult(player, state, hit, informWhy);
    }

    /**
     * @author Codex
     * @reason Expose the backported 1.21 climate cache fields in the existing TFC debug overlay without touching gameplay.
     */
    @Inject(method = "onRenderGameOverlayText", at = @At("TAIL"), remap = false, require = 0)
    private static void tfe$appendBackportedClimateDebugText(CustomizeGuiOverlayEvent.DebugText event, CallbackInfo ci)
    {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !minecraft.options.renderDebug || !TFCConfig.CLIENT.enableDebug.get())
        {
            return;
        }

        final Entity camera = minecraft.getCameraEntity();
        if (camera == null)
        {
            return;
        }

        final BlockPos pos = BlockPos.containing(camera.getX(), camera.getBoundingBox().minY, camera.getZ());
        if (!minecraft.level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4))
        {
            return;
        }

        final NTEClimateRenderCacheBridge cache = (NTEClimateRenderCacheBridge) (Object) ClimateRenderCache.INSTANCE;
        final List<String> tooltip = event.getLeft();
        final String[] replacement = new String[] {
            "Temperature: Sea Level Avg: %.3f Avg: %.3f Now: %.3f".formatted(
                cache.getAverageSeaLevelTemperature(),
                ClimateRenderCache.INSTANCE.getAverageTemperature(),
                cache.getInstantTemperature()
            ),
            "Rain: Avg: %.3f Var: %.3f Now: %.3f".formatted(
                cache.getAverageRainfall(),
                cache.getRainVariance(),
                cache.getInstantRainfall()
            ),
            "Water: Avg: %.3f Base: %.3f Now: %.3f".formatted(
                cache.getAverageGroundwater(),
                cache.getBaseGroundwater(),
                cache.getInstantGroundwater()
            )
        };

        for (int i = tooltip.size() - 1; i >= 0; i--)
        {
            if (tooltip.get(i).startsWith("Avg: "))
            {
                tooltip.remove(i);
                tooltip.addAll(i, List.of(replacement));
                return;
            }
        }

        tooltip.addAll(List.of(replacement));
    }
}
