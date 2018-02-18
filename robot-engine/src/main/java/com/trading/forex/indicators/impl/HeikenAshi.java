package com.trading.forex.indicators.impl;

import com.trading.forex.common.model.Candle;
import com.trading.forex.common.utils.CustomList;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

public class HeikenAshi {

    public static CustomList <HeikenAshiBar> OnCalculate(List<Candle> candles) {
        int i, pos;
        double[] ExtLowHighBuffer = new double[candles.size()];
        double[] ExtHighLowBuffer=new double[candles.size()];
        double[] ExtOpenBuffer=new double[candles.size()];
        double[] ExtCloseBuffer=new double[candles.size()];

        double haOpen, haHigh, haLow, haClose;
        double[] open=candles.stream().mapToDouble(candle -> candle.getOpen()).toArray();
        double[] high=candles.stream().mapToDouble(candle -> candle.getHigh()).toArray();
        double[] low=candles.stream().mapToDouble(candle -> candle.getLow()).toArray();
        double[] close=candles.stream().mapToDouble(candle -> candle.getClose()).toArray();
        //--- set first candle
        if (open[0] < close[0]) {
            ExtLowHighBuffer[0] = low[0];
            ExtHighLowBuffer[0] = high[0];
        } else {
            ExtLowHighBuffer[0] = high[0];
            ExtHighLowBuffer[0] = low[0];
        }
        ExtOpenBuffer[0] = open[0];
        ExtCloseBuffer[0] = close[0];
        //---
        pos = 1;

//--- main loop of calculations
        for (i = pos; i < close.length; i++) {
            haOpen = (ExtOpenBuffer[i - 1] + ExtCloseBuffer[i - 1]) / 2;
            haClose = (open[i] + high[i] + low[i] + close[i]) / 4;
            haHigh = Math.max(high[i], Math.max(haOpen, haClose));
            haLow = Math.min(low[i], Math.min(haOpen, haClose));
            if (haOpen < haClose) {
                ExtLowHighBuffer[i] = haLow;
                ExtHighLowBuffer[i] = haHigh;
            } else {
                ExtLowHighBuffer[i] = haHigh;
                ExtHighLowBuffer[i] = haLow;
            }
            ExtOpenBuffer[i] = haOpen;
            ExtCloseBuffer[i] = haClose;
        }
//--- done
        return buildResult(ExtOpenBuffer,ExtCloseBuffer);
    }

    private static CustomList <HeikenAshiBar> buildResult(double[] open,double[] close){
        final CustomList<HeikenAshiBar> result=new CustomList();
        for(int i=0;i< open.length;i++){
            result.add(new HeikenAshiBar(open[i],close[i]));
        }
        return result;
    }

    @Data
    public static class HeikenAshiBar{

        public HeikenAshiBar(double open,double close){
            this.open=open;
            this.close=close;
            this.body=close-open;
        }

        private double open;
        private double close;
        private double body;

    }



}
