package com.trading.forex.client;

import lombok.Data;

import java.util.List;

@Data
public class PolygonMarketData {


    private String ticker;
    private String status;
    private Long queryCount;
    private Boolean adjusted;
    private List<PolygonCandle> results;

}
