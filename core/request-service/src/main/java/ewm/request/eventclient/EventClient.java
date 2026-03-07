package ewm.request.eventclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "event-service")
public interface EventClient {

    @GetMapping("/internal/events/{id}")
    InternalEventDto getEventById(@PathVariable("id") Long id);
}

