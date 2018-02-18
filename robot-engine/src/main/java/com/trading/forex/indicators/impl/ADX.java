package com.trading.forex.indicators.impl;

import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MInteger;
import com.trading.forex.common.model.Candle;
import com.trading.forex.indicators.IndicatorUtils;

import java.util.List;

/**
 * Created by hsouidi on 10/24/2017.
 */
public class ADX extends IndicatorUtils {


/*    public static double[] values(List<Candle> candles, int periode) {
        Core core = new Core();
        MInteger begin = new MInteger();
        MInteger length = new MInteger();
        double[] out = new double[candles.size()];
        double[] closePrice = candles.stream().mapToDouble(candle -> candle.getClose()).toArray();
        //core.adxLookback(periode);
        core.adx(0, closePrice.length - 1, candles.stream().mapToDouble(candle -> candle.getHigh()).toArray(),
                candles.stream().mapToDouble(candle -> candle.getLow()).toArray(),
                closePrice, periode, begin, length, out);
        return out;
    }*/

    public static ADXCustom.ADXResult values(List<Candle> candles, int periode) {
        ADXCustom adxCustom=new ADXCustom();
        return adxCustom.OnCalculate(candles.stream().mapToDouble(candle -> candle.getHigh()).toArray(),
                candles.stream().mapToDouble(candle -> candle.getLow()).toArray(),
                candles.stream().mapToDouble(candle -> candle.getClose()).toArray(),periode);
    }

    public static ADXCustom.ADXResultValue value(List<Candle> candles, int periode) {
        final ADXCustom.ADXResult adxResult=values(candles, periode);
        final int index=getValueIndex(adxResult.getAdx());
        return new ADXCustom.ADXResultValue(adxResult.getAdx()[index],adxResult.getPdi()[index],adxResult.getNdi()[index]);
    }
}
