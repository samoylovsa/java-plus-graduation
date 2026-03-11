package ru.practicum.ewm.stats.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.analyzer.entity.UserEventInteraction;
import ru.practicum.ewm.stats.analyzer.repository.UserEventInteractionRepository;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActionUpdateService {

    private final UserEventInteractionRepository repository;
    private final ActionWeightResolver weightResolver;

    @Transactional
    public void process(UserActionAvro action) {
        long userId = action.getUserId();
        long eventId = action.getEventId();
        double weight = weightResolver.toWeight(action.getActionType());
        Instant timestamp = action.getTimestamp() != null ? action.getTimestamp() : Instant.now();

        var existing = repository.findByUserIdAndEventId(userId, eventId);
        if (existing.isEmpty()) {
            repository.save(new UserEventInteraction(userId, eventId, weight, timestamp));
            log.debug("Inserted user_event_interaction userId={}, eventId={}, maxWeight={}", userId, eventId, weight);
        } else {
            UserEventInteraction e = existing.get();
            if (weight > e.getMaxWeight()) {
                e.setMaxWeight(weight);
                e.setUpdatedAt(timestamp);
                repository.save(e);
                log.debug("Updated user_event_interaction userId={}, eventId={}, maxWeight={}", userId, eventId, weight);
            }
        }
    }
}
