package com.trading.forex.indicators.impl;

import com.trading.forex.common.model.Candle;
import com.trading.forex.common.utils.CustomList;
import com.trading.forex.model.Trend;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.List;

@Slf4j
public class ZigZag {

    //--- input parameters
    private static int ExtDepth = 12;
    private static int ExtDeviation = 5;
    private static int ExtBackstep = 3;
    static int level = 3;             // recounting depth
    static double deviation;           // deviation in points

    //+------------------------------------------------------------------+
//|  searching index of the highest bar                              |
//+------------------------------------------------------------------+
    private static int iHighest(double array[],
                 int depth,
                 int startPos) {
        int index = startPos;
//--- start index validation
        if (startPos < 0) {
            log.info("Invalid parameter in the function iHighest, startPos =", startPos);
            return 0;
        }
        int size = array.length;
//--- depth correction if need
        if (startPos - depth < 0) depth = startPos;
        double max = array[startPos];
//--- start searching
        for (int i = startPos; i > startPos - depth; i--) {
            if (array[i] > max) {
                index = i;
                max = array[i];
            }
        }
//--- return index of the highest bar
        return (index);
    }

    //+------------------------------------------------------------------+
//|  searching index of the lowest bar                               |
//+------------------------------------------------------------------+
    private static int iLowest(double array[],
                int depth,
                int startPos) {
        int index = startPos;
//--- start index validation
        if (startPos < 0) {
            log.info("Invalid parameter in the function iLowest, startPos =", startPos);
            return 0;
        }
        int size = array.length;
//--- depth correction if need
        if (startPos - depth < 0) depth = startPos;
        double min = array[startPos];
//--- start searching
        for (int i = startPos; i > startPos - depth; i--) {
            if (array[i] < min) {
                index = i;
                min = array[i];
            }
        }
//--- return index of the lowest bar
        return (index);
    }

    //+------------------------------------------------------------------+
//| Custom indicator iteration function                              |
//+------------------------------------------------------------------+
    public static CustomList<TrendInfo> values(List<Candle> candles)

    {
//--- to use in cycle
        deviation = ExtDeviation * 3;
        int prev_calculated = 0;
        int rates_total = candles.size();
        double[] high = candles.stream().mapToDouble(candle -> candle.getHigh()).toArray();
        double[] low = candles.stream().mapToDouble(candle -> candle.getLow()).toArray();
        int i = 0;
        int limit = 0, counterZ = 0, whatlookfor = 0;
        int shift = 0, back = 0, lasthighpos = 0, lastlowpos = 0;
        double val = 0, res = 0;
        double curlow = 0, curhigh = 0, lasthigh = 0, lastlow = 0;
//--- auxiliary enumeration
        int Pike = 1;  // searching for next high
        int Sill = -1;  // searching for next low
        //--- indicator buffers
        double[] ZigzagBuffer = new double[rates_total];      // main buffer
        double[] HighMapBuffer = new double[rates_total];
        ;     // highs
        double[] LowMapBuffer = new double[rates_total];
        ;

//--- set start position for calculations
        if (prev_calculated == 0) limit = ExtDepth;

//--- ZigZag was already counted before
        if (prev_calculated > 0) {
            i = rates_total - 1;
            //--- searching third extremum from the last uncompleted bar
            while (counterZ < level && i > rates_total - 100) {
                res = ZigzagBuffer[i];
                if (res != 0) counterZ++;
                i--;
            }
            i++;
            limit = i;

            //--- what type of exremum we are going to find
            if (LowMapBuffer[i] != 0) {
                curlow = LowMapBuffer[i];
                whatlookfor = Pike;
            } else {
                curhigh = HighMapBuffer[i];
                whatlookfor = Sill;
            }
            //--- chipping
            for (i = limit + 1; i < rates_total; i++) {
                ZigzagBuffer[i] = 0.0;
                LowMapBuffer[i] = 0.0;
                HighMapBuffer[i] = 0.0;
            }
        }

//--- searching High and Low
        for (shift = limit; shift < rates_total; shift++) {
            val = low[iLowest(low, ExtDepth, shift)];
            if (val == lastlow) val = 0.0;
            else {
                lastlow = val;
                if ((low[shift] - val) > deviation) val = 0.0;
                else {
                    for (back = 1; back <= ExtBackstep; back++) {
                        res = LowMapBuffer[shift - back];
                        if ((res != 0) && (res > val)) LowMapBuffer[shift - back] = 0.0;
                    }
                }
            }
            if (low[shift] == val) LowMapBuffer[shift] = val;
            else LowMapBuffer[shift] = 0.0;
            //--- high
            val = high[iHighest(high, ExtDepth, shift)];
            if (val == lasthigh) val = 0.0;
            else {
                lasthigh = val;
                if ((val - high[shift]) > deviation) val = 0.0;
                else {
                    for (back = 1; back <= ExtBackstep; back++) {
                        res = HighMapBuffer[shift - back];
                        if ((res != 0) && (res < val)) HighMapBuffer[shift - back] = 0.0;
                    }
                }
            }
            if (high[shift] == val) HighMapBuffer[shift] = val;
            else HighMapBuffer[shift] = 0.0;
        }

//--- last preparation
        if (whatlookfor == 0)// uncertain quantity
        {
            lastlow = 0;
            lasthigh = 0;
        } else {
            lastlow = curlow;
            lasthigh = curhigh;
        }

//--- final rejection
        for (shift = limit; shift < rates_total; shift++) {
            res = 0.0;
            switch (whatlookfor) {
                case 0: // search for peak or lawn
                    if (lastlow == 0 && lasthigh == 0) {
                        if (HighMapBuffer[shift] != 0) {
                            lasthigh = high[shift];
                            lasthighpos = shift;
                            whatlookfor = Sill;
                            ZigzagBuffer[shift] = lasthigh;
                            res = 1;
                        }
                        if (LowMapBuffer[shift] != 0) {
                            lastlow = low[shift];
                            lastlowpos = shift;
                            whatlookfor = Pike;
                            ZigzagBuffer[shift] = lastlow;
                            res = 1;
                        }
                    }
                    break;
                case 1: // Pike = 1 search for peak
                    if (LowMapBuffer[shift] != 0.0 && LowMapBuffer[shift] < lastlow && HighMapBuffer[shift] == 0.0) {
                        ZigzagBuffer[lastlowpos] = 0.0;
                        lastlowpos = shift;
                        lastlow = LowMapBuffer[shift];
                        ZigzagBuffer[shift] = lastlow;
                        res = 1;
                    }
                    if (HighMapBuffer[shift] != 0.0 && LowMapBuffer[shift] == 0.0) {
                        lasthigh = HighMapBuffer[shift];
                        lasthighpos = shift;
                        ZigzagBuffer[shift] = lasthigh;
                        whatlookfor = Sill;
                        res = 1;
                    }
                    break;
                case -1: // Sill = -1 search for lawn
                    if (HighMapBuffer[shift] != 0.0 && HighMapBuffer[shift] > lasthigh && LowMapBuffer[shift] == 0.0) {
                        ZigzagBuffer[lasthighpos] = 0.0;
                        lasthighpos = shift;
                        lasthigh = HighMapBuffer[shift];
                        ZigzagBuffer[shift] = lasthigh;
                    }
                    if (LowMapBuffer[shift] != 0.0 && HighMapBuffer[shift] == 0.0) {
                        lastlow = LowMapBuffer[shift];
                        lastlowpos = shift;
                        ZigzagBuffer[shift] = lastlow;
                        whatlookfor = Pike;
                    }
                    break;
                default:
                    break;
            }
        }
        return normalize(ZigzagBuffer,candles);
    }

    private static CustomList<TrendInfo> normalize(double[] ZigzagBuffer,List<Candle> candles){

        final CustomList<TrendInfo>  result=new CustomList<>();

        double startPrice=0.0;
        int weight=1;
        int index=0;
        TrendInfo previousTrendInfo=null;
        Candle begin=null;
        for(double value:ZigzagBuffer){
            final Candle last=candles.get(index);
            if(value!=0.0){
                if(previousTrendInfo==null){
                    begin=last;
                    previousTrendInfo=new TrendInfo(Trend.UNKNOWN,weight,startPrice,value,null,last.date());
                    startPrice=value;
                    weight=1;
                }else{
                    previousTrendInfo=new TrendInfo(value>startPrice?Trend.UP :Trend.DOWN,weight,startPrice,value,begin.date(),last.date());
                    result.add(previousTrendInfo);
                    startPrice=value;
                    begin=last;
                    weight=1;
                }
            }else{
                weight++;
            }
            index++;

        }
        return result;
    }

    @Data
    @AllArgsConstructor
    public static class TrendInfo {
        private Trend trend;
        private Integer weight;
        private double startPrice;
        private double endPrice;
        private Date start;
        private Date end;

        public double diff(){
            return startPrice-endPrice;
        }
    }

}
