package com.trading.forex.common.model;

import com.trading.forex.common.exceptions.RobotTechnicalException;
import lombok.Getter;

/**
 * Created by hsouidi on 11/19/2017.
 */
@Getter
public enum Importance {

    HIGH("high", 3), MEDIUM("medium", 2), LOW("low", 1), NA("#n/a", 0);

    private String value;
    private Integer valueInt;

    Importance(String value, Integer valueInt) {
        this.value = value;
        this.valueInt = valueInt;
    }

    public static Importance fromValue(String value) {
        for (Importance importance : Importance.values()) {
            if (value.trim().equals(importance.getValue())) {
                return importance;
            }
        }
        throw new RobotTechnicalException("Cannot found enum for  " + value);
    }

    public static Importance fromValue(Integer value) {
        if(value==null){
            return Importance.NA;
        }
        for (Importance importance : Importance.values()) {
            if (value.equals(importance.getValueInt())) {
                return importance;
            }
        }
        throw new RobotTechnicalException("Cannot found enum for  " + value);
    }

}
