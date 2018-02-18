package com.trading.forex.economic.controller;

/**
 * Created by hsouidi on 10/20/2017.
 */

import com.trading.forex.common.utils.CustomList;
import com.trading.forex.economic.entity.EconomicCalendarEntity;
import com.trading.forex.common.model.EconomicData;
import com.trading.forex.economic.entity.ForexSignal;
import com.trading.forex.common.model.Importance;
import com.trading.forex.economic.model.ZuluPosition;
import com.trading.forex.economic.repository.EconomicCalendarRepository;

import com.trading.forex.economic.service.IndicatorService;
import com.trading.forex.economic.service.impl.IndicatorServiceImpl;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.util.Arrays.asList;

@Slf4j
@RestController
@CrossOrigin
@RequestMapping(value = "/economic", produces = MediaType.APPLICATION_JSON_VALUE)
public class EconomicNewsController {


    @Autowired
    private EconomicCalendarRepository economicCalendarRepository;

    @Autowired
    private IndicatorService indicatorService;


    @RequestMapping(value = "/news", method = RequestMethod.GET)
    @ResponseBody
    @Transactional
    public EconomicData economicCalendar(@RequestParam(value = "date", required = false) final String date,@RequestParam(value = "importance", required = false) final Importance importance) throws ParseException, InterruptedException {
        final LocalDate localDate = date==null?LocalDate.now():LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyyMMdd"));
        final List<EconomicCalendarEntity> economicCalendarEntityList = economicCalendarRepository.findAllByEventDateAndImportance(toDate(localDate.atTime(LocalTime.MIN)), toDate(localDate.atTime(LocalTime.MAX)), asList(Importance.HIGH));
/*        if(economicCalendarEntityList.isEmpty()){
            economicCalendarPut(localDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        }*/
        economicCalendarEntityList.sort(Comparator.comparing(a -> a.getEconomicCalendarID().getEventDate()));
        return new EconomicData(EconomicCalendarEntity.toEconomicCalendarDataList(economicCalendarEntityList));

    }

    @RequestMapping(value = "/news/{date}", method = RequestMethod.POST)
    @ApiOperation(value = "retrive economic news and update database, date format yyyyMMdd")
    @ResponseBody
    @Transactional
    public void economicCalendarPut(@PathVariable("date") final String date) throws ParseException, InterruptedException {
        economicCalendarRepository.saveAll(indicatorService.getEconomicCalendarData(null
               , new SimpleDateFormat("yyyy/MMdd").format(new SimpleDateFormat("yyyyMMdd").parse(date))));
    }

    private Date toDate(final LocalDateTime localDate) {
        return Date.from(localDate.atZone(ZoneId.systemDefault()).toInstant());
    }
    private Date toDate(final LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }


    @GetMapping(value = "/test",produces = MediaType.APPLICATION_STREAM_JSON_VALUE)
    @ApiOperation(value = "test")
    public Flux<IndicatorServiceImpl.StockTransaction>  stockTransactionEvents(){

        return null;
    }

    @RequestMapping(value = "/signals/{traderId}", method = RequestMethod.GET)
    @ApiOperation(value = "retrive forex signals")
    @ResponseBody
    public List<ZuluPosition> getSignals(@PathVariable("traderId") String traderId){
        return indicatorService.getZuluPosition(traderId);
    }

}