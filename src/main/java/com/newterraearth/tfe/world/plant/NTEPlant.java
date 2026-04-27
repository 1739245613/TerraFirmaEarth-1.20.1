package com.newterraearth.tfe.world.plant;

import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.BlockPathTypes;

import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.TFCBlockStateProperties;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.plant.BodyPlantBlock;
import net.dries007.tfc.common.blocks.plant.CreepingPlantBlock;
import net.dries007.tfc.common.blocks.plant.EpiphytePlantBlock;
import net.dries007.tfc.common.blocks.plant.FloatingWaterPlantBlock;
import net.dries007.tfc.common.blocks.plant.PlantBlock;
import net.dries007.tfc.common.blocks.plant.ShortGrassBlock;
import net.dries007.tfc.common.blocks.plant.TFCTallGrassBlock;
import net.dries007.tfc.util.calendar.Month;
import net.dries007.tfc.util.registry.RegistryPlant;

import com.newterraearth.tfe.common.NTEBlocks;
import com.newterraearth.tfe.common.item.FuelBlockItem;
import com.newterraearth.tfe.common.block.plant.NTEBambooSaplingBlock;
import com.newterraearth.tfe.common.block.plant.NTEBambooStalkBlock;
import com.newterraearth.tfe.common.block.plant.NTECactusBedBlock;
import com.newterraearth.tfe.common.block.plant.NTECreepingWaterPlantBlock;
import com.newterraearth.tfe.common.block.plant.NTEPassableCactusBlock;
import com.newterraearth.tfe.common.block.plant.NTEPlantBlock;
import com.newterraearth.tfe.common.block.plant.NTERotatableWaterPlantBlock;
import com.newterraearth.tfe.common.block.plant.NTETallShrubBlock;

public enum NTEPlant implements RegistryPlant
{
    ANEMONE_GREEN(NTEPlantType.OCEAN_CREEPING),
    ANEMONE_LARGE_ORANGE(NTEPlantType.OCEAN_ROTATABLE),
    ANEMONE_LARGE_PURPLE(NTEPlantType.OCEAN_ROTATABLE),
    ANEMONE_PURPLE(NTEPlantType.OCEAN_CREEPING),
    AZALEA(NTEPlantType.TALL_SHRUB),
    BARNACLES(NTEPlantType.OCEAN_ROCK_CREEPING),
    BEAR_GRASS(NTEPlantType.TALL_GRASS),
    BIRD_NEST_FERN(NTEPlantType.PERCHED_EPIPHYTE),
    BUTTERCUP(NTEPlantType.FLOWERBED, NTEPlantColor.GRASS),
    CORNFLOWER(NTEPlantType.STANDARD),
    CYCAD(NTEPlantType.TWISTING_SOLID_TOP, NTEPlantColor.FOLIAGE, false, true),
    CYCAD_PLANT(NTEPlantType.TWISTING_SOLID, NTEPlantColor.FOLIAGE, false, false),
    DRY_GRASS(NTEPlantType.DRY),
    EDELWEISS(NTEPlantType.STANDARD),
    ELEGANT_SUNBURST_LICHEN(NTEPlantType.CREEPING_STONE),
    FAN_PALM(NTEPlantType.TALL_GRASS),
    FLAME_VINE(NTEPlantType.WEEPING_TOP),
    FLAME_VINE_PLANT(NTEPlantType.WEEPING, NTEPlantColor.NONE, false, false),
    GOLDEN_BAMBOO(NTEPlantType.BAMBOO),
    GOLDEN_BAMBOO_SAPLING(NTEPlantType.BAMBOO_SAPLING, NTEPlantColor.NONE, false, false),
    KINNIKINNICK(NTEPlantType.SHORT_SHRUB),
    MOSS_CAMPION(NTEPlantType.DRY),
    MOUNTAIN_HULLWORT(NTEPlantType.TALL_SHRUB),
    MUSSELS(NTEPlantType.OCEAN_ROCK_CREEPING),
    PALASH(NTEPlantType.TALL_SHRUB),
    PENWORTEL(NTEPlantType.SHRUB),
    PRICKLY_PEAR(NTEPlantType.PASSABLE_CACTUS),
    PRICKLY_PEAR_PURPLE(NTEPlantType.PASSABLE_CACTUS),
    PURPLE_WATER_LILY(NTEPlantType.FLOATING_FRESH, NTEPlantColor.GRASS),
    QANTU(NTEPlantType.SHRUB),
    RAMIREZELLA(NTEPlantType.EPIPHYTE),
    RAMUNDA(NTEPlantType.STANDARD),
    RED_OAT_GRASS(NTEPlantType.SHORT_GRASS, NTEPlantColor.TALL_GRASS, true, true),
    SHAWIASH(NTEPlantType.SHRUB),
    SILKEN_PINCUSHION_CACTUS(NTEPlantType.CACTUS_BED),
    SILVER_BROMELIAD(NTEPlantType.PERCHED_EPIPHYTE),
    STARFISH(NTEPlantType.OCEAN_ROTATABLE),
    SUNFLOWER(NTEPlantType.TALL_GRASS),
    TANK_BROMELIAD(NTEPlantType.PERCHED_EPIPHYTE, NTEPlantColor.WATER),
    WHITE_WATER_LILY(NTEPlantType.FLOATING_FRESH, NTEPlantColor.GRASS),
    YELLOW_SAXIFRAGE(NTEPlantType.FLOWERBED, NTEPlantColor.GRASS),
    YELLOW_WATER_LILY(NTEPlantType.FLOATING_FRESH, NTEPlantColor.GRASS);

    private final NTEPlantType type;
    private final NTEPlantColor color;
    private final boolean itemTinted;
    private final boolean needsItem;

    NTEPlant(NTEPlantType type)
    {
        this(type, NTEPlantColor.NONE, false, true);
    }

    NTEPlant(NTEPlantType type, NTEPlantColor color)
    {
        this(type, color, false, true);
    }

    NTEPlant(NTEPlantType type, NTEPlantColor color, boolean itemTinted, boolean needsItem)
    {
        this.type = type;
        this.color = color;
        this.itemTinted = itemTinted;
        this.needsItem = needsItem;
    }

    public String serializedName()
    {
        return name().toLowerCase(Locale.ROOT);
    }

    public String id()
    {
        return "plant/" + serializedName();
    }

    public Block create()
    {
        return switch (type)
        {
            case STANDARD -> PlantBlock.create(this, fire(nonSolid()).offsetType(BlockBehaviour.OffsetType.XZ));
            case SHORT_GRASS -> ShortGrassBlock.create(this, fire(nonSolid()).offsetType(BlockBehaviour.OffsetType.XZ));
            case SHORT_SHRUB -> NTEPlantBlock.createShortShrub(this, fire(nonSolid()));
            case SHRUB -> NTEPlantBlock.createShrub(this, fire(nonSolid()));
            case FLOWERBED -> NTEPlantBlock.createFlowerbed(this, fire(nonSolid()));
            case CACTUS_BED -> NTECactusBedBlock.createBarrel(this, fire(solid().strength(0.25F).sound(SoundType.WOOL).offsetType(BlockBehaviour.OffsetType.XZ).dynamicShape()));
            case PASSABLE_CACTUS -> NTEPassableCactusBlock.create(this, fire(nonSolid().strength(0.25F).sound(SoundType.WOOL)).pathType(BlockPathTypes.DAMAGE_OTHER));
            case DRY -> PlantBlock.createDry(this, fire(nonSolid().offsetType(BlockBehaviour.OffsetType.XZ)));
            case CREEPING_STONE -> CreepingPlantBlock.createStone(this, fire(nonSolid().hasPostProcess(TFCBlocks::always)));
            case EPIPHYTE -> EpiphytePlantBlock.create(this, fire(nonSolid().hasPostProcess(TFCBlocks::always)));
            case PERCHED_EPIPHYTE -> NTEPlantBlock.createPerchedEpiphyte(this, fire(nonSolid()).offsetType(BlockBehaviour.OffsetType.XZ));
            case TALL_GRASS -> TFCTallGrassBlock.create(this, fire(nonSolid()).offsetType(BlockBehaviour.OffsetType.XZ));
            case TALL_SHRUB -> NTETallShrubBlock.create(this, fire(nonSolid()));
            case WEEPING -> new BodyPlantBlock(fire(nonSolidTallPlant()), transform(), BodyPlantBlock.BODY_SHAPE, Direction.DOWN);
            case WEEPING_TOP -> new net.dries007.tfc.common.blocks.plant.TopPlantBlock(fire(nonSolidTallPlant()), transform(), Direction.DOWN, BodyPlantBlock.WEEPING_SHAPE);
            case TWISTING_SOLID -> new BodyPlantBlock(fire(solidTallPlant()), transform(), BodyPlantBlock.BODY_SHAPE, Direction.UP);
            case TWISTING_SOLID_TOP -> new net.dries007.tfc.common.blocks.plant.TopPlantBlock(fire(solidTallPlant()), transform(), Direction.UP, BodyPlantBlock.TWISTING_SHAPE);
            case OCEAN_ROCK_CREEPING -> NTECreepingWaterPlantBlock.createRock(this, TFCBlockStateProperties.SALT_WATER, ExtendedProperties.of(nonSolid().sound(SoundType.BASALT)));
            case OCEAN_CREEPING -> NTECreepingWaterPlantBlock.create(this, TFCBlockStateProperties.SALT_WATER, ExtendedProperties.of(nonSolid().sound(SoundType.BASALT)));
            case OCEAN_ROTATABLE -> NTERotatableWaterPlantBlock.create(this, TFCBlockStateProperties.SALT_WATER, ExtendedProperties.of(nonSolid().sound(SoundType.SLIME_BLOCK)));
            case FLOATING_FRESH -> FloatingWaterPlantBlock.create(this, () -> Fluids.WATER, nonSolid());
            case BAMBOO_SAPLING -> new NTEBambooSaplingBlock(BlockBehaviour.Properties.copy(Blocks.BAMBOO_SAPLING), transform());
            case BAMBOO -> new NTEBambooStalkBlock(BlockBehaviour.Properties.copy(Blocks.BAMBOO), transform());
        };
    }

    @Nullable
    public Function<Block, BlockItem> createBlockItem(Item.Properties properties)
    {
        if (!needsItem)
        {
            return null;
        }
        if (type == NTEPlantType.FLOATING_FRESH)
        {
            return block -> new PlaceOnWaterBlockItem(block, properties);
        }
        if (this == GOLDEN_BAMBOO)
        {
            return block -> new FuelBlockItem(block, properties, Items.BAMBOO);
        }
        return block -> new BlockItem(block, properties);
    }

    public boolean isBlockTinted()
    {
        return color != NTEPlantColor.NONE;
    }

    public boolean isTallGrass()
    {
        return color == NTEPlantColor.TALL_GRASS;
    }

    public boolean isFoliage()
    {
        return color == NTEPlantColor.FOLIAGE;
    }

    public boolean usesWaterTint()
    {
        return color == NTEPlantColor.WATER;
    }

    public boolean isItemTinted()
    {
        return itemTinted;
    }

    @Override
    public int stageFor(Month month)
    {
        return 0;
    }

    @Nullable
    @Override
    public IntegerProperty getStageProperty()
    {
        return null;
    }

    private Supplier<? extends Block> transform()
    {
        return NTEBlocks.getPlant(switch (this)
        {
            case FLAME_VINE -> FLAME_VINE_PLANT;
            case FLAME_VINE_PLANT -> FLAME_VINE;
            case CYCAD -> CYCAD_PLANT;
            case CYCAD_PLANT -> CYCAD;
            case GOLDEN_BAMBOO -> GOLDEN_BAMBOO_SAPLING;
            case GOLDEN_BAMBOO_SAPLING -> GOLDEN_BAMBOO;
            default -> throw new IllegalStateException("No paired plant for " + serializedName());
        });
    }

    private static BlockBehaviour.Properties solid()
    {
        return BlockBehaviour.Properties.of().instabreak().noOcclusion().sound(SoundType.GRASS).randomTicks().pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties nonSolid()
    {
        return solid().replaceable().noCollission();
    }

    private static BlockBehaviour.Properties solidTallPlant()
    {
        return BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).instabreak().noOcclusion().randomTicks().sound(SoundType.WEEPING_VINES).pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties nonSolidTallPlant()
    {
        return solidTallPlant().noCollission();
    }

    private static ExtendedProperties fire(BlockBehaviour.Properties properties)
    {
        return ExtendedProperties.of(properties).flammable(60, 30);
    }
}
