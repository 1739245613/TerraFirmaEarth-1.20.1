package com.newterraearth.tfe.mixin;

import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Stream;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.biome.TFCBiomes;
import net.dries007.tfc.world.placement.VolcanoPlacement;

import com.newterraearth.tfe.world.NTEBiomeExtensionAccess;
import com.newterraearth.tfe.world.NTESeed;
import com.newterraearth.tfe.world.volcano.NTECenteredFeatureBlendType;
import com.newterraearth.tfe.world.volcano.NTECenteredFeatureNoiseSampler;

@Mixin(VolcanoPlacement.class)
public abstract class VolcanoPlacementMixin
{
    @Shadow(remap = false) @Final private boolean center;
    @Shadow(remap = false) @Final private float distance;

    @Unique
    private final ThreadLocal<tfe$LocalContext> tfe$localContext = ThreadLocal.withInitial(() -> null);

    // VolcanoPlacement overrides PlacementModifier#getPositions, so this method must remain remapped for runtime 3.2.x jars.
    @Inject(method = "getPositions", at = @At("HEAD"), cancellable = true, require = 0)
    private void tfe$useCenteredFeaturePlacement(PlacementContext context, RandomSource random, BlockPos pos, CallbackInfoReturnable<Stream<BlockPos>> cir)
    {
        final WorldGenLevel level = context.getLevel();
        final Biome biome = level.getBiome(pos).value();
        final BiomeExtension extension = TFCBiomes.getExtensionOrThrow(level, biome);
        final NTECenteredFeatureBlendType blendType = ((NTEBiomeExtensionAccess) (Object) extension).tfe$getCenteredFeatureBlendType();
        if (blendType == NTECenteredFeatureBlendType.NONE)
        {
            return;
        }

        final tfe$LocalContext local = tfe$getOrCreateLocalContext(level.getSeed());
        final NTECenteredFeatureNoiseSampler sampler = local.samplers().get(blendType);
        if (sampler == null || !sampler.isValidBiome(extension))
        {
            cir.setReturnValue(Stream.empty());
            return;
        }

        if (center)
        {
            final BlockPos centerPos = sampler.calculateCenter(pos, extension);
            if (centerPos != null
                && tfe$isSameChunk(centerPos, pos)
                && sampler.isValidBiome(TFCBiomes.getExtensionOrThrow(level, level.getBiome(centerPos).value())))
            {
                cir.setReturnValue(Stream.of(centerPos));
            }
            else
            {
                cir.setReturnValue(Stream.empty());
            }
            return;
        }

        cir.setReturnValue(sampler.calculateEasing(pos, extension) > distance ? Stream.of(pos) : Stream.empty());
    }

    @Unique
    private tfe$LocalContext tfe$getOrCreateLocalContext(long seed)
    {
        tfe$LocalContext local = tfe$localContext.get();
        if (local == null || local.seed() != seed)
        {
            final NTESeed nteSeed = NTESeed.of(seed);
            final EnumMap<NTECenteredFeatureBlendType, NTECenteredFeatureNoiseSampler> samplers = new EnumMap<>(NTECenteredFeatureBlendType.class);
            for (NTECenteredFeatureBlendType blendType : NTECenteredFeatureBlendType.ALL)
            {
                samplers.put(blendType, blendType.createNoiseSampler(nteSeed));
            }
            local = new tfe$LocalContext(seed, samplers);
            tfe$localContext.set(local);
        }
        return local;
    }

    @Unique
    private static boolean tfe$isSameChunk(BlockPos centerPos, BlockPos origin)
    {
        return SectionPos.blockToSectionCoord(centerPos.getX()) == SectionPos.blockToSectionCoord(origin.getX())
            && SectionPos.blockToSectionCoord(centerPos.getZ()) == SectionPos.blockToSectionCoord(origin.getZ());
    }

    @Unique
    private record tfe$LocalContext(long seed, Map<NTECenteredFeatureBlendType, NTECenteredFeatureNoiseSampler> samplers)
    {
    }
}
