package com.trading.forex.service.impl;

import com.trading.forex.service.MessageSenderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;


@Service
@Slf4j
public class MessageSenderServiceImpl implements MessageSenderService {

    private final RabbitTemplate rabbitTemplate;

    private String delayedExchange;

    private SimpMessagingTemplate websocketMessagingTemplate;


    @Autowired
    public MessageSenderServiceImpl(final RabbitTemplate rabbitTemplate, @Value("${delayed.exchange}") final String delayedExchange, final SimpMessagingTemplate websocketMessagingTemplate) {

        this.rabbitTemplate = rabbitTemplate;
        this.delayedExchange = delayedExchange;
        this.websocketMessagingTemplate = websocketMessagingTemplate;

    }

    @Override
    public void sendDelayedMessage(final String queue, final Serializable message, final long delay) {
        log.info("send message queue {} delay {}",queue,delay);
        rabbitTemplate.convertAndSend(delayedExchange, queue, message, msg -> {
                    msg.getMessageProperties().setHeader("x-delay", delay);
                    return msg;
                }
        );
    }

    @Override
    public void sendMessage(final String queue, final Serializable message) {
        rabbitTemplate.convertAndSend(queue, message);
    }

    @Override
    public void sendStompMessage(final String topic, final Serializable message) {
        websocketMessagingTemplate.convertAndSend(topic, message);

    }


}
