package ewm.request.controller;

import ewm.request.dto.UpdateStatusRequestDtoReq;
import ewm.request.dto.UpdateStatusRequestDtoResp;
import ewm.request.dto.UserRequestDto;
import ewm.request.service.RequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
public class EventRequestController {

    private final RequestService requestService;

    @GetMapping("/{eventId}/requests")
    public List<UserRequestDto> getRequestsByEventId(@PathVariable("userId") Long userId,
                                                     @PathVariable("eventId") Long eventId) {
        log.debug("Controller: getRequestsByEventId userId={}, eventId={}", userId, eventId);
        return requestService.getRequestsByEventId(userId, eventId);
    }

    @PatchMapping("/{eventId}/requests")
    public UpdateStatusRequestDtoResp updateRequestStatus(@PathVariable("userId") Long userId,
                                                          @PathVariable("eventId") Long eventId,
                                                          @RequestBody UpdateStatusRequestDtoReq request) {
        log.debug("Controller: updateRequestStatus userId={}, eventId={}, request={}", userId, eventId, request);
        return requestService.updateRequestStatus(userId, eventId, request);
    }
}

