package ewm.client;

import ewm.user.client.UserClient;
import ewm.user.client.dto.UserDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ResilientUserClient {

    private final UserClient userClient;

    @CircuitBreaker(name = "userService")
    @Retry(name = "userService")
    public UserDto getUserById(Long id) {
        return userClient.getUserById(id);
    }

    @CircuitBreaker(name = "userService")
    @Retry(name = "userService")
    public List<UserDto> getUsersByIds(List<Long> ids) {
        return userClient.getUsersByIds(ids);
    }
}
