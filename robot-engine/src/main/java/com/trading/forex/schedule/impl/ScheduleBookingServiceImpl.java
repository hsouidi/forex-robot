package com.trading.forex.schedule.impl;

import com.trading.forex.connector.service.PositionService;
import com.trading.forex.controller.RobotRestController;
import com.trading.forex.schedule.ScheduleBookingService;
import com.trading.forex.service.RobotConfigurationService;
import com.trading.forex.service.RobotMailService;
import com.trading.forex.strategies.executors.StategyExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.function.Predicate;

/**
 * Created by hsouidi on 10/19/2017.
 */
@Service
@Slf4j
public class ScheduleBookingServiceImpl implements ScheduleBookingService {

    @Autowired
    private RobotConfigurationService robotConfigurationService;

    @Autowired
    private StategyExecutorService stategyExecutorService;

    @Value("${robot.endOfDay}")
    private String robotEndOfDay;

    @Value("${robot.startOfDay}")
    private String robotStartOfDay;

    @Autowired
    private RobotMailService robotMailService;


    @Autowired
    private PositionService positionService;

    @Override
    @Scheduled(fixedDelayString = "${robot.schedule.delay}")
    public void runStrategy() {
        Predicate<LocalDateTime> localDateTimePredicate = time -> time.getMinute() % 5 == 0;
        LocalDateTime localDateTime = LocalDateTime.now();
        if (localDateTime.getDayOfWeek() == DayOfWeek.SUNDAY || localDateTime.getDayOfWeek() == DayOfWeek.SATURDAY) {
            log.info("Robot is off on weekend ..");
            robotConfigurationService.setRunBooking(false);
        } else if (LocalTime.parse(robotEndOfDay).isBefore(localDateTime.toLocalTime())) {
            log.info("End of day reached  --> stop robot and close all positions");
            robotConfigurationService.setRunBooking(false);
            positionService.closeOpenedPosition();
        } else if (LocalTime.parse(robotStartOfDay).isAfter(localDateTime.toLocalTime())) {
            log.info("start of day not ready  --> stop robot");
            robotConfigurationService.setRunBooking(false);
        } else if (!robotConfigurationService.getRunBooking() && LocalTime.parse(robotStartOfDay).isBefore(localDateTime.toLocalTime())) {
            log.info("start of day  --> start robot");
            robotConfigurationService.setRunBooking(true);
            robotMailService.sendStatusMail("["+robotConfigurationService.getMode()+"] Robot started");
        } else {
            LocalDateTime currentTime = localDateTime.minusSeconds(localDateTime.getSecond()).minusNanos(localDateTime.getNano());
            if (robotConfigurationService.getRunBooking() && localDateTimePredicate.test(currentTime) && localDateTime.toLocalTime().isBefore(LocalTime.parse(robotEndOfDay).minusMinutes(55))) {
                stategyExecutorService.process(currentTime);
            }

            while (localDateTimePredicate.test(LocalDateTime.now())) {
                // Wait end process time
            }
        }
    }

}
