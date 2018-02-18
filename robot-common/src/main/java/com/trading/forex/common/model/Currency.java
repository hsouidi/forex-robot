package com.trading.forex.common.model;

/**
 * Created by hsouidi on 11/19/2017.
 */
public enum  Currency {

    EUR,USD,JPY,NZD,AUD,GBP,CHF,CAD,CNY,MXN;

    public static Currency  fromValue(String value){
        for(Currency currency:Currency.values()){
            if(value.trim().equals(currency.name())){
                return  currency;
            }
        }
        return null;
    }

}
