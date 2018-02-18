package com.trading.forex.indicators.impl;

import com.trading.forex.common.model.Candle;
import com.trading.forex.common.utils.CustomList;
import com.trading.forex.model.PivotPointResult;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by hsouidi on 11/28/2017.
 */
@Slf4j
public class PivotPoint {


    public static PivotPointResult calcul(CustomList<Candle> candles) {

        final Candle last=candles.getLast();
        double low=last.getLow();
        double high=last.getHigh();
        for(Candle candle:candles){
            if(candle.getLow()<low){
                low=candle.getLow();
            }
            if(candle.getHigh()>high){
                high=candle.getHigh();
            }
       }

       return calcul(last.getClose(),high,low);

    }


    public static PivotPointResult calcul(Candle jm1Candle){
            return calcul(jm1Candle.getClose(),jm1Candle.getHigh(),jm1Candle.getLow());
    }

    private  static PivotPointResult calcul(double c,double h,double l) {
        log.info("Calcul point pivot  close={} , high={}  , low={}",c,h,l);
        double pivot = (l + h + c ) / 3;

        double s1 = (pivot * 2) - h;
        double s2 = pivot - (h - l);
        double s3 = l - 2 * (h - pivot);

        double r1 = (pivot * 2) - l;
        double r2 = pivot + (h - l);
        double r3 = h + 2 * (pivot - l);

        double ps1 = 100 * (s1 - c) / c;
        double ps2 = 100 * (s2 - c) / c;
        double ps3 = 100 * (s3 - c) / c;

        double pr1 = 100 * (r1 - c) / c;
        double pr2 = 100 * (r2 - c) / c;
        double pr3 = 100 * (r3 - c) / c;

        return new PivotPointResult( r1,  r2,  r3,  s1,  s2,  s3,  pivot,  pr1,  pr2,  pr3,  ps1,  ps2,  ps3);
    }
}
