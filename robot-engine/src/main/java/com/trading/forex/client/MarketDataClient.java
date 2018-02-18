package com.trading.forex.client;


import com.trading.forex.connector.exceptions.ConnectorTechnicalException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value="market-data",url = "https://finnhub.io")
public interface MarketDataClient {

    @RequestMapping(value = "/api/v1/stock/candle", method = RequestMethod.GET)
    MarketData getMarketDatz(@RequestParam(value = "symbol", required = true) final String symbol,
                               @RequestParam(value = "resolution", required = false) final String resolution,
                               @RequestParam(value = "from", required = false) final long from,
                               @RequestParam(value = "to", required = false) final long to,
                               @RequestParam(value = "token", required = false) final String token);

}
