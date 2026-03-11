package ru.practicum.ewm.stats.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.stats.analyzer.entity.UserEventInteraction;
import ru.practicum.ewm.stats.analyzer.entity.UserEventInteractionId;

import java.util.List;
import java.util.Optional;

public interface UserEventInteractionRepository extends JpaRepository<UserEventInteraction, UserEventInteractionId> {

    List<UserEventInteraction> findByUserIdOrderByUpdatedAtDesc(Long userId, org.springframework.data.domain.Pageable pageable);

    List<UserEventInteraction> findByUserId(Long userId);

    Optional<UserEventInteraction> findByUserIdAndEventId(Long userId, Long eventId);

    @Query("SELECT u.eventId FROM UserEventInteraction u WHERE u.userId = :userId")
    List<Long> findEventIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT SUM(u.maxWeight) FROM UserEventInteraction u WHERE u.eventId = :eventId")
    Double sumMaxWeightByEventId(@Param("eventId") Long eventId);
}
