package ewm.service.compilation;

import ewm.dto.compilation.CompilationDto;
import ewm.dto.compilation.CreateCompilationDto;
import ewm.dto.compilation.UpdateCompilationDto;
import ewm.dto.event.EventShortDto;
import ewm.dto.user.UserShortDto;
import ewm.exception.ConflictException;
import ewm.exception.NotFoundException;
import ewm.mapper.compilation.CompilationMapper;
import ewm.mapper.event.EventMapper;
import ewm.model.compilation.Compilation;
import ewm.model.event.Event;
import ewm.repository.compilation.CompilationRepository;
import ewm.repository.event.EventRepository;
import ewm.user.client.UserClient;
import ewm.user.client.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;
    private final EventMapper eventMapper;
    private final UserClient userClient;

    @Override
    @Transactional
    public CompilationDto createCompilation(CreateCompilationDto request) {
        if (compilationRepository.existsByTitle(request.getTitle()))
            throw new ConflictException("Подборка с таким названием (" + request.getTitle() + ") уже существует");

        Compilation compilation = compilationMapper.toEntity(request);

        if (request.getEvents() != null && !request.getEvents().isEmpty()) {
            List<Event> events = eventRepository.findAllById(new ArrayList<>(request.getEvents()));

            if (events.size() != request.getEvents().size()) throw new NotFoundException("Не все события найдены");
            compilation.setEvents(new HashSet<>(events));

        } else compilation.setEvents(new HashSet<>());
        Compilation savedCompilation = compilationRepository.save(compilation);
        HashSet<EventShortDto> eventShortDtos = new HashSet<>(eventsToShortDtosWithZeroStats(savedCompilation.getEvents()));
        return compilationMapper.toDto(savedCompilation, eventShortDtos);
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        if (!compilationRepository.existsById(compId))
            throw new NotFoundException("Подборка с идентификатором " + compId + " не найдена");
        compilationRepository.deleteById(compId);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationDto request) {
        Compilation compilation = getCompilationById(compId);

        if (request.getTitle() != null
                && !request.getTitle().equals(compilation.getTitle())
                && compilationRepository.existsByTitle(request.getTitle())) {
            throw new ConflictException("Подборка с названием \"" + request.getTitle() + "\" уже существует");
        }

        if (request.getEvents() != null) {
            if (request.getEvents().isEmpty()) {
                compilation.setEvents(new HashSet<>());
            } else {
                List<Event> events = eventRepository.findAllById(new HashSet<>(request.getEvents()));
                if (events.size() != request.getEvents().size()) {
                    throw new NotFoundException("Некоторые события не найдены");
                }
                compilation.setEvents(new HashSet<>(events));
            }
        }
        HashSet<EventShortDto> eventShortDtos = new HashSet<>(eventsToShortDtosWithZeroStats(compilation.getEvents()));
        return compilationMapper.toDto(compilationRepository.save(compilation), eventShortDtos);
    }

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, Pageable pageable) {

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by("id").ascending()
        );

        List<Long> ids = compilationRepository.findIdsByPinned(pinned, sortedPageable);

        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<Compilation> compilationsWithEvents = compilationRepository.findAllByIdInWithEvents(ids);

        Map<Long, Compilation> byId = compilationsWithEvents.stream()
                .collect(Collectors.toMap(Compilation::getId, Function.identity()));

        List<Compilation> ordered = ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .toList();
        return ordered.stream()
                .map(compilation -> compilationMapper.toDto(compilation, new HashSet<>(eventsToShortDtosWithZeroStats(compilation.getEvents()))))
                .toList();
    }

    @Override
    public CompilationDto getCompilationDtoById(Long compId) {
        Compilation compilation = getCompilationById(compId);
        HashSet<EventShortDto> eventShortDtos = new HashSet<>(eventsToShortDtosWithZeroStats(compilation.getEvents()));
        return compilationMapper.toDto(compilation, eventShortDtos);
    }

    private List<EventShortDto> eventsToShortDtosWithZeroStats(Collection<Event> events) {
        if (events == null || events.isEmpty()) return List.of();
        List<Event> list = new ArrayList<>(events);
        List<Long> initiatorIds = list.stream().map(Event::getInitiatorId).distinct().toList();
        List<UserDto> userDtos = initiatorIds.isEmpty() ? List.of() : userClient.getUsersByIds(initiatorIds);
        if (userDtos == null) userDtos = emptyList();
        Map<Long, UserShortDto> initiatorMap = userDtos.stream()
                .collect(Collectors.toMap(UserDto::getId, u -> {
                    UserShortDto dto = new UserShortDto();
                    dto.setId(u.getId());
                    dto.setName(u.getName());
                    return dto;
                }));
        return list.stream()
                .map(event -> eventMapper.toShortDto(
                        event,
                        0,
                        0L,
                        initiatorMap.getOrDefault(event.getInitiatorId(), unknownUserShortDto(event.getInitiatorId()))
                ))
                .toList();
    }

    private static UserShortDto unknownUserShortDto(Long id) {
        UserShortDto dto = new UserShortDto();
        dto.setId(id);
        dto.setName("Unknown");
        return dto;
    }

    private Compilation getCompilationById(Long compId) {
        return compilationRepository.findByIdWithEvents(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с идентификатором " + compId + " не найдена"));
    }
}
