package com.trading.forex.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

public interface MarketDataClientProxy{

    MarketData getMarketDatz(String symbol, String resolution, long from, long to, String token) ;
}
