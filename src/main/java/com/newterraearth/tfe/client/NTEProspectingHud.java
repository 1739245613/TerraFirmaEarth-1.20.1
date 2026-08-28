package com.newterraearth.tfe.client;

import java.util.ArrayList;
import java.util.Arrays;
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

/** Player-relative result text and animated direction display for the latest propick result. */
public final class NTEProspectingHud
{
    static final long RESULT_TEXT_DISPLAY_NANOS = 3_000_000_000L;
    static final long DIRECTION_DISPLAY_NANOS = 6_000_000_000L;
    static final long GUIDE_TRANSITION_NANOS = 450_000_000L;
    static final long MIN_MARKER_CYCLE_NANOS = 1_000_000_000L;
    static final long MAX_MARKER_CYCLE_NANOS = 3_000_000_000L;
    static final double FULL_RING_DISTANCE = 2D;
    static final double MAX_NAVIGATION_DISTANCE = 33D;
    static final double MIN_ARC_FRACTION = 0.08D;
    static final double MIN_RING_CENTER_THICKNESS = 1.25D;
    static final double MAX_RING_CENTER_THICKNESS = 7D;
    static final int RING_SEGMENTS = 768;
    static final int SUBPIXEL_SCALE = 2;
    static final int RING_RADIUS = 34 * SUBPIXEL_SCALE;

    private static final int MIN_VISIBLE_ARC_ALPHA = 5;
    private static final int RING_BLUR_SUBPIXELS = 2;
    private static final int MAX_RING_RADIAL_OFFSET = 9;
    private static final int RING_BUFFER_RADIUS = RING_RADIUS + MAX_RING_RADIAL_OFFSET;
    private static final int RING_BUFFER_SIDE = RING_BUFFER_RADIUS * 2 + 1;
    private static final int[] RING_ALPHA_BUFFER = new int[RING_BUFFER_SIDE * RING_BUFFER_SIDE];
    private static final int[] RING_RESOLVE_BUFFER = new int[RING_BUFFER_SIDE * RING_BUFFER_SIDE];
    private static final int[] ARC_RGB = {0, 0xE7EBF0, 0x59CF83, 0xE9BB5B, 0xE65F67};
    private static final int[] MARKER_RGB = {0, 0xFFFFFF, 0x9AE6B0, 0xF7DB88, 0xF28A90};
    private static final int INACTIVE_MARKER_BORDER_RGB = 0x4A5058;
    private static final int ACTIVE_MARKER_BORDER_RGB = 0xAEB5BD;

    enum MarkerShape
    {
        UP,
        DOWN,
        DIAMOND
    }

    record MarkerAnimation(double sweep, boolean lighting)
    {
    }

    record GuideSignature(BlockPos target, int colorTier)
    {
        GuideSignature
        {
            target = target.immutable();
        }
    }

    @Nullable private static BlockPos target;
    @Nullable private static GuideSignature guideSignature;
    private static List<Component> actionbarLines = List.of();
    private static long actionbarExpiresAtNanos;
    private static long directionExpiresAtNanos;
    private static long lastMarkerAnimationNanos;
    private static double markerAnimationPhase;
    private static long guideTransitionStartedAtNanos;
    private static double transitionFromTargetX;
    private static double transitionFromTargetY;
    private static double transitionFromTargetZ;
    private static double transitionToTargetX;
    private static double transitionToTargetY;
    private static double transitionToTargetZ;
    private static int transitionFromArcRgb;
    private static int transitionFromMarkerRgb;
    private static int transitionToArcRgb;
    private static int transitionToMarkerRgb;

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

        final long now = System.nanoTime();
        if (!minerals.isEmpty() && nearestPos != null)
        {
            acceptGuide(nearestPos, minerals, hiddenMinerals, now);
        }
        else
        {
            clearTarget();
        }
        actionbarExpiresAtNanos = actionbarLines.isEmpty() ? 0L : now + RESULT_TEXT_DISPLAY_NANOS;
    }

    private static void acceptGuide(BlockPos nearestPos, List<MineralResult> minerals, int hiddenMinerals, long now)
    {
        final int colorTier = mineralColorTier(minerals.size(), hiddenMinerals);
        final GuideSignature nextSignature = new GuideSignature(nearestPos, colorTier);
        final boolean currentGuideActive = target != null && directionExpiresAtNanos > now;
        if (currentGuideActive && nextSignature.equals(guideSignature))
        {
            directionExpiresAtNanos = now + DIRECTION_DISPLAY_NANOS;
            return;
        }

        final double nextTargetX = nearestPos.getX() + 0.5D;
        final double nextTargetY = nearestPos.getY() + 0.5D;
        final double nextTargetZ = nearestPos.getZ() + 0.5D;
        if (currentGuideActive)
        {
            final double progress = guideTransitionProgress(now, guideTransitionStartedAtNanos);
            final Minecraft minecraft = Minecraft.getInstance();
            final double playerX = minecraft.player == null ? 0D : minecraft.player.getX();
            final double playerZ = minecraft.player == null ? 0D : minecraft.player.getZ();
            final double fromDx = transitionFromTargetX - playerX;
            final double fromDz = transitionFromTargetZ - playerZ;
            final double toDx = transitionToTargetX - playerX;
            final double toDz = transitionToTargetZ - playerZ;
            final double horizontalDistance = Mth.lerp(progress, Math.hypot(fromDx, fromDz), Math.hypot(toDx, toDz));
            final double worldDirection = lerpRadians(Math.atan2(fromDx, fromDz), Math.atan2(toDx, toDz), progress);
            transitionFromTargetX = playerX + Math.sin(worldDirection) * horizontalDistance;
            transitionFromTargetY = Mth.lerp(progress, transitionFromTargetY, transitionToTargetY);
            transitionFromTargetZ = playerZ + Math.cos(worldDirection) * horizontalDistance;
            transitionFromArcRgb = lerpRgb(transitionFromArcRgb, transitionToArcRgb, progress);
            transitionFromMarkerRgb = lerpRgb(transitionFromMarkerRgb, transitionToMarkerRgb, progress);
            guideTransitionStartedAtNanos = now;
        }
        else
        {
            transitionFromTargetX = nextTargetX;
            transitionFromTargetY = nextTargetY;
            transitionFromTargetZ = nextTargetZ;
            transitionFromArcRgb = ARC_RGB[colorTier];
            transitionFromMarkerRgb = MARKER_RGB[colorTier];
            guideTransitionStartedAtNanos = 0L;
            lastMarkerAnimationNanos = now;
            markerAnimationPhase = 0D;
        }

        transitionToTargetX = nextTargetX;
        transitionToTargetY = nextTargetY;
        transitionToTargetZ = nextTargetZ;
        transitionToArcRgb = ARC_RGB[colorTier];
        transitionToMarkerRgb = MARKER_RGB[colorTier];
        target = nearestPos.immutable();
        guideSignature = nextSignature;
        directionExpiresAtNanos = now + DIRECTION_DISPLAY_NANOS;
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
        final String amount = mineral.isOverAmount() ? "over_amount" : switch (mineral.result())
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
        final long now = System.nanoTime();
        BlockPos currentTarget = target;
        List<Component> currentLines = actionbarLines;
        long directionRemainingNanos = directionExpiresAtNanos - now;
        long textRemainingNanos = actionbarExpiresAtNanos - now;
        if (currentTarget != null && directionRemainingNanos <= 0L)
        {
            clearTarget();
            currentTarget = null;
            directionRemainingNanos = 0L;
        }
        if (!currentLines.isEmpty() && textRemainingNanos <= 0L)
        {
            actionbarLines = List.of();
            actionbarExpiresAtNanos = 0L;
            currentLines = List.of();
            textRemainingNanos = 0L;
        }
        if (currentTarget == null && currentLines.isEmpty())
        {
            return;
        }
        if (minecraft.player == null || minecraft.screen != null || minecraft.options.hideGui)
        {
            return;
        }

        if (!currentLines.isEmpty())
        {
            final float textLifetimeAlpha = lifetimeAlpha(textRemainingNanos);
            final int centerX = event.getWindow().getGuiScaledWidth() / 2;
            final int bottomY = event.getWindow().getGuiScaledHeight() - 68;
            final int firstY = bottomY - (currentLines.size() - 1) * 10;
            final int textColor = color((int) (255F * textLifetimeAlpha), 0xFFFFFF);
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
        final double transitionProgress = guideTransitionProgress(now, guideTransitionStartedAtNanos);
        final double fromDx = transitionFromTargetX - playerX;
        final double fromDy = transitionFromTargetY - playerY;
        final double fromDz = transitionFromTargetZ - playerZ;
        final double toDx = transitionToTargetX - playerX;
        final double toDy = transitionToTargetY - playerY;
        final double toDz = transitionToTargetZ - playerZ;
        final double fromDistance = Math.sqrt(fromDx * fromDx + fromDy * fromDy + fromDz * fromDz);
        final double toDistance = Math.sqrt(toDx * toDx + toDy * toDy + toDz * toDz);
        final double distance = Mth.lerp(transitionProgress, fromDistance, toDistance);
        final float directionLifetimeAlpha = lifetimeAlpha(directionRemainingNanos);
        final MarkerAnimation markerAnimation = advanceMarkerAnimation(now, distance);

        final double fromRelativeYaw = relativeDirection(
            playerX,
            playerZ,
            minecraft.player.getViewYRot(partialTick),
            transitionFromTargetX,
            transitionFromTargetZ
        );
        final double toRelativeYaw = relativeDirection(
            playerX,
            playerZ,
            minecraft.player.getViewYRot(partialTick),
            transitionToTargetX,
            transitionToTargetZ
        );
        final double relativeYaw = lerpRadians(fromRelativeYaw, toRelativeYaw, transitionProgress);
        renderDirection(
            event.getGuiGraphics(),
            event.getWindow().getGuiScaledWidth() / 2,
            event.getWindow().getGuiScaledHeight() / 2,
            relativeYaw,
            distance,
            fromDy,
            toDy,
            transitionProgress,
            directionLifetimeAlpha,
            lerpRgb(transitionFromArcRgb, transitionToArcRgb, transitionProgress),
            lerpRgb(transitionFromMarkerRgb, transitionToMarkerRgb, transitionProgress),
            markerAnimation
        );
    }

    static void renderDirection(
        GuiGraphics graphics,
        int centerX,
        int centerY,
        double direction,
        double distance,
        double fromVerticalOffset,
        double toVerticalOffset,
        double transitionProgress,
        float lifetimeAlpha,
        int arcRgb,
        int markerRgb,
        MarkerAnimation markerAnimation
    )
    {
        final double fraction = arcFraction(distance);
        final double halfSpan = Math.PI * fraction;
        final int arcSegments = Math.max(2, Mth.ceil(RING_SEGMENTS * fraction));
        final int scaledCenterX = centerX * SUBPIXEL_SCALE;
        final int scaledCenterY = centerY * SUBPIXEL_SCALE;

        graphics.pose().pushPose();
        graphics.pose().scale(1F / SUBPIXEL_SCALE, 1F / SUBPIXEL_SCALE, 1F);
        try
        {
            drawTaperedArc(graphics, scaledCenterX, scaledCenterY, direction, fraction, halfSpan, arcSegments, lifetimeAlpha, arcRgb);

            final int arrowX = scaledCenterX + (int) Math.round(Math.sin(direction) * RING_RADIUS);
            final int arrowY = scaledCenterY - (int) Math.round(Math.cos(direction) * RING_RADIUS);
            drawTransitioningMarker(
                graphics,
                arrowX,
                arrowY,
                markerShape(fromVerticalOffset),
                markerShape(toVerticalOffset),
                transitionProgress,
                markerAnimation,
                lifetimeAlpha,
                markerRgb
            );
        }
        finally
        {
            graphics.pose().popPose();
        }
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
        final double smoothCloseness = closeness * closeness * (3D - 2D * closeness);
        return MIN_ARC_FRACTION + (1D - MIN_ARC_FRACTION) * smoothCloseness;
    }

    static double ringCenterThickness(double fraction)
    {
        final double clampedFraction = Mth.clamp(fraction, MIN_ARC_FRACTION, 1D);
        return Mth.clamp(
            MIN_RING_CENTER_THICKNESS / Math.pow(clampedFraction, 0.8D),
            MIN_RING_CENTER_THICKNESS,
            MAX_RING_CENTER_THICKNESS
        );
    }

    static double ringThickness(double fraction, double normalizedOffset)
    {
        final double offset = Mth.clamp(normalizedOffset, 0D, 1D);
        final double taper = Math.pow(Math.max(0D, Math.cos(offset * Math.PI * 0.5D)), 0.72D);
        return Math.max(0.5D, ringCenterThickness(fraction) * taper);
    }

    static void fillEnclosedRingGaps(int[] source, int[] target, int side)
    {
        if (source.length != side * side || target.length != source.length)
        {
            throw new IllegalArgumentException("Ring gap buffers must match their square side length");
        }
        System.arraycopy(source, 0, target, 0, source.length);
        for (int y = 1; y < side - 1; y++)
        {
            for (int x = 1; x < side - 1; x++)
            {
                final int index = y * side + x;
                if (source[index] >= MIN_VISIBLE_ARC_ALPHA)
                {
                    continue;
                }

                int visibleNeighbors = 0;
                int alphaSum = 0;
                for (int offsetY = -1; offsetY <= 1; offsetY++)
                {
                    for (int offsetX = -1; offsetX <= 1; offsetX++)
                    {
                        if (offsetX == 0 && offsetY == 0)
                        {
                            continue;
                        }
                        final int neighborAlpha = source[(y + offsetY) * side + x + offsetX];
                        if (neighborAlpha >= MIN_VISIBLE_ARC_ALPHA)
                        {
                            visibleNeighbors++;
                            alphaSum += neighborAlpha;
                        }
                    }
                }
                if (visibleNeighbors >= 5)
                {
                    target[index] = alphaSum / visibleNeighbors;
                }
            }
        }
    }

    static int mineralColorTier(int visibleMinerals, int hiddenMinerals)
    {
        final int visible = Mth.clamp(visibleMinerals, 0, 4);
        final int hidden = Mth.clamp(hiddenMinerals, 0, 4);
        return Math.min(4, visible + hidden);
    }

    static double guideTransitionProgress(long now, long transitionStartedAtNanos)
    {
        if (transitionStartedAtNanos <= 0L)
        {
            return 1D;
        }
        final double linearProgress = Mth.clamp(
            (now - transitionStartedAtNanos) / (double) GUIDE_TRANSITION_NANOS,
            0D,
            1D
        );
        return linearProgress * linearProgress * (3D - 2D * linearProgress);
    }

    static int lerpRgb(int fromRgb, int toRgb, double progress)
    {
        final double clampedProgress = Mth.clamp(progress, 0D, 1D);
        final int red = (int) Math.round(Mth.lerp(clampedProgress, fromRgb >> 16 & 0xFF, toRgb >> 16 & 0xFF));
        final int green = (int) Math.round(Mth.lerp(clampedProgress, fromRgb >> 8 & 0xFF, toRgb >> 8 & 0xFF));
        final int blue = (int) Math.round(Mth.lerp(clampedProgress, fromRgb & 0xFF, toRgb & 0xFF));
        return red << 16 | green << 8 | blue;
    }

    static double lerpRadians(double fromRadians, double toRadians, double progress)
    {
        final double wrappedDelta = Math.toRadians(Mth.wrapDegrees(Math.toDegrees(toRadians - fromRadians)));
        return fromRadians + wrappedDelta * Mth.clamp(progress, 0D, 1D);
    }

    static long markerCycleNanos(double distance)
    {
        final double distanceFraction = Mth.clamp(
            (distance - FULL_RING_DISTANCE) / (MAX_NAVIGATION_DISTANCE - FULL_RING_DISTANCE),
            0D,
            1D
        );
        return Math.round(Mth.lerp(distanceFraction, (double) MIN_MARKER_CYCLE_NANOS, (double) MAX_MARKER_CYCLE_NANOS));
    }

    static MarkerAnimation markerAnimation(double phase)
    {
        final double wrappedPhase = phase - Math.floor(phase);
        return wrappedPhase <= 0.5D
            ? new MarkerAnimation(wrappedPhase * 2D, true)
            : new MarkerAnimation((wrappedPhase - 0.5D) * 2D, false);
    }

    static double markerBorderActivation(MarkerShape shape, int y)
    {
        final int minY = markerMinY(shape) - 1;
        final int maxY = markerMaxY(shape) + 1;
        return switch (shape)
        {
            case UP -> (maxY - y) / (double) (maxY - minY);
            case DOWN -> (y - minY) / (double) (maxY - minY);
            case DIAMOND -> 1D - Math.abs(y) / (double) maxY;
        };
    }

    static boolean markerBorderActive(MarkerShape shape, int y, MarkerAnimation animation)
    {
        final double activation = markerBorderActivation(shape, y);
        if (animation.lighting())
        {
            return animation.sweep() >= 1D || animation.sweep() > 0D && activation <= animation.sweep();
        }
        return animation.sweep() <= 0D || animation.sweep() < 1D && activation > animation.sweep();
    }

    static double relativeDirection(double playerX, double playerZ, float playerYaw, double targetX, double targetZ)
    {
        final double targetYaw = Math.toDegrees(Math.atan2(playerX - targetX, targetZ - playerZ));
        return Math.toRadians(Mth.wrapDegrees(targetYaw - playerYaw));
    }

    private static MarkerAnimation advanceMarkerAnimation(long now, double distance)
    {
        if (lastMarkerAnimationNanos <= 0L)
        {
            lastMarkerAnimationNanos = now;
        }
        final long elapsedNanos = Math.max(0L, now - lastMarkerAnimationNanos);
        markerAnimationPhase = (markerAnimationPhase + elapsedNanos / (double) markerCycleNanos(distance)) % 1D;
        lastMarkerAnimationNanos = now;
        return markerAnimation(markerAnimationPhase);
    }

    private static float lifetimeAlpha(long remainingNanos)
    {
        return remainingNanos < 250_000_000L ? Mth.clamp(remainingNanos / 250_000_000F, 0F, 1F) : 1F;
    }

    private static void drawTaperedArc(
        GuiGraphics graphics,
        int centerX,
        int centerY,
        double direction,
        double fraction,
        double halfSpan,
        int arcSegments,
        float lifetimeAlpha,
        int rgb
    )
    {
        Arrays.fill(RING_ALPHA_BUFFER, 0);
        final boolean fullRing = fraction >= 0.999D;
        final int samples = fullRing ? arcSegments : arcSegments + 1;
        for (int i = 0; i < samples; i++)
        {
            final double unit = i / (double) arcSegments;
            final double offset = Mth.lerp(unit, -halfSpan, halfSpan);
            final double normalizedOffset = Math.abs(offset) / Math.max(halfSpan, 1.0E-6D);
            final double bell = 0.5D + 0.5D * Math.cos(Math.PI * normalizedOffset);
            final double intensity = fullRing ? 0.2D + 0.8D * bell : bell;
            final double angle = direction + offset;
            final double halfCoreWidth = ringThickness(fraction, normalizedOffset) * SUBPIXEL_SCALE * 0.5D;
            final int radialExtent = Math.min(
                MAX_RING_RADIAL_OFFSET,
                Mth.ceil(halfCoreWidth + RING_BLUR_SUBPIXELS)
            );
            for (int radialOffset = -radialExtent; radialOffset <= radialExtent; radialOffset++)
            {
                final double outsideCore = Math.abs(radialOffset) - halfCoreWidth;
                final double radialFade = outsideCore <= 0D
                    ? 1D
                    : Mth.clamp(1D - outsideCore / RING_BLUR_SUBPIXELS, 0D, 1D);
                final int alpha = Mth.clamp((int) Math.round(242D * intensity * radialFade * lifetimeAlpha), 0, 255);
                if (alpha < MIN_VISIBLE_ARC_ALPHA)
                {
                    continue;
                }

                final int radius = RING_RADIUS + radialOffset;
                final int x = (int) Math.round(Math.sin(angle) * radius);
                final int y = (int) Math.round(-Math.cos(angle) * radius);
                if (Math.abs(x) > RING_BUFFER_RADIUS || Math.abs(y) > RING_BUFFER_RADIUS)
                {
                    continue;
                }
                final int index = (y + RING_BUFFER_RADIUS) * RING_BUFFER_SIDE + x + RING_BUFFER_RADIUS;
                RING_ALPHA_BUFFER[index] = Math.max(RING_ALPHA_BUFFER[index], alpha);
            }
        }

        fillEnclosedRingGaps(RING_ALPHA_BUFFER, RING_RESOLVE_BUFFER, RING_BUFFER_SIDE);
        fillEnclosedRingGaps(RING_RESOLVE_BUFFER, RING_ALPHA_BUFFER, RING_BUFFER_SIDE);

        for (int y = -RING_BUFFER_RADIUS; y <= RING_BUFFER_RADIUS; y++)
        {
            final int rowOffset = (y + RING_BUFFER_RADIUS) * RING_BUFFER_SIDE + RING_BUFFER_RADIUS;
            for (int x = -RING_BUFFER_RADIUS; x <= RING_BUFFER_RADIUS; x++)
            {
                final int alpha = RING_ALPHA_BUFFER[rowOffset + x];
                if (alpha >= MIN_VISIBLE_ARC_ALPHA)
                {
                    drawRingPixel(graphics, centerX + x, centerY + y, color(alpha, rgb));
                }
            }
        }
    }

    private static void drawRingPixel(GuiGraphics graphics, int x, int y, int color)
    {
        graphics.fill(x, y, x + 1, y + 1, color);
    }

    static MarkerShape markerShape(double verticalOffset)
    {
        return verticalOffset > 1D ? MarkerShape.UP : verticalOffset < -1D ? MarkerShape.DOWN : MarkerShape.DIAMOND;
    }

    private static void drawTransitioningMarker(
        GuiGraphics graphics,
        int x,
        int y,
        MarkerShape fromShape,
        MarkerShape toShape,
        double transitionProgress,
        MarkerAnimation animation,
        float lifetimeAlpha,
        int fillRgb
    )
    {
        final float progress = (float) Mth.clamp(transitionProgress, 0D, 1D);
        if (fromShape == toShape || progress >= 1F)
        {
            drawMarker(graphics, x, y, toShape, animation, lifetimeAlpha, fillRgb);
            return;
        }
        if (progress <= 0F)
        {
            drawMarker(graphics, x, y, fromShape, animation, lifetimeAlpha, fillRgb);
            return;
        }
        drawMarker(graphics, x, y, fromShape, animation, lifetimeAlpha * (1F - progress), fillRgb);
        drawMarker(graphics, x, y, toShape, animation, lifetimeAlpha * progress, fillRgb);
    }

    private static void drawMarker(
        GuiGraphics graphics,
        int centerX,
        int centerY,
        MarkerShape shape,
        MarkerAnimation animation,
        float lifetimeAlpha,
        int fillRgb
    )
    {
        final int minY = markerMinY(shape);
        final int maxY = markerMaxY(shape);
        final int maxHalfWidth = markerMaxHalfWidth(shape);
        for (int markerY = minY - 1; markerY <= maxY + 1; markerY++)
        {
            for (int markerX = -maxHalfWidth - 1; markerX <= maxHalfWidth + 1; markerX++)
            {
                if (!isMarkerBorder(shape, markerX, markerY))
                {
                    continue;
                }
                final boolean active = markerBorderActive(shape, markerY, animation);
                final int alpha = (int) ((active ? 255F : 210F) * lifetimeAlpha);
                final int rgb = active ? ACTIVE_MARKER_BORDER_RGB : INACTIVE_MARKER_BORDER_RGB;
                graphics.fill(centerX + markerX, centerY + markerY, centerX + markerX + 1, centerY + markerY + 1, color(alpha, rgb));
            }
        }

        final int fillColor = color((int) (255F * lifetimeAlpha), fillRgb);
        for (int markerY = minY; markerY <= maxY; markerY++)
        {
            final int halfWidth = markerHalfWidth(shape, markerY);
            graphics.fill(
                centerX - halfWidth,
                centerY + markerY,
                centerX + halfWidth + 1,
                centerY + markerY + 1,
                fillColor
            );
        }
    }

    private static boolean isMarkerBorder(MarkerShape shape, int x, int y)
    {
        if (isMarkerInterior(shape, x, y))
        {
            return false;
        }
        for (int offsetY = -1; offsetY <= 1; offsetY++)
        {
            for (int offsetX = -1; offsetX <= 1; offsetX++)
            {
                if ((offsetX != 0 || offsetY != 0) && isMarkerInterior(shape, x + offsetX, y + offsetY))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isMarkerInterior(MarkerShape shape, int x, int y)
    {
        final int halfWidth = markerHalfWidth(shape, y);
        return halfWidth >= 0 && Math.abs(x) <= halfWidth;
    }

    private static int markerHalfWidth(MarkerShape shape, int y)
    {
        return switch (shape)
        {
            case UP -> y < -12 || y > 9 ? -1 : y <= -6 ? y + 13 : 2;
            case DOWN -> y < -9 || y > 12 ? -1 : y >= 6 ? 13 - y : 2;
            case DIAMOND -> y < -6 || y > 6 ? -1 : 6 - Math.abs(y);
        };
    }

    private static int markerMinY(MarkerShape shape)
    {
        return switch (shape)
        {
            case UP -> -12;
            case DOWN -> -9;
            case DIAMOND -> -6;
        };
    }

    private static int markerMaxY(MarkerShape shape)
    {
        return switch (shape)
        {
            case UP -> 9;
            case DOWN -> 12;
            case DIAMOND -> 6;
        };
    }

    private static int markerMaxHalfWidth(MarkerShape shape)
    {
        return shape == MarkerShape.DIAMOND ? 6 : 7;
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
        clearTarget();
        actionbarLines = List.of();
        actionbarExpiresAtNanos = 0L;
    }

    private static void clearTarget()
    {
        target = null;
        guideSignature = null;
        directionExpiresAtNanos = 0L;
        lastMarkerAnimationNanos = 0L;
        markerAnimationPhase = 0D;
        guideTransitionStartedAtNanos = 0L;
        transitionFromTargetX = 0D;
        transitionFromTargetY = 0D;
        transitionFromTargetZ = 0D;
        transitionToTargetX = 0D;
        transitionToTargetY = 0D;
        transitionToTargetZ = 0D;
        transitionFromArcRgb = 0;
        transitionFromMarkerRgb = 0;
        transitionToArcRgb = 0;
        transitionToMarkerRgb = 0;
    }
}
