package com.trading.forex.repository

import com.trading.forex.SpringTestConfig
import com.trading.forex.common.model.Symbol
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlGroup
import spock.lang.Specification

import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD

@ContextConfiguration(classes =  SpringTestConfig.class)
@SqlGroup([
        @Sql(scripts = "classpath:sql/candle-insert.sql", executionPhase = BEFORE_TEST_METHOD),
        @Sql(scripts = "classpath:sql/candle-rollback.sql", executionPhase = AFTER_TEST_METHOD)
])
class CandleHistoryRepositoryTest extends Specification{

    @Autowired
    private CandleHistoryRepository candleHistoryRepository;

    def "GetCandlesHistory"() {

        given:
        def symbol = Symbol.EUR_USD
        def candlestickGranularity= com.trading.forex.common.model.CandlestickGranularity.M5
        def epochFrom=1513689600000
        def epochTo=1515084600000
        when: 'Find pricing data for specific symbol and date  '
        def result=candleHistoryRepository.findAllByKeyEpochBetweenAndKeySymbolAndKeyCandlestickGranularityOrderByKeyEpoch(epochFrom,epochTo,symbol,candlestickGranularity)

        then: 'expect result'
        result.size()==2916
        noExceptionThrown()
    }

}