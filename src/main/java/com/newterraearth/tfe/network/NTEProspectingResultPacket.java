package com.newterraearth.tfe.network;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import net.dries007.tfc.common.items.ProspectResult;

import com.newterraearth.tfe.client.NTEProspectingHud;
import com.newterraearth.tfe.world.prospecting.NTEProspectingRules.MineralResult;

/** Replaces TFC's two-field client packet while preserving its client event and message behavior. */
public record NTEProspectingResultPacket(
    Block eventBlock,
    ProspectResult eventResult,
    List<MineralResult> minerals,
    int hiddenMinerals,
    @Nullable BlockPos nearestPos
)
{
    private static final int MAX_MINERALS = 4;

    public NTEProspectingResultPacket
    {
        minerals = List.copyOf(minerals);
        if (minerals.size() > MAX_MINERALS)
        {
            throw new IllegalArgumentException("A propick packet cannot reveal more than " + MAX_MINERALS + " minerals");
        }
        hiddenMinerals = Math.max(0, hiddenMinerals);
    }

    public NTEProspectingResultPacket(FriendlyByteBuf buffer)
    {
        this(
            BuiltInRegistries.BLOCK.byId(buffer.readVarInt()),
            ProspectResult.valueOf(buffer.readByte()),
            readMinerals(buffer),
            buffer.readVarInt(),
            buffer.readBoolean() ? buffer.readBlockPos() : null
        );
    }

    public void encode(FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(BuiltInRegistries.BLOCK.getId(eventBlock));
        buffer.writeByte(eventResult.ordinal());
        buffer.writeVarInt(minerals.size());
        for (MineralResult mineral : minerals)
        {
            buffer.writeVarInt(BuiltInRegistries.BLOCK.getId(mineral.block()));
            buffer.writeByte(mineral.result().ordinal());
        }
        buffer.writeVarInt(hiddenMinerals);
        buffer.writeBoolean(nearestPos != null);
        if (nearestPos != null)
        {
            buffer.writeBlockPos(nearestPos);
        }
    }

    public void handle()
    {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
            NTEProspectingHud.acceptResult(eventBlock, eventResult, minerals, hiddenMinerals, nearestPos)
        );
    }

    private static List<MineralResult> readMinerals(FriendlyByteBuf buffer)
    {
        final int size = buffer.readVarInt();
        if (size < 0 || size > MAX_MINERALS)
        {
            throw new IllegalArgumentException("Invalid propick mineral result count: " + size);
        }
        final List<MineralResult> minerals = new ArrayList<>(size);
        for (int i = 0; i < size; i++)
        {
            minerals.add(new MineralResult(
                BuiltInRegistries.BLOCK.byId(buffer.readVarInt()),
                ProspectResult.valueOf(buffer.readByte())
            ));
        }
        return minerals;
    }
}
