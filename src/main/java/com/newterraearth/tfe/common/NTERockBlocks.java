package com.newterraearth.tfe.common;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

import net.dries007.tfc.common.blocks.OreDeposit;
import net.dries007.tfc.common.blocks.SandstoneBlockType;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.rock.Ore;
import net.dries007.tfc.common.blocks.rock.Rock;
import net.dries007.tfc.common.items.PropickItem;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.registry.RegistrationHelpers;
import net.dries007.tfc.world.settings.RockSettings;

import com.newterraearth.tfe.NewTerraEarthMod;

public final class NTERockBlocks
{
    public static final Map<NTERock, RegistryObject<Item>> BRICKS = Helpers.mapOfKeys(NTERock.class, rock ->
        NTEBlocks.ITEMS.register("brick/" + rock.getSerializedName(), () -> new Item(new Item.Properties()))
    );

    public static final Map<NTERock, Map<Rock.BlockType, RegistryObject<Block>>> ROCK_BLOCKS = Helpers.mapOfKeys(NTERock.class, rock ->
        Helpers.mapOfKeys(Rock.BlockType.class, type ->
            register("rock/" + type.getSerializedName() + "/" + rock.getSerializedName(), () -> type.create(rock), block -> new BlockItem(block, new Item.Properties()))
        )
    );

    public static final Map<NTERock, Map<Rock.BlockType, NTERock.DecorationSet>> ROCK_DECORATIONS = Helpers.mapOfKeys(NTERock.class, rock ->
        Helpers.mapOfKeys(Rock.BlockType.class, Rock.BlockType::hasVariants, type -> new NTERock.DecorationSet(
            register("rock/" + type.getSerializedName() + "/" + rock.getSerializedName() + "_slab", () -> type.createSlab(rock), block -> new BlockItem(block, new Item.Properties())),
            register("rock/" + type.getSerializedName() + "/" + rock.getSerializedName() + "_stairs", () -> type.createStairs(rock), block -> new BlockItem(block, new Item.Properties())),
            register("rock/" + type.getSerializedName() + "/" + rock.getSerializedName() + "_wall", () -> type.createWall(rock), block -> new BlockItem(block, new Item.Properties()))
        ))
    );

    public static final Map<NTERock, Map<Ore, RegistryObject<Block>>> ORES = Helpers.mapOfKeys(NTERock.class, rock ->
        Helpers.mapOfKeys(Ore.class, ore -> !ore.isGraded(), ore ->
            register("ore/" + ore.name() + "/" + rock.getSerializedName(), () -> ore.create(rock), block -> new BlockItem(block, new Item.Properties()))
        )
    );

    public static final Map<NTERock, Map<Ore, Map<Ore.Grade, RegistryObject<Block>>>> GRADED_ORES = Helpers.mapOfKeys(NTERock.class, rock ->
        Helpers.mapOfKeys(Ore.class, Ore::isGraded, ore ->
            Helpers.mapOfKeys(Ore.Grade.class, grade ->
                register("ore/" + grade.name() + "_" + ore.name() + "/" + rock.getSerializedName(), () -> ore.create(rock), block -> new BlockItem(block, new Item.Properties()))
            )
        )
    );

    public static final Map<NTERock, Map<OreDeposit, RegistryObject<Block>>> ORE_DEPOSITS = Helpers.mapOfKeys(NTERock.class, rock ->
        Helpers.mapOfKeys(OreDeposit.class, deposit ->
            register("deposit/" + deposit.name() + "/" + rock.getSerializedName(), () -> new Block(Block.Properties.of().mapColor(MapColor.STONE).sound(net.minecraft.world.level.block.SoundType.GRAVEL).strength(rock.category().hardness(2.0f))), block -> new BlockItem(block, new Item.Properties()))
        )
    );

    private NTERockBlocks()
    {
    }

    public static void init()
    {
    }

    public static void registerRockSettings()
    {
        for (NTERock rock : NTERock.values())
        {
            final RockSettings settings = new RockSettings(
                ROCK_BLOCKS.get(rock).get(Rock.BlockType.RAW).get(),
                ROCK_BLOCKS.get(rock).get(Rock.BlockType.HARDENED).get(),
                ROCK_BLOCKS.get(rock).get(Rock.BlockType.GRAVEL).get(),
                ROCK_BLOCKS.get(rock).get(Rock.BlockType.COBBLE).get(),
                TFCBlocks.SAND.get(rock.sandType()).get(),
                TFCBlocks.SANDSTONE.get(rock.sandType()).get(SandstoneBlockType.RAW).get(),
                Optional.of(ROCK_BLOCKS.get(rock).get(Rock.BlockType.SPIKE).get()),
                Optional.of(ROCK_BLOCKS.get(rock).get(Rock.BlockType.LOOSE).get()),
                Optional.of(ROCK_BLOCKS.get(rock).get(Rock.BlockType.MOSSY_LOOSE).get())
            );

            RockSettings.register(Helpers.identifier(rock.getSerializedName()), settings);
            RockSettings.register(new ResourceLocation(NewTerraEarthMod.MOD_ID, rock.getSerializedName()), settings);
        }
    }

    public static void registerProspectingRepresentatives()
    {
        GRADED_ORES.values().forEach(ores -> ores.values().forEach(blocks -> PropickItem.registerRepresentative(
            blocks.get(Ore.Grade.NORMAL).get(),
            blocks.get(Ore.Grade.RICH).get(),
            blocks.get(Ore.Grade.POOR).get()
        )));
    }

    private static <T extends Block> RegistryObject<T> register(String name, Supplier<T> blockSupplier, @Nullable Function<T, ? extends BlockItem> blockItemFactory)
    {
        return RegistrationHelpers.registerBlock(NTEBlocks.BLOCKS, NTEBlocks.ITEMS, name, blockSupplier, blockItemFactory);
    }
}
