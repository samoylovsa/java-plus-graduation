package ewm.event.client;

import ewm.event.client.dto.InternalEventDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "event-service", configuration = ewm.event.client.config.EventClientConfig.class)
public interface EventClient {

    @GetMapping("/internal/events/{id}")
    InternalEventDto getEventById(@PathVariable("id") Long id);
}