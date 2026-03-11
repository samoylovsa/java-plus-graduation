package ewm.request.client;

import ewm.request.client.dto.CountConfirmedRequestsByEventId;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "request-service", configuration = ewm.request.client.config.RequestClientConfig.class)
public interface RequestClient {

    @GetMapping("/internal/requests/confirmed-count")
    List<CountConfirmedRequestsByEventId> countConfirmedRequestsByEventIds(
            @RequestParam("eventIds") List<Long> eventIds
    );

    @GetMapping("/internal/requests/visited")
    boolean hasUserVisitedEvent(@RequestParam("userId") Long userId,
                                @RequestParam("eventId") Long eventId);
}

