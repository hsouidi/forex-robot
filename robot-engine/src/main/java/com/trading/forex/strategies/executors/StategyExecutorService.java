package com.trading.forex.strategies.executors;

import java.time.LocalDateTime;

public interface StategyExecutorService {

    void process(LocalDateTime currentTime);

}


