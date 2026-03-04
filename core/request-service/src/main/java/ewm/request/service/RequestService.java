package ewm.request.service;

import ewm.request.client.dto.CountConfirmedRequestsByEventId;
import ewm.request.dto.UpdateStatusRequestDtoReq;
import ewm.request.dto.UpdateStatusRequestDtoResp;
import ewm.request.dto.UserRequestDto;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface RequestService {

    List<UserRequestDto> getRequestsByUser(Long userId);

    UserRequestDto addRequest(Long userId, Long eventId);

    UserRequestDto cancelRequest(Long userId, Long requestId);

    UserRequestDto getRequestById(Long requestId);

    List<UserRequestDto> getRequestsByEventId(Long userId, Long eventId);

    @Transactional
    UpdateStatusRequestDtoResp updateRequestStatus(Long userId, Long eventId, UpdateStatusRequestDtoReq request);

    List<CountConfirmedRequestsByEventId> countConfirmedRequestsByEventIds(List<Long> eventIds);
}