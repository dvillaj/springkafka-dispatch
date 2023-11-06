package dev.lydtech.dispatch.integration;

import dev.lydtech.dispatch.config.DispatchConfiguration;
import dev.lydtech.dispatch.message.DispatchCompleted;
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
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static dev.lydtech.dispatch.integration.WiremockUtils.stubWiremock;
import static java.util.UUID.randomUUID;
import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;


@Slf4j
@SpringBootTest(classes = {DispatchConfiguration.class})
@AutoConfigureWireMock(port=0)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@EmbeddedKafka(controlledShutdown = true)
public class OrderDispatchIntegrationTest {

    private static final String ORDER_DISPATCHED_TOPIC = "order.dispatched";
    private static final String DISPATCH_TRACKING_TOPIC = "dispatch.tracking";
    private static final String ORDER_CREATED_TOPIC = "order.created";

    private static final String ORDER_CREATED_DLQ_TOPIC = "order.created.DLT";

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

    @KafkaListener(groupId = "KafkaIntegrationTest",
            topics = {DISPATCH_TRACKING_TOPIC, ORDER_DISPATCHED_TOPIC, ORDER_CREATED_DLQ_TOPIC})
    public static class KafkaTestListener {
        AtomicInteger dispatchPreparingCounter = new AtomicInteger(0);
        AtomicInteger dispatchCompletedCounter = new AtomicInteger(0);
        AtomicInteger orderDispatchedCounter = new AtomicInteger(0);

        AtomicInteger orderCreatedDLQCounter = new AtomicInteger(0);

        @KafkaHandler
        void receiveDispatchPreparing(@Header(KafkaHeaders.RECEIVED_KEY) String key,
                                      @Payload DispatchPreparing payload) {
            log.info("Receiving DispatchPreparing Event: key: {} - payload: {}", key, payload);

            assertThat(key).isNotNull();
            assertThat(payload).isNotNull();
            dispatchPreparingCounter.incrementAndGet();
        }

        @KafkaHandler
        void receiveDispatchCompleted(@Header(KafkaHeaders.RECEIVED_KEY) String key,
                                      @Payload DispatchCompleted payload) {
            log.info("Receiving DispatchCompleted Event: key: {} - payload: {}", key, payload);

            assertThat(key).isNotNull();
            assertThat(payload).isNotNull();
            dispatchCompletedCounter.incrementAndGet();
        }


        @KafkaHandler
        void receiveDispatchPreparing(@Header(KafkaHeaders.RECEIVED_KEY) String key,
                                      @Payload OrderDispached payload) {
            log.info("Receiving OrderDispatched Event: key: {} - payload: {}", key, payload);

            assertThat(key).isNotNull();
            assertThat(payload).isNotNull();
            orderDispatchedCounter.incrementAndGet();
        }


        @KafkaHandler
        void receiveOrderCompletedDLQ(@Header(KafkaHeaders.RECEIVED_KEY) String key,
                                      @Payload OrderCreated payload) {
            log.info("Receiving OrderCreated DQL Event: key: {} - payload: {}", key, payload);

            assertThat(key).isNotNull();
            assertThat(payload).isNotNull();
            orderCreatedDLQCounter.incrementAndGet();
        }
    }

    @BeforeEach
    public void setUp() {
        testListener.dispatchPreparingCounter.set(0);
        testListener.orderDispatchedCounter.set(0);
        testListener.dispatchCompletedCounter.set(0);
        testListener.orderCreatedDLQCounter.set(0);

        WiremockUtils.reset();

        registry.getListenerContainers().stream().forEach(container ->
                ContainerTestUtils.waitForAssignment(container,
                        container.getContainerProperties().getTopics().length * embeddedKafkaBroker.getPartitionsPerTopic()));
    }

    @Test
    @SneakyThrows
    public void testOrderDispatchedFlow_Success() {

        stubWiremock("/api/stock?item=test-item", 200, "true");
        OrderCreated orderCreated = TestEventData.buildOrderCreatedEvent(randomUUID(), "test-item");
        String key = randomUUID().toString();

        sendMessage(ORDER_CREATED_TOPIC, key, orderCreated);

        await().atMost(3, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.dispatchPreparingCounter::get, equalTo(1));

        await().atMost(1, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.orderDispatchedCounter::get, equalTo(1));

        await().atMost(1, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.dispatchCompletedCounter::get, equalTo(1));

        await().atMost(1, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.orderCreatedDLQCounter::get, equalTo(0));
    }

    @Test
    @SneakyThrows
    public void testOrderDispatchedFlow_NotRetryableException() {

        stubWiremock("/api/stock?item=test-item", 400, "Bad Request");
        OrderCreated orderCreated = TestEventData.buildOrderCreatedEvent(randomUUID(), "test-item");
        String key = randomUUID().toString();

        sendMessage(ORDER_CREATED_TOPIC, key, orderCreated);

        //TimeUnit.SECONDS.sleep(3);

        await().atMost(3, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.orderCreatedDLQCounter::get, equalTo(1));

        assertThat(testListener.dispatchPreparingCounter.get()).isEqualTo(0);
        assertThat(testListener.orderDispatchedCounter.get()).isEqualTo(0);
        assertThat(testListener.dispatchCompletedCounter.get()).isEqualTo(0);
    }

    @Test
    @SneakyThrows
    public void testOrderDispatchedFlow_RetryableException() {

        stubWiremock("/api/stock?item=test-item", 503, "service unavailable",
                "failOne", STARTED, "succeedNextTime");
        stubWiremock("/api/stock?item=test-item", 200, "true",
                "failOne", "succeedNextTime", "succeedNextTime");

        OrderCreated orderCreated = TestEventData.buildOrderCreatedEvent(randomUUID(), "test-item");
        String key = randomUUID().toString();

        sendMessage(ORDER_CREATED_TOPIC, key, orderCreated);

        await().atMost(3, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.dispatchPreparingCounter::get, equalTo(1));

        await().atMost(1, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.orderDispatchedCounter::get, equalTo(1));

        await().atMost(1, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.dispatchCompletedCounter::get, equalTo(1));

        await().atMost(1, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.orderCreatedDLQCounter::get, equalTo(0));
    }


    @Test
    @SneakyThrows
    public void testOrderDispatchedFlow_RetryUntilFailure() {

        stubWiremock("/api/stock?item=test-item", 503, "service unavailable");
        OrderCreated orderCreated = TestEventData.buildOrderCreatedEvent(randomUUID(), "test-item");
        String key = randomUUID().toString();

        sendMessage(ORDER_CREATED_TOPIC, key, orderCreated);

        //TimeUnit.SECONDS.sleep(3);

        await().atMost(5, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.orderCreatedDLQCounter::get, equalTo(1));

        assertThat(testListener.dispatchPreparingCounter.get()).isEqualTo(0);
        assertThat(testListener.orderDispatchedCounter.get()).isEqualTo(0);
        assertThat(testListener.dispatchCompletedCounter.get()).isEqualTo(0);
    }


    @SneakyThrows
    private void sendMessage(String topic, String key, Object payload) {
        var message = MessageBuilder
                .withPayload(payload)
                .setHeader(KafkaHeaders.KEY, key)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .build();

        kafkaTemplate.send(message).get();
    }

}
