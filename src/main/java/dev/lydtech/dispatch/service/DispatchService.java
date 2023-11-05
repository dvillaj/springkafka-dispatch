package dev.lydtech.dispatch.service;

import dev.lydtech.dispatch.message.DispatchPreparing;
import dev.lydtech.dispatch.message.OrderCreated;
import dev.lydtech.dispatch.message.OrderDispached;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static java.util.UUID.randomUUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchService {

    private static final String ORDER_DISPATCHED_TOPIC = "order.dispatched";
    private static final String DISPATCH_TRACKING_TOPIC = "dispatch.tracking";


    private static final UUID APPLICATION_ID = randomUUID();

    private final KafkaTemplate<String, Object> kafkaProducer;

    public void process(String key, OrderCreated orderCreated) throws Exception {
        DispatchPreparing dispatchPreparing = DispatchPreparing.builder()
                .orderId(orderCreated.getOrderId())
                .build();

        kafkaProducer.send(DISPATCH_TRACKING_TOPIC, key, dispatchPreparing).get();

        OrderDispached orderDispached = OrderDispached.builder()
                .orderId(orderCreated.getOrderId())
                .processedById(APPLICATION_ID)
                .notes("Dispatch: " + orderCreated.getItem())
                .build();

        kafkaProducer.send(ORDER_DISPATCHED_TOPIC, key, orderDispached).get();

        log.info("Sent message: key: {} - OrderId: {} -  processedById: {}", key, orderCreated.getOrderId(), APPLICATION_ID);

    }
}
