package ru.practicum.ewm.stats.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.aggregator.producer.EventSimilarityProducer;
import ru.practicum.ewm.stats.aggregator.service.SimilarityCalculator.SimilarityUpdate;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActionProcessor {

    private final SimilarityState state;
    private final ActionWeightResolver weightResolver;
    private final EventSimilarityProducer producer;

    public void process(UserActionAvro action) {
        long userId = action.getUserId();
        long eventId = action.getEventId();
        double weight = weightResolver.toWeight(action.getActionType());
        var timestamp = action.getTimestamp();

        List<SimilarityUpdate> updates = calculator().process(userId, eventId, weight, timestamp);

        for (SimilarityUpdate u : updates) {
            producer.send(u.eventA(), u.eventB(), u.score(), u.timestamp());
        }

        if (!updates.isEmpty()) {
            log.debug("Processed action userId={}, eventId={}, emitted {} similarity updates", userId, eventId, updates.size());
        }
    }

    private SimilarityCalculator calculator() {
        return new SimilarityCalculator(state, weightResolver);
    }
}