package com.trading.forex.common.model;

import com.trading.forex.common.exceptions.RobotTechnicalException;
import lombok.Getter;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by hsouidi on 04/23/2017.
 */
@Getter
public enum Symbol implements Serializable {

    EUR_USD("EURUSD", "EUR_USD", "eur-usd", 0, 5, false),
    EUR_JPY("EURJPY", "EUR_JPY", "eur-jpy", 1, 3, false),
    EUR_GBP("EURGBP", "EUR_GBP", "eur-gbp", 2, 5, false),
    EUR_CHF("EURCHF", "EUR_CHF", "eur-chf", 3, 5, false),
    USD_JPY("USDJPY", "USD_JPY", "usd-jpy", 4, 3, false),
    USD_CHF("USDCHF", "USD_CHF", "usd-chf", 5, 5, false),
    GBP_USD("GBPUSD", "GBP_USD", "gbp-usd", 6, 5, false), // a eviter trop volatille
    GBP_JPY("GBPJPY", "GBP_JPY", "gbp-jpy", 7, 3, false),
    GBP_CHF("GBPCHF", "GBP_CHF", "gbp-chf", 8, 5, false),
    AUD_USD("AUDUSD", "AUD_USD", "aud-usd", 9, 5, false),
    USD_CAD("USDCAD", "USD_CAD", "usd-cad", 10, 5, false),
    AUD_JPY("AUDJPY", "AUD_JPY", "aud-jpy", 11, 3, false),
    NZD_USD("NZDUSD", "NZD_USD", "nzd-usd", 12, 5, false),
    AUD_CAD("AUDCAD", "AUD_CAD", "aud-cad", 13, 5, false),
    AUD_CHF("AUDCHF", "AUD_CHF", "aud-chf", 14, 5, false),
    EUR_AUD("EURAUD", "EUR_AUD", "eur-aud", 15, 5, false),
    SUGAR_USD("SUGARUSD", "SUGARUSD", "", 16, 5, false),
    DE30EUR("DE30EUR", "DE30_EUR", "", 17, 1, false),
    FR40EUR("FR40EUR", "FR40_EUR", "", 18, 1, false),
    SPX500USD("SPX500USD", "SPX500_USD", "", 19, 1, true);


    private String indicatorValue;
    private String brokerValue;
    private String investingValue;
    private int value;
    public int decimal;
    private boolean activated;

    public static final List<Symbol> activatedSymbol;
    public static final List<Symbol> majorsLiquidSymbol;
    public static final List<Symbol> majorsComoditiySymbol;


    static {
        activatedSymbol = Stream.of(Symbol.values()).filter(s -> s.isActivated()).collect(Collectors.toList());
        majorsLiquidSymbol = Arrays.asList(EUR_USD, USD_JPY, EUR_JPY, EUR_CHF, USD_CHF);
        majorsComoditiySymbol = Arrays.asList(AUD_USD, USD_CAD, NZD_USD);
    }

    Symbol(String indicatorValue, String brokerValue, String investingValue, int value, int decimal, boolean activated) {
        this.brokerValue = brokerValue;
        this.indicatorValue = indicatorValue;
        this.investingValue = investingValue;
        this.value = value;
        this.decimal = decimal;
        this.activated = activated;

    }

    public static List<Symbol> fromCurrency(Currency currency) {
        return activatedSymbol.stream().filter(e -> e.name().contains(currency.name())).collect(Collectors.toList());
    }


    public static Symbol fromInvestingValue(String investingValue) {
        for (Symbol symbol : Symbol.values()) {
            if (symbol.getInvestingValue().equals(investingValue)) {
                return symbol;
            }
        }
        throw new RobotTechnicalException("cannot found investingValue" + investingValue);
    }

    public static Symbol fromBrokerValue(String brokerValue) {
        for (Symbol symbol : Symbol.values()) {
            if (symbol.getBrokerValue().equalsIgnoreCase(brokerValue)) {
                return symbol;
            }
        }
        throw new RobotTechnicalException("cannot found brokerValue" + brokerValue);
    }

    public static Symbol fromIndicatorValue(String indicatorValue) {
        for (Symbol symbol : Symbol.values()) {
            if (symbol.getIndicatorValue().equalsIgnoreCase(indicatorValue)) {
                return symbol;
            }
        }
        throw new RobotTechnicalException("cannot found indicatorValue" + indicatorValue);
    }

    public static List<Symbol> getActivatedSymbol() {
        return activatedSymbol;
    }
}
