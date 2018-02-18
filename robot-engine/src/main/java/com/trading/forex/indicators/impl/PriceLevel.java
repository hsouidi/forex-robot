package com.trading.forex.indicators.impl;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class PriceLevel {

    private int weight;
    private Double price;
}
