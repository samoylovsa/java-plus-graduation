package ru.practicum.ewm.stats.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.analyzer.entity.EventSimilarity;
import ru.practicum.ewm.stats.analyzer.entity.EventSimilarityId;
import ru.practicum.ewm.stats.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSimilarityUpdateService {

    private final EventSimilarityRepository repository;

    @Transactional
    public void process(EventSimilarityAvro avro) {
        long eventA = avro.getEventA();
        long eventB = avro.getEventB();
        double score = avro.getScore();
        Instant updatedAt = avro.getTimestamp() != null ? avro.getTimestamp() : Instant.now();

        EventSimilarity entity = repository.findById(new EventSimilarityId(eventA, eventB))
                .orElse(new EventSimilarity(eventA, eventB, score, updatedAt));
        entity.setScore(score);
        entity.setUpdatedAt(updatedAt);
        repository.save(entity);
        log.debug("Upserted event_similarity eventA={}, eventB={}, score={}", eventA, eventB, score);
    }
}
