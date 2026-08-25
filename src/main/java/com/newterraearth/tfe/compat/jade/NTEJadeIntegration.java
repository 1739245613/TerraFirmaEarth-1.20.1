package com.newterraearth.tfe.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.dries007.tfc.common.blockentities.CropBlockEntity;
import net.dries007.tfc.common.blocks.crop.CropBlock;

import com.newterraearth.tfe.NewTerraEarthMod;
import com.newterraearth.tfe.world.crop.NTECropTemperatureAccess;
import com.newterraearth.tfe.world.crop.NTECropTemperatureModel;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public final class NTEJadeIntegration implements snownee.jade.api.IWailaPlugin
{
    private static final String PRESSURE_TAG = "tfeTempStress";
    private static final ResourceLocation PRESSURE_UID = new ResourceLocation(NewTerraEarthMod.MOD_ID, "crop_temperature_pressure");

    @Override
    public void register(IWailaCommonRegistration registration)
    {
        registration.registerBlockDataProvider(new TemperaturePressureDataProvider(), CropBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration)
    {
        registration.registerBlockComponent(new TemperaturePressureProvider(), CropBlock.class);
    }

    private static final class TemperaturePressureProvider implements IBlockComponentProvider
    {
        @Override
        public ResourceLocation getUid()
        {
            return PRESSURE_UID;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config)
        {
            final CompoundTag serverData = accessor.getServerData();
            if (serverData == null || !serverData.contains(PRESSURE_TAG))
            {
                return;
            }

            final int pressure = Mth.clamp(serverData.getByte(PRESSURE_TAG), 0, NTECropTemperatureModel.STRESS_LIMIT);
            tooltip.add(Component.translatable("tfe.jade.crop_health", NTECropTemperatureModel.healthPercent(pressure)));
        }
    }

    private static final class TemperaturePressureDataProvider implements IServerDataProvider<BlockAccessor>
    {
        @Override
        public ResourceLocation getUid()
        {
            return PRESSURE_UID;
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor)
        {
            final BlockEntity blockEntity = accessor.getBlockEntity();
            if (blockEntity instanceof NTECropTemperatureAccess stress)
            {
                data.putByte(PRESSURE_TAG, (byte) Mth.clamp(stress.tfe$getTemperatureStress(), 0, NTECropTemperatureModel.STRESS_LIMIT));
            }
        }
    }
}
