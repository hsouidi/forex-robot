package com.trading.forex.configuration;

import com.trading.forex.common.model.CandlestickGranularity;
import com.trading.forex.common.model.Symbol;
import com.trading.forex.model.Trade;
import com.trading.forex.service.TechnicalAnalysisService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.trading.forex.common.utils.AlgoUtils.getStartOfDay;

@Configuration
public class SessionConfig {


    @Bean
    public Date sessionDate() {
        return new Date();
    }

    @Bean
    public Map<Symbol, Trade> sessionProposals() {
        return new ConcurrentHashMap<>();
    }


    @Bean
    public Map<Symbol, Map<Symbol, Double>> correlations(TechnicalAnalysisService technicalAnalysisService) {

        return correlations(technicalAnalysisService, new Date());
    }

    public static Map<Symbol, Map<Symbol, Double>> correlations(TechnicalAnalysisService technicalAnalysisService, final Date date) {
        final Map<Symbol, Map<Symbol, Double>> result = technicalAnalysisService.calculteCorrelation(Symbol.getActivatedSymbol(), CandlestickGranularity.D, 10, getStartOfDay(date));
        for (Map.Entry<Symbol, Map<Symbol, Double>> entry : result.entrySet()) {
            entry.getValue().entrySet().removeIf(e -> Math.abs(e.getValue()) < 0.7 || e.getKey().equals(entry.getKey()));
        }
        return result;
    }
}
