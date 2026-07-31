package com.newterraearth.tfe.mixin;

import net.minecraft.core.QuartPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.biome.BiomeSourceExtension;
import net.dries007.tfc.world.biome.TFCBiomes;
import net.dries007.tfc.world.region.RegionPartition;
import net.dries007.tfc.world.region.RiverEdge;
import net.dries007.tfc.world.region.Units;
import com.newterraearth.tfe.world.biome.NTERiverBiomeResolver;
import com.newterraearth.tfe.world.river.NTERiverHydrology;

@Mixin(value = BiomeSourceExtension.class, remap = false)
public interface BiomeSourceExtensionMixin
{
    @Shadow BiomeExtension getBiomeExtensionNoRiver(int quartX, int quartZ);
    @Shadow RegionPartition.Point getPartition(int blockX, int blockZ);

    /**
     * @author Codex
     * @reason Keep runtime biome queries and /locate biome aligned with the visible, fill-safe river water core.
     */
    @Overwrite
    default BiomeExtension getBiomeExtension(int quartX, int quartZ)
    {
        final BiomeExtension biome = getBiomeExtensionNoRiver(quartX, quartZ);
        if (!biome.hasRivers())
        {
            return biome;
        }

        final int blockX = QuartPos.toBlock(quartX);
        final int blockZ = QuartPos.toBlock(quartZ);
        final NTERiverHydrology hydrology = NTERiverBiomeResolver.hydrology((BiomeSourceExtension) this);
        if (hydrology == null)
        {
            final double exactGridX = Units.quartToGridExact(quartX);
            final double exactGridZ = Units.quartToGridExact(quartZ);
            for (RiverEdge edge : getPartition(blockX, blockZ).rivers())
            {
                if (edge.fractal().intersect(exactGridX, exactGridZ, 0.08f))
                {
                    return TFCBiomes.RIVER;
                }
            }
            return biome;
        }
        return NTERiverBiomeResolver.isVisibleRiver((BiomeSourceExtension) this, blockX, blockZ)
            ? TFCBiomes.RIVER
            : biome;
    }
}
