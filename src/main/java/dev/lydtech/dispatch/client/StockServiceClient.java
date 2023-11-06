package dev.lydtech.dispatch.client;

import dev.lydtech.dispatch.exception.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class StockServiceClient {

    private final RestTemplate restTemplate;

    private final String stockServiceEndPoint;


    public StockServiceClient(@Autowired RestTemplate restTemplate,
                              @Value("${dispatch.stockServiceEndpoint}") String stockServiceEndpoint) {
        this.restTemplate = restTemplate;
        this.stockServiceEndPoint = stockServiceEndpoint;
    }

    /**
     * The Stock service returns true if item is available, false otherwise
     */
    public String checkAvailable(String item) {
        try {
            String url = stockServiceEndPoint + "?item=" + item;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            var httpResponseCode = response.getStatusCode();
            if (httpResponseCode.value() != 200) {
                throw new RuntimeException(String.format("error %s", httpResponseCode.value()));
            }
            return response.getBody();
        } catch (HttpServerErrorException | ResourceAccessException e) {
            throw new RetryableException(e);
        } catch (Exception e) {
            log.error("Exception thrown {}", e.getClass().getName(), e);
            throw e;
        }
    }

}
