package com.trading.forex.service.impl;

import com.trading.forex.entity.TradeHistoryEntity;
import com.trading.forex.model.TradeHistoryResponse;
import com.trading.forex.repository.TradeHistoryRepository;
import com.trading.forex.service.TradeHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TradeHistoryServiceImpl implements TradeHistoryService{

    private TradeHistoryRepository tradeHistoryRepository;

    private Date sessionDate;

    @Autowired
    public TradeHistoryServiceImpl(final TradeHistoryRepository tradeHistoryRepository,Date sessionDate){
        this.tradeHistoryRepository=tradeHistoryRepository;
        this.sessionDate=sessionDate;

    }


    @Override
    @Transactional
    public TradeHistoryEntity save(TradeHistoryEntity tradeHistoryEntity) {
        return tradeHistoryRepository.save(tradeHistoryEntity);
    }

    @Override
    public List<TradeHistoryEntity> findOpenedTrade() {
        return tradeHistoryRepository.findByResultIsNull();
    }

    @Override
    public List<TradeHistoryResponse> findSessionClosedTrade() {
        return tradeHistoryRepository.findByResultIsNotNullAndTradeDateAfterOrderByTradeDateDesc(sessionDate).stream()
                .map(TradeHistoryResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
