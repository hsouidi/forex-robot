package com.trading.forex.indicators.impl;

import lombok.AllArgsConstructor;
import lombok.Data;

public class ADXCustom {

    double ExtADXBuffer[];
    double ExtPDIBuffer[];
    double ExtNDIBuffer[];
    double ExtPDBuffer[];
    double ExtNDBuffer[];
    double ExtTmpBuffer[];
    //--- global variables
    int ExtADXPeriod;

    ADXResult OnCalculate(double high[],
                         double low[],
                         double close[],int periode) {
        int rates_total = high.length;
        this.ExtADXPeriod=periode;
//--- checking for bars count
        if (high.length < ExtADXPeriod)
            return new ADXResult (new double[]{},new double[]{},new double[]{});
//--- detect start position
        int start = 1;
        ExtADXBuffer = new double[rates_total];
        ExtPDIBuffer = new double[rates_total];
        ExtNDIBuffer = new double[rates_total];
        ExtPDBuffer = new double[rates_total];
        ExtNDBuffer = new double[rates_total];
        ExtTmpBuffer = new double[rates_total];
        ExtPDIBuffer[0] = 0.0;
        ExtNDIBuffer[0] = 0.0;
        ExtADXBuffer[0] = 0.0;
//--- main cycle
        for (int i = start; i < rates_total; i++) {
            //--- get some data
            double Hi = high[i];
            double prevHi = high[i - 1];
            double Lo = low[i];
            double prevLo = low[i - 1];
            double prevCl = close[i - 1];
            //--- fill main positive and main negative buffers
            double dTmpP = Hi - prevHi;
            double dTmpN = prevLo - Lo;
            if (dTmpP < 0.0) dTmpP = 0.0;
            if (dTmpN < 0.0) dTmpN = 0.0;
            if (dTmpP > dTmpN) dTmpN = 0.0;
            else {
                if (dTmpP < dTmpN) dTmpP = 0.0;
                else {
                    dTmpP = 0.0;
                    dTmpN = 0.0;
                }
            }
            //--- define TR
            double tr = Math.max(Math.max(Math.abs(Hi - Lo), Math.abs(Hi - prevCl)), Math.abs(Lo - prevCl));
            //---
            if (tr != 0.0) {
                ExtPDBuffer[i] = 100.0 * dTmpP / tr;
                ExtNDBuffer[i] = 100.0 * dTmpN / tr;
            } else {
                ExtPDBuffer[i] = 0.0;
                ExtNDBuffer[i] = 0.0;
            }
            //--- fill smoothed positive and negative buffers
            ExtPDIBuffer[i] = ExponentialMA(i, ExtADXPeriod, ExtPDIBuffer[i - 1], ExtPDBuffer);
            ExtNDIBuffer[i] = ExponentialMA(i, ExtADXPeriod, ExtNDIBuffer[i - 1], ExtNDBuffer);
            //--- fill ADXTmp buffer
            double dTmp = ExtPDIBuffer[i] + ExtNDIBuffer[i];
            if (dTmp != 0.0)
                dTmp = 100.0 * Math.abs((ExtPDIBuffer[i] - ExtNDIBuffer[i]) / dTmp);
            else
                dTmp = 0.0;
            ExtTmpBuffer[i] = dTmp;
            //--- fill smoothed ADX buffer
            ExtADXBuffer[i] = ExponentialMA(i, ExtADXPeriod, ExtADXBuffer[i - 1], ExtTmpBuffer);
        }
//---- OnCalculate done. Return new prev_calculated.
        return new ADXResult(ExtADXBuffer,ExtPDIBuffer,ExtNDBuffer);
    }


    double ExponentialMA(int position, int period, double prev_value, double price[]) {
//---
        double result = 0.0;
//--- calculate value
        if (period > 0) {
            double pr = 2.0 / (period + 1.0);
            result = price[position] * pr + prev_value * (1 - pr);
        }
//---
        return (result);
    }

    @Data
    @AllArgsConstructor
    public static class ADXResult{

        private double[] adx;
        private double[] pdi;
        private double[] ndi;

    }


    @Data
    @AllArgsConstructor
    public static class ADXResultValue{

        private double adx;
        private double pdi;
        private double ndi;

    }
}
