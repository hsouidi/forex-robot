package com.trading.forex.common.utils;

import com.trading.forex.common.exceptions.RobotTechnicalException;
import com.trading.forex.common.model.Candle;
import com.trading.forex.common.model.CandlestickGranularity;
import com.trading.forex.common.model.Symbol;
import com.trading.forex.common.model.Way;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.function.Function;

import static com.trading.forex.common.model.Way.BUY;
import static java.time.temporal.ChronoUnit.MINUTES;

/**
 * Created by hsouidi on 12/09/2017.
 */
@Slf4j
public class AlgoUtils {


    private AlgoUtils() {
        throw new AssertionError();
    }


    public static boolean checkDateWithCandlestickGranularity(CandlestickGranularity candlestickGranularity, Date toCheck, Date dateInput) {
        return candlestickGranularityToTimeStamp(candlestickGranularity) >= Math.abs(dateInput.getTime() - toCheck.getTime());
    }

    public static Date getFromDate(Date to, CandlestickGranularity candlestickGranularity) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(to.toInstant(), ZoneId.systemDefault());
        switch (candlestickGranularity) {
            case H4:
                return process(localDateTime, candlestickGranularity.getDateFunction(), 3000);
            case H1:
                return process(localDateTime, candlestickGranularity.getDateFunction(), 3000);
            case M15:
                return process(localDateTime, candlestickGranularity.getDateFunction(), 1000);
            case M30:
                return process(localDateTime, candlestickGranularity.getDateFunction(), 1000);
            case M5:
                return process(localDateTime, candlestickGranularity.getDateFunction(), 4000);
            case M1:
                return process(localDateTime, candlestickGranularity.getDateFunction(), 4000);
            case D:
                return process(localDateTime, candlestickGranularity.getDateFunction(), 3000);
            case M:
                return process(localDateTime, candlestickGranularity.getDateFunction(), 100);
            case W:
                return process(localDateTime, candlestickGranularity.getDateFunction(), 1000);
            default:
                throw new RobotTechnicalException("Cannot found handler for candlestickGranularity " + candlestickGranularity);
        }

    }

    public static Date getFromDate(Date to, CandlestickGranularity candlestickGranularity, int period) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(to.toInstant(), ZoneId.systemDefault());
        return process(localDateTime, candlestickGranularity.getDateFunction(), period);
    }

    public static Date resetToTimeFrame(Date to, CandlestickGranularity candlestickGranularity) {
        Long timestamp = to.getTime() % (candlestickGranularity.getNumberMinutes() * 60);
        return toDate(LocalDateTime.ofInstant(to.toInstant(), ZoneId.systemDefault()).minusSeconds(timestamp));
    }


    public static void check(Candle lastCandle, Date to, String process, int maxDecalage) {
        LocalDateTime localDateTime = toLocalDateTime(to);
        LocalDateTime localDateTimeCandle = toLocalDateTime(lastCandle.date());
        final long nbMinutes = localDateTimeCandle.until(localDateTime, MINUTES);
        if (nbMinutes > maxDecalage) {
            //throw new RuntimeException("decalae !!");
            log.error("[{}] decalage {} {}  {}", process, nbMinutes, localDateTime, localDateTimeCandle);
        }
    }


    public static long candlestickGranularityToTimeStamp(CandlestickGranularity candlestickGranularity) {
        long sixty = 60L;
        long minute = 60L * 1000;
        switch (candlestickGranularity) {
            case H1:
                return sixty * minute;
            case H4:
                return sixty * 4 * minute;
            case M15:
                return 15L * minute;
            case M30:
                return 30L * minute;
            case M5:
                return 5L * minute;
            case M1:
                return 1L * minute;
            case D:
                return 24L * sixty * minute;
            case M:
                return 31L * 24L * sixty * minute;
            case W:
                return 7L * 24L * sixty * minute;
            default:
                throw new RobotTechnicalException("Cannot found handler for candlestickGranularity " + candlestickGranularity);
        }

    }


    public static Double normalize(Double value, Symbol symbol) {
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(symbol.getDecimal(), RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static Date getStartOfDay(Date day) {
        final Date input = day == null ? new Date() : day;
        Calendar cal = Calendar.getInstance();
        cal.setTime(input);
        cal.set(Calendar.HOUR_OF_DAY, cal.getMinimum(Calendar.HOUR_OF_DAY));
        cal.set(Calendar.MINUTE, cal.getMinimum(Calendar.MINUTE));
        cal.set(Calendar.SECOND, cal.getMinimum(Calendar.SECOND));
        cal.set(Calendar.MILLISECOND, cal.getMinimum(Calendar.MILLISECOND));
        return cal.getTime();
    }

    public static Date getStartOfDay(LocalDateTime day) {

        return getStartOfDay(toDate(day));
    }


    public static Date getEndOfDay(Date day) {
        final Date input = day == null ? new Date() : day;
        Calendar cal = Calendar.getInstance();
        cal.setTime(input);
        cal.set(Calendar.HOUR_OF_DAY, cal.getMaximum(Calendar.HOUR_OF_DAY));
        cal.set(Calendar.MINUTE, cal.getMaximum(Calendar.MINUTE));
        cal.set(Calendar.SECOND, cal.getMaximum(Calendar.SECOND));
        cal.set(Calendar.MILLISECOND, cal.getMaximum(Calendar.MILLISECOND));
        return cal.getTime();
    }

    public static double toPip(Symbol symbol, double value) {
        return value * Math.pow(10, symbol.getDecimal() - 1D);
    }

    public static double toQuote(Symbol symbol, double pip) {
        return pip * Math.pow(10, -symbol.getDecimal() + 1D);
    }

    public static Date process(LocalDateTime localDateTime, Function<LocalDateTime, LocalDateTime> function, int count) {

        LocalDateTime dateTime = localDateTime;
        for (int i = 0; i < count; i++) {
            dateTime = function.apply(dateTime);
        }

        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static LocalDateTime convertToLocalDateTime(Date dateToConvert) {
        return dateToConvert.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    public static String getMyPublicIp() {

        try (final BufferedReader in = new BufferedReader(new InputStreamReader(
                new URL("http://checkip.amazonaws.com").openStream()))) {
            return in.readLine(); //you get the IP as a String
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return "GET_IP_TECH-ERROR";
        }
    }

    public static double barreDistance(double currentPrice, double barreIncrement, Way way, Symbol symbol) {
        final double barreDown = currentPrice - currentPrice % barreIncrement;
        final double barreUp = barreDown + barreIncrement;
        return way == BUY ? toPip(symbol, barreUp - currentPrice) : toPip(symbol, currentPrice - barreDown);

    }

    public static Date toDate(final LocalDateTime localDate) {
        return Date.from(localDate.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static LocalDateTime toLocalDateTime(final Date date) {
        return LocalDateTime.ofInstant(date.toInstant(),
                ZoneId.systemDefault());
    }

    public static ZonedDateTime toZonedDateTime(Date date, ZoneId zoneId) {
        return ZonedDateTime.ofInstant(date.toInstant(),
                zoneId);
    }


    public static Date toDate(ZonedDateTime date) {
        return Date.from(date.toInstant());
    }

    public static Date toDate(final LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public static ZonedDateTime previousWorkingDay(ZonedDateTime zonedDateTime) {
        ZonedDateTime tmp = zonedDateTime.minusDays(1);
        while (tmp.getDayOfWeek() == DayOfWeek.SUNDAY || tmp.getDayOfWeek() == DayOfWeek.SATURDAY ||
                (tmp.getDayOfMonth() == 25 && tmp.getMonth() == Month.DECEMBER)
                || (tmp.getDayOfMonth() == 1 && tmp.getMonth() == Month.JANUARY)
                || (tmp.getDayOfMonth() == 10 && tmp.getMonth() == Month.APRIL && tmp.getYear() == 2020)) {
            tmp = tmp.minusDays(1);
        }
        return tmp;
    }

    public static boolean isCrossPrice(Candle candle, Double price) {

        int fact = candle.body() > 0 ? 1 : -1;
        return fact * (price - candle.getOpen()) > 0 && fact * (price - candle.getClose()) < 0;
    }

    public static Double getUnit(Double unit, Way way) {
        return way.getValue() * unit;
    }

    public static Way getPositionWay(final Double shortValue, final Double longValue) {
        return Math.abs(shortValue) > Math.abs(longValue) ? Way.SELL : Way.BUY;
    }


    public static long getDelay(final int seconds) {
        return seconds * 1000;
    }

    public static LocalDateTime truncate(final LocalDateTime localDateTime, int minute) {
        int relica = localDateTime.getMinute() % minute;
        return localDateTime.minusSeconds(localDateTime.getSecond()).minusNanos(localDateTime.getNano()).minusMinutes(relica);
    }

    public static long getDelay(final LocalDateTime target) {
        return (ChronoUnit.SECONDS.between(LocalDateTime.now(), target) + 1) * 1000;
    }


}







