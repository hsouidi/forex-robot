package com.trading.forex.strategies.impl;

import com.trading.forex.common.model.Candle;
import com.trading.forex.common.model.CandlestickGranularity;
import com.trading.forex.common.model.Symbol;
import com.trading.forex.common.model.Way;
import com.trading.forex.common.utils.CustomList;
import com.trading.forex.connector.service.InstrumentService;
import com.trading.forex.indicators.IndicatorUtils;
import com.trading.forex.indicators.impl.*;
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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.trading.forex.common.utils.AlgoUtils.*;
import static com.trading.forex.common.utils.AlgoUtils.toPip;
import static com.trading.forex.indicators.IndicatorUtils.getPriceLevels;
import static com.trading.forex.indicators.impl.MovingAverage.*;


/**
 * Created by hsouidi on 11/01/2017.
 */
@Slf4j
//@Service
public class BrahimChauvinStrategy implements Strategy {

    @Autowired
    private InstrumentService instrumentService;

    @Autowired
    private TechnicalAnalysisService technicalAnalysisService;

    @Value("${trade.default.margin.inPips}")
    private int defaultMargin;

    @Override
    public Trade check(Symbol symbol, Date to) {
        final CustomList<Candle> candleM5 = instrumentService.getPricing(CandlestickGranularity.M5, symbol, to, getFromDate(to, CandlestickGranularity.M5))
                .stream().filter(candle -> candle.date().before(to)).collect(Collectors.toCollection(CustomList::new));

        final CustomList<Candle> candleH1 = instrumentService.getPricing(CandlestickGranularity.H1, symbol, to, getFromDate(to, CandlestickGranularity.H1))

                .stream().filter(candle -> candle.date().before(to)).collect(Collectors.toCollection(CustomList::new));

        final CustomList<Candle> candleM15 = instrumentService.getPricing(CandlestickGranularity.M15, symbol, to, getFromDate(to, CandlestickGranularity.M15))

                .stream().filter(candle -> candle.date().before(to)).collect(Collectors.toCollection(CustomList::new));


        final CustomList<Candle> candleD1 = instrumentService.getPricing(CandlestickGranularity.D, symbol, to, getFromDate(to, CandlestickGranularity.D))
                .stream().filter(candle -> candle.date().before(to)).collect(Collectors.toCollection(CustomList::new));

        return check(symbol, candleM5, candleH1, candleD1, candleM15);
    }

    private Trade check(Symbol symbol, CustomList<Candle> candleM5, CustomList<Candle> candleH1, CustomList<Candle> candleD1, CustomList<Candle> candleM15) {

        final double close = candleM5.getLast().getClose();
        ADXCustom.ADXResultValue adx = ADX.value(candleM5, 14);
        if (adx.getAdx() < 15) {
            return null;
        }
        CustomList<ZigZag.TrendInfo> trendInfosD1 = ZigZag.values(candleD1);
        CustomList<ZigZag.TrendInfo> trendInfosH1 = ZigZag.values(candleH1);
        CustomList<ZigZag.TrendInfo> trendInfosM5 = ZigZag.values(candleM5);

        Stoch.StochRSI stockM5 = Stoch.valuesRSI(candleM5, 11, 5, 3);
        CustomList<Double> stockM5Values = IndicatorUtils.toList(stockM5.getOutFastD());
        // Calcul EMA
        final double ema26H1 = valueEma(candleH1, 100);
        final double ema26M5 = valueEma(candleM5, 100);
        final double ema26M15 = valueEma(candleM15, 100);
        final boolean isEmaSell = close < ema26H1 && close < ema26M15;
        final boolean isEmaBuy = close > ema26H1 && close > ema26M15;
        PivotPointResult pivotPoint = PivotPoint.calcul(candleD1.getLastMinus(1));

        List<PriceLevel> pricesLEvels = getPriceLevels(trendInfosD1, trendInfosH1, trendInfosM5);
        if (isEmaBuy && close > pivotPoint.getPivot() && stockM5Values.getLast() < 30 && stockM5Values.getLastMinus(1) > 30) {
            final Optional<PriceLevel> stopLoss = pricesLEvels.stream().filter(c -> close - c.getPrice() < 0).findFirst();
            final double margeEma = toQuote(symbol, ema26M5 - close);
            return new Trade(Way.BUY, symbol, close - toQuote(symbol, 10));

        } else if (isEmaSell && close < pivotPoint.getPivot() && stockM5Values.getLast() > 80 && stockM5Values.getLastMinus(1) < 80) {  // SELL

            final Optional<PriceLevel> stopLoss = pricesLEvels.stream().filter(c -> c.getPrice() - close > 0).findFirst();
            final double margeEma = toQuote(symbol, ema26M5 - close);
            return new Trade(Way.SELL, symbol, close + toQuote(symbol, 10));
        }
        return null;
    }

    @Override
    public boolean checkBooking(Trade trade, CustomList<Candle> candles) {
        final Symbol symbol = trade.getSymbol();
        final Way way = trade.getWay();
        final double currentPrice = trade.getLastPrice();
        final CustomList<ZigZag.TrendInfo> zigzagM5 = ZigZag.values(candles);
        final ZigZag.TrendInfo trendInfo = zigzagM5.getLast();
        if (toPip(symbol, way.getValue() * (currentPrice - trendInfo.getEndPrice())) > -10D && (way == Way.BUY && trendInfo.getTrend() == Trend.UP) || (way == Way.SELL && trendInfo.getTrend() == Trend.DOWN)) {
            // else if (trendInfo.getWeight()>4&&(way == Way.BUY && trendInfo.getTrend() == Trend.UP) || (way == Way.SELL && trendInfo.getTrend() == Trend.DOWN)) {
            final double margin = way.getValue() * toQuote(trade.getSymbol(), defaultMargin);
            //final double stopLoss = currentPrice - margin;
            //trade.setStopLoss(stopLoss);
            //double atr= ATR.value(candles,14);

            final double risque = way.getValue() * toPip(symbol, currentPrice - trendInfo.getStartPrice());
            //final double risque =10D;
            //final double takeProfit = currentPrice + (risque*1.5);
            trade.setTakeProift((currentPrice + way.getValue() * toQuote(symbol, risque * 1.5D)));
            trade.setStopLoss((currentPrice - way.getValue() * toQuote(symbol, risque)));
            // trade.setTakeProift(barrier.getResistance());
            trade.setRisqueInPip(toPip(symbol, risque));
            trade.setRisque(risque);
            return true;

        }
        return false;
    }

    @Override
    public boolean checkStatus(Trade trade, CustomList<Candle> candles) {
        int wayFactor = trade.getWay().getValue();
        final double closePrice = candles.getLast().getClose();
        final Symbol symbol = trade.getSymbol();
        if (wayFactor * (closePrice - trade.getResistance()) > 0) {
            final Double risque = trade.getRisque();
            trade.setSupport(trade.getResistance() - (wayFactor * toQuote(symbol, 2D)));
            trade.setResistance(trade.getResistance() + (risque / 2));
            log.info("symbol {} upgrade resistance   for {}", symbol, trade);
            trade.setUpgrated(true);
            return false;
        }
        return false;
    }

    @Override
    public CandlestickGranularity getAnalysTimeFrame() {
        return CandlestickGranularity.M5;
    }

    @Override
    public CandlestickGranularity getCheckStatusTimeFrame() {
        return CandlestickGranularity.M1;
    }


}

