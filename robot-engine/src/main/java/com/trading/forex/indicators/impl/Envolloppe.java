package com.trading.forex.indicators.impl;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;
import java.util.List;

@AllArgsConstructor
@Data
public class Envolloppe {

    private List<TrendPoint> haut;
    private List<TrendPoint> bas;
}
