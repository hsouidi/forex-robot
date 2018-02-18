package com.trading.forex.service;

public interface RobotConfigurationService {
    String getMode();

    void setMode(String mode);

    Double getUnit();

    void setUnit(Double unit);

    Boolean getRunBooking();

    void setRunBooking(Boolean runBooking);

    void setRunBookingManuel(Boolean runBooking);
}
