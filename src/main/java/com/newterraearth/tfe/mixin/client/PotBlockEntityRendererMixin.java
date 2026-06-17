package com.newterraearth.tfe.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.dries007.tfc.client.render.blockentity.PotBlockEntityRenderer;
import net.dries007.tfc.common.blockentities.PotBlockEntity;

import com.newterraearth.tfe.common.NTEDevices;

@Mixin(value = PotBlockEntityRenderer.class, remap = false)
public abstract class PotBlockEntityRendererMixin
{
    @Inject(
        method = "render(Lnet/dries007/tfc/common/blockentities/PotBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/common/blockentities/PotBlockEntity;getOutput()Lnet/dries007/tfc/common/recipes/PotRecipe$Output;"
        ),
        remap = false
    )
    private void translateStovePotContents(PotBlockEntity pot, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay, CallbackInfo ci)
    {
        if (pot.getBlockState().is(NTEDevices.STOVE_POT.get()))
        {
            poseStack.translate(0.0D, 0.4375D, 0.0D);
            final Direction facing = pot.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
            switch (facing)
            {
                case NORTH -> poseStack.translate(0.0D, 0.0D, -0.0625D);
                case SOUTH -> poseStack.translate(0.0D, 0.0D, 0.0625D);
                case EAST -> poseStack.translate(0.0625D, 0.0D, 0.0D);
                case WEST -> poseStack.translate(-0.0625D, 0.0D, 0.0D);
                default -> {
                }
            }
        }
    }
}
