package com.trading.forex.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value="polygon-market-data",url = "https://api.polygon.io/v2")
public interface PolygonMarketDataClient {

    @RequestMapping(value = "/aggs/ticker/{symbol}/range/1/{resolution}/{from}/{to}", method = RequestMethod.GET)
    PolygonMarketData getMarketDatz(@PathVariable(value = "symbol", required = true) final String symbol,
                             @PathVariable(value = "resolution", required = false) final String resolution,
                             @PathVariable(value = "from", required = false) final String from,
                             @PathVariable(value = "to", required = false) final String to,
                             @RequestParam(value = "apiKey", required = false) final String token);

}
