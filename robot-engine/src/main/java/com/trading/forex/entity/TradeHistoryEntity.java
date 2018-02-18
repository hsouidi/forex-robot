package com.trading.forex.entity;


import com.trading.forex.common.model.Decision;
import com.trading.forex.common.model.Symbol;
import com.trading.forex.common.model.Way;
import com.trading.forex.connector.model.OrderCreateResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by hsouidi on 05/09/2017.
 */
@Data
@Builder
@Entity(name = "TRADE_HISTORY")
@NoArgsConstructor
@AllArgsConstructor
public class TradeHistoryEntity {

    private Double buyPrice;
    private Double takeprofit;
    private Double stopLoss;
    private String orderId;
    private Double result;
    private Double pip;
    private Date endTime;
    @Id
    @Column(name = "TRADE_ID")
    private String tradeId;
    private Double adx;
    private String adxAction;
    private Double rsi;
    private Double atr;
    private String atrVolatility;
    @Enumerated(EnumType.STRING)
    private Decision indicatorResultCinqMinute;
    @Enumerated(EnumType.STRING)
    private Decision indicatorResultQuinzeMinute;
    @Enumerated(EnumType.STRING)
    private Decision indicatorResultHeure;
    @Enumerated(EnumType.STRING)
    private Decision indicatorResultJour;
    @Enumerated(EnumType.STRING)
    private Decision indicatorResultMensuel;
    private String techIndicatorResult;
    private Double techIndicatorBuyNb;
    private Double techIndicatorSellNb;
    private Double techIndicatorNeutralNb;
    private Double cci;
    private Double macd;
    private Double williamR;
    private Double stochRSI;
    private Double ultimateOscillator;
    private Double roc;
    private Double kumo;
    private Double chikou;
    private Double tenkenSen;
    private Double kijunSen;
    private Double spanB;
    private Double spanA;
    @Enumerated(EnumType.STRING)
    private Symbol symbol;
    @Enumerated(EnumType.STRING)
    private Way way;
    private Date tradeDate;

    public static TradeHistoryEntity fromBookingResponse(final OrderCreateResponse response) {
        return TradeHistoryEntity.builder()
                .tradeId(response.getTradeID())
                .tradeDate(new Date())
                .takeprofit(response.getTakeProfit())
                .stopLoss(response.getStopLoss())
                .symbol(response.getSymbol())
                .buyPrice(response.getPrice())
                .way(response.getWay())
                .build();
    }


}
