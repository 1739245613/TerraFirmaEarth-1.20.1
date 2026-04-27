package com.newterraearth.tfe.client.model.entity;

import com.mojang.math.Constants;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

import net.dries007.tfc.client.model.entity.HierarchicalAnimatedModel;
import net.dries007.tfc.common.entities.prey.Pest;

public class NTELemmingModel extends HierarchicalAnimatedModel<Pest>
{
    public static LayerDefinition createBodyLayer()
    {
        final MeshDefinition meshdefinition = new MeshDefinition();
        final PartDefinition partdefinition = meshdefinition.getRoot();

        final PartDefinition wholebody = partdefinition.addOrReplaceChild("wholebody", CubeListBuilder.create(), PartPose.offset(0.0F, 23.0F, 0.0F));
        final PartDefinition legs0 = wholebody.addOrReplaceChild("legs0", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 1.0F));

        final PartDefinition legs1 = legs0.addOrReplaceChild("legs1", CubeListBuilder.create(), PartPose.offset(1.1F, 0.0F, -0.1F));
        legs1.addOrReplaceChild("part_r1", CubeListBuilder.create().texOffs(12, 13).addBox(1.5F, 2.8593F, 1.2625F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.1F, -3.0F, 2.1F, -1.0036F, 0.0F, 0.0F));
        final PartDefinition legs2 = legs1.addOrReplaceChild("legs2", CubeListBuilder.create(), PartPose.offset(-0.1F, 1.0F, 0.1F));
        legs2.addOrReplaceChild("part_r2", CubeListBuilder.create().texOffs(4, 14).addBox(-0.5F, 0.044F, -1.035F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.5272F, 0.0F, 0.0F));

        final PartDefinition legs = legs0.addOrReplaceChild("legs", CubeListBuilder.create(), PartPose.offset(-1.1F, 0.0F, -0.1F));
        legs.addOrReplaceChild("part_r3", CubeListBuilder.create().texOffs(8, 13).addBox(1.5F, 2.8593F, 1.2625F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.9F, -3.0F, 2.1F, -1.0036F, 0.0F, 0.0F));
        final PartDefinition legs3 = legs.addOrReplaceChild("legs3", CubeListBuilder.create(), PartPose.offset(0.1F, 1.0F, 0.1F));
        legs3.addOrReplaceChild("part_r4", CubeListBuilder.create().texOffs(14, 10).addBox(-0.5F, 0.044F, -1.035F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.5272F, 0.0F, 0.0F));

        final PartDefinition upperbody = wholebody.addOrReplaceChild("upperbody", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -1.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        final PartDefinition body = upperbody.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 0.6014F, -0.0926F));
        body.addOrReplaceChild("part_r5", CubeListBuilder.create().texOffs(0, 15).addBox(-0.5F, 0.5558F, 0.4396F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.0F, 2.0F, -0.6109F, 0.0F, 0.0F));
        body.addOrReplaceChild("part_r6", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -3.0F, 1.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, -3.0F, -0.3054F, 0.0F, 0.0F));

        final PartDefinition head = upperbody.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, -0.3986F, -2.0926F));
        head.addOrReplaceChild("part_r7", CubeListBuilder.create().texOffs(10, 7).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.0F, -1.0F, -0.3054F, 0.0F, 0.0F));
        head.addOrReplaceChild("part_r8", CubeListBuilder.create().texOffs(0, 7).addBox(-1.5F, -3.3F, -3.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 2.0F, 1.0F, -0.3054F, 0.0F, 0.0F));

        final PartDefinition nose = head.addOrReplaceChild("nose", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, -1.0F));
        nose.addOrReplaceChild("part_r9", CubeListBuilder.create().texOffs(4, 12).addBox(-0.5F, -0.4F, -0.8F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.1745F, 0.0F, 0.0F));

        upperbody.addOrReplaceChild("legs4", CubeListBuilder.create(), PartPose.offsetAndRotation(1.1F, 0.6014F, -1.2926F, -1.1345F, 0.0F, 0.0F))
            .addOrReplaceChild("part_r10", CubeListBuilder.create().texOffs(10, 10).addBox(0.1F, -1.0F, -3.2F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.1F, 0.5163F, 3.5289F, -0.0436F, 0.0F, 0.0F));
        upperbody.addOrReplaceChild("legs5", CubeListBuilder.create(), PartPose.offsetAndRotation(-1.1F, 0.6014F, -1.2926F, -1.1345F, 0.0F, 0.0F))
            .addOrReplaceChild("part_r11", CubeListBuilder.create().texOffs(0, 12).addBox(-1.1F, -1.0F, -3.2F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.1F, 0.5163F, 3.5289F, -0.0436F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    private final ModelPart head;
    private final ModelPart legs1;
    private final ModelPart legs;
    private final ModelPart legs4;
    private final ModelPart legs5;

    public NTELemmingModel(ModelPart root)
    {
        super(root);
        final ModelPart wholebody = root.getChild("wholebody");
        final ModelPart legs0 = wholebody.getChild("legs0");
        final ModelPart upperbody = wholebody.getChild("upperbody");
        this.legs1 = legs0.getChild("legs1");
        this.legs = legs0.getChild("legs");
        this.head = upperbody.getChild("head");
        this.legs4 = upperbody.getChild("legs4");
        this.legs5 = upperbody.getChild("legs5");
    }

    @Override
    public void setupAnim(Pest entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        final float walk = Mth.cos(limbSwing * 1.2F) * 0.55F * limbSwingAmount;
        this.head.xRot += headPitch * Constants.DEG_TO_RAD;
        this.head.yRot = netHeadYaw * Constants.DEG_TO_RAD;
        this.legs1.xRot += walk;
        this.legs.xRot -= walk;
        this.legs4.xRot -= walk;
        this.legs5.xRot += walk;
    }
}
