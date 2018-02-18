package com.trading.forex.economic.model;

import com.trading.forex.common.model.Symbol;
import com.trading.forex.common.model.Way;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ZuluPosition {

    private Symbol currency;
    private Way type;
    private Double lots;
    private Date openDate;
    private Double openPrice;
    private Double stopPrice;
    private Double takeProfit;
    private Double currentPrice;
}
