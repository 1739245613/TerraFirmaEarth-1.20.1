package com.newterraearth.tfe.client.model.entity;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

import net.dries007.tfc.client.model.entity.HierarchicalAnimatedModel;

import com.newterraearth.tfe.common.entity.NTEArmadillo;

/** 1.20-compatible port of the vanilla 1.21 armadillo model. */
public final class NTEArmadilloModel extends HierarchicalAnimatedModel<NTEArmadillo>
{
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart cube;
    private final ModelPart head;
    private final ModelPart tail;

    public NTEArmadilloModel(ModelPart root)
    {
        super(root);
        this.root = root;
        this.body = root.getChild("body");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.head = body.getChild("head");
        this.tail = body.getChild("tail");
        this.cube = root.getChild("cube");
    }

    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
            .texOffs(0, 20).addBox(-4.0F, -7.0F, -10.0F, 8.0F, 8.0F, 12.0F, new CubeDeformation(0.3F))
            .texOffs(0, 40).addBox(-4.0F, -7.0F, -10.0F, 8.0F, 8.0F, 12.0F, new CubeDeformation(0.0F)),
            PartPose.offset(0.0F, 21.0F, 4.0F));

        body.addOrReplaceChild("tail", CubeListBuilder.create()
            .texOffs(44, 53).addBox(-0.5F, -0.0865F, 0.0933F, 1.0F, 6.0F, 1.0F),
            PartPose.offsetAndRotation(0.0F, -3.0F, 1.0F, 0.5061F, 0.0F, 0.0F));

        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, -2.0F, -11.0F));
        head.addOrReplaceChild("head_cube", CubeListBuilder.create()
            .texOffs(43, 15).addBox(-1.5F, -1.0F, -1.0F, 3.0F, 5.0F, 2.0F),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.3927F, 0.0F, 0.0F));

        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create(), PartPose.offset(-1.0F, -3.0F, 0.0F));
        rightEar.addOrReplaceChild("right_ear_cube", CubeListBuilder.create()
            .texOffs(43, 10).addBox(-2.0F, -3.0F, 0.0F, 2.0F, 5.0F, 0.0F),
            PartPose.offsetAndRotation(-0.5F, 0.0F, -0.6F, 0.1886F, -0.3864F, -0.0718F));

        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create(), PartPose.offset(1.0F, -2.0F, 0.0F));
        leftEar.addOrReplaceChild("left_ear_cube", CubeListBuilder.create()
            .texOffs(47, 10).addBox(0.0F, -3.0F, 0.0F, 2.0F, 5.0F, 0.0F),
            PartPose.offsetAndRotation(0.5F, 1.0F, -0.6F, 0.1886F, 0.3864F, 0.0718F));

        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create()
            .texOffs(51, 31).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
            PartPose.offset(-2.0F, 21.0F, 4.0F));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create()
            .texOffs(42, 31).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
            PartPose.offset(2.0F, 21.0F, 4.0F));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create()
            .texOffs(51, 43).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
            PartPose.offset(-2.0F, 21.0F, -4.0F));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create()
            .texOffs(42, 43).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
            PartPose.offset(2.0F, 21.0F, -4.0F));

        root.addOrReplaceChild("cube", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-5.0F, -10.0F, -6.0F, 10.0F, 10.0F, 10.0F),
            PartPose.offset(0.0F, 24.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(NTEArmadillo entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        final boolean rolled = entity.isRolled();
        body.skipDraw = rolled;
        rightHindLeg.visible = !rolled;
        leftHindLeg.visible = !rolled;
        tail.visible = !rolled;
        cube.visible = rolled;
        if (!rolled)
        {
            head.xRot = Mth.clamp(headPitch, -22.5F, 25.0F) * Mth.DEG_TO_RAD;
            head.yRot = Mth.clamp(netHeadYaw, -32.5F, 32.5F) * Mth.DEG_TO_RAD;
            final float walk = limbSwing * 0.9F;
            final float amount = limbSwingAmount * 0.65F;
            rightHindLeg.xRot = Mth.cos(walk) * amount;
            leftHindLeg.xRot = Mth.cos(walk + Mth.PI) * amount;
        }
    }

    @Override
    public ModelPart root()
    {
        return root;
    }
}
