package com.trading.forex.controller;

/**
 * Created by hsouidi on 10/20/2017.
 */

import com.trading.forex.common.exceptions.RobotTechnicalException;
import com.trading.forex.connector.service.PositionService;
import com.trading.forex.model.Status;
import com.trading.forex.model.TradeHistoryResponse;
import com.trading.forex.service.BalanceService;
import com.trading.forex.service.RobotConfigurationService;
import com.trading.forex.service.RobotReportService;
import com.trading.forex.service.TradeHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@CrossOrigin
//@PreAuthorize("hasAuthority('TRADER')")
@RequestMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.ALL_VALUE)
public class RobotRestController {


    @Autowired
    private RobotConfigurationService robotConfigurationService;

    @Autowired
    private RobotReportService robotReportService;
    @Autowired
    private BalanceService balanceService;

    @Autowired
    private PositionService positionService;


    @RequestMapping(value = "action/{action}", method = RequestMethod.GET)
    public String action(@PathVariable String action) {
        log.info("Execute Action :" + action);
        switch (action) {
            case "limit":
                break;
            case "stop":
                robotConfigurationService.setRunBookingManuel(false);
                break;
            case "start":
                robotConfigurationService.setRunBookingManuel(true);
                break;
            case "close":
                positionService.closeOpenedPosition();
                break;
            default:
                throw new RobotTechnicalException("Undefined action " + action);

        }
        return null;
    }

    @RequestMapping(value = "payout/{payout}", method = RequestMethod.POST)
    public void setPayout(@PathVariable Double payout) {
        log.info("Set Unit :" + payout);
        robotConfigurationService.setUnit(payout);
    }

    @RequestMapping(value = "status", method = RequestMethod.GET)
    @ResponseBody
    public Status status() {
        return robotReportService.status();

    }


    @RequestMapping(value = "solde/reset", method = RequestMethod.POST)
    public void reset() {
        log.info("Execute Action : reset");
        balanceService.reset();
    }
}