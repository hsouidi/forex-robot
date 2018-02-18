package com.trading.forex;

import com.google.common.util.concurrent.AtomicDouble;
import com.google.gson.Gson;
import com.opencsv.CSVReader;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.trading.forex.client.EconomicNewsClient;
import com.trading.forex.client.MarketDataClient;
import com.trading.forex.common.model.Candle;
import com.trading.forex.common.model.CandlestickGranularity;
import com.trading.forex.common.model.Symbol;
import com.trading.forex.common.model.Way;
import com.trading.forex.common.utils.CustomList;
import com.trading.forex.configuration.*;
import com.trading.forex.connector.model.Position;
import com.trading.forex.connector.service.InstrumentService;
import com.trading.forex.connector.service.PositionService;
import com.trading.forex.entity.TradeHistoryEntity;
import com.trading.forex.listener.OrderBookingHandler;
import com.trading.forex.listener.OrderStatusHandler;
import com.trading.forex.model.Trade;
import com.trading.forex.oanda.service.OandaInstrumentServiceImpl;
import com.trading.forex.repository.TradeHistoryRepository;
import com.trading.forex.service.TechnicalAnalysisService;
import com.trading.forex.service.impl.BalanceServiceImpl;
import com.trading.forex.strategies.Strategy;
import com.trading.forex.strategies.executors.StategyExecutorService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.retry.annotation.EnableRetry;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.trading.forex.CsvUtility.saveCsv;
import static com.trading.forex.common.utils.AlgoUtils.getFromDate;
import static com.trading.forex.common.utils.AlgoUtils.toDate;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;

/**
 * Created by hsouidi on 10/19/2017.
 */
@SpringBootApplication
@EnableFeignClients
@EnableRetry
@Slf4j
@Import(IntegrationTestConfiguration.class)
@EnableAutoConfiguration(exclude = {RabbitAutoConfiguration.class})
@ComponentScan(basePackages = {"com.trading.forex.strategies", "com.trading.forex.oanda"}
        , excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = {RobotApp.class, EsConfig.class,
                BalanceServiceImpl.class
                , SecurityConfig.class, AuthorizationServerConfig.class, ResourceServerConfig.class})})
public class RobotAppBackTest {

    private static final String TIME_ZONE = "Europe/Moscow";
    //private static final String TIME_ZONE = "Europe/Paris";
    private static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm");
    private static SimpleDateFormat DATE_WT_FORMAT = new SimpleDateFormat("yyyy/MM/dd");
    private static ConfigurableApplicationContext application = null;
    private static final String PATH_FILE = "./backtest.log";
    private static Double SPREAD = 1D;
    private static boolean stopPositionEndOfDay;
    private static int TIMEFRAME;
    private static int CHECK_STATUS;
    private static LocalTime startOfDate;
    private static LocalTime endOfDay;
    private static EconomicNewsClient economicNewsClient = null;
    static InstrumentService instrumentService;
    static InstrumentServiceDBImpl instrumentServiceDB;
    static PositionService positionService;
    static Strategy strategy;
    static Map<Symbol, Trade> openedTrades;
    static Map<Symbol, Trade> sessionProposals ;
    static Map<Symbol, Position> openedPosition ;
    static List<RobotAppBackTest.Result> result ;
    static LocalDateTime lastDate;
    public static LocalDate start = LocalDate.of(2016, 03, 27);
    public static LocalDate end = LocalDate.of(2018, 05, 1);
    public static boolean callOandaPricing = false;
    public static List<LocalDate> excludeDates = Arrays.asList(
/*            LocalDate.of(2020, 02, 4),
            LocalDate.of(2020, 02, 5),
            LocalDate.of(2020, 02, 6),
            LocalDate.of(2020, 02, 7),
            LocalDate.of(2020, 02, 10),
            LocalDate.of(2020, 02, 11),
            LocalDate.of(2020, 02, 12),
            LocalDate.of(2020, 02, 13),
            LocalDate.of(2020, 02, 17),
            LocalDate.of(2020, 02, 18),
            LocalDate.of(2020, 02, 19),
            LocalDate.of(2020, 04, 10),
            LocalDate.of(2020, 03, 18),
            LocalDate.of(2020, 03, 23)*/
            LocalDate.of(2020, 04, 10),
            LocalDate.of(2020, 01, 1)
            , LocalDate.of(2019, 12, 25)
    );

    public static void main(String[] args) throws IOException {
        //TimeZone.setDefault(TimeZone.getTimeZone("Europe/Moscow"));
        final SpringApplication springApplication = new SpringApplication(RobotAppBackTest.class);
        springApplication.setAdditionalProfiles("integration");
        application = springApplication.run(args);

        economicNewsClient = application.getBean(EconomicNewsClient.class);
        instrumentService = application.getBean(OandaInstrumentServiceImpl.class);
        positionService = (PositionService) application.getBean("positionService");
        instrumentServiceDB = application.getBean(InstrumentServiceDBImpl.class);
        strategy = application.getBean(Strategy.class);

        TIMEFRAME = strategy.getAnalysTimeFrame().getNumberMinutes();
        CHECK_STATUS = strategy.getCheckStatusTimeFrame().getNumberMinutes();
        final Environment environment = application.getBean(Environment.class);
        startOfDate = LocalTime.parse(environment.getProperty("robot.startOfDay"));
        stopPositionEndOfDay = strategy.stopAtEndOfDate();
        endOfDay = LocalTime.parse(environment.getProperty("robot.endOfDay"));
        final Long start = System.currentTimeMillis();
        //List<DayResult> globalResult = backTestYear(2017);
       //List<DayResult> globalResult = backTestMonth(2020, 4);
        //List<DayResult> globalResult = backTestMonth(2020, 5);
      //List<DayResult> globalResult = asList(backTestExecDay(2020, 6, 12));
      List<DayResult> globalResult = asList(backTestExecDay(2020, 3, 16));
      // List<DayResult> globalResult = asList(backTestExecWeek(2020,5,31));
       //List<DayResult> globalResult = asList(backTestExecWeek(2020,6,7));
        //List<DayResult> globalResult = asList(backTestExecWeek(2020,5,10));
        //List<DayResult> globalResult = asList(backTestExecCustom(LocalDate.of(2020,5,1),LocalDate.of(2020,5,15)));
       // List<DayResult> globalResult = asList(backTestExecCustom(LocalDate.of(2020,4,1),LocalDate.of(2020,5,15)));
        //List<DayResult> globalResult = asList(backTestExecCustom(LocalDate.of(2020,4,7),LocalDate.of(2020,4,29)));
        // List<DayResult> globalResult = asList(backTestExecCustom(LocalDate.of(2020,1,1),LocalDate.of(2020,4,17)));
        //List<DayResult> globalResult = asList(backTestExecCustom(LocalDate.of(2019, 10, 10), LocalDate.of(2020, 5, 29)));

        closePositionEOD(result,lastDate);

        double sum = 0;
        double nbsum = 0;
        int nbok = 0;
        for (DayResult dayResult : globalResult) {
            final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            final String dateEco = dateTimeFormatter.format(dayResult.localDateTime);
            //List<EconomicCalendarData> economicCalendarData = economicNewsClient.economicCalendar(dateEco, Importance.HIGH).getEconomicCalendars();
            dayResult.results.sort(Comparator.comparing(Result::getBegin));
            List<Result> result = dayResult.results;
            result.stream().forEach(result1 -> result1.setResult(result1.getResult() - SPREAD));
            log.info("\n\n Simulation Total:" + result.size());
            log.info("Simulation Result:" + result.stream().mapToDouble(Result::getResult).sum());
            log.info("Simulation Percentages Position Win:" + (new Double(result.stream().filter(result1 -> result1.getResult().compareTo(Double.MIN_VALUE) > 0).count()) / new Double(result.size())) * 100D + "%");

/*            economicCalendarData.stream().forEach(dayRslt ->
                    log.info(new StringBuilder()
                            .append("Currency=").append(dayRslt.getCurrency())
                            .append(", Date=").append(DATE_FORMAT.format(dayRslt.getEventDate()))
                            .append(", Event=").append(dayRslt.getEvent())
                            .append(", Importance=").append(dayRslt.getImportance())
                            .append(", Previous=").append(dayRslt.getPrevious())
                            .append(", Actual=").append(dayRslt.getActual())
                            .append(", Forecast=").append(dayRslt.getForecast())

                            .toString())
            );*/


            for (Result res : result) {
                final double tradeResult = res.getResult();
                if (res.end == null) {
                    log.info("End date null !!!  {}", res);
                    continue;
                }
                sum += res.getResult();
                if (res.getResult() > 0) {
                    nbok++;
                }
                nbsum++;
                log.info("Result: Symbol: " + res.getSymbol() + " status: " + res.status() + " Pip=" + tradeResult + " ,Way =" + res.getWay() + " ,Begin =" + dateToString(res.begin) + " End=" + dateToString(res.end) + " maxLoss={}  , maxProfit={} , risque={} " + (StringUtils.isNotBlank(res.getComment()) ? " , Comment = " + res.getComment() : ""), res.maxLoss, res.maxProfit, res.risque);
            }
            //printRealTradeHistories(dayResult.localDateTime.getYear(), dayResult.localDateTime.getMonth().getValue(), dayResult.localDateTime.getDayOfMonth());
        }
        saveCsv(instrumentServiceDB.candles);

        log.info("Result ALL WEEK: PIP: {}  for total trade= {}  porcentage {}%", sum, nbsum, (nbok / nbsum) * 100);

        final Map<String, Double> previous = getPreviousBackTest();
        final Map<String, Double> current = globalResult.stream().filter(p -> !p.results.isEmpty())
                .collect(Collectors.toMap(p -> DATE_WT_FORMAT.format(p.results.get(0).getBegin()), result -> result.results.stream().mapToDouble(result1 -> result1.getResult()).sum(), (o1, o2) -> o1, TreeMap::new));
        saveCharts(current);
        final String resultCheck = current.entrySet().stream().map((k) -> {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("\n").toString();
                    stringBuilder.append(k.getKey() + "=");

                    if (!previous.containsKey(k.getKey()) || previous.get(k.getKey()).equals(k.getValue())) {
                        stringBuilder.append("EQ");
                    } else if (previous.get(k.getKey()) > k.getValue()) {
                        stringBuilder.append("DOWN");

                    } else if (previous.get(k.getKey()) < k.getValue()) {
                        stringBuilder.append("UP");

                    }
                    stringBuilder.append(" ( new =" + k.getValue() + " , old=" + previous.get(k.getKey()) + ") ");
                    return stringBuilder.append("\n").toString();
                }

        ).collect(Collectors.joining(""));
        log.info(resultCheck);
        previous.putAll(current);
        Files.write(Paths.get(PATH_FILE), new Gson().toJson(previous).getBytes());
        log.info("Batch Test duration {} minutes ", (System.currentTimeMillis() - start) / 60000);
        System.exit(0);

    }

    private static Map<String, Double> getPreviousBackTest() throws IOException {
        if (!Files.exists(Paths.get(PATH_FILE))) {
            return new HashMap<>();
        } else {
            return new Gson().fromJson(new String(Files.readAllBytes(Paths.get(PATH_FILE))), Map.class);
        }

    }

    private static String dateToString(final Date date) {
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat();
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone(TIME_ZONE));
        return simpleDateFormat.format(date);
    }

    public static List<LocalDateTime> allDates(int year, int month, int day) {
        LocalDateTime dateTime = stopPositionEndOfDay? LocalDateTime.of(year, month, day, startOfDate.getHour(), startOfDate.getMinute()): LocalDateTime.of(year, month, day,0, 0);
        LocalDateTime endDateTime = stopPositionEndOfDay? LocalDateTime.of(year, month, day, endOfDay.getHour(), endOfDay.getMinute()): LocalDateTime.of(year, month, day,23, 59);
        List<LocalDateTime> localDateTimes = new ArrayList<>();
        for (; !dateTime.isAfter(endDateTime.minusMinutes(TIMEFRAME)); dateTime = dateTime.plusMinutes(TIMEFRAME)) {
            localDateTimes.add(dateTime);
        }

        return localDateTimes;
    }

    public static List<LocalDateTime> allDatesForcheckStatus(final LocalDateTime localDateTime) {
        final LocalDateTime limitDate = localDateTime.plusMinutes(TIMEFRAME);
        List<LocalDateTime> localDateTimes = new ArrayList<>();
        LocalDateTime dateTime = localDateTime.plusSeconds(0L);
        for (; !dateTime.isAfter(limitDate); dateTime = dateTime.plusMinutes(CHECK_STATUS)) {
            localDateTimes.add(dateTime);
        }

        return localDateTimes;
    }

    private static void printRealTradeHistories(int year, int month, int day) {
        List<LocalDateTime> localDateTimes = allDates(year, month, day);
        final Date begin = Date.from(localDateTimes.get(0).atZone(ZoneId.systemDefault()).toInstant());
        final Date end = Date.from(localDateTimes.get(localDateTimes.size() - 1).atZone(ZoneId.systemDefault()).toInstant());
        List<TradeHistoryEntity> tradeHistories = application.getBean(TradeHistoryRepository.class).findByTradeDateBetween(begin, end).stream().filter(p -> p.getPip() != null).collect(Collectors.toList());
        tradeHistories.sort(Comparator.comparing(TradeHistoryEntity::getTradeDate));
        log.info("Real : Total Pip " + tradeHistories.parallelStream().mapToDouble(p -> p.getPip()).sum());
        log.info("Real : Percentages Position Win:" + (new Double(tradeHistories.stream().filter(result1 -> result1.getPip().compareTo(Double.MIN_VALUE) > 0).count()) / new Double(tradeHistories.size())) * 100D + "%");
        tradeHistories.stream().forEach(
                tradeHistory ->
                        log.info("Real : Symbol: " + tradeHistory.getSymbol() + " status: " + (tradeHistory.getPip() > 0 ? "WIN" : "LOOSE") + ", Pip=" + tradeHistory.getPip() + " Result=" + tradeHistory.getResult() + ",Way =" + tradeHistory.getWay() + " ,Begin =" + tradeHistory.getTradeDate() + " End=" + tradeHistory.getEndTime()));

    }

    @Data
    @AllArgsConstructor
    public static class DayResult {
        private List<Result> results;
        private LocalDateTime localDateTime;
    }

    @Data
    @AllArgsConstructor
    public static class Result {
        private Double result;
        private Way way;
        private Date begin;
        private Date end;
        private Symbol symbol;
        private String comment;
        private Double maxLoss;
        private Double maxProfit;
        private Double risque;


        public String status() {
            return result.compareTo(Double.MIN_VALUE) > 0 ? "WIN" : "LOOSE";
        }

    }

    private static DayResult backTestExecDay(int year, int month, int day) {
        return backTestExecDay(allDates(year, month, day));
    }

    private static DayResult backTestExecDay(List<LocalDateTime> localDateTimes) {
        LocalDateTime l = localDateTimes.get(0);
        //economicNewsClient.economicCalendarPut(dateTimeFormatter.format(localDate.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))));

        final StategyExecutorService intradayStrategy = application.getBean(StategyExecutorService.class);
        final OrderBookingHandler orderBookingHandler = application.getBean(OrderBookingHandler.class);
        final OrderStatusHandler orderStatusHandler = application.getBean(OrderStatusHandler.class);
        //final TechnicalAnalysisService technicalAnalysisService = application.getBean(TechnicalAnalysisService.class);
        openedTrades = (Map<Symbol, Trade>) application.getBean("openedTrades");
        sessionProposals = (Map<Symbol, Trade>) application.getBean("sessionProposals");
        openedPosition = (Map<Symbol, Position>) application.getBean("openedPosition");
        result = (List<Result>) application.getBean("result");
        //final Map<Symbol, Map<Symbol, Double>> correlations = (Map<Symbol, Map<Symbol, Double>>) application.getBean("correlations");
        //correlations.clear();
        //correlations.putAll(SessionConfig.correlations(technicalAnalysisService, toDate(localDateTimes.get(0))));

        for (LocalDateTime localDateTime : localDateTimes) {

            // check proposal
            sessionProposals.entrySet().forEach(entry -> orderBookingHandler.processTradeProposal(entry.getValue(), toDate(localDateTime)));

            log.info("start for " + dateToString(toDate(localDateTime)));

            if (localDateTime.toLocalTime().isBefore(endOfDay.minusMinutes(55))){
                intradayStrategy.process(localDateTime);
            }


            for (LocalDateTime checkStatusDate : allDatesForcheckStatus(localDateTime)) {

                if (!openedTrades.isEmpty()) {
                    Candle candle = instrumentServiceDB.getPricing(CandlestickGranularity.M1, openedTrades.keySet().iterator().next(), toDate(checkStatusDate), getFromDate(toDate(checkStatusDate), CandlestickGranularity.M1, 5000)).getLast();
                    // check stop loss
                    openedTrades.entrySet().stream().filter(pos -> pos.getValue().getStopLoss() != null && pos.getValue().getWay().getValue() * (candle.getClose() - pos.getValue().getStopLoss()) < 0)
                            .forEach(pos -> {
                                Trade trade = openedTrades.get(pos.getKey());
                                trade.setLastPrice(trade.getStopLoss());
                                List<Position> positions = positionService.getOpenedPositions(pos.getKey());
                                positionService.closeOpenedPosition(positions);
                            });
                    if (!openedTrades.isEmpty()) {
                        log.info("check statut for " + dateToString(toDate(checkStatusDate)));
                        openedTrades.entrySet().forEach(entry -> orderStatusHandler.handleStatus(entry.getValue(), toDate(checkStatusDate)));
                    }
                }
            }
        }
        // Close positions at EOD
        if (stopPositionEndOfDay) {
            return closePositionEOD(result,localDateTimes.get(0));
        }else {
           return new DayResult(new ArrayList<>(result), localDateTimes.get(0));
        }
    }

    private static DayResult closePositionEOD(final List<Result> result,LocalDateTime localDateTime){

        for (Map.Entry<Symbol, Trade> symbolTradeEntry : openedTrades.entrySet()) {
            final Trade trade = symbolTradeEntry.getValue();
            result.add(new Result(trade.currentProfitInPip(), trade.getWay(), trade.getEntryTime(), trade.getLastCheck(), trade.getSymbol(), trade.getComment(), trade.getMaxLoss(), trade.getMaxProfit(), trade.getRisqueInPip()));
        }
        sessionProposals.clear();
        openedTrades.clear();
        openedPosition.clear();
        final List<Result> forDay = new ArrayList<>(result);
        result.clear();
        return new DayResult(forDay, localDateTime);
    }


    private static List<DayResult> backTestExecWeek(int year, int month, int day) {
        log.info(" start backtestForweek  year={}  month={}  day={}", year, month, day);
        List<DayResult> globalResult = new ArrayList<>();
        for (List<LocalDateTime> localDateTimes : allDatesSemaine(year, month, day)) {

            globalResult.add(backTestExecDay(localDateTimes));
        }
        lastDate=new CustomList<>(new CustomList<>(allDatesSemaine(year, month, day)).getLast()).getLast();
        return globalResult;
    }

    private static List<DayResult> backTestExecCustom(LocalDate start, LocalDate end) {
        log.info(" start backtest start {}  end {} ", start, end);
        List<DayResult> globalResult = new ArrayList<>();

        List<List<LocalDateTime>> lists = new ArrayList<>();
        LocalDate localDate = start;
        while (localDate.isBefore(end) || localDate.equals(end)) {
            localDate = localDate.plusDays(1);
            if (excludeDates.contains(localDate) || localDate.getDayOfWeek() == DayOfWeek.SUNDAY || localDate.getDayOfWeek() == DayOfWeek.SATURDAY) {
                continue;
            }
            lists.add(allDates(localDate.getYear(), localDate.getMonth().getValue(), localDate.getDayOfMonth()));

        }
        for (List<LocalDateTime> localDateTimes : lists) {

            globalResult.add(backTestExecDay(localDateTimes));
        }
        return globalResult;
    }

    private static List<DayResult> backTestMonth(int year, int month) {
        log.info(" start backtestForweek  year={} month={}", year, month);
        List<LocalDate> localDates = new ArrayList<>();
        LocalDate dateTime = LocalDate.of(year, month, 1);
        LocalDate localDate = dateTime;

        while (localDate.getYear() == year && localDate.getMonthValue() == month) {
            localDate = localDate.plusDays(1);
            if (excludeDates.contains(localDate) || localDate.getDayOfWeek() == DayOfWeek.SUNDAY || localDate.getDayOfWeek() == DayOfWeek.SATURDAY) {
                continue;
            }
            localDates.add(localDate);
        }


        // Load pricing

        //Map<CandlestickGranularity, Map.Entry<Date, Candle>>

        // end pricing
        lastDate=localDates.stream().flatMap(day ->
                allDates(day.getYear(), day.getMonthValue(), day.getDayOfMonth()).stream()).collect(Collectors.toCollection(CustomList::new)).getLast();
        return localDates.stream().map(day ->
                backTestExecDay(allDates(day.getYear(), day.getMonthValue(), day.getDayOfMonth()))).collect(Collectors.toCollection(CopyOnWriteArrayList::new));

    }


    private static List<DayResult> backTestYear(int year) {
        log.info(" start backtestForweek  year={}", year);
        List<LocalDate> localDates = new ArrayList<>();
        LocalDate dateTime = LocalDate.of(year, 1, 1);
        LocalDate localDate = dateTime;

        while (localDate.getYear() == year) {
            localDate = localDate.plusDays(1);
            if (excludeDates.contains(localDate) || localDate.getDayOfWeek() == DayOfWeek.SATURDAY || localDate.getDayOfWeek() == DayOfWeek.SUNDAY || (localDate.getMonthValue() == 1 && localDate.getDayOfMonth() == 1)) {
                continue;
            }
            localDates.add(localDate);
        }


        // Load pricing

        //Map<CandlestickGranularity, Map.Entry<Date, Candle>>

        // end pricing
        lastDate=localDates.stream().flatMap(day ->
                allDates(day.getYear(), day.getMonthValue(), day.getDayOfMonth()).stream()).collect(Collectors.toCollection(CustomList::new)).getLast();
        return localDates.stream().map(day ->
                backTestExecDay(allDates(day.getYear(), day.getMonthValue(), day.getDayOfMonth()))).collect(Collectors.toCollection(CopyOnWriteArrayList::new));

    }


    public static List<List<LocalDateTime>> allDatesSemaine(int year, int month, int day) {
        List<List<LocalDateTime>> lists = new ArrayList<>();
        LocalDate dateTime = LocalDate.of(year, month, day);
        LocalDate localDate=null;
        for (int i = 1; i <= 5; i++) {
            localDate = dateTime.plusDays(i);
            lists.add(allDates(localDate.getYear(), localDate.getMonth().getValue(), localDate.getDayOfMonth()));
        }
        return lists;

    }

    private static void saveCharts(final Map<String, Double> current) {
        DefaultCategoryDataset line_chart_dataset = new DefaultCategoryDataset();
        final AtomicDouble pl = new AtomicDouble(0D);
        current.entrySet().stream().forEach(
                entry -> {
                    line_chart_dataset.addValue(pl.addAndGet(entry.getValue()), "PIP", entry.getKey());
                }

        );
        JFreeChart lineChartObject = ChartFactory.createLineChart(
                "PIP Vs Day", "Day",
                "Pip",
                line_chart_dataset, PlotOrientation.VERTICAL,
                true, true, false);

        int width = 1640;    /* Width of the image */
        int height = 480;   /* Height of the image */
        try {
            File dir = new File("reports");
            if (!dir.exists()) dir.mkdirs();
            File lineChart = new File("reports/PerfomanceChart_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".jpeg");
            ChartUtils.saveChartAsJPEG(lineChart, lineChartObject, width, height);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

}
