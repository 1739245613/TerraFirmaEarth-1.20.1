package com.newterraearth.tfe.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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
 * including the downward pull of falling water, untouched. Player-controlled
 * boats also receive extra recovery lift while actively moving forward from a
 * fully submerged state.</p>
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

    @Unique
    private static final double TFE_DRIVEN_SUBMERGED_LIFT = 0.03D;

    @Shadow
    private boolean inputUp;

    @Unique
    private boolean tfe$fullySubmerged;

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
        tfe$fullySubmerged = status == Boat.Status.UNDER_WATER || status == Boat.Status.UNDER_FLOWING_WATER;
        if (tfe$fullySubmerged)
        {
            cir.setReturnValue(Boat.Status.IN_WATER);
        }
    }

    @Inject(method = "controlBoat", at = @At("TAIL"))
    private void tfe$boostDrivenSubmergedLift(CallbackInfo ci)
    {
        final Boat boat = (Boat) (Object) this;
        if (tfe$fullySubmerged && inputUp && boat.getControllingPassenger() instanceof Player)
        {
            boat.setDeltaMovement(boat.getDeltaMovement().add(0D, TFE_DRIVEN_SUBMERGED_LIFT, 0D));
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
