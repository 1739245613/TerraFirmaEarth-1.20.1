package com.newterraearth.tfe.mixin;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.dries007.tfc.world.chunkdata.ChunkData;

import com.newterraearth.tfe.world.NTEChunkWeatherBridge;

@Mixin(value = ChunkData.class, remap = false)
public abstract class ChunkDataMixin implements NTEChunkWeatherBridge
{
    @Unique private static final byte[] TFE$SHUFFLED_BLOCK_POSITIONS = tfe$createShuffledBlockPositions();
    @Unique private static final long TFE$RANDOM_TICK_SAVE_INTERVAL = 4_000L;
    @Unique private long tfe$lastRandomTick = Integer.MIN_VALUE;
    @Unique private long tfe$lastCalendarTick = Long.MIN_VALUE;
    @Unique private byte tfe$nextSnowPosition = 0;

    @Inject(method = "serializeNBT", at = @At("RETURN"))
    private void tfe$serializeWeatherState(CallbackInfoReturnable<CompoundTag> cir)
    {
        final CompoundTag nbt = cir.getReturnValue();
        nbt.putLong("tfeLastRandomTick", tfe$lastRandomTick);
        nbt.putLong("tfeLastCalendarTick", tfe$lastCalendarTick);
        nbt.putByte("tfeNextSnowPosition", tfe$nextSnowPosition);
    }

    @Inject(method = "deserializeNBT", at = @At("TAIL"))
    private void tfe$deserializeWeatherState(CompoundTag nbt, CallbackInfo ci)
    {
        tfe$lastRandomTick = nbt.contains("tfeLastRandomTick", Tag.TAG_LONG) ? nbt.getLong("tfeLastRandomTick") : Integer.MIN_VALUE;
        tfe$lastCalendarTick = nbt.contains("tfeLastCalendarTick", Tag.TAG_LONG) ? nbt.getLong("tfeLastCalendarTick") : Long.MIN_VALUE;
        tfe$nextSnowPosition = nbt.contains("tfeNextSnowPosition", Tag.TAG_BYTE) ? nbt.getByte("tfeNextSnowPosition") : 0;
    }

    @Override
    public long tfe$getLastRandomTick()
    {
        return tfe$lastRandomTick;
    }

    @Override
    public void tfe$setLastRandomTick(ChunkAccess chunk, long lastRandomTick)
    {
        final long previousTick = tfe$lastRandomTick;
        tfe$lastRandomTick = lastRandomTick;
        if (previousTick == Integer.MIN_VALUE || tfe$shouldPersistRandomTick(previousTick, lastRandomTick))
        {
            chunk.setUnsaved(true);
        }
    }

    @Override
    public long tfe$getLastCalendarTick()
    {
        return tfe$lastCalendarTick;
    }

    @Override
    public void tfe$setLastCalendarTick(ChunkAccess chunk, long lastCalendarTick)
    {
        final long previousTick = tfe$lastCalendarTick;
        tfe$lastCalendarTick = lastCalendarTick;
        if (previousTick == Long.MIN_VALUE || tfe$shouldPersistRandomTick(previousTick, lastCalendarTick))
        {
            chunk.setUnsaved(true);
        }
    }

    @Override
    public BlockPos tfe$getNextSnowPos(ChunkPos chunkPos)
    {
        final int index = tfe$nextSnowPosition & 0xFF;
        final int encoded = TFE$SHUFFLED_BLOCK_POSITIONS[index] & 0xFF;
        final int x = encoded & 15;
        final int z = encoded >> 4 & 15;
        return new BlockPos(chunkPos.getMinBlockX() + x, 0, chunkPos.getMinBlockZ() + z);
    }

    @Override
    public void tfe$iterateSnowPos(ChunkAccess chunk)
    {
        tfe$nextSnowPosition++;
        chunk.setUnsaved(true);
    }

    @Unique
    private static byte[] tfe$createShuffledBlockPositions()
    {
        final byte[] shuffled = new byte[256];
        for (int i = 0; i < shuffled.length; i++)
        {
            shuffled[i] = (byte) i;
        }

        final Random random = new Random(0x54F3F98DL);
        for (int i = shuffled.length - 1; i > 0; i--)
        {
            final int j = random.nextInt(i + 1);
            final byte tmp = shuffled[i];
            shuffled[i] = shuffled[j];
            shuffled[j] = tmp;
        }
        return shuffled;
    }

    @Unique
    private static boolean tfe$shouldPersistRandomTick(long previousTick, long nextTick)
    {
        return Math.floorDiv(previousTick, TFE$RANDOM_TICK_SAVE_INTERVAL) != Math.floorDiv(nextTick, TFE$RANDOM_TICK_SAVE_INTERVAL);
    }
}
