package ru.practicum.ewm.stats.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.analyzer.entity.EventSimilarity;
import ru.practicum.ewm.stats.analyzer.entity.UserEventInteraction;
import ru.practicum.ewm.stats.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.stats.analyzer.repository.UserEventInteractionRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationsQueryService {

    private final UserEventInteractionRepository userEventRepo;
    private final EventSimilarityRepository similarityRepo;

    @Value("${app.recommendations.recent-interactions-limit:20}")
    private int recentInteractionsLimit;

    @Value("${app.recommendations.similar-candidates-limit:100}")
    private int similarCandidatesLimit;

    @Value("${app.recommendations.nearest-neighbors-k:10}")
    private int nearestNeighborsK;

    @Transactional(readOnly = true)
    public List<EventScore> getSimilarEvents(long eventId, long userId, int maxResults) {
        Set<Long> userInteractedEventIds = new HashSet<>(userEventRepo.findEventIdsByUserId(userId));
        return similarityRepo.findAllByEventIdOrderByScoreDesc(eventId).stream()
                .map(es -> {
                    Long other = es.getOtherEventId(eventId);
                    return other != null && !userInteractedEventIds.contains(other)
                            ? new EventScore(other, es.getScore()) : null;
                })
                .filter(java.util.Objects::nonNull)
                .limit(maxResults)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventScore> getRecommendationsForUser(long userId, int maxResults) {
        List<UserEventInteraction> allUserInteractions = userEventRepo.findByUserId(userId);
        if (allUserInteractions.isEmpty()) {
            return List.of();
        }
        Map<Long, Double> userWeights = allUserInteractions.stream()
                .collect(Collectors.toMap(UserEventInteraction::getEventId, UserEventInteraction::getMaxWeight, (a, b) -> a));
        Set<Long> userEventIds = userWeights.keySet();

        List<UserEventInteraction> recent = userEventRepo.findByUserIdOrderByUpdatedAtDesc(
                userId, PageRequest.of(0, recentInteractionsLimit));

        Map<Long, Double> candidateBestSimilarity = new java.util.HashMap<>();
        for (UserEventInteraction ue : recent) {
            similarityRepo.findAllByEventIdOrderByScoreDesc(ue.getEventId()).stream()
                    .limit(similarCandidatesLimit)
                    .forEach(es -> {
                        Long other = es.getOtherEventId(ue.getEventId());
                        if (other != null && !userEventIds.contains(other)) {
                            candidateBestSimilarity.merge(other, es.getScore(), Math::max);
                        }
                    });
        }

        List<Long> candidates = candidateBestSimilarity.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(Math.max(maxResults * 2, similarCandidatesLimit))
                .map(Map.Entry::getKey)
                .toList();

        List<EventScore> result = new ArrayList<>();
        for (Long candidateId : candidates) {
            double predicted = predictRating(userId, candidateId, userWeights);
            result.add(new EventScore(candidateId, predicted));
        }
        return result.stream()
                .sorted(Comparator.comparingDouble(EventScore::score).reversed())
                .limit(maxResults)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventScore> getInteractionsCount(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return List.of();
        }
        return eventIds.stream()
                .map(id -> {
                    Double sum = userEventRepo.sumMaxWeightByEventId(id);
                    return new EventScore(id, sum != null ? sum : 0.0);
                })
                .toList();
    }

    private double predictRating(long userId, long candidateEventId, Map<Long, Double> userWeights) {
        List<EventSimilarity> similarPairs = similarityRepo.findAllByEventIdOrderByScoreDesc(candidateEventId);
        double sumWeighted = 0;
        double sumSimilarity = 0;
        int k = 0;
        for (EventSimilarity es : similarPairs) {
            if (k >= nearestNeighborsK) break;
            Long otherId = es.getOtherEventId(candidateEventId);
            if (otherId == null) continue;
            Double weight = userWeights.get(otherId);
            if (weight == null) continue;
            sumWeighted += weight * es.getScore();
            sumSimilarity += es.getScore();
            k++;
        }
        if (sumSimilarity <= 0) return 0.0;
        return sumWeighted / sumSimilarity;
    }


    public record EventScore(long eventId, double score) {}
}
