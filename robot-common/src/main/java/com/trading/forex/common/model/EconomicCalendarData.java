package com.trading.forex.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by hsouidi on 11/19/2017.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EconomicCalendarData {

    private Date eventDate;
    private String event;
    private Currency currency;
    private Double actual;
    private Double forecast;
    private Double previous;
    private Importance importance;

}


