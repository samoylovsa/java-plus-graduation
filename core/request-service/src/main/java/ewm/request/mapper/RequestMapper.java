package ewm.request.mapper;

import ewm.request.dto.UpdateStatusRequestDtoResp;
import ewm.request.dto.UserRequestDto;
import ewm.request.model.Request;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RequestMapper {

    public UserRequestDto toDto(Request request) {
        return new UserRequestDto(
                request.getId(),
                request.getCreated(),
                request.getEventId(),
                request.getRequesterId(),
                request.getStatus()
        );
    }

    public UpdateStatusRequestDtoResp toUpdateStatusRequestDtoResp(List<UserRequestDto> confirmedRequests,
                                                                   List<UserRequestDto> rejectedRequests) {
        return new UpdateStatusRequestDtoResp(
                confirmedRequests,
                rejectedRequests
        );
    }
}

