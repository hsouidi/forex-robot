package com.trading.forex.economic.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "oanda",url="https://api-fxpractice.oanda.com")
public interface OandaEconomicCalendar {

    @RequestMapping(method = RequestMethod.GET, value = "/labs/v1/calendar", produces = "application/json")
    List<EconomicCalendarData> getEconomicCalendarData(final @RequestParam("instrument") String instrument,@RequestParam("period") final Integer period);
}
