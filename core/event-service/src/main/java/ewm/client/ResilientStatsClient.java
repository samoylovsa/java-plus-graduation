package ewm.client;

import dto.GetStatsDto;
import dto.SaveHitDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import stats.client.StatsClient;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class ResilientStatsClient {

    private final StatsClient statsClient;

    @CircuitBreaker(name = "statsServer", fallbackMethod = "saveHitFallback")
    @Retry(name = "statsServer")
    public void saveHit(SaveHitDto saveHitDto) {
        statsClient.saveHit(saveHitDto);
    }

    public void saveHitFallback(SaveHitDto saveHitDto, Throwable t) {
        log.warn("stats-server unavailable, skipping saveHit: {}", t.getMessage());
    }

    @CircuitBreaker(name = "statsServer", fallbackMethod = "getStatsFallback")
    @Retry(name = "statsServer")
    public List<GetStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        return statsClient.getStats(start, end, uris, unique);
    }

    public List<GetStatsDto> getStatsFallback(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique, Throwable t) {
        log.warn("stats-server unavailable, using fallback (0 views): {}", t.getMessage());
        return Collections.emptyList();
    }
}
