package org.shrtr.core.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.shrtr.core.domain.entities.BaseEntity;
import org.shrtr.core.domain.entities.Link;
import org.shrtr.core.domain.entities.User;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.charset.Charset;
import java.util.List;

@Primary
@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitEventService implements EventService {

    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitAdmin admin;
    private final SimpleMessageListenerContainer messageListenerContainer;


    /**
     * We will create and exchange for each entity: E.g. user, link, linkmetric
     *
     * Events will be sent using routing keys with the following format:
     *
     *      | exchange     | type   | routing key       |
     *      |-------------------------------------------|
     *      | user         | topic  | user.registered   |
     *      | user         | topic  | user.created      |
     *      | user         | topic  | user.updated      |
     *      | user         | topic  | user.deleted      |
     *      | link         | topic  | link.created      |
     *      | link         | topic  | link.updated      |
     *      | link         | topic  | link.deleted      |
     *      ...
     */
    @PostConstruct
    void initRabbit (){
        List<String> entities = GetEntities().stream().map(String::toLowerCase).toList();

        for (String entity: entities) {

            var exchange = new TopicExchange(entity);
            admin.declareExchange(exchange);
        }

        // Declare debug queues for the monolith to listen to the event it itself emits
        for (String entity: entities) {

            String queueName = entity + "-monolith-debug-queue";
            String routingKey = entity + ".*"; // Receive all events related to 'entity'
            Queue debugQueue = new Queue(queueName, false, false, false);
            Binding debugQueueBinding = new Binding(queueName, Binding.DestinationType.QUEUE, entity, routingKey, null);
            admin.declareQueue(debugQueue);
            admin.declareBinding(debugQueueBinding);
            messageListenerContainer.addQueues(debugQueue);
        }

        // set-up listener
        messageListenerContainer.setMessageListener(message -> {
            String topic = message.getMessageProperties().getReceivedRoutingKey();
            String content = new String(message.getBody(), Charset.defaultCharset());
            log.info("New Event! {}: {}", topic, content);
        });
    }

    private List<String> GetEntities() {
        Reflections reflections = new Reflections(BaseEntity.class);
        return reflections.getSubTypesOf(BaseEntity.class)
                .stream()
                .map(Class::getSimpleName)
                .toList();
    }

    @Override
    public void userCreated(User user) {
        try {
            String exchange = "user";
            String routingKey = "user-registered";
            rabbitTemplate.convertAndSend(exchange, routingKey, objectMapper.writeValueAsString(user));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public void entityEvent(Object entity, String event) {
        try {
            String exchange = entity.getClass().getSimpleName().toLowerCase();
            String routingKey = entity.getClass().getSimpleName().toLowerCase() + "." + event;
            rabbitTemplate.convertAndSend(exchange, routingKey, objectMapper.writeValueAsString(entity));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void entityCreated(Object entity) { entityEvent(entity, "created"); }

    @Override
    public void entityDeleted(Object entity) { entityEvent(entity, "deleted"); }

    @Override
    public void entityUpdated(Object entity) { entityEvent(entity, "updated"); }

    @Override
    public void linkRedirected(LinkRedirectedEvent event) {
        try {
            rabbitTemplate.convertAndSend("link", "link.redirected", objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
