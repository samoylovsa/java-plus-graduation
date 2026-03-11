package ewm.request.controller;

import ewm.request.client.dto.CountConfirmedRequestsByEventId;
import ewm.request.service.RequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/requests")
public class InternalRequestController {

    private final RequestService requestService;

    @GetMapping("/confirmed-count")
    public List<CountConfirmedRequestsByEventId> getConfirmedCounts(@RequestParam("eventIds") List<Long> eventIds) {
        return requestService.countConfirmedRequestsByEventIds(eventIds);
    }

    @GetMapping("/visited")
    public boolean hasUserVisitedEvent(@RequestParam("userId") Long userId,
                                       @RequestParam("eventId") Long eventId) {
        return requestService.userHasVisitedEvent(userId, eventId);
    }
}

