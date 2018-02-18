package com.trading.forex.client;

/**
 * Created by hsouidi on 10/20/2017.
 */

import com.trading.forex.common.model.EconomicCalendarData;
import com.trading.forex.common.model.EconomicData;
import com.trading.forex.common.model.Importance;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.List;

@FeignClient(value="economic-news",url = "http://localhost:8087")
public interface EconomicNewsClient {


    @RequestMapping(value = "/economic/news", method = RequestMethod.GET)
    EconomicData economicCalendar(@RequestParam(value = "date", required = false) final String date, @RequestParam(value = "importance", required = false) final Importance importance);

    @RequestMapping(value = "/economic/news/{date}", method = RequestMethod.POST)
    void economicCalendarPut(@PathVariable("date") final String date) ;
}