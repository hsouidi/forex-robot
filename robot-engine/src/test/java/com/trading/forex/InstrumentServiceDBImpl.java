package com.trading.forex;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import com.trading.forex.client.*;
import com.trading.forex.common.model.Candle;
import com.trading.forex.common.model.CandlestickGranularity;
import com.trading.forex.common.model.Symbol;
import com.trading.forex.common.utils.AlgoUtils;
import com.trading.forex.common.utils.CustomList;
import com.trading.forex.connector.service.InstrumentService;
import com.trading.forex.entity.CandleEntity;
import com.trading.forex.oanda.service.OandaInstrumentServiceImpl;
import com.trading.forex.repository.CandleHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.trading.forex.CsvUtility.toCandleCsv;
import static com.trading.forex.RobotAppBackTest.callOandaPricing;
import static com.trading.forex.common.utils.AlgoUtils.*;
import static com.trading.forex.common.utils.AlgoUtils.toDate;
import static java.util.Arrays.asList;

/**
 * Created by wf on 10/21/2017.
 */
@Service
@Slf4j
public class InstrumentServiceDBImpl implements InstrumentService {

    private CandleHistoryRepository candleHistoryRepository;

    private OandaInstrumentServiceImpl oandaInstrumentService;

    public Map<CandlestickGranularity, TreeSet<Candle>> candles = new HashMap<>();

    MarketDataClientProxy marketDataClient;

    private PolygonMarketDataClientProxy polygonMarketDataClient;


    @Autowired
    public InstrumentServiceDBImpl(PolygonMarketDataClientProxy polygonMarketDataClient, MarketDataClientProxy marketDataClient, CandleHistoryRepository candleHistoryRepository, OandaInstrumentServiceImpl oandaInstrumentService) throws FileNotFoundException {
        this.candleHistoryRepository = candleHistoryRepository;
        this.oandaInstrumentService = oandaInstrumentService;
        this.marketDataClient = marketDataClient;
        this.polygonMarketDataClient = polygonMarketDataClient;
        Symbol symbol = Symbol.SPX500USD;
/*

        buildCsvMarketDataPolygone(CandlestickGranularity.M1, RobotAppBackTest.start, RobotAppBackTest.end, symbol);
        buildCsvMarketDataPolygone(CandlestickGranularity.H1, RobotAppBackTest.start, RobotAppBackTest.end, symbol);
        buildCsvMarketDataPolygone(CandlestickGranularity.D, RobotAppBackTest.start, RobotAppBackTest.end, symbol);
        symbol.getBrokerValue();*/


        //final List<Candle> cands=convertTo( getCandles(CandlestickGranularity.M1, symbol),CandlestickGranularity.M5);

        if(callOandaPricing) {
            buildCsvMarketData(CandlestickGranularity.M1, RobotAppBackTest.start, RobotAppBackTest.end, symbol);
            buildCsvMarketData(CandlestickGranularity.M5, RobotAppBackTest.start, RobotAppBackTest.end, symbol);
            buildCsvMarketData(CandlestickGranularity.M15, RobotAppBackTest.start, RobotAppBackTest.end, symbol);
            buildCsvMarketData(CandlestickGranularity.H1, RobotAppBackTest.start, RobotAppBackTest.end, symbol);
            buildCsvMarketData(CandlestickGranularity.D, RobotAppBackTest.start, RobotAppBackTest.end, symbol);
        }else{
            String folder = "C:\\marketdata\\";

            List<File> files = TestUtils.listDirectory(folder, 4);
            for (File file : files) {
                pushCandles(file);
            }
        }

/*
        candles.put(CandlestickGranularity.M5, getCandles(CandlestickGranularity.M5, symbol, folder));
        candles.put(CandlestickGranularity.M15, getCandles(CandlestickGranularity.M15, symbol, folder));
        candles.put(CandlestickGranularity.H1, getCandles(CandlestickGranularity.H1, symbol, folder));
        candles.put(CandlestickGranularity.D, getCandles(CandlestickGranularity.D, symbol, folder));*/



/*        candles.put(CandlestickGranularity.D, getCandle(CandlestickGranularity.D, RobotAppBackTest.start, RobotAppBackTest.end, 100));
        candles.put(CandlestickGranularity.M1, getCandle(CandlestickGranularity.M1, RobotAppBackTest.start, RobotAppBackTest.end, 1));
        candles.put(CandlestickGranularity.M5, getCandle(CandlestickGranularity.M5, RobotAppBackTest.start, RobotAppBackTest.end, 12));
        candles.put(CandlestickGranularity.H1, getCandle(CandlestickGranularity.H1, RobotAppBackTest.start, RobotAppBackTest.end, 40));
        candles.put(CandlestickGranularity.M15, getCandle(CandlestickGranularity.M15, RobotAppBackTest.start, RobotAppBackTest.end, 30));*/

    }


    private void buildCsvMarketData(final CandlestickGranularity candlestickGranularity, LocalDate from, LocalDate to, Symbol symbol) {
        final List<CandleCsv> candles = new ArrayList<>();

        Date temp = toDate(to.atStartOfDay());
        LocalDateTime start = from.atStartOfDay();


        while (toLocalDateTime(temp).isAfter(start)) {

            Date fromD = AlgoUtils.getFromDate(temp, candlestickGranularity);
            candles.addAll(toCandleCsv(oandaInstrumentService.getPricing(candlestickGranularity,symbol,temp,fromD)));
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


    private void pushCandles(File file) throws FileNotFoundException {
        Pattern pattern = Pattern.compile("(.*?)_(.*?).csv");
        String fileNme = file.getName();
        Matcher matcher = pattern.matcher(fileNme);
        CandlestickGranularity candlestickGranularity=null;
        Symbol symbol=null;
        if (matcher.find()) {
            symbol=Symbol.valueOf(matcher.group(1));
            candlestickGranularity=CandlestickGranularity.valueOf(matcher.group(2));
        }


        final TreeSet<Candle> result = new TreeSet<>(Comparator.comparing(Candle::getEpoch));
        final InputStream inputStream = new FileInputStream(file);
        CsvToBean<CandleCsv> csvToBean = new CsvToBeanBuilder<CandleCsv>(new InputStreamReader(inputStream))
                .withType(CandleCsv.class)
                .withSeparator(',')
                //.withMappingStrategy(strategy)
                .withIgnoreLeadingWhiteSpace(true)
                .build();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
        for (CandleCsv candleCsv : csvToBean.parse()) {
            try {
                final Date date = simpleDateFormat.parse(candleCsv.getDate() + " " + candleCsv.getTime());
                result.add(Candle.builder()
                        .close(candleCsv.getClose())
                        .open(candleCsv.getOpen())
                        .low(candleCsv.getLow())
                        .high(candleCsv.getHigh())
                        .epoch(date.getTime())
                        .date(date)
                        .volume(candleCsv.getVolume())
                        .symbol(symbol)
                        .complete(true)
                        .build());
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        TreeSet<Candle> value=candles.getOrDefault(candlestickGranularity,new TreeSet<>(Comparator.comparing(Candle::getEpoch)) ) ;
        value.addAll(result);
        candles.put(candlestickGranularity,value);
    }


    @Override
    public CustomList<Candle> getPricing(CandlestickGranularity candlestickGranularity, Symbol symbol, int count) {
        return null;
    }

    private TreeSet<Candle> getCandle(final CandlestickGranularity candlestickGranularity, LocalDate from, LocalDate to, int pageSize, Symbol symbol) {
        final TreeSet<Candle> candles = new TreeSet<>(Comparator.comparing(Candle::date));

        Date temp = toDate(to.atStartOfDay());
        LocalDateTime start = from.atStartOfDay();


        while (toLocalDateTime(temp).isAfter(start)) {

            Date fromD = AlgoUtils.getFromDate(temp, candlestickGranularity);
            candles.addAll(oandaInstrumentService.getPricing(candlestickGranularity, symbol, temp, fromD));
            temp = fromD;

        }

        return candles;
    }

    @Override
    public CustomList<Candle> getPricing(CandlestickGranularity candlestickGranularity, Symbol symbol, Date toB, Date from) {
        //return candles.get(candlestickGranularity).subSet(Candle.builder().epoch(from.getTime()).build(), Candle.builder().epoch(to.getTime()).build()).stream().collect(Collectors.toCollection(CustomList::new));
        Date to= AlgoUtils.resetToTimeFrame(toB,candlestickGranularity);
        if(callOandaPricing){
            return getPricingOld(candlestickGranularity,symbol,to,from);
        }else{
            return candles.get(candlestickGranularity).stream().filter(candle -> candle.date().before(to) && candle.date().after(from))
                    .collect(Collectors.toCollection(CustomList::new));
        }

    }


    //@Override
    public CustomList<Candle> getPricingOld(CandlestickGranularity candlestickGranularity, Symbol symbol, Date to, Date from) {
        // download & Push candles to cache
        CustomList<Candle> candlesFromOanda = oandaInstrumentService.getPricing(candlestickGranularity, symbol, to, from);
        // candleHistoryRepository.saveAll(candlesFromOanda.stream().map(candle1 -> CandleEntity.build(candle1, candlestickGranularity, symbol))
        //     .collect(Collectors.toList()));*/
        TreeSet<Candle> values = candles.getOrDefault(candlestickGranularity, new TreeSet<>(Comparator.comparing(Candle::getEpoch)));
        values.addAll(candlesFromOanda);
        candles.put(candlestickGranularity, values);
        return candlesFromOanda;
    }

    @Override
    public CustomList<Candle> getPricingHeinkin(CandlestickGranularity candlestickGranularity, Symbol symbol, Date to, Date from) {
        CustomList<Candle> candles = getPricing(candlestickGranularity, symbol, to, from);
        final CustomList<Candle> result = new CustomList<>();
        for (int i = 1; i < candles.size(); i++) {
            final Candle currentCandle = candles.get(i);
            final Candle previousCandle = candles.get(i - 1);
            final double close = (currentCandle.getClose() + currentCandle.getOpen() + currentCandle.getHigh() + currentCandle.getLow()) / 4;
            // Open = (Open of Previous Bar + Close of Previous Bar) / 2 o This is the midpoint of the previous bar.
            final double open = (previousCandle.getOpen() + previousCandle.getClose()) / 2;
            // High = Max of (High, Open, Close) o Highest value of the three.
            final double high = NumberUtils.max(currentCandle.getHigh(), currentCandle.getOpen(), currentCandle.getClose());
            // Low = Min of (Low, Open, Close) o Lowest value of the three.
            final double low = NumberUtils.min(currentCandle.getHigh(), currentCandle.getOpen(), currentCandle.getClose());
            result.add(Candle.builder()
                    .epoch(currentCandle.getEpoch())
                    .symbol(currentCandle.getSymbol())
                    .low(low)
                    .high(high)
                    .close(close)
                    .open(open)
                    .volume(currentCandle.getVolume())
                    .complete(true)
                    .build());
        }

        return result;
    }


}
