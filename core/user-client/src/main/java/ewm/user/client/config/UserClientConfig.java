package ewm.user.client.config;

import ewm.common.exception.NotFoundException;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;

@Slf4j
public class UserClientConfig {

    private final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

    @Bean
    public ErrorDecoder userClientErrorDecoder() {
        return (methodKey, response) -> {
            if (response.status() == 404) {
                log.debug("User not found from user-service: {} -> 404", methodKey);
                return new NotFoundException("User not found");
            }
            if (response.status() >= 500) {
                log.warn("user-service error: {} -> {}", methodKey, response.status());
            }
            return defaultDecoder.decode(methodKey, response);
        };
    }
}
