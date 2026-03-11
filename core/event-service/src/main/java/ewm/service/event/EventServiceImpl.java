package ewm.service.event;

import ewm.dto.event.*;
import ewm.client.ResilientRequestClient;
import ewm.client.ResilientUserClient;
import ewm.request.client.dto.CountConfirmedRequestsByEventId;
import ewm.common.exception.BusinessRuleException;
import ewm.common.exception.ConflictException;
import ewm.common.exception.NotFoundException;
import ewm.common.exception.ValidationException;
import ewm.mapper.event.EventMapper;
import ewm.model.category.Category;
import ewm.user.client.dto.UserShortDto;
import ewm.model.event.Event;
import ewm.model.event.EventState;
import ewm.repository.category.CategoryRepository;
import ewm.repository.event.EventRepository;
import ewm.repository.event.SearchEventSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import stats.client.RecommendationsGrpcClient;
import stats.client.UserActionGrpcClient;
import ewm.user.client.dto.UserDto;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final ResilientUserClient userClient;
    private final CategoryRepository categoryRepository;
    private final EventMapper eventMapper;
    private final ResilientRequestClient requestClient;
    private final UserActionGrpcClient userActionGrpcClient;
    private final RecommendationsGrpcClient recommendationsGrpcClient;

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        UserDto user = userClient.getUserById(userId);
        Category category = getCategoryEntity(newEventDto.getCategory());
        validateEventDate(newEventDto.getEventDate());

        Event event = eventMapper.toEntity(newEventDto, userId, category);
        event = eventRepository.save(event);
        UserShortDto initiator = toUserShortDto(user);
        return eventMapper.toFullDto(event, 0, 0.0, initiator);
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id"));
        Page<Event> page = eventRepository.findAllByInitiatorId(userId, pageable);

        List<Event> events = page.getContent();
        if (events.isEmpty()) return List.of();

        return getEventsShortDtoWithStats(events);
    }

    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest) {
        Event event = getEventEntity(eventId);

        validateUserIsInitiator(event, userId);
        validateEventNotPublished(event);

        if (updateEventUserRequest.getEventDate() != null) {
            validateEventDate(updateEventUserRequest.getEventDate());
        }

        updateEventFields(event, updateEventUserRequest);

        Event updatedEvent = eventRepository.save(event);
        return getEventFullDtoWithStats(updatedEvent);
    }

    @Override
    public EventFullDto getUserEvent(Long userId, Long eventId, String ip) {
        Event event = getEventEntity(eventId);
        validateUserIsInitiator(event, userId);

        return getEventFullDtoWithStats(event);
    }

    @Override
    public EventFullDto getEventById(Long eventId) {
        Event event = getEventEntity(eventId);
        return getEventFullDtoWithStats(event);
    }

    @Override
    public List<EventFullDto> getEventsAdmin(GetEventAdminRequest request, Pageable pageable) {
        validateRangeStartAndEnd(request.getRangeStart(), request.getRangeEnd());

        Specification<Event> specification = buildAdminSpecification(request);
        List<Event> events = eventRepository.findAll(specification, pageable).getContent();

        if (events.isEmpty()) return List.of();

        return getEventsFullDtoWithStats(events);
    }

    @Override
    @Transactional
    public EventFullDto updateEventAdmin(Long eventId, UpdateEventAdminRequest request) {
        Event event = getEventEntity(eventId);

        if (request.getStateAction() != null) {
            handleAdminStateAction(event, request.getStateAction());
        }

        updateEventFields(event, request);

        Event updatedEvent = eventRepository.save(event);
        return getEventFullDtoWithStats(updatedEvent);
    }

    @Override
    public List<EventShortDto> getEventsPublic(GetEventPublicRequest request, Pageable pageable) {
        validateRangeStartAndEnd(request.getRangeStart(), request.getRangeEnd());

        Specification<Event> specification = buildPublicSpecification(request);
        List<Event> events = eventRepository.findAll(specification, pageable).getContent();

        if (events.isEmpty()) return List.of();

        Map<Long, Integer> confirmedRequestsForStats = null;
        if (request.getOnlyAvailable()) {
            List<Long> eventIds = events.stream().map(Event::getId).toList();
            Map<Long, Integer> confirmed = getConfirmedRequests(eventIds);
            events = events.stream()
                    .filter(event -> {
                        Integer limit = event.getParticipantLimit();
                        if (limit == null || limit == 0) return true;
                        int confirmedCount = confirmed.getOrDefault(event.getId(), 0);
                        return confirmedCount < limit;
                    })
                    .toList();
            if (events.isEmpty()) {
                return List.of();
            }
            confirmedRequestsForStats = confirmed;
        }

        List<EventShortDto> result = getEventsShortDtoWithStats(events, confirmedRequestsForStats);
        return sortEvents(result, request.getSort());
    }

    @Override
    public EventFullDto getEventByIdPublic(Long eventId, Long userId) {
        Event event = getPublishedEventEntity(eventId);
        userActionGrpcClient.collectUserAction(userId, eventId, ActionTypeProto.ACTION_VIEW, Instant.now());
        return getEventFullDtoWithStats(event);
    }

    @Override
    public List<EventShortDto> getRecommendedEvents(Long userId) {
        List<RecommendedEventProto> recommendations = recommendationsGrpcClient.getRecommendationsForUser(userId, 20);
        if (recommendations.isEmpty()) {
            return List.of();
        }
        List<Long> eventIds = recommendations.stream()
                .map(RecommendedEventProto::getEventId)
                .toList();
        Map<Long, Double> ratingByEventId = recommendations.stream()
                .collect(Collectors.toMap(RecommendedEventProto::getEventId, RecommendedEventProto::getScore));
        List<Event> events = eventRepository.findAllById(eventIds);
        if (events.isEmpty()) {
            return List.of();
        }
        List<EventShortDto> dtos = getEventsShortDtoWithStats(events);
        return dtos.stream()
                .peek(dto -> dto.setRating(ratingByEventId.getOrDefault(dto.getId(), dto.getRating() != null ? dto.getRating() : 0.0)))
                .toList();
    }

    @Override
    @Transactional
    public void likeEvent(Long userId, Long eventId) {
        getPublishedEventEntity(eventId);
        boolean visited = requestClient.userHasVisitedEvent(userId, eventId);
        if (!visited) {
            throw new BusinessRuleException("Пользователь может лайкать только посещённые им мероприятия");
        }
        userActionGrpcClient.collectUserAction(userId, eventId, ActionTypeProto.ACTION_LIKE, Instant.now());
    }

    private void validateRangeStartAndEnd(LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd))
            throw new BusinessRuleException("Дата начала не может быть позже даты окончания");
    }

    private Map<Long, Integer> getConfirmedRequests(List<Long> eventIds) {
        if (eventIds.isEmpty()) return Map.of();

        List<CountConfirmedRequestsByEventId> events = requestClient.countConfirmedRequestsByEventIds(eventIds);
        Map<Long, Integer> confirmedRequests = eventIds.stream()
                .collect(Collectors.toMap(id -> id, id -> 0));

        events.forEach(dto -> confirmedRequests.put(dto.getEventId(), dto.getCountConfirmedRequests()));

        return confirmedRequests;
    }

    private Map<Long, Double> getRatings(List<Long> eventIds) {
        if (eventIds.isEmpty()) return Map.of();

        List<RecommendedEventProto> responses = recommendationsGrpcClient.getInteractionsCount(eventIds);

        Map<Long, Double> ratings = eventIds.stream()
                .collect(Collectors.toMap(id -> id, id -> 0.0));

        if (responses != null && !responses.isEmpty()) {
            responses.forEach(resp -> ratings.put(resp.getEventId(), resp.getScore()));
        }
        return ratings;
    }

    private Category getCategoryEntity(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Category with id=%d was not found", catId)));
    }

    private UserShortDto toUserShortDto(UserDto dto) {
        UserShortDto shortDto = new UserShortDto();
        shortDto.setId(dto.getId());
        shortDto.setName(dto.getName());
        return shortDto;
    }

    private void validateEventDate(LocalDateTime eventDate) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BusinessRuleException(String.format("Field: eventDate. Error: должно содержать дату, " +
                    "которая еще не наступила. Value:%s", eventDate));
        }
    }

    private Event getEventEntity(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Event with id=%d not found", eventId)));
    }

    private void validateUserIsInitiator(Event event, Long userId) {
        if (!event.getInitiatorId().equals(userId)) {
            throw new BusinessRuleException(
                    String.format("User with id=%d is not the initiator of this event", userId));
        }
    }

    private void validateEventNotPublished(Event event) {
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Event must not be published");
        }
    }

    private void updateEventFields(Event event, UpdateEventUserRequest updateRequest) {
        if (updateRequest.getTitle() != null)
            event.setTitle(updateRequest.getTitle());
        if (updateRequest.getDescription() != null)
            event.setDescription(updateRequest.getDescription());
        if (updateRequest.getAnnotation() != null)
            event.setAnnotation(updateRequest.getAnnotation());
        if (updateRequest.getCategory() != null) {
            Category category = getCategoryEntity(updateRequest.getCategory());
            event.setCategory(category);
        }
        if (updateRequest.getEventDate() != null)
            event.setEventDate(updateRequest.getEventDate());
        if (updateRequest.getPaid() != null)
            event.setPaid(updateRequest.getPaid());
        if (updateRequest.getParticipantLimit() != null)
            event.setParticipantLimit(updateRequest.getParticipantLimit());
        if (updateRequest.getRequestModeration() != null)
            event.setRequestModeration(updateRequest.getRequestModeration());
        if (updateRequest.getLocation() != null) {
            event.getLocation().setLat(updateRequest.getLocation().getLat());
            event.getLocation().setLon(updateRequest.getLocation().getLon());
        }
        if (updateRequest.getStateAction() != null) {
            handleStateAction(event, updateRequest.getStateAction());
        }
    }

    private void handleStateAction(Event event, StateAction stateAction) {
        switch (stateAction) {
            case CANCEL_REVIEW:
                if (event.getState() == EventState.PENDING) {
                    event.setState(EventState.CANCELED);
                } else {
                    throw new BusinessRuleException("Only pending events can be canceled");
                }
                break;
            case SEND_TO_REVIEW:
                event.setState(EventState.PENDING);
                break;
        }
    }

    private EventFullDto getEventFullDtoWithStats(Event event) {
        List<Long> searchEventIds = List.of(event.getId());
        Map<Long, Integer> confirmedRequestsCount = getConfirmedRequests(searchEventIds);
        Map<Long, Double> ratings = getRatings(searchEventIds);
        UserShortDto initiator = toUserShortDto(userClient.getUserById(event.getInitiatorId()));
        return eventMapper.toFullDto(event,
                confirmedRequestsCount.getOrDefault(event.getId(), 0),
                ratings.getOrDefault(event.getId(), 0.0),
                initiator);
    }

    private Specification<Event> buildAdminSpecification(GetEventAdminRequest request) {
        Specification<Event> specification = SearchEventSpecifications.addWhereNull();

        if (request.getUsers() != null && !request.getUsers().isEmpty())
            specification = specification.and(SearchEventSpecifications.addWhereUsers(request.getUsers()));
        if (request.getStates() != null && !request.getStates().isEmpty())
            specification = specification.and(SearchEventSpecifications.addWhereStates(request.getStates()));
        if (request.getCategories() != null && !request.getCategories().isEmpty())
            specification = specification.and(SearchEventSpecifications.addWhereCategories(request.getCategories()));
        if (request.getRangeStart() != null)
            specification = specification.and(SearchEventSpecifications.addWhereStartsBefore(request.getRangeStart()));
        if (request.getRangeEnd() != null)
            specification = specification.and(SearchEventSpecifications.addWhereEndsAfter(request.getRangeEnd()));
        if (request.getRangeStart() == null && request.getRangeEnd() == null)
            specification = specification.and(SearchEventSpecifications.addWhereStartsBefore(LocalDateTime.now()));

        return specification;
    }

    private List<EventFullDto> getEventsFullDtoWithStats(List<Event> events) {
        List<Long> eventIds = events.stream().map(Event::getId).toList();
        List<Long> initiatorIds = events.stream().map(Event::getInitiatorId).distinct().toList();
        List<UserDto> initiatorDtos = initiatorIds.isEmpty() ? List.of() : userClient.getUsersByIds(initiatorIds);
        if (initiatorDtos == null) initiatorDtos = Collections.emptyList();
        Map<Long, UserShortDto> initiatorMap = initiatorDtos.stream()
                .collect(Collectors.toMap(UserDto::getId, this::toUserShortDto));
        Map<Long, UserShortDto> finalInitiatorMap = initiatorMap;
        Map<Long, Integer> confirmedRequestsCount = getConfirmedRequests(eventIds);
        Map<Long, Double> ratings = getRatings(eventIds);

        return events.stream()
                .map(event -> eventMapper.toFullDto(
                        event,
                        confirmedRequestsCount.getOrDefault(event.getId(), 0),
                        ratings.getOrDefault(event.getId(), 0.0),
                        finalInitiatorMap.getOrDefault(event.getInitiatorId(), toUnknownUserShortDto(event.getInitiatorId()))
                ))
                .toList();
    }

    private UserShortDto toUnknownUserShortDto(Long id) {
        UserShortDto dto = new UserShortDto();
        dto.setId(id);
        dto.setName("Unknown");
        return dto;
    }

    private void handleAdminStateAction(Event event, StateAdminAction stateAction) {
        EventState currentState = event.getState();

        if (stateAction == StateAdminAction.PUBLISH_EVENT) {
            validateEventCanBePublished(event, currentState);
            event.setState(EventState.PUBLISHED);
            event.setPublishedOn(LocalDateTime.now());
        } else if (stateAction == StateAdminAction.REJECT_EVENT) {
            validateEventCanBeRejected(currentState);
            event.setState(EventState.CANCELED);
        }
    }

    private void validateEventCanBePublished(Event event, EventState currentState) {
        if (currentState != EventState.PENDING) {
            throw new ConflictException("Событие можно опубликовать только если оно в состоянии ожидания публикации");
        }
        if (event.getEventDate() != null && event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new ValidationException("Дата начала события не может быть ранее чем через 1 часа(ов)");
        }
    }

    private void validateEventCanBeRejected(EventState currentState) {
        if (currentState == EventState.PUBLISHED) {
            throw new ConflictException("Событие можно отклонить пока оно не опубликовано");
        }
    }

    private void updateEventFields(Event event, UpdateEventAdminRequest request) {
        if (request.getAnnotation() != null)
            event.setAnnotation(request.getAnnotation());
        if (request.getPaid() != null)
            event.setPaid(request.getPaid());
        if (request.getEventDate() != null)
            event.setEventDate(request.getEventDate());
        if (request.getDescription() != null)
            event.setDescription(request.getDescription());
        if (request.getTitle() != null)
            event.setTitle(request.getTitle());
        if (request.getParticipantLimit() != null)
            event.setParticipantLimit(request.getParticipantLimit());
        if (request.getCategory() != null) {
            Category category = getCategoryEntity(request.getCategory());
            event.setCategory(category);
        }
    }

    private Specification<Event> buildPublicSpecification(GetEventPublicRequest request) {
        Specification<Event> specification = SearchEventSpecifications.addWhereNull();

        if (request.getText() != null && !request.getText().trim().isEmpty()) {
            specification = specification.and(SearchEventSpecifications.addLikeText(request.getText()));
        }
        if (request.getCategories() != null && !request.getCategories().isEmpty()) {
            specification = specification.and(SearchEventSpecifications.addWhereCategories(request.getCategories()));
        }
        if (request.getPaid() != null) {
            specification = specification.and(SearchEventSpecifications.isPaid(request.getPaid()));
        }

        LocalDateTime rangeStart = (request.getRangeStart() == null && request.getRangeEnd() == null) ?
                LocalDateTime.now() : request.getRangeStart();
        if (rangeStart != null) {
            specification = specification.and(SearchEventSpecifications.addWhereStartsBefore(rangeStart));
        }

        if (request.getRangeEnd() != null) {
            specification = specification.and(SearchEventSpecifications.addWhereEndsAfter(request.getRangeEnd()));
        }

        return specification;
    }

    private List<EventShortDto> sortEvents(List<EventShortDto> events, SortState sort) {
        if (sort == null) return events;

        switch (sort) {
            case VIEWS:
                return events.stream()
                        .sorted(Comparator.comparing(EventShortDto::getRating, Comparator.nullsLast(Double::compareTo)).reversed())
                        .toList();
            case EVENT_DATE:
                return events.stream()
                        .sorted(Comparator.comparing(EventShortDto::getEventDate))
                        .toList();
            default:
                return events;
        }
    }

    private List<EventShortDto> getEventsShortDtoWithStats(List<Event> events) {
        return getEventsShortDtoWithStats(events, null);
    }

    private List<EventShortDto> getEventsShortDtoWithStats(List<Event> events, Map<Long, Integer> confirmedRequestsCountOrNull) {
        List<Long> eventIds = events.stream().map(Event::getId).toList();
        List<Long> initiatorIds = events.stream().map(Event::getInitiatorId).distinct().toList();
        List<UserDto> initiatorDtos = initiatorIds.isEmpty() ? List.of() : userClient.getUsersByIds(initiatorIds);
        if (initiatorDtos == null) initiatorDtos = Collections.emptyList();
        Map<Long, UserShortDto> initiatorMap = initiatorDtos.stream()
                .collect(Collectors.toMap(UserDto::getId, this::toUserShortDto));
        Map<Long, Integer> confirmedRequestsCount = confirmedRequestsCountOrNull != null
                ? confirmedRequestsCountOrNull
                : getConfirmedRequests(eventIds);
        Map<Long, Double> ratings = getRatings(eventIds);

        return events.stream()
                .map(event -> eventMapper.toShortDto(
                        event,
                        confirmedRequestsCount.getOrDefault(event.getId(), 0),
                        ratings.getOrDefault(event.getId(), 0.0),
                        initiatorMap.getOrDefault(event.getInitiatorId(), toUnknownUserShortDto(event.getInitiatorId()))
                ))
                .toList();
    }

    private Event getPublishedEventEntity(Long eventId) {
        return eventRepository.findById(eventId)
                .filter(ev -> ev.getState() == EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Событие c id " + eventId + " не найдено"));
    }
}