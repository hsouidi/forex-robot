package com.trading.forex.strategies.impl;

import com.trading.forex.common.model.Candle;
import com.trading.forex.common.model.CandlestickGranularity;
import com.trading.forex.common.model.Symbol;
import com.trading.forex.common.model.Way;
import com.trading.forex.common.utils.AlgoUtils;
import com.trading.forex.common.utils.CustomList;
import com.trading.forex.connector.service.InstrumentService;
import com.trading.forex.indicators.impl.*;
import com.trading.forex.model.Trade;
import com.trading.forex.service.TechnicalAnalysisService;
import com.trading.forex.strategies.Strategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;
import java.util.stream.Collectors;

import static com.trading.forex.common.utils.AlgoUtils.*;
import static com.trading.forex.indicators.IndicatorUtils.toList;
import static com.trading.forex.indicators.impl.MovingAverage.getCrossList;
import static com.trading.forex.indicators.impl.MovingAverage.valueEma;


/**
 * Created by hsouidi on 11/01/2017.
 */
@Slf4j
//@Service
public class MovingAverageCrossStrategyInTest06 implements Strategy {

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

        AlgoUtils.check(candleM5.getLast(), to, "MovingAverageCrossStrategyInLive  M5", 5);
        AlgoUtils.check(candleH1.getLast(), to, "MovingAverageCrossStrategyInLive H1", 60);
        AlgoUtils.check(candleM15.getLast(), to, "MovingAverageCrossStrategyInLive M15", 15);


        return check(symbol, candleM5, candleH1, candleD1, candleM15);
    }


    private Trade check(Symbol symbol, CustomList<Candle> candleM5, CustomList<Candle> candleH1, CustomList<Candle> candleD1, CustomList<Candle> candleM15) {

        final double close = candleM5.getLast().getClose();
        ADXCustom.ADXResultValue adx = ADX.value(candleM5, 14);
        double atr = ATR.value(candleM5, 14);
        log.info("Last Candle {}", candleM5.getLast());
        log.info("ADX = {}", adx);
        log.info("ATR = {}", atr);

        CustomList<HeikenAshi.HeikenAshiBar> heikenAshiBarsM5 = HeikenAshi.OnCalculate(candleM5);

        final Stoch.StochResultValue stock = Stoch.valueCustom(candleM5, 11, 5, 3);

        log.info("Stoch = {}", stock);

        final CustomList<Integer> crossM5Buy = getCrossList(candleM5, 20, 50, Way.BUY, symbol);
        final CustomList<Integer> crossM5Sell = getCrossList(candleM5, 20, 50, Way.SELL, symbol);

        CustomList<ZigZag.TrendInfo> trendInfosM5 = ZigZag.values(candleM5);

        ZigZag.TrendInfo trendInfoEnd = trendInfosM5.getLastMinus(1);
        ZigZag.TrendInfo trendInfoStart = trendInfosM5.getLastMinus(2);

        Date trendInfoEndD = trendInfoEnd.getEnd();
        Date trendInfoStartD = trendInfoStart.getStart();
        long x = Duration.between(AlgoUtils.toLocalDateTime(trendInfoStartD), AlgoUtils.toLocalDateTime(trendInfoEndD)).toMinutes() / 5;
        double rsiM5 = RSI.value(candleM5, 21);
        log.info("RSI {}", rsiM5);
        // candle close
        final double h1Close = candleH1.getLast().getClose();
        final double M15Close = candleM15.getLast().getClose();
        // Calcul EMA
        final double ema100H1 = valueEma(candleH1, 100);
        final double ema100M15 = valueEma(candleM15, 100);

        final boolean isEmaSell = h1Close < ema100H1 && toPip(symbol, ema100H1 - close) > 10D && close < ema100M15 && M15Close < ema100M15;
        final boolean isEmaBuy = h1Close > ema100H1 && toPip(symbol, close - ema100H1) > 10D && close > ema100M15 && M15Close > ema100M15;
        if (
                heikenAshiBarsM5.getLast().getBody() < 0 &&
                        isEmaBuy
                        && crossM5Buy.getLast() < crossM5Sell.getLast()
        ) {
            return new Trade(Way.BUY, symbol);

        } else if (
                heikenAshiBarsM5.getLast().getBody() > 0 &&
                        isEmaSell
                        && crossM5Buy.getLast() > crossM5Sell.getLast()
        ) {  // SELL

            return new Trade(Way.SELL, symbol);
        }
        return null;
    }


    @Override
    public boolean checkBooking(Trade trade, CustomList<Candle> candles) {
        final Symbol symbol = trade.getSymbol();
        final Way way = trade.getWay();
        final double currentPrice = trade.getLastPrice();
        CustomList<HeikenAshi.HeikenAshiBar> heikenAshiBarsM5 = HeikenAshi.OnCalculate(candles);
        HeikenAshi.HeikenAshiBar last = heikenAshiBarsM5.getLast();
        final double ema20M5 = valueEma(candles, 20);
        Ichimoku ichimoku=new Ichimoku(candles);
        final double pos= ichimoku.positionWithCloud(candles.getLast());
        if ((way.getValue() * candles.getLast().body() > 2D)
                && (way.getValue()*pos>0)
                && ((way == Way.BUY && last.getBody() > 0) || (way == Way.SELL && last.getBody() < 0))) {
            final double risque = 10D;
            trade.setTakeProift(currentPrice + (way.getValue() * toQuote(symbol, risque * 1.5)));
            trade.setStopLoss(currentPrice - (way.getValue() * toQuote(symbol, risque)));
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
        if (wayFactor * (closePrice - trade.getSupport()) < 0) {
            Date to=candles.getLast().date();
            final CustomList<Candle> candleM5 = instrumentService.getPricing(CandlestickGranularity.M5, symbol, to, getFromDate(to, CandlestickGranularity.M5))
                    .stream().collect(Collectors.toCollection(CustomList::new));
            CustomList<HeikenAshi.HeikenAshiBar> heikenAshiBarsM5 = HeikenAshi.OnCalculate(candleM5);
            if(heikenAshiBarsM5.subList(heikenAshiBarsM5.size()-4,heikenAshiBarsM5.size()-1).stream().anyMatch(c->wayFactor*c.getBody()>0)){
                return false;
            }
            log.info("support reached ==> close position for {}", trade.toString());
            if (!trade.isUpgrated() && trade.getStopLoss() != null) {
                trade.setLastPrice(trade.getStopLoss());
            }
            return true;
        } else if (wayFactor * (closePrice - trade.getResistance()) > 0) {
            final Double risque = trade.getRisque();
            trade.setSupport(trade.getResistance() - (wayFactor * toQuote(symbol, 2D)));
            trade.setResistance(trade.getResistance() + ((wayFactor * risque) / 2));
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
        return CandlestickGranularity.M5;
    }


}