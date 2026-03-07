package ewm.comment.client;

import ewm.comment.exception.ServiceUnavailableException;
import ewm.event.client.EventClient;
import ewm.event.client.dto.InternalEventDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResilientEventClient {

    private final EventClient eventClient;

    @CircuitBreaker(name = "eventService", fallbackMethod = "getEventByIdFallback")
    @Retry(name = "eventService")
    public InternalEventDto getEventById(Long id) {
        return eventClient.getEventById(id);
    }

    public InternalEventDto getEventByIdFallback(Long id, Throwable t) {
        log.warn("event-service unavailable for eventId={}: {}", id, t.getMessage());
        throw new ServiceUnavailableException("Event service is temporarily unavailable", t);
    }
}
