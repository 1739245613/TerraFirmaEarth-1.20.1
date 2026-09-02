/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.newterraearth.tfe.client.render.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import net.dries007.tfc.client.model.entity.HierarchicalAnimatedModel;
import net.dries007.tfc.util.Helpers;

import com.newterraearth.tfe.common.entity.NTEBactrianCamel;

/** Dedicated renderer matching the TFC 4.2.x Bactrian model scale. */
public final class NTEBactrianCamelRenderer extends MobRenderer<NTEBactrianCamel, HierarchicalAnimatedModel<NTEBactrianCamel>>
{
    private final ResourceLocation young;
    private final ResourceLocation old;
    private final ResourceLocation saddled;
    private final ResourceLocation oldSaddled;

    public NTEBactrianCamelRenderer(EntityRendererProvider.Context context, HierarchicalAnimatedModel<NTEBactrianCamel> model, float shadow)
    {
        super(context, model, shadow);
        this.young = Helpers.animalTexture("bactrian_camel_young");
        this.old = Helpers.animalTexture("bactrian_camel_old");
        this.saddled = Helpers.animalTexture("bactrian_camel_saddle");
        this.oldSaddled = Helpers.animalTexture("bactrian_camel_old_saddle");
    }

    @Override
    public ResourceLocation getTextureLocation(NTEBactrianCamel entity)
    {
        if (entity.isSaddled())
        {
            return entity.isBaby() ? saddled : oldSaddled;
        }
        return entity.isBaby() ? young : old;
    }
}
