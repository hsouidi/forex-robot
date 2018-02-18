package com.trading.forex.model;

import com.trading.forex.common.exceptions.RobotTechnicalException;
import com.trading.forex.common.model.Candle;
import com.trading.forex.common.model.Symbol;
import com.trading.forex.common.model.Way;
import com.trading.forex.connector.model.Position;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

import static com.trading.forex.common.utils.AlgoUtils.toPip;

/**
 * Created by hsouidi on 11/01/2017.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Trade implements Serializable {

    private Way way;
    private Symbol symbol;
    private Double takeProift;
    private Double stopLoss;
    private String comment;
    private Double entryPoint;
    private Date entryTime;
    private Date lastCheck;
    private Double support;
    private Double resistance;
    private Double risqueInPip;
    private Double risque;
    private Double lastPrice;
    private Trend targetTrend;
    private Trend currentTrend;
    private boolean isUpgrated=false;
    private PivotPointResult pivotPointResult;
    private Double maxProfit=0D;
    private Double maxLoss=0D;


    public Trade(Way way, Symbol symbol) {
        this.way = way;
        this.symbol = symbol;
    }

    public Trade(Way way, Symbol symbol,PivotPointResult pivotPointResult) {
        this.way = way;
        this.symbol = symbol;
        this.pivotPointResult=pivotPointResult;
    }




    public Trade(Way way, Symbol symbol,double stopLoss) {
        this.way = way;
        this.symbol = symbol;
        this.stopLoss=stopLoss;
    }

    public void check(){
        if((stopLoss!=null&&way.getValue()*(entryPoint-stopLoss)<0)||(way.getValue()*(takeProift-entryPoint)<0)){
            throw new RuntimeException("invalid trade");
        }
    }

    public Double currentProfit() {
        return way.getValue() * (lastPrice - entryPoint);
    }

    public Double currentProfitInPip() {
        return toPip(symbol,way.getValue() * (lastPrice - entryPoint));
    }

    public Double status(Candle candle) {
        if (candle == null) {
            throw new RobotTechnicalException("Candle  is null !!!!");
        }
        switch (way) {
            case BUY:
                return takeProift != null && candle.getHigh() > takeProift ? takeProift : stopLoss != null && candle.getLow() < stopLoss ? stopLoss : 0;
            case SELL:
                return takeProift != null && candle.getLow() < takeProift ? takeProift : stopLoss != null && candle.getHigh() > stopLoss ? stopLoss : 0;
            default:
                return 0D;
        }
    }
}
