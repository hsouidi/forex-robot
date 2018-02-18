package com.trading.forex.model;

import com.trading.forex.common.model.Symbol;
import com.trading.forex.common.model.Way;
import com.trading.forex.entity.TradeHistoryEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class TradeHistoryResponse implements Serializable {

    private Symbol symbol;
    private Way way;
    private Double pip;
    private Date inputDate;
    private Date closeDate;


    public static TradeHistoryResponse fromEntity(final TradeHistoryEntity tradeHistoryEntity) {
        return TradeHistoryResponse.builder()
                .symbol(tradeHistoryEntity.getSymbol())
                .way(tradeHistoryEntity.getWay())
                .pip(tradeHistoryEntity.getPip())
                .inputDate(tradeHistoryEntity.getTradeDate())
                .closeDate(tradeHistoryEntity.getEndTime())
                .build();
    }
}
