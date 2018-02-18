package com.trading.forex.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableRabbit
public class RabbitConfiguration {
    @Autowired
    private ConnectionFactory connectionFactory;

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitAdmin rabbitAdmin() {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public RabbitTemplate simpleRabbitTemplate() {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate();
        rabbitTemplate.setConnectionFactory(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }

    @Bean
    public CustomExchange delayExchange(@Value("${delayed.exchange}") final String delayedExchange) {

        Map<String, Object> args = new HashMap<String, Object>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange(delayedExchange, "x-delayed-message", true, false, args);
    }

    @Bean
    public Queue orderBookingTempoQueue(@Value("${order.booking.tempo.queue}") final String orderBookingTempoQueue) {
        return new Queue(orderBookingTempoQueue, true, false, false);

    }

    @Bean
    public Queue orderStatusTempoQueue(@Value("${order.status.tempo.queue}") final String orderStatusTempoQueue) {
        return  new Queue(orderStatusTempoQueue, true, false, false);
    }


    @Bean
    public Binding bindingOrderStatusTempoQueue(final Queue orderStatusTempoQueue,@Value("${order.status.tempo.queue}") final String orderStatusTempoQueueName, final Exchange delayExchange) {
        return BindingBuilder.bind(orderStatusTempoQueue).to(delayExchange).with(orderStatusTempoQueueName).noargs();
    }

    @Bean
    public Binding bindingOrderBookingTempoQueue(final Queue orderBookingTempoQueue,@Value("${order.booking.tempo.queue}") final String orderBookingTempoQueueName, final Exchange delayExchange) {
        return BindingBuilder.bind(orderBookingTempoQueue).to(delayExchange).with(orderBookingTempoQueueName).noargs();
    }


}
