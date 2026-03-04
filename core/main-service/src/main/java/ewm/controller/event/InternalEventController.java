package ewm.controller.event;

import ewm.dto.event.EventFullDto;
import ewm.service.event.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/events")
public class InternalEventController {

    private final EventService eventService;

    @GetMapping("/{id}")
    public InternalEventDto getInternalEvent(@PathVariable("id") Long id) {
        EventFullDto event = eventService.getEventById(id);
        InternalEventDto dto = new InternalEventDto();
        dto.setId(event.getId());
        dto.setInitiatorId(event.getInitiator() != null ? event.getInitiator().getId() : null);
        dto.setParticipantLimit(event.getParticipantLimit());
        dto.setRequestModeration(event.getRequestModeration());
        dto.setState(event.getState());
        return dto;
    }
}