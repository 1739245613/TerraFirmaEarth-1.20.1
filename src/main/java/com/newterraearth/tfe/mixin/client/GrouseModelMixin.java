package com.newterraearth.tfe.mixin.client;

import net.dries007.tfc.client.ClientCalendar;
import net.dries007.tfc.client.model.entity.GrouseModel;
import net.dries007.tfc.util.calendar.ICalendar;
import net.dries007.tfc.util.calendar.Month;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.newterraearth.tfe.client.NTEClimateRenderHelpers;

@Mixin(value = GrouseModel.class, remap = false)
public abstract class GrouseModelMixin
{
    /**
     * @author Codex
     * @reason Grouse client animation season should respect the local hemisphere like 1.21.
     */
    @Redirect(
        method = "setupAnim",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/util/calendar/ICalendar;getCalendarMonthOfYear()Lnet/dries007/tfc/util/calendar/Month;"
        ),
        require = 0,
        remap = false
    )
    private Month tfe$useHemispheralMonth(ICalendar calendar)
    {
        return NTEClimateRenderHelpers.getClientHemispheralCalendarMonthOfYear();
    }

    @Redirect(
        method = "setupAnim",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/client/ClientCalendar;getCalendarMonthOfYear()Lnet/dries007/tfc/util/calendar/Month;"
        ),
        require = 0,
        remap = false
    )
    private Month tfe$useHemispheralMonth319(ClientCalendar calendar)
    {
        return NTEClimateRenderHelpers.getClientHemispheralCalendarMonthOfYear();
    }
}
