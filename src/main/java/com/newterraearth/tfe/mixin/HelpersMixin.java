package com.newterraearth.tfe.mixin;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.util.Helpers;

import com.newterraearth.tfe.world.NTEPestHelpers;

@Mixin(value = Helpers.class, remap = false)
public abstract class HelpersMixin
{
    /**
     * Keep the existing 1.20 infestation flow and only swap the pest selection step to the 1.21 climate-aware rules.
     */
    @Redirect(
        method = "tickInfestation",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/util/Helpers;randomEntity(Lnet/minecraft/tags/TagKey;Lnet/minecraft/util/RandomSource;)Ljava/util/Optional;"
        ),
        remap = false,
        require = 0
    )
    private static Optional<EntityType<?>> tfe$chooseClimateAwarePest(
        TagKey<EntityType<?>> tag,
        RandomSource random,
        Level level,
        BlockPos pos,
        int infestation,
        @Nullable Player player
    )
    {
        if (tag.location().equals(TFCTags.Entities.PESTS.location()))
        {
            return NTEPestHelpers.choosePest(level, pos);
        }
        return Helpers.randomEntity(tag, random);
    }
}
