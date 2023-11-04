package dev.lydtech.dispatch.service;

import dev.lydtech.dispatch.message.DispatchPreparing;
import dev.lydtech.dispatch.message.OrderCreated;
import dev.lydtech.dispatch.message.OrderDispached;
import dev.lydtech.dispatch.util.TestEventData;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DispatchServiceTest {


    private DispatchService service;
    private KafkaTemplate kafkaProducerMock;

    @BeforeEach
    void setUp() {
        kafkaProducerMock = mock(KafkaTemplate.class);
        service = new DispatchService(kafkaProducerMock);
    }

    @Test
    @SneakyThrows
    void process_Sucess() {
        when(kafkaProducerMock.send(anyString(), any(OrderDispached.class))).thenReturn(mock(CompletableFuture.class));
        when(kafkaProducerMock.send(anyString(), any(DispatchPreparing.class))).thenReturn(mock(CompletableFuture.class));

        UUID id = randomUUID();

        OrderCreated testEvent = TestEventData.buildOrderCreatedEvent(id, id.toString());
        OrderDispached dispachedEvent = TestEventData.buildOrderDispatchedEvent(id);
        DispatchPreparing dispatchPreparingEvent = TestEventData.buildDispatchPreparingEvent(id);

        service.process(testEvent);

        verify(kafkaProducerMock, times(1)).send(eq("order.dispatched"), eq(dispachedEvent));
        verify(kafkaProducerMock, times(1)).send(eq("dispatch.tracking"), eq(dispatchPreparingEvent));
    }

    @Test
    @SneakyThrows
    void process_ProducerThrowsException() {
        OrderCreated testEvent = TestEventData.buildOrderCreatedEvent(randomUUID(), randomUUID().toString());
        doThrow(new RuntimeException("Producer failure")).when(kafkaProducerMock).send(eq("order.dispatched"), any(OrderDispached.class));

        Exception exception = assertThrows(RuntimeException.class, ()-> service.process(testEvent));

        verify(kafkaProducerMock, times(1)).send(eq("order.dispatched"), any(OrderDispached.class));
        assertThat(exception.getMessage()).isEqualTo("Producer failure");
    }
}