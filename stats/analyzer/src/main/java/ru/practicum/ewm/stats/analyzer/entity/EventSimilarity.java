package ru.practicum.ewm.stats.analyzer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "event_similarity", schema = "stats")
@IdClass(EventSimilarityId.class)
@Getter
@Setter
@NoArgsConstructor
public class EventSimilarity {

    @Id
    @Column(name = "event_a", nullable = false)
    private Long eventA;

    @Id
    @Column(name = "event_b", nullable = false)
    private Long eventB;

    @Column(name = "score", nullable = false)
    private Double score;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public EventSimilarity(Long eventA, Long eventB, Double score, Instant updatedAt) {
        this.eventA = eventA;
        this.eventB = eventB;
        this.score = score;
        this.updatedAt = updatedAt;
    }

    public Long getOtherEventId(long eventId) {
        if (eventA != null && eventA == eventId) return eventB;
        if (eventB != null && eventB == eventId) return eventA;
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventSimilarity that = (EventSimilarity) o;
        return Objects.equals(eventA, that.eventA) && Objects.equals(eventB, that.eventB);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventA, eventB);
    }
}
