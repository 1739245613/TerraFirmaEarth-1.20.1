package com.newterraearth.tfe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.dries007.tfc.common.blockentities.CropBlockEntity;

import com.newterraearth.tfe.world.crop.NTECropTemperatureAccess;

@Mixin(value = CropBlockEntity.class, remap = false)
public abstract class CropBlockEntityMixin implements NTECropTemperatureAccess
{
    @Unique private static final String TFE_TEMPERATURE_STRESS = "tfeTempStress";
    @Unique private int tfe$temperatureStress;

    @Inject(method = "loadAdditional", at = @At("TAIL"), remap = false)
    private void tfe$loadTemperatureStress(CompoundTag tag, CallbackInfo ci)
    {
        tfe$temperatureStress = tag.getByte(TFE_TEMPERATURE_STRESS);
    }

    @Inject(method = {"saveAdditional", "m_183515_"}, at = @At("TAIL"), remap = false, require = 1)
    private void tfe$saveTemperatureStress(CompoundTag tag, CallbackInfo ci)
    {
        tfe$writeTemperatureStress(tag);
    }

    @Unique
    private void tfe$writeTemperatureStress(CompoundTag tag)
    {
        if (tfe$temperatureStress > 0)
        {
            tag.putByte(TFE_TEMPERATURE_STRESS, (byte) tfe$temperatureStress);
        }
    }

    @Override
    public int tfe$getTemperatureStress()
    {
        return tfe$temperatureStress;
    }

    @Override
    public void tfe$setTemperatureStress(int stress)
    {
        if (tfe$temperatureStress != stress)
        {
            tfe$temperatureStress = stress;
            final BlockEntity blockEntity = (BlockEntity) (Object) this;
            blockEntity.setChanged();
        }
    }
}
