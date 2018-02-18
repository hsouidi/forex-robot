package com.trading.forex.indicators.impl;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@AllArgsConstructor
@Data
public class TrendPoint {

    private double price;
    private Date date;
}
