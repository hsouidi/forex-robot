package com.trading.forex.common.model;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class EconomicData {

    private List<EconomicCalendarData> economicCalendars;
}
