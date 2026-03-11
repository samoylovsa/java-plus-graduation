package ru.practicum.ewm.stats.analyzer.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
public class EventSimilarityId implements Serializable {

    private Long eventA;
    private Long eventB;

    public EventSimilarityId(Long eventA, Long eventB) {
        this.eventA = eventA;
        this.eventB = eventB;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventSimilarityId that = (EventSimilarityId) o;
        return Objects.equals(eventA, that.eventA) && Objects.equals(eventB, that.eventB);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventA, eventB);
    }
}
