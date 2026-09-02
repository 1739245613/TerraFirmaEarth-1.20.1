package com.newterraearth.tfe.common.entity;

import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.Ingredient;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.entities.ai.TFCGroundPathNavigation;

/** 1.20-compatible base for the 4.2.x camel species. */
public abstract class NTECamel extends Camel
{
    private static final TagKey<Item> CAMEL_FOOD = ItemTags.create(new ResourceLocation("tfc", "camel_food"));
    private static final TagKey<Block> CAMEL_FASTER_ON = BlockTags.create(new ResourceLocation("tfc", "camel_faster_on"));

    protected NTECamel(EntityType<? extends Camel> type, Level level)
    {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        // Keep the complete AbstractHorse/Camel attribute set. In particular,
        // Camel's builder supplies the jump-strength attribute that the
        // PlayerRideableJumping path reads when executing a rider jump.
        return Camel.createAttributes();
    }

    @Override
    protected void registerGoals()
    {
        super.registerGoals();
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.4D));
        goalSelector.addGoal(5, new BreedGoal(this, 1.0D));
        goalSelector.addGoal(6, new TemptGoal(this, 1.1D, Ingredient.of(CAMEL_FOOD), false));
        goalSelector.addGoal(7, new RandomStrollGoal(this, 0.7D));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isFood(ItemStack stack)
    {
        return stack.is(CAMEL_FOOD);
    }

    @Override
    public Camel getBreedOffspring(ServerLevel level, AgeableMob other)
    {
        return (Camel) getType().create(level);
    }

    @Override
    protected float getBlockSpeedFactor()
    {
        return level().getBlockState(blockPosition().below()).is(CAMEL_FASTER_ON) ? 1.2F : super.getBlockSpeedFactor();
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level)
    {
        return level.getBlockState(pos.below()).is(TFCTags.Blocks.BUSH_PLANTABLE_ON)
            ? 10.0F
            : level.getPathfindingCostFromLightLevels(pos);
    }

    @Override
    protected PathNavigation createNavigation(Level level)
    {
        return new TFCGroundPathNavigation(this, level);
    }

    @Override
    public boolean isInWall()
    {
        return !level().isClientSide && super.isInWall();
    }

    @Override
    protected void pushEntities()
    {
        if (!level().isClientSide)
        {
            super.pushEntities();
        }
    }

    @Override
    protected float getRiddenSpeed(Player player)
    {
        // TFC's 4.2 camel baseline keeps the vanilla sprint bonus while using
        // the Camel movement attribute. BactrianCamel overrides this with its
        // species-specific 0.0875F bonus below.
        final float sprintBonus = player.isSprinting() && getJumpCooldown() == 0 ? 0.12F : 0.0F;
        return (float) getAttributeValue(Attributes.MOVEMENT_SPEED) + sprintBonus;
    }

    public boolean isBactrian()
    {
        return this instanceof NTEBactrianCamel;
    }
}
