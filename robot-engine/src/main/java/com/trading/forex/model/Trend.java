package com.trading.forex.model;

import com.trading.forex.common.model.Candle;
import com.trading.forex.common.model.Symbol;
import com.trading.forex.common.utils.CustomList;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.function.Function;

import static com.trading.forex.common.utils.AlgoUtils.isCrossPrice;
import static com.trading.forex.common.utils.AlgoUtils.toPip;
import static com.trading.forex.indicators.IndicatorUtils.toList;
import static com.trading.forex.indicators.impl.MovingAverage.valuesEma;


public enum Trend {

    UP, DOWN, RANGE,UNKNOWN;

    public Trend inverse(){
        return this==RANGE||this==UNKNOWN?null:this== UP ? DOWN : UP;
    }

    public static Trend getTrend(CustomList<Candle> candles, Symbol symbol) {

        double threshold = 0.1;
        Candle last = candles.getLast();
        Candle lastMinus3 = candles.getLastMinus(3);
        final CustomList<Double> ema5 = toList(valuesEma(candles, 5));
        final CustomList<Double> ema26 = toList(valuesEma(candles, 26));
        final CustomList<Double> ema100 = toList(valuesEma(candles, 100));
        final CustomList<Double> ema300 = toList(valuesEma(candles, 300));
        final CustomList<Double> ema600 = toList(valuesEma(candles, 600));
        final CustomList<Double> ema1000 = toList(valuesEma(candles, 1000));

        double diffEma5 = toPip(symbol, (ema5.getLast() - ema5.getLastMinus(5)));
        double diffEma26 = toPip(symbol, (ema26.getLast() - ema26.getLastMinus(10)));
        double diffEma100 = toPip(symbol, (ema100.getLast() - ema100.getLastMinus(10)));
        double diffEma300 = toPip(symbol, (ema300.getLast() - ema300.getLastMinus(10)));
        double diffEma600 = toPip(symbol, (ema600.getLast() - ema600.getLastMinus(10)));
        double diffEma1000 = toPip(symbol, (ema1000.getLast() - ema1000.getLastMinus(10)));


        if (isCrossPrice(last, ema26.getLast()) || isCrossPrice(lastMinus3, ema5.getLast())
                || isCrossPrice(last, ema100.getLast()) || isCrossPrice(lastMinus3, ema100.getLast())
                || isCrossPrice(last, ema300.getLast()) || isCrossPrice(lastMinus3, ema300.getLast())
                || isCrossPrice(last, ema600.getLast()) || isCrossPrice(lastMinus3, ema600.getLast())
                || isCrossPrice(last, ema1000.getLast()) || isCrossPrice(lastMinus3, ema1000.getLast())

                ) {
            return Trend.RANGE;
        }
        if (diffEma5 > threshold && diffEma26 > threshold && diffEma100 > threshold && diffEma300 > threshold
                && diffEma600 > threshold && diffEma1000 > threshold
                ) {
            return Trend.UP;

        } else if (diffEma5 < -threshold && diffEma26 < -threshold && diffEma100 < -threshold && diffEma300 < -threshold
                && diffEma600 < -threshold && diffEma1000 < -threshold
                ) {
            return Trend.DOWN;
        }
        return Trend.RANGE;
    }


    private static Trend calculGlobalTrend(CustomList<TrendInfo> trendInfos) {

        double trendBullishNb = 0;
        double trendBurrishNb = 0;
        final double threshold=55D;
        int all = 0;
        for (TrendInfo trendInfo : trendInfos) {
            all += trendInfo.weight;
            if (trendInfo.getTrend() == UP) {
                trendBullishNb += trendInfo.weight;
            } else if (trendInfo.getTrend() == DOWN) {
                trendBurrishNb += trendInfo.weight;
            }
        }
        double trendBullishInPerc=(trendBullishNb/all)*100;
        double trendBurrishNbInPerc=(trendBurrishNb/all)*100;

        return trendBullishInPerc>threshold?Trend.UP :trendBurrishNbInPerc>threshold?Trend.UP : Trend.RANGE;

    }

    private static CustomList<TrendInfo> getTrendInfos(List<Integer> elements, List<Candle> candles, Function<Candle, Double> candlePriceFunc) {
        int up = 0;
        int down = 0;
        final CustomList<TrendInfo> trendInfos = new CustomList<>();

        for (int index = 1; index < elements.size(); index++) {
            Double candle = candlePriceFunc.apply(candles.get(index));
            Double candleMinus1 = candlePriceFunc.apply(candles.get(index - 1));
            if (candle.compareTo(candleMinus1) > 0) {
                if (down > 0) {
                    trendInfos.add(new TrendInfo(Trend.UP, down));
                    down = 0;
                }
                up++;

            } else if (candle.compareTo(candleMinus1) < 0) {
                if (up > 0) {
                    trendInfos.add(new TrendInfo(Trend.DOWN, up));
                    up = 0;
                }
                down++;
            }
        }
        return trendInfos;
    }

    @Data
    @AllArgsConstructor
    public static class TrendInfo {
        private Trend trend;
        private Integer weight;
    }


}
