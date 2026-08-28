package com.newterraearth.tfe.world.prospecting;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.dries007.tfc.common.items.PropickItem;
import net.dries007.tfc.util.Helpers;

/**
 * The extra information TFE derives from one bounded TFC propick scan.
 *
 * <p>The public ore count first uses TFC representative blocks for tag
 * compatibility, then merges the same mineral across grade prefixes and
 * host-rock suffixes. The navigation target is still the nearest individual
 * prospectable block within the smaller navigation range to the player at
 * scan time.</p>
 */
public record NTEProspectingScan(Object2IntMap<Block> counts, @Nullable BlockPos nearestPos)
{
    public static NTEProspectingScan scan(
        Level level,
        BlockPos center,
        Player player,
        int scanRadius,
        int navigationRadius,
        TagKey<Block> tag
    )
    {
        return scan(
            level,
            center,
            player.getX(),
            player.getY() + player.getBbHeight() * 0.5D,
            player.getZ(),
            scanRadius,
            navigationRadius,
            tag
        );
    }

    public static NTEProspectingScan scan(
        Level level,
        BlockPos center,
        double playerX,
        double playerY,
        double playerZ,
        int scanRadius,
        int navigationRadius,
        TagKey<Block> tag
    )
    {
        if (navigationRadius < 0 || navigationRadius > scanRadius)
        {
            throw new IllegalArgumentException("Navigation radius must be between zero and the scan radius");
        }

        final Object2IntMap<Block> counts = new Object2IntOpenHashMap<>();
        final Map<ResourceLocation, Block> displayBlocks = new HashMap<>();
        BlockPos nearest = null;
        double nearestDistanceSqr = Double.POSITIVE_INFINITY;

        for (BlockPos cursor : BlockPos.betweenClosed(
            center.getX() - scanRadius,
            center.getY() - scanRadius,
            center.getZ() - scanRadius,
            center.getX() + scanRadius,
            center.getY() + scanRadius,
            center.getZ() + scanRadius
        ))
        {
            final Block rawBlock = level.getBlockState(cursor).getBlock();
            final Block block = PropickItem.getRepresentative(rawBlock);
            if (!Helpers.isBlock(block, tag))
            {
                continue;
            }

            final ResourceLocation mineralKey = NTEProspectingRules.mineralKey(rawBlock);
            final Block displayBlock = displayBlocks.computeIfAbsent(mineralKey, ignored -> block);
            counts.mergeInt(displayBlock, 1, Integer::sum);
            if (!isWithinNavigationRange(center, cursor, navigationRadius))
            {
                continue;
            }
            final double dx = cursor.getX() + 0.5D - playerX;
            final double dy = cursor.getY() + 0.5D - playerY;
            final double dz = cursor.getZ() + 0.5D - playerZ;
            final double distanceSqr = dx * dx + dy * dy + dz * dz;
            if (distanceSqr < nearestDistanceSqr || distanceSqr == nearestDistanceSqr && isEarlier(cursor, nearest))
            {
                nearestDistanceSqr = distanceSqr;
                nearest = cursor.immutable();
            }
        }

        return new NTEProspectingScan(counts, nearest);
    }

    public int mineralTypes()
    {
        return counts.size();
    }

    private static boolean isWithinNavigationRange(BlockPos center, BlockPos candidate, int radius)
    {
        return Math.abs(candidate.getX() - center.getX()) <= radius
            && Math.abs(candidate.getY() - center.getY()) <= radius
            && Math.abs(candidate.getZ() - center.getZ()) <= radius;
    }

    private static boolean isEarlier(BlockPos candidate, @Nullable BlockPos current)
    {
        if (current == null)
        {
            return true;
        }
        if (candidate.getY() != current.getY())
        {
            return candidate.getY() < current.getY();
        }
        if (candidate.getX() != current.getX())
        {
            return candidate.getX() < current.getX();
        }
        return candidate.getZ() < current.getZ();
    }
}
