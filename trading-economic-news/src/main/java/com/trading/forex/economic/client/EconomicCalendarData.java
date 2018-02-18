package com.trading.forex.economic.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class EconomicCalendarData implements Serializable {

    @JsonProperty("market")
    private String market;
    @JsonProperty("actual")
    private Double actual;
    @JsonProperty("region")
    private String region;
    @JsonProperty("currency")
    private String currency;
    @JsonProperty("forecast")
    private Double forecast;
    @JsonProperty("previous")
    private Double previous;
    @JsonProperty("unit")
    private String unit;
    @JsonProperty("timestamp")
    private long timestamp;
    @JsonProperty("title")
    private String title;
    @JsonProperty("impact")
    private Integer impact;

}