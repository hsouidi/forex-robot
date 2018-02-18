package com.trading.forex.listener.impl;

import com.trading.forex.common.model.Candle;
import com.trading.forex.common.model.CandlestickGranularity;
import com.trading.forex.common.model.Symbol;
import com.trading.forex.common.model.Way;
import com.trading.forex.common.utils.AlgoUtils;
import com.trading.forex.common.utils.CustomList;
import com.trading.forex.connector.model.OrderCreateResponse;
import com.trading.forex.connector.model.OrderStatus;
import com.trading.forex.connector.model.Position;
import com.trading.forex.connector.service.InstrumentService;
import com.trading.forex.connector.service.OrderService;
import com.trading.forex.connector.service.PositionService;
import com.trading.forex.entity.TradeHistoryEntity;
import com.trading.forex.indicators.impl.ATR;
import com.trading.forex.indicators.impl.ZigZag;
import com.trading.forex.listener.OrderBookingHandler;
import com.trading.forex.model.PivotPointResult;
import com.trading.forex.model.Trade;
import com.trading.forex.model.TradeNotification;
import com.trading.forex.model.Trend;
import com.trading.forex.repository.ESRepository;
import com.trading.forex.service.MessageSenderService;
import com.trading.forex.service.RobotConfigurationService;
import com.trading.forex.service.TradeHistoryService;
import com.trading.forex.strategies.Strategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static com.trading.forex.common.utils.AlgoUtils.*;
import static com.trading.forex.configuration.WebSocketConfig.TRADE_NOTIF_TOPIC;


@Service
@Slf4j
public class OrderBookingHandlerImpl implements OrderBookingHandler {

    private MessageSenderService messageSenderService;

    private OrderService orderService;

    private PositionService positionService;

    private ESRepository esRepository;

    private InstrumentService instrumentService;

    private String orderTempoQueue;

    private String orderStatusTempoQueue;

    private TradeHistoryService tradeHistoryService;

    private RobotConfigurationService robotConfigurationService;

    private Double unit;

    private int defaultMargin;

    private Map<Symbol, Trade> sessionProposals;

    private Strategy strategy;

    @Autowired
    public OrderBookingHandlerImpl(final Strategy strategy,
                                   final InstrumentService instrumentService,
                                   final MessageSenderService messageSenderService,
                                   final OrderService orderService,
                                   final PositionService positionService,
                                   final ESRepository esRepository,
                                   final TradeHistoryService tradeHistoryService,
                                   @Value("${order.booking.tempo.queue}") final String orderBookingTempoQueue,
                                   @Value("${order.status.tempo.queue}") final String orderStatusTempoQueue,
                                   @Value("${trade.default.margin.inPips}") int defaultMargin,
                                   @Value("${unit}") Double unit,
                                   final RobotConfigurationService robotConfigurationService,
                                   final Map<Symbol, Trade> sessionProposals) {

        this.strategy = strategy;
        this.unit = unit;
        this.defaultMargin = defaultMargin;
        this.messageSenderService = messageSenderService;
        this.orderService = orderService;
        this.positionService = positionService;
        this.esRepository = esRepository;
        this.instrumentService = instrumentService;
        this.orderTempoQueue = orderBookingTempoQueue;
        this.orderStatusTempoQueue = orderStatusTempoQueue;
        this.tradeHistoryService = tradeHistoryService;
        this.robotConfigurationService = robotConfigurationService;
        this.sessionProposals = sessionProposals;


    }

    @Override
    @RabbitListener(queues = "${order.booking.tempo.queue}")
    public void processTradeProposal(final Trade trade) {
        final Date to = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        processTradeProposal(trade, to);
    }

    @Override
    public void processTradeProposal(final Trade trade, final Date target) {
        final LocalDateTime currentDate=truncate(toLocalDateTime(target),strategy.getAnalysTimeFrame().getNumberMinutes());
        final Date to= AlgoUtils.toDate(currentDate);
        if (!robotConfigurationService.getRunBooking()) {
            log.info("robot is down -> ignore trade proposal {}", trade);
            return;
        }
        if (!sessionProposals.containsKey(trade.getSymbol())) {
            log.info(" obsolete proposal not found in proposals session   -> ignore trade proposal {}", trade);
            return;
        }
        final Symbol symbol = trade.getSymbol();
        log.info("check booking entry for {}", trade);
        final Way way = trade.getWay();
        final CustomList<Candle> candles = instrumentService.getPricing(strategy.getAnalysTimeFrame(), symbol, to, getFromDate(to, strategy.getAnalysTimeFrame(), 1500))
                .stream().collect(Collectors.toCollection(CustomList::new));
        final Candle candleM5 = candles.getLast();
        //final Date candleEnd=toDate(truncate(toLocalDateTime(candleM5.date()),strategy.getAnalysTimeFrame().getNumberMinutes()).plusMinutes(strategy.getAnalysTimeFrame().getNumberMinutes()));
        log.info("Last Candle {}", candleM5);
        if (candles.isEmpty()) {
            log.error("cannot found pricing for {} {}", to, CandlestickGranularity.M5);
            throw new RuntimeException("cannot found price ");
        }
        check(candleM5, to, this.getClass().getSimpleName(), 5);
        final Double currentPrice = candleM5.getClose();
        trade.setLastPrice(currentPrice);
        if (trade.getStopLoss() != null && trade.getTakeProift() != null) {
            book(trade, currentPrice, to);
        } else if (trade.getStopLoss() != null && way.getValue() * (currentPrice - trade.getStopLoss()) <= 0) {
            log.info(" ignore notif because stop loss is reached {}", trade);
            sessionProposals.remove(symbol);
        } else if (strategy.checkBooking(trade, candles)) {
            book(trade, currentPrice, to);
        } else {
            messageSenderService.sendDelayedMessage(orderTempoQueue, trade, getDelay(currentDate.plusMinutes(strategy.getAnalysTimeFrame().getNumberMinutes())));
        }

    }

    private void book(final Trade trade, final Double currentPrice, final Date to) {
        final LocalDateTime currentDate=truncate(toLocalDateTime(to),strategy.getAnalysTimeFrame().getNumberMinutes());
        final Symbol symbol = trade.getSymbol();
        final Way way = trade.getWay();
        final List<Position> openedPositions = positionService.getOpenedPositions(symbol);
        if (!openedPositions.isEmpty()) {
/*            final Position position = openedPositions.get(0);
            if (!getPositionWay(position.getShortValue(), position.getLongValue()).equals(way)) {
                log.info("trade proposal have inverse way of opened position {}  -> close opened position", trade);
                positionService.closeOpenedPosition(position);
            } else {
                log.info("trade proposal have same way as opened position {}  -> ignore trade", trade);*/
            log.info("trade proposal on the same symbol as opened position {} trade (} -> ignore trade", openedPositions.get(0), trade);
            sessionProposals.remove(symbol);
            return;
            //}
        }
        sessionProposals.remove(symbol);
        final Long unitValue = getUnit(unit, way).longValue();
        trade.setEntryPoint(currentPrice);
        trade.setEntryTime(to);
        trade.setResistance(trade.getTakeProift());
        if(null==trade.getSupport()){
            trade.setSupport(trade.getStopLoss());
        }
        log.info("Order Exec for trade " + trade.toString());
        pushToELK(trade, unitValue);
        trade.check();
        final OrderCreateResponse orderCreateResponse = orderService.requestOrder(trade.getSymbol()
                , unitValue
                , trade.getStopLoss()
                , null);
        if (OrderStatus.KO.equals(orderCreateResponse.getOrderStatus())) {
            log.warn(" Not Booked !! {}", orderCreateResponse.toString());
        } else {
            final TradeHistoryEntity tradeHistoryEntity = TradeHistoryEntity.fromBookingResponse(orderCreateResponse);
            tradeHistoryService.save(tradeHistoryEntity);
            messageSenderService.sendDelayedMessage(orderStatusTempoQueue, trade, getDelay(currentDate.plusMinutes(strategy.getAnalysTimeFrame().getNumberMinutes())));
            messageSenderService.sendStompMessage(TRADE_NOTIF_TOPIC, TradeNotification.build(tradeHistoryEntity));
        }
    }

    private void pushToELK(final Trade investingDataGroup, final Long unitValue) {
        Map<String, Object> map = new HashMap<>();
        map.put("symbol", investingDataGroup.getSymbol());
        map.put("way", investingDataGroup.getWay());
        map.put("unit", unitValue);
        map.put("stopLoss", investingDataGroup.getStopLoss());
        map.put("takeProfit", investingDataGroup.getTakeProift());
        map.put("data-type", "order-exec");
        esRepository.push(map);
    }

}