package com.trading.forex.economic.schedule.impl;

import com.trading.forex.economic.repository.EconomicCalendarRepository;
import com.trading.forex.economic.repository.ForexSignalRepository;
import com.trading.forex.economic.schedule.EconomicEventSchedule;
import com.trading.forex.economic.service.IndicatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Created by hsouidi on 11/21/2017.
 */
@Service
public class EconomicEventScheduleImpl implements EconomicEventSchedule {


    @Autowired
    private IndicatorService indicatorService;

    @Autowired
    private EconomicCalendarRepository economicCalendarRepository;

    @Autowired
    private ForexSignalRepository forexSignalRepository;

    @Override
    @Scheduled(fixedDelay = 120000)
    public void extractEvent() {
            economicCalendarRepository.saveAll(indicatorService.getEconomicCalendarData(null, null));
    }

    @Override
    //@Scheduled(fixedDelay = 5000)
    public void extractSignal() {
        //forexSignalRepository.saveAll(indicatorService.getForexSignal());
    }
}
