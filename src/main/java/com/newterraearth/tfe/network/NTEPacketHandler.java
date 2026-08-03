package com.newterraearth.tfe.network;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import org.apache.commons.lang3.mutable.MutableInt;

import com.newterraearth.tfe.NewTerraEarthMod;

public final class NTEPacketHandler
{
    private static final String VERSION = "2";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(NewTerraEarthMod.MOD_ID, "network"),
        () -> VERSION,
        VERSION::equals,
        VERSION::equals
    );
    private static final MutableInt ID = new MutableInt(0);

    private NTEPacketHandler()
    {
    }

    public static void init()
    {
        register(NTERainVarianceChunkPacket.class, NTERainVarianceChunkPacket::encode, NTERainVarianceChunkPacket::new, NTERainVarianceChunkPacket::handle);
        register(NTEProspectingResultPacket.class, NTEProspectingResultPacket::encode, NTEProspectingResultPacket::new, NTEProspectingResultPacket::handle);
    }

    public static void send(PacketDistributor.PacketTarget target, Object message)
    {
        CHANNEL.send(target, message);
    }

    private static <T> void register(Class<T> cls, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder, Consumer<T> handler)
    {
        register(cls, encoder, decoder, (packet, player) -> handler.accept(packet));
    }

    private static <T> void register(Class<T> cls, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, ServerPlayer> handler)
    {
        CHANNEL.registerMessage(ID.getAndIncrement(), cls, encoder, decoder, (packet, context) -> {
            context.get().setPacketHandled(true);
            context.get().enqueueWork(() -> handler.accept(packet, context.get().getSender()));
        });
    }
}
