package com.newterraearth.tfe.world;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import org.jetbrains.annotations.Nullable;

public final class NTESeed
{
    public static NTESeed of(long levelSeed)
    {
        return new NTESeed(levelSeed, new XoroshiroRandomSource(levelSeed));
    }

    public static NTESeed unsafeOf(long levelSeed)
    {
        return new NTESeed(levelSeed, null);
    }

    private final long seed;
    private final @Nullable XoroshiroRandomSource next;

    private NTESeed(long seed, @Nullable XoroshiroRandomSource next)
    {
        this.seed = seed;
        this.next = next;
    }

    public long seed()
    {
        return seed;
    }

    public long next()
    {
        assert next != null : "Unsafe to use next() in this context";
        return next.nextLong();
    }

    public RandomSource fork()
    {
        return new XoroshiroRandomSource(next(), next());
    }

    public NTESeed forkStable()
    {
        return of(seed);
    }
}
