package com.trading.forex.economic.service;

import com.trading.forex.common.model.Duration;
import com.trading.forex.common.model.Symbol;
import com.trading.forex.common.utils.CustomList;
import com.trading.forex.economic.entity.EconomicCalendarEntity;
import com.trading.forex.economic.entity.ForexSignal;
import com.trading.forex.common.model.Importance;
import com.trading.forex.economic.model.ZuluPosition;
import com.trading.forex.economic.service.impl.IndicatorServiceImpl;
import com.trading.forex.model.InvestingDataGroup;
import com.trading.forex.model.InvestingTechIndicator;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * Created by hsouidi on 04/19/2017.
 */
public interface IndicatorService {


    CustomList<EconomicCalendarEntity> getEconomicCalendarData(Importance importanceFilter, final String weekBeginDate);

    InvestingDataGroup expertDecision(Symbol symbol);

    Map<Symbol, InvestingTechIndicator> expertDecision(Duration duration);

    InvestingTechIndicator expertDecision(Symbol symbol, Duration duration);

    List<ForexSignal> getForexSignal();

    Flux<IndicatorServiceImpl.StockTransaction>  test();

    List<ZuluPosition> getZuluPosition(String traderId);
}
