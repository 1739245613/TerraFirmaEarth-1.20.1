package com.newterraearth.tfe.compat;

import java.lang.reflect.Method;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;

public final class NTEFirmalifeGreenhouseCompat
{
    public static final String MOD_ID = "firmalife_greenhouse_patch";

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String GREENHOUSE_HELPER_CLASS = "com.g1739.firmalifegreenhousepatch.common.temperature.GreenhouseTemperatureHelper";

    @Nullable private static volatile Method isControlledGreenhouseMethod;
    private static volatile boolean resolved;
    private static volatile boolean available;

    private NTEFirmalifeGreenhouseCompat()
    {
    }

    public static boolean isControlledGreenhouse(Level level, BlockPos pos)
    {
        if (!ModList.get().isLoaded(MOD_ID))
        {
            return false;
        }

        final Method method = resolveMethod();
        if (method == null)
        {
            return false;
        }

        try
        {
            return Boolean.TRUE.equals(method.invoke(null, level, pos));
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            LOGGER.warn("Failed to query Firmalife greenhouse control state", e);
            return false;
        }
    }

    @Nullable
    private static Method resolveMethod()
    {
        if (resolved)
        {
            return available ? isControlledGreenhouseMethod : null;
        }

        synchronized (NTEFirmalifeGreenhouseCompat.class)
        {
            if (resolved)
            {
                return available ? isControlledGreenhouseMethod : null;
            }

            resolved = true;
            try
            {
                final Class<?> helperClass = Class.forName(GREENHOUSE_HELPER_CLASS);
                final Method method = helperClass.getMethod("isControlledGreenhouse", Level.class, BlockPos.class);
                method.setAccessible(true);
                isControlledGreenhouseMethod = method;
                available = true;
            }
            catch (ReflectiveOperationException | RuntimeException e)
            {
                available = false;
                LOGGER.warn("Failed to initialize Firmalife greenhouse compatibility hook", e);
            }
        }

        return available ? isControlledGreenhouseMethod : null;
    }
}
