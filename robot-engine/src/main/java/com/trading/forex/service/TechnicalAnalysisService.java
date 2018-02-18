package com.trading.forex.service;

import com.trading.forex.common.model.CandlestickGranularity;
import com.trading.forex.common.model.Symbol;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface TechnicalAnalysisService {

    Map<Symbol, Map<Symbol, Double>> calculteCorrelation(List<Symbol> source, CandlestickGranularity timeframe, int period, Date to);

}
