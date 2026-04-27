package com.newterraearth.tfe.common;

import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

import net.dries007.tfc.common.blocks.rock.Rock;
import net.dries007.tfc.common.blocks.rock.RockDisplayCategory;
import net.dries007.tfc.common.blocks.soil.SandBlockType;
import net.dries007.tfc.util.registry.RegistryRock;

public enum NTERock implements RegistryRock
{
    TUFF(RockDisplayCategory.SEDIMENTARY, MapColor.TERRACOTTA_GRAY, SandBlockType.GREEN);

    private final String serializedName;
    private final RockDisplayCategory category;
    private final MapColor color;
    private final SandBlockType sandType;

    NTERock(RockDisplayCategory category, MapColor color, SandBlockType sandType)
    {
        this.serializedName = name().toLowerCase(Locale.ROOT);
        this.category = category;
        this.color = color;
        this.sandType = sandType;
    }

    public SandBlockType sandType()
    {
        return sandType;
    }

    @Override
    public RockDisplayCategory displayCategory()
    {
        return category;
    }

    @Override
    public MapColor color()
    {
        return color;
    }

    @Override
    public Supplier<? extends Block> getBlock(Rock.BlockType type)
    {
        return NTERockBlocks.ROCK_BLOCKS.get(this).get(type);
    }

    @Override
    public Supplier<? extends Block> getAnvil()
    {
        return () -> Blocks.AIR;
    }

    @Override
    public Supplier<? extends SlabBlock> getSlab(Rock.BlockType type)
    {
        return NTERockBlocks.ROCK_DECORATIONS.get(this).get(type).slab();
    }

    @Override
    public Supplier<? extends StairBlock> getStair(Rock.BlockType type)
    {
        return NTERockBlocks.ROCK_DECORATIONS.get(this).get(type).stair();
    }

    @Override
    public Supplier<? extends WallBlock> getWall(Rock.BlockType type)
    {
        return NTERockBlocks.ROCK_DECORATIONS.get(this).get(type).wall();
    }

    @Override
    public String getSerializedName()
    {
        return serializedName;
    }

    public record DecorationSet(RegistryObject<SlabBlock> slab, RegistryObject<StairBlock> stair, RegistryObject<WallBlock> wall) {}
}
