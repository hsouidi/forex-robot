package com.trading.forex.indicators.impl;

public class ATRCustom {

    //--- input parameter
    int InpAtrPeriod = 14; // ATR Period
    //--- buffers
    double ExtATRBuffer[];
    double ExtTRBuffer[];

    //+------------------------------------------------------------------+
//| Average True Range                                               |
//+------------------------------------------------------------------+
    double[] OnCalculate(
            double high[],
            double low[],
            double close[]) {
        int rates_total = close.length;
        int i, limit;
//--- check for bars count and input parameter
//--- counting from 0 to rates_total
        ExtATRBuffer=new double[rates_total];
        ExtTRBuffer=new double[rates_total];


        ExtTRBuffer[0] = 0.0;
        ExtATRBuffer[0] = 0.0;
        //--- filling out the array of True Range values for each period
        for (i = 1; i < rates_total; i++)
            ExtTRBuffer[i] = Math.max(high[i], close[i - 1]) - Math.min(low[i], close[i - 1]);
        //--- first AtrPeriod values of the indicator are not calculated
        double firstValue = 0.0;
        for (i = 1; i <= InpAtrPeriod; i++) {
            ExtATRBuffer[i] = 0.0;
            firstValue += ExtTRBuffer[i];
        }
        //--- calculating the first value of the indicator
        firstValue /= InpAtrPeriod;
        ExtATRBuffer[InpAtrPeriod] = firstValue;
        limit = InpAtrPeriod + 1;
//--- the main loop of calculations
        for (i = limit; i < rates_total; i++) {
            ExtTRBuffer[i] =Math.max(high[i], close[i - 1]) - Math.min(low[i], close[i - 1]);
            ExtATRBuffer[i] = ExtATRBuffer[i - 1] + (ExtTRBuffer[i] - ExtTRBuffer[i - InpAtrPeriod]) / InpAtrPeriod;
        }
//--- return value of prev_calculated for next call
        return ExtATRBuffer;
    }


}
