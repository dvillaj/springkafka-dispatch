package dev.lydtech.dispatch.handler;

import dev.lydtech.dispatch.exception.NotRetryableException;
import dev.lydtech.dispatch.exception.RetryableException;
import dev.lydtech.dispatch.message.OrderCreated;
import dev.lydtech.dispatch.service.DispatchService;
import dev.lydtech.dispatch.util.TestEventData;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static java.util.UUID.randomUUID;

class OrderCreatedHandlerTest {

    private OrderCreatedHandler handler;
    private DispatchService dispatchServiceMock;

    @BeforeEach
    void setUp() {
        dispatchServiceMock = mock(DispatchService.class);
        handler = new OrderCreatedHandler(dispatchServiceMock);
    }

    @Test
    @SneakyThrows
    void listen_Success()  {
        String key = randomUUID().toString();

        OrderCreated testEvent = TestEventData.buildOrderCreatedEvent(randomUUID(), randomUUID().toString());
        handler.listen(0, key, testEvent);
        verify(dispatchServiceMock, times(1)).process(key, testEvent);
    }

    @Test
    @SneakyThrows
    void listen_ServiceThrowsNotRetryableException()  {
        String key = randomUUID().toString();

        OrderCreated testEvent = TestEventData.buildOrderCreatedEvent(randomUUID(), randomUUID().toString());
        doThrow(new RuntimeException("Service failure")).when(dispatchServiceMock).process(key, testEvent);

        Exception exception = assertThrows(NotRetryableException.class, () -> handler.listen(0, key, testEvent));
        assertThat(exception.getMessage()).isEqualTo("java.lang.RuntimeException: Service failure");
        verify(dispatchServiceMock, times(1)).process(key, testEvent);
    }


    @Test
    @SneakyThrows
    void listen_ServiceThrowsRetryableException()  {
        String key = randomUUID().toString();

        OrderCreated testEvent = TestEventData.buildOrderCreatedEvent(randomUUID(), randomUUID().toString());
        doThrow(new RetryableException("retry failure")).when(dispatchServiceMock).process(key, testEvent);

        Exception exception = assertThrows(RetryableException.class, () -> handler.listen(0, key, testEvent));
        assertThat(exception.getMessage()).isEqualTo("retry failure");
        verify(dispatchServiceMock, times(1)).process(key, testEvent);
    }
}