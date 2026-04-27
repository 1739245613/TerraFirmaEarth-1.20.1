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

public class NTEMongooseModel extends HierarchicalAnimatedModel<Pest>
{
    public static LayerDefinition createBodyLayer()
    {
        final MeshDefinition meshdefinition = new MeshDefinition();
        final PartDefinition partdefinition = meshdefinition.getRoot();

        final PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 17.0F, 0.0F));
        final PartDefinition upperpart = body.addOrReplaceChild("upperpart", CubeListBuilder.create(), PartPose.offset(0.0F, 2.0F, 5.0F));

        upperpart.addOrReplaceChild("leg_q", CubeListBuilder.create().texOffs(22, 26).addBox(-1.1F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.0F, 0.0F, -9.0F));
        upperpart.addOrReplaceChild("leg_w", CubeListBuilder.create().texOffs(22, 26).addBox(-0.9F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(1.0F, 0.0F, -9.0F));

        final PartDefinition tailQ = upperpart.addOrReplaceChild("tail_q", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -2.0F, 2.0F, 0.5061F, 0.0F, 0.0F));
        final PartDefinition tailW = tailQ.addOrReplaceChild("tail_w", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -0.095F, -1.234F, -0.48F, 0.0F, 0.0F));
        tailW.addOrReplaceChild("tail_r1", CubeListBuilder.create().texOffs(0, 18).addBox(-1.0F, -8.0F, -6.0F, 2.0F, 2.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 9.0F, 2.0F, -0.5236F, 0.0F, 0.0F));
        final PartDefinition tailE = tailW.addOrReplaceChild("tail_e", CubeListBuilder.create(), PartPose.offset(0.0F, 3.875F, 8.35F));
        tailE.addOrReplaceChild("tail_r2", CubeListBuilder.create().texOffs(0, 29).addBox(-0.5F, -0.8695F, -0.9914F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.125F, -0.35F, 1.4399F, 0.0F, 0.0F));

        final PartDefinition head = upperpart.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, -2.0F, -11.0F));
        final PartDefinition headQ = head.addOrReplaceChild("head_q", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        headQ.addOrReplaceChild("head_r1", CubeListBuilder.create().texOffs(22, 18).addBox(-2.5F, -2.0F, -3.0F, 5.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, -1.0F, -1.5708F, 0.0F, 0.0F));
        headQ.addOrReplaceChild("head_r2", CubeListBuilder.create().texOffs(4, 29).addBox(-1.0F, 7.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, -12.0F, 1.5708F, 0.0F, 0.0F));

        final PartDefinition rightEar = headQ.addOrReplaceChild("right_ear", CubeListBuilder.create(), PartPose.offset(-1.0F, -1.0F, -1.0F));
        rightEar.addOrReplaceChild("right_ear_r1", CubeListBuilder.create().texOffs(12, 29).addBox(-1.2065F, -0.7767F, -0.3508F, 2.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.5F, -1.0F, 1.4F, -0.0568F, -0.3827F, 0.1165F));
        final PartDefinition leftEar = headQ.addOrReplaceChild("left_ear", CubeListBuilder.create(), PartPose.offset(1.0F, -2.0F, -1.0F));
        leftEar.addOrReplaceChild("left_ear_r1", CubeListBuilder.create().texOffs(16, 29).addBox(-0.7935F, -0.7767F, -0.3508F, 2.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.5F, 0.0F, 1.4F, -0.0568F, 0.3827F, -0.1165F));

        upperpart.addOrReplaceChild("belly", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.0F, -6.0F, 4.0F, 5.0F, 13.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -1.0F, -5.0F));
        body.addOrReplaceChild("leg_e", CubeListBuilder.create().texOffs(22, 26).addBox(-0.9F, -1.0F, 0.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(1.0F, 3.0F, 4.0F));
        body.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(22, 26).addBox(-1.1F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.0F, 2.0F, 5.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    private final ModelPart head;
    private final ModelPart legQ;
    private final ModelPart legW;
    private final ModelPart legE;
    private final ModelPart legR;
    private final ModelPart tailQ;
    private final ModelPart tailE;

    public NTEMongooseModel(ModelPart root)
    {
        super(root);
        final ModelPart body = root.getChild("body");
        final ModelPart upperpart = body.getChild("upperpart");
        this.head = upperpart.getChild("head");
        this.legQ = upperpart.getChild("leg_q");
        this.legW = upperpart.getChild("leg_w");
        this.tailQ = upperpart.getChild("tail_q");
        this.tailE = this.tailQ.getChild("tail_w").getChild("tail_e");
        this.legE = body.getChild("leg_e");
        this.legR = body.getChild("leg_r");
    }

    @Override
    public void setupAnim(Pest entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        final float walk = Mth.cos(limbSwing * 1.1F) * 0.65F * limbSwingAmount;
        this.head.xRot += headPitch * Constants.DEG_TO_RAD;
        this.head.yRot = netHeadYaw * Constants.DEG_TO_RAD;
        this.legQ.xRot += walk;
        this.legW.xRot -= walk;
        this.legE.xRot -= walk;
        this.legR.xRot += walk;
        this.tailQ.yRot = Mth.cos(ageInTicks * 0.15F) * 0.15F;
        this.tailE.yRot = -this.tailQ.yRot * 1.5F;
    }
}
