package com.trading.forex.indicators.impl;

/**
 * Created by hsouidi on 12/02/2017.
 */
public class StochCustom {

    //--- input parameters
    int InpKPeriod = 5; // K Period
    int InpDPeriod = 3; // D Period
    int InpSlowing = 3; // Slowing
    //--- buffers
    double ExtMainBuffer[];
    double ExtSignalBuffer[];
    double ExtHighesBuffer[];
    double ExtLowesBuffer[];
    //---

    //+------------------------------------------------------------------+
//| Custom indicator initialization function                         |
//+------------------------------------------------------------------+

    //+------------------------------------------------------------------+
//| Stochastic oscillator                                            |
//+------------------------------------------------------------------+
    Stoch.StochResult OnCalculate(double high[],
                         double low[],
                         double close[]) {
        int rates_total = high.length;
        ExtHighesBuffer = new double[rates_total];
        ExtLowesBuffer = new double[rates_total];
        ExtMainBuffer = new double[rates_total];
        ExtSignalBuffer= new double[rates_total];


        int i, k, pos;
//--- check for bars count
        if (rates_total <= InpKPeriod + InpDPeriod + InpSlowing)
            return new Stoch.StochResult(new double[0],new double[0]);
//--- counting from 0 to rates_total
//---
        pos = InpKPeriod - 1;
        for (i = 0; i < pos; i++) {
            ExtLowesBuffer[i] = 0.0;
            ExtHighesBuffer[i] = 0.0;
        }
//--- calculate HighesBuffer[] and ExtHighesBuffer[]
        for (i = pos; i < rates_total; i++) {
            double dmin = 1000000.0;
            double dmax = -1000000.0;
            for (k = i - InpKPeriod + 1; k <= i; k++) {
                if (dmin > low[k])
                    dmin = low[k];
                if (dmax < high[k])
                    dmax = high[k];
            }
            ExtLowesBuffer[i] = dmin;
            ExtHighesBuffer[i] = dmax;
        }
//--- %K line
        pos = InpKPeriod - 1 + InpSlowing - 1;
        for (i = 0; i < pos; i++)
            ExtMainBuffer[i] = 0.0;
//--- main cycle
        for (i = pos; i < rates_total; i++) {
            double sumlow = 0.0;
            double sumhigh = 0.0;
            for (k = (i - InpSlowing + 1); k <= i; k++) {
                sumlow += (close[k] - ExtLowesBuffer[k]);
                sumhigh += (ExtHighesBuffer[k] - ExtLowesBuffer[k]);
            }
            if (sumhigh == 0.0)
                ExtMainBuffer[i] = 100.0;
            else
                ExtMainBuffer[i] = sumlow / sumhigh * 100.0;
        }
//--- signal
        pos = InpDPeriod - 1;
        for (i = 0; i < pos; i++)
            ExtSignalBuffer[i] = 0.0;
        for (i = pos; i < rates_total; i++) {
            double sum = 0.0;
            for (k = 0; k < InpDPeriod; k++)
                sum += ExtMainBuffer[i - k];
            ExtSignalBuffer[i] = sum / InpDPeriod;
        }
//--- OnCalculate done. Return new prev_calculated.
        return new Stoch.StochResult(ExtSignalBuffer,ExtMainBuffer);
    }

}
