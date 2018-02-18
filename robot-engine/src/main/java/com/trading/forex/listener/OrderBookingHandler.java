package com.trading.forex.listener;

import com.trading.forex.model.Trade;

import java.util.Date;

public interface OrderBookingHandler {


    void processTradeProposal(final Trade trade);

    void processTradeProposal(Trade trade, Date to);
}
