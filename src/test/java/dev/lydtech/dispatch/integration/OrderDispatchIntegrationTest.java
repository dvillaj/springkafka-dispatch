package dev.lydtech.dispatch.integration;

import dev.lydtech.dispatch.config.DispatchConfiguration;
import dev.lydtech.dispatch.message.DispatchPreparing;
import dev.lydtech.dispatch.message.OrderCreated;
import dev.lydtech.dispatch.message.OrderDispached;
import dev.lydtech.dispatch.util.TestEventData;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.UUID.randomUUID;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.IsEqual.equalTo;

@Slf4j
@SpringBootTest(classes = {DispatchConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@EmbeddedKafka(controlledShutdown = true)
public class OrderDispatchIntegrationTest {

    private static final String ORDER_DISPATCHED_TOPIC = "order.dispatched";
    private static final String DISPATCH_TRACKING_TOPIC = "dispatch.tracking";
    private static final String ORDER_CREATED_TOPIC = "order.created";

    @Autowired
    private KafkaTemplate kafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @Autowired
    private KafkaTestListener testListener;

    @Configuration
    static class TestConfig {
        @Bean
        public KafkaTestListener testListener() {
            return new KafkaTestListener();
        }
    }

    public static class KafkaTestListener {
        AtomicInteger dispatchPreparingCounter = new AtomicInteger(0);
        AtomicInteger orderDispatchedCounter = new AtomicInteger(0);

        @KafkaListener(groupId = "KafkaIntegrationTest",
                       topics = DISPATCH_TRACKING_TOPIC)
        void receiveDispatchPreparing(@Payload DispatchPreparing payload) {
            log.info("Receiving DispatchPreparing Event: {}", payload);

            dispatchPreparingCounter.incrementAndGet();
        }

        @KafkaListener(groupId = "KafkaIntegrationTest",
                topics = ORDER_DISPATCHED_TOPIC)
        void receiveDispatchPreparing(@Payload OrderDispached payload) {
            log.info("Receiving OrderDispached Event: {}", payload);

            orderDispatchedCounter.incrementAndGet();
        }
    }

    @BeforeEach
    public void setUp() {
        testListener.dispatchPreparingCounter.set(0);
        testListener.orderDispatchedCounter.set(0);

        registry.getListenerContainers().stream().forEach(container ->
                ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic()));
    }

    @Test
    @SneakyThrows
    public void testOrderDispatchedFlow() {

        OrderCreated orderCreated = TestEventData.buildOrderCreatedEvent(randomUUID(), "test-item");
        sendMessage(ORDER_CREATED_TOPIC, orderCreated);

        await().atMost(3, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.dispatchPreparingCounter::get, equalTo(1));

        await().atMost(1, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.orderDispatchedCounter::get, equalTo(1));
    }

    @SneakyThrows
    private void sendMessage(String topic, Object payload) {
        var message = MessageBuilder
                .withPayload(payload)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .build();

        kafkaTemplate.send(message).get();
    }

}
