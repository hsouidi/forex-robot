package com.trading.forex.economic.entity;

import com.trading.forex.common.model.Currency;
import com.trading.forex.common.model.EconomicCalendarData;
import com.trading.forex.common.model.Importance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by hsouidi on 11/19/2017.
 */
@Data
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class EconomicCalendarEntity {

    @EmbeddedId
    private EconomicCalendarID economicCalendarID;

    @Enumerated(EnumType.STRING)
    private Currency currency;
    private Double actual;
    private Double forecast;
    private Double previous;
    @Enumerated(EnumType.STRING)
    private Importance importance;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public static class EconomicCalendarID implements Serializable {
        private Date eventDate;
        private String event;
    }

    public static EconomicCalendarData toEconomicCalendarData(EconomicCalendarEntity economicCalendarEntity){
        return EconomicCalendarData.builder()
                .actual(economicCalendarEntity.actual)
                .currency(economicCalendarEntity.currency)
                .event(economicCalendarEntity.economicCalendarID.event)
                .eventDate(economicCalendarEntity.economicCalendarID.eventDate)
                .forecast(economicCalendarEntity.forecast)
                .importance(economicCalendarEntity.importance)
                .previous(economicCalendarEntity.previous)
                .build();
    }

    public static List<EconomicCalendarData> toEconomicCalendarDataList(List<EconomicCalendarEntity> economicCalendarEntity){
        return economicCalendarEntity.stream().map(EconomicCalendarEntity::toEconomicCalendarData).collect(Collectors.toList());
    }
}


