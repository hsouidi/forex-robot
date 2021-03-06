package com.trading.forex.entity;


import com.trading.forex.common.model.Symbol;
import com.trading.forex.common.model.Way;
import com.trading.forex.connector.model.OrderCreateResponse;
import com.trading.forex.indicators.impl.Ichimoku;
import com.trading.forex.model.Decision;
import com.trading.forex.model.InvestingData;
import com.trading.forex.model.InvestingDataGroup;
import com.trading.forex.model.InvestingTechIndicator;
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
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class TradeHistory {

    private Double buyPrice;
    private Double takeprofit;
    private Double stopLoss;
    private String orderId;
    private Double result;
    private Double pip;
    private Date endTime;
    @Column(name="TRADE_ID")
    private String tradeId;
    @Id
    private String transactionId;
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


    public static TradeHistory fromBookingResponse(OrderCreateResponse response, Way way, InvestingTechIndicator investingTechIndicator, InvestingDataGroup investingDataGroup, Ichimoku ichimoku) {
        final InvestingData investingData = investingDataGroup.getSummary();
        return TradeHistory.builder()
                .tradeId(response.getTradeID())
                .tradeDate(new Date())
                .takeprofit(response.getTakeProfit())
                .stopLoss(response.getStopLoss())
                .symbol(response.getSymbol())
                .buyPrice(response.getPrice())
                .adx(investingTechIndicator.getAdx().getValue())
                .adxAction(investingTechIndicator.getAdx().getAction().name())
                .techIndicatorResult(investingTechIndicator.getSummary().name())
                .techIndicatorBuyNb(investingTechIndicator.getBuy())
                .techIndicatorSellNb(investingTechIndicator.getSell())
                .techIndicatorNeutralNb(investingTechIndicator.getNeutral())
                .atr(investingTechIndicator.getAtr().getValue())
                .atrVolatility(investingTechIndicator.getAtr().getAction().name())
                .cci(investingTechIndicator.getCci().getValue())
                .macd(investingTechIndicator.getMacd().getValue())
                .rsi(investingTechIndicator.getRsi().getValue())
                .stochRSI(investingTechIndicator.getStochRsi().getValue())
                .williamR(investingTechIndicator.getWilliamR().getValue())
                .ultimateOscillator(investingTechIndicator.getUltimateOscilator().getValue())
                .chikou(ichimoku.getChikou().getLast())
                .kijunSen(ichimoku.getKijunSen().getLast())
                .kumo(ichimoku.getKumo().getLast())
                .spanA(ichimoku.getSpanA().getLast())
                .spanB(ichimoku.getSpanB().getLast())
                .roc(investingTechIndicator.getRoc().getValue())
                .indicatorResultCinqMinute(investingData.getCinqMinute())
                .indicatorResultQuinzeMinute(investingData.getQuinzeMinute())
                .indicatorResultHeure(investingData.getHeure())
                .indicatorResultJour(investingData.getJour())
                .indicatorResultMensuel(investingData.getMensuel())
                .way(response.getWay())
                .build();
    }


}
