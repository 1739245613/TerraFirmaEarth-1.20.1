package com.newterraearth.tfe.event;

import java.io.IOException;
import java.nio.file.Path;

import com.mojang.logging.LogUtils;
import com.newterraearth.tfe.NewTerraEarthMod;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.resource.PathPackResources;

public final class NTEBuiltinPackEvents
{
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    private static final String AFC_MOD_ID = "afc";
    private static final String OVERRIDE_PACK_PATH = "tfe_server_data_overrides";
    private static final String OVERRIDE_PACK_ID = "tfe_afc_forest_overrides";

    private NTEBuiltinPackEvents()
    {
    }

    public static void init(IEventBus modBus)
    {
        modBus.addListener(NTEBuiltinPackEvents::onPackFinder);
    }

    private static void onPackFinder(AddPackFindersEvent event)
    {
        if (event.getPackType() != PackType.SERVER_DATA || !ModList.get().isLoaded(AFC_MOD_ID))
        {
            return;
        }

        try
        {
            final Path resourcePath = ModList.get().getModFileById(NewTerraEarthMod.MOD_ID).getFile().findResource(OVERRIDE_PACK_PATH);
            try (PathPackResources pack = new PathPackResources(OVERRIDE_PACK_PATH, true, resourcePath))
            {
                final PackMetadataSection metadata = pack.getMetadataSection(PackMetadataSection.TYPE);
                if (metadata != null)
                {
                    LOGGER.info("Injecting TerraFirmaEarth AFC forest override pack");
                    event.addRepositorySource(consumer ->
                        consumer.accept(Pack.readMetaAndCreate(
                            OVERRIDE_PACK_ID,
                            Component.literal("TerraFirmaEarth AFC Forest Overrides"),
                            true,
                            id -> pack,
                            PackType.SERVER_DATA,
                            Pack.Position.TOP,
                            PackSource.BUILT_IN
                        ))
                    );
                }
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
