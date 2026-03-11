package ewm.request.service;

import ewm.request.client.dto.CountConfirmedRequestsByEventId;
import ewm.request.dto.UpdateStatusRequestDtoReq;
import ewm.request.dto.UpdateStatusRequestDtoResp;
import ewm.request.dto.UserRequestDto;
import ewm.event.client.dto.EventState;
import ewm.event.client.dto.InternalEventDto;
import ewm.request.client.ResilientEventClient;
import ewm.request.client.ResilientUserClient;
import ewm.request.mapper.RequestMapper;
import ewm.common.exception.ConflictException;
import ewm.common.exception.NotFoundException;
import ewm.common.exception.ValidationException;
import ewm.request.model.Request;
import ewm.request.model.RequestStatus;
import ewm.request.repository.RequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import stats.client.UserActionGrpcClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final RequestMapper requestMapper;
    private final ResilientUserClient userClient;
    private final ResilientEventClient eventClient;
    private final UserActionGrpcClient userActionGrpcClient;

    @Override
    public List<UserRequestDto> getRequestsByUser(Long userId) {
        userClient.getUserById(userId);
        return requestRepository.findAllByRequesterId(userId).stream()
                .map(requestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public UserRequestDto addRequest(Long userId, Long eventId) {
        userClient.getUserById(userId);
        InternalEventDto event = eventClient.getEventById(eventId);

        validateRequestCreation(userId, eventId, event);

        Request request = createRequest(userId, eventId, event);
        request = requestRepository.save(request);

        userActionGrpcClient.collectUserAction(
                userId,
                eventId,
                ActionTypeProto.ACTION_REGISTER,
                LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toInstant()
        );

        return requestMapper.toDto(request);
    }

    @Override
    public UserRequestDto cancelRequest(Long userId, Long requestId) {
        userClient.getUserById(userId);
        Request request = getRequestEntity(requestId);

        validateUserOwnsRequest(request, userId);

        request.setStatus(RequestStatus.CANCELED);
        request = requestRepository.save(request);
        return requestMapper.toDto(request);
    }

    @Override
    public UserRequestDto getRequestById(Long requestId) {
        return requestMapper.toDto(getRequestEntity(requestId));
    }

    @Override
    public List<UserRequestDto> getRequestsByEventId(Long userId, Long eventId) {
        InternalEventDto event = eventClient.getEventById(eventId);
        validateUserIsInitiator(event, userId);

        return requestRepository.findAllByEventId(eventId).stream()
                .map(requestMapper::toDto)
                .toList();
    }

    @Transactional
    @Override
    public UpdateStatusRequestDtoResp updateRequestStatus(Long userId, Long eventId, UpdateStatusRequestDtoReq request) {
        InternalEventDto event = eventClient.getEventById(eventId);
        validateUserIsInitiator(event, userId);
        validateStatusUpdateRequired(event);

        RequestStatus newStatus = request.getStatus();
        validateStatusValue(newStatus);

        List<Request> requestsForUpdate = getRequestsForUpdate(request.getRequestIds());
        validateRequestsForUpdate(requestsForUpdate, eventId);

        return processStatusUpdate(requestsForUpdate, event, newStatus);
    }

    @Override
    public List<CountConfirmedRequestsByEventId> countConfirmedRequestsByEventIds(List<Long> eventIds) {
        return requestRepository.countConfirmedRequestsByEventIds(eventIds);
    }

    @Override
    public boolean userHasVisitedEvent(Long userId, Long eventId) {
        List<Request> requests = requestRepository.findAllByEventIdAndRequesterId(eventId, userId);
        return requests.stream().anyMatch(r -> r.getStatus() == RequestStatus.CONFIRMED);
    }

    private void validateRequestCreation(Long userId, Long eventId, InternalEventDto event) {
        if (!requestRepository.findAllByEventIdAndRequesterId(eventId, userId).isEmpty()) {
            throw new ConflictException("Такой запрос уже существует");
        }

        if (event.getInitiatorId().equals(userId)) {
            throw new ConflictException("Инициатор не может отправить запрос на участие в своём событии");
        }

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Событие неопубликованно, нельзя добавить запрос");
        }

        if (!eventHasFreeSlot(event)) {
            throw new ConflictException("Нет свободных мест для участия в событии с id = " + eventId);
        }
    }

    private Request createRequest(Long userId, Long eventId, InternalEventDto event) {
        Request request = new Request();
        request.setRequesterId(userId);
        request.setEventId(eventId);
        request.setCreated(LocalDateTime.now());

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
        } else {
            request.setStatus(RequestStatus.PENDING);
        }

        return request;
    }

    private Request getRequestEntity(Long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос с id = " + requestId + " не найден"));
    }

    private void validateUserOwnsRequest(Request request, Long userId) {
        if (!request.getRequesterId().equals(userId)) {
            throw new ConflictException("Запрос с id = " + request.getId() + " не принадлежит пользователю с id = " + userId);
        }
    }

    private void validateUserIsInitiator(InternalEventDto event, Long userId) {
        if (!event.getInitiatorId().equals(userId)) {
            throw new ValidationException("Пользователь с id = " + userId + " не является инициатором события");
        }
    }

    private void validateStatusUpdateRequired(InternalEventDto event) {
        if (Boolean.FALSE.equals(event.getRequestModeration()) || event.getParticipantLimit() == 0) {
            throw new ValidationException("Для данного события подтверждение заявок не требуется");
        }
    }

    private void validateStatusValue(RequestStatus status) {
        if (status != RequestStatus.CONFIRMED && status != RequestStatus.REJECTED) {
            throw new ValidationException("Устанавливать можно только статусы CONFIRMED или REJECTED");
        }
    }

    private List<Request> getRequestsForUpdate(List<Long> requestIds) {
        List<Request> requests = requestRepository.findAllById(requestIds);
        if (requests.size() < requestIds.size()) {
            throw new NotFoundException("Вы пытаетесь обновить запрос(ы) которых не существует");
        }
        return requests;
    }

    private void validateRequestsForUpdate(List<Request> requests, Long eventId) {
        for (Request req : requests) {
            if (req.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Можно изменять только запросы в статусе PENDING");
            }

            if (!req.getEventId().equals(eventId)) {
                throw new ConflictException("Запрос с id = " + req.getId() + " не относится к событию с id = " + eventId);
            }
        }
    }

    private UpdateStatusRequestDtoResp processStatusUpdate(List<Request> requests, InternalEventDto event, RequestStatus newStatus) {
        List<UserRequestDto> confirmedRequests = new ArrayList<>();
        List<UserRequestDto> rejectedRequests = new ArrayList<>();

        if (newStatus == RequestStatus.CONFIRMED) {
            processConfirmedRequests(requests, event, confirmedRequests, rejectedRequests);
        } else {
            processRejectedRequests(requests, rejectedRequests);
        }

        requestRepository.saveAll(requests);
        return requestMapper.toUpdateStatusRequestDtoResp(confirmedRequests, rejectedRequests);
    }

    private void processConfirmedRequests(List<Request> requests, InternalEventDto event,
                                          List<UserRequestDto> confirmedRequests, List<UserRequestDto> rejectedRequests) {
        int currentConfirmedCount = requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
        int availableSlots = event.getParticipantLimit() - currentConfirmedCount;

        if (availableSlots <= 0) {
            throw new ConflictException("Свободных мест больше нет");
        }

        int confirmedCount = 0;
        for (Request req : requests) {
            if (confirmedCount < availableSlots) {
                req.setStatus(RequestStatus.CONFIRMED);
                confirmedRequests.add(requestMapper.toDto(req));
                confirmedCount++;
            } else {
                req.setStatus(RequestStatus.REJECTED);
                rejectedRequests.add(requestMapper.toDto(req));
            }
        }

        if (currentConfirmedCount + confirmedCount >= event.getParticipantLimit()) {
            rejectPendingRequests(event.getId(), rejectedRequests);
        }
    }

    private void processRejectedRequests(List<Request> requests, List<UserRequestDto> rejectedRequests) {
        for (Request req : requests) {
            req.setStatus(RequestStatus.REJECTED);
            rejectedRequests.add(requestMapper.toDto(req));
        }
    }

    private void rejectPendingRequests(Long eventId, List<UserRequestDto> rejectedRequests) {
        List<Request> pendingRequests = requestRepository.findAllByEventIdAndStatus(eventId, RequestStatus.PENDING);

        for (Request pendingReq : pendingRequests) {
            pendingReq.setStatus(RequestStatus.REJECTED);
            rejectedRequests.add(requestMapper.toDto(pendingReq));
        }

        if (!pendingRequests.isEmpty()) {
            requestRepository.saveAll(pendingRequests);
        }
    }

    private boolean eventHasFreeSlot(InternalEventDto event) {
        Integer limit = event.getParticipantLimit();
        if (limit == null || limit == 0) return true;
        int confirmed = requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
        return confirmed < limit;
    }
}

