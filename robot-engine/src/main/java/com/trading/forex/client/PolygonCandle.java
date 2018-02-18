package com.trading.forex.client;

import lombok.Data;

import java.util.List;

@Data
public class PolygonCandle {

    private Double c;
    private Double h;
    private Double l;
    private Double o;
    private Long t;
    private Double v;
}
