package com.trading.forex.service;

import com.trading.forex.entity.TradeHistoryEntity;
import com.trading.forex.model.TradeHistoryResponse;

import java.util.List;

public interface TradeHistoryService {

    TradeHistoryEntity save(TradeHistoryEntity tradeHistoryEntity);
    List<TradeHistoryEntity> findOpenedTrade();
    List<TradeHistoryResponse> findSessionClosedTrade();
}
