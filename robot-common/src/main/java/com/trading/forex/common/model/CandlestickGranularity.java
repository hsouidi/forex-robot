package com.trading.forex.common.model;

import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.function.Function;

@Getter
public enum CandlestickGranularity implements Serializable {

    S5(localDateTime -> localDateTime.minusSeconds(5)), S10(localDateTime -> localDateTime.minusSeconds(10)), S15(localDateTime -> localDateTime.minusSeconds(15)), S30(localDateTime -> localDateTime.minusSeconds(30))
    , M1(localDateTime -> localDateTime.minusMinutes(1),1), M2(localDateTime -> localDateTime.minusMinutes(2)), M4(localDateTime -> localDateTime.minusMinutes(4)), M5(localDateTime -> localDateTime.minusMinutes(5),5), M10(localDateTime -> localDateTime.minusMinutes(10)), M15(localDateTime -> localDateTime.minusMinutes(15),15), M30(localDateTime -> localDateTime.minusMinutes(30))
    , H1(localDateTime -> localDateTime.minusHours(1),60), H2(localDateTime -> localDateTime.minusHours(2)), H3(localDateTime -> localDateTime.minusHours(3)), H4(localDateTime -> localDateTime.minusHours(4),240), H6(localDateTime -> localDateTime.minusHours(6)), H8(localDateTime -> localDateTime.minusHours(8)), H12(localDateTime -> localDateTime.minusHours(12))
    , D(localDateTime -> localDateTime.minusDays(1),60*24)
    , W(localDateTime -> localDateTime.minusWeeks(1))
    , M(localDateTime -> localDateTime.minusMonths(1));

    Function<LocalDateTime, LocalDateTime> dateFunction;
    Integer numberMinutes;

    CandlestickGranularity(final Function<LocalDateTime, LocalDateTime> localDateTimeFunction) {
        this(localDateTimeFunction,0);
    }

    CandlestickGranularity(final Function<LocalDateTime, LocalDateTime> localDateTimeFunction,Integer numberMinutes) {
        this.dateFunction = localDateTimeFunction;
        this.numberMinutes=numberMinutes;
    }

}
