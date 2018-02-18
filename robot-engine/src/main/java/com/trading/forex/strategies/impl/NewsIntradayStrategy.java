package com.trading.forex.strategies.impl;

import com.trading.forex.common.model.Candle;
import com.trading.forex.common.model.CandlestickGranularity;
import com.trading.forex.common.model.Symbol;
import com.trading.forex.common.model.Way;
import com.trading.forex.common.utils.CustomList;
import com.trading.forex.connector.service.InstrumentService;
import com.trading.forex.indicators.impl.ADX;
import com.trading.forex.indicators.impl.PivotPoint;
import com.trading.forex.indicators.impl.Stoch;
import com.trading.forex.indicators.impl.ZigZag;
import com.trading.forex.model.PivotPointResult;
import com.trading.forex.model.Trade;
import com.trading.forex.model.Trend;
import com.trading.forex.service.TechnicalAnalysisService;
import com.trading.forex.strategies.Strategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.stream.Collectors;

import static com.trading.forex.common.utils.AlgoUtils.getFromDate;
import static com.trading.forex.common.utils.AlgoUtils.toPip;
import static com.trading.forex.common.utils.AlgoUtils.toQuote;
import static com.trading.forex.indicators.impl.MovingAverage.valueEma;


/**
 * Created by hsouidi on 11/01/2017.
 */
@Slf4j
//@Service
public class NewsIntradayStrategy {

    @Autowired
    private InstrumentService instrumentService;

    public Trade check(Symbol symbol, Date to) {
        final CustomList<Candle> candleM5 = instrumentService.getPricing(CandlestickGranularity.M5, symbol, to, getFromDate(to, CandlestickGranularity.M5))
                .stream().filter(candle -> candle.date().before(to)).collect(Collectors.toCollection(CustomList::new));
        return check(symbol, candleM5, null,null);
    }

    private Trade check(Symbol symbol, CustomList<Candle> candleM5, CustomList<Candle> candleH1,CustomList<Candle> candleD1) {

        final double close = candleM5.getLast().getClose();
        final CustomList<ZigZag.TrendInfo> zigzagM5 = ZigZag.values(candleM5);
        ZigZag.TrendInfo lastZigzagM5= zigzagM5.getLast();
        final double closeNews = candleM5.getLastMinus(10).getClose();
        final double ema26M5 = valueEma(candleM5, 26);



        if (closeNews<close) {
            Trade trade=new Trade(Way.BUY, symbol);
            final double margin = Way.BUY.getValue() * toQuote(trade.getSymbol(), 40D);
            final double stopLoss = close - (margin);
            trade.setStopLoss(stopLoss);
            final double risque = close - stopLoss;
            final double takeProfit = close + risque;
            trade.setTakeProift(takeProfit);
            trade.setRisqueInPip(toPip(symbol, risque));
            trade.setRisque(risque);
            trade.setCurrentTrend(Trend.DOWN);
            trade.setTargetTrend(Trend.UP);
            return trade;

        } else if(closeNews>close) {  // SELL
            Trade trade=new Trade(Way.SELL, symbol);
            final double margin = Way.SELL.getValue() * toQuote(trade.getSymbol(), 40D);
            final double stopLoss = close - (margin);
            trade.setStopLoss(stopLoss);
            final double risque = close - stopLoss;
            final double takeProfit = close + risque;
            trade.setTakeProift(takeProfit);
            trade.setRisqueInPip(toPip(symbol, risque));
            trade.setRisque(risque);
            trade.setCurrentTrend(Trend.UP);
            trade.setTargetTrend(Trend.DOWN);
            return trade;
        }
        return null;
     }

}

