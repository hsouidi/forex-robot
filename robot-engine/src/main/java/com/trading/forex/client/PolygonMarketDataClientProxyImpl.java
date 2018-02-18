package com.trading.forex.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
public class PolygonMarketDataClientProxyImpl implements PolygonMarketDataClientProxy {


    PolygonMarketDataClient polygonMarketDataClient;

    @Autowired
    public PolygonMarketDataClientProxyImpl(PolygonMarketDataClient polygonMarketDataClient){
        this.polygonMarketDataClient=polygonMarketDataClient;
    }

    @Override
    @Retryable(
            maxAttempts = 30,
            backoff = @Backoff(delay = 60000))
    public PolygonMarketData getMarketDatz(String symbol, String resolution, String from, String to, String token) {
        return polygonMarketDataClient.getMarketDatz(symbol,resolution,from,to,token);
    }
}
