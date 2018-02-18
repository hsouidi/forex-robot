package com.trading.forex.strategies.impl;

import com.trading.forex.common.model.Candle;
import com.trading.forex.common.model.CandlestickGranularity;
import com.trading.forex.common.model.Symbol;
import com.trading.forex.common.model.Way;
import com.trading.forex.common.utils.AlgoUtils;
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
import static com.trading.forex.indicators.IndicatorUtils.getPriceLevels;
import static com.trading.forex.indicators.impl.MovingAverage.getCrossList;
import static com.trading.forex.indicators.impl.MovingAverage.valueEma;


/**
 * Created by hsouidi on 11/01/2017.
 */
@Slf4j
//@Service
public class MovingAverageCrossStrategyInLive1504 implements Strategy {

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

        AlgoUtils.check(candleM5.getLast(),to,"MovingAverageCrossStrategyInLive",5);
        AlgoUtils.check(candleH1.getLast(),to,"MovingAverageCrossStrategyInLive",60);
        AlgoUtils.check(candleM15.getLast(),to,"MovingAverageCrossStrategyInLive",15);


        return check(symbol, candleM5, candleH1, candleD1, candleM15);
    }

    private Trade check(Symbol symbol, CustomList<Candle> candleM5, CustomList<Candle> candleH1, CustomList<Candle> candleD1, CustomList<Candle> candleM15) {

        final double close = candleM5.getLast().getClose();
        ADXCustom.ADXResultValue adx=ADX.value(candleM5,14);
        log.info("Last Candle {}",candleM5.getLast());
        log.info("ADX = {}",adx);
        if(adx.getAdx()<25
                //||Stoch.divergence(candleM5,11,5,3)
        ){
            return null;
        }

        final Stoch.StochResultValue stock=Stoch.valueCustom(candleM5,11,5,3);

        log.info("Stoch = {}",stock);

        final CustomList<Integer> crossM5Buy=getCrossList(candleM5,20,50,Way.BUY,symbol);
        final CustomList<Integer> crossM5Sell=getCrossList(candleM5,20,50,Way.SELL,symbol);

        CustomList<ZigZag.TrendInfo> trendInfosD1 = ZigZag.values(candleD1);
        CustomList<ZigZag.TrendInfo> trendInfosH1 = ZigZag.values(candleH1);
        CustomList<ZigZag.TrendInfo> trendInfosM5 = ZigZag.values(candleM5);

        double rsiM5 = RSI.value(candleM5, 21);
        log.info("RSI {}",rsiM5);
        // Calcul EMA
        final double ema100H1 = valueEma(candleH1, 100);
        //final double ema26M5 = valueEma(candleM5, 100);
        final double ema100M15 = valueEma(candleM15, 100);
        final boolean isEmaSell = close < ema100H1 && close < ema100M15;
        final boolean isEmaBuy = close > ema100H1 && close > ema100M15;
        //PivotPointResult pivotPoint = PivotPoint.calcul(candleD1.getLastMinus(1));

        //List<PriceLevel> pricesLEvels = getPriceLevels(trendInfosD1, trendInfosH1, trendInfosM5);
        if (stock.getOutSlowD()<80&&stock.getOutSlowD()>20&&isEmaBuy && crossM5Buy.getLast()<crossM5Sell.getLast() ) {
            //final Optional<PriceLevel> stopLoss = pricesLEvels.stream().filter(c -> close - c.getPrice() < 0).findFirst();
            return new Trade(Way.BUY, symbol);

        } else if (stock.getOutSlowD()>30&&stock.getOutSlowD()<80&&isEmaSell && crossM5Buy.getLast()>crossM5Sell.getLast()) {  // SELL

            //final Optional<PriceLevel> stopLoss = pricesLEvels.stream().filter(c -> c.getPrice() - close > 0).findFirst();
            return new Trade(Way.SELL, symbol);
        }
        return null;
    }

    @Override
    public boolean checkBooking(Trade trade,CustomList<Candle> candles) {
        final Symbol symbol = trade.getSymbol();
        final Way way = trade.getWay();
        final double currentPrice = trade.getLastPrice();
        final CustomList<ZigZag.TrendInfo> zigzagM5 = ZigZag.values(candles);
        final ZigZag.TrendInfo trendInfo = zigzagM5.getLast();
        if (toPip(symbol, way.getValue() * (currentPrice - trendInfo.getEndPrice())) > -10D && (way == Way.BUY && trendInfo.getTrend() == Trend.UP) || (way == Way.SELL && trendInfo.getTrend() == Trend.DOWN)) {
            final double risque = 10D;
            trade.setTakeProift((currentPrice+way.getValue()*toQuote(symbol,15)));
            trade.setStopLoss((currentPrice-way.getValue()*toQuote(symbol,10)));
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