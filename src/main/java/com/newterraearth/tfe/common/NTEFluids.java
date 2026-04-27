package com.newterraearth.tfe.common;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import net.dries007.tfc.common.blocks.FluidCauldronBlock;
import net.dries007.tfc.common.fluids.ExtendedFluidType;
import net.dries007.tfc.common.fluids.FluidRegistryObject;
import net.dries007.tfc.common.fluids.FluidTypeClientProperties;
import net.dries007.tfc.common.fluids.MixingFluid;
import net.dries007.tfc.common.fluids.TFCFluids;
import net.dries007.tfc.util.CauldronInteractions;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.registry.RegistrationHelpers;

import com.newterraearth.tfe.NewTerraEarthMod;

public final class NTEFluids
{
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, NewTerraEarthMod.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, NewTerraEarthMod.MOD_ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, NewTerraEarthMod.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, NewTerraEarthMod.MOD_ID);

    public static final Map<NTEFluid, FluidRegistryObject<ForgeFlowingFluid>> SIMPLE_FLUIDS = Helpers.mapOfKeys(NTEFluid.class, fluid -> register(
        fluid.getId(),
        properties -> properties
            .block(getBlock(fluid))
            .bucket(getBucket(fluid)),
        waterLike()
            .descriptionId("fluid.tfe." + fluid.getId())
            .canConvertToSource(false),
        new FluidTypeClientProperties(TFCFluids.ALPHA_MASK | fluid.getColor(), TFCFluids.WATER_STILL, TFCFluids.WATER_FLOW, TFCFluids.WATER_OVERLAY, TFCFluids.UNDERWATER_LOCATION),
        MixingFluid.Source::new,
        MixingFluid.Flowing::new
    ));

    public static final Map<NTEFluid, RegistryObject<LiquidBlock>> SIMPLE_FLUID_BLOCKS = Helpers.mapOfKeys(NTEFluid.class, fluid ->
        BLOCKS.register("fluid/" + fluid.getId(), () -> new LiquidBlock(SIMPLE_FLUIDS.get(fluid).source(), BlockBehaviour.Properties.copy(Blocks.WATER).noLootTable()))
    );

    public static final Map<NTEFluid, RegistryObject<FluidCauldronBlock>> FLUID_CAULDRONS = Helpers.mapOfKeys(NTEFluid.class, fluid ->
        BLOCKS.register("cauldron/" + fluid.getId(), () -> new FluidCauldronBlock(BlockBehaviour.Properties.copy(Blocks.CAULDRON)))
    );

    public static final Map<NTEFluid, RegistryObject<BucketItem>> FLUID_BUCKETS = Helpers.mapOfKeys(NTEFluid.class, fluid ->
        ITEMS.register("bucket/" + fluid.getId(), () -> new BucketItem(SIMPLE_FLUIDS.get(fluid).source(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)))
    );

    private NTEFluids()
    {
    }

    public static void register(IEventBus bus)
    {
        FLUID_TYPES.register(bus);
        FLUIDS.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    public static RegistryObject<LiquidBlock> getBlock(NTEFluid fluid)
    {
        return SIMPLE_FLUID_BLOCKS.get(fluid);
    }

    public static RegistryObject<BucketItem> getBucket(NTEFluid fluid)
    {
        return FLUID_BUCKETS.get(fluid);
    }

    public static void registerCauldronInteractions()
    {
        for (NTEFluid fluid : NTEFluid.values())
        {
            CauldronInteractions.registerCauldronBlock(FLUID_CAULDRONS.get(fluid).get(), SIMPLE_FLUIDS.get(fluid).source().get());
            CauldronInteractions.registerForVanillaCauldrons(FLUID_BUCKETS.get(fluid).get(), CauldronInteractions::interactWithBucket);
        }
    }

    private static FluidType.Properties waterLike()
    {
        return FluidType.Properties.create()
            .adjacentPathType(BlockPathTypes.WATER)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
            .canConvertToSource(true)
            .canDrown(true)
            .canExtinguish(true)
            .canHydrate(true)
            .canPushEntity(true)
            .canSwim(true)
            .supportsBoating(true);
    }

    private static <F extends FlowingFluid> FluidRegistryObject<F> register(String name, Consumer<ForgeFlowingFluid.Properties> builder, FluidType.Properties typeProperties, FluidTypeClientProperties clientProperties, Function<ForgeFlowingFluid.Properties, F> sourceFactory, Function<ForgeFlowingFluid.Properties, F> flowingFactory)
    {
        final int index = name.lastIndexOf('/');
        final String flowingName = index == -1 ? "flowing_" + name : name.substring(0, index) + "/flowing_" + name.substring(index + 1);

        return RegistrationHelpers.registerFluid(FLUID_TYPES, FLUIDS, name, name, flowingName, builder, () -> new ExtendedFluidType(typeProperties, clientProperties), sourceFactory, flowingFactory);
    }
}
