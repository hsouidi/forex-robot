package com.trading.forex;


import com.trading.forex.repository.ESRepositoryImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.PropertySource;

@EnableAutoConfiguration
@PropertySource("classpath:application-unit-test.properties")
@ComponentScan(basePackages = {"com.trading.forex.repository"
        , "org.springframework.security.core.userdetails"}, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = {ESRepositoryImpl.class})})
@Slf4j
public class SpringTestConfig {

}
