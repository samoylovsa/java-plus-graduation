package ewm.request.client;

import ewm.user.client.UserClient;
import ewm.user.client.dto.UserDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResilientUserClient {

    private final UserClient userClient;

    @CircuitBreaker(name = "userService")
    @Retry(name = "userService")
    public UserDto getUserById(Long id) {
        return userClient.getUserById(id);
    }
}
