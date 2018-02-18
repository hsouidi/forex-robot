package com.trading.forex.model;

import com.trading.forex.common.model.Symbol;
import com.trading.forex.common.model.Way;
import com.trading.forex.entity.TradeHistoryEntity;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

import static com.trading.forex.common.utils.AlgoUtils.toPip;

@Data
@Builder
public class TradeNotification implements Serializable {

    private Way way;
    private Symbol symbol;
    private Double realPip;
    private Double expectedPip;


    public static TradeNotification build(final TradeHistoryEntity tradeHistoryEntity) {
        return TradeNotification.builder()
                .way(tradeHistoryEntity.getWay())
                .symbol(tradeHistoryEntity.getSymbol())
                .realPip(tradeHistoryEntity.getPip())
                .expectedPip(tradeHistoryEntity.getTakeprofit()!=null?toPip(tradeHistoryEntity.getSymbol(), tradeHistoryEntity.getWay().getValue() * (tradeHistoryEntity.getTakeprofit() - tradeHistoryEntity.getBuyPrice())):tradeHistoryEntity.getStopLoss()!=null?toPip(tradeHistoryEntity.getSymbol(), tradeHistoryEntity.getWay().getValue() * (tradeHistoryEntity.getBuyPrice()-tradeHistoryEntity.getStopLoss())):0.0)
                .build();

    }
}
