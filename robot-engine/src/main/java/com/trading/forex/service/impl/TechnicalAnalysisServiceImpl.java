package com.trading.forex.service.impl;

import com.trading.forex.common.model.CandlestickGranularity;
import com.trading.forex.common.model.Candle;
import com.trading.forex.common.model.Symbol;
import com.trading.forex.common.utils.CustomList;
import com.trading.forex.connector.service.InstrumentService;
import com.trading.forex.service.TechnicalAnalysisService;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.trading.forex.common.utils.AlgoUtils.getFromDate;

@Service
public class TechnicalAnalysisServiceImpl implements TechnicalAnalysisService {

    private InstrumentService instrumentService;

    @Autowired
    public TechnicalAnalysisServiceImpl(final InstrumentService instrumentService) {

        this.instrumentService = instrumentService;
    }

    @Override
    public Map<Symbol, Map<Symbol, Double>> calculteCorrelation(final List<Symbol> source, final CandlestickGranularity timeframe, int period, final Date to) {

        final Map<Symbol, List<Candle>> prices = source.stream().map(symbol -> normalizeList(instrumentService.getPricing(timeframe, symbol, to, getFromDate(to, timeframe, period * 3)), period)).collect(Collectors.toMap(list -> list.get(0).getSymbol(), Function.identity()));

        final Map<Symbol, Map<Symbol, Double>> result = new HashMap<>();
        for (Symbol symbol : source) {
            final Map<Symbol, Double> subResult = new HashMap<>();
            final double[] symbolPrices = prices.get(symbol).stream().mapToDouble(candle -> candle.getClose().doubleValue()).toArray();
            for (Symbol subSymbol : source) {
                final double[] subSymbolPrices = prices.get(subSymbol).stream().mapToDouble(candle -> candle.getClose().doubleValue()).toArray();
                subResult.put(subSymbol, new PearsonsCorrelation().correlation(symbolPrices, subSymbolPrices));
            }
            result.put(symbol, subResult);
        }
        return result;
    }

    private List<Candle> normalizeList(final List<Candle> candles, int targetSize) {
        final int size = candles.size();
        return candles.subList(size - targetSize, size);

    }
}
