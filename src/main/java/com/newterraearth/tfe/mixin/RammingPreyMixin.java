package com.newterraearth.tfe.mixin;

import com.mojang.serialization.Dynamic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.dries007.tfc.common.entities.prey.RammingPrey;

import com.newterraearth.tfe.common.entity.ai.NTERammingPreyAi;

@Mixin(RammingPrey.class)
public abstract class RammingPreyMixin
{
    @Shadow
    protected abstract Brain.Provider<? extends RammingPrey> brainProvider();

    /**
     * Route every TFC ramming animal through the 1.21-compatible brain.
     * @author Codex
     * @reason Replace the 1.20 RammingPrey brain with the addon-owned 1.21 port.
     */
    @Overwrite
    protected Brain<?> makeBrain(Dynamic<?> dynamic)
    {
        return NTERammingPreyAi.makeBrain(brainProvider().makeBrain(dynamic));
    }

    /**
     * Keep the activity update paired with the replacement brain.
     * @author Codex
     * @reason Use the matching addon-owned activity scheduler for every RammingPrey.
     */
    @Overwrite
    protected void customServerAiStep()
    {
        final RammingPrey prey = (RammingPrey) (Object) this;
        prey.getBrain().tick((ServerLevel) prey.level(), prey);
        NTERammingPreyAi.updateActivity(prey);
    }

    @Inject(method = "finalizeSpawn", at = @At("TAIL"))
    private void tfe$initialize121Cooldown(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnData, @Nullable net.minecraft.nbt.CompoundTag tag, CallbackInfoReturnable<SpawnGroupData> cir)
    {
        NTERammingPreyAi.initMemories((RammingPrey) (Object) this, level.getRandom());
    }
}
