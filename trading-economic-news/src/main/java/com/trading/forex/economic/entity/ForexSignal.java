package com.trading.forex.economic.entity;


import com.trading.forex.common.model.Symbol;
import com.trading.forex.common.model.Way;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.persistence.Entity;
import java.io.Serializable;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class ForexSignal {

    @EmbeddedId
    private ForexSignalID forexSignalID;
    private Date till;
    private Double stopLoss;
    private Double takeProfit;
   @Enumerated(EnumType.STRING)
    private Way way;
    private Double sellAt;
    private Double buyAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public static class ForexSignalID implements Serializable {
        @Enumerated(EnumType.STRING)
        private Symbol symbol;
        private Date fromDate;
    }
}
