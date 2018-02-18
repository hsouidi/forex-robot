package com.trading.forex.oanda.service;

import com.oanda.v20.Context;
import com.oanda.v20.ExecuteException;
import com.oanda.v20.RequestException;
import com.oanda.v20.account.AccountID;
import com.oanda.v20.instrument.Candlestick;
import com.oanda.v20.instrument.CandlestickData;
import com.oanda.v20.instrument.InstrumentCandlesRequest;
import com.oanda.v20.instrument.InstrumentCandlesResponse;
import com.oanda.v20.primitives.DateTime;
import com.oanda.v20.primitives.InstrumentName;
import com.trading.forex.common.model.Candle;
import com.trading.forex.common.model.CandlestickGranularity;
import com.trading.forex.common.model.Symbol;
import com.trading.forex.common.utils.CustomList;
import com.trading.forex.connector.exceptions.ConnectorTechnicalException;
import com.trading.forex.connector.service.InstrumentService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.lucene.util.MathUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * Created by wf on 10/21/2017.
 */
@Service
@Slf4j
public class OandaInstrumentServiceImpl implements InstrumentService {

    @Autowired
    private Context context;

    @Autowired
    private AccountID accountID;


    private static final String FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'";

    @Override
    public CustomList<Candle> getPricing(CandlestickGranularity candlestickGranularity, Symbol symbol, int count) {
        try {
            InstrumentCandlesResponse instrumentCandlesResponse = getPricing(candlestickGranularity, symbol.getBrokerValue(), null,null,count);
            return instrumentCandlesResponse.getCandles().stream().map(candlestick -> toCandle(candlestick,symbol)).collect(Collectors.toCollection(CustomList::new));
        } catch (Exception e) {
            throw new ConnectorTechnicalException(e);
        }
    }


    public static Candle toCandle(Candlestick candlestick, Symbol symbol) {
        final CandlestickData candlestickData = candlestick.getBid();
        final long epoch=toEpoch(candlestick.getTime());
        return Candle.builder()
                .open(candlestickData.getO().doubleValue())
                .close(candlestickData.getC().doubleValue())
                .high(candlestickData.getH().doubleValue())
                .low(candlestickData.getL().doubleValue())
                .volume(candlestick.getVolume())
                .epoch(epoch)
                .date(new Date(epoch))
                .symbol(symbol)
                .complete(candlestick.getComplete())
                .build();
    }

    private static Long toEpoch(DateTime dateTime) {
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(FORMAT);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone(ZoneId.of("GMT")));
        try {
            return simpleDateFormat.parse(dateTime.toString()).getTime();
        } catch (ParseException e) {
            throw new ConnectorTechnicalException(e);
        }
    }

    @Override
    @Retryable(
            value = {ConnectorTechnicalException.class},
            maxAttempts = 30,
            backoff = @Backoff(delay = 1000))
    public CustomList<Candle> getPricing(CandlestickGranularity candlestickGranularity, Symbol symbol, Date to, Date from) {
        try {
            log.debug("Retrieving princing for CandlestickGranularity {}, Symbol {} , Date {} , Date {}",candlestickGranularity, symbol,to, from);
            InstrumentCandlesResponse instrumentCandlesResponse = getPricing(candlestickGranularity, symbol.getBrokerValue(), toDateTime(to),toDateTime(from),null);
            return instrumentCandlesResponse.getCandles().stream().map(candlestick -> toCandle(candlestick,symbol)).collect(Collectors.toCollection(CustomList::new));
        } catch (Exception e) {
            log.debug(e.getMessage(),e);
            throw new ConnectorTechnicalException(e);
        }
    }

    @Override
    public CustomList<Candle> getPricingHeinkin(CandlestickGranularity candlestickGranularity, Symbol symbol, Date to, Date from) {
        CustomList<Candle> candles=getPricing(candlestickGranularity,symbol,to,from);
        final CustomList<Candle> result=new CustomList<>();
        for(int i=1;i<candles.size();i++){
            final Candle currentCandle=candles.get(i);
            final Candle previousCandle=candles.get(i-1);
            final double close=(currentCandle.getClose()+currentCandle.getOpen()+currentCandle.getHigh()+currentCandle.getLow())/4;
            // Open = (Open of Previous Bar + Close of Previous Bar) / 2 o This is the midpoint of the previous bar.
            final double open=(previousCandle.getOpen()+previousCandle.getClose())/2;
            // High = Max of (High, Open, Close) o Highest value of the three.
            final double high= NumberUtils.max(currentCandle.getHigh(),currentCandle.getOpen(),currentCandle.getClose());
            // Low = Min of (Low, Open, Close) o Lowest value of the three.
            final double low= NumberUtils.min(currentCandle.getHigh(),currentCandle.getOpen(),currentCandle.getClose());
            result.add(Candle.builder()
                    .epoch(currentCandle.getEpoch())
                    .symbol(currentCandle.getSymbol())
                    .low(low)
                    .high(high)
                    .close(close)
                    .open(open)
                    .volume(currentCandle.getVolume())
                    .build());
        }

        return result;
    }


    private DateTime toDateTime(Date dateTime) {
/*        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(FORMAT);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));*/
        return new DateTime(String.valueOf(dateTime.getTime()/1000));
    }

    private InstrumentCandlesResponse getPricing(CandlestickGranularity candlestickGranularity, String instrumentName, DateTime to, DateTime from,Integer count) throws ExecuteException, RequestException {
        final InstrumentCandlesRequest instrumentCandlesRequest = new InstrumentCandlesRequest(new InstrumentName(instrumentName));
        instrumentCandlesRequest.setGranularity(com.oanda.v20.instrument.CandlestickGranularity.valueOf(candlestickGranularity.name()));
        instrumentCandlesRequest.setIncludeFirst(true);
        instrumentCandlesRequest.setPrice("B");

        if (to != null && from != null) {
            instrumentCandlesRequest.setTo(to.toString());
            instrumentCandlesRequest.setFrom(from.toString());
        } else {
            instrumentCandlesRequest.setCount(count);
        }
        return context.instrument.candles(instrumentCandlesRequest);
    }
}
