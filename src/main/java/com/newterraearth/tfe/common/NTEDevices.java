package com.newterraearth.tfe.common;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraftforge.registries.RegistryObject;

import net.dries007.tfc.common.blockentities.AbstractFirepitBlockEntity;
import net.dries007.tfc.common.blockentities.TFCBlockEntities;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.TFCBlocks;

import com.newterraearth.tfe.common.block.devices.NTEStoveBlock;
import com.newterraearth.tfe.common.block.devices.NTEStovePotBlock;
import com.newterraearth.tfe.common.item.NTEProvidedBlockItem;

/**
 * Stove / stove pot devices. We register the blocks under the tfc namespace so existing tfc:stove /
 * tfc:stove_pot resource ids remain consistent with 1.21 TFC. Both blocks share TFC's upstream
 * FirepitBlockEntity / PotBlockEntity types so vanilla chunk-load validation, menus and fire-start
 * handlers transparently reuse the firepit / pot pipelines. The {@link
 * com.newterraearth.tfe.mixin.BlockEntityTypeMixin} relaxes BlockEntityType#isValid so those upstream
 * types accept our stove / stove_pot blocks.
 */
public final class NTEDevices
{
    public static final RegistryObject<Block> STOVE = NTEBlocks.TFC_BLOCKS.register("stove", () -> new NTEStoveBlock(
        ExtendedProperties.of(MapColor.DIRT)
            .strength(0.4F, 0.4F)
            .sound(SoundType.NETHER_WART)
            .randomTicks()
            .noOcclusion()
            .lightLevel(TFCBlocks.litBlockEmission(15))
            .blockEntity(TFCBlockEntities.FIREPIT)
            .pathType(BlockPathTypes.DAMAGE_FIRE)
            .adjacentPathType(BlockPathTypes.DANGER_FIRE)
            .<AbstractFirepitBlockEntity<?>>ticks(AbstractFirepitBlockEntity::serverTick, AbstractFirepitBlockEntity::clientTick)));

    public static final RegistryObject<Block> STOVE_POT = NTEBlocks.TFC_BLOCKS.register("stove_pot", () -> new NTEStovePotBlock(
        ExtendedProperties.of(MapColor.DIRT)
            .strength(0.4F, 0.4F)
            .sound(SoundType.NETHER_WART)
            .randomTicks()
            .noOcclusion()
            .lightLevel(TFCBlocks.litBlockEmission(15))
            .blockEntity(TFCBlockEntities.POT)
            .pathType(BlockPathTypes.DAMAGE_FIRE)
            .adjacentPathType(BlockPathTypes.DANGER_FIRE)
            .<AbstractFirepitBlockEntity<?>>ticks(AbstractFirepitBlockEntity::serverTick, AbstractFirepitBlockEntity::clientTick)));

    public static final RegistryObject<Item> STOVE_ITEM = NTEBlocks.TFC_ITEMS.register("stove", () -> new NTEProvidedBlockItem(STOVE.get(), new Item.Properties()));
    public static final RegistryObject<Item> STOVE_POT_ITEM = NTEBlocks.TFC_ITEMS.register("stove_pot", () -> new NTEProvidedBlockItem(STOVE_POT.get(), new Item.Properties()));

    private NTEDevices() {}

    public static void touch()
    {
        // Forces static initialisation so the DeferredRegister entries above register before the mod event bus fires.
    }
}
