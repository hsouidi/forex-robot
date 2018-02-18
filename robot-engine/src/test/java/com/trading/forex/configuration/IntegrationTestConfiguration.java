package com.trading.forex.configuration;


import com.trading.forex.InstrumentServiceDBImpl;
import com.trading.forex.RobotAppBackTest;
import com.trading.forex.RobotAppBackTest.Result;
import com.trading.forex.client.*;
import com.trading.forex.common.model.Symbol;
import com.trading.forex.connector.model.OrderCreateResponse;
import com.trading.forex.connector.model.Position;
import com.trading.forex.connector.service.InstrumentService;
import com.trading.forex.connector.service.OrderService;
import com.trading.forex.connector.service.PositionService;
import com.trading.forex.listener.OrderBookingHandler;
import com.trading.forex.listener.OrderStatusHandler;
import com.trading.forex.listener.impl.OrderBookingHandlerImpl;
import com.trading.forex.listener.impl.OrderStatusHandlerImpl;
import com.trading.forex.model.Trade;
import com.trading.forex.oanda.service.OandaInstrumentServiceImpl;
import com.trading.forex.repository.CandleHistoryRepository;
import com.trading.forex.repository.ESRepository;
import com.trading.forex.service.MessageSenderService;
import com.trading.forex.service.RobotConfigurationService;
import com.trading.forex.service.TechnicalAnalysisService;
import com.trading.forex.service.TradeHistoryService;
import com.trading.forex.service.impl.TechnicalAnalysisServiceImpl;
import com.trading.forex.strategies.Strategy;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@Configuration
public class IntegrationTestConfiguration {

    @Bean
    public InstrumentService instrumentService(PolygonMarketDataClientProxy polygonMarketDataClient, MarketDataClientProxy marketDataClient, CandleHistoryRepository candleHistoryRepository, OandaInstrumentServiceImpl oandaInstrumentService) {
        try {
            return new InstrumentServiceDBImpl(polygonMarketDataClient, marketDataClient, candleHistoryRepository, oandaInstrumentService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    public TechnicalAnalysisService technicalAnalysisService(InstrumentService instrumentService) {
        return new TechnicalAnalysisServiceImpl(instrumentService);
    }

    @Bean
    public PolygonMarketDataClientProxy polygonMarketDataClientProxy(PolygonMarketDataClient polygonMarketDataClient) {
        return new PolygonMarketDataClientProxyImpl(polygonMarketDataClient);
    }

    @Bean
    public MarketDataClientProxy marketDataClientProxy(MarketDataClient marketDataClient) {
        return new MarketDataClientProxyimpl(marketDataClient);
    }

    @Bean
    public MessageSenderService messageSenderService(final Map<Symbol, Trade> openedTrades) {
        final MessageSenderService messageSenderService = Mockito.mock(MessageSenderService.class);
        doAnswer(invocation -> {
            final Trade trade = invocation.getArgument(1);
            openedTrades.put(trade.getSymbol(), trade);
            return null;
        }).when(messageSenderService).sendDelayedMessage(eq("orderStatusTempoQueue"), any(Trade.class), anyLong());
        return messageSenderService;
    }

    @Bean
    public OrderService orderService(final Map<Symbol, Position> openedPosition) {
        final OrderService orderService = Mockito.mock(OrderService.class);
        doAnswer(invocation -> {
            // make the changes you need here
            final Long unit = invocation.getArgument(1);
            final Symbol symbol = invocation.getArgument(0);
            final Position position = Position.builder().symbol(symbol).build();
            if (unit > 0) {
                position.setLongValue(Math.abs((double) unit));
                position.setShortValue(0D);
            } else {
                position.setShortValue(Math.abs((double) unit));
                position.setLongValue(0D);
            }
            openedPosition.put(symbol, position);
            return OrderCreateResponse.builder().build();
        })
                .when(orderService).requestOrder(any(), any(), any(), any());
        return orderService;
    }

    @Bean
    public PositionService positionService(final List<Result> result, final Map<Symbol, Position> openedPosition, final Map<Symbol, Trade> openedTrades) {
        final PositionService positionService = Mockito.mock(PositionService.class);
        final Consumer<Position> positionConsumer = position -> {
            if (!openedPosition.containsKey(position.getSymbol())) {
                return;
            }
            final Trade trade = openedTrades.get(position.getSymbol());
            result.add(new Result(trade.currentProfitInPip(), trade.getWay(), trade.getEntryTime(), trade.getLastCheck(), trade.getSymbol(), "", trade.getMaxLoss(), trade.getMaxProfit(), trade.getRisqueInPip()));
            openedPosition.remove(position.getSymbol());
            openedTrades.remove(position.getSymbol());
        };
        doAnswer(invocation -> {
            // make the changes you need here
            return new ArrayList<>(openedPosition.values());
        }).when(positionService).getOpenedPositions();
        doAnswer(invocation -> {
            // make the changes you need here
            return openedPosition.entrySet().stream().filter(e -> e.getKey() == invocation.getArgument(0)).map(e -> e.getValue()).collect(Collectors.toList());
        }).when(positionService).getOpenedPositions(any());
        doAnswer(invocation -> {
            // make the changes you need here
            final Position position = invocation.getArgument(0);
            positionConsumer.accept(position);
            return true;
        }).when(positionService).closeOpenedPosition(any(Position.class));
        doAnswer(invocation -> {
            // make the changes you need here
            final List<Position> positions = invocation.getArgument(0);
            positions.forEach(position -> positionConsumer.accept(position));
            return true;
        }).when(positionService).closeOpenedPosition(ArgumentMatchers.anyList());
        return positionService;
    }


    @Bean
    public ESRepository esRepository() {
        return Mockito.mock(ESRepository.class);
    }

    @Bean
    public TradeHistoryService tradeHistoryService() {
        return Mockito.mock(TradeHistoryService.class);
    }

    @Bean
    public RobotConfigurationService robotConfigurationService() {
        final RobotConfigurationService robotConfigurationService = Mockito.mock(RobotConfigurationService.class);
        when(robotConfigurationService.getRunBooking()).thenReturn(Boolean.TRUE);
        return robotConfigurationService;
    }

    @Bean
    public Map<Symbol, Trade> sessionProposals() {
        return new ConcurrentHashMap<>();
    }


    @Bean
    public Map<Symbol, Map<Symbol, Double>> correlations() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public Map<Symbol, Position> openedPosition() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public Map<Symbol, Trade> openedTrades() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public List<Result> result() {
        return new CopyOnWriteArrayList<>();
    }


    @Bean
    public OrderBookingHandler orderBookingHandler(final Strategy strategy,
                                                   final InstrumentService instrumentService,
                                                   final MessageSenderService messageSenderService,
                                                   final OrderService orderService,
                                                   final PositionService positionService,
                                                   final ESRepository esRepository,
                                                   final TradeHistoryService tradeHistoryService,
                                                   final RobotConfigurationService robotConfigurationService,
                                                   final Map<Symbol, Trade> sessionProposals) {
        return new OrderBookingHandlerImpl(strategy,instrumentService, messageSenderService, orderService, positionService, esRepository, tradeHistoryService, "orderBookingTempoQueue", "orderStatusTempoQueue", 14, 3000D, robotConfigurationService, sessionProposals);
    }

    @Bean
    public OrderStatusHandler orderStatusHandler(final Strategy strategy,
                                                 final InstrumentService instrumentService,
                                                 final MessageSenderService messageSenderService,
                                                 final PositionService positionService) {
        return new OrderStatusHandlerImpl(strategy,100, "orderStatusTempoQueue", instrumentService, positionService, messageSenderService);
    }
}
