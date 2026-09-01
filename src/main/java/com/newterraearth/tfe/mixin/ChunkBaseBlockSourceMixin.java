package com.newterraearth.tfe.mixin;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.dries007.tfc.world.ChunkBaseBlockSource;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.biome.TFCBiomes;

import com.newterraearth.tfe.world.NTEChunkBaseBlockSourceAccess;

@Mixin(value = ChunkBaseBlockSource.class, remap = false)
public abstract class ChunkBaseBlockSourceMixin implements NTEChunkBaseBlockSourceAccess
{
    @Shadow @Final private BlockState[] cachedFluidStates;

    @Override
    public void tfe$useAccurateBiome(int localX, int localZ, BiomeExtension biome, double weight, boolean couldBeSalty, boolean forceCoastalSaltWater)
    {
        cachedFluidStates[(localX & 15) | ((localZ & 15) << 4)] =
            !forceCoastalSaltWater && (!couldBeSalty || (!biome.isSalty() && (weight > 0.5 || biome == TFCBiomes.RIVER)))
                ? Blocks.WATER.defaultBlockState()
                : net.dries007.tfc.common.blocks.TFCBlocks.SALT_WATER.get().defaultBlockState();
    }
}
