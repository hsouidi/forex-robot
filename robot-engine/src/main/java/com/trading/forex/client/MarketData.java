package com.trading.forex.client;

import lombok.Data;

import java.util.List;

@Data
public class MarketData {

    private List<Double> c;
    private List<Double> h;
    private List<Double> l;
    private List<Double> o;
    private String s;
    private List<Long> t;
    private List<Double> v;
}
