package com.trading.forex.economic;

import com.trading.forex.economic.service.IndicatorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan(basePackages = {"com.trading.forex.economic"})
@EnableScheduling
@EntityScan
@EnableFeignClients
@EnableRetry
@Slf4j
public class EconomicNewsApp {

    public static void main(String[] args) {
        new SpringApplicationBuilder(EconomicNewsApp.class).web(WebApplicationType.REACTIVE).run(args);
    }


}
