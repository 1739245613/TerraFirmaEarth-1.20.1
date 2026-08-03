package com.newterraearth.tfe.world.prospecting;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
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
 * <p>The public ore count uses TFC representative blocks, so poor, normal and
 * rich variants still count as one mineral type. The navigation target is the
 * nearest individual prospectable block to the player at scan time.</p>
 */
public record NTEProspectingScan(Object2IntMap<Block> counts, @Nullable BlockPos nearestPos)
{
    public static NTEProspectingScan scan(Level level, BlockPos center, Player player, int radius, TagKey<Block> tag)
    {
        return scan(level, center, player.getX(), player.getY() + player.getBbHeight() * 0.5D, player.getZ(), radius, tag);
    }

    public static NTEProspectingScan scan(
        Level level,
        BlockPos center,
        double playerX,
        double playerY,
        double playerZ,
        int radius,
        TagKey<Block> tag
    )
    {
        final Object2IntMap<Block> counts = new Object2IntOpenHashMap<>();
        BlockPos nearest = null;
        double nearestDistanceSqr = Double.POSITIVE_INFINITY;

        for (BlockPos cursor : BlockPos.betweenClosed(
            center.getX() - radius,
            center.getY() - radius,
            center.getZ() - radius,
            center.getX() + radius,
            center.getY() + radius,
            center.getZ() + radius
        ))
        {
            final Block rawBlock = level.getBlockState(cursor).getBlock();
            final Block block = PropickItem.getRepresentative(rawBlock);
            if (!Helpers.isBlock(block, tag))
            {
                continue;
            }

            counts.mergeInt(block, 1, Integer::sum);
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
