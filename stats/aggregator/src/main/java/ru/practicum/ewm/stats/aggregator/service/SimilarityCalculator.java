package ru.practicum.ewm.stats.aggregator.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


public class SimilarityCalculator {

    private final SimilarityState state;
    private final ActionWeightResolver weightResolver;

    public SimilarityCalculator(SimilarityState state, ActionWeightResolver weightResolver) {
        this.state = state;
        this.weightResolver = weightResolver;
    }

    public List<SimilarityUpdate> process(long userId, long eventId, double weight, Instant timestamp) {
        double oldWeight = state.getWeight(eventId, userId);
        if (weight <= oldWeight) {
            return List.of();
        }

        state.setWeight(eventId, userId, weight);
        double deltaSum = weight - oldWeight;
        state.addToEventSum(eventId, deltaSum);

        List<SimilarityUpdate> toEmit = new ArrayList<>();

        for (Long otherEventId : state.getEventIds()) {
            if (otherEventId.equals(eventId)) continue;
            if (!state.hasUserWeight(otherEventId, userId)) continue;

            double wB = state.getWeight(otherEventId, userId);
            double deltaMin = Math.min(weight, wB) - Math.min(oldWeight, wB);
            state.addToMinSum(eventId, otherEventId, deltaMin);

            double score = computeSimilarity(eventId, otherEventId);
            long eventA = Math.min(eventId, otherEventId);
            long eventB = Math.max(eventId, otherEventId);
            toEmit.add(new SimilarityUpdate(eventA, eventB, score, timestamp));
        }

        return toEmit;
    }

    private double computeSimilarity(long eventId1, long eventId2) {
        double sMin = state.getMinSum(eventId1, eventId2);
        double s1 = state.getEventSum(eventId1);
        double s2 = state.getEventSum(eventId2);
        if (s1 <= 0 || s2 <= 0) return 0.0;
        double denom = Math.sqrt(s1) * Math.sqrt(s2);
        if (denom <= 0) return 0.0;
        return sMin / denom;
    }

    public record SimilarityUpdate(long eventA, long eventB, double score, Instant timestamp) {}
}
