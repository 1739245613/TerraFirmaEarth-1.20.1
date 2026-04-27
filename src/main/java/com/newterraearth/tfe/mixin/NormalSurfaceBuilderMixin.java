package com.newterraearth.tfe.mixin;

import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.SurfaceState;
import net.dries007.tfc.world.surface.SurfaceStates;
import net.dries007.tfc.world.surface.builder.NormalSurfaceBuilder;

import com.newterraearth.tfe.world.surface.NTESurfaceStates;

@Mixin(value = NormalSurfaceBuilder.class, remap = false)
public abstract class NormalSurfaceBuilderMixin
{
    @Shadow @Final private int subsurfaceMinDepth;

    /**
     * @author Codex
     * @reason Use 1.21-style climate-sensitive default surface states on 1.20.
     */
    @Overwrite(remap = false)
    public void buildSurface(SurfaceBuilderContext context, int startY, int endY)
    {
        buildSurface(context, startY, endY, NTESurfaceStates.TOP_GRASS_TO_GRAVEL, NTESurfaceStates.MID_DIRT_TO_GRAVEL, NTESurfaceStates.UNDER_GRAVEL);
    }

    /**
     * @author Codex
     * @reason Default underwater layers also follow the 1.21 gravel-first profile.
     */
    @Overwrite(remap = false)
    public void buildSurface(SurfaceBuilderContext context, int startY, int endY, SurfaceState topState, SurfaceState midState, SurfaceState underState)
    {
        buildSurface(context, startY, endY, topState, midState, underState, SurfaceStates.GRAVEL, SurfaceStates.GRAVEL);
    }

    /**
     * @author Codex
     * @reason Backport the 1.21 normal-surface depth behavior while keeping the 1.20 API.
     */
    @Overwrite(remap = false)
    public void buildSurface(SurfaceBuilderContext context, int startY, int endY, SurfaceState topState, SurfaceState midState, SurfaceState underState, SurfaceState underWaterState, SurfaceState thinUnderWaterState)
    {
        int surfaceDepth = -1;
        int surfaceY = 0;
        boolean underwaterLayer = false;
        boolean firstLayer = false;
        SurfaceState surfaceState = SurfaceStates.RAW;

        for (int y = startY; y >= endY; --y)
        {
            final BlockState stateAt = context.getBlockState(y);
            if (stateAt.isAir())
            {
                surfaceDepth = -1;
            }
            else if (context.isDefaultBlock(stateAt))
            {
                if (surfaceDepth == -1)
                {
                    surfaceY = y;
                    firstLayer = true;
                    if (y < context.getSeaLevel() - 1)
                    {
                        surfaceDepth = context.calculateAltitudeSlopeSurfaceDepth(surfaceY, 5, -1);
                        if (surfaceDepth < -1)
                        {
                            surfaceDepth = 0;
                        }
                        else if (surfaceDepth == -1)
                        {
                            surfaceDepth = 0;
                            context.setBlockState(y, thinUnderWaterState);
                        }
                        else
                        {
                            context.setBlockState(y, underWaterState);
                        }
                        surfaceState = underWaterState;
                        underwaterLayer = true;
                    }
                    else
                    {
                        surfaceDepth = context.calculateAltitudeSlopeSurfaceDepth(surfaceY, 5, subsurfaceMinDepth);
                        if (surfaceDepth < -1)
                        {
                            surfaceDepth = 0;
                        }
                        else if (surfaceDepth == -1)
                        {
                            surfaceDepth = 0;
                            context.setBlockState(y, underState);
                        }
                        else
                        {
                            context.setBlockState(y, topState);
                        }
                        surfaceState = midState;
                        underwaterLayer = false;
                    }
                }
                else if (surfaceDepth > 0)
                {
                    surfaceDepth--;
                    context.setBlockState(y, surfaceState);
                    if (surfaceDepth == 0 && firstLayer)
                    {
                        firstLayer = false;
                        surfaceDepth = context.calculateAltitudeSlopeSurfaceDepth(surfaceY, 5, 0);
                        surfaceState = underwaterLayer ? thinUnderWaterState : underState;
                    }
                }
            }
        }
    }
}
