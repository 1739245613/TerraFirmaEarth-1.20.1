package com.newterraearth.tfe.mixin.client;

import net.dries007.tfc.client.particle.TFCParticles;
import net.dries007.tfc.common.blocks.plant.PlantBlock;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.calendar.Season;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.newterraearth.tfe.client.NTEClimateRenderHelpers;

@Mixin(value = PlantBlock.class, remap = false)
public abstract class PlantBlockMixin
{
    /**
     * @author Codex
     * @reason Client-side spring butterfly visuals should respect the local hemisphere.
     */
    @Inject(
        method = "animateTick",
        at = @At("HEAD"),
        cancellable = true,
        remap = true
    )
    private void tfe$useHemispheralMonth(BlockState state, Level level, BlockPos pos, RandomSource random, CallbackInfo ci)
    {
        if (random.nextInt(400) == 0
            && Helpers.isBlock(state, BlockTags.FLOWERS)
            && NTEClimateRenderHelpers.getClientHemispheralCalendarMonthOfYear().getSeason() == Season.SPRING)
        {
            level.addParticle((ParticleOptions) TFCParticles.BUTTERFLY.get(),
                pos.getX() + random.nextFloat(),
                pos.getY() + random.nextFloat(),
                pos.getZ() + random.nextFloat(),
                0d, 0d, 0d);
        }
        ci.cancel();
    }
}
