package ru.practicum.ewm.stats.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.stats.analyzer.entity.EventSimilarity;
import ru.practicum.ewm.stats.analyzer.entity.EventSimilarityId;

import java.util.List;
import java.util.Optional;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, EventSimilarityId> {

    @Query("SELECT e FROM EventSimilarity e WHERE e.eventA = :eventId OR e.eventB = :eventId ORDER BY e.score DESC")
    List<EventSimilarity> findAllByEventIdOrderByScoreDesc(@Param("eventId") Long eventId);

    @Query("SELECT e FROM EventSimilarity e WHERE (e.eventA = :id1 AND e.eventB = :id2) OR (e.eventA = :id2 AND e.eventB = :id1)")
    Optional<EventSimilarity> findSimilarityBetween(@Param("id1") Long eventId1, @Param("id2") Long eventId2);
}
