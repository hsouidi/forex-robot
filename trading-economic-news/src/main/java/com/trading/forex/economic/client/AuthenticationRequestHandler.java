package com.trading.forex.economic.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;


@Component
public class AuthenticationRequestHandler implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate requestTemplate) {
        requestTemplate.header("Authorization","Bearer 3ae05d6847e40c58d06977f743a6585d-0d8687939e1baaae47ee095198865a75");

    }
}
