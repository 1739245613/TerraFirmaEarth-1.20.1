package com.newterraearth.tfe.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.dries007.tfc.client.screen.CalendarScreen;
import net.dries007.tfc.client.screen.TFCContainerScreen;
import net.dries007.tfc.common.container.Container;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.calendar.Month;

import com.newterraearth.tfe.client.NTEClimateRenderHelpers;

@Mixin(value = CalendarScreen.class, remap = false)
public abstract class CalendarScreenMixin extends TFCContainerScreen<Container>
{
    protected CalendarScreenMixin(Container container, Inventory playerInv, Component name, ResourceLocation texture)
    {
        super(container, playerInv, name, texture);
    }

    /**
     * @author Codex
     * @reason Calendar season text should respect the local hemisphere like the 1.21 calendar UI.
     */
    @Overwrite(remap = false)
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
    {
        super.renderLabels(graphics, mouseX, mouseY);

        final Minecraft minecraft = Minecraft.getInstance();
        final Level level = minecraft.level;
        final Player player = minecraft.player;
        final Month month = level != null && player != null
            ? NTEClimateRenderHelpers.getHemispheralCalendarMonthOfYear(level, player.blockPosition())
            : Calendars.CLIENT.getCalendarMonthOfYear();

        final String season = I18n.get("tfc.tooltip.calendar_season", I18n.get(month.getTranslationKey(Month.Style.SEASON)));
        final String day = I18n.get("tfc.tooltip.calendar_day", Calendars.CLIENT.getCalendarDayOfYear().getString());
        final String date = I18n.get("tfc.tooltip.calendar_date", Calendars.CLIENT.getCalendarTimeAndDate().getString());

        graphics.drawString(font, season, (imageWidth - font.width(season)) / 2, 25, 0x404040, false);
        graphics.drawString(font, day, (imageWidth - font.width(day)) / 2, 36, 0x404040, false);
        graphics.drawString(font, date, (imageWidth - font.width(date)) / 2, 47, 0x404040, false);
    }
}
