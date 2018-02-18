package com.trading.forex.listener.impl;

import com.trading.forex.common.model.Candle;
import com.trading.forex.common.model.CandlestickGranularity;
import com.trading.forex.common.model.Symbol;
import com.trading.forex.common.utils.CustomList;
import com.trading.forex.connector.model.Position;
import com.trading.forex.connector.service.InstrumentService;
import com.trading.forex.connector.service.PositionService;
import com.trading.forex.indicators.impl.HeikenAshi;
import com.trading.forex.listener.OrderStatusHandler;
import com.trading.forex.model.PivotPointResult;
import com.trading.forex.model.Trade;
import com.trading.forex.service.MessageSenderService;
import com.trading.forex.strategies.Strategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.trading.forex.common.utils.AlgoUtils.*;
import static com.trading.forex.common.utils.AlgoUtils.toLocalDateTime;
import static java.time.temporal.ChronoUnit.MINUTES;

@Service
@Slf4j
public class OrderStatusHandlerImpl implements OrderStatusHandler {

    private InstrumentService instrumentService;

    private PositionService positionService;

    private MessageSenderService messageSenderService;

    private String orderStatusQueue;

    private Strategy strategy;

    @Autowired
    public OrderStatusHandlerImpl(Strategy strategy, @Value("${trade.default.margin.inPips}") final int defaultMargin
            , @Value("${order.status.tempo.queue}") final String orderStatusQueue
            , final InstrumentService instrumentService
            , final PositionService positionService
            , final MessageSenderService messageSenderService) {

        this.instrumentService = instrumentService;
        this.positionService = positionService;
        this.messageSenderService = messageSenderService;
        this.orderStatusQueue = orderStatusQueue;
        this.strategy = strategy;
    }

    @Override
    @RabbitListener(queues = "${order.status.tempo.queue}")
    public void handleStatus(final Trade trade) {
        final Date to = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        handleStatus(trade, to);
    }

    @Override
    public void handleStatus(final Trade trade, final Date to) {

        log.info("check booking status for {}", trade);
        final Symbol symbol = trade.getSymbol();
        CandlestickGranularity candlestickGranularity=CandlestickGranularity.M1;
        CustomList<Candle> candles = instrumentService.getPricing(candlestickGranularity, symbol, to, getFromDate(to,candlestickGranularity, 5000));

        if (candles.isEmpty()) {
            log.error("cannot found pricing for {} {}", to, candlestickGranularity);
            throw new RuntimeException("cannot found price ");
        }

        final Candle candle = candles.getLast();
        final LocalDateTime currentDate=truncate(toLocalDateTime(to),strategy.getCheckStatusTimeFrame().getNumberMinutes());

        check(candle, to, this.getClass().getSimpleName(), 1);
        final Double closePrice = candle.getClose();
        final double result = toPip(symbol, trade.getWay().getValue() * (closePrice - trade.getEntryPoint()));
        trade.setMaxLoss(result < 0 && result < trade.getMaxLoss() ? result : trade.getMaxLoss());
        trade.setMaxProfit(result > 0 && result > trade.getMaxProfit() ? result : trade.getMaxProfit());
        trade.setLastPrice(closePrice);
        trade.setLastCheck(to);
        final List<Position> openedPosition = positionService.getOpenedPositions(symbol);
        if (openedPosition.isEmpty()) {
            log.info(" no position for trade {}  ->  ignore", trade.toString());
            return;
        }else if (strategy.checkStatus(trade, candles)) {
            log.info("strategy decision close position ==> close position for {}", trade.toString());
            positionService.closeOpenedPosition(positionService.getOpenedPositions(symbol));
        } else
            messageSenderService.sendDelayedMessage(orderStatusQueue, trade, getDelay(currentDate.plusMinutes(strategy.getCheckStatusTimeFrame().getNumberMinutes())));
    }

}

