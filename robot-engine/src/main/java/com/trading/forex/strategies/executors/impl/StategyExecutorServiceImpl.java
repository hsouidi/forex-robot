package com.trading.forex.strategies.executors.impl;

import com.trading.forex.client.EconomicNewsClient;
import com.trading.forex.common.model.*;
import com.trading.forex.common.model.Currency;
import com.trading.forex.connector.service.PositionService;
import com.trading.forex.model.Trade;
import com.trading.forex.service.MessageSenderService;
import com.trading.forex.service.TechnicalAnalysisService;
import com.trading.forex.strategies.Strategy;
import com.trading.forex.strategies.executors.StategyExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.trading.forex.common.utils.AlgoUtils.*;

@Slf4j
@Service
public class StategyExecutorServiceImpl implements StategyExecutorService {

    @Autowired
    private Strategy intradayStrategy;

    @Value("${order.booking.tempo.queue}")
    private String orderBookingTempoQueue;

    @Autowired
    private MessageSenderService messageSenderService;

    @Autowired
    private TechnicalAnalysisService technicalAnalysisService;

    @Autowired
    private Map<Symbol, Trade> sessionProposals;

    @Autowired
    private Map<Symbol, Map<Symbol, Double>> correlations;

    @Autowired
    private PositionService positionService;

    @Autowired
    private EconomicNewsClient economicNewsClient;

    @Override
    public void process(LocalDateTime currentTime) {

        //final Map<Currency, List<EconomicCalendarData>> economicData = economicNewsClient.economicCalendar(DateTimeFormatter.ofPattern("yyyyMMdd").format(currentTime), Importance.HIGH).getEconomicCalendars().stream().collect(Collectors.groupingBy(EconomicCalendarData::getCurrency));
        final Map<Symbol, Trade> tradesBySymbol = new CopyOnWriteArrayList<>(Symbol.getActivatedSymbol()).stream()
/*                .filter(symbol -> {
                    boolean isAfterNews = economicData.entrySet().stream()
                            .filter(e -> symbol.name().contains(e.getKey().name()))
                            .flatMap(e -> e.getValue().stream())
                            .allMatch(e -> convertToLocalDateTime(e.getEventDate()).plusMinutes(15L).isBefore(currentTime));
                    return isAfterNews && symbol.isActivated() && !sessionProposals.containsKey(symbol);
                })*/
                .map(symbol -> intradayStrategy.check(symbol, Date.from(currentTime.atZone(ZoneId.systemDefault()).toInstant())))
                .filter(result -> result != null)
                .collect(Collectors.toMap(Trade::getSymbol, Function.identity()));


        // Filter correlation
        final Set<Symbol> tobeDeleted = new HashSet<>();
        /* for (Map.Entry<Symbol, Trade> tradeEntry : tradesBySymbol.entrySet()) {

            final Symbol symbol = tradeEntry.getKey();
            final Way way = tradeEntry.getValue().getWay();
           final Map<Symbol, Double> symbolCorrelationMap = correlations.get(tradeEntry.getKey());
            for (Map.Entry<Symbol, Trade> sub : tradesBySymbol.entrySet()) {
                final Double correlationValue = symbolCorrelationMap.get(sub.getKey());
                if (sub.getKey() == tradeEntry.getKey() || null == correlationValue) {
                    continue;
                }
                final Way waysub = sub.getValue().getWay();
                if ((waysub == way && correlationValue > 0 || waysub != way && correlationValue < 0) && Symbol.majorsLiquidSymbol.contains(symbol)) {
                    tobeDeleted.add(sub.getKey());
                } else if (waysub == way && correlationValue < 0 || waysub != way && correlationValue > 0) {
                    tobeDeleted.add(symbol);
                    tobeDeleted.add(sub.getKey());
                }

            }
        }
        tradesBySymbol.entrySet().removeIf(e -> tobeDeleted.contains(e.getValue().getSymbol()));
        final Set<Symbol> tobeDeletedProp = new HashSet<>();
        final Set<Symbol> tobeDeletedTradeSymbol = new HashSet<>();
        for (Map.Entry<Symbol, Trade> tradeEntry : tradesBySymbol.entrySet()) {
            final Map<Symbol, Double> symbolCorrelationMap = correlations.get(tradeEntry.getKey());
            final Symbol symbol = tradeEntry.getKey();
            final Way way = tradeEntry.getValue().getWay();
            for (Map.Entry<Symbol, Trade> posSymbol : sessionProposals.entrySet()) {
                final Way posWay = posSymbol.getValue().getWay();
                if (symbolCorrelationMap.containsKey(posSymbol.getKey())
                        && ((symbolCorrelationMap.get(posSymbol.getKey()) < 0 && posWay == way) || (symbolCorrelationMap.get(posSymbol.getKey()) > 0 && posWay != way))) {
                    tobeDeletedTradeSymbol.add(symbol);
                    tobeDeletedProp.add(posSymbol.getKey());
                }
            }
        }
        tradesBySymbol.entrySet().removeIf(e -> tobeDeletedTradeSymbol.contains(e.getValue().getSymbol()));
        sessionProposals.entrySet().removeIf(e -> tobeDeletedProp.contains(e.getKey()));
*/
        final Collection<Trade> trades = tradesBySymbol.values();
        log.info("Analysis  Done {}", trades);
        for (Trade trade : trades) {
            if(sessionProposals.containsKey(trade.getSymbol())){
                log.info("same symbol Trade found in sessionProposals  {}", sessionProposals);
                if(sessionProposals.get(trade.getSymbol()).getWay()!=trade.getWay()){
                    log.info("same symbol Trade found in sessionProposals  {}  but with another way", sessionProposals);
                    sessionProposals.remove(trade.getSymbol());
                 }else{
                    log.info("same symbol Trade found in sessionProposals  {}  with sthe ame way", sessionProposals);
                    continue;
                 }
            }
            sessionProposals.putIfAbsent(trade.getSymbol(), trade);
            log.info("sendDelayedMessage to booking queue {}",orderBookingTempoQueue);
            messageSenderService.sendDelayedMessage(orderBookingTempoQueue, trade, getDelay(truncate(LocalDateTime.now(),intradayStrategy.getAnalysTimeFrame().getNumberMinutes()).plusMinutes(intradayStrategy.getAnalysTimeFrame().getNumberMinutes())));
        }
    }
}
