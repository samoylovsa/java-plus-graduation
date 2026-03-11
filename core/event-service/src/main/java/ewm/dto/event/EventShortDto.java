package ewm.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import ewm.dto.category.CategoryDto;
import ewm.user.client.dto.UserShortDto;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class EventShortDto {
    private Long id;
    private String title;
    private String annotation;
    private CategoryDto category;
    private Boolean paid;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;

    private UserShortDto initiator;
    private Integer confirmedRequests;
    private Double rating;
}
