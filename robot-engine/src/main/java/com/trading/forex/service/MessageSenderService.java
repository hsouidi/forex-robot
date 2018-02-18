package com.trading.forex.service;

import java.io.Serializable;

public interface MessageSenderService {
    void sendDelayedMessage(String queue, Serializable message, long delayInMills);

    void sendMessage(String queue, Serializable message);

    void sendStompMessage(String topic, Serializable message);
}
