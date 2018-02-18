package com.trading.forex.strategies;

import com.trading.forex.common.model.Candle;
import com.trading.forex.common.model.CandlestickGranularity;
import com.trading.forex.common.model.Symbol;
import com.trading.forex.common.utils.CustomList;
import com.trading.forex.model.Trade;

import java.util.Date;

public interface Strategy {


    Trade check(Symbol symbol, Date to);

    boolean checkBooking(Trade trade, CustomList<Candle> candles);

    boolean checkStatus(Trade trade, CustomList<Candle> candles);

    CandlestickGranularity getAnalysTimeFrame();

    CandlestickGranularity getCheckStatusTimeFrame();

    default boolean stopAtEndOfDate(){
        return true;
    }



}
