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

public class NTEJerboaModel extends HierarchicalAnimatedModel<Pest>
{
    public static LayerDefinition createBodyLayer()
    {
        final MeshDefinition meshdefinition = new MeshDefinition();
        final PartDefinition partdefinition = meshdefinition.getRoot();

        final PartDefinition wholebody = partdefinition.addOrReplaceChild("wholebody", CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 2.0F));
        final PartDefinition legs = wholebody.addOrReplaceChild("legs", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        final PartDefinition footLb = legs.addOrReplaceChild("foot_lb", CubeListBuilder.create(), PartPose.offset(1.1F, 0.0F, -0.1F));
        footLb.addOrReplaceChild("Body_r1", CubeListBuilder.create().texOffs(14, 15).addBox(1.5F, -0.1407F, -0.7375F, 1.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.1F, 1.0F, 0.1F, -1.0036F, 0.0F, 0.0F));
        final PartDefinition one = footLb.addOrReplaceChild("one", CubeListBuilder.create(), PartPose.offset(-0.1F, 5.0F, -1.9F));
        one.addOrReplaceChild("Body_r2", CubeListBuilder.create().texOffs(22, 5).addBox(-0.5F, 0.044F, -1.035F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.5272F, 0.0F, 0.0F));

        final PartDefinition footRb = legs.addOrReplaceChild("foot_rb", CubeListBuilder.create(), PartPose.offset(-1.1F, 0.0F, -0.1F));
        footRb.addOrReplaceChild("Body_r3", CubeListBuilder.create().texOffs(14, 15).addBox(1.5F, -0.1407F, -0.7375F, 1.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.9F, 1.0F, 0.1F, -1.0036F, 0.0F, 0.0F));
        final PartDefinition two = footRb.addOrReplaceChild("two", CubeListBuilder.create(), PartPose.offset(0.1F, 5.0F, -1.9F));
        two.addOrReplaceChild("Body_r4", CubeListBuilder.create().texOffs(18, 5).addBox(-0.5F, 0.044F, -1.035F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.5272F, 0.0F, 0.0F));

        final PartDefinition part = wholebody.addOrReplaceChild("part", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        final PartDefinition belly = part.addOrReplaceChild("belly", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        belly.addOrReplaceChild("Body_r5", CubeListBuilder.create().texOffs(0, 10).addBox(-1.5F, -3.3F, -1.0F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.0F, -3.0F, -0.3054F, 0.0F, 0.0F));

        final PartDefinition tail = part.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        tail.addOrReplaceChild("tail_r1", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -4.4F, 2.9F, 3.0F, 4.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.0F, -3.0F, -0.5672F, 0.0F, 0.0F));

        final PartDefinition head = part.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, -1.0F, -3.0F));
        head.addOrReplaceChild("Body_r6", CubeListBuilder.create().texOffs(0, 17).addBox(-1.5F, -3.3F, -3.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 2.0F, 0.0F, -0.3054F, 0.0F, 0.0F));

        final PartDefinition nose = head.addOrReplaceChild("nose", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, -2.0F));
        nose.addOrReplaceChild("nose_r1", CubeListBuilder.create().texOffs(18, 3).addBox(-0.5F, -0.4F, -0.8F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.3054F, 0.0F, 0.0F));

        final PartDefinition ears = head.addOrReplaceChild("ears", CubeListBuilder.create(), PartPose.offset(0.0F, -1.0F, -1.0F));
        ears.addOrReplaceChild("Ears_r1", CubeListBuilder.create().texOffs(14, 10).addBox(-3.5F, -4.3F, 0.1F, 7.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.3491F, 0.0F, 0.0F));

        part.addOrReplaceChild("foot_lf", CubeListBuilder.create().texOffs(10, 17).addBox(-1.0F, 0.0F, -1.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.1F, 1.0F, -2.2F, -1.1345F, 0.0F, 0.0F));
        part.addOrReplaceChild("foot_rf", CubeListBuilder.create().texOffs(18, 0).addBox(0.0F, 0.0F, -1.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.1F, 1.0F, -2.2F, -1.1345F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    private final ModelPart head;
    private final ModelPart tail;
    private final ModelPart footLb;
    private final ModelPart footRb;
    private final ModelPart footLf;
    private final ModelPart footRf;

    public NTEJerboaModel(ModelPart root)
    {
        super(root);
        final ModelPart wholebody = root.getChild("wholebody");
        final ModelPart legs = wholebody.getChild("legs");
        final ModelPart part = wholebody.getChild("part");
        this.footLb = legs.getChild("foot_lb");
        this.footRb = legs.getChild("foot_rb");
        this.tail = part.getChild("tail");
        this.head = part.getChild("head");
        this.footLf = part.getChild("foot_lf");
        this.footRf = part.getChild("foot_rf");
    }

    @Override
    public void setupAnim(Pest entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        final float walk = Mth.cos(limbSwing * 1.2F) * 0.6F * limbSwingAmount;
        this.head.xRot += headPitch * Constants.DEG_TO_RAD;
        this.head.yRot = netHeadYaw * Constants.DEG_TO_RAD;
        this.tail.yRot = Mth.cos(ageInTicks * 0.15F) * 0.1F;
        this.footLf.xRot += walk;
        this.footRf.xRot -= walk;
        this.footLb.xRot -= walk;
        this.footRb.xRot += walk;
    }
}
