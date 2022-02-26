package org.shrtr.core.events;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RabbitConfiguration {

    @Value("${spring.rabbitmq.host}")
    private String rabbitMqHost;

    @Value("${spring.rabbitmq.port}")
    private int rabbitMqPort;

    @Value("${spring.rabbitmq.username}")
    private String rabbitMqUsername;

    @Value("${spring.rabbitmq.password}")
    private String rabbitMqPassword;

    @Bean
    public CachingConnectionFactory connectionFactory() {
        var cachingConnectionFactory = new CachingConnectionFactory(rabbitMqHost);
        cachingConnectionFactory.setPort(rabbitMqPort);
        cachingConnectionFactory.setPort(rabbitMqPort);
        cachingConnectionFactory.setUsername(rabbitMqUsername);
        cachingConnectionFactory.setPassword(rabbitMqPassword);
        return new CachingConnectionFactory(rabbitMqHost);
    }

    @Bean
    public RabbitAdmin amqpAdmin() {
        return new RabbitAdmin(connectionFactory());
    }

    @Bean
    public RabbitTemplate rabbitTemplate() {
        return new RabbitTemplate(connectionFactory());
    }

    @Bean
    public SimpleMessageListenerContainer messageListenerContainer() {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory());
        container.setAcknowledgeMode(AcknowledgeMode.AUTO);
        return container;
    }

}
