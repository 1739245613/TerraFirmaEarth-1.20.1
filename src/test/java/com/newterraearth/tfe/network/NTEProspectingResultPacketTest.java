package com.newterraearth.tfe.network;

import java.util.List;

import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;

import net.dries007.tfc.common.items.ProspectResult;

import com.newterraearth.tfe.world.prospecting.NTEProspectingRules.MineralResult;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NTEProspectingResultPacketTest
{
    @BeforeAll
    static void bootstrapMinecraft()
    {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void packetRoundTripPreservesVisibleMineralsHiddenCountAndNearestTarget()
    {
        final NTEProspectingResultPacket source = new NTEProspectingResultPacket(
            Blocks.STONE,
            ProspectResult.LARGE,
            List.of(
                new MineralResult(Blocks.IRON_ORE, ProspectResult.LARGE),
                new MineralResult(Blocks.GOLD_ORE, ProspectResult.SMALL),
                new MineralResult(Blocks.COAL_ORE, ProspectResult.VERY_LARGE)
            ),
            2,
            new BlockPos(17, -22, 31)
        );
        final FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try
        {
            source.encode(buffer);
            assertEquals(source, new NTEProspectingResultPacket(buffer));
        }
        finally
        {
            buffer.release();
        }
    }
}
