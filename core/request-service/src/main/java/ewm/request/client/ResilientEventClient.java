package ewm.request.client;

import ewm.event.client.EventClient;
import ewm.event.client.dto.InternalEventDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResilientEventClient {

    private final EventClient eventClient;

    @CircuitBreaker(name = "eventService")
    @Retry(name = "eventService")
    public InternalEventDto getEventById(Long id) {
        return eventClient.getEventById(id);
    }
}
