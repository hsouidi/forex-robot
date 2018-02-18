package com.trading.forex.service.impl;

import com.trading.forex.connector.service.PositionService;
import com.trading.forex.model.Status;
import com.trading.forex.model.TradeHistoryResponse;
import com.trading.forex.service.BalanceService;
import com.trading.forex.service.RobotConfigurationService;
import com.trading.forex.service.RobotReportService;
import com.trading.forex.service.TradeHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;


@Service
public class RobotReportServiceImpl implements RobotReportService {


    @Autowired
    private RobotConfigurationService robotConfigurationService;

    @Autowired
    private BalanceService balanceService;

    @Autowired
    private PositionService positionService;

    @Autowired
    private TradeHistoryService tradeHistoryService;

    @Autowired
    private Date sessionDate;


    @Override
    public Status status() {
        final List<TradeHistoryResponse> closedTrades = tradeHistoryService.findSessionClosedTrade();
        closedTrades.sort((a, b) -> b.getCloseDate().compareTo(a.getCloseDate()));
        return Status.builder()
                .maxLoss(Optional.ofNullable(balanceService.getMaxloss()).orElse(0.0))
                .maxProfit(Optional.ofNullable(balanceService.getMaxProfit()).orElse(0.0))
                .mode(robotConfigurationService.getMode())
                .solde(Optional.ofNullable(balanceService.getSolde()).orElse(0.0))
                .serverStatus(Optional.ofNullable(robotConfigurationService.getRunBooking()).orElse(Boolean.FALSE))
                .limit(false)
                .nbOpenedTransaction(positionService.getOpenedPositions().size())
                .payout(Double.valueOf(robotConfigurationService.getUnit()))
                .positionProfit(positionService.getProfitOpenedPositions())
                .openedPositions(positionService.getOpenedPositions())
                .closedTrades(closedTrades)
                .maxPosition(balanceService.getMaxPosition())
                .minPosition(balanceService.getMinPosition())
                .sessionDate(sessionDate)
                .build();

    }
}
