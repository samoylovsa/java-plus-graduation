package ewm.comment.client;

import ewm.user.client.UserClient;
import ewm.user.client.dto.UserDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResilientUserClient {

    private static final String UNKNOWN_NAME = "Unknown";

    private final UserClient userClient;

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByIdFallback")
    @Retry(name = "userService")
    public UserDto getUserById(Long id) {
        return userClient.getUserById(id);
    }

    public UserDto getUserByIdFallback(Long id, Throwable t) {
        log.warn("user-service unavailable, using fallback for userId={}: {}", id, t.getMessage());
        return new UserDto(id, UNKNOWN_NAME, null);
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "getUsersByIdsFallback")
    @Retry(name = "userService")
    public List<UserDto> getUsersByIds(List<Long> ids) {
        return userClient.getUsersByIds(ids);
    }

    public List<UserDto> getUsersByIdsFallback(List<Long> ids, Throwable t) {
        log.warn("user-service unavailable, using fallback for {} ids: {}", ids != null ? ids.size() : 0, t.getMessage());
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream()
                .map(id -> new UserDto(id, UNKNOWN_NAME, null))
                .collect(Collectors.toList());
    }
}
