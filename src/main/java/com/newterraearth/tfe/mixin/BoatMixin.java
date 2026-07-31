package com.newterraearth.tfe.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps wooden boats buoyant when water reaches above the hull.
 *
 * <p>Vanilla treats fully submerged source water and non-source flowing water
 * as loss-of-control states which sink the boat and eventually eject its
 * passengers. Reusing the ordinary in-water state retains vanilla's
 * depth-based surface correction while leaving fluid current vectors,
 * including the downward pull of falling water, untouched.</p>
 *
 * <p>The horizontal body is also narrowed for block movement so a centered
 * boat can pass through a one-block channel. Vanilla's wider entity query and
 * auto-boarding width threshold are retained independently.</p>
 */
@Mixin(Boat.class)
public abstract class BoatMixin
{
    @Unique
    private static final float TFE_COMPACT_BOAT_WIDTH = 0.9F;

    @Inject(
        method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V",
        at = @At("TAIL")
    )
    private void tfe$applyCompactBoatDimensions(EntityType<? extends Boat> entityType, Level level, CallbackInfo ci)
    {
        ((Boat) (Object) this).refreshDimensions();
    }

    @Inject(method = "getStatus", at = @At("RETURN"), cancellable = true)
    private void tfe$keepSubmergedBoatsBuoyant(CallbackInfoReturnable<Boat.Status> cir)
    {
        final Boat.Status status = cir.getReturnValue();
        if (status == Boat.Status.UNDER_WATER || status == Boat.Status.UNDER_FLOWING_WATER)
        {
            cir.setReturnValue(Boat.Status.IN_WATER);
        }
    }

    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/phys/AABB;inflate(DDD)Lnet/minecraft/world/phys/AABB;"
        )
    )
    private AABB tfe$preserveEntityInteractionBounds(AABB bounds, double x, double y, double z)
    {
        final double registeredWidth = ((Boat) (Object) this).getType().getDimensions().width;
        final double widthCompensation = Math.max(0D, (registeredWidth - TFE_COMPACT_BOAT_WIDTH) * 0.5D);
        return bounds.inflate(x + widthCompensation, y, z + widthCompensation);
    }

    @Inject(method = "hasEnoughSpaceFor", at = @At("HEAD"), cancellable = true)
    private void tfe$preserveAutoBoardingWidth(Entity entity, CallbackInfoReturnable<Boolean> cir)
    {
        final Boat boat = (Boat) (Object) this;
        cir.setReturnValue(entity.getBbWidth() < boat.getType().getDimensions().width);
    }
}
