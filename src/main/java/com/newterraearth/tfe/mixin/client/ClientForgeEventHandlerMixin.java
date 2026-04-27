package com.newterraearth.tfe.mixin.client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.dries007.tfc.client.ClimateRenderCache;
import net.dries007.tfc.client.ClientForgeEventHandler;
import net.dries007.tfc.config.TFCConfig;

import com.newterraearth.tfe.client.NTEClimateRenderCacheBridge;

@Mixin(value = ClientForgeEventHandler.class, remap = false)
public final class ClientForgeEventHandlerMixin
{
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
