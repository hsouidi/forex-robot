package com.trading.forex.model;

import com.trading.forex.connector.model.Position;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * Created by hsouidi on 07/11/2017.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Status {

    private Boolean serverStatus;
    private Double solde;
    private Double maxProfit;
    private Double maxLoss;
    private String mode;
    private Boolean limit;
    private Integer nbOpenedTransaction;
    private Double payout;
    private Double positionProfit;
    private List<Position> openedPositions;
    private List<TradeHistoryResponse> closedTrades;
    private Double maxPosition;
    private Double minPosition;
    private Date sessionDate;
}
