package ru.practicum.ewm.stats.aggregator.service;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


@Component
public class SimilarityState {

    private final Map<Long, Map<Long, Double>> eventToUserToWeight = new HashMap<>();
    private final Map<Long, Double> eventToSum = new HashMap<>();
    private final Map<Long, Map<Long, Double>> minWeightsSums = new HashMap<>();

    public double getWeight(long eventId, long userId) {
        Map<Long, Double> userWeights = eventToUserToWeight.get(eventId);
        if (userWeights == null) return 0.0;
        return userWeights.getOrDefault(userId, 0.0);
    }

    public void setWeight(long eventId, long userId, double weight) {
        eventToUserToWeight
                .computeIfAbsent(eventId, k -> new HashMap<>())
                .put(userId, weight);
    }

    public double getEventSum(long eventId) {
        return eventToSum.getOrDefault(eventId, 0.0);
    }

    public void addToEventSum(long eventId, double delta) {
        eventToSum.merge(eventId, delta, Double::sum);
    }

    public double getMinSum(long eventA, long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        Map<Long, Double> inner = minWeightsSums.get(first);
        if (inner == null) return 0.0;
        return inner.getOrDefault(second, 0.0);
    }

    public void addToMinSum(long eventA, long eventB, double delta) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        minWeightsSums
                .computeIfAbsent(first, k -> new HashMap<>())
                .merge(second, delta, Double::sum);
    }

    public Set<Long> getEventIds() {
        return eventToUserToWeight.keySet();
    }

    public boolean hasUserWeight(long eventId, long userId) {
        Map<Long, Double> userWeights = eventToUserToWeight.get(eventId);
        return userWeights != null && userWeights.containsKey(userId);
    }
}
