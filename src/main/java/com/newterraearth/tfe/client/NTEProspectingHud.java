package com.newterraearth.tfe.client;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;

import net.dries007.tfc.common.items.ProspectResult;
import net.dries007.tfc.config.TFCConfig;
import net.dries007.tfc.util.events.ProspectedEvent;

import com.newterraearth.tfe.world.prospecting.NTEProspectingRules.MineralResult;

/** Three-second, player-relative direction display for the latest propick result. */
public final class NTEProspectingHud
{
    static final long DISPLAY_NANOS = 3_000_000_000L;
    static final double FULL_RING_DISTANCE = 2D;
    static final double MAX_NAVIGATION_DISTANCE = 33D;
    static final int RING_SEGMENTS = 144;
    static final int RING_RADIUS = 34;

    private static final int BASE_RING_COLOR = 0x28D8C773;
    private static final int ACTIVE_RING_RGB = 0xF4D96B;
    private static final int ARROW_RGB = 0xFFF1A3;

    @Nullable private static BlockPos target;
    private static List<Component> actionbarLines = List.of();
    private static long expiresAtNanos;

    private NTEProspectingHud()
    {
    }

    public static void init()
    {
        MinecraftForge.EVENT_BUS.addListener(NTEProspectingHud::onRenderOverlay);
        MinecraftForge.EVENT_BUS.addListener(NTEProspectingHud::onPlayerLoggedOut);
    }

    public static void acceptResult(
        Block eventBlock,
        ProspectResult eventResult,
        List<MineralResult> minerals,
        int hiddenMinerals,
        @Nullable BlockPos nearestPos
    )
    {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null)
        {
            return;
        }

        MinecraftForge.EVENT_BUS.post(new ProspectedEvent(minecraft.player, eventResult, eventBlock));
        final List<Component> lines = buildResultLines(eventBlock, eventResult, minerals, hiddenMinerals);
        if (TFCConfig.CLIENT.sendProspectResultsToActionbar.get())
        {
            minecraft.gui.setOverlayMessage(Component.empty(), false);
            actionbarLines = lines;
        }
        else
        {
            actionbarLines = List.of();
            for (Component line : lines)
            {
                minecraft.player.displayClientMessage(line, false);
            }
        }

        if (!minerals.isEmpty() && nearestPos != null)
        {
            target = nearestPos.immutable();
        }
        else
        {
            target = null;
        }
        expiresAtNanos = target != null || !actionbarLines.isEmpty() ? System.nanoTime() + DISPLAY_NANOS : 0L;
    }

    static List<Component> buildResultLines(
        Block eventBlock,
        ProspectResult eventResult,
        List<MineralResult> minerals,
        int hiddenMinerals
    )
    {
        if (minerals.isEmpty())
        {
            return List.of(eventResult.getText(eventBlock));
        }

        final List<Component> lines = new ArrayList<>((minerals.size() + 1) / 2);
        for (int start = 0; start < minerals.size(); start += 2)
        {
            final MutableComponent line = Component.empty();
            final int end = Math.min(start + 2, minerals.size());
            for (int index = start; index < end; index++)
            {
                if (index > start)
                {
                    line.append(Component.translatable("tfe.tooltip.propick.separator"));
                }
                line.append(mineralText(minerals.get(index)));
            }
            if (end == minerals.size() && hiddenMinerals > 0)
            {
                final MutableComponent unknowns = Component.empty();
                for (int i = 0; i < hiddenMinerals; i++)
                {
                    unknowns.append(Component.translatable("tfe.tooltip.propick.unknown"));
                }
                line.append(Component.translatable("tfe.tooltip.propick.and_hidden", unknowns));
            }
            lines.add(line);
        }
        return List.copyOf(lines);
    }

    private static Component mineralText(MineralResult mineral)
    {
        if (mineral.result() == ProspectResult.NOTHING)
        {
            return mineral.result().getText(mineral.block());
        }
        final Component blockName = Component.translatable(
            Util.makeDescriptionId("block", BuiltInRegistries.BLOCK.getKey(mineral.block())) + ".prospected"
        );
        final String amount = switch (mineral.result())
        {
            case VERY_LARGE -> "very_large";
            case LARGE -> "large";
            case MEDIUM -> "medium";
            case SMALL -> "small";
            case TRACES -> "traces";
            case FOUND -> "found";
            case NOTHING -> throw new IllegalStateException("Handled above");
        };
        return Component.translatable("tfe.tooltip.propick.result." + amount, blockName);
    }

    private static void onRenderOverlay(RenderGuiOverlayEvent.Post event)
    {
        if (event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type())
        {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        final BlockPos currentTarget = target;
        final List<Component> currentLines = actionbarLines;
        final long remainingNanos = expiresAtNanos - System.nanoTime();
        if (remainingNanos <= 0L)
        {
            clear();
            return;
        }
        if (currentTarget == null && currentLines.isEmpty())
        {
            return;
        }
        if (minecraft.player == null || minecraft.screen != null || minecraft.options.hideGui)
        {
            return;
        }

        final float lifetimeAlpha = remainingNanos < 250_000_000L ? remainingNanos / 250_000_000F : 1F;
        if (!currentLines.isEmpty())
        {
            final int centerX = event.getWindow().getGuiScaledWidth() / 2;
            final int bottomY = event.getWindow().getGuiScaledHeight() - 68;
            final int firstY = bottomY - (currentLines.size() - 1) * 10;
            final int textColor = color((int) (255F * lifetimeAlpha), 0xFFFFFF);
            for (int i = 0; i < currentLines.size(); i++)
            {
                event.getGuiGraphics().drawCenteredString(minecraft.font, currentLines.get(i), centerX, firstY + i * 10, textColor);
            }
        }
        if (currentTarget == null)
        {
            return;
        }

        final float partialTick = event.getPartialTick();
        final double playerX = Mth.lerp(partialTick, minecraft.player.xOld, minecraft.player.getX());
        final double playerY = Mth.lerp(partialTick, minecraft.player.yOld, minecraft.player.getY()) + minecraft.player.getBbHeight() * 0.5D;
        final double playerZ = Mth.lerp(partialTick, minecraft.player.zOld, minecraft.player.getZ());
        final double dx = currentTarget.getX() + 0.5D - playerX;
        final double dy = currentTarget.getY() + 0.5D - playerY;
        final double dz = currentTarget.getZ() + 0.5D - playerZ;
        final double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        final double relativeYaw = relativeDirection(
            playerX,
            playerZ,
            minecraft.player.getViewYRot(partialTick),
            currentTarget.getX() + 0.5D,
            currentTarget.getZ() + 0.5D
        );
        renderDirection(
            event.getGuiGraphics(),
            event.getWindow().getGuiScaledWidth() / 2,
            event.getWindow().getGuiScaledHeight() / 2,
            relativeYaw,
            distance,
            dy,
            lifetimeAlpha
        );
    }

    static void renderDirection(
        GuiGraphics graphics,
        int centerX,
        int centerY,
        double direction,
        double distance,
        double verticalOffset,
        float lifetimeAlpha
    )
    {
        drawFullRing(graphics, centerX, centerY, lifetimeAlpha);

        final double fraction = arcFraction(distance);
        final double halfSpan = Math.PI * fraction;
        final int arcSegments = Math.max(2, Mth.ceil(RING_SEGMENTS * fraction));
        for (int i = 0; i <= arcSegments; i++)
        {
            final double unit = i / (double) arcSegments;
            final double offset = Mth.lerp(unit, -halfSpan, halfSpan);
            final double fade = 1D - Math.abs(offset) / Math.max(halfSpan, 1.0E-6D);
            final double intensity = fraction >= 0.999D ? 0.18D + 0.82D * fade * fade : fade * fade;
            final int alpha = Mth.clamp((int) (235D * intensity * lifetimeAlpha), 0, 255);
            drawRingPoint(graphics, centerX, centerY, direction + offset, color(alpha, ACTIVE_RING_RGB), 1);
        }

        final int arrowX = centerX + Mth.floor(Math.sin(direction) * RING_RADIUS);
        final int arrowY = centerY - Mth.floor(Math.cos(direction) * RING_RADIUS);
        drawVerticalArrow(graphics, arrowX, arrowY, verticalOffset, color((int) (255F * lifetimeAlpha), ARROW_RGB));
    }

    static double arcFraction(double distance)
    {
        if (distance <= FULL_RING_DISTANCE)
        {
            return 1D;
        }
        final double closeness = 1D - Mth.clamp(
            (distance - FULL_RING_DISTANCE) / (MAX_NAVIGATION_DISTANCE - FULL_RING_DISTANCE),
            0D,
            1D
        );
        return 0.08D + 0.92D * closeness;
    }

    static double relativeDirection(double playerX, double playerZ, float playerYaw, double targetX, double targetZ)
    {
        final double targetYaw = Math.toDegrees(Math.atan2(playerX - targetX, targetZ - playerZ));
        return Math.toRadians(Mth.wrapDegrees(targetYaw - playerYaw));
    }

    private static void drawFullRing(GuiGraphics graphics, int centerX, int centerY, float lifetimeAlpha)
    {
        final int baseAlpha = Mth.clamp((int) (((BASE_RING_COLOR >>> 24) & 0xFF) * lifetimeAlpha), 0, 255);
        final int baseColor = color(baseAlpha, BASE_RING_COLOR & 0xFFFFFF);
        for (int i = 0; i < RING_SEGMENTS; i++)
        {
            drawRingPoint(graphics, centerX, centerY, Math.PI * 2D * i / RING_SEGMENTS, baseColor, 0);
        }
    }

    private static void drawRingPoint(GuiGraphics graphics, int centerX, int centerY, double angle, int color, int radius)
    {
        final int x = centerX + Mth.floor(Math.sin(angle) * RING_RADIUS);
        final int y = centerY - Mth.floor(Math.cos(angle) * RING_RADIUS);
        graphics.fill(x - radius, y - radius, x + radius + 1, y + radius + 1, color);
    }

    private static void drawVerticalArrow(GuiGraphics graphics, int x, int y, double verticalOffset, int color)
    {
        final int shadow = color(((color >>> 24) & 0xFF) * 2 / 3, 0x241F12);
        if (verticalOffset > 1D)
        {
            drawUpArrow(graphics, x + 1, y + 1, shadow);
            drawUpArrow(graphics, x, y, color);
        }
        else if (verticalOffset < -1D)
        {
            drawDownArrow(graphics, x + 1, y + 1, shadow);
            drawDownArrow(graphics, x, y, color);
        }
        else
        {
            drawDiamond(graphics, x + 1, y + 1, shadow);
            drawDiamond(graphics, x, y, color);
        }
    }

    private static void drawUpArrow(GuiGraphics graphics, int x, int y, int color)
    {
        graphics.fill(x, y - 6, x + 1, y + 5, color);
        graphics.fill(x - 1, y - 5, x + 2, y - 3, color);
        graphics.fill(x - 2, y - 4, x + 3, y - 2, color);
        graphics.fill(x - 3, y - 3, x + 4, y - 1, color);
    }

    private static void drawDownArrow(GuiGraphics graphics, int x, int y, int color)
    {
        graphics.fill(x, y - 4, x + 1, y + 7, color);
        graphics.fill(x - 1, y + 3, x + 2, y + 5, color);
        graphics.fill(x - 2, y + 2, x + 3, y + 4, color);
        graphics.fill(x - 3, y + 1, x + 4, y + 3, color);
    }

    private static void drawDiamond(GuiGraphics graphics, int x, int y, int color)
    {
        graphics.fill(x, y - 3, x + 1, y + 4, color);
        graphics.fill(x - 1, y - 2, x + 2, y + 3, color);
        graphics.fill(x - 2, y - 1, x + 3, y + 2, color);
    }

    private static int color(int alpha, int rgb)
    {
        return Mth.clamp(alpha, 0, 255) << 24 | rgb & 0xFFFFFF;
    }

    private static void onPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event)
    {
        clear();
    }

    private static void clear()
    {
        target = null;
        actionbarLines = List.of();
        expiresAtNanos = 0L;
    }
}
