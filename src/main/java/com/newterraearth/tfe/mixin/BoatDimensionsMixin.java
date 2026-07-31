package com.newterraearth.tfe.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.vehicle.Boat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Supplies the compact horizontal dimensions used by boats for block movement.
 */
@Mixin(Entity.class)
public abstract class BoatDimensionsMixin
{
    @Unique
    private static final float TFE_COMPACT_BOAT_WIDTH = 0.9F;

    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    private void tfe$useCompactBoatDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir)
    {
        final Entity entity = (Entity) (Object) this;
        if (entity instanceof Boat)
        {
            final EntityDimensions registeredDimensions = entity.getType().getDimensions();
            cir.setReturnValue(new EntityDimensions(
                TFE_COMPACT_BOAT_WIDTH,
                registeredDimensions.height,
                registeredDimensions.fixed
            ));
        }
    }
}
