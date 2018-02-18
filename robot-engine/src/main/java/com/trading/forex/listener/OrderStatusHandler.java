package com.trading.forex.listener;

import com.trading.forex.model.Trade;

import java.util.Date;

public interface OrderStatusHandler {

    void handleStatus(final Trade trade);

    void handleStatus(Trade trade, Date to);
}
