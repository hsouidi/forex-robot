package com.trading.forex.common.model;


import lombok.Builder;
import lombok.Data;

import java.util.Date;

import static com.trading.forex.common.utils.AlgoUtils.toPip;


/**
 * Created by hsouidi on 10/21/2017.
 */
@Builder
@Data
public class Candle {

    private Long epoch;
    private Date date;
    private Double close;
    private Double high;
    private Double low;
    private Double open;
    private double volume;
    private Symbol symbol;
    private Boolean complete;

    private static String FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'";

    public double body() {
        return toPip(symbol, this.close - this.open);
    }

    public Date date() {
        return new Date(epoch);
    }
}
