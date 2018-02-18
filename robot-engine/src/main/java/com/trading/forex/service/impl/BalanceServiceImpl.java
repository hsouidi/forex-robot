package com.trading.forex.service.impl;

import com.trading.forex.connector.service.PortfolioInfosService;
import com.trading.forex.connector.service.PositionService;
import com.trading.forex.repository.ESRepository;
import com.trading.forex.service.BalanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by wf on 10/20/2017.
 */
@Service
public class BalanceServiceImpl implements BalanceService {

    private Double beginBalance = null;
    private Double solde = null;
    private Double maxloss = 0D;
    private Double maxProfit = 0D;
    private Double maxPosition = 0D;
    private Double minPosition = 0D;

    @Autowired
    private PortfolioInfosService portfolioInfosService;

    @Autowired
    private PositionService positionService;

    @Autowired
    private ESRepository esRepository;

    @Override
    @Scheduled(fixedDelay = 5000)
    public void updateBalanceInfos() {
        double current = portfolioInfosService.getBalance();
        double currentPosition = positionService.getProfitOpenedPositions();
        if (null == beginBalance) {
            beginBalance = current;
            solde = 0.0;
            return;
        }
        solde = current - beginBalance;
        if (solde > maxProfit) {
            maxProfit = solde;
        }
        if (solde < maxloss) {
            maxloss = solde;
        }
        if (currentPosition > maxPosition) {
            maxPosition = currentPosition;
        }
        if (currentPosition < minPosition) {
            minPosition = currentPosition;
        }
        pushToELK();


    }

    private void pushToELK() {
        Map<String, Object> map = new HashMap<>();
        map.put("account-solde", solde);
        map.put("data-type", "account-solde");
        esRepository.push(map);
    }

    @Override
    public void reset() {

        beginBalance = null;
        solde = null;
        maxloss = 0D;
        maxProfit = 0D;
    }


    @Override
    public Double getSolde() {
        return solde;
    }

    @Override
    public Double getMaxloss() {
        return maxloss;
    }

    @Override
    public Double getMaxProfit() {
        return maxProfit;
    }

    @Override
    public Double getMaxPosition() {
        return maxPosition;
    }

    @Override
    public Double getMinPosition() {
        return minPosition;
    }
}
