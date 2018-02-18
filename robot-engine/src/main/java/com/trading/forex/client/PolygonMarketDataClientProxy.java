package com.trading.forex.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

public interface PolygonMarketDataClientProxy {

    PolygonMarketData getMarketDatz(final String symbol,
                                    final String resolution,
                                    final String from,
                                    final String to,
                                    final String token);

}
