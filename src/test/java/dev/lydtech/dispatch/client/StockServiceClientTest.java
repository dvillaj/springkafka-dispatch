package dev.lydtech.dispatch.client;

import dev.lydtech.dispatch.exception.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static javax.management.Query.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class StockServiceClientTest {

    private RestTemplate restTemplateMock;

    private StockServiceClient client;

    private final String STOCK_SERVICE_ENDPOINT = "endpoint";

    private final String STOCK_SERVICE_ITEM = "my-item";
    private final String STOCK_SERVICE_QUERY = STOCK_SERVICE_ENDPOINT + "?item=" + STOCK_SERVICE_ITEM;

    @BeforeEach
    public void setUp() {
        restTemplateMock = mock(RestTemplate.class);
        client = new StockServiceClient(restTemplateMock, "endpoint");
    }

    @Test
    public void testCheckAvailability_Success() {
        ResponseEntity<String> response = new ResponseEntity<>("true", HttpStatusCode.valueOf(200));

        when(restTemplateMock.getForEntity(STOCK_SERVICE_QUERY, String.class)).thenReturn(response);
        assertThat(client.checkAvailable(STOCK_SERVICE_ITEM)).isEqualTo("true");
        verify(restTemplateMock, times(1)).getForEntity(STOCK_SERVICE_QUERY, String.class);
    }

    @Test
    public void testCheckAvailability_ServerError() {
        doThrow(new HttpServerErrorException(HttpStatusCode.valueOf(500)))
                .when(restTemplateMock).getForEntity(STOCK_SERVICE_QUERY, String.class);

        Exception exception = assertThrows(RetryableException.class, ()-> client.checkAvailable(STOCK_SERVICE_ITEM));
        verify(restTemplateMock, times(1)).getForEntity(STOCK_SERVICE_QUERY, String.class);
        assertThat(exception.getMessage()).isEqualTo("org.springframework.web.client.HttpServerErrorException: 500 INTERNAL_SERVER_ERROR");
    }

    @Test
    public void testCheckAvailability_ResourceAddressException() {
        doThrow(new ResourceAccessException("access exception"))
                .when(restTemplateMock).getForEntity(STOCK_SERVICE_QUERY, String.class);

        Exception exception = assertThrows(RetryableException.class, ()-> client.checkAvailable(STOCK_SERVICE_ITEM));
        verify(restTemplateMock, times(1)).getForEntity(STOCK_SERVICE_QUERY, String.class);
        assertThat(exception.getMessage()).isEqualTo("org.springframework.web.client.ResourceAccessException: access exception");
    }

    @Test
    public void testCheckAvailability_RuntimeException() {
        doThrow(new RuntimeException("runtime exception"))
                .when(restTemplateMock).getForEntity(STOCK_SERVICE_QUERY, String.class);

        Exception exception = assertThrows(Exception.class, ()-> client.checkAvailable(STOCK_SERVICE_ITEM));
        verify(restTemplateMock, times(1)).getForEntity(STOCK_SERVICE_QUERY, String.class);
        assertThat(exception.getMessage()).isEqualTo("runtime exception");
    }
}
