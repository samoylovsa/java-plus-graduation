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
@Table(name = "user_event_interaction", schema = "stats")
@IdClass(UserEventInteractionId.class)
@Getter
@Setter
@NoArgsConstructor
public class UserEventInteraction {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Id
    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "max_weight", nullable = false)
    private Double maxWeight;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UserEventInteraction(Long userId, Long eventId, Double maxWeight, Instant updatedAt) {
        this.userId = userId;
        this.eventId = eventId;
        this.maxWeight = maxWeight;
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserEventInteraction that = (UserEventInteraction) o;
        return Objects.equals(userId, that.userId) && Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, eventId);
    }
}
