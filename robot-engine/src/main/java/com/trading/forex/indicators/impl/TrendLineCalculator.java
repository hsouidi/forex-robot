package com.trading.forex.indicators.impl;

import com.trading.forex.common.utils.AlgoUtils;
import com.trading.forex.common.utils.CustomList;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.trading.forex.common.utils.AlgoUtils.toDate;
import static com.trading.forex.common.utils.AlgoUtils.toLocalDateTime;

public class TrendLineCalculator {


    public static Envolloppe calculTrendLine(CustomList<ZigZag.TrendInfo> trendInfos){
        ZigZag.TrendInfo trendInfoEnd=trendInfos.getLast();
        ZigZag.TrendInfo trendInfoStart=trendInfos.getFirst();
        List<TrendPoint> result=new ArrayList<>();
        Date trendInfoEndD=trendInfoEnd.getEnd();
        Date trendInfoStartD=trendInfoStart.getStart();
        long x= Duration.between(toLocalDateTime(trendInfoStartD), toLocalDateTime(trendInfoEndD)).toMinutes()/5;
        double priceY=trendInfoStart.getStartPrice();
        double priceYplus1=trendInfoStart.getEndPrice();
        //double p=trendInfoStart.getStartPrice()+trendInfoStart.getEndPrice()/x+1;
        double p=(priceYplus1-priceY)/(x-1);
        // y=px+b;
        // b=y-px
        double b=priceY-p;
        result.add(new TrendPoint(priceY,trendInfoStart.getStart()));
        result.add(new TrendPoint(priceYplus1,trendInfoEnd.getEnd()));
        LocalDateTime time=toLocalDateTime(trendInfoEnd.getEnd());
        for(long i=x+1;i<300;i++){
            time=time.plusMinutes(5);
            result.add(new TrendPoint((p*i)+b,toDate(time)));
        }

        return new Envolloppe(result,null);


    }
}
