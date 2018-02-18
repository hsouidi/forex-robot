package com.trading.forex.schedule.impl;

import com.trading.forex.connector.model.BrokerTrade;
import com.trading.forex.connector.model.TradeStatus;
import com.trading.forex.connector.service.PositionService;
import com.trading.forex.connector.service.TradeService;
import com.trading.forex.entity.TradeHistoryEntity;
import com.trading.forex.model.TradeNotification;
import com.trading.forex.repository.ESRepository;
import com.trading.forex.schedule.TradeScheduleService;
import com.trading.forex.service.MessageSenderService;
import com.trading.forex.service.TradeHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.trading.forex.configuration.WebSocketConfig.TRADE_NOTIF_TOPIC;

/**
 * Created by hsouidi on 11/09/2017.
 */
@Service
@Slf4j
public class TradeScheduleServiceImpl implements TradeScheduleService {


    @Autowired
    private TradeHistoryService tradeHistoryService;

    @Autowired
    private TradeService tradeService;

    @Autowired
    private ESRepository esRepository;

    @Autowired
    private PositionService positionService;

    @Autowired
    private MessageSenderService messageSenderService;


    @Override
    @Scheduled(fixedDelay = 10000)
    public void runSchedule() {
        pushToELK();
        List<TradeHistoryEntity> tradeHistoryEntityList = tradeHistoryService.findOpenedTrade();
        if (!tradeHistoryEntityList.isEmpty()) {
            final List<String> tradeIds = tradeHistoryEntityList.stream()
                    .map(tradeHistory -> tradeHistory.getTradeId()).collect(Collectors.toList());
            try {
                final Map<String, BrokerTrade> tradeMap = tradeService.getTradesByIds(tradeIds).stream()
                        .collect(Collectors.toMap(o -> o.getTradeID(), Function.identity()));
                tradeHistoryEntityList.stream().forEach(tradeHistory -> {
                    final BrokerTrade trade = tradeMap.get(tradeHistory.getTradeId());
                    if (trade != null && TradeStatus.CLOSED.equals(trade.getStatus())) {
                        tradeHistory.setResult(trade.getResult());
                        tradeHistory.setEndTime(trade.getEndTime());
                        tradeHistory.setPip(tradeHistory.getWay().getValue() * trade.getPip());
                        messageSenderService.sendStompMessage(TRADE_NOTIF_TOPIC, TradeNotification.build(tradeHistory));
                        tradeHistoryService.save(tradeHistory);
                    }
                });
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private void pushToELK() {
        Map<String, Object> map = new HashMap<>();
        map.put("position-profit", positionService.getProfitOpenedPositions());
        map.put("data-type", "position-profit");
        esRepository.push(map);
    }


}
