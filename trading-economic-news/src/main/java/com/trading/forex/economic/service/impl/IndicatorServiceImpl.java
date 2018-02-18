package com.trading.forex.economic.service.impl;

import com.trading.forex.common.exceptions.RobotTechnicalException;
import com.trading.forex.common.model.Currency;
import com.trading.forex.common.model.*;
import com.trading.forex.common.utils.CustomList;
import com.trading.forex.economic.client.EconomicCalendarData;
import com.trading.forex.economic.client.OandaEconomicCalendar;
import com.trading.forex.economic.entity.EconomicCalendarEntity;
import com.trading.forex.economic.entity.ForexSignal;
import com.trading.forex.economic.model.ZuluPosition;
import com.trading.forex.economic.service.IndicatorService;
import com.trading.forex.model.InvestingData;
import com.trading.forex.model.InvestingDataGroup;
import com.trading.forex.model.InvestingTechIndicator;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.TextUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Created by wf on 04/19/2017.
 */
@Service
@Slf4j
public class IndicatorServiceImpl implements IndicatorService {


    private String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    //@Autowired
    private ChromeDriver chromeDriver;

    @Autowired
    private OandaEconomicCalendar oandaEconomicCalendar;


    List<Stock> stockList = new ArrayList<>();

    @Override
    public CustomList<EconomicCalendarEntity> getEconomicCalendarData(final Importance importanceFilter, final String weekBeginDate) {
        CustomList<EconomicCalendarEntity> calendarDataCustomList = new CustomList();
        final List<EconomicCalendarData> economicCalendarData = oandaEconomicCalendar.getEconomicCalendarData("EUR_USD", 31536000);
        economicCalendarData.forEach(e -> calendarDataCustomList.add(EconomicCalendarEntity.builder()
                .actual(e.getActual())
                .forecast(e.getForecast())
                .previous(e.getPrevious())
                .importance(Importance.fromValue(e.getImpact()))
                .currency(Currency.fromValue(e.getCurrency()))
                .economicCalendarID(EconomicCalendarEntity.EconomicCalendarID.builder().eventDate(new Date(e.getTimestamp() * 1000)).event(e.getTitle()).build())
                .build()));
        log.info("retrieve economic data size  {} ..", calendarDataCustomList.size());
        return calendarDataCustomList;
    }

    private Double extractDouble(String source) {

        if (TextUtils.isEmpty(source)) {
            return null;
        }
        boolean isSpecial = source.indexOf(",") != -1 && source.indexOf(".") != -1;
        String number = "0";
        String sign = "";
        int length = source.length();

        boolean cutNumber = false;
        for (int i = 0; i < length; i++) {
            char c = source.charAt(i);
            if (c == '-') {
                sign = String.valueOf(c);
            }
            if (cutNumber) {
                if (Character.isDigit(c) || c == '.' || c == ',') {
                    if (!isSpecial && c == ',') {
                        c = (c == ',' ? '.' : c);
                        number += c;
                    }
                } else {
                    cutNumber = false;
                    break;
                }
            } else {
                if (Character.isDigit(c)) {
                    cutNumber = true;
                    number += c;
                }
            }
        }
        try {
            return Double.parseDouble(sign + number);
        } catch (NumberFormatException e) {
            log.error("error when parsing {} ", source);
            log.error(e.getMessage(), e);
            return null;
        }
    }


    @Override
    public InvestingDataGroup expertDecision(final Symbol symbol) {
        try {

            Connection.Response response = Jsoup.connect("http://fr.investing.com/currencies/" + symbol.getInvestingValue() + "/")
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
                    .referrer("http://www.google.com")
                    .timeout(12000)
                    .followRedirects(true)
                    .execute();
            for (Element element : response.parse().select("table")) {
                if (element.attributes().get("class").equals("genTbl closedTbl technicalSummaryTbl ")) {
                    InvestingData investingDataMovingAverage = buildInvestingData(element.select("tbody").select("tr").get(0).select("td"));
                    InvestingData investingDataTechIndicator = buildInvestingData(element.select("tbody").select("tr").get(1).select("td"));
                    InvestingData investingDataSum = buildInvestingData(element.select("tbody").select("tr").get(2).select("td"));

                    return InvestingDataGroup.builder()
                            .movingAverage(investingDataMovingAverage)
                            .summary(investingDataSum)
                            .technicalIndicator(investingDataTechIndicator)
                            .symbol(symbol)
                            .build();
                }
            }
            throw new RobotTechnicalException("Cannot found data");
        } catch (Exception e) {
            log.error("Erreur lors e la recuperation tc pour le symbol " + symbol.getIndicatorValue());
            throw new RobotTechnicalException(e);
        }
    }

    @Override
    public Map<Symbol, InvestingTechIndicator> expertDecision(Duration duration) {

        try {
            final Map<Symbol, InvestingTechIndicator> result = new HashMap<>();
            Connection.Response response = Jsoup.connect("https://www.investing.com/technical/indicators")
                    .ignoreContentType(true)
                    .method(Connection.Method.POST)
                    .data("period", String.valueOf(duration.getDuration() * 60))
                    .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
                    .referrer("http://www.google.com")
                    .timeout(12000)
                    .followRedirects(true)
                    .execute();
            final Elements parents = response.parse().getElementsByAttributeValue("name", "toolTable");
            final Elements allCurs = parents.get(0).getElementsByAttributeValue("class", "h3LikeTitle");
            for (Element element : allCurs) {
                boolean flag = false;
                for (Element child : parents.get(0).children()) {
                    if (child.equals(element)) {
                        int begin = child.elementSiblingIndex();
                        Symbol symbol = Symbol.fromIndicatorValue(element.getElementsByIndexEquals(begin).get(0).ownText().replace("/", ""));
                        flag = true;
                        Element datas = parents.get(0).children().get(begin + 3).child(0).child(0);
                        Double buy = extractNumber(datas.child(0).ownText());
                        Double sell = extractNumber(datas.child(1).ownText());
                        Double neutral = extractNumber(datas.child(2).ownText());
                        Decision summary = Decision.fromValue(datas.child(3).select("span").get(0).ownText());
                        Elements table = parents.get(0).children();
                        Elements indsPage1 = table.get(begin + 1).select("tbody").select("tr");
                        Elements indsPage2 = table.get(begin + 2).select("tbody").select("tr");
                        result.put(symbol, InvestingTechIndicator.builder()
                                .buy(buy)
                                .sell(sell)
                                .neutral(neutral)
                                .symbol(symbol)
                                .rsi(build(indsPage1.get(0)))
                                .stoch(build(indsPage1.get(1)))
                                .stochRsi(build(indsPage1.get(2)))
                                .macd(build(indsPage1.get(3)))
                                .adx(build(indsPage1.get(4)))
                                .williamR(build(indsPage1.get(5)))
                                .cci(build(indsPage2.get(0)))
                                .atr(build(indsPage2.get(1)))
                                .highLows(build(indsPage2.get(2)))
                                .ultimateOscilator(build(indsPage2.get(3)))
                                .roc(build(indsPage2.get(4)))
                                .bullBearPower(build(indsPage2.get(5)))
                                .summary(summary)
                                .build()
                        );
                        break;
                    }
                }
                if (!flag) {
                    throw new RobotTechnicalException("Cannot found data");
                }
            }
            return result;
        } catch (Exception e) {
            log.error("Erreur lors e la recuperation pour la pêriod " + duration);
            throw new RobotTechnicalException(e);
        }

    }

    @Override
    public InvestingTechIndicator expertDecision(Symbol symbol, Duration duration) {

        try {
            Connection.Response response = Jsoup.connect("https://www.investing.com/currencies/" + symbol.getInvestingValue() + "-technical")
                    .ignoreContentType(true)
                    .method(Connection.Method.POST)
                    .data("period", String.valueOf(duration.getDuration() * 60), "viewType", "normal")
                    .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
                    .referrer("http://www.google.com")
                    .timeout(12000)
                    .followRedirects(true)
                    .execute();
            final Elements parents = response.parse().getElementsByAttributeValue("class", "genTbl closedTbl technicalIndicatorsTbl smallTbl float_lang_base_1");
            Elements indsPage1 = parents.get(0).children().select("tr");
            Elements datas = indsPage1.get(13).select("span");
            Double buy = extractNumber(datas.get(1).ownText());
            Double sell = extractNumber(datas.get(3).ownText());
            Double neutral = extractNumber(datas.get(5).ownText());
            Decision summary = Decision.fromValue(datas.get(6).select("span").get(0).ownText());
            return InvestingTechIndicator.builder()
                    .buy(buy)
                    .sell(sell)
                    .neutral(neutral)
                    .symbol(symbol)
                    .rsi(build(indsPage1.get(1)))
                    .stoch(build(indsPage1.get(2)))
                    .stochRsi(build(indsPage1.get(3)))
                    .macd(build(indsPage1.get(4)))
                    .adx(build(indsPage1.get(5)))
                    .williamR(build(indsPage1.get(6)))
                    .cci(build(indsPage1.get(7)))
                    .atr(build(indsPage1.get(8)))
                    .highLows(build(indsPage1.get(9)))
                    .ultimateOscilator(build(indsPage1.get(10)))
                    .roc(build(indsPage1.get(11)))
                    .bullBearPower(build(indsPage1.get(12)))
                    .summary(summary)
                    .build();
        } catch (Exception e) {
            log.error("Erreur lors e la recuperation pour la pêriod " + duration);
            throw new RobotTechnicalException(e);
        }

    }

    private InvestingTechIndicator.IndicatorAction build(Element element) {
        Elements elements = element.select("td");
        return InvestingTechIndicator.IndicatorAction.builder()
                .value(Double.valueOf(elements.get(1).ownText()))
                .action(Decision.fromValue(elements.get(2).select("span").get(0).ownText()))
                .build();
    }

    private Double extractNumber(String chaine) {
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(chaine);
        if (matcher.find()) {
            return Double.valueOf(matcher.group());
        }
        throw new RobotTechnicalException("Cannot found number in  string" + chaine);
    }


    private InvestingData buildInvestingData(Elements elements) {
        return InvestingData.builder().cinqMinute(converToDecision(elements.get(1).ownText()))
                .quinzeMinute(converToDecision(elements.get(2).ownText()))
                .heure(converToDecision(elements.get(3).ownText()))
                .jour(converToDecision(elements.get(4).ownText()))
                .mensuel(converToDecision(elements.get(5).ownText()))
                .build();
    }

    private Decision converToDecision(String value) {

        switch (value) {
            case "Achat":
                return Decision.BUY;
            case "Achat Fort":
                return Decision.STRONG_BUY;
            case "Vente":
                return Decision.SELL;
            case "Vente Forte":
                return Decision.STRONG_SELL;
            case "Neutre":
                return Decision.NEUTRAL;
            default:
                return Decision.NEUTRAL;
        }
    }

    private Symbol getSymbol(final String value) {
        return Symbol.valueOf(value.substring(0, 7).replace("/", "_"));
    }

    @Override
    public List<ForexSignal> getForexSignal() {
        final String url = "https://live-forex-signals.com/en/";
        chromeDriver.get(url);
        final List<WebElement> elements = chromeDriver.findElementsByClassName("signal-body");
        final List<ForexSignal> result = new ArrayList<>();
        for (WebElement webElement : elements) {
            final String[] data = webElement.getText().split("\n");
            final ForexSignal forexSignal = new ForexSignal();
            forexSignal.setForexSignalID(new ForexSignal.ForexSignalID(getSymbol(data[0]), parseDate(data[2])));
            forexSignal.setTill(parseDate(data[4]));
            forexSignal.setWay(Way.safeValueOf(data[5]));
            if (null == forexSignal.getWay()) {
                continue;
            }
            if ("Buy at".equals(data[6])) {
                forexSignal.setBuyAt(Double.valueOf(data[7]));
            } else if ("Sell at".equals(data[6])) {
                forexSignal.setBuyAt(Double.valueOf(data[7]));
            }
            forexSignal.setTakeProfit(Double.valueOf(data[9]));
            forexSignal.setStopLoss(Double.valueOf(data[11]));
            result.add(forexSignal);
        }
        return result;
    }

    @Override
    public Flux<StockTransaction> test() {
        Flux<Long> interval = Flux.interval(java.time.Duration.ofSeconds(1));
        interval.subscribe((i) -> stockList.forEach(stock -> stock.setPrice(i)));

        Flux<StockTransaction> stockTransactionFlux = Flux.fromStream(Stream.generate(() -> new StockTransaction("user", new Stock(), new Date())));
        return Flux.zip(interval, stockTransactionFlux).map(Tuple2::getT2);

    }

    @Override
    public List<ZuluPosition> getZuluPosition(final String traderId) {
        final String baseXpath = "/html/body/app/zl-layout/zl-trader/div[3]/div/div/div[2]/div[2]/trader-trading/div[3]/ngl-tabs/div/zl-trading-open-positions-table-container/zl-trading-open-positions-table";
        chromeDriver.get("https://www.zulutrade.com/trader/" + traderId + "/trading");
        final WebElement element = chromeDriver.findElementByPartialLinkText("Open Positions");
        element.click();
        final String text = element.findElements(By.xpath(baseXpath)).get(0).getText();
        if (text.equals("No data found to display")) {
            return new ArrayList<>();
        }
        final List<WebElement> elements = element.findElements(By.xpath(baseXpath + "/table/tbody/tr/td"));
        int i = 1;
        final List<ZuluPosition> zuluPositions = new ArrayList<>();
        ZuluPosition zuluPosition = null;
        for (WebElement row : elements) {
            if (row.getText().trim().isEmpty()) {
                continue;
            }
            switch (i) {
                case 1:
                    zuluPosition = ZuluPosition.builder().build();
                    zuluPosition.setCurrency(Symbol.fromIndicatorValue(row.getText().replace("/", "").trim()));
                    break;
                case 2:
                    zuluPosition.setType(Way.safeValueOf(row.getText().trim()));
                    break;
                case 3:
                    zuluPosition.setLots(Double.valueOf(row.getText()));
                    break;
                case 4:
                    zuluPosition.setOpenDate(toDate(row.getText()));
                    break;
                case 5:
                    zuluPosition.setOpenPrice(Double.valueOf(row.getText()));
                    break;
                case 6:
                    zuluPosition.setStopPrice(Double.valueOf(row.getText()));
                    break;
                case 7:
                    zuluPosition.setTakeProfit(Double.valueOf(row.getText()));
                    break;
                case 8:
                    zuluPosition.setCurrentPrice(Double.valueOf(row.getText()));
                    break;
                case 9:
                    zuluPositions.add(zuluPosition);
                    break;
            }
            i = i == 9 ? 1 : ++i;
        }
        return zuluPositions;

    }

    private Date toDate(final String strDate) {
        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);
        return Date.from(ZonedDateTime.parse(strDate.replaceAll("\\r|\\n", " "), dateTimeFormatter).toInstant());
    }

    private void scroll(ChromeDriver chromeDriver) {
        //To maximize the window. This code may not work with Selenium 3 jars. If script fails you can remove the line below
        //chromeDriver.manage().window().maximize();
        // This  will scroll down the page by  1000 pixel vertical
        chromeDriver.executeScript("window.scrollBy(0,1000)");

    }

    private Date parseDate(final String value) {
        final int length = value.length();
        final LocalTime localTime = LocalTime.parse(value.substring(length - 5, length));
        final LocalDateTime date = LocalDateTime.now();
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(date.getYear(), date.getMonth().getValue(), date.getDayOfMonth(), localTime.getHour(), localTime.getMinute(), localTime.getSecond());
        return calendar.getTime();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class StockTransaction {
        String user;
        Stock stock;
        Date when;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    static class Stock {
        String name;
        float price;
    }

}
