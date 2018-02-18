package com.trading.forex.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
public class MarketDataClientProxyimpl implements  MarketDataClientProxy {

    MarketDataClient marketDataClient;

    @Autowired
    public MarketDataClientProxyimpl(MarketDataClient marketDataClient){
        this.marketDataClient=marketDataClient;
    }

    @Override
    @Retryable(
            maxAttempts = 30,
            backoff = @Backoff(delay = 60000))
    public MarketData getMarketDatz(String symbol, String resolution, long from, long to, String token) {
        return marketDataClient.getMarketDatz(symbol,resolution,from,to,token);
    }
}
