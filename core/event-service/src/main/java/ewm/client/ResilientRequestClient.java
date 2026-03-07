package ewm.client;

import ewm.request.client.RequestClient;
import ewm.request.client.dto.CountConfirmedRequestsByEventId;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class ResilientRequestClient {

    private final RequestClient requestClient;

    @CircuitBreaker(name = "requestService", fallbackMethod = "countConfirmedRequestsByEventIdsFallback")
    @Retry(name = "requestService")
    public List<CountConfirmedRequestsByEventId> countConfirmedRequestsByEventIds(List<Long> eventIds) {
        return requestClient.countConfirmedRequestsByEventIds(eventIds);
    }

    public List<CountConfirmedRequestsByEventId> countConfirmedRequestsByEventIdsFallback(List<Long> eventIds, Throwable t) {
        log.warn("request-service unavailable, using fallback (0 confirmed): {}", t.getMessage());
        return List.of();
    }
}
