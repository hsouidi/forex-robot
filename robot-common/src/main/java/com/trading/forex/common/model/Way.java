package com.trading.forex.common.model;

import lombok.Getter;

/**
 * Created by hsouidi on 05/08/2017.
 */
@Getter
public enum Way {
    SELL(-1), BUY(1), NEUTRE(0);
    int value;


    Way(int value) {
        this.value = value;
    }

    public static Way safeValueOf(final String name) {
        try {

            return valueOf(name.toUpperCase());

        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public Way inverse() {
        return this.equals(SELL) ? BUY : SELL;
    }
}
