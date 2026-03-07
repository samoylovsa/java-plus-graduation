package ewm.controller.event;

import ewm.model.event.EventState;
import lombok.Data;

@Data
public class InternalEventDto {

    private Long id;
    private Long initiatorId;
    private Integer participantLimit;
    private Boolean requestModeration;
    private EventState state;
}

