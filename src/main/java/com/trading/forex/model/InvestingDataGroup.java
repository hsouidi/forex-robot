package com.trading.forex.model;

import lombok.Builder;
import lombok.Data;

/**
 * Created by wf on 07/10/2017.
 */
@Data
@Builder
public class InvestingDataGroup {

    private InvestingData movingAverage;
    private InvestingData technicalIndicator;
    private InvestingData summary;
    private Symbol symbol;
}