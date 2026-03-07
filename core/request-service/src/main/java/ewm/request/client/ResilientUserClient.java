package ewm.request.client;

import ewm.request.exception.ServiceUnavailableException;
import ewm.user.client.UserClient;
import ewm.user.client.dto.UserDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResilientUserClient {

    private final UserClient userClient;

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByIdFallback")
    @Retry(name = "userService")
    public UserDto getUserById(Long id) {
        return userClient.getUserById(id);
    }

    public UserDto getUserByIdFallback(Long id, Throwable t) {
        log.warn("user-service unavailable for userId={}: {}", id, t.getMessage());
        throw new ServiceUnavailableException("User service is temporarily unavailable", t);
    }
}
