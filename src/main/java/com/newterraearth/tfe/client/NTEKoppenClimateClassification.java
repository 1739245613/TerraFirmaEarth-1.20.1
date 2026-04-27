package com.newterraearth.tfe.client;

import java.util.Locale;

public enum NTEKoppenClimateClassification
{
    AF("Af"),
    AM("Am"),
    AW("Aw"),
    AS("As"),
    BWH("BWh"),
    BWK("BWk"),
    BSH("BSh"),
    BSK("BSk"),
    CSA("Csa"),
    CSB("Csb"),
    CSC("Csc"),
    CWA("Cwa"),
    CWB("Cwb"),
    CWC("Cwc"),
    CFA("Cfa"),
    CFB("Cfb"),
    CFC("Cfc"),
    DSA("Dsa"),
    DSB("Dsb"),
    DSC("Dsc"),
    DSD("Dsd"),
    DWA("Dwa"),
    DWB("Dwb"),
    DWC("Dwc"),
    DWD("Dwd"),
    DFA("Dfa"),
    DFB("Dfb"),
    DFC("Dfc"),
    DFD("Dfd"),
    ET("ET"),
    EF("EF");

    public static NTEKoppenClimateClassification classify(float averageTemperature, float rainfall, float rainVariance, boolean northernHemisphere)
    {
        if (!northernHemisphere)
        {
            rainVariance = -rainVariance;
        }

        if (averageTemperature < -17f + 0.006f * rainfall)
        {
            return EF;
        }
        else if (averageTemperature <= -12f)
        {
            return ET;
        }
        else if (rainfall < 75f)
        {
            return averageTemperature > 18f ? BWH : BWK;
        }
        else if (rainfall < 150f)
        {
            return averageTemperature > 18f ? BSH : BSK;
        }
        else if (averageTemperature > 21f)
        {
            if (rainfall * (1f + rainVariance) > 600f)
            {
                return AM;
            }
            else if (rainVariance > 0.5f)
            {
                return AW;
            }
            else if (rainVariance < -0.5f)
            {
                return AS;
            }
            return AF;
        }
        else if (averageTemperature > 8f)
        {
            if (averageTemperature > 17f)
            {
                return rainVariance > 0.5f ? CWA : rainVariance < -0.5f ? CSA : CFA;
            }
            else if (averageTemperature > 12f)
            {
                return rainVariance > 0.5f ? CWB : rainVariance < -0.5f ? CSB : CFB;
            }
            return rainVariance > 0.5f ? CWC : rainVariance < -0.5f ? CSC : CFC;
        }
        else if (averageTemperature > 3f)
        {
            return rainVariance > 0.5f ? DWA : rainVariance < -0.5f ? DSA : DFA;
        }
        else if (averageTemperature > -2f)
        {
            return rainVariance > 0.5f ? DWB : rainVariance < -0.5f ? DSB : DFB;
        }
        else if (averageTemperature > -8f)
        {
            return rainVariance > 0.5f ? DWC : rainVariance < -0.5f ? DSC : DFC;
        }
        else if (rainVariance > 0.5f)
        {
            return DWD;
        }
        else if (rainVariance < -0.5f)
        {
            return DSD;
        }
        return DFD;
    }

    private final String displayCode;

    NTEKoppenClimateClassification(String displayCode)
    {
        this.displayCode = displayCode;
    }

    public String displayCode()
    {
        return displayCode;
    }

    public String translationKey()
    {
        return "tfc.enum.koppenclimateclassification." + name().toLowerCase(Locale.ROOT);
    }
}
