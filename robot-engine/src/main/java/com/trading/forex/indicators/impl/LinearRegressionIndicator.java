package com.trading.forex.indicators.impl;

import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MInteger;
import com.trading.forex.common.model.Candle;

import java.util.List;

import static com.trading.forex.indicators.IndicatorUtils.getValue;

public class LinearRegressionIndicator {

    public static double[] values(List<Candle> candles, int period) {
        Core lib = new Core();
        MInteger begin = new MInteger();
        MInteger length = new MInteger();
        double[] out = new double[candles.size()];

        lib.linearRegIntercept(0, candles.size() - 1
                , candles.stream().mapToDouble(candle -> candle.getClose()).toArray()
                , period, begin, length, out);
        return out;
    }

    public static double[] valuesAngle(List<Candle> candles, int period) {
        Core lib = new Core();
        MInteger begin = new MInteger();
        MInteger length = new MInteger();
        double[] out = new double[candles.size()];

        lib.linearRegAngle(0, candles.size() - 1
                , candles.stream().mapToDouble(candle -> candle.getClose()).toArray()
                , period, begin, length, out);
        return out;
    }

    public static double[] valuesAngle(double[] values, int period) {
        Core lib = new Core();
        MInteger begin = new MInteger();
        MInteger length = new MInteger();
        double[] out = new double[values.length];

        lib.linearRegAngle(0, values.length - 1
                , values
                , period, begin, length, out);
        return out;
    }




    public static Double value(List<Candle> candles, int periode) {
        return getValue(values(candles, periode));
    }
}


