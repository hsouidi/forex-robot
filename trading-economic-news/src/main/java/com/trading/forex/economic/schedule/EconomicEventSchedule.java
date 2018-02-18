package com.trading.forex.economic.schedule;

import org.springframework.scheduling.annotation.Scheduled;

/**
 * Created by hsouidi on 11/21/2017.
 */
public interface EconomicEventSchedule {


    void extractEvent();

    @Scheduled(fixedDelay = 5000)
    void extractSignal();
}
