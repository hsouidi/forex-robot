package com.trading.forex.service.impl;

import com.trading.forex.service.RobotConfigurationService;
import com.trading.forex.service.RobotMailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RobotConfigurationServiceImpl implements RobotConfigurationService {

    @Value("${env.profile}")
    private String mode;

    @Autowired
    private RobotMailService robotMailService;

    private Boolean runBookingManual = true;
    private Boolean runBooking = false;

    @Value("${unit}")
    private Double unit;


    @Override
    public String getMode() {
        return mode;
    }

    @Override
    public void setMode(String mode) {
        this.mode = mode;
    }

    @Override
    public Double getUnit() {
        return unit;
    }

    @Override
    public void setUnit(Double unit) {
        this.unit = unit;
    }

    @Override
    public Boolean getRunBooking() {
        return runBooking && runBookingManual;
    }

    @Override
    public void setRunBooking(Boolean runBooking) {
        boolean sendMail=false;
        if (this.runBooking && !runBooking) {
            // Stop robot
            sendMail=true;
        }
        this.runBooking = runBooking;
        if(sendMail){
            try {
                log.info("wait close position");
                Thread.sleep(60000);
                robotMailService.sendStatusMail("[" + mode + "] Report EOD ");
            } catch (InterruptedException e) {
                log.error(e.getMessage(),e);
            }
        }

    }

    @Override
    public void setRunBookingManuel(Boolean runBooking) {
        this.runBookingManual = runBooking;
    }
}
