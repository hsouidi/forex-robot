package com.trading.forex.repository;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public interface ESRepository {

    @Async
    void push(final Map<String, Object> data);
}
