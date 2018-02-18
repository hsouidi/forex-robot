package com.trading.forex;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import com.trading.forex.client.MarketData;
import com.trading.forex.client.PolygonCandle;
import com.trading.forex.client.PolygonMarketData;
import com.trading.forex.common.model.Candle;
import com.trading.forex.common.model.CandlestickGranularity;
import com.trading.forex.common.model.Symbol;
import com.trading.forex.common.utils.AlgoUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static com.trading.forex.RobotAppBackTest.callOandaPricing;
import static com.trading.forex.common.utils.AlgoUtils.toDate;
import static com.trading.forex.common.utils.AlgoUtils.toLocalDateTime;

@Slf4j
public class CsvUtility {


    public static void saveCsv(Map<CandlestickGranularity, TreeSet<Candle>> candles) {
        if (!callOandaPricing) {
            return;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");

        final String folder = "C:\\marketdata\\marketdata_" + format.format(candles.get(CandlestickGranularity.D).first().date()) + "_" + format.format(candles.get(CandlestickGranularity.D).last().date()) + "\\SPX500_USD\\";

        candles.entrySet().forEach(entry -> save(entry.getKey(), entry.getValue(), folder));

    }

    public static List<CandleCsv> toCandleCsv(Collection<Candle> inpur) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
        return inpur.stream().map(c -> {
            String[] strDate = simpleDateFormat.format(c.date()).split(" ");
            return CandleCsv.builder()
                    .high(c.getHigh())
                    .low(c.getLow())
                    .close(c.getClose())
                    .open(c.getOpen())
                    .time(strDate[1])
                    .date(strDate[0])
                    .volume(c.getVolume())
                    .build();
        }).collect(Collectors.toList());

    }

    private static void save(CandlestickGranularity candlestickGranularity, TreeSet<Candle> candles, String fi) {
        Writer writer = null;
        Symbol symbol = candles.first().getSymbol();
        final List<CandleCsv> candleCsvs = candles.stream().map(
                c -> {
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");
                    simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
                    String[] strDate = simpleDateFormat.format(c.date()).split(" ");
                    return CandleCsv.builder()
                            .high(c.getHigh())
                            .low(c.getLow())
                            .close(c.getClose())
                            .open(c.getOpen())
                            .time(strDate[1])
                            .date(strDate[0])
                            .volume(c.getVolume())
                            .build();
                }

        ).collect(Collectors.toList());
        try {
            Path path = Paths.get(fi);

            if (!Files.exists(path)) {
                File file = new File(fi);
                file.mkdirs();
            }

            writer = new FileWriter(fi + symbol.name() + "_" + candlestickGranularity.name() + ".csv");
            StatefulBeanToCsv beanToCsv = new StatefulBeanToCsvBuilder(writer)
                    .withApplyQuotesToAll(false)
                    .build();
            beanToCsv.write(candleCsvs);
            writer.close();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException();
        }
    }


    private List<Candle> convertTo(TreeSet<Candle> candlesM1, CandlestickGranularity candlestickGranularity) {

        final List<Candle> result = new ArrayList<>();
        LocalDateTime startCandle = toLocalDateTime(candlesM1.first().date());
        LocalDateTime endCandle = startCandle.plusMinutes(5);

        while (toLocalDateTime(toDate(RobotAppBackTest.end)).isAfter(endCandle)) {

            final LocalDateTime start = startCandle;
            final LocalDateTime end = endCandle;
            //SortedSet<Candle> sub= candlesM1.subSet(Candle.builder().epoch(toDate(startCandle).getTime()).build(), true,Candle.builder().epoch(toDate(endCandle).getTime()).build(),false);
            List<Candle> sub = candlesM1.stream().filter(c -> c.getEpoch() >= toDate(start).getTime() && c.getEpoch() < toDate(end).getTime()).collect(Collectors.toList());
            if (!sub.isEmpty()) {
                result.add(Candle.builder()
                        .symbol(sub.get(0).getSymbol())
                        .volume(sub.stream().mapToDouble(Candle::getVolume).sum())
                        .low(sub.stream().mapToDouble(Candle::getLow).min().getAsDouble())
                        .high(sub.stream().mapToDouble(Candle::getHigh).max().getAsDouble())
                        .open(sub.get(0).getOpen())
                        .close(sub.get(sub.size() - 1).getClose())
                        .date(toDate(startCandle))
                        .epoch(toDate(startCandle).getTime())
                        .build());
            }
            startCandle = endCandle;
            endCandle = startCandle.plusMinutes(5);


        }
        return result;

    }

    private void buildCsvMarketDataPolygone(final CandlestickGranularity candlestickGranularity, LocalDate from, LocalDate to, Symbol symbol) {
        TreeSet<CandleCsv> candles = new TreeSet<>(Comparator.comparing(CandleCsv::epouch));


        String resolution = null;
        switch (candlestickGranularity) {
            case M1:
                resolution = "minute";
                break;
            case H1:
                resolution = "hour";
                break;
            case D:
                resolution = "day";
                break;
            case W:
                resolution = "week";
                break;
            case M:
                resolution = "month";
                break;
            default:
                throw new RuntimeException();
        }


        Date temp = toDate(to.atStartOfDay());
        LocalDateTime start = from.atStartOfDay();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        int index = 1;
        int part = 1;
        while (toLocalDateTime(temp).isAfter(start)) {

            Date fromD = toDate(toLocalDateTime(temp).minusDays(5l));
            //candles.addAll(getCandles(polygonMarketDataClient.getMarketDatz("SPY", resolution, simpleDateFormat.format(fromD), simpleDateFormat.format(temp), "VAPxP_fCj1PukBfHmBBVty_M_e39lzy4ZD_PMH")));
            temp = fromD;

            if (index % 1000 == 0) {
                saveCsv(symbol, candlestickGranularity, candles, part++);
                candles = new TreeSet<>(Comparator.comparing(CandleCsv::epouch));
            }
            index++;

        }
        //saveCsv(symbol,candlestickGranularity,candles,1);


        if (!candles.isEmpty()) {
            saveCsv(symbol, candlestickGranularity, candles, part++);
        }
    }

    private void saveCsv(final Symbol symbol, CandlestickGranularity candlestickGranularity, TreeSet<CandleCsv> candles, int part) {
        Writer writer = null;
        try {
            writer = new FileWriter(symbol.name() + "_" + candlestickGranularity.name() + "_" + part + ".csv");
            StatefulBeanToCsv beanToCsv = new StatefulBeanToCsvBuilder(writer)
                    .withApplyQuotesToAll(false)
                    .build();
            beanToCsv.write(new ArrayList(candles));
            writer.close();
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    private void buildCsvMarketData(final CandlestickGranularity candlestickGranularity, LocalDate from, LocalDate to, Symbol symbol) {
        final List<CandleCsv> candles = new ArrayList<>();

        Date temp = toDate(to.atStartOfDay());
        LocalDateTime start = from.atStartOfDay();


        while (toLocalDateTime(temp).isAfter(start)) {

            Date fromD = AlgoUtils.getFromDate(temp, candlestickGranularity);
            //candles.addAll(getCandles(marketDataClient.getMarketDatz("ICMTRADER:107", candlestickGranularity.getNumberMinutes(), fromD.getTime() / 1000, temp.getTime() / 1000, "bq92t07rh5rc96c0m7eg")));
            temp = fromD;

        }
        Writer writer = null;
        try {
            writer = new FileWriter(symbol.name() + "_" + candlestickGranularity.name() + ".csv");
            StatefulBeanToCsv beanToCsv = new StatefulBeanToCsvBuilder(writer)
                    .withApplyQuotesToAll(false)
                    .build();
            beanToCsv.write(candles);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CsvDataTypeMismatchException e) {
            e.printStackTrace();
        } catch (CsvRequiredFieldEmptyException e) {
            e.printStackTrace();
        }
    }

    private TreeSet<CandleCsv> getCandles(PolygonMarketData marketData) {

        final TreeSet<CandleCsv> result = new TreeSet<>(Comparator.comparing(CandleCsv::epouch));

        int i = 0;
        List<PolygonCandle> results = marketData.getResults();
        if (null == results) {
            return result;
        }
        for (PolygonCandle polygonCandle : results) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
            String[] strDate = simpleDateFormat.format(polygonCandle.getT()).split(" ");
            result.add(CandleCsv.builder()
                    .close(polygonCandle.getC() * 10)
                    .high(polygonCandle.getH() * 10)
                    .low(polygonCandle.getL() * 10)
                    .open(polygonCandle.getO() * 10)
                    .time(strDate[1])
                    .date(strDate[0])
                    .volume(polygonCandle.getV())
                    .build());
            i++;
        }

        return result;
    }

    private List<CandleCsv> getCandles(MarketData marketData) {

        List<CandleCsv> result = new ArrayList<>();

        int i = 0;
        List<Double> closes = marketData.getC();
        if (null == closes) {
            return result;
        }
        for (Double close : closes) {
            Date date = new Date(marketData.getT().get(i) * 1000);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
            String[] strDate = simpleDateFormat.format(date).split(" ");
            result.add(CandleCsv.builder()
                    .close(close)
                    .high(marketData.getH().get(i))
                    .low(marketData.getL().get(i))
                    .open(marketData.getO().get(i))
                    .time(strDate[1])
                    .date(strDate[0])
                    .volume(marketData.getV().get(i))
                    .build());
            i++;
        }

        return result;
    }
}
