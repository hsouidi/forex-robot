package com.trading.forex.strategies.executors.impl;

import com.trading.forex.client.EconomicNewsClient;
import com.trading.forex.common.model.Currency;
import com.trading.forex.common.model.*;
import com.trading.forex.connector.service.PositionService;
import com.trading.forex.model.Trade;
import com.trading.forex.service.MessageSenderService;
import com.trading.forex.service.TechnicalAnalysisService;
import com.trading.forex.strategies.executors.StategyExecutorService;
import com.trading.forex.strategies.impl.NewsIntradayStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.trading.forex.common.utils.AlgoUtils.convertToLocalDateTime;
import static com.trading.forex.common.utils.AlgoUtils.getDelay;

@Slf4j
//@Service
public class NewsStategyExecutorServiceImpl implements StategyExecutorService {

    @Value("${order.booking.tempo.queue}")
    private String orderBookingTempoQueue;

    @Autowired
    private MessageSenderService messageSenderService;

    @Autowired
    private TechnicalAnalysisService technicalAnalysisService;

    @Autowired
    private Map<Symbol, Trade> sessionProposals;

    @Autowired
    private PositionService positionService;

    @Autowired
    private EconomicNewsClient economicNewsClient;

    @Autowired
    private NewsIntradayStrategy newsIntradayStrategy;

    @Override
    public void process(LocalDateTime currentTime) {

        final Map<Currency, List<EconomicCalendarData>> economicData = economicNewsClient.economicCalendar(DateTimeFormatter.ofPattern("yyyyMMdd").format(currentTime), Importance.HIGH).getEconomicCalendars().stream().collect(Collectors.groupingBy(EconomicCalendarData::getCurrency));
        final Map<Symbol, Trade> tradesBySymbol = economicData.entrySet()
                .stream()
                .filter(e -> e.getValue().stream().filter(m -> convertToLocalDateTime(m.getEventDate()).equals(currentTime)).count() > 0)
                .flatMap(e -> Symbol.fromCurrency(e.getKey()).stream())
                .map(symbol -> newsIntradayStrategy.check(symbol, Date.from(currentTime.atZone(ZoneId.systemDefault()).toInstant())))
                .filter(result -> result != null)
                .distinct()
                .collect(Collectors.toMap(Trade::getSymbol, Function.identity()));

        final Collection<Trade> trades = tradesBySymbol.values();
        log.info("Analysis  Done {}", trades);
        for (Trade trade : trades) {
            sessionProposals.putIfAbsent(trade.getSymbol(), trade);
            messageSenderService.sendDelayedMessage(orderBookingTempoQueue, trade, getDelay(5));
        }
    }
}
