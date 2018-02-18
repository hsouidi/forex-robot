package com.trading.forex.model;

import com.trading.forex.common.model.Candle;
import com.trading.forex.common.model.Way;
import com.trading.forex.common.utils.CustomList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by hsouidi on 12/15/2017.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FibonacciResult {

    private static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm");


    private Way way;
    private double ret_0;
    private double ret_23_6;
    private double ret_38_2;
    private double ret_50;
    private double ret_61_8;
    private double ret_76_4;
    private double ret_100;
    private double ret_161_8;
    private double ret_261_8;
    private double ret_423_6;
    private double ext_23_6;
    private double ext_38_2;
    private double ext_261_8;
    private double ext_200;
    private double ext_161_8;
    private double ext_138_2;
    private double ext_100;
    private double ext_61_8;
    private double ext_423_6;
    private Candle indexHigh;
    private Candle indexLow;

    public Double getNextBarrier(double price, Way wayTrade) {
        List<Double> barries = Arrays.asList(
                ret_0,
                ret_23_6,
                ret_38_2,
                ret_50,
                ret_61_8,
                //ret_76_4,
                ret_100,
                ret_161_8,
                ret_261_8,
                ret_423_6);

        if (wayTrade == Way.BUY) {
            return barries.stream().filter(p -> p - price >= 0).findFirst().orElse(null);
        } else {
            inverse(barries);
            return barries.stream().filter(p -> p - price <= 0).findFirst().orElse(null);
        }
    }

    private void inverse(List input) {
        int size = input.size();
        int fromEndIndex = size - 1;
        for (int j = 0; fromEndIndex > j; fromEndIndex--, j++) {
            Object tmp = input.get(fromEndIndex);
            input.set(fromEndIndex, input.get(j));
            input.set(j, tmp);
        }
    }

    public Double getPreviousBarrier(double price, Way wayTrade) {
        return getPreviousBarrier(price, wayTrade, false);
    }


    public Double getPreviousBarrier(double price, Way wayTrade, boolean beforePrice) {
        List<Double> barries = Arrays.asList(
                ret_0,
                ret_23_6,
                ret_38_2,
                ret_50,
                ret_61_8,
                //ret_76_4,
                ret_100,
                ret_161_8,
                ret_261_8,
                ret_423_6
        );

        if (wayTrade == Way.BUY) {
            return barries.stream().filter(p -> p - price <= 0).findFirst().orElse(null);
        } else {
            inverse(barries);
            return barries.stream().filter(p -> p - price >= 0).findFirst().orElse(null);
        }
    }


    public CustomList<Placement> getPlacementHistory(List<Candle> candles) {
        List<Map.Entry<FiboLevel, Double>> barries = Arrays.asList(
                new AbstractMap.SimpleEntry(FiboLevel.ret_0, ret_0),
                new AbstractMap.SimpleEntry(FiboLevel.ret_23_6, ret_23_6),
                new AbstractMap.SimpleEntry(FiboLevel.ret_38_2, ret_38_2),
                new AbstractMap.SimpleEntry(FiboLevel.ret_50, ret_50),
                new AbstractMap.SimpleEntry(FiboLevel.ret_61_8, ret_61_8),
                //ret_76_4,
                new AbstractMap.SimpleEntry(FiboLevel.ret_100, ret_100),
                new AbstractMap.SimpleEntry(FiboLevel.ret_161_8, ret_161_8),
                new AbstractMap.SimpleEntry(FiboLevel.ret_261_8, ret_261_8),
                new AbstractMap.SimpleEntry(FiboLevel.ret_423_6, ret_423_6),
                // ext
                new AbstractMap.SimpleEntry(FiboLevel.ext_423_6, ext_423_6),
                new AbstractMap.SimpleEntry(FiboLevel.ext_261_8, ext_261_8),
                new AbstractMap.SimpleEntry(FiboLevel.ext_200, ext_200),
                new AbstractMap.SimpleEntry(FiboLevel.ext_161_8, ext_161_8),
                new AbstractMap.SimpleEntry(FiboLevel.ext_138_2, ext_138_2),
                new AbstractMap.SimpleEntry(FiboLevel.ext_100, ext_100),
                new AbstractMap.SimpleEntry(FiboLevel.ext_61_8, ext_61_8),
                new AbstractMap.SimpleEntry(FiboLevel.ext_23_6, ext_23_6),
                new AbstractMap.SimpleEntry(FiboLevel.ext_38_2, ext_38_2)
        );

        CustomList<Placement> result = new CustomList<>();

        for (Candle candle : candles) {

            Placement placement = result.getLast();
            Map.Entry<FiboLevel, Double> above = barries.stream().filter(p -> p.getValue() > candle.getClose()).min(Comparator.comparing(Map.Entry::getValue)).get();
            Map.Entry<FiboLevel, Double> bellow = barries.stream().filter(p -> p.getValue() < candle.getClose()).max(Comparator.comparing(Map.Entry::getValue)).get();
            if (placement != null && placement.above.equals(above) && placement.bellow.equals(bellow)) {
                placement.setWeight(placement.weight + 1);
            } else {
                result.add(new Placement(1, above, bellow));
            }
        }
        return result;


    }

    @Data
    @AllArgsConstructor
    @Builder
    @NoArgsConstructor
    public static class Placement {
        int weight;
        Map.Entry<FiboLevel, Double> above;
        Map.Entry<FiboLevel, Double> bellow;
    }

    enum FiboLevel {
        ret_0,
        ret_23_6,
        ret_38_2,
        ret_50,
        ret_61_8,
        ret_100,
        ret_161_8,
        ret_261_8,
        ret_423_6,
        ext_261_8,
        ext_200,
        ext_161_8,
        ext_138_2,
        ext_100,
        ext_61_8,
        ext_423_6,
        ext_23_6,
        ext_38_2
    }


}
