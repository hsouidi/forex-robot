package com.trading.forex.indicators.impl;

import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MInteger;
import com.trading.forex.common.model.Candle;

import java.util.List;

/**
 * Created by hsouidi on 11/01/2017.
 */
public class HighLows {


    public static double[] valuesMin(List<Candle> candles, int periode) {
        Core core = new Core();
        MInteger begin = new MInteger();
        MInteger length = new MInteger();
        double[] out = new double[candles.size()];
        double[] closePrice = candles.stream().mapToDouble(candle -> candle.getLow()).toArray();
        core.min(0, closePrice.length - 1, closePrice, periode, begin, length, out);
        return out;
    }

     public static double[] valuesMax(List<Candle> candles, int periode) {
        Core core = new Core();
        MInteger begin = new MInteger();
        MInteger length = new MInteger();
        double[] out = new double[candles.size()];
        double[] closePrice = candles.stream().mapToDouble(candle -> candle.getHigh()).toArray();
        core.max(0, closePrice.length - 1, closePrice, periode, begin, length, out);
        return out;
    }
}
