package com.newterraearth.tfe.client;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.fml.ModList;

final class NTEJourneyMapCompat
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static boolean installed;

    private NTEJourneyMapCompat()
    {
    }

    static void install()
    {
        if (installed || !ModList.get().isLoaded("journeymap"))
        {
            return;
        }

        try
        {
            final Class<?> delegateClass = Class.forName("journeymap.client.mod.ModBlockDelegate");
            final Object delegate = delegateClass.getField("INSTANCE").get(null);
            final Method reset = delegateClass.getMethod("reset");
            reset.invoke(delegate);

            final Class<?> handlerClass = Class.forName("journeymap.client.mod.impl.TerraFirmaCraft");
            final Constructor<?> constructor = handlerClass.getDeclaredConstructor();
            constructor.setAccessible(true);

            getMap(delegateClass, delegate, "handlerClasses").put("tfe", handlerClass);
            getMap(delegateClass, delegate, "handlers").put("tfe", constructor.newInstance());

            installed = true;
            LOGGER.info("Installed JourneyMap TFC-style block handler for tfe blocks");
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            LOGGER.warn("Failed to install JourneyMap compatibility for tfe blocks", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getMap(Class<?> delegateClass, Object delegate, String fieldName) throws ReflectiveOperationException
    {
        final Field field = delegateClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (Map<String, Object>) field.get(delegate);
    }
}
