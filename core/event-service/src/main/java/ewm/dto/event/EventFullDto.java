package ewm.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import ewm.dto.category.CategoryDto;
import ewm.user.client.dto.UserShortDto;
import ewm.model.event.EventState;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class EventFullDto {

    private Long id;
    private String title;
    private String annotation;
    private String description;

    private CategoryDto category;

    private Boolean paid;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdOn;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishedOn;

    private Integer participantLimit;
    private Boolean requestModeration;

    private EventState state;

    private UserShortDto initiator;

    private LocationDto location;

    private Integer confirmedRequests;

    private Double rating;
}
