package com.newterraearth.tfe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;

import net.dries007.tfc.common.blocks.devices.BurningLogPileBlock;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blockentities.BurningLogPileBlockEntity;

import com.newterraearth.tfe.common.NTELogPileHelpers;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_AXIS;

@Mixin(value = BurningLogPileBlock.class, remap = false)
public abstract class BurningLogPileBlockMixin extends BaseEntityBlock
{
    @Unique private static final ThreadLocal<Direction.Axis> tfe$lightingAxis = ThreadLocal.withInitial(() -> Direction.Axis.X);

    protected BurningLogPileBlockMixin(ExtendedProperties properties)
    {
        super(properties.properties());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder.add(HORIZONTAL_AXIS).add(NTELogPileHelpers.LOG_PILE_COUNT));
    }

    @Inject(method = "lightLogPile", at = @At("HEAD"), remap = false)
    private static void tfe$rememberLitPileAxis(Level level, BlockPos pos, CallbackInfo ci)
    {
        final BlockState state = level.getBlockState(pos);
        tfe$lightingAxis.set(state.hasProperty(HORIZONTAL_AXIS) ? state.getValue(HORIZONTAL_AXIS) : Direction.Axis.X);
    }

    @Inject(method = "lightLogPile", at = @At("TAIL"), remap = false)
    private static void tfe$copyVisibleLogCount(Level level, BlockPos pos, CallbackInfo ci)
    {
        if (level.getBlockEntity(pos) instanceof BurningLogPileBlockEntity burningPile)
        {
            BlockState state = level.getBlockState(pos);
            if (state.hasProperty(HORIZONTAL_AXIS))
            {
                state = state.setValue(HORIZONTAL_AXIS, tfe$lightingAxis.get());
            }
            level.setBlockAndUpdate(pos, NTELogPileHelpers.withLogCount(state, burningPile.getLogs()));
        }
        tfe$lightingAxis.remove();
    }

    // These inherited Block methods are not TFC targets, so they need production runtime names in the reobf jar.
    @SuppressWarnings("deprecation")
    public VoxelShape m_5909_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return NTELogPileHelpers.getShape(state.getValue(HORIZONTAL_AXIS), NTELogPileHelpers.getVisibleLogCount(state));
    }

    @SuppressWarnings("deprecation")
    public VoxelShape m_7952_(BlockState state, BlockGetter level, BlockPos pos)
    {
        return Shapes.empty();
    }

    @SuppressWarnings("deprecation")
    public boolean m_7923_(BlockState state)
    {
        return true;
    }
}
