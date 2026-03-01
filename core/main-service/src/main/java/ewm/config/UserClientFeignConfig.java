package ewm.config;

import ewm.exception.NotFoundException;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class UserClientFeignConfig {

    private final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

    @Bean
    public ErrorDecoder userServiceErrorDecoder() {
        return (methodKey, response) -> {
            if (methodKey != null && methodKey.startsWith("UserClient#") && response.status() == 404) {
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
