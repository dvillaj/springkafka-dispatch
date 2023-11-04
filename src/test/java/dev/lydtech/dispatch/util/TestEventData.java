package dev.lydtech.dispatch.util;

import dev.lydtech.dispatch.message.DispatchPreparing;
import dev.lydtech.dispatch.message.OrderCreated;
import dev.lydtech.dispatch.message.OrderDispached;

import java.util.UUID;

public class TestEventData {

    public static OrderCreated buildOrderCreatedEvent(UUID orderId, String item) {

        return OrderCreated.builder()
                .orderId(orderId)
                .item(item)
                .build();
    }

    public static OrderDispached buildOrderDispatchedEvent(UUID orderId) {

        return OrderDispached.builder()
                .orderId(orderId)
                .build();
    }

    public static DispatchPreparing buildDispatchPreparingEvent(UUID orderId) {

        return DispatchPreparing.builder()
                .orderId(orderId)
                .build();
    }

}
